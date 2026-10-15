package com.douduoyu.app

import java.util.UUID
import kotlin.math.abs
import kotlin.math.ln

data class GridPoint(
    val x: Int,
    val y: Int,
)

data class GridLineSegment(
    val start: GridPoint,
    val end: GridPoint,
) {
    val isHorizontal: Boolean
        get() = start.y == end.y && start.x != end.x

    val isVertical: Boolean
        get() = start.x == end.x && start.y != end.y
}

enum class EqualSplitPreset {
    VerticalHalves,
    HorizontalHalves,
    FourParts,
    SixParts,
    EightParts,
    NineParts,
    SixteenParts,
}

data class BoardSplitLayout(
    val boardWidth: Int,
    val boardHeight: Int,
    val imageOffsetX: Int,
    val imageOffsetY: Int,
    val verticalDividers: List<Int>,
    val horizontalDividers: List<Int>,
    val segments: List<GridLineSegment>,
) {
    val columnCount: Int
        get() = verticalDividers.size + 1

    val rowCount: Int
        get() = horizontalDividers.size + 1

    val partCount: Int
        get() = columnCount * rowCount
}

enum class ProjectSplitFailureReason {
    InvalidGrid,
    NoEffectiveLines,
    NotSeparated,
    TooManyParts,
    OutputTooLarge,
}

sealed class ProjectSplitResult {
    data class Success(
        val projects: List<BeadProject>,
        val geometricRegionCount: Int,
    ) : ProjectSplitResult() {
        val partCount: Int
            get() = projects.size
    }

    data class Failure(
        val reason: ProjectSplitFailureReason,
        val message: String,
    ) : ProjectSplitResult()
}

object ProjectSplitting {
    const val DEFAULT_MAX_PARTS = 64
    const val DEFAULT_MAX_OUTPUT_CELLS = ProjectStitching.DEFAULT_MAX_CELLS

    fun orthogonalizeStroke(
        rawPoints: List<GridPoint>,
        width: Int,
        height: Int,
    ): List<GridPoint> {
        if (width <= 0 || height <= 0) {
            return emptyList()
        }
        val points =
            rawPoints
                .map { point -> GridPoint(point.x.coerceIn(0, width), point.y.coerceIn(0, height)) }
                .fold(mutableListOf<GridPoint>()) { output, point ->
                    if (output.lastOrNull() != point) output += point
                    output
                }
        if (points.size < 2) {
            return points
        }

        val vertices = mutableListOf(points.first())
        var legStart = points.first()
        var endpoint = legStart
        var previousRaw = legStart
        var horizontal: Boolean? = null

        points.drop(1).forEach { point ->
            if (horizontal == null) {
                val deltaX = abs(point.x - legStart.x)
                val deltaY = abs(point.y - legStart.y)
                if (deltaX == 0 && deltaY == 0) {
                    return@forEach
                }
                horizontal = deltaX >= deltaY
            }

            if (horizontal == true) {
                val alongMotion = abs(point.x - previousRaw.x)
                val perpendicularMotion = abs(point.y - previousRaw.y)
                if (point.y != legStart.y && perpendicularMotion > alongMotion) {
                    appendVertex(vertices, endpoint)
                    legStart = endpoint
                    horizontal = false
                    endpoint = GridPoint(legStart.x, point.y)
                } else {
                    endpoint = GridPoint(point.x, legStart.y)
                }
            } else {
                val alongMotion = abs(point.y - previousRaw.y)
                val perpendicularMotion = abs(point.x - previousRaw.x)
                if (point.x != legStart.x && perpendicularMotion > alongMotion) {
                    appendVertex(vertices, endpoint)
                    legStart = endpoint
                    horizontal = true
                    endpoint = GridPoint(point.x, legStart.y)
                } else {
                    endpoint = GridPoint(legStart.x, point.y)
                }
            }
            previousRaw = point
        }
        appendVertex(vertices, endpoint)
        return simplifyVertices(vertices)
    }

    fun segmentsForVertices(vertices: List<GridPoint>): List<GridLineSegment> {
        return vertices
            .zipWithNext()
            .mapNotNull { (start, end) ->
                GridLineSegment(start, end).takeIf { it.isHorizontal || it.isVertical }
            }
    }

