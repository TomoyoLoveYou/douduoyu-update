package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardViewportMathTest {
    @Test
    fun normalCanvasKeepsOneToOneScroll() {
        val state =
            BoardViewportMath.resolveAxis(
                logicalContentSizePx = 15_600f,
                viewportSizePx = 1_080,
                scrollValuePx = 2_400,
            )

        assertEquals(2_400f, state.logicalScrollPx, 0.01f)
        assertEquals(1f, state.panToScrollScale, 0.0001f)
        assertEquals(15_600, state.proxyContentSizePx)
    }

    @Test
    fun hugeCanvasUsesBoundedProxyAndPreservesCenter() {
        val logicalContent = 123_200f
        val viewport = 1_080
        val proxyMax = BoardViewportMath.MAX_SCROLL_PROXY_DIMENSION_PX - viewport
        val state =
            BoardViewportMath.resolveAxis(
                logicalContentSizePx = logicalContent,
                viewportSizePx = viewport,
                scrollValuePx = proxyMax / 2,
            )

        assertEquals(BoardViewportMath.MAX_SCROLL_PROXY_DIMENSION_PX, state.proxyContentSizePx)
        assertEquals((logicalContent - viewport) / 2f, state.logicalScrollPx, 1f)
        assertTrue(state.panToScrollScale in 0f..1f)
    }

    @Test
    fun visibleRangeReturnsOnlyViewportCellsWithOverscan() {
        val range =
            BoardViewportMath.visibleRange(
                boardStartPx = -600f,
                cellSizePx = 60f,
                cellCount = 256,
                viewportSizePx = 1_080,
                overscanCells = 1,
            )

        assertEquals(9..29, range)
    }

    @Test
    fun offscreenBoardHasNoVisibleRange() {
        assertNull(
            BoardViewportMath.visibleRange(
                boardStartPx = 1_200f,
                cellSizePx = 60f,
                cellCount = 256,
                viewportSizePx = 1_080,
            ),
        )
    }

    @Test
    fun centeredScrollUsesLeadingPaddingInsteadOfProxyMidpoint() {
        val target =
            BoardViewportMath.centeredScrollValue(
                boardSizePx = 2_000f,
                viewportSizePx = 2_400,
                leadingPaddingPx = 536f,
                trailingPaddingPx = 920f,
            )

        assertEquals(336, target)
    }

    @Test
    fun centeredScrollTargetsUncoveredViewportArea() {
        val target =
            BoardViewportMath.centeredScrollValue(
                boardSizePx = 1_000f,
                viewportSizePx = 2_400,
                leadingPaddingPx = 1_036f,
                trailingPaddingPx = 1_420f,
                viewportLeadingInsetPx = 200,
                viewportTrailingInsetPx = 600,
            )

        assertEquals(536, target)
    }

    @Test
    fun centeredScrollMapsHugeCanvasIntoBoundedProxy() {
        val target =
            BoardViewportMath.centeredScrollValue(
                boardSizePx = 100_000f,
                viewportSizePx = 1_080,
                leadingPaddingPx = 100f,
                trailingPaddingPx = 200f,
            )

        assertEquals(11_448, target)
    }
}
