package com.douduoyu.app

import kotlin.math.roundToInt

internal object OutputLevelScale {
    const val MIN_SHORT_EDGE = 4
    const val LOW_RANGE_END = 64
    const val MEDIUM_RANGE_END = 128
    const val HIGH_RANGE_END = 256
    const val MAX_SHORT_EDGE = OutputSizePolicy.MAX_SHORT_EDGE

    private const val LOW_RANGE_POSITION_END = 0.40f
    private const val MEDIUM_RANGE_POSITION_END = 0.62f
    private const val HIGH_RANGE_POSITION_END = 0.82f

    val tickLevels = listOf(MIN_SHORT_EDGE, LOW_RANGE_END, MEDIUM_RANGE_END, HIGH_RANGE_END, MAX_SHORT_EDGE)

    fun levelForPosition(position: Float): Int {
        val normalized = position.coerceIn(0f, 1f)
        return when {
            normalized <= LOW_RANGE_POSITION_END -> {
                val fraction = normalized / LOW_RANGE_POSITION_END
                val raw = MIN_SHORT_EDGE + fraction * (LOW_RANGE_END - MIN_SHORT_EDGE)
                snap(raw, step = 1, start = MIN_SHORT_EDGE, end = LOW_RANGE_END)
            }

            normalized <= MEDIUM_RANGE_POSITION_END -> {
                val fraction =
                    (normalized - LOW_RANGE_POSITION_END) /
                        (MEDIUM_RANGE_POSITION_END - LOW_RANGE_POSITION_END)
                val raw = LOW_RANGE_END + fraction * (MEDIUM_RANGE_END - LOW_RANGE_END)
                snap(raw, step = 2, start = LOW_RANGE_END, end = MEDIUM_RANGE_END)
            }

            normalized <= HIGH_RANGE_POSITION_END -> {
                val fraction =
                    (normalized - MEDIUM_RANGE_POSITION_END) /
                        (HIGH_RANGE_POSITION_END - MEDIUM_RANGE_POSITION_END)
                val raw = MEDIUM_RANGE_END + fraction * (HIGH_RANGE_END - MEDIUM_RANGE_END)
                snap(raw, step = 4, start = MEDIUM_RANGE_END, end = HIGH_RANGE_END)
            }

            else -> {
                val fraction =
                    (normalized - HIGH_RANGE_POSITION_END) /
                        (1f - HIGH_RANGE_POSITION_END)
                val raw = HIGH_RANGE_END + fraction * (MAX_SHORT_EDGE - HIGH_RANGE_END)
                snap(raw, step = 8, start = HIGH_RANGE_END, end = MAX_SHORT_EDGE)
            }
        }
    }

    fun positionForLevel(level: Int): Float {
        val normalized = level.coerceIn(MIN_SHORT_EDGE, MAX_SHORT_EDGE)
        return when {
            normalized <= LOW_RANGE_END -> {
                val fraction =
                    (normalized - MIN_SHORT_EDGE).toFloat() /
                        (LOW_RANGE_END - MIN_SHORT_EDGE)
                fraction * LOW_RANGE_POSITION_END
            }

            normalized <= MEDIUM_RANGE_END -> {
                val fraction =
                    (normalized - LOW_RANGE_END).toFloat() /
                        (MEDIUM_RANGE_END - LOW_RANGE_END)
                LOW_RANGE_POSITION_END +
                    fraction * (MEDIUM_RANGE_POSITION_END - LOW_RANGE_POSITION_END)
            }

            normalized <= HIGH_RANGE_END -> {
                val fraction =
                    (normalized - MEDIUM_RANGE_END).toFloat() /
                        (HIGH_RANGE_END - MEDIUM_RANGE_END)
                MEDIUM_RANGE_POSITION_END +
                    fraction * (HIGH_RANGE_POSITION_END - MEDIUM_RANGE_POSITION_END)
            }

            else -> {
                val fraction =
                    (normalized - HIGH_RANGE_END).toFloat() /
                        (MAX_SHORT_EDGE - HIGH_RANGE_END)
                HIGH_RANGE_POSITION_END +
                    fraction * (1f - HIGH_RANGE_POSITION_END)
            }
        }.coerceIn(0f, 1f)
    }

    private fun snap(raw: Float, step: Int, start: Int, end: Int): Int {
        val offset = ((raw - start) / step).roundToInt() * step
        return (start + offset).coerceIn(start, end)
    }
}
