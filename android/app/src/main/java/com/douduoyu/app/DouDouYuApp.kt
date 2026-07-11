@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.douduoyu.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoFilter
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ImageToolPanel {
    Crop,
}

private enum class PatternToolPanel {
    Tool,
    CanvasExpand,
    Options,
    Output,
    Logic,
    Usage,
    Zoom,
    Fullscreen,
}

private enum class PatternToolSubPanel {
    Remove,
    Color,
    Mirror,
    Rotate,
}

private enum class PaletteDetailSection {
    Brand,
    ColorCount,
}

private enum class BuildToolPanel {
    Mode,
    Zoom,
    Focus,
    Usage,
    MarkDone,
    Fullscreen,
}

private enum class MainTab {
    Projects,
    Create,
    Settings,
}

private enum class CreateFlowKind {
    ImageToPattern,
    ChartRecognition,
}

private enum class SettingsPage {
    Home,
    ColorConfidence,
}

private data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val publishedAt: String,
    val title: String,
    val summary: String,
    val changelog: List<String>,
    val downloadUrl: String,
    val password: String,
    val forceUpdate: Boolean,
    val sourceUrl: String,
)

private data class CurrentAppVersion(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
) {
    val displayLabel: String
        get() = "$versionName ($versionCode)"
}

private sealed class UpdateCheckResult {
    data class UpdateAvailable(val update: AppUpdateInfo) : UpdateCheckResult()

    data class UpToDate(
        val versionName: String,
        val sourceUrl: String,
    ) : UpdateCheckResult()

    data class Failed(val message: String) : UpdateCheckResult()
}

private sealed class UpdateCheckUiState {
    data object Idle : UpdateCheckUiState()

    data object Checking : UpdateCheckUiState()

    data class Checked(
        val versionName: String,
        val sourceUrl: String,
    ) : UpdateCheckUiState()

    data class Failed(val message: String) : UpdateCheckUiState()
}

private object AppUpdateChecker {
    private const val CONNECTION_TIMEOUT_MS = 8000
    private const val READ_TIMEOUT_MS = 8000

    private val metadataUrls =
        listOf(
            "https://gitee.com/TomoyoLoveYou/douduoyu-update/raw/main/version.json",
            "https://raw.githubusercontent.com/TomoyoLoveYou/douduoyu-update/main/version.json",
        )

    suspend fun check(currentVersion: CurrentAppVersion): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            for (metadataUrl in metadataUrls) {
                try {
                    return@withContext parseMetadata(
                        raw = fetchMetadata(metadataUrl),
                        sourceUrl = metadataUrl,
                        currentVersion = currentVersion,
                    )
                } catch (error: Exception) {
                    errors += "${metadataUrl.substringAfter("://").substringBefore('/')}: ${error.message ?: "读取失败"}"
                }
            }
            UpdateCheckResult.Failed(errors.joinToString("；").ifBlank { "无法读取版本信息。" })
        }

    private fun fetchMetadata(metadataUrl: String): String {
        val connection =
            (URL(metadataUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }

        return try {
            val statusCode = connection.responseCode
            val responseText =
                (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode")
            }
            responseText
        } finally {
            connection.disconnect()
        }
    }

    private fun parseMetadata(
        raw: String,
        sourceUrl: String,
        currentVersion: CurrentAppVersion,
    ): UpdateCheckResult {
        val root = JSONObject(raw)
        val platform = root.optString("platform")
        val packageName = root.optString("packageName")
        if (platform.isNotBlank() && platform != "android") {
            throw IllegalArgumentException("平台不匹配")
        }
        if (packageName.isNotBlank() && packageName != currentVersion.packageName) {
            throw IllegalArgumentException("包名不匹配")
        }

        val latest = root.getJSONObject("latest")
        val versionCode = latest.optLong("versionCode", 0L)
        val download = latest.optJSONObject("download")
        val mirrorArray = download?.optJSONArray("mirrorUrls")
        val mirrorUrls =
            List(mirrorArray?.length() ?: 0) { index ->
                mirrorArray?.optString(index).orEmpty()
            }.filter { it.isNotBlank() }
        val downloadUrl =
            listOf(
                download?.optString("pageUrl").orEmpty(),
                download?.optString("apkUrl").orEmpty(),
            ).plus(mirrorUrls).firstOrNull { it.isNotBlank() }.orEmpty()
        val changelogArray = latest.optJSONArray("changelog")
        val changelog =
            List(changelogArray?.length() ?: 0) { index ->
                changelogArray?.optString(index).orEmpty()
            }.filter { it.isNotBlank() }
        val versionName = latest.optString("versionName", versionCode.toString())

        return if (versionCode > currentVersion.versionCode) {
            UpdateCheckResult.UpdateAvailable(
                AppUpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    publishedAt = latest.optString("publishedAt"),
                    title = latest.optString("title", "发现新版本"),
                    summary = latest.optString("summary"),
                    changelog = changelog,
                    downloadUrl = downloadUrl,
                    password = download?.optString("password").orEmpty(),
                    forceUpdate = latest.optBoolean("forceUpdate", false),
                    sourceUrl = sourceUrl,
                ),
            )
        } else {
            UpdateCheckResult.UpToDate(versionName = versionName, sourceUrl = sourceUrl)
        }
    }
}

private fun currentAppVersion(context: Context): CurrentAppVersion {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    return CurrentAppVersion(
        packageName = context.packageName,
        versionCode = versionCode,
        versionName = packageInfo.versionName ?: versionCode.toString(),
    )
}

private fun openExternalBrowser(
    context: Context,
    url: String,
): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, "打开下载链接"))
        true
    }.getOrDefault(false)
}

private data class PatternEditorSnapshot(
    val project: BeadProject,
    val settings: GenerationSettings,
)

private data class SurfaceTapRecord(
    val timeMillis: Long,
    val position: Offset,
)

@Composable
private fun HandleFullscreenState(
    isFullscreen: Boolean,
    onExitFullscreen: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var shouldExitOnNextResume by remember(lifecycleOwner) { mutableStateOf(false) }

    BackHandler(enabled = isFullscreen) {
        onExitFullscreen()
    }

    DisposableEffect(lifecycleOwner, isFullscreen, onExitFullscreen) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        if (isFullscreen) {
                            shouldExitOnNextResume = true
                        }
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        if (isFullscreen && shouldExitOnNextResume) {
                            shouldExitOnNextResume = false
                            onExitFullscreen()
                        }
                    }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private enum class CropAspectPreset {
    Custom,
    Original,
    Square,
    Ratio4x3,
    Ratio3x4,
    Ratio16x9,
    Ratio9x16,
}

