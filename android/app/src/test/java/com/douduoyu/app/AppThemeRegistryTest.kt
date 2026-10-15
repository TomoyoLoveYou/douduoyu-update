package com.douduoyu.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class AppThemeRegistryTest {
    @Test
    fun themeIdsAreUniqueAndDefaultIsStable() {
        val options = AppThemeRegistry.options

        assertEquals(options.size, options.map { it.id }.distinct().size)
        assertEquals(1, options.count { it.isDefault })
        assertEquals("soft_lilac", AppThemeRegistry.defaultOption.id)
        assertTrue(options.all { it.label.isNotBlank() })
    }

    @Test
    fun addedThemesAreRegisteredWithReadablePrimaryText() {
        val addedThemeIds = setOf("mint_leaf", "dusty_rose", "sunlit_amber", "sea_glass", "ink_gray")
        val addedThemes = AppThemeRegistry.options.filter { it.id in addedThemeIds }

        assertEquals(addedThemeIds, addedThemes.map { it.id }.toSet())
        addedThemes.forEach { theme ->
            assertTrue(
                "${theme.id} primary contrast was ${contrastRatio(theme.colorScheme.primary, theme.colorScheme.onPrimary)}",
                contrastRatio(theme.colorScheme.primary, theme.colorScheme.onPrimary) >= 4.5,
            )
        }
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearChannel(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) {
                value / 12.92
            } else {
                Math.pow((value + 0.055) / 1.055, 2.4)
            }
        }

        return 0.2126 * linearChannel(color.red) +
            0.7152 * linearChannel(color.green) +
            0.0722 * linearChannel(color.blue)
    }
}
