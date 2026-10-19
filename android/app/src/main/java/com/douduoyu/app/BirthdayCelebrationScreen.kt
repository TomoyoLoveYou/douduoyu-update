@file:OptIn(ExperimentalMaterial3Api::class)

package com.douduoyu.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** 3D 动画结束、拼豆开始成形之后，才让文案面板淡入。 */
private const val BIRTHDAY_MESSAGE_START = 0.95f
private const val BIRTHDAY_MESSAGE_END = 0.998f

/**
 * Displays the birthday reward after a code has been redeemed.
 *
 * 3D 蛋糕动画由 WebView + three.js 渲染（assets/birthday），
 * 时间轴、镜头运动、彩带、像素化与拼豆转场都在页面内完成；
 * 这里只负责进度订阅、文案面板与保存项目等原生交互。
 *
 * Passing [reduceMotion] or disabling Android's animator duration scale shows the completed
 * composition without running the entrance animation.
 */
@Composable
internal fun BirthdayCelebrationScreen(
    config: BirthdayCelebrationConfig,
    onBack: () -> Unit,
    onSaveProject: (BeadProject) -> Unit,
    reduceMotion: Boolean = false,
    qualityOverride: BirthdayAnimationQuality? = null,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val useSidePanel = configuration.screenWidthDp > configuration.screenHeightDp

    val systemAnimationsDisabled =
        remember(context) {
            runCatching {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            }.getOrDefault(false)
        }
    val shouldReduceMotion = reduceMotion || systemAnimationsDisabled
    val detectedQuality = remember(context) { BirthdayAnimationQualitySelector.detect(context) }
    val quality = qualityOverride ?: detectedQuality

    val controller = remember(config.id) { BirthdayCakeController() }
    val fallbackProject = remember(config.id) { BirthdayCelebration.createCakeProject() }
    val celebrationStore = remember { BirthdayCelebrationStore(context) }
    // 兑换后的第一遍固定草莓奶油；之后每次进入页面都随机换一个不同颜色
    var cakeTheme by remember(config.id) { mutableIntStateOf(pickCakeTheme(celebrationStore)) }
    // 转场终点背景色：与拼豆项目页（PatternBoard 外层）一致的米黄色
    val finalBackgroundHex = "#F1E8DB"

    var progress by remember(config.id) { mutableFloatStateOf(if (shouldReduceMotion) 1f else 0f) }
    var isComplete by remember(config.id) { mutableStateOf(shouldReduceMotion) }
    var pageReady by remember(config.id) { mutableStateOf(false) }
    // 由 96×96 像素取样映射出的拼豆工程；保存时直接用这个
    var generatedProject by remember(config.id) { mutableStateOf<BeadProject?>(null) }

    val effectiveComplete = isComplete || progress >= 0.995f
    val messageAlphaTarget =
        maxOf(
            celebrationPhase(progress, BIRTHDAY_MESSAGE_START, BIRTHDAY_MESSAGE_END),
            if (effectiveComplete) 1f else 0f,
        )
    val messageAlpha by animateFloatAsState(
        targetValue = messageAlphaTarget,
        animationSpec = tween(durationMillis = 320),
        label = "birthdayMessageAlpha",
    )

    // 不依赖 WebView 的 onReady 回调：加载后主动打通桥接
    LaunchedEffect(config.id) {
        kotlinx.coroutines.delay(800)
        controller.markReady()
        controller.setLayout(useSidePanel)
        controller.setFinalBackground(finalBackgroundHex)
        if (shouldReduceMotion) {
            // 系统开了“减少动态效果”：连彩带也定格
            controller.skip(animateConfetti = false)
        }
    }

    BackHandler(onBack = onBack)

    // 生日祝福页锁定竖屏：进出页面时切换 requestedOrientation，离开后恢复系统默认（可自动旋转）
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("生日祝福") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回设置",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            progress = 1f
                            isComplete = true
                            controller.skip()
                        },
                        enabled = !effectiveComplete,
                        modifier = Modifier.alpha(if (effectiveComplete) 0f else 1f),
                    ) {
                        Text("跳过")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            BirthdayCakeWebView(
                controller = controller,
                quality = quality,
                initialTheme = cakeTheme,
                modifier = Modifier.fillMaxSize(),
                onReady = { pageReady = true },
                onProgress = { progress = it },
                onComplete = {
                    progress = 1f
                    isComplete = true
                },
                onPixelReady = { dataUrl ->
                    val mapped =
                        runCatching { birthdayPixelProject(dataUrl) }.getOrNull()
                            ?: fallbackProject
                    Log.d("BirthdayCake", "mapped ${mapped.width}x${mapped.height} cells=${mapped.cells.size}")
                    generatedProject = mapped
                    controller.markReady()
                    controller.setPattern(birthdayBeadsJson(mapped))
                },
            )

            if (messageAlpha > 0.002f) {
                BirthdayMessagePanel(
                    nickname = config.nickname,
                    message = config.message,
                    actionsEnabled = effectiveComplete,
                    onReplay = {
                        progress = 0f
                        isComplete = false
                        // 旧颜色的工程不能留着，等新的取样结果覆盖
                        generatedProject = null
                        val nextTheme = pickCakeTheme(celebrationStore)
                        cakeTheme = nextTheme
                        controller.setTheme(nextTheme)
                        controller.restart()
                    },
                    onSaveProject = {
                        onSaveProject(generatedProject ?: fallbackProject)
                    },
                    isSidePanel = useSidePanel,
                    modifier =
                        if (useSidePanel) {
                            Modifier
                                .align(Alignment.CenterEnd)
                                .widthIn(min = 260.dp, max = 340.dp)
                                .fillMaxHeight()
                                .alpha(messageAlpha)
                        } else {
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .alpha(messageAlpha)
                        },
                )
            }
        }
    }
}