    fun presetSegments(
        width: Int,
        height: Int,
        preset: EqualSplitPreset,
    ): List<GridLineSegment> {
        if (width <= 0 || height <= 0) {
            return emptyList()
        }
        return when (preset) {
            EqualSplitPreset.VerticalHalves ->
                if (width >= 2) {
                    val x = width / 2
                    listOf(GridLineSegment(GridPoint(x, 0), GridPoint(x, height)))
                } else {
                    emptyList()
                }

            EqualSplitPreset.HorizontalHalves ->
                if (height >= 2) {
                    val y = height / 2
                    listOf(GridLineSegment(GridPoint(0, y), GridPoint(width, y)))
                } else {
                    emptyList()
                }

            EqualSplitPreset.FourParts -> equalPartSegments(width = width, height = height, partCount = 4)
            EqualSplitPreset.SixParts -> equalPartSegments(width = width, height = height, partCount = 6)
            EqualSplitPreset.EightParts -> equalPartSegments(width = width, height = height, partCount = 8)
            EqualSplitPreset.NineParts -> equalPartSegments(width = width, height = height, partCount = 9)
            EqualSplitPreset.SixteenParts -> equalPartSegments(width = width, height = height, partCount = 16)
        }
    }

    fun equalPartSegments(
        width: Int,
        height: Int,
        partCount: Int,
    ): List<GridLineSegment> {
        if (width <= 0 || height <= 0 || partCount < 2) {
            return emptyList()
        }
        val layout = bestFactorLayout(width = width, height = height, partCount = partCount) ?: return emptyList()
        return equalGridSegments(width = width, height = height, columns = layout.first, rows = layout.second)
    }

    fun boardLayout(
        width: Int,
        height: Int,
        boardWidth: Int,
        boardHeight: Int,
        imageOffsetX: Int = 0,
        imageOffsetY: Int = 0,
    ): BoardSplitLayout? {
        if (width <= 0 || height <= 0 || boardWidth <= 0 || boardHeight <= 0) {
            return null
        }
        val localOffsetX = Math.floorMod(-imageOffsetX.toLong(), boardWidth.toLong()).toInt()
        val localOffsetY = Math.floorMod(-imageOffsetY.toLong(), boardHeight.toLong()).toInt()
        val vertical = periodicInternalPositions(width, boardWidth, localOffsetX)
        val horizontal = periodicInternalPositions(height, boardHeight, localOffsetY)
        return BoardSplitLayout(
            boardWidth = boardWidth,
            boardHeight = boardHeight,
            imageOffsetX = imageOffsetX,
            imageOffsetY = imageOffsetY,
            verticalDividers = vertical,
            horizontalDividers = horizontal,
            segments =
                vertical.map { x -> GridLineSegment(GridPoint(x, 0), GridPoint(x, height)) } +
                    horizontal.map { y -> GridLineSegment(GridPoint(0, y), GridPoint(width, y)) },
        )
    }

    private fun periodicInternalPositions(
        length: Int,
        period: Int,
        localOffset: Int,
    ): List<Int> {
        var position = if (localOffset == 0) period.toLong() else localOffset.toLong()
        return buildList {
            while (position < length.toLong()) {
                add(position.toInt())
                position += period.toLong()
            }
        }
    }

    fun normalizeSegments(
        segments: List<GridLineSegment>,
        width: Int,
        height: Int,
    ): List<GridLineSegment> {
        if (width <= 0 || height <= 0) {
            return emptyList()
        }
        return segments
            .mapNotNull { segment -> normalizeSegment(segment, width, height) }
            .distinct()
    }

