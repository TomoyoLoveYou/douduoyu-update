package com.douduoyu.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 视频源尺寸（res/raw/xiangxiang.mp4 固定为 768×1152）。
 *  不用 MediaPlayer 上报的宽高：部分机型/解码器会给出交换或含旋转的值，
 *  一旦拿错，等比缩放就会把画面放大并偏出容器。 */
private const val SOURCE_VIDEO_WIDTH = 768
private const val SOURCE_VIDEO_HEIGHT = 1152

/** 视频画面比例 */
private const val VIDEO_ASPECT = SOURCE_VIDEO_WIDTH.toFloat() / SOURCE_VIDEO_HEIGHT

/** 画面之外的底色：取视频里深棕的暗部，让留白看起来是设计而不是黑边 */
private val BackdropColor = Color(0xFF17110D)

/** 角落模糊补丁占据画面宽高的比例（刚好盖住自带的「AI生成」水印） */
private const val CORNER_BLUR_WIDTH_FRACTION = 0.34f
private const val CORNER_BLUR_HEIGHT_FRACTION = 0.18f

/**
 * 「小鱼生快」寄语页。
 *
 * 整页由四层构成：
 * 1. 全屏循环视频（静音、无音轨，长按/点击都不响应媒体控件）；
 * 2. 上下渐隐的柔光，用来托住文字；
 * 3. 左上 / 右下两处压边覆盖，把画面自带的「AI 生成」水印抹平；
 * 4. 诗 —— 标题、题款、四句对句常显；最下面那行小字单击可隐藏 / 显示。
 *
 * 页面在进出时单独锁定竖屏与沉浸式状态栏，离开后恢复系统原设置。
 */
@Composable
internal fun XiangXiangScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isSecretVisible by rememberSaveable { mutableStateOf(true) }
    // 小字末尾的颜文字：进来先随机一个，之后每次重新显示小字都换一个（不与上一次重复）
    var emoticonIndex by rememberSaveable { mutableStateOf(XiangXiangGift.nextEmoticonIndex(-1)) }

    BackHandler(onBack = onBack)

    // 整页禁止横屏：进入时锁定竖屏，离开时恢复原方向
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation =
                previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 沉浸式全屏 + 播放期间不熄屏
    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 只切换最下面那行小字：诗的主体常显，小字淡入淡出（保留占位，避免整块文字跳动）
    val secretAlpha by animateFloatAsState(
        targetValue = if (isSecretVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "xiangxiangSecretAlpha",
    )
    val tapInteraction = remember { MutableInteractionSource() }
    var isVideoRendered by remember { mutableStateOf(false) }
    // 一次解码首帧，干三件事：起播前当静态海报、给两个角落各做一张模糊补丁盖水印。
    val poster by
        produceState(initialValue = VideoPoster(null, null, null)) {
            value =
                withContext(Dispatchers.IO) {
                    sampleVideoPoster(context.applicationContext, R.raw.xiangxiang)
                }
        }
    val posterAlpha by animateFloatAsState(
        targetValue = if (isVideoRendered) 0f else 1f,
        animationSpec = tween(durationMillis = 420),
        label = "xiangxiangPosterAlpha",
    )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackdropColor)
                .clickable(
                    interactionSource = tapInteraction,
                    indication = null,
                ) {
                    if (isSecretVisible) {
                        isSecretVisible = false
                    } else {
                        // 重新显示时才换颜文字：隐藏过程中不重排，淡出更干净
                        emoticonIndex = XiangXiangGift.nextEmoticonIndex(emoticonIndex)
                        isSecretVisible = true
                    }
                },
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        // 整幅显示：按 2:3 完整放在屏幕上半部，宁可下方留白也不裁画面
        val frameWidth = minOf(screenWidth, screenHeight * VIDEO_ASPECT)
        val frameHeight = frameWidth / VIDEO_ASPECT
        val frameLeft = (screenWidth - frameWidth) / 2

        Box(
            modifier =
                Modifier
                    .offset(x = frameLeft)
                    .size(width = frameWidth, height = frameHeight),
        ) {
            LoopingMutedVideoBackdrop(
                resId = R.raw.xiangxiang,
                onFirstFrame = { isVideoRendered = true },
                modifier = Modifier.fillMaxSize(),
            )

            // 首帧海报：解码器起播前先顶住画面，视频出画后淡出
            poster.frame?.let { frame ->
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = posterAlpha },
                    contentScale = ContentScale.Fit,
                )
            }

            // 水印遮挡：用画面自己的角落做真实模糊（缩到极小再放大），
            // 沿两条内边线性淡出，和底图无缝衔接，不会出现平涂色块。
            poster.topLeftBlur?.let { blur ->
                Image(
                    bitmap = blur.asImageBitmap(),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth(CORNER_BLUR_WIDTH_FRACTION)
                            .fillMaxHeight(CORNER_BLUR_HEIGHT_FRACTION),
                    contentScale = ContentScale.FillBounds,
                )
            }
            poster.bottomRightBlur?.let { blur ->
                Image(
                    bitmap = blur.asImageBitmap(),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth(CORNER_BLUR_WIDTH_FRACTION)
                            .fillMaxHeight(CORNER_BLUR_HEIGHT_FRACTION),
                    contentScale = ContentScale.FillBounds,
                )
            }

            // 画面下沿融进底色，避免一条生硬的分界线
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                1.00f to BackdropColor,
                            ),
                        ),
            )
        }

        // 文字区柔光：越往下越暗，保证诗句在任何画面上都读得清
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.40f to BackdropColor.copy(alpha = 0.82f),
                            1.00f to BackdropColor.copy(alpha = 0.98f),
                        ),
                    ),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = XiangXiangGift.POEM_TITLE,
                style =
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        letterSpacing = 7.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.82f),
                        shadow = poemShadow(),
                    ),
            )
            Spacer(Modifier.height(14.dp))
            // 题款：原来放过一根横线，现在署名
            Text(
                text = XiangXiangGift.POEM_AUTHOR,
                style =
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        letterSpacing = 5.sp,
                        color = Color.White.copy(alpha = 0.52f),
                        shadow = poemShadow(),
                    ),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = XiangXiangGift.POEM_LINES.joinToString("\n"),
                style =
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        lineHeight = 34.sp,
                        letterSpacing = 1.6.sp,
                        color = Color.White.copy(alpha = 0.96f),
                        textAlign = TextAlign.Center,
                        shadow = poemShadow(),
                    ),
            )
            Spacer(Modifier.height(24.dp))
            // 末句：小字、低对比度，单击屏幕只切换这一行；末尾颜文字每次重新显示都换一个
            Text(
                text = XiangXiangGift.poemSecret(emoticonIndex),
                modifier =
                    Modifier
                        .widthIn(max = 300.dp)
                        .graphicsLayer { alpha = secretAlpha },
                style =
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.7.sp,
                        color = Color.White.copy(alpha = 0.40f),
                        textAlign = TextAlign.Center,
                    ),
            )
        }

        // 这里原来是一行「轻点屏幕 隐藏或显示末尾小字」的提示，已按要求去掉。
        // 仍占同样高度（原提示为 10sp 单行，整行高度约 13sp，这里按 sp 换算以跟随系统字号），
        // 底部留白与改动前一致；它在 Box 内绝对定位，不参与上方 Column 的排版。
        Spacer(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp)
                    .height(with(LocalDensity.current) { 13.sp.toDp() }),
        )
    }
}

