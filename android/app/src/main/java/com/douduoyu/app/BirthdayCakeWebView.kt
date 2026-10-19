package com.douduoyu.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONObject
import java.io.IOException

private const val BIRTHDAY_ASSET_HOST = "appassets.local"
private const val BIRTHDAY_PAGE_PATH = "birthday/index.html"
private const val BIRTHDAY_BRIDGE_NAME = "__birthday"
private const val BIRTHDAY_LOG_TAG = "BirthdayCake"

/**
 * 承载 three.js 生日蛋糕动画的 WebView。
 *
 * 资源通过 [BirthdayAssetClient] 从 `assets/birthday/` 映射到
 * `https://appassets.local/...`，这样 ES module（`import * as THREE`）不会被
 * `file://` 的跨域策略拦住，也不需要联网。
 */
internal class BirthdayCakeController {
    private var webView: WebView? = null
    private var patternJson: String? = null
    private var sideLayout: Boolean? = null
    private var pageReady = false

    fun attach(view: WebView) {
        webView = view
        pageReady = false
        Log.d(BIRTHDAY_LOG_TAG, "attach")
    }

    fun markReady() {
        pageReady = true
        Log.d(BIRTHDAY_LOG_TAG, "markReady pattern=${patternJson?.length ?: -1}")
        patternJson?.let(::pushPattern)
        sideLayout?.let(::pushLayout)
    }

    fun setPattern(json: String) {
        patternJson = json
        Log.d(BIRTHDAY_LOG_TAG, "setPattern len=${json.length} ready=$pageReady")
        if (pageReady) pushPattern(json)
    }

    fun setLayout(side: Boolean) {
        sideLayout = side
        if (pageReady) pushLayout(side)
    }

    /** 转场终点背景色（拼豆页同款），仅展示用 */
    fun setFinalBackground(hex: String) {
        evaluate("window.birthdayCake && window.birthdayCake.setFinalBackground(${JSONObject.quote(hex)})", "bg")
    }

    /** 换配色（0=草莓奶油…），只改材质颜色，不动时间轴 */
    fun setTheme(index: Int) =
        evaluate("window.birthdayCake && window.birthdayCake.setTheme($index)", "theme")

    fun restart() =
        evaluate("window.birthdayCake && window.birthdayCake.restart()")

    /** animateConfetti=false 时定格（减少动态效果用），否则跳过后彩带继续飘 */
    fun skip(animateConfetti: Boolean = true) =
        evaluate("window.birthdayCake && window.birthdayCake.skip($animateConfetti)")

    fun pause() {
        webView?.onPause()
        evaluate("window.birthdayCake && window.birthdayCake.pause()")
    }

    fun resume() {
        webView?.onResume()
        evaluate("window.birthdayCake && window.birthdayCake.resume()")
    }

    fun destroy() {
        val view = webView ?: return
        webView = null
        pageReady = false
        runCatching { (view.parent as? ViewGroup)?.removeView(view) }
        runCatching { view.removeJavascriptInterface(BIRTHDAY_BRIDGE_NAME) }
        runCatching { view.stopLoading() }
        runCatching { view.destroy() }
    }

    private fun pushPattern(json: String) {
        val script =
            "(function(){if(!window.birthdayCake)return 'no-api';" +
                "window.birthdayCake.setPattern(${JSONObject.quote(json)});return 'ok';})()"
        evaluate(script, "setPattern")
    }

    private fun pushLayout(side: Boolean) {
        evaluate("window.birthdayCake && window.birthdayCake.setLayout({ side: $side })", "layout")
    }

