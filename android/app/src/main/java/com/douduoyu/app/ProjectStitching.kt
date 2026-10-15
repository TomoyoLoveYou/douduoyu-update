package com.douduoyu.app

import java.util.UUID
import kotlin.math.abs

data class StitchBounds(
    val left: Long,
    val top: Long,
    val rightExclusive: Long,
    val bottomExclusive: Long,
) {
    val width: Long
        get() = rightExclusive - left

    val height: Long
        get() = bottomExclusive - top
}

data class StitchLayer(
    val project: BeadProject,
    val x: Int = 0,
    val y: Int = 0,
    val trimmed: Boolean = false,
) {
    private val occupiedBounds: EditorTools.GridBounds? by lazy(LazyThreadSafetyMode.NONE) {
        EditorTools.occupiedBounds(
            cells = project.cells,
            width = project.width,
            height = project.height,
        )
    }

    private val trimmedGrid: EditorTools.GridTransform by lazy(LazyThreadSafetyMode.NONE) {
        EditorTools.trimOuterEmptyCells(
            cells = project.cells,
            width = project.width,
            height = project.height,
        )
    }

    val cropLeft: Int
        get() = if (trimmed) occupiedBounds?.startColumn ?: 0 else 0

    val cropTop: Int
        get() = if (trimmed) occupiedBounds?.startRow ?: 0 else 0

    val width: Int
        get() = if (trimmed) trimmedGrid.width else project.width

    val height: Int
        get() = if (trimmed) trimmedGrid.height else project.height

    val cells: List<String?>
        get() = if (trimmed) trimmedGrid.cells else project.cells

    val bounds: StitchBounds
        get() =
            StitchBounds(
                left = x.toLong(),
                top = y.toLong(),
                rightExclusive = x.toLong() + width.toLong(),
                bottomExclusive = y.toLong() + height.toLong(),
            )
}

enum class StitchSide {
    Left,
    Right,
    Above,
    Below,
}

enum class StitchAlignment {
    Start,
    Center,
    End,
}

enum class StitchEdge {
    Left,
    Right,
    Top,
    Bottom,
}

data class StitchSnapCandidate(
    val layer: StitchLayer,
    val targetProjectId: String,
    val side: StitchSide,
    val alignment: StitchAlignment?,
    val distanceCells: Int,
)

data class StitchSnapResult(
    val layer: StitchLayer,
    val candidate: StitchSnapCandidate?,
) {
    val snapped: Boolean
        get() = candidate != null
}

sealed class PaletteCompatibilityKey {
    data class BuiltInBrand(val brandId: String) : PaletteCompatibilityKey()

    data class CustomGroup(val groupId: String) : PaletteCompatibilityKey()
}

data class StitchPalette(
    val colorScheme: ColorSchemePreset,
    val customColorSchemeId: String? = null,
    val colors: List<PaletteColor> = BeadPalette.palette(colorScheme, customColorSchemeId),
    val label: String = BeadPalette.schemeLabel(colorScheme, customColorSchemeId),
) {
    val compatibilityKey: PaletteCompatibilityKey
        get() {
            val customId = customColorSchemeId
            if (customId != null) {
                val groupId =
                    BeadPalette.customColorScheme(customId)
                        ?.customGroupId
                        ?.takeIf(String::isNotBlank)
                        ?: customId
                return PaletteCompatibilityKey.CustomGroup(groupId)
            }
            return PaletteCompatibilityKey.BuiltInBrand(colorScheme.brandId)
        }

    val isAvailable: Boolean
        get() = customColorSchemeId == null || BeadPalette.customColorScheme(customColorSchemeId) != null

    companion object {
        fun fromProject(project: BeadProject): StitchPalette =
            StitchPalette(
                colorScheme = project.colorScheme,
                customColorSchemeId = project.customColorSchemeId,
            )
    }
}

data class TargetPaletteOptions(
    val automatic: StitchPalette?,
    val choices: List<StitchPalette>,
    val requiresUserChoice: Boolean,
)

enum class ColorMappingKind {
    ExactId,
    ExactCode,
    NearestRgb,
    Unresolved,
}

data class SourceColorKey(
    val projectId: String,
    val colorId: String,
)

