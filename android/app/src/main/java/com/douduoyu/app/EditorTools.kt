package com.douduoyu.app

import kotlin.math.max
import kotlin.math.min

object EditorTools {
    data class GridTransform(
        val width: Int,
        val height: Int,
        val cells: List<String?>,
    )

    data class RemovableOuterEmptyLines(
        val up: Int,
        val down: Int,
        val left: Int,
        val right: Int,
    ) {
        val total: Int
            get() = up + down + left + right

        operator fun get(direction: CanvasExpandDirection): Int =
            when (direction) {
                CanvasExpandDirection.Up -> up
                CanvasExpandDirection.Down -> down
                CanvasExpandDirection.Left -> left
                CanvasExpandDirection.Right -> right
            }
    }

    data class GridBounds(
        val startColumn: Int,
        val endColumn: Int,
        val startRow: Int,
        val endRow: Int,
    ) {
        val width: Int
            get() = endColumn - startColumn + 1
        val height: Int
            get() = endRow - startRow + 1
    }

    fun occupiedBounds(
        cells: List<String?>,
        width: Int,
        height: Int,
    ): GridBounds? {
        if (!isValidGrid(cells = cells, width = width, height = height)) {
            return null
        }

        var minColumn = width
        var maxColumn = -1
        var minRow = height
        var maxRow = -1
        for (row in 0 until height) {
            for (column in 0 until width) {
                if (cells.getOrNull(row * width + column) == null) {
                    continue
                }
                minColumn = min(minColumn, column)
                maxColumn = max(maxColumn, column)
                minRow = min(minRow, row)
                maxRow = max(maxRow, row)
            }
        }

        if (maxColumn < 0 || maxRow < 0) {
            return null
        }
        return GridBounds(
            startColumn = minColumn,
            endColumn = maxColumn,
            startRow = minRow,
            endRow = maxRow,
        )
    }

    fun removableOuterEmptyLines(
        cells: List<String?>,
        width: Int,
        height: Int,
    ): RemovableOuterEmptyLines {
        if (!isValidGrid(cells = cells, width = width, height = height)) {
            return RemovableOuterEmptyLines(up = 0, down = 0, left = 0, right = 0)
        }

        val bounds = occupiedBounds(cells = cells, width = width, height = height)
        if (bounds == null) {
            return RemovableOuterEmptyLines(
                up = height - 1,
                down = height - 1,
                left = width - 1,
                right = width - 1,
            )
        }

        return RemovableOuterEmptyLines(
            up = bounds.startRow,
            down = height - 1 - bounds.endRow,
            left = bounds.startColumn,
            right = width - 1 - bounds.endColumn,
        )
    }

    fun removeOuterEmptyLine(
        cells: List<String?>,
        width: Int,
        height: Int,
        direction: CanvasExpandDirection,
    ): GridTransform {
        if (!isValidGrid(cells = cells, width = width, height = height)) {
            return GridTransform(width = width, height = height, cells = cells)
        }

        val canRemove = removableOuterEmptyLines(cells = cells, width = width, height = height)[direction] > 0
        if (!canRemove) {
            return GridTransform(width = width, height = height, cells = cells)
        }

        return when (direction) {
            CanvasExpandDirection.Up ->
                GridTransform(
                    width = width,
                    height = height - 1,
                    cells = cells.drop(width),
                )

            CanvasExpandDirection.Down ->
                GridTransform(
                    width = width,
                    height = height - 1,
                    cells = cells.dropLast(width),
                )

            CanvasExpandDirection.Left,
            CanvasExpandDirection.Right,
            -> {
                val removedColumn = if (direction == CanvasExpandDirection.Left) 0 else width - 1
                val nextCells =
                    buildList<String?>((width - 1) * height) {
                        for (row in 0 until height) {
                            for (column in 0 until width) {
                                if (column != removedColumn) {
                                    add(cells[row * width + column])
                                }
                            }
                        }
                    }
                GridTransform(
                    width = width - 1,
                    height = height,
                    cells = nextCells,
                )
            }
        }
    }

    fun removeAllOuterEmptyLines(
        cells: List<String?>,
        width: Int,
        height: Int,
    ): GridTransform {
        if (!isValidGrid(cells = cells, width = width, height = height)) {
            return GridTransform(width = width, height = height, cells = cells)
        }
        if (cells.none { it != null }) {
            return GridTransform(width = 1, height = 1, cells = listOf(null))
        }
        return trimOuterEmptyCells(cells = cells, width = width, height = height)
    }