private fun poemShadow(): Shadow =
    Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(0f, 1.5f),
        blurRadius = 9f,
    )

/**
 * 视频底色。
 *
 * 用 TextureView + MediaPlayer 而不是 VideoView：
 * - VideoView 的 onMeasure 会按视频比例自己收窄，铺不满整屏；
 * - SurfaceView 的独立图层在某些机型上会盖住 Compose 覆盖层。
 * 这里手动做「等比放大 + 居中裁切」，保证任何屏幕比例下都是满屏。
 */
@Composable
private fun LoopingMutedVideoBackdrop(
    @RawRes resId: Int,
    onFirstFrame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var player by remember(resId) { mutableStateOf<MediaPlayer?>(null) }

    fun releasePlayer() {
        player?.let { active ->
            runCatching { if (active.isPlaying) active.stop() }
            runCatching { active.release() }
        }
        player = null
    }

    DisposableEffect(resId) {
        onDispose { releasePlayer() }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            object : TextureView(viewContext) {
                init {
                    isOpaque = true
                    surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                releasePlayer()
                                val created = MediaPlayer()
                                player = created
                                runCatching {
                                    viewContext.resources.openRawResourceFd(resId).use { afd ->
                                        created.setDataSource(
                                            afd.fileDescriptor,
                                            afd.startOffset,
                                            afd.length,
                                        )
                                    }
                                    created.setSurface(Surface(surface))
                                    // 静音 + 单曲循环；文件本身已去掉音轨，这里再做一层保险
                                    created.setVolume(0f, 0f)
                                    created.isLooping = true
                                    created.setOnPreparedListener { prepared ->
                                        prepared.start()
                                    }
                                    // 真正出画后再收起首帧海报，软解慢的机型也不会长时间黑屏
                                    created.setOnInfoListener { _, what, _ ->
                                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                            onFirstFrame()
                                        }
                                        false
                                    }
                                    created.prepareAsync()
                                }.onFailure { releasePlayer() }
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) = Unit

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                releasePlayer()
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                        }
                }
            }
        },
    )
}

