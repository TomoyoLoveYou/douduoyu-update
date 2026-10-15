package com.douduoyu.app

internal object ColorBlockIndexing {
    fun collect(
        cells: List<String?>,
        width: Int,
        height: Int,
        colorCode: String?,
    ): List<IntArray> {
        if (colorCode == null || width <= 0 || height <= 0 || cells.isEmpty()) {
            return emptyList()
        }
        val validCellCount =
            (width.toLong() * height.toLong())
                .coerceAtMost(cells.size.toLong())
                .toInt()
        if (validCellCount <= 0) {
            return emptyList()
        }
        val visited = BooleanArray(validCellCount)
        val queue = IntArray(validCellCount)
        val blocks = ArrayList<IntArray>()

        for (startIndex in 0 until validCellCount) {
            if (visited[startIndex] || cells[startIndex] != colorCode) {
                continue
            }
            var queueStart = 0
            var queueEnd = 0
            queueEnd = enqueue(startIndex, validCellCount, cells, colorCode, visited, queue, queueEnd)
            while (queueStart < queueEnd) {
                val index = queue[queueStart++]
                val x = index % width
                val y = index / width
                if (x > 0) {
                    queueEnd = enqueue(index - 1, validCellCount, cells, colorCode, visited, queue, queueEnd)
                }
                if (x + 1 < width) {
                    queueEnd = enqueue(index + 1, validCellCount, cells, colorCode, visited, queue, queueEnd)
                }
                if (y > 0) {
                    queueEnd = enqueue(index - width, validCellCount, cells, colorCode, visited, queue, queueEnd)
                }
                if (y + 1 < height) {
                    queueEnd = enqueue(index + width, validCellCount, cells, colorCode, visited, queue, queueEnd)
                }
            }
            blocks += queue.copyOf(queueEnd)
        }

        return blocks.sortedByDescending { it.size }
    }

    private fun enqueue(
        index: Int,
        validCellCount: Int,
        cells: List<String?>,
        colorCode: String,
        visited: BooleanArray,
        queue: IntArray,
        queueEnd: Int,
    ): Int {
        if (index !in 0 until validCellCount || visited[index] || cells[index] != colorCode) {
            return queueEnd
        }
        visited[index] = true
        queue[queueEnd] = index
        return queueEnd + 1
    }
}
