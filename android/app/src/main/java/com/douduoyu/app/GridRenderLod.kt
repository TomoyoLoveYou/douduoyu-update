package com.douduoyu.app

import kotlin.math.ceil

internal data class GridRenderLod(
    val showMinorLines: Boolean,
    val majorStrideCells: Int,
)

internal object GridRenderLodPolicy {
    const val MIN_CELL_EDGE_FOR_MINOR_LINES_PX = 12f
    const val MIN_MAJOR_LINE_SPACING_PX = 16f

    fun resolve(
        cellEdgePx: Float,
        configuredInterval: Int,
    ): GridRenderLod? {
        if (!cellEdgePx.isFinite() || cellEdgePx <= 0f) {
            return null
        }

        val interval = configuredInterval.coerceAtLeast(1)
        val configuredSpacingPx = cellEdgePx.toDouble() * interval.toDouble()
        val requiredMultiplier =
            ceil(MIN_MAJOR_LINE_SPACING_PX.toDouble() / configuredSpacingPx)
                .toLong()
                .coerceAtLeast(1L)
        val maximumMultiplier = (Int.MAX_VALUE / interval).toLong().coerceAtLeast(1L)
        var multiplier = 1L
        while (multiplier < requiredMultiplier && multiplier <= maximumMultiplier / 2L) {
            multiplier *= 2L
        }
        if (multiplier < requiredMultiplier) {
            multiplier = maximumMultiplier
        }

        return GridRenderLod(
            showMinorLines = cellEdgePx >= MIN_CELL_EDGE_FOR_MINOR_LINES_PX,
            majorStrideCells = (interval.toLong() * multiplier).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        )
    }
}
