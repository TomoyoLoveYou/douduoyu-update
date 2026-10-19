@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.douduoyu.app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.Draggable2DState
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
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
    Grid,
    Options,
    Output,
    Logic,
    Usage,
    Export,
    Zoom,
    Fullscreen,
}

private enum class CanvasAdjustMode {
    Add,
    Remove,
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
    Grid,
    Focus,
    Usage,
    MarkDone,
    Fullscreen,
}

private data class GridGuideColorOption(
    val label: String,
    val argb: Int,
)

private enum class GridGuideToolSection {
    Size,
    Thickness,
    Color,
}

private val GRID_GUIDE_COLOR_OPTIONS =
    listOf(
        GridGuideColorOption("棕色", 0xFF997A58.toInt()),
        GridGuideColorOption("黑色", 0xFF211D19.toInt()),
        GridGuideColorOption("白色", 0xFFF7F2E8.toInt()),
        GridGuideColorOption("红色", 0xFFC74450.toInt()),
        GridGuideColorOption("蓝色", 0xFF3569B7.toInt()),
        GridGuideColorOption("绿色", 0xFF2E7D5B.toInt()),
        GridGuideColorOption("紫色", 0xFF7657A6.toInt()),
        GridGuideColorOption("橙色", 0xFFD9782D.toInt()),
    )

private enum class MainTab {
    Projects,
    Create,
    Settings,
}

private val MAIN_TABS = listOf(MainTab.Projects, MainTab.Create, MainTab.Settings)

private enum class CreateFlowKind {
    ImageToPattern,
    ChartRecognition,
}

private enum class SettingsPage {
    Home,
    Redemption,
    Celebration,
    ColorConfidence,
    Debt,
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
    val canvasTransitionFromPrevious: BoardCanvasAnchorTransition? = null,
)

private const val MAX_EDITOR_HISTORY_CELL_REFERENCES = 1_048_576L
private const val MAX_EDITOR_HISTORY_SNAPSHOTS = 64
private const val MIN_EDITOR_HISTORY_SNAPSHOTS = 2

private fun appendEditorHistorySnapshot(
    history: List<PatternEditorSnapshot>,
    snapshot: PatternEditorSnapshot,
): List<PatternEditorSnapshot> {
    val appended = history + snapshot
    var firstKeptIndex = appended.lastIndex
    var keptSnapshotCount = 1
    var keptCellReferences = snapshot.project.cells.size.toLong()

    while (firstKeptIndex > 0 && keptSnapshotCount < MAX_EDITOR_HISTORY_SNAPSHOTS) {
        val candidateIndex = firstKeptIndex - 1
        val candidateCellReferences = appended[candidateIndex].project.cells.size.toLong()
        val mustKeepPrevious = keptSnapshotCount < MIN_EDITOR_HISTORY_SNAPSHOTS
        if (
            !mustKeepPrevious &&
            keptCellReferences + candidateCellReferences > MAX_EDITOR_HISTORY_CELL_REFERENCES
        ) {
            break
        }
        firstKeptIndex = candidateIndex
        keptSnapshotCount += 1
        keptCellReferences += candidateCellReferences
    }

    return if (firstKeptIndex == 0) appended else appended.subList(firstKeptIndex, appended.size).toList()
}

private data class BoardCanvasAnchorTransition(
    val oldAnchorX: Float,
    val oldAnchorY: Float,
    val newAnchorX: Float,
    val newAnchorY: Float,
)

private data class SurfaceTapRecord(
    val timeMillis: Long,
    val position: Offset,
)

private data class BoardCanvasGeometry(
    val projectId: String,
    val topLeftInRoot: Offset,
    val size: IntSize,
    val viewportSize: IntSize,
    val cellSizePx: Float,
    val horizontalPanToScrollScale: Float,
    val verticalPanToScrollScale: Float,
    val gridWidth: Int,
    val gridHeight: Int,
)

