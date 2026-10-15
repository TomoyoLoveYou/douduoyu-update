package com.douduoyu.app

internal data class GridGuideSettings(
    val enabled: Boolean = true,
    val interval: Int = DEFAULT_INTERVAL,
    val lineWidthDp: Float = DEFAULT_LINE_WIDTH_DP,
    val colorArgb: Int = DEFAULT_COLOR_ARGB,
) {
    fun normalized(): GridGuideSettings =
        copy(
            interval = interval.coerceIn(MIN_INTERVAL, MAX_INTERVAL),
            lineWidthDp =
                if (lineWidthDp.isFinite()) {
                    lineWidthDp.coerceIn(MIN_LINE_WIDTH_DP, MAX_LINE_WIDTH_DP)
                } else {
                    DEFAULT_LINE_WIDTH_DP
                },
        )

    companion object {
        const val DEFAULT_INTERVAL = 5
        const val DEFAULT_LINE_WIDTH_DP = 1f
        val DEFAULT_COLOR_ARGB = 0xFF997A58.toInt()
        const val MIN_INTERVAL = 1
        const val MAX_INTERVAL = 20
        const val MIN_LINE_WIDTH_DP = 0.5f
        const val MAX_LINE_WIDTH_DP = 4f
    }
}
