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