private data class PendingBoardCanvasAdjustment(
    val projectId: String,
    val oldGeometry: BoardCanvasGeometry,
    val oldWidth: Int,
    val oldHeight: Int,
    val oldAnchorX: Float,
    val oldAnchorY: Float,
    val newAnchorX: Float,
    val newAnchorY: Float,
    val expectedWidth: Int,
    val expectedHeight: Int,
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
            val customColorSchemeStore = remember(context) { CustomColorSchemeStore(context) }
            val initialCustomColorSchemes =
                remember(customColorSchemeStore) {
                    customColorSchemeStore.loadAll().also(BeadPalette::registerCustomColorSchemes)
                }
            val appIconManager = remember(context) { AppIconManager(context) }
            val birthdayCelebrationStore = remember(context) { BirthdayCelebrationStore(context) }
            val birthdayAnimationDeveloperStore = remember(context) { BirthdayAnimationDeveloperStore(context) }

            var screen by remember { mutableStateOf(AppScreen.Home) }
            var customColorSchemes by remember { mutableStateOf(initialCustomColorSchemes) }
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
            var buildCompletedCellIndices by remember { mutableStateOf(CellIndexMask.empty()) }
            var boardZoom by remember { mutableFloatStateOf(DEFAULT_BOARD_ZOOM) }
            var gridGuideSettings by remember { mutableStateOf(store.loadGridGuideSettings()) }
            val boardHorizontalScroll = rememberScrollState()
            val boardVerticalScroll = rememberScrollState()
            var editorBoardCanvasGeometry by remember { mutableStateOf<BoardCanvasGeometry?>(null) }
            var pendingBoardCanvasAdjustment by remember { mutableStateOf<PendingBoardCanvasAdjustment?>(null) }
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
            var isBirthdayCelebrationRedeemed by remember {
                mutableStateOf(birthdayCelebrationStore.isRedeemed())
            }
            var isAnimationDeveloperModeEnabled by remember {
                mutableStateOf(birthdayAnimationDeveloperStore.isUnlocked())
            }
            var animationQualityOverride by remember {
                mutableStateOf(birthdayAnimationDeveloperStore.qualityOverride())
            }
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
            var pendingProjectActions by remember { mutableStateOf<BeadProject?>(null) }
            var pendingEditorStitchProject by remember { mutableStateOf<BeadProject?>(null) }
            var pendingEditorSplitProject by remember { mutableStateOf<BeadProject?>(null) }
            var showStitchProjectLimitNotice by remember { mutableStateOf(false) }
            var splitProjectLimitMessage by remember { mutableStateOf<String?>(null) }
            var renameDraftTitle by remember { mutableStateOf("") }
            var stitchProjects by remember { mutableStateOf(emptyList<BeadProject>()) }
            var stitchInitialProjectId by remember { mutableStateOf<String?>(null) }
            var stitchReturnScreen by remember { mutableStateOf(AppScreen.Home) }
            var splitProject by remember { mutableStateOf<BeadProject?>(null) }
            var splitReturnScreen by remember { mutableStateOf(AppScreen.Home) }
            var settingsPage by remember { mutableStateOf(SettingsPage.Home) }
            var isSettingsPickerOpen by remember { mutableStateOf(false) }
            var isCustomColorSchemeManagerOpen by remember { mutableStateOf(false) }
            var customColorSchemeInitialEditId by remember { mutableStateOf<String?>(null) }
            var customColorSchemeStartCreating by remember { mutableStateOf(false) }
            var pendingGallerySaveProject by remember { mutableStateOf<BeadProject?>(null) }
            var pendingGalleryGridSettings by remember { mutableStateOf<GridGuideSettings?>(null) }

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

            LaunchedEffect(
                screen,
                currentProject?.id,
                currentProject?.width,
                currentProject?.height,
                editorBoardCanvasGeometry,
                pendingBoardCanvasAdjustment,
            ) {
                val project = currentProject
                if (screen != AppScreen.PatternEditor || project == null) {
                    pendingBoardCanvasAdjustment = null
                    editorBoardCanvasGeometry = null
                    return@LaunchedEffect
                }
                val adjustment = pendingBoardCanvasAdjustment ?: return@LaunchedEffect
                if (project.id != adjustment.projectId) {
                    pendingBoardCanvasAdjustment = null
                    editorBoardCanvasGeometry = null
                    return@LaunchedEffect
                }
                val geometry = editorBoardCanvasGeometry ?: return@LaunchedEffect
                if (geometry.projectId != project.id) {
                    return@LaunchedEffect
                }
                if (
                    project.width != adjustment.expectedWidth ||
                    project.height != adjustment.expectedHeight
                ) {
                    pendingBoardCanvasAdjustment = null
                    return@LaunchedEffect
                }
                if (
                    geometry.gridWidth != adjustment.expectedWidth ||
                    geometry.gridHeight != adjustment.expectedHeight
                ) {
                    return@LaunchedEffect
                }
                val oldCellWidth = adjustment.oldGeometry.size.width.toFloat() / adjustment.oldWidth.coerceAtLeast(1)
                val oldCellHeight = adjustment.oldGeometry.size.height.toFloat() / adjustment.oldHeight.coerceAtLeast(1)
                val newCellWidth = geometry.size.width.toFloat() / project.width.coerceAtLeast(1)
                val newCellHeight = geometry.size.height.toFloat() / project.height.coerceAtLeast(1)
                val oldPoint =
                    adjustment.oldGeometry.topLeftInRoot +
                        Offset(adjustment.oldAnchorX * oldCellWidth, adjustment.oldAnchorY * oldCellHeight)
                val newPoint =
                    geometry.topLeftInRoot +
                        Offset(adjustment.newAnchorX * newCellWidth, adjustment.newAnchorY * newCellHeight)
                val delta = newPoint - oldPoint
                if (delta.x != 0f) {
                    boardHorizontalScroll.dispatchRawDelta(delta.x * geometry.horizontalPanToScrollScale)
                }
                if (delta.y != 0f) {
                    boardVerticalScroll.dispatchRawDelta(delta.y * geometry.verticalPanToScrollScale)
                }
                pendingBoardCanvasAdjustment = null
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
                customColorSchemeId: String? = null,
            ): String {
                return BeadPalette.palette(scheme, customColorSchemeId).firstOrNull()?.id
                    ?: BeadPalette.palette(mode).firstOrNull()?.id
                    ?: BeadPalette.completeColors.first().id
            }

            fun persistCustomColorSchemes(next: List<CustomColorScheme>) {
                val normalized =
                    next
                        .mapNotNull(CustomColorScheme::normalizedOrNull)
                        .distinctBy { it.id }
                        .sortedByDescending { it.updatedAt }
                customColorSchemeStore.saveAll(normalized)
                BeadPalette.registerCustomColorSchemes(normalized)
                customColorSchemes = normalized
            }

            fun saveCustomColorScheme(scheme: CustomColorScheme) {
                val now = System.currentTimeMillis()
                val existing = customColorSchemes.firstOrNull { it.id == scheme.id }
                val targetGroupId = scheme.customGroupId.ifBlank { scheme.id }
                val requestedName = scheme.name.trim()
                if (isBuiltInColorSchemeName(requestedName)) {
                    statusText = "自定义品牌不能与内置品牌或方案重名。"
                    return
                }
                if (
                    customColorSchemes.any {
                        !it.archived &&
                            it.customGroupId != targetGroupId &&
                            normalizedColorSchemeName(it.name) == normalizedColorSchemeName(requestedName)
                    }
                ) {
                    statusText = "已存在同名自定义品牌。"
                    return
                }
                val candidate =
                    scheme
                        .copy(
                            createdAt = existing?.createdAt ?: scheme.createdAt,
                            updatedAt = now,
                            archived = false,
                        ).normalizedOrNull()
                        ?: return
                val selectionChanged =
                    existing != null &&
                        (existing.baseScheme != candidate.baseScheme || existing.selectedCodes != candidate.selectedCodes)
                val overridesChanged = existing != null && existing.colorOverrides != candidate.colorOverrides
                val paletteChanged = selectionChanged || overridesChanged
                val saved: CustomColorScheme
                val retainedSchemesBeforeRename: List<CustomColorScheme>
                if (overridesChanged && existing != null) {
                    val activeGroup =
                        customColorSchemes.filter { !it.archived && it.customGroupId == existing.customGroupId }
                    val versionByOriginalId =
                        activeGroup.associate { original ->
                            val source = if (original.id == existing.id) candidate else original
                            original.id to
                                source.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    colorOverrides = candidate.colorOverrides,
                                    createdAt = now,
                                    updatedAt = now,
                                    archived = false,
                                )
                        }
                    saved = versionByOriginalId.getValue(existing.id)
                    val activeIds = activeGroup.mapTo(hashSetOf()) { it.id }
                    retainedSchemesBeforeRename =
                        versionByOriginalId.values.filterNot { it.id == saved.id } +
                            activeGroup.map { it.copy(archived = true, updatedAt = now) } +
                            customColorSchemes.filterNot { it.id in activeIds }
                } else {
                    saved =
                        if (selectionChanged) {
                            candidate.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                createdAt = now,
                                updatedAt = now,
                            )
                        } else {
                            candidate
                        }
                    retainedSchemesBeforeRename =
                        if (selectionChanged && existing != null) {
                            listOf(existing.copy(archived = true, updatedAt = now)) +
                                customColorSchemes.filterNot { it.id == existing.id }
                        } else {
                            customColorSchemes.filterNot { it.id == saved.id }
                        }
                }
                val groupNameChanged = existing != null && existing.name != saved.name
                val retainedSchemes =
                    if (groupNameChanged) {
                        retainedSchemesBeforeRename.map { retained ->
                            if (retained.customGroupId == saved.customGroupId) {
                                retained.copy(name = saved.name, updatedAt = now)
                            } else {
                                retained
                            }
                        }
                    } else {
                        retainedSchemesBeforeRename
                    }
                persistCustomColorSchemes(listOf(saved) + retainedSchemes)
                if (!paletteChanged && settings.customColorSchemeId == saved.id) {
                    settings = settings.copy(maxColors = saved.selectedCodes.size.coerceAtLeast(2))
                }
                statusText =
                    if (paletteChanged) {
                        "已将修改保存为“${saved.name}”的新版本，旧工程继续使用原版本。"
                    } else {
                        "已保存自定义配色“${saved.name}”。"
                    }
            }

            fun archiveCustomColorScheme(id: String) {
                val target = customColorSchemes.firstOrNull { it.id == id } ?: return
                persistCustomColorSchemes(
                    customColorSchemes.map { scheme ->
                        if (scheme.id == id) {
                            scheme.copy(archived = true, updatedAt = System.currentTimeMillis())
                        } else {
                            scheme
                        }
                    },
                )
                statusText =
                    "已删除“${target.name}”中的 ${target.selectedCodes.size} 色方案，使用它的旧工程不会受影响。"
            }

            fun openCustomColorSchemeManager(
                editSchemeId: String? = null,
                startCreating: Boolean = false,
            ) {
                customColorSchemeInitialEditId = editSchemeId
                customColorSchemeStartCreating = startCreating
                isCustomColorSchemeManagerOpen = true
            }

            fun saveProjects(next: List<BeadProject>) {
                val sorted = next.sortedByDescending { it.updatedAt }.take(MAX_SAVED_PROJECTS)
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

            fun redeemBirthdayCode(passcode: String): Boolean {
                val redeemed = birthdayCelebrationStore.redeem(passcode)
                if (redeemed) {
                    isBirthdayCelebrationRedeemed = true
                    statusText = "生日祝福已解锁。"
                }
                return redeemed
            }

            fun redeemAnimationDeveloperCode(passcode: String): Boolean {
                val unlocked = birthdayAnimationDeveloperStore.unlock(passcode)
                if (unlocked) {
                    isAnimationDeveloperModeEnabled = true
                    statusText = "动画开发者模式已解锁。"
                }
                return unlocked
            }

            fun saveAnimationQualityOverride(quality: BirthdayAnimationQuality?) {
                animationQualityOverride = quality
                birthdayAnimationDeveloperStore.saveQualityOverride(quality)
                statusText = if (quality == null) "动画精度已恢复为自动。" else "动画精度已指定为${quality.label()}。"
            }

            fun saveBirthdayCelebrationProject(source: BeadProject) {
                val existing =
                    projects.firstOrNull {
                        it.id == BirthdayCelebration.GIFT_ID || it.sourceKind == ProjectSourceKind.Celebration
                    }
                if (existing == null && projects.size >= MAX_SAVED_PROJECTS) {
                    Toast.makeText(context, "项目数量已达上限，请先删除一个项目。", Toast.LENGTH_LONG).show()
                    return
                }

                // 生日工程只保留一份：再次保存时用最新的像素拼豆图覆盖旧工程
                val others =
                    projects.filterNot {
                        it.id == BirthdayCelebration.GIFT_ID || it.sourceKind == ProjectSourceKind.Celebration
                    }
                val usedTitles = others.mapTo(mutableSetOf()) { it.title.trim() }
                val project =
                    source.copy(
                        id = BirthdayCelebration.GIFT_ID,
                        title = existing?.title
                            ?: nextUniqueProjectTitle(BirthdayCelebration.PROJECT_TITLE, usedTitles),
                    )
                saveProjects(listOf(project) + others)
                saveProjectResumeScreen(project.id, AppScreen.PatternEditor)
                store.saveProjectBoardViewport(project.id, centeredBoardViewport())
                statusText =
                    if (existing == null) {
                        "“${project.title}”已保存到项目页。"
                    } else {
                        "“${project.title}”已更新为最新的像素拼豆图。"
                    }
                Toast.makeText(
                    context,
                    if (existing == null) "生日蛋糕已保存为项目。" else "生日蛋糕已更新为最新的像素拼豆图。",
                    Toast.LENGTH_SHORT,
                ).show()
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

            fun beginProjectStitch(
                seedProject: BeadProject,
                returnScreen: AppScreen,
                currentDraft: BeadProject? = null,
            ) {
                if (!PROJECT_STITCHING_ENABLED) {
                    pendingProjectActions = null
                    pendingEditorStitchProject = null
                    return
                }
                if (projects.size >= MAX_SAVED_PROJECTS) {
                    pendingProjectActions = null
                    pendingEditorStitchProject = null
                    showStitchProjectLimitNotice = true
                    return
                }
                val draft = currentDraft?.takeIf { it.id == seedProject.id }
                val candidates =
                    buildList<BeadProject> {
                        draft?.let(::add)
                        addAll(projects.filterNot { it.id == draft?.id })
                        if (none { it.id == seedProject.id }) {
                            add(0, seedProject)
                        }
                    }
                pendingProjectActions = null
                stitchProjects = candidates
                stitchInitialProjectId = seedProject.id
                stitchReturnScreen = returnScreen
                screen = AppScreen.ProjectStitch
            }

            fun closeProjectStitch() {
                val destination = stitchReturnScreen
                stitchProjects = emptyList()
                stitchInitialProjectId = null
                stitchReturnScreen = AppScreen.Home
                screen = destination
            }

            fun beginProjectSplit(
                project: BeadProject,
                returnScreen: AppScreen,
            ) {
                if (!PROJECT_SPLITTING_ENABLED) {
                    pendingProjectActions = null
                    pendingEditorSplitProject = null
                    return
                }
                pendingProjectActions = null
                splitProject = project
                splitReturnScreen = returnScreen
                screen = AppScreen.ProjectSplit
            }

            fun closeProjectSplit() {
                val destination = splitReturnScreen
                splitProject = null
                splitReturnScreen = AppScreen.Home
                screen = destination
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
                val availablePalette = BeadPalette.palette(project.colorScheme, project.customColorSchemeId)
                val availableCodes = availablePalette.mapTo(hashSetOf()) { it.id }
                if (selectedColorCode !in availableCodes) {
                    selectedColorCode =
                        PatternEngine.materialStats(project.cells).firstOrNull()?.code?.takeIf { it in availableCodes }
                            ?: availablePalette.firstOrNull()?.id
                            ?: paletteFallback(project.colorScheme, project.paletteMode, project.customColorSchemeId)
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

            fun clearBoardCanvasAdjustmentState() {
                pendingBoardCanvasAdjustment = null
                editorBoardCanvasGeometry = null
            }

            fun pendingCanvasAdjustment(
                fromProject: BeadProject,
                toProject: BeadProject,
                transition: BoardCanvasAnchorTransition,
            ): PendingBoardCanvasAdjustment? {
                if (
                    fromProject.id != toProject.id ||
                    (fromProject.width == toProject.width && fromProject.height == toProject.height)
                ) {
                    return null
                }
                val geometry =
                    editorBoardCanvasGeometry?.takeIf {
                        it.projectId == fromProject.id &&
                            it.gridWidth == fromProject.width &&
                            it.gridHeight == fromProject.height
                    } ?: return null
                return PendingBoardCanvasAdjustment(
                    projectId = toProject.id,
                    oldGeometry = geometry,
                    oldWidth = fromProject.width,
                    oldHeight = fromProject.height,
                    oldAnchorX = transition.oldAnchorX,
                    oldAnchorY = transition.oldAnchorY,
                    newAnchorX = transition.newAnchorX,
                    newAnchorY = transition.newAnchorY,
                    expectedWidth = toProject.width,
                    expectedHeight = toProject.height,
                )
            }

            fun prepositionBoardCanvasAdjustment(adjustment: PendingBoardCanvasAdjustment) {
                val geometry = adjustment.oldGeometry
                val predictedWidth =
                    (geometry.cellSizePx * adjustment.expectedWidth)
                        .roundToInt()
                        .coerceAtLeast(1)
                val predictedHeight =
                    (geometry.cellSizePx * adjustment.expectedHeight)
                        .roundToInt()
                        .coerceAtLeast(1)
                val oldCellWidth = geometry.size.width.toFloat() / adjustment.oldWidth.coerceAtLeast(1)
                val oldCellHeight = geometry.size.height.toFloat() / adjustment.oldHeight.coerceAtLeast(1)
                val newCellWidth = predictedWidth.toFloat() / adjustment.expectedWidth.coerceAtLeast(1)
                val newCellHeight = predictedHeight.toFloat() / adjustment.expectedHeight.coerceAtLeast(1)
                val oldCenteringX =
                    (geometry.viewportSize.width - geometry.size.width)
                        .coerceAtLeast(0) / 2f
                val oldCenteringY =
                    (geometry.viewportSize.height - geometry.size.height)
                        .coerceAtLeast(0) / 2f
                val newCenteringX =
                    (geometry.viewportSize.width - predictedWidth)
                        .coerceAtLeast(0) / 2f
                val newCenteringY =
                    (geometry.viewportSize.height - predictedHeight)
                        .coerceAtLeast(0) / 2f
                val predictedDeltaX =
                    newCenteringX - oldCenteringX +
                        adjustment.newAnchorX * newCellWidth -
                        adjustment.oldAnchorX * oldCellWidth
                val predictedDeltaY =
                    newCenteringY - oldCenteringY +
                        adjustment.newAnchorY * newCellHeight -
                        adjustment.oldAnchorY * oldCellHeight
                if (predictedDeltaX != 0f) {
                    boardHorizontalScroll.dispatchRawDelta(predictedDeltaX * geometry.horizontalPanToScrollScale)
                }
                if (predictedDeltaY != 0f) {
                    boardVerticalScroll.dispatchRawDelta(predictedDeltaY * geometry.verticalPanToScrollScale)
                }
            }

            fun restoreEditorSnapshotKeepingCanvasAnchor(
                snapshot: PatternEditorSnapshot,
                transition: BoardCanvasAnchorTransition?,
            ) {
                val fromProject = currentProject
                val adjustment =
                    if (fromProject != null && transition != null) {
                        pendingCanvasAdjustment(fromProject, snapshot.project, transition)
                    } else {
                        null
                    }
                pendingBoardCanvasAdjustment = adjustment
                adjustment?.let(::prepositionBoardCanvasAdjustment)
                restoreEditorSnapshot(snapshot)
            }

            fun startPatternEditorSession(
                project: BeadProject,
                sessionSettings: GenerationSettings,
                savedProject: BeadProject?,
                initialViewport: BoardViewportState = centeredBoardViewport(),
            ) {
                clearBoardCanvasAdjustmentState()
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
                buildCompletedCellIndices = CellIndexMask.empty()
            }

            fun clearOutputLevelGenerationState() {
                outputPreviewJob?.cancel()
                outputPreviewJob = null
                pendingOutputLevel = null
                lastOutputPreviewDispatchAt = 0L
                outputRequestedLevel = null
            }

            fun cancelGenerationJobAndClearWhenComplete() {
                val cancelledJob = generationJob ?: return
                cancelledJob.cancel()
                scope.launch {
                    withContext(NonCancellable) {
                        cancelledJob.join()
                    }
                    if (generationJob === cancelledJob) {
                        generationJob = null
                    }
                }
            }

            fun updateGridGuideSettings(next: GridGuideSettings) {
                val normalized = next.normalized()
                gridGuideSettings = normalized
                store.saveGridGuideSettings(normalized)
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

            fun closeImageEditorToCreateHub() {
                sourceBitmap = null
                sourceBitmapPath = null
                isImportingImage = false
                mainTab = MainTab.Create
                screen = AppScreen.Home
                statusText = "已回到创建页。"
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
                editorUndoStack = appendEditorHistorySnapshot(editorUndoStack, normalized)
                editorRedoStack = emptyList()
                if (message != null) {
                    statusText = message
                }
            }

            fun replaceCurrentEditorSnapshot(snapshot: PatternEditorSnapshot) {
                restoreEditorSnapshot(snapshot)
                editorUndoStack = appendEditorHistorySnapshot(editorUndoStack.dropLast(1), snapshot)
            }

            fun applyNonHistoricalEditorSnapshot(
                snapshot: PatternEditorSnapshot,
                message: String? = null,
            ) {
                val normalized =
                    snapshot.copy(
                        project = snapshot.project.copy(updatedAt = System.currentTimeMillis()),
                    )
                val previousProject = currentProject
                if (
                    previousProject != null &&
                    (previousProject.width != normalized.project.width || previousProject.height != normalized.project.height)
                ) {
                    clearBoardCanvasAdjustmentState()
                    restoreEditorSnapshot(normalized)
                    editorUndoStack = listOf(normalized)
                } else {
                    replaceCurrentEditorSnapshot(normalized)
                }
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
                replaceCurrentEditorSnapshot(
                    PatternEditorSnapshot(
                        project = savedProject,
                        settings = settings,
                        canvasTransitionFromPrevious = editorUndoStack.lastOrNull()?.canvasTransitionFromPrevious,
                    ),
                )
                if (message != null) {
                    statusText = message
                }
                return savedProject
            }

            fun launchGallerySave(
                project: BeadProject,
                gridSettings: GridGuideSettings = gridGuideSettings,
            ) {
                statusText = "正在保存 PNG 到相册..."
                val exportGridSettings = gridSettings.normalized()
                scope.launch {
                    val saveResult =
                        withContext(Dispatchers.IO) {
                            runCatching { saveProjectPngToGallery(context, project, exportGridSettings) }
                        }

                    saveResult
                        .onSuccess {
                            statusText = "PNG 图纸已保存到相册。"
                            Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            statusText = "保存到相册失败，请稍后重试。"
                            Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                        }
                }
            }

            val galleryWritePermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    val project = pendingGallerySaveProject
                    val exportGridSettings = pendingGalleryGridSettings
                    pendingGallerySaveProject = null
                    pendingGalleryGridSettings = null
                    if (isGranted && project != null && exportGridSettings != null) {
                        launchGallerySave(project, exportGridSettings)
                    } else {
                        statusText = "没有存储权限，无法保存到相册。"
                        Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                    }
                }

            fun requestSaveProjectPngToGallery(project: BeadProject) {
                val exportGridSettings = gridGuideSettings.normalized()
                val canWriteGallery =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (canWriteGallery) {
                    launchGallerySave(project, exportGridSettings)
                    return
                }

                pendingGallerySaveProject = project
                pendingGalleryGridSettings = exportGridSettings
                statusText = "需要存储权限才能保存到相册。"
                galleryWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            fun shareProjectPng(project: BeadProject) {
                val exportProject = projectForPngExport(project)
                val exportGridSettings = gridGuideSettings.normalized()
                statusText = "正在生成 PNG 图纸..."
                scope.launch {
                    val exportResult =
                        withContext(Dispatchers.IO) {
                            runCatching { exportProjectPngToCache(context, exportProject, exportGridSettings) }
                        }

                    exportResult
                        .onSuccess { file ->
                            val shareResult =
                                runCatching {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, "${exportProject.title} PNG 图纸")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${exportProject.title} · ${exportProject.width} x ${exportProject.height}",
                                            )
                                            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    val chooser =
                                        Intent.createChooser(shareIntent, "分享 PNG 图纸").apply {
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    context.startActivity(chooser)
                                }

                            shareResult
                                .onSuccess {
                                    statusText = "PNG 图纸已生成，可选择应用分享。"
                                }
                                .onFailure { error ->
                                    statusText =
                                        if (error is ActivityNotFoundException) {
                                            "PNG 已生成，但没有可用的分享应用。"
                                        } else {
                                            "PNG 已生成，但分享面板打开失败。"
                                        }
                                    Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                                }
                        }
                        .onFailure {
                            statusText = "PNG 图纸生成失败，请稍后重试。"
                            Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                        }
                }
            }

            fun hasUnsavedPatternEditorChanges(): Boolean {
                val draftProject = currentProject ?: return false
                return editorLastSavedProject == null || !isSameProjectContent(draftProject, editorLastSavedProject!!)
            }

            fun requestEditorProjectStitch(project: BeadProject) {
                if (!PROJECT_STITCHING_ENABLED) {
                    pendingEditorStitchProject = null
                    return
                }
                if (projects.size >= MAX_SAVED_PROJECTS) {
                    showStitchProjectLimitNotice = true
                    return
                }
                if (hasUnsavedPatternEditorChanges()) {
                    pendingEditorStitchProject = project
                } else {
                    beginProjectStitch(
                        seedProject = project,
                        returnScreen = AppScreen.PatternEditor,
                        currentDraft = project,
                    )
                }
            }

            fun saveAndBeginEditorProjectStitch() {
                if (!PROJECT_STITCHING_ENABLED) {
                    pendingEditorStitchProject = null
                    return
                }
                val draft = pendingEditorStitchProject ?: return
                pendingEditorStitchProject = null
                val saved =
                    saveCurrentEditorProject(
                        message = "当前项目已保存，可以开始拼接。",
                        resumeScreen = AppScreen.PatternEditor,
                    ) ?: return
                beginProjectStitch(
                    seedProject = saved,
                    returnScreen = AppScreen.PatternEditor,
                    currentDraft = saved,
                )
            }

            fun requestEditorProjectSplit(project: BeadProject) {
                if (!PROJECT_SPLITTING_ENABLED) {
                    pendingEditorSplitProject = null
                    return
                }
                if (isGenerating || outputPreviewJob != null || pendingOutputLevel != null) {
                    Toast.makeText(context, "请等待像素大小应用完成后再拆分。", Toast.LENGTH_SHORT).show()
                    return
                }
                if (pendingBoardCanvasAdjustment != null) {
                    Toast.makeText(context, "请先确认画布扩展结果，再开始拆分。", Toast.LENGTH_SHORT).show()
                    return
                }
                if (hasUnsavedPatternEditorChanges()) {
                    pendingEditorSplitProject = project
                } else {
                    beginProjectSplit(project, AppScreen.PatternEditor)
                }
            }

            fun saveAndBeginEditorProjectSplit() {
                pendingEditorSplitProject ?: return
                pendingEditorSplitProject = null
                val saved =
                    saveCurrentEditorProject(
                        message = "当前项目已保存，可以开始拆分。",
                        resumeScreen = AppScreen.PatternEditor,
                    ) ?: return
                beginProjectSplit(saved, AppScreen.PatternEditor)
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
                clearBoardCanvasAdjustmentState()
                sourceBitmap = null
                sourceBitmapPath = null
                clearOutputLevelGenerationState()
                buildCompletedCellIndices = CellIndexMask.empty()
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
                clearBoardCanvasAdjustmentState()
                sourceBitmap = null
                sourceBitmapPath = null
                clearOutputLevelGenerationState()
                buildCompletedCellIndices = CellIndexMask.empty()
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
                clearBoardCanvasAdjustmentState()
                screen = AppScreen.PatternEditor
                statusText = "已进入图纸编辑。"
            }

            fun toggleCompletedBuildSelection(indices: CellIndexMask) {
                val project = currentProject ?: return
                if (indices.isEmpty()) {
                    return
                }
                val allCompleted = buildCompletedCellIndices.containsAll(indices)
                val nextCompleted =
                    if (allCompleted) {
                        buildCompletedCellIndices.withoutIndices(indices)
                    } else {
                        buildCompletedCellIndices.withIndices(indices)
                    }
                buildCompletedCellIndices = nextCompleted
                store.saveProjectCompletedCellIndices(project, nextCompleted)
            }

            fun undoPatternEdit() {
                if (editorUndoStack.size <= 1 || pendingBoardCanvasAdjustment != null) {
                    return
                }
                val currentSnapshot = editorUndoStack.last()
                val previousSnapshot = editorUndoStack[editorUndoStack.lastIndex - 1]
                editorUndoStack = editorUndoStack.dropLast(1)
                editorRedoStack = appendEditorHistorySnapshot(editorRedoStack, currentSnapshot)
                restoreEditorSnapshotKeepingCanvasAnchor(
                    snapshot = previousSnapshot,
                    transition =
                        currentSnapshot.canvasTransitionFromPrevious?.let {
                            BoardCanvasAnchorTransition(
                                oldAnchorX = it.newAnchorX,
                                oldAnchorY = it.newAnchorY,
                                newAnchorX = it.oldAnchorX,
                                newAnchorY = it.oldAnchorY,
                            )
                        },
                )
                statusText = "已撤回上一步修改。"
            }

            fun redoPatternEdit() {
                if (pendingBoardCanvasAdjustment != null) {
                    return
                }
                val nextSnapshot = editorRedoStack.lastOrNull() ?: return
                editorRedoStack = editorRedoStack.dropLast(1)
                editorUndoStack = appendEditorHistorySnapshot(editorUndoStack, nextSnapshot)
                restoreEditorSnapshotKeepingCanvasAnchor(
                    snapshot = nextSnapshot,
                    transition = nextSnapshot.canvasTransitionFromPrevious,
                )
                statusText = "已恢复刚才撤回的修改。"
            }

            fun openProject(project: BeadProject) {
                val loadedSourceBitmap = store.loadSourceBitmap(project)
                val resumeScreen = store.loadProjectResumeScreen(project.id)
                val loadedCompletedCellIndices = store.loadProjectCompletedCellIndices(project)
                val loadedBoardViewport = store.loadProjectBoardViewport(project.id) ?: centeredBoardViewport()
                sourceBitmap = loadedSourceBitmap
                sourceBitmapPath = project.sourceImagePath?.takeIf { loadedSourceBitmap != null }
                val baseSessionSettings =
                    applyColorScheme(
                        settings.copy(
                            title = project.title,
                            width = project.width,
                            height = project.height,
                        ),
                        project.colorScheme,
                    )
                val sessionSettings =
                    BeadPalette.customColorScheme(project.customColorSchemeId)
                        ?.let { customScheme -> applyCustomColorScheme(baseSessionSettings, customScheme) }
                        ?: baseSessionSettings
                selectedColorCode =
                    PatternEngine.materialStats(project.cells).firstOrNull()?.code
                        ?: paletteFallback(project.colorScheme, project.paletteMode, project.customColorSchemeId)
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

            fun completeProjectStitch(project: BeadProject) {
                if (!PROJECT_STITCHING_ENABLED) {
                    closeProjectStitch()
                    return
                }
                if (projects.size >= MAX_SAVED_PROJECTS) {
                    showStitchProjectLimitNotice = true
                    return
                }
                val savedProject = persistProject(project)
                saveProjectResumeScreen(savedProject.id, AppScreen.PatternEditor)
                stitchProjects = emptyList()
                stitchInitialProjectId = null
                stitchReturnScreen = AppScreen.Home
                openProject(savedProject)
                statusText = "项目拼接已完成，已创建新项目并进入编辑模式。"
            }

            fun completeProjectSplit(parts: List<BeadProject>) {
                val source = splitProject ?: return
                if (parts.size < 2) {
                    splitProjectLimitMessage = "拆分结果不足两个项目。"
                    return
                }
                val availableSlots = MAX_SAVED_PROJECTS - projects.size
                if (parts.size > availableSlots) {
                    splitProjectLimitMessage =
                        "将生成 ${parts.size} 个项目，但当前只能再保存 $availableSlots 个。请先删除不需要的项目。"
                    return
                }
                val now = System.currentTimeMillis()
                val usedTitles = projects.mapTo(mutableSetOf()) { it.title.trim() }
                val normalized =
                    parts.mapIndexed { index, part ->
                        val uniqueTitle = nextUniqueProjectTitle(part.title, usedTitles)
                        usedTitles += uniqueTitle
                        part.copy(title = uniqueTitle, updatedAt = now - index)
                    }
                saveProjects(normalized + projects)
                normalized.forEach { part ->
                    saveProjectResumeScreen(part.id, AppScreen.PatternEditor)
                    store.saveProjectBoardViewport(part.id, centeredBoardViewport())
                }
                splitProject = null
                splitReturnScreen = AppScreen.Home
                currentProject = null
                cancelGenerationJobAndClearWhenComplete()
                generationVersion += 1
                isGenerating = false
                clearBoardCanvasAdjustmentState()
                sourceBitmap = null
                sourceBitmapPath = null
                clearOutputLevelGenerationState()
                buildCompletedCellIndices = CellIndexMask.empty()
                editorUndoStack = emptyList()
                editorRedoStack = emptyList()
                editorLastSavedProject = null
                mainTab = MainTab.Projects
                screen = AppScreen.Home
                statusText = "${source.title} 已拆分为 ${normalized.size} 个新项目，原项目未修改。"
                Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
            }

            fun updateCurrentProject(
                message: String? = null,
                canvasTransitionFromPrevious: BoardCanvasAnchorTransition? = null,
                transform: (BeadProject) -> BeadProject,
            ) {
                val project = currentProject ?: return
                val nextProject = transform(project)
                if (isSameProjectContent(nextProject, project)) {
                    return
                }
                pushEditorSnapshot(
                    PatternEditorSnapshot(
                        project = nextProject,
                        settings = settings,
                        canvasTransitionFromPrevious = canvasTransitionFromPrevious,
                    ),
                    message,
                )
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
                            buildCompletedCellIndices = CellIndexMask.empty()
                            store.saveProjectCompletedCellIndices(project, CellIndexMask.empty())
                        }
                        project.copy(cells = nextCells)
                    }
                }
            }

            fun updateCurrentProjectGrid(
                message: String? = null,
                canvasTransitionFromPrevious: BoardCanvasAnchorTransition? = null,
                transform: (BeadProject) -> EditorTools.GridTransform,
            ) {
                updateCurrentProject(message, canvasTransitionFromPrevious) { project ->
                    val transformed = transform(project)
                    if (
                        transformed.width == project.width &&
                        transformed.height == project.height &&
                        transformed.cells == project.cells
                    ) {
                        project
                    } else {
                        if (buildCompletedCellIndices.isNotEmpty()) {
                            buildCompletedCellIndices = CellIndexMask.empty()
                            store.saveProjectCompletedCellIndices(project, CellIndexMask.empty())
                        }
                        project.copy(
                            width = transformed.width,
                            height = transformed.height,
                            cells = transformed.cells,
                        )
                    }
                }
            }

            fun updateCurrentProjectGridKeepingAnchor(
                message: String,
                oldAnchorX: Float,
                oldAnchorY: Float,
                newAnchorX: Float,
                newAnchorY: Float,
                transform: (BeadProject) -> EditorTools.GridTransform,
            ) {
                if (pendingBoardCanvasAdjustment != null) {
                    return
                }
                val project = currentProject ?: return
                val transformed = transform(project)
                if (
                    transformed.width == project.width &&
                    transformed.height == project.height &&
                    transformed.cells == project.cells
                ) {
                    return
                }
                val adjustment =
                    BoardCanvasAnchorTransition(
                        oldAnchorX = oldAnchorX,
                        oldAnchorY = oldAnchorY,
                        newAnchorX = newAnchorX,
                        newAnchorY = newAnchorY,
                    )
                val nextProject =
                    project.copy(
                        width = transformed.width,
                        height = transformed.height,
                        cells = transformed.cells,
                    )
                val pendingAdjustment = pendingCanvasAdjustment(project, nextProject, adjustment)
                pendingBoardCanvasAdjustment = pendingAdjustment
                pendingAdjustment?.let(::prepositionBoardCanvasAdjustment)
                updateCurrentProjectGrid(
                    message = message,
                    canvasTransitionFromPrevious = adjustment,
                ) { transformed }
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
                val previousJob = generationJob
                previousJob?.cancel()
                generationVersion += 1
                val requestVersion = generationVersion
                val outputDimensions = OutputSizePolicy.constrainDimensions(overrideSettings.width, overrideSettings.height)
                val generationSettings =
                    overrideSettings.copy(
                        width = outputDimensions.width,
                        height = outputDimensions.height,
                        cropZoom = 1f,
                        cropX = 0f,
                        cropY = 0f,
                    )
                val preservedProjectId = existingProject?.id
                val preservedPackSize = existingProject?.beadsPerPack
                isGenerating = true
                statusText = startedMessage
                generationJob =
                    scope.launch {
                        try {
                            withContext(NonCancellable) {
                                previousJob?.join()
                            }
                            if (requestVersion != generationVersion || !currentCoroutineContext().isActive) {
                                return@launch
                            }
                            val generatedBase =
                                withContext(Dispatchers.Default) {
                                    val generationContext = currentCoroutineContext()
                                    PatternEngine.generateProject(baseBitmap, generationSettings) {
                                        !generationContext.isActive
                                    }
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
                                    ?: paletteFallback(
                                        generated.colorScheme,
                                        generated.paletteMode,
                                        generated.customColorSchemeId,
                                    )
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
                                generationJob = null
                                isGenerating = false
                            }
                        }
                    }
            }

            fun launchChartRecognition(
                chartBitmap: Bitmap,
                title: String,
            ) {
                val previousJob = generationJob
                previousJob?.cancel()
                generationVersion += 1
                val requestVersion = generationVersion
                isGenerating = true
                statusText = "正在识别图纸网格和色块..."
                generationJob =
                    scope.launch {
                        try {
                            withContext(NonCancellable) {
                                previousJob?.join()
                            }
                            if (requestVersion != generationVersion || !currentCoroutineContext().isActive) {
                                return@launch
                            }
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
                            val baseNextSettings =
                                applyColorScheme(
                                    settings.copy(
                                        title = recognized.title,
                                        width = recognized.width,
                                        height = recognized.height,
                                    ),
                                    recognized.colorScheme,
                                )
                            val nextSettings =
                                BeadPalette.customColorScheme(recognized.customColorSchemeId)
                                    ?.let { customScheme -> applyCustomColorScheme(baseNextSettings, customScheme) }
                                    ?: baseNextSettings
                            settings = nextSettings
                            val nextSelectedColorCode =
                                PatternEngine.materialStats(recognized.cells).firstOrNull()?.code
                                    ?: paletteFallback(
                                        recognized.colorScheme,
                                        recognized.paletteMode,
                                        recognized.customColorSchemeId,
                                    )
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
                                generationJob = null
                                isGenerating = false
                            }
                        }
                    }
            }

            fun cancelRunningPatternGeneration(message: String? = null) {
                cancelGenerationJobAndClearWhenComplete()
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

            BackHandler(
                enabled =
                    screen == AppScreen.PatternEditor &&
                        !showEditorExitDialog &&
                        !isCustomColorSchemeManagerOpen,
            ) {
                if (pendingBoardCanvasAdjustment == null) {
                    requestClosePatternEditor()
                }
            }

            BackHandler(enabled = screen == AppScreen.BuildMode) {
                closeBuildModeToHome()
            }

            // 图片转图纸 / 图纸识别（都落在 ImageEditor）之前没有返回处理，
            // 系统返回会直接退出应用；这里与页面左上角的返回按钮走同一套收尾逻辑。
            BackHandler(enabled = screen == AppScreen.ImageEditor) {
                closeImageEditorToCreateHub()
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

            pendingEditorStitchProject?.let {
                AlertDialog(
                    onDismissRequest = { pendingEditorStitchProject = null },
                    title = { Text("保存当前修改后拼接？") },
                    text = { Text("当前项目有未保存修改。先保存当前项目，才能在完成拼接后保留这些修改。") },
                    dismissButton = {
                        TextButton(onClick = { pendingEditorStitchProject = null }) {
                            Text("继续编辑")
                        }
                    },
                    confirmButton = {
                        Button(onClick = ::saveAndBeginEditorProjectStitch) {
                            Text("保存并拼接")
                        }
                    },
                )
            }

            pendingEditorSplitProject?.let {
                AlertDialog(
                    onDismissRequest = { pendingEditorSplitProject = null },
                    title = { Text("保存当前修改后拆分？") },
                    text = { Text("当前项目有未保存修改。先保存当前项目，拆分画布才会包含这些修改。") },
                    dismissButton = {
                        TextButton(onClick = { pendingEditorSplitProject = null }) { Text("继续编辑") }
                    },
                    confirmButton = {
                        Button(onClick = ::saveAndBeginEditorProjectSplit) { Text("保存并拆分") }
                    },
                )
            }

            splitProjectLimitMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { splitProjectLimitMessage = null },
                    title = { Text("无法保存拆分结果") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { splitProjectLimitMessage = null }) { Text("知道了") }
                    },
                )
            }

            if (showStitchProjectLimitNotice) {
                AlertDialog(
                    onDismissRequest = { showStitchProjectLimitNotice = false },
                    title = { Text("项目数量已达上限") },
                    text = { Text("最多保存 $MAX_SAVED_PROJECTS 个项目。请先删除一个不需要的项目，再创建拼接项目。") },
                    confirmButton = {
                        TextButton(onClick = { showStitchProjectLimitNotice = false }) {
                            Text("知道了")
                        }
                    },
                )
            }

            pendingProjectActions?.let { project ->
                AlertDialog(
                    onDismissRequest = { pendingProjectActions = null },
                    title = { Text(project.title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (PROJECT_STITCHING_ENABLED) {
                                OutlinedButton(
                                    onClick = {
                                        beginProjectStitch(
                                            seedProject = project,
                                            returnScreen = AppScreen.Home,
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.CallMerge, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("项目拼接")
                                }
                            }
                            if (PROJECT_SPLITTING_ENABLED) {
                                OutlinedButton(
                                    onClick = { beginProjectSplit(project, AppScreen.Home) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.ContentCut, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("项目拆分")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingProjectActions = null
                                    requestProjectRename(project)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("重命名")
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingProjectActions = null
                                    requestProjectDeletion(project)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("删除")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { pendingProjectActions = null }) {
                            Text("取消")
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
                AppScreen.Home -> {
                    val mainPagerState =
                        rememberPagerState(
                            initialPage = MAIN_TABS.indexOf(mainTab),
                            pageCount = { MAIN_TABS.size },
                        )
                    val latestMainTab by rememberUpdatedState(mainTab)
                    val latestSelectMainTab by rememberUpdatedState<(MainTab) -> Unit>(::selectMainTab)

                    LaunchedEffect(mainPagerState) {
                        snapshotFlow { mainPagerState.settledPage }
                            .distinctUntilChanged()
                            .collect { page ->
                                val settledTab = MAIN_TABS[page]
                                if (settledTab != latestMainTab) {
                                    latestSelectMainTab(settledTab)
                                }
                            }
                    }

                    Scaffold(
                        bottomBar =
                            if (
                                isCustomColorSchemeManagerOpen ||
                                (mainTab == MainTab.Settings && (settingsPage != SettingsPage.Home || isSettingsPickerOpen))
                            ) {
                                {}
                            } else {
                                {
                                    MainBottomBar(
                                        selectedTab = MAIN_TABS[mainPagerState.currentPage],
                                        onSelect = { tab ->
                                            val page = MAIN_TABS.indexOf(tab)
                                            if (page >= 0) {
                                                scope.launch {
                                                    mainPagerState.animateScrollToPage(page)
                                                }
                                            }
                                        },
                                    )
                                }
                            },
                    ) { innerPadding ->
                        HorizontalPager(
                            state = mainPagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { page -> MAIN_TABS[page] },
                            userScrollEnabled =
                                !isCustomColorSchemeManagerOpen &&
                                    (MAIN_TABS[mainPagerState.settledPage] != MainTab.Settings ||
                                        (settingsPage == SettingsPage.Home && !isSettingsPickerOpen)),
                        ) { page ->
                            when (MAIN_TABS[page]) {
                                MainTab.Projects ->
                                    HomeScreen(
                                        projects = projects,
                                        extraBottomPadding = innerPadding.calculateBottomPadding(),
                                        onOpen = ::openProject,
                                        onLongPress = { pendingProjectActions = it },
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
                                        isBirthdayCelebrationRedeemed = isBirthdayCelebrationRedeemed,
                                        onRedeemBirthdayCode = ::redeemBirthdayCode,
                                        onSaveBirthdayProject = ::saveBirthdayCelebrationProject,
                                        isAnimationDeveloperModeEnabled = isAnimationDeveloperModeEnabled,
                                        animationQualityOverride = animationQualityOverride,
                                        onRedeemAnimationDeveloperCode = ::redeemAnimationDeveloperCode,
                                        onSelectAnimationQuality = ::saveAnimationQualityOverride,
                                        onOpenCustomColorSchemes = { openCustomColorSchemeManager() },
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
                        onBack = ::closeImageEditorToCreateHub,
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
                            onLongPress = { pendingProjectActions = it },
                            onDelete = ::requestProjectDeletion,
                        )
                    } else {
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
                            gridGuideSettings = gridGuideSettings,
                            boardHorizontalScroll = boardHorizontalScroll,
                            boardVerticalScroll = boardVerticalScroll,
                            isBoardViewportRestoring = isBoardViewportRestoring,
                            isCanvasAdjustmentPending = pendingBoardCanvasAdjustment != null,
                            onBoardCanvasGeometryChange = { editorBoardCanvasGeometry = it },
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
                                val oldAnchorX = 0f
                                val oldAnchorY = 0f
                                val newAnchorX = if (direction == CanvasExpandDirection.Left) 1f else 0f
                                val newAnchorY = if (direction == CanvasExpandDirection.Up) 1f else 0f
                                updateCurrentProjectGridKeepingAnchor(
                                    message = canvasExpandDirectionActionText(direction),
                                    oldAnchorX = oldAnchorX,
                                    oldAnchorY = oldAnchorY,
                                    newAnchorX = newAnchorX,
                                    newAnchorY = newAnchorY,
                                ) {
                                    EditorTools.expandCanvas(
                                        cells = it.cells,
                                        width = it.width,
                                        height = it.height,
                                        direction = direction,
                                    )
                                }
                            },
                            onCanvasReduceApply = { direction ->
                                val oldAnchorX = if (direction == CanvasExpandDirection.Left) 1f else 0f
                                val oldAnchorY = if (direction == CanvasExpandDirection.Up) 1f else 0f
                                updateCurrentProjectGridKeepingAnchor(
                                    message = canvasReduceDirectionActionText(direction),
                                    oldAnchorX = oldAnchorX,
                                    oldAnchorY = oldAnchorY,
                                    newAnchorX = 0f,
                                    newAnchorY = 0f,
                                ) {
                                    EditorTools.removeOuterEmptyLine(
                                        cells = it.cells,
                                        width = it.width,
                                        height = it.height,
                                        direction = direction,
                                    )
                                }
                            },
                            onCanvasTrimAll = {
                                val current = currentProject
                                if (current != null) {
                                    val bounds = EditorTools.occupiedBounds(current.cells, current.width, current.height)
                                    val oldAnchorX = bounds?.startColumn?.toFloat() ?: 0f
                                    val oldAnchorY = bounds?.startRow?.toFloat() ?: 0f
                                    updateCurrentProjectGridKeepingAnchor(
                                        message = "已去除画布四周空白。",
                                        oldAnchorX = oldAnchorX,
                                        oldAnchorY = oldAnchorY,
                                        newAnchorX = 0f,
                                        newAnchorY = 0f,
                                    ) {
                                        EditorTools.removeAllOuterEmptyLines(it.cells, it.width, it.height)
                                    }
                                }
                            },
                            onStartProjectStitch = {
                                requestEditorProjectStitch(project)
                            },
                            onStartProjectSplit = {
                                requestEditorProjectSplit(project)
                            },
                            onOutputLevelCommit = ::commitOutputLevel,
                            onSavePngToGallery = { requestSaveProjectPngToGallery(project) },
                            onExportPng = { shareProjectPng(project) },
                            customColorSchemes = customColorSchemes.filterNot { it.archived },
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
                                if (
                                    nextSettings.colorScheme == settings.colorScheme &&
                                    nextSettings.customColorSchemeId == settings.customColorSchemeId
                                ) {
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
                            onCustomColorSchemeSelect = { customScheme ->
                                val nextSettings = applyCustomColorScheme(settings, customScheme)
                                if (nextSettings.customColorSchemeId == settings.customColorSchemeId) {
                                    return@PatternEditorScreen
                                }
                                val baseBitmap = sourceBitmap
                                if (baseBitmap == null) {
                                    statusText = "当前项目没有保留来源图片，无法切换到 ${customScheme.name} 并重生成图纸。"
                                    return@PatternEditorScreen
                                }
                                launchPatternGeneration(
                                    baseBitmap = baseBitmap,
                                    existingProject = project,
                                    openPatternEditor = false,
                                    startedMessage = "正在切换到 ${customScheme.name}...",
                                    completedMessage = "已切换到 ${customScheme.name}。",
                                    overrideSettings = nextSettings,
                                )
                            },
                            onOpenCustomColorSchemes = { openCustomColorSchemeManager(startCreating = true) },
                            onSelectedColorChange = {
                                selectedColorCode = it
                                focusColorCode = it
                            },
                            onBoardZoomChange = { boardZoom = it },
                            onGridGuideSettingsChange = ::updateGridGuideSettings,
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
                                if (pendingBoardCanvasAdjustment != null) {
                                    return@PatternEditorScreen
                                }
                                saveCurrentEditorProject(resumeScreen = AppScreen.BuildMode)
                                focusColorCode = PatternEngine.materialStats(project.cells).firstOrNull()?.code ?: selectedColorCode
                                focusRow = 0
                                focusColumn = 0
                                focusColorBlockIndex = 0
                                buildMode = store.loadProjectBuildMode(project.id)
                                clearBoardCanvasAdjustmentState()
                                screen = AppScreen.BuildMode
                                statusText = "图纸已保存，并已进入拼豆模式。可以按颜色、颜色区块、按行或按列查看。"
                            },
                        )
                    }
                }

                AppScreen.ProjectStitch -> {
                    if (PROJECT_STITCHING_ENABLED) {
                        ProjectStitchScreen(
                            projects = stitchProjects,
                            initialProjectId = stitchInitialProjectId,
                            newProjectTitle = nextProjectDefaultTitle(projects),
                            onBack = ::closeProjectStitch,
                            onComplete = ::completeProjectStitch,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            closeProjectStitch()
                        }
                    }
                }

                AppScreen.ProjectSplit -> {
                    val project = splitProject
                    if (PROJECT_SPLITTING_ENABLED && project != null) {
                        ProjectSplitScreen(
                            project = project,
                            onBack = ::closeProjectSplit,
                            onComplete = ::completeProjectSplit,
                        )
                    } else {
                        LaunchedEffect(Unit) { closeProjectSplit() }
                    }
                }

                AppScreen.BuildMode -> {
                    val project = currentProject
                    if (project == null) {
                        HomeScreen(
                            projects = projects,
                            extraBottomPadding = 0.dp,
                            onOpen = ::openProject,
                            onLongPress = { pendingProjectActions = it },
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
                            gridGuideSettings = gridGuideSettings,
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
                            onGridGuideSettingsChange = ::updateGridGuideSettings,
                        )
                    }
                }
            }

            if (isCustomColorSchemeManagerOpen) {
                CustomColorSchemeManagerScreen(
                    schemes = customColorSchemes,
                    initialEditSchemeId = customColorSchemeInitialEditId,
                    startCreating = customColorSchemeStartCreating,
                    onBack = {
                        isCustomColorSchemeManagerOpen = false
                        customColorSchemeInitialEditId = null
                        customColorSchemeStartCreating = false
                    },
                    onSave = ::saveCustomColorScheme,
                    onArchive = ::archiveCustomColorScheme,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    projects: List<BeadProject>,
    extraBottomPadding: Dp,
    onOpen: (BeadProject) -> Unit,
    onLongPress: (BeadProject) -> Unit,
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
                    onLongPress = { onLongPress(project) },
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
    isBirthdayCelebrationRedeemed: Boolean,
    onRedeemBirthdayCode: (String) -> Boolean,
    onSaveBirthdayProject: (BeadProject) -> Unit,
    isAnimationDeveloperModeEnabled: Boolean,
    animationQualityOverride: BirthdayAnimationQuality?,
    onRedeemAnimationDeveloperCode: (String) -> Boolean,
    onSelectAnimationQuality: (BirthdayAnimationQuality?) -> Unit,
    onOpenCustomColorSchemes: () -> Unit,
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

    DisposableEffect(Unit) {
        onDispose { onPickerOpenChange(false) }
    }

    BackHandler(enabled = currentPage != SettingsPage.Home) {
        onPageChange(SettingsPage.Home)
    }

    if (currentPage == SettingsPage.Celebration) {
        BirthdayCelebrationScreen(
            config = BirthdayCelebration.config,
            onBack = { onPageChange(SettingsPage.Home) },
            onSaveProject = onSaveBirthdayProject,
            qualityOverride = animationQualityOverride,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                SectionTopBar(
                    title =
                        when (currentPage) {
                            SettingsPage.Home -> "设置"
                            SettingsPage.Redemption -> "兑换码"
                            SettingsPage.Celebration -> "生日祝福"
                            SettingsPage.ColorConfidence -> "颜色置信度"
                            SettingsPage.Debt -> "负债"
                        },
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
                            onPickerOpenChange(true)
                            showThemePicker = true
                        },
                        selectedOption = selectedOption,
                        appIcons = appIcons,
                        onOpenAppIconPicker = {
                            pendingIconId = selectedIconId
                            onPickerOpenChange(true)
                            showAppIconPicker = true
                        },
                        isBirthdayCelebrationRedeemed = isBirthdayCelebrationRedeemed,
                        onOpenRedemption = {
                            onPageChange(SettingsPage.Redemption)
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
                        onOpenCustomColorSchemes = onOpenCustomColorSchemes,
                        onOpenDebt = {
                            onPageChange(SettingsPage.Debt)
                        },
                    )

                SettingsPage.Redemption ->
                    BirthdayRedemptionSettingsPage(
                        innerPadding = innerPadding,
                        extraBottomPadding = extraBottomPadding,
                        isRedeemed = isBirthdayCelebrationRedeemed,
                        onRedeem = onRedeemBirthdayCode,
                        isAnimationDeveloperModeEnabled = isAnimationDeveloperModeEnabled,
                        animationQualityOverride = animationQualityOverride,
                        onRedeemAnimationDeveloperCode = onRedeemAnimationDeveloperCode,
                        onSelectAnimationQuality = onSelectAnimationQuality,
                        onOpenCelebration = {
                            onPageChange(SettingsPage.Celebration)
                        },
                    )

                SettingsPage.Celebration -> Unit

                SettingsPage.ColorConfidence ->
                    ColorConfidenceSettingsPage(
                        innerPadding = innerPadding,
                        extraBottomPadding = extraBottomPadding,
                    )

                SettingsPage.Debt ->
                    MoraDebtSettingsPage(
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
                onDismissRequest = {
                    showThemePicker = false
                    onPickerOpenChange(false)
                },
                heightFraction = 0.58f,
                bottomReservedPadding = 0.dp,
                confirmEnabled = pendingThemeId != selectedThemeId,
                onConfirm = {
                    val pendingOption = themes.firstOrNull { it.id == pendingThemeId } ?: AppThemeRegistry.defaultOption
                    onSelectTheme(pendingOption)
                    showThemePicker = false
                    onPickerOpenChange(false)
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
                onDismissRequest = {
                    showAppIconPicker = false
                    onPickerOpenChange(false)
                },
                heightFraction = 0.72f,
                bottomReservedPadding = 0.dp,
                confirmEnabled = pendingIconId != selectedIconId,
                onConfirm = {
                    val pendingOption = appIcons.firstOrNull { it.id == pendingIconId } ?: AppIconRegistry.defaultOption
                    if (onSelectIcon(pendingOption)) {
                        showAppIconPicker = false
                        onPickerOpenChange(false)
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
        val panelBottomOffset = (bottomReservedPadding - navigationBottomInset).coerceAtLeast(0.dp)
        val contentBottomPadding = if (panelBottomOffset == 0.dp) navigationBottomInset + 4.dp else 4.dp
        val availableHeight = (maxHeight - panelBottomOffset).coerceAtLeast(0.dp)
        val preferredHeight = maxHeight * heightFraction.coerceIn(0.35f, 0.92f)
        val panelHeight = minOf(availableHeight, maxOf(160.dp, preferredHeight + contentBottomPadding))

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = panelBottomOffset)
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
                        .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = contentBottomPadding),
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
    isBirthdayCelebrationRedeemed: Boolean,
    onOpenRedemption: () -> Unit,
    currentVersionLabel: String,
    updateCheckState: UpdateCheckUiState,
    onCheckUpdate: () -> Unit,
    onOpenColorConfidence: () -> Unit,
    onOpenCustomColorSchemes: () -> Unit,
    onOpenDebt: () -> Unit,
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
        SettingsEntryCard(
            title = "自定义配色",
            summary = "基于内置方案增减颜色、校正色值和管理方案",
            icon = Icons.Outlined.Palette,
            onClick = onOpenCustomColorSchemes,
        )
        SettingsEntryCard(
            title = "负债",
            summary = "3000 万摩拉 · 日利率 1% · 查看实时复利走势",
            icon = Icons.Outlined.AccountBalanceWallet,
            onClick = onOpenDebt,
        )
        SettingsEntryCard(
            title = "兑换码",
            summary = "输入兑换码，解锁项目",
            icon = Icons.Outlined.CardGiftcard,
            onClick = onOpenRedemption,
        )
    }
}

@Composable
private fun BirthdayRedemptionSettingsPage(
    innerPadding: PaddingValues,
    extraBottomPadding: Dp,
    isRedeemed: Boolean,
    onRedeem: (String) -> Boolean,
    isAnimationDeveloperModeEnabled: Boolean,
    animationQualityOverride: BirthdayAnimationQuality?,
    onRedeemAnimationDeveloperCode: (String) -> Boolean,
    onSelectAnimationQuality: (BirthdayAnimationQuality?) -> Unit,
    onOpenCelebration: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var hasInvalidCode by rememberSaveable { mutableStateOf(false) }
    fun redeem() {
        val birthdayRedeemed = onRedeem(code)
        if (birthdayRedeemed) {
            hasInvalidCode = false
            onOpenCelebration()
            return
        }
        val developerUnlocked = onRedeemAnimationDeveloperCode(code)
        hasInvalidCode = !developerUnlocked
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 20.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isRedeemed) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "小鱼生日快乐",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Button(
                        onClick = onOpenCelebration,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("观看生日祝福")
                    }
                }
            }
        }
        if (isAnimationDeveloperModeEnabled) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "动画开发者模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "当前精度：${animationQualityOverride?.label() ?: "自动"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    val qualityOptions =
                        listOf(
                            null to "自动",
                            BirthdayAnimationQuality.Low to "低",
                            BirthdayAnimationQuality.Standard to "标准",
                            BirthdayAnimationQuality.Refined to "精致",
                        )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        qualityOptions.forEachIndexed { index, (quality, label) ->
                            SegmentedButton(
                                selected = quality == animationQualityOverride,
                                onClick = { onSelectAnimationQuality(quality) },
                                shape = SegmentedButtonDefaults.itemShape(index, qualityOptions.size),
                                icon = {},
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it
                hasInvalidCode = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("兑换码") },
            supportingText =
                if (hasInvalidCode) {
                    { Text("兑换码不正确") }
                } else {
                    null
                },
            isError = hasInvalidCode,
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { redeem() }),
        )
        Button(
            onClick = ::redeem,
            modifier = Modifier.fillMaxWidth(),
            enabled = code.isNotBlank(),
        ) {
            Icon(Icons.Outlined.CardGiftcard, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(if (isAnimationDeveloperModeEnabled) "兑换 / 解锁" else "兑换")
        }
    }
}

@Composable
private fun MoraDebtSettingsPage(
    innerPadding: PaddingValues,
    extraBottomPadding: Dp,
) {
    var nowEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowEpochMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val chartRangeOptions = listOf(30L to "30天", 365L to "1年", 730L to "2年")
    var chartRangeDays by remember { mutableStateOf(30L) }
    val snapshot = remember(nowEpochMillis) { MoraDebtCalculator.snapshot(nowEpochMillis) }
    val selectedChartRangeLabel = chartRangeOptions.first { it.first == chartRangeDays }.second
    val chartEndDay = remember(snapshot.completedDays, chartRangeDays) { snapshot.completedDays + chartRangeDays }
    val chartPoints =
        remember(chartEndDay) {
            MoraDebtCalculator.projection(endDay = chartEndDay, maxPoints = 61)
        }
    val currentAmount = formatMoraCompact(snapshot.currentAmount)
    val accruedInterest = formatMoraCompact(snapshot.accruedInterest)
    val todayInterest = snapshot.todayInterest
    val todayInterestText = formatMoraCompact(todayInterest)
    val nextInterest = formatMoraCompact(snapshot.nextInterest)
    val countdown = formatDebtCountdown(snapshot.millisUntilNextCompound)
    val currentTime = formatBeijingTime(snapshot.nowEpochMillis, "yyyy年M月d日 HH:mm:ss")

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
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.mora_icon),
                    contentDescription = "摩拉",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "当前负债",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        currentAmount,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(
                        "摩拉",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "北京时间 $currentTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MoraDebtMetric(
                    label = "初始负债",
                    value = "3000 万",
                    modifier = Modifier.weight(1f),
                )
                MoraDebtMetric(
                    label = "已计息",
                    value = "${snapshot.completedDays} 天",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MoraDebtMetric(
                    label = "今日利息",
                    value = if (todayInterest.signum() == 0) "0" else "+$todayInterestText",
                    modifier = Modifier.weight(1f),
                    highlighted = true,
                )
                MoraDebtMetric(
                    label = "累计利息",
                    value = if (snapshot.accruedInterest.signum() == 0) "0" else "+$accruedInterest",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                "复利走势 · 未来 $selectedChartRangeLabel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "从起始日至第 $chartEndDay 天",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            "终点 · 第 $chartEndDay 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            formatMoraCompact(chartPoints.last().amount),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    chartRangeOptions.forEachIndexed { index, (days, label) ->
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = chartRangeDays == days,
                            onClick = { chartRangeDays = days },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = chartRangeOptions.size),
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                MoraDebtLineChart(
                    points = chartPoints,
                    currentDay = snapshot.completedDays,
                    currentAmount = snapshot.currentAmount,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.42f),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        if (snapshot.hasStarted) "距离下一次复利" else "距离开始计息",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        countdown,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        "预计增加",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                    Text(
                        "+$nextInterest",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                    )
                    Text(
                        "摩拉",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        Text(
            "起始：2026年7月10日 00:00（北京时间）\n日利率：1% · 每满 24 小时复利一次",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "纯娱乐数据",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun MoraDebtMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Surface(
        modifier = modifier.heightIn(min = 78.dp),
        shape = RoundedCornerShape(10.dp),
        color =
            if (highlighted) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MoraDebtLineChart(
    points: List<MoraDebtPoint>,
    currentDay: Long,
    currentAmount: BigDecimal,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.tertiary
    val markerLabelColor = MaterialTheme.colorScheme.onTertiary
    val markerCenterColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val chartBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    val firstAmount = points.firstOrNull()?.amount ?: MoraDebtCalculator.initialAmount
    val lastAmount = points.lastOrNull()?.amount ?: firstAmount
    val lastDay = points.lastOrNull()?.completedDays ?: 0L
    val yAxisLabels =
        remember(firstAmount, lastAmount) {
            val range = lastAmount.subtract(firstAmount)
            (0..4).map { index ->
                val amount =
                    firstAmount.add(
                        range
                            .multiply(BigDecimal.valueOf((4 - index).toLong()))
                            .divide(BigDecimal.valueOf(4L), MathContext.DECIMAL64),
                    )
                formatMoraChartLabel(amount)
            }
        }
    val xAxisLabels =
        remember(lastDay) {
            listOf(0L, lastDay / 2L, lastDay)
                .distinct()
                .map { day ->
                    Triple(
                        day,
                        formatBeijingTime(
                            MoraDebtCalculator.START_EPOCH_MILLIS + day * MoraDebtCalculator.DAY_MILLIS,
                            "yy/M/d",
                        ),
                        "第${day}天",
                    )
                }
        }
    val markerLabel = remember(currentAmount) { "当前 ${formatMoraChartLabel(currentAmount)}" }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(chartBackground),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.size < 2 || size.width <= 0f || size.height <= 0f) {
                return@Canvas
            }

            val plotLeft = 54.dp.toPx()
            val plotTop = 10.dp.toPx()
            val plotRight = size.width - 8.dp.toPx()
            val plotBottom = size.height - 34.dp.toPx()
            if (plotRight <= plotLeft || plotBottom <= plotTop) {
                return@Canvas
            }
            val plotWidth = plotRight - plotLeft
            val plotHeight = plotBottom - plotTop
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val axisPaint =
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = axisTextColor.toArgb()
                    textSize = 9.sp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

            for (index in 0..4) {
                val y = plotTop + plotHeight * index / 4f
                drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1.dp.toPx())

                axisPaint.textAlign = AndroidPaint.Align.RIGHT
                val metrics = axisPaint.fontMetrics
                val baseline = y - (metrics.ascent + metrics.descent) / 2f
                nativeCanvas.drawText(yAxisLabels[index], plotLeft - 6.dp.toPx(), baseline, axisPaint)
            }

            fun xFor(day: Long): Float {
                return if (lastDay <= 0L) {
                    plotLeft
                } else {
                    plotLeft + plotWidth * (day.toDouble() / lastDay.toDouble()).toFloat()
                }
            }

            fun yFor(amount: BigDecimal): Float {
                return plotTop + plotHeight * (1f - debtChartRatio(amount, firstAmount, lastAmount))
            }

            xAxisLabels.forEachIndexed { index, (day, dateLabel, dayLabel) ->
                val x = xFor(day)
                drawLine(gridColor, Offset(x, plotTop), Offset(x, plotBottom), strokeWidth = 1.dp.toPx())

                axisPaint.textAlign =
                    when (index) {
                        0 -> AndroidPaint.Align.LEFT
                        xAxisLabels.lastIndex -> AndroidPaint.Align.RIGHT
                        else -> AndroidPaint.Align.CENTER
                    }
                nativeCanvas.drawText(dateLabel, x, plotBottom + 12.dp.toPx(), axisPaint)
                nativeCanvas.drawText(dayLabel, x, plotBottom + 25.dp.toPx(), axisPaint)
            }

            val linePath = Path()
            points.forEachIndexed { index, point ->
                val x = xFor(point.completedDays)
                val y = yFor(point.amount)
                if (index == 0) {
                    linePath.moveTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                }
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(plotRight, plotBottom)
                lineTo(plotLeft, plotBottom)
                close()
            }
            drawPath(
                path = fillPath,
                brush =
                    Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                        startY = plotTop,
                        endY = plotBottom,
                    ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            if (currentDay in 0L..lastDay) {
                val markerX = xFor(currentDay)
                val markerY = yFor(currentAmount)
                drawLine(
                    color = markerColor.copy(alpha = 0.55f),
                    start = Offset(markerX, plotTop),
                    end = Offset(markerX, plotBottom),
                    strokeWidth = 1.5.dp.toPx(),
                )
                drawCircle(markerColor, radius = 5.dp.toPx(), center = Offset(markerX, markerY))
                drawCircle(markerCenterColor, radius = 2.dp.toPx(), center = Offset(markerX, markerY))

                val markerPaint =
                    AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = markerColor.toArgb()
                        style = AndroidPaint.Style.FILL
                    }
                val markerTextPaint =
                    AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = markerLabelColor.toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = AndroidPaint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                val bubbleHeight = 24.dp.toPx()
                val bubbleWidth =
                    (markerTextPaint.measureText(markerLabel) + 16.dp.toPx())
                        .coerceAtMost(plotWidth)
                val bubbleLeft =
                    (markerX - bubbleWidth / 2f)
                        .coerceIn(plotLeft, plotRight - bubbleWidth)
                val preferredTop = markerY - bubbleHeight - 8.dp.toPx()
                val bubbleTop =
                    if (preferredTop >= plotTop) {
                        preferredTop
                    } else {
                        (markerY + 8.dp.toPx()).coerceAtMost(plotBottom - bubbleHeight)
                    }
                val bubbleRect = RectF(bubbleLeft, bubbleTop, bubbleLeft + bubbleWidth, bubbleTop + bubbleHeight)
                nativeCanvas.drawRoundRect(bubbleRect, 6.dp.toPx(), 6.dp.toPx(), markerPaint)
                val markerMetrics = markerTextPaint.fontMetrics
                val markerBaseline = bubbleRect.centerY() - (markerMetrics.ascent + markerMetrics.descent) / 2f
                nativeCanvas.drawText(markerLabel, bubbleRect.centerX(), markerBaseline, markerTextPaint)
            }
        }
    }
}

private fun debtChartRatio(
    value: BigDecimal,
    minimum: BigDecimal,
    maximum: BigDecimal,
): Float {
    val range = maximum.subtract(minimum)
    if (range.signum() == 0) {
        return 0.5f
    }
    return value
        .subtract(minimum)
        .divide(range, MathContext.DECIMAL64)
        .toFloat()
        .coerceIn(0f, 1f)
}

private fun formatMoraAmount(amount: BigDecimal): String {
    return NumberFormat
        .getIntegerInstance(Locale.CHINA)
        .format(amount.setScale(0, RoundingMode.HALF_UP))
}

private fun formatMoraCompact(amount: BigDecimal): String {
    val absolute = amount.abs()
    val (divisor, suffix) =
        when {
            absolute >= BigDecimal("1000000000000") -> BigDecimal("1000000000000") to "万亿"
            absolute >= BigDecimal("100000000") -> BigDecimal("100000000") to "亿"
            absolute >= BigDecimal("10000") -> BigDecimal("10000") to "万"
            else -> return formatMoraAmount(amount)
        }
    val scaled = amount.divide(divisor, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return "$scaled $suffix"
}

private fun formatMoraChartLabel(amount: BigDecimal): String {
    val absolute = amount.abs()
    val (divisor, suffix) =
        when {
            absolute >= BigDecimal("1000000000000") -> BigDecimal("1000000000000") to "万亿"
            absolute >= BigDecimal("100000000") -> BigDecimal("100000000") to "亿"
            absolute >= BigDecimal("10000") -> BigDecimal("10000") to "万"
            else -> return formatMoraAmount(amount)
        }
    val scaled = amount.divide(divisor, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return "$scaled$suffix"
}

private fun formatDebtCountdown(millis: Long): String {
    val safeMillis = millis.coerceAtLeast(0L)
    val totalSeconds = safeMillis / 1_000L + if (safeMillis % 1_000L == 0L) 0L else 1L
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val clock = "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
    return if (days > 0L) "$days 天 $clock" else clock
}

private fun formatBeijingTime(
    epochMillis: Long,
    pattern: String,
): String {
    return SimpleDateFormat(pattern, Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(Date(epochMillis))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorConfidenceSettingsPage(
    innerPadding: PaddingValues,
    extraBottomPadding: Dp,
) {
    var selectedDimension by remember { mutableStateOf(ColorConfidenceDimension.Overall) }
    val pageListState = rememberLazyListState()
    val matrixHorizontalScrollState = rememberScrollState()
    val matrixDragState =
        rememberDraggable2DState { delta ->
            matrixHorizontalScrollState.dispatchRawDelta(-delta.x)
            pageListState.dispatchRawDelta(-delta.y)
        }
    val brands = ColorConfidenceData.brands
    val rows = ColorConfidenceData.variantRows
    val brandCellWidth = 78.dp
    val schemeLabelWidth = 74.dp
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

    LazyColumn(
        state = pageListState,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + extraBottomPadding + 20.dp,
            ),
    ) {
        item(key = "confidence-intro") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                    Text(
                        "颜色置信度矩阵",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        item(key = "confidence-legend") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
        }

        item(key = "confidence-controls") {
            ColorConfidenceMatrixControlsCard(
                selectedDimension = selectedDimension,
                onDimensionSelected = { selectedDimension = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }

        stickyHeader(key = "confidence-matrix-header") {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = 4.dp),
            ) {
                ConfidenceMatrixHeaderRow(
                    brands = brands,
                    selectedDimension = selectedDimension,
                    horizontalScrollState = matrixHorizontalScrollState,
                    dragState = matrixDragState,
                    schemeLabelWidth = schemeLabelWidth,
                    brandCellWidth = brandCellWidth,
                )
            }
        }

        items(
            items = rows,
            key = { row -> "confidence-row-${row.key}" },
        ) { row ->
            ConfidenceMatrixDataRow(
                row = row,
                brands = brands,
                selectedDimension = selectedDimension,
                horizontalScrollState = matrixHorizontalScrollState,
                dragState = matrixDragState,
                schemeLabelWidth = schemeLabelWidth,
                brandCellWidth = brandCellWidth,
                isLast = row == rows.lastOrNull(),
            )
        }

        stickyHeader(key = "confidence-matrix-end") {
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.background),
            )
        }

        item(key = "confidence-sources") {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                ColorConfidenceSourcesCard()
            }
        }
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
private fun ColorConfidenceMatrixControlsCard(
    selectedDimension: ColorConfidenceDimension,
    onDimensionSelected: (ColorConfidenceDimension) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ColorConfidenceDimension.entries.forEachIndexed { index, dimension ->
                    SegmentedButton(
                        modifier = Modifier.weight(1f),
                        selected = selectedDimension == dimension,
                        onClick = { onDimensionSelected(dimension) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ColorConfidenceDimension.entries.size,
                            ),
                        label = {
                            Text(
                                dimension.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConfidenceMatrixHeaderRow(
    brands: List<ColorConfidenceBrandMatrix>,
    selectedDimension: ColorConfidenceDimension,
    horizontalScrollState: ScrollState,
    dragState: Draggable2DState,
    schemeLabelWidth: Dp,
    brandCellWidth: Dp,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        shadowElevation = 3.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                    .padding(12.dp)
                    .draggable2D(state = dragState, startDragImmediately = true),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = schemeLabelWidth + 8.dp)
                        .horizontalScroll(horizontalScrollState, enabled = false),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    brands.forEach { brandMatrix ->
                        ConfidenceBrandHeaderCell(
                            brand = brandMatrix.brand,
                            score = brandMatrix.scores.valueFor(selectedDimension),
                            width = brandCellWidth,
                        )
                    }
                }
            }
            ConfidenceSchemeHeaderCell(width = schemeLabelWidth)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConfidenceMatrixDataRow(
    row: ColorConfidenceVariantRow,
    brands: List<ColorConfidenceBrandMatrix>,
    selectedDimension: ColorConfidenceDimension,
    horizontalScrollState: ScrollState,
    dragState: Draggable2DState,
    schemeLabelWidth: Dp,
    brandCellWidth: Dp,
    isLast: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        shape =
            RoundedCornerShape(
                bottomStart = if (isLast) 22.dp else 0.dp,
                bottomEnd = if (isLast) 22.dp else 0.dp,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .draggable2D(state = dragState, startDragImmediately = true),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = schemeLabelWidth + 8.dp)
                        .horizontalScroll(horizontalScrollState, enabled = false),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    brands.forEach { brandMatrix ->
                        ConfidenceScoreCell(
                            score = brandMatrix.schemeScores[row.key]?.valueFor(selectedDimension),
                            width = brandCellWidth,
                        )
                    }
                }
            }
            ConfidenceSchemeLabelCell(
                label = row.label,
                width = schemeLabelWidth,
            )
        }
    }
}

@Composable
private fun ConfidenceSchemeHeaderCell(
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(width).height(68.dp),
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
    score: Double,
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
                formatConfidencePercent(score),
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
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ImageEditorActionButton(
                                onClick = { cropImageView?.post { cropImageView?.resetCropRect() } },
                                enabled = !isImportingImage && !isGenerating,
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    "重置",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                            ImageEditorActionButton(
                                onClick = {
                                    val croppedBitmap = cropImageView?.getCroppedImage() ?: sourceBitmap
                                    onApplyCrop(croppedBitmap)
                                    activeToolPanel = null
                                },
                                enabled = !isImportingImage && !isGenerating,
                            ) {
                                Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    "确认裁剪",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
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
                    range = 16f..256f,
                    steps = 239,
                    onChange = { onSettingsChange(settings.copy(width = it.roundToInt())) },
                )
                ImageEditorSliderRow(
                    label = "高度",
                    value = settings.height.toFloat(),
                    valueText = settings.height.toString(),
                    range = 16f..256f,
                    steps = 239,
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
    val maxSchemeColors =
        BeadPalette.palette(settings.colorScheme, settings.customColorSchemeId).size.coerceAtLeast(6)
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
            Text("先选品牌，再选系列或颜色数。", style = MaterialTheme.typography.labelMedium)
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
            Text(
                if (selectedBrandId == "artkal") "系列 / 颜色数" else "颜色数",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectableSchemes.forEach { scheme ->
                    ImageEditorFilterChip(
                        selected = settings.customColorSchemeId == null && settings.colorScheme == scheme,
                        label = scheme.variantLabel,
                        onClick = { onSettingsChange(applyColorScheme(settings, scheme)) },
                    )
                }
            }
            Text(
                "当前方案：${BeadPalette.schemeLabel(settings.colorScheme, settings.customColorSchemeId)}",
                style = MaterialTheme.typography.labelMedium,
            )
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
    gridGuideSettings: GridGuideSettings,
    boardHorizontalScroll: ScrollState,
    boardVerticalScroll: ScrollState,
    isBoardViewportRestoring: Boolean,
    isCanvasAdjustmentPending: Boolean,
    onBoardCanvasGeometryChange: (BoardCanvasGeometry) -> Unit,
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
    onCanvasReduceApply: (CanvasExpandDirection) -> Unit,
    onCanvasTrimAll: () -> Unit,
    onStartProjectStitch: () -> Unit,
    onStartProjectSplit: () -> Unit,
    onOutputLevelCommit: (Int) -> Unit,
    onSavePngToGallery: () -> Unit,
    onExportPng: () -> Unit,
    customColorSchemes: List<CustomColorScheme>,
    onColorSchemeSelect: (ColorSchemePreset) -> Unit,
    onCustomColorSchemeSelect: (CustomColorScheme) -> Unit,
    onOpenCustomColorSchemes: () -> Unit,
    onSelectedColorChange: (String) -> Unit,
    onBoardZoomChange: (Float) -> Unit,
    onGridGuideSettingsChange: (GridGuideSettings) -> Unit,
    onCellTap: (Int) -> Unit,
    onMirrorProject: () -> Unit,
    onEnterBuildMode: () -> Unit,
) {
    val selectedColor = BeadPalette.find(selectedColorCode)
    val selectedColorValue = selectedColor?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.surfaceVariant
    val selectedColorContentColor = if (selectedColorValue.luminance() > 0.55f) Color(0xFF1F1B16) else Color.White
    var activeToolPanel by remember(project.id) { mutableStateOf<PatternToolPanel?>(null) }
    var activeToolSubPanel by remember(project.id) { mutableStateOf<PatternToolSubPanel?>(null) }
    var canvasAdjustMode by remember(project.id) { mutableStateOf(CanvasAdjustMode.Add) }
    var areCellCodesEnabled by remember(project.id) { mutableStateOf(true) }
    var rotationDirection by remember(project.id) { mutableStateOf(RotationDirection.Clockwise) }
    var isBoardFullscreen by remember(project.id) { mutableStateOf(false) }
    var boardFitZoom by remember(project.id) { mutableFloatStateOf(BOARD_ZOOM_STANDARD_MIN) }
    var boardFitRequestId by remember(project.id) { mutableIntStateOf(0) }
    var boardTopOverlayHeightPx by remember(project.id) { mutableIntStateOf(0) }
    var boardBottomOverlayHeightPx by remember(project.id) { mutableIntStateOf(0) }
    val usageSummary =
        remember(project.id, project.updatedAt) {
            lazy(LazyThreadSafetyMode.NONE) { UsageAnalyzer.summarize(project) }
        }
    val totalBeadCount =
        remember(project.id, project.updatedAt) {
            project.cells.count { it != null }
        }
    val shouldShowCellCodes =
        areCellCodesEnabled &&
            activeToolPanel != PatternToolPanel.Output &&
            activeToolPanel != PatternToolPanel.Logic
    val isDrawingToolActive = activeToolPanel == PatternToolPanel.Tool
    val boardBottomPadding = 0.dp
    val requestBoardFit = {
        onBoardZoomChange(boardFitZoom.coerceIn(BOARD_ZOOM_RANGE))
        boardFitRequestId = if (boardFitRequestId == Int.MAX_VALUE) 1 else boardFitRequestId + 1
    }

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
            gridGuideSettings = gridGuideSettings,
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
            fitViewportInsetTopPx = if (isBoardFullscreen) 0 else boardTopOverlayHeightPx,
            fitViewportInsetBottomPx = if (isBoardFullscreen) 0 else boardBottomOverlayHeightPx,
            fitRequestId = boardFitRequestId,
            onFitZoomChange = { boardFitZoom = it },
            onFitRequest = requestBoardFit,
            onCanvasGeometryChange = onBoardCanvasGeometryChange,
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
                        .padding(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 2.dp)
                        .onGloballyPositioned { coordinates ->
                            boardTopOverlayHeightPx = coordinates.size.height
                        },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        onClick = onBack,
                        enabled = !isCanvasAdjustmentPending,
                    )
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "撤回",
                        onClick = onUndo,
                        enabled = canUndo && !isCanvasAdjustmentPending,
                    )
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = "取消撤回",
                        onClick = onRedo,
                        enabled = canRedo && !isCanvasAdjustmentPending,
                    )
                    OverlayCircleButton(
                        icon = Icons.Outlined.Save,
                        contentDescription = "保存",
                        onClick = onSave,
                        enabled = !isCanvasAdjustmentPending,
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
                    enabled = !isCanvasAdjustmentPending,
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
                        "${project.width} x ${project.height} · $totalBeadCount 颗 · ${toolModeLabel(toolMode)}",
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
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            boardBottomOverlayHeightPx = coordinates.size.height
                        },
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                CanvasAdjustMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        modifier = Modifier.weight(1f),
                                        selected = canvasAdjustMode == mode,
                                        onClick = { canvasAdjustMode = mode },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = CanvasAdjustMode.entries.size),
                                        label = { Text(canvasAdjustModeLabel(mode), maxLines = 1) },
                                    )
                                }
                            }
                            val removableLines =
                                remember(project.width, project.height, project.cells) {
                                    EditorTools.removableOuterEmptyLines(project.cells, project.width, project.height)
                                }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CanvasExpandDirection.entries.forEach { direction ->
                                    val removableCount = removableLines[direction]
                                    CanvasAdjustToolButton(
                                        modifier = Modifier.weight(1f),
                                        enabled =
                                            !isCanvasAdjustmentPending &&
                                                (canvasAdjustMode == CanvasAdjustMode.Add || removableCount > 0),
                                        onClick = {
                                            if (canvasAdjustMode == CanvasAdjustMode.Add) {
                                                onCanvasExpandApply(direction)
                                            } else {
                                                onCanvasReduceApply(direction)
                                            }
                                        },
                                        label =
                                            if (canvasAdjustMode == CanvasAdjustMode.Add) {
                                                "${canvasExpandDirectionLabel(direction)} +1"
                                            } else {
                                                "${canvasExpandDirectionLabel(direction)}$removableCount"
                                        },
                                        showBorder = true,
                                    )
                                }
                                if (canvasAdjustMode == CanvasAdjustMode.Remove) {
                                    CanvasAdjustToolButton(
                                        modifier = Modifier.weight(1f),
                                        enabled = !isCanvasAdjustmentPending && removableLines.total > 0,
                                        onClick = onCanvasTrimAll,
                                        label = "全部",
                                        emphasized = true,
                                    )
                                }
                            }
                        }
                    }

                PatternToolPanel.Grid ->
                    GridGuideToolPanel(
                        settings = gridGuideSettings,
                        onSettingsChange = onGridGuideSettingsChange,
                    )

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
                            customColorSchemes = customColorSchemes,
                            onSchemeSelect = onColorSchemeSelect,
                            onCustomSchemeSelect = onCustomColorSchemeSelect,
                            onOpenCustomColorSchemes = onOpenCustomColorSchemes,
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
                            summary = usageSummary.value,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                        )
                    }

                PatternToolPanel.Export ->
                    FloatingToolCard(
                        containerColor = Color.Transparent,
                        contentPadding = PaddingValues(0.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        PatternExportPanel(
                            project = project,
                            summary = usageSummary.value,
                            onSavePngToGallery = onSavePngToGallery,
                            onExportPng = onExportPng,
                        )
                    }

                PatternToolPanel.Zoom ->
                    FloatingToolCard {
                        ZoomSliderPanel(
                            value = boardZoom,
                            fitValue = boardFitZoom,
                            onChange = onBoardZoomChange,
                            onFit = requestBoardFit,
                        )
                    }

                PatternToolPanel.Fullscreen ->
                    FloatingToolCard(modifier = Modifier.fillMaxWidth()) {
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
                            label = "画布调整",
                            icon = Icons.Outlined.Crop,
                            selected = activeToolPanel == PatternToolPanel.CanvasExpand,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.CanvasExpand) null else PatternToolPanel.CanvasExpand
                            },
                        )
                        ToolPanelChip(
                            label = "网格",
                            icon = Icons.Outlined.GridView,
                            selected = activeToolPanel == PatternToolPanel.Grid,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Grid) null else PatternToolPanel.Grid
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
                            label = "导出",
                            icon = Icons.Outlined.FileDownload,
                            selected = activeToolPanel == PatternToolPanel.Export,
                            onClick = {
                                activeToolSubPanel = null
                                activeToolPanel = if (activeToolPanel == PatternToolPanel.Export) null else PatternToolPanel.Export
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
                        if (PROJECT_SPLITTING_ENABLED) {
                            ToolPanelChip(
                                label = "项目拆分",
                                icon = Icons.Outlined.ContentCut,
                                selected = false,
                                enabled = !isGenerating && !isCanvasAdjustmentPending,
                                onClick = onStartProjectSplit,
                            )
                        }
                        if (PROJECT_STITCHING_ENABLED) {
                            ToolPanelChip(
                                label = "项目拼接",
                                icon = Icons.Outlined.CallMerge,
                                selected = false,
                                onClick = onStartProjectStitch,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasAdjustToolButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    showBorder: Boolean = false,
) {
    FilledTonalButton(
        modifier = modifier.height(36.dp),
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
        border =
            if (showBorder) {
                BorderStroke(
                    width = 1.dp,
                    color =
                        if (enabled) {
                            Color.Black
                        } else {
                            Color.Black.copy(alpha = 0.32f)
                        },
                )
            } else {
                null
            },
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor =
                    if (emphasized) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    },
                contentColor =
                    if (emphasized) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
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
        modifier = Modifier.width(64.dp),
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
private fun PatternExportPanel(
    project: BeadProject,
    summary: UsageSummary,
    onSavePngToGallery: () -> Unit,
    onExportPng: () -> Unit,
) {
    val exportProject = remember(project) { projectForPngExport(project) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "PNG 图纸",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${exportProject.width} x ${exportProject.height} · ${summary.totalBeads} 颗 · ${summary.totalColors} 色",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSavePngToGallery,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("保存到相册")
                }
                FilledTonalButton(
                    onClick = onExportPng,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("分享 PNG")
                }
            }
        }
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
    onOutputLevelCommit: (Int) -> Unit,
) {
    val outputLevel = outputLevelFromSettings(settings)
    val maxOutputLevel =
        if (hasSourceBitmap && sourceBitmapWidth != null && sourceBitmapHeight != null) {
            OutputSizePolicy
                .maxShortEdge(sourceBitmapWidth, sourceBitmapHeight)
                .coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
        } else {
            OUTPUT_MAX_SHORT_EDGE
        }
    val initialOutputLevel = outputLevel.coerceIn(OUTPUT_MIN_SHORT_EDGE, maxOutputLevel)
    val maxSliderPosition = OutputLevelScale.positionForLevel(maxOutputLevel)
    var previewLevel by remember { mutableIntStateOf(initialOutputLevel) }
    var sliderPosition by remember { mutableFloatStateOf(OutputLevelScale.positionForLevel(initialOutputLevel)) }
    var isDragging by remember { mutableStateOf(false) }
    var isLevelInputOpen by remember { mutableStateOf(false) }
    var levelInputText by remember { mutableStateOf(outputLevel.toString()) }
    LaunchedEffect(outputLevel, maxOutputLevel, isGenerating, isDragging) {
        if (!isDragging && !isGenerating) {
            previewLevel = outputLevel.coerceIn(OUTPUT_MIN_SHORT_EDGE, maxOutputLevel)
            sliderPosition = OutputLevelScale.positionForLevel(previewLevel)
        }
    }
    val previewSettings =
        if (hasSourceBitmap && sourceBitmapWidth != null && sourceBitmapHeight != null) {
            settingsForOutputLevel(
                current = settings,
                baseWidth = sourceBitmapWidth,
                baseHeight = sourceBitmapHeight,
                level = previewLevel,
            )
        } else {
            settings
        }
    val isPreviewing = hasSourceBitmap && previewLevel != outputLevel
    val displayedWidth = if (isPreviewing) previewSettings.width else projectWidth
    val displayedHeight = if (isPreviewing) previewSettings.height else projectHeight
    val gridCount = displayedWidth.toLong() * displayedHeight.toLong()
    val statusLabel =
        when {
            !hasSourceBitmap -> "不可调"
            isGenerating -> "应用中"
            isPreviewing -> "预览中"
            else -> "已应用"
        }

    fun applyOutputLevel(level: Int) {
        val nextLevel = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, maxOutputLevel)
        isDragging = false
        previewLevel = nextLevel
        sliderPosition = OutputLevelScale.positionForLevel(nextLevel)
        onOutputLevelCommit(nextLevel)
    }

    fun adjustOutputLevel(delta: Int) {
        val nextLevel = (previewLevel + delta).coerceIn(OUTPUT_MIN_SHORT_EDGE, maxOutputLevel)
        if (nextLevel == previewLevel) {
            return
        }
        applyOutputLevel(nextLevel)
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
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            ) {
                Text(
                    text = "短边 $previewLevel",
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = hasSourceBitmap) {
                                levelInputText = previewLevel.toString()
                                isLevelInputOpen = true
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$displayedWidth x $displayedHeight（$gridCount 格）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
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
                    enabled = hasSourceBitmap && previewLevel > OUTPUT_MIN_SHORT_EDGE,
                    onClick = { adjustOutputLevel(-1) },
                )
                OutputLevelSlider(
                    modifier = Modifier.weight(1f),
                    position = sliderPosition,
                    maximumPosition = maxSliderPosition,
                    onValueChange = {
                        val nextPosition = it.coerceIn(0f, maxSliderPosition)
                        isDragging = true
                        sliderPosition = nextPosition
                        previewLevel = OutputLevelScale.levelForPosition(nextPosition).coerceAtMost(maxOutputLevel)
                    },
                    onValueChangeFinished = {
                        applyOutputLevel(previewLevel)
                    },
                    enabled = hasSourceBitmap,
                )
                CompactOutputStepButton(
                    label = "+",
                    enabled = hasSourceBitmap && previewLevel < maxOutputLevel,
                    onClick = { adjustOutputLevel(1) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(42.dp))
                OutputLevelTicks(
                    maximumLevel = maxOutputLevel,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(42.dp))
            }
        }
    }

    if (isLevelInputOpen) {
        val inputLevel = levelInputText.toIntOrNull()?.takeIf { it in OUTPUT_MIN_SHORT_EDGE..maxOutputLevel }
        val maxInputLength = maxOutputLevel.toString().length
        AlertDialog(
            onDismissRequest = { isLevelInputOpen = false },
            title = { Text("输入短边大小") },
            text = {
                OutlinedTextField(
                    value = levelInputText,
                    onValueChange = { input ->
                        levelInputText = input.filter(Char::isDigit).take(maxInputLength)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("短边 ($OUTPUT_MIN_SHORT_EDGE-$maxOutputLevel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = levelInputText.isNotEmpty() && inputLevel == null,
                )
            },
            dismissButton = {
                TextButton(onClick = { isLevelInputOpen = false }) {
                    Text("取消")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        inputLevel?.let(::applyOutputLevel)
                        isLevelInputOpen = false
                    },
                    enabled = inputLevel != null && hasSourceBitmap,
                ) {
                    Text("应用")
                }
            },
        )
    }
}

@Composable
private fun OutputLevelSlider(
    position: Float,
    maximumPosition: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
    val inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
    val disabledTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val boundedMaximumPosition = maximumPosition.coerceIn(0f, 1f)

    Box(modifier = modifier) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = position.coerceIn(0f, boundedMaximumPosition),
            onValueChange = { onValueChange(it.coerceIn(0f, boundedMaximumPosition)) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..1f,
            steps = 0,
            enabled = enabled,
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
        Canvas(modifier = Modifier.matchParentSize()) {
            val trackInset = 10.dp.toPx()
            val trackWidth = (size.width - trackInset * 2f).coerceAtLeast(0f)
            val trackCenterY = size.height / 2f
            val boundedPosition = position.coerceIn(0f, boundedMaximumPosition)
            val currentX = trackInset + trackWidth * boundedPosition
            val thumbRadius = 10.dp.toPx()
            val tickRadius = 2.dp.toPx()

            OutputLevelScale.tickLevels.forEach { tickLevel ->
                val tickPosition = OutputLevelScale.positionForLevel(tickLevel)
                val tickX = trackInset + trackWidth * tickPosition
                if (abs(tickX - currentX) > thumbRadius) {
                    val tickColor =
                        when {
                            !enabled || tickPosition > boundedMaximumPosition -> disabledTickColor
                            tickPosition < boundedPosition -> activeTickColor
                            else -> inactiveTickColor
                        }
                    drawCircle(
                        color = tickColor,
                        radius = tickRadius,
                        center = Offset(tickX, trackCenterY),
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputLevelTicks(
    maximumLevel: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(horizontal = 10.dp)) {
        val textStyle = MaterialTheme.typography.labelSmall
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant
        OutputLevelScale.tickLevels.forEach { level ->
            val position = OutputLevelScale.positionForLevel(level)
            Text(
                text = level.toString(),
                modifier = Modifier.align(BiasAlignment(position * 2f - 1f, 0f)),
                style = textStyle,
                color = if (level <= maximumLevel) textColor else textColor.copy(alpha = 0.35f),
            )
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
    customColorSchemes: List<CustomColorScheme>,
    onSchemeSelect: (ColorSchemePreset) -> Unit,
    onCustomSchemeSelect: (CustomColorScheme) -> Unit,
    onOpenCustomColorSchemes: () -> Unit,
) {
    val currentCustomScheme = BeadPalette.customColorScheme(settings.customColorSchemeId)
    val currentBrandKey =
        currentCustomScheme
            ?.customGroupId
            ?.let { groupId -> "custom:$groupId" }
            ?: settings.colorScheme.brandId
    var selectedBrandKey by remember(currentBrandKey) { mutableStateOf(currentBrandKey) }
    var expandedSection by remember(settings.colorScheme, settings.customColorSchemeId) { mutableStateOf<PaletteDetailSection?>(null) }
    val selectableBrands = ColorSchemePreset.selectableBrands(settings.colorScheme)
    val customGroups = customColorSchemes.distinctBy { it.customGroupId }
    val selectedCustomGroupId =
        selectedBrandKey
            .takeIf { it.startsWith("custom:") }
            ?.removePrefix("custom:")
    val selectableSchemes =
        if (selectedCustomGroupId == null) {
            ColorSchemePreset.selectableSchemesForBrand(selectedBrandKey, settings.colorScheme)
        } else {
            emptyList()
        }
    val selectableCustomSchemes =
        if (selectedCustomGroupId == null) {
            emptyList()
        } else {
            customColorSchemes
                .filter { it.customGroupId == selectedCustomGroupId }
                .sortedByDescending { it.selectedCodes.size }
        }
    val activeColorCount = BeadPalette.palette(settings.colorScheme, settings.customColorSchemeId).size

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
                        BeadPalette.schemeLabel(settings.colorScheme, settings.customColorSchemeId),
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
                        text = if (isGenerating) "应用中" else "$activeColorCount 色",
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
                                selected = selectedBrandKey == brand.id,
                                enabled = !isGenerating,
                                onClick = {
                                    selectedBrandKey = brand.id
                                    expandedSection = PaletteDetailSection.ColorCount
                                },
                                label = { Text(brand.label) },
                            )
                        }
                        customGroups.forEach { customGroup ->
                            val customBrandKey = "custom:${customGroup.customGroupId}"
                            FilterChip(
                                selected = selectedBrandKey == customBrandKey,
                                enabled = !isGenerating,
                                onClick = {
                                    selectedBrandKey = customBrandKey
                                    expandedSection = PaletteDetailSection.ColorCount
                                },
                                label = { Text(customGroup.name) },
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
                                selected = settings.customColorSchemeId == null && settings.colorScheme == scheme,
                                enabled = !isGenerating,
                                onClick = { onSchemeSelect(scheme) },
                                label = { Text(scheme.variantLabel) },
                            )
                        }
                        selectableCustomSchemes.forEach { customScheme ->
                            FilterChip(
                                selected = settings.customColorSchemeId == customScheme.id,
                                enabled = !isGenerating,
                                onClick = { onCustomSchemeSelect(customScheme) },
                                label = { Text("${customScheme.selectedCodes.size}色") },
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
                    label = { Text(if (selectedBrandKey == "artkal") "系列 / 颜色数" else "颜色数") },
                )
                AssistChip(
                    onClick = onOpenCustomColorSchemes,
                    enabled = !isGenerating,
                    label = { Text("新建配色") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
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
        BeadPalette.palette(project.colorScheme, project.customColorSchemeId).forEach { color ->
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
    gridGuideSettings: GridGuideSettings,
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
    onToggleCompletedSelection: (CellIndexMask) -> Unit,
    onBoardZoomChange: (Float) -> Unit,
    onGridGuideSettingsChange: (GridGuideSettings) -> Unit,
) {
    val context = LocalContext.current
    var activeToolPanel by remember(project.id) { mutableStateOf<BuildToolPanel?>(null) }
    var markDoneArmed by remember(project.id) { mutableStateOf(false) }
    var buildModeBubbleMessage by remember(project.id) { mutableStateOf<String?>(null) }
    var activeBuildModeToast by remember(project.id) { mutableStateOf<Toast?>(null) }
    var isBoardFullscreen by remember(project.id) { mutableStateOf(false) }
    var areCellCodesEnabled by remember(project.id) { mutableStateOf(true) }
    var areAxisLabelsEnabled by remember(project.id) { mutableStateOf(false) }
    var boardFitZoom by remember(project.id) { mutableFloatStateOf(BOARD_ZOOM_STANDARD_MIN) }
    var boardFitRequestId by remember(project.id) { mutableIntStateOf(0) }
    var boardTopOverlayHeightPx by remember(project.id) { mutableIntStateOf(0) }
    var boardBottomOverlayHeightPx by remember(project.id) { mutableIntStateOf(0) }
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
        remember(project.id, project.updatedAt, activeToolPanel) {
            if (activeToolPanel == BuildToolPanel.Usage) UsageAnalyzer.summarize(project) else null
        }
    val colorBlocks =
        remember(project.id, project.updatedAt, buildMode, activeFocusColorCode) {
            if (buildMode == BuildMode.ColorBlock) {
                ColorBlockIndexing.collect(
                    cells = project.cells,
                    width = project.width,
                    height = project.height,
                    colorCode = activeFocusColorCode,
                )
            } else {
                emptyList()
            }
        }
    val safeFocusColorBlockIndex = focusColorBlockIndex.coerceIn(0, (colorBlocks.size - 1).coerceAtLeast(0))
    val focusedColorBlock = colorBlocks.getOrNull(safeFocusColorBlockIndex)
    val focusedColorBlockSet =
        remember(focusedColorBlock) {
            focusedColorBlock?.let { CellIndexMask.from(it) }
        }
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
                            buildModeBubbleMessage =
                                "当前是 ${colorCodeLabel(selection.colorCode)} 的第 ${selection.blockIndex + 1}/${selection.blockCount} 个区块"
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
    val requestBoardFit = {
        onBoardZoomChange(boardFitZoom.coerceIn(BOARD_ZOOM_RANGE))
        boardFitRequestId = if (boardFitRequestId == Int.MAX_VALUE) 1 else boardFitRequestId + 1
    }

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
            gridGuideSettings = gridGuideSettings,
            dragBufferStart = BOARD_DRAG_BUFFER_START,
            dragBufferEnd = BOARD_DRAG_BUFFER_END,
            dragBufferTop = BOARD_DRAG_BUFFER_TOP,
            dragBufferBottom = BOARD_DRAG_BUFFER_BOTTOM,
            highlightRow = if (shouldShowFocusHighlight && buildMode == BuildMode.Row) safeFocusRow else null,
            highlightColumn = if (shouldShowFocusHighlight && buildMode == BuildMode.Column) safeFocusColumn else null,
            highlightColorCode = if (shouldShowFocusHighlight && buildMode == BuildMode.Color) activeFocusColorCode else null,
            highlightIndices = if (shouldShowFocusHighlight && buildMode == BuildMode.ColorBlock) focusedColorBlockSet else null,
            completedIndices = completedCellIndices,
            dimCompletedIndices = shouldShowFocusHighlight,
            onCellTap = onBoardCellTap,
            onBoardDoubleTap = { isBoardFullscreen = !isBoardFullscreen },
            onCellSizeChange = onBoardZoomChange,
            fitViewportInsetTopPx = if (isBoardFullscreen) 0 else boardTopOverlayHeightPx,
            fitViewportInsetBottomPx = if (isBoardFullscreen) 0 else boardBottomOverlayHeightPx,
            fitRequestId = boardFitRequestId,
            onFitZoomChange = { boardFitZoom = it },
            onFitRequest = requestBoardFit,
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
                        .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp)
                        .onGloballyPositioned { coordinates ->
                            boardTopOverlayHeightPx = coordinates.size.height
                        },
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
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            boardBottomOverlayHeightPx = coordinates.size.height
                        },
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
                            fitValue = boardFitZoom,
                            onChange = onBoardZoomChange,
                            onFit = requestBoardFit,
                        )
                    }

                BuildToolPanel.Grid ->
                    GridGuideToolPanel(
                        settings = gridGuideSettings,
                        onSettingsChange = onGridGuideSettingsChange,
                    )

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
                                            ColorBlockIndexing.collect(
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
                        usageSummary?.let { summary ->
                            PatternUsagePanel(
                                summary = summary,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                            )
                        }
                    }

                BuildToolPanel.Fullscreen ->
                    FloatingToolCard(modifier = Modifier.fillMaxWidth()) {
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
                            label = "网格",
                            icon = Icons.Outlined.GridView,
                            selected = activeToolPanel == BuildToolPanel.Grid,
                            onClick = {
                                activeToolPanel = if (activeToolPanel == BuildToolPanel.Grid) null else BuildToolPanel.Grid
                                markDoneArmed = false
                            },
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
        modifier = Modifier.size(40.dp),
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun OverlayActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        modifier = Modifier.width(120.dp).height(40.dp),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ToolPanelChip(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    usePrimaryWhenSelected: Boolean = false,
    enabled: Boolean = true,
) {
    val leadingIcon: (@Composable () -> Unit)? =
        icon?.let { imageVector ->
            {
                Icon(
                    imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    FilterChip(
        selected = selected,
        enabled = enabled,
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
        leadingIcon = leadingIcon,
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
        modifier = Modifier.size(40.dp),
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
        modifier = Modifier.height(40.dp),
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val shape = RoundedCornerShape(16.dp)
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
                    .size(width = 82.dp, height = 86.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .height(30.dp)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .height(24.dp)
                            .aspectRatio(previewRatio.coerceAtLeast(0.6f))
                            .border(2.dp, contentColor, RoundedCornerShape(3.dp)),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
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
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = ProjectCardMaxHeight),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onOpen,
                        onLongClick = onLongPress,
                        onLongClickLabel = "项目操作",
                    ).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(0.42f),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier =
                        Modifier
                            .heightIn(max = ProjectThumbnailMaxHeight)
                            .aspectRatio(project.width.coerceAtLeast(1).toFloat() / project.height.coerceAtLeast(1)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6EC)),
                ) {
                    PatternThumbnail(project = project)
                }
            }
            Column(modifier = Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text("${project.width} x ${project.height}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${PatternEngine.materialStats(project.cells).size} 种颜色 · " +
                        BeadPalette.schemeLabel(project.colorScheme, project.customColorSchemeId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(PatternEngine.formatTimestamp(project.updatedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除")
            }
        }
    }
}

private val ProjectCardMaxHeight = 160.dp
private val ProjectThumbnailMaxHeight = 132.dp

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
private fun GridGuideToolPanel(
    settings: GridGuideSettings,
    onSettingsChange: (GridGuideSettings) -> Unit,
) {
    var activeSection by remember { mutableStateOf<GridGuideToolSection?>(null) }
    var sizeSliderValue by remember { mutableFloatStateOf(settings.interval.toFloat()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (activeSection) {
            GridGuideToolSection.Size ->
                FloatingToolCard {
                    SliderRow(
                        label = "网格大小",
                        value = sizeSliderValue,
                        valueText = "${sizeSliderValue.roundToInt()} x ${sizeSliderValue.roundToInt()}",
                        range = GridGuideSettings.MIN_INTERVAL.toFloat()..GridGuideSettings.MAX_INTERVAL.toFloat(),
                        steps = 0,
                        onChange = { value ->
                            sizeSliderValue = value
                            onSettingsChange(settings.copy(interval = value.roundToInt()))
                        },
                    )
                }

            GridGuideToolSection.Thickness ->
                FloatingToolCard {
                    SliderRow(
                        label = "网格粗细",
                        value = settings.lineWidthDp,
                        valueText = String.format(Locale.US, "%.1f dp", settings.lineWidthDp),
                        range = GridGuideSettings.MIN_LINE_WIDTH_DP..GridGuideSettings.MAX_LINE_WIDTH_DP,
                        steps = 0,
                        onChange = { onSettingsChange(settings.copy(lineWidthDp = it)) },
                    )
                }

            GridGuideToolSection.Color ->
                FloatingToolCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GRID_GUIDE_COLOR_OPTIONS.forEach { option ->
                            val swatchColor = Color(option.argb)
                            val selected = settings.colorArgb == option.argb
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color =
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                },
                                            shape = CircleShape,
                                        ).semantics { contentDescription = "网格颜色：${option.label}" }
                                        .clickable { onSettingsChange(settings.copy(colorArgb = option.argb)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = if (swatchColor.luminance() > 0.55f) Color(0xFF1F1B16) else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

            null -> Unit
        }

        FloatingToolCard(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolPanelChip(
                    label = "显示网格",
                    selected = settings.enabled,
                    onClick = { onSettingsChange(settings.copy(enabled = !settings.enabled)) },
                    usePrimaryWhenSelected = true,
                )
                ToolPanelChip(
                    label = "网格大小",
                    selected = activeSection == GridGuideToolSection.Size,
                    onClick = {
                        activeSection = if (activeSection == GridGuideToolSection.Size) null else GridGuideToolSection.Size
                    },
                )
                ToolPanelChip(
                    label = "网格粗细",
                    selected = activeSection == GridGuideToolSection.Thickness,
                    onClick = {
                        activeSection =
                            if (activeSection == GridGuideToolSection.Thickness) null else GridGuideToolSection.Thickness
                    },
                )
                ToolPanelChip(
                    label = "网格颜色",
                    selected = activeSection == GridGuideToolSection.Color,
                    onClick = {
                        activeSection = if (activeSection == GridGuideToolSection.Color) null else GridGuideToolSection.Color
                    },
                )
            }
        }
    }
}

@Composable
private fun ZoomSliderPanel(
    value: Float,
    fitValue: Float,
    onChange: (Float) -> Unit,
    onFit: () -> Unit,
) {
    val dynamicMinimum =
        BoardZoomMath.minimumZoomDp(
            standardMinimumZoomDp = BOARD_ZOOM_STANDARD_MIN,
            fitCellSizeDp = fitValue,
        )
    val safeValue =
        value.takeIf { it.isFinite() }?.coerceIn(BOARD_ZOOM_RANGE)
            ?: dynamicMinimum
    // Keep an already-restored value below the current fit minimum representable until the
    // user explicitly chooses fit. This avoids a zoom jump during rotation or a resize.
    val sliderMinimum =
        min(dynamicMinimum, safeValue.coerceAtLeast(BoardZoomMath.ABSOLUTE_MIN_ZOOM_DP))
    val sliderFraction =
        BoardZoomMath.sliderFractionForZoom(
            zoomDp = safeValue,
            minimumZoomDp = sliderMinimum,
            maximumZoomDp = BOARD_ZOOM_MAX,
        )
    var latestSliderFraction by remember(sliderMinimum) { mutableFloatStateOf(sliderFraction) }
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
                    "${formatBoardZoomValue(safeValue)} dp",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Slider(
            value = sliderFraction,
            onValueChange = {
                latestSliderFraction = it
                onChange(
                    BoardZoomMath.zoomForSliderFraction(
                        fraction = it,
                        minimumZoomDp = sliderMinimum,
                        maximumZoomDp = BOARD_ZOOM_MAX,
                    ),
                )
            },
            onValueChangeFinished = {
                if (latestSliderFraction <= 0.001f) {
                    onFit()
                }
            },
            valueRange = 0f..1f,
            steps = 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("缩小", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(
                onClick = onFit,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Outlined.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("适应全图")
            }
            Text("放大", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatBoardZoomValue(value: Float): String {
    val safeValue = value.takeIf { it.isFinite() } ?: 0f
    return when {
        safeValue < 0.01f -> "%.4f".format(Locale.ROOT, safeValue)
        safeValue < 0.1f -> "%.3f".format(Locale.ROOT, safeValue)
        safeValue < 1f -> "%.2f".format(Locale.ROOT, safeValue)
        safeValue < 10f -> "%.1f".format(Locale.ROOT, safeValue)
        else -> safeValue.roundToInt().toString()
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
    val preview =
        remember(project.id, project.updatedAt, project.width, project.height) {
            createGridPreviewBitmap(
                cells = project.cells,
                width = project.width,
                height = project.height,
                maxBitmapEdge = BOARD_THUMBNAIL_MAX_EDGE,
                emptyCellArgb = 0xFFF3E9DB.toInt(),
            )
        }
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        drawRect(Color(0xFFFFF6EC))
        drawImage(
            image = preview.image,
            dstSize =
                IntSize(
                    size.width.roundToInt().coerceAtLeast(1),
                    size.height.roundToInt().coerceAtLeast(1),
                ),
            filterQuality = FilterQuality.None,
        )
    }
}

/**
 * 双指缩放的锚点参数（单根轴）。
 *
 * [currentLeadingPadding] / [nextLeadingPadding] 是缩放前后的起始留白：
 * 画布比视口小时 `resolveDragPaddingPair` 会额外补一半的居中留白，
 * 这块留白随格子尺寸变化，所以缩放前后必须分别计算，否则锚点会偏。
 */
private data class BoardZoomAnchorAxis(
    val scrollState: ScrollState,
    val currentLeadingPadding: Float,
    val nextLeadingPadding: Float,
    val currentPanToScrollScale: Float,
    val nextPanToScrollScale: Float,
)

/**
 * 双指缩放时把放大/缩小中心挪到两根手指的中点上。
 *
 * 屏幕坐标与内容坐标的关系：屏幕 = 起始留白 + 棋盘内坐标 − 逻辑滚动量。
 * 缩放时棋盘内坐标按 ratio 缩放、留白换成新的那份，要让指下的点保持不动：
 *
 *     棋盘内坐标 = 屏幕坐标 + 逻辑滚动量 − 当前留白
 *     新滚动量   = 新留白 + 棋盘内坐标 × ratio − 屏幕坐标
 *
 * 最后按缩放后的 panToScrollScale 换算回 ScrollState 的 proxy 单位（大画布会被压缩）。
 */
private fun anchorBoardZoomAt(
    centroid: Offset,
    ratio: Float,
    horizontal: BoardZoomAnchorAxis,
    vertical: BoardZoomAnchorAxis,
) {
    if (!ratio.isFinite() || ratio <= 0f) return
    anchorBoardZoomAxis(centroid.x, ratio, horizontal)
    anchorBoardZoomAxis(centroid.y, ratio, vertical)
}

private fun anchorBoardZoomAxis(
    position: Float,
    ratio: Float,
    axis: BoardZoomAnchorAxis,
) {
    val currentScale = axis.currentPanToScrollScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    val nextScale = axis.nextPanToScrollScale.takeIf { it.isFinite() && it > 0f } ?: currentScale
    val currentProxy = axis.scrollState.value.toFloat()
    val logicalScroll = currentProxy / currentScale
    val boardPoint = position + logicalScroll - axis.currentLeadingPadding
    val targetLogicalScroll = axis.nextLeadingPadding + boardPoint * ratio - position
    axis.scrollState.dispatchRawDelta(targetLogicalScroll * nextScale - currentProxy)
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
    gridGuideSettings: GridGuideSettings,
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
    fitViewportInsetTopPx: Int = 0,
    fitViewportInsetBottomPx: Int = 0,
    fitRequestId: Int = 0,
    onFitZoomChange: ((Float) -> Unit)? = null,
    onFitRequest: (() -> Unit)? = null,
    onCanvasGeometryChange: ((BoardCanvasGeometry) -> Unit)? = null,
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
    var visibleBoardWindow by remember(project.id) { mutableStateOf<BoardDisplayWindow?>(null) }
    val currentTapEnabled by rememberUpdatedState(tapEnabled)
    val currentOnCellTap by rememberUpdatedState(onCellTap)
    val currentOnBoardDoubleTap by rememberUpdatedState(onBoardDoubleTap)
    val currentOnFitZoomChange by rememberUpdatedState(onFitZoomChange)
    val currentOnFitRequest by rememberUpdatedState(onFitRequest)
    val currentOnCanvasGeometryChange by rememberUpdatedState(onCanvasGeometryChange)
    val currentCanvasTopLeft by rememberUpdatedState(canvasTopLeft)
    val currentCanvasSizePx by rememberUpdatedState(canvasSizePx)
    // 手势节点不随这些值重建（见 pointerInput 的 key），所以用最新值读取
    val currentCellSize by rememberUpdatedState(cellSize)
    val currentOnCellSizeChange by rememberUpdatedState(onCellSizeChange)
    val currentViewportRestoring by rememberUpdatedState(isViewportRestoring)
    // 单指拖动松手后的惯性滑动；新的手势一来就取消，避免和拖动抢滚动量
    val flingScope = rememberCoroutineScope()
    val flingJob = remember { mutableStateOf<Job?>(null) }
    val axisPanelColor = MaterialTheme.colorScheme.surface
    val axisTextColor = MaterialTheme.colorScheme.onSurface
    val axisHighlightColor = MaterialTheme.colorScheme.primary
    val axisOutlineColor = MaterialTheme.colorScheme.outlineVariant
    val cellSizePx = with(density) { cellSize.dp.toPx() }
    val predictedBoardWidthPx = boardDisplayWindow.columnCount * cellSizePx
    val predictedBoardHeightPx = boardDisplayWindow.rowCount * cellSizePx
    val rasterPreview =
        if (
            cellSize <= BOARD_RASTER_PREVIEW_MAX_CELL_DP
        ) {
            remember(project.id, project.updatedAt, project.width, project.height) {
                createGridPreviewBitmap(
                    cells = project.cells,
                    width = project.width,
                    height = project.height,
                    maxBitmapEdge = BOARD_RASTER_PREVIEW_MAX_EDGE,
                    emptyCellArgb = 0xFFF2EBDD.toInt(),
                )
            }
        } else {
            null
        }
    val canUseRasterPreview =
        rasterPreview != null &&
            highlightRow == null &&
            highlightColumn == null &&
            highlightColorCode == null &&
            highlightIndices == null &&
            (!dimCompletedIndices || completedIndices.isEmpty())
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
                val fitCellSize =
                    BoardZoomMath.fitCellSizeDp(
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx,
                        density = density.density,
                        columnCount = boardDisplayWindow.columnCount,
                        rowCount = boardDisplayWindow.rowCount,
                        insetTopPx = fitViewportInsetTopPx,
                        insetBottomPx = fitViewportInsetBottomPx,
                    )
                // 手势节点不随 fitCellSize 重建，缩放时要用最新值算最小缩放
                val currentFitCellSize by rememberUpdatedState(fitCellSize)
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
                val edgePanPaddingStartPx = with(density) { edgePanPaddingStart.toPx() }
                val edgePanPaddingEndPx = with(density) { edgePanPaddingEnd.toPx() }
                val edgePanPaddingTopPx = with(density) { edgePanPaddingTop.toPx() }
                val edgePanPaddingBottomPx = with(density) { edgePanPaddingBottom.toPx() }
                val horizontalAxis =
                    BoardViewportMath.resolveAxis(
                        logicalContentSizePx = predictedBoardWidthPx + edgePanPaddingStartPx + edgePanPaddingEndPx,
                        viewportSizePx = viewportWidthPx,
                        scrollValuePx = horizontalScrollState.value,
                    )
                val verticalAxis =
                    BoardViewportMath.resolveAxis(
                        logicalContentSizePx = predictedBoardHeightPx + edgePanPaddingTopPx + edgePanPaddingBottomPx,
                        viewportSizePx = viewportHeightPx,
                        scrollValuePx = verticalScrollState.value,
                    )
                val fitTargetHorizontalScroll =
                    BoardViewportMath.centeredScrollValue(
                        boardSizePx = predictedBoardWidthPx,
                        viewportSizePx = viewportWidthPx,
                        leadingPaddingPx = edgePanPaddingStartPx,
                        trailingPaddingPx = edgePanPaddingEndPx,
                    )
                val fitTargetVerticalScroll =
                    BoardViewportMath.centeredScrollValue(
                        boardSizePx = predictedBoardHeightPx,
                        viewportSizePx = viewportHeightPx,
                        leadingPaddingPx = edgePanPaddingTopPx,
                        trailingPaddingPx = edgePanPaddingBottomPx,
                        viewportLeadingInsetPx = fitViewportInsetTopPx,
                        viewportTrailingInsetPx = fitViewportInsetBottomPx,
                    )
                val currentFitTargetHorizontalScroll by rememberUpdatedState(fitTargetHorizontalScroll)
                val currentFitTargetVerticalScroll by rememberUpdatedState(fitTargetVerticalScroll)
                val currentFitExpectedHorizontalMax by rememberUpdatedState(horizontalAxis.proxyMaxScrollPx)
                val currentFitExpectedVerticalMax by rememberUpdatedState(verticalAxis.proxyMaxScrollPx)
                val boardLeftPx = edgePanPaddingStartPx - horizontalAxis.logicalScrollPx
                val boardTopPx = edgePanPaddingTopPx - verticalAxis.logicalScrollPx
                val renderWindow =
                    visibleBoardDisplayWindow(
                        fullWindow = boardDisplayWindow,
                        boardLeftPx = boardLeftPx,
                        boardTopPx = boardTopPx,
                        cellSizePx = cellSizePx,
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx,
                    )
                val proxyWidth = with(density) { horizontalAxis.proxyContentSizePx.toDp() }
                val proxyHeight = with(density) { verticalAxis.proxyContentSizePx.toDp() }

                LaunchedEffect(
                    project.id,
                    boardLeftPx,
                    boardTopPx,
                    predictedBoardWidthPx,
                    predictedBoardHeightPx,
                    viewportWidthPx,
                    viewportHeightPx,
                    surfaceTopLeftInRoot,
                    renderWindow,
                ) {
                    val nextCanvasTopLeft = Offset(boardLeftPx, boardTopPx)
                    val nextCanvasSize =
                        IntSize(
                            predictedBoardWidthPx.roundToInt().coerceAtLeast(1),
                            predictedBoardHeightPx.roundToInt().coerceAtLeast(1),
                        )
                    canvasTopLeft = nextCanvasTopLeft
                    canvasSizePx = nextCanvasSize
                    visibleBoardWindow = renderWindow
                    currentOnCanvasGeometryChange?.invoke(
                        BoardCanvasGeometry(
                            projectId = project.id,
                            topLeftInRoot = surfaceTopLeftInRoot + nextCanvasTopLeft,
                            size = nextCanvasSize,
                            viewportSize = IntSize(viewportWidthPx, viewportHeightPx),
                            cellSizePx = cellSizePx,
                            horizontalPanToScrollScale = horizontalAxis.panToScrollScale,
                            verticalPanToScrollScale = verticalAxis.panToScrollScale,
                            gridWidth = project.width,
                            gridHeight = project.height,
                        ),
                    )
                }

                LaunchedEffect(project.id, fitCellSize) {
                    fitCellSize?.let { currentOnFitZoomChange?.invoke(it) }
                }

                LaunchedEffect(fitRequestId) {
                    if (fitRequestId <= 0) {
                        return@LaunchedEffect
                    }
                    withFrameNanos {}
                    withTimeoutOrNull(1_000L) {
                        snapshotFlow {
                            abs(horizontalScrollState.maxValue - currentFitExpectedHorizontalMax) <= 1 &&
                                abs(verticalScrollState.maxValue - currentFitExpectedVerticalMax) <= 1
                        }.first { it }
                    }
                    horizontalScrollState.scrollTo(
                        currentFitTargetHorizontalScroll.coerceIn(0, horizontalScrollState.maxValue),
                    )
                    verticalScrollState.scrollTo(
                        currentFitTargetVerticalScroll.coerceIn(0, verticalScrollState.maxValue),
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(
                                project.id,
                                project.width,
                                project.height,
                                project.cells,
                            ) {
                                if (currentViewportRestoring) {
                                    return@pointerInput
                                }
                                // 画布比视口小时，起始留白里含一半居中补偿，会随格子尺寸变化
                                fun dragPaddingPx(
                                    viewportPx: Int,
                                    boardPx: Float,
                                    buffer: Dp,
                                ): Float = ((viewportPx - boardPx).coerceAtLeast(0f) / 2f) + buffer.toPx()

                                /** 「逻辑滚动量 → ScrollState 值」的换算比例，按给定格子尺寸现算 */
                                fun panScales(cellSizeDp: Float): Pair<Float, Float> {
                                    val cellPx = cellSizeDp.dp.toPx()
                                    val boardWidth = boardDisplayWindow.columnCount * cellPx
                                    val boardHeight = boardDisplayWindow.rowCount * cellPx
                                    val lx = dragPaddingPx(size.width, boardWidth, dragBufferStart)
                                    val tx = dragPaddingPx(size.width, boardWidth, dragBufferEnd)
                                    val ly = dragPaddingPx(size.height, boardHeight, dragBufferTop)
                                    val ty = dragPaddingPx(size.height, boardHeight, dragBufferBottom)
                                    val scaleX =
                                        BoardViewportMath
                                            .resolveAxis(boardWidth + lx + tx, size.width, horizontalScrollState.value)
                                            .panToScrollScale
                                    val scaleY =
                                        BoardViewportMath
                                            .resolveAxis(boardHeight + ly + ty, size.height, verticalScrollState.value)
                                            .panToScrollScale
                                    return scaleX to scaleY
                                }
                                awaitEachGesture {
                                    val velocityTracker = VelocityTracker()
                                    var flingScaleX = 1f
                                    var flingScaleY = 1f
                                    // 注意：cellSize 不能作为 pointerInput 的 key。
                                    // 否则每次缩放都会重建手势节点、把正在进行的手势打断，
                                    // 表现为缩放一顿一顿、锚点对不上。这里每个事件读最新值。
                                    var gestureCellSize = currentCellSize
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
                                                // 手指落下就停掉上一次的惯性滑动（不能放在 awaitEachGesture 开头，
                                                // 那里会在上一次松手后立刻执行，把刚启动的惯性取消掉）
                                                flingJob.value?.cancel()
                                            }
                                        }
                                        if (pressedCount >= 2) {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            val onCellSizeChange = currentOnCellSizeChange

                                            // 当前这一帧的棋盘尺寸与留白。画布比视口小时，
                                            // 起始留白里含一半居中补偿，会随格子尺寸变化，必须每次现算。
                                            val cellPxNow = gestureCellSize.dp.toPx()
                                            val boardWidthNow = boardDisplayWindow.columnCount * cellPxNow
                                            val boardHeightNow = boardDisplayWindow.rowCount * cellPxNow
                                            val leadingXNow = dragPaddingPx(size.width, boardWidthNow, dragBufferStart)
                                            val trailingXNow = dragPaddingPx(size.width, boardWidthNow, dragBufferEnd)
                                            val leadingYNow = dragPaddingPx(size.height, boardHeightNow, dragBufferTop)
                                            val trailingYNow = dragPaddingPx(size.height, boardHeightNow, dragBufferBottom)
                                            val axisXNow =
                                                BoardViewportMath.resolveAxis(
                                                    logicalContentSizePx = boardWidthNow + leadingXNow + trailingXNow,
                                                    viewportSizePx = size.width,
                                                    scrollValuePx = horizontalScrollState.value,
                                                )
                                            val axisYNow =
                                                BoardViewportMath.resolveAxis(
                                                    logicalContentSizePx = boardHeightNow + leadingYNow + trailingYNow,
                                                    viewportSizePx = size.height,
                                                    scrollValuePx = verticalScrollState.value,
                                                )

                                            // 先按手指位移平移，再缩放：这样双指中心所指的那块内容始终在指下
                                            if (panChange.x != 0f) {
                                                horizontalScrollState.dispatchRawDelta(-panChange.x * axisXNow.panToScrollScale)
                                            }
                                            if (panChange.y != 0f) {
                                                verticalScrollState.dispatchRawDelta(-panChange.y * axisYNow.panToScrollScale)
                                            }

                                            if (zoomChange != 1f && onCellSizeChange != null) {
                                                val minimumCellSize =
                                                    BoardZoomMath.minimumZoomDp(
                                                        standardMinimumZoomDp = BOARD_ZOOM_STANDARD_MIN,
                                                        fitCellSizeDp = currentFitCellSize,
                                                    )
                                                val gestureMinimumCellSize =
                                                    min(
                                                        minimumCellSize,
                                                        gestureCellSize.coerceAtLeast(BoardZoomMath.ABSOLUTE_MIN_ZOOM_DP),
                                                    )
                                                val nextCellSize =
                                                    (gestureCellSize * zoomChange).coerceIn(
                                                        gestureMinimumCellSize..BOARD_ZOOM_MAX,
                                                    )
                                                if (nextCellSize != gestureCellSize) {
                                                    val reachedFitMinimum =
                                                        gestureCellSize > minimumCellSize * 1.001f &&
                                                            nextCellSize <= minimumCellSize * 1.001f
                                                    val ratio = nextCellSize / gestureCellSize
                                                    val centroid = event.calculateCentroid()
                                                    gestureCellSize = nextCellSize
                                                    onCellSizeChange(nextCellSize)

                                                    // 以双指中心为缩放中心：放大/缩小都保持指下的那颗格子不动。
                                                    // 缩放后留白与 proxy 换算比例都会变，先算出新的一套再补偿滚动量。
                                                    if (ratio != 1f && centroid.isSpecified) {
                                                        val nextCellPx = nextCellSize.dp.toPx()
                                                        val nextBoardWidthPx = boardDisplayWindow.columnCount * nextCellPx
                                                        val nextBoardHeightPx = boardDisplayWindow.rowCount * nextCellPx
                                                        val nextLeadingX = dragPaddingPx(size.width, nextBoardWidthPx, dragBufferStart)
                                                        val nextTrailingX = dragPaddingPx(size.width, nextBoardWidthPx, dragBufferEnd)
                                                        val nextLeadingY = dragPaddingPx(size.height, nextBoardHeightPx, dragBufferTop)
                                                        val nextTrailingY = dragPaddingPx(size.height, nextBoardHeightPx, dragBufferBottom)
                                                        val nextAxisX =
                                                            BoardViewportMath.resolveAxis(
                                                                logicalContentSizePx = nextBoardWidthPx + nextLeadingX + nextTrailingX,
                                                                viewportSizePx = size.width,
                                                                scrollValuePx = 0,
                                                            )
                                                        val nextAxisY =
                                                            BoardViewportMath.resolveAxis(
                                                                logicalContentSizePx = nextBoardHeightPx + nextLeadingY + nextTrailingY,
                                                                viewportSizePx = size.height,
                                                                scrollValuePx = 0,
                                                            )
                                                        anchorBoardZoomAt(
                                                            centroid = centroid,
                                                            ratio = ratio,
                                                            horizontal =
                                                                BoardZoomAnchorAxis(
                                                                    scrollState = horizontalScrollState,
                                                                    currentLeadingPadding = leadingXNow,
                                                                    nextLeadingPadding = nextLeadingX,
                                                                    currentPanToScrollScale = axisXNow.panToScrollScale,
                                                                    nextPanToScrollScale = nextAxisX.panToScrollScale,
                                                                ),
                                                            vertical =
                                                                BoardZoomAnchorAxis(
                                                                    scrollState = verticalScrollState,
                                                                    currentLeadingPadding = leadingYNow,
                                                                    nextLeadingPadding = nextLeadingY,
                                                                    currentPanToScrollScale = axisYNow.panToScrollScale,
                                                                    nextPanToScrollScale = nextAxisY.panToScrollScale,
                                                                ),
                                                        )
                                                    }
                                                    if (reachedFitMinimum) {
                                                        currentOnFitRequest?.invoke()
                                                    }
                                                }
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
                                                    val (scaleX, scaleY) = panScales(gestureCellSize)
                                                    flingScaleX = scaleX
                                                    flingScaleY = scaleY
                                                    horizontalScrollState.dispatchRawDelta(-panChange.x * scaleX)
                                                    verticalScrollState.dispatchRawDelta(-panChange.y * scaleY)
                                                    event.changes.forEach { change ->
                                                        if (change.positionChanged()) {
                                                            change.consume()
                                                        }
                                                    }
                                                }
                                            }
                                            // 记录速度，松手后用来做惯性滑动
                                            event.changes.forEach { velocityTracker.addPointerInputChange(it) }
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

                                    // 单指拖动松手后按速度惯性滑动，靠近边缘会自然停下
                                    if (isDragging && pressedCountMax == 1) {
                                        val velocity = velocityTracker.calculateVelocity()
                                        val startVx = -velocity.x * flingScaleX
                                        val startVy = -velocity.y * flingScaleY
                                        if (abs(startVx) > 60f || abs(startVy) > 60f) {
                                            flingJob.value =
                                                flingScope.launch {
                                                    var vx = startVx
                                                    var vy = startVy
                                                    var lastFrame = 0L
                                                    while (abs(vx) > 25f || abs(vy) > 25f) {
                                                        withFrameNanos { now ->
                                                            if (lastFrame != 0L) {
                                                                val dt =
                                                                    ((now - lastFrame) / 1_000_000_000f)
                                                                        .coerceIn(0f, 0.05f)
                                                                if (dt > 0f) {
                                                                    val consumedX =
                                                                        horizontalScrollState.dispatchRawDelta(vx * dt)
                                                                    val consumedY =
                                                                        verticalScrollState.dispatchRawDelta(vy * dt)
                                                                    if (abs(consumedX) < 0.5f) vx = 0f
                                                                    if (abs(consumedY) < 0.5f) vy = 0f
                                                                    val decay = exp(-dt * 4.2f)
                                                                    vx *= decay
                                                                    vy *= decay
                                                                }
                                                            }
                                                            lastFrame = now
                                                        }
                                                    }
                                                }
                                        }
                                    }
                                }
                            },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState, enabled = false)
                                .verticalScroll(verticalScrollState, enabled = false),
                    ) {
                        Spacer(Modifier.size(proxyWidth, proxyHeight))
                    }

                    Canvas(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val visibleWindow = renderWindow ?: return@Canvas
                        val cellWidth = cellSizePx
                        val cellHeight = cellSizePx
                        val minCellEdge = minOf(cellWidth, cellHeight)
                        val visibleBoardLeft =
                            boardLeftPx + (visibleWindow.startColumn - boardDisplayWindow.startColumn) * cellWidth
                        val visibleBoardTop =
                            boardTopPx + (visibleWindow.startRow - boardDisplayWindow.startRow) * cellHeight
                        val minimumCellCodeTextSizePx = with(density) { 5.sp.toPx() }
                        val cellCodeHorizontalPaddingPx = with(density) { 2.dp.toPx() }
                        val shouldRenderCellCodes = showCellCodes && cellSize >= CELL_CODE_MIN_SIZE_DP

                        if (canUseRasterPreview) {
                            rasterPreview?.let { preview ->
                                drawImage(
                                    image = preview.image,
                                    dstOffset = IntOffset(boardLeftPx.roundToInt(), boardTopPx.roundToInt()),
                                    dstSize =
                                        IntSize(
                                            predictedBoardWidthPx.roundToInt().coerceAtLeast(1),
                                            predictedBoardHeightPx.roundToInt().coerceAtLeast(1),
                                        ),
                                    filterQuality = FilterQuality.None,
                                )
                            }
                        }
                        drawBoardCellsAndGrid(
                            project = project,
                            displayWindow = visibleWindow,
                            fullDisplayWindow = boardDisplayWindow,
                            boardLeft = visibleBoardLeft,
                            boardTop = visibleBoardTop,
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                            highlightRow = highlightRow,
                            highlightColumn = highlightColumn,
                            highlightColorCode = highlightColorCode,
                            highlightIndices = highlightIndices,
                            completedIndices = completedIndices,
                            dimCompletedIndices = dimCompletedIndices,
                            gridGuideSettings = gridGuideSettings,
                            gridStrokeWidthPx = with(density) { gridGuideSettings.lineWidthDp.dp.toPx() },
                            drawCells = !canUseRasterPreview,
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

                            for (displayRow in 0 until visibleWindow.rowCount) {
                                val row = visibleWindow.startRow + displayRow
                                for (displayColumn in 0 until visibleWindow.columnCount) {
                                    val column = visibleWindow.startColumn + displayColumn
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
                                    val textSize = minCellEdge * textSizeScale
                                    if (textSize < minimumCellCodeTextSizePx) {
                                        continue
                                    }
                                    fillPaint.textSize = textSize
                                    val maximumTextWidth = (cellWidth - cellCodeHorizontalPaddingPx * 2f).coerceAtLeast(0f)
                                    if (fillPaint.measureText(displayCode) > maximumTextWidth) {
                                        continue
                                    }
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

                                    fillPaint.color = fillColor
                                    fillPaint.alpha = if (shouldDim) 120 else 255
                                    strokePaint.textSize = textSize
                                    strokePaint.color = outlineColor
                                    strokePaint.alpha = if (shouldDim) 90 else 255

                                    val centerX = visibleBoardLeft + displayColumn * cellWidth + cellWidth / 2f
                                    val centerY = visibleBoardTop + displayRow * cellHeight + cellHeight / 2f
                                    val baseline = centerY - (fillPaint.descent() + fillPaint.ascent()) / 2f
                                    drawContext.canvas.nativeCanvas.apply {
                                        drawText(displayCode, centerX, baseline, strokePaint)
                                        drawText(displayCode, centerX, baseline, fillPaint)
                                    }
                                }
                            }
                        }

                        if (completedIndices.isNotEmpty() && minCellEdge >= 9f) {
                            for (displayRow in 0 until visibleWindow.rowCount) {
                                val row = visibleWindow.startRow + displayRow
                                for (displayColumn in 0 until visibleWindow.columnCount) {
                                    val column = visibleWindow.startColumn + displayColumn
                                    val index = row * project.width + column
                                    if (index !in completedIndices) {
                                        continue
                                    }
                                    val code = project.cells.getOrNull(index) ?: continue
                                    val baseColor = BeadPalette.find(code)?.let { Color(it.argb) } ?: Color(0xFFF2EBDD)
                                    val checkColor = if (baseColor.luminance() > 0.5f) Color(0xFF395A4D) else Color(0xFFF8F4EC)
                                    val left = visibleBoardLeft + displayColumn * cellWidth
                                    val top = visibleBoardTop + displayRow * cellHeight
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
                                    if (canUseRasterPreview) {
                                        rasterPreview?.let { preview ->
                                            drawImage(
                                                image = preview.image,
                                                dstOffset = IntOffset(boardLeft.roundToInt(), boardTop.roundToInt()),
                                                dstSize =
                                                    IntSize(
                                                        boardWidth.roundToInt().coerceAtLeast(1),
                                                        boardHeight.roundToInt().coerceAtLeast(1),
                                                    ),
                                                filterQuality = FilterQuality.None,
                                            )
                                        }
                                    }
                                    visibleBoardWindow?.let { visibleWindow ->
                                        val visibleBoardLeft =
                                            boardLeft +
                                                (visibleWindow.startColumn - boardDisplayWindow.startColumn) * cellWidth
                                        val visibleBoardTop =
                                            boardTop +
                                                (visibleWindow.startRow - boardDisplayWindow.startRow) * cellHeight
                                        drawBoardCellsAndGrid(
                                            project = project,
                                            displayWindow = visibleWindow,
                                            fullDisplayWindow = boardDisplayWindow,
                                            boardLeft = visibleBoardLeft,
                                            boardTop = visibleBoardTop,
                                            cellWidth = cellWidth,
                                            cellHeight = cellHeight,
                                            highlightRow = highlightRow,
                                            highlightColumn = highlightColumn,
                                            highlightColorCode = highlightColorCode,
                                            highlightIndices = highlightIndices,
                                            completedIndices = completedIndices,
                                            dimCompletedIndices = dimCompletedIndices,
                                            gridGuideSettings = gridGuideSettings,
                                            gridStrokeWidthPx = with(density) { gridGuideSettings.lineWidthDp.dp.toPx() },
                                            drawCells = !canUseRasterPreview,
                                        )
                                    }
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

private const val OUTPUT_MIN_SHORT_EDGE = OutputLevelScale.MIN_SHORT_EDGE
private const val OUTPUT_MAX_SHORT_EDGE = OutputLevelScale.MAX_SHORT_EDGE
private const val OUTPUT_DEFAULT_SHORT_EDGE = 20
private const val OUTPUT_PREVIEW_THROTTLE_MS = 100L
private const val MAX_SAVED_PROJECTS = 24
private const val PROJECT_STITCHING_ENABLED = true
private const val PROJECT_SPLITTING_ENABLED = true
private const val DEFAULT_BOARD_ZOOM = 20f
private const val BOARD_ZOOM_STANDARD_MIN = 2f
private const val BOARD_ZOOM_MAX = 160f
private const val CELL_CODE_MIN_SIZE_DP = 18f
private const val BOARD_THUMBNAIL_MAX_EDGE = 256
private const val BOARD_RASTER_PREVIEW_MAX_CELL_DP = 4f
private const val BOARD_RASTER_PREVIEW_MAX_EDGE = 512
private val BOARD_ZOOM_RANGE = BoardZoomMath.ABSOLUTE_MIN_ZOOM_DP..BOARD_ZOOM_MAX
private val BOARD_DRAG_BUFFER_START = 88.dp
private val BOARD_DRAG_BUFFER_END = 88.dp
private val BOARD_DRAG_BUFFER_TOP = 112.dp
private val BOARD_DRAG_BUFFER_BOTTOM = 240.dp
private const val EXPORT_MAX_BITMAP_EDGE = 4096
private const val EXPORT_TARGET_BOARD_EDGE = 1180
private const val EXPORT_MIN_CELL_SIZE = 1
private const val EXPORT_MAX_CELL_SIZE = 28
private const val EXPORT_CELL_CODE_MIN_SIZE = 16
private const val EXPORT_PAGE_MARGIN = 54
private const val EXPORT_AXIS_SIZE = 38
private const val EXPORT_TITLE_HEIGHT = 112
private const val EXPORT_LEGEND_COLUMNS = 8
private const val EXPORT_LEGEND_COLUMN_WIDTH = 150
private const val EXPORT_LEGEND_TITLE_HEIGHT = 60
private const val EXPORT_LEGEND_ROW_HEIGHT = 52
private val EXPORT_BACKGROUND_COLOR = AndroidColor.WHITE
private val EXPORT_TEXT_COLOR = AndroidColor.rgb(31, 27, 22)
private val EXPORT_MUTED_TEXT_COLOR = AndroidColor.rgb(96, 88, 78)
private val EXPORT_AXIS_TEXT_COLOR = AndroidColor.rgb(116, 98, 79)
private val EXPORT_EMPTY_CELL_COLOR = AndroidColor.rgb(242, 235, 221)
private val EXPORT_SWATCH_BORDER_COLOR = AndroidColor.argb(86, 31, 27, 22)

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
    val indices: CellIndexMask,
    val description: String,
)

private data class ColorBlockSelection(
    val colorCode: String,
    val blockIndex: Int,
    val blockCount: Int,
    val indices: IntArray,
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

private data class ProjectExportImageSpec(
    val width: Int,
    val height: Int,
    val cellSize: Int,
    val showCellCodes: Boolean,
    val margin: Int,
    val axis: Int,
    val titleHeight: Int,
    val contentWidth: Int,
    val legendColumns: Int,
    val legendRowHeight: Int,
)

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

    val bounds =
        EditorTools.occupiedBounds(
            cells = project.cells,
            width = project.width,
            height = project.height,
        )
    if (bounds == null) {
        return BoardDisplayWindow(
            startColumn = 0,
            endColumn = (project.width - 1).coerceAtLeast(0),
            startRow = 0,
            endRow = (project.height - 1).coerceAtLeast(0),
        )
    }

    return BoardDisplayWindow(
        startColumn = bounds.startColumn,
        endColumn = bounds.endColumn,
        startRow = bounds.startRow,
        endRow = bounds.endRow,
    )
}

private fun visibleBoardDisplayWindow(
    fullWindow: BoardDisplayWindow,
    boardLeftPx: Float,
    boardTopPx: Float,
    cellSizePx: Float,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
): BoardDisplayWindow? {
    val visibleColumns =
        BoardViewportMath.visibleRange(
            boardStartPx = boardLeftPx,
            cellSizePx = cellSizePx,
            cellCount = fullWindow.columnCount,
            viewportSizePx = viewportWidthPx,
        ) ?: return null
    val visibleRows =
        BoardViewportMath.visibleRange(
            boardStartPx = boardTopPx,
            cellSizePx = cellSizePx,
            cellCount = fullWindow.rowCount,
            viewportSizePx = viewportHeightPx,
        ) ?: return null
    return BoardDisplayWindow(
        startColumn = fullWindow.startColumn + visibleColumns.first,
        endColumn = fullWindow.startColumn + visibleColumns.last,
        startRow = fullWindow.startRow + visibleRows.first,
        endRow = fullWindow.startRow + visibleRows.last,
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
    fullDisplayWindow: BoardDisplayWindow = displayWindow,
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
    gridGuideSettings: GridGuideSettings,
    gridStrokeWidthPx: Float,
    drawCells: Boolean = true,
) {
    if (displayWindow.columnCount <= 0 || displayWindow.rowCount <= 0) {
        return
    }

    if (drawCells) {
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
    }

    if (gridGuideSettings.enabled) {
        val interval = gridGuideSettings.interval.coerceAtLeast(1)
        val baseGridColor = Color(gridGuideSettings.colorArgb)
        val minCellEdge = min(cellWidth, cellHeight)
        val lod = GridRenderLodPolicy.resolve(minCellEdge, interval) ?: return
        val majorStrokeLimit = (minCellEdge * 0.36f).coerceAtLeast(0.5f)
        val minorStrokeLimit = (minCellEdge * 0.18f).coerceAtLeast(0.35f)
        val majorStrokeWidth = gridStrokeWidthPx.coerceIn(0.5f, majorStrokeLimit)
        val minorStrokeWidth = (majorStrokeWidth * 0.45f).coerceIn(0.35f, minorStrokeLimit)
        val boardRight = boardLeft + displayWindow.columnCount * cellWidth
        val boardBottom = boardTop + displayWindow.rowCount * cellHeight
        val firstVisibleColumnBoundary = displayWindow.startColumn
        val lastVisibleColumnBoundary = displayWindow.endColumn + 1
        val firstVisibleRowBoundary = displayWindow.startRow
        val lastVisibleRowBoundary = displayWindow.endRow + 1

        clipRect(boardLeft, boardTop, boardRight, boardBottom) {
            fun drawVerticalBoundary(
                originalBoundaryColumn: Int,
                isMajorLine: Boolean,
            ) {
                val displayColumn = originalBoundaryColumn - displayWindow.startColumn
                val x = boardLeft + displayColumn * cellWidth
                drawLine(
                    color = baseGridColor.copy(alpha = if (isMajorLine) 0.92f else 0.32f),
                    start = Offset(x, boardTop),
                    end = Offset(x, boardBottom),
                    strokeWidth = if (isMajorLine) majorStrokeWidth else minorStrokeWidth,
                    cap = StrokeCap.Butt,
                )
            }

            fun drawHorizontalBoundary(
                originalBoundaryRow: Int,
                isMajorLine: Boolean,
            ) {
                val displayRow = originalBoundaryRow - displayWindow.startRow
                val y = boardTop + displayRow * cellHeight
                drawLine(
                    color = baseGridColor.copy(alpha = if (isMajorLine) 0.92f else 0.32f),
                    start = Offset(boardLeft, y),
                    end = Offset(boardRight, y),
                    strokeWidth = if (isMajorLine) majorStrokeWidth else minorStrokeWidth,
                    cap = StrokeCap.Butt,
                )
            }

            if (lod.showMinorLines) {
                for (originalBoundaryColumn in firstVisibleColumnBoundary..lastVisibleColumnBoundary) {
                    val isOuterLine =
                        originalBoundaryColumn == fullDisplayWindow.startColumn ||
                            originalBoundaryColumn == fullDisplayWindow.endColumn + 1
                    val isMajorLine = isOuterLine || originalBoundaryColumn % lod.majorStrideCells == 0
                    drawVerticalBoundary(originalBoundaryColumn, isMajorLine)
                }
                for (originalBoundaryRow in firstVisibleRowBoundary..lastVisibleRowBoundary) {
                    val isOuterLine =
                        originalBoundaryRow == fullDisplayWindow.startRow ||
                            originalBoundaryRow == fullDisplayWindow.endRow + 1
                    val isMajorLine = isOuterLine || originalBoundaryRow % lod.majorStrideCells == 0
                    drawHorizontalBoundary(originalBoundaryRow, isMajorLine)
                }
            } else {
                val stride = lod.majorStrideCells.toLong()
                var columnBoundary =
                    ((firstVisibleColumnBoundary.toLong() + stride - 1L) / stride) * stride
                while (columnBoundary <= lastVisibleColumnBoundary.toLong()) {
                    drawVerticalBoundary(columnBoundary.toInt(), isMajorLine = true)
                    columnBoundary += stride
                }
                val outerStartColumn = fullDisplayWindow.startColumn
                val outerEndColumn = fullDisplayWindow.endColumn + 1
                if (
                    outerStartColumn in firstVisibleColumnBoundary..lastVisibleColumnBoundary &&
                        outerStartColumn.toLong() % stride != 0L
                ) {
                    drawVerticalBoundary(outerStartColumn, isMajorLine = true)
                }
                if (
                    outerEndColumn != outerStartColumn &&
                        outerEndColumn in firstVisibleColumnBoundary..lastVisibleColumnBoundary &&
                        outerEndColumn.toLong() % stride != 0L
                ) {
                    drawVerticalBoundary(outerEndColumn, isMajorLine = true)
                }

                var rowBoundary =
                    ((firstVisibleRowBoundary.toLong() + stride - 1L) / stride) * stride
                while (rowBoundary <= lastVisibleRowBoundary.toLong()) {
                    drawHorizontalBoundary(rowBoundary.toInt(), isMajorLine = true)
                    rowBoundary += stride
                }
                val outerStartRow = fullDisplayWindow.startRow
                val outerEndRow = fullDisplayWindow.endRow + 1
                val isOuterLine =
                    outerStartRow in firstVisibleRowBoundary..lastVisibleRowBoundary &&
                        outerStartRow.toLong() % stride != 0L
                if (isOuterLine) {
                    drawHorizontalBoundary(outerStartRow, isMajorLine = true)
                }
                if (
                    outerEndRow != outerStartRow &&
                        outerEndRow in firstVisibleRowBoundary..lastVisibleRowBoundary &&
                        outerEndRow.toLong() % stride != 0L
                ) {
                    drawHorizontalBoundary(outerEndRow, isMajorLine = true)
                }
            }
        }
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
            val indicesBuilder = CellIndexMask.builder()
            for (column in 0 until project.width) {
                val index = row * project.width + column
                if (project.cells.getOrNull(index) != null) indicesBuilder.add(index)
            }
            val indices = indicesBuilder.build()
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
            val indicesBuilder = CellIndexMask.builder()
            for (row in 0 until project.height) {
                val index = row * project.width + column
                if (project.cells.getOrNull(index) != null) indicesBuilder.add(index)
            }
            val indices = indicesBuilder.build()
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
            val indicesBuilder = CellIndexMask.builder()
            project.cells.forEachIndexed { index, code ->
                if (code == colorCode) indicesBuilder.add(index)
            }
            val indices = indicesBuilder.build()
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
            BuildModeSelection(
                indices = CellIndexMask.from(selection.indices),
                description = "颜色 ${colorCodeLabel(colorCode)} 的第 ${selection.blockIndex + 1}/${selection.blockCount} 个区块",
            )
        }
    }
}

private fun resolveColorBlockSelection(
    cells: List<String?>,
    width: Int,
    height: Int,
    startIndex: Int,
): ColorBlockSelection? {
    val colorCode = cells.getOrNull(startIndex) ?: return null
    val blocks = ColorBlockIndexing.collect(cells, width, height, colorCode)
    val blockIndex = blocks.indexOfFirst { block -> startIndex in block }
    val selectedBlock = blocks.getOrNull(blockIndex) ?: return null
    return ColorBlockSelection(
        colorCode = colorCode,
        blockIndex = blockIndex,
        blockCount = blocks.size,
        indices = selectedBlock,
    )
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
    val safeBaseWidth = baseWidth.coerceAtLeast(1)
    val safeBaseHeight = baseHeight.coerceAtLeast(1)
    val maxShortEdge =
        OutputSizePolicy
            .maxShortEdge(safeBaseWidth, safeBaseHeight)
            .coerceIn(OUTPUT_MIN_SHORT_EDGE, OUTPUT_MAX_SHORT_EDGE)
    val targetEdge = level.coerceIn(OUTPUT_MIN_SHORT_EDGE, maxShortEdge)
    val requestedDimensions =
        if (safeBaseWidth >= safeBaseHeight) {
            OutputDimensions(
                width = (targetEdge * safeBaseWidth.toDouble() / safeBaseHeight.toDouble()).roundToInt().coerceAtLeast(1),
                height = targetEdge,
            )
        } else {
            OutputDimensions(
                width = targetEdge,
                height = (targetEdge * safeBaseHeight.toDouble() / safeBaseWidth.toDouble()).roundToInt().coerceAtLeast(1),
            )
        }
    val constrainedDimensions =
        OutputSizePolicy.constrainDimensions(
            width = requestedDimensions.width,
            height = requestedDimensions.height,
        )
    return current.copy(
        width = constrainedDimensions.width,
        height = constrainedDimensions.height,
    )
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
        customColorSchemeId = null,
        paletteMode = scheme.paletteMode,
        maxColors = scheme.maxColors,
    )
}

private fun applyCustomColorScheme(
    settings: GenerationSettings,
    scheme: CustomColorScheme,
): GenerationSettings {
    val normalized = scheme.normalizedOrNull() ?: return settings
    return settings.copy(
        colorScheme = normalized.baseScheme,
        customColorSchemeId = normalized.id,
        paletteMode = normalized.baseScheme.paletteMode,
        maxColors = normalized.selectedCodes.size,
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

private fun canvasAdjustModeLabel(mode: CanvasAdjustMode): String {
    return when (mode) {
        CanvasAdjustMode.Add -> "增加空白"
        CanvasAdjustMode.Remove -> "减少空白"
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

private fun canvasReduceDirectionActionText(direction: CanvasExpandDirection): String {
    return when (direction) {
        CanvasExpandDirection.Up -> "已删除顶部 1 行空白行。"
        CanvasExpandDirection.Down -> "已删除底部 1 行空白行。"
        CanvasExpandDirection.Left -> "已删除左侧 1 列空白列。"
        CanvasExpandDirection.Right -> "已删除右侧 1 列空白列。"
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

private fun projectForPngExport(project: BeadProject): BeadProject {
    val trimmed =
        EditorTools.trimOuterEmptyCells(
            cells = project.cells,
            width = project.width,
            height = project.height,
        )
    if (trimmed.width == project.width && trimmed.height == project.height && trimmed.cells === project.cells) {
        return project
    }
    return project.copy(
        width = trimmed.width,
        height = trimmed.height,
        cells = trimmed.cells,
    )
}

private fun exportProjectPngToCache(
    context: Context,
    project: BeadProject,
    gridGuideSettings: GridGuideSettings,
): File {
    val exportProject = projectForPngExport(project)
    val exportDirectory = File(context.cacheDir, "exports")
    if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
        error("Export directory could not be created.")
    }

    val fileName = exportPngFileName(exportProject)
    val file = File(exportDirectory, fileName)
    val bitmap = renderProjectExportBitmap(exportProject, gridGuideSettings)
    try {
        FileOutputStream(file).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                error("Bitmap compression failed.")
            }
        }
    } finally {
        bitmap.recycle()
    }
    return file
}

private suspend fun saveProjectPngToGallery(
    context: Context,
    project: BeadProject,
    gridGuideSettings: GridGuideSettings,
): Uri {
    val exportProject = projectForPngExport(project)
    val fileName = exportPngFileName(exportProject)
    val bitmap = renderProjectExportBitmap(exportProject, gridGuideSettings)
    try {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveProjectPngToMediaStore(context, bitmap, fileName)
        } else {
            saveProjectPngToLegacyPictures(context, bitmap, fileName)
        }
    } finally {
        bitmap.recycle()
    }
}

private fun saveProjectPngToMediaStore(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
): Uri {
    val resolver = context.contentResolver
    val nowMillis = System.currentTimeMillis()
    val nowSeconds = nowMillis / 1_000L
    val collectionUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val values =
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.TITLE, fileName.substringBeforeLast('.'))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DouDouYu/")
            put(MediaStore.Images.Media.DATE_TAKEN, nowMillis)
            put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    val uri = resolver.insert(collectionUri, values)
        ?: error("Gallery image URI could not be created.")

    try {
        resolver.openOutputStream(uri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                error("Bitmap compression failed.")
            }
        } ?: error("Gallery output stream could not be opened.")
        val publishValues =
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
                put(MediaStore.Images.Media.DATE_TAKEN, nowMillis)
                put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
                put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)
            }
        check(resolver.update(uri, publishValues, null, null) > 0) {
            "Gallery image could not be published."
        }
        resolver.notifyChange(collectionUri, null)
        return uri
    } catch (error: Throwable) {
        resolver.delete(uri, null, null)
        throw error
    }
}

private suspend fun saveProjectPngToLegacyPictures(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
): Uri {
    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DouDouYu")
    if (!directory.exists() && !directory.mkdirs()) {
        error("Gallery directory could not be created.")
    }
    val file = File(directory, fileName)
    FileOutputStream(file).use { output ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            error("Bitmap compression failed.")
        }
    }
    file.setLastModified(System.currentTimeMillis())
    return kotlin.coroutines.suspendCoroutine { continuation ->
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
            ) { _, scannedUri ->
                continuation.resumeWith(
                    scannedUri
                        ?.let { Result.success(it) }
                        ?: Result.failure(IllegalStateException("Gallery media scan failed.")),
                )
            }
        } catch (error: Throwable) {
            continuation.resumeWith(Result.failure(error))
        }
    }
}

private fun renderProjectExportBitmap(
    sourceProject: BeadProject,
    gridGuideSettings: GridGuideSettings,
): Bitmap {
    val project = projectForPngExport(sourceProject)
    val stats = PatternEngine.materialStats(project.cells)
    val spec = createProjectExportImageSpec(project = project, colorCount = stats.size)
    val bitmap = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(EXPORT_BACKGROUND_COLOR)

    val titlePaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = EXPORT_TEXT_COLOR
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = AndroidPaint.Align.LEFT
        }
    val subtitlePaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = EXPORT_MUTED_TEXT_COLOR
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = AndroidPaint.Align.LEFT
        }
    val maxTitleWidth = spec.width - spec.margin * 2f
    canvas.drawText(
        ellipsizeForPaint(project.title.ifBlank { "拼豆图纸" }, titlePaint, maxTitleWidth),
        spec.margin.toFloat(),
        spec.margin + 34f,
        titlePaint,
    )
    val totalBeads = stats.sumOf { it.count }
    val subtitle =
        "${BeadPalette.schemeLabel(project.colorScheme, project.customColorSchemeId)} · " +
            "${project.width} x ${project.height} · ${totalBeads} 颗"
    canvas.drawText(
        ellipsizeForPaint(subtitle, subtitlePaint, maxTitleWidth),
        spec.margin.toFloat(),
        spec.margin + 70f,
        subtitlePaint,
    )

    val boardWidth = project.width.coerceAtLeast(1) * spec.cellSize
    val boardHeight = project.height.coerceAtLeast(1) * spec.cellSize
    val boardAreaWidth = spec.axis * 2 + boardWidth
    val boardAreaLeft = spec.margin + (spec.contentWidth - boardAreaWidth) / 2f
    val gridLeft = boardAreaLeft + spec.axis
    val gridTop = spec.margin + spec.titleHeight + spec.axis.toFloat()

    drawExportCells(
        canvas = canvas,
        project = project,
        spec = spec,
        gridLeft = gridLeft,
        gridTop = gridTop,
    )
    drawExportGrid(
        canvas = canvas,
        project = project,
        spec = spec,
        gridLeft = gridLeft,
        gridTop = gridTop,
        gridGuideSettings = gridGuideSettings.normalized(),
    )
    drawExportAxisLabels(
        canvas = canvas,
        project = project,
        spec = spec,
        gridLeft = gridLeft,
        gridTop = gridTop,
        boardWidth = boardWidth.toFloat(),
        boardHeight = boardHeight.toFloat(),
    )
    drawExportLegend(
        canvas = canvas,
        stats = stats,
        spec = spec,
        top = gridTop + boardHeight + spec.axis,
    )

    return bitmap
}

private fun drawExportCells(
    canvas: AndroidCanvas,
    project: BeadProject,
    spec: ProjectExportImageSpec,
    gridLeft: Float,
    gridTop: Float,
) {
    val fillPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { style = AndroidPaint.Style.FILL }
    val codePaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    val rect = RectF()
    val cellSize = spec.cellSize.toFloat()

    for (row in 0 until project.height) {
        for (column in 0 until project.width) {
            val index = row * project.width + column
            val code = project.cells.getOrNull(index)
            val paletteColor = BeadPalette.find(code)
            val color = paletteColor?.argb ?: EXPORT_EMPTY_CELL_COLOR
            val left = gridLeft + column * cellSize
            val top = gridTop + row * cellSize
            rect.set(left, top, left + cellSize, top + cellSize)
            fillPaint.color = color
            canvas.drawRect(rect, fillPaint)

            if (code != null && paletteColor != null && spec.showCellCodes) {
                val label = paletteColor.code
                codePaint.color = exportTextColorFor(color)
                codePaint.textSize = if (label.length <= 3) 13f else 11f
                drawCenteredText(canvas, label, left + cellSize / 2f, top + cellSize / 2f, codePaint)
            }
        }
    }
}

private fun drawExportGrid(
    canvas: AndroidCanvas,
    project: BeadProject,
    spec: ProjectExportImageSpec,
    gridLeft: Float,
    gridTop: Float,
    gridGuideSettings: GridGuideSettings,
) {
    if (!gridGuideSettings.enabled) {
        return
    }

    val gridPaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.SQUARE
        }
    val cellSize = spec.cellSize.toFloat()
    val boardWidth = project.width * cellSize
    val boardHeight = project.height * cellSize
    val interval = gridGuideSettings.interval.coerceAtLeast(1)
    val majorColor = gridGuideSettings.colorArgb
    val minorColor = with(gridGuideSettings.colorArgb) {
        android.graphics.Color.argb(
            (android.graphics.Color.alpha(this) * 0.32f).roundToInt().coerceIn(0, 255),
            android.graphics.Color.red(this),
            android.graphics.Color.green(this),
            android.graphics.Color.blue(this),
        )
    }
    val exportDensityScale = spec.cellSize / DEFAULT_BOARD_ZOOM
    val majorStrokeLimit = (cellSize * 0.36f).coerceAtLeast(0.5f)
    val minorStrokeLimit = (cellSize * 0.18f).coerceAtLeast(0.25f)
    val majorStrokeWidth =
        (gridGuideSettings.lineWidthDp * exportDensityScale)
            .coerceIn(0.5f, majorStrokeLimit)
    val minorStrokeWidth = (majorStrokeWidth * 0.45f).coerceIn(0.25f, minorStrokeLimit)

    for (column in 0..project.width) {
        val isMajorLine = column == 0 || column == project.width || column % interval == 0
        val x = gridLeft + column * cellSize
        gridPaint.color = if (isMajorLine) majorColor else minorColor
        gridPaint.strokeWidth = if (isMajorLine) majorStrokeWidth else minorStrokeWidth
        canvas.drawLine(x, gridTop, x, gridTop + boardHeight, gridPaint)
    }
    for (row in 0..project.height) {
        val isMajorLine = row == 0 || row == project.height || row % interval == 0
        val y = gridTop + row * cellSize
        gridPaint.color = if (isMajorLine) majorColor else minorColor
        gridPaint.strokeWidth = if (isMajorLine) majorStrokeWidth else minorStrokeWidth
        canvas.drawLine(gridLeft, y, gridLeft + boardWidth, y, gridPaint)
    }
}

private fun drawExportAxisLabels(
    canvas: AndroidCanvas,
    project: BeadProject,
    spec: ProjectExportImageSpec,
    gridLeft: Float,
    gridTop: Float,
    boardWidth: Float,
    boardHeight: Float,
) {
    val axisPaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = EXPORT_AXIS_TEXT_COLOR
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = AndroidPaint.Align.CENTER
        }
    val step = exportAxisLabelStep(spec.cellSize)
    val cellSize = spec.cellSize.toFloat()
    val topLabelY = gridTop - spec.axis / 2f
    val bottomLabelY = gridTop + boardHeight + spec.axis / 2f
    val leftLabelX = gridLeft - spec.axis / 2f
    val rightLabelX = gridLeft + boardWidth + spec.axis / 2f

    for (column in 0 until project.width) {
        if (!shouldShowExportAxisLabel(index = column, total = project.width, step = step)) {
            continue
        }
        val label = (column + 1).toString()
        val centerX = gridLeft + (column + 0.5f) * cellSize
        drawCenteredText(canvas, label, centerX, topLabelY, axisPaint)
        drawCenteredText(canvas, label, centerX, bottomLabelY, axisPaint)
    }
    for (row in 0 until project.height) {
        if (!shouldShowExportAxisLabel(index = row, total = project.height, step = step)) {
            continue
        }
        val label = (row + 1).toString()
        val centerY = gridTop + (row + 0.5f) * cellSize
        drawCenteredText(canvas, label, leftLabelX, centerY, axisPaint)
        drawCenteredText(canvas, label, rightLabelX, centerY, axisPaint)
    }
}

private fun drawExportLegend(
    canvas: AndroidCanvas,
    stats: List<MaterialStat>,
    spec: ProjectExportImageSpec,
    top: Float,
) {
    val titlePaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = EXPORT_TEXT_COLOR
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = AndroidPaint.Align.LEFT
        }
    val detailPaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = EXPORT_TEXT_COLOR
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = AndroidPaint.Align.LEFT
        }
    val emptyPaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = EXPORT_MUTED_TEXT_COLOR
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = AndroidPaint.Align.LEFT
        }
    val swatchPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { style = AndroidPaint.Style.FILL }
    val borderPaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE
            color = EXPORT_SWATCH_BORDER_COLOR
            strokeWidth = 1f
        }
    val codePaint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    val legendLeft = spec.margin.toFloat()
    val itemWidth = spec.contentWidth / spec.legendColumns.toFloat()
    val itemsTop = top + 46f
    val swatchSize = 40f
    val rect = RectF()

    canvas.drawText("颜色用量", legendLeft, top + 24f, titlePaint)
    if (stats.isEmpty()) {
        canvas.drawText("暂无珠子", legendLeft, itemsTop + 18f, emptyPaint)
        return
    }

    stats.forEachIndexed { index, stat ->
        val column = index % spec.legendColumns
        val row = index / spec.legendColumns
        val itemLeft = legendLeft + column * itemWidth
        val itemTop = itemsTop + row * spec.legendRowHeight
        rect.set(itemLeft, itemTop, itemLeft + swatchSize, itemTop + swatchSize)
        swatchPaint.color = stat.argb
        canvas.drawRoundRect(rect, 8f, 8f, swatchPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        codePaint.color = exportTextColorFor(stat.argb)
        codePaint.textSize = if (stat.displayCode.length <= 3) 13f else 11f
        drawCenteredText(canvas, stat.displayCode, itemLeft + swatchSize / 2f, itemTop + swatchSize / 2f, codePaint)

        val textLeft = itemLeft + swatchSize + 10f
        drawCenteredText(canvas, "x${stat.count}", textLeft, itemTop + swatchSize / 2f, detailPaint)
    }
}

private fun createProjectExportImageSpec(
    project: BeadProject,
    colorCount: Int,
): ProjectExportImageSpec {
    val safeProjectWidth = project.width.coerceAtLeast(1)
    val safeProjectHeight = project.height.coerceAtLeast(1)
    val longestSide = maxOf(safeProjectWidth, safeProjectHeight)
    var cellSize = (EXPORT_TARGET_BOARD_EDGE / longestSide).coerceIn(EXPORT_MIN_CELL_SIZE, EXPORT_MAX_CELL_SIZE)

    fun buildSpec(cell: Int): ProjectExportImageSpec {
        val boardWidth = safeProjectWidth * cell
        val boardHeight = safeProjectHeight * cell
        val boardAreaWidth = EXPORT_AXIS_SIZE * 2 + boardWidth
        val baseHeight = EXPORT_PAGE_MARGIN * 2 + EXPORT_TITLE_HEIGHT + EXPORT_AXIS_SIZE * 2 + boardHeight
        val availableLegendHeight = (EXPORT_MAX_BITMAP_EDGE - baseHeight).coerceAtLeast(EXPORT_LEGEND_TITLE_HEIGHT + EXPORT_LEGEND_ROW_HEIGHT)
        val maxLegendRows = ((availableLegendHeight - EXPORT_LEGEND_TITLE_HEIGHT) / EXPORT_LEGEND_ROW_HEIGHT).coerceAtLeast(1)
        val maxLegendColumns = ((EXPORT_MAX_BITMAP_EDGE - EXPORT_PAGE_MARGIN * 2) / EXPORT_LEGEND_COLUMN_WIDTH).coerceAtLeast(1)
        val legendColumns =
            ceil(colorCount.coerceAtLeast(1) / maxLegendRows.toDouble())
                .toInt()
                .coerceIn(EXPORT_LEGEND_COLUMNS, maxLegendColumns)
        val legendRows = ceil(colorCount.coerceAtLeast(1) / legendColumns.toDouble()).toInt()
        val contentWidth = maxOf(boardAreaWidth, legendColumns * EXPORT_LEGEND_COLUMN_WIDTH)
        val legendHeight = EXPORT_LEGEND_TITLE_HEIGHT + legendRows * EXPORT_LEGEND_ROW_HEIGHT
        return ProjectExportImageSpec(
            width = EXPORT_PAGE_MARGIN * 2 + contentWidth,
            height = EXPORT_PAGE_MARGIN * 2 + EXPORT_TITLE_HEIGHT + EXPORT_AXIS_SIZE * 2 + boardHeight + legendHeight,
            cellSize = cell,
            showCellCodes = cell >= EXPORT_CELL_CODE_MIN_SIZE,
            margin = EXPORT_PAGE_MARGIN,
            axis = EXPORT_AXIS_SIZE,
            titleHeight = EXPORT_TITLE_HEIGHT,
            contentWidth = contentWidth,
            legendColumns = legendColumns,
            legendRowHeight = EXPORT_LEGEND_ROW_HEIGHT,
        )
    }

    var spec = buildSpec(cellSize)
    while ((spec.width > EXPORT_MAX_BITMAP_EDGE || spec.height > EXPORT_MAX_BITMAP_EDGE) && cellSize > EXPORT_MIN_CELL_SIZE) {
        cellSize -= 1
        spec = buildSpec(cellSize)
    }
    require(spec.width <= EXPORT_MAX_BITMAP_EDGE && spec.height <= EXPORT_MAX_BITMAP_EDGE) {
        "Export dimensions exceed the bitmap memory limit."
    }
    return spec
}

private fun drawCenteredText(
    canvas: AndroidCanvas,
    text: String,
    centerX: Float,
    centerY: Float,
    paint: AndroidPaint,
) {
    val baseline = centerY - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(text, centerX, baseline, paint)
}

private fun ellipsizeForPaint(
    text: String,
    paint: AndroidPaint,
    maxWidth: Float,
): String {
    if (text.isBlank() || paint.measureText(text) <= maxWidth) {
        return text
    }
    val ellipsis = "..."
    var end = text.length
    while (end > 0 && paint.measureText(text.take(end) + ellipsis) > maxWidth) {
        end -= 1
    }
    return if (end == 0) ellipsis else text.take(end) + ellipsis
}

private fun exportTextColorFor(argb: Int): Int {
    val luminance =
        (AndroidColor.red(argb) * 0.299 + AndroidColor.green(argb) * 0.587 + AndroidColor.blue(argb) * 0.114) / 255.0
    return if (luminance > 0.56) EXPORT_TEXT_COLOR else AndroidColor.WHITE
}

private fun exportAxisLabelStep(cellSize: Int): Int {
    val rawStep = ceil(36.0 / cellSize.coerceAtLeast(1)).toInt().coerceAtLeast(1)
    return when {
        rawStep <= 1 -> 1
        rawStep <= 2 -> 2
        rawStep <= 5 -> 5
        else -> 10
    }
}

private fun shouldShowExportAxisLabel(
    index: Int,
    total: Int,
    step: Int,
): Boolean {
    return index == 0 || index == total - 1 || (index + 1) % step == 0
}

private fun safeExportFileName(title: String): String {
    val normalized = title.trim().replace(Regex("""[\\/:*?"<>|\s]+"""), "_").trim('_')
    return normalized.ifBlank { "douduoyu-pattern" }.take(48)
}

private fun exportPngFileName(project: BeadProject): String {
    val exportProject = projectForPngExport(project)
    return "${safeExportFileName(exportProject.title)}-${exportProject.width}x${exportProject.height}-${System.currentTimeMillis()}.png"
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

