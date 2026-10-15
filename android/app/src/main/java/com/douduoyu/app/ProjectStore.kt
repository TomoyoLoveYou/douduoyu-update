package com.douduoyu.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
                }.orderedByMostRecent()
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
                    ?.preservingUpdatedAtIfEquivalentTo(existingDemo)
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
        return (listOf(nextDemo) + archivedProjects + others).orderedByMostRecent()
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
            val encodedCells = ProjectCellsCodec.encode(project.cells)
            array.put(
                JSONObject()
                    .put("id", project.id)
                    .put("title", project.title)
                    .put("width", project.width)
                    .put("height", project.height)
                    .put("paletteMode", project.paletteMode.name)
                    .put("colorScheme", project.colorScheme.name)
                    .put("customColorSchemeId", project.customColorSchemeId ?: JSONObject.NULL)
                    .put("beadsPerPack", project.beadsPerPack)
                    .put("sourceKind", project.sourceKind.name)
                    .put("sourceImagePath", project.sourceImagePath ?: JSONObject.NULL)
                    .put(
                        "stitchSourceRegions",
                        JSONArray().apply {
                            project.stitchSourceRegions.forEach { region ->
                                put(
                                    JSONObject()
                                        .put("sourceProjectId", region.sourceProjectId)
                                        .put("sourceTitle", region.sourceTitle)
                                        .put("left", region.left)
                                        .put("top", region.top)
                                        .put("width", region.width)
                                        .put("height", region.height),
                                )
                            }
                        },
                    )
                    .put("updatedAt", project.updatedAt)
                    .put("cellsCodec", ProjectCellsCodec.FORMAT)
                    .put("cellsCount", encodedCells.cellCount)
                    .put("cellsPalette", JSONArray(encodedCells.palette))
                    .put("cellsData", Base64.encodeToString(encodedCells.payload, Base64.NO_WRAP)),
            )
        }

        preferences.edit().putString(KEY, array.toString()).apply()
    }

    internal fun loadGridGuideSettings(): GridGuideSettings =
        GridGuideSettings(
            enabled = preferences.getBoolean(GRID_GUIDE_ENABLED_KEY, true),
            interval = preferences.getInt(GRID_GUIDE_INTERVAL_KEY, GridGuideSettings.DEFAULT_INTERVAL),
            lineWidthDp =
                preferences.getFloat(
                    GRID_GUIDE_LINE_WIDTH_DP_KEY,
                    GridGuideSettings.DEFAULT_LINE_WIDTH_DP,
                ),
            colorArgb = preferences.getInt(GRID_GUIDE_COLOR_ARGB_KEY, GridGuideSettings.DEFAULT_COLOR_ARGB),
        ).normalized()

    internal fun saveGridGuideSettings(settings: GridGuideSettings) {
        val normalized = settings.normalized()
        preferences
            .edit()
            .putBoolean(GRID_GUIDE_ENABLED_KEY, normalized.enabled)
            .putInt(GRID_GUIDE_INTERVAL_KEY, normalized.interval)
            .putFloat(GRID_GUIDE_LINE_WIDTH_DP_KEY, normalized.lineWidthDp)
            .putInt(GRID_GUIDE_COLOR_ARGB_KEY, normalized.colorArgb)
            .apply()
    }

    internal fun hasShownSplitMoveTutorial(): Boolean =
        preferences.getBoolean(SPLIT_MOVE_TUTORIAL_SHOWN_KEY, false)

    internal fun markSplitMoveTutorialShown() {
        preferences.edit().putBoolean(SPLIT_MOVE_TUTORIAL_SHOWN_KEY, true).apply()
    }

    internal fun loadSplitBoardSize(): Pair<Int, Int> =
        preferences.getInt(SPLIT_BOARD_WIDTH_KEY, DEFAULT_SPLIT_BOARD_SIZE).coerceAtLeast(1) to
            preferences.getInt(SPLIT_BOARD_HEIGHT_KEY, DEFAULT_SPLIT_BOARD_SIZE).coerceAtLeast(1)

    internal fun saveSplitBoardSize(
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) return
        preferences
            .edit()
            .putInt(SPLIT_BOARD_WIDTH_KEY, width)
            .putInt(SPLIT_BOARD_HEIGHT_KEY, height)
            .apply()
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

    internal fun loadProjectCompletedCellIndices(project: BeadProject): CellIndexMask {
        val raw = preferences.getString(COMPLETED_CELLS_KEY, null)?.takeIf { it.isNotBlank() }
            ?: return CellIndexMask.empty()
        return runCatching {
            val payload = JSONObject(raw)
            val stored = payload.opt(project.id) ?: return CellIndexMask.empty()
            val builder = CellIndexMask.builder()
            when (stored) {
                is JSONArray -> {
                    for (index in 0 until stored.length()) {
                        val cellIndex = stored.optInt(index, -1)
                        if (cellIndex in project.cells.indices && project.cells[cellIndex] != null) {
                            builder.add(cellIndex)
                        }
                    }
                }

                is JSONObject -> {
                    if (stored.optString("format") == COMPLETED_CELLS_FORMAT) {
                        val encoded = Base64.decode(stored.getString("data"), Base64.DEFAULT)
                        CellIndexMask.fromByteArray(encoded).forEach { cellIndex ->
                            if (cellIndex in project.cells.indices && project.cells[cellIndex] != null) {
                                builder.add(cellIndex)
                            }
                        }
                    }
                }
            }
            builder.build()
        }.getOrDefault(CellIndexMask.empty())
    }

    fun saveProjectCompletedCellIndices(
        project: BeadProject,
        indices: Set<Int>,
    ) {
        val normalizedBuilder = CellIndexMask.builder()
        indices.forEach { index ->
            if (index in project.cells.indices && project.cells[index] != null) {
                normalizedBuilder.add(index)
            }
        }
        val normalized = normalizedBuilder.build()
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
                JSONObject()
                    .put("format", COMPLETED_CELLS_FORMAT)
                    .put("data", Base64.encodeToString(normalized.toByteArray(), Base64.NO_WRAP)),
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
        val width = json.optInt("width", 29).coerceAtLeast(1)
        val height = json.optInt("height", 29).coerceAtLeast(1)
        val cells = readProjectCells(json, colorScheme, width, height)

        return BeadProject(
            id = json.optString("id", ""),
            title = json.optString("title", "豆豆鱼图纸"),
            width = width,
            height = height,
            cells = cells,
            paletteMode = paletteMode,
            colorScheme = colorScheme,
            customColorSchemeId = json.optNullableString("customColorSchemeId"),
            beadsPerPack = json.optInt("beadsPerPack", 500).coerceAtLeast(1),
            sourceKind = readProjectSourceKind(json),
            sourceImagePath = json.optNullableString("sourceImagePath"),
            stitchSourceRegions = readStitchSourceRegions(json),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    private fun readProjectCells(
        json: JSONObject,
        colorScheme: ColorSchemePreset,
        width: Int,
        height: Int,
    ): List<String?> {
        val encodedCells =
            if (json.optString("cellsCodec") == ProjectCellsCodec.FORMAT) {
                runCatching {
                    val paletteArray = json.getJSONArray("cellsPalette")
                    val palette = List(paletteArray.length()) { index -> paletteArray.getString(index) }
                    val payload = Base64.decode(json.getString("cellsData"), Base64.DEFAULT)
                    ProjectCellsCodec.decode(
                        palette = palette,
                        payload = payload,
                        cellCount = json.optInt("cellsCount", -1),
                    )
                }.getOrNull()
            } else {
                null
            }
        val storedCells =
            encodedCells ?: runCatching {
                val cellsArray = json.getJSONArray("cells")
                List<String?>(cellsArray.length()) { index ->
                    if (cellsArray.isNull(index)) null else cellsArray.getString(index)
                }
            }.getOrNull()
        val expectedCellCount =
            (width.toLong() * height.toLong())
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        return if (storedCells != null && storedCells.size == expectedCellCount) {
            storedCells.map { code -> BeadPalette.normalizeStoredCode(code = code, scheme = colorScheme) }
        } else {
            List(expectedCellCount) { null }
        }
    }

    private fun readStitchSourceRegions(json: JSONObject): List<StitchSourceRegion> {
        val array = json.optJSONArray("stitchSourceRegions") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                if (size >= MAX_STITCH_SOURCE_REGIONS) break
                val item = array.optJSONObject(index) ?: continue
                val width = item.optInt("width", 0)
                val height = item.optInt("height", 0)
                if (width <= 0 || height <= 0) continue
                add(
                    StitchSourceRegion(
                        sourceProjectId = item.optString("sourceProjectId", ""),
                        sourceTitle = item.optString("sourceTitle", "来源项目"),
                        left = item.optInt("left", 0),
                        top = item.optInt("top", 0),
                        width = width,
                        height = height,
                    ),
                )
            }
        }
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
        return sameProjectContentIgnoringTimestamp(left, right)
    }

    private companion object {
        const val KEY = "projects_json"
        const val RESUME_SCREEN_KEY = "project_resume_screens_json"
        const val BUILD_MODE_KEY = "project_build_modes_json"
        const val COMPLETED_CELLS_KEY = "project_completed_cells_json"
        const val COMPLETED_CELLS_FORMAT = "bitset-v1"
        const val BOARD_VIEWPORT_KEY = "project_board_viewports_json"
        const val DEMO_ICON_ID_KEY = "demo_icon_id"
        const val GRID_GUIDE_ENABLED_KEY = "grid_guide_enabled"
        const val GRID_GUIDE_INTERVAL_KEY = "grid_guide_interval"
        const val GRID_GUIDE_LINE_WIDTH_DP_KEY = "grid_guide_line_width_dp"
        const val GRID_GUIDE_COLOR_ARGB_KEY = "grid_guide_color_argb"
        const val SPLIT_MOVE_TUTORIAL_SHOWN_KEY = "split_move_tutorial_shown"
        const val SPLIT_BOARD_WIDTH_KEY = "split_board_width"
        const val SPLIT_BOARD_HEIGHT_KEY = "split_board_height"
        const val DEFAULT_SPLIT_BOARD_SIZE = 29
        const val MAX_STITCH_SOURCE_REGIONS = 64
    }
}

internal fun List<BeadProject>.orderedByMostRecent(): List<BeadProject> =
    sortedByDescending { it.updatedAt }

internal fun BeadProject.preservingUpdatedAtIfEquivalentTo(existing: BeadProject?): BeadProject {
    return if (existing != null && sameProjectContentIgnoringTimestamp(this, existing)) {
        copy(updatedAt = existing.updatedAt)
    } else {
        this
    }
}

private fun sameProjectContentIgnoringTimestamp(
    left: BeadProject,
    right: BeadProject,
): Boolean {
    return left.copy(updatedAt = 0L, sourceImagePath = null) == right.copy(updatedAt = 0L, sourceImagePath = null)
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) {
        return null
    }
    return optString(key, "").trim().takeIf { it.isNotEmpty() }
}
