package com.douduoyu.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.coroutines.cancellation.CancellationException

private data class StyleProfile(
    val sampleSide: Int,
    val backgroundThreshold: Double,
    val dominanceThreshold: Double,
    val candidateTargetRatio: Double,
    val candidateDistanceFactor: Double,
    val postStrengthBias: Int,
    val useAverageFallback: Boolean,
)

private data class SampledGrid(
    val samples: IntArray,
    val sampleCounts: ByteArray,
    val averageRgb: IntArray,
    val backgroundCounts: ByteArray,
    val samplesPerCell: Int,
) {
    val cellCount: Int
        get() = sampleCounts.size

    fun sampleCount(cellIndex: Int): Int = sampleCounts[cellIndex].toInt() and 0xFF

    fun backgroundShare(cellIndex: Int): Double =
        (backgroundCounts[cellIndex].toInt() and 0xFF) / samplesPerCell.toDouble()
}

private class QuantizedPaletteLookup(
    private val candidates: List<PaletteColor>,
) {
    private val nearestIndices = IntArray(1 shl 15) { -1 }

    fun nearestIndex(rgb: Int): Int {
        val key = (((rgb ushr 16 and 0xFF) ushr 3) shl 10) or
            (((rgb ushr 8 and 0xFF) ushr 3) shl 5) or
            ((rgb and 0xFF) ushr 3)
        val cached = nearestIndices[key]
        if (cached >= 0) {
            return cached
        }

        val red = (((key ushr 10) and 0x1F) shl 3) + 4
        val green = (((key ushr 5) and 0x1F) shl 3) + 4
        val blue = ((key and 0x1F) shl 3) + 4
        return BeadPalette.nearestIndex(red.coerceAtMost(255), green.coerceAtMost(255), blue.coerceAtMost(255), candidates)
            .also { nearestIndices[key] = it }
    }
}

private data class RankedPaletteColor(
    val color: PaletteColor,
    val count: Int,
    val score: Double,
)

data class ChartGridEstimate(
    val gridWidth: Int,
    val gridHeight: Int,
    val cropLeft: Int,
    val cropTop: Int,
    val cropWidth: Int,
    val cropHeight: Int,
    val confidence: Double,
)

private data class GridLineDetection(
    val lines: List<Double>,
    val spacing: Double,
    val centers: List<Double>,
)

private data class ChartCellSample(
    val rgb: IntArray?,
    val isTransparent: Boolean,
    val forcedColorCode: String? = null,
)

object PatternEngine {
    const val DEMO_PROJECT_ID = "demo-fish"
    private const val DEMO_SHORT_EDGE = 18
    private const val CHART_RECOGNITION_MAX_EDGE = 1200

    private val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

    private val cartoonProfile =
        StyleProfile(
            sampleSide = 7,
            backgroundThreshold = 0.72,
            dominanceThreshold = 0.34,
            candidateTargetRatio = 0.58,
            candidateDistanceFactor = 1.35,
            postStrengthBias = 1,
            useAverageFallback = false,
        )

    private val realisticProfile =
        StyleProfile(
            sampleSide = 5,
            backgroundThreshold = 0.58,
            dominanceThreshold = 0.26,
            candidateTargetRatio = 0.95,
            candidateDistanceFactor = 0.65,
            postStrengthBias = -1,
            useAverageFallback = true,
        )

    fun formatTimestamp(value: Long): String = formatter.format(Instant.ofEpochMilli(value))

    fun cropPreview(source: Bitmap, settings: GenerationSettings, previewWidth: Int = 720): Bitmap {
        val targetWidth = settings.width.coerceAtLeast(1)
        val targetHeight = settings.height.coerceAtLeast(1)
        val previewHeight = max(160, (previewWidth * targetHeight / targetWidth.toFloat()).roundToInt())
        val cropRect = cropRect(
            imageWidth = source.width,
            imageHeight = source.height,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            zoom = settings.cropZoom,
            offsetX = settings.cropX,
            offsetY = settings.cropY,
        )

        val output = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        canvas.drawBitmap(source, cropRect, Rect(0, 0, previewWidth, previewHeight), paint)
        return output
    }

