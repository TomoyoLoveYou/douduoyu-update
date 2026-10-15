package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectStitchingTest {
    @Test
    fun initialHorizontalLayout_placesLayersEdgeToEdge() {
        val first = project(id = "first", width = 2, height = 1, cells = listOf(null, null))
        val second = project(id = "second", width = 3, height = 1, cells = listOf(null, null, null))

        val layers = ProjectStitching.initialHorizontalLayout(listOf(first, second))

        assertEquals(0, layers[0].x)
        assertEquals(2, layers[1].x)
        assertEquals(0, layers[1].y)
    }

    @Test
    fun setTrimmed_preservesOccupiedWorldPositionsAndRoundTrips() {
        val cells = MutableList<String?>(25) { null }.apply { this[1 * 5 + 2] = mardColor(ColorSchemePreset.MardBrand24, "A4").id }
        val source = project(id = "trim", width = 5, height = 5, cells = cells)
        val original = StitchLayer(project = source, x = 10, y = -3)

        val trimmed = ProjectStitching.setTrimmed(original, true)

        assertEquals(12, trimmed.x)
        assertEquals(-2, trimmed.y)
        assertEquals(1, trimmed.width)
        assertEquals(1, trimmed.height)
        assertEquals(source.cells[7], trimmed.cells.single())
        assertEquals(original.x + 2, trimmed.x)
        assertEquals(original.y + 1, trimmed.y)

        assertEquals(original, ProjectStitching.setTrimmed(trimmed, false))
        assertEquals(cells, source.cells)
    }

    @Test
    fun setTrimmed_keepsEmptyProjectDimensionsAndPosition() {
        val source = project(id = "empty", width = 4, height = 3, cells = List(12) { null })
        val original = StitchLayer(source, x = -8, y = 6)

        val trimmed = ProjectStitching.setTrimmed(original, true)

        assertEquals(-8, trimmed.x)
        assertEquals(6, trimmed.y)
        assertEquals(4, trimmed.width)
        assertEquals(3, trimmed.height)
    }

    @Test
    fun movePlaceRelativeAndAlignEdge_useIntegerGridGeometry() {
        val reference = StitchLayer(project("reference", 2, 2, List(4) { null }), x = 5, y = 7)
        val moving = StitchLayer(project("moving", 1, 1, listOf(null)))

        assertEquals(4, ProjectStitching.placeRelative(moving, reference, StitchSide.Left).x)
        assertEquals(7, ProjectStitching.placeRelative(moving, reference, StitchSide.Right).x)
        assertEquals(6, ProjectStitching.placeRelative(moving, reference, StitchSide.Above).y)
        assertEquals(9, ProjectStitching.placeRelative(moving, reference, StitchSide.Below).y)

        val endAligned = ProjectStitching.placeRelative(moving, reference, StitchSide.Right, StitchAlignment.End)
        assertEquals(8, endAligned.y)
        assertEquals(6, ProjectStitching.alignEdge(moving, reference, StitchEdge.Right).x)
        assertEquals(8, ProjectStitching.alignEdge(moving, reference, StitchEdge.Bottom).y)
        assertEquals(StitchLayer(moving.project, -3, 4), ProjectStitching.move(moving, -3, 4))
    }

    @Test
    fun snap_joinsNearbyEdgesAndAlignsNearbySecondaryEdge() {
        val reference = StitchLayer(project("reference", 2, 2, List(4) { null }))
        val moving = StitchLayer(project("moving", 1, 1, listOf(null)), x = 3, y = 0)

        val result = ProjectStitching.snap(moving, listOf(reference), threshold = 1)

        assertTrue(result.snapped)
        assertEquals(2, result.layer.x)
        assertEquals(0, result.layer.y)
        assertEquals(StitchSide.Right, result.candidate?.side)
        assertEquals(StitchAlignment.Start, result.candidate?.alignment)
    }

    @Test
    fun snap_doesNotJoinOrthogonallyDistantLayersOrThrowAtIntBoundary() {
        val reference = StitchLayer(project("reference", 10, 10, List(100) { null }))
        val farAway = StitchLayer(project("moving", 1, 1, listOf(null)), x = 10, y = 100)

        assertFalse(ProjectStitching.snap(farAway, listOf(reference)).snapped)

        val boundary = StitchLayer(project("boundary", 1, 1, listOf(null)), x = Int.MAX_VALUE, y = 0)
        ProjectStitching.snapCandidates(
            moving = StitchLayer(project("edge-moving", 1, 1, listOf(null)), x = Int.MAX_VALUE, y = 0),
            others = listOf(boundary),
        )
    }

    @Test
    fun compatibilityKey_usesBuiltInBrandAndCustomGroup() {
        val builtIn = project("built-in", 1, 1, listOf(null), scheme = ColorSchemePreset.MardBrand24)
        assertEquals(
            PaletteCompatibilityKey.BuiltInBrand("mard"),
            ProjectStitching.compatibilityKey(builtIn),
        )

        val baseColors = BeadPalette.palette(ColorSchemePreset.MardBrand48).take(3).map(PaletteColor::code)
        val olderLarger = customScheme("custom-large", "group", baseColors, updatedAt = 1L)
        val newerSmaller = customScheme("custom-small", "group", baseColors.take(2), updatedAt = 2L)
        try {
            BeadPalette.registerCustomColorSchemes(listOf(olderLarger, newerSmaller))
            val largeProject =
                project(
                    id = "large",
                    width = 1,
                    height = 1,
                    cells = listOf(olderLarger.palette().first().id),
                    scheme = olderLarger.baseScheme,
                    customId = olderLarger.id,
                )
            val smallProject =
                project(
                    id = "small",
                    width = 1,
                    height = 1,
                    cells = listOf(newerSmaller.palette().first().id),
                    scheme = newerSmaller.baseScheme,
                    customId = newerSmaller.id,
                )

            assertEquals(
                PaletteCompatibilityKey.CustomGroup("group"),
                ProjectStitching.compatibilityKey(largeProject),
            )
            val options = ProjectStitching.targetPaletteOptions(listOf(StitchLayer(largeProject), StitchLayer(smallProject)))
            assertFalse(options.requiresUserChoice)
            assertEquals(olderLarger.id, options.automatic?.customColorSchemeId)

            val dodo = BeadPalette.palette(ColorSchemePreset.Dodo24).first()
            val crossGroupOptions =
                ProjectStitching.targetPaletteOptions(
                    listOf(
                        StitchLayer(largeProject),
                        StitchLayer(smallProject),
                        StitchLayer(project("dodo-custom-test", 1, 1, listOf(dodo.id), ColorSchemePreset.Dodo24)),
                    ),
                )
            val customChoices = crossGroupOptions.choices.filter { it.customColorSchemeId != null }
            assertEquals(listOf(olderLarger.id, newerSmaller.id), customChoices.map(StitchPalette::customColorSchemeId))
        } finally {
            BeadPalette.registerCustomColorSchemes(emptyList())
        }
    }

    @Test
    fun targetPaletteOptions_automaticallyChoosesLargerSameBrandScheme() {
        val color24 = mardColor(ColorSchemePreset.MardBrand24, "A4")
        val color48 = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val layers =
            listOf(
                StitchLayer(project("24", 1, 1, listOf(color24.id), ColorSchemePreset.MardBrand24)),
                StitchLayer(project("48", 1, 1, listOf(color48.id), ColorSchemePreset.MardBrand48)),
            )

        val options = ProjectStitching.targetPaletteOptions(layers)

        assertFalse(options.requiresUserChoice)
        assertEquals(ColorSchemePreset.MardBrand48, options.automatic?.colorScheme)
    }

    @Test
    fun targetPaletteOptions_customGroupChoosesVariantThatCoversEveryUsedCode() {
        val codes = CustomColorScheme.catalogColors(ColorSchemePreset.MardBrand48).map(PaletteColor::code).distinct()
        val covering = customScheme("covering", "group", listOf(codes[0], codes[1], codes[2]), updatedAt = 1L)
        val newerNonCovering = customScheme("non-covering", "group", listOf(codes[0], codes[2], codes[3]), updatedAt = 3L)
        val smaller = customScheme("smaller", "group", listOf(codes[0], codes[1]), updatedAt = 2L)
        try {
            BeadPalette.registerCustomColorSchemes(listOf(covering, newerNonCovering, smaller))
            val projects =
                listOf(
                    customProject("covering-project", covering, codes[0]),
                    customProject("non-covering-project", newerNonCovering, codes[0]),
                    customProject("smaller-project", smaller, codes[1]),
                )
            val layers = ProjectStitching.initialHorizontalLayout(projects)

            val options = ProjectStitching.targetPaletteOptions(layers)

            assertFalse(options.requiresUserChoice)
            assertEquals(covering.id, options.automatic?.customColorSchemeId)

            val target = options.automatic!!
            val plan = ProjectStitching.buildPalettePlan(layers, target)
            val smallerMapping = plan.mappingFor("smaller-project", smaller.customColorId(codes[1]))!!
            assertEquals(ColorMappingKind.ExactCode, smallerMapping.kind)
            assertEquals(covering.customColorId(codes[1]), smallerMapping.targetColor?.id)
            assertFalse(plan.requiresConversionConfirmation)

            val composed = ProjectStitching.compose(layers, plan) as StitchComposeResult.Success
            val stitched =
                ProjectStitching.buildProject(
                    composed = composed,
                    target = target,
                    id = "custom-stitched",
                    updatedAt = 7L,
                )
            assertEquals(ColorSchemePreset.MardBrand48, stitched.colorScheme)
            assertEquals(covering.id, stitched.customColorSchemeId)
            assertTrue(stitched.cells.filterNotNull().all { it.startsWith("custom:${covering.id}:") })
        } finally {
            BeadPalette.registerCustomColorSchemes(emptyList())
        }
    }

    @Test
    fun targetPaletteOptions_requiresChoiceAcrossBrands() {
        val mard = mardColor(ColorSchemePreset.MardBrand24, "A4")
        val dodo = BeadPalette.palette(ColorSchemePreset.Dodo24).first()
        val layers =
            listOf(
                StitchLayer(project("mard", 1, 1, listOf(mard.id), ColorSchemePreset.MardBrand24)),
                StitchLayer(project("dodo", 1, 1, listOf(dodo.id), ColorSchemePreset.Dodo24)),
            )

        val options = ProjectStitching.targetPaletteOptions(layers)

        assertTrue(options.requiresUserChoice)
        assertNull(options.automatic)
        val mardChoices = options.choices.filter { it.colorScheme.brandId == "mard" }
        assertEquals(ColorSchemePreset.MardBrand295, mardChoices.first().colorScheme)
        assertTrue(mardChoices.any { it.colorScheme == ColorSchemePreset.MardBrand221 })
        assertEquals(
            ColorSchemePreset.visibleSchemesForBrand("mard").toSet(),
            mardChoices.map(StitchPalette::colorScheme).toSet(),
        )
        assertEquals(
            mardChoices.map { it.colorScheme.maxColors }.sortedDescending(),
            mardChoices.map { it.colorScheme.maxColors },
        )
    }

    @Test
    fun buildPalettePlan_mapsSameBrandByExactCodeToTargetId() {
        val sourceColor = mardColor(ColorSchemePreset.MardBrand24, "A4")
        val targetColor = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val layer = StitchLayer(project("source", 2, 1, listOf(sourceColor.id, sourceColor.id), ColorSchemePreset.MardBrand24))

        val plan = ProjectStitching.buildPalettePlan(listOf(layer), StitchPalette(ColorSchemePreset.MardBrand48))
        val mapping = plan.mappingFor("source", sourceColor.id)!!

        assertEquals(ColorMappingKind.ExactCode, mapping.kind)
        assertEquals(targetColor.id, mapping.targetColor?.id)
        assertEquals(2, plan.impact.rewrittenCells)
        assertEquals(2, plan.impact.exactCodeCells)
    }

    @Test
    fun buildPalettePlan_crossBrandSameDisplayCodeUsesNearestRgb() {
        val targetPalette = BeadPalette.palette(ColorSchemePreset.MardBrand24)
        val targetCodes = targetPalette.mapTo(hashSetOf(), PaletteColor::code)
        val sourceColor = BeadPalette.palette(ColorSchemePreset.Dodo24).first { it.code in targetCodes }
        val layer = StitchLayer(project("dodo", 1, 1, listOf(sourceColor.id), ColorSchemePreset.Dodo24))

        val plan = ProjectStitching.buildPalettePlan(listOf(layer), StitchPalette(ColorSchemePreset.MardBrand24))
        val mapping = plan.mappingFor("dodo", sourceColor.id)!!

        assertEquals(ColorMappingKind.NearestRgb, mapping.kind)
        assertEquals(1, plan.impact.nearestColorCells)
    }

    @Test
    fun buildPalettePlan_crossBrandIdCollisionStillRequiresNearestColorConfirmation() {
        val sourceColor = BeadPalette.palette(ColorSchemePreset.Dodo24).first()
        val collidingTargetColor =
            PaletteColor(
                id = sourceColor.id,
                code = "TARGET-COLOR",
                series = "target",
                argb = 0xFF123456.toInt(),
            )
        val layer = StitchLayer(project("dodo-collision", 1, 1, listOf(sourceColor.id), ColorSchemePreset.Dodo24))
        val target = StitchPalette(ColorSchemePreset.MardBrand24, colors = listOf(collidingTargetColor))

        val plan = ProjectStitching.buildPalettePlan(listOf(layer), target)

        assertEquals(ColorMappingKind.NearestRgb, plan.mappingFor("dodo-collision", sourceColor.id)?.kind)
        assertEquals(1, plan.impact.nearestColorCells)
        assertTrue(plan.requiresConversionConfirmation)
        assertFailure(
            ProjectStitching.compose(listOf(layer), plan),
            StitchComposeFailureReason.ConversionConfirmationRequired,
        )
    }

    @Test
    fun compose_crossBrandConvertsEveryCellToTheChosenConcreteTargetScheme() {
        val mard = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val dodo = BeadPalette.palette(ColorSchemePreset.Dodo24).first()
        val layers =
            ProjectStitching.initialHorizontalLayout(
                listOf(
                    project("mard", 1, 1, listOf(mard.id), ColorSchemePreset.MardBrand48),
                    project("dodo", 1, 1, listOf(dodo.id), ColorSchemePreset.Dodo24),
                ),
            )
        val target = StitchPalette(ColorSchemePreset.MardBrand48)
        val plan = ProjectStitching.buildPalettePlan(layers, target)

        assertEquals(ColorMappingKind.ExactId, plan.mappingFor("mard", mard.id)?.kind)
        assertEquals(ColorMappingKind.NearestRgb, plan.mappingFor("dodo", dodo.id)?.kind)
        assertEquals(1, plan.impact.nearestColorCells)
        assertFailure(
            ProjectStitching.compose(layers, plan),
            StitchComposeFailureReason.ConversionConfirmationRequired,
        )

        val composed =
            ProjectStitching.compose(
                layers = layers,
                plan = plan,
                allowNearestColorMapping = true,
            ) as StitchComposeResult.Success
        val targetIds = target.colors.mapTo(hashSetOf(), PaletteColor::id)
        assertTrue(composed.grid.cells.filterNotNull().all { it in targetIds })
        val stitched = ProjectStitching.buildProject(composed, target, id = "cross-brand")
        assertEquals(ColorSchemePreset.MardBrand48, stitched.colorScheme)
        assertNull(stitched.customColorSchemeId)
    }

    @Test
    fun compose_rejectsCustomTargetThatWasRemovedAfterPlanning() {
        val codes = CustomColorScheme.catalogColors(ColorSchemePreset.MardBrand48).map(PaletteColor::code)
        val custom = customScheme("temporary", "temporary-group", codes.take(2), updatedAt = 1L)
        try {
            BeadPalette.registerCustomColorSchemes(listOf(custom))
            val source = customProject("temporary-project", custom, codes.first())
            val layer = StitchLayer(source)
            val target = StitchPalette(custom.baseScheme, custom.id)
            val plan = ProjectStitching.buildPalettePlan(listOf(layer), target)
            assertFalse(plan.hasUnresolvedColors)

            BeadPalette.registerCustomColorSchemes(emptyList())

            assertFailure(
                ProjectStitching.compose(listOf(layer), plan),
                StitchComposeFailureReason.UnresolvedColors,
            )
        } finally {
            BeadPalette.registerCustomColorSchemes(emptyList())
        }
    }

    @Test
    fun buildPalettePlan_usesNearestRgbWhenTargetHasNoExactCodeAndReportsUnknown() {
        val target = StitchPalette(ColorSchemePreset.MardBrand24)
        val targetCodes = target.colors.mapTo(hashSetOf(), PaletteColor::code)
        val sourceColor = BeadPalette.palette(ColorSchemePreset.MardBrand48).first { it.code !in targetCodes }
        val nearestLayer = StitchLayer(project("nearest", 1, 1, listOf(sourceColor.id), ColorSchemePreset.MardBrand48))

        val nearestPlan = ProjectStitching.buildPalettePlan(listOf(nearestLayer), target)
        assertEquals(ColorMappingKind.NearestRgb, nearestPlan.mappingFor("nearest", sourceColor.id)?.kind)
        assertEquals(1, nearestPlan.impact.nearestColorCells)
        assertTrue(nearestPlan.requiresConversionConfirmation)
        assertFailure(
            ProjectStitching.compose(listOf(nearestLayer), nearestPlan),
            StitchComposeFailureReason.ConversionConfirmationRequired,
        )
        assertTrue(
            ProjectStitching.compose(
                layers = listOf(nearestLayer),
                plan = nearestPlan,
                allowNearestColorMapping = true,
            ) is StitchComposeResult.Success,
        )

        val unknownLayer = StitchLayer(project("unknown", 1, 1, listOf("unknown-color"), ColorSchemePreset.MardBrand24))
        val unknownPlan = ProjectStitching.buildPalettePlan(listOf(unknownLayer), target)
        assertTrue(unknownPlan.hasUnresolvedColors)
        assertEquals(1, unknownPlan.impact.unresolvedCells)
    }

    @Test
    fun overlapDetection_deduplicatesSameMappedColorAndReportsDifferentColors() {
        val a24 = mardColor(ColorSchemePreset.MardBrand24, "A4")
        val a48 = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val different = BeadPalette.palette(ColorSchemePreset.MardBrand48).first { it.code != "A4" }
        val first = StitchLayer(project("first", 1, 1, listOf(a24.id), ColorSchemePreset.MardBrand24))
        val same = StitchLayer(project("same", 1, 1, listOf(a48.id), ColorSchemePreset.MardBrand48))
        val other = StitchLayer(project("other", 1, 1, listOf(different.id), ColorSchemePreset.MardBrand48))
        val target = StitchPalette(ColorSchemePreset.MardBrand48)

        val samePlan = ProjectStitching.buildPalettePlan(listOf(first, same), target)
        assertTrue(ProjectStitching.detectOverlaps(listOf(first, same), samePlan).isEmpty())

        val conflictPlan = ProjectStitching.buildPalettePlan(listOf(first, other), target)
        val conflicts = ProjectStitching.detectOverlaps(listOf(first, other), conflictPlan)
        assertEquals(1, conflicts.size)
        assertEquals(0L, conflicts.single().x)
        assertEquals(0L, conflicts.single().y)
    }

    @Test
    fun compose_rejectsConflictsAndSupportsStableEarlierOrLaterResolution() {
        val firstColor = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val laterColor = BeadPalette.palette(ColorSchemePreset.MardBrand48).first { it.code != "A4" }
        val first = StitchLayer(project("first", 1, 1, listOf(firstColor.id), ColorSchemePreset.MardBrand48))
        val later = StitchLayer(project("later", 1, 1, listOf(laterColor.id), ColorSchemePreset.MardBrand48))
        val target = StitchPalette(ColorSchemePreset.MardBrand48)
        val plan = ProjectStitching.buildPalettePlan(listOf(first, later), target)

        val rejected = ProjectStitching.compose(listOf(first, later), plan)
        assertFailure(rejected, StitchComposeFailureReason.OverlapConflict)

        val keepEarlier = ProjectStitching.compose(listOf(first, later), plan, OverlapPolicy.KeepEarlier) as StitchComposeResult.Success
        val keepLater = ProjectStitching.compose(listOf(first, later), plan, OverlapPolicy.KeepLater) as StitchComposeResult.Success
        assertEquals(firstColor.id, keepEarlier.grid.cells.single())
        assertEquals(laterColor.id, keepLater.grid.cells.single())
    }

    @Test
    fun compose_normalizesNegativePositionsAndBuildsIndependentStitchedProject() {
        val firstColor = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val secondColor = BeadPalette.palette(ColorSchemePreset.MardBrand48).first { it.code != "A4" }
        val firstProject = project("first", 2, 1, listOf(firstColor.id, null), ColorSchemePreset.MardBrand48)
        val secondProject = project("second", 1, 1, listOf(secondColor.id), ColorSchemePreset.MardBrand48)
        val layers = listOf(StitchLayer(firstProject, x = -2, y = -1), StitchLayer(secondProject, x = 0, y = 0))
        val target = StitchPalette(ColorSchemePreset.MardBrand48)
        val plan = ProjectStitching.buildPalettePlan(layers, target)

        val composed = ProjectStitching.compose(layers, plan) as StitchComposeResult.Success

        assertEquals(-2L, composed.originX)
        assertEquals(-1L, composed.originY)
        assertEquals(3, composed.grid.width)
        assertEquals(2, composed.grid.height)
        assertEquals(firstColor.id, composed.grid.cells[0])
        assertEquals(secondColor.id, composed.grid.cells[5])

        val result =
            ProjectStitching.buildProject(
                composed = composed,
                target = target,
                title = "result",
                id = "stitched",
                updatedAt = 123L,
            )
        assertEquals("stitched", result.id)
        assertNotEquals(firstProject.id, result.id)
        assertEquals(ProjectSourceKind.Stitched, result.sourceKind)
        assertNull(result.sourceImagePath)
        assertEquals(listOf(firstColor.id, null), firstProject.cells)
    }

    @Test
    fun sourceRegions_useComposedCoordinatesAndBuildProjectPersistsThem() {
        val color = mardColor(ColorSchemePreset.MardBrand48, "A4")
        val first = StitchLayer(project("first", 2, 2, List(4) { color.id }, ColorSchemePreset.MardBrand48), x = -3, y = 4)
        val second = StitchLayer(project("second", 1, 2, List(2) { color.id }, ColorSchemePreset.MardBrand48), x = -1, y = 4)
        val layers = listOf(first, second)
        val target = StitchPalette(ColorSchemePreset.MardBrand48)
        val composed =
            ProjectStitching.compose(
                layers,
                ProjectStitching.buildPalettePlan(layers, target),
            ) as StitchComposeResult.Success

        val regions = ProjectStitching.sourceRegions(layers, composed)
        val stitched = ProjectStitching.buildProject(composed, target, sourceRegions = regions)

        assertEquals(
            listOf(
                StitchSourceRegion("first", "first", left = 0, top = 0, width = 2, height = 2),
                StitchSourceRegion("second", "second", left = 2, top = 0, width = 1, height = 2),
            ),
            regions,
        )
        assertEquals(regions, stitched.stitchSourceRegions)
    }

    @Test
    fun compose_rejectsStalePlanInsteadOfDroppingUnmappedLayer() {
        val color = mardColor(ColorSchemePreset.MardBrand24, "A4")
        val first = StitchLayer(project("first", 1, 1, listOf(color.id), ColorSchemePreset.MardBrand24))
        val second = StitchLayer(project("second", 1, 1, listOf(color.id), ColorSchemePreset.MardBrand24), x = 1)
        val target = StitchPalette(ColorSchemePreset.MardBrand24)
        val stalePlan = ProjectStitching.buildPalettePlan(listOf(first), target)

        assertFailure(
            ProjectStitching.compose(listOf(first, second), stalePlan),
            StitchComposeFailureReason.UnresolvedColors,
        )
    }

    @Test
    fun compose_validatesGridCellLimitAndExtremeCoordinatesWithLongMath() {
        val color = mardColor(ColorSchemePreset.MardBrand24, "A4")
        val invalid = StitchLayer(project("invalid", 2, 1, listOf(color.id), ColorSchemePreset.MardBrand24))
        val invalidPlan = ProjectStitching.buildPalettePlan(listOf(invalid), StitchPalette(ColorSchemePreset.MardBrand24))
        assertFailure(ProjectStitching.compose(listOf(invalid), invalidPlan), StitchComposeFailureReason.InvalidGrid)

        val first = StitchLayer(project("first", 1, 1, listOf(color.id), ColorSchemePreset.MardBrand24), Int.MIN_VALUE, 0)
        val second = StitchLayer(project("second", 1, 1, listOf(color.id), ColorSchemePreset.MardBrand24), Int.MAX_VALUE, 0)
        val plan = ProjectStitching.buildPalettePlan(listOf(first, second), StitchPalette(ColorSchemePreset.MardBrand24))
        assertFailure(
            ProjectStitching.compose(listOf(first, second), plan),
            StitchComposeFailureReason.CanvasTooLarge,
        )

        val twoByTwo = StitchLayer(project("square", 2, 2, List(4) { color.id }, ColorSchemePreset.MardBrand24))
        val squarePlan = ProjectStitching.buildPalettePlan(listOf(twoByTwo), StitchPalette(ColorSchemePreset.MardBrand24))
        assertTrue(ProjectStitching.compose(listOf(twoByTwo), squarePlan, maxCells = 4L) is StitchComposeResult.Success)
        assertFailure(
            ProjectStitching.compose(listOf(twoByTwo), squarePlan, maxCells = 3L),
            StitchComposeFailureReason.CanvasTooLarge,
        )

        val limitStart = StitchLayer(project("limit-start", 1, 1, listOf(color.id), ColorSchemePreset.MardBrand24))
        val limitEnd =
            StitchLayer(
                project("limit-end", 1, 1, listOf(color.id), ColorSchemePreset.MardBrand24),
                x = 999,
                y = 499,
            )
        val limitPlan =
            ProjectStitching.buildPalettePlan(
                listOf(limitStart, limitEnd),
                StitchPalette(ColorSchemePreset.MardBrand24),
            )
        assertTrue(ProjectStitching.compose(listOf(limitStart, limitEnd), limitPlan) is StitchComposeResult.Success)
        assertFailure(
            ProjectStitching.compose(listOf(limitStart, limitEnd.copy(x = 1_000)), limitPlan),
            StitchComposeFailureReason.CanvasTooLarge,
        )
    }

    private fun assertFailure(
        result: StitchComposeResult,
        reason: StitchComposeFailureReason,
    ) {
        assertTrue(result is StitchComposeResult.Failure)
        assertEquals(reason, (result as StitchComposeResult.Failure).reason)
    }

    private fun mardColor(
        scheme: ColorSchemePreset,
        code: String,
    ): PaletteColor = BeadPalette.palette(scheme).first { it.code == code }

    private fun customScheme(
        id: String,
        groupId: String,
        selectedCodes: List<String>,
        updatedAt: Long,
    ): CustomColorScheme =
        CustomColorScheme(
            id = id,
            name = id,
            baseScheme = ColorSchemePreset.MardBrand48,
            selectedCodes = selectedCodes,
            colorOverrides = emptyMap(),
            createdAt = 0L,
            updatedAt = updatedAt,
            customGroupId = groupId,
            templateSelectedCodes = selectedCodes,
        )

    private fun customProject(
        id: String,
        scheme: CustomColorScheme,
        code: String,
    ): BeadProject =
        project(
            id = id,
            width = 1,
            height = 1,
            cells = listOf(scheme.customColorId(code)),
            scheme = scheme.baseScheme,
            customId = scheme.id,
        )

    private fun project(
        id: String,
        width: Int,
        height: Int,
        cells: List<String?>,
        scheme: ColorSchemePreset = ColorSchemePreset.MardBrand24,
        customId: String? = null,
    ): BeadProject =
        BeadProject(
            id = id,
            title = id,
            width = width,
            height = height,
            cells = cells,
            colorScheme = scheme,
            paletteMode = scheme.paletteMode,
            customColorSchemeId = customId,
            updatedAt = 0L,
        )
}