@Composable
fun DouDouYuApp() {
    val context = LocalContext.current
    val themeManager = remember(context) { ThemeManager(context) }
    var selectedThemeId by remember { mutableStateOf(themeManager.getSelectedThemeId()) }
    val currentTheme = remember(selectedThemeId) {
        AppThemeRegistry.options.firstOrNull { it.id == selectedThemeId } ?: AppThemeRegistry.defaultOption
    }

    MaterialTheme(colorScheme = currentTheme.colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val scope = rememberCoroutineScope()
            val store = remember(context) { ProjectStore(context) }
            val appIconManager = remember(context) { AppIconManager(context) }

            var screen by remember { mutableStateOf(AppScreen.Home) }
            var projects by remember { mutableStateOf(store.loadProjects()) }
            var currentProject by remember { mutableStateOf<BeadProject?>(null) }
            var settings by remember { mutableStateOf(defaultSettings(nextProjectDefaultTitle(projects))) }
            var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var sourceBitmapPath by remember { mutableStateOf<String?>(null) }
            var selectedColorCode by remember { mutableStateOf(BeadPalette.palette(ColorSchemePreset.MardBrand221).first().id) }
            var toolMode by remember { mutableStateOf(ToolMode.Pencil) }
            var removeMode by remember { mutableStateOf(RemoveMode.SameConnected) }
            var mirrorDirection by remember { mutableStateOf(MirrorDirection.Horizontal) }
            var buildMode by remember { mutableStateOf(BuildMode.Color) }
            var focusRow by remember { mutableIntStateOf(0) }
            var focusColumn by remember { mutableIntStateOf(0) }
            var focusColorCode by remember { mutableStateOf<String?>(null) }
            var focusColorBlockIndex by remember { mutableIntStateOf(0) }
            var buildCompletedCellIndices by remember { mutableStateOf(emptySet<Int>()) }
            var boardZoom by remember { mutableFloatStateOf(DEFAULT_BOARD_ZOOM) }
            val boardHorizontalScroll = rememberScrollState()
            val boardVerticalScroll = rememberScrollState()
            var pendingBoardViewportRestore by remember { mutableStateOf<BoardViewportState?>(null) }
            var isRestoringBoardViewport by remember { mutableStateOf(false) }
            var mainTab by remember { mutableStateOf(MainTab.Projects) }
            var createFlowKind by remember { mutableStateOf(CreateFlowKind.ImageToPattern) }
            var isImportingImage by remember { mutableStateOf(false) }
            var isGenerating by remember { mutableStateOf(false) }
            var generationJob by remember { mutableStateOf<Job?>(null) }
            var generationVersion by remember { mutableIntStateOf(0) }
            var statusText by remember {
                mutableStateOf("首页只保留 Android 本地图纸。新建后先裁剪，再按开源逻辑生成，再进编辑器处理。")
            }
            var selectedAppIconId by remember { mutableStateOf(appIconManager.getSelectedIconId()) }
            var editorUndoStack by remember { mutableStateOf(emptyList<PatternEditorSnapshot>()) }
            var editorRedoStack by remember { mutableStateOf(emptyList<PatternEditorSnapshot>()) }
            var editorLastSavedProject by remember { mutableStateOf<BeadProject?>(null) }
            var showEditorExitDialog by remember { mutableStateOf(false) }
            var outputPreviewJob by remember { mutableStateOf<Job?>(null) }
            var pendingOutputLevel by remember { mutableStateOf<Int?>(null) }
            var lastOutputPreviewDispatchAt by remember { mutableStateOf(0L) }
            var outputRequestedLevel by remember { mutableStateOf<Int?>(null) }
            var pendingDeleteProject by remember { mutableStateOf<BeadProject?>(null) }
            var protectedProjectDeleteNotice by remember { mutableStateOf<BeadProject?>(null) }
            var pendingRenameProject by remember { mutableStateOf<BeadProject?>(null) }
            var protectedProjectRenameNotice by remember { mutableStateOf<BeadProject?>(null) }
            var renameDraftTitle by remember { mutableStateOf("") }
            var settingsPage by remember { mutableStateOf(SettingsPage.Home) }
            var isSettingsPickerOpen by remember { mutableStateOf(false) }

            LaunchedEffect(appIconManager) {
                selectedAppIconId = appIconManager.reconcileSelection()
            }

            LaunchedEffect(currentProject?.id) {
                val projectId = currentProject?.id ?: return@LaunchedEffect
                snapshotFlow {
                    BoardViewportState(
                        scrollX = boardHorizontalScroll.value,
                        scrollY = boardVerticalScroll.value,
                        zoom = boardZoom,
                    )
                }.distinctUntilChanged()
                    .collect { viewport ->
                        if (!isRestoringBoardViewport && pendingBoardViewportRestore == null) {
                            store.saveProjectBoardViewport(projectId, viewport)
                        }
                    }
            }

            LaunchedEffect(currentProject?.id, pendingBoardViewportRestore) {
                val viewport = pendingBoardViewportRestore ?: return@LaunchedEffect
                currentProject?.id ?: return@LaunchedEffect
                isRestoringBoardViewport = true
                try {
                    boardZoom = viewport.zoom.coerceIn(BOARD_ZOOM_RANGE)
                    repeat(2) { withFrameNanos {} }
                    if (viewport.centerOnRestore) {
                        boardHorizontalScroll.scrollTo(boardHorizontalScroll.maxValue / 2)
                        boardVerticalScroll.scrollTo(boardVerticalScroll.maxValue / 2)
                    } else {
                        boardHorizontalScroll.scrollTo(viewport.scrollX.coerceAtMost(boardHorizontalScroll.maxValue))
                        boardVerticalScroll.scrollTo(viewport.scrollY.coerceAtMost(boardVerticalScroll.maxValue))
                    }
                    pendingBoardViewportRestore = null
                } finally {
                    isRestoringBoardViewport = false
                }
            }

            fun isSameProjectContent(
                left: BeadProject,
                right: BeadProject,
            ): Boolean {
                return left.copy(updatedAt = 0L) == right.copy(updatedAt = 0L)
            }

            fun isSameSnapshotContent(
                left: PatternEditorSnapshot,
                right: PatternEditorSnapshot,
            ): Boolean {
                return isSameProjectContent(left.project, right.project) && left.settings == right.settings
            }

            fun paletteFallback(
                scheme: ColorSchemePreset,
                mode: PaletteMode = scheme.paletteMode,
            ): String {
                return BeadPalette.palette(scheme).firstOrNull()?.id
                    ?: BeadPalette.palette(mode).firstOrNull()?.id
                    ?: BeadPalette.completeColors.first().id
            }

            fun saveProjects(next: List<BeadProject>) {
                val sorted = next.sortedByDescending { it.updatedAt }.take(24)
                projects = sorted
                store.saveProjects(sorted)
            }

            fun persistProject(project: BeadProject): BeadProject {
                val normalized = project.copy(updatedAt = System.currentTimeMillis())
                saveProjects(listOf(normalized) + projects.filterNot { it.id == normalized.id })
                currentProject = normalized
                return normalized
            }

            fun saveProjectResumeScreen(
                projectId: String,
                screen: AppScreen,
            ) {
                store.saveProjectResumeScreen(projectId, screen)
            }

            fun saveCurrentBoardViewport(projectId: String) {
                store.saveProjectBoardViewport(
                    projectId = projectId,
                    viewport =
                        BoardViewportState(
                            scrollX = boardHorizontalScroll.value,
                            scrollY = boardVerticalScroll.value,
                            zoom = boardZoom,
                        ),
                )
            }

            fun requestProjectDeletion(project: BeadProject) {
                if (store.canDeleteProject(project)) {
                    pendingDeleteProject = project
                } else {
                    protectedProjectDeleteNotice = project
                    statusText = "示例项目不能删除，修改后衍生出来的新项目可以删除。"
                }
            }

            fun confirmPendingProjectDeletion() {
                val project = pendingDeleteProject ?: return
                pendingDeleteProject = null
                if (!store.canDeleteProject(project)) {
                    protectedProjectDeleteNotice = project
                    statusText = "示例项目不能删除，修改后衍生出来的新项目可以删除。"
                    return
                }
                store.deleteProjectSource(project)
                saveProjects(projects.filterNot { it.id == project.id })
                statusText = "已删除 ${project.title}。"
            }

            fun requestProjectRename(project: BeadProject) {
                if (store.canRenameProject(project)) {
                    renameDraftTitle = project.title
                    pendingRenameProject = project
                } else {
                    protectedProjectRenameNotice = project
                    statusText = "示例项目不能重命名。"
                }
            }

            fun confirmPendingProjectRename() {
                val project = pendingRenameProject ?: return
                val nextTitle = renameDraftTitle.trim()
                if (nextTitle.isBlank()) {
                    return
                }
                pendingRenameProject = null
                renameDraftTitle = ""
                if (!store.canRenameProject(project)) {
                    protectedProjectRenameNotice = project
                    statusText = "示例项目不能重命名。"
                    return
                }
                if (nextTitle == project.title) {
                    statusText = "项目名称未变化。"
                    return
                }
                val renamedProject = project.copy(title = nextTitle, updatedAt = System.currentTimeMillis())
                saveProjects(listOf(renamedProject) + projects.filterNot { it.id == renamedProject.id })
                if (currentProject?.id == renamedProject.id) {
                    currentProject = currentProject?.copy(title = nextTitle, updatedAt = renamedProject.updatedAt)
                    settings = settings.copy(title = nextTitle)
                }
                if (editorLastSavedProject?.id == renamedProject.id) {
                    editorLastSavedProject = editorLastSavedProject?.copy(title = nextTitle, updatedAt = renamedProject.updatedAt)
                }
                statusText = "已将 ${project.title} 重命名为 ${nextTitle}。"
            }

            fun syncSelectionForProject(project: BeadProject) {
                val availableCodes = BeadPalette.palette(project.colorScheme).map { it.id }.toSet()
                if (selectedColorCode !in availableCodes) {
                    selectedColorCode =
                        PatternEngine.materialStats(project.cells).firstOrNull()?.code?.takeIf { it in availableCodes }
                            ?: paletteFallback(project.colorScheme, project.paletteMode)
                }
                val currentFocusColor = focusColorCode
                if (currentFocusColor != null && project.cells.none { it == currentFocusColor }) {
                    focusColorCode = selectedColorCode
                }
            }

            fun restoreEditorSnapshot(snapshot: PatternEditorSnapshot) {
                currentProject = snapshot.project
                settings = snapshot.settings
                syncSelectionForProject(snapshot.project)
            }

            fun startPatternEditorSession(
                project: BeadProject,
                sessionSettings: GenerationSettings,
                savedProject: BeadProject?,
                initialViewport: BoardViewportState = centeredBoardViewport(),
            ) {
                boardZoom = initialViewport.zoom.coerceIn(BOARD_ZOOM_RANGE)
                pendingBoardViewportRestore = initialViewport
                val snapshot = PatternEditorSnapshot(project = project, settings = sessionSettings)
                restoreEditorSnapshot(snapshot)
                editorUndoStack = listOf(snapshot)
                editorRedoStack = emptyList()
                editorLastSavedProject = savedProject
                showEditorExitDialog = false
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                pendingOutputLevel = null
                lastOutputPreviewDispatchAt = 0L
                outputRequestedLevel = null
                buildCompletedCellIndices = emptySet()
            }

            fun clearOutputLevelGenerationState() {
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                pendingOutputLevel = null
                lastOutputPreviewDispatchAt = 0L
                outputRequestedLevel = null
            }

            fun startImageToPatternFlow() {
                createFlowKind = CreateFlowKind.ImageToPattern
                settings = defaultSettings(nextProjectDefaultTitle(projects))
                sourceBitmap = null
                sourceBitmapPath = null
                currentProject = null
                clearOutputLevelGenerationState()
                editorUndoStack = emptyList()
                editorRedoStack = emptyList()
                editorLastSavedProject = null
                showEditorExitDialog = false
                mainTab = MainTab.Create
                screen = AppScreen.ImageEditor
                statusText = "先导入图片，再裁剪，再调整生成逻辑。"
            }

            fun startChartRecognitionFlow() {
                createFlowKind = CreateFlowKind.ChartRecognition
                settings = defaultSettings(nextProjectDefaultTitle(projects))
                sourceBitmap = null
                sourceBitmapPath = null
                currentProject = null
                clearOutputLevelGenerationState()
                editorUndoStack = emptyList()
                editorRedoStack = emptyList()
                editorLastSavedProject = null
                showEditorExitDialog = false
                mainTab = MainTab.Create
                screen = AppScreen.ImageEditor
                statusText = "先导入图纸图片，再裁掉多余留白，最后直接识别成可编辑图纸。"
            }

            fun selectMainTab(tab: MainTab) {
                if (tab != MainTab.Settings) {
                    settingsPage = SettingsPage.Home
                }
                if (tab == MainTab.Create && sourceBitmap == null) {
                    settings = defaultSettings(nextProjectDefaultTitle(projects))
                    sourceBitmapPath = null
                    clearOutputLevelGenerationState()
                    isImportingImage = false
                    statusText = "先导入图片，再裁剪，再调整生成逻辑。"
                }
                mainTab = tab
                screen = AppScreen.Home
            }

            /* fun cancelRunningPatternGeneration(message: String? = null) {
                generationJob?.cancel()
                generationJob = null
                generationVersion += 1
                isGenerating = false
                if (message != null) {
                    statusText = message
                }
            }

            fun dispatchOutputLevelGeneration(
                level: Int,
                final: Boolean,
            ) {
                val baseBitmap = sourceBitmap
                if (baseBitmap == null) {
                    clearOutputLevelGenerationState()
                    statusText = "当前项目没有保留来源图片，无法按像素大小实时重生成。"
                    return
                }
                val targetLevel = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
                val currentLevel = outputLevelFromSettings(settings)
                val nextSettings =
                    settingsForOutputLevel(
                        current = settings,
                        baseWidth = baseBitmap.width,
                        baseHeight = baseBitmap.height,
                        level = targetLevel,
                    )
                val targetMatchesCurrent = nextSettings.width == settings.width && nextSettings.height == settings.height
                if (targetMatchesCurrent) {
                    pendingOutputLevel = null
                    if (generationJob != null && outputRequestedLevel != null && outputRequestedLevel != currentLevel) {
                        cancelRunningPatternGeneration("已恢复当前像素大小。")
                    }
                    outputRequestedLevel = currentLevel
                    return
                }
                pendingOutputLevel = null
                outputRequestedLevel = targetLevel
                lastOutputPreviewDispatchAt = System.currentTimeMillis()
                launchPatternGeneration(
                    baseBitmap = baseBitmap,
                    existingProject = currentProject,
                    openPatternEditor = false,
                    startedMessage = if (final) "正在应用最终像素大小..." else "正在按像素大小更新图纸...",
                    completedMessage = "像素大小已更新。",
                    overrideSettings = nextSettings,
                    recordInHistory = false,
                )
            }

            fun scheduleOutputLevelPreview(level: Int) {
                val targetLevel = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
                pendingOutputLevel = targetLevel
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                val now = System.currentTimeMillis()
                val elapsed = now - lastOutputPreviewDispatchAt
                if (elapsed >= OUTPUT_PREVIEW_THROTTLE_MS) {
                    dispatchOutputLevelGeneration(targetLevel, final = false)
                    return
                }
                outputPreviewJob =
                    scope.launch {
                        delay((OUTPUT_PREVIEW_THROTTLE_MS - elapsed).coerceAtLeast(0L))
                        outputPreviewJob = null
                        val latestLevel = pendingOutputLevel ?: return@launch
                        dispatchOutputLevelGeneration(latestLevel, final = false)
                    }
            }

            fun commitOutputLevel(level: Int) {
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                dispatchOutputLevelGeneration(level, final = true)
            }*/

            fun pushEditorSnapshot(
                snapshot: PatternEditorSnapshot,
                message: String? = null,
            ) {
                val normalized =
                    snapshot.copy(
                        project = snapshot.project.copy(updatedAt = System.currentTimeMillis()),
                    )
                val currentSnapshot = editorUndoStack.lastOrNull()
                if (currentSnapshot != null && isSameSnapshotContent(normalized, currentSnapshot)) {
                    return
                }
                restoreEditorSnapshot(normalized)
                editorUndoStack = editorUndoStack + normalized
                editorRedoStack = emptyList()
                if (message != null) {
                    statusText = message
                }
            }

            fun replaceCurrentEditorSnapshot(snapshot: PatternEditorSnapshot) {
                restoreEditorSnapshot(snapshot)
                editorUndoStack =
                    if (editorUndoStack.isEmpty()) {
                        listOf(snapshot)
                    } else {
                        editorUndoStack.dropLast(1) + snapshot
                    }
            }

            fun applyNonHistoricalEditorSnapshot(
                snapshot: PatternEditorSnapshot,
                message: String? = null,
            ) {
                replaceCurrentEditorSnapshot(
                    snapshot.copy(
                        project = snapshot.project.copy(updatedAt = System.currentTimeMillis()),
                    ),
                )
                editorRedoStack = emptyList()
                if (message != null) {
                    statusText = message
                }
            }

            fun saveCurrentEditorProject(
                message: String? = null,
                resumeScreen: AppScreen = AppScreen.PatternEditor,
            ): BeadProject? {
                val project = currentProject ?: return null
                val lastSavedProject = editorLastSavedProject
                val savedProject =
                    if (lastSavedProject != null && isSameProjectContent(project, lastSavedProject)) {
                        currentProject = lastSavedProject
                        lastSavedProject
                    } else {
                        persistProject(project)
                    }
                saveProjectResumeScreen(savedProject.id, resumeScreen)
                saveCurrentBoardViewport(savedProject.id)
                editorLastSavedProject = savedProject
                replaceCurrentEditorSnapshot(PatternEditorSnapshot(savedProject, settings))
                if (message != null) {
                    statusText = message
                }
                return savedProject
            }

            fun hasUnsavedPatternEditorChanges(): Boolean {
                val draftProject = currentProject ?: return false
                return editorLastSavedProject == null || !isSameProjectContent(draftProject, editorLastSavedProject!!)
            }

            fun closePatternEditor(saveBeforeExit: Boolean) {
                val draftProject = currentProject
                val hasUnsavedChanges = hasUnsavedPatternEditorChanges()
                if (saveBeforeExit) {
                    saveCurrentEditorProject("图纸已保存到首页。", resumeScreen = AppScreen.PatternEditor)
                } else {
                    editorLastSavedProject?.id?.let {
                        saveProjectResumeScreen(it, AppScreen.PatternEditor)
                        saveCurrentBoardViewport(it)
                    }
                    if (editorLastSavedProject == null && draftProject != null) {
                        store.deleteProjectSource(draftProject)
                    }
                    statusText =
                        if (hasUnsavedChanges) {
                            "已退出图纸编辑，未保存本次修改。"
                        } else {
                            "已退出图纸编辑。"
                        }
                }
                currentProject = null
                sourceBitmap = null
                sourceBitmapPath = null
                clearOutputLevelGenerationState()
                buildCompletedCellIndices = emptySet()
                editorUndoStack = emptyList()
                editorRedoStack = emptyList()
                editorLastSavedProject = null
                showEditorExitDialog = false
                mainTab = MainTab.Projects
                screen = AppScreen.Home
            }

            fun requestClosePatternEditor() {
                if (hasUnsavedPatternEditorChanges()) {
                    showEditorExitDialog = true
                } else {
                    closePatternEditor(saveBeforeExit = false)
                }
            }

            fun closeBuildModeToHome() {
                currentProject?.id?.let {
                    saveProjectResumeScreen(it, AppScreen.BuildMode)
                    saveCurrentBoardViewport(it)
                }
                currentProject = null
                sourceBitmap = null
                sourceBitmapPath = null
                clearOutputLevelGenerationState()
                buildCompletedCellIndices = emptySet()
                editorUndoStack = emptyList()
                editorRedoStack = emptyList()
                editorLastSavedProject = null
                showEditorExitDialog = false
                mainTab = MainTab.Projects
                screen = AppScreen.Home
                statusText = "已回到首页。"
            }

            fun openPatternEditorFromBuildMode() {
                currentProject?.id?.let {
                    saveProjectResumeScreen(it, AppScreen.PatternEditor)
                    saveCurrentBoardViewport(it)
                }
                screen = AppScreen.PatternEditor
                statusText = "已进入图纸编辑。"
            }

            fun toggleCompletedBuildSelection(indices: Set<Int>) {
                val project = currentProject ?: return
                val normalized =
                    indices.filter { index ->
                        index in project.cells.indices && project.cells[index] != null
                    }.toSet()
                if (normalized.isEmpty()) {
                    return
                }
                val allCompleted = normalized.all { it in buildCompletedCellIndices }
                val nextCompleted =
                    if (allCompleted) {
                        buildCompletedCellIndices - normalized
                    } else {
                        buildCompletedCellIndices + normalized
                    }
                buildCompletedCellIndices = nextCompleted
                store.saveProjectCompletedCellIndices(project, nextCompleted)
            }

            fun undoPatternEdit() {
                if (editorUndoStack.size <= 1) {
                    return
                }
                val currentSnapshot = editorUndoStack.last()
                val previousSnapshot = editorUndoStack[editorUndoStack.lastIndex - 1]
                editorUndoStack = editorUndoStack.dropLast(1)
                editorRedoStack = editorRedoStack + currentSnapshot
                restoreEditorSnapshot(previousSnapshot)
                statusText = "已撤回上一步修改。"
            }

            fun redoPatternEdit() {
                val nextSnapshot = editorRedoStack.lastOrNull() ?: return
                editorRedoStack = editorRedoStack.dropLast(1)
                editorUndoStack = editorUndoStack + nextSnapshot
                restoreEditorSnapshot(nextSnapshot)
                statusText = "已恢复刚才撤回的修改。"
            }

            fun openProject(project: BeadProject) {
                val loadedSourceBitmap = store.loadSourceBitmap(project)
                val resumeScreen = store.loadProjectResumeScreen(project.id)
                val loadedCompletedCellIndices = store.loadProjectCompletedCellIndices(project)
                val loadedBoardViewport = store.loadProjectBoardViewport(project.id) ?: centeredBoardViewport()
                sourceBitmap = loadedSourceBitmap
                sourceBitmapPath = project.sourceImagePath?.takeIf { loadedSourceBitmap != null }
                val sessionSettings =
                    applyColorScheme(
                        settings.copy(
                            title = project.title,
                            width = project.width,
                            height = project.height,
                        ),
                        project.colorScheme,
                    )
                selectedColorCode =
                    PatternEngine.materialStats(project.cells).firstOrNull()?.code
                        ?: paletteFallback(project.colorScheme, project.paletteMode)
                focusColorCode = selectedColorCode
                focusRow = 0
                focusColumn = 0
                focusColorBlockIndex = 0
                toolMode = ToolMode.Pencil
                removeMode = RemoveMode.SameConnected
                mirrorDirection = MirrorDirection.Horizontal
                startPatternEditorSession(
                    project = project,
                    sessionSettings = sessionSettings,
                    savedProject = project,
                    initialViewport = loadedBoardViewport,
                )
                buildCompletedCellIndices = loadedCompletedCellIndices
                buildMode = store.loadProjectBuildMode(project.id)
                if (resumeScreen == AppScreen.BuildMode) {
                    focusColorCode = PatternEngine.materialStats(project.cells).firstOrNull()?.code ?: selectedColorCode
                    focusRow = 0
                    focusColumn = 0
                    focusColorBlockIndex = 0
                    screen = AppScreen.BuildMode
                    statusText = "已进入拼豆模式。可以按颜色、颜色区块、按行或按列查看。"
                } else {
                    screen = AppScreen.PatternEditor
                    statusText = "已进入图纸编辑。现在支持画笔、橡皮、填充、移除、颜色替换、取色和镜像。"
                }
            }

            fun updateCurrentProject(
                message: String? = null,
                transform: (BeadProject) -> BeadProject,
            ) {
                val project = currentProject ?: return
                val nextProject = transform(project)
                if (isSameProjectContent(nextProject, project)) {
                    return
                }
                pushEditorSnapshot(PatternEditorSnapshot(project = nextProject, settings = settings), message)
            }

            fun updateCurrentProjectCells(
                message: String? = null,
                transform: (BeadProject) -> List<String?>,
            ) {
                updateCurrentProject(message) { project ->
                    val nextCells = transform(project)
                    if (nextCells == project.cells) {
                        project
                    } else {
                        if (buildCompletedCellIndices.isNotEmpty()) {
                            buildCompletedCellIndices = emptySet()
                            store.saveProjectCompletedCellIndices(project, emptySet())
                        }
                        project.copy(cells = nextCells)
                    }
                }
            }

            fun updateCurrentProjectGrid(
                message: String? = null,
                transform: (BeadProject) -> EditorTools.GridTransform,
            ) {
                updateCurrentProject(message) { project ->
                    val transformed = transform(project)
                    if (
                        transformed.width == project.width &&
                        transformed.height == project.height &&
                        transformed.cells == project.cells
                    ) {
                        project
                    } else {
                        if (buildCompletedCellIndices.isNotEmpty()) {
                            buildCompletedCellIndices = emptySet()
                            store.saveProjectCompletedCellIndices(project, emptySet())
                        }
                        project.copy(
                            width = transformed.width,
                            height = transformed.height,
                            cells = transformed.cells,
                        )
                    }
                }
            }

            fun launchPatternGeneration(
                baseBitmap: Bitmap,
                existingProject: BeadProject? = null,
                openPatternEditor: Boolean,
                startedMessage: String,
                completedMessage: String,
                overrideSettings: GenerationSettings = settings,
                recordInHistory: Boolean = true,
            ) {
                generationJob?.cancel()
                generationVersion += 1
                val requestVersion = generationVersion
                val generationSettings = overrideSettings.copy(cropZoom = 1f, cropX = 0f, cropY = 0f)
                val preservedProjectId = existingProject?.id
                val preservedPackSize = existingProject?.beadsPerPack
                isGenerating = true
                statusText = startedMessage
                generationJob =
                    scope.launch {
                        try {
                            val generatedBase =
                                withContext(Dispatchers.Default) {
                                    PatternEngine.generateProject(baseBitmap, generationSettings)
                                }
                            if (requestVersion != generationVersion) {
                                return@launch
                            }
                            val targetProjectId = preservedProjectId ?: generatedBase.id
                            val persistedSourcePath =
                                if (sourceBitmapPath != null && sourceBitmap === baseBitmap) {
                                    sourceBitmapPath
                                } else {
                                    withContext(Dispatchers.IO) {
                                        store.persistSourceBitmap(targetProjectId, baseBitmap)
                                    }
                                }
                            val generated =
                                generatedBase.copy(
                                    id = targetProjectId,
                                    beadsPerPack = preservedPackSize ?: generatedBase.beadsPerPack,
                                    sourceImagePath = persistedSourcePath ?: existingProject?.sourceImagePath,
                                )
                            sourceBitmap = baseBitmap
                            sourceBitmapPath = generated.sourceImagePath
                            val nextSelectedColorCode =
                                PatternEngine.materialStats(generated.cells).firstOrNull()?.code
                                    ?: paletteFallback(generated.colorScheme, generated.paletteMode)
                            selectedColorCode = nextSelectedColorCode
                            focusColorCode = nextSelectedColorCode
                            focusRow = 0
                            if (openPatternEditor) {
                                startPatternEditorSession(
                                    project = generated,
                                    sessionSettings = generationSettings,
                                    savedProject = null,
                                )
                                screen = AppScreen.PatternEditor
                                statusText = completedMessage
                            } else if (recordInHistory) {
                                pushEditorSnapshot(
                                    PatternEditorSnapshot(
                                        project = generated,
                                        settings = generationSettings,
                                    ),
                                    message = completedMessage,
                                )
                            } else {
                                applyNonHistoricalEditorSnapshot(
                                    PatternEditorSnapshot(
                                        project = generated,
                                        settings = generationSettings,
                                    ),
                                    message = completedMessage,
                                )
                            }
                        } catch (_: CancellationException) {
                            // A newer generation request replaced this one.
                        } finally {
                            if (requestVersion == generationVersion) {
                                isGenerating = false
                            }
                        }
                    }
            }

            fun launchChartRecognition(
                chartBitmap: Bitmap,
                title: String,
            ) {
                generationJob?.cancel()
                generationVersion += 1
                val requestVersion = generationVersion
                isGenerating = true
                statusText = "正在识别图纸网格和色块..."
                generationJob =
                    scope.launch {
                        try {
                            val recognizedBase =
                                withContext(Dispatchers.Default) {
                                    PatternEngine.recognizePatternChart(
                                        source = chartBitmap,
                                        settings = settings.copy(title = title),
                                        title = title,
                                        ignoreWatermark = true,
                                    )
                                }
                            if (requestVersion != generationVersion) {
                                return@launch
                            }
                            val persistedSourcePath =
                                withContext(Dispatchers.IO) {
                                    store.persistSourceBitmap(recognizedBase.id, chartBitmap)
                                }
                            val recognized = recognizedBase.copy(sourceImagePath = persistedSourcePath)
                            sourceBitmap = chartBitmap
                            sourceBitmapPath = persistedSourcePath
                            val nextSettings =
                                applyColorScheme(
                                    settings.copy(
                                        title = recognized.title,
                                        width = recognized.width,
                                        height = recognized.height,
                                    ),
                                    recognized.colorScheme,
                                )
                            settings = nextSettings
                            val nextSelectedColorCode =
                                PatternEngine.materialStats(recognized.cells).firstOrNull()?.code
                                    ?: paletteFallback(recognized.colorScheme, recognized.paletteMode)
                            selectedColorCode = nextSelectedColorCode
                            focusColorCode = nextSelectedColorCode
                            focusRow = 0
                            focusColumn = 0
                            focusColorBlockIndex = 0
                            startPatternEditorSession(
                                project = recognized,
                                sessionSettings = nextSettings,
                                savedProject = null,
                            )
                            screen = AppScreen.PatternEditor
                            statusText = "图纸已识别。可以继续修正颜色、扩展画布，或进入拼豆模式。"
                        } catch (_: CancellationException) {
                            // A newer generation request replaced this one.
                        } finally {
                            if (requestVersion == generationVersion) {
                                isGenerating = false
                            }
                        }
                    }
            }

            fun cancelRunningPatternGeneration(message: String? = null) {
                generationJob?.cancel()
                generationJob = null
                generationVersion += 1
                isGenerating = false
                if (message != null) {
                    statusText = message
                }
            }

            fun dispatchOutputLevelGeneration(
                level: Int,
                final: Boolean,
            ) {
                val baseBitmap = sourceBitmap
                if (baseBitmap == null) {
                    clearOutputLevelGenerationState()
                    statusText = "当前项目没有保留来源图片，无法按像素大小重新生成。"
                    return
                }
                val targetLevel = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
                val currentLevel = outputLevelFromSettings(settings)
                val nextSettings =
                    settingsForOutputLevel(
                        current = settings,
                        baseWidth = baseBitmap.width,
                        baseHeight = baseBitmap.height,
                        level = targetLevel,
                    )
                val targetMatchesCurrent = nextSettings.width == settings.width && nextSettings.height == settings.height
                if (targetMatchesCurrent) {
                    pendingOutputLevel = null
                    if (generationJob != null && outputRequestedLevel != null && outputRequestedLevel != currentLevel) {
                        cancelRunningPatternGeneration("已恢复当前像素大小。")
                    }
                    outputRequestedLevel = currentLevel
                    return
                }
                pendingOutputLevel = null
                outputRequestedLevel = targetLevel
                lastOutputPreviewDispatchAt = System.currentTimeMillis()
                launchPatternGeneration(
                    baseBitmap = baseBitmap,
                    existingProject = currentProject,
                    openPatternEditor = false,
                    startedMessage = if (final) "正在应用最终像素大小..." else "正在按像素大小更新图纸...",
                    completedMessage = "像素大小已更新。",
                    overrideSettings = nextSettings,
                    recordInHistory = false,
                )
            }

            fun scheduleOutputLevelPreview(level: Int) {
                val targetLevel = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
                pendingOutputLevel = targetLevel
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                val now = System.currentTimeMillis()
                val elapsed = now - lastOutputPreviewDispatchAt
                if (elapsed >= OUTPUT_PREVIEW_THROTTLE_MS) {
                    dispatchOutputLevelGeneration(targetLevel, final = false)
                    return
                }
                outputPreviewJob =
                    scope.launch {
                        delay((OUTPUT_PREVIEW_THROTTLE_MS - elapsed).coerceAtLeast(0L))
                        outputPreviewJob = null
                        val latestLevel = pendingOutputLevel ?: return@launch
                        dispatchOutputLevelGeneration(latestLevel, final = false)
                    }
            }

            fun commitOutputLevel(level: Int) {
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                dispatchOutputLevelGeneration(level, final = true)
            }

            fun applyImportedBitmap(
                bitmap: Bitmap,
                title: String,
            ) {
                sourceBitmap = bitmap
                sourceBitmapPath = null
                val importedSettings = settings.copy(title = settings.title.ifBlank { title })
                settings =
                    settingsForOutputLevel(
                        current = importedSettings,
                        baseWidth = bitmap.width,
                        baseHeight = bitmap.height,
                        level = OUTPUT_DEFAULT_SHORT_EDGE,
                    )
                statusText = "图片已导入。裁剪保持原流程，其它生成参数已经切到开源项目风格。"
            }

            var pendingCameraCaptureFile by remember { mutableStateOf<File?>(null) }
            var pendingCameraCaptureUri by remember { mutableStateOf<Uri?>(null) }

            val cameraCaptureLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                    val captureUri = pendingCameraCaptureUri
                    val captureFile = pendingCameraCaptureFile
                    pendingCameraCaptureUri = null
                    pendingCameraCaptureFile = null
                    if (!success || captureUri == null) {
                        isImportingImage = false
                        captureFile?.delete()
                        statusText = "已取消相机导入。"
                        return@rememberLauncherForActivityResult
                    }
                    statusText = "正在导入相机图片..."
                    scope.launch {
                        val bitmap =
                            withContext(Dispatchers.IO) {
                                loadBitmapFromUri(context, captureUri)
                            }
                        isImportingImage = false
                        captureFile?.delete()
                        if (bitmap == null) {
                            statusText = "相机图片导入失败，请重试。"
                            return@launch
                        }
                        applyImportedBitmap(bitmap, "相机照片 ${LocalDate.now()}")
                    }
                }

            fun openCameraImport() {
                val captureFile =
                    runCatching {
                        File.createTempFile("camera-import-", ".jpg", context.cacheDir)
                    }.getOrNull()
                if (captureFile == null) {
                    statusText = "无法创建相机缓存文件。"
                    return
                }
                val captureUri =
                    runCatching {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", captureFile)
                    }.getOrNull()
                if (captureUri == null) {
                    captureFile.delete()
                    statusText = "无法打开相机导入。"
                    return
                }
                pendingCameraCaptureFile?.delete()
                pendingCameraCaptureFile = captureFile
                pendingCameraCaptureUri = captureUri
                isImportingImage = true
                statusText = "正在打开相机..."
                cameraCaptureLauncher.launch(captureUri)
            }

            val imagePicker =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri == null) {
                        statusText = "已取消选择图片。"
                        return@rememberLauncherForActivityResult
                    }
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    isImportingImage = true
                    statusText = "正在导入图片..."
                    scope.launch {
                        val bitmap =
                            withContext(Dispatchers.IO) {
                                loadBitmapFromUri(context, uri)
                            }

                        isImportingImage = false
                        if (bitmap == null) {
                            statusText = "图片导入失败，请换一张图片再试。"
                            return@launch
                        }
                        val title =
                            uri.lastPathSegment
                                ?.substringAfterLast('/')
                                ?.substringBeforeLast('.')
                                ?.ifBlank { settings.title }
                                ?: settings.title
                        applyImportedBitmap(bitmap, title)
                    }
                }

            val isBoardViewportRestoring = pendingBoardViewportRestore != null || isRestoringBoardViewport

            BackHandler(enabled = screen == AppScreen.PatternEditor && !showEditorExitDialog) {
                requestClosePatternEditor()
            }

            BackHandler(enabled = screen == AppScreen.BuildMode) {
                closeBuildModeToHome()
            }

            if (showEditorExitDialog) {
                AlertDialog(
                    onDismissRequest = { showEditorExitDialog = false },
                    title = { Text("退出图纸编辑模式？") },
                    text = { Text("是否保存当前修改？") },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { showEditorExitDialog = false }) {
                                Text("继续编辑")
                            }
                            TextButton(onClick = { closePatternEditor(saveBeforeExit = false) }) {
                                Text("不保存")
                            }
                            Button(
                                onClick = { closePatternEditor(saveBeforeExit = true) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text("保存并退出", maxLines = 1)
                            }
                        }
                    },
                )
            }

            pendingDeleteProject?.let { project ->
                AlertDialog(
                    onDismissRequest = { pendingDeleteProject = null },
                    title = { Text("删除项目？") },
                    text = { Text("确定删除“${project.title}”吗？删除后无法恢复。") },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteProject = null }) {
                            Text("取消")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = ::confirmPendingProjectDeletion,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("删除")
                        }
                    },
                )
            }

            protectedProjectDeleteNotice?.let {
                AlertDialog(
                    onDismissRequest = { protectedProjectDeleteNotice = null },
                    title = { Text("示例项目不可删除") },
                    confirmButton = {
                        TextButton(onClick = { protectedProjectDeleteNotice = null }) {
                            Text("知道了")
                        }
                    },
                )
            }

            pendingRenameProject?.let {
                AlertDialog(
                    onDismissRequest = {
                        pendingRenameProject = null
                        renameDraftTitle = ""
                    },
                    title = { Text("重命名项目") },
                    text = {
                        OutlinedTextField(
                            value = renameDraftTitle,
                            onValueChange = { renameDraftTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("项目名称") },
                        )
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingRenameProject = null
                                renameDraftTitle = ""
                            },
                        ) {
                            Text("取消")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = ::confirmPendingProjectRename,
                            enabled = renameDraftTitle.trim().isNotEmpty(),
                        ) {
                            Text("保存")
                        }
                    },
                )
            }

            protectedProjectRenameNotice?.let {
                AlertDialog(
                    onDismissRequest = { protectedProjectRenameNotice = null },
                    title = { Text("示例项目不可重命名") },
                    confirmButton = {
                        TextButton(onClick = { protectedProjectRenameNotice = null }) {
                            Text("知道了")
                        }
                    },
                )
            }

            when (screen) {
                AppScreen.Home ->
                    Scaffold(
                        bottomBar =
                            if (mainTab == MainTab.Settings && (settingsPage != SettingsPage.Home || isSettingsPickerOpen)) {
                                {}
                            } else {
                                {
                                    MainBottomBar(
                                        selectedTab = mainTab,
                                        onSelect = ::selectMainTab,
                                    )
                                }
                            },
                    ) { innerPadding ->
                        when (mainTab) {
                            MainTab.Projects ->
                                HomeScreen(
                                    projects = projects,
                                    extraBottomPadding = innerPadding.calculateBottomPadding(),
                                    onOpen = ::openProject,
                                    onRename = ::requestProjectRename,
                                    onDelete = ::requestProjectDeletion,
                                )

                            MainTab.Create ->
                                CreateHubScreen(
                                    extraBottomPadding = innerPadding.calculateBottomPadding(),
                                    onImageToPattern = ::startImageToPatternFlow,
                                    onChartRecognition = ::startChartRecognitionFlow,
                                )

                            MainTab.Settings ->
                                SettingsScreen(
                                    currentPage = settingsPage,
                                    onPageChange = { settingsPage = it },
                                    onPickerOpenChange = { isSettingsPickerOpen = it },
                                    extraBottomPadding = innerPadding.calculateBottomPadding(),
                                    themes = AppThemeRegistry.options,
                                    selectedThemeId = selectedThemeId,
                                    onSelectTheme = { option ->
                                        selectedThemeId = themeManager.applyTheme(option.id)
                                        statusText = "主题色已切换为 ${option.label}。"
                                        Toast.makeText(context, "已切换主题色：${option.label}", Toast.LENGTH_SHORT).show()
                                    },
                                    appIcons = AppIconRegistry.options,
                                    selectedIconId = selectedAppIconId,
                                    onSelectIcon = { option ->
                                        val switched = appIconManager.applyIcon(option.id)
                                        if (switched) {
                                            selectedAppIconId = option.id
                                            val syncedProjects = store.syncDemoProject(projects, option)
                                            saveProjects(syncedProjects)
                                            val syncedDemoProject = syncedProjects.firstOrNull { it.id == PatternEngine.DEMO_PROJECT_ID }
                                            if (currentProject?.id == PatternEngine.DEMO_PROJECT_ID && syncedDemoProject != null) {
                                                currentProject = syncedDemoProject
                                                if (editorLastSavedProject?.id == PatternEngine.DEMO_PROJECT_ID) {
                                                    editorLastSavedProject = syncedDemoProject
                                                }
                                                pendingBoardViewportRestore = centeredBoardViewport()
                                            }
                                            statusText = "应用图标已切换为 ${option.label}。"
                                            Toast.makeText(context, "已切换图标：${option.label}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            statusText = "应用图标切换失败，请重试。"
                                            Toast.makeText(context, "图标切换失败，请重试。", Toast.LENGTH_SHORT).show()
                                        }
                                        switched
                                    },
                                )
                        }
                    }

                AppScreen.ImageEditor ->
                    ImageEditorScreen(
                        statusText = statusText,
                        settings = settings,
                        sourceBitmap = sourceBitmap,
                        flowKind = createFlowKind,
                        isImportingImage = isImportingImage,
                        isGenerating = isGenerating,
                        onBack = {
                            sourceBitmap = null
                            sourceBitmapPath = null
                            isImportingImage = false
                            mainTab = MainTab.Create
                            screen = AppScreen.Home
                            statusText = "已回到创建页。"
                        },
                        onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                        onPickFromCamera = ::openCameraImport,
                        onSettingsChange = { settings = it },
                        onApplyCrop = { croppedBitmap ->
                            sourceBitmap = croppedBitmap
                            sourceBitmapPath = null
                            settings =
                                settingsForOutputLevel(
                                    current = settings,
                                    baseWidth = croppedBitmap.width,
                                    baseHeight = croppedBitmap.height,
                                    level = outputLevelFromSettings(settings),
                                )
                            statusText = "裁剪已应用。现在可以继续调整开源生成参数。"
                        },
                        onGenerate = { croppedBitmap ->
                            if (createFlowKind == CreateFlowKind.ChartRecognition) {
                                val title = settings.title.ifBlank { nextProjectDefaultTitle(projects) }
                                launchChartRecognition(
                                    chartBitmap = croppedBitmap,
                                    title = title,
                                )
                            } else {
                                val generationSettings =
                                    settingsForOutputLevel(
                                        current = settings,
                                        baseWidth = croppedBitmap.width,
                                        baseHeight = croppedBitmap.height,
                                        level = outputLevelFromSettings(settings),
                                    )
                                settings = generationSettings
                                launchPatternGeneration(
                                    baseBitmap = croppedBitmap,
                                    existingProject = null,
                                    openPatternEditor = true,
                                    overrideSettings = generationSettings,
                                    startedMessage = "正在按开源逻辑生成拼豆图纸...",
                                    completedMessage = "图纸已生成。可以继续做填充、移除、颜色替换、镜像等编辑。",
                                )
                            }
                        },
                    )

                AppScreen.PatternEditor -> {
                    val project = currentProject
                    if (project == null) {
                        HomeScreen(
                            projects = projects,
                            extraBottomPadding = 0.dp,
                            onOpen = ::openProject,
                            onRename = ::requestProjectRename,
                            onDelete = ::requestProjectDeletion,
                        )
                    } else {
                        val materialStats = PatternEngine.materialStats(project.cells)
                        val usageSummary = remember(project.id, project.updatedAt) { UsageAnalyzer.summarize(project) }
                        PatternEditorScreen(
                            project = project,
                            settings = settings,
                            hasSourceBitmap = sourceBitmap != null && project.sourceKind == ProjectSourceKind.ImageConversion,
                            sourceBitmapWidth = sourceBitmap?.width,
                            sourceBitmapHeight = sourceBitmap?.height,
                            isGenerating = isGenerating,
                            selectedColorCode = selectedColorCode,
                            toolMode = toolMode,
                            removeMode = removeMode,
                            mirrorDirection = mirrorDirection,
                            boardZoom = boardZoom,
                            boardHorizontalScroll = boardHorizontalScroll,
                            boardVerticalScroll = boardVerticalScroll,
                            isBoardViewportRestoring = isBoardViewportRestoring,
                            usageSummary = usageSummary,
                            canUndo = editorUndoStack.size > 1,
                            canRedo = editorRedoStack.isNotEmpty(),
                            onBack = ::requestClosePatternEditor,
                            onUndo = ::undoPatternEdit,
                            onRedo = ::redoPatternEdit,
                            onSave = { saveCurrentEditorProject("图纸已保存。", resumeScreen = AppScreen.PatternEditor) },
                            onToolModeChange = { toolMode = it },
                            onRemoveModeChange = { removeMode = it },
                            onMirrorApply = { direction ->
                                mirrorDirection = direction
                                updateCurrentProject("已${mirrorDirectionLabel(direction)}。") {
                                    val nextCells =
                                        EditorTools.mirrorWholeProject(
                                            cells = it.cells,
                                            width = it.width,
                                            height = it.height,
                                            direction = direction,
                                        )
                                    it.copy(cells = nextCells)
                                }
                            },
                            onRotateApply = { direction ->
                                updateCurrentProjectGrid("已${rotationDirectionLabel(direction)}。") {
                                    EditorTools.rotateWholeProject(
                                        cells = it.cells,
                                        width = it.width,
                                        height = it.height,
                                        direction = direction,
                                    )
                                }
                            },
                            onCanvasExpandApply = { direction ->
                                updateCurrentProjectGrid(canvasExpandDirectionActionText(direction)) {
                                    EditorTools.expandCanvas(
                                        cells = it.cells,
                                        width = it.width,
                                        height = it.height,
                                        direction = direction,
                                    )
                                }
                            },
                            onOutputLevelPreviewChange = ::scheduleOutputLevelPreview,
                            onOutputLevelCommit = ::commitOutputLevel,
                            /* onOutputLevelChange = { outputLevel ->
                                val baseBitmap = sourceBitmap
                                if (baseBitmap == null) {
                                    statusText = "当前项目没有保留来源图片，无法按像素大小实时重生成。"
                                    return@PatternEditorScreen
                                }
                                val nextSettings =
                                    settingsForOutputLevel(
                                        current = settings,
                                        sourceWidth = baseBitmap.width,
                                        sourceHeight = baseBitmap.height,
                                        level = outputLevel,
                                    )
                                if (nextSettings.width == settings.width && nextSettings.height == settings.height) {
                                    return@PatternEditorScreen
                                }
                                launchPatternGeneration(
                                    baseBitmap = baseBitmap,
                                    existingProject = project,
                                    openPatternEditor = false,
                                    startedMessage = "正在按新的像素大小重生成图纸...",
                                    completedMessage = "像素大小已更新。",
                                    overrideSettings = nextSettings,
                                    recordInHistory = false,
                                )
                            },*/
                            onColorSchemeSelect = { preset ->
                                val nextSettings = applyColorScheme(settings, preset)
                                if (nextSettings.colorScheme == settings.colorScheme) {
                                    return@PatternEditorScreen
                                }
                                val baseBitmap = sourceBitmap
                                if (baseBitmap == null) {
                                    statusText = "当前项目没有保留来源图片，无法切换到 ${preset.label} 并重生成图纸。"
                                    return@PatternEditorScreen
                                }
                                launchPatternGeneration(
                                    baseBitmap = baseBitmap,
                                    existingProject = project,
                                    openPatternEditor = false,
                                    startedMessage = "正在切换到 ${preset.label} 方案...",
                                    completedMessage = "已切换到 ${preset.label} 方案。",
                                    overrideSettings = nextSettings,
                                )
                            },
                            onSelectedColorChange = {
                                selectedColorCode = it
                                focusColorCode = it
                            },
                            onBoardZoomChange = { boardZoom = it },
                            onCellTap = { index ->
                                val tappedCode = project.cells.getOrNull(index)
                                when (toolMode) {
                                    ToolMode.Pencil ->
                                        updateCurrentProjectCells {
                                            PatternEngine.updateCell(it.cells, index, selectedColorCode)
                                        }

                                    ToolMode.Eraser ->
                                        updateCurrentProjectCells {
                                            PatternEngine.updateCell(it.cells, index, null)
                                        }

                                    ToolMode.Fill ->
                                        updateCurrentProjectCells("已完成填充。") {
                                            EditorTools.floodFill(
                                                cells = it.cells,
                                                width = it.width,
                                                height = it.height,
                                                startIndex = index,
                                                replacement = selectedColorCode,
                                            )
                                        }

                                    ToolMode.Remove -> {
                                        val removalMessage =
                                            when (removeMode) {
                                                RemoveMode.SameConnected -> "已移除连通同色区域。"
                                                RemoveMode.AllSameColor -> "已移除整张图里的同色珠。"
                                                RemoveMode.Connected -> "已移除连通占用区域。"
                                            }
                                        updateCurrentProjectCells(removalMessage) {
                                            val indices =
                                                when (removeMode) {
                                                    RemoveMode.SameConnected ->
                                                        EditorTools.collectConnectedCellIndices(
                                                            cells = it.cells,
                                                            width = it.width,
                                                            height = it.height,
                                                            startIndex = index,
                                                        )

                                                    RemoveMode.AllSameColor ->
                                                        EditorTools.collectSameColorIndices(
                                                            cells = it.cells,
                                                            startIndex = index,
                                                        )

                                                    RemoveMode.Connected ->
                                                        EditorTools.collectConnectedOccupiedIndices(
                                                            cells = it.cells,
                                                            width = it.width,
                                                            height = it.height,
                                                            startIndex = index,
                                                        )
                                                }
                                            EditorTools.clearIndices(it.cells, indices)
                                        }
                                    }

                                    ToolMode.Recolor -> {
                                        if (tappedCode == null) {
                                            return@PatternEditorScreen
                                        }
                                        updateCurrentProjectCells(
                                            "已将 ${colorCodeLabel(tappedCode)} 替换为 ${colorCodeLabel(selectedColorCode)}。",
                                        ) {
                                            EditorTools.recolorAll(
                                                cells = it.cells,
                                                sourceColorCode = tappedCode,
                                                replacementColorCode = selectedColorCode,
                                            )
                                        }
                                    }

                                    ToolMode.Picker -> {
                                        if (tappedCode != null) {
                                            selectedColorCode = tappedCode
                                            focusColorCode = tappedCode
                                            statusText = "已吸取颜色 ${colorCodeLabel(tappedCode)}。"
                                        }
                                    }

                                    ToolMode.Mirror -> {
                                        if (tappedCode == null) {
                                            return@PatternEditorScreen
                                        }
                                        updateCurrentProjectCells("已镜像当前连通区域。") {
                                            val selection =
                                                EditorTools.collectConnectedOccupiedIndices(
                                                    cells = it.cells,
                                                    width = it.width,
                                                    height = it.height,
                                                    startIndex = index,
                                                )
                                            EditorTools.mirrorSelection(
                                                cells = it.cells,
                                                width = it.width,
                                                height = it.height,
                                                direction = mirrorDirection,
                                                selectionIndices = selection,
                                            )
                                        }
                                    }
                                }
                            },
                            onMirrorProject = {
                                updateCurrentProjectCells("已镜像整张图纸。") {
                                    EditorTools.mirrorWholeProject(
                                        cells = it.cells,
                                        width = it.width,
                                        height = it.height,
                                        direction = mirrorDirection,
                                    )
                                }
                            },
                            onEnterBuildMode = {
                                saveCurrentEditorProject(resumeScreen = AppScreen.BuildMode)
                                focusColorCode = materialStats.firstOrNull()?.code ?: selectedColorCode
                                focusRow = 0
                                focusColumn = 0
                                focusColorBlockIndex = 0
                                buildMode = store.loadProjectBuildMode(project.id)
                                screen = AppScreen.BuildMode
                                statusText = "图纸已保存，并已进入拼豆模式。可以按颜色、颜色区块、按行或按列查看。"
                            },
                        )
                    }
                }

                AppScreen.BuildMode -> {
                    val project = currentProject
                    if (project == null) {
                        HomeScreen(
                            projects = projects,
                            extraBottomPadding = 0.dp,
                            onOpen = ::openProject,
                            onRename = ::requestProjectRename,
                            onDelete = ::requestProjectDeletion,
                        )
                    } else {
                        val materialStats = PatternEngine.materialStats(project.cells)
                        BuildModeScreen(
                            project = project,
                            buildMode = buildMode,
                            focusRow = focusRow,
                            focusColumn = focusColumn,
                            focusColorCode = focusColorCode ?: materialStats.firstOrNull()?.code,
                            focusColorBlockIndex = focusColorBlockIndex,
                            boardZoom = boardZoom,
                            boardHorizontalScroll = boardHorizontalScroll,
                            boardVerticalScroll = boardVerticalScroll,
                            isBoardViewportRestoring = isBoardViewportRestoring,
                            completedCellIndices = buildCompletedCellIndices,
                            materialStats = materialStats,
                            onBack = ::closeBuildModeToHome,
                            onEnterPatternEditor = ::openPatternEditorFromBuildMode,
                            onBuildModeChange = {
                                val focusWindow = computeBoardDisplayWindow(project = project, trimOuterEmpty = true)
                                buildMode = it
                                store.saveProjectBuildMode(project.id, it)
                                when (it) {
                                    BuildMode.Color -> {
                                        focusColorCode = focusColorCode ?: materialStats.firstOrNull()?.code ?: selectedColorCode
                                    }
                                    BuildMode.ColorBlock -> {
                                        focusColorCode = focusColorCode ?: materialStats.firstOrNull()?.code ?: selectedColorCode
                                        focusColorBlockIndex = 0
                                    }
                                    BuildMode.Row -> {
                                        focusRow = focusWindow.coerceRowIndex(focusRow)
                                    }
                                    BuildMode.Column -> {
                                        focusColumn = focusWindow.coerceColumnIndex(focusColumn)
                                    }
                                }
                            },
                            onFocusRowChange = { focusRow = it },
                            onFocusColumnChange = { focusColumn = it },
                            onFocusColorChange = {
                                focusColorCode = it
                                focusColorBlockIndex = 0
                            },
                            onFocusColorBlockIndexChange = { focusColorBlockIndex = it },
                            onToggleCompletedSelection = ::toggleCompletedBuildSelection,
                            onBoardZoomChange = { boardZoom = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    projects: List<BeadProject>,
    extraBottomPadding: Dp,
    onOpen: (BeadProject) -> Unit,
    onRename: (BeadProject) -> Unit,
    onDelete: (BeadProject) -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar("项目")
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onOpen = { onOpen(project) },
                    onRename = { onRename(project) },
                    onDelete = { onDelete(project) },
                )
            }
        }
    }
}

@Composable
private fun SectionTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
        },
        title = {
            Text(
                title,
                modifier = Modifier.offset(y = (-4).dp),
            )
        },
    )
}

