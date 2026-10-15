package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAnalyzerTest {
    @Test
    fun summarize_sparse512Grid_countsWithoutChangingResults() {
        val width = 512
        val height = 512
        val cells =
            List<String?>(width * height) { index ->
                val row = index / width
                val column = index % width
                if (row % 4 == 0 && column % 4 == 0) "A1" else null
            }
        val project =
            BeadProject(
                title = "large usage",
                width = width,
                height = height,
                cells = cells,
                updatedAt = 0L,
            )

        val summary = UsageAnalyzer.summarize(project)

        assertEquals(16_384, summary.totalBeads)
        assertEquals(1, summary.totalColors)
        assertEquals(16_384, summary.isolatedBeads)
        assertEquals(16_384, summary.stats.single().isolated)
    }

    @Test
    fun findIsolatedBeads_keepsReturningCellIndices() {
        val cells =
            listOf<String?>(
                "A1", "A1", null, "H1",
                null, null, null, "A2",
            )

        val isolated = UsageAnalyzer.findIsolatedBeads(cells, width = 4, height = 2)

        assertEquals(listOf(3, 7), isolated)
    }

    @Test
    fun summarize_unknownColor_keepsIsolationCompatibility() {
        val project =
            BeadProject(
                title = "unknown color",
                width = 1,
                height = 1,
                cells = listOf("__missing_usage_test_color__"),
                updatedAt = 0L,
            )

        val summary = UsageAnalyzer.summarize(project)

        assertEquals(0, summary.totalBeads)
        assertEquals(0, summary.totalColors)
        assertEquals(1, summary.isolatedBeads)
        assertTrue(summary.stats.isEmpty())
    }
}
