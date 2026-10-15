@file:OptIn(ExperimentalMaterial3Api::class)

package com.douduoyu.app

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val CELEBRATION_DURATION_MILLIS = 30_000

private object CelebrationTimeline {
    const val NIGHT_END = 0.10f
    const val GIANT_METEOR_START = 0.22f
    const val METEOR_IMPACT = 0.31f
    const val POND_SPLASH_END = 0.37f
    const val POND_COLOR_START = 0.42f
    const val POND_COLORED = 0.55f
    const val GIFT_START = 0.58f
    const val GIFT_ARRIVED = 0.68f
    const val GIFT_SHAKE_END = 0.77f
    const val FISH_EXIT = 0.75f
    const val GIFT_OPEN = 0.86f
    const val CAKE_START = 0.80f
    const val CAKE_FORMED = 0.945f
    const val CANDLES_START = 0.95f
    const val CANDLES_LIT = 0.975f
    const val MESSAGE_START = 0.98f
    const val MESSAGE_END = 0.998f
}

private val NIGHT_BACKGROUND = Color(0xFF10172E)
private val NIGHT_BACKGROUND_ACCENT = Color(0xFF182443)
private val STAR_GOLD = Color(0xFFFFD166)
private val STAR_PALE = Color(0xFFFFF1B8)
private val POND_BLUE = Color(0xFF48A9E6)
private val POND_MINT = Color(0xFF42C7BE)
private val POND_ROSE = Color(0xFFE782A9)
private val BANK_GRASS = Color(0xFF354A40)
private val SHORE_COLOR = Color(0xFF68705C)
private val METEOR_OUTLINE = Color(0xFF332D3B)
private val METEOR_STONE = Color(0xFF716779)
private val METEOR_HIGHLIGHT = Color(0xFFAFA2B5)
private val GIFT_PINK = Color(0xFFF06C9B)
private val GIFT_LIGHT = Color(0xFFFFB5CB)
private val RIBBON_GOLD = Color(0xFFFFC857)
private val FINAL_SPARKLE_POSITIONS =
    floatArrayOf(
        0.05f, 0.47f,
        0.12f, 0.18f,
        0.34f, 0.05f,
        0.66f, 0.04f,
        0.88f, 0.18f,
        0.96f, 0.47f,
        0.83f, 0.76f,
        0.17f, 0.78f,
    )
private val POND_BANK_GRASS_X = floatArrayOf(-0.46f, -0.37f, -0.29f, 0.30f, 0.38f, 0.46f)
private val METEOR_BODY_PATTERN =
    arrayOf(
        "...OOO...",
        "..OHHOO..",
        ".OHSSSO..",
        "OOSSSSSO.",
        "OSSSGSSOO",
        "OOSSSSSO.",
        ".OOSSSO..",
        "..OOOO...",
    )
private val STAR_PIXEL_POSITIONS =
    floatArrayOf(
        0.06f, 0.16f,
        0.14f, 0.07f,
        0.23f, 0.22f,
        0.34f, 0.10f,
        0.44f, 0.29f,
        0.55f, 0.06f,
        0.66f, 0.18f,
        0.77f, 0.09f,
        0.88f, 0.25f,
        0.95f, 0.12f,
        0.09f, 0.38f,
        0.18f, 0.31f,
        0.29f, 0.45f,
        0.38f, 0.35f,
        0.49f, 0.15f,
        0.58f, 0.42f,
        0.69f, 0.33f,
        0.81f, 0.47f,
        0.91f, 0.37f,
        0.04f, 0.49f,
        0.12f, 0.25f,
        0.26f, 0.06f,
        0.36f, 0.48f,
        0.47f, 0.39f,
        0.61f, 0.27f,
        0.72f, 0.44f,
        0.84f, 0.16f,
        0.93f, 0.52f,
        0.52f, 0.52f,
        0.75f, 0.24f,
    )

private data class CelebrationBead(
    val column: Int,
    val row: Int,
    val color: Color,
)

private data class BirthdayCakeComposition(
    val width: Int,
    val height: Int,
    val beads: List<CelebrationBead>,
)

private data class BirthdayCakeLayout(
    val cellSize: Float,
    val origin: Offset,
)

private data class SceneBead(
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val delay: Float,
    val sizeScale: Float = 1f,
)

private data class GiftBead(
    val x: Float,
    val y: Float,
    val color: Color,
    val isLid: Boolean,
)

private data class BirthdayCelebrationScene(
    val cake: BirthdayCakeComposition,
    val nightBeads: List<SceneBead>,
    val giftBeads: List<GiftBead>,
)

/**
 * Displays the birthday reward after a code has been redeemed.
 *
 * The caller owns navigation and project creation. Passing [reduceMotion] or disabling Android's
 * animator duration scale shows the completed composition without running the entrance animation.
 */
@Composable
internal fun BirthdayCelebrationScreen(
    config: BirthdayCelebrationConfig,
    onBack: () -> Unit,
    onSaveProject: () -> Unit,
    reduceMotion: Boolean = false,
    qualityOverride: BirthdayAnimationQuality? = null,
) {
    val context = LocalContext.current
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
    val initialQuality = remember(context) { BirthdayAnimationQualitySelector.detect(context) }
    var qualityName by rememberSaveable(config.id, qualityOverride) {
        mutableStateOf((qualityOverride ?: initialQuality).name)
    }
    val quality =
        remember(qualityName) {
            runCatching { BirthdayAnimationQuality.valueOf(qualityName) }
                .getOrDefault(BirthdayAnimationQuality.Low)
        }
    LaunchedEffect(qualityOverride) {
        if (qualityOverride != null && qualityName != qualityOverride.name) {
            qualityName = qualityOverride.name
        }
    }
    val timeline = remember { Animatable(0f) }
    var replayKey by rememberSaveable(config.id) { mutableIntStateOf(0) }
    var wasSkipped by remember(config.id, replayKey) { mutableStateOf(shouldReduceMotion) }

    LaunchedEffect(config.id, replayKey, shouldReduceMotion, wasSkipped) {
        if (wasSkipped) {
            timeline.snapTo(1f)
            return@LaunchedEffect
        }
        timeline.snapTo(if (shouldReduceMotion) 1f else 0f)
        if (!shouldReduceMotion) {
            timeline.animateTo(
                targetValue = 1f,
                animationSpec =
                    tween(
                        durationMillis = CELEBRATION_DURATION_MILLIS,
                        easing = LinearEasing,
                    ),
            )
        }
    }

    val progress = timeline.value
    val isComplete = progress >= 0.999f
    val finalContentAlpha =
        celebrationPhase(
            progress,
            start = CelebrationTimeline.MESSAGE_START,
            end = CelebrationTimeline.MESSAGE_END,
        )
    val messagePanelAlpha = max(finalContentAlpha, if (isComplete) 1f else 0f)

    BackHandler(onBack = onBack)
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
                            wasSkipped = true
                        },
                        enabled = !isComplete,
                        modifier = Modifier.alpha(if (isComplete) 0f else 1f),
                    ) {
                        Text("跳过")
                    }
                },
            )
        },
    ) { innerPadding ->
        BirthdayCelebrationLayout(
            nickname = config.nickname,
            message = config.message,
            progress = progress,
            isComplete = isComplete,
            animateFlames = !shouldReduceMotion,
            quality = quality,
            allowAdaptiveDowngrade = qualityOverride == null,
            onQualityDowngrade = {
                qualityName = quality.lower().name
            },
            finalContentAlpha = messagePanelAlpha,
            onReplay = { replayKey += 1 },
            onSaveProject = onSaveProject,
            onBack = onBack,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }
}