@Composable
private fun MainBottomBar(
    selectedTab: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.Projects,
            onClick = { onSelect(MainTab.Projects) },
            icon = { Icon(Icons.Outlined.GridView, contentDescription = null) },
            label = { Text("项目") },
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.Create,
            onClick = { onSelect(MainTab.Create) },
            icon = { Icon(Icons.Outlined.AutoFixHigh, contentDescription = null) },
            label = { Text("创建") },
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.Settings,
            onClick = { onSelect(MainTab.Settings) },
            icon = { Icon(Icons.Outlined.SettingsSuggest, contentDescription = null) },
            label = { Text("设置") },
        )
    }
}

@Composable
private fun CreateHubScreen(
    extraBottomPadding: Dp,
    onImageToPattern: () -> Unit,
    onChartRecognition: () -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar("创建")
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 20.dp,
                    ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Button(
                onClick = onImageToPattern,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("图片转图纸")
            }
            FilledTonalButton(
                onClick = onChartRecognition,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("图纸识别")
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    currentPage: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    onPickerOpenChange: (Boolean) -> Unit,
    extraBottomPadding: Dp,
    themes: List<AppThemeOption>,
    selectedThemeId: String,
    onSelectTheme: (AppThemeOption) -> Unit,
    appIcons: List<AppIconOption>,
    selectedIconId: String,
    onSelectIcon: (AppIconOption) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showThemePicker by remember { mutableStateOf(false) }
    var pendingThemeId by remember(selectedThemeId) { mutableStateOf(selectedThemeId) }
    var showAppIconPicker by remember { mutableStateOf(false) }
    var pendingIconId by remember(selectedIconId) { mutableStateOf(selectedIconId) }
    var updateCheckState by remember { mutableStateOf<UpdateCheckUiState>(UpdateCheckUiState.Idle) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val selectedTheme = themes.firstOrNull { it.id == selectedThemeId } ?: AppThemeRegistry.defaultOption
    val selectedOption = appIcons.firstOrNull { it.id == selectedIconId } ?: AppIconRegistry.defaultOption
    val currentVersion = remember(context) { currentAppVersion(context) }
    val currentVersionLabel = currentVersion.displayLabel

    DisposableEffect(showThemePicker, showAppIconPicker) {
        onPickerOpenChange(showThemePicker || showAppIconPicker)
        onDispose { onPickerOpenChange(false) }
    }

    BackHandler(enabled = currentPage != SettingsPage.Home) {
        onPageChange(SettingsPage.Home)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                SectionTopBar(
                    title = if (currentPage == SettingsPage.Home) "设置" else "颜色置信度",
                    onBack =
                        if (currentPage == SettingsPage.Home) {
                            null
                        } else {
                            { onPageChange(SettingsPage.Home) }
                        },
                )
            },
        ) { innerPadding ->
            when (currentPage) {
                SettingsPage.Home ->
                    SettingsHomePage(
                        innerPadding = innerPadding,
                        extraBottomPadding = extraBottomPadding,
                        selectedTheme = selectedTheme,
                        onOpenThemePicker = {
                            pendingThemeId = selectedThemeId
                            showThemePicker = true
                        },
                        selectedOption = selectedOption,
                        appIcons = appIcons,
                        onOpenAppIconPicker = {
                            pendingIconId = selectedIconId
                            showAppIconPicker = true
                        },
                        currentVersionLabel = currentVersionLabel,
                        updateCheckState = updateCheckState,
                        onCheckUpdate = {
                            if (updateCheckState != UpdateCheckUiState.Checking) {
                                updateCheckState = UpdateCheckUiState.Checking
                                scope.launch {
                                    when (val result = AppUpdateChecker.check(currentVersion)) {
                                        is UpdateCheckResult.UpdateAvailable -> {
                                            availableUpdate = result.update
                                            updateCheckState =
                                                UpdateCheckUiState.Checked(
                                                    versionName = result.update.versionName,
                                                    sourceUrl = result.update.sourceUrl,
                                                )
                                        }

                                        is UpdateCheckResult.UpToDate -> {
                                            updateCheckState =
                                                UpdateCheckUiState.Checked(
                                                    versionName = result.versionName,
                                                    sourceUrl = result.sourceUrl,
                                                )
                                            Toast
                                                .makeText(
                                                    context,
                                                    "当前已是最新版本：${result.versionName}",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }

                                        is UpdateCheckResult.Failed -> {
                                            updateCheckState = UpdateCheckUiState.Failed(result.message)
                                            Toast
                                                .makeText(context, "检查更新失败：${result.message}", Toast.LENGTH_LONG)
                                                .show()
                                        }
                                    }
                                }
                            }
                        },
                        onOpenColorConfidence = {
                            onPageChange(SettingsPage.ColorConfidence)
                        },
                    )

                SettingsPage.ColorConfidence ->
                    ColorConfidenceSettingsPage(
                        innerPadding = innerPadding,
                        extraBottomPadding = extraBottomPadding,
                    )
            }
        }

        availableUpdate?.let { update ->
            AlertDialog(
                onDismissRequest = { availableUpdate = null },
                title = { Text(update.title.ifBlank { "发现新版本" }) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("当前版本：$currentVersionLabel")
                        Text("最新版本：${update.versionName} (${update.versionCode})")
                        if (update.publishedAt.isNotBlank()) {
                            Text("发布时间：${update.publishedAt}")
                        }
                        if (update.summary.isNotBlank()) {
                            Text(update.summary)
                        }
                        if (update.changelog.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                update.changelog.take(5).forEach { item ->
                                    Text("• $item", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (update.password.isNotBlank()) {
                            Text("提取码：${update.password}", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            update.downloadUrl.ifBlank { "暂无下载链接" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { availableUpdate = null }) {
                        Text(if (update.forceUpdate) "稍后处理" else "稍后")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val opened = openExternalBrowser(context, update.downloadUrl)
                            if (!opened) {
                                Toast.makeText(context, "无法打开浏览器，请手动复制链接。", Toast.LENGTH_LONG).show()
                            } else {
                                availableUpdate = null
                            }
                        },
                        enabled = update.downloadUrl.isNotBlank(),
                    ) {
                        Text("打开浏览器")
                    }
                },
            )
        }

        if (showThemePicker) {
            BottomPickerOverlay(
                onDismissRequest = { showThemePicker = false },
                heightFraction = 0.58f,
                bottomReservedPadding = extraBottomPadding,
                confirmEnabled = pendingThemeId != selectedThemeId,
                onConfirm = {
                    val pendingOption = themes.firstOrNull { it.id == pendingThemeId } ?: AppThemeRegistry.defaultOption
                    onSelectTheme(pendingOption)
                    showThemePicker = false
                },
            ) { gridModifier ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    modifier = gridModifier,
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItems(
                        items = themes,
                        key = { option -> option.id },
                    ) { option ->
                        AppThemeOptionCard(
                            option = option,
                            selected = option.id == pendingThemeId,
                            onClick = {
                                pendingThemeId = option.id
                            },
                        )
                    }
                }
            }
        }

        if (showAppIconPicker) {
            BottomPickerOverlay(
                onDismissRequest = { showAppIconPicker = false },
                heightFraction = 0.72f,
                bottomReservedPadding = extraBottomPadding,
                confirmEnabled = pendingIconId != selectedIconId,
                onConfirm = {
                    val pendingOption = appIcons.firstOrNull { it.id == pendingIconId } ?: AppIconRegistry.defaultOption
                    if (onSelectIcon(pendingOption)) {
                        showAppIconPicker = false
                    }
                },
            ) { gridModifier ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    modifier = gridModifier,
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItems(
                        items = appIcons,
                        key = { option -> option.id },
                    ) { option ->
                        AppIconOptionCard(
                            option = option,
                            selected = option.id == pendingIconId,
                            onClick = {
                                pendingIconId = option.id
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomPickerOverlay(
    onDismissRequest: () -> Unit,
    heightFraction: Float,
    bottomReservedPadding: Dp,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BackHandler(onBack = onDismissRequest)
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismissRequest,
                ),
    ) {
        val navigationBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomInset = maxOf(bottomReservedPadding, navigationBottomInset)
        val availableHeight = (maxHeight - bottomInset).coerceAtLeast(0.dp)
        val preferredHeight = maxHeight * heightFraction.coerceIn(0.35f, 0.92f)
        val panelHeight = minOf(availableHeight, maxOf(160.dp, preferredHeight))

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset)
                    .fillMaxWidth()
                    .height(panelHeight)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {},
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 14.dp),
            ) {
                content(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Button(
                    onClick = onConfirm,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    enabled = confirmEnabled,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                ) {
                    Text("确认更换")
                }
            }
        }
    }
}

@Composable
private fun SettingsHomePage(
    innerPadding: PaddingValues,
    extraBottomPadding: Dp,
    selectedTheme: AppThemeOption,
    onOpenThemePicker: () -> Unit,
    selectedOption: AppIconOption,
    appIcons: List<AppIconOption>,
    onOpenAppIconPicker: () -> Unit,
    currentVersionLabel: String,
    updateCheckState: UpdateCheckUiState,
    onCheckUpdate: () -> Unit,
    onOpenColorConfidence: () -> Unit,
) {
    val updateSummary =
        when (updateCheckState) {
            UpdateCheckUiState.Idle -> "当前：$currentVersionLabel · 点击检查新版本"
            UpdateCheckUiState.Checking -> "正在读取版本信息，请稍候"
            is UpdateCheckUiState.Checked -> "当前：$currentVersionLabel · 最新：${updateCheckState.versionName}"
            is UpdateCheckUiState.Failed -> "检查失败：${updateCheckState.message}"
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 20.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsEntryCard(
            title = "更改主题色",
            summary = "当前：${selectedTheme.label} · 统一整体界面主色",
            icon = Icons.Outlined.FormatColorFill,
            onClick = onOpenThemePicker,
        )
        SettingsEntryCard(
            title = "切换软件图标",
            summary = "当前：${selectedOption.label} · 共 ${appIcons.size} 个内置图标",
            icon = Icons.Outlined.Palette,
            onClick = onOpenAppIconPicker,
        )
        SettingsEntryCard(
            title = "版本更新",
            summary = updateSummary,
            icon = Icons.Outlined.Refresh,
            onClick = onCheckUpdate,
        )
        SettingsEntryCard(
            title = "颜色置信度",
            summary = "按品牌 x 颜色方案查看置信度矩阵和参考来源",
            icon = Icons.Outlined.GridView,
            onClick = onOpenColorConfidence,
        )
    }
}

@Composable
private fun ColorConfidenceSettingsPage(
    innerPadding: PaddingValues,
    extraBottomPadding: Dp,
) {
    val legendEntries =
        listOf(
            ConfidenceLegendEntry("极高", "0.95 - 1.00", 0.98),
            ConfidenceLegendEntry("高", "0.85 - 0.949", 0.89),
            ConfidenceLegendEntry("中高", "0.78 - 0.849", 0.82),
            ConfidenceLegendEntry("中", "0.70 - 0.779", 0.73),
            ConfidenceLegendEntry("偏低", "0.55 - 0.699", 0.63),
            ConfidenceLegendEntry("低", "< 0.55", 0.42),
            ConfidenceLegendEntry("未收录", "无数据", null),
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 20.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                                    ),
                                ),
                        ).padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "颜色置信度矩阵",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "图例",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    legendEntries.forEach { entry ->
                        ConfidenceLegendSwatch(entry = entry)
                    }
                }
            }
        }

        ColorConfidenceMatrixCard()
        ColorConfidenceSourcesCard()
    }
}