data class StitchColorMapping(
    val source: SourceColorKey,
    val sourceColor: PaletteColor?,
    val targetColor: PaletteColor?,
    val kind: ColorMappingKind,
    val cellCount: Int,
) {
    val rewritesId: Boolean
        get() = targetColor != null && targetColor.id != source.colorId

    val changesColor: Boolean
        get() =
            sourceColor != null &&
                targetColor != null &&
                (sourceColor.code != targetColor.code || sourceColor.argb != targetColor.argb)
}

data class ConversionImpact(
    val totalCells: Int,
    val rewrittenCells: Int,
    val colorChangedCells: Int,
    val exactCodeCells: Int,
    val nearestColorCells: Int,
    val unresolvedCells: Int,
    val rewrittenColors: Int,
    val colorChangedColors: Int,
    val nearestColorCount: Int,
    val unresolvedColorCount: Int,
)

data class PaletteMappingPlan(
    val target: StitchPalette,
    val mappings: Map<SourceColorKey, StitchColorMapping>,
    val impact: ConversionImpact,
) {
    val hasUnresolvedColors: Boolean
        get() = impact.unresolvedCells > 0

    val requiresConversionConfirmation: Boolean
        get() = impact.nearestColorCells > 0

    fun mappingFor(
        projectId: String,
        colorId: String,
    ): StitchColorMapping? = mappings[SourceColorKey(projectId, colorId)]
}

data class StitchCellContribution(
    val projectId: String,
    val projectTitle: String,
    val sourceColorId: String,
    val targetColorId: String?,
)

data class OverlapConflict(
    val x: Long,
    val y: Long,
    val contributions: List<StitchCellContribution>,
)

enum class OverlapPolicy {
    Reject,
    KeepEarlier,
    KeepLater,
}

enum class StitchComposeFailureReason {
    NoLayers,
    InvalidGrid,
    UnresolvedColors,
    ConversionConfirmationRequired,
    OverlapConflict,
    CanvasTooLarge,
}

sealed class StitchComposeResult {
    data class Success(
        val grid: EditorTools.GridTransform,
        val originX: Long,
        val originY: Long,
        val conflicts: List<OverlapConflict>,
        val impact: ConversionImpact,
    ) : StitchComposeResult()

    data class Failure(
        val reason: StitchComposeFailureReason,
        val message: String,
        val conflicts: List<OverlapConflict> = emptyList(),
    ) : StitchComposeResult()
}

object ProjectStitching {
    const val DEFAULT_INITIAL_GAP = 0
    const val DEFAULT_SNAP_THRESHOLD = 1
    const val DEFAULT_MAX_CELLS = 500_000L

    fun initialHorizontalLayout(
        projects: List<BeadProject>,
        trimmed: Boolean = false,
        gap: Int = DEFAULT_INITIAL_GAP,
    ): List<StitchLayer> {
        require(gap >= 0) { "gap must be non-negative" }
        var nextX = 0L
        return projects.map { project ->
            val layer =
                StitchLayer(
                    project = project,
                    x = nextX.checkedInt("Initial layout is too wide"),
                    trimmed = trimmed,
                )
            nextX = layer.bounds.rightExclusive + gap.toLong()
            layer
        }
    }

    fun setTrimmed(
        layer: StitchLayer,
        trimmed: Boolean,
    ): StitchLayer {
        if (layer.trimmed == trimmed) {
            return layer
        }
        val occupied =
            EditorTools.occupiedBounds(
                cells = layer.project.cells,
                width = layer.project.width,
                height = layer.project.height,
            ) ?: return layer.copy(trimmed = trimmed)
        val horizontalShift = if (trimmed) occupied.startColumn else -occupied.startColumn
        val verticalShift = if (trimmed) occupied.startRow else -occupied.startRow
        return layer.copy(
            x = (layer.x.toLong() + horizontalShift).checkedInt("Trimmed layer x is out of range"),
            y = (layer.y.toLong() + verticalShift).checkedInt("Trimmed layer y is out of range"),
            trimmed = trimmed,
        )
    }

    fun setAllTrimmed(
        layers: List<StitchLayer>,
        trimmed: Boolean,
    ): List<StitchLayer> = layers.map { setTrimmed(it, trimmed) }

