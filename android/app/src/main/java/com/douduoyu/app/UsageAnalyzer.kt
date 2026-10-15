package com.douduoyu.app

import kotlin.math.ceil

data class UsageStat(
    val code: String,
    val displayCode: String,
    val count: Int,
    val packs: Int,
    val isolated: Int,
    val argb: Int,
)

data class UsageSummary(
    val totalBeads: Int,
    val totalColors: Int,
    val isolatedBeads: Int,
    val beadsPerPack: Int,
    val stats: List<UsageStat>,
)

object UsageAnalyzer {
    fun summarize(project: BeadProject): UsageSummary {
        val colorCounts = linkedMapOf<String, Int>()
        val isolatedCounts = linkedMapOf<String, Int>()
        var isolatedTotal = 0

        project.cells.forEachIndexed { index, colorCode ->
            if (colorCode == null) {
                return@forEachIndexed
            }

            colorCounts[colorCode] = (colorCounts[colorCode] ?: 0) + 1
            if (hasMatchingNeighbor(project.cells, project.width, project.height, index, colorCode)) {
                return@forEachIndexed
            }

            isolatedTotal += 1
            isolatedCounts[colorCode] = (isolatedCounts[colorCode] ?: 0) + 1
        }

        val stats =
            colorCounts.entries
                .mapNotNull { (code, count) ->
                    val color = BeadPalette.find(code) ?: return@mapNotNull null
                    UsageStat(
                        code = code,
                        displayCode = color.code,
                        count = count,
                        packs = ceil(count / project.beadsPerPack.toDouble()).toInt().coerceAtLeast(1),
                        isolated = isolatedCounts[code] ?: 0,
                        argb = color.argb,
                    )
                }
                .sortedWith(compareByDescending<UsageStat> { it.count }.thenBy { it.displayCode })

        return UsageSummary(
            totalBeads = stats.sumOf { it.count },
            totalColors = stats.size,
            isolatedBeads = isolatedTotal,
            beadsPerPack = project.beadsPerPack,
            stats = stats,
        )
    }

    fun findIsolatedBeads(
        cells: List<String?>,
        width: Int,
        height: Int,
    ): List<Int> {
        return cells.mapIndexedNotNull { index, colorCode ->
            if (colorCode == null) {
                return@mapIndexedNotNull null
            }

            if (hasMatchingNeighbor(cells, width, height, index, colorCode)) {
                null
            } else {
                index
            }
        }
    }

    private fun hasMatchingNeighbor(
        cells: List<String?>,
        width: Int,
        height: Int,
        index: Int,
        colorCode: String,
    ): Boolean {
        val x = index % width
        val y = index / width
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
                if (cells[nextY * width + nextX] == colorCode) {
                    return true
                }
            }
        }
        return false
    }
}