private data class ConfidenceLegendEntry(
    val title: String,
    val rangeLabel: String,
    val sampleScore: Double?,
)

@Composable
private fun ConfidenceLegendSwatch(
    entry: ConfidenceLegendEntry,
) {
    val swatchColor = confidenceCellColor(entry.sampleScore)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.width(104.dp).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(26.dp),
                color = swatchColor,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, swatchColor.copy(alpha = 0.88f)),
            ) {}
            Text(entry.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                entry.rangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorConfidenceMatrixCard() {
    val brands = ColorConfidenceData.brands
    val rows = ColorConfidenceData.variantRows
    val brandCellWidth = 78.dp
    val schemeLabelWidth = 74.dp

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "品牌 x 颜色方案",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
                        .padding(12.dp),
            ) {
                Column(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConfidenceSchemeHeaderCell(width = schemeLabelWidth)
                        brands.forEach { brandMatrix ->
                            ConfidenceBrandHeaderCell(
                                brand = brandMatrix.brand,
                                width = brandCellWidth,
                            )
                        }
                    }
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConfidenceSchemeLabelCell(
                                label = row.label,
                                width = schemeLabelWidth,
                            )
                            brands.forEach { brandMatrix ->
                                ConfidenceScoreCell(
                                    score = brandMatrix.schemeScores[row.key],
                                    width = brandCellWidth,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfidenceSchemeHeaderCell(width: Dp) {
    Surface(
        modifier = Modifier.width(width).height(68.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "方案",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "颜色数",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConfidenceBrandHeaderCell(
    brand: ColorSchemeBrand,
    width: Dp,
) {
    Surface(
        modifier = Modifier.width(width).height(68.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                brand.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                formatConfidencePercent(brand.confidenceScore),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ConfidenceSchemeLabelCell(
    label: String,
    width: Dp,
) {
    Surface(
        modifier = Modifier.width(width).height(42.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ConfidenceScoreCell(
    score: Double?,
    width: Dp,
) {
    val backgroundColor = confidenceCellColor(score)

    Surface(
        modifier = Modifier.width(width).height(42.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, backgroundColor.copy(alpha = 0.88f)),
    ) {}
}

@Composable
private fun ColorConfidenceSourcesCard() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "参考来源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "下面是这份置信度表主要参考的来源，以及它们各自承担的核对职责。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ColorConfidenceData.sources.forEach { source ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            source.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            source.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatConfidencePercent(score: Double): String {
    val scaled = ((score * 1000).roundToInt()) / 10.0
    return if (scaled % 1.0 == 0.0) {
        "${scaled.toInt()}%"
    } else {
        "${scaled}%"
    }
}

private fun confidenceCellColor(score: Double?): Color {
    if (score == null) {
        return Color(0xFFEAE3DA)
    }

    return when {
        score >= ColorConfidenceData.highConfidenceMin -> {
            val progress =
                ((score - ColorConfidenceData.highConfidenceMin) / (1.0 - ColorConfidenceData.highConfidenceMin))
                    .coerceIn(0.0, 1.0)
                    .toFloat()
            lerp(Color(0xFFD4F0D9), Color(0xFF4AA66C), progress)
        }

        score >= ColorConfidenceData.mediumConfidenceMin -> {
            val progress =
                ((score - ColorConfidenceData.mediumConfidenceMin) /
                    (ColorConfidenceData.highConfidenceMin - ColorConfidenceData.mediumConfidenceMin))
                    .coerceIn(0.0, 1.0)
                    .toFloat()
            lerp(Color(0xFFF8E9B0), Color(0xFFE3B23B), progress)
        }

        else -> {
            val progress =
                (score / ColorConfidenceData.mediumConfidenceMin)
                    .coerceIn(0.0, 1.0)
                    .toFloat()
            lerp(Color(0xFFF8D8D1), Color(0xFFD96557), progress)
        }
    }
}

@Composable
private fun SettingsEntryCard(
    title: String,
    summary: String,
    icon: ImageVector = Icons.Outlined.Palette,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AppIconOptionCard(
    option: AppIconOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(option.previewResId),
                        contentDescription = option.label,
                        modifier = Modifier.size(54.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Text(
                option.label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AppThemeOptionCard(
    option: AppThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(18.dp),
                color = option.previewColor.copy(alpha = 0.18f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(option.previewColor),
                    )
                }
            }
            Text(
                option.label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ImageEditorScreen(
    statusText: String,
    settings: GenerationSettings,
    sourceBitmap: Bitmap?,
    flowKind: CreateFlowKind,
    isImportingImage: Boolean,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    onPickFromCamera: () -> Unit,
    onSettingsChange: (GenerationSettings) -> Unit,
    onApplyCrop: (Bitmap) -> Unit,
    onGenerate: (Bitmap) -> Unit,
    showBackButton: Boolean = true,
    extraBottomPadding: Dp = 0.dp,
) {
    var activeToolPanel by remember(sourceBitmap) {
        mutableStateOf<ImageToolPanel?>(null)
    }
    var cropAspectPreset by remember(sourceBitmap) { mutableStateOf(CropAspectPreset.Custom) }
    var cropImageView by remember(sourceBitmap) { mutableStateOf<CropImageView?>(null) }
    val lockCropAspectRatio = cropAspectPreset != CropAspectPreset.Custom
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = if (extraBottomPadding > navigationBottomInset) extraBottomPadding else navigationBottomInset

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1514)),
    ) {
        if (sourceBitmap == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Outlined.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("选择一张图片开始编辑", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = onPickImage, enabled = !isImportingImage) {
                            Text(if (isImportingImage) "正在导入..." else "选择本地图片")
                        }
                        FilledTonalButton(
                            onClick = onPickFromCamera,
                            enabled = !isImportingImage,
                        ) {
                            Text("从相机导入")
                        }
                    }
                }
            }
        } else {
            ImageEditorViewport(
                bitmap = sourceBitmap,
                settings = settings,
                showCropOverlay = activeToolPanel == ImageToolPanel.Crop,
                lockCropAspectRatio = lockCropAspectRatio,
                onCropperReady = { cropImageView = it },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, top = topInset + 12.dp, bottom = bottomInset + 12.dp),
            )
        }

        if (showBackButton || sourceBitmap != null) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showBackButton) {
                        ImageEditorIconButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                            onClick = onBack,
                        )
                    }
                    ImageEditorIconButton(
                        icon = Icons.Outlined.Collections,
                        contentDescription = "更换图片",
                        enabled = !isImportingImage && !isGenerating,
                        onClick = onPickImage,
                    )
                    ImageEditorIconButton(
                        icon = Icons.Outlined.PhotoCamera,
                        contentDescription = "从相机导入",
                        enabled = !isImportingImage && !isGenerating,
                        onClick = onPickFromCamera,
                    )
                }
                if (sourceBitmap != null) {
                    if (activeToolPanel == ImageToolPanel.Crop) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ImageEditorActionButton(
                                onClick = { cropImageView?.post { cropImageView?.resetCropRect() } },
                                enabled = !isImportingImage && !isGenerating,
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("重置")
                            }
                            ImageEditorActionButton(
                                onClick = {
                                    val croppedBitmap = cropImageView?.getCroppedImage() ?: sourceBitmap
                                    onApplyCrop(croppedBitmap)
                                    activeToolPanel = null
                                },
                                enabled = !isImportingImage && !isGenerating,
                            ) {
                                Icon(Icons.Outlined.Crop, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text("确认裁剪")
                            }
                        }
                    } else {
                        ImageEditorPrimaryButton(
                            onClick = {
                                val croppedBitmap = cropImageView?.getCroppedImage() ?: sourceBitmap
                                onGenerate(croppedBitmap)
                            },
                            enabled = !isImportingImage && !isGenerating,
                        ) {
                            Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text(
                                when {
                                    isGenerating && flowKind == CreateFlowKind.ChartRecognition -> "识别中..."
                                    isGenerating -> "生成中..."
                                    flowKind == CreateFlowKind.ChartRecognition -> "识别图纸"
                                    else -> "生成图纸"
                                },
                            )
                        }
                    }
                }
            }
        }

        if (sourceBitmap != null) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (activeToolPanel) {
                    ImageToolPanel.Crop ->
                        FloatingToolCard(
                            containerColor = Color.Transparent,
                            contentPadding = PaddingValues(0.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                        ) {
                            CropAspectPresetStrip(
                                selectedPreset = cropAspectPreset,
                                sourceBitmap = sourceBitmap,
                                onSelect = { preset ->
                                    cropAspectPreset = preset
                                    val nextSettings =
                                        when (preset) {
                                            CropAspectPreset.Custom -> settings
                                            CropAspectPreset.Original -> applyAspectRatio(settings, sourceBitmap.width, sourceBitmap.height)
                                            CropAspectPreset.Square -> applyAspectRatio(settings, 1, 1)
                                            CropAspectPreset.Ratio4x3 -> applyAspectRatio(settings, 4, 3)
                                            CropAspectPreset.Ratio3x4 -> applyAspectRatio(settings, 3, 4)
                                            CropAspectPreset.Ratio16x9 -> applyAspectRatio(settings, 16, 9)
                                            CropAspectPreset.Ratio9x16 -> applyAspectRatio(settings, 9, 16)
                                        }
                                    if (nextSettings != settings) {
                                        onSettingsChange(nextSettings)
                                    }
                                    cropImageView?.post { cropImageView?.resetCropRect() }
                                },
                            )
                        }

                    null -> Unit
                }

                FloatingToolCard(
                    containerColor = Color.Transparent,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ImageEditorToolPanelChip(
                            label = "裁剪",
                            icon = Icons.Outlined.ContentCut,
                            selected = activeToolPanel == ImageToolPanel.Crop,
                            onClick = {
                                activeToolPanel = if (activeToolPanel == ImageToolPanel.Crop) null else ImageToolPanel.Crop
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageEditorSettingsPanel(
    statusText: String,
    settings: GenerationSettings,
    onSettingsChange: (GenerationSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ImageEditorInfoBadge(statusText)
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = settings.title,
                    onValueChange = { onSettingsChange(settings.copy(title = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("图纸名称") },
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(29 to 29, 52 to 52, 58 to 58, 72 to 58, 96 to 96).forEach { (width, height) ->
                        ImageEditorFilterChip(
                            selected = settings.width == width && settings.height == height,
                            label = "${width} x $height",
                            onClick = {
                                onSettingsChange(
                                    settings.copy(
                                        width = width,
                                        height = height,
                                    ),
                                )
                            },
                        )
                    }
                }
                ImageEditorSliderRow(
                    label = "宽度",
                    value = settings.width.toFloat(),
                    valueText = settings.width.toString(),
                    range = 16f..120f,
                    steps = 103,
                    onChange = { onSettingsChange(settings.copy(width = it.roundToInt())) },
                )
                ImageEditorSliderRow(
                    label = "高度",
                    value = settings.height.toFloat(),
                    valueText = settings.height.toString(),
                    range = 16f..120f,
                    steps = 103,
                    onChange = { onSettingsChange(settings.copy(height = it.roundToInt())) },
                )
            }
        }
    }
}

@Composable
private fun ImageEditorLogicPanel(
    statusText: String,
    settings: GenerationSettings,
    onSettingsChange: (GenerationSettings) -> Unit,
) {
    val maxSchemeColors = settings.colorScheme.maxColors.coerceAtLeast(6)
    var selectedBrandId by remember(settings.colorScheme.brandId) { mutableStateOf(settings.colorScheme.brandId) }
    val selectableBrands = ColorSchemePreset.selectableBrands(settings.colorScheme)
    val selectableSchemes = ColorSchemePreset.selectableSchemesForBrand(selectedBrandId, settings.colorScheme)

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(statusText, style = MaterialTheme.typography.labelMedium)
            Text("配色方案", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text("先选品牌，再选颜色数。", style = MaterialTheme.typography.labelMedium)
            Text("品牌", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectableBrands.forEach { brand ->
                    ImageEditorFilterChip(
                        selected = selectedBrandId == brand.id,
                        label = brand.label,
                        onClick = { selectedBrandId = brand.id },
                    )
                }
            }
            Text("颜色数", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectableSchemes.forEach { scheme ->
                    ImageEditorFilterChip(
                        selected = settings.colorScheme == scheme,
                        label = scheme.variantLabel,
                        onClick = { onSettingsChange(applyColorScheme(settings, scheme)) },
                    )
                }
            }
            Text("当前方案：${settings.colorScheme.label}", style = MaterialTheme.typography.labelMedium)
            Text("生成风格", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GenerationStyle.entries.forEach { style ->
                    ImageEditorFilterChip(
                        selected = settings.generationStyle == style,
                        label = if (style == GenerationStyle.Cartoon) "卡通" else "写实",
                        onClick = { onSettingsChange(settings.copy(generationStyle = style)) },
                    )
                }
            }
            ImageEditorSliderRow(
                label = "颜色上限",
                value = settings.maxColors.toFloat(),
                valueText = settings.maxColors.toString(),
                range = 6f..maxSchemeColors.toFloat(),
                steps = (maxSchemeColors - 7).coerceAtLeast(0),
                onChange = { onSettingsChange(settings.copy(maxColors = it.roundToInt())) },
            )
            Text("背景策略", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BackgroundMode.entries.forEach { mode ->
                    ImageEditorFilterChip(
                        selected = settings.backgroundMode == mode,
                        label = if (mode == BackgroundMode.Keep) "保留背景" else "去白底",
                        onClick = { onSettingsChange(settings.copy(backgroundMode = mode)) },
                    )
                }
            }
            if (settings.backgroundMode == BackgroundMode.RemoveWhite) {
                ImageEditorSliderRow(
                    label = "背景容差",
                    value = settings.tolerance.toFloat(),
                    valueText = settings.tolerance.toString(),
                    range = 0f..96f,
                    steps = 95,
                    onChange = { onSettingsChange(settings.copy(tolerance = it.roundToInt())) },
                )
            }
            ImageEditorSliderRow(
                label = "斑点压缩",
                value = settings.speckleReduction.toFloat(),
                valueText = settings.speckleReduction.toString(),
                range = 0f..4f,
                steps = 3,
                onChange = { onSettingsChange(settings.copy(speckleReduction = it.roundToInt())) },
            )
        }
    }
}

@Composable
private fun ImageEditorViewport(
    bitmap: Bitmap,
    settings: GenerationSettings,
    showCropOverlay: Boolean,
    lockCropAspectRatio: Boolean,
    modifier: Modifier = Modifier,
    onCropperReady: (CropImageView) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        key(bitmap) {
            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
                factory = { context ->
                    CropImageView(context).apply {
                        val options =
                            CropImageOptions().apply {
                                guidelines = CropImageView.Guidelines.ON_TOUCH
                                scaleType = CropImageView.ScaleType.FIT_CENTER
                                cropShape = CropImageView.CropShape.RECTANGLE
                                this.showCropOverlay = showCropOverlay
                                autoZoomEnabled = true
                                multiTouchEnabled = true
                                centerMoveEnabled = true
                                fixAspectRatio = lockCropAspectRatio
                                aspectRatioX = settings.width
                                aspectRatioY = settings.height
                                showProgressBar = true
                                borderCornerThickness = 6f
                                borderLineThickness = 3f
                                guidelinesThickness = 1.5f
                                cropperLabelText = "裁剪区"
                                cropperLabelTextColor = android.graphics.Color.WHITE
                                backgroundColor = android.graphics.Color.argb(160, 0, 0, 0)
                            }
                        setImageCropOptions(options)
                        setFixedAspectRatio(lockCropAspectRatio)
                        if (lockCropAspectRatio) {
                            setAspectRatio(settings.width, settings.height)
                        }
                        setImageBitmap(bitmap)
                        onCropperReady(this)
                    }
                },
                update = { cropper ->
                    cropper.setFixedAspectRatio(lockCropAspectRatio)
                    if (lockCropAspectRatio) {
                        cropper.setAspectRatio(settings.width, settings.height)
                    }
                    cropper.guidelines = if (showCropOverlay) CropImageView.Guidelines.ON_TOUCH else CropImageView.Guidelines.OFF
                    cropper.isShowCropOverlay = showCropOverlay
                    onCropperReady(cropper)
                },
            )
        }
    }
}

@Composable
private fun PatternEditorScreen(
    project: BeadProject,
    settings: GenerationSettings,
    hasSourceBitmap: Boolean,
    sourceBitmapWidth: Int?,
    sourceBitmapHeight: Int?,
    isGenerating: Boolean,
    selectedColorCode: String,
    toolMode: ToolMode,
    removeMode: RemoveMode,
    mirrorDirection: MirrorDirection,
    boardZoom: Float,
    boardHorizontalScroll: ScrollState,
    boardVerticalScroll: ScrollState,
    isBoardViewportRestoring: Boolean,
    usageSummary: UsageSummary,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onToolModeChange: (ToolMode) -> Unit,
    onRemoveModeChange: (RemoveMode) -> Unit,
    onMirrorApply: (MirrorDirection) -> Unit,
    onRotateApply: (RotationDirection) -> Unit,
    onCanvasExpandApply: (CanvasExpandDirection) -> Unit,
    onOutputLevelPreviewChange: (Int) -> Unit,
    onOutputLevelCommit: (Int) -> Unit,
    onColorSchemeSelect: (ColorSchemePreset) -> Unit,
    onSelectedColorChange: (String) -> Unit,
    onBoardZoomChange: (Float) -> Unit,
    onCellTap: (Int) -> Unit,
    onMirrorProject: () -> Unit,
    onEnterBuildMode: () -> Unit,
) {
    val selectedColor = BeadPalette.find(selectedColorCode)
    val selectedColorValue = selectedColor?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.surfaceVariant
    val selectedColorContentColor = if (selectedColorValue.luminance() > 0.55f) Color(0xFF1F1B16) else Color.White
    var activeToolPanel by remember(project.id) { mutableStateOf<PatternToolPanel?>(null) }
    var activeToolSubPanel by remember(project.id) { mutableStateOf<PatternToolSubPanel?>(null) }
    var areCellCodesEnabled by remember(project.id) { mutableStateOf(true) }
    var rotationDirection by remember(project.id) { mutableStateOf(RotationDirection.Clockwise) }
    var isBoardFullscreen by remember(project.id) { mutableStateOf(false) }
    val shouldShowCellCodes =
        areCellCodesEnabled &&
            activeToolPanel != PatternToolPanel.Output &&
            activeToolPanel != PatternToolPanel.Logic
    val isDrawingToolActive = activeToolPanel == PatternToolPanel.Tool
    val boardBottomPadding = 0.dp

    HandleFullscreenState(isFullscreen = isBoardFullscreen) {
        isBoardFullscreen = false
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF1E8DB)),
    ) {
        PatternBoard(
            project = project,
            cellSize = boardZoom,
            horizontalScrollState = boardHorizontalScroll,
            verticalScrollState = boardVerticalScroll,
            isViewportRestoring = isBoardViewportRestoring,
            editable = true,
            tapEnabled = isDrawingToolActive,
            showCellCodes = shouldShowCellCodes,
            dragBufferStart = BOARD_DRAG_BUFFER_START,
            dragBufferEnd = BOARD_DRAG_BUFFER_END,
            dragBufferTop = BOARD_DRAG_BUFFER_TOP,
            dragBufferBottom = BOARD_DRAG_BUFFER_BOTTOM,
            highlightRow = null,
            highlightColumn = null,
            highlightColorCode = null,
            highlightIndices = null,
            completedIndices = emptySet(),
            onCellTap = onCellTap,
            onBoardDoubleTap = { isBoardFullscreen = !isBoardFullscreen },
            onCellSizeChange = onBoardZoomChange,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = boardBottomPadding),
        )

        if (!isBoardFullscreen) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                    )
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "撤回",
                        onClick = onUndo,
                        enabled = canUndo,
                    )
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = "取消撤回",
                        onClick = onRedo,
                        enabled = canRedo,
                    )
                    OverlayCircleButton(
                        icon = Icons.Outlined.Save,
                        contentDescription = "保存",
                        onClick = onSave,
                    )
                }
                if (false) FilledTonalButton(onClick = onEnterBuildMode) {
                    Icon(Icons.Outlined.GridView, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("进入拼豆模式")
                }
                OverlayActionButton(
                    icon = Icons.Outlined.GridView,
                    label = "进入拼豆模式",
                    contentDescription = "进入拼豆模式",
                    onClick = onEnterBuildMode,
                )
            }
        }

        if (false) {
            ElevatedCard(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(selectedColor?.let { Color(it.argb) } ?: Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(project.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${project.width} x ${project.height} · ${usageSummary.totalBeads} 颗 · ${toolModeLabel(toolMode)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }

        if (!isBoardFullscreen) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (activeToolPanel) {
                PatternToolPanel.Tool ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (activeToolSubPanel) {
                            PatternToolSubPanel.Remove ->
                                FloatingToolCard(
                                    containerColor = Color.Transparent,
                                    contentPadding = PaddingValues(0.dp),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                                ) {
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        RemoveMode.entries.forEach { mode ->
                                            FilterChip(
                                                selected = removeMode == mode,
                                                onClick = {
                                                    onRemoveModeChange(mode)
                                                    onToolModeChange(ToolMode.Remove)
                                                    activeToolSubPanel = null
                                                },
                                                label = { Text(removeModeLabel(mode)) },
                                                colors =
                                                    FilterChipDefaults.filterChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                    ),
                                            )
                                        }
                                    }
                                }

                            PatternToolSubPanel.Color ->
                                FloatingToolCard(
                                    containerColor = Color.Transparent,
                                    contentPadding = PaddingValues(0.dp),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
                                        shape = RoundedCornerShape(16.dp),
                                    ) {
                                        PatternEditorPaletteStrip(
                                            project = project,
                                            selectedColorCode = selectedColorCode,
                                            onSelect = onSelectedColorChange,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        )
                                    }
                                }

                            PatternToolSubPanel.Mirror ->
                                FloatingToolCard(
                                    containerColor = Color.Transparent,
                                    contentPadding = PaddingValues(0.dp),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                                ) {
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        MirrorDirection.entries.forEach { direction ->
                                            FilterChip(
                                                selected = mirrorDirection == direction,
                                                onClick = { onMirrorApply(direction) },
                                                label = { Text(mirrorDirectionLabel(direction)) },
                                                colors =
                                                    FilterChipDefaults.filterChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                    ),
                                            )
                                        }
                                    }
                                }

                            PatternToolSubPanel.Rotate ->
                                FloatingToolCard(
                                    containerColor = Color.Transparent,
                                    contentPadding = PaddingValues(0.dp),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                                ) {
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        RotationDirection.entries.forEach { direction ->
                                            FilterChip(
                                                selected = rotationDirection == direction,
                                                onClick = {
                                                    rotationDirection = direction
                                                    onRotateApply(direction)
                                                },
                                                label = { Text(rotationDirectionLabel(direction)) },
                                                colors =
                                                    FilterChipDefaults.filterChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                    ),
                                            )
                                        }
                                    }
                                }

                            null -> Unit
                        }
                        FloatingToolCard {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PatternEditorColorToolChip(
                                    color = selectedColorValue,
                                    contentColor = selectedColorContentColor,
                                    selectedColorCode = selectedColorCode,
                                    expanded = activeToolSubPanel == PatternToolSubPanel.Color,
                                    onClick = {
                                        activeToolSubPanel =
                                            if (activeToolSubPanel == PatternToolSubPanel.Color) null else PatternToolSubPanel.Color
                                    },
                                )
                                ToolMode.entries.filter { it != ToolMode.Remove && it != ToolMode.Mirror }.forEach { mode ->
                                    FilterChip(
                                        selected = toolMode == mode,
                                        onClick = {
                                            activeToolSubPanel = null
                                            onToolModeChange(mode)
                                        },
                                        label = { Text(toolModeLabel(mode)) },
                                    )
                                }
                                PatternEditorActionToolChip(
                                    label = toolModeLabel(ToolMode.Remove),
                                    selected = toolMode == ToolMode.Remove || activeToolSubPanel == PatternToolSubPanel.Remove,
                                    onClick = {
                                        onToolModeChange(ToolMode.Remove)
                                        activeToolSubPanel =
                                            if (activeToolSubPanel == PatternToolSubPanel.Remove) null else PatternToolSubPanel.Remove
                                    },
                                )
                                PatternEditorActionToolChip(
                                    label = toolModeLabel(ToolMode.Mirror),
                                    selected = activeToolSubPanel == PatternToolSubPanel.Mirror,
                                    onClick = {
                                        activeToolSubPanel =
                                            if (activeToolSubPanel == PatternToolSubPanel.Mirror) null else PatternToolSubPanel.Mirror
                                    },
                                )
                                PatternEditorActionToolChip(
                                    label = "旋转",
                                    selected = activeToolSubPanel == PatternToolSubPanel.Rotate,
                                    onClick = {
                                        activeToolSubPanel =
                                            if (activeToolSubPanel == PatternToolSubPanel.Rotate) null else PatternToolSubPanel.Rotate
                                    },
                                )
                            }
                        }
                    }

                PatternToolPanel.CanvasExpand ->
                    FloatingToolCard(
                        containerColor = Color.Transparent,
                        contentPadding = PaddingValues(0.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CanvasExpandDirection.entries.forEach { direction ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onCanvasExpandApply(direction) },
                                    label = { Text(canvasExpandDirectionLabel(direction)) },
                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                )
                            }
                        }
                    }

                PatternToolPanel.Options ->
                    FloatingToolCard {
                        Text(toolModeHint(toolMode, removeMode, mirrorDirection), style = MaterialTheme.typography.bodyMedium)
                        /*
                        when (toolMode) {
                            ToolMode.Remove -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    RemoveMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = removeMode == mode,
                                            onClick = { onRemoveModeChange(mode) },
                                            label = { Text(removeModeLabel(mode)) },
                                        )
                                    }
                                }
                            }

                            ToolMode.Mirror -> {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MirrorDirection.entries.forEach { direction ->
                                        FilterChip(
                                            selected = mirrorDirection == direction,
                                            onClick = { onMirrorDirectionChange(direction) },
                                            label = { Text(mirrorDirectionLabel(direction)) },
                                        )
                                    }
                                }
                                FilledTonalButton(onClick = onMirrorProject) {
                                    Icon(Icons.Outlined.Flip, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text("镜像整张图纸")
                                }
                            }

                            else -> Unit
                        }
                        */
                    }

                PatternToolPanel.Output ->
                    FloatingToolCard(
                        containerColor = Color.Transparent,
                        contentPadding = PaddingValues(0.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        PatternOutputPanel(
                            projectWidth = project.width,
                            projectHeight = project.height,
                            settings = settings,
                            hasSourceBitmap = hasSourceBitmap,
                            sourceBitmapWidth = sourceBitmapWidth,
                            sourceBitmapHeight = sourceBitmapHeight,
                            isGenerating = isGenerating,
                            onOutputLevelPreviewChange = onOutputLevelPreviewChange,
                            onOutputLevelCommit = onOutputLevelCommit,
                        )
                    }

                PatternToolPanel.Logic ->
                    FloatingToolCard(
                        containerColor = Color.Transparent,
                        contentPadding = PaddingValues(0.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        PatternLogicSchemesPanel(
                            settings = settings,
                            isGenerating = isGenerating,
                            onSchemeSelect = onColorSchemeSelect,
                        )
                    }

                PatternToolPanel.Usage ->
                    FloatingToolCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                    ) {
                        PatternUsagePanel(
                            summary = usageSummary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                        )
                    }

                PatternToolPanel.Zoom ->
                    FloatingToolCard {
                        ZoomSliderPanel(
                            value = boardZoom,
                            onChange = onBoardZoomChange,
                        )
                    }

                PatternToolPanel.Fullscreen ->
                    FloatingToolCard {
                        Text(
                            text = "双击画板进入全屏，再次双击退出全屏。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                null -> Unit
            }

                FloatingToolCard(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ToolPanelChip(
                            label = "像素大小",
                            icon = Icons.Outlined.Straighten,
                            selected = activeToolPanel == PatternToolPanel.Output,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Output) null else PatternToolPanel.Output
                            },
                        )
                        ToolPanelChip(
                            label = "配色",
                            icon = Icons.Outlined.SettingsSuggest,
                            selected = activeToolPanel == PatternToolPanel.Logic,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Logic) null else PatternToolPanel.Logic
                            },
                        )
                        ToolPanelChip(
                            label = "绘制",
                            icon = Icons.Outlined.Brush,
                            selected = activeToolPanel == PatternToolPanel.Tool,
                            onClick = {
                                if (activeToolPanel != PatternToolPanel.Tool) {
                                    activeToolSubPanel = null
                                }
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Tool) null else PatternToolPanel.Tool
                            },
                        )
                        ToolPanelChip(
                            label = "画布扩展",
                            icon = Icons.Outlined.Crop,
                            selected = activeToolPanel == PatternToolPanel.CanvasExpand,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.CanvasExpand) null else PatternToolPanel.CanvasExpand
                            },
                        )
                        ToolPanelChip(
                            label = "用量",
                            icon = Icons.Outlined.PhotoFilter,
                            selected = activeToolPanel == PatternToolPanel.Usage,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Usage) null else PatternToolPanel.Usage
                            },
                        )
                        ToolPanelChip(
                            label = "色号",
                            icon = Icons.Outlined.Visibility,
                            selected = areCellCodesEnabled,
                            onClick = { areCellCodesEnabled = !areCellCodesEnabled },
                            usePrimaryWhenSelected = true,
                        )
                        ToolPanelChip(
                            label = "缩放",
                            icon = Icons.Outlined.ZoomIn,
                            selected = activeToolPanel == PatternToolPanel.Zoom,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Zoom) null else PatternToolPanel.Zoom
                            },
                        )
                        ToolPanelChip(
                            label = "全屏",
                            icon = Icons.Outlined.Fullscreen,
                            selected = activeToolPanel == PatternToolPanel.Fullscreen,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Fullscreen) null else PatternToolPanel.Fullscreen
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternUsagePanel(
    summary: UsageSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = {}, label = { Text("${summary.totalBeads} 颗") })
            AssistChip(onClick = {}, label = { Text("${summary.totalColors} 色") })
            AssistChip(onClick = {}, label = { Text("${summary.isolatedBeads} 颗孤立珠") })
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            summary.stats.forEach { stat ->
                UsageStatBlock(stat = stat)
            }
        }
    }
}

