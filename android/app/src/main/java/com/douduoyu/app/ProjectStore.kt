package com.douduoyu.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = context.getSharedPreferences("douduoyu-projects", Context.MODE_PRIVATE)
    private val sourceImageDirectory by lazy {
        File(appContext.filesDir, "project-sources").apply { mkdirs() }
    }

    fun loadProjects(): List<BeadProject> {
        val raw = preferences.getString(KEY, null)?.takeIf { it.isNotBlank() }
        if (raw == null) {
            val seed = syncDemoProject(emptyList())
            saveProjects(seed)
            return seed
        }

        val parsed =
            runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        add(readProject(array.getJSONObject(index)))
                    }
                }.sortedByDescending { it.updatedAt }
            }.getOrElse {
                val seed = syncDemoProject(emptyList())
                saveProjects(seed)
                return seed
            }
        val synced = syncDemoProject(parsed)
        if (synced != parsed) {
            saveProjects(synced)
        }
        return synced
    }

    internal fun syncDemoProject(
        projects: List<BeadProject>,
        iconOption: AppIconOption = currentAppIconOption(),
    ): List<BeadProject> {
        val existingDemo = projects.firstOrNull { it.id == PatternEngine.DEMO_PROJECT_ID }
        val previousIconId = preferences.getString(DEMO_ICON_ID_KEY, null)
        val previousIconOption = AppIconRegistry.options.firstOrNull { it.id == previousIconId } ?: iconOption
        val shouldRefreshDemo = existingDemo == null || previousIconId != iconOption.id

        val archivedProjects =
            if (shouldRefreshDemo && existingDemo != null && isDemoProjectModified(existingDemo, previousIconOption)) {
                listOf(archiveDemoProject(existingDemo))
            } else {
                emptyList()
            }

        val nextDemo =
            if (shouldRefreshDemo) {
                buildDemoProject(iconOption, PatternEngine.DEMO_PROJECT_ID)
                    ?: existingDemo
                    ?: PatternEngine.demoProject()
            } else {
                existingDemo ?: buildDemoProject(iconOption, PatternEngine.DEMO_PROJECT_ID) ?: PatternEngine.demoProject()
            }

        if (shouldRefreshDemo) {
            saveProjectResumeScreen(PatternEngine.DEMO_PROJECT_ID, AppScreen.PatternEditor)
            saveProjectCompletedCellIndices(nextDemo, emptySet())
            saveProjectBoardViewport(
                projectId = PatternEngine.DEMO_PROJECT_ID,
                viewport = BoardViewportState(centerOnRestore = true),
            )
        }
        preferences.edit().putString(DEMO_ICON_ID_KEY, iconOption.id).apply()

        val others = projects.filterNot { it.id == PatternEngine.DEMO_PROJECT_ID }
        return listOf(nextDemo) + archivedProjects + others
    }

    fun isDemoProject(project: BeadProject): Boolean = project.id == PatternEngine.DEMO_PROJECT_ID

    fun canDeleteProject(project: BeadProject): Boolean = !isDemoProject(project)

    fun canRenameProject(project: BeadProject): Boolean = !isDemoProject(project)

    fun saveProjects(projects: List<BeadProject>) {
        cleanupRemovedSourceImages(projects)
        cleanupRemovedProjectResumeScreens(projects)
        cleanupRemovedProjectBuildModes(projects)
        cleanupRemovedProjectCompletedCells(projects)
        cleanupRemovedProjectBoardViewports(projects)
        val array = JSONArray()
        projects.forEach { project ->
            array.put(
                JSONObject()
                    .put("id", project.id)
                    .put("title", project.title)
                    .put("width", project.width)
                    .put("height", project.height)
                    .put("paletteMode", project.paletteMode.name)
                    .put("colorScheme", project.colorScheme.name)
                    .put("beadsPerPack", project.beadsPerPack)
                    .put("sourceKind", project.sourceKind.name)
                    .put("sourceImagePath", project.sourceImagePath ?: JSONObject.NULL)
                    .put("updatedAt", project.updatedAt)
                    .put(
                        "cells",
                        JSONArray().apply {
                            project.cells.forEach { code ->
                                put(code ?: JSONObject.NULL)
                            }
                        },
                    ),
            )
        }

        preferences.edit().putString(KEY, array.toString()).apply()
    }

    fun loadProjectResumeScreen(projectId: String): AppScreen {
        val raw = preferences.getString(RESUME_SCREEN_KEY, null)?.takeIf { it.isNotBlank() } ?: return AppScreen.PatternEditor
        val storedName =
            runCatching {
                JSONObject(raw).optString(projectId, AppScreen.PatternEditor.name)
            }.getOrDefault(AppScreen.PatternEditor.name)
        return if (storedName == AppScreen.BuildMode.name) {
            AppScreen.BuildMode
        } else {
            AppScreen.PatternEditor
        }
    }

    fun saveProjectResumeScreen(
        projectId: String,
        screen: AppScreen,
    ) {
        val normalizedScreen =
            if (screen == AppScreen.BuildMode) {
                AppScreen.BuildMode
            } else {
                AppScreen.PatternEditor
            }
        val payload =
            runCatching {
                JSONObject(preferences.getString(RESUME_SCREEN_KEY, null)?.takeIf { it.isNotBlank() } ?: "{}")
            }.getOrElse {
                JSONObject()
            }
        payload.put(projectId, normalizedScreen.name)
        preferences.edit().putString(RESUME_SCREEN_KEY, payload.toString()).apply()
    }

    fun loadProjectBuildMode(projectId: String): BuildMode {
        val raw = preferences.getString(BUILD_MODE_KEY, null)?.takeIf { it.isNotBlank() } ?: return BuildMode.Color
        val storedName =
            runCatching {
                JSONObject(raw).optString(projectId, BuildMode.Color.name)
            }.getOrDefault(BuildMode.Color.name)
        return runCatching {
            BuildMode.valueOf(storedName)
        }.getOrDefault(BuildMode.Color)
    }

    fun saveProjectBuildMode(
        projectId: String,
        mode: BuildMode,
    ) {
        val payload =
            runCatching {
                JSONObject(preferences.getString(BUILD_MODE_KEY, null)?.takeIf { it.isNotBlank() } ?: "{}")
            }.getOrElse {
                JSONObject()
            }
        payload.put(projectId, mode.name)
        preferences.edit().putString(BUILD_MODE_KEY, payload.toString()).apply()
    }

    fun loadProjectBoardViewport(projectId: String): BoardViewportState? {
        val raw = preferences.getString(BOARD_VIEWPORT_KEY, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return runCatching {
            val payload = JSONObject(raw)
            val stored = payload.optJSONObject(projectId) ?: return null
            BoardViewportState(
                scrollX = stored.optInt("scrollX", 0).coerceAtLeast(0),
                scrollY = stored.optInt("scrollY", 0).coerceAtLeast(0),
                zoom = stored.optDouble("zoom", 20.0).toFloat().takeIf { it.isFinite() } ?: 20f,
                centerOnRestore = stored.optBoolean("centerOnRestore", false),
            )
        }.getOrNull()
    }

    fun saveProjectBoardViewport(
        projectId: String,
        viewport: BoardViewportState,
    ) {
        val payload =
            runCatching {
                JSONObject(preferences.getString(BOARD_VIEWPORT_KEY, null)?.takeIf { it.isNotBlank() } ?: "{}")
            }.getOrElse {
                JSONObject()
            }
        payload.put(
            projectId,
            JSONObject()
                .put("scrollX", viewport.scrollX.coerceAtLeast(0))
                .put("scrollY", viewport.scrollY.coerceAtLeast(0))
                .put("zoom", viewport.zoom)
                .put("centerOnRestore", viewport.centerOnRestore),
        )
        preferences.edit().putString(BOARD_VIEWPORT_KEY, payload.toString()).apply()
    }

    fun loadProjectCompletedCellIndices(project: BeadProject): Set<Int> {
        val raw = preferences.getString(COMPLETED_CELLS_KEY, null)?.takeIf { it.isNotBlank() } ?: return emptySet()
        return runCatching {
            val payload = JSONObject(raw)
            val stored = payload.optJSONArray(project.id) ?: return emptySet()
            buildSet {
                for (index in 0 until stored.length()) {
                    val cellIndex = stored.optInt(index, -1)
                    if (cellIndex in project.cells.indices && project.cells[cellIndex] != null) {
                        add(cellIndex)
                    }
                }
            }
        }.getOrDefault(emptySet())
    }

    fun saveProjectCompletedCellIndices(
        project: BeadProject,
        indices: Set<Int>,
    ) {
        val normalized =
            indices
                .filter { index -> index in project.cells.indices && project.cells[index] != null }
                .distinct()
                .sorted()
        val payload =
            runCatching {
                JSONObject(preferences.getString(COMPLETED_CELLS_KEY, null)?.takeIf { it.isNotBlank() } ?: "{}")
            }.getOrElse {
                JSONObject()
            }
        if (normalized.isEmpty()) {
            payload.remove(project.id)
        } else {
            payload.put(
                project.id,
                JSONArray().apply {
                    normalized.forEach(::put)
                },
            )
        }
        preferences.edit().putString(COMPLETED_CELLS_KEY, payload.toString()).apply()
    }

    fun persistSourceBitmap(
        projectId: String,
        bitmap: Bitmap,
    ): String? {
        val targetFile = sourceFileForProject(projectId)
        return runCatching {
            targetFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    fun loadSourceBitmap(project: BeadProject): Bitmap? {
        val rawPath = project.sourceImagePath?.takeIf { it.isNotBlank() } ?: return null
        val file = File(rawPath)
        if (!file.exists()) {
            return null
        }
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun deleteProjectSource(project: BeadProject) {
        val rawPath = project.sourceImagePath?.takeIf { it.isNotBlank() } ?: return
        runCatching {
            File(rawPath).delete()
        }
    }

    private fun readProject(json: JSONObject): BeadProject {
        val paletteMode = readPaletteMode(json)
        val colorScheme = readColorScheme(json, paletteMode)
        val cellsArray = json.getJSONArray("cells")
        val cells = buildList<String?> {
            for (index in 0 until cellsArray.length()) {
                add(
                    BeadPalette.normalizeStoredCode(
                        code = if (cellsArray.isNull(index)) null else cellsArray.getString(index),
                        scheme = colorScheme,
                    ),
                )
            }
        }

        return BeadProject(
            id = json.optString("id", ""),
            title = json.optString("title", "豆豆鱼图纸"),
            width = json.optInt("width", 29),
            height = json.optInt("height", 29),
            cells = cells,
            paletteMode = paletteMode,
            colorScheme = colorScheme,
            beadsPerPack = json.optInt("beadsPerPack", 500).coerceAtLeast(1),
            sourceKind = readProjectSourceKind(json),
            sourceImagePath = json.optString("sourceImagePath", "").ifBlank { null },
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    private fun readPaletteMode(json: JSONObject): PaletteMode {
        return runCatching {
            PaletteMode.valueOf(json.optString("paletteMode", PaletteMode.Basic.name))
        }.getOrDefault(PaletteMode.Basic)
    }

    private fun readColorScheme(
        json: JSONObject,
        paletteMode: PaletteMode = readPaletteMode(json),
    ): ColorSchemePreset {
        val stored =
            runCatching {
                ColorSchemePreset.valueOf(json.optString("colorScheme"))
            }.getOrNull()
        if (stored != null) {
            return stored
        }
        return if (paletteMode == PaletteMode.Complete) {
            ColorSchemePreset.Complete291
        } else {
            ColorSchemePreset.MardBrand221
        }
    }

    private fun readProjectSourceKind(json: JSONObject): ProjectSourceKind {
        return runCatching {
            ProjectSourceKind.valueOf(
                json.optString("sourceKind", ProjectSourceKind.ImageConversion.name),
            )
        }.getOrDefault(ProjectSourceKind.ImageConversion)
    }

    private fun cleanupRemovedSourceImages(projects: List<BeadProject>) {
        val raw = preferences.getString(KEY, null)?.takeIf { it.isNotBlank() } ?: return
        val keptPaths = projects.mapNotNull { it.sourceImagePath }.toSet()
        runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val path = array.getJSONObject(index).optString("sourceImagePath", "")
                if (path.isNotBlank() && path !in keptPaths) {
                    File(path).delete()
                }
            }
        }
    }

    private fun cleanupRemovedProjectResumeScreens(projects: List<BeadProject>) {
        val raw = preferences.getString(RESUME_SCREEN_KEY, null)?.takeIf { it.isNotBlank() } ?: return
        val keptIds = projects.map { it.id }.toSet()
        runCatching {
            val payload = JSONObject(raw)
            val toRemove = mutableListOf<String>()
            val keys = payload.keys()
            while (keys.hasNext()) {
                val projectId = keys.next()
                if (projectId !in keptIds) {
                    toRemove += projectId
                }
            }
            toRemove.forEach(payload::remove)
            preferences.edit().putString(RESUME_SCREEN_KEY, payload.toString()).apply()
        }
    }

    private fun cleanupRemovedProjectBuildModes(projects: List<BeadProject>) {
        val raw = preferences.getString(BUILD_MODE_KEY, null)?.takeIf { it.isNotBlank() } ?: return
        val keptIds = projects.map { it.id }.toSet()
        runCatching {
            val payload = JSONObject(raw)
            val toRemove = mutableListOf<String>()
            val keys = payload.keys()
            while (keys.hasNext()) {
                val projectId = keys.next()
                if (projectId !in keptIds) {
                    toRemove += projectId
                }
            }
            toRemove.forEach(payload::remove)
            preferences.edit().putString(BUILD_MODE_KEY, payload.toString()).apply()
        }
    }

    private fun cleanupRemovedProjectCompletedCells(projects: List<BeadProject>) {
        val raw = preferences.getString(COMPLETED_CELLS_KEY, null)?.takeIf { it.isNotBlank() } ?: return
        val keptIds = projects.map { it.id }.toSet()
        runCatching {
            val payload = JSONObject(raw)
            val toRemove = mutableListOf<String>()
            val keys = payload.keys()
            while (keys.hasNext()) {
                val projectId = keys.next()
                if (projectId !in keptIds) {
                    toRemove += projectId
                }
            }
            toRemove.forEach(payload::remove)
            preferences.edit().putString(COMPLETED_CELLS_KEY, payload.toString()).apply()
        }
    }

    private fun cleanupRemovedProjectBoardViewports(projects: List<BeadProject>) {
        val raw = preferences.getString(BOARD_VIEWPORT_KEY, null)?.takeIf { it.isNotBlank() } ?: return
        val keptIds = projects.map { it.id }.toSet()
        runCatching {
            val payload = JSONObject(raw)
            val toRemove = mutableListOf<String>()
            val keys = payload.keys()
            while (keys.hasNext()) {
                val projectId = keys.next()
                if (projectId !in keptIds) {
                    toRemove += projectId
                }
            }
            toRemove.forEach(payload::remove)
            preferences.edit().putString(BOARD_VIEWPORT_KEY, payload.toString()).apply()
        }
    }

    private fun sourceFileForProject(projectId: String): File {
        return File(sourceImageDirectory, "$projectId.png")
    }

    private fun currentAppIconOption(): AppIconOption {
        val iconId = AppIconManager(appContext).getSelectedIconId()
        return AppIconRegistry.options.firstOrNull { it.id == iconId } ?: AppIconRegistry.defaultOption
    }

    private fun buildDemoProject(
        iconOption: AppIconOption,
        projectId: String,
    ): BeadProject? {
        val sourceBitmap = BitmapFactory.decodeResource(appContext.resources, iconOption.previewResId) ?: return null
        val trimmedBitmap = trimTransparentBounds(sourceBitmap)
        val persistedSourcePath = persistSourceBitmap(projectId, trimmedBitmap)
        return PatternEngine
            .demoProjectFromSource(trimmedBitmap, iconOption.label)
            .copy(
                id = projectId,
                sourceImagePath = persistedSourcePath,
            )
    }

    private fun archiveDemoProject(project: BeadProject): BeadProject {
        val archivedId = UUID.randomUUID().toString()
        val archiveSuffix = "（已修改）"
        val archivedTitle =
            if (project.title.endsWith(archiveSuffix)) {
                project.title
            } else {
                "${project.title}$archiveSuffix"
            }
        val archivedSourcePath =
            loadSourceBitmap(project)?.let { bitmap ->
                persistSourceBitmap(archivedId, bitmap)
            } ?: project.sourceImagePath
        val archived =
            project.copy(
                id = archivedId,
                title = archivedTitle,
                sourceImagePath = archivedSourcePath,
                updatedAt = System.currentTimeMillis(),
        )
        saveProjectResumeScreen(archived.id, loadProjectResumeScreen(project.id))
        saveProjectBuildMode(archived.id, loadProjectBuildMode(project.id))
        saveProjectCompletedCellIndices(archived, loadProjectCompletedCellIndices(project))
        loadProjectBoardViewport(project.id)?.let { viewport ->
            saveProjectBoardViewport(archived.id, viewport)
        }
        return archived
    }

    private fun isDemoProjectModified(
        project: BeadProject,
        iconOption: AppIconOption,
    ): Boolean {
        val baseline = buildDemoProject(iconOption, PatternEngine.DEMO_PROJECT_ID) ?: return true
        return !sameProjectIgnoringTimestamp(project, baseline)
    }

    private fun trimTransparentBounds(bitmap: Bitmap): Bitmap {
        var minX = bitmap.width
        var minY = bitmap.height
        var maxX = -1
        var maxY = -1
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (((pixels[y * bitmap.width + x] ushr 24) and 0xFF) > 0) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return bitmap
        }
        if (minX == 0 && minY == 0 && maxX == bitmap.width - 1 && maxY == bitmap.height - 1) {
            return bitmap
        }
        return Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    private fun sameProjectIgnoringTimestamp(
        left: BeadProject,
        right: BeadProject,
    ): Boolean {
        return left.copy(updatedAt = 0L, sourceImagePath = null) == right.copy(updatedAt = 0L, sourceImagePath = null)
    }

    private companion object {
        const val KEY = "projects_json"
        const val RESUME_SCREEN_KEY = "project_resume_screens_json"
        const val BUILD_MODE_KEY = "project_build_modes_json"
        const val COMPLETED_CELLS_KEY = "project_completed_cells_json"
        const val BOARD_VIEWPORT_KEY = "project_board_viewports_json"
        const val DEMO_ICON_ID_KEY = "demo_icon_id"
    }
}