@Composable
private fun BirthdayCelebrationLayout(
    nickname: String,
    message: String,
    progress: Float,
    isComplete: Boolean,
    animateFlames: Boolean,
    quality: BirthdayAnimationQuality,
    allowAdaptiveDowngrade: Boolean,
    onQualityDowngrade: () -> Unit,
    finalContentAlpha: Float,
    onReplay: () -> Unit,
    onSaveProject: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useSidePanel = maxWidth > maxHeight
            BirthdayCakeCanvas(
                nickname = nickname,
                progress = progress,
                animateFlames = animateFlames,
                quality = quality,
                allowAdaptiveDowngrade = allowAdaptiveDowngrade,
                onQualityDowngrade = onQualityDowngrade,
                modifier = Modifier.fillMaxSize(),
            )
            if (finalContentAlpha > 0f) {
                BirthdayMessagePanel(
                    nickname = nickname,
                    message = message,
                    actionsEnabled = isComplete,
                    onReplay = onReplay,
                    onSaveProject = onSaveProject,
                    onBack = onBack,
                    isSidePanel = useSidePanel,
                    modifier =
                        if (useSidePanel) {
                            Modifier
                                .align(Alignment.CenterEnd)
                                .widthIn(min = 260.dp, max = 340.dp)
                                .fillMaxHeight()
                                .alpha(finalContentAlpha)
                        } else {
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .alpha(finalContentAlpha)
                        },
                )
            }
        }
    }
}