    private fun evaluate(
        script: String,
        label: String = "eval",
        attempt: Int = 0,
    ) {
        val view = webView ?: run {
            Log.w(BIRTHDAY_LOG_TAG, "$label skipped: no webView")
            return
        }
        view.post {
            runCatching {
                view.evaluateJavascript(script) { result ->
                    Log.d(BIRTHDAY_LOG_TAG, "$label result=$result len=${script.length} attempt=$attempt")
                    // JS 侧 API 还没挂载时自动重试（页面刚就绪的竞态）
                    if (result != null && result.contains("no-api") && attempt < 24) {
                        view.postDelayed({ evaluate(script, label, attempt + 1) }, 300L)
                    }
                }
            }.onFailure { Log.e(BIRTHDAY_LOG_TAG, "$label failed", it) }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun BirthdayCakeWebView(
    controller: BirthdayCakeController,
    quality: BirthdayAnimationQuality,
    initialTheme: Int = 0,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onPixelReady: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestReady by rememberUpdatedState(onReady)
    val latestProgress by rememberUpdatedState(onProgress)
    val latestComplete by rememberUpdatedState(onComplete)
    val latestPixelReady by rememberUpdatedState(onPixelReady)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isLongClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setOnLongClickListener { true }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    textZoom = 100
                }

                webViewClient = BirthdayAssetClient()
                addJavascriptInterface(
                    BirthdayJsBridge(
                        onProgress = { latestProgress(it) },
                        onComplete = { latestComplete() },
                        onReady = {
                            controller.markReady()
                            latestReady()
                        },
                        pixelReady = { latestPixelReady(it) },
                    ),
                    BIRTHDAY_BRIDGE_NAME,
                )

                // 首帧配色直接从 URL 带入，页面加载时就是正确颜色，不会先闪一下草莓色
                loadUrl(birthdayPageUrl(quality, initialTheme))
                controller.attach(this)
            }
        },
    )

    DisposableEffect(lifecycleOwner, controller) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> controller.resume()
                    Lifecycle.Event.ON_PAUSE -> controller.pause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.destroy()
        }
    }
}

private class BirthdayJsBridge(
    private val onProgress: (Float) -> Unit,
    private val onComplete: () -> Unit,
    private val onReady: () -> Unit,
    private val pixelReady: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onProgress(value: Double) {
        val clamped = value.toFloat().coerceIn(0f, 1f)
        mainHandler.post { onProgress(clamped) }
    }

    @JavascriptInterface
    fun onComplete() {
        mainHandler.post { onComplete() }
    }

    @JavascriptInterface
    fun onReady() {
        mainHandler.post { onReady() }
    }

    @JavascriptInterface
    fun onPixelReady(dataUrl: String) {
        Log.d(BIRTHDAY_LOG_TAG, "onPixelReady len=${dataUrl.length}")
        mainHandler.post { pixelReady(dataUrl) }
    }
}

/**
 * 把 `https://appassets.local/birthday/...` 映射到 `assets/birthday/...`。
 * 这样页面拥有正常的 https 源，ES module 可以正常 import。
 */
private class BirthdayAssetClient : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url
        if (url.host != BIRTHDAY_ASSET_HOST) return null
        val path = url.path.orEmpty().trimStart('/')
        if (path.isEmpty()) return null
        // 只允许访问生日动画目录，避免越权读取其它 assets
        if (!path.startsWith("birthday/")) {
            return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), null)
        }
        return try {
            val stream = view.context.assets.open(path)
            WebResourceResponse(
                mimeTypeFor(path),
                "UTF-8",
                200,
                "OK",
                mapOf("Cache-Control" to "no-cache"),
                stream,
            )
        } catch (error: IOException) {
            WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), null)
        }
    }

    private fun mimeTypeFor(path: String): String =
        when {
            path.endsWith(".html", ignoreCase = true) -> "text/html"
            path.endsWith(".js", ignoreCase = true) -> "application/javascript"
            path.endsWith(".mjs", ignoreCase = true) -> "application/javascript"
            path.endsWith(".json", ignoreCase = true) -> "application/json"
            path.endsWith(".css", ignoreCase = true) -> "text/css"
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
            path.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "application/octet-stream"
        }
}

private fun birthdayPageUrl(
    quality: BirthdayAnimationQuality,
    theme: Int,
): String {
    val name =
        when (quality) {
            BirthdayAnimationQuality.Low -> "low"
            BirthdayAnimationQuality.Standard -> "standard"
            BirthdayAnimationQuality.Refined -> "refined"
        }
    return "https://$BIRTHDAY_ASSET_HOST/$BIRTHDAY_PAGE_PATH?quality=$name&theme=$theme"
}