@Composable
private fun UsageStatBlock(stat: UsageStat) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(stat.argb))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val labelColor = if (Color(stat.argb).luminance() > 0.55f) Color(0xFF1F1B16) else Color.White
            Text(
                text = stat.displayCode,
                color = labelColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "${stat.count} 颗",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PatternOutputPanel(
    projectWidth: Int,
    projectHeight: Int,
    settings: GenerationSettings,
    hasSourceBitmap: Boolean,
    sourceBitmapWidth: Int?,
    sourceBitmapHeight: Int?,
    isGenerating: Boolean,
    onOutputLevelPreviewChange: (Int) -> Unit,
    onOutputLevelCommit: (Int) -> Unit,
) {
    val outputLevel = outputLevelFromSettings(settings)
    var sliderValue by remember { mutableFloatStateOf(outputLevel.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(outputLevel) {
        if (!isDragging) {
            sliderValue = outputLevel.toFloat()
        }
    }
    val minLabel = "粗"
    val maxLabel = "细"
    val currentLevel = sliderValue.roundToInt()
    val previewSettings =
        if (hasSourceBitmap && sourceBitmapWidth != null && sourceBitmapHeight != null) {
            settingsForOutputLevel(
                current = settings,
                baseWidth = sourceBitmapWidth,
                baseHeight = sourceBitmapHeight,
                level = currentLevel,
            )
        } else {
            settings
        }
    val isPreviewing = hasSourceBitmap && currentLevel != outputLevel
    val displayedWidth = if (isPreviewing) previewSettings.width else projectWidth
    val displayedHeight = if (isPreviewing) previewSettings.height else projectHeight
    val gridCount = displayedWidth * displayedHeight
    val statusLabel =
        when {
            !hasSourceBitmap -> "不可调"
            isGenerating -> "应用中"
            isPreviewing -> "预览中"
            else -> "已应用"
        }

    fun adjustOutputLevel(delta: Int) {
        val nextLevel = (currentLevel + delta).coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
        if (nextLevel == currentLevel) {
            return
        }
        isDragging = false
        sliderValue = nextLevel.toFloat()
        onOutputLevelCommit(nextLevel)
    }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(18.dp),
                ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "$displayedWidth x $displayedHeight",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (hasSourceBitmap) "$gridCount 格" else "当前项目没有原图，无法调整",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color =
                        if (hasSourceBitmap) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        },
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (hasSourceBitmap) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactOutputStepButton(
                    label = "−",
                    enabled = hasSourceBitmap && currentLevel > OUTPUT_MIN_SHORT_EDGE,
                    onClick = { adjustOutputLevel(-1) },
                )
                Slider(
                    modifier = Modifier.weight(1f),
                    value = sliderValue,
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                        onOutputLevelPreviewChange(it.roundToInt())
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onOutputLevelCommit(sliderValue.roundToInt())
                    },
                    valueRange = OUTPUT_MIN_SHORT_EDGE.toFloat()..OUTPUT_MAX_SHORT_EDGE.toFloat(),
                    steps = (OUTPUT_MAX_SHORT_EDGE - OUTPUT_MIN_SHORT_EDGE - 1).coerceAtLeast(0),
                    enabled = hasSourceBitmap,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledThumbColor = MaterialTheme.colorScheme.outlineVariant,
                            disabledActiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                            disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        ),
                )
                CompactOutputStepButton(
                    label = "+",
                    enabled = hasSourceBitmap && currentLevel < OUTPUT_MAX_SHORT_EDGE,
                    onClick = { adjustOutputLevel(1) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(minLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "短边 $currentLevel",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasSourceBitmap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactOutputStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            ),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PatternLogicSchemesPanel(
    settings: GenerationSettings,
    isGenerating: Boolean,
    onSchemeSelect: (ColorSchemePreset) -> Unit,
) {
    var selectedBrandId by remember(settings.colorScheme.brandId) { mutableStateOf(settings.colorScheme.brandId) }
    var expandedSection by remember(settings.colorScheme) { mutableStateOf<PaletteDetailSection?>(null) }
    val selectableBrands = ColorSchemePreset.selectableBrands(settings.colorScheme)
    val selectableSchemes = ColorSchemePreset.selectableSchemesForBrand(selectedBrandId, settings.colorScheme)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp),
                    ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "当前方案",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${settings.colorScheme.brandLabel} ${settings.colorScheme.variantLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    color =
                        if (isGenerating) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        },
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (isGenerating) "应用中" else "${settings.colorScheme.maxColors} 色",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (isGenerating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
        when (expandedSection) {
            PaletteDetailSection.Brand ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectableBrands.forEach { brand ->
                            FilterChip(
                                selected = selectedBrandId == brand.id,
                                enabled = !isGenerating,
                                onClick = {
                                    selectedBrandId = brand.id
                                    expandedSection = PaletteDetailSection.ColorCount
                                },
                                label = { Text(brand.label) },
                            )
                        }
                    }
                }

            PaletteDetailSection.ColorCount ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectableSchemes.forEach { scheme ->
                            FilterChip(
                                selected = settings.colorScheme == scheme,
                                enabled = !isGenerating,
                                onClick = { onSchemeSelect(scheme) },
                                label = { Text(scheme.variantLabel) },
                            )
                        }
                    }
                }

            null -> Unit
        }
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = expandedSection == PaletteDetailSection.Brand,
                    enabled = !isGenerating,
                    onClick = {
                        expandedSection =
                            if (expandedSection == PaletteDetailSection.Brand) {
                                null
                            } else {
                                PaletteDetailSection.Brand
                            }
                    },
                    label = { Text("品牌") },
                )
                FilterChip(
                    selected = expandedSection == PaletteDetailSection.ColorCount,
                    enabled = !isGenerating,
                    onClick = {
                        expandedSection =
                            if (expandedSection == PaletteDetailSection.ColorCount) {
                                null
                            } else {
                                PaletteDetailSection.ColorCount
                            }
                    },
                    label = { Text("颜色数") },
                )
            }
        }
    }
}

