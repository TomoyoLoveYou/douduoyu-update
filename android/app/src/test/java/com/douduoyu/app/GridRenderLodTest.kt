package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridRenderLodTest {
    @Test
    fun fullGridIsShownAtReadableCellSize() {
        val lod = GridRenderLodPolicy.resolve(cellEdgePx = 20f, configuredInterval = 5)

        assertTrue(lod?.showMinorLines == true)
        assertEquals(5, lod?.majorStrideCells)
    }

    @Test
    fun minorLinesHideBeforeConfiguredMajorLines() {
        val lod = GridRenderLodPolicy.resolve(cellEdgePx = 11.9f, configuredInterval = 5)

        assertFalse(lod?.showMinorLines ?: true)
        assertEquals(5, lod?.majorStrideCells)
    }

    @Test
    fun minorLinesReturnAtRaisedReadabilityThreshold() {
        val lod = GridRenderLodPolicy.resolve(cellEdgePx = 12f, configuredInterval = 5)

        assertTrue(lod?.showMinorLines == true)
        assertEquals(5, lod?.majorStrideCells)
    }

    @Test
    fun majorLinesThinOutInStablePowersOfTwo() {
        assertEquals(20, GridRenderLodPolicy.resolve(cellEdgePx = 1f, configuredInterval = 5)?.majorStrideCells)
        assertEquals(80, GridRenderLodPolicy.resolve(cellEdgePx = 0.25f, configuredInterval = 5)?.majorStrideCells)
    }

    @Test
    fun invalidCellSizeHidesGridSafely() {
        assertNull(GridRenderLodPolicy.resolve(cellEdgePx = 0f, configuredInterval = 5))
        assertNull(GridRenderLodPolicy.resolve(cellEdgePx = Float.NaN, configuredInterval = 5))
    }
}
