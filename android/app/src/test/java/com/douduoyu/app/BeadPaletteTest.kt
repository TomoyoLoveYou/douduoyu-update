package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeadPaletteTest {
    @Test
    fun coco24Palette_matchesFullReferenceCodes() {
        val expected =
            listOf(
                "A01", "A02", "B03", "B05", "B06", "B09", "C07", "D05", "D08", "E07", "F04", "F10",
                "F11", "H04", "H07", "H14", "J04", "J11", "J15", "K15", "K21", "Z02", "Z10", "Z18",
            )

        assertEquals(expected, BeadPalette.palette(ColorSchemePreset.Coco24).map { it.code })
    }

    @Test
    fun visibleBrandCatalog_matchesFullReferenceCatalog() {
        val expectedBrands =
            listOf(
                "mard",
                "xiaowu",
                "tongqu",
                "manman",
                "panpan",
                "huangdoudou",
                "mixiaowo",
                "artkal",
                "dodo",
                "kaka",
                "shishi",
                "coco",
            )
        val expectedConfidences =
            mapOf(
                "mard" to 0.9217,
                "xiaowu" to 0.975,
                "tongqu" to 0.97,
                "manman" to 0.9009,
                "panpan" to 0.8168,
                "huangdoudou" to 0.7472,
                "mixiaowo" to 0.7342,
                "artkal" to 0.6853,
                "dodo" to 0.6779,
                "kaka" to 0.6702,
                "shishi" to 0.6497,
                "coco" to 0.565,
            )
        val expectedVariants =
            mapOf(
                "huangdoudou" to listOf("24", "48", "72", "96", "120", "144", "168", "291"),
                "kaka" to listOf("284", "286"),
                "manman" to listOf("216", "278", "290"),
                "mixiaowo" to listOf("230", "290", "291", "292"),
                "panpan" to listOf("289", "290", "291"),
                "shishi" to listOf("217", "220"),
                "tongqu" to listOf("120"),
                "xiaowu" to listOf("290", "291"),
                "artkal" to listOf("174", "197", "221", "418", "418-New"),
                "coco" to listOf("24", "48", "72", "96", "120", "144", "168", "192", "221", "291", "293"),
                "dodo" to listOf("24", "48", "72", "96", "120", "144", "168", "192", "216", "288", "290", "291"),
                "mard" to listOf("24", "48", "72", "96", "120", "144", "168", "221", "264", "291", "295"),
            )

        assertEquals(expectedBrands, ColorSchemePreset.visibleBrands.map { it.id })
        expectedConfidences.forEach { (brandId, score) ->
            assertEquals(score, ColorSchemePreset.visibleBrands.first { it.id == brandId }.confidenceScore, 0.0)
        }
        expectedVariants.forEach { (brandId, variantKeys) ->
            assertEquals(variantKeys, ColorSchemePreset.visibleSchemesForBrand(brandId).map { it.variantKey })
        }
    }

    @Test
    fun fullReferenceSchemeSizes_matchExpectedCounts() {
        ColorSchemePreset.entries
            .filter { it.showInLogicPanel }
            .forEach { scheme ->
                val palette = BeadPalette.palette(scheme)
                assertEquals(scheme.maxColors, palette.size)
                assertEquals(scheme.maxColors, palette.map { it.id }.distinct().size)
                assertTrue(palette.all { it.id.startsWith("${scheme.name}:") && it.code.isNotBlank() })
            }
    }

    @Test
    fun storedCodes_areNormalized_forSchemeScopedPalettes() {
        assertEquals("Coco24:A01", BeadPalette.normalizeStoredCode("A01", ColorSchemePreset.Coco24))
        assertEquals("Coco24:A01", BeadPalette.normalizeStoredCode("coco:A01", ColorSchemePreset.Coco24))
        assertEquals("MardBrand24:A4", BeadPalette.normalizeStoredCode("A4", ColorSchemePreset.MardBrand24))
        assertEquals("A4", BeadPalette.normalizeStoredCode("A4", ColorSchemePreset.Mard24))
    }

    @Test
    fun defaultScheme_isMard221() {
        assertEquals(ColorSchemePreset.MardBrand221, GenerationSettings().colorScheme)
        assertEquals(ColorSchemePreset.MardBrand221, BeadProject(title = "test", width = 1, height = 1, cells = listOf(null), updatedAt = 0L).colorScheme)
    }

    @Test
    fun legacyMard24Palette_stillAvailableForExistingProjects() {
        val expected =
            listOf(
                "B3", "C3", "D9", "E2", "G1", "A4", "B5", "C5", "D6", "E4", "G5", "A6",
                "B8", "C8", "D7", "F5", "G7", "A7", "H1", "H2", "H3", "H4", "H5", "H7",
            )

        assertEquals(expected, BeadPalette.palette(ColorSchemePreset.Mard24).map { it.code })
    }
}