@Composable
private fun PatternRegenerateCard(
    hasSourceBitmap: Boolean,
    isGenerating: Boolean,
    onRegenerate: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (hasSourceBitmap) {
                    "修改完参数后，可以直接在图纸编辑页重新生成。"
                } else {
                    "当前项目没有保留来源图片，输出和逻辑参数暂时无法直接重生成。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = onRegenerate,
                enabled = hasSourceBitmap && !isGenerating,
            ) {
                Icon(Icons.Outlined.AutoFixHigh, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(if (isGenerating) "重生成中..." else "按当前参数重新生成")
            }
        }
    }
}

@Composable
private fun PatternEditorColorChip(
    color: Color,
    contentColor: Color,
    selectedColorCode: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (expanded) 2.dp else 1.dp,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(999.dp),
                ),
        color = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "颜色",
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = colorCodeLabel(selectedColorCode),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PatternEditorColorHeader(
    color: Color,
    contentColor: Color,
    selectedColorCode: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (expanded) 2.dp else 1.dp,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(999.dp),
                ),
        color = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, contentColor.copy(alpha = 0.35f), CircleShape),
            )
            Text(
                text = "颜色",
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = colorCodeLabel(selectedColorCode),
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PatternEditorPaletteStrip(
    project: BeadProject,
    selectedColorCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BeadPalette.palette(project.colorScheme).forEach { color ->
            FilterChip(
                selected = color.id == selectedColorCode,
                onClick = { onSelect(color.id) },
                label = { Text(color.code) },
                leadingIcon = {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(color.argb))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                },
            )
        }
    }
}

@Composable
private fun PatternEditorColorToolChip(
    color: Color,
    contentColor: Color,
    selectedColorCode: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = expanded,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("颜色", fontWeight = FontWeight.SemiBold)
                Text(colorCodeLabel(selectedColorCode), fontWeight = FontWeight.Bold)
            }
        },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = color,
                labelColor = contentColor,
                selectedContainerColor = color,
                selectedLabelColor = contentColor,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = expanded,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                borderWidth = 1.dp,
                selectedBorderWidth = 2.dp,
            ),
    )
}

@Composable
private fun PatternEditorActionToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun BuildModeColorStrip(
    materialStats: List<MaterialStat>,
    selectedColorCode: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        materialStats.forEach { stat ->
            val chipColor = Color(stat.argb)
            val labelColor = if (chipColor.luminance() > 0.55f) Color(0xFF1F1B16) else Color.White
            FilterChip(
                selected = stat.code == selectedColorCode,
                onClick = { onSelect(stat.code) },
                label = { Text("${stat.displayCode} · ${stat.count}") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = chipColor,
                        labelColor = labelColor,
                        selectedContainerColor = chipColor,
                        selectedLabelColor = labelColor,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = stat.code == selectedColorCode,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp,
                    ),
            )
        }
    }
}

@Composable
private fun BuildModeScreen(
    project: BeadProject,
    buildMode: BuildMode,
    focusRow: Int,
    focusColumn: Int,
    focusColorCode: String?,
    focusColorBlockIndex: Int,
    boardZoom: Float,
    boardHorizontalScroll: ScrollState,
    boardVerticalScroll: ScrollState,
    isBoardViewportRestoring: Boolean,
    completedCellIndices: Set<Int>,
    materialStats: List<MaterialStat>,
    onBack: () -> Unit,
    onEnterPatternEditor: () -> Unit,
    onBuildModeChange: (BuildMode) -> Unit,
    onFocusRowChange: (Int) -> Unit,
    onFocusColumnChange: (Int) -> Unit,
    onFocusColorChange: (String) -> Unit,
    onFocusColorBlockIndexChange: (Int) -> Unit,
    onToggleCompletedSelection: (Set<Int>) -> Unit,
    onBoardZoomChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    var activeToolPanel by remember(project.id) { mutableStateOf<BuildToolPanel?>(null) }
    var markDoneArmed by remember(project.id) { mutableStateOf(false) }
    var buildModeBubbleMessage by remember(project.id) { mutableStateOf<String?>(null) }
    var activeBuildModeToast by remember(project.id) { mutableStateOf<Toast?>(null) }
    var isBoardFullscreen by remember(project.id) { mutableStateOf(false) }
    var areCellCodesEnabled by remember(project.id) { mutableStateOf(true) }
    var areAxisLabelsEnabled by remember(project.id) { mutableStateOf(false) }
    val focusDisplayWindow =
        remember(project.id, project.updatedAt) {
            computeBoardDisplayWindow(project = project, trimOuterEmpty = true)
        }
    val safeFocusRow = focusDisplayWindow.coerceRowIndex(focusRow)
    val safeFocusColumn = focusDisplayWindow.coerceColumnIndex(focusColumn)
    val visibleFocusRow = focusDisplayWindow.toVisibleRowIndex(safeFocusRow)
    val visibleFocusColumn = focusDisplayWindow.toVisibleColumnIndex(safeFocusColumn)
    val activeFocusColorCode = focusColorCode ?: materialStats.firstOrNull()?.code
    val focusedStat = materialStats.firstOrNull { it.code == activeFocusColorCode }
    val usageSummary =
        remember(project.id, project.updatedAt) {
            UsageAnalyzer.summarize(project)
        }
    val colorBlocks =
        remember(project.id, project.updatedAt, activeFocusColorCode) {
            collectColorBlockIndices(
                cells = project.cells,
                width = project.width,
                height = project.height,
                colorCode = activeFocusColorCode,
            )
        }
    val safeFocusColorBlockIndex = focusColorBlockIndex.coerceIn(0, (colorBlocks.size - 1).coerceAtLeast(0))
    val focusedColorBlock = colorBlocks.getOrNull(safeFocusColorBlockIndex)
    val totalPixelCount =
        remember(project.id, project.updatedAt) {
            project.cells.count { it != null }
        }
    val completedPixelCount = completedCellIndices.size.coerceAtMost(totalPixelCount)
    val completedPercent =
        if (totalPixelCount > 0) {
            ((completedPixelCount * 100f) / totalPixelCount).roundToInt()
        } else {
            0
        }
    val shouldShowFocusHighlight = activeToolPanel == BuildToolPanel.Focus
    val isMarkDoneInteractionActive =
        activeToolPanel == BuildToolPanel.MarkDone ||
            (activeToolPanel == BuildToolPanel.Mode && markDoneArmed)

    HandleFullscreenState(isFullscreen = isBoardFullscreen) {
        isBoardFullscreen = false
    }
    val onBoardCellTap: (Int) -> Unit =
        remember(
            activeToolPanel,
            markDoneArmed,
            buildMode,
            project.id,
            project.updatedAt,
            activeFocusColorCode,
            safeFocusColorBlockIndex,
            completedCellIndices,
        ) {
            { index: Int ->
                if (isMarkDoneInteractionActive) {
                    resolveBuildModeSelection(project, buildMode, index)?.let { selection ->
                        val shouldClear = selection.indices.all { it in completedCellIndices }
                        onToggleCompletedSelection(selection.indices)
                        buildModeBubbleMessage =
                            if (shouldClear) {
                                "已取消标记${selection.description}"
                            } else {
                                "已标记${selection.description}"
                            }
                    }
                } else when (buildMode) {
                    BuildMode.Row -> {
                        val row = (index / project.width).coerceIn(0, (project.height - 1).coerceAtLeast(0))
                        if (row in focusDisplayWindow.startRow..focusDisplayWindow.endRow) {
                            onFocusRowChange(row)
                        }
                    }
                    BuildMode.Column -> {
                        val column = (index % project.width).coerceIn(0, (project.width - 1).coerceAtLeast(0))
                        if (column in focusDisplayWindow.startColumn..focusDisplayWindow.endColumn) {
                            onFocusColumnChange(column)
                        }
                    }
                    BuildMode.Color -> {
                        project.cells.getOrNull(index)?.let { colorCode -> onFocusColorChange(colorCode) }
                    }
                    BuildMode.ColorBlock -> {
                        resolveColorBlockSelection(
                            cells = project.cells,
                            width = project.width,
                            height = project.height,
                            startIndex = index,
                        )?.let { selection ->
                            if (selection.colorCode != activeFocusColorCode) {
                                onFocusColorChange(selection.colorCode)
                            }
                            onFocusColorBlockIndexChange(selection.blockIndex)
                            val blockCount =
                                collectColorBlockIndices(
                                    cells = project.cells,
                                    width = project.width,
                                    height = project.height,
                                    colorCode = selection.colorCode,
                                ).size
                            buildModeBubbleMessage =
                                "当前是 ${colorCodeLabel(selection.colorCode)} 的第 ${selection.blockIndex + 1}/$blockCount 个区块"
                        }
                    }
                }
            }
        }
    @Suppress("UNUSED_VARIABLE")
    val focusLabel =
        when (buildMode) {
            BuildMode.Row -> "第 ${focusDisplayWindow.toVisibleRowNumber(safeFocusRow)} 行 / 共 ${focusDisplayWindow.rowCount} 行"
            BuildMode.Column -> "第 ${focusDisplayWindow.toVisibleColumnNumber(safeFocusColumn)} 列 / 共 ${focusDisplayWindow.columnCount} 列"
            BuildMode.Color ->
                focusedStat?.let { "颜色 ${it.displayCode} · ${it.count} 颗" } ?: "未选择颜色"
            BuildMode.ColorBlock ->
                if (activeFocusColorCode == null || focusedColorBlock == null) {
                    "未选择颜色区块"
                } else {
                    "颜色 ${colorCodeLabel(activeFocusColorCode)} · 区块 ${safeFocusColorBlockIndex + 1}/${colorBlocks.size} · ${focusedColorBlock.size} 颗"
                }
        }
    val boardBottomPadding = 0.dp

    LaunchedEffect(buildModeBubbleMessage) {
        val message = buildModeBubbleMessage ?: return@LaunchedEffect
        activeBuildModeToast?.cancel()
        activeBuildModeToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        activeBuildModeToast?.show()
        buildModeBubbleMessage = null
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF1E8DB)),
    ) {
        PatternBoard(
            project = project,
            cellSize = boardZoom,
            horizontalScrollState = boardHorizontalScroll,
            verticalScrollState = boardVerticalScroll,
            isViewportRestoring = isBoardViewportRestoring,
            editable = false,
            tapEnabled = true,
            showCellCodes = areCellCodesEnabled,
            showAxisLabels = areAxisLabelsEnabled,
            dragBufferStart = BOARD_DRAG_BUFFER_START,
            dragBufferEnd = BOARD_DRAG_BUFFER_END,
            dragBufferTop = BOARD_DRAG_BUFFER_TOP,
            dragBufferBottom = BOARD_DRAG_BUFFER_BOTTOM,
            highlightRow = if (shouldShowFocusHighlight && buildMode == BuildMode.Row) safeFocusRow else null,
            highlightColumn = if (shouldShowFocusHighlight && buildMode == BuildMode.Column) safeFocusColumn else null,
            highlightColorCode = if (shouldShowFocusHighlight && buildMode == BuildMode.Color) activeFocusColorCode else null,
            highlightIndices = if (shouldShowFocusHighlight && buildMode == BuildMode.ColorBlock) focusedColorBlock?.toSet() else null,
            completedIndices = completedCellIndices,
            dimCompletedIndices = shouldShowFocusHighlight,
            onCellTap = onBoardCellTap,
            onBoardDoubleTap = { isBoardFullscreen = !isBoardFullscreen },
            onCellSizeChange = onBoardZoomChange,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = boardBottomPadding),
        )

        if (!isBoardFullscreen) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverlayCircleButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
                OverlayActionButton(
                    icon = Icons.Outlined.Brush,
                    label = "进入编辑模式",
                    contentDescription = "进入编辑模式",
                    onClick = onEnterPatternEditor,
                )
            }
        }

        if (!isBoardFullscreen) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (activeToolPanel) {
                BuildToolPanel.Mode ->
                    FloatingToolCard {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BuildMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = buildMode == mode,
                                    onClick = {
                                        onBuildModeChange(mode)
                                        if (markDoneArmed) {
                                            activeToolPanel = BuildToolPanel.MarkDone
                                        }
                                    },
                                    label = { Text(buildModeLabel(mode)) },
                                )
                            }
                        }
                    }

                BuildToolPanel.Zoom ->
                    FloatingToolCard {
                        ZoomSliderPanel(
                            value = boardZoom,
                            onChange = onBoardZoomChange,
                        )
                    }

                BuildToolPanel.Focus ->
                    FloatingToolCard {
                        when (buildMode) {
                            BuildMode.Row ->
                                SliderRow(
                                    label = "当前行",
                                    value = visibleFocusRow.toFloat(),
                                    valueText = focusDisplayWindow.toVisibleRowNumber(safeFocusRow).toString(),
                                    range = 0f..(focusDisplayWindow.rowCount - 1).coerceAtLeast(0).toFloat(),
                                    steps = (focusDisplayWindow.rowCount - 2).coerceAtLeast(0),
                                    onChange = {
                                        onFocusRowChange(focusDisplayWindow.coerceRowIndex(focusDisplayWindow.startRow + it.roundToInt()))
                                    },
                                )

                            BuildMode.Column ->
                                SliderRow(
                                    label = "当前列",
                                    value = visibleFocusColumn.toFloat(),
                                    valueText = focusDisplayWindow.toVisibleColumnNumber(safeFocusColumn).toString(),
                                    range = 0f..(focusDisplayWindow.columnCount - 1).coerceAtLeast(0).toFloat(),
                                    steps = (focusDisplayWindow.columnCount - 2).coerceAtLeast(0),
                                    onChange = {
                                        onFocusColumnChange(focusDisplayWindow.coerceColumnIndex(focusDisplayWindow.startColumn + it.roundToInt()))
                                    },
                                )

                            BuildMode.Color ->
                                BuildModeColorStrip(
                                    materialStats = materialStats,
                                    selectedColorCode = activeFocusColorCode,
                                    onSelect = onFocusColorChange,
                                )

                            BuildMode.ColorBlock -> {
                                BuildModeColorStrip(
                                    materialStats = materialStats,
                                    selectedColorCode = activeFocusColorCode,
                                    onSelect = { colorCode ->
                                        val nextBlockCount =
                                            collectColorBlockIndices(
                                                cells = project.cells,
                                                width = project.width,
                                                height = project.height,
                                                colorCode = colorCode,
                                            ).size
                                        if (colorCode != activeFocusColorCode) {
                                            onFocusColorChange(colorCode)
                                            onFocusColorBlockIndexChange(0)
                                            if (nextBlockCount > 0) {
                                                buildModeBubbleMessage = "当前是 $colorCode 的第 1/$nextBlockCount 个区块"
                                            }
                                        } else if (colorBlocks.isNotEmpty()) {
                                            val nextIndex = (safeFocusColorBlockIndex + 1) % colorBlocks.size
                                            onFocusColorBlockIndexChange(nextIndex)
                                            buildModeBubbleMessage = "当前是 $colorCode 的第 ${nextIndex + 1}/${colorBlocks.size} 个区块"
                                        }
                                    },
                                )
                            }
                        }
                    }

                BuildToolPanel.MarkDone ->
                    FloatingToolCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "已拼像素",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "$completedPixelCount / $totalPixelCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "$completedPercent%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                BuildToolPanel.Usage ->
                    FloatingToolCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                    ) {
                        PatternUsagePanel(
                            summary = usageSummary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                        )
                    }

                BuildToolPanel.Fullscreen ->
                    FloatingToolCard {
                        Text(
                            text = "双击画板进入全屏，再次双击退出全屏。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                null -> Unit
            }

                FloatingToolCard(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ToolPanelChip(
                            label = "焦点",
                            icon = Icons.Outlined.Visibility,
                            selected = activeToolPanel == BuildToolPanel.Focus,
                            onClick = {
                                val wasFocusOpen = activeToolPanel == BuildToolPanel.Focus && !markDoneArmed
                                markDoneArmed = false
                                activeToolPanel = if (wasFocusOpen) null else BuildToolPanel.Focus
                            },
                        )
                        ToolPanelChip(
                            label = "模式选择",
                            icon = Icons.Outlined.Tune,
                            selected = activeToolPanel == BuildToolPanel.Mode,
                            onClick = {
                                activeToolPanel =
                                    if (activeToolPanel == BuildToolPanel.Mode) {
                                        if (markDoneArmed) {
                                            BuildToolPanel.MarkDone
                                        } else {
                                            null
                                        }
                                    } else {
                                        BuildToolPanel.Mode
                                    }
                            },
                        )
                        ToolPanelChip(
                            label = "标记已拼",
                            icon = Icons.Outlined.Save,
                            selected = activeToolPanel == BuildToolPanel.MarkDone || (activeToolPanel == BuildToolPanel.Mode && markDoneArmed),
                            onClick = {
                                if (activeToolPanel == BuildToolPanel.MarkDone) {
                                    activeToolPanel = null
                                    markDoneArmed = false
                                } else {
                                    activeToolPanel = BuildToolPanel.MarkDone
                                    markDoneArmed = true
                                }
                            },
                        )
                        ToolPanelChip(
                            label = "用量",
                            icon = Icons.Outlined.PhotoFilter,
                            selected = activeToolPanel == BuildToolPanel.Usage,
                            onClick = {
                                activeToolPanel = if (activeToolPanel == BuildToolPanel.Usage) null else BuildToolPanel.Usage
                                markDoneArmed = false
                            },
                        )
                        ToolPanelChip(
                            label = "行列号",
                            icon = Icons.Outlined.Straighten,
                            selected = areAxisLabelsEnabled,
                            onClick = { areAxisLabelsEnabled = !areAxisLabelsEnabled },
                            usePrimaryWhenSelected = true,
                        )
                        ToolPanelChip(
                            label = "缩放",
                            icon = Icons.Outlined.ZoomIn,
                            selected = activeToolPanel == BuildToolPanel.Zoom,
                            onClick = {
                                activeToolPanel = if (activeToolPanel == BuildToolPanel.Zoom) null else BuildToolPanel.Zoom
                                markDoneArmed = false
                            },
                        )
                        ToolPanelChip(
                            label = "色号",
                            icon = Icons.Outlined.Visibility,
                            selected = areCellCodesEnabled,
                            onClick = { areCellCodesEnabled = !areCellCodesEnabled },
                            usePrimaryWhenSelected = true,
                        )
                        ToolPanelChip(
                            label = "全屏",
                            icon = Icons.Outlined.Fullscreen,
                            selected = activeToolPanel == BuildToolPanel.Fullscreen,
                            onClick = {
                                activeToolPanel = if (activeToolPanel == BuildToolPanel.Fullscreen) null else BuildToolPanel.Fullscreen
                                markDoneArmed = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun OverlayActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Icon(icon, contentDescription = contentDescription)
        Spacer(Modifier.size(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun ToolPanelChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    usePrimaryWhenSelected: Boolean = false,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors =
            if (usePrimaryWhenSelected) {
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                FilterChipDefaults.filterChipColors()
            },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
private fun FloatingToolCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = elevation,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun ImageEditorIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun ImageEditorPrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
        ),
        content = content,
    )
}

@Composable
private fun ImageEditorActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.62f),
        ),
        content = content,
    )
}

@Composable
private fun ImageEditorInfoBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(18.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
        )
    }
}

@Composable
private fun ImageEditorFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
        }
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = contentColor.copy(alpha = if (selected) 0.48f else 0.18f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ImageEditorToolPanelChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
        }
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = contentColor.copy(alpha = if (selected) 0.48f else 0.18f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CropAspectPresetStrip(
    selectedPreset: CropAspectPreset,
    sourceBitmap: Bitmap?,
    onSelect: (CropAspectPreset) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cropAspectPresetItems(sourceBitmap).forEach { item ->
            CropAspectPresetCard(
                title = item.title,
                previewRatio = item.previewRatio,
                selected = selectedPreset == item.preset,
                onClick = { onSelect(item.preset) },
            )
        }
    }
}

@Composable
private fun CropAspectPresetCard(
    title: String,
    previewRatio: Float,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
        }
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = contentColor.copy(alpha = if (selected) 0.48f else 0.18f),
        ),
    ) {
        Column(
            modifier =
                Modifier
                    .size(width = 104.dp, height = 110.dp)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .height(44.dp)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .height(28.dp)
                            .aspectRatio(previewRatio.coerceAtLeast(0.6f))
                            .border(3.dp, contentColor, RoundedCornerShape(4.dp)),
                )
            }
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private data class CropAspectPresetItem(
    val preset: CropAspectPreset,
    val title: String,
    val previewRatio: Float,
)

private fun cropAspectPresetItems(sourceBitmap: Bitmap?): List<CropAspectPresetItem> {
    val originalRatio =
        sourceBitmap
            ?.let { bitmap -> bitmap.width.coerceAtLeast(1) / bitmap.height.coerceAtLeast(1).toFloat() }
            ?.coerceIn(0.5f, 2.2f)
            ?: 1f

    return listOf(
        CropAspectPresetItem(CropAspectPreset.Custom, "自由", 1f),
        CropAspectPresetItem(CropAspectPreset.Original, "原图", originalRatio),
        CropAspectPresetItem(CropAspectPreset.Square, "1:1", 1f),
        CropAspectPresetItem(CropAspectPreset.Ratio4x3, "4:3", 4f / 3f),
        CropAspectPresetItem(CropAspectPreset.Ratio3x4, "3:4", 3f / 4f),
        CropAspectPresetItem(CropAspectPreset.Ratio16x9, "16:9", 16f / 9f),
        CropAspectPresetItem(CropAspectPreset.Ratio9x16, "9:16", 9f / 16f),
    )
}

