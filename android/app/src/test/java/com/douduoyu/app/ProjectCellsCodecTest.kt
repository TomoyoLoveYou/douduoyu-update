package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCellsCodecTest {
    @Test
    fun roundTrip_preservesLargeGridAndNullCells() {
        val cells =
            List<String?>(512 * 512) { index ->
                when {
                    index % 17 == 0 -> null
                    index % 3 == 0 -> "A1"
                    index % 3 == 1 -> "B2"
                    else -> "C3"
                }
            }

        val encoded = ProjectCellsCodec.encode(cells)
        val decoded = ProjectCellsCodec.decode(encoded.palette, encoded.payload, encoded.cellCount)

        assertEquals(cells, decoded)
        assertTrue(encoded.payload.size < cells.size * 2)
    }

    @Test
    fun decode_rejectsTruncatedPayload() {
        val encoded = ProjectCellsCodec.encode(listOf("A1", null, "B1"))

        val decoded =
            ProjectCellsCodec.decode(
                palette = encoded.palette,
                payload = encoded.payload.copyOf(encoded.payload.size / 2),
                cellCount = encoded.cellCount,
            )

        assertEquals(null, decoded)
    }
}
