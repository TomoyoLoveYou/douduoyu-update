package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorBlockIndexingTest {
    @Test
    fun fragmented256Grid_indexesEveryBlockWithoutLosingCells() {
        val width = 256
        val height = 256
        val cells =
            List<String?>(width * height) { index ->
                val x = index % width
                val y = index / width
                if ((x + y) % 2 == 0) "A1" else "B1"
            }

        val blocks =
            ColorBlockIndexing.collect(
                cells = cells,
                width = width,
                height = height,
                colorCode = "A1",
            )

        assertEquals(32_768, blocks.size)
        assertEquals(32_768, blocks.sumOf { it.size })
        assertTrue(blocks.all { it.size == 1 })
    }
}
