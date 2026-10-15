package com.douduoyu.app

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal object BoardViewportMath {
    internal const val MAX_SCROLL_PROXY_DIMENSION_PX = 24_000

    data class AxisState(
        val logicalScrollPx: Float,
        val panToScrollScale: Float,
        val proxyContentSizePx: Int,
        val proxyMaxScrollPx: Int,
    )

    fun resolveAxis(
        logicalContentSizePx: Float,
        viewportSizePx: Int,
        scrollValuePx: Int,
    ): AxisState {
        val safeViewport = viewportSizePx.coerceAtLeast(0)
        val safeLogicalContent = logicalContentSizePx.coerceAtLeast(safeViewport.toFloat())
        val proxyContentSize =
            ceil(safeLogicalContent.toDouble())
                .toInt()
                .coerceAtMost(MAX_SCROLL_PROXY_DIMENSION_PX)
                .coerceAtLeast(safeViewport)
        val proxyMaxScroll = (proxyContentSize - safeViewport).coerceAtLeast(0)
        val logicalMaxScroll = (safeLogicalContent - safeViewport).coerceAtLeast(0f)
        val safeScrollValue = scrollValuePx.coerceIn(0, proxyMaxScroll)
        val logicalScroll =
            if (logicalMaxScroll > 0f && proxyMaxScroll > 0) {
                safeScrollValue.toFloat() / proxyMaxScroll * logicalMaxScroll
            } else {
                0f
            }
        val panToScrollScale =
            if (logicalMaxScroll > 0f && proxyMaxScroll > 0) {
                proxyMaxScroll / logicalMaxScroll
            } else {
                1f
            }
        return AxisState(
            logicalScrollPx = logicalScroll,
            panToScrollScale = panToScrollScale,
            proxyContentSizePx = proxyContentSize,
            proxyMaxScrollPx = proxyMaxScroll,
        )
    }

    fun visibleRange(
        boardStartPx: Float,
        cellSizePx: Float,
        cellCount: Int,
        viewportSizePx: Int,
        overscanCells: Int = 1,
    ): IntRange? {
        if (cellCount <= 0 || cellSizePx <= 0f || viewportSizePx <= 0) {
            return null
        }
        val boardEndPx = boardStartPx + cellCount * cellSizePx
        if (boardEndPx <= 0f || boardStartPx >= viewportSizePx) {
            return null
        }
        val rawStart = floor((0f - boardStartPx) / cellSizePx).toInt()
        val rawEnd = floor((viewportSizePx - boardStartPx) / cellSizePx).toInt()
        val overscan = overscanCells.coerceAtLeast(0)
        val start = (rawStart - overscan).coerceIn(0, cellCount - 1)
        val end = (rawEnd + overscan).coerceIn(0, cellCount - 1)
        return if (end >= start) start..end else null
    }

    fun centeredScrollValue(
        boardSizePx: Float,
        viewportSizePx: Int,
        leadingPaddingPx: Float,
        trailingPaddingPx: Float,
        viewportLeadingInsetPx: Int = 0,
        viewportTrailingInsetPx: Int = 0,
    ): Int {
        if (
            !boardSizePx.isFinite() ||
                !leadingPaddingPx.isFinite() ||
                !trailingPaddingPx.isFinite() ||
                boardSizePx < 0f ||
                viewportSizePx <= 0
        ) {
            return 0
        }

        val safeLeadingPadding = leadingPaddingPx.coerceAtLeast(0f)
        val safeTrailingPadding = trailingPaddingPx.coerceAtLeast(0f)
        val logicalContentSize = boardSizePx + safeLeadingPadding + safeTrailingPadding
        val logicalMaxScroll = (logicalContentSize - viewportSizePx).coerceAtLeast(0f)
        if (logicalMaxScroll <= 0f) {
            return 0
        }

        val visibleStart = viewportLeadingInsetPx.coerceIn(0, viewportSizePx).toFloat()
        val visibleEnd =
            (viewportSizePx - viewportTrailingInsetPx.coerceAtLeast(0))
                .coerceIn(visibleStart.toInt(), viewportSizePx)
                .toFloat()
        val centeredBoardStart = visibleStart + (visibleEnd - visibleStart - boardSizePx) / 2f
        val desiredLogicalScroll =
            (safeLeadingPadding - centeredBoardStart)
                .coerceIn(0f, logicalMaxScroll)
        val proxyMaxScroll =
            resolveAxis(
                logicalContentSizePx = logicalContentSize,
                viewportSizePx = viewportSizePx,
                scrollValuePx = 0,
            ).proxyMaxScrollPx
        if (proxyMaxScroll <= 0) {
            return 0
        }

        return (desiredLogicalScroll / logicalMaxScroll * proxyMaxScroll)
            .roundToInt()
            .coerceIn(0, proxyMaxScroll)
    }
}