    fun generateProject(
        source: Bitmap,
        settings: GenerationSettings,
        shouldCancel: () -> Boolean = { false },
    ): BeadProject {
        checkCancelled(shouldCancel)
        val outputDimensions = OutputSizePolicy.constrainDimensions(settings.width, settings.height)
        val outputWidth = outputDimensions.width
        val outputHeight = outputDimensions.height
        val outputCellCountLong = outputWidth.toLong() * outputHeight.toLong()
        require(outputCellCountLong in 1L..OutputSizePolicy.MAX_TOTAL_CELLS.toLong()) {
            "Pattern dimensions exceed the supported cell count."
        }
        val outputCellCount = outputCellCountLong.toInt()
        val cropRect = cropRect(
            imageWidth = source.width,
            imageHeight = source.height,
            targetWidth = outputWidth,
            targetHeight = outputHeight,
            zoom = settings.cropZoom,
            offsetX = settings.cropX,
            offsetY = settings.cropY,
        )

        val activePalette = BeadPalette.palette(settings.colorScheme, settings.customColorSchemeId)
        val paletteLimit = settings.maxColors.coerceAtMost(activePalette.size).coerceAtLeast(2)
        val baseProfile = styleProfile(settings.generationStyle)
        val profile = baseProfile.copy(
            sampleSide = effectiveSampleSide(
                baseSampleSide = baseProfile.sampleSide,
                cellCount = outputCellCount,
            ),
        )
        val requestedSpeckle = settings.speckleReduction.coerceIn(0, 4)
        val speckleStrength =
            if (requestedSpeckle > 0) {
                clampStrength(requestedSpeckle + profile.postStrengthBias)
            } else {
                0
            }

        val detectedBackgroundColor = estimateBackgroundColor(source, cropRect)
        checkCancelled(shouldCancel)
        val samplingWidth = samplingBitmapDimension(cropRect.width(), outputWidth, profile.sampleSide)
        val samplingHeight = samplingBitmapDimension(cropRect.height(), outputHeight, profile.sampleSide)
        val samplingBitmap = Bitmap.createBitmap(samplingWidth, samplingHeight, Bitmap.Config.ARGB_8888)
        val pixels =
            try {
                Canvas(samplingBitmap).drawBitmap(
                    source,
                    cropRect,
                    Rect(0, 0, samplingWidth, samplingHeight),
                    Paint().apply {
                        isAntiAlias = false
                        isFilterBitmap = false
                        isDither = false
                    },
                )
                checkCancelled(shouldCancel)
                val sampledPixels = IntArray(samplingWidth * samplingHeight)
                samplingBitmap.getPixels(
                    sampledPixels,
                    0,
                    samplingWidth,
                    0,
                    0,
                    samplingWidth,
                    samplingHeight,
                )
                sampledPixels
            } finally {
                samplingBitmap.recycle()
            }
        checkCancelled(shouldCancel)
        val sampledGrid =
            sampleGridCells(
                pixels = pixels,
                sourceWidth = samplingWidth,
                sourceHeight = samplingHeight,
                outputWidth = outputWidth,
                outputHeight = outputHeight,
                settings = settings,
                detectedBackgroundColor = detectedBackgroundColor,
                profile = profile,
                speckleStrength = speckleStrength,
                shouldCancel = shouldCancel,
            )
        val ranked = rankPaletteColors(sampledGrid, paletteLimit, activePalette, profile, shouldCancel)
        val candidates = selectCandidateColors(
            ranked = ranked,
            maxColors = paletteLimit,
            activePalette = activePalette,
            strength = speckleStrength,
            profile = profile,
        )
        val chosenCells = chooseCellColors(sampledGrid, candidates, profile, shouldCancel)
        checkCancelled(shouldCancel)
        val merged = mergeSimilarColors(chosenCells, speckleStrength, candidates)
        checkCancelled(shouldCancel)
        val compact = reduceTinyRegions(merged, outputWidth, outputHeight, speckleStrength, candidates, shouldCancel)
        val reducedIndices = reduceSpeckles(compact, outputWidth, outputHeight, speckleStrength, candidates, shouldCancel)
        checkCancelled(shouldCancel)
        val reduced = ArrayList<String?>(outputCellCount)
        reducedIndices.forEach { colorIndex ->
            reduced += if (colorIndex >= 0) candidates[colorIndex].id else null
        }

        return BeadProject(
            title = settings.title.ifBlank { "\u8c46\u8c46\u9c7c\u56fe\u7eb8" },
            width = outputWidth,
            height = outputHeight,
            cells = reduced,
            paletteMode = settings.colorScheme.paletteMode,
            colorScheme = settings.colorScheme,
            customColorSchemeId = settings.customColorSchemeId,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun estimateChartGrid(
        source: Bitmap,
        maxEdge: Int = CHART_RECOGNITION_MAX_EDGE,
    ): ChartGridEstimate {
        val normalized = normalizeBitmapForRecognition(source, maxEdge = maxEdge)
        val pixels = IntArray(normalized.width * normalized.height)
        normalized.getPixels(pixels, 0, normalized.width, 0, 0, normalized.width, normalized.height)
        val verticalDetection = detectGridLines(pixels, normalized.width, normalized.height, isVertical = true)
        val horizontalDetection = detectGridLines(pixels, normalized.width, normalized.height, isVertical = false)
        if (verticalDetection.lines.isEmpty() || horizontalDetection.lines.isEmpty()) {
            return ChartGridEstimate(
                gridWidth = 0,
                gridHeight = 0,
                cropLeft = 0,
                cropTop = 0,
                cropWidth = normalized.width,
                cropHeight = normalized.height,
                confidence = 0.0,
            )
        }

        val verticalLines = stripCoordinateBands(verticalDetection.lines, verticalDetection.spacing)
        val horizontalLines = stripCoordinateBands(horizontalDetection.lines, horizontalDetection.spacing)
        if (verticalLines.size < 2 || horizontalLines.size < 2) {
            return ChartGridEstimate(
                gridWidth = 0,
                gridHeight = 0,
                cropLeft = 0,
                cropTop = 0,
                cropWidth = normalized.width,
                cropHeight = normalized.height,
                confidence = 0.0,
            )
        }

        val cropLeft = verticalLines.first().roundToInt().coerceIn(0, normalized.width - 1)
        val cropTop = horizontalLines.first().roundToInt().coerceIn(0, normalized.height - 1)
        val cropRight = verticalLines.last().roundToInt().coerceIn(cropLeft + 1, normalized.width)
        val cropBottom = horizontalLines.last().roundToInt().coerceIn(cropTop + 1, normalized.height)
        val gridWidth = (verticalLines.size - 1).coerceAtLeast(0)
        val gridHeight = (horizontalLines.size - 1).coerceAtLeast(0)
        val confidence = estimateGridConfidence(verticalDetection, horizontalDetection, verticalLines, horizontalLines)

        return ChartGridEstimate(
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            cropLeft = cropLeft,
            cropTop = cropTop,
            cropWidth = (cropRight - cropLeft).coerceAtLeast(1),
            cropHeight = (cropBottom - cropTop).coerceAtLeast(1),
            confidence = (confidence * 100.0).roundToInt() / 100.0,
        )
    }

    fun recognizePatternChart(
        source: Bitmap,
        settings: GenerationSettings,
        title: String = settings.title,
        ignoreWatermark: Boolean = true,
    ): BeadProject {
        val activePalette = BeadPalette.palette(settings.colorScheme, settings.customColorSchemeId)
        val estimate = estimateChartGrid(source)
        val normalized = normalizeBitmapForRecognition(source, maxEdge = CHART_RECOGNITION_MAX_EDGE)
        val pixels = IntArray(normalized.width * normalized.height)
        normalized.getPixels(pixels, 0, normalized.width, 0, 0, normalized.width, normalized.height)

        val gridWidth =
            if (estimate.gridWidth >= 1) {
                estimate.gridWidth
            } else {
                settings.width.coerceAtLeast(1)
            }
        val gridHeight =
            if (estimate.gridHeight >= 1) {
                estimate.gridHeight
            } else {
                settings.height.coerceAtLeast(1)
            }

        val cropLeft = estimate.cropLeft.coerceIn(0, normalized.width - 1)
        val cropTop = estimate.cropTop.coerceIn(0, normalized.height - 1)
        val cropWidth = estimate.cropWidth.coerceAtLeast(1)
        val cropHeight = estimate.cropHeight.coerceAtLeast(1)

        val cells = ArrayList<String?>(gridWidth * gridHeight)
        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val left = cropLeft + cropWidth * x / gridWidth.toFloat()
                val top = cropTop + cropHeight * y / gridHeight.toFloat()
                val cellWidth = cropWidth / gridWidth.toFloat()
                val cellHeight = cropHeight / gridHeight.toFloat()
                val sampled =
                    sampleChartCell(
                        pixels = pixels,
                        imageWidth = normalized.width,
                        imageHeight = normalized.height,
                        left = left,
                        top = top,
                        width = cellWidth,
                        height = cellHeight,
                        ignoreWatermark = ignoreWatermark,
                    )
                val colorCode =
                    when {
                        sampled.isTransparent -> null
                        sampled.forcedColorCode != null -> sampled.forcedColorCode
                        sampled.rgb != null -> {
                            BeadPalette.nearest(
                                sampled.rgb[0],
                                sampled.rgb[1],
                                sampled.rgb[2],
                                activePalette,
                            ).id
                        }
                        else -> null
                    }
                cells += colorCode
            }
        }

        return BeadProject(
            title = title.ifBlank { "识别图纸" },
            width = gridWidth,
            height = gridHeight,
            cells = cells,
            paletteMode = settings.colorScheme.paletteMode,
            colorScheme = settings.colorScheme,
            customColorSchemeId = settings.customColorSchemeId,
            sourceKind = ProjectSourceKind.ChartRecognition,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun materialStats(cells: List<String?>): List<MaterialStat> {
        val counts = linkedMapOf<String, Int>()
        for (code in cells) {
            if (code == null) {
                continue
            }
            counts[code] = (counts[code] ?: 0) + 1
        }

        return counts.entries
            .mapNotNull { (code, count) ->
                val color = BeadPalette.find(code) ?: return@mapNotNull null
                MaterialStat(
                    code = code,
                    displayCode = color.code,
                    count = count,
                    argb = color.argb,
                    series = color.series,
                )
            }
            .sortedWith(compareByDescending<MaterialStat> { it.count }.thenBy { it.displayCode })
    }

    fun updateCell(cells: List<String?>, index: Int, code: String?): List<String?> {
        if (index !in cells.indices || cells[index] == code) {
            return cells
        }
        return cells.toMutableList().also { it[index] = code }
    }

    fun demoProject(): BeadProject {
        val rows = listOf(
            "................",
            "......bbbb......",
            "....bbggggbb....",
            "...bbgggggwhb...",
            "..bbgggggggwhb..",
            "..bbgggggggwhb..",
            "...bbgggggwhb...",
            "....bbggggbb....",
            "......bbbb......",
            "................",
        )
        val mapping = mapOf(
            'b' to "coco:B09",
            'g' to "coco:F10",
            'w' to "coco:A02",
            'h' to "coco:B06",
        )
        val width = rows.first().length
        val cells = buildList<String?> {
            rows.forEach { row ->
                row.forEach { token ->
                    add(mapping[token])
                }
            }
        }

        return BeadProject(
            id = DEMO_PROJECT_ID,
            title = "\u793a\u4f8b\u5c0f\u9c7c",
            width = width,
            height = rows.size,
            cells = cells,
            paletteMode = PaletteMode.Basic,
            colorScheme = ColorSchemePreset.MardBrand221,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun demoProjectFromSource(
        source: Bitmap,
        title: String,
    ): BeadProject {
        val safeTitle = title.ifBlank { "\u793a\u4f8b\u65b9\u6848" }
        val settings =
            fitSettingsForShortEdge(
                current =
                    GenerationSettings(
                        title = safeTitle,
                        width = DEMO_SHORT_EDGE,
                        height = DEMO_SHORT_EDGE,
                        maxColors = ColorSchemePreset.MardBrand221.maxColors,
                        paletteMode = ColorSchemePreset.MardBrand221.paletteMode,
                        colorScheme = ColorSchemePreset.MardBrand221,
                        backgroundMode = BackgroundMode.Keep,
                        tolerance = 32,
                        speckleReduction = 1,
                        generationStyle = GenerationStyle.Cartoon,
                    ),
                sourceWidth = source.width,
                sourceHeight = source.height,
                shortEdge = DEMO_SHORT_EDGE,
            )
        return generateProject(source, settings).copy(
            id = DEMO_PROJECT_ID,
            title = safeTitle,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun fitSettingsForShortEdge(
        current: GenerationSettings,
        sourceWidth: Int,
        sourceHeight: Int,
        shortEdge: Int,
    ): GenerationSettings {
        val targetEdge = shortEdge.coerceAtLeast(1)
        val safeSourceWidth = sourceWidth.coerceAtLeast(1)
        val safeSourceHeight = sourceHeight.coerceAtLeast(1)
        return if (safeSourceWidth >= safeSourceHeight) {
            current.copy(
                width = (targetEdge * safeSourceWidth / safeSourceHeight.toFloat()).roundToInt().coerceAtLeast(1),
                height = targetEdge,
            )
        } else {
            current.copy(
                width = targetEdge,
                height = (targetEdge * safeSourceHeight / safeSourceWidth.toFloat()).roundToInt().coerceAtLeast(1),
            )
        }
    }

    private fun styleProfile(style: GenerationStyle): StyleProfile {
        return if (style == GenerationStyle.Realistic) realisticProfile else cartoonProfile
    }

    private fun effectiveSampleSide(baseSampleSide: Int, cellCount: Int): Int {
        return when {
            cellCount >= 40_000 -> min(baseSampleSide, 3)
            cellCount >= 16_000 -> min(baseSampleSide, 5)
            else -> baseSampleSide
        }
    }

    internal fun samplingBitmapDimension(
        cropDimension: Int,
        outputDimension: Int,
        sampleSide: Int,
    ): Int {
        val safeCropDimension = cropDimension.coerceAtLeast(1)
        val requestedSamples = outputDimension.coerceAtLeast(1).toLong() * sampleSide.coerceAtLeast(1)
        return min(safeCropDimension.toLong(), requestedSamples).coerceAtLeast(1L).toInt()
    }

    private fun sampleGridCells(
        pixels: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        outputWidth: Int,
        outputHeight: Int,
        settings: GenerationSettings,
        detectedBackgroundColor: IntArray,
        profile: StyleProfile,
        speckleStrength: Int,
        shouldCancel: () -> Boolean,
    ): SampledGrid {
        val cellCountLong = outputWidth.toLong() * outputHeight.toLong()
        require(cellCountLong in 1L..OutputSizePolicy.MAX_TOTAL_CELLS.toLong())
        val cellCount = cellCountLong.toInt()
        val totalSamples = profile.sampleSide * profile.sampleSide
        val sampleStorageSize = (cellCountLong * totalSamples.toLong()).also {
            require(it <= Int.MAX_VALUE.toLong())
        }.toInt()
        val samples = IntArray(sampleStorageSize)
        val sampleCounts = ByteArray(cellCount)
        val averageRgb = IntArray(cellCount) { -1 }
        val backgroundCounts = ByteArray(cellCount)
        val sampleXPositions = IntArray(outputWidth * profile.sampleSide)
        val sampleYPositions = IntArray(outputHeight * profile.sampleSide)

        for (x in 0 until outputWidth) {
            for (sampleX in 0 until profile.sampleSide) {
                sampleXPositions[x * profile.sampleSide + sampleX] =
                    min(
                        sourceWidth - 1,
                        max(
                            0,
                            floor(((x + (sampleX + 0.5) / profile.sampleSide) / outputWidth) * sourceWidth).toInt(),
                        ),
                    )
            }
        }
        for (y in 0 until outputHeight) {
            for (sampleY in 0 until profile.sampleSide) {
                sampleYPositions[y * profile.sampleSide + sampleY] =
                    min(
                        sourceHeight - 1,
                        max(
                            0,
                            floor(((y + (sampleY + 0.5) / profile.sampleSide) / outputHeight) * sourceHeight).toInt(),
                        ),
                    )
            }
        }

        for (y in 0 until outputHeight) {
            checkCancelled(shouldCancel)
            for (x in 0 until outputWidth) {
                val cellIndex = y * outputWidth + x
                val sampleOffset = cellIndex * totalSamples
                var sampleCount = 0
                var backgroundCount = 0
                var redTotal = 0
                var greenTotal = 0
                var blueTotal = 0

                for (sampleY in 0 until profile.sampleSide) {
                    for (sampleX in 0 until profile.sampleSide) {
                        val sourceX = sampleXPositions[x * profile.sampleSide + sampleX]
                        val sourcePixelY = sampleYPositions[y * profile.sampleSide + sampleY]
                        val pixel = pixels[sourcePixelY * sourceWidth + sourceX]
                        val alpha = pixel ushr 24 and 0xFF
                        val red = pixel shr 16 and 0xFF
                        val green = pixel shr 8 and 0xFF
                        val blue = pixel and 0xFF
                        if (isBackgroundSample(alpha, red, green, blue, settings, detectedBackgroundColor)) {
                            backgroundCount += 1
                            continue
                        }
                        val simplified = simplifySourceRgb(red, green, blue, speckleStrength)
                        samples[sampleOffset + sampleCount++] = simplified
                        redTotal += simplified ushr 16 and 0xFF
                        greenTotal += simplified ushr 8 and 0xFF
                        blueTotal += simplified and 0xFF
                    }
                }

                sampleCounts[cellIndex] = sampleCount.toByte()
                backgroundCounts[cellIndex] = backgroundCount.toByte()
                if (sampleCount > 0) {
                    averageRgb[cellIndex] =
                        packRgb(
                            (redTotal / sampleCount.toDouble()).roundToInt(),
                            (greenTotal / sampleCount.toDouble()).roundToInt(),
                            (blueTotal / sampleCount.toDouble()).roundToInt(),
                        )
                }
            }
        }

        return SampledGrid(
            samples = samples,
            sampleCounts = sampleCounts,
            averageRgb = averageRgb,
            backgroundCounts = backgroundCounts,
            samplesPerCell = totalSamples,
        )
    }

    private fun rankPaletteColors(
        sampledGrid: SampledGrid,
        maxColors: Int,
        activePalette: List<PaletteColor>,
        profile: StyleProfile,
        shouldCancel: () -> Boolean,
    ): List<RankedPaletteColor> {
        val counts = IntArray(activePalette.size)
        val paletteLookup = QuantizedPaletteLookup(activePalette)
        var total = 0
        val cellStride = max(1, (sampledGrid.cellCount + 15_999) / 16_000)

        for (index in 0 until sampledGrid.cellCount) {
            if (index % 512 == 0) {
                checkCancelled(shouldCancel)
            }
            if (index % cellStride != 0) {
                continue
            }
            val sampleCount = sampledGrid.sampleCount(index)
            if (sampledGrid.backgroundShare(index) >= profile.backgroundThreshold || sampleCount == 0) {
                continue
            }
            val sampleOffset = index * sampledGrid.samplesPerCell
            for (sampleIndex in 0 until sampleCount) {
                val rgb = sampledGrid.samples[sampleOffset + sampleIndex]
                total += 1
                val colorIndex = paletteLookup.nearestIndex(rgb)
                counts[colorIndex] += 1
            }
        }

        val rows =
            activePalette.mapIndexedNotNull { index, color ->
                val count = counts[index]
                if (count <= 0) {
                    return@mapIndexedNotNull null
                }
                val chroma = colorChroma(color.red, color.green, color.blue)
                val share = if (total > 0) count / total.toDouble() else 0.0
                val lightness = luminance(color.red, color.green, color.blue)
                val accentBoost =
                    when {
                        chroma > 48 -> 2.25
                        chroma > 28 -> 1.4
                        else -> 1.0
                    }
                val minorityAccentBoost = if (chroma > 58 && share < 0.018) 2.45 else 1.0
                val darkLineBoost = if (lightness < 48) 1.55 else 1.0
                val lightNeutralPenalty = if (lightness > 210 && chroma < 22) 0.82 else 1.0
                RankedPaletteColor(
                    color = color,
                    count = count,
                    score = count * accentBoost * minorityAccentBoost * darkLineBoost * lightNeutralPenalty,
                )
            }
                .sortedWith(compareByDescending<RankedPaletteColor> { it.score }.thenByDescending { it.count })

        return if (rows.isNotEmpty()) {
            rows
        } else {
            activePalette.take(maxColors.coerceAtLeast(2)).map { RankedPaletteColor(it, 1, 1.0) }
        }
    }

    private fun selectCandidateColors(
        ranked: List<RankedPaletteColor>,
        maxColors: Int,
        activePalette: List<PaletteColor>,
        strength: Int,
        profile: StyleProfile,
    ): List<PaletteColor> {
        val level = clampStrength(strength)
        val selected = ArrayList<PaletteColor>(maxColors)
        val selectedCodes = linkedSetOf<String>()

        fun add(color: PaletteColor, allowSimilar: Boolean = false): Boolean {
            if (selectedCodes.contains(color.id) || selected.size >= maxColors) {
                return false
            }
            if (!allowSimilar && selected.isNotEmpty()) {
                val chroma = colorChroma(intArrayOf(color.red, color.green, color.blue))
                val baseThreshold =
                    if (luminance(intArrayOf(color.red, color.green, color.blue)) < 48 || chroma > 58) {
                        18.0
                    } else {
                        34.0
                    }
                val threshold = (baseThreshold + level * if (chroma > 58) 3 else 7) * profile.candidateDistanceFactor
                val nearestDistance = selected.minOf { colorDistance(color, it) }
                if (nearestDistance < threshold) {
                    return false
                }
            }
            selected += color
            selectedCodes += color.id
            return true
        }

        fun chooseDiverseUntil(targetCount: Int) {
            while (selected.size < min(maxColors, targetCount)) {
                val choices =
                    ranked
                        .filterNot { selectedCodes.contains(it.color.id) }
                        .map { it to candidateDiversityScore(it, selected) }
                        .sortedByDescending { it.second }
                val added = choices.any { add(it.first.color) }
                if (!added) {
                    break
                }
            }
        }

        ranked.sortedByDescending { it.count }.forEach { entry ->
            if (selected.size < ceil(maxColors * 0.28).toInt()) {
                add(entry.color)
            }
        }

        val protectedFeatureSlots = max(2, ceil(maxColors * if (profile.useAverageFallback) 0.28 else 0.22).toInt())
        ranked
            .filter { isFeatureColor(it.color, it.count, ranked) }
            .map { it to featureColorScore(it, selected) }
            .sortedByDescending { it.second }
            .forEach { (entry, _) ->
                if (selected.size < maxColors && selected.count { isFeaturePaletteColor(it) } < protectedFeatureSlots) {
                    add(entry.color, shouldAllowFeatureSimilarity(entry.color, selected))
                }
            }

        ranked
            .filter { isFeaturePaletteColor(it.color) }
            .forEach { add(it.color, shouldAllowFeatureSimilarity(it.color, selected)) }

        chooseDiverseUntil(ceil(maxColors * profile.candidateTargetRatio).toInt())
        ranked.forEach { add(it.color) }
        if (profile.useAverageFallback || selected.size < min(6, maxColors)) {
            ranked.forEach { add(it.color, true) }
        }

        return if (selected.isNotEmpty()) selected else activePalette.take(maxColors)
    }

    private fun chooseCellColors(
        sampledGrid: SampledGrid,
        candidates: List<PaletteColor>,
        profile: StyleProfile,
        shouldCancel: () -> Boolean,
    ): IntArray {
        val chosenCells = IntArray(sampledGrid.cellCount) { -1 }
        val paletteLookup = QuantizedPaletteLookup(candidates)
        val counts = IntArray(candidates.size)
        val touchedIndices = IntArray(min(sampledGrid.samplesPerCell, candidates.size))
        for (cellIndex in 0 until sampledGrid.cellCount) {
            if (cellIndex % 512 == 0) {
                checkCancelled(shouldCancel)
            }
            val sampleCount = sampledGrid.sampleCount(cellIndex)
            if (sampledGrid.backgroundShare(cellIndex) >= profile.backgroundThreshold || sampleCount == 0) {
                continue
            }

            var touchedCount = 0
            val sampleOffset = cellIndex * sampledGrid.samplesPerCell
            for (sampleIndex in 0 until sampleCount) {
                val rgb = sampledGrid.samples[sampleOffset + sampleIndex]
                val colorIndex = paletteLookup.nearestIndex(rgb)
                if (counts[colorIndex] == 0) {
                    touchedIndices[touchedCount++] = colorIndex
                }
                counts[colorIndex] += 1
            }

            var primaryIndex = -1
            var primaryCount = 0
            for (touchedIndex in 0 until touchedCount) {
                val colorIndex = touchedIndices[touchedIndex]
                val count = counts[colorIndex]
                if (count > primaryCount || (count == primaryCount && colorIndex < primaryIndex)) {
                    primaryIndex = colorIndex
                    primaryCount = count
                }
                counts[colorIndex] = 0
            }
            if (primaryIndex < 0) {
                continue
            }

            val primaryShare = primaryCount / sampleCount.toDouble()
            val average = sampledGrid.averageRgb[cellIndex]
            chosenCells[cellIndex] =
                if (primaryShare >= profile.dominanceThreshold || !profile.useAverageFallback || average < 0) {
                    primaryIndex
                } else {
                    paletteLookup.nearestIndex(average)
                }
        }
        return chosenCells
    }

    private fun reduceSpeckles(
        cells: IntArray,
        width: Int,
        height: Int,
        strength: Int,
        candidates: List<PaletteColor>,
        shouldCancel: () -> Boolean,
    ): IntArray {
        val iterations = clampStrength(strength)
        if (iterations == 0) {
            return cells
        }

        var current = cells
        val neighborColors = IntArray(8)
        val neighborCounts = IntArray(8)

        repeat(iterations) { pass ->
            checkCancelled(shouldCancel)
            val next = current.copyOf()

            for (y in 0 until height) {
                if (y % 16 == 0) {
                    checkCancelled(shouldCancel)
                }
                for (x in 0 until width) {
                    val index = y * width + x
                    val colorIndex = current[index]
                    if (colorIndex < 0) {
                        continue
                    }
                    var sameCount = 0
                    var uniqueNeighborCount = 0
                    for (deltaY in -1..1) {
                        for (deltaX in -1..1) {
                            if (deltaX == 0 && deltaY == 0) {
                                continue
                            }
                            val nextX = x + deltaX
                            val nextY = y + deltaY
                            if (nextX !in 0 until width || nextY !in 0 until height) {
                                continue
                            }
                            val neighborColor = current[nextY * width + nextX]
                            if (neighborColor == colorIndex) {
                                sameCount += 1
                            }
                            if (neighborColor < 0) {
                                continue
                            }
                            var neighborSlot = -1
                            for (slot in 0 until uniqueNeighborCount) {
                                if (neighborColors[slot] == neighborColor) {
                                    neighborSlot = slot
                                    break
                                }
                            }
                            if (neighborSlot >= 0) {
                                neighborCounts[neighborSlot] += 1
                            } else {
                                neighborColors[uniqueNeighborCount] = neighborColor
                                neighborCounts[uniqueNeighborCount] = 1
                                uniqueNeighborCount += 1
                            }
                        }
                    }
                    var majorityColorIndex = -1
                    var majorityCount = 0
                    for (slot in 0 until uniqueNeighborCount) {
                        if (neighborCounts[slot] > majorityCount) {
                            majorityColorIndex = neighborColors[slot]
                            majorityCount = neighborCounts[slot]
                        }
                    }
                    if (
                        majorityColorIndex < 0 ||
                        !areSimilarColors(candidates[colorIndex], candidates[majorityColorIndex], strength, rare = false)
                    ) {
                        continue
                    }
                    val requiredMajority =
                        if (iterations >= 4) {
                            if (pass == 0) 3 else 4
                        } else {
                            if (pass == 0) 4 else 5
                        }
                    val sameLimit =
                        if (iterations >= 4) {
                            if (pass == 0) 2 else 1
                        } else {
                            if (pass == 0) 1 else 0
                        }
                    if (sameCount <= sameLimit && majorityCount >= requiredMajority) {
                        next[index] = majorityColorIndex
                    }
                }
            }
            current = next
        }

        return current
    }

    private fun reduceTinyRegions(
        cells: IntArray,
        width: Int,
        height: Int,
        strength: Int,
        candidates: List<PaletteColor>,
        shouldCancel: () -> Boolean,
    ): IntArray {
        val level = clampStrength(strength)
        val maxRegionSize = intArrayOf(0, 1, 2, 5, 9)[level]
        if (maxRegionSize <= 0) {
            return cells
        }

        val next = cells.copyOf()
        val visited = BooleanArray(cells.size)
        val queue = IntArray(cells.size)
        val smallRegion = IntArray(maxRegionSize)
        val borderCounts = IntArray(candidates.size)
        val touchedBorderColors = IntArray(candidates.size)

        for (start in cells.indices) {
            if (start % 512 == 0) {
                checkCancelled(shouldCancel)
            }
            val colorIndex = cells[start]
            if (colorIndex < 0) {
                continue
            }
            if (visited[start]) {
                continue
            }

            var queueHead = 0
            var queueTail = 0
            var regionSize = 0
            var touchedBorderCount = 0
            queue[queueTail++] = start
            visited[start] = true

            while (queueHead < queueTail) {
                if (queueHead > 0 && queueHead % 4096 == 0) {
                    checkCancelled(shouldCancel)
                }
                val index = queue[queueHead++]
                if (regionSize < maxRegionSize) {
                    smallRegion[regionSize] = index
                }
                regionSize += 1
                val x = index % width
                val y = index / width

                for (direction in 0 until 4) {
                    val nextX = x + if (direction == 0) 1 else if (direction == 1) -1 else 0
                    val nextY = y + if (direction == 2) 1 else if (direction == 3) -1 else 0
                    if (nextX !in 0 until width || nextY !in 0 until height) {
                        continue
                    }
                    val neighborIndex = nextY * width + nextX
                    val neighborColor = cells[neighborIndex]
                    if (neighborColor == colorIndex && !visited[neighborIndex]) {
                        visited[neighborIndex] = true
                        queue[queueTail++] = neighborIndex
                        continue
                    }
                    if (neighborColor >= 0 && neighborColor != colorIndex) {
                        if (borderCounts[neighborColor] == 0) {
                            touchedBorderColors[touchedBorderCount++] = neighborColor
                        }
                        borderCounts[neighborColor] += 1
                    }
                }
            }

            var replacement = -1
            var replacementBorderCount = 0
            if (regionSize <= maxRegionSize) {
                for (touchedIndex in 0 until touchedBorderCount) {
                    val borderColor = touchedBorderColors[touchedIndex]
                    val count = borderCounts[borderColor]
                    if (
                        count > replacementBorderCount &&
                        areSimilarColors(candidates[colorIndex], candidates[borderColor], strength, rare = true)
                    ) {
                        replacement = borderColor
                        replacementBorderCount = count
                    }
                }
            }
            for (touchedIndex in 0 until touchedBorderCount) {
                borderCounts[touchedBorderColors[touchedIndex]] = 0
            }
            if (replacement >= 0) {
                for (regionIndex in 0 until regionSize) {
                    next[smallRegion[regionIndex]] = replacement
                }
            }
        }

        return next
    }

    private fun mergeSimilarColors(
        cells: IntArray,
        strength: Int,
        candidates: List<PaletteColor>,
    ): IntArray {
        val level = clampStrength(strength)
        if (level == 0) {
            return cells
        }

        val counts = IntArray(candidates.size)
        val firstSeen = IntArray(candidates.size) { Int.MAX_VALUE }
        cells.forEachIndexed { index, colorIndex ->
            if (colorIndex >= 0) {
                if (counts[colorIndex] == 0) {
                    firstSeen[colorIndex] = index
                }
                counts[colorIndex] += 1
            }
        }

        val rows =
            candidates.indices
                .filter { counts[it] > 0 }
                .sortedWith(compareByDescending<Int> { counts[it] }.thenBy { firstSeen[it] })

        val total = rows.sumOf { counts[it] }.coerceAtLeast(1)
        val replacements = IntArray(candidates.size) { it }
        var hasReplacements = false

        rows.forEachIndexed { index, colorIndex ->
            val rare = counts[colorIndex] <= max(3.0, total * (0.006 + level * 0.003)).roundToInt()
            val target =
                rows.take(index).firstOrNull { candidateIndex ->
                    areSimilarColors(candidates[colorIndex], candidates[candidateIndex], level, rare)
                }
            if (target != null) {
                replacements[colorIndex] = replacements[target]
                hasReplacements = true
            }
        }

        if (!hasReplacements) {
            return cells
        }
        return IntArray(cells.size) { index ->
            val colorIndex = cells[index]
            if (colorIndex < 0) -1 else replacements[colorIndex]
        }
    }

    private fun isFeatureColor(
        color: PaletteColor,
        count: Int,
        ranked: List<RankedPaletteColor>,
    ): Boolean {
        val maxCount = max(1, ranked.firstOrNull()?.count ?: 1)
        val shareOfLargest = count / maxCount.toDouble()
        val chroma = colorChroma(intArrayOf(color.red, color.green, color.blue))
        val lightness = luminance(intArrayOf(color.red, color.green, color.blue))
        return when {
            chroma > 70 && shareOfLargest > 0.002 -> true
            chroma > 52 && shareOfLargest > 0.006 -> true
            lightness < 36 && shareOfLargest > 0.003 -> true
            else -> false
        }
    }

    private fun isFeaturePaletteColor(color: PaletteColor): Boolean {
        val rgb = intArrayOf(color.red, color.green, color.blue)
        return colorChroma(rgb) > 48 || luminance(rgb) < 45
    }

    private fun shouldAllowFeatureSimilarity(
        color: PaletteColor,
        selected: List<PaletteColor>,
    ): Boolean {
        if (selected.isEmpty()) {
            return false
        }
        val nearestDistance = selected.minOf { colorDistance(color, it) }
        val chroma = colorChroma(intArrayOf(color.red, color.green, color.blue))
        return chroma > 68 && nearestDistance > 14
    }

    private fun featureColorScore(
        entry: RankedPaletteColor,
        selected: List<PaletteColor>,
    ): Double {
        val rgb = intArrayOf(entry.color.red, entry.color.green, entry.color.blue)
        val chroma = colorChroma(rgb)
        val lightness = luminance(rgb)
        val selectedDistance = if (selected.isNotEmpty()) selected.minOf { colorDistance(entry.color, it) } else 90.0
        val saturationBoost = 1 + min(3.2, chroma / 34.0)
        val contrastBoost = if (lightness < 42) 1.8 else 1.0
        val differenceBoost = max(0.8, min(4.5, selectedDistance / 22.0))
        return sqrt(max(1.0, entry.count.toDouble())) * saturationBoost * contrastBoost * differenceBoost
    }

    private fun candidateDiversityScore(
        entry: RankedPaletteColor,
        selected: List<PaletteColor>,
    ): Double {
        if (selected.isEmpty()) {
            return entry.score
        }
        val distance = selected.minOf { colorDistance(entry.color, it) }
        val rgb = intArrayOf(entry.color.red, entry.color.green, entry.color.blue)
        val chroma = colorChroma(rgb)
        val lightness = luminance(rgb)
        val distinctBoost = max(0.35, min(5.4, (distance / 26.0).pow(1.55)))
        val featureBoost =
            when {
                chroma > 58 || lightness < 48 -> 2.15
                chroma > 34 -> 1.36
                else -> 1.0
            }
        val countSignal = sqrt(max(1.0, entry.count.toDouble()))
        return countSignal * distinctBoost * featureBoost
    }

    private fun areSimilarColors(
        from: PaletteColor?,
        to: PaletteColor?,
        strength: Int,
        rare: Boolean,
    ): Boolean {
        if (from == null || to == null || from.id == to.id) {
            return false
        }
        val level = clampStrength(strength)
        val baseThreshold = doubleArrayOf(0.0, 26.0, 42.0, 62.0, 84.0)[level]
        if (baseThreshold <= 0.0) {
            return false
        }

        val fromChroma = colorChroma(from.red, from.green, from.blue).toDouble()
        val toChroma = colorChroma(to.red, to.green, to.blue).toDouble()
        val fromLuminance = luminance(from.red, from.green, from.blue)
        val toLuminance = luminance(to.red, to.green, to.blue)
        val distance = colorDistance(from, to)
        val saturationMismatch = kotlin.math.abs(fromChroma - toChroma)
        val luminanceMismatch = kotlin.math.abs(fromLuminance - toLuminance)
        val bothDark = fromLuminance < 84 && toLuminance < 84
        val bothLightNeutral = fromLuminance > 188 && toLuminance > 188 && fromChroma < 46 && toChroma < 46
        val bothMuted = fromLuminance > 96 && toLuminance > 96 && fromChroma < 82 && toChroma < 82

        var threshold = baseThreshold * if (rare) 1.45 else 1.0
        if (bothDark || bothLightNeutral || bothMuted) {
            threshold *= 1.15
        }
        if (fromChroma > 78 || toChroma > 78) {
            threshold *= 0.7
        }
        if (saturationMismatch > 56) {
            threshold *= 0.58
        }
        if (luminanceMismatch > 86 && !bothDark) {
            threshold *= 0.55
        }
        return distance <= threshold
    }

    private fun isBackgroundSample(
        alpha: Int,
        red: Int,
        green: Int,
        blue: Int,
        settings: GenerationSettings,
        detectedBackgroundColor: IntArray,
    ): Boolean {
        if (alpha < 24) {
            return true
        }
        if (settings.backgroundMode == BackgroundMode.Keep) {
            return false
        }
        val deltaRed = (red - detectedBackgroundColor[0]).toDouble()
        val deltaGreen = (green - detectedBackgroundColor[1]).toDouble()
        val deltaBlue = (blue - detectedBackgroundColor[2]).toDouble()
        val distance = sqrt(deltaRed * deltaRed + deltaGreen * deltaGreen + deltaBlue * deltaBlue)
        return distance <= settings.tolerance
    }

    private fun luminance(rgb: IntArray): Double {
        return luminance(rgb[0], rgb[1], rgb[2])
    }

    private fun colorChroma(rgb: IntArray): Double {
        return colorChroma(rgb[0], rgb[1], rgb[2]).toDouble()
    }

    private fun luminance(red: Int, green: Int, blue: Int): Double {
        return 0.299 * red + 0.587 * green + 0.114 * blue
    }

    private fun colorChroma(red: Int, green: Int, blue: Int): Int {
        return max(red, max(green, blue)) - min(red, min(green, blue))
    }

    private fun simplifySourceRgb(red: Int, green: Int, blue: Int, strength: Int): Int {
        val level = clampStrength(strength)
        if (level == 0) {
            return packRgb(red, green, blue)
        }
        val channelStep = intArrayOf(1, 5, 9, 14, 20)[level]
        val neutralThreshold = intArrayOf(0, 8, 14, 20, 28)[level]
        val chroma = colorChroma(red, green, blue)
        return if (chroma <= neutralThreshold) {
            val value = quantizeChannel(luminance(red, green, blue).roundToInt(), channelStep)
            packRgb(value, value, value)
        } else {
            packRgb(
                quantizeChannel(red, channelStep),
                quantizeChannel(green, channelStep),
                quantizeChannel(blue, channelStep),
            )
        }
    }

    private fun packRgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private fun redOf(rgb: Int): Int = rgb ushr 16 and 0xFF

    private fun greenOf(rgb: Int): Int = rgb ushr 8 and 0xFF

    private fun blueOf(rgb: Int): Int = rgb and 0xFF

    private fun quantizeChannel(value: Int, step: Int): Int {
        return max(0, min(255, (value / step.toDouble()).roundToInt() * step))
    }

    private fun clampStrength(value: Int): Int {
        return max(0, min(4, value))
    }

    private fun checkCancelled(shouldCancel: () -> Boolean) {
        if (shouldCancel()) {
            throw CancellationException("Pattern generation cancelled")
        }
    }

    private fun estimateBackgroundColor(
        source: Bitmap,
        cropRect: Rect,
    ): IntArray {
        val width = cropRect.width().coerceAtLeast(1)
        val height = cropRect.height().coerceAtLeast(1)
        val samples = ArrayList<IntArray>()
        val maxSamplesPerEdge = 80
        val xStep = max(1, width / maxSamplesPerEdge)
        val yStep = max(1, height / maxSamplesPerEdge)

        for (x in 0 until width step xStep) {
            pushOpaqueSample(samples, source, cropRect.left + x, cropRect.top)
            pushOpaqueSample(samples, source, cropRect.left + x, cropRect.bottom - 1)
        }
        for (y in 0 until height step yStep) {
            pushOpaqueSample(samples, source, cropRect.left, cropRect.top + y)
            pushOpaqueSample(samples, source, cropRect.right - 1, cropRect.top + y)
        }

        if (samples.isEmpty()) {
            return intArrayOf(255, 255, 255)
        }

        data class Bucket(val sum: IntArray, var count: Int)
        val buckets = linkedMapOf<String, Bucket>()

        samples.forEach { rgb ->
            val bucketKey =
                rgb.joinToString(",") { channel ->
                    ((channel / 16.0).roundToInt() * 16).toString()
                }
            val bucket = buckets.getOrPut(bucketKey) { Bucket(intArrayOf(0, 0, 0), 0) }
            bucket.sum[0] += rgb[0]
            bucket.sum[1] += rgb[1]
            bucket.sum[2] += rgb[2]
            bucket.count += 1
        }

        val best = buckets.values.maxByOrNull { it.count } ?: return intArrayOf(255, 255, 255)
        return intArrayOf(
            (best.sum[0] / best.count.toDouble()).roundToInt(),
            (best.sum[1] / best.count.toDouble()).roundToInt(),
            (best.sum[2] / best.count.toDouble()).roundToInt(),
        )
    }

    private fun pushOpaqueSample(
        samples: MutableList<IntArray>,
        source: Bitmap,
        x: Int,
        y: Int,
    ) {
        val pixel = source.getPixel(x, y)
        if (pixel ushr 24 and 0xFF < 24) {
            return
        }
        samples += intArrayOf(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
    }

    private fun pixelRgb(pixel: Int): IntArray {
        return intArrayOf(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
    }

    private fun normalizeBitmapForRecognition(
        source: Bitmap,
        maxEdge: Int,
    ): Bitmap {
        val safeMaxEdge = maxEdge.coerceAtLeast(1)
        val sourceMaxEdge = max(source.width, source.height).coerceAtLeast(1)
        if (sourceMaxEdge <= safeMaxEdge) {
            return if (source.config == Bitmap.Config.ARGB_8888) source else source.copy(Bitmap.Config.ARGB_8888, false)
        }
        val scale = safeMaxEdge / sourceMaxEdge.toFloat()
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun detectGridLines(
        pixels: IntArray,
        width: Int,
        height: Int,
        isVertical: Boolean,
    ): GridLineDetection {
        val scores = scanLineContrastScores(pixels, width, height, isVertical)
        if (scores.isEmpty()) {
            return GridLineDetection(emptyList(), 0.0, emptyList())
        }
        val centers = scoreCenters(scores)
        val spacing = dominantGridSpacing(centers)
        if (centers.isEmpty() || spacing <= 0.0) {
            return GridLineDetection(emptyList(), 0.0, centers)
        }
        val bestChain = longestRegularChain(centers, spacing)
        if (bestChain.size < 3) {
            return GridLineDetection(emptyList(), spacing, centers)
        }
        val expanded = expandRegularGrid(bestChain, centers, spacing)
        return GridLineDetection(
            lines = expanded,
            spacing = spacing,
            centers = centers,
        )
    }

    private fun scanLineContrastScores(
        pixels: IntArray,
        width: Int,
        height: Int,
        isVertical: Boolean,
    ): List<Double> {
        val lineLike =
            if (isVertical) {
                BooleanArray(width * height)
            } else {
                BooleanArray(width * height)
            }
        if (width < 3 || height < 3) {
            return emptyList()
        }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = pixels[y * width + x]
                val alpha = pixel ushr 24 and 0xFF
                if (alpha < 24) {
                    continue
                }
                val rgb = intArrayOf(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
                if (!isLightGridPixel(rgb)) {
                    continue
                }
                val current = luminance(rgb)
                val left = luminance(pixelRgb(pixels[y * width + (x - 1)]))
                val right = luminance(pixelRgb(pixels[y * width + (x + 1)]))
                val up = luminance(pixelRgb(pixels[(y - 1) * width + x]))
                val down = luminance(pixelRgb(pixels[(y + 1) * width + x]))
                val verticalContrast = ((left + right) / 2.0) - current
                val horizontalContrast = ((up + down) / 2.0) - current
                val isLine =
                    if (isVertical) {
                        verticalContrast > 7.0
                    } else {
                        horizontalContrast > 7.0
                    }
                if (isLine) {
                    lineLike[y * width + x] = true
                }
            }
        }

        val limit = if (isVertical) width else height
        val cross = if (isVertical) height else width
        return List(limit) { index ->
            var hits = 0
            for (crossIndex in 0 until cross) {
                val x = if (isVertical) index else crossIndex
                val y = if (isVertical) crossIndex else index
                if (lineLike[y * width + x]) {
                    hits += 1
                }
            }
            hits / cross.toDouble().coerceAtLeast(1.0)
        }
    }

    private fun scoreCenters(scores: List<Double>): List<Double> {
        val threshold = max(0.03, percentile(scores, 0.92) * 0.65)
        data class Group(
            val center: Double,
        )

        val groups = ArrayList<Group>()
        var start = -1
        var weighted = 0.0
        var total = 0.0
        scores.forEachIndexed { index, score ->
            if (score >= threshold) {
                if (start < 0) {
                    start = index
                }
                weighted += index * score
                total += score
            } else if (start >= 0) {
                groups += Group(center = if (total > 0.0) weighted / total else (start + index - 1) / 2.0)
                start = -1
                weighted = 0.0
                total = 0.0
            }
        }
        if (start >= 0) {
            groups += Group(center = if (total > 0.0) weighted / total else (start + scores.lastIndex) / 2.0)
        }

        val centers = ArrayList<Double>()
        groups.forEach { group ->
            if (centers.isEmpty() || group.center - centers.last() > 2.0) {
                centers += group.center
            }
        }
        return centers
    }

    private fun dominantGridSpacing(centers: List<Double>): Double {
        val diffs =
            centers
                .zipWithNext()
                .map { (left, right) -> right - left }
                .filter { it in 8.0..90.0 }
        if (diffs.isEmpty()) {
            return 0.0
        }
        val buckets = linkedMapOf<Int, Int>()
        diffs.forEach { diff ->
            val key = diff.roundToInt()
            buckets[key] = (buckets[key] ?: 0) + 1
        }
        val dominant = buckets.maxByOrNull { it.value }?.key?.toDouble() ?: return median(diffs)
        val clustered = diffs.filter { kotlin.math.abs(it - dominant) <= 4.0 }
        return median(if (clustered.isNotEmpty()) clustered else diffs)
    }

    private fun longestRegularChain(
        centers: List<Double>,
        spacing: Double,
    ): List<Double> {
        if (centers.isEmpty()) {
            return emptyList()
        }
        var best = emptyList<Double>()
        centers.forEachIndexed { index, seed ->
            val used = linkedSetOf(index)
            val chain = mutableListOf(seed)
            var current = seed
            while (true) {
                val target = current + spacing
                val nextIndex =
                    centers.indices
                        .filterNot { it in used }
                        .minByOrNull { kotlin.math.abs(centers[it] - target) }
                        ?.takeIf { kotlin.math.abs(centers[it] - target) <= spacing * 0.35 }
                        ?: break
                used += nextIndex
                current = centers[nextIndex]
                chain += current
            }
            current = seed
            while (true) {
                val target = current - spacing
                val nextIndex =
                    centers.indices
                        .filterNot { it in used }
                        .minByOrNull { kotlin.math.abs(centers[it] - target) }
                        ?.takeIf { kotlin.math.abs(centers[it] - target) <= spacing * 0.35 }
                        ?: break
                used += nextIndex
                current = centers[nextIndex]
                chain.add(0, current)
            }
            if (chain.size > best.size) {
                best = chain.toList()
            }
        }
        return best
    }

    private fun expandRegularGrid(
        bestChain: List<Double>,
        centers: List<Double>,
        spacing: Double,
    ): List<Double> {
        if (bestChain.isEmpty()) {
            return emptyList()
        }
        val filled = ArrayList<Double>()
        var expected = bestChain.first()
        val last = bestChain.last()
        while (expected <= last + spacing * 0.5) {
            val nearby = centers.filter { kotlin.math.abs(it - expected) <= spacing * 0.35 }
            filled += if (nearby.isNotEmpty()) nearby.average() else expected
            expected += spacing
        }
        return filled
    }

    private fun stripCoordinateBands(
        lines: List<Double>,
        spacing: Double,
    ): List<Double> {
        if (lines.size < 4 || spacing <= 0.0) {
            return lines
        }
        val leadingGap = lines[1] - lines[0]
        val trailingGap = lines.last() - lines[lines.lastIndex - 1]
        val normalizedLeading = leadingGap / spacing
        val normalizedTrailing = trailingGap / spacing
        var startIndex = 0
        var endIndex = lines.lastIndex
        if (normalizedLeading in 0.45..0.85) {
            startIndex += 1
        }
        if (normalizedTrailing in 0.45..0.85 && endIndex - startIndex >= 2) {
            endIndex -= 1
        }
        return lines.subList(startIndex, endIndex + 1)
    }

    private fun estimateGridConfidence(
        verticalDetection: GridLineDetection,
        horizontalDetection: GridLineDetection,
        verticalLines: List<Double>,
        horizontalLines: List<Double>,
    ): Double {
        val verticalCoverage =
            verticalLines.size / max(verticalDetection.centers.size.toDouble(), verticalLines.size.toDouble())
        val horizontalCoverage =
            horizontalLines.size / max(horizontalDetection.centers.size.toDouble(), horizontalLines.size.toDouble())
        val lineBalance =
            min(
                verticalLines.size.coerceAtLeast(1),
                horizontalLines.size.coerceAtLeast(1),
            ) / max(
                verticalLines.size.coerceAtLeast(1),
                horizontalLines.size.coerceAtLeast(1),
            ).toDouble()
        return (min(1.0, verticalCoverage * horizontalCoverage * lineBalance) * 100.0).roundToInt() / 100.0
    }

    private fun sampleChartCell(
        pixels: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        ignoreWatermark: Boolean,
    ): ChartCellSample {
        val rgb =
            averageChartRegion(
                pixels = pixels,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                left = left,
                top = top,
                width = width,
                height = height,
                skipCenter = true,
                skipLightGrid = true,
                ignoreWatermark = ignoreWatermark,
            ) ?: averageChartRegion(
                pixels = pixels,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                left = left,
                top = top,
                width = width,
                height = height,
                skipCenter = true,
                skipLightGrid = true,
                ignoreWatermark = false,
            )
        if (rgb == null) {
            return ChartCellSample(rgb = null, isTransparent = true)
        }

        val centerStats = countChartCellInk(pixels, imageWidth, imageHeight, left, top, width, height, ignoreWatermark)
        val lum = luminance(rgb)
        val chroma = colorChroma(rgb)
        val hasDarkText = centerStats.first > max(4.0, centerStats.third * 0.015).roundToInt()
        val isBlank = lum > 238.0 && chroma < 20.0 && !hasDarkText
        if (isBlank) {
            return ChartCellSample(rgb = null, isTransparent = true)
        }
        if (lum > 238.0 && chroma < 24.0 && hasDarkText) {
            return ChartCellSample(rgb = intArrayOf(255, 255, 251), isTransparent = false)
        }
        return ChartCellSample(rgb = rgb, isTransparent = false)
    }

    private fun averageChartRegion(
        pixels: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        skipCenter: Boolean,
        skipLightGrid: Boolean,
        ignoreWatermark: Boolean,
    ): IntArray? {
        val startX = floor(left).toInt().coerceIn(0, imageWidth - 1)
        val endX = ceil(left + width).toInt().coerceIn(startX + 1, imageWidth)
        val startY = floor(top).toInt().coerceIn(0, imageHeight - 1)
        val endY = ceil(top + height).toInt().coerceIn(startY + 1, imageHeight)
        var totalRed = 0
        var totalGreen = 0
        var totalBlue = 0
        var count = 0

        for (y in startY until endY) {
            for (x in startX until endX) {
                val rx = (x - left) / max(width, 1f)
                val ry = (y - top) / max(height, 1f)
                if (rx < 0.12f || rx > 0.88f || ry < 0.12f || ry > 0.88f) {
                    continue
                }
                if (skipCenter && rx > 0.32f && rx < 0.68f && ry > 0.28f && ry < 0.72f) {
                    continue
                }
                val pixel = pixels[y * imageWidth + x]
                val alpha = pixel ushr 24 and 0xFF
                if (alpha < 24) {
                    continue
                }
                val rgb = intArrayOf(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
                if (ignoreWatermark && isWatermarkPixel(rgb)) {
                    continue
                }
                if (skipLightGrid && isLightGridPixel(rgb) && (rx < 0.18f || rx > 0.82f || ry < 0.18f || ry > 0.82f)) {
                    continue
                }
                totalRed += rgb[0]
                totalGreen += rgb[1]
                totalBlue += rgb[2]
                count += 1
            }
        }
        if (count == 0) {
            return null
        }
        return intArrayOf(
            (totalRed / count.toDouble()).roundToInt(),
            (totalGreen / count.toDouble()).roundToInt(),
            (totalBlue / count.toDouble()).roundToInt(),
        )
    }

    private fun countChartCellInk(
        pixels: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        ignoreWatermark: Boolean,
    ): Triple<Int, Int, Int> {
        val startX = floor(left + width * 0.28f).toInt().coerceIn(0, imageWidth - 1)
        val endX = ceil(left + width * 0.72f).toInt().coerceIn(startX + 1, imageWidth)
        val startY = floor(top + height * 0.25f).toInt().coerceIn(0, imageHeight - 1)
        val endY = ceil(top + height * 0.75f).toInt().coerceIn(startY + 1, imageHeight)
        var darkPixels = 0
        var lightPixels = 0
        var total = 0

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = pixels[y * imageWidth + x]
                val alpha = pixel ushr 24 and 0xFF
                if (alpha < 24) {
                    continue
                }
                val rgb = intArrayOf(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
                if (ignoreWatermark && isWatermarkPixel(rgb)) {
                    continue
                }
                val luminance = luminance(rgb)
                if (luminance < 80.0) {
                    darkPixels += 1
                }
                if (luminance > 235.0) {
                    lightPixels += 1
                }
                total += 1
            }
        }
        return Triple(darkPixels, lightPixels, total)
    }

    private fun isLightGridPixel(rgb: IntArray): Boolean {
        val lum = luminance(rgb)
        val chroma = colorChroma(rgb)
        return lum > 145.0 && lum < 242.0 && chroma < 34.0
    }

    private fun isWatermarkPixel(rgb: IntArray): Boolean {
        val lum = luminance(rgb)
        val chroma = colorChroma(rgb)
        return lum > 88.0 && lum < 226.0 && chroma < 18.0
    }

    private fun percentile(
        values: List<Double>,
        p: Double,
    ): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val sorted = values.sorted()
        val index = (sorted.size * p).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) {
            return 0.0
        }
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }

    private fun colorDistance(left: PaletteColor, right: PaletteColor): Double {
        return weightedColorDistance(
            left.red,
            left.green,
            left.blue,
            right.red,
            right.green,
            right.blue,
        )
    }

    private fun weightedColorDistance(
        leftRed: Int,
        leftGreen: Int,
        leftBlue: Int,
        rightRed: Int,
        rightGreen: Int,
        rightBlue: Int,
    ): Double {
        val redMean = (leftRed + rightRed) / 2.0
        val deltaRed = (leftRed - rightRed).toDouble()
        val deltaGreen = (leftGreen - rightGreen).toDouble()
        val deltaBlue = (leftBlue - rightBlue).toDouble()
        return sqrt(
            (2 + redMean / 256.0) * deltaRed * deltaRed +
                4 * deltaGreen * deltaGreen +
                (2 + (255 - redMean) / 256.0) * deltaBlue * deltaBlue,
        )
    }

    private fun cropRect(
        imageWidth: Int,
        imageHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
    ): Rect {
        val safeZoom = zoom.coerceIn(1f, 2.5f)
        val targetRatio = targetWidth / targetHeight.toFloat()
        val imageRatio = imageWidth / imageHeight.toFloat()

        var cropWidth = imageWidth.toFloat()
        var cropHeight = imageHeight.toFloat()

        if (imageRatio > targetRatio) {
            cropWidth = imageHeight * targetRatio
        } else {
            cropHeight = imageWidth / targetRatio
        }

        cropWidth /= safeZoom
        cropHeight /= safeZoom

        val maxOffsetX = ((imageWidth - cropWidth) / 2f).coerceAtLeast(0f)
        val maxOffsetY = ((imageHeight - cropHeight) / 2f).coerceAtLeast(0f)

        val left = clamp(
            value = imageWidth / 2f - cropWidth / 2f + maxOffsetX * offsetX.coerceIn(-1f, 1f),
            minValue = 0f,
            maxValue = imageWidth - cropWidth,
        )
        val top = clamp(
            value = imageHeight / 2f - cropHeight / 2f + maxOffsetY * offsetY.coerceIn(-1f, 1f),
            minValue = 0f,
            maxValue = imageHeight - cropHeight,
        )

        return Rect(
            left.roundToInt(),
            top.roundToInt(),
            (left + cropWidth).roundToInt(),
            (top + cropHeight).roundToInt(),
        )
    }

    private fun clamp(value: Float, minValue: Float, maxValue: Float): Float {
        return min(maxValue, max(minValue, value))
    }
}