    fun move(
        layer: StitchLayer,
        x: Int,
        y: Int,
    ): StitchLayer = layer.copy(x = x, y = y)

    fun moveBy(
        layer: StitchLayer,
        deltaX: Int,
        deltaY: Int,
    ): StitchLayer =
        layer.copy(
            x = (layer.x.toLong() + deltaX).checkedInt("Layer x is out of range"),
            y = (layer.y.toLong() + deltaY).checkedInt("Layer y is out of range"),
        )

    fun placeRelative(
        layer: StitchLayer,
        reference: StitchLayer,
        side: StitchSide,
        alignment: StitchAlignment = StitchAlignment.Start,
    ): StitchLayer {
        val referenceBounds = reference.bounds
        val layerWidth = layer.width.toLong()
        val layerHeight = layer.height.toLong()
        val targetX: Long
        val targetY: Long
        when (side) {
            StitchSide.Left -> {
                targetX = referenceBounds.left - layerWidth
                targetY = alignedStart(referenceBounds.top, referenceBounds.height, layerHeight, alignment)
            }
            StitchSide.Right -> {
                targetX = referenceBounds.rightExclusive
                targetY = alignedStart(referenceBounds.top, referenceBounds.height, layerHeight, alignment)
            }
            StitchSide.Above -> {
                targetX = alignedStart(referenceBounds.left, referenceBounds.width, layerWidth, alignment)
                targetY = referenceBounds.top - layerHeight
            }
            StitchSide.Below -> {
                targetX = alignedStart(referenceBounds.left, referenceBounds.width, layerWidth, alignment)
                targetY = referenceBounds.bottomExclusive
            }
        }
        return layer.copy(
            x = targetX.checkedInt("Relative layer x is out of range"),
            y = targetY.checkedInt("Relative layer y is out of range"),
        )
    }

    fun alignEdge(
        layer: StitchLayer,
        reference: StitchLayer,
        edge: StitchEdge,
    ): StitchLayer {
        val referenceBounds = reference.bounds
        return when (edge) {
            StitchEdge.Left -> layer.copy(x = referenceBounds.left.checkedInt("Aligned x is out of range"))
            StitchEdge.Right ->
                layer.copy(
                    x =
                        (referenceBounds.rightExclusive - layer.width.toLong())
                            .checkedInt("Aligned x is out of range"),
                )
            StitchEdge.Top -> layer.copy(y = referenceBounds.top.checkedInt("Aligned y is out of range"))
            StitchEdge.Bottom ->
                layer.copy(
                    y =
                        (referenceBounds.bottomExclusive - layer.height.toLong())
                            .checkedInt("Aligned y is out of range"),
                )
        }
    }

    fun snapCandidates(
        moving: StitchLayer,
        others: List<StitchLayer>,
        threshold: Int = DEFAULT_SNAP_THRESHOLD,
    ): List<StitchSnapCandidate> {
        require(threshold >= 0) { "threshold must be non-negative" }
        return buildList {
            others
                .asSequence()
                .filterNot { it.project.id == moving.project.id }
                .forEach { target ->
                    StitchSide.entries.forEach { side ->
                        snapCandidate(moving, target, side, threshold)?.let(::add)
                    }
                }
        }.sortedWith(
            compareBy<StitchSnapCandidate> { it.distanceCells }
                .thenBy { if (it.alignment == null) 1 else 0 }
                .thenBy { it.targetProjectId }
                .thenBy { it.side.ordinal },
        )
    }

    fun snap(
        moving: StitchLayer,
        others: List<StitchLayer>,
        threshold: Int = DEFAULT_SNAP_THRESHOLD,
    ): StitchSnapResult {
        val candidate = snapCandidates(moving, others, threshold).firstOrNull()
        return StitchSnapResult(
            layer = candidate?.layer ?: moving,
            candidate = candidate,
        )
    }

    fun compatibilityKey(project: BeadProject): PaletteCompatibilityKey =
        StitchPalette.fromProject(project).compatibilityKey

