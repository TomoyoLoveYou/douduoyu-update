package com.douduoyu.app

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

internal data class EncodedProjectCells(
    val palette: List<String>,
    val payload: ByteArray,
    val cellCount: Int,
)

internal object ProjectCellsCodec {
    const val FORMAT = "palette-u16-deflate-v1"

    fun encode(cells: List<String?>): EncodedProjectCells {
        val palette = ArrayList<String>()
        val paletteIndexByCode = HashMap<String, Int>()
        cells.forEach { code ->
            if (code != null && code !in paletteIndexByCode) {
                require(palette.size < U16_MAX_VALUE) { "Too many project colors" }
                paletteIndexByCode[code] = palette.size + 1
                palette += code
            }
        }

        val rawSize = cells.size.toLong() * BYTES_PER_CELL
        require(rawSize <= Int.MAX_VALUE) { "Project grid is too large" }
        val raw = ByteArray(rawSize.toInt())
        cells.forEachIndexed { index, code ->
            val paletteIndex = code?.let(paletteIndexByCode::get) ?: 0
            val offset = index * BYTES_PER_CELL
            raw[offset] = (paletteIndex ushr 8).toByte()
            raw[offset + 1] = paletteIndex.toByte()
        }

        val compressed = ByteArrayOutputStream((raw.size / 2).coerceAtLeast(64))
        DeflaterOutputStream(compressed, Deflater(Deflater.BEST_SPEED)).use { output ->
            output.write(raw)
        }
        return EncodedProjectCells(
            palette = palette,
            payload = compressed.toByteArray(),
            cellCount = cells.size,
        )
    }

    fun decode(
        palette: List<String>,
        payload: ByteArray,
        cellCount: Int,
    ): List<String?>? {
        if (cellCount < 0 || palette.size > U16_MAX_VALUE) {
            return null
        }
        val rawSize = cellCount.toLong() * BYTES_PER_CELL
        if (rawSize > Int.MAX_VALUE) {
            return null
        }
        val raw = ByteArray(rawSize.toInt())
        val fullyRead =
            runCatching {
                InflaterInputStream(ByteArrayInputStream(payload)).use { input ->
                    var offset = 0
                    while (offset < raw.size) {
                        val read = input.read(raw, offset, raw.size - offset)
                        if (read < 0) {
                            return@use false
                        }
                        offset += read
                    }
                    input.read() == -1
                }
            }.getOrDefault(false)
        if (!fullyRead) {
            return null
        }

        return List(cellCount) { index ->
            val offset = index * BYTES_PER_CELL
            val paletteIndex = ((raw[offset].toInt() and 0xFF) shl 8) or (raw[offset + 1].toInt() and 0xFF)
            when {
                paletteIndex == 0 -> null
                paletteIndex <= palette.size -> palette[paletteIndex - 1]
                else -> return null
            }
        }
    }

    private const val BYTES_PER_CELL = 2
    private const val U16_MAX_VALUE = 65_535
}
