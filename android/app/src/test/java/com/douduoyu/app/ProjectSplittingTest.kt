package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSplittingTest {
    @Test
    fun nextUniqueProjectTitle_avoidsExistingAndBatchConflicts() {
        val used = mutableSetOf("原项目 1", "原项目 1 (2)", "  其他项目  ")

        val first = nextUniqueProjectTitle("原项目 1", used)
        used += first
        val second = nextUniqueProjectTitle("原项目 1", used)

        assertEquals("原项目 1 (3)", first)
        assertEquals("原项目 1 (4)", second)
        assertEquals("新项目", nextUniqueProjectTitle("  新项目  ", used))
        assertEquals("未命名项目", nextUniqueProjectTitle("   ", used))
    }

    @Test
    fun verticalLine_splitsProjectIntoLeftAndRightProjects() {
        val project =
            filledProject(width = 4, height = 2).copy(
                customColorSchemeId = "custom-scheme",
                beadsPerPack = 750,
                sourceImagePath = "source.png",
                stitchSourceRegions = listOf(StitchSourceRegion("source", "来源", 0, 0, 4, 2)),
            )
        val result =
            ProjectSplitting.split(
                project,
                listOf(GridLineSegment(GridPoint(2, 0), GridPoint(2, 2))),
                updatedAt = 100L,
            ) as ProjectSplitResult.Success

        assertEquals(2, result.projects.size)
        assertEquals(listOf(2 to 2, 2 to 2), result.projects.map { it.width to it.height })
        assertTrue(result.projects.all { it.sourceKind == ProjectSourceKind.Split })
        assertTrue(result.projects.all { it.customColorSchemeId == "custom-scheme" && it.beadsPerPack == 750 })
        assertTrue(result.projects.all { it.sourceImagePath == null && it.stitchSourceRegions.isEmpty() })
    }

    @Test
    fun partialLine_doesNotCreateAFakeSplit() {
        val result =
            ProjectSplitting.split(
                filledProject(width = 4, height = 3),
                listOf(GridLineSegment(GridPoint(2, 0), GridPoint(2, 2))),
            ) as ProjectSplitResult.Failure

        assertEquals(ProjectSplitFailureReason.NotSeparated, result.reason)
    }

    @Test
    fun emptySeparatedRegion_isNotCreatedAsAProject() {
        val project =
            filledProject(width = 4, height = 2).copy(
                cells =
                    listOf(
                        "coco:A01", "coco:A01", null, null,
                        "coco:A01", "coco:A01", null, null,
                    ),
            )
        val result =
            ProjectSplitting.split(
                project,
                listOf(GridLineSegment(GridPoint(2, 0), GridPoint(2, 2))),
            ) as ProjectSplitResult.Failure

        assertEquals(ProjectSplitFailureReason.NotSeparated, result.reason)
    }

    @Test
    fun emptyGeometricRegions_doNotCountAgainstOutputLimit() {
        val project =
            filledProject(width = 6, height = 2).copy(
                cells =
                    listOf(
                        "left", null, null, null, null, "right",
                        "left", null, null, null, null, "right",
                    ),
            )
        val lines =
            (1 until project.width).map { x ->
                GridLineSegment(GridPoint(x, 0), GridPoint(x, project.height))
            }

        val result = ProjectSplitting.split(project, lines, maxParts = 2) as ProjectSplitResult.Success

        assertEquals(6, result.geometricRegionCount)
        assertEquals(2, result.partCount)
        assertEquals(listOf("left", "left", "right", "right"), result.projects.flatMap { it.cells }.filterNotNull())
    }

    @Test
    fun eightPartPreset_createsEightRegions() {
        val project = filledProject(width = 8, height = 4)
        val segments = ProjectSplitting.presetSegments(8, 4, EqualSplitPreset.EightParts)
        val result = ProjectSplitting.split(project, segments) as ProjectSplitResult.Success

        assertEquals(8, result.projects.size)
        assertTrue(result.projects.all { it.width * it.height == 4 })
    }

    @Test
    fun equalPartPresets_createRequestedRegionCounts() {
        val project = filledProject(width = 48, height = 48)
        val presets =
            listOf(
                EqualSplitPreset.FourParts to 4,
                EqualSplitPreset.SixParts to 6,
                EqualSplitPreset.EightParts to 8,
                EqualSplitPreset.NineParts to 9,
                EqualSplitPreset.SixteenParts to 16,
            )

        presets.forEach { (preset, expectedCount) ->
            val result =
                ProjectSplitting.split(
                    project,
                    ProjectSplitting.presetSegments(project.width, project.height, preset),
                ) as ProjectSplitResult.Success

            assertEquals(expectedCount, result.partCount)
        }
    }

    @Test
    fun equalPartPreset_isUnavailableWhenCanvasCannotFitItsFactorLayout() {
        assertTrue(ProjectSplitting.presetSegments(3, 3, EqualSplitPreset.SixteenParts).isEmpty())
    }

    @Test
    fun boardLayout_usesInfiniteBoardBoundariesCrossingTheImage() {
        val layout = ProjectSplitting.boardLayout(width = 50, height = 70, boardWidth = 29, boardHeight = 29)!!

        assertEquals(listOf(29), layout.verticalDividers)
        assertEquals(listOf(29, 58), layout.horizontalDividers)
        assertEquals(2, layout.columnCount)
        assertEquals(3, layout.rowCount)
        assertEquals(6, layout.partCount)
    }

    @Test
    fun boardLayout_draggingImageChangesHowManyBeadsFallOnEachBoard() {
        val centered = ProjectSplitting.boardLayout(width = 50, height = 20, boardWidth = 29, boardHeight = 29)!!
        val shifted = ProjectSplitting.boardLayout(width = 50, height = 20, boardWidth = 29, boardHeight = 29, imageOffsetX = 8)!!

        assertEquals(listOf(29), centered.verticalDividers)
        assertEquals(listOf(21), shifted.verticalDividers)
        assertEquals(8, shifted.imageOffsetX)
    }

    @Test
    fun boardLayout_offsetRepeatsAcrossTheInfiniteBoardPeriod() {
        val first = ProjectSplitting.boardLayout(50, 20, 29, 29, imageOffsetX = -4)!!
        val repeated = ProjectSplitting.boardLayout(50, 20, 29, 29, imageOffsetX = 25)!!

        assertEquals(first.segments, repeated.segments)
    }

    @Test
    fun boardLayout_largerBoardNeedsNoDivider() {
        assertTrue(ProjectSplitting.boardLayout(10, 8, 20, 20)!!.segments.isEmpty())
    }

    @Test
    fun orthogonalizeStroke_locksLegsAndCreatesRightAngleTurn() {
        val vertices =
            ProjectSplitting.orthogonalizeStroke(
                rawPoints =
                    listOf(
                        GridPoint(0, 1),
                        GridPoint(2, 1),
                        GridPoint(4, 1),
                        GridPoint(4, 2),
                        GridPoint(4, 4),
                    ),
                width = 6,
                height = 6,
            )

        assertEquals(listOf(GridPoint(0, 1), GridPoint(4, 1), GridPoint(4, 4)), vertices)
        assertTrue(ProjectSplitting.segmentsForVertices(vertices).all { it.isHorizontal || it.isVertical })
    }

    @Test
    fun orthogonalizeStroke_preservesAOneCellCorner() {
        val vertices =
            ProjectSplitting.orthogonalizeStroke(
                rawPoints = listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(1, 1)),
                width = 2,
                height = 2,
            )

        assertEquals(listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(1, 1)), vertices)
    }

    @Test
    fun crossingLines_createFourPartsAndPreserveEveryColor() {
        val colors = List(16) { index -> "color-$index" }
        val project = filledProject(width = 4, height = 4).copy(cells = colors)
        val result =
            ProjectSplitting.split(
                project,
                listOf(
                    GridLineSegment(GridPoint(2, 0), GridPoint(2, 4)),
                    GridLineSegment(GridPoint(0, 2), GridPoint(4, 2)),
                ),
            ) as ProjectSplitResult.Success

        assertEquals(4, result.partCount)
        assertEquals(colors.sorted(), result.projects.flatMap { it.cells }.filterNotNull().sorted())
    }

    @Test
    fun duplicateAndReversedLines_haveTheSameResultAsOneLine() {
        val line = GridLineSegment(GridPoint(2, 0), GridPoint(2, 3))
        val project = filledProject(width = 4, height = 3)

        val result =
            ProjectSplitting.split(
                project,
                listOf(line, line, GridLineSegment(line.end, line.start)),
            ) as ProjectSplitResult.Success

        assertEquals(2, result.partCount)
    }

    @Test
    fun outerBorder_isNotAnEffectiveSplitLine() {
        val result =
            ProjectSplitting.split(
                filledProject(width = 3, height = 3),
                listOf(GridLineSegment(GridPoint(0, 0), GridPoint(0, 3))),
            ) as ProjectSplitResult.Failure

        assertEquals(ProjectSplitFailureReason.NoEffectiveLines, result.reason)
    }

    @Test
    fun clippingDoesNotTurnAnInvalidDiagonalIntoASplitLine() {
        val result =
            ProjectSplitting.split(
                filledProject(width = 3, height = 3),
                listOf(GridLineSegment(GridPoint(-1, 0), GridPoint(0, 3))),
            ) as ProjectSplitResult.Failure

        assertEquals(ProjectSplitFailureReason.NoEffectiveLines, result.reason)
    }

    @Test
    fun totalOutputArea_isLimitedBeforeProjectsAreAllocated() {
        val result =
            ProjectSplitting.split(
                filledProject(width = 4, height = 4),
                listOf(
                    GridLineSegment(GridPoint(2, 0), GridPoint(2, 4)),
                    GridLineSegment(GridPoint(0, 2), GridPoint(4, 2)),
                ),
                maxOutputCells = 15,
            ) as ProjectSplitResult.Failure

        assertEquals(ProjectSplitFailureReason.OutputTooLarge, result.reason)
    }

    @Test
    fun nonRectangularRegion_doesNotCopyCellsFromNeighboringRegion() {
        val project =
            filledProject(width = 3, height = 3).copy(
                cells =
                    listOf(
                        "A", "B", "C",
                        "D", "E", "F",
                        "G", "H", "I",
                    ),
            )
        val result =
            ProjectSplitting.split(
                project,
                listOf(
                    GridLineSegment(GridPoint(1, 0), GridPoint(1, 2)),
                    GridLineSegment(GridPoint(0, 2), GridPoint(1, 2)),
                ),
            ) as ProjectSplitResult.Success

        assertEquals(2, result.partCount)
        assertEquals(listOf("A", "D"), result.projects.first().cells.filterNotNull())
        assertFalse(result.projects.first().cells.any { it in setOf("B", "C", "E", "F", "G", "H", "I") })
        assertEquals(project.cells.filterNotNull().sorted(), result.projects.flatMap { it.cells }.filterNotNull().sorted())
    }

    @Test
    fun oddSizedHalfPreset_differsByAtMostOneCell() {
        val result =
            ProjectSplitting.split(
                filledProject(width = 5, height = 3),
                ProjectSplitting.presetSegments(5, 3, EqualSplitPreset.VerticalHalves),
            ) as ProjectSplitResult.Success

        assertEquals(listOf(2, 3), result.projects.map(BeadProject::width).sorted())
    }

    @Test
    fun eightPartPreset_isUnavailableWhenCanvasCannotContainEightParts() {
        assertTrue(ProjectSplitting.presetSegments(2, 2, EqualSplitPreset.EightParts).isEmpty())
    }

    private fun filledProject(
        width: Int,
        height: Int,
    ): BeadProject {
        return BeadProject(
            title = "测试项目",
            width = width,
            height = height,
            cells = List(width * height) { "coco:A01" },
            updatedAt = 1L,
        )
    }
}
