package com.douduoyu.app

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal data class AppThemeOption(
    val id: String,
    val label: String,
    val previewColor: Color,
    val colorScheme: ColorScheme,
    val isDefault: Boolean = false,
)

internal object AppThemeRegistry {
    val options: List<AppThemeOption> =
        listOf(
            AppThemeOption(
                id = "soft_lilac",
                label = "淡紫",
                previewColor = Color(0xFFB89AE8),
                isDefault = true,
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF8C6BC2),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFE7DDF8),
                        onPrimaryContainer = Color(0xFF30194D),
                        secondary = Color(0xFFCA93C7),
                        onSecondary = Color(0xFF3F203E),
                        secondaryContainer = Color(0xFFF5E2F4),
                        onSecondaryContainer = Color(0xFF4B2749),
                        tertiary = Color(0xFF89A7E0),
                        onTertiary = Color.White,
                        background = Color(0xFFF8F3FC),
                        surface = Color(0xFFFFFBFF),
                        surfaceVariant = Color(0xFFF1EAF8),
                        outlineVariant = Color(0xFFDCCFEA),
                    ),
            ),
            AppThemeOption(
                id = "mist_blue",
                label = "雾蓝",
                previewColor = Color(0xFF7EA8D8),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF5E86B8),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFDCE9F7),
                        onPrimaryContainer = Color(0xFF17304D),
                        secondary = Color(0xFF88A7B6),
                        onSecondary = Color(0xFF1E333D),
                        secondaryContainer = Color(0xFFDDEDF2),
                        onSecondaryContainer = Color(0xFF26414C),
                        tertiary = Color(0xFFB5A0D9),
                        onTertiary = Color.White,
                        background = Color(0xFFF4F7FB),
                        surface = Color(0xFFFCFCFF),
                        surfaceVariant = Color(0xFFEAF0F7),
                        outlineVariant = Color(0xFFD2DDEB),
                    ),
            ),
            AppThemeOption(
                id = "peach_cream",
                label = "蜜桃",
                previewColor = Color(0xFFE8A07D),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFFD88666),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFF8E0D4),
                        onPrimaryContainer = Color(0xFF4E2414),
                        secondary = Color(0xFFE4B072),
                        onSecondary = Color(0xFF432A10),
                        secondaryContainer = Color(0xFFF9E8CF),
                        onSecondaryContainer = Color(0xFF513518),
                        tertiary = Color(0xFFC78FB2),
                        onTertiary = Color.White,
                        background = Color(0xFFFFF6F1),
                        surface = Color(0xFFFFFBF8),
                        surfaceVariant = Color(0xFFF6E8E1),
                        outlineVariant = Color(0xFFE7D1C6),
                    ),
            ),
            AppThemeOption(
                id = "mint_leaf",
                label = "薄荷",
                previewColor = Color(0xFF5C9D89),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF397565),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFC4EBDD),
                        onPrimaryContainer = Color(0xFF12372E),
                        secondary = Color(0xFF6E6F43),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFE2E5BB),
                        onSecondaryContainer = Color(0xFF282B08),
                        tertiary = Color(0xFF76546D),
                        onTertiary = Color.White,
                        tertiaryContainer = Color(0xFFF0D9EA),
                        onTertiaryContainer = Color(0xFF2E1728),
                        background = Color(0xFFF5FAF7),
                        onBackground = Color(0xFF181D1B),
                        surface = Color(0xFFFBFDFB),
                        onSurface = Color(0xFF181D1B),
                        surfaceVariant = Color(0xFFDFE7E2),
                        onSurfaceVariant = Color(0xFF414946),
                        outline = Color(0xFF717A75),
                        outlineVariant = Color(0xFFC0C9C3),
                        inverseSurface = Color(0xFF2D322F),
                        inverseOnSurface = Color(0xFFEFF1EF),
                        inversePrimary = Color(0xFFA8D4C5),
                    ),
            ),
            AppThemeOption(
                id = "dusty_rose",
                label = "樱粉",
                previewColor = Color(0xFFC86C8B),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFFA44F6D),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFFFD9E3),
                        onPrimaryContainer = Color(0xFF3F001D),
                        secondary = Color(0xFF77565F),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFF0DDE1),
                        onSecondaryContainer = Color(0xFF2C151B),
                        tertiary = Color(0xFF7D5636),
                        onTertiary = Color.White,
                        tertiaryContainer = Color(0xFFFFDCC2),
                        onTertiaryContainer = Color(0xFF2E1503),
                        background = Color(0xFFFFF7F8),
                        onBackground = Color(0xFF211A1C),
                        surface = Color(0xFFFFF8F8),
                        onSurface = Color(0xFF211A1C),
                        surfaceVariant = Color(0xFFF2DEE3),
                        onSurfaceVariant = Color(0xFF514347),
                        outline = Color(0xFF837378),
                        outlineVariant = Color(0xFFD5C2C7),
                        inverseSurface = Color(0xFF362F31),
                        inverseOnSurface = Color(0xFFFFECF0),
                        inversePrimary = Color(0xFFFFB0C8),
                    ),
            ),
            AppThemeOption(
                id = "sunlit_amber",
                label = "琥珀",
                previewColor = Color(0xFFB98714),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF835D00),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFFFE09A),
                        onPrimaryContainer = Color(0xFF291800),
                        secondary = Color(0xFF705D3A),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFF7DFB4),
                        onSecondaryContainer = Color(0xFF281A05),
                        tertiary = Color(0xFF4E6547),
                        onTertiary = Color.White,
                        tertiaryContainer = Color(0xFFD0E9C5),
                        onTertiaryContainer = Color(0xFF0B2008),
                        background = Color(0xFFFFF9F0),
                        onBackground = Color(0xFF1F1B13),
                        surface = Color(0xFFFFF9F2),
                        onSurface = Color(0xFF1F1B13),
                        surfaceVariant = Color(0xFFEEE3D0),
                        onSurfaceVariant = Color(0xFF4D4639),
                        outline = Color(0xFF7E7668),
                        outlineVariant = Color(0xFFD1C7B6),
                        inverseSurface = Color(0xFF343027),
                        inverseOnSurface = Color(0xFFF8F0E5),
                        inversePrimary = Color(0xFFF7C451),
                    ),
            ),
            AppThemeOption(
                id = "sea_glass",
                label = "海盐",
                previewColor = Color(0xFF2C918C),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF006A67),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFF9CF2ED),
                        onPrimaryContainer = Color(0xFF00201F),
                        secondary = Color(0xFF4A6361),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFCDE8E5),
                        onSecondaryContainer = Color(0xFF051F1E),
                        tertiary = Color(0xFF4B607C),
                        onTertiary = Color.White,
                        tertiaryContainer = Color(0xFFD3E4FF),
                        onTertiaryContainer = Color(0xFF041C35),
                        background = Color(0xFFF3FBFA),
                        onBackground = Color(0xFF161D1C),
                        surface = Color(0xFFF8FCFB),
                        onSurface = Color(0xFF161D1C),
                        surfaceVariant = Color(0xFFDAE5E3),
                        onSurfaceVariant = Color(0xFF3F4948),
                        outline = Color(0xFF6F7977),
                        outlineVariant = Color(0xFFBEC9C7),
                        inverseSurface = Color(0xFF2B3231),
                        inverseOnSurface = Color(0xFFECF2F1),
                        inversePrimary = Color(0xFF80D5D0),
                    ),
            ),
            AppThemeOption(
                id = "ink_gray",
                label = "墨灰",
                previewColor = Color(0xFF687F8E),
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF475D6B),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFCBE7F6),
                        onPrimaryContainer = Color(0xFF041E29),
                        secondary = Color(0xFF5D5F62),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFE2E2E6),
                        onSecondaryContainer = Color(0xFF1A1C1E),
                        tertiary = Color(0xFF76546D),
                        onTertiary = Color.White,
                        tertiaryContainer = Color(0xFFF0D9EA),
                        onTertiaryContainer = Color(0xFF2E1728),
                        background = Color(0xFFF6F9FA),
                        onBackground = Color(0xFF191C1E),
                        surface = Color(0xFFFAFCFD),
                        onSurface = Color(0xFF191C1E),
                        surfaceVariant = Color(0xFFDFE3E6),
                        onSurfaceVariant = Color(0xFF43474A),
                        outline = Color(0xFF74787B),
                        outlineVariant = Color(0xFFC3C7CA),
                        inverseSurface = Color(0xFF2E3133),
                        inverseOnSurface = Color(0xFFF0F1F3),
                        inversePrimary = Color(0xFFAFCBDA),
                    ),
            ),
        )

    val defaultOption: AppThemeOption
        get() = options.first { it.isDefault }
}

internal class ThemeManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedThemeId(): String {
        val storedId = preferences.getString(KEY_SELECTED_THEME_ID, null)
        return AppThemeRegistry.options.firstOrNull { it.id == storedId }?.id ?: AppThemeRegistry.defaultOption.id
    }

    fun applyTheme(themeId: String): String {
        val targetOption = AppThemeRegistry.options.firstOrNull { it.id == themeId } ?: AppThemeRegistry.defaultOption
        preferences.edit().putString(KEY_SELECTED_THEME_ID, targetOption.id).apply()
        return targetOption.id
    }

    private companion object {
        const val PREFS_NAME = "douduoyu-theme"
        const val KEY_SELECTED_THEME_ID = "selected_theme_id"
    }
}