@Composable
private fun BirthdayMessagePanel(
    nickname: String,
    message: String,
    actionsEnabled: Boolean,
    onReplay: () -> Unit,
    onSaveProject: () -> Unit,
    isSidePanel: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = if (isSidePanel) Modifier.fillMaxHeight() else Modifier) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSidePanel) {
                                Modifier.weight(1f)
                            } else {
                                Modifier
                            },
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "愿每一颗拼豆，都把${nickname}今天的快乐拼得更完整。",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.size(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 横屏侧栏只有 ~220dp 可用，两个按钮各占一半时省略图标，避免文字被挤断
                    val buttonPadding = PaddingValues(horizontal = if (isSidePanel) 10.dp else 16.dp)
                    FilledTonalButton(
                        onClick = onReplay,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f),
                        contentPadding = buttonPadding,
                    ) {
                        if (!isSidePanel) {
                            Icon(Icons.Outlined.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("再看一次", maxLines = 1)
                    }
                    Button(
                        onClick = onSaveProject,
                        enabled = actionsEnabled,
                        modifier = Modifier.weight(1f),
                        contentPadding = buttonPadding,
                    ) {
                        if (!isSidePanel) {
                            Icon(Icons.Outlined.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("保存为项目")
                    }
                }
            }
        }
    }
}

/**
 * 96×96 像素取样（PNG data URL）→ 按 Mard 221 色板映射成拼豆工程。
 * 透明像素（alpha < 128）留空，保证工程里只有蛋糕本身。
 */
private fun birthdayPixelProject(dataUrl: String): BeadProject? {
    if (dataUrl.isBlank()) return null
    val payload = dataUrl.substringAfter("base64,", "")
    if (payload.isEmpty()) return null
    val bytes = Base64.decode(payload, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) {
        bitmap.recycle()
        return null
    }

    val palette = BeadPalette.palette(ColorSchemePreset.MardBrand221)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    bitmap.recycle()

    val cells = ArrayList<String?>(pixels.size)
    val cache = HashMap<Int, String>()
    for (pixel in pixels) {
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha < 128) {
            cells.add(null)
            continue
        }
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        val key = (red shl 16) or (green shl 8) or blue
        cells.add(
            cache.getOrPut(key) {
                BeadPalette.nearest(red, green, blue, palette).id
            },
        )
    }

    return BeadProject(
        id = BirthdayCelebration.GIFT_ID,
        title = BirthdayCelebration.PROJECT_TITLE,
        width = width,
        height = height,
        cells = cells,
        paletteMode = ColorSchemePreset.MardBrand221.paletteMode,
        colorScheme = ColorSchemePreset.MardBrand221,
        sourceKind = ProjectSourceKind.Celebration,
        updatedAt = System.currentTimeMillis(),
    )
}

/** 把工程转成 {w,h,palette,cells} 的色板索引网格，回传给 WebView 绘制。 */
private fun birthdayBeadsJson(project: BeadProject): String {
    val palette = BeadPalette.palette(project.colorScheme)
    val indexById = HashMap<String, Int>()
    val hexes = ArrayList<String>()
    palette.forEach { color ->
        if (!indexById.containsKey(color.id)) {
            indexById[color.id] = hexes.size
            hexes.add(String.format(Locale.ROOT, "#%06X", color.argb and 0xFFFFFF))
        }
    }
    val cells = JSONArray()
    project.cells.forEach { id ->
        cells.put(if (id == null) -1 else indexById[id] ?: -1)
    }
    return JSONObject()
        .put("w", project.width)
        .put("h", project.height)
        .put("palette", JSONArray(hexes))
        .put("cells", cells)
        .toString()
}

private fun celebrationPhase(
    progress: Float,
    start: Float,
    end: Float,
): Float {
    if (end <= start) return if (progress >= end) 1f else 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

/**
 * 从 Compose 的 LocalContext 里找到宿主 Activity（ContextWrapper 一层层往外剥）。
 * 用于进出页面时临时锁定/恢复屏幕方向。
 */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * 挑选这一遍要播的蛋糕配色。
 *
 * - 兑换后的第一遍固定「草莓奶油」（THEMES[0]），作为这份礼物的默认样子；
 * - 之后每次播放（重进页面或点「再看一次」）都从其余配色里随机挑一个，
 *   且不与上一次重复，保证每次看到的都是不同颜色；
 * - 选择结果会写回存储，下次播放据此避免连播同一个颜色。
 */
private fun pickCakeTheme(store: BirthdayCelebrationStore): Int {
    if (!store.hasPlayedCake()) {
        store.recordCakeTheme(BirthdayCelebration.CAKE_THEME_STRAWBERRY)
        return BirthdayCelebration.CAKE_THEME_STRAWBERRY
    }
    val last = store.lastCakeTheme()
    val pool =
        (1 until BirthdayCelebration.CAKE_THEME_COUNT).filter { it != last }
    val next =
        if (pool.isEmpty()) {
            BirthdayCelebration.CAKE_THEME_STRAWBERRY
        } else {
            pool.random()
        }
    store.recordCakeTheme(next)
    return next
}
