package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomColorSchemeTest {
    @Test
    fun create_usesBasePaletteAsTemplate() {
        val scheme = CustomColorScheme.create(ColorSchemePreset.Coco24, "  My colors  ", now = 123L)

        assertEquals("My colors", scheme.name)
        assertEquals(ColorSchemePreset.Coco24, scheme.baseScheme)
        assertEquals(BeadPalette.palette(ColorSchemePreset.Coco24).map { it.code }, scheme.selectedCodes)
        assertEquals(scheme.id, scheme.customGroupId)
        assertEquals(scheme.selectedCodes, scheme.templateSelectedCodes)
        assertEquals(123L, scheme.createdAt)
        assertEquals(123L, scheme.updatedAt)
        assertNotNull(scheme.normalizedOrNull())
    }

    @Test
    fun catalog_usesLargestSchemeOrderAndBaseColorsForDuplicateCodes() {
        val base = ColorSchemePreset.MardBrand24
        val largest =
            ColorSchemePreset.entries
                .filter { it.showInLogicPanel && it.brandId == base.brandId }
                .maxBy { it.maxColors }
        val catalog = CustomColorScheme.catalogColors(base)
        val largestPalette = BeadPalette.palette(largest)
        val basePalette = BeadPalette.palette(base)
        val sharedColor = basePalette.first { baseColor -> largestPalette.any { it.code == baseColor.code } }

        assertEquals(largestPalette.first().code, catalog.first().code)
        assertEquals(sharedColor, catalog.first { it.code == sharedColor.code })
        assertEquals(catalog.size, catalog.map { it.code }.distinct().size)
        assertTrue(catalog.size >= largestPalette.size)
    }

    @Test
    fun normalized_filtersUnknownCodesAndForcesOverrideAlpha() {
        val base = ColorSchemePreset.Coco24
        val catalogCodes = CustomColorScheme.catalogColors(base).map { it.code }
        val first = catalogCodes[0]
        val second = catalogCodes[1]
        val normalized =
            CustomColorScheme(
                id = "  scheme-id  ",
                name = "  Scheme  ",
                baseScheme = base,
                selectedCodes = listOf(first, "missing", first, second),
                colorOverrides = linkedMapOf(first to 0x00112233, "missing" to 0x00123456),
                createdAt = -10L,
                updatedAt = -20L,
                archived = true,
            ).normalizedOrNull()

        requireNotNull(normalized)
        assertEquals("scheme-id", normalized.id)
        assertEquals("scheme-id", normalized.customGroupId)
        assertEquals("Scheme", normalized.name)
        assertEquals(listOf(first, second), normalized.selectedCodes)
        assertEquals(listOf(first, second), normalized.templateSelectedCodes)
        assertEquals(mapOf(first to 0xFF112233.toInt()), normalized.colorOverrides)
        assertEquals(0L, normalized.createdAt)
        assertEquals(0L, normalized.updatedAt)
        assertTrue(normalized.archived)
    }

    @Test
    fun normalized_rejectsFewerThanTwoSelectedColors() {
        val base = ColorSchemePreset.Coco24
        val onlyCode = CustomColorScheme.templateCodes(base).first()

        assertNull(
            CustomColorScheme(
                id = "id",
                name = "name",
                baseScheme = base,
                selectedCodes = listOf(onlyCode),
                colorOverrides = emptyMap(),
                createdAt = 1L,
                updatedAt = 1L,
            ).normalizedOrNull(),
        )
    }

    @Test
    fun palette_appliesOverridesAndCanIncludeDisabledColors() {
        val base = ColorSchemePreset.Coco24
        val catalog = CustomColorScheme.catalogColors(base)
        val enabledCodes = catalog.take(2).map { it.code }
        val scheme =
            CustomColorScheme(
                id = "scheme-id",
                name = "name",
                baseScheme = base,
                selectedCodes = enabledCodes,
                colorOverrides = mapOf(enabledCodes.first() to 0x00123456),
                createdAt = 1L,
                updatedAt = 1L,
            )

        val enabledPalette = scheme.palette()
        val fullPalette = scheme.palette(includeDisabled = true)

        assertEquals(enabledCodes, enabledPalette.map { it.code })
        assertEquals(catalog.size, fullPalette.size)
        assertEquals(0xFF123456.toInt(), enabledPalette.first().argb)
        assertEquals("custom:scheme-id:${enabledCodes.first()}", enabledPalette.first().id)
        assertTrue(enabledPalette.all { it.id.startsWith(CustomColorScheme.COLOR_ID_PREFIX) })
        assertFalse(fullPalette.drop(enabledPalette.size).isEmpty())
    }

    @Test
    fun registry_resolvesCustomPaletteAndOverriddenColors() {
        val base = ColorSchemePreset.Coco24
        val template = CustomColorScheme.create(base, "custom", now = 1L)
        val firstCode = template.selectedCodes.first()
        val scheme = template.copy(colorOverrides = mapOf(firstCode to 0xFF123456.toInt()))

        try {
            BeadPalette.registerCustomColorSchemes(listOf(scheme))

            val palette = BeadPalette.palette(base, scheme.id)
            assertEquals(scheme.selectedCodes.size, palette.size)
            assertEquals(0xFF123456.toInt(), BeadPalette.find(scheme.customColorId(firstCode))?.argb)
            assertEquals("custom", BeadPalette.schemeLabel(base, scheme.id))
        } finally {
            BeadPalette.registerCustomColorSchemes(emptyList())
        }
    }

    @Test
    fun archivedScheme_remainsResolvableForExistingProjects() {
        val base = ColorSchemePreset.Coco24
        val scheme = CustomColorScheme.create(base, "archived", now = 1L).copy(archived = true)

        try {
            BeadPalette.registerCustomColorSchemes(listOf(scheme))

            assertEquals(scheme.selectedCodes.size, BeadPalette.palette(base, scheme.id).size)
            assertNotNull(BeadPalette.find(scheme.customColorId(scheme.selectedCodes.first())))
        } finally {
            BeadPalette.registerCustomColorSchemes(emptyList())
        }
    }

    @Test
    fun customGroupNames_rejectBuiltInsAndGenerateUniqueDefaults() {
        assertTrue(isBuiltInColorSchemeName(" mArD "))
        assertTrue(isBuiltInColorSchemeName(ColorSchemePreset.Coco24.label))
        assertFalse(isBuiltInColorSchemeName("Mard 自定义"))

        val existing = CustomColorScheme.create(ColorSchemePreset.MardBrand24, "Mard 自定义", now = 1L)
        assertEquals(
            "Mard 自定义 2",
            nextAvailableCustomGroupName("Mard 自定义", listOf(existing)),
        )
    }
}
