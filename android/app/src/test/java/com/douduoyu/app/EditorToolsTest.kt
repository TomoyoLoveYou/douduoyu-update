package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorToolsTest {
    @Test
    fun floodFill_replacesOnlyConnectedRegion() {
        val cells =
            listOf<String?>(
                null, null, "H1",
                null, "H1", "H1",
            )

        val filled =
            EditorTools.floodFill(
                cells = cells,
                width = 3,
                height = 2,
                startIndex = 0,
                replacement = "A1",
            )

        assertEquals(
            listOf<String?>(
                "A1", "A1", "H1",
                "A1", "H1", "H1",
            ),
            filled,
        )
    }

    @Test
    fun connectedOccupiedRegion_includesDiagonalNeighbors() {
        val cells =
            listOf<String?>(
                "A1", null,
                null, "A1",
            )

        val region =
            EditorTools.collectConnectedOccupiedIndices(
                cells = cells,
                width = 2,
                height = 2,
                startIndex = 0,
            )

        assertEquals(setOf(0, 3), region.toSet())
    }

    @Test
    fun mirrorWholeProject_usesOccupiedBoundingBox() {
        val cells =
            listOf<String?>(
                "A1", "H1", null, null,
                "A1", "H1", null, null,
            )

        val mirrored =
            EditorTools.mirrorWholeProject(
                cells = cells,
                width = 4,
                height = 2,
                direction = MirrorDirection.Horizontal,
            )

        assertEquals(
            listOf<String?>(
                "H1", "A1", null, null,
                "H1", "A1", null, null,
            ),
            mirrored,
        )
    }

    @Test
    fun usageSummary_countsPacksAndIsolatedBeads() {
        val project =
            BeadProject(
                title = "usage",
                width = 4,
                height = 1,
                cells = listOf("A1", "A1", "A1", "H1"),
                beadsPerPack = 2,
                updatedAt = 0L,
            )

        val summary = UsageAnalyzer.summarize(project)

        assertEquals(4, summary.totalBeads)
        assertEquals(2, summary.totalColors)
        assertEquals(1, summary.isolatedBeads)
        assertEquals(2, summary.stats.first { it.code == "A1" }.packs)
        assertTrue(summary.stats.any { it.code == "H1" && it.isolated == 1 })
    }
}