/** 首帧解出来的东西：[frame] 起播前当海报，两张 blur 用来盖水印。 */
private data class VideoPoster(
    val frame: Bitmap?,
    val topLeftBlur: Bitmap?,
    val bottomRightBlur: Bitmap?,
)

/**
 * 解一帧视频首帧并备好三张位图：整帧缩小一半作海报，两个角落各做一张模糊补丁。
 *
 * 模糊不是盖色块，而是把这个角落**自己的画面**缩到极小再放大回来——
 * 水印被压成一团看不出字形的微光，纹理和明暗都跟底图一脉相承。
 * 放在 IO 线程做，几十毫秒即可算完。
 */
private fun sampleVideoPoster(
    context: Context,
    @RawRes resId: Int,
): VideoPoster {
    val retriever = MediaMetadataRetriever()
    return try {
        context.resources.openRawResourceFd(resId).use { afd ->
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
        val frame =
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return VideoPoster(null, null, null)
        val poster =
            Bitmap.createScaledBitmap(
                frame,
                maxOf(1, frame.width / 2),
                maxOf(1, frame.height / 2),
                true,
            )
        VideoPoster(
            frame = poster,
            topLeftBlur = buildCornerBlur(poster, fromTopLeft = true),
            bottomRightBlur = buildCornerBlur(poster, fromTopLeft = false),
        )
    } catch (error: Exception) {
        VideoPoster(null, null, null)
    } finally {
        runCatching { retriever.release() }
    }
}

/**
 * 裁下画面的一角，靠"缩到 10 像素宽再放大"做出重度模糊，再沿两条内边线性淡出。
 *
 * 淡出是必须的：否则补丁会是一条清晰的方块边界，比水印还显眼。
 */
private fun buildCornerBlur(
    frame: Bitmap,
    fromTopLeft: Boolean,
): Bitmap? {
    val cropWidth = (frame.width * CORNER_BLUR_WIDTH_FRACTION).toInt().coerceAtLeast(16)
    val cropHeight = (frame.height * CORNER_BLUR_HEIGHT_FRACTION).toInt().coerceAtLeast(16)
    val left = if (fromTopLeft) 0 else (frame.width - cropWidth).coerceAtLeast(0)
    val top = if (fromTopLeft) 0 else (frame.height - cropHeight).coerceAtLeast(0)
    return runCatching {
        val crop = Bitmap.createBitmap(frame, left, top, cropWidth, cropHeight)
        val tinyWidth = 10
        val tinyHeight = (tinyWidth * cropHeight / cropWidth).coerceAtLeast(3)
        val tiny = Bitmap.createScaledBitmap(crop, tinyWidth, tinyHeight, true)
        val blown = Bitmap.createScaledBitmap(tiny, cropWidth, cropHeight, true)
        val mutable = blown.copy(Bitmap.Config.ARGB_8888, true)
        applyCornerFade(mutable, fromTopLeft)
    }.getOrNull()
}

/** 两次 DST_IN：横向一条渐变、纵向一条渐变，叠出"角落实、内边虚"的柔和衰减。 */
private fun applyCornerFade(
    bitmap: Bitmap,
    fromTopLeft: Boolean,
): Bitmap {
    val width = bitmap.width.toFloat()
    val height = bitmap.height.toFloat()
    val canvas = AndroidCanvas(bitmap)
    val fadeStops = floatArrayOf(0f, 0.45f, 1f)
    val fadeColors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0x00FFFFFF)

    val horizontal =
        LinearGradient(
            if (fromTopLeft) 0f else width,
            0f,
            if (fromTopLeft) width else 0f,
            0f,
            fadeColors,
            fadeStops,
            Shader.TileMode.CLAMP,
        )
    canvas.drawRect(
        0f,
        0f,
        width,
        height,
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = horizontal
        },
    )

    val vertical =
        LinearGradient(
            0f,
            if (fromTopLeft) 0f else height,
            0f,
            if (fromTopLeft) height else 0f,
            fadeColors,
            fadeStops,
            Shader.TileMode.CLAMP,
        )
    canvas.drawRect(
        0f,
        0f,
        width,
        height,
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = vertical
        },
    )
    return bitmap
}

/** 从 Compose 的 LocalContext 里找到宿主 Activity（ContextWrapper 一层层往外剥）。 */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