@Composable
private fun HeroCard(
    projectCount: Int,
    statusText: String,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("本地图纸", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "首页只放已经保存到本地的拼豆图纸。裁剪保留原生流程，生成和编辑已经切到开源逻辑。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoRow("$projectCount 个项目", "Android 原生", "APK 可直接调试")
            StatusCard(statusText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    project: BeadProject,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onOpen,
                        onLongClick = onRename,
                        onLongClickLabel = "重命名项目",
                    ).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                modifier = Modifier.weight(0.42f).aspectRatio(project.width / project.height.toFloat()).heightIn(max = 132.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6EC)),
            ) {
                PatternThumbnail(project = project)
            }
            Column(modifier = Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${project.width} x ${project.height}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${PatternEngine.materialStats(project.cells).size} 种颜色 · ${project.colorScheme.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(PatternEngine.formatTimestamp(project.updatedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除")
            }
        }
    }
}

@Composable
private fun StatusCard(statusText: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            text = statusText,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun ZoomSliderPanel(
    value: Float,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("画布缩放", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "拖动连续调节格子大小",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    "${value.roundToInt()}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Slider(
            value = value.coerceIn(BOARD_ZOOM_RANGE),
            onValueChange = { onChange(it.coerceIn(BOARD_ZOOM_RANGE)) },
            valueRange = BOARD_ZOOM_RANGE,
            steps = 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("缩小", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("放大", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImageEditorSliderRow(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(valueText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onPrimary,
                    activeTrackColor = MaterialTheme.colorScheme.onPrimary,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                    inactiveTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                ),
            )
        }
    }
}

@Composable
private fun InfoRow(vararg texts: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        texts.forEach { text -> AssistChip(onClick = {}, label = { Text(text) }) }
    }
}

@Composable
private fun PatternThumbnail(project: BeadProject) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val cellWidth = size.width / project.width
        val cellHeight = size.height / project.height
        drawRect(Color(0xFFFFF6EC))
        project.cells.forEachIndexed { index, code ->
            val row = index / project.width
            val column = index % project.width
            val color = BeadPalette.find(code)?.let { Color(it.argb) } ?: Color(0xFFF3E9DB)
            drawRect(color = color, topLeft = Offset(column * cellWidth, row * cellHeight), size = Size(cellWidth, cellHeight))
        }
    }
}