    fun targetPaletteOptions(layers: List<StitchLayer>): TargetPaletteOptions {
        if (layers.isEmpty()) {
            return TargetPaletteOptions(
                automatic = null,
                choices = emptyList(),
                requiresUserChoice = false,
            )
        }

        val grouped = linkedMapOf<PaletteCompatibilityKey, MutableList<StitchLayer>>()
        layers.forEach { layer ->
            grouped.getOrPut(compatibilityKey(layer.project), ::mutableListOf).add(layer)
        }
        return if (grouped.size == 1) {
            val automatic = automaticPaletteForCompatibleLayers(grouped.values.single())
            TargetPaletteOptions(
                automatic = automatic,
                choices = listOf(automatic),
                requiresUserChoice = false,
            )
        } else {
            val choices =
                grouped.flatMap { (key, compatibleLayers) ->
                    targetChoicesForCompatibilityKey(key, compatibleLayers)
                }
            TargetPaletteOptions(
                automatic = null,
                choices = choices,
                requiresUserChoice = true,
            )
        }
    }

    fun buildPalettePlan(
        layers: List<StitchLayer>,
        target: StitchPalette,
    ): PaletteMappingPlan {
        val usage = linkedMapOf<SourceColorKey, Int>()
        val sourcePalettes = layers.associate { it.project.id to StitchPalette.fromProject(it.project) }
        layers.forEach { layer ->
            layer.cells.forEach { colorId ->
                if (colorId != null) {
                    val key = SourceColorKey(layer.project.id, colorId)
                    usage[key] = (usage[key] ?: 0) + 1
                }
            }
        }

        val targetColors = target.colors.takeIf { target.isAvailable }.orEmpty()
        val targetById = targetColors.associateBy(PaletteColor::id)
        val targetByCode = targetColors.associateBy(PaletteColor::code)
        val mappings = linkedMapOf<SourceColorKey, StitchColorMapping>()
        usage.forEach { (sourceKey, cellCount) ->
            val sourcePalette = sourcePalettes[sourceKey.projectId]
            val sourceColor = resolveSourceColor(sourceKey.colorId, sourcePalette)
            val isCompatible = sourcePalette?.compatibilityKey == target.compatibilityKey
            val exactId = if (isCompatible) targetById[sourceKey.colorId] else null
            val exactCode =
                if (isCompatible) {
                    sourceColor?.code?.let(targetByCode::get) ?: targetByCode[sourceKey.colorId]
                } else {
                    null
                }
            val targetColor: PaletteColor?
            val kind: ColorMappingKind
            when {
                exactId != null -> {
                    targetColor = exactId
                    kind = ColorMappingKind.ExactId
                }
                exactCode != null -> {
                    targetColor = exactCode
                    kind = ColorMappingKind.ExactCode
                }
                sourceColor != null && targetColors.isNotEmpty() -> {
                    targetColor =
                        BeadPalette.nearest(
                            red = sourceColor.red,
                            green = sourceColor.green,
                            blue = sourceColor.blue,
                            candidates = targetColors,
                        )
                    kind = ColorMappingKind.NearestRgb
                }
                else -> {
                    targetColor = null
                    kind = ColorMappingKind.Unresolved
                }
            }
            mappings[sourceKey] =
                StitchColorMapping(
                    source = sourceKey,
                    sourceColor = sourceColor ?: targetColor,
                    targetColor = targetColor,
                    kind = kind,
                    cellCount = cellCount,
                )
        }

        return PaletteMappingPlan(
            target = target,
            mappings = mappings,
            impact = conversionImpact(mappings.values),
        )
    }

    fun detectOverlaps(
        layers: List<StitchLayer>,
        plan: PaletteMappingPlan,
    ): List<OverlapConflict> {
        val contributions = linkedMapOf<WorldCell, MutableList<StitchCellContribution>>()
        layers.forEach { layer ->
            forEachOccupiedCell(layer) { column, row, sourceColorId ->
                val mapping = plan.mappingFor(layer.project.id, sourceColorId)
                val point =
                    WorldCell(
                        x = layer.x.toLong() + column.toLong(),
                        y = layer.y.toLong() + row.toLong(),
                    )
                contributions.getOrPut(point, ::mutableListOf).add(
                    StitchCellContribution(
                        projectId = layer.project.id,
                        projectTitle = layer.project.title,
                        sourceColorId = sourceColorId,
                        targetColorId = mapping?.targetColor?.id,
                    ),
                )
            }
        }
        return contributions
            .asSequence()
            .filter { (_, items) ->
                items.size > 1 && items.map { it.targetColorId ?: "unresolved:${it.sourceColorId}" }.distinct().size > 1
            }
            .map { (point, items) ->
                OverlapConflict(
                    x = point.x,
                    y = point.y,
                    contributions = items.toList(),
                )
            }
            .sortedWith(compareBy<OverlapConflict> { it.y }.thenBy { it.x })
            .toList()
    }

