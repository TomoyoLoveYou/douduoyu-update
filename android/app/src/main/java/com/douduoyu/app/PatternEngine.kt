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

private data class StyleProfile(
    val sampleSide: Int,
    val backgroundThreshold: Double,
    val dominanceThreshold: Double,
    val candidateTargetRatio: Double,
    val candidateDistanceFactor: Double,
    val postStrengthBias: Int,
    val useAverageFallback: Boolean,
)

private data class SampledCell(
    val samples: List<IntArray>,
    val average: IntArray?,
    val backgroundShare: Double,
)

private data class RankedPaletteColor(
    val color: PaletteColor,
    val count: Int,
    val score: Double,
)

private data class MajorityColor(
    val colorCode: String?,
    val count: Int,
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

    fun generateProject(source: Bitmap, settings: GenerationSettings): BeadProject {
        val outputWidth = settings.width.coerceAtLeast(1)
        val outputHeight = settings.height.coerceAtLeast(1)
        val cropRect = cropRect(
            imageWidth = source.width,
            imageHeight = source.height,
            targetWidth = outputWidth,
            targetHeight = outputHeight,
            zoom = settings.cropZoom,
            offsetX = settings.cropX,
            offsetY = settings.cropY,
        )

        val samplingWidth = cropRect.width().coerceAtLeast(1)
        val samplingHeight = cropRect.height().coerceAtLeast(1)
        val sampledBitmap = Bitmap.createBitmap(samplingWidth, samplingHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sampledBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        canvas.drawBitmap(source, cropRect, Rect(0, 0, samplingWidth, samplingHeight), paint)

        val pixels = IntArray(samplingWidth * samplingHeight)
        sampledBitmap.getPixels(pixels, 0, samplingWidth, 0, 0, samplingWidth, samplingHeight)

        val activePalette = BeadPalette.palette(settings.colorScheme)
        val paletteLimit = settings.maxColors.coerceAtMost(activePalette.size).coerceAtLeast(2)
        val profile = styleProfile(settings.generationStyle)
        val requestedSpeckle = settings.speckleReduction.coerceIn(0, 4)
        val speckleStrength =
            if (requestedSpeckle > 0) {
                clampStrength(requestedSpeckle + profile.postStrengthBias)
            } else {
                0
            }

        val detectedBackgroundColor = estimateBackgroundColor(pixels, samplingWidth, samplingHeight)
        val sampledCells = sampleGridCells(
            pixels = pixels,
            sourceWidth = samplingWidth,
            sourceHeight = samplingHeight,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            settings = settings,
            detectedBackgroundColor = detectedBackgroundColor,
            profile = profile,
            speckleStrength = speckleStrength,
        )
        val ranked = rankPaletteColors(sampledCells, paletteLimit, activePalette, profile)
        val candidates = selectCandidateColors(
            ranked = ranked,
            maxColors = paletteLimit,
            activePalette = activePalette,
            strength = speckleStrength,
            profile = profile,
        )
        val chosenCells = sampledCells.map { chooseCellColor(it, candidates, profile) }
        val merged = mergeSimilarColors(chosenCells, speckleStrength, candidates)
        val compact = reduceTinyRegions(merged, outputWidth, outputHeight, speckleStrength, candidates)
        val reduced = reduceSpeckles(compact, outputWidth, outputHeight, speckleStrength, candidates)

        return BeadProject(
            title = settings.title.ifBlank { "\u8c46\u8c46\u9c7c\u56fe\u7eb8" },
            width = outputWidth,
            height = outputHeight,
            cells = reduced,
            paletteMode = settings.colorScheme.paletteMode,
            colorScheme = settings.colorScheme,
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
        val activePalette = BeadPalette.palette(settings.colorScheme)
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
    ): List<SampledCell> {
        val cells = ArrayList<SampledCell>(outputWidth * outputHeight)
        val totalSamples = profile.sampleSide * profile.sampleSide

        for (y in 0 until outputHeight) {
            for (x in 0 until outputWidth) {
                val samples = ArrayList<IntArray>(totalSamples)
                var backgroundCount = 0
                var redTotal = 0
                var greenTotal = 0
                var blueTotal = 0

                for (sampleY in 0 until profile.sampleSide) {
                    for (sampleX in 0 until profile.sampleSide) {
                        val sourceX =
                            min(
                                sourceWidth - 1,
                                max(
                                    0,
                                    kotlin.math.floor(
                                        ((x + (sampleX + 0.5) / profile.sampleSide) / outputWidth) * sourceWidth,
                                    ).toInt(),
                                ),
                            )
                        val sourcePixelY =
                            min(
                                sourceHeight - 1,
                                max(
                                    0,
                                    kotlin.math.floor(
                                        ((y + (sampleY + 0.5) / profile.sampleSide) / outputHeight) * sourceHeight,
                                    ).toInt(),
                                ),
                            )
                        val pixel = pixels[sourcePixelY * sourceWidth + sourceX]
                        val alpha = pixel ushr 24 and 0xFF
                        val rgb = intArrayOf(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
                        if (isBackgroundSample(alpha, rgb, settings, detectedBackgroundColor)) {
                            backgroundCount += 1
                            continue
                        }
                        val simplified = simplifySourceRgb(rgb, speckleStrength)
                        samples += simplified
                        redTotal += simplified[0]
                        greenTotal += simplified[1]
                        blueTotal += simplified[2]
                    }
                }

                cells +=
                    SampledCell(
                        samples = samples,
                        average =
                            if (samples.isNotEmpty()) {
                                intArrayOf(
                                    (redTotal / samples.size.toDouble()).roundToInt(),
                                    (greenTotal / samples.size.toDouble()).roundToInt(),
                                    (blueTotal / samples.size.toDouble()).roundToInt(),
                                )
                            } else {
                                null
                            },
                        backgroundShare = backgroundCount / totalSamples.toDouble(),
                    )
            }
        }

        return cells
    }

    private fun rankPaletteColors(
        sampledCells: List<SampledCell>,
        maxColors: Int,
        activePalette: List<PaletteColor>,
        profile: StyleProfile,
    ): List<RankedPaletteColor> {
        val counts = linkedMapOf<String, Pair<PaletteColor, Int>>()
        var total = 0

        sampledCells.forEach { cell ->
            if (cell.backgroundShare >= profile.backgroundThreshold || cell.samples.isEmpty()) {
                return@forEach
            }
            cell.samples.forEach { rgb ->
                total += 1
                val color = BeadPalette.nearest(rgb[0], rgb[1], rgb[2], activePalette)
                val current = counts[color.id]
                counts[color.id] = color to ((current?.second ?: 0) + 1)
            }
        }

        val rows =
            counts.values
                .map { (color, count) ->
                    val chroma = colorChroma(intArrayOf(color.red, color.green, color.blue))
                    val share = if (total > 0) count / total.toDouble() else 0.0
                    val lightness = luminance(intArrayOf(color.red, color.green, color.blue))
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

    private fun chooseCellColor(
        cell: SampledCell,
        candidates: List<PaletteColor>,
        profile: StyleProfile,
    ): String? {
        if (cell.backgroundShare >= profile.backgroundThreshold || cell.samples.isEmpty()) {
            return null
        }

        val counts = linkedMapOf<String, Pair<PaletteColor, Int>>()
        cell.samples.forEach { rgb ->
            val color = BeadPalette.nearest(rgb[0], rgb[1], rgb[2], candidates)
            val current = counts[color.id]
            counts[color.id] = color to ((current?.second ?: 0) + 1)
        }

        val ranked =
            counts.values
                .map { RankedPaletteColor(color = it.first, count = it.second, score = it.second.toDouble()) }
                .sortedByDescending { it.count }
        val primary = ranked.firstOrNull() ?: return null
        val primaryShare = primary.count / cell.samples.size.toDouble()
        if (primaryShare >= profile.dominanceThreshold || !profile.useAverageFallback || cell.average == null) {
            return primary.color.id
        }

        return BeadPalette.nearest(cell.average[0], cell.average[1], cell.average[2], candidates).id
    }

    private fun reduceSpeckles(
        cells: List<String?>,
        width: Int,
        height: Int,
        strength: Int,
        candidates: List<PaletteColor>,
    ): List<String?> {
        val iterations = clampStrength(strength)
        if (iterations == 0) {
            return cells
        }

        val colorMap = candidates.associateBy { it.id }
        var next = cells.toMutableList()

        repeat(iterations) { pass ->
            val current = next.toList()
            next = current.toMutableList()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    val colorCode = current[index] ?: continue
                    val neighbors = neighborColorIds(current, width, height, x, y)
                    val sameCount = neighbors.count { it == colorCode }
                    val majority = majorityColor(neighbors)
                    val majorityCode = majority.colorCode ?: continue
                    if (!areSimilarColors(colorMap[colorCode], colorMap[majorityCode], strength, rare = false)) {
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
                    if (sameCount <= sameLimit && majority.count >= requiredMajority) {
                        next[index] = majorityCode
                    }
                }
            }
        }

        return next
    }

    private fun reduceTinyRegions(
        cells: List<String?>,
        width: Int,
        height: Int,
        strength: Int,
        candidates: List<PaletteColor>,
    ): List<String?> {
        val level = clampStrength(strength)
        val maxRegionSize = intArrayOf(0, 1, 2, 5, 9)[level]
        if (maxRegionSize <= 0) {
            return cells
        }

        val colorMap = candidates.associateBy { it.id }
        val next = cells.toMutableList()
        val visited = BooleanArray(cells.size)
        val neighborOffsets = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

        for (start in cells.indices) {
            val colorCode = cells[start] ?: continue
            if (visited[start]) {
                continue
            }

            val region = ArrayList<Int>()
            val borderCounts = linkedMapOf<String, Int>()
            val queue = ArrayDeque<Int>()
            queue.addLast(start)
            visited[start] = true

            while (queue.isNotEmpty()) {
                val index = queue.removeFirst()
                region += index
                val x = index % width
                val y = index / width

                neighborOffsets.forEach { (dx, dy) ->
                    val nextX = x + dx
                    val nextY = y + dy
                    if (nextX !in 0 until width || nextY !in 0 until height) {
                        return@forEach
                    }
                    val neighborIndex = nextY * width + nextX
                    val neighborColor = cells[neighborIndex]
                    if (neighborColor == colorCode && !visited[neighborIndex]) {
                        visited[neighborIndex] = true
                        queue.addLast(neighborIndex)
                        return@forEach
                    }
                    if (neighborColor != null && neighborColor != colorCode) {
                        borderCounts[neighborColor] = (borderCounts[neighborColor] ?: 0) + 1
                    }
                }
            }

            if (region.size > maxRegionSize || borderCounts.isEmpty()) {
                continue
            }

            val replacement =
                borderCounts.entries
                    .sortedByDescending { it.value }
                    .firstOrNull {
                        areSimilarColors(colorMap[colorCode], colorMap[it.key], strength, rare = true)
                    }?.key
                    ?: continue

            region.forEach { next[it] = replacement }
        }

        return next
    }

    private fun mergeSimilarColors(
        cells: List<String?>,
        strength: Int,
        candidates: List<PaletteColor>,
    ): List<String?> {
        val level = clampStrength(strength)
        if (level == 0) {
            return cells
        }

        val colorMap = candidates.associateBy { it.id }
        val counts = linkedMapOf<String, Int>()
        cells.forEach { colorCode ->
            if (colorCode != null) {
                counts[colorCode] = (counts[colorCode] ?: 0) + 1
            }
        }

        val rows =
            counts.entries
                .mapNotNull { (code, count) ->
                    colorMap[code]?.let { RankedPaletteColor(color = it, count = count, score = count.toDouble()) }
                }
                .sortedByDescending { it.count }

        val total = rows.sumOf { it.count }.coerceAtLeast(1)
        val replacements = linkedMapOf<String, String>()

        rows.forEachIndexed { index, row ->
            val rare = row.count <= max(3.0, total * (0.006 + level * 0.003)).roundToInt()
            val target =
                rows.take(index).firstOrNull { candidate ->
                    areSimilarColors(row.color, candidate.color, level, rare)
                }
            if (target != null) {
                replacements[row.color.id] = replacements[target.color.id] ?: target.color.id
            }
        }

        if (replacements.isEmpty()) {
            return cells
        }
        return cells.map { colorCode -> if (colorCode == null) null else replacements[colorCode] ?: colorCode }
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

        val fromRgb = intArrayOf(from.red, from.green, from.blue)
        val toRgb = intArrayOf(to.red, to.green, to.blue)
        val fromChroma = colorChroma(fromRgb)
        val toChroma = colorChroma(toRgb)
        val fromLuminance = luminance(fromRgb)
        val toLuminance = luminance(toRgb)
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

    private fun neighborColorIds(
        cells: List<String?>,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ): List<String?> {
        val result = ArrayList<String?>(8)
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
                result += cells[nextY * width + nextX]
            }
        }
        return result
    }

    private fun majorityColor(colors: List<String?>): MajorityColor {
        val counts = linkedMapOf<String, Int>()
        colors.forEach { colorCode ->
            if (colorCode != null) {
                counts[colorCode] = (counts[colorCode] ?: 0) + 1
            }
        }
        val best = counts.entries.sortedByDescending { it.value }.firstOrNull()
        return MajorityColor(best?.key, best?.value ?: 0)
    }

    private fun isBackgroundSample(
        alpha: Int,
        rgb: IntArray,
        settings: GenerationSettings,
        detectedBackgroundColor: IntArray,
    ): Boolean {
        if (alpha < 24) {
            return true
        }
        if (settings.backgroundMode == BackgroundMode.Keep) {
            return false
        }
        val deltaRed = (rgb[0] - detectedBackgroundColor[0]).toDouble()
        val deltaGreen = (rgb[1] - detectedBackgroundColor[1]).toDouble()
        val deltaBlue = (rgb[2] - detectedBackgroundColor[2]).toDouble()
        val distance = sqrt(deltaRed * deltaRed + deltaGreen * deltaGreen + deltaBlue * deltaBlue)
        return distance <= settings.tolerance
    }

    private fun luminance(rgb: IntArray): Double {
        return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]
    }

    private fun colorChroma(rgb: IntArray): Double {
        return (max(rgb[0], max(rgb[1], rgb[2])) - min(rgb[0], min(rgb[1], rgb[2]))).toDouble()
    }

    private fun simplifySourceRgb(rgb: IntArray, strength: Int): IntArray {
        val level = clampStrength(strength)
        if (level == 0) {
            return rgb
        }
        val channelStep = intArrayOf(1, 5, 9, 14, 20)[level]
        val neutralThreshold = intArrayOf(0, 8, 14, 20, 28)[level]
        val chroma = colorChroma(rgb)
        return if (chroma <= neutralThreshold) {
            val value = quantizeChannel(luminance(rgb).roundToInt(), channelStep)
            intArrayOf(value, value, value)
        } else {
            intArrayOf(
                quantizeChannel(rgb[0], channelStep),
                quantizeChannel(rgb[1], channelStep),
                quantizeChannel(rgb[2], channelStep),
            )
        }
    }

    private fun quantizeChannel(value: Int, step: Int): Int {
        return max(0, min(255, (value / step.toDouble()).roundToInt() * step))
    }

    private fun clampStrength(value: Int): Int {
        return max(0, min(4, value))
    }

    private fun estimateBackgroundColor(
        pixels: IntArray,
        width: Int,
        height: Int,
    ): IntArray {
        val samples = ArrayList<IntArray>()
        val maxSamplesPerEdge = 80
        val xStep = max(1, width / maxSamplesPerEdge)
        val yStep = max(1, height / maxSamplesPerEdge)

        for (x in 0 until width step xStep) {
            pushOpaqueSample(samples, pixels, width, x, 0)
            pushOpaqueSample(samples, pixels, width, x, height - 1)
        }
        for (y in 0 until height step yStep) {
            pushOpaqueSample(samples, pixels, width, 0, y)
            pushOpaqueSample(samples, pixels, width, width - 1, y)
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
        pixels: IntArray,
        width: Int,
        x: Int,
        y: Int,
    ) {
        val pixel = pixels[y * width + x]
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
            return ChartCellSample(rgb = intArrayOf(255, 255, 251), isTransparent = false, forcedColorCode = "H2")
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
