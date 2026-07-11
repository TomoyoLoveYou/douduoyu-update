package com.douduoyu.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

internal class AppIconManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedIconId(): String {
        val storedId = preferences.getString(KEY_SELECTED_ICON_ID, null)
        return AppIconRegistry.options.firstOrNull { it.id == storedId }?.id ?: AppIconRegistry.defaultOption.id
    }

    fun getCurrentIconId(): String {
        val enabledOptions = AppIconRegistry.options.filter(::isLauncherEnabled)
        if (enabledOptions.isEmpty()) {
            return AppIconRegistry.defaultOption.id
        }
        val storedId = getSelectedIconId()
        return enabledOptions.firstOrNull { it.id == storedId }?.id ?: enabledOptions.first().id
    }

    fun reconcileSelection(): String {
        val targetId = getSelectedIconId()
        ensureStableLaunchersEnabled()
        if (getCurrentIconId() != targetId) {
            applyIcon(targetId)
        } else {
            preferences.edit().putString(KEY_SELECTED_ICON_ID, targetId).apply()
        }
        return getSelectedIconId()
    }

    fun applyIcon(iconId: String): Boolean {
        val targetOption = AppIconRegistry.options.firstOrNull { it.id == iconId } ?: AppIconRegistry.defaultOption
        return runCatching {
            val packageManager = appContext.packageManager
            ensureStableLaunchersEnabled(packageManager)
            val targetComponent = componentNameFor(targetOption)
            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            AppIconRegistry.options
                .filterNot { it.id == targetOption.id }
                .forEach { option ->
                    packageManager.setComponentEnabledSetting(
                        componentNameFor(option),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            preferences.edit().putString(KEY_SELECTED_ICON_ID, targetOption.id).apply()
        }.isSuccess
    }

    private fun componentNameFor(option: AppIconOption): ComponentName {
        return ComponentName(appContext, option.componentClassName)
    }

    private fun stableLauncherComponentNames(): List<ComponentName> {
        return STABLE_LAUNCHER_COMPONENTS.map { className -> ComponentName(appContext, className) }
    }

    private fun ensureStableLaunchersEnabled(packageManager: PackageManager = appContext.packageManager) {
        stableLauncherComponentNames().forEach { componentName ->
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private fun isLauncherEnabled(option: AppIconOption): Boolean {
        val state = appContext.packageManager.getComponentEnabledSetting(componentNameFor(option))
        return when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
            -> false
            else -> option.isDefault
        }
    }

    private companion object {
        const val PREFS_NAME = "douduoyu-app-icons"
        const val KEY_SELECTED_ICON_ID = "selected_icon_id"
        val STABLE_LAUNCHER_COMPONENTS =
            listOf(
                "com.douduoyu.app.MainActivityDefaultLauncher",
                "com.douduoyu.app.MainActivityDefaultHomeLauncher",
            )
    }
}
