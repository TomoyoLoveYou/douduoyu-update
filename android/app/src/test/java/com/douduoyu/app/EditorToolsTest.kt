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
    fun collectConnectedCellIndices_returnsOnlyMatchingOrthogonalRegion() {
        val cells =
            listOf<String?>(
                "A1", "A1", "B1",
                "A1", "B1", "B1",
                "C1", "B1", "A1",
            )

        val region =
            EditorTools.collectConnectedCellIndices(
                cells = cells,
                width = 3,
                height = 3,
                startIndex = 0,
            )

        assertEquals(setOf(0, 1, 3), region.toSet())
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
    fun trimOuterEmptyCells_cropsOnlyOuterRowsAndColumns() {
        val cells =
            listOf<String?>(
                null, null, null, null, null,
                null, "A1", null, "H1", null,
                null, null, null, null, null,
                null, null, null, "P1", null,
                null, null, null, null, null,
            )

        val trimmed = EditorTools.trimOuterEmptyCells(cells = cells, width = 5, height = 5)

        assertEquals(3, trimmed.width)
        assertEquals(3, trimmed.height)
        assertEquals(
            listOf<String?>(
                "A1", null, "H1",
                null, null, null,
                null, null, "P1",
            ),
            trimmed.cells,
        )
    }

    @Test
    fun trimOuterEmptyCells_reducesSingleBeadToOneCell() {
        val trimmed =
            EditorTools.trimOuterEmptyCells(
                cells =
                    listOf<String?>(
                        null, null, null,
                        null, "A1", null,
                        null, null, null,
                    ),
                width = 3,
                height = 3,
            )

        assertEquals(EditorTools.GridTransform(width = 1, height = 1, cells = listOf("A1")), trimmed)
    }

    @Test
    fun trimOuterEmptyCells_keepsEmptyProjectSize() {
        val cells = List<String?>(12) { null }

        val trimmed = EditorTools.trimOuterEmptyCells(cells = cells, width = 4, height = 3)

        assertEquals(EditorTools.GridTransform(width = 4, height = 3, cells = cells), trimmed)
    }

    @Test
    fun removeAllOuterEmptyLines_trimsOccupiedGridAndReducesEmptyGridToOneCell() {
        val occupied =
            listOf<String?>(
                null, null, null, null,
                null, "A1", null, null,
                null, null, null, null,
            )

        assertEquals(
            EditorTools.GridTransform(width = 1, height = 1, cells = listOf("A1")),
            EditorTools.removeAllOuterEmptyLines(occupied, width = 4, height = 3),
        )
        assertEquals(
            EditorTools.GridTransform(width = 1, height = 1, cells = listOf(null)),
            EditorTools.removeAllOuterEmptyLines(List(12) { null }, width = 4, height = 3),
        )
    }

    @Test
    fun removableOuterEmptyLines_countsEachOccupiedBoundMargin() {
        val cells =
            listOf<String?>(
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, "A1", null, "H1", null,
                null, null, null, "P1", null, null,
                null, null, null, null, null, null,
            )

        val removable = EditorTools.removableOuterEmptyLines(cells = cells, width = 6, height = 5)

        assertEquals(
            EditorTools.RemovableOuterEmptyLines(up = 2, down = 1, left = 2, right = 1),
            removable,
        )
        assertEquals(2, removable[CanvasExpandDirection.Up])
        assertEquals(1, removable[CanvasExpandDirection.Right])
    }

    @Test
    fun removeOuterEmptyLine_removesOneLineFromEachDirection() {
        val cells =
            listOf<String?>(
                null, null, null, null,
                null, "A1", "H1", null,
                null, "P1", "Q1", null,
                null, null, null, null,
            )

        assertEquals(
            EditorTools.GridTransform(
                width = 4,
                height = 3,
                cells =
                    listOf<String?>(
                        null, "A1", "H1", null,
                        null, "P1", "Q1", null,
                        null, null, null, null,
                    ),
            ),
            EditorTools.removeOuterEmptyLine(cells, 4, 4, CanvasExpandDirection.Up),
        )
        assertEquals(
            EditorTools.GridTransform(
                width = 4,
                height = 3,
                cells =
                    listOf<String?>(
                        null, null, null, null,
                        null, "A1", "H1", null,
                        null, "P1", "Q1", null,
                    ),
            ),
            EditorTools.removeOuterEmptyLine(cells, 4, 4, CanvasExpandDirection.Down),
        )
        assertEquals(
            EditorTools.GridTransform(
                width = 3,
                height = 4,
                cells =
                    listOf<String?>(
                        null, null, null,
                        "A1", "H1", null,
                        "P1", "Q1", null,
                        null, null, null,
                    ),
            ),
            EditorTools.removeOuterEmptyLine(cells, 4, 4, CanvasExpandDirection.Left),
        )
        assertEquals(
            EditorTools.GridTransform(
                width = 3,
                height = 4,
                cells =
                    listOf<String?>(
                        null, null, null,
                        null, "A1", "H1",
                        null, "P1", "Q1",
                        null, null, null,
                    ),
            ),
            EditorTools.removeOuterEmptyLine(cells, 4, 4, CanvasExpandDirection.Right),
        )
    }

    @Test
    fun removeOuterEmptyLine_keepsGridWhenRequestedEdgeIsOccupied() {
        val cells =
            listOf<String?>(
                null, "A1", null,
                null, null, null,
            )

        val result = EditorTools.removeOuterEmptyLine(cells, 3, 2, CanvasExpandDirection.Up)

        assertEquals(EditorTools.GridTransform(width = 3, height = 2, cells = cells), result)
    }

    @Test
    fun emptyGrid_removalNeverReducesEitherDimensionBelowOne() {
        val original = listOf<String?>(null, null, null, null)
        val first = EditorTools.removeOuterEmptyLine(original, 2, 2, CanvasExpandDirection.Up)
        val second = EditorTools.removeOuterEmptyLine(first.cells, first.width, first.height, CanvasExpandDirection.Down)
        val third = EditorTools.removeOuterEmptyLine(second.cells, second.width, second.height, CanvasExpandDirection.Left)
        val fourth = EditorTools.removeOuterEmptyLine(third.cells, third.width, third.height, CanvasExpandDirection.Right)

        assertEquals(EditorTools.GridTransform(width = 1, height = 1, cells = listOf(null)), third)
        assertEquals(third, fourth)
        assertEquals(
            EditorTools.RemovableOuterEmptyLines(up = 0, down = 0, left = 0, right = 0),
            EditorTools.removableOuterEmptyLines(third.cells, third.width, third.height),
        )
    }

    @Test
    fun invalidGrid_allCanvasReductionOperationsKeepOriginalValues() {
        val cells = listOf<String?>(null, "A1")

        assertEquals(
            EditorTools.RemovableOuterEmptyLines(up = 0, down = 0, left = 0, right = 0),
            EditorTools.removableOuterEmptyLines(cells, width = 3, height = 2),
        )
        assertEquals(
            EditorTools.GridTransform(width = 3, height = 2, cells = cells),
            EditorTools.removeOuterEmptyLine(cells, 3, 2, CanvasExpandDirection.Left),
        )
        assertEquals(
            EditorTools.GridTransform(width = 3, height = 2, cells = cells),
            EditorTools.trimOuterEmptyCells(cells, 3, 2),
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