    fun trimOuterEmptyCells(
        cells: List<String?>,
        width: Int,
        height: Int,
    ): GridTransform {
        if (!isValidGrid(cells = cells, width = width, height = height)) {
            return GridTransform(width = width, height = height, cells = cells)
        }
        val bounds = occupiedBounds(cells = cells, width = width, height = height)
            ?: return GridTransform(width = width, height = height, cells = cells)
        if (bounds.width == width && bounds.height == height) {
            return GridTransform(width = width, height = height, cells = cells)
        }

        val trimmedCells =
            buildList<String?>(bounds.width * bounds.height) {
                for (row in bounds.startRow..bounds.endRow) {
                    for (column in bounds.startColumn..bounds.endColumn) {
                        add(cells.getOrNull(row * width + column))
                    }
                }
            }
        return GridTransform(
            width = bounds.width,
            height = bounds.height,
            cells = trimmedCells,
        )
    }

    fun floodFill(
        cells: List<String?>,
        width: Int,
        height: Int,
        startIndex: Int,
        replacement: String,
    ): List<String?> {
        if (startIndex !in cells.indices) {
            return cells
        }
        val next = cells.toMutableList()
        val target = next[startIndex]
        if (target == replacement) {
            return cells
        }

        val queue = ArrayDeque<Int>()
        queue.addLast(startIndex)

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            if (next[index] != target) {
                continue
            }
            next[index] = replacement
            for (neighbor in orthogonalNeighbors(index, width, height)) {
                if (next[neighbor] == target) {
                    queue.addLast(neighbor)
                }
            }
        }