    fun compose(
        layers: List<StitchLayer>,
        plan: PaletteMappingPlan,
        overlapPolicy: OverlapPolicy = OverlapPolicy.Reject,
        maxCells: Long = DEFAULT_MAX_CELLS,
        allowNearestColorMapping: Boolean = false,
    ): StitchComposeResult {
        require(maxCells > 0L) { "maxCells must be positive" }
        if (layers.isEmpty()) {
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.NoLayers,
                message = "At least one layer is required",
            )
        }
        layers.firstOrNull { !hasValidGrid(it.project) }?.let { invalid ->
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.InvalidGrid,
                message = "Invalid grid for project ${invalid.project.id}",
            )
        }
        val hasMissingMappings =
            layers.any { layer ->
                layer.cells.any { colorId ->
                    colorId != null && plan.mappingFor(layer.project.id, colorId)?.targetColor == null
                }
            }
        if (!plan.target.isAvailable || plan.hasUnresolvedColors || hasMissingMappings) {
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.UnresolvedColors,
                message = "Some source colors could not be mapped to the target palette",
            )
        }
        if (plan.requiresConversionConfirmation && !allowNearestColorMapping) {
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.ConversionConfirmationRequired,
                message = "Nearest-color conversion requires explicit confirmation",
            )
        }

        val conflicts = detectOverlaps(layers, plan)
        if (overlapPolicy == OverlapPolicy.Reject && conflicts.isNotEmpty()) {
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.OverlapConflict,
                message = "Different colors overlap in ${conflicts.size} cells",
                conflicts = conflicts,
            )
        }

        val left = layers.minOf { it.bounds.left }
        val top = layers.minOf { it.bounds.top }
        val right = layers.maxOf { it.bounds.rightExclusive }
        val bottom = layers.maxOf { it.bounds.bottomExclusive }
        val width = right - left
        val height = bottom - top
        if (
            width !in 1L..Int.MAX_VALUE.toLong() ||
            height !in 1L..Int.MAX_VALUE.toLong()
        ) {
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.CanvasTooLarge,
                message = "Stitched canvas ${width}x${height} exceeds the supported size",
            )
        }
        val cellCount = width * height
        if (cellCount <= 0L || cellCount > maxCells || cellCount > Int.MAX_VALUE.toLong()) {
            return StitchComposeResult.Failure(
                reason = StitchComposeFailureReason.CanvasTooLarge,
                message = "Stitched canvas ${width}x${height} exceeds the supported size",
            )
        }

        val outputWidth = width.toInt()
        val outputHeight = height.toInt()
        val output = MutableList<String?>(cellCount.toInt()) { null }
        layers.forEach { layer ->
            forEachOccupiedCell(layer) { column, row, sourceColorId ->
                val targetColorId = plan.mappingFor(layer.project.id, sourceColorId)?.targetColor?.id
                    ?: return@forEachOccupiedCell
                val outputX = (layer.x.toLong() + column - left).toInt()
                val outputY = (layer.y.toLong() + row - top).toInt()
                val outputIndex = outputY * outputWidth + outputX
                when (overlapPolicy) {
                    OverlapPolicy.KeepEarlier -> if (output[outputIndex] == null) output[outputIndex] = targetColorId
                    OverlapPolicy.KeepLater,
                    OverlapPolicy.Reject,
                    -> output[outputIndex] = targetColorId
                }
            }
        }
        return StitchComposeResult.Success(
            grid = EditorTools.GridTransform(width = outputWidth, height = outputHeight, cells = output),
            originX = left,
            originY = top,
            conflicts = conflicts,
            impact = plan.impact,
        )
    }

    fun buildProject(
        composed: StitchComposeResult.Success,
        target: StitchPalette,
        sourceRegions: List<StitchSourceRegion> = emptyList(),
        title: String = "\u62fc\u63a5\u9879\u76ee",
        beadsPerPack: Int = 500,
        id: String = UUID.randomUUID().toString(),
        updatedAt: Long = System.currentTimeMillis(),
    ): BeadProject {
        require(target.isAvailable) { "Target palette is no longer available" }
        val targetColorIds = target.colors.mapTo(hashSetOf(), PaletteColor::id)
        require(composed.grid.cells.all { colorId -> colorId == null || colorId in targetColorIds }) {
            "Stitched cells do not belong to the target palette"
        }
        return BeadProject(
            id = id,
            title = title.trim().ifEmpty { "\u62fc\u63a5\u9879\u76ee" },
            width = composed.grid.width,
            height = composed.grid.height,
            cells = composed.grid.cells,
            paletteMode = target.colorScheme.paletteMode,
            colorScheme = target.colorScheme,
            customColorSchemeId = target.customColorSchemeId,
            beadsPerPack = beadsPerPack.coerceAtLeast(1),
            sourceKind = ProjectSourceKind.Stitched,
            sourceImagePath = null,
            stitchSourceRegions = sourceRegions,
            updatedAt = updatedAt.coerceAtLeast(0L),
        )
    }

    fun sourceRegions(
        layers: List<StitchLayer>,
        composed: StitchComposeResult.Success,
    ): List<StitchSourceRegion> {
        return layers.map { layer ->
            StitchSourceRegion(
                sourceProjectId = layer.project.id,
                sourceTitle = layer.project.title,
                left = (layer.bounds.left - composed.originX).checkedInt("Source region left is out of range"),
                top = (layer.bounds.top - composed.originY).checkedInt("Source region top is out of range"),
                width = layer.width,
                height = layer.height,
            )
        }
    }

    private fun automaticPaletteForCompatibleLayers(layers: List<StitchLayer>): StitchPalette {
        val first = layers.first()
        return when (compatibilityKey(first.project)) {
            is PaletteCompatibilityKey.BuiltInBrand -> automaticBuiltInPalette(layers)
            is PaletteCompatibilityKey.CustomGroup -> automaticCustomPalette(layers)
        }
    }

    private fun targetChoicesForCompatibilityKey(
        key: PaletteCompatibilityKey,
        layers: List<StitchLayer>,
    ): List<StitchPalette> =
        when (key) {
            is PaletteCompatibilityKey.BuiltInBrand -> {
                val currentHiddenSchemes =
                    layers
                        .map { it.project.colorScheme }
                        .filterNot(ColorSchemePreset::showInLogicPanel)
                (ColorSchemePreset.entries.filter { it.showInLogicPanel && it.brandId == key.brandId } + currentHiddenSchemes)
                    .distinct()
                    .sortedWith(
                        compareByDescending<ColorSchemePreset>(ColorSchemePreset::maxColors)
                            .thenBy(ColorSchemePreset::ordinal),
                    )
                    .map { StitchPalette(colorScheme = it) }
            }
            is PaletteCompatibilityKey.CustomGroup ->
                layers
                    .map { StitchPalette.fromProject(it.project) }
                    .filter(StitchPalette::isAvailable)
                    .distinctBy(StitchPalette::customColorSchemeId)
                    .sortedWith(
                        compareByDescending<StitchPalette> { it.colors.size }
                            .thenByDescending {
                                BeadPalette.customColorScheme(it.customColorSchemeId)?.updatedAt ?: Long.MIN_VALUE
                            }
                            .thenBy { it.customColorSchemeId.orEmpty() },
                    )
        }

    private fun automaticBuiltInPalette(layers: List<StitchLayer>): StitchPalette {
        val sourceSchemes = layers.map { it.project.colorScheme }
        val brandId = sourceSchemes.first().brandId
        val minimumColors = sourceSchemes.maxOf(ColorSchemePreset::maxColors)
        val usedCodes =
            buildSet {
                layers.forEach { layer ->
                    val sourcePalette = StitchPalette.fromProject(layer.project)
                    layer.cells.filterNotNull().forEach { colorId ->
                        add(resolveSourceColor(colorId, sourcePalette)?.code ?: colorId)
                    }
                }
            }
        val covering =
            ColorSchemePreset.entries
                .asSequence()
                .filter { it.brandId == brandId && it.maxColors >= minimumColors }
                .map { scheme -> StitchPalette(colorScheme = scheme) }
                .filter { palette -> palette.colors.mapTo(hashSetOf(), PaletteColor::code).containsAll(usedCodes) }
                .sortedWith(compareBy<StitchPalette> { it.colorScheme.maxColors }.thenBy { it.colorScheme.ordinal })
                .firstOrNull()
        return covering
            ?: StitchPalette(
                colorScheme = sourceSchemes.maxByOrNull(ColorSchemePreset::maxColors) ?: firstDefaultScheme(),
            )
    }

    private fun automaticCustomPalette(layers: List<StitchLayer>): StitchPalette {
        val palettes =
            layers
                .map { StitchPalette.fromProject(it.project) }
                .distinctBy(StitchPalette::customColorSchemeId)
        val availablePalettes = palettes.filter(StitchPalette::isAvailable)
        val minimumColors = availablePalettes.maxOfOrNull { it.colors.size } ?: 0
        val usedCodes =
            buildSet {
                layers.forEach { layer ->
                    val sourcePalette = StitchPalette.fromProject(layer.project)
                    layer.cells.filterNotNull().forEach { colorId ->
                        add(resolveSourceColor(colorId, sourcePalette)?.code ?: colorId)
                    }
                }
            }
        val covering =
            availablePalettes
                .asSequence()
                .filter { it.colors.size >= minimumColors }
                .filter { palette -> palette.colors.mapTo(hashSetOf(), PaletteColor::code).containsAll(usedCodes) }
                .sortedWith(
                    compareBy<StitchPalette> { it.colors.size }
                        .thenByDescending {
                            BeadPalette.customColorScheme(it.customColorSchemeId)?.updatedAt ?: Long.MIN_VALUE
                        }
                        .thenBy { it.customColorSchemeId.orEmpty() },
                )
                .firstOrNull()
        return covering ?: availablePalettes.maxWithOrNull(
            compareBy<StitchPalette> { it.colors.size }.thenBy {
                BeadPalette.customColorScheme(it.customColorSchemeId)?.updatedAt ?: Long.MIN_VALUE
            },
        ) ?: palettes.first()
    }

    private fun firstDefaultScheme(): ColorSchemePreset = ColorSchemePreset.MardBrand221

    private fun resolveSourceColor(
        colorId: String,
        sourcePalette: StitchPalette?,
    ): PaletteColor? =
        sourcePalette?.colors?.firstOrNull { it.id == colorId }
            ?: sourcePalette?.colors?.firstOrNull { it.code == colorId }
            ?: BeadPalette.find(colorId)

    private fun conversionImpact(mappings: Collection<StitchColorMapping>): ConversionImpact =
        ConversionImpact(
            totalCells = mappings.sumOf(StitchColorMapping::cellCount),
            rewrittenCells = mappings.filter(StitchColorMapping::rewritesId).sumOf(StitchColorMapping::cellCount),
            colorChangedCells = mappings.filter(StitchColorMapping::changesColor).sumOf(StitchColorMapping::cellCount),
            exactCodeCells = mappings.filter { it.kind == ColorMappingKind.ExactCode }.sumOf(StitchColorMapping::cellCount),
            nearestColorCells = mappings.filter { it.kind == ColorMappingKind.NearestRgb }.sumOf(StitchColorMapping::cellCount),
            unresolvedCells = mappings.filter { it.kind == ColorMappingKind.Unresolved }.sumOf(StitchColorMapping::cellCount),
            rewrittenColors = mappings.count(StitchColorMapping::rewritesId),
            colorChangedColors = mappings.count(StitchColorMapping::changesColor),
            nearestColorCount = mappings.count { it.kind == ColorMappingKind.NearestRgb },
            unresolvedColorCount = mappings.count { it.kind == ColorMappingKind.Unresolved },
        )

    private fun snapCandidate(
        moving: StitchLayer,
        target: StitchLayer,
        side: StitchSide,
        threshold: Int,
    ): StitchSnapCandidate? {
        val targetBounds = target.bounds
        val exactPrimary =
            when (side) {
                StitchSide.Left -> targetBounds.left - moving.width.toLong()
                StitchSide.Right -> targetBounds.rightExclusive
                StitchSide.Above -> targetBounds.top - moving.height.toLong()
                StitchSide.Below -> targetBounds.bottomExclusive
            }
        if (exactPrimary !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            return null
        }
        val primaryDistance =
            when (side) {
                StitchSide.Left,
                StitchSide.Right,
                -> abs(moving.x.toLong() - exactPrimary)
                StitchSide.Above,
                StitchSide.Below,
                -> abs(moving.y.toLong() - exactPrimary)
            }
        if (primaryDistance > threshold.toLong()) {
            return null
        }

        val alignments =
            StitchAlignment.entries.mapNotNull { alignment ->
                runCatching { placeRelative(moving, target, side, alignment) }
                    .getOrNull()
                    ?.let { placed ->
                        val distance =
                            when (side) {
                                StitchSide.Left,
                                StitchSide.Right,
                                -> abs(moving.y.toLong() - placed.y.toLong())
                                StitchSide.Above,
                                StitchSide.Below,
                                -> abs(moving.x.toLong() - placed.x.toLong())
                            }
                        alignment to (placed to distance)
                    }
            }
        val secondary = alignments.minByOrNull { it.second.second }
        val shouldAlign = secondary != null && secondary.second.second <= threshold.toLong()
        val orthogonalGap =
            when (side) {
                StitchSide.Left,
                StitchSide.Right,
                -> intervalGap(
                    moving.y.toLong(),
                    moving.y.toLong() + moving.height,
                    targetBounds.top,
                    targetBounds.bottomExclusive,
                )
                StitchSide.Above,
                StitchSide.Below,
                -> intervalGap(
                    moving.x.toLong(),
                    moving.x.toLong() + moving.width,
                    targetBounds.left,
                    targetBounds.rightExclusive,
                )
                }
        if (!shouldAlign && orthogonalGap > threshold.toLong()) {
            return null
        }
        val snappedLayer =
            if (shouldAlign) {
                secondary!!.second.first
            } else {
                when (side) {
                    StitchSide.Left,
                    StitchSide.Right,
                    -> moving.copy(x = exactPrimary.toInt())
                    StitchSide.Above,
                    StitchSide.Below,
                    -> moving.copy(y = exactPrimary.toInt())
                }
            }
        val secondaryDistance = if (shouldAlign) secondary!!.second.second else 0L
        return StitchSnapCandidate(
            layer = snappedLayer,
            targetProjectId = target.project.id,
            side = side,
            alignment = if (shouldAlign) secondary!!.first else null,
            distanceCells = (primaryDistance + secondaryDistance).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )
    }

    private fun alignedStart(
        referenceStart: Long,
        referenceSize: Long,
        movingSize: Long,
        alignment: StitchAlignment,
    ): Long =
        when (alignment) {
            StitchAlignment.Start -> referenceStart
            StitchAlignment.Center -> referenceStart + (referenceSize - movingSize) / 2L
            StitchAlignment.End -> referenceStart + referenceSize - movingSize
        }

    private fun intervalGap(
        firstStart: Long,
        firstEnd: Long,
        secondStart: Long,
        secondEnd: Long,
    ): Long =
        when {
            firstEnd < secondStart -> secondStart - firstEnd
            secondEnd < firstStart -> firstStart - secondEnd
            else -> 0L
        }

    private fun hasValidGrid(project: BeadProject): Boolean {
        if (project.width <= 0 || project.height <= 0) {
            return false
        }
        return project.width.toLong() * project.height.toLong() == project.cells.size.toLong()
    }

    private inline fun forEachOccupiedCell(
        layer: StitchLayer,
        action: (column: Int, row: Int, colorId: String) -> Unit,
    ) {
        val width = layer.width
        if (width <= 0) {
            return
        }
        layer.cells.forEachIndexed { index, colorId ->
            if (colorId != null) {
                action(index % width, index / width, colorId)
            }
        }
    }

    private data class WorldCell(
        val x: Long,
        val y: Long,
    )
}

private fun Long.checkedInt(message: String): Int {
    require(this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { message }
    return toInt()
}
