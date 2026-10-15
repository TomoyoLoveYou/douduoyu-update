package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputSizePolicyTest {
    @Test
    fun constrainDimensions_preservesSafeSizeAndShrinksOversizedRequests() {
        assertEquals(OutputDimensions(512, 512), OutputSizePolicy.constrainDimensions(512, 512))
        assertEquals(OutputDimensions(512, 512), OutputSizePolicy.constrainDimensions(1024, 1024))
        assertEquals(OutputDimensions(2048, 1), OutputSizePolicy.constrainDimensions(4096, 1))
        assertEquals(OutputDimensions(362, 724), OutputSizePolicy.constrainDimensions(512, 1024))
    }

    @Test
    fun maxShortEdge_respectsAspectRatioCellAndAxisLimits() {
        assertEquals(512, OutputSizePolicy.maxShortEdge(1, 1))
        assertEquals(256, OutputSizePolicy.maxShortEdge(4, 1))
        assertEquals(128, OutputSizePolicy.maxShortEdge(16, 1))
        assertEquals(1, OutputSizePolicy.maxShortEdge(2048, 1))
    }
}
