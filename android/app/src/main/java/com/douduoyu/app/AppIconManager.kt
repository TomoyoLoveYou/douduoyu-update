package com.douduoyu.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    /**
     * 启动时对齐图标选择。
     *
     * 必须**每次都幂等重放**一次对齐：全新安装（或清除数据）后，所有启动入口都处于
     * 「默认启用」状态，`getComponentEnabledSetting` 读不到显式开关，[getCurrentIconId]
     * 会误以为「已经对齐」，于是 30 多个 LAUNCHER 入口全部保持启用——桌面会冒出一堆图标，
     * 系统的任务列表也会认不出该用哪个入口。
     *
     * 这里不重建任务：启动时当前入口就是目标入口，禁用其余入口不会影响当前任务。
     */
    fun reconcileSelection(): String {
        applyIconInternal(getSelectedIconId(), relaunchOnTarget = false)
        return getSelectedIconId()
    }

    /**
     * 切换启动图标。
     *
     * 桌面图标由「处于启用状态的启动入口」决定，所以**必须只留一个**：多留一个就多一个图标。
     * 但直接禁用当前任务所在的入口，系统会把这个任务一起结束（应用直接消失），
     * 因此顺序是：先启用新入口 → 用新入口把任务重建到新组件上 → 最后才禁用旧入口。
     * 副作用是切换后应用会回到首页重新加载（任务被重建），这是这个机制绕不开的代价。
     */
    fun applyIcon(iconId: String): Boolean = applyIconInternal(iconId, relaunchOnTarget = true)

    private fun applyIconInternal(
        iconId: String,
        relaunchOnTarget: Boolean,
    ): Boolean {
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
            // 先把任务切换到新入口，再禁用旧入口；反过来的话当前任务会先被系统结束
            if (relaunchOnTarget) {
                relaunchOn(targetComponent)
            }
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

    /** 清掉当前任务，用指定的启动入口重新开一个——任务根组件必须始终是启用的那个入口。 */
    private fun relaunchOn(component: ComponentName) {
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setComponent(component)
                // 必须另起一个**新**任务：CLEAR_TASK 会沿用原任务的记录，
                // 随后禁用旧入口时新活动会跟着旧任务一起被系统清掉。
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
        runCatching { appContext.startActivity(intent) }
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
