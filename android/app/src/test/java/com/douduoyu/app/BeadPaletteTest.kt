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
                "tongqu",
                "xiaowu",
                "manman",
                "panpan",
                "mixiaowo",
                "coco",
                "dodo",
                "artkal",
                "huangdoudou",
                "shishi",
                "kaka",
            )
        val expectedConfidences =
            mapOf(
                "mard" to 0.9704,
                "xiaowu" to 0.98,
                "tongqu" to 0.98,
                "manman" to 0.9387,
                "panpan" to 0.9617,
                "huangdoudou" to 0.8313,
                "mixiaowo" to 0.9478,
                "artkal" to 0.8143,
                "dodo" to 0.8491,
                "kaka" to 0.7137,
                "shishi" to 0.7778,
                "coco" to 0.8833,
            )
        val expectedVariants =
            mapOf(
                "huangdoudou" to listOf("24", "48", "72", "96", "120", "144", "168", "291"),
                "kaka" to listOf("284", "286"),
                "manman" to listOf("24", "48", "72", "96", "120", "144", "168", "192", "216", "278", "290"),
                "mixiaowo" to listOf("24", "48", "72", "96", "120", "144", "168", "192", "230", "290", "291", "292"),
                "panpan" to listOf("24", "48", "72", "96", "120", "144", "168", "192", "289", "290", "291"),
                "shishi" to listOf("217", "220"),
                "tongqu" to listOf("120"),
                "xiaowu" to listOf("290", "291"),
                "artkal" to listOf("197", "221", "418", "174", "418-New"),
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
    fun artkalSchemes_exposeSeriesAndPutOfficialCurrentPalettesFirst() {
        val schemes = ColorSchemePreset.visibleSchemesForBrand("artkal")

        assertEquals(
            listOf(
                "C \u7cfb\u5217 197\u8272",
                "M \u7cfb\u5217 221\u8272",
                "C+M \u5168\u91cf 418\u8272",
                "C \u7cfb\u5217 174\u8272\uff08\u65e7\u8868\uff09",
                "418-New 418\u8272\uff08\u7b2c\u4e09\u65b9\u8868\uff09",
            ),
            schemes.map { it.variantLabel },
        )
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
    fun confidenceData_matchesSynchronizedReferenceDimensions() {
        assertEquals(12, ColorConfidenceData.referenceBrandCount)
        assertEquals(88, ColorConfidenceData.referenceSchemeCount)
        assertEquals(14_781, ColorConfidenceData.referenceColorEntryCount)
        assertEquals(
            ColorSchemePreset.visibleBrands.map { it.id },
            ColorConfidenceData.brands.map { it.brand.id },
        )

        ColorConfidenceData.brands.forEach { brandMatrix ->
            assertEquals(brandMatrix.brand.confidenceScore, brandMatrix.scores.overall, 0.0)
            assertEquals(
                ColorSchemePreset.visibleSchemesForBrand(brandMatrix.brand.id).map { it.variantKey }.toSet(),
                brandMatrix.schemeScores.keys,
            )
            brandMatrix.schemeScores.values.forEach { scores ->
                assertTrue(scores.overall in 0.0..1.0)
                assertTrue(scores.colorData in 0.0..1.0)
                assertTrue(scores.schemeIntegrity in 0.0..1.0)
            }
        }

        val mixiaowo230 =
            ColorConfidenceData.brands
                .first { it.brand.id == "mixiaowo" }
                .schemeScores
                .getValue("230")
        assertEquals(0.8778, mixiaowo230.overall, 0.0)
        assertEquals(0.8816, mixiaowo230.colorData, 0.0)
        assertEquals(0.8626, mixiaowo230.schemeIntegrity, 0.0)
    }

    @Test
    fun storedCodes_areNormalized_forSchemeScopedPalettes() {
        assertEquals("Coco24:A01", BeadPalette.normalizeStoredCode("A01", ColorSchemePreset.Coco24))
        assertEquals("Coco24:A01", BeadPalette.normalizeStoredCode("coco:A01", ColorSchemePreset.Coco24))
        assertEquals("MardBrand24:A4", BeadPalette.normalizeStoredCode("A4", ColorSchemePreset.MardBrand24))
        assertEquals("A4", BeadPalette.normalizeStoredCode("A4", ColorSchemePreset.Mard24))
    }

    @Test
    fun hexToArgb_supportsOpaqueRgbAndReferenceRgba() {
        assertEquals(0xFFC6C6D2.toInt(), BeadPalette.hexToArgb("#C6C6D2"))
        assertEquals(0x80C6C6D2.toInt(), BeadPalette.hexToArgb("#C6C6D280"))
        assertEquals(0x00FFFFFF, BeadPalette.hexToArgb("#FFFFFF00"))
    }

    @Test
    fun nearest_matchesReferenceWeightedDistance() {
        ColorSchemePreset.entries
            .filter { it.showInLogicPanel }
            .forEach { scheme ->
                val palette = BeadPalette.palette(scheme)
                for (red in 0..255 step 51) {
                    for (green in 0..255 step 51) {
                        for (blue in 0..255 step 51) {
                            val expected = referenceNearest(red, green, blue, palette)
                            assertEquals(expected.id, BeadPalette.nearest(red, green, blue, palette).id)
                        }
                    }
                }
            }
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

    private fun referenceNearest(
        red: Int,
        green: Int,
        blue: Int,
        palette: List<PaletteColor>,
    ): PaletteColor {
        var best = palette.first()
        var bestDistance = Double.POSITIVE_INFINITY
        palette.forEach { color ->
            val redMean = (red + color.red) / 2.0
            val deltaRed = (red - color.red).toDouble()
            val deltaGreen = (green - color.green).toDouble()
            val deltaBlue = (blue - color.blue).toDouble()
            val distance =
                kotlin.math.sqrt(
                    (2 + redMean / 256.0) * deltaRed * deltaRed +
                        4 * deltaGreen * deltaGreen +
                        (2 + (255 - redMean) / 256.0) * deltaBlue * deltaBlue,
                )
            if (distance < bestDistance) {
                bestDistance = distance
                best = color
            }
        }
        return best
    }
}