    fun split(
        project: BeadProject,
        segments: List<GridLineSegment>,
        maxParts: Int = DEFAULT_MAX_PARTS,
        maxOutputCells: Long = maxOf(DEFAULT_MAX_OUTPUT_CELLS, project.cells.size.toLong()),
        updatedAt: Long = System.currentTimeMillis(),
    ): ProjectSplitResult {
        if (
            project.width <= 0 ||
            project.height <= 0 ||
            project.width.toLong() * project.height.toLong() != project.cells.size.toLong()
        ) {
            return ProjectSplitResult.Failure(
                reason = ProjectSplitFailureReason.InvalidGrid,
                message = "项目网格数据无效。",
            )
        }
        val normalized = normalizeSegments(segments, project.width, project.height)
        val barriers = buildBarriers(normalized, project.width, project.height)
        if (barriers.isEmpty()) {
            return ProjectSplitResult.Failure(
                reason = ProjectSplitFailureReason.NoEffectiveLines,
                message = "还没有能切开画布的拆分线。",
            )
        }

        val partLimit = maxParts.coerceAtLeast(1)
        val collected = collectOccupiedRegions(project, barriers, partLimit)
        val occupiedRegions =
            collected.regions
                .sortedWith(compareBy<OccupiedRegion> { it.top }.thenBy { it.left })
        if (occupiedRegions.size < 2) {
            return ProjectSplitResult.Failure(
                reason = ProjectSplitFailureReason.NotSeparated,
                message = "这些线还没有把有内容的画布分成至少两个区域。",
            )
        }
        if (occupiedRegions.size > partLimit) {
            return ProjectSplitResult.Failure(
                reason = ProjectSplitFailureReason.TooManyParts,
                message = "拆分结果过多，最多支持 $partLimit 个。",
            )
        }
        val outputCellCount =
            occupiedRegions.sumOf { region ->
                (region.right - region.left + 1).toLong() * (region.bottom - region.top + 1).toLong()
            }
        if (outputCellCount > maxOutputCells.coerceAtLeast(1L)) {
            return ProjectSplitResult.Failure(
                reason = ProjectSplitFailureReason.OutputTooLarge,
                message = "拆分结果总尺寸过大，请减少拆分区域。",
            )
        }

        val projects =
            occupiedRegions.mapIndexed { index, region ->
                val outputWidth = region.right - region.left + 1
                val outputHeight = region.bottom - region.top + 1
                val output = MutableList<String?>(outputWidth * outputHeight) { null }
                region.occupiedIndices.forEach { sourceIndex ->
                    val colorId = project.cells[sourceIndex] ?: return@forEach
                    val sourceX = sourceIndex % project.width
                    val sourceY = sourceIndex / project.width
                    val targetX = sourceX - region.left
                    val targetY = sourceY - region.top
                    output[targetY * outputWidth + targetX] = colorId
                }
                project.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${project.title} ${index + 1}",
                    width = outputWidth,
                    height = outputHeight,
                    cells = output,
                    sourceKind = ProjectSourceKind.Split,
                    sourceImagePath = null,
                    stitchSourceRegions = emptyList(),
                    updatedAt = updatedAt.coerceAtLeast(0L),
                )
            }
        return ProjectSplitResult.Success(projects = projects, geometricRegionCount = collected.geometricRegionCount)
    }

    private fun equalGridSegments(
        width: Int,
        height: Int,
        columns: Int,
        rows: Int,
    ): List<GridLineSegment> {
        val vertical =
            (1 until columns).mapNotNull { index ->
                val x = (index.toLong() * width / columns).toInt()
                if (x in 1 until width) GridLineSegment(GridPoint(x, 0), GridPoint(x, height)) else null
            }
        val horizontal =
            (1 until rows).mapNotNull { index ->
                val y = (index.toLong() * height / rows).toInt()
                if (y in 1 until height) GridLineSegment(GridPoint(0, y), GridPoint(width, y)) else null
            }
        return (vertical + horizontal).distinct()
    }

    private fun bestFactorLayout(
        width: Int,
        height: Int,
        partCount: Int,
    ): Pair<Int, Int>? {
        return (1..partCount)
            .asSequence()
            .filter { columns -> partCount % columns == 0 }
            .map { columns -> columns to partCount / columns }
            .filter { (columns, rows) -> columns <= width && rows <= height }
            .minWithOrNull(
                compareBy<Pair<Int, Int>> { (columns, rows) ->
                    val tileWidth = width.toDouble() / columns
                    val tileHeight = height.toDouble() / rows
                    abs(ln(tileWidth / tileHeight))
                }.thenByDescending { (columns, _) -> columns },
            )
    }

    private fun normalizeSegment(
        segment: GridLineSegment,
        width: Int,
        height: Int,
    ): GridLineSegment? {
        if (!segment.isHorizontal && !segment.isVertical) {
            return null
        }
        val start = GridPoint(segment.start.x.coerceIn(0, width), segment.start.y.coerceIn(0, height))
        val end = GridPoint(segment.end.x.coerceIn(0, width), segment.end.y.coerceIn(0, height))
        return when {
            start.x == end.x && start.y != end.y -> {
                val top = minOf(start.y, end.y)
                val bottom = maxOf(start.y, end.y)
                GridLineSegment(GridPoint(start.x, top), GridPoint(start.x, bottom))
            }
            start.y == end.y && start.x != end.x -> {
                val left = minOf(start.x, end.x)
                val right = maxOf(start.x, end.x)
                GridLineSegment(GridPoint(left, start.y), GridPoint(right, start.y))
            }
            else -> null
        }
    }

    private fun buildBarriers(
        segments: List<GridLineSegment>,
        width: Int,
        height: Int,
    ): GridBarriers {
        val vertical = BooleanArray((width - 1).coerceAtLeast(0) * height)
        val horizontal = BooleanArray(width * (height - 1).coerceAtLeast(0))
        segments.forEach { segment ->
            if (segment.isVertical && segment.start.x in 1 until width) {
                for (y in segment.start.y until segment.end.y) {
                    if (y in 0 until height) {
                        vertical[y * (width - 1) + segment.start.x - 1] = true
                    }
                }
            } else if (segment.isHorizontal && segment.start.y in 1 until height) {
                for (x in segment.start.x until segment.end.x) {
                    if (x in 0 until width) {
                        horizontal[(segment.start.y - 1) * width + x] = true
                    }
                }
            }
        }
        return GridBarriers(vertical = vertical, horizontal = horizontal)
    }

    private fun collectOccupiedRegions(
        project: BeadProject,
        barriers: GridBarriers,
        maxOccupiedRegions: Int,
    ): CollectedRegions {
        val width = project.width
        val height = project.height
        val visited = BooleanArray(width * height)
        val regions = mutableListOf<OccupiedRegion>()
        val queue = IntArray(visited.size)
        val occupiedBuffer = IntArray(visited.size)
        var geometricRegionCount = 0
        for (start in visited.indices) {
            if (visited[start]) continue
            var queueHead = 0
            var queueTail = 0
            var occupiedCount = 0
            var left = width
            var top = height
            var right = -1
            var bottom = -1
            visited[start] = true
            queue[queueTail++] = start
            while (queueHead < queueTail) {
                val index = queue[queueHead++]
                val x = index % width
                val y = index / width
                if (project.cells[index] != null) {
                    occupiedBuffer[occupiedCount++] = index
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
                fun visit(
                    neighbor: Int,
                    blocked: Boolean,
                ) {
                    if (!blocked && !visited[neighbor]) {
                        visited[neighbor] = true
                        queue[queueTail++] = neighbor
                    }
                }
                if (x > 0) visit(index - 1, barriers.vertical[y * (width - 1) + x - 1])
                if (x + 1 < width) visit(index + 1, barriers.vertical[y * (width - 1) + x])
                if (y > 0) visit(index - width, barriers.horizontal[(y - 1) * width + x])
                if (y + 1 < height) visit(index + width, barriers.horizontal[y * width + x])
            }
            geometricRegionCount += 1
            if (occupiedCount > 0) {
                regions +=
                    OccupiedRegion(
                        occupiedIndices = occupiedBuffer.copyOf(occupiedCount),
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                    )
                if (regions.size > maxOccupiedRegions) {
                    return CollectedRegions(regions = regions, geometricRegionCount = geometricRegionCount)
                }
            }
        }
        return CollectedRegions(regions = regions, geometricRegionCount = geometricRegionCount)
    }

    private fun appendVertex(
        vertices: MutableList<GridPoint>,
        point: GridPoint,
    ) {
        if (vertices.lastOrNull() != point) {
            vertices += point
        }
    }

    private fun simplifyVertices(vertices: List<GridPoint>): List<GridPoint> {
        val output = mutableListOf<GridPoint>()
        vertices.forEach { point ->
            appendVertex(output, point)
            while (output.size >= 3) {
                val first = output[output.lastIndex - 2]
                val middle = output[output.lastIndex - 1]
                val last = output.last()
                val collinear =
                    (first.x == middle.x && middle.x == last.x) ||
                        (first.y == middle.y && middle.y == last.y)
                if (!collinear) break
                output.removeAt(output.lastIndex - 1)
            }
        }
        return output
    }

    private data class OccupiedRegion(
        val occupiedIndices: IntArray,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    private data class CollectedRegions(
        val regions: List<OccupiedRegion>,
        val geometricRegionCount: Int,
    )

    private data class GridBarriers(
        val vertical: BooleanArray,
        val horizontal: BooleanArray,
    ) {
        fun isEmpty(): Boolean = vertical.none { it } && horizontal.none { it }
    }
}

internal fun nextUniqueProjectTitle(
    proposed: String,
    existingTitles: Collection<String>,
): String {
    val base = proposed.trim().ifEmpty { "未命名项目" }
    val used = existingTitles.asSequence().map(String::trim).filter(String::isNotEmpty).toHashSet()
    if (base !in used) return base
    return generateSequence(2) { it + 1 }
        .map { index -> "$base ($index)" }
        .first { candidate -> candidate !in used }
}
