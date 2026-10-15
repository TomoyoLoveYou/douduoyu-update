package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardZoomMathTest {
    @Test
    fun square512BoardFitsInsidePortraitViewport() {
        val fit =
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 1_080,
                viewportHeightPx = 1_400,
                density = 3f,
                columnCount = 512,
                rowCount = 512,
            )

        assertEquals(0.65625f, fit ?: 0f, 0.00001f)
    }

    @Test
    fun wide2048By128BoardIsConstrainedByWidth() {
        val fit =
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 1_080,
                viewportHeightPx = 900,
                density = 3f,
                columnCount = 2_048,
                rowCount = 128,
            )

        assertEquals(0.1640625f, fit ?: 0f, 0.00001f)
    }

    @Test
    fun fitExcludesMeasuredToolOverlays() {
        val fit =
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 1_080,
                viewportHeightPx = 2_400,
                density = 3f,
                columnCount = 128,
                rowCount = 512,
                insetTopPx = 216,
                insetBottomPx = 660,
            )

        assertEquals(0.9453125f, fit ?: 0f, 0.00001f)
    }

    @Test
    fun invalidOrUnmeasuredViewportHasNoFitValue() {
        assertNull(
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 0,
                viewportHeightPx = 900,
                density = 3f,
                columnCount = 512,
                rowCount = 512,
            ),
        )
        assertNull(
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 1_080,
                viewportHeightPx = 900,
                density = Float.NaN,
                columnCount = 512,
                rowCount = 512,
            ),
        )
        assertNull(
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 1_080,
                viewportHeightPx = 900,
                density = 3f,
                columnCount = -1,
                rowCount = 512,
            ),
        )
        assertNull(
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 40,
                viewportHeightPx = 40,
                density = 3f,
                columnCount = 512,
                rowCount = 512,
                marginDp = 12f,
            ),
        )
    }

    @Test
    fun minimumZoomUsesFitOnlyWhenBoardNeedsIt() {
        assertEquals(
            0.65625f,
            BoardZoomMath.minimumZoomDp(
                standardMinimumZoomDp = 2f,
                fitCellSizeDp = 0.65625f,
            ),
            0f,
        )
        assertEquals(
            2f,
            BoardZoomMath.minimumZoomDp(
                standardMinimumZoomDp = 2f,
                fitCellSizeDp = 8f,
            ),
            0f,
        )
        assertEquals(
            2f,
            BoardZoomMath.minimumZoomDp(
                standardMinimumZoomDp = 2f,
                fitCellSizeDp = null,
            ),
            0f,
        )
        assertEquals(
            BoardZoomMath.ABSOLUTE_MIN_ZOOM_DP,
            BoardZoomMath.minimumZoomDp(
                standardMinimumZoomDp = 2f,
                fitCellSizeDp = 0.00001f,
            ),
            0f,
        )
    }

    @Test
    fun stitchedSingleRowProjectCanStillFit() {
        val fit =
            BoardZoomMath.fitCellSizeDp(
                viewportWidthPx = 1_080,
                viewportHeightPx = 900,
                density = 3f,
                columnCount = 500_000,
                rowCount = 1,
            )

        assertEquals(0.000672f, fit ?: 0f, 0.0000001f)
        assertEquals(
            fit ?: 0f,
            BoardZoomMath.minimumZoomDp(
                standardMinimumZoomDp = 2f,
                fitCellSizeDp = fit,
            ),
            0f,
        )
    }

    @Test
    fun logarithmicSliderUsesGeometricMidpoint() {
        val zoom =
            BoardZoomMath.zoomForSliderFraction(
                fraction = 0.5f,
                minimumZoomDp = 0.125f,
                maximumZoomDp = 128f,
            )

        assertEquals(4f, zoom, 0.0001f)
        assertEquals(
            0.5f,
            BoardZoomMath.sliderFractionForZoom(
                zoomDp = zoom,
                minimumZoomDp = 0.125f,
                maximumZoomDp = 128f,
            ),
            0.0001f,
        )
    }

    @Test
    fun logarithmicSliderClampsEndpointsAndRoundTrips() {
        val minimum = 0.1640625f
        val maximum = 160f

        assertEquals(
            minimum,
            BoardZoomMath.zoomForSliderFraction(-1f, minimum, maximum),
            0f,
        )
        assertEquals(
            maximum,
            BoardZoomMath.zoomForSliderFraction(2f, minimum, maximum),
            0f,
        )
        assertEquals(0f, BoardZoomMath.sliderFractionForZoom(0f, minimum, maximum), 0f)
        assertEquals(1f, BoardZoomMath.sliderFractionForZoom(1_000f, minimum, maximum), 0f)

        listOf(minimum, 0.5f, 2f, 16f, maximum).forEach { zoom ->
            val restored =
                BoardZoomMath.zoomForSliderFraction(
                    fraction = BoardZoomMath.sliderFractionForZoom(zoom, minimum, maximum),
                    minimumZoomDp = minimum,
                    maximumZoomDp = maximum,
                )
            assertEquals(zoom, restored, zoom * 0.00001f)
        }
    }
}
