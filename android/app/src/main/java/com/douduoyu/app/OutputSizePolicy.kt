package com.douduoyu.app

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class OutputDimensions(
    val width: Int,
    val height: Int,
) {
    val cellCount: Int
        get() = (width.toLong() * height.toLong()).toInt()
}

internal object OutputSizePolicy {
    const val MAX_TOTAL_CELLS = 262_144
    const val MAX_DIMENSION = 2_048
    const val MAX_SHORT_EDGE = 512

    fun constrainDimensions(
        width: Int,
        height: Int,
    ): OutputDimensions {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        if (fits(safeWidth, safeHeight)) {
            return OutputDimensions(safeWidth, safeHeight)
        }

        val axisScale = MAX_DIMENSION.toDouble() / maxOf(safeWidth, safeHeight).toDouble()
        val cellScale = sqrt(MAX_TOTAL_CELLS.toDouble() / (safeWidth.toDouble() * safeHeight.toDouble()))
        val scale = min(1.0, min(axisScale, cellScale))
        var constrainedWidth = floor(safeWidth * scale).toInt().coerceIn(1, MAX_DIMENSION)
        var constrainedHeight = floor(safeHeight * scale).toInt().coerceIn(1, MAX_DIMENSION)

        while (constrainedWidth.toLong() * constrainedHeight.toLong() > MAX_TOTAL_CELLS) {
            if (constrainedWidth >= constrainedHeight && constrainedWidth > 1) {
                constrainedWidth -= 1
            } else if (constrainedHeight > 1) {
                constrainedHeight -= 1
            } else {
                break
            }
        }
        return OutputDimensions(constrainedWidth, constrainedHeight)
    }

    fun maxShortEdge(
        sourceWidth: Int,
        sourceHeight: Int,
    ): Int {
        val safeWidth = sourceWidth.coerceAtLeast(1)
        val safeHeight = sourceHeight.coerceAtLeast(1)
        var low = 1
        var high = MAX_SHORT_EDGE
        var best = 1
        while (low <= high) {
            val candidate = (low + high) ushr 1
            val dimensions = dimensionsForShortEdge(safeWidth, safeHeight, candidate)
            if (fits(dimensions.width, dimensions.height)) {
                best = candidate
                low = candidate + 1
            } else {
                high = candidate - 1
            }
        }
        return best
    }

    private fun dimensionsForShortEdge(
        sourceWidth: Int,
        sourceHeight: Int,
        shortEdge: Int,
    ): OutputDimensions {
        val safeShortEdge = shortEdge.coerceAtLeast(1)
        return if (sourceWidth >= sourceHeight) {
            OutputDimensions(
                width = (safeShortEdge * sourceWidth.toDouble() / sourceHeight.toDouble()).roundToInt().coerceAtLeast(1),
                height = safeShortEdge,
            )
        } else {
            OutputDimensions(
                width = safeShortEdge,
                height = (safeShortEdge * sourceHeight.toDouble() / sourceWidth.toDouble()).roundToInt().coerceAtLeast(1),
            )
        }
    }

    private fun fits(
        width: Int,
        height: Int,
    ): Boolean =
        width in 1..MAX_DIMENSION &&
            height in 1..MAX_DIMENSION &&
            width.toLong() * height.toLong() <= MAX_TOTAL_CELLS
}
