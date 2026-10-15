package com.douduoyu.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal data class GridPreviewBitmap(
    val image: ImageBitmap,
    val width: Int,
    val height: Int,
)

internal fun createGridPreviewBitmap(
    cells: List<String?>,
    width: Int,
    height: Int,
    maxBitmapEdge: Int,
    emptyCellArgb: Int = 0x00FFFFFF,
): GridPreviewBitmap {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val safeMaxBitmapEdge = maxBitmapEdge.coerceAtLeast(1)
    val bitmapWidth: Int
    val bitmapHeight: Int
    if (safeWidth >= safeHeight && safeWidth > safeMaxBitmapEdge) {
        bitmapWidth = safeMaxBitmapEdge
        bitmapHeight = scaledPreviewDimension(safeHeight, safeWidth, safeMaxBitmapEdge)
    } else if (safeHeight > safeWidth && safeHeight > safeMaxBitmapEdge) {
        bitmapWidth = scaledPreviewDimension(safeWidth, safeHeight, safeMaxBitmapEdge)
        bitmapHeight = safeMaxBitmapEdge
    } else {
        bitmapWidth = safeWidth
        bitmapHeight = safeHeight
    }
    val colorArgbById = HashMap<String, Int>()
    val pixels =
        IntArray(bitmapWidth * bitmapHeight) { index ->
            val previewX = index % bitmapWidth
            val previewY = index / bitmapWidth
            val sourceX = nearestSourceCoordinate(previewX, bitmapWidth, safeWidth)
            val sourceY = nearestSourceCoordinate(previewY, bitmapHeight, safeHeight)
            val sourceIndex = sourceY.toLong() * safeWidth.toLong() + sourceX.toLong()
            val code = sourceIndex.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.let { cells.getOrNull(it.toInt()) }
            if (code == null) {
                emptyCellArgb
            } else {
                colorArgbById.getOrPut(code) { BeadPalette.find(code)?.argb ?: emptyCellArgb }
            }
        }
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
    return GridPreviewBitmap(bitmap.asImageBitmap(), safeWidth, safeHeight)
}

private fun scaledPreviewDimension(
    minorDimension: Int,
    majorDimension: Int,
    maxBitmapEdge: Int,
): Int =
    ((minorDimension.toLong() * maxBitmapEdge + majorDimension / 2L) / majorDimension)
        .coerceIn(1L, maxBitmapEdge.toLong())
        .toInt()

private fun nearestSourceCoordinate(
    previewCoordinate: Int,
    previewDimension: Int,
    sourceDimension: Int,
): Int =
    (((previewCoordinate.toLong() * 2L + 1L) * sourceDimension) / (previewDimension.toLong() * 2L))
        .coerceIn(0L, sourceDimension.toLong() - 1L)
        .toInt()
