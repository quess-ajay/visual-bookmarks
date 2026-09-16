package com.example.screenshot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID
import kotlin.coroutines.resume

data class ScreenshotCaptureResult(
    val url: String,
    val title: String,
    val domain: String,
    val description: String?,
    val screenshotPath: String?,
    val faviconPath: String?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class ScreenshotGenerator(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun captureWebpage(rawUrl: String): ScreenshotCaptureResult {
        val normalizedUrl = normalizeUrl(rawUrl)
        val domain = extractDomain(normalizedUrl)

        if (normalizedUrl.isBlank()) {
            return ScreenshotCaptureResult(
                url = rawUrl,
                title = rawUrl,
                domain = domain,
                description = null,
                screenshotPath = null,
                faviconPath = null,
                isSuccess = false,
                errorMessage = "Invalid or empty URL"
            )
        }

        // Try WebView capture with a timeout
        val result = withTimeoutOrNull(12000) {
            captureWithWebView(normalizedUrl, domain)
        }

        return if (result != null && result.isSuccess && !result.screenshotPath.isNullOrBlank()) {
            result
        } else {
            // If timed out, renderer failed, or no path generated, build clean visual snapshot
            val fallbackPath = generateFallbackSnapshot(domain, normalizedUrl)
            ScreenshotCaptureResult(
                url = normalizedUrl,
                title = if (result?.title?.isNotBlank() == true) result.title else domain.ifBlank { normalizedUrl },
                domain = domain,
                description = result?.description,
                screenshotPath = fallbackPath,
                faviconPath = result?.faviconPath,
                isSuccess = true,
                errorMessage = result?.errorMessage
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun captureWithWebView(url: String, domain: String): ScreenshotCaptureResult =
        suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                var isResumed = false
                var webView: WebView? = null

                fun finishWithResult(result: ScreenshotCaptureResult) {
                    if (!isResumed) {
                        isResumed = true
                        try {
                            webView?.stopLoading()
                            webView?.destroy()
                        } catch (ignored: Exception) {}
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                }

                try {
                    webView = WebView(context)
                    // Disable hardware acceleration on offscreen WebView to prevent Mesa/GPU driver crashes
                    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                    val width = 1080
                    val height = 1440

                    webView.layout(0, 0, width, height)
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    webView.measure(widthSpec, heightSpec)
                    webView.layout(0, 0, width, height)

                    val settings = webView.settings
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

                    var extractedFavicon: Bitmap? = null
                    var pageTitle = ""

                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                pageTitle = title
                            }
                        }

                        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                            super.onReceivedIcon(view, icon)
                            if (icon != null) {
                                extractedFavicon = icon
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        private var hasFinished = false

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            // Safely handle renderer crashes without terminating the app process
                            finishWithResult(
                                ScreenshotCaptureResult(
                                    url = url,
                                    title = pageTitle.ifBlank { domain },
                                    domain = domain,
                                    description = null,
                                    screenshotPath = null,
                                    faviconPath = null,
                                    isSuccess = false,
                                    errorMessage = "Render process gone"
                                )
                            )
                            return true
                        }

                        override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                            super.onPageFinished(view, loadedUrl)
                            if (hasFinished) return
                            hasFinished = true

                            if (pageTitle.isBlank() && view?.title?.isNotBlank() == true) {
                                pageTitle = view.title ?: ""
                            }
                            if (extractedFavicon == null) {
                                extractedFavicon = view?.favicon
                            }

                            // Wait 1000ms for JavaScript rendering/animations to settle
                            mainHandler.postDelayed({
                                try {
                                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    view?.draw(canvas)

                                    // Save in background
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).run {
                                        val screenshotPath = saveBitmapToFile(bitmap, "screenshot_${UUID.randomUUID()}.jpg")
                                        val faviconPath = extractedFavicon?.let {
                                            saveBitmapToFile(it, "favicon_${UUID.randomUUID()}.png")
                                        }

                                        val finalTitle = if (pageTitle.isNotBlank()) pageTitle else domain.ifBlank { url }
                                        finishWithResult(
                                            ScreenshotCaptureResult(
                                                url = url,
                                                title = finalTitle,
                                                domain = domain,
                                                description = null,
                                                screenshotPath = screenshotPath,
                                                faviconPath = faviconPath,
                                                isSuccess = true
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    finishWithResult(
                                        ScreenshotCaptureResult(
                                            url = url,
                                            title = if (pageTitle.isNotBlank()) pageTitle else domain,
                                            domain = domain,
                                            description = null,
                                            screenshotPath = null,
                                            faviconPath = null,
                                            isSuccess = false,
                                            errorMessage = e.message
                                        )
                                    )
                                }
                            }, 1000)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                        }
                    }

                    webView.loadUrl(url)

                } catch (e: Exception) {
                    finishWithResult(
                        ScreenshotCaptureResult(
                            url = url,
                            title = domain,
                            domain = domain,
                            description = null,
                            screenshotPath = null,
                            faviconPath = null,
                            isSuccess = false,
                            errorMessage = e.localizedMessage
                        )
                    )
                }

                continuation.invokeOnCancellation {
                    mainHandler.post {
                        try {
                            webView?.stopLoading()
                            webView?.destroy()
                        } catch (ignored: Exception) {}
                    }
                }
            }
        }

    fun saveBitmapToFile(bitmap: Bitmap, filename: String): String {
        val screenshotsDir = File(context.filesDir, "screenshots")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        val file = File(screenshotsDir, filename)
        FileOutputStream(file).use { out ->
            if (filename.endsWith(".png")) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        }
        return file.absolutePath
    }

    suspend fun generateFallbackSnapshot(domain: String, url: String): String = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Gradient background based on domain hash
        val hash = domain.hashCode()
        val r = ((hash and 0xFF0000 shr 16) % 150 + 50).coerceIn(30, 200)
        val g = ((hash and 0x00FF00 shr 8) % 150 + 50).coerceIn(40, 210)
        val b = ((hash and 0x0000FF) % 150 + 60).coerceIn(60, 230)

        // Background canvas
        paint.color = Color.rgb(243, 244, 246)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Browser top bar
        paint.color = Color.rgb(229, 231, 235)
        canvas.drawRect(0f, 0f, width.toFloat(), 120f, paint)

        // 3 browser window control dots
        paint.color = Color.rgb(239, 68, 68) // red
        canvas.drawCircle(60f, 60f, 16f, paint)
        paint.color = Color.rgb(245, 158, 11) // yellow
        canvas.drawCircle(110f, 60f, 16f, paint)
        paint.color = Color.rgb(34, 197, 94) // green
        canvas.drawCircle(160f, 60f, 16f, paint)

        // Search address bar
        paint.color = Color.WHITE
        val addressBarRect = RectF(220f, 32f, width.toFloat() - 40f, 88f)
        canvas.drawRoundRect(addressBarRect, 28f, 28f, paint)

        // URL text in address bar
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 30f
        paint.textAlign = Paint.Align.LEFT
        val displayDomain = domain.ifBlank { url }
        canvas.drawText("🔒 $displayDomain", 250f, 72f, paint)

        // Hero card in center
        val heroRect = RectF(80f, 220f, width.toFloat() - 80f, 820f)
        paint.color = Color.rgb(r, g, b)
        canvas.drawRoundRect(heroRect, 48f, 48f, paint)

        // Large Domain initial
        paint.color = Color.WHITE
        paint.textSize = 200f
        paint.textAlign = Paint.Align.CENTER
        val initial = displayDomain.trimStart().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
        canvas.drawText(initial, width / 2f, 540f, paint)

        // Domain title
        paint.textSize = 60f
        canvas.drawText(displayDomain, width / 2f, 660f, paint)

        // Stylized wireframe body lines
        paint.color = Color.rgb(209, 213, 219)
        val mock1 = RectF(80f, 890f, width.toFloat() - 80f, 960f)
        canvas.drawRoundRect(mock1, 20f, 20f, paint)

        val mock2 = RectF(80f, 990f, width.toFloat() - 220f, 1050f)
        canvas.drawRoundRect(mock2, 20f, 20f, paint)

        val mock3 = RectF(80f, 1080f, width.toFloat() - 340f, 1130f)
        canvas.drawRoundRect(mock3, 20f, 20f, paint)

        saveBitmapToFile(bitmap, "snapshot_${UUID.randomUUID()}.jpg")
    }

    companion object {
        fun normalizeUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return ""
            return if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                "https://$trimmed"
            } else {
                trimmed
            }
        }

        fun extractDomain(urlStr: String): String {
            return try {
                val uri = URI(urlStr)
                var host = uri.host ?: ""
                if (host.startsWith("www.", ignoreCase = true)) {
                    host = host.substring(4)
                }
                if (host.isBlank()) {
                    val androidUri = Uri.parse(urlStr)
                    host = androidUri.host ?: ""
                    if (host.startsWith("www.", ignoreCase = true)) {
                        host = host.substring(4)
                    }
                }
                host.ifBlank { urlStr }
            } catch (e: Exception) {
                urlStr.removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .substringBefore("/")
            }
        }
    }
}