@Composable
private fun PatternBoard(
    project: BeadProject,
    cellSize: Float,
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
    isViewportRestoring: Boolean = false,
    editable: Boolean,
    tapEnabled: Boolean = editable,
    showCellCodes: Boolean,
    showAxisLabels: Boolean = false,
    dragBufferStart: Dp = 72.dp,
    dragBufferEnd: Dp = 72.dp,
    dragBufferTop: Dp = 72.dp,
    dragBufferBottom: Dp = 72.dp,
    highlightRow: Int?,
    highlightColumn: Int?,
    highlightColorCode: String?,
    highlightIndices: Set<Int>?,
    completedIndices: Set<Int>,
    dimCompletedIndices: Boolean = true,
    onCellTap: (Int) -> Unit,
    onBoardDoubleTap: (() -> Unit)? = null,
    onCellSizeChange: ((Float) -> Unit)? = null,
    modifier: Modifier =
        Modifier
            .fillMaxWidth()
            .height(420.dp),
) {
    val boardDisplayWindow =
        remember(project.id, project.width, project.height, project.cells) {
            computeBoardDisplayWindow(project = project, trimOuterEmpty = false)
        }
    val axisDisplayWindow =
        remember(project.id, project.width, project.height, project.cells) {
            computeBoardDisplayWindow(project = project, trimOuterEmpty = true)
        }
    val density = LocalDensity.current
    val doubleTapSlopPx = with(density) { 56.dp.toPx() }
    val doubleTapTimeoutMillis = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }
    var lastSurfaceTap by remember(project.id) { mutableStateOf<SurfaceTapRecord?>(null) }
    var surfaceTopLeftInRoot by remember(project.id) { mutableStateOf(Offset.Zero) }
    var surfaceSize by remember(project.id) { mutableStateOf(IntSize.Zero) }
    var canvasTopLeft by remember(project.id) { mutableStateOf(Offset.Zero) }
    var canvasSizePx by remember(project.id) { mutableStateOf(IntSize.Zero) }
    val currentTapEnabled by rememberUpdatedState(tapEnabled)
    val currentOnCellTap by rememberUpdatedState(onCellTap)
    val currentOnBoardDoubleTap by rememberUpdatedState(onBoardDoubleTap)
    val currentCanvasTopLeft by rememberUpdatedState(canvasTopLeft)
    val currentCanvasSizePx by rememberUpdatedState(canvasSizePx)
    val axisPanelColor = MaterialTheme.colorScheme.surface
    val axisTextColor = MaterialTheme.colorScheme.onSurface
    val axisHighlightColor = MaterialTheme.colorScheme.primary
    val axisOutlineColor = MaterialTheme.colorScheme.outlineVariant
    val predictedBoardWidthPx = with(density) { (boardDisplayWindow.columnCount * cellSize).dp.toPx() }
    val predictedBoardHeightPx = with(density) { (boardDisplayWindow.rowCount * cellSize).dp.toPx() }
    fun resolveDragPaddingPair(
        viewportPx: Int,
        boardPx: Float,
        leadingBuffer: Dp,
        trailingBuffer: Dp,
    ): Pair<Dp, Dp> {
        val leadingBufferPx = with(density) { leadingBuffer.toPx() }
        val trailingBufferPx = with(density) { trailingBuffer.toPx() }
        if (viewportPx <= 0) {
            return leadingBuffer to trailingBuffer
        }
        // Small charts also keep a scrollable buffer, so overlays do not pin the content in place.
        val centeringCompensationPx = ((viewportPx.toFloat() - boardPx).coerceAtLeast(0f)) / 2f
        return with(density) {
            (centeringCompensationPx + leadingBufferPx).toDp() to
                (centeringCompensationPx + trailingBufferPx).toDp()
        }
    }

    fun resolveCellIndexAtSurfacePosition(position: Offset): Int? {
        val canvasSize = currentCanvasSizePx
        if (
            !currentTapEnabled ||
                boardDisplayWindow.columnCount <= 0 ||
                boardDisplayWindow.rowCount <= 0 ||
                canvasSize.width <= 0 ||
                canvasSize.height <= 0
        ) {
            return null
        }

        val localPosition = position - currentCanvasTopLeft
        if (
            localPosition.x < 0f ||
                localPosition.y < 0f ||
                localPosition.x >= canvasSize.width.toFloat() ||
                localPosition.y >= canvasSize.height.toFloat()
        ) {
            return null
        }

        val cellWidth = canvasSize.width.toFloat() / boardDisplayWindow.columnCount
        val cellHeight = canvasSize.height.toFloat() / boardDisplayWindow.rowCount
        if (cellWidth <= 0f || cellHeight <= 0f) {
            return null
        }

        val displayColumn = floor(localPosition.x / cellWidth).toInt()
        val displayRow = floor(localPosition.y / cellHeight).toInt()
        if (displayColumn !in 0 until boardDisplayWindow.columnCount || displayRow !in 0 until boardDisplayWindow.rowCount) {
            return null
        }

        val column = boardDisplayWindow.startColumn + displayColumn
        val row = boardDisplayWindow.startRow + displayRow
        return row * project.width + column
    }

    fun handleSurfaceTap(
        position: Offset,
        cellIndex: Int?,
    ) {
        cellIndex?.let(currentOnCellTap)
        val onDoubleTap = currentOnBoardDoubleTap ?: return
        val now = SystemClock.uptimeMillis()
        val previous = lastSurfaceTap
        val isDoubleTap =
            previous != null &&
                now - previous.timeMillis <= doubleTapTimeoutMillis &&
                (position - previous.position).getDistance() <= doubleTapSlopPx
        if (isDoubleTap) {
            lastSurfaceTap = null
            onDoubleTap()
        } else {
            lastSurfaceTap = SurfaceTapRecord(timeMillis = now, position = position)
        }
    }

    Card(
        modifier = modifier.alpha(if (isViewportRestoring) 0f else 1f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6EC)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        surfaceTopLeftInRoot = coordinates.positionInRoot()
                        surfaceSize = coordinates.size
                    }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportWidthPx = constraints.maxWidth
                val viewportHeightPx = constraints.maxHeight
                Box(
                    modifier =
                        Modifier
                        .fillMaxSize()
                        .pointerInput(project.id, project.width, project.height, project.cells, cellSize, onCellSizeChange, isViewportRestoring) {
                            if (isViewportRestoring) {
                                return@pointerInput
                            }
                            awaitEachGesture {
                                var gestureCellSize = cellSize
                                var dragOffset = Offset.Zero
                                var isDragging = false
                                var pressedCountMax = 0
                                var firstDownPosition: Offset? = null
                                val touchSlop = viewConfiguration.touchSlop
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount > 0) {
                                        pressedCountMax = max(pressedCountMax, pressedCount)
                                        if (firstDownPosition == null) {
                                            firstDownPosition = event.changes.firstOrNull { it.pressed }?.position
                                        }
                                    }
                                    if (pressedCount >= 2) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        if (zoomChange != 1f && onCellSizeChange != null) {
                                            val nextCellSize = (gestureCellSize * zoomChange).coerceIn(BOARD_ZOOM_RANGE)
                                            if (nextCellSize != gestureCellSize) {
                                                gestureCellSize = nextCellSize
                                                onCellSizeChange(nextCellSize)
                                            }
                                        }

                                        if (panChange.x != 0f) {
                                            horizontalScrollState.dispatchRawDelta(-panChange.x)
                                        }
                                        if (panChange.y != 0f) {
                                            verticalScrollState.dispatchRawDelta(-panChange.y)
                                        }

                                        event.changes.forEach { change ->
                                            if (change.positionChanged()) {
                                                change.consume()
                                            }
                                        }
                                    } else if (pressedCount == 1) {
                                        val panChange = event.calculatePan()
                                        if (panChange != Offset.Zero) {
                                            if (!isDragging) {
                                                dragOffset += panChange
                                                isDragging = dragOffset.getDistance() > touchSlop
                                            }
                                            if (isDragging) {
                                                horizontalScrollState.dispatchRawDelta(-panChange.x)
                                                verticalScrollState.dispatchRawDelta(-panChange.y)
                                                event.changes.forEach { change ->
                                                    if (change.positionChanged()) {
                                                        change.consume()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (!isDragging && pressedCountMax == 1) {
                                    firstDownPosition?.let { position ->
                                        handleSurfaceTap(
                                            position = position,
                                            cellIndex = resolveCellIndexAtSurfacePosition(position),
                                        )
                                    }
                                }
                            }
                        }
                        .horizontalScroll(horizontalScrollState, enabled = false)
                        .verticalScroll(verticalScrollState, enabled = false),
                    contentAlignment = Alignment.Center,
                ) {
                    val (edgePanPaddingStart, edgePanPaddingEnd) =
                    resolveDragPaddingPair(
                        viewportPx = viewportWidthPx,
                        boardPx = predictedBoardWidthPx,
                        leadingBuffer = dragBufferStart,
                        trailingBuffer = dragBufferEnd,
                    )
                    val (edgePanPaddingTop, edgePanPaddingBottom) =
                    resolveDragPaddingPair(
                        viewportPx = viewportHeightPx,
                        boardPx = predictedBoardHeightPx,
                        leadingBuffer = dragBufferTop,
                        trailingBuffer = dragBufferBottom,
                    )
                    Box(
                    modifier =
                        Modifier.padding(
                            start = edgePanPaddingStart,
                            end = edgePanPaddingEnd,
                            top = edgePanPaddingTop,
                            bottom = edgePanPaddingBottom,
                        ),
                ) {
                    Canvas(
                        modifier =
                            Modifier
                                .size((boardDisplayWindow.columnCount * cellSize).dp, (boardDisplayWindow.rowCount * cellSize).dp)
                                .onGloballyPositioned { coordinates ->
                                    canvasTopLeft = coordinates.positionInRoot() - surfaceTopLeftInRoot
                                    canvasSizePx = coordinates.size
                                }
                    ) {
                        if (boardDisplayWindow.columnCount <= 0 || boardDisplayWindow.rowCount <= 0) {
                            return@Canvas
                        }
                        val cellWidth = size.width / boardDisplayWindow.columnCount
                        val cellHeight = size.height / boardDisplayWindow.rowCount
                        val minCellEdge = minOf(cellWidth, cellHeight)
                        val shouldRenderCellCodes = showCellCodes && minCellEdge >= 14f
                        drawRect(Color(0xFFFFF6EC))
                        drawBoardCellsAndGrid(
                            project = project,
                            displayWindow = boardDisplayWindow,
                            boardLeft = 0f,
                            boardTop = 0f,
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                            highlightRow = highlightRow,
                            highlightColumn = highlightColumn,
                            highlightColorCode = highlightColorCode,
                            highlightIndices = highlightIndices,
                            completedIndices = completedIndices,
                            dimCompletedIndices = dimCompletedIndices,
                        )

                        if (shouldRenderCellCodes) {
                            val fillPaint =
                                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                    textAlign = AndroidPaint.Align.CENTER
                                    isSubpixelText = true
                                    isFakeBoldText = true
                                }
                            val strokePaint =
                                AndroidPaint(fillPaint).apply {
                                    style = AndroidPaint.Style.STROKE
                                    strokeWidth = (minCellEdge * 0.08f).coerceAtLeast(1.4f)
                                }

                            for (displayRow in 0 until boardDisplayWindow.rowCount) {
                                val row = boardDisplayWindow.startRow + displayRow
                                for (displayColumn in 0 until boardDisplayWindow.columnCount) {
                                    val column = boardDisplayWindow.startColumn + displayColumn
                                    val index = row * project.width + column
                                    val code = project.cells.getOrNull(index) ?: continue
                                    if (index in completedIndices) {
                                        continue
                                    }

                                    val shouldDim =
                                        shouldDimBoardCell(
                                            row = row,
                                            column = column,
                                            code = code,
                                            index = index,
                                            highlightRow = highlightRow,
                                            highlightColumn = highlightColumn,
                                            highlightColorCode = highlightColorCode,
                                            highlightIndices = highlightIndices,
                                            completedIndices = emptySet(),
                                            dimCompletedIndices = false,
                                        )
                                    val paletteColor = BeadPalette.find(code)
                                    val displayCode = paletteColor?.code ?: code
                                    val rgb = paletteColor?.argb ?: 0xFFFFFFFF.toInt()
                                    val red = rgb shr 16 and 0xFF
                                    val green = rgb shr 8 and 0xFF
                                    val blue = rgb and 0xFF
                                    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
                                    val textSizeScale =
                                        when (displayCode.length) {
                                            1 -> 0.58f
                                            2 -> 0.42f
                                            3 -> 0.30f
                                            else -> 0.26f
                                        }
                                    val textSize = (minCellEdge * textSizeScale).coerceAtLeast(9f)
                                    val fillColor =
                                        if (luminance < 128) {
                                            android.graphics.Color.WHITE
                                        } else {
                                            android.graphics.Color.rgb(24, 24, 24)
                                        }
                                    val outlineColor =
                                        if (luminance < 128) {
                                            android.graphics.Color.argb(180, 24, 24, 24)
                                        } else {
                                            android.graphics.Color.argb(170, 255, 255, 255)
                                        }

                                    fillPaint.textSize = textSize
                                    fillPaint.color = fillColor
                                    fillPaint.alpha = if (shouldDim) 120 else 255
                                    strokePaint.textSize = textSize
                                    strokePaint.color = outlineColor
                                    strokePaint.alpha = if (shouldDim) 90 else 255

                                    val centerX = displayColumn * cellWidth + cellWidth / 2f
                                    val centerY = displayRow * cellHeight + cellHeight / 2f
                                    val baseline = centerY - (fillPaint.descent() + fillPaint.ascent()) / 2f
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(displayCode, centerX, baseline, strokePaint)
                                        drawText(displayCode, centerX, baseline, fillPaint)
                                    }
                                }
                            }
                        }

                        if (completedIndices.isNotEmpty() && minCellEdge >= 9f) {
                            for (displayRow in 0 until boardDisplayWindow.rowCount) {
                                val row = boardDisplayWindow.startRow + displayRow
                                for (displayColumn in 0 until boardDisplayWindow.columnCount) {
                                    val column = boardDisplayWindow.startColumn + displayColumn
                                    val index = row * project.width + column
                                    if (index !in completedIndices) {
                                        continue
                                    }
                                    val code = project.cells.getOrNull(index) ?: continue
                                    val baseColor = BeadPalette.find(code)?.let { Color(it.argb) } ?: Color(0xFFF2EBDD)
                                    val checkColor = if (baseColor.luminance() > 0.5f) Color(0xFF395A4D) else Color(0xFFF8F4EC)
                                    val left = displayColumn * cellWidth
                                    val top = displayRow * cellHeight
                                    val start = Offset(left + cellWidth * 0.22f, top + cellHeight * 0.56f)
                                    val middle = Offset(left + cellWidth * 0.42f, top + cellHeight * 0.75f)
                                    val end = Offset(left + cellWidth * 0.78f, top + cellHeight * 0.30f)
                                    val strokeWidth = (minCellEdge * 0.12f).coerceAtLeast(1.8f)
                                    drawLine(
                                        color = checkColor,
                                        start = start,
                                        end = middle,
                                        strokeWidth = strokeWidth,
                                        cap = StrokeCap.Round,
                                    )
                                    drawLine(
                                        color = checkColor,
                                        start = middle,
                                        end = end,
                                        strokeWidth = strokeWidth,
                                        cap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            if (
                showAxisLabels &&
                    boardDisplayWindow.columnCount > 0 &&
                    boardDisplayWindow.rowCount > 0 &&
                    axisDisplayWindow.columnCount > 0 &&
                    axisDisplayWindow.rowCount > 0
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (surfaceSize == IntSize.Zero || canvasSizePx == IntSize.Zero) {
                        return@Canvas
                    }

                    val boardLeft = canvasTopLeft.x
                    val boardTop = canvasTopLeft.y
                    val boardWidth = canvasSizePx.width.toFloat()
                    val boardHeight = canvasSizePx.height.toFloat()
                    if (boardWidth <= 0f || boardHeight <= 0f) {
                        return@Canvas
                    }

                    val boardRight = boardLeft + boardWidth
                    val boardBottom = boardTop + boardHeight
                    val cellWidth = boardWidth / boardDisplayWindow.columnCount
                    val cellHeight = boardHeight / boardDisplayWindow.rowCount
                    val axisLeft = boardLeft + (axisDisplayWindow.startColumn - boardDisplayWindow.startColumn) * cellWidth
                    val axisTop = boardTop + (axisDisplayWindow.startRow - boardDisplayWindow.startRow) * cellHeight
                    val axisRight = boardLeft + (axisDisplayWindow.endColumn - boardDisplayWindow.startColumn + 1) * cellWidth
                    val axisBottom = boardTop + (axisDisplayWindow.endRow - boardDisplayWindow.startRow + 1) * cellHeight
                    val minCellEdge = min(cellWidth, cellHeight)
                    val viewportWidth = size.width
                    val viewportHeight = size.height
                    val textSize = (minCellEdge * 0.44f).coerceIn(10f, 18f)
                    val rowDigits = axisDisplayWindow.rowCount.toString().length.coerceAtLeast(1)
                    val columnDigits = axisDisplayWindow.columnCount.toString().length.coerceAtLeast(1)
                    val rowGutterWidth = max(with(density) { 24.dp.toPx() }, textSize * rowDigits * 0.66f + with(density) { 12.dp.toPx() })
                    val columnGutterHeight = max(with(density) { 22.dp.toPx() }, textSize * 1.55f + with(density) { 8.dp.toPx() })
                    val blurRadius = (minCellEdge * 0.32f).coerceIn(8f, 18f)
                    val rowVisibleRange =
                        visibleAxisRange(
                            boardStart = axisTop,
                            cellExtent = cellHeight,
                            totalCount = axisDisplayWindow.rowCount,
                            viewportExtent = viewportHeight,
                        )
                    val columnVisibleRange =
                        visibleAxisRange(
                            boardStart = axisLeft,
                            cellExtent = cellWidth,
                            totalCount = axisDisplayWindow.columnCount,
                            viewportExtent = viewportWidth,
                        )

                    fun drawAxisBackdrop(
                        left: Float,
                        top: Float,
                        right: Float,
                        bottom: Float,
                    ) {
                        if (right <= left || bottom <= top) {
                            return
                        }
                        val overlapsBoard =
                            left < boardRight &&
                                right > boardLeft &&
                                top < boardBottom &&
                                bottom > boardTop

                        if (overlapsBoard) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val blurPaint =
                                    AndroidPaint().apply {
                                        maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                    }
                                val saveCount = drawContext.canvas.nativeCanvas.saveLayer(left, top, right, bottom, blurPaint)
                                clipRect(left, top, right, bottom) {
                                    drawBoardCellsAndGrid(
                                        project = project,
                                        displayWindow = boardDisplayWindow,
                                        boardLeft = boardLeft,
                                        boardTop = boardTop,
                                        cellWidth = cellWidth,
                                        cellHeight = cellHeight,
                                        highlightRow = highlightRow,
                                        highlightColumn = highlightColumn,
                                        highlightColorCode = highlightColorCode,
                                        highlightIndices = highlightIndices,
                                        completedIndices = completedIndices,
                                        dimCompletedIndices = dimCompletedIndices,
                                    )
                                }
                                drawContext.canvas.nativeCanvas.restoreToCount(saveCount)
                            }
                        }

                        drawRect(
                            color = axisPanelColor.copy(alpha = if (overlapsBoard) 0.74f else 0.92f),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                        )
                        drawRect(
                            color = axisOutlineColor.copy(alpha = 0.35f),
                            topLeft = Offset(left, top),
                            size = Size(right - left, 1f),
                        )
                    }

                    val leftGutterLeft = if (axisLeft >= rowGutterWidth) axisLeft - rowGutterWidth else 0f
                    val rightGutterLeft = if (axisRight + rowGutterWidth <= viewportWidth) axisRight else viewportWidth - rowGutterWidth
                    val sideTop = max(0f, axisTop)
                    val sideBottom = min(viewportHeight, axisBottom)
                    val topGutterTop = if (axisTop >= columnGutterHeight) axisTop - columnGutterHeight else 0f
                    val bottomGutterTop = if (axisBottom + columnGutterHeight <= viewportHeight) axisBottom else viewportHeight - columnGutterHeight
                    val horizontalLeft = max(0f, axisLeft)
                    val horizontalRight = min(viewportWidth, axisRight)

                    drawAxisBackdrop(leftGutterLeft, sideTop, leftGutterLeft + rowGutterWidth, sideBottom)
                    drawAxisBackdrop(rightGutterLeft, sideTop, rightGutterLeft + rowGutterWidth, sideBottom)
                    drawAxisBackdrop(horizontalLeft, topGutterTop, horizontalRight, topGutterTop + columnGutterHeight)
                    drawAxisBackdrop(horizontalLeft, bottomGutterTop, horizontalRight, bottomGutterTop + columnGutterHeight)

                    val textPaint =
                        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            textAlign = AndroidPaint.Align.CENTER
                            isFakeBoldText = true
                            isSubpixelText = true
                            setShadowLayer(3f, 0f, 0f, android.graphics.Color.argb(48, 0, 0, 0))
                            this.textSize = textSize
                        }
                    val rowStep = axisLabelStep(cellHeight, textSize * 1.2f + with(density) { 4.dp.toPx() })
                    val columnStep = axisLabelStep(cellWidth, textSize * columnDigits * 0.68f + with(density) { 6.dp.toPx() })

                    rowVisibleRange?.forEach { displayRow ->
                        val originalRow = axisDisplayWindow.startRow + displayRow
                        val visibleRowNumber = displayRow + 1
                        val shouldShow =
                            displayRow == 0 ||
                                displayRow == axisDisplayWindow.rowCount - 1 ||
                                displayRow % rowStep == 0
                        if (!shouldShow) {
                            return@forEach
                        }
                        val labelCenterY = axisTop + (displayRow + 0.5f) * cellHeight
                        textPaint.color = if (highlightRow == originalRow) axisHighlightColor.toArgb() else axisTextColor.toArgb()
                        val baseline = labelCenterY - (textPaint.descent() + textPaint.ascent()) / 2f
                        drawContext.canvas.nativeCanvas.drawText(visibleRowNumber.toString(), leftGutterLeft + rowGutterWidth / 2f, baseline, textPaint)
                        drawContext.canvas.nativeCanvas.drawText(visibleRowNumber.toString(), rightGutterLeft + rowGutterWidth / 2f, baseline, textPaint)
                    }

                    columnVisibleRange?.forEach { displayColumn ->
                        val originalColumn = axisDisplayWindow.startColumn + displayColumn
                        val visibleColumnNumber = displayColumn + 1
                        val shouldShow =
                            displayColumn == 0 ||
                                displayColumn == axisDisplayWindow.columnCount - 1 ||
                                displayColumn % columnStep == 0
                        if (!shouldShow) {
                            return@forEach
                        }
                        val labelCenterX = axisLeft + (displayColumn + 0.5f) * cellWidth
                        textPaint.color = if (highlightColumn == originalColumn) axisHighlightColor.toArgb() else axisTextColor.toArgb()
                        val topBaseline = topGutterTop + columnGutterHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                        val bottomBaseline = bottomGutterTop + columnGutterHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                        drawContext.canvas.nativeCanvas.drawText(visibleColumnNumber.toString(), labelCenterX, topBaseline, textPaint)
                        drawContext.canvas.nativeCanvas.drawText(visibleColumnNumber.toString(), labelCenterX, bottomBaseline, textPaint)
                    }
                }
            }
        }
    }
}

private const val OUTPUT_MIN_SHORT_EDGE = 4
private const val OUTPUT_MAX_SHORT_EDGE = 124
private const val OUTPUT_DEFAULT_SHORT_EDGE = 20
private const val OUTPUT_PREVIEW_THROTTLE_MS = 100L
private const val DEFAULT_BOARD_ZOOM = 20f
private const val BOARD_ZOOM_MIN = 6f
private const val BOARD_ZOOM_MAX = 96f
private val BOARD_ZOOM_RANGE = BOARD_ZOOM_MIN..BOARD_ZOOM_MAX
private val BOARD_DRAG_BUFFER_START = 88.dp
private val BOARD_DRAG_BUFFER_END = 88.dp
private val BOARD_DRAG_BUFFER_TOP = 112.dp
private val BOARD_DRAG_BUFFER_BOTTOM = 240.dp

private fun centeredBoardViewport(): BoardViewportState {
    return BoardViewportState(zoom = DEFAULT_BOARD_ZOOM, centerOnRestore = true)
}

private fun buildModeLabel(mode: BuildMode): String {
    return when (mode) {
        BuildMode.Color -> "按颜色"
        BuildMode.ColorBlock -> "按颜色区块"
        BuildMode.Row -> "按行"
        BuildMode.Column -> "按列"
    }
}

private data class BuildModeSelection(
    val indices: Set<Int>,
    val description: String,
)

private data class ColorBlockSelection(
    val colorCode: String,
    val blockIndex: Int,
)

private data class BoardDisplayWindow(
    val startColumn: Int,
    val endColumn: Int,
    val startRow: Int,
    val endRow: Int,
) {
    val columnCount: Int
        get() = (endColumn - startColumn + 1).coerceAtLeast(0)
    val rowCount: Int
        get() = (endRow - startRow + 1).coerceAtLeast(0)

    fun coerceRowIndex(row: Int): Int = row.coerceIn(startRow, endRow.coerceAtLeast(startRow))

    fun coerceColumnIndex(column: Int): Int = column.coerceIn(startColumn, endColumn.coerceAtLeast(startColumn))

    fun toVisibleRowIndex(row: Int): Int = (coerceRowIndex(row) - startRow).coerceAtLeast(0)

    fun toVisibleColumnIndex(column: Int): Int = (coerceColumnIndex(column) - startColumn).coerceAtLeast(0)

    fun toVisibleRowNumber(row: Int): Int = toVisibleRowIndex(row) + 1

    fun toVisibleColumnNumber(column: Int): Int = toVisibleColumnIndex(column) + 1
}

private fun computeBoardDisplayWindow(
    project: BeadProject,
    trimOuterEmpty: Boolean,
): BoardDisplayWindow {
    if (!trimOuterEmpty || project.width <= 0 || project.height <= 0) {
        return BoardDisplayWindow(
            startColumn = 0,
            endColumn = (project.width - 1).coerceAtLeast(0),
            startRow = 0,
            endRow = (project.height - 1).coerceAtLeast(0),
        )
    }

    var minColumn = project.width
    var maxColumn = -1
    var minRow = project.height
    var maxRow = -1

    project.cells.forEachIndexed { index, code ->
        if (code == null) {
            return@forEachIndexed
        }
        val row = index / project.width
        val column = index % project.width
        minColumn = min(minColumn, column)
        maxColumn = max(maxColumn, column)
        minRow = min(minRow, row)
        maxRow = max(maxRow, row)
    }

    if (maxColumn < 0 || maxRow < 0) {
        return BoardDisplayWindow(
            startColumn = 0,
            endColumn = (project.width - 1).coerceAtLeast(0),
            startRow = 0,
            endRow = (project.height - 1).coerceAtLeast(0),
        )
    }

    return BoardDisplayWindow(
        startColumn = minColumn,
        endColumn = maxColumn,
        startRow = minRow,
        endRow = maxRow,
    )
}

private fun shouldDimBoardCell(
    row: Int,
    column: Int,
    code: String?,
    index: Int,
    highlightRow: Int?,
    highlightColumn: Int?,
    highlightColorCode: String?,
    highlightIndices: Set<Int>?,
    completedIndices: Set<Int>,
    dimCompletedIndices: Boolean,
): Boolean {
    val isCompleted = index in completedIndices
    return (highlightRow != null && row != highlightRow) ||
        (highlightColumn != null && column != highlightColumn) ||
        (highlightColorCode != null && code != highlightColorCode) ||
        (highlightIndices != null && index !in highlightIndices) ||
        (dimCompletedIndices && isCompleted)
}

private fun DrawScope.drawBoardCellsAndGrid(
    project: BeadProject,
    displayWindow: BoardDisplayWindow,
    boardLeft: Float,
    boardTop: Float,
    cellWidth: Float,
    cellHeight: Float,
    highlightRow: Int?,
    highlightColumn: Int?,
    highlightColorCode: String?,
    highlightIndices: Set<Int>?,
    completedIndices: Set<Int>,
    dimCompletedIndices: Boolean,
) {
    if (displayWindow.columnCount <= 0 || displayWindow.rowCount <= 0) {
        return
    }

    for (displayRow in 0 until displayWindow.rowCount) {
        val row = displayWindow.startRow + displayRow
        for (displayColumn in 0 until displayWindow.columnCount) {
            val column = displayWindow.startColumn + displayColumn
            val index = row * project.width + column
            val code = project.cells.getOrNull(index)
            val baseColor = BeadPalette.find(code)?.let { Color(it.argb) } ?: Color(0xFFF2EBDD)
            val shouldDim =
                shouldDimBoardCell(
                    row = row,
                    column = column,
                    code = code,
                    index = index,
                    highlightRow = highlightRow,
                    highlightColumn = highlightColumn,
                    highlightColorCode = highlightColorCode,
                    highlightIndices = highlightIndices,
                    completedIndices = completedIndices,
                    dimCompletedIndices = dimCompletedIndices,
                )
            drawRect(
                color = if (shouldDim) baseColor.copy(alpha = 0.22f) else baseColor,
                topLeft = Offset(boardLeft + displayColumn * cellWidth, boardTop + displayRow * cellHeight),
                size = Size(cellWidth, cellHeight),
            )
        }
    }

    for (displayColumn in 0..displayWindow.columnCount) {
        val originalBoundaryColumn = displayWindow.startColumn + displayColumn
        val isMajorLine = originalBoundaryColumn % 5 == 0
        val x = boardLeft + displayColumn * cellWidth
        drawLine(
            color = if (isMajorLine) Color(0xFF997A58) else Color(0xFFD8C9B9),
            start = Offset(x, boardTop),
            end = Offset(x, boardTop + displayWindow.rowCount * cellHeight),
            strokeWidth = if (isMajorLine) 2f else 1f,
            cap = StrokeCap.Round,
        )
    }
    for (displayRow in 0..displayWindow.rowCount) {
        val originalBoundaryRow = displayWindow.startRow + displayRow
        val isMajorLine = originalBoundaryRow % 5 == 0
        val y = boardTop + displayRow * cellHeight
        drawLine(
            color = if (isMajorLine) Color(0xFF997A58) else Color(0xFFD8C9B9),
            start = Offset(boardLeft, y),
            end = Offset(boardLeft + displayWindow.columnCount * cellWidth, y),
            strokeWidth = if (isMajorLine) 2f else 1f,
            cap = StrokeCap.Round,
        )
    }
}

private fun axisLabelStep(
    cellExtent: Float,
    minimumSpan: Float,
): Int {
    if (cellExtent <= 0f) {
        return 1
    }
    return ceil(minimumSpan / cellExtent).toInt().coerceAtLeast(1)
}

private fun visibleAxisRange(
    boardStart: Float,
    cellExtent: Float,
    totalCount: Int,
    viewportExtent: Float,
): IntRange? {
    if (totalCount <= 0 || cellExtent <= 0f) {
        return null
    }
    val boardEnd = boardStart + totalCount * cellExtent
    if (boardEnd <= 0f || boardStart >= viewportExtent) {
        return null
    }
    val start = floor((0f - boardStart) / cellExtent).toInt().coerceIn(0, totalCount - 1)
    val end = floor((viewportExtent - boardStart) / cellExtent).toInt().coerceIn(0, totalCount - 1)
    if (end < start) {
        return null
    }
    return start..end
}

private fun resolveBuildModeSelection(
    project: BeadProject,
    buildMode: BuildMode,
    startIndex: Int,
): BuildModeSelection? {
    if (startIndex !in project.cells.indices) {
        return null
    }
    val focusWindow = computeBoardDisplayWindow(project = project, trimOuterEmpty = true)
    return when (buildMode) {
        BuildMode.Row -> {
            val row = startIndex / project.width
            val indices =
                (0 until project.width)
                    .map { column -> row * project.width + column }
                    .filter { index -> project.cells.getOrNull(index) != null }
                    .toSet()
            if (indices.isEmpty()) {
                null
            } else {
                BuildModeSelection(
                    indices = indices,
                    description = "第 ${focusWindow.toVisibleRowNumber(row)} 行",
                )
            }
        }
        BuildMode.Column -> {
            val column = startIndex % project.width
            val indices =
                (0 until project.height)
                    .map { row -> row * project.width + column }
                    .filter { index -> project.cells.getOrNull(index) != null }
                    .toSet()
            if (indices.isEmpty()) {
                null
            } else {
                BuildModeSelection(
                    indices = indices,
                    description = "第 ${focusWindow.toVisibleColumnNumber(column)} 列",
                )
            }
        }
        BuildMode.Color -> {
            val colorCode = project.cells[startIndex] ?: return null
            val indices =
                project.cells
                    .mapIndexedNotNull { index, code -> if (code == colorCode) index else null }
                    .toSet()
            BuildModeSelection(indices = indices, description = "颜色 ${colorCodeLabel(colorCode)}")
        }
        BuildMode.ColorBlock -> {
            val colorCode = project.cells[startIndex] ?: return null
            val selection =
                resolveColorBlockSelection(
                    cells = project.cells,
                    width = project.width,
                    height = project.height,
                    startIndex = startIndex,
                ) ?: return null
            val indices = EditorTools.collectConnectedCellIndices(project.cells, project.width, project.height, startIndex).toSet()
            val totalBlockCount = collectColorBlockIndices(project.cells, project.width, project.height, colorCode).size
            BuildModeSelection(
                indices = indices,
                description = "颜色 ${colorCodeLabel(colorCode)} 的第 ${selection.blockIndex + 1}/$totalBlockCount 个区块",
            )
        }
    }
}

private fun collectColorBlockIndices(
    cells: List<String?>,
    width: Int,
    height: Int,
    colorCode: String?,
): List<List<Int>> {
    if (colorCode == null) {
        return emptyList()
    }
    val remaining =
        cells.indices
            .filter { cells[it] == colorCode }
            .toMutableSet()
    val blocks = mutableListOf<List<Int>>()
    while (remaining.isNotEmpty()) {
        val startIndex = remaining.first()
        val block = EditorTools.collectConnectedCellIndices(cells, width, height, startIndex)
        remaining.removeAll(block.toSet())
        blocks += block
    }
    return blocks.sortedByDescending { it.size }
}

private fun resolveColorBlockSelection(
    cells: List<String?>,
    width: Int,
    height: Int,
    startIndex: Int,
): ColorBlockSelection? {
    val colorCode = cells.getOrNull(startIndex) ?: return null
    val selectedBlock = EditorTools.collectConnectedCellIndices(cells, width, height, startIndex)
    if (selectedBlock.isEmpty()) {
        return null
    }
    val selectedIndices = selectedBlock.toSet()
    val blocks = collectColorBlockIndices(cells, width, height, colorCode)
    val blockIndex =
        blocks.indexOfFirst { block ->
            block.size == selectedBlock.size && block.toSet() == selectedIndices
        }.takeIf { it >= 0 } ?: 0
    return ColorBlockSelection(colorCode = colorCode, blockIndex = blockIndex)
}

private fun outputLevelFromSettings(settings: GenerationSettings): Int {
    return minOf(settings.width, settings.height).coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
}

private fun settingsForOutputLevel(
    current: GenerationSettings,
    baseWidth: Int,
    baseHeight: Int,
    level: Int,
): GenerationSettings {
    val targetEdge = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
    val safeBaseWidth = baseWidth.coerceAtLeast(1)
    val safeBaseHeight = baseHeight.coerceAtLeast(1)
    return if (safeBaseWidth >= safeBaseHeight) {
        current.copy(
            width = (targetEdge * safeBaseWidth / safeBaseHeight.toFloat()).roundToInt().coerceAtLeast(1),
            height = targetEdge,
        )
    } else {
        current.copy(
            width = targetEdge,
            height = (targetEdge * safeBaseHeight / safeBaseWidth.toFloat()).roundToInt().coerceAtLeast(1),
        )
    }
}

private fun colorCodeLabel(code: String?): String = BeadPalette.displayCode(code)

private val ProjectDefaultTitlePattern = Regex("^项目(\\d+)$")

private fun nextProjectDefaultTitle(projects: List<BeadProject>): String {
    val usedNumbers =
        projects
            .mapNotNull { project ->
                ProjectDefaultTitlePattern
                    .matchEntire(project.title.trim())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
            }.toSet()
    val nextNumber = generateSequence(1) { it + 1 }.first { it !in usedNumbers }
    return "项目$nextNumber"
}

private fun defaultSettings(defaultTitle: String = "项目1"): GenerationSettings {
    return applyColorScheme(
        GenerationSettings(title = defaultTitle),
        ColorSchemePreset.MardBrand221,
    )
}

private fun applyColorScheme(
    settings: GenerationSettings,
    scheme: ColorSchemePreset,
): GenerationSettings {
    return settings.copy(
        colorScheme = scheme,
        paletteMode = scheme.paletteMode,
        maxColors = scheme.maxColors,
    )
}

private fun applyAspectRatio(
    settings: GenerationSettings,
    ratioX: Int,
    ratioY: Int,
): GenerationSettings {
    val safeRatioX = ratioX.coerceAtLeast(1)
    val safeRatioY = ratioY.coerceAtLeast(1)
    val baseEdge = max(settings.width, settings.height).coerceAtLeast(1)

    return if (safeRatioX >= safeRatioY) {
        settings.copy(
            width = baseEdge,
            height = (baseEdge * safeRatioY / safeRatioX.toFloat()).roundToInt().coerceAtLeast(1),
        )
    } else {
        settings.copy(
            width = (baseEdge * safeRatioX / safeRatioY.toFloat()).roundToInt().coerceAtLeast(1),
            height = baseEdge,
        )
    }
}

private fun toolModeLabel(mode: ToolMode): String {
    return when (mode) {
        ToolMode.Pencil -> "画笔"
        ToolMode.Eraser -> "橡皮"
        ToolMode.Fill -> "填充"
        ToolMode.Remove -> "移除"
        ToolMode.Recolor -> "颜色替换"
        ToolMode.Picker -> "取色"
        ToolMode.Mirror -> "镜像"
    }
}

private fun removeModeLabel(mode: RemoveMode): String {
    return when (mode) {
        RemoveMode.SameConnected -> "连通同色"
        RemoveMode.AllSameColor -> "全部同色"
        RemoveMode.Connected -> "连通区域"
    }
}

private fun mirrorDirectionLabel(direction: MirrorDirection): String {
    return if (direction == MirrorDirection.Horizontal) "水平镜像" else "垂直镜像"
}

private fun rotationDirectionLabel(direction: RotationDirection): String {
    return if (direction == RotationDirection.Clockwise) "顺时针旋转" else "逆时针旋转"
}

private fun canvasExpandDirectionLabel(direction: CanvasExpandDirection): String {
    return when (direction) {
        CanvasExpandDirection.Up -> "上"
        CanvasExpandDirection.Down -> "下"
        CanvasExpandDirection.Left -> "左"
        CanvasExpandDirection.Right -> "右"
    }
}

private fun canvasExpandDirectionActionText(direction: CanvasExpandDirection): String {
    return when (direction) {
        CanvasExpandDirection.Up -> "已向上扩展画布，新增 1 行空白行。"
        CanvasExpandDirection.Down -> "已向下扩展画布，新增 1 行空白行。"
        CanvasExpandDirection.Left -> "已向左扩展画布，新增 1 列空白列。"
        CanvasExpandDirection.Right -> "已向右扩展画布，新增 1 列空白列。"
    }
}

private fun toolModeHint(
    toolMode: ToolMode,
    removeMode: RemoveMode,
    mirrorDirection: MirrorDirection,
): String {
    return when (toolMode) {
        ToolMode.Pencil -> "点击网格把当前选中颜色画上去。"
        ToolMode.Eraser -> "点击网格删除当前珠子。"
        ToolMode.Fill -> "点击目标区域，按四向连通规则填充为当前颜色。"
        ToolMode.Remove -> "点击目标区域后按 ${removeModeLabel(removeMode)} 规则移除。"
        ToolMode.Recolor -> "先在色板选替换后的颜色，再点图上的原颜色即可做颜色替换。"
        ToolMode.Picker -> "点击图上的珠子吸取颜色。"
        ToolMode.Mirror -> "点击连通区域按 ${mirrorDirectionLabel(mirrorDirection)} 镜像，也可以直接镜像整张图纸。"
    }
}

private fun loadBitmapFromUri(
    context: Context,
    uri: Uri,
): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width.coerceAtLeast(1)
                val height = info.size.height.coerceAtLeast(1)
                val maxEdge = maxOf(width, height)
                if (maxEdge > 2048) {
                    val scale = 2048f / maxEdge.toFloat()
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1),
                    )
                }
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                decoder.setMemorySizePolicy(ImageDecoder.MEMORY_POLICY_LOW_RAM)
                decoder.isMutableRequired = false
            }
        } else {
            decodeBitmapPreAndroidP(context, uri)
        }
    }.getOrNull()
}

private fun decodeBitmapPreAndroidP(
    context: Context,
    uri: Uri,
): Bitmap? {
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val options =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 2048)
        }

    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxEdge: Int,
): Int {
    val safeMaxEdge = maxEdge.coerceAtLeast(1)
    val sourceMaxEdge = maxOf(width, height).coerceAtLeast(1)
    var sampleSize = 1

    while (sourceMaxEdge / sampleSize > safeMaxEdge) {
        sampleSize *= 2
    }

    return sampleSize
}

