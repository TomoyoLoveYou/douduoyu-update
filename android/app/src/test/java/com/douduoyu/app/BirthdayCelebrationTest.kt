package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayCelebrationTest {
    @Test
    fun passcode_ignoresOuterWhitespaceAndAsciiCase() {
        assertEquals("xiaoyu250", BirthdayCelebration.normalizePasscode("  XiaoYu250\n"))
        assertTrue(BirthdayCelebration.matchesPasscode("xiaoyu250"))
        assertTrue(BirthdayCelebration.matchesPasscode("  XIAOYU250  "))
        assertFalse(BirthdayCelebration.matchesPasscode("xiaoyu251"))
        assertFalse(BirthdayCelebration.matchesPasscode(""))
    }

    @Test
    fun config_containsRequestedBirthdayGift() {
        assertEquals("\u5c0f\u9c7c", BirthdayCelebration.RECIPIENT_NAME)
        assertEquals("\u5c0f\u9c7c\u751f\u65e5\u5feb\u4e50", BirthdayCelebration.GREETING)
        assertEquals("\u5c0f\u9c7c\u7684\u751f\u65e5\u86cb\u7cd5", BirthdayCelebration.PROJECT_TITLE)
        assertEquals(BirthdayCelebrationTemplate.BirthdayCake, BirthdayCelebration.template)
        assertEquals(BirthdayCelebration.GIFT_ID, BirthdayCelebration.config.id)
        assertEquals(BirthdayCelebration.RECIPIENT_NAME, BirthdayCelebration.config.nickname)
        assertEquals(BirthdayCelebration.GREETING, BirthdayCelebration.config.message)
        assertEquals(BirthdayCelebration.PROJECT_TITLE, BirthdayCelebration.config.projectTitle)
    }

    @Test
    fun cakeProject_isEditableAndUsesOnlyValidSchemeColors() {
        val updatedAt = 123_456L
        val project = BirthdayCelebration.createCakeProject(updatedAt)
        val paletteIds = BeadPalette.palette(project.colorScheme).mapTo(hashSetOf()) { it.id }

        assertEquals(BirthdayCelebration.PROJECT_TITLE, project.title)
        assertEquals(40, project.width)
        assertEquals(40, project.height)
        assertEquals(40 * 40, project.cells.size)
        assertEquals(ColorSchemePreset.MardBrand221, project.colorScheme)
        assertEquals(ColorSchemePreset.MardBrand221.paletteMode, project.paletteMode)
        assertEquals(ProjectSourceKind.Celebration, project.sourceKind)
        assertEquals(updatedAt, project.updatedAt)
        assertNull(project.sourceImagePath)
        assertTrue(project.cells.any { it == null })
        assertEquals(1_134, project.cells.count { it != null })
        assertTrue(project.cells.filterNotNull().toSet().size >= 11)
        assertTrue(project.cells.filterNotNull().all(paletteIds::contains))
    }

    @Test
    fun cakeProject_matchesFortyPixelPosterSprite() {
        val project = BirthdayCelebration.createCakeProject(0L)
        val palette = BeadPalette.palette(project.colorScheme)
        val outline =
            BeadPalette.nearest(
                red = 253,
                green = 192,
                blue = 208,
                candidates = palette,
            ).id
        val darkOutline =
            BeadPalette.nearest(
                red = 193,
                green = 68,
                blue = 74,
                candidates = palette,
            ).id

        fun cell(x: Int, y: Int): String? = project.cells[y * project.width + x]
        fun filledCount(y: Int): Int = (0 until project.width).count { x -> cell(x, y) != null }

        assertEquals(6, filledCount(0))
        assertEquals(listOf(13, 19, 25), BirthdayCelebration.candleColumns.toList())
        assertEquals(42, (0..3).sumOf(::filledCount))
        BirthdayCelebration.candleColumns.forEach { x ->
            assertEquals(outline, cell(x, 0))
            assertTrue(cell(x, 10) != null)
            assertEquals(
                14,
                (0..3).sumOf { y ->
                    ((x - 1)..(x + 2)).count { column -> cell(column, y) != null }
                },
            )
        }

        assertEquals(28, filledCount(11))
        assertEquals(38, filledCount(24))
        assertEquals(40, filledCount(25))
        assertEquals(36, filledCount(39))
        assertTrue(filledCount(25) > filledCount(11))
        assertEquals(outline, cell(0, 25))
        assertEquals(outline, cell(39, 25))
        assertEquals(darkOutline, cell(7, 18))
        assertNotEquals(outline, darkOutline)

        for (y in 0 until project.height) {
            for (x in 0 until project.width) {
                assertEquals(cell(x, y) != null, cell(project.width - 1 - x, y) != null)
            }
        }
    }

    @Test
    fun cakeProjectFactory_reusesGiftIdAndCreatesIndependentCellLists() {
        val first = BirthdayCelebration.createCakeProject(1L)
        val second = BirthdayCelebration.createCakeProject(2L)

        assertEquals(BirthdayCelebration.GIFT_ID, first.id)
        assertEquals(first.id, second.id)
        assertNotEquals(first.updatedAt, second.updatedAt)
        assertEquals(first.cells, second.cells)
        assertTrue(first.cells !== second.cells)
    }
}
