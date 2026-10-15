package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputLevelScaleTest {
    @Test
    fun breakpoints_useConfiguredTrackProportions() {
        assertEquals(0f, OutputLevelScale.positionForLevel(4), 0.0001f)
        assertEquals(0.40f, OutputLevelScale.positionForLevel(64), 0.0001f)
        assertEquals(0.62f, OutputLevelScale.positionForLevel(128), 0.0001f)
        assertEquals(0.82f, OutputLevelScale.positionForLevel(256), 0.0001f)
        assertEquals(1f, OutputLevelScale.positionForLevel(512), 0.0001f)
    }

    @Test
    fun boundaries_mapWithoutJumps() {
        OutputLevelScale.tickLevels.forEach { level ->
            val position = OutputLevelScale.positionForLevel(level)
            assertEquals(level, OutputLevelScale.levelForPosition(position))
        }
    }

    @Test
    fun positions_areMonotonicAcrossEntireRange() {
        var previousPosition = -1f
        for (level in OutputLevelScale.MIN_SHORT_EDGE..OutputLevelScale.MAX_SHORT_EDGE) {
            val position = OutputLevelScale.positionForLevel(level)
            assertTrue(position >= previousPosition)
            previousPosition = position
        }
    }

    @Test
    fun adaptiveRanges_useExpectedSteps() {
        assertEquals(37, OutputLevelScale.levelForPosition(OutputLevelScale.positionForLevel(37)))
        assertEquals(96, OutputLevelScale.levelForPosition(OutputLevelScale.positionForLevel(95)))
        assertEquals(192, OutputLevelScale.levelForPosition(OutputLevelScale.positionForLevel(191)))
        assertEquals(384, OutputLevelScale.levelForPosition(OutputLevelScale.positionForLevel(383)))
    }

    @Test
    fun representableLevels_roundTripAcrossEveryRange() {
        val levels =
            (4..64).toList() +
                (66..128 step 2) +
                (132..256 step 4) +
                (264..512 step 8)

        levels.forEach { level ->
            assertEquals(level, OutputLevelScale.levelForPosition(OutputLevelScale.positionForLevel(level)))
        }
    }

    @Test
    fun positions_outsideTrack_areClamped() {
        assertEquals(4, OutputLevelScale.levelForPosition(-1f))
        assertEquals(512, OutputLevelScale.levelForPosition(2f))
    }
}