@Composable
private fun BirthdayCakeCanvas(
    nickname: String,
    progress: Float,
    animateFlames: Boolean,
    quality: BirthdayAnimationQuality,
    allowAdaptiveDowngrade: Boolean,
    onQualityDowngrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = quality.detail
    val scene = remember(quality) { createBirthdayCelebrationScene(detail.nightStarCount) }
    val officialFish = ImageBitmap.imageResource(R.drawable.dd_app_icon_fish_left_official_rose_transparent)
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var twinkleTick by remember { mutableIntStateOf(0) }
    val latestProgress by rememberUpdatedState(progress)
    LaunchedEffect(animateFlames, isResumed, quality, allowAdaptiveDowngrade) {
        if (!animateFlames || !isResumed || quality == BirthdayAnimationQuality.Low || !allowAdaptiveDowngrade) {
            return@LaunchedEffect
        }
        var previousFrameNanos = withFrameNanos { it }
        var measuredFrameCount = 0
        var slowFrameCount = 0
        while (latestProgress < CelebrationTimeline.CANDLES_START) {
            val frameNanos = withFrameNanos { it }
            val frameMillis = (frameNanos - previousFrameNanos) / 1_000_000L
            previousFrameNanos = frameNanos
            if (frameMillis in 5L..200L) {
                measuredFrameCount += 1
                if (frameMillis >= 28L) slowFrameCount += 1
            }
            if (measuredFrameCount >= 30) {
                if (slowFrameCount >= 10) {
                    onQualityDowngrade()
                    return@LaunchedEffect
                }
                measuredFrameCount = 0
                slowFrameCount = 0
            }
        }
    }
    LaunchedEffect(animateFlames, isResumed, detail.twinkleIntervalMillis) {
        if (!animateFlames || !isResumed) {
            twinkleTick = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(detail.twinkleIntervalMillis)
            twinkleTick = (twinkleTick + 1) % 48
        }
    }
    Box(
        modifier =
            modifier.semantics {
                contentDescription = "送给${nickname}的拼豆生日蛋糕"
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(NIGHT_BACKGROUND)
            drawNightSky(scene.nightBeads, progress, twinkleTick, detail)
            drawBirthdayPond(progress, detail)
            drawMeteorFishStory(officialFish, progress, detail)
            drawGiftBox(scene.giftBeads, progress, detail)

            val cakeLayout = birthdayCakeLayout(size, scene.cake.width, scene.cake.height)
            val cellSize = cakeLayout.cellSize
            val gridOrigin = cakeLayout.origin
            drawFinalSparkles(
                progress = progress,
                twinkleTick = twinkleTick,
                gridOrigin = gridOrigin,
                cellSize = cellSize,
                gridWidth = scene.cake.width,
                gridHeight = scene.cake.height,
                stardustCount = detail.finalStardustCount,
            )
            drawCakeFromGift(
                cake = scene.cake,
                progress = progress,
                flameTick = twinkleTick,
                gridOrigin = gridOrigin,
                cellSize = cellSize,
                shadowStride = detail.cakeShadowStride,
                specularStride = detail.cakeSpecularStride,
            )
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
    onBack: () -> Unit,
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
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onSaveProject,
                        enabled = actionsEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存为项目")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onReplay,
                            enabled = actionsEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("再看一次")
                        }
                        TextButton(
                            onClick = onBack,
                            enabled = actionsEnabled,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("返回设置")
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawNightSky(
    beads: List<SceneBead>,
    progress: Float,
    twinkleTick: Int,
    detail: BirthdayAnimationDetail,
) {
    val assemble = celebrationPhase(progress, 0f, CelebrationTimeline.NIGHT_END)
    if (assemble <= 0f) return
    val baseSide = max(1.8f, min(size.width, size.height) * 0.0065f)
    beads.forEachIndexed { index, bead ->
        val start = bead.delay * 0.76f
        val localProgress = celebrationPhase(assemble, start, min(1f, start + 0.24f))
        if (localProgress <= 0f) return@forEachIndexed
        val target = Offset(size.width * bead.xFraction, size.height * bead.yFraction)
        val twinkleAngle = twinkleTick * PI * 2.0 / 48.0 + index * 1.71
        val twinkle =
            0.38f +
                0.62f *
                ((sin(twinkleAngle).toFloat() + 1f) / 2f)
        val pulse = 0.78f + twinkle * 0.30f
        if (detail.starGlowStride > 0 && index % detail.starGlowStride == 0) {
            drawCircle(
                color = bead.color.copy(alpha = twinkle * 0.10f),
                center = target,
                radius = baseSide * (1.55f + twinkle * 0.8f),
            )
        }
        drawSceneSquare(
            center = target,
            side = baseSide * bead.sizeScale * FastOutSlowInEasing.transform(localProgress) * pulse,
            color = bead.color,
            alpha = min(1f, localProgress * 2.2f) * twinkle,
        )
        if (detail.starRayStride > 0 && index % detail.starRayStride == 0 && twinkle > 0.84f) {
            val raySide = baseSide * 0.28f
            val rayOffset = baseSide * 0.64f
            listOf(
                Offset(rayOffset, 0f),
                Offset(-rayOffset, 0f),
                Offset(0f, rayOffset),
                Offset(0f, -rayOffset),
            ).forEach { offset ->
                drawSceneSquare(
                    center = target + offset,
                    side = raySide,
                    color = bead.color,
                    alpha = twinkle * 0.46f,
                )
            }
        }
    }
}

private fun DrawScope.drawMeteorFishStory(
    officialFish: ImageBitmap,
    progress: Float,
    detail: BirthdayAnimationDetail,
) {
    drawPassingMeteors(progress, detail.passingMeteorTrailCount)

    val impact = Offset(size.width * 0.52f, size.height * 0.755f)
    val meteorStart = Offset(size.width * 0.06f, -size.height * 0.08f)
    val meteorControl = Offset(size.width * 0.38f, size.height * 0.10f)
    val fall =
        celebrationPhase(
            progress,
            CelebrationTimeline.GIANT_METEOR_START,
            CelebrationTimeline.METEOR_IMPACT,
        )
    if (fall > 0f && fall < 1f) {
        val eased = fall * fall
        val meteorHead = quadraticBezier(meteorStart, meteorControl, impact, eased)
        val pixelSide = max(1.8f, min(size.width, size.height) * 0.0058f) * (0.86f + fall * 0.28f)
        repeat(detail.giantMeteorTrailCount) { index ->
            val previousProgress = eased - index * (0.016f * 24f / detail.giantMeteorTrailCount)
            if (previousProgress <= 0f) return@repeat
            val trail = index / (detail.giantMeteorTrailCount - 1f).coerceAtLeast(1f)
            val trailCenter = quadraticBezier(meteorStart, meteorControl, impact, previousProgress)
            drawSceneSquare(
                center = trailCenter,
                side = pixelSide * (1f - trail * 0.55f),
                color = if (index % 4 == 0) STAR_GOLD else STAR_PALE,
                alpha = (1f - trail) * min(1f, fall * 3f),
            )
        }
        drawMeteorBody(
            center = meteorHead,
            pixelSide = pixelSide,
            alpha = min(1f, fall * 3f),
            rotation = fall * 300f,
        )
        if (detail.meteorDebrisCount > 0) {
            repeat(detail.meteorDebrisCount) { index ->
                val angle = fall * PI * 3.2 + index * (PI * 2.0 / detail.meteorDebrisCount)
                val debrisDistance = pixelSide * (2.4f + index * 1.15f) * fall
                drawSceneSquare(
                    center =
                        meteorHead +
                            Offset(
                                cos(angle).toFloat() * debrisDistance,
                                sin(angle).toFloat() * debrisDistance,
                            ),
                    side = pixelSide * (0.34f + (index % 2) * 0.18f),
                    color = if (index % 2 == 0) METEOR_HIGHLIGHT else STAR_GOLD,
                    alpha = min(1f, fall * 2.4f) * (1f - fall * 0.25f),
                )
            }
        }
        drawCircle(
            color = STAR_GOLD.copy(alpha = 0.18f + fall * 0.20f),
            center = meteorHead,
            radius = pixelSide * 5.5f,
        )
    }

    val splash =
        celebrationPhase(
            progress,
            CelebrationTimeline.METEOR_IMPACT,
            CelebrationTimeline.POND_SPLASH_END,
        )
    if (splash > 0f && splash < 1f) {
        val splashAlpha = (1f - splash * 0.82f).coerceIn(0f, 1f)
        val pixelSide = max(2f, min(size.width, size.height) * 0.0072f)
        val columnHeight = size.height * 0.13f * sin(PI * splash).toFloat().coerceAtLeast(0f)
        val columnRows = 9
        repeat(columnRows) { row ->
            val rowFraction = (row + 1f) / columnRows
            if (rowFraction * size.height * 0.13f <= columnHeight) {
                val spread = (1f - rowFraction) * pixelSide * 0.75f
                drawSceneSquare(
                    center =
                        impact - Offset(0f, rowFraction * size.height * 0.13f) +
                            Offset(if (row % 2 == 0) -spread else spread, 0f),
                    side = pixelSide * (1.28f - rowFraction * 0.38f),
                    color = if (row % 3 == 0) STAR_PALE else POND_BLUE,
                    alpha = splashAlpha,
                )
            }
        }
        val centerIndex = (detail.meteorImpactDropCount - 1) / 2f
        repeat(detail.meteorImpactDropCount) { index ->
            val direction = (index - centerIndex) / centerIndex.coerceAtLeast(1f)
            val distance = size.width * direction * (0.09f + (index % 3) * 0.018f) * splash
            val arcHeight = size.height * (0.07f + (index % 4) * 0.014f)
            val vertical = -arcHeight * 4f * splash * (1f - splash) + size.height * 0.012f * splash
            drawSceneSquare(
                center = impact + Offset(distance, vertical),
                side = pixelSide * (0.72f + (index % 3) * 0.18f) * (1f - splash * 0.28f),
                color =
                    when (index % 4) {
                        0 -> STAR_PALE
                        1 -> POND_MINT
                        else -> POND_BLUE
                    },
                alpha = splashAlpha,
            )
        }
    }

    val giftRise = celebrationPhase(progress, CelebrationTimeline.GIFT_START, CelebrationTimeline.GIFT_ARRIVED)
    val fishDrop =
        celebrationPhase(
            progress,
            CelebrationTimeline.GIFT_ARRIVED,
            CelebrationTimeline.FISH_EXIT,
        )
    val pondStart = Offset(size.width * 0.61f, size.height * 0.77f)
    val pondEnd = Offset(size.width * 0.43f, size.height * 0.77f)
    drawFishWaterTransition(
        center = pondStart,
        splashProgress = celebrationPhase(progress, CelebrationTimeline.GIFT_START, 0.635f),
        dropCount = detail.fishSplashDropCount,
    )
    if (giftRise > 0f && fishDrop < 1f) {
        val gravityDrop = fishDrop * fishDrop
        val arcProgress =
            if (fishDrop > 0f) {
                0.5f + gravityDrop * 0.5f
            } else {
                giftRise * 0.5f
            }
        val center = parabolicArc(pondStart, pondEnd, size.height * 0.18f, arcProgress)
        drawOfficialFish(
            image = officialFish,
            center = center,
            width = min(size.width, size.height) * 0.18f,
            alpha = min(1f, giftRise * 3f) * (1f - celebrationPhase(fishDrop, 0.92f, 1f)),
            rotation =
                parabolicTangentRotation(
                    start = pondStart,
                    end = pondEnd,
                    arcHeight = size.height * 0.18f,
                    progress = arcProgress,
                ),
            swimPhase = arcProgress * 2.6f,
            bendAmount = 0.52f + abs(sin(PI * arcProgress * 2f)).toFloat() * 0.38f,
            bodyStretch = sin(PI * arcProgress).toFloat(),
            segmentCount = detail.fishSegmentCount,
            waveHarmonics = detail.fishWaveHarmonics,
        )
        if (detail.fishBubbleCount > 0) {
            repeat(detail.fishBubbleCount) { index ->
                val bubblePhase = ((arcProgress * 1.6f + index * 0.23f) % 1f)
                drawCircle(
                    color = STAR_PALE.copy(alpha = (1f - bubblePhase) * 0.58f),
                    center =
                        center +
                            Offset(
                                x = -size.width * (0.035f + index * 0.012f),
                                y = size.height * (0.025f + bubblePhase * 0.04f),
                            ),
                    radius = min(size.width, size.height) * (0.006f + index * 0.0015f),
                    style = Stroke(width = max(1f, min(size.width, size.height) * 0.002f)),
                )
            }
        }
    }
    drawFishWaterTransition(
        center = pondEnd,
        splashProgress = celebrationPhase(progress, CelebrationTimeline.FISH_EXIT, 0.835f),
        dropCount = detail.fishSplashDropCount,
    )
}

private fun DrawScope.drawFishWaterTransition(
    center: Offset,
    splashProgress: Float,
    dropCount: Int,
) {
    val unit = max(1.8f, min(size.width, size.height) * 0.0068f)
    if (splashProgress <= 0f || splashProgress >= 1f) return

    val alpha = (1f - splashProgress * 0.78f).coerceIn(0f, 1f)
    val waterColumnHeight =
        size.height * 0.055f * sin(PI * splashProgress).toFloat().coerceAtLeast(0f)
    repeat(4) { row ->
        val rowFraction = (row + 1f) / 4f
        if (rowFraction * size.height * 0.055f <= waterColumnHeight) {
            drawSceneSquare(
                center = center - Offset(0f, rowFraction * size.height * 0.055f),
                side = unit * (1.15f - rowFraction * 0.24f),
                color = if (row % 2 == 0) STAR_PALE else POND_BLUE,
                alpha = alpha,
            )
        }
    }
    val centerIndex = (dropCount - 1) / 2f
    repeat(dropCount) { index ->
        val direction = (index - centerIndex) / centerIndex.coerceAtLeast(1f)
        val horizontal =
            direction * size.width * (0.028f + (index % 3) * 0.008f) * splashProgress
        val arcHeight = size.height * (0.035f + (index % 4) * 0.010f)
        val vertical =
            -arcHeight * 4f * splashProgress * (1f - splashProgress) +
                size.height * 0.008f * splashProgress
        drawSceneSquare(
            center = center + Offset(horizontal, vertical),
            side = unit * (0.72f + (index % 3) * 0.16f) * (1f - splashProgress * 0.24f),
            color =
                when (index % 3) {
                    0 -> STAR_PALE
                    1 -> POND_MINT
                    else -> POND_BLUE
                },
            alpha = alpha,
        )
    }
}

private fun DrawScope.drawPassingMeteors(progress: Float, trailCount: Int) {
    val starts = floatArrayOf(0.095f, 0.155f)
    starts.forEachIndexed { meteorIndex, start ->
        val travel = celebrationPhase(progress, start, start + 0.055f)
        if (travel <= 0f || travel >= 1f) return@forEachIndexed
        val startPoint =
            if (meteorIndex == 0) {
                Offset(-size.width * 0.14f, size.height * 0.06f)
            } else {
                Offset(-size.width * 0.16f, size.height * 0.16f)
            }
        val controlPoint =
            if (meteorIndex == 0) {
                Offset(size.width * 0.40f, size.height * 0.12f)
            } else {
                Offset(size.width * 0.46f, size.height * 0.23f)
            }
        val endPoint =
            if (meteorIndex == 0) {
                Offset(size.width * 1.16f, size.height * 0.42f)
            } else {
                Offset(size.width * 1.18f, size.height * 0.52f)
            }
        val eased = FastOutSlowInEasing.transform(travel)
        val head = quadraticBezier(startPoint, controlPoint, endPoint, eased)
        val pixelSide = max(1.45f, min(size.width, size.height) * 0.0038f)
        val visibility = min(1f, travel * 7f)
        val tangent =
            Offset(
                x =
                    2f *
                        ((1f - eased) * (controlPoint.x - startPoint.x) +
                            eased * (endPoint.x - controlPoint.x)),
                y =
                    2f *
                        ((1f - eased) * (controlPoint.y - startPoint.y) +
                            eased * (endPoint.y - controlPoint.y)),
            )
        val tangentLength = sqrt(tangent.x * tangent.x + tangent.y * tangent.y)
        val direction =
            if (tangentLength > 0f) {
                Offset(tangent.x / tangentLength, tangent.y / tangentLength)
            } else {
                Offset(1f, 0f)
            }
        repeat(trailCount) { index ->
            val trail = index / (trailCount - 1f).coerceAtLeast(1f)
            val distance = pixelSide * (1f + index * 1.05f)
            val trailCenter =
                head -
                    Offset(
                        x = direction.x * distance,
                        y = direction.y * distance,
                    )
            drawSceneSquare(
                center = trailCenter,
                side = pixelSide * (1.05f - trail * 0.58f),
                color = if ((meteorIndex + index) % 4 == 0) STAR_GOLD else STAR_PALE,
                alpha = visibility * (1f - trail * 0.82f),
            )
        }
        drawMeteorBody(
            center = head,
            pixelSide = pixelSide,
            alpha = visibility,
            rotation = 292f + meteorIndex * 8f + travel * 30f,
        )
        drawCircle(
            color = STAR_GOLD.copy(alpha = visibility * 0.26f),
            center = head,
            radius = pixelSide * 5.2f,
        )
    }
}

private fun DrawScope.drawBirthdayPond(
    progress: Float,
    detail: BirthdayAnimationDetail,
) {
    val reveal = celebrationPhase(progress, 0.12f, 0.20f)
    if (reveal <= 0f) return
    val colorCycle =
        celebrationPhase(
            progress,
            CelebrationTimeline.POND_COLOR_START,
            CelebrationTimeline.POND_COLORED,
        )
    val pondColor =
        when {
            colorCycle < 0.34f -> lerpSceneColor(POND_BLUE, POND_MINT, colorCycle / 0.34f)
            colorCycle < 0.67f -> lerpSceneColor(POND_MINT, POND_ROSE, (colorCycle - 0.34f) / 0.33f)
            else -> lerpSceneColor(POND_ROSE, POND_BLUE, (colorCycle - 0.67f) / 0.33f)
        }
    val center = Offset(size.width * 0.52f, size.height * 0.76f)
    val pondWidth = size.width * (0.20f + reveal * 0.48f)
    val pondHeight = size.height * (0.035f + reveal * 0.11f)
    val grassSide = max(1.5f, min(size.width, size.height) * 0.0045f)
    drawOval(
        color = BANK_GRASS.copy(alpha = reveal * 0.82f),
        topLeft = Offset(center.x - pondWidth * 0.62f, center.y - pondHeight * 0.36f),
        size = Size(pondWidth * 0.27f, pondHeight * 0.64f),
    )
    drawOval(
        color = BANK_GRASS.copy(alpha = reveal * 0.82f),
        topLeft = Offset(center.x + pondWidth * 0.35f, center.y - pondHeight * 0.31f),
        size = Size(pondWidth * 0.27f, pondHeight * 0.64f),
    )
    POND_BANK_GRASS_X.forEachIndexed { index, offsetX ->
        val base =
            Offset(
                x = center.x + pondWidth * offsetX,
                y = center.y - pondHeight * (0.13f + (index % 2) * 0.08f),
            )
        drawLine(
            color = POND_MINT.copy(alpha = reveal * 0.58f),
            start = base,
            end = base + Offset(if (index < 3) -grassSide else grassSide, -grassSide * 2.4f),
            strokeWidth = grassSide,
        )
    }
    drawOval(
        color = SHORE_COLOR.copy(alpha = reveal * 0.92f),
        topLeft = Offset(center.x - pondWidth * 0.58f, center.y - pondHeight * 0.64f),
        size = Size(pondWidth * 1.16f, pondHeight * 1.28f),
    )
    drawOval(
        color = pondColor.copy(alpha = 0.22f),
        topLeft = Offset(center.x - pondWidth * 0.56f, center.y - pondHeight * 0.56f),
        size = Size(pondWidth * 1.12f, pondHeight * 1.12f),
    )
    drawOval(
        color = pondColor.copy(alpha = 0.82f),
        topLeft = Offset(center.x - pondWidth / 2f, center.y - pondHeight / 2f),
        size = Size(pondWidth, pondHeight),
    )
    if (detail.pondShimmerCount > 0) {
        repeat(detail.pondShimmerCount) { index ->
            val shimmer = (progress * 1.35f + sceneHash(index, 401)) % 1f
            val shimmerX = center.x - pondWidth * 0.38f + shimmer * pondWidth * 0.76f
            val shimmerY = center.y - pondHeight * (0.16f + sceneHash(index, 433) * 0.42f)
            val shimmerLength = pondWidth * (0.035f + sceneHash(index, 467) * 0.055f)
            drawLine(
                color = STAR_PALE.copy(alpha = (1f - shimmer) * 0.22f * reveal),
                start = Offset(shimmerX - shimmerLength / 2f, shimmerY),
                end = Offset(shimmerX + shimmerLength / 2f, shimmerY),
                strokeWidth = max(1f, min(size.width, size.height) * 0.0022f),
                cap = StrokeCap.Round,
            )
        }
    }

    val meteorWave =
        celebrationPhase(
            progress,
            CelebrationTimeline.METEOR_IMPACT,
            CelebrationTimeline.POND_COLOR_START,
        )
    val fishExitWave =
        celebrationPhase(
            progress,
            CelebrationTimeline.GIFT_START,
            CelebrationTimeline.GIFT_ARRIVED,
        )
    val fishEntryWave =
        celebrationPhase(
            progress,
            CelebrationTimeline.FISH_EXIT,
            CelebrationTimeline.GIFT_OPEN,
        )
    val disturbancePhase =
        when {
            meteorWave > 0f && meteorWave < 1f -> meteorWave
            fishExitWave > 0f && fishExitWave < 1f -> fishExitWave
            fishEntryWave > 0f && fishEntryWave < 1f -> fishEntryWave
            else -> 0f
        }
    val disturbanceCenterX =
        when {
            meteorWave > 0f && meteorWave < 1f -> size.width * 0.52f
            fishExitWave > 0f && fishExitWave < 1f -> size.width * 0.61f
            fishEntryWave > 0f && fishEntryWave < 1f -> size.width * 0.43f
            else -> center.x
        }
    val disturbanceStrength =
        sin(PI * disturbancePhase).toFloat().coerceAtLeast(0f)

    val rippleCount = detail.pondRippleCount
    val rippleCenterIndex = (rippleCount - 1) / 2f
    repeat(rippleCount) { index ->
        val ripple = (progress * 9f + index / rippleCount.toFloat()) % 1f
        val sidePush = (index - rippleCenterIndex) * pondWidth * 0.025f * disturbanceStrength
        val interactionPush = (center.x - disturbanceCenterX) * 0.22f * disturbanceStrength
        val rippleCenter =
            center +
                Offset(
                    x = sidePush + interactionPush,
                    y = sin((ripple + index) * PI * 2f).toFloat() * pondHeight * 0.04f * disturbanceStrength,
                )
        val rippleWidth =
            pondWidth *
                (0.24f + ripple * 0.68f) *
                (1f + disturbanceStrength * (0.12f + abs(index - 1) * 0.05f))
        val rippleHeight = pondHeight * (0.28f + ripple * 0.66f) * (1f + disturbanceStrength * 0.08f)
        drawOval(
            color =
                STAR_PALE.copy(
                    alpha = (1f - ripple) * (0.28f + disturbanceStrength * 0.22f) * reveal,
                ),
            topLeft = Offset(rippleCenter.x - rippleWidth / 2f, rippleCenter.y - rippleHeight / 2f),
            size = Size(rippleWidth, rippleHeight),
            style = Stroke(width = max(1f, min(size.width, size.height) * 0.003f)),
        )
    }
    if (disturbancePhase > 0f && disturbancePhase < 1f) {
        repeat(detail.pondResponseRippleCount) { index ->
            val response = (disturbancePhase - index * 0.13f).coerceIn(0f, 1f)
            if (response > 0f) {
                val responseCenter =
                    Offset(
                        x = disturbanceCenterX + (center.x - disturbanceCenterX) * response * 0.12f,
                        y = center.y,
                    )
                val responseWidth = pondWidth * (0.08f + response * 0.72f)
                val responseHeight = pondHeight * (0.12f + response * 0.68f)
                drawOval(
                    color =
                        lerpSceneColor(STAR_PALE, pondColor, response * 0.18f).copy(
                            alpha = (1f - response) * (0.52f + disturbanceStrength * 0.18f) * reveal,
                        ),
                    topLeft =
                        Offset(
                            responseCenter.x - responseWidth / 2f,
                            responseCenter.y - responseHeight / 2f,
                        ),
                    size = Size(responseWidth, responseHeight),
                    style = Stroke(width = max(1f, min(size.width, size.height) * 0.0038f)),
                )
            }
        }
    }
}

private fun DrawScope.drawOfficialFish(
    image: ImageBitmap,
    center: Offset,
    width: Float,
    alpha: Float,
    rotation: Float,
    swimPhase: Float,
    bendAmount: Float,
    bodyStretch: Float,
    segmentCount: Int,
    waveHarmonics: Float,
) {
    if (alpha <= 0f || width <= 0f) return
    val fishWidth = width * (1f + bodyStretch.coerceIn(0f, 1f) * 0.07f)
    val fishHeight = width * (1f - bodyStretch.coerceIn(0f, 1f) * 0.05f)
    val left = center.x - fishWidth / 2f
    val top = center.y - fishHeight / 2f
    val overlap = max(1, (fishWidth / 120f).toInt())
    rotate(degrees = rotation, pivot = center) {
        repeat(segmentCount) { segment ->
            val sourceLeft = image.width * segment / segmentCount
            val sourceRight = image.width * (segment + 1) / segmentCount
            val destinationLeft = (fishWidth * segment / segmentCount).toInt()
            val destinationRight = (fishWidth * (segment + 1) / segmentCount).toInt()
            val normalized = segment / (segmentCount - 1f)
            val tailWeight = ((normalized - 0.24f) / 0.76f).coerceIn(0f, 1f)
            val weightedBend = tailWeight * tailWeight * bendAmount.coerceIn(0f, 1f)
            val primaryWave = sin((swimPhase - normalized * 0.55f) * PI * 2f).toFloat()
            val secondaryWave =
                sin((swimPhase * 1.65f - normalized * 0.82f) * PI * 2f).toFloat() * waveHarmonics
            val wave = (primaryWave + secondaryWave) / (1f + waveHarmonics * 0.35f)
            val verticalOffset = wave * fishHeight * 0.11f * weightedBend
            val destinationStart = destinationLeft - if (segment == 0) 0 else overlap
            val destinationEnd = destinationRight + if (segment == segmentCount - 1) 0 else overlap
            drawImage(
                image = image,
                srcOffset = IntOffset(max(0, sourceLeft - 1), 0),
                srcSize =
                    IntSize(
                        (min(image.width, sourceRight + 1) - max(0, sourceLeft - 1)).coerceAtLeast(1),
                        image.height,
                    ),
                dstOffset = IntOffset((left + destinationStart).toInt(), (top + verticalOffset).toInt()),
                dstSize =
                    IntSize(
                        (destinationEnd - destinationStart).coerceAtLeast(1),
                        fishHeight.toInt().coerceAtLeast(1),
                    ),
                alpha = alpha.coerceIn(0f, 1f),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

private fun DrawScope.drawMeteorBody(
    center: Offset,
    pixelSide: Float,
    alpha: Float,
    rotation: Float,
) {
    if (alpha <= 0f || pixelSide <= 0f) return
    rotate(rotation, center) {
        val centerX = (METEOR_BODY_PATTERN.maxOf(String::length) - 1) / 2f
        val centerY = (METEOR_BODY_PATTERN.size - 1) / 2f
        METEOR_BODY_PATTERN.forEachIndexed { row, line ->
            line.forEachIndexed { column, marker ->
                val color =
                    when (marker) {
                        'O' -> METEOR_OUTLINE
                        'S' -> METEOR_STONE
                        'H' -> METEOR_HIGHLIGHT
                        'G' -> STAR_GOLD
                        else -> null
                    }
                if (color != null) {
                    drawSceneSquare(
                        center =
                            center +
                                Offset(
                                    (column - centerX) * pixelSide,
                                    (row - centerY) * pixelSide,
                                ),
                        side = pixelSide * 0.90f,
                        color = color,
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGiftBox(
    beads: List<GiftBead>,
    progress: Float,
    detail: BirthdayAnimationDetail,
) {
    if (progress < CelebrationTimeline.GIFT_START || progress >= 0.88f) return
    val arrival =
        celebrationPhase(
            progress,
            CelebrationTimeline.GIFT_START,
            CelebrationTimeline.GIFT_ARRIVED,
        )
    val shake = celebrationPhase(progress, CelebrationTimeline.GIFT_ARRIVED, CelebrationTimeline.GIFT_SHAKE_END)
    val open = celebrationPhase(progress, CelebrationTimeline.GIFT_SHAKE_END, CelebrationTimeline.GIFT_OPEN)
    val fade = 1f - celebrationPhase(progress, 0.80f, 0.88f)
    val unit = max(2.2f, min(size.width, size.height) * 0.0105f)
    val pondStart = Offset(size.width * 0.61f, size.height * 0.77f)
    val pondEnd = Offset(size.width * 0.43f, size.height * 0.77f)
    val fishCenter =
        parabolicArc(
            start = pondStart,
            end = pondEnd,
            arcHeight = size.height * 0.18f,
            progress = arrival * 0.5f,
        )
    val fishCollisionRadius = min(size.width, size.height) * 0.18f * 0.44f
    val giftBottomRadius = unit * 5.5f
    val collisionGap = unit * 0.42f
    val baseCenter =
        fishCenter -
            Offset(
                x = 0f,
                y = fishCollisionRadius + giftBottomRadius + collisionGap,
            )
    val shakeEnvelope = sin(PI * shake).toFloat().coerceAtLeast(0f)
    val center =
        baseCenter +
            Offset(
                x = sin(shake * PI * 17.0).toFloat() * unit * 0.70f * shakeEnvelope,
                y = -abs(sin(shake * PI * 7.0).toFloat()) * unit * 0.25f * shakeEnvelope,
            )
    val lidFlipAngle = open * (PI * 0.32).toFloat()
    val lidDepthScale = cos(lidFlipAngle.toDouble()).toFloat().coerceAtLeast(0.52f)
    val lidLift = sin(lidFlipAngle.toDouble()).toFloat() * unit * 2.15f
    val lidTiltAngle = -open * (PI * 0.12).toFloat()
    val lidTiltCos = cos(lidTiltAngle.toDouble()).toFloat()
    val lidTiltSin = sin(lidTiltAngle.toDouble()).toFloat()
    val lidHingeRow = -2.5f
    if (open > 0f && detail.giftLightParticleCount > 0) {
        repeat(detail.giftLightParticleCount) { index ->
            val angle = index * (PI * 2.0 / detail.giftLightParticleCount) + PI * 0.15
            val distance = unit * (2.8f + (index % 4) * 0.8f) * open
            val particleCenter =
                center +
                    Offset(
                        x = cos(angle).toFloat() * distance,
                        y = sin(angle).toFloat() * distance - unit * open * 1.2f,
                    )
            drawSceneSquare(
                center = particleCenter,
                side = unit * (0.22f + (index % 3) * 0.06f) * (1f - open * 0.28f),
                color = if (index % 3 == 0) STAR_PALE else RIBBON_GOLD,
                alpha = (1f - open * 0.55f).coerceIn(0f, 1f),
            )
        }
    }
    if (detail.giftShineBandCount > 0 && fade > 0f) {
        repeat(detail.giftShineBandCount) { index ->
            val sweep = (shake * 0.8f + open * 1.35f + index * 0.42f) % 1f
            val shineX = center.x + (sweep - 0.5f) * unit * 9.5f
            drawLine(
                color = Color.White.copy(alpha = fade * (0.12f + open * 0.16f)),
                start = Offset(shineX, center.y - unit * 1.7f),
                end = Offset(shineX + unit * 1.0f, center.y + unit * 4.6f),
                strokeWidth = unit * 0.34f,
                cap = StrokeCap.Round,
            )
        }
    }
    beads.forEach { bead ->
        if (bead.isLid) {
            val localX = bead.x * unit
            val localY = (bead.y - lidHingeRow) * unit * lidDepthScale
            val tiltedX = localX * lidTiltCos - localY * lidTiltSin
            val tiltedY = localX * lidTiltSin + localY * lidTiltCos
            val hinge =
                center +
                    Offset(
                        x = open * unit * 0.38f,
                        y = lidHingeRow * unit - lidLift,
                    )
            val beadCenter =
                hinge + Offset(tiltedX, tiltedY)
            drawRoundRect(
                color = bead.color.copy(alpha = fade.coerceIn(0f, 1f)),
                topLeft =
                    Offset(
                        x = beadCenter.x - unit * 0.43f,
                        y = beadCenter.y - unit * 0.43f * lidDepthScale,
                    ),
                size = Size(unit * 0.86f, unit * 0.86f * lidDepthScale),
                cornerRadius = CornerRadius(unit * 0.17f * lidDepthScale),
            )
        } else {
            drawSceneSquare(
                center = center + Offset(bead.x * unit, bead.y * unit),
                side = unit * 0.86f,
                color = bead.color,
                alpha = fade,
            )
        }
    }
}

private fun DrawScope.drawCakeFromGift(
    cake: BirthdayCakeComposition,
    progress: Float,
    flameTick: Int,
    gridOrigin: Offset,
    cellSize: Float,
    shadowStride: Int,
    specularStride: Int,
) {
    val reveal =
        celebrationPhase(
            progress,
            CelebrationTimeline.CAKE_START,
            CelebrationTimeline.CAKE_FORMED,
        )
    val candleLight =
        celebrationPhase(
            progress,
            CelebrationTimeline.CANDLES_START,
            CelebrationTimeline.CANDLES_LIT,
        )
    if (reveal <= 0f) return
    val targetSide = cellSize * 0.84f
    val giftUnit = max(1.8f, min(size.width, size.height) * 0.0105f)
    val giftCenter = Offset(size.width * 0.52f, size.height * 0.51f)
    cake.beads.forEachIndexed { index, bead ->
        val candleIndex =
            if (bead.row in 0..10) {
                BirthdayCelebration.candleColumns.indexOfFirst { column ->
                    bead.column in (column - 1)..(column + 2)
                }
            } else {
                -1
            }
        val localCandleLight =
            if (candleIndex >= 0) {
                celebrationPhase(candleLight, candleIndex * 0.16f, 0.68f + candleIndex * 0.12f)
            } else {
                0f
            }
        val stagger = ((index * 47) % 101) / 101f * 0.35f
        val localProgress = celebrationPhase(reveal, stagger, min(1f, stagger + 0.58f))
        if (localProgress <= 0f) return@forEachIndexed
        val eased = FastOutSlowInEasing.transform(localProgress)
        val giftColumn = index % 13 - 6
        val giftRow = (index / 13) % 11 - 5
        val origin = giftCenter + Offset(giftColumn * giftUnit * 0.82f, giftRow * giftUnit * 0.82f)
        val target =
            gridOrigin +
                Offset(
                    x = (bead.column + 0.5f) * cellSize,
                    y = (bead.row + 0.5f) * cellSize,
                )
        val arcHeight =
            -sin(PI * eased).toFloat() * cellSize * (1.7f + ((index * 29) % 7) * 0.18f)
        val center = lerp(origin, target, eased) + Offset(0f, arcHeight)
        val side = giftUnit * 0.72f + (targetSide - giftUnit * 0.72f) * eased
        val alpha = min(1f, localProgress * 2.5f)
        val isFlamePixel = candleIndex >= 0 && bead.row in 0..3
        val displayColor =
            if (isFlamePixel) {
                lerpSceneColor(
                    start = brightenCelebrationCakeColor(bead.color),
                    end = birthdayFlameColor(bead, candleIndex, flameTick),
                    fraction = localCandleLight,
                )
            } else {
                brightenCelebrationCakeColor(bead.color)
            }
        if (shadowStride != Int.MAX_VALUE && side >= 4f && eased > 0.70f && index % shadowStride == 0) {
            drawSceneSquare(
                center = center + Offset(side * 0.10f, side * 0.12f),
                side = side * 0.94f,
                color = NIGHT_BACKGROUND.copy(alpha = alpha * 0.16f),
                alpha = 1f,
            )
        }
        drawSceneSquare(center, side, displayColor, alpha)
        if (side >= 5f && eased > 0.72f) {
            val topLeft = Offset(center.x - side / 2f, center.y - side / 2f)
            drawRoundRect(
                color = NIGHT_BACKGROUND_ACCENT.copy(alpha = alpha * 0.30f),
                topLeft = topLeft,
                size = Size(side, side),
                cornerRadius = CornerRadius(side * 0.20f),
                style = Stroke(width = max(1f, cellSize * 0.045f)),
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.38f),
                radius = side * 0.075f,
                center = center - Offset(side * 0.18f, side * 0.18f),
            )
        }
        if (specularStride != Int.MAX_VALUE && side >= 4f && eased > 0.78f && index % specularStride == 0) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.24f),
                radius = side * 0.045f,
                center = center - Offset(side * 0.22f, side * 0.22f),
            )
        }
    }
}

private fun birthdayFlameColor(
    bead: CelebrationBead,
    candleIndex: Int,
    flameTick: Int,
): Color {
    val candleStart = BirthdayCelebration.candleColumns[candleIndex] - 1
    val localColumn = bead.column - candleStart
    val frame = (flameTick / 2 + candleIndex * 2) % 6
    val flicker = (bead.row * 3 + localColumn + frame) % 6
    val isHotCenter = localColumn in 1..2
    return when {
        bead.row == 0 && frame % 3 == 0 -> STAR_PALE
        bead.row == 0 -> STAR_GOLD
        isHotCenter && flicker <= 1 -> Color(0xFFFFF8D0)
        isHotCenter -> STAR_GOLD
        flicker == 0 -> Color(0xFFFFC247)
        else -> Color(0xFFFF7A45)
    }
}

private fun DrawScope.drawFinalSparkles(
    progress: Float,
    twinkleTick: Int,
    gridOrigin: Offset,
    cellSize: Float,
    gridWidth: Int,
    gridHeight: Int,
    stardustCount: Int,
) {
    val reveal = celebrationPhase(progress, CelebrationTimeline.CANDLES_LIT, CelebrationTimeline.MESSAGE_END)
    if (reveal <= 0f) return
    val loop = twinkleTick / 48f
    val gridPixelWidth = gridWidth * cellSize
    val gridPixelHeight = gridHeight * cellSize

    repeat(FINAL_SPARKLE_POSITIONS.size / 2) { index ->
        val entrance =
            celebrationPhase(
                reveal,
                start = (index % 4) * 0.06f,
                end = 0.52f + (index % 4) * 0.06f,
            )
        if (entrance <= 0f) return@repeat
        val wave =
            ((sin(PI * 2.0 * loop + index * 1.37).toFloat() + 1f) / 2f)
                .coerceIn(0f, 1f)
        val center =
            gridOrigin +
                Offset(
                    x = gridPixelWidth * FINAL_SPARKLE_POSITIONS[index * 2],
                    y = gridPixelHeight * FINAL_SPARKLE_POSITIONS[index * 2 + 1],
                )
        val alpha = entrance * (0.55f + wave * 0.43f)
        val color = if (index % 3 == 0) STAR_GOLD else STAR_PALE
        drawSceneSquare(
            center = center,
            side = cellSize * 0.34f * entrance,
            color = color,
            alpha = alpha,
        )
        repeat(4) { ray ->
            val rayOffset =
                when (ray) {
                    0 -> Offset(cellSize * 0.62f, 0f)
                    1 -> Offset(0f, cellSize * 0.62f)
                    2 -> Offset(-cellSize * 0.62f, 0f)
                    else -> Offset(0f, -cellSize * 0.62f)
                }
            drawSceneSquare(
                center = center + rayOffset,
                side = cellSize * 0.20f * entrance,
                color = color,
                alpha = alpha * 0.82f,
            )
        }
    }

    val stardustReveal = celebrationPhase(reveal, 0.34f, 1f)
    if (stardustReveal > 0f) {
        repeat(stardustCount) { index ->
            val cycle = (loop * 0.55f + sceneHash(index, 181)) % 1f
            val onLeft = index % 2 == 0
            val baseX =
                if (onLeft) {
                    0.03f + sceneHash(index, 211) * 0.12f
                } else {
                    0.85f + sceneHash(index, 211) * 0.12f
                }
            val x =
                baseX +
                    sin(PI * 2.0 * cycle + index).toFloat() * 0.009f
            val y = 0.74f - cycle * 0.52f
            val alpha = sin(PI * cycle).toFloat().coerceAtLeast(0f) * stardustReveal
            drawSceneSquare(
                center = gridOrigin + Offset(gridPixelWidth * x, gridPixelHeight * y),
                side = cellSize * (0.18f + sceneHash(index, 233) * 0.13f),
                color = if (index % 4 == 0) POND_MINT else STAR_GOLD,
                alpha = alpha * 0.72f,
            )
        }
    }
}

private fun DrawScope.drawSceneSquare(
    center: Offset,
    side: Float,
    color: Color,
    alpha: Float,
) {
    if (side <= 0f || alpha <= 0f) return
    drawRoundRect(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
        size = Size(side, side),
        cornerRadius = CornerRadius(side * 0.20f),
    )
}

private fun createBirthdayCelebrationScene(nightStarCount: Int): BirthdayCelebrationScene =
    BirthdayCelebrationScene(
        cake = createBirthdayCakeComposition(),
        nightBeads = createNightBeads(nightStarCount),
        giftBeads = createGiftBeads(),
    )

private fun createNightBeads(nightStarCount: Int): List<SceneBead> =
    List(nightStarCount.coerceAtLeast(STAR_PIXEL_POSITIONS.size / 2)) { index ->
        val count = nightStarCount.coerceAtLeast(STAR_PIXEL_POSITIONS.size / 2)
        // Stratified vertical bands prevent accidental rows while keeping each star's x position random.
        val band = (index * 17 + 7) % count
        val bandJitter = (starScatterHash(index * 43 + 11, 331) - 0.5f) * 0.24f
        val xFraction =
            (0.035f + starScatterHash(index * 17 + 5, 307) * 0.93f)
                .coerceIn(0.025f, 0.975f)
        val yFraction =
            (0.038f + ((band + 0.5f + bandJitter) / count.toFloat()) * 0.55f)
                .coerceIn(0.035f, 0.62f)
        SceneBead(
            xFraction = xFraction,
            yFraction = yFraction,
            color = if (index % 4 == 0) STAR_PALE else STAR_GOLD,
            delay = starScatterHash(index * 29 + 7, 47),
            sizeScale = 0.76f + starScatterHash(index * 31 + 13, 71) * 0.58f,
        )
    }

private fun createGiftBeads(): List<GiftBead> =
    buildList {
        for (row in -2..5) {
            for (column in -5..5) {
                val color =
                    when {
                        column == -5 || column == 5 || row == 5 -> GIFT_LIGHT
                        column == 0 -> RIBBON_GOLD
                        else -> GIFT_PINK
                    }
                add(GiftBead(column.toFloat(), row.toFloat(), color, isLid = false))
            }
        }
        for (row in -4..-3) {
            for (column in -6..6) {
                val color = if (column == 0) RIBBON_GOLD else GIFT_LIGHT
                add(GiftBead(column.toFloat(), row.toFloat(), color, isLid = true))
            }
        }
        val bowCoordinates =
            intArrayOf(
                -3, -6,
                -2, -7,
                -1, -6,
                0, -5,
                1, -6,
                2, -7,
                3, -6,
                -2, -5,
                2, -5,
            )
        var index = 0
        while (index < bowCoordinates.size) {
            add(
                GiftBead(
                    bowCoordinates[index].toFloat(),
                    bowCoordinates[index + 1].toFloat(),
                    RIBBON_GOLD,
                    isLid = true,
                ),
            )
            index += 2
        }
    }

private fun createBirthdayCakeComposition(): BirthdayCakeComposition {
    val project = BirthdayCelebration.createCakeProject(updatedAt = 0L)
    val beads =
        project.cells.mapIndexedNotNull { index, colorId ->
            colorId ?: return@mapIndexedNotNull null
            val color = BeadPalette.find(colorId)?.let { Color(it.argb) } ?: Color(0xFFF2EBDD)
            CelebrationBead(
                column = index % project.width,
                row = index / project.width,
                color = color,
            )
        }
    return BirthdayCakeComposition(
        width = project.width,
        height = project.height,
        beads = beads,
    )
}

private fun birthdayCakeLayout(
    canvasSize: Size,
    gridWidth: Int,
    gridHeight: Int,
): BirthdayCakeLayout {
    val cellSize =
        min(
            canvasSize.width / (gridWidth + 4f),
            canvasSize.height / (gridHeight + 8f),
        ).coerceAtLeast(1f)
    val gridPixelWidth = gridWidth * cellSize
    val gridPixelHeight = gridHeight * cellSize
    val remainingHeight = (canvasSize.height - gridPixelHeight).coerceAtLeast(0f)
    return BirthdayCakeLayout(
        cellSize = cellSize,
        origin =
            Offset(
                x = (canvasSize.width - gridPixelWidth) / 2f,
                y = remainingHeight * 0.28f,
            ),
    )
}

private fun sceneHash(
    index: Int,
    salt: Int,
): Float {
    val mixed = ((index + 1L) * 1_103L + salt * 7_919L + index.toLong() * index * 37L) % 9_973L
    return mixed.toFloat() / 9_972f
}

/** A decorrelated deterministic hash for spatial star placement. */
private fun starScatterHash(
    index: Int,
    salt: Int,
): Float {
    var value =
        (index.toLong() + 1L) * 374_761_393L +
            salt.toLong() * 668_265_263L
    value = (value xor (value ushr 29)) * 1_099_511_628_211L
    value = value xor (value ushr 32)
    return (value and 0x7FFF_FFFFL).toFloat() / 2_147_483_647f
}

private fun celebrationPhase(
    progress: Float,
    start: Float,
    end: Float,
): Float {
    if (end <= start) return if (progress >= end) 1f else 0f
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private fun lerp(
    start: Offset,
    end: Offset,
    fraction: Float,
): Offset = start + (end - start) * fraction

private fun parabolicArc(
    start: Offset,
    end: Offset,
    arcHeight: Float,
    progress: Float,
): Offset {
    val t = progress.coerceIn(0f, 1f)
    return lerp(start, end, t) - Offset(0f, 4f * arcHeight * t * (1f - t))
}

private fun parabolicTangentRotation(
    start: Offset,
    end: Offset,
    arcHeight: Float,
    progress: Float,
): Float {
    val t = progress.coerceIn(0f, 1f)
    val directionX = end.x - start.x
    val directionY = end.y - start.y - 4f * arcHeight * (1f - 2f * t)
    val directionAngle = Math.toDegrees(atan2(directionY.toDouble(), directionX.toDouble())).toFloat()
    var rotationFromLeft = directionAngle - 180f
    while (rotationFromLeft > 180f) rotationFromLeft -= 360f
    while (rotationFromLeft < -180f) rotationFromLeft += 360f
    return rotationFromLeft
}

private fun quadraticBezier(
    start: Offset,
    control: Offset,
    end: Offset,
    progress: Float,
): Offset {
    val t = progress.coerceIn(0f, 1f)
    val inverse = 1f - t
    return start * (inverse * inverse) + control * (2f * inverse * t) + end * (t * t)
}

private fun lerpSceneColor(
    start: Color,
    end: Color,
    fraction: Float,
): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t,
    )
}

private fun brightenCelebrationCakeColor(color: Color): Color {
    val luminance = color.red * 0.2126f + color.green * 0.7152f + color.blue * 0.0722f
    val lift =
        when {
            luminance < 0.45f -> 0.28f
            luminance < 0.72f -> 0.18f
            else -> 0.08f
        }
    return lerpSceneColor(color, Color(0xFFFFF4ED), lift)
}
