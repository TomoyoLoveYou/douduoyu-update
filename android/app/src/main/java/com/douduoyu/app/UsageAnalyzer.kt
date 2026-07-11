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
        val isolated = findIsolatedBeads(project.cells, project.width, project.height)
        val isolatedCounts = linkedMapOf<String, Int>()
        isolated.forEach { index ->
            val code = project.cells[index] ?: return@forEach
            isolatedCounts[code] = (isolatedCounts[code] ?: 0) + 1
        }

        val stats =
            PatternEngine.materialStats(project.cells).map { stat ->
                UsageStat(
                    code = stat.code,
                    displayCode = stat.displayCode,
                    count = stat.count,
                    packs = ceil(stat.count / project.beadsPerPack.toDouble()).toInt().coerceAtLeast(1),
                    isolated = isolatedCounts[stat.code] ?: 0,
                    argb = stat.argb,
                )
            }

        return UsageSummary(
            totalBeads = stats.sumOf { it.count },
            totalColors = stats.size,
            isolatedBeads = isolated.size,
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

            val x = index % width
            val y = index / width
            var matchesNeighbor = false
            loop@ for (deltaY in -1..1) {
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
                        matchesNeighbor = true
                        break@loop
                    }
                }
            }

            if (matchesNeighbor) {
                null
            } else {
                index
            }
        }
    }
}