        return next
    }

    fun collectConnectedCellIndices(
        cells: List<String?>,
        width: Int,
        height: Int,
        startIndex: Int,
    ): List<Int> {
        if (startIndex !in cells.indices || width <= 0 || height <= 0) {
            return emptyList()
        }
        val target = cells[startIndex]
        val seen = BooleanArray(cells.size)
        val queue = IntArray(cells.size)
        val result = ArrayList<Int>()
        var queueStart = 0
        var queueEnd = 0

        fun enqueue(index: Int) {
            if (index in cells.indices && !seen[index] && cells[index] == target) {
                seen[index] = true
                queue[queueEnd++] = index
            }
        }

        enqueue(startIndex)
        while (queueStart < queueEnd) {
            val index = queue[queueStart++]
            result += index
            val x = index % width
            val y = index / width
            if (x > 0) enqueue(index - 1)
            if (x + 1 < width) enqueue(index + 1)
            if (y > 0) enqueue(index - width)
            if (y + 1 < height) enqueue(index + width)
        }

        return result
    }

    fun collectSameColorIndices(
        cells: List<String?>,
        startIndex: Int,
    ): List<Int> {
        if (startIndex !in cells.indices) {
            return emptyList()
        }
        val target = cells[startIndex] ?: return emptyList()
        return cells.mapIndexedNotNull { index, colorCode -> if (colorCode == target) index else null }
    }

    fun collectConnectedOccupiedIndices(
        cells: List<String?>,
        width: Int,
        height: Int,
        startIndex: Int,
    ): List<Int> {
        if (startIndex !in cells.indices || cells[startIndex] == null) {
            return emptyList()
        }

        val seen = linkedSetOf<Int>()
        val queue = ArrayDeque<Int>()
        queue.addLast(startIndex)

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            if (!seen.add(index) || cells[index] == null) {
                continue
            }
            allNeighbors(index, width, height)
                .filterTo(queue) { neighbor -> !seen.contains(neighbor) && cells[neighbor] != null }
        }

        return seen.toList()
    }

    fun clearIndices(
        cells: List<String?>,
        indices: Collection<Int>,
    ): List<String?> {
        if (indices.isEmpty()) {
            return cells
        }
        val next = cells.toMutableList()
        indices.forEach { index ->
            if (index in next.indices) {
                next[index] = null
            }
        }
        return next
    }

    fun recolorAll(
        cells: List<String?>,
        sourceColorCode: String,
        replacementColorCode: String,
    ): List<String?> {
        if (sourceColorCode == replacementColorCode) {
            return cells
        }
        return cells.map { colorCode ->
            if (colorCode == sourceColorCode) {
                replacementColorCode
            } else {
                colorCode
            }
        }
    }

    fun mirrorSelection(
        cells: List<String?>,
        width: Int,
        height: Int,
        direction: MirrorDirection,
        selectionIndices: List<Int>,
    ): List<String?> {
        if (selectionIndices.isEmpty()) {
            return cells
        }

        val normalizedSelection = selectionIndices.filter { it in cells.indices }.distinct()
        if (normalizedSelection.isEmpty()) {
            return cells
        }

        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1
        normalizedSelection.forEach { index ->
            val x = index % width
            val y = index / width
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        val selected = normalizedSelection.toSet()
        val next = cells.toMutableList()
        val canMirror =
            normalizedSelection.all { index ->
                val x = index % width
                val y = index / width
                val nextX = if (direction == MirrorDirection.Horizontal) minX + maxX - x else x
                val nextY = if (direction == MirrorDirection.Vertical) minY + maxY - y else y
                if (nextX !in 0 until width || nextY !in 0 until height) {
                    return@all false
                }
                val nextIndex = nextY * width + nextX
                selected.contains(nextIndex) || cells[nextIndex] == null
            }

        if (!canMirror) {
            return cells
        }

        normalizedSelection.forEach { next[it] = null }
        normalizedSelection.forEach { index ->
            val colorCode = cells[index] ?: return@forEach
            val x = index % width
            val y = index / width
            val nextX = if (direction == MirrorDirection.Horizontal) minX + maxX - x else x
            val nextY = if (direction == MirrorDirection.Vertical) minY + maxY - y else y
            next[nextY * width + nextX] = colorCode
        }

        return next
    }

    fun mirrorWholeProject(
        cells: List<String?>,
        width: Int,
        height: Int,
        direction: MirrorDirection,
    ): List<String?> {
        val occupied = cells.mapIndexedNotNull { index, colorCode -> if (colorCode != null) index else null }
        if (occupied.isEmpty()) {
            return cells
        }

        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1
        occupied.forEach { index ->
            val x = index % width
            val y = index / width
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        val next = MutableList<String?>(width * height) { null }
        occupied.forEach { index ->
            val colorCode = cells[index] ?: return@forEach
            val x = index % width
            val y = index / width
            val nextX = if (direction == MirrorDirection.Horizontal) minX + maxX - x else x
            val nextY = if (direction == MirrorDirection.Vertical) minY + maxY - y else y
            next[nextY * width + nextX] = colorCode
        }
        return next
    }

    fun rotateWholeProject(
        cells: List<String?>,
        width: Int,
        height: Int,
        direction: RotationDirection,
    ): GridTransform {
        if (cells.isEmpty() || width <= 0 || height <= 0) {
            return GridTransform(width = width, height = height, cells = cells)
        }

        val nextWidth = height
        val nextHeight = width
        val next = MutableList<String?>(nextWidth * nextHeight) { null }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val colorCode = cells[y * width + x]
                val nextX: Int
                val nextY: Int
                if (direction == RotationDirection.Clockwise) {
                    nextX = height - 1 - y
                    nextY = x
                } else {
                    nextX = y
                    nextY = width - 1 - x
                }
                next[nextY * nextWidth + nextX] = colorCode
            }
        }

        return GridTransform(width = nextWidth, height = nextHeight, cells = next)
    }

    fun expandCanvas(
        cells: List<String?>,
        width: Int,
        height: Int,
        direction: CanvasExpandDirection,
    ): GridTransform {
        if (width <= 0 || height <= 0 || cells.size != width * height) {
            return GridTransform(width = width, height = height, cells = cells)
        }

        val nextWidth = if (direction == CanvasExpandDirection.Left || direction == CanvasExpandDirection.Right) width + 1 else width
        val nextHeight = if (direction == CanvasExpandDirection.Up || direction == CanvasExpandDirection.Down) height + 1 else height
        val xOffset = if (direction == CanvasExpandDirection.Left) 1 else 0
        val yOffset = if (direction == CanvasExpandDirection.Up) 1 else 0
        val next = MutableList<String?>(nextWidth * nextHeight) { null }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val nextX = x + xOffset
                val nextY = y + yOffset
                next[nextY * nextWidth + nextX] = cells[y * width + x]
            }
        }

        return GridTransform(width = nextWidth, height = nextHeight, cells = next)
    }

    private fun isValidGrid(
        cells: List<String?>,
        width: Int,
        height: Int,
    ): Boolean =
        width > 0 &&
            height > 0 &&
            width.toLong() * height.toLong() == cells.size.toLong()

    private fun orthogonalNeighbors(
        index: Int,
        width: Int,
        height: Int,
    ): List<Int> {
        val x = index % width
        val y = index / width
        return buildList(4) {
            if (x > 0) add(index - 1)
            if (x + 1 < width) add(index + 1)
            if (y > 0) add(index - width)
            if (y + 1 < height) add(index + width)
        }
    }

    private fun allNeighbors(
        index: Int,
        width: Int,
        height: Int,
    ): List<Int> {
        val x = index % width
        val y = index / width
        return buildList(8) {
            for (deltaY in -1..1) {
                for (deltaX in -1..1) {
                    if (deltaX == 0 && deltaY == 0) {
                        continue
                    }
                    val nextX = x + deltaX
                    val nextY = y + deltaY
                    if (nextX in 0 until width && nextY in 0 until height) {
                        add(nextY * width + nextX)
                    }
                }
            }
        }
    }
}
