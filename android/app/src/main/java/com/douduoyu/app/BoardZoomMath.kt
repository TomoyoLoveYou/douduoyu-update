package com.douduoyu.app

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

internal object BoardZoomMath {
    const val DEFAULT_FIT_MARGIN_DP = 12f
    // Stitched projects may be much longer than generated projects, so keep only a tiny
    // positive floor for logarithmic slider math instead of imposing a visible-size limit.
    const val ABSOLUTE_MIN_ZOOM_DP = 0.0001f

    /**
     * Returns the largest cell size that keeps the full board inside the viewport.
     * The result is expressed in dp because that is the unit used by PatternBoard.
     */
    fun fitCellSizeDp(
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        density: Float,
        columnCount: Int,
        rowCount: Int,
        marginDp: Float = DEFAULT_FIT_MARGIN_DP,
        insetStartPx: Int = 0,
        insetTopPx: Int = 0,
        insetEndPx: Int = 0,
        insetBottomPx: Int = 0,
    ): Float? {
        if (
            viewportWidthPx <= 0 ||
                viewportHeightPx <= 0 ||
                columnCount <= 0 ||
                rowCount <= 0 ||
                !density.isFinite() ||
                density <= 0f
        ) {
            return null
        }

        val safeMarginDp = marginDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val doubleDensity = density.toDouble()
        val marginPx = safeMarginDp.toDouble() * doubleDensity
        val availableWidthPx =
            viewportWidthPx.toDouble() -
                insetStartPx.coerceAtLeast(0).toDouble() -
                insetEndPx.coerceAtLeast(0).toDouble() -
                marginPx * 2.0
        val availableHeightPx =
            viewportHeightPx.toDouble() -
                insetTopPx.coerceAtLeast(0).toDouble() -
                insetBottomPx.coerceAtLeast(0).toDouble() -
                marginPx * 2.0
        if (availableWidthPx <= 0.0 || availableHeightPx <= 0.0) {
            return null
        }

        val cellSizePx =
            min(
                availableWidthPx / columnCount.toDouble(),
                availableHeightPx / rowCount.toDouble(),
            )
        return (cellSizePx / doubleDensity)
            .toFloat()
            .takeIf { it.isFinite() && it > 0f }
    }

    /**
     * Keeps the familiar minimum for ordinary boards, but allows a large board's fit value
     * to become the slider minimum. The absolute floor guards logarithmic slider math.
     */
    fun minimumZoomDp(
        standardMinimumZoomDp: Float,
        fitCellSizeDp: Float?,
        absoluteMinimumZoomDp: Float = ABSOLUTE_MIN_ZOOM_DP,
    ): Float {
        require(standardMinimumZoomDp.isFinite() && standardMinimumZoomDp > 0f)
        require(absoluteMinimumZoomDp.isFinite() && absoluteMinimumZoomDp > 0f)
        require(absoluteMinimumZoomDp <= standardMinimumZoomDp)

        val fitValue = fitCellSizeDp?.takeIf { it.isFinite() && it > 0f }
        return min(standardMinimumZoomDp, fitValue ?: standardMinimumZoomDp)
            .coerceAtLeast(absoluteMinimumZoomDp)
    }

    fun sliderFractionForZoom(
        zoomDp: Float,
        minimumZoomDp: Float,
        maximumZoomDp: Float,
    ): Float {
        requireValidRange(minimumZoomDp, maximumZoomDp)
        val safeZoomDp = zoomDp.takeIf { it.isFinite() }?.coerceIn(minimumZoomDp, maximumZoomDp) ?: minimumZoomDp
        val logarithmicSpan = ln(maximumZoomDp.toDouble() / minimumZoomDp.toDouble())
        return (ln(safeZoomDp.toDouble() / minimumZoomDp.toDouble()) / logarithmicSpan)
            .toFloat()
            .coerceIn(0f, 1f)
    }

    fun zoomForSliderFraction(
        fraction: Float,
        minimumZoomDp: Float,
        maximumZoomDp: Float,
    ): Float {
        requireValidRange(minimumZoomDp, maximumZoomDp)
        val safeFraction = fraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        val minimum = minimumZoomDp.toDouble()
        val logarithmicSpan = ln(maximumZoomDp.toDouble() / minimum)
        return (minimum * exp(logarithmicSpan * safeFraction.toDouble()))
            .toFloat()
            .coerceIn(minimumZoomDp, maximumZoomDp)
    }

    private fun requireValidRange(
        minimumZoomDp: Float,
        maximumZoomDp: Float,
    ) {
        require(minimumZoomDp.isFinite() && minimumZoomDp > 0f)
        require(maximumZoomDp.isFinite() && maximumZoomDp > minimumZoomDp)
    }
}
