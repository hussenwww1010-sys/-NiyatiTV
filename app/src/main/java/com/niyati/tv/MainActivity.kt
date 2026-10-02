package com.niyati.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.PlayerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Date
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class MainActivity : Activity() {

    // =========================
    // Firebase
    // =========================

    private val firebaseUrl =
        "https://niyati-tv-default-rtdb.europe-west1.firebasedatabase.app"

    private lateinit var database: FirebaseDatabase
    private lateinit var rootRef: DatabaseReference
    private var dataListener: ValueEventListener? = null

    // =========================
    // Player
    // =========================

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    // =========================
    // Web player (YouTube / Facebook)
    // =========================

    private lateinit var webView: WebView

    // true while the current channel is played inside the WebView
    private var playingWeb = false

    private val desktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

    // =========================
    // Views
    // =========================

    private lateinit var root: FrameLayout
    private lateinit var normalScreen: LinearLayout
    private lateinit var packagesList: LinearLayout
    private lateinit var channelsList: LinearLayout
    private lateinit var channelScroll: ScrollView
    private lateinit var packagesHeader: SectionHeader
    private lateinit var channelsHeader: SectionHeader
    private lateinit var playerFrame: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private lateinit var statusOverlay: LinearLayout
    private lateinit var statusIcon: FrameLayout
    private lateinit var statusPlay: PlayIcon
    private lateinit var statusBang: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView

    private lateinit var nowTitle: TextView
    private lateinit var nowSub: TextView
    private lateinit var livePill: LinearLayout
    // receiver-style channel banner (fullscreen)
    private lateinit var zapBanner: LinearLayout
    private lateinit var bannerPrev: BannerCard
    private lateinit var bannerCur: BannerCard
    private lateinit var bannerNext: BannerCard

    // quality badge on the video
    private lateinit var qualityBadge: TextView
    private var qualityText = ""
    private var qualityColor = Color.WHITE

    // header: weather + date
    private lateinit var weatherIcon: TextView
    private lateinit var weatherTemp: TextView
    private lateinit var weatherCityView: TextView
    private lateinit var dateView: TextView
    private var weatherLat = 33.3152
    private var weatherLon = 44.3661
    private var weatherCity = "بغداد"

    // =========================
    // Data
    // =========================

    data class Channel(
        val name: String,
        val group: String,
        val url: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int,
        val userAgent: String = "",
        val referer: String = ""
    )

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    data class Accent(
        val start: Int,
        val end: Int
    )

    private class PackageRow(
        val item: PackageItem,
        val card: TvCard
    )

    private class ChannelRow(
        val channel: Channel,
        val card: TvCard,
        val setPlaying: (Boolean) -> Unit
    )

    private var packages: List<PackageItem> = emptyList()
    private var channels: List<Channel> = emptyList()
    private var visibleChannels: List<Channel> = emptyList()

    private val packageRows = mutableListOf<PackageRow>()
    private val channelRows = mutableListOf<ChannelRow>()

    private var selectedPackage = ""
    private var pendingPackageId: String? = null
    private var playingChannel: Channel? = null
    private var playingList: List<Channel> = emptyList()
    private var lastFocusedChannel: Channel? = null

    private var dataReady = false

    // ---- favorites / custom order ----
    private val FAV_ID = "__favorites__"
    private val gold = Color.rgb(250, 204, 21)
    private val favorites = mutableSetOf<String>()
    private var basePackages: List<PackageItem> = emptyList()
    private var autoPlayDone = false

    private val prefs by lazy {
        getSharedPreferences("niyati_prefs", MODE_PRIVATE)
    }

    private class MoveState(val isPackage: Boolean, val id: String)

    private var moveState: MoveState? = null

    // ---- long-press menu ----
    private lateinit var menuOverlay: FrameLayout
    private lateinit var menuTitle: TextView
    private lateinit var menuList: LinearLayout
    private var menuReturnFocus: View? = null

    // ---- splash ----
    private lateinit var splash: FrameLayout
    private var splashVisible = true
    private var splashStart = 0L
    private var isFullscreen = false
    private var resumeOnStart = false

    private var retryCount = 0
    private var backPressedAt = 0L

    // prevents retry after the final error is shown
    private var finalPlaybackError = false

    // =========================
    // Colors
    // =========================

    private val bgTop = Color.rgb(6, 9, 24)
    private val bgMid = Color.rgb(17, 10, 44)
    private val bgBottom = Color.rgb(4, 20, 36)

    private val cyan = Color.rgb(0, 229, 255)
    private val violet = Color.rgb(168, 85, 247)
    private val white = Color.WHITE
    private val dimWhite = Color.argb(235, 255, 255, 255)
    private val gray = Color.rgb(148, 163, 184)

    private val palette = listOf(
        Accent(Color.rgb(0, 229, 255), Color.rgb(41, 121, 255)),
        Accent(Color.rgb(192, 132, 252), Color.rgb(236, 72, 153)),
        Accent(Color.rgb(255, 183, 77), Color.rgb(244, 63, 94)),
        Accent(Color.rgb(52, 211, 153), Color.rgb(6, 182, 212)),
        Accent(Color.rgb(250, 204, 21), Color.rgb(249, 115, 22)),
        Accent(Color.rgb(129, 140, 248), Color.rgb(56, 189, 248))
    )

    private fun accentFor(index: Int): Accent =
        palette[((index % palette.size) + palette.size) % palette.size]

    // =========================
    // Helpers
    // =========================

    private val mainHandler = Handler(Looper.getMainLooper())

    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(3)

    private val imageCache =
        object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }

    private val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun lp(w: Int, h: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h, weight)

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun mix(a: Int, b: Int, t: Float): Int =
        Color.rgb(
            (Color.red(a) * t + Color.red(b) * (1 - t)).toInt(),
            (Color.green(a) * t + Color.green(b) * (1 - t)).toInt(),
            (Color.blue(a) * t + Color.blue(b) * (1 - t)).toInt()
        )

    private fun label(
        text: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.RIGHT,
        lines: Int = 1
    ): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = sizeSp
        tv.setTextColor(color)
        tv.gravity = gravity
        tv.maxLines = lines
        tv.ellipsize = TextUtils.TruncateAt.END
        tv.textDirection = View.TEXT_DIRECTION_ANY_RTL
        if (bold) {
            tv.typeface = Typeface.DEFAULT_BOLD
        }
        return tv
    }

    // shrink the text automatically until the whole name is visible
    private fun setupFit(tv: TextView, maxSp: Int, minSp: Int) {
        if (Build.VERSION.SDK_INT >= 26) {
            tv.setAutoSizeTextTypeUniformWithConfiguration(
                minSp,
                maxSp,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
    }

    private fun fitName(tv: TextView, text: String, maxSp: Int, minSp: Int) {
        tv.text = text
        if (Build.VERSION.SDK_INT < 26) {
            val size =
                if (text.length <= 16) {
                    maxSp.toFloat()
                } else {
                    (maxSp * 16f / text.length).coerceAtLeast(minSp.toFloat())
                }
            tv.textSize = size
        }
    }

    // =========================
    // onCreate
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window.statusBarColor = bgTop
        window.navigationBarColor = bgBottom

        initPlayer()

        initWebView()

        database = FirebaseDatabase.getInstance(firebaseUrl)
        rootRef = database.reference

        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())

        buildInterface()

        splashStart = SystemClock.elapsedRealtime()

        mainHandler.postDelayed(splashRunnable, 1800)

        startFirebase()

        mainHandler.post(weatherRunnable)

        mainHandler.post(dateRunnable)
    }

    // =========================
    // Player setup
    // =========================

    private enum class StreamKind(
        val label: String,
        val mime: String?
    ) {
        AUTO("\u062a\u0644\u0642\u0627\u0626\u064a", null),
        HLS("HLS / M3U8", MimeTypes.APPLICATION_M3U8),
        DASH("DASH / MPD", MimeTypes.APPLICATION_MPD),
        SS("Smooth Streaming", MimeTypes.APPLICATION_SS),
        TS("MPEG-TS", MimeTypes.VIDEO_MP2T),
        MP4("MP4", MimeTypes.VIDEO_MP4),
        RTSP("RTSP", MimeTypes.APPLICATION_RTSP)
    }

    private data class Source(
        val url: String,
        val headers: Map<String, String>
    )

    private lateinit var httpFactory: DefaultHttpDataSource.Factory

    private var currentSource: Source? = null
    private var attempts: List<StreamKind> = listOf(StreamKind.AUTO)
    private var attemptIndex = 0
    private var firstError: PlaybackException? = null
    private var liveWindowRetries = 0
    private var playbackStarted = false

    private val defaultUserAgent =
        "Mozilla/5.0 (Linux; Android 13; TV) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

    // errors meaning the guessed format is wrong (we try another format)
    private val formatErrors = setOf(
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED
    )

    private val retryRunnable = Runnable {
        if (finalPlaybackError || playingChannel == null || playingWeb) {
            return@Runnable
        }
        try {
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            finalPlaybackError = true
            showDetailedPlaybackError(null, e)
        }
    }

    private fun initPlayer() {

        // HTTP / HTTPS + redirects + per-channel headers
        httpFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent(defaultUserAgent)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

        // DefaultDataSource = http + https + rtmp + udp + file + content
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        // helps live TS streams (avoids black screen with audio)
        val extractorsFactory =
            DefaultExtractorsFactory()
                .setTsExtractorFlags(
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                )

        val mediaSourceFactory =
            DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        // fall back to another decoder if the primary one fails
        val renderersFactory =
            DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                )

        // small start buffers = channels open faster
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(3000, 30000, 700, 2000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

        player =
            ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()

        player.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {

                    // the WebView is in charge right now, ignore ExoPlayer events
                    if (playingWeb) {
                        return
                    }

                    when (playbackState) {

                        Player.STATE_BUFFERING -> {
                            // full-screen status only at the start of playback,
                            // after that PlayerView shows its own buffering spinner
                            if (
                                playingChannel != null &&
                                !finalPlaybackError &&
                                !playbackStarted
                            ) {
                                showStatus(
                                    "\u062c\u0627\u0631\u064a \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0628\u062b\u2026",
                                    playingChannel?.name ?: "",
                                    false
                                )
                            }
                        }

                        Player.STATE_READY -> {
                            retryCount = 0
                            liveWindowRetries = 0
                            finalPlaybackError = false
                            playbackStarted = true

                            hideStatus()

                            livePill.visibility =
                                if (playingChannel?.let { isVodChannel(it) } == true) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }
                        }

                        Player.STATE_ENDED -> {
                            if (playingChannel != null && !finalPlaybackError) {
                                val vod = playingChannel?.let { isVodChannel(it) } == true

                                showStatus(
                                    if (vod) "انتهى الفيديو" else "انتهى البث",
                                    if (vod) "اضغط على القناة لإعادة التشغيل" else "تم إنهاء مصدر البث",
                                    true
                                )
                            }
                        }

                        else -> {
                        }
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (!playingWeb) {
                        updateQuality(videoSize.height)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (playingWeb) {
                        return
                    }
                    handlePlayerError(error)
                }
            }
        )
    }

    // =========================
    // Web player (YouTube / Facebook)
    // =========================

    private val hideWebStatusRunnable = Runnable {
        if (playingWeb && !finalPlaybackError) {
            hideStatus()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {

        webView = WebView(this)

        webView.setBackgroundColor(Color.BLACK)

        // becomes focusable only in fullscreen
        webView.isFocusable = false

        webView.visibility = View.GONE

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            userAgentString = desktopUserAgent
        }

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient =
            object : WebViewClient() {

                // block navigation away from the embedded player
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = request.isForMainFrame

                override fun onPageFinished(view: WebView?, url: String?) {

                    if (
                        playingWeb &&
                        url != null &&
                        !url.startsWith("about:")
                    ) {
                        mainHandler.removeCallbacks(hideWebStatusRunnable)
                        mainHandler.postDelayed(hideWebStatusRunnable, 1500)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {

                    if (playingWeb && request?.isForMainFrame == true) {

                        finalPlaybackError = true

                        livePill.visibility = View.GONE

                        showStatus(
                            "\u062a\u0639\u0630\u0631 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629",
                            "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u062a\u0635\u0627\u0644 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a \u0623\u0648 \u0645\u0646 \u0627\u0644\u0631\u0627\u0628\u0637",
                            true
                        )
                    }
                }
            }
    }

    private fun hostOf(url: String): String? =
        try {
            Uri.parse(url.trim()).host?.lowercase(Locale.US)
        } catch (_: Exception) {
            null
        }

    private fun isYoutubeHost(host: String?): Boolean =
        host != null &&
                (host == "youtu.be" ||
                        host == "youtube.com" ||
                        host.endsWith(".youtube.com") ||
                        host == "youtube-nocookie.com" ||
                        host.endsWith(".youtube-nocookie.com"))

    private fun isFacebookHost(host: String?): Boolean =
        host != null &&
                (host == "fb.watch" ||
                        host == "facebook.com" ||
                        host.endsWith(".facebook.com") ||
                        host == "fb.com" ||
                        host.endsWith(".fb.com"))

    // true when the link is a YouTube / Facebook page (not a direct stream)
    private fun isWebSource(url: String): Boolean {

        val lowerPath =
            url.trim()
                .lowercase(Locale.US)
                .substringBefore("#")
                .substringBefore("?")

        if (
            lowerPath.endsWith(".m3u8") ||
            lowerPath.endsWith(".mpd") ||
            lowerPath.endsWith(".ts") ||
            lowerPath.endsWith(".mp4")
        ) {
            return false
        }

        val host = hostOf(url)

        return isYoutubeHost(host) || isFacebookHost(host)
    }

    private fun youtubeEmbedUrl(url: String): String? {

        val uri = Uri.parse(url.trim())

        val host = uri.host?.lowercase(Locale.US)

        val segs = uri.pathSegments

        val params = "autoplay=1&playsinline=1&rel=0&modestbranding=1"

        // channel live: youtube.com/channel/UCxxxx/live
        if (segs.size >= 2 && segs[0] == "channel") {
            return "https://www.youtube.com/embed/live_stream?channel=${segs[1]}&$params"
        }

        var id: String? = null

        if (host == "youtu.be") {

            id = segs.firstOrNull()

        } else {

            val v = uri.getQueryParameter("v")

            if (!v.isNullOrBlank()) {
                id = v
            } else if (
                segs.size >= 2 &&
                segs[0] in setOf("embed", "live", "shorts", "v")
            ) {
                id = segs[1]
            }
        }

        if (!id.isNullOrBlank()) {
            return "https://www.youtube.com/embed/$id?$params"
        }

        val list = uri.getQueryParameter("list")

        if (!list.isNullOrBlank()) {
            return "https://www.youtube.com/embed/videoseries?list=$list&$params"
        }

        return null
    }

    private fun facebookEmbedUrl(url: String): String =
        "https://www.facebook.com/plugins/video.php" +
                "?href=${Uri.encode(url.trim())}" +
                "&show_text=false&autoplay=true&allowfullscreen=true&width=1280"

    private fun buildEmbedHtml(embedUrl: String): String {

        val safe = embedUrl.replace("&", "&amp;")

        return "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<style>" +
                "html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden}" +
                "iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}" +
                "</style></head><body>" +
                "<iframe src=\"$safe\" " +
                "allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture\" " +
                "allowfullscreen></iframe>" +
                "</body></html>"
    }

    private fun playWeb(channel: Channel, rawUrl: String) {

        val host = hostOf(rawUrl)

        val isYoutube = isYoutubeHost(host)

        val embedUrl =
            if (isYoutube) youtubeEmbedUrl(rawUrl) else facebookEmbedUrl(rawUrl)

        val baseUrl =
            if (isYoutube) "https://www.youtube.com" else "https://www.facebook.com"

        val sourceName = if (isYoutube) "YouTube" else "Facebook"

        // stop ExoPlayer, the WebView takes over
        try {
            player.stop()
            player.clearMediaItems()
        } catch (_: Exception) {
        }

        playingWeb = true

        if (embedUrl == null) {

            finalPlaybackError = true

            livePill.visibility = View.GONE

            webView.visibility = View.GONE

            showStatus(
                "\u0631\u0627\u0628\u0637 \u063a\u064a\u0631 \u0645\u062f\u0639\u0648\u0645",
                "\u062a\u0639\u0630\u0651\u0631 \u0627\u0633\u062a\u062e\u0631\u0627\u062c \u0645\u0639\u0631\u0651\u0641 \u0627\u0644\u0641\u064a\u062f\u064a\u0648 \u0645\u0646 \u0631\u0627\u0628\u0637 $sourceName",
                true
            )

            return
        }

        showStatus(
            "\u062c\u0627\u0631\u064a \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629\u2026",
            "${channel.name}\n\u0627\u0644\u0645\u0635\u062f\u0631: $sourceName",
            false
        )

        webView.visibility = View.VISIBLE

        webView.onResume()

        webView.loadDataWithBaseURL(
            baseUrl,
            buildEmbedHtml(embedUrl),
            "text/html",
            "utf-8",
            null
        )

        livePill.visibility = View.VISIBLE
    }

    private fun stopWeb() {

        mainHandler.removeCallbacks(hideWebStatusRunnable)

        if (playingWeb) {
            playingWeb = false
            webView.loadUrl("about:blank")
        }

        webView.visibility = View.GONE
    }

    // =========================
    // Source parsing / detection
    // =========================

    /*
     * Supports the common Kodi format used in IPTV lists:
     * http://server/live/1.m3u8|User-Agent=VLC&Referer=http://site.com/
     */
    private fun parseSource(raw: String): Source {

        val trimmed = raw.trim()

        val idx = trimmed.indexOf('|')

        if (idx < 0) {
            return Source(trimmed, emptyMap())
        }

        val url = trimmed.substring(0, idx).trim()

        val map = linkedMapOf<String, String>()

        trimmed
            .substring(idx + 1)
            .split('&')
            .forEach { part ->
                val eq = part.indexOf('=')
                if (eq > 0) {
                    val k = Uri.decode(part.substring(0, eq)).trim()
                    val v = Uri.decode(part.substring(eq + 1)).trim()
                    if (k.isNotEmpty()) {
                        map[k] = v
                    }
                }
            }

        return Source(url, map)
    }

    private fun detectKind(rawUrl: String): StreamKind {

        val lower =
            rawUrl
                .trim()
                .lowercase(Locale.US)
                .substringBefore("#")

        val path = lower.substringBefore("?")

        return when {

            lower.startsWith("rtsp://") || lower.startsWith("rtsps://") ->
                StreamKind.RTSP

            path.endsWith(".m3u8") || path.contains(".m3u8/") ->
                StreamKind.HLS

            path.endsWith(".mpd") ->
                StreamKind.DASH

            path.endsWith(".ism") ||
                    path.endsWith(".isml") ||
                    path.contains(".ism/manifest") ||
                    path.contains(".isml/manifest") ->
                StreamKind.SS

            path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".mov") ->
                StreamKind.MP4

            path.endsWith(".ts") ->
                StreamKind.TS

            // extension inside the query, e.g. play.php?file=abc.m3u8
            lower.contains(".m3u8") ->
                StreamKind.HLS

            lower.contains(".mpd") ->
                StreamKind.DASH

            lower.contains(".mp4") ->
                StreamKind.MP4

            else ->
                StreamKind.AUTO
        }
    }

    /*
     * Attempt order:
     * - URL without extension (e.g. Xtream): auto -> HLS -> TS -> DASH
     * - URL with a clear extension: that format -> auto
     */
    private fun attemptsFor(url: String): List<StreamKind> =
        when (val kind = detectKind(url)) {

            StreamKind.AUTO ->
                listOf(
                    StreamKind.AUTO,
                    StreamKind.HLS,
                    StreamKind.TS,
                    StreamKind.DASH
                )

            StreamKind.RTSP,
            StreamKind.SS ->
                listOf(kind)

            else ->
                listOf(kind, StreamKind.AUTO)
        }

    private fun createMediaItem(url: String, kind: StreamKind): MediaItem {

        val builder = MediaItem.Builder().setUri(Uri.parse(url))

        kind.mime?.let {
            builder.setMimeType(it)
        }

        return builder.build()
    }

    private fun applyRequestHeaders(channel: Channel, source: Source) {

        val headers =
            linkedMapOf(
                "Accept" to "*/*",
                "Accept-Language" to "ar-IQ,ar;q=0.9,en;q=0.8"
            )

        var userAgent =
            channel.userAgent.ifBlank {
                defaultUserAgent
            }

        if (channel.referer.isNotBlank()) {

            headers["Referer"] = channel.referer

            val ref = Uri.parse(channel.referer)

            if (ref.scheme != null && ref.authority != null) {
                headers["Origin"] = "${ref.scheme}://${ref.authority}"
            }
        }

        // headers inside the URL (after |) override the others
        source.headers.forEach { (k, v) ->
            if (k.equals("user-agent", ignoreCase = true)) {
                userAgent = v
            } else {
                headers[k] = v
            }
        }

        httpFactory.setUserAgent(userAgent)
        httpFactory.setDefaultRequestProperties(headers)
    }

    private fun startCurrentAttempt(channel: Channel) {

        val source = currentSource ?: return

        val kind =
            attempts.getOrElse(attemptIndex) {
                StreamKind.AUTO
            }

        try {

            if (attemptIndex > 0) {
                showStatus(
                    "\u062a\u062c\u0631\u0628\u0629 \u0635\u064a\u063a\u0629 ${kind.label}\u2026",
                    channel.name,
                    false
                )
            }

            applyRequestHeaders(channel, source)

            player.setMediaItem(createMediaItem(source.url, kind))

            player.prepare()

            player.playWhenReady = true

        } catch (e: Exception) {

            finalPlaybackError = true

            livePill.visibility = View.GONE

            showDetailedPlaybackError(null, e)
        }
    }

    // =========================
    // Playback error handling
    // =========================

    private fun handlePlayerError(error: PlaybackException) {

        val channel = playingChannel

        val detailedMessage = buildPlaybackErrorMessage(error)

        android.util.Log.e(
            "NIYATI_PLAYER",
            "Channel=${channel?.name} " +
                    "URL=${channel?.url} " +
                    "Code=${error.errorCodeName} " +
                    "Attempt=${attempts.getOrNull(attemptIndex)?.label}\n" +
                    detailedMessage,
            error
        )

        if (channel == null) {
            return
        }

        // 1) live window passed: seek to the newest point instead of failing
        if (
            error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW &&
            liveWindowRetries < 3
        ) {
            liveWindowRetries++
            player.seekToDefaultPosition()
            player.prepare()
            return
        }

        // 2) wrong format: try the next one (only before playback started)
        if (
            error.errorCode in formatErrors &&
            !playbackStarted &&
            attemptIndex + 1 < attempts.size
        ) {
            if (firstError == null) {
                firstError = error
            }
            attemptIndex++
            startCurrentAttempt(channel)
            return
        }

        // 3) network/server errors: retry
        if (retryCount < 3 && !finalPlaybackError) {

            retryCount++

            showStatus(
                "\u0625\u0639\u0627\u062f\u0629 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629 $retryCount/3",
                detailedMessage,
                false
            )

            mainHandler.removeCallbacks(retryRunnable)

            mainHandler.postDelayed(retryRunnable, 1500L * retryCount)

            return
        }

        finalPlaybackError = true

        livePill.visibility = View.GONE

        val shown =
            if (error.errorCode in formatErrors) {
                firstError ?: error
            } else {
                error
            }

        val tried =
            if (attempts.size > 1) {
                "\n\u0627\u0644\u0635\u064a\u063a \u0627\u0644\u0645\u062c\u0631\u0651\u0628\u0629: " +
                        attempts
                            .take(attemptIndex + 1)
                            .joinToString(" \u060c ") { it.label }
            } else {
                ""
            }

        showStatus(
            "\u062e\u0637\u0623 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0628\u062b",
            buildPlaybackErrorMessage(shown) + tried,
            true
        )
    }

    private fun buildPlaybackErrorMessage(error: PlaybackException): String {

        val lines = mutableListOf<String>()

        lines.add("\u0646\u0648\u0639 \u0627\u0644\u062e\u0637\u0623: ${error.errorCodeName}")

        val httpException = findHttpException(error)

        if (httpException != null) {

            val code = httpException.responseCode

            val responseMessage = httpException.responseMessage

            lines.add(
                "HTTP: $code" +
                        (if (!responseMessage.isNullOrBlank()) " $responseMessage" else "")
            )

            when (code) {
                401 -> lines.add("\u0627\u0644\u062e\u0627\u062f\u0645 \u064a\u0637\u0644\u0628 \u0645\u0635\u0627\u062f\u0642\u0629 \u0623\u0648 \u0635\u0644\u0627\u062d\u064a\u0629.")
                403 -> lines.add("\u0627\u0644\u062e\u0627\u062f\u0645 \u0631\u0641\u0636 \u0627\u0644\u0637\u0644\u0628 403 (\u062c\u0631\u0628 Referer \u0623\u0648 User-Agent).")
                404 -> lines.add("\u0627\u0644\u0631\u0627\u0628\u0637 \u063a\u064a\u0631 \u0645\u0648\u062c\u0648\u062f 404.")
                429 -> lines.add("\u0627\u0644\u062e\u0627\u062f\u0645 \u0631\u0641\u0636 \u0627\u0644\u0637\u0644\u0628\u0627\u062a \u0645\u0624\u0642\u062a\u0627\u064b 429.")
                in 500..599 -> lines.add("\u0645\u0634\u0643\u0644\u0629 \u0645\u0646 \u062e\u0627\u062f\u0645 \u0627\u0644\u0628\u062b.")
            }
        }

        val dataSourceException = findDataSourceException(error)

        if (dataSourceException != null) {
            lines.add("DataSource: ${dataSourceException.reason}")
        }

        val cause = error.cause

        if (cause != null) {

            val simpleName = cause.javaClass.simpleName

            val message = cause.message

            if (!message.isNullOrBlank()) {
                lines.add("$simpleName: " + shortenError(message))
            } else {
                lines.add("\u0627\u0644\u0633\u0628\u0628: $simpleName")
            }
        }

        if (lines.size == 1) {
            lines.add("\u0627\u0644\u0633\u0628\u0628: ${error.message ?: "\u063a\u064a\u0631 \u0645\u0639\u0631\u0648\u0641"}")
        }

        return lines.joinToString("\n")
    }

    private fun findHttpException(
        error: PlaybackException
    ): HttpDataSource.InvalidResponseCodeException? {

        var current: Throwable? = error

        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException) {
                return current
            }
            current = current.cause
        }

        return null
    }

    private fun findDataSourceException(
        error: PlaybackException
    ): DataSourceException? {

        var current: Throwable? = error

        while (current != null) {
            if (current is DataSourceException) {
                return current
            }
            current = current.cause
        }

        return null
    }

    private fun shortenError(text: String): String {

        val cleaned = text.replace("\n", " ").trim()

        return if (cleaned.length > 180) {
            cleaned.take(177) + "..."
        } else {
            cleaned
        }
    }

    // =========================
    // Build interface
    // =========================

    private fun buildInterface() {

        buildQualityBadge()

        root = FrameLayout(this)

        root.layoutDirection = View.LAYOUT_DIRECTION_LTR

        root.addView(
            AmbientBackground(),
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        normalScreen = LinearLayout(this)

        normalScreen.orientation = LinearLayout.VERTICAL

        root.addView(
            normalScreen,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        normalScreen.addView(buildHeader(), lp(matchParent, dp(66)))

        val contentRow = LinearLayout(this)

        contentRow.orientation = LinearLayout.HORIZONTAL

        contentRow.setPadding(dp(18), dp(4), dp(18), dp(16))

        normalScreen.addView(contentRow, lp(matchParent, 0, 1f))

        // ---- Packages ----

        val packagesPanel = createPanel()

        packagesHeader = SectionHeader("\u0627\u0644\u0628\u0627\u0642\u0627\u062a", "PACKAGES", accentFor(0))

        packagesPanel.addView(packagesHeader, lp(matchParent, dp(64)))

        val packageScroll = createScroll()

        packagesList = createList()

        packageScroll.addView(packagesList)

        packagesPanel.addView(packageScroll, lp(matchParent, 0, 1f))

        contentRow.addView(packagesPanel, panelParams(0.23f))

        // ---- Channels ----

        val channelsPanel = createPanel()

        channelsHeader = SectionHeader("\u0627\u0644\u0642\u0646\u0648\u0627\u062a", "CHANNELS", accentFor(1))

        channelsPanel.addView(channelsHeader, lp(matchParent, dp(64)))

        channelScroll = createScroll()

        channelsList = createList()

        channelScroll.addView(channelsList)

        channelsPanel.addView(channelScroll, lp(matchParent, 0, 1f))

        contentRow.addView(channelsPanel, panelParams(0.30f))

        // ---- Player ----

        val playerPanel = createPanel()

        playerPanel.addView(
            SectionHeader("\u0627\u0644\u0645\u0634\u063a\u0644", "PLAYER", accentFor(2), false),
            lp(matchParent, dp(64))
        )

        playerFrame = FrameLayout(this)

        playerFrame.setPadding(dp(4), dp(4), dp(4), dp(4))

        playerFrame.isFocusable = true

        playerFrame.isFocusableInTouchMode = true

        playerFrame.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        applyPlayerFrameBackground(false)

        playerFrame.setOnFocusChangeListener { _, hasFocus ->
            applyPlayerFrameBackground(hasFocus)
        }

        playerFrame.setOnClickListener {
            enterFullscreen()
        }

        playerPanel.addView(
            playerFrame,
            lp(matchParent, 0, 1f).apply {
                setMargins(dp(6), dp(4), dp(6), 0)
            }
        )

        playerPanel.addView(
            buildInfoBar(),
            lp(matchParent, dp(60)).apply {
                setMargins(dp(6), dp(8), dp(6), 0)
            }
        )

        val hint = label(Txt.HINT, 11f, gray, false, Gravity.CENTER)

        playerPanel.addView(hint, lp(matchParent, dp(30)))

        contentRow.addView(playerPanel, panelParams(0.47f))

        // ---- Video + status ----

        playerView = PlayerView(this)

        playerView.useController = false

        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

        playerView.setBackgroundColor(Color.BLACK)

        playerView.isFocusable = false

        playerView.player = player

        buildStatusOverlay()

        moveVideoTo(playerFrame)

        showStatus(Txt.IDLE_TITLE, Txt.IDLE_SUB, false)

        // ---- Fullscreen ----

        fullscreenContainer = FrameLayout(this)

        fullscreenContainer.setBackgroundColor(Color.BLACK)

        fullscreenContainer.visibility = View.GONE

        buildZapBanner()

        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        buildMenuOverlay()

        buildSplash()

        setContentView(root)
    }

    private fun panelParams(weight: Float): LinearLayout.LayoutParams =
        lp(0, matchParent, weight).apply {
            setMargins(dp(7), 0, dp(7), 0)
        }

    private fun buildHeader(): View {

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL

        header.gravity = Gravity.CENTER_VERTICAL

        header.setPadding(dp(34), dp(10), dp(34), dp(6))

        val mark = TextView(this)

        mark.text = "N"
        mark.textSize = 22f
        mark.setTextColor(white)
        mark.typeface = Typeface.DEFAULT_BOLD
        mark.gravity = Gravity.CENTER

        mark.background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(cyan, violet)
            ).apply {
                cornerRadius = dp(14).toFloat()
            }

        header.addView(mark, lp(dp(44), dp(44)))

        val brandCol = LinearLayout(this)

        brandCol.orientation = LinearLayout.VERTICAL

        brandCol.setPadding(dp(12), 0, 0, 0)

        val brand = label("NIYATI", 22f, white, true, Gravity.LEFT)

        brand.letterSpacing = 0.14f

        val sport = label("SPORTS IPTV", 10.5f, cyan, true, Gravity.LEFT)

        sport.letterSpacing = 0.28f

        brandCol.addView(brand, lp(wrap, wrap))

        brandCol.addView(sport, lp(wrap, wrap))

        header.addView(brandCol, lp(0, wrap, 1f))

        header.addView(createLivePill(), lp(wrap, wrap))

        // ---- weather ----
        val weatherChip = LinearLayout(this)

        weatherChip.orientation = LinearLayout.HORIZONTAL

        weatherChip.gravity = Gravity.CENTER_VERTICAL

        weatherChip.setPadding(dp(12), dp(3), dp(14), dp(3))

        weatherChip.background =
            GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(22, 255, 255, 255))
                setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }

        weatherIcon = TextView(this)

        weatherIcon.text = "\u2601\uFE0F"

        weatherIcon.textSize = 22f

        weatherIcon.gravity = Gravity.CENTER

        weatherChip.addView(weatherIcon, lp(wrap, wrap))

        val weatherCol = LinearLayout(this)

        weatherCol.orientation = LinearLayout.VERTICAL

        weatherCol.setPadding(dp(8), 0, 0, 0)

        weatherTemp = label("--\u00B0", 17f, white, true, Gravity.LEFT)

        weatherCityView = label(weatherCity, 10f, gray, false, Gravity.LEFT)

        weatherCol.addView(weatherTemp, lp(wrap, wrap))

        weatherCol.addView(weatherCityView, lp(wrap, wrap))

        weatherChip.addView(weatherCol, lp(wrap, wrap))

        header.addView(
            weatherChip,
            lp(wrap, wrap).apply {
                setMargins(dp(16), 0, 0, 0)
            }
        )

        // ---- clock + date ----
        val timeCol = LinearLayout(this)

        timeCol.orientation = LinearLayout.VERTICAL

        timeCol.gravity = Gravity.RIGHT

        val clock = TextClock(this)

        clock.format12Hour = "hh:mm a"

        clock.format24Hour = "hh:mm a"

        clock.textSize = 20f

        clock.setTextColor(white)

        clock.typeface = Typeface.DEFAULT_BOLD

        clock.gravity = Gravity.RIGHT

        timeCol.addView(clock, lp(wrap, wrap))

        dateView = label("", 11f, gray, false, Gravity.RIGHT)

        timeCol.addView(dateView, lp(wrap, wrap))

        header.addView(
            timeCol,
            lp(wrap, wrap).apply {
                setMargins(dp(18), 0, 0, 0)
            }
        )

        return header
    }

    private fun createLivePill(): LinearLayout {

        val pill = LinearLayout(this)

        pill.orientation = LinearLayout.HORIZONTAL

        pill.gravity = Gravity.CENTER_VERTICAL

        pill.setPadding(dp(10), dp(4), dp(12), dp(4))

        pill.background =
            GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(46, 255, 59, 92))
                setStroke(dp(1), Color.argb(150, 255, 59, 92))
            }

        val dot = View(this)

        dot.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(255, 59, 92))
            }

        val blink =
            AlphaAnimation(1f, 0.2f).apply {
                duration = 800
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }

        dot.startAnimation(blink)

        pill.addView(dot, lp(dp(8), dp(8)))

        val text = label("LIVE", 11f, white, true, Gravity.CENTER)

        text.letterSpacing = 0.12f

        pill.addView(
            text,
            lp(wrap, wrap).apply {
                setMargins(dp(7), 0, 0, 0)
            }
        )

        return pill
    }

    private fun buildInfoBar(): LinearLayout {

        val bar = LinearLayout(this)

        bar.orientation = LinearLayout.HORIZONTAL

        bar.gravity = Gravity.CENTER_VERTICAL

        bar.setPadding(dp(14), 0, dp(14), 0)

        bar.background =
            GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(22, 255, 255, 255))
                setStroke(dp(1), Color.argb(26, 255, 255, 255))
            }

        livePill = createLivePill()

        livePill.visibility = View.GONE

        bar.addView(livePill, lp(wrap, wrap))

        bar.addView(
            qualityBadge,
            lp(wrap, wrap).apply {
                setMargins(dp(8), 0, 0, 0)
            }
        )

        val col = LinearLayout(this)

        col.orientation = LinearLayout.VERTICAL

        col.gravity = Gravity.CENTER_VERTICAL

        nowTitle = label("\u0644\u0645 \u064a\u062a\u0645 \u0627\u062e\u062a\u064a\u0627\u0631 \u0642\u0646\u0627\u0629", 16f, white, true)

        nowSub = label("\u0627\u062e\u062a\u0631 \u0642\u0646\u0627\u0629 \u0645\u0646 \u0627\u0644\u0642\u0627\u0626\u0645\u0629", 12f, gray)

        setupFit(nowTitle, 16, 9)

        nowTitle.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

        col.addView(nowTitle, lp(matchParent, dp(24)))

        col.addView(nowSub, lp(matchParent, wrap))

        bar.addView(col, lp(0, wrap, 1f))

        return bar
    }

    private fun buildStatusOverlay() {

        statusOverlay = LinearLayout(this)

        statusOverlay.orientation = LinearLayout.VERTICAL

        statusOverlay.gravity = Gravity.CENTER

        statusOverlay.setPadding(dp(16), dp(16), dp(16), dp(16))

        statusOverlay.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.rgb(12, 16, 38),
                    Color.rgb(7, 9, 22)
                )
            )

        statusIcon = FrameLayout(this)

        statusPlay = PlayIcon(Color.WHITE)

        statusBang = TextView(this)

        statusBang.text = "!"
        statusBang.textSize = 26f
        statusBang.setTextColor(white)
        statusBang.typeface = Typeface.DEFAULT_BOLD
        statusBang.gravity = Gravity.CENTER
        statusBang.visibility = View.GONE

        statusIcon.addView(
            statusPlay,
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER).apply {
                leftMargin = dp(4)
            }
        )

        statusIcon.addView(
            statusBang,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        statusOverlay.addView(statusIcon, lp(dp(58), dp(58)))

        statusTitle = label("", 17f, white, true, Gravity.CENTER, 3)

        statusSub = label("", 12f, gray, false, Gravity.CENTER, 7)

        statusOverlay.addView(
            statusTitle,
            lp(matchParent, wrap).apply {
                topMargin = dp(14)
            }
        )

        statusOverlay.addView(
            statusSub,
            lp(matchParent, wrap).apply {
                topMargin = dp(4)
            }
        )
    }

    private fun showStatus(title: String, sub: String, error: Boolean) {

        statusTitle.text = title

        statusSub.text = sub

        val colors =
            if (error) {
                intArrayOf(Color.rgb(255, 82, 82), Color.rgb(244, 63, 94))
            } else {
                intArrayOf(cyan, violet)
            }

        statusIcon.background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                colors
            ).apply {
                shape = GradientDrawable.OVAL
            }

        statusPlay.visibility = if (error) View.GONE else View.VISIBLE

        statusBang.visibility = if (error) View.VISIBLE else View.GONE

        statusOverlay.visibility = View.VISIBLE

        if (error && ::qualityBadge.isInitialized) {
            qualityBadge.visibility = View.GONE
        }
    }

    private fun hideStatus() {
        statusOverlay.visibility = View.GONE
    }

    private fun moveVideoTo(container: FrameLayout) {

        (playerView.parent as? ViewGroup)?.removeView(playerView)

        (webView.parent as? ViewGroup)?.removeView(webView)

        (statusOverlay.parent as? ViewGroup)?.removeView(statusOverlay)

        // order: ExoPlayer video, then WebView, then the status overlay on top
        container.addView(
            playerView,
            0,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        container.addView(
            webView,
            1,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        container.addView(
            statusOverlay,
            2,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )
    }

    private fun applyPlayerFrameBackground(focused: Boolean) {

        playerFrame.background =
            GradientDrawable().apply {

                cornerRadius = dp(10).toFloat()

                setColor(Color.BLACK)

                setStroke(
                    if (focused) dp(3) else dp(1),
                    if (focused) cyan else Color.argb(45, 255, 255, 255)
                )
            }
    }

    private fun createPanel(): LinearLayout {

        val panel = LinearLayout(this)

        panel.orientation = LinearLayout.VERTICAL

        panel.setPadding(dp(4), dp(6), dp(4), dp(6))

        panel.clipChildren = false

        panel.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.argb(30, 255, 255, 255),
                    Color.argb(12, 255, 255, 255)
                )
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.argb(38, 255, 255, 255))
            }

        return panel
    }

    private fun createScroll(): ScrollView {

        val scroll = ScrollView(this)

        scroll.isFillViewport = true

        scroll.isVerticalScrollBarEnabled = false

        scroll.overScrollMode = View.OVER_SCROLL_NEVER

        scroll.clipToPadding = false

        scroll.clipChildren = false

        return scroll
    }

    private fun createList(): LinearLayout {

        val list = LinearLayout(this)

        list.orientation = LinearLayout.VERTICAL

        list.setPadding(dp(10), dp(4), dp(10), dp(10))

        list.clipChildren = false

        return list
    }

    // =========================
    // Custom views
    // =========================

    private inner class AmbientBackground : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var base: Shader? = null
        private var glowA: Shader? = null
        private var glowB: Shader? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

            super.onSizeChanged(w, h, oldw, oldh)

            val fw = w.toFloat()

            val fh = h.toFloat()

            base =
                LinearGradient(
                    0f,
                    0f,
                    fw,
                    fh,
                    intArrayOf(bgTop, bgMid, bgBottom),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )

            glowA =
                RadialGradient(
                    fw * 0.88f,
                    fh * 0.08f,
                    fw * 0.55f,
                    intArrayOf(
                        Color.argb(95, 168, 85, 247),
                        Color.argb(0, 168, 85, 247)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )

            glowB =
                RadialGradient(
                    fw * 0.08f,
                    fh * 0.98f,
                    fw * 0.5f,
                    intArrayOf(
                        Color.argb(80, 0, 229, 255),
                        Color.argb(0, 0, 229, 255)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
        }

        override fun onDraw(canvas: Canvas) {

            val w = width.toFloat()

            val h = height.toFloat()

            paint.shader = base
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = glowA
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = glowB
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    private inner class PlayIcon(tint: Int) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val path = Path()

        init {
            paint.color = tint
        }

        fun setTint(color: Int) {
            paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {

            val w = width.toFloat()

            val h = height.toFloat()

            path.reset()

            path.moveTo(w * 0.2f, h * 0.08f)

            path.lineTo(w * 0.95f, h * 0.5f)

            path.lineTo(w * 0.2f, h * 0.92f)

            path.close()

            canvas.drawPath(path, paint)
        }
    }

    private inner class SectionHeader(
        arabic: String,
        english: String,
        accent: Accent,
        showCount: Boolean = true
    ) : LinearLayout(this@MainActivity) {

        private val countChip = TextView(this@MainActivity)

        private val subtitle = label(english, 10.5f, accent.start)

        init {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(dp(14), 0, dp(14), 0)

            countChip.textSize = 12f

            countChip.setTextColor(white)

            countChip.typeface = Typeface.DEFAULT_BOLD

            countChip.gravity = Gravity.CENTER

            countChip.minWidth = dp(30)

            countChip.setPadding(dp(10), dp(4), dp(10), dp(4))

            countChip.background =
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(withAlpha(accent.start, 60))
                    setStroke(dp(1), withAlpha(accent.start, 140))
                }

            countChip.visibility = if (showCount) View.VISIBLE else View.GONE

            addView(countChip, LinearLayout.LayoutParams(wrap, wrap))

            val col = LinearLayout(this@MainActivity)

            col.orientation = LinearLayout.VERTICAL

            col.gravity = Gravity.CENTER_VERTICAL

            col.setPadding(dp(10), 0, dp(12), 0)

            col.addView(
                label(arabic, 19f, white, true),
                LinearLayout.LayoutParams(matchParent, wrap)
            )

            subtitle.letterSpacing = 0.15f

            col.addView(subtitle, LinearLayout.LayoutParams(matchParent, wrap))

            addView(col, LinearLayout.LayoutParams(0, wrap, 1f))

            val bar = View(this@MainActivity)

            bar.background =
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(accent.start, accent.end)
                ).apply {
                    cornerRadius = dp(2).toFloat()
                }

            addView(bar, LinearLayout.LayoutParams(dp(4), dp(36)))
        }

        fun setCount(count: Int) {
            countChip.text = count.toString()
        }

        fun setSubtitle(text: String) {

            subtitle.text = text

            val hasArabic = text.any { it in '\u0600'..'\u06FF' }

            subtitle.letterSpacing = if (hasArabic) 0f else 0.15f
        }
    }

    private inner class LogoBadge(
        sizeDp: Int,
        private val accent: Accent
    ) : FrameLayout(this@MainActivity) {

        private val image = ImageView(this@MainActivity)

        private val letter = TextView(this@MainActivity)

        private val radius = dp(sizeDp / 3).toFloat()

        private var currentUrl = ""

        init {

            image.scaleType = ImageView.ScaleType.FIT_CENTER

            image.setPadding(dp(5), dp(5), dp(5), dp(5))

            image.visibility = View.GONE

            letter.textSize = sizeDp * 0.42f

            letter.setTextColor(Color.WHITE)

            letter.typeface = Typeface.DEFAULT_BOLD

            letter.gravity = Gravity.CENTER

            addView(image, FrameLayout.LayoutParams(matchParent, matchParent))

            addView(letter, FrameLayout.LayoutParams(matchParent, matchParent))

            showFallback()
        }

        private fun showFallback() {

            background =
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(accent.start, accent.end)
                ).apply {
                    cornerRadius = radius
                }

            image.visibility = View.GONE

            letter.visibility = View.VISIBLE
        }

        private fun showImage(bitmap: Bitmap) {

            image.setImageBitmap(bitmap)

            image.visibility = View.VISIBLE

            letter.visibility = View.GONE

            background =
                GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(Color.argb(242, 250, 250, 255))
                }
        }

        fun bind(name: String, url: String) {

            currentUrl = url

            letter.text = name.trim().take(1).uppercase()

            showFallback()

            if (!url.startsWith("http", ignoreCase = true)) {
                return
            }

            val cached = imageCache.get(url)

            if (cached != null) {
                showImage(cached)
                return
            }

            try {

                ioExecutor.execute {

                    val bitmap = downloadBitmap(url)

                    if (bitmap != null) {

                        imageCache.put(url, bitmap)

                        post {
                            if (currentUrl == url) {
                                showImage(bitmap)
                            }
                        }
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    private inner class BannerCard(
        private val big: Boolean,
        private val accent: Accent
    ) : LinearLayout(this@MainActivity) {

        private val numberView =
            label("", if (big) 24f else 13f, white, true, Gravity.CENTER)

        private val badge = LogoBadge(if (big) 58 else 38, accent)

        private val nameView = label("", if (big) 22f else 15f, white, true)

        private val subView = label("", 12f, gray)

        private val qualityChip = TextView(this@MainActivity)

        init {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(dp(12), 0, dp(14), 0)

            alpha = if (big) 1f else 0.7f

            background =
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        if (big) mix(accent.start, bgTop, 0.35f) else Color.argb(215, 12, 16, 38),
                        if (big) mix(accent.end, bgTop, 0.35f) else Color.argb(215, 8, 10, 28)
                    )
                ).apply {
                    cornerRadius = dp(if (big) 26 else 20).toFloat()
                    setStroke(
                        if (big) dp(2) else dp(1),
                        if (big) Color.WHITE else Color.argb(60, 255, 255, 255)
                    )
                }

            addView(numberView, LinearLayout.LayoutParams(dp(if (big) 52 else 34), matchParent))

            val texts = LinearLayout(this@MainActivity)

            texts.orientation = LinearLayout.VERTICAL

            texts.gravity = Gravity.CENTER_VERTICAL

            texts.setPadding(dp(6), 0, dp(10), 0)

            setupFit(nameView, if (big) 22 else 15, 10)

            nameView.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

            texts.addView(
                nameView,
                LinearLayout.LayoutParams(matchParent, dp(if (big) 30 else 22))
            )

            if (big) {

                val row = LinearLayout(this@MainActivity)

                row.orientation = LinearLayout.HORIZONTAL

                row.gravity = Gravity.CENTER_VERTICAL

                qualityChip.textSize = 11f

                qualityChip.typeface = Typeface.DEFAULT_BOLD

                qualityChip.gravity = Gravity.CENTER

                qualityChip.setPadding(dp(8), dp(2), dp(8), dp(2))

                qualityChip.visibility = View.GONE

                row.addView(qualityChip, LinearLayout.LayoutParams(wrap, wrap))

                row.addView(
                    subView,
                    LinearLayout.LayoutParams(0, wrap, 1f).apply {
                        setMargins(dp(8), 0, 0, 0)
                    }
                )

                texts.addView(row, LinearLayout.LayoutParams(matchParent, wrap))
            }

            addView(texts, LinearLayout.LayoutParams(0, matchParent, 1f))

            addView(badge, LinearLayout.LayoutParams(dp(if (big) 58 else 38), dp(if (big) 58 else 38)))
        }

        fun bind(channel: Channel, number: Int, sub: String) {

            numberView.text = String.format(Locale.US, "%02d", number)

            fitName(nameView, channel.name, if (big) 22 else 15, 10)

            subView.text = sub

            badge.bind(channel.name, channel.logo)
        }

        fun setQuality(text: String, color: Int) {

            if (!big) {
                return
            }

            if (text.isEmpty()) {

                qualityChip.visibility = View.GONE

            } else {

                qualityChip.text = text

                qualityChip.setTextColor(color)

                qualityChip.background =
                    GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(Color.argb(150, 8, 10, 28))
                        setStroke(dp(1), color)
                    }

                qualityChip.visibility = View.VISIBLE
            }
        }
    }

    private inner class TvCard(
        val accent: Accent,
        badgeSizeDp: Int,
        leadWidthDp: Int
    ) : LinearLayout(this@MainActivity) {

        val lead = FrameLayout(this@MainActivity)

        val badge = LogoBadge(badgeSizeDp, accent)

        val nameView = label("", 16f, white, true)

        val subView = label("", 11.5f, gray)

        var stateListener: ((Boolean, Boolean) -> Unit)? = null

        var onFocused: (() -> Unit)? = null

        var focusedNow = false
            private set

        var chosen = false
            set(value) {
                field = value
                refresh()
            }

        var moving = false
            set(value) {
                field = value
                refresh()
            }

        init {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(dp(10), 0, dp(12), 0)

            isFocusable = true

            isFocusableInTouchMode = true

            clipChildren = false

            addView(lead, LinearLayout.LayoutParams(dp(leadWidthDp), matchParent))

            val texts = LinearLayout(this@MainActivity)

            texts.orientation = LinearLayout.VERTICAL

            texts.gravity = Gravity.CENTER_VERTICAL

            texts.setPadding(dp(8), 0, dp(12), 0)

            setupFit(nameView, 16, 9)

            nameView.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

            texts.addView(nameView, LinearLayout.LayoutParams(matchParent, dp(24)))

            texts.addView(
                subView,
                LinearLayout.LayoutParams(matchParent, wrap).apply {
                    topMargin = dp(2)
                }
            )

            addView(texts, LinearLayout.LayoutParams(0, matchParent, 1f))

            addView(
                badge,
                LinearLayout.LayoutParams(dp(badgeSizeDp), dp(badgeSizeDp))
            )

            refresh()
        }

        override fun onFocusChanged(
            gainFocus: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {

            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

            focusedNow = gainFocus

            val scale = if (gainFocus) 1.04f else 1f

            animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(130)
                .start()

            refresh()

            if (gainFocus) {
                onFocused?.invoke()
            }
        }

        private fun refresh() {

            val d: GradientDrawable

            if (moving) {

                d =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(withAlpha(gold, 150), withAlpha(Color.rgb(249, 115, 22), 150))
                    )

                d.setStroke(dp(3), gold)

            } else if (focusedNow) {

                d =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(
                            mix(accent.start, bgMid, 0.62f),
                            mix(accent.end, bgMid, 0.62f)
                        )
                    )

                d.setStroke(dp(2), Color.WHITE)

            } else if (chosen) {

                d = GradientDrawable()

                d.setColor(withAlpha(accent.start, 46))

                d.setStroke(dp(1), withAlpha(accent.start, 190))

            } else {

                d = GradientDrawable()

                d.setColor(Color.argb(20, 255, 255, 255))

                d.setStroke(dp(1), Color.argb(26, 255, 255, 255))
            }

            d.cornerRadius = dp(18).toFloat()

            background = d

            nameView.setTextColor(white)

            subView.setTextColor(if (focusedNow) dimWhite else gray)

            stateListener?.invoke(focusedNow, chosen)
        }
    }

    // =========================
    // Card factories
    // =========================

    private fun createPackageCard(item: PackageItem, index: Int): TvCard {

        val accent = accentOfPackage(item)

        val card = TvCard(accent, 44, 18)

        val count = channels.count { belongs(it, item) }

        fitName(card.nameView, item.name, 16, 9)

        card.subView.text = "$count \u0642\u0646\u0627\u0629"

        if (item.id == FAV_ID) {
            card.badge.bind("★", "")
        } else {
            card.badge.bind(item.name, item.logo)
        }

        val dot = View(this)

        dot.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent.start)
            }

        dot.visibility = View.INVISIBLE

        card.lead.addView(
            dot,
            FrameLayout.LayoutParams(dp(8), dp(8), Gravity.CENTER)
        )

        card.stateListener = { focused, chosen ->

            dot.visibility = if (chosen) View.VISIBLE else View.INVISIBLE

            val dotBg = dot.background as GradientDrawable

            dotBg.setColor(
                if (focused) Color.WHITE else accent.start
            )
        }

        return card
    }

    private fun createChannelRow(
        channel: Channel,
        number: Int,
        accent: Accent
    ): ChannelRow {

        val card = TvCard(accent, 46, 34)

        val hasUrl = channel.url.isNotBlank()

        val isVod = hasUrl && isVodChannel(channel)

        fitName(card.nameView, channel.name, 16, 9)

        card.badge.bind(channel.name, channel.logo)

        val numberView =
            label(
                String.format(Locale.US, "%02d", number),
                12f,
                gray,
                true,
                Gravity.CENTER
            )

        val playIcon = PlayIcon(accent.start)

        playIcon.visibility = View.GONE

        card.lead.addView(
            numberView,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        card.lead.addView(
            playIcon,
            FrameLayout.LayoutParams(dp(15), dp(15), Gravity.CENTER)
        )

        var playing = false

        fun applyState() {

            val focused = card.focusedNow

            numberView.visibility = if (playing) View.GONE else View.VISIBLE

            playIcon.visibility = if (playing) View.VISIBLE else View.GONE

            playIcon.setTint(if (focused) Color.WHITE else accent.start)

            numberView.setTextColor(if (focused) dimWhite else gray)

            card.subView.text =
                when {
                    !hasUrl -> "\u063a\u064a\u0631 \u0645\u062a\u0627\u062d \u062d\u0627\u0644\u064a\u0627\u064b"
                    playing -> "\u064a\u0639\u0631\u0636 \u0627\u0644\u0622\u0646"
                    isVod -> "فيديو"
                    else -> "بث مباشر"
                }

            card.subView.setTextColor(
                when {
                    focused -> dimWhite
                    playing -> accent.start
                    else -> gray
                }
            )

            card.alpha = if (hasUrl) 1f else 0.55f
        }

        card.stateListener = { _, _ ->
            applyState()
        }

        applyState()

        card.onFocused = {
            lastFocusedChannel = channel
        }

        card.setOnLongClickListener {
            showChannelMenu(channel)
            true
        }

        card.setOnClickListener {

            val alreadyPlaying =
                playingChannel == channel &&
                        (playingWeb || player.playbackState != Player.STATE_IDLE)

            if (alreadyPlaying) {
                enterFullscreen()
            } else {
                playChannel(channel)
            }
        }

        return ChannelRow(channel, card) { value ->
            playing = value
            applyState()
        }
    }

    // =========================
    // Firebase
    // =========================

    private val loadTimeout = Runnable {

        if (!dataReady) {

            showListMessage(packagesList, "\u0644\u0627 \u064a\u0648\u062c\u062f \u0627\u062a\u0635\u0627\u0644", "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a")

            showListMessage(
                channelsList,
                Txt.LOAD_FAIL_TITLE,
                Txt.LOAD_FAIL_SUB
            )
        }
    }

    private fun startFirebase() {

        showListMessage(packagesList, "\u062c\u0627\u0631\u064a \u0627\u0644\u062a\u062d\u0645\u064a\u0644", "\u064a\u0631\u062c\u0649 \u0627\u0644\u0627\u0646\u062a\u0638\u0627\u0631")

        showListMessage(channelsList, "\u062c\u0627\u0631\u064a \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0642\u0646\u0648\u0627\u062a", "\u064a\u0631\u062c\u0649 \u0627\u0644\u0627\u0646\u062a\u0638\u0627\u0631")

        mainHandler.postDelayed(loadTimeout, 12000)

        val listener =
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    onDataLoaded(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {

                    mainHandler.removeCallbacks(loadTimeout)

                    Toast.makeText(
                        this@MainActivity,
                        Txt.DB_ERROR,
                        Toast.LENGTH_LONG
                    ).show()

                    if (!dataReady) {

                        showListMessage(
                            packagesList,
                            "\u062a\u0639\u0630\u0631 \u0627\u0644\u0627\u062a\u0635\u0627\u0644",
                            "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u0644\u0625\u0639\u062f\u0627\u062f\u0627\u062a"
                        )

                        showListMessage(
                            channelsList,
                            "\u062a\u0639\u0630\u0631 \u0627\u0644\u0627\u062a\u0635\u0627\u0644",
                            "\u0628\u0642\u0627\u0639\u062f\u0629 \u0627\u0644\u0628\u064a\u0627\u0646\u0627\u062a"
                        )
                    }
                }
            }

        dataListener = listener

        rootRef.addValueEventListener(listener)
    }

    private fun onDataLoaded(snapshot: DataSnapshot) {

        mainHandler.removeCallbacks(loadTimeout)

        readWeatherSettings(snapshot)

        val newChannels = parseChannels(snapshot)

        var newPackages = parsePackages(snapshot)

        if (newPackages.isEmpty() && newChannels.isNotEmpty()) {

            newPackages =
                newChannels
                    .map { it.group }
                    .distinct()
                    .mapIndexed { i, g ->
                        PackageItem(g, g, "", true, i)
                    }
        }

        if (
            dataReady &&
            newPackages == basePackages &&
            newChannels == channels
        ) {
            return
        }

        dataReady = true

        basePackages = newPackages

        channels = newChannels

        rebuildPackages()

        renderPackages()

        val keep =
            packages.firstOrNull { it.id == selectedPackage }
                ?: packages.firstOrNull { it.id != FAV_ID }
                ?: packages.firstOrNull()

        if (keep != null) {

            selectPackage(keep.id, true)

        } else {

            selectedPackage = ""

            renderChannels()
        }

        if (!autoPlayDone) {
            autoPlayDone = true
            playLastChannel()
        }

        if (currentFocus == null && !isFullscreen) {
            focusSelectedPackage()
        }
    }

    private fun DataSnapshot.readString(key: String): String? =
        child(key)
            .value
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun DataSnapshot.readBool(key: String, default: Boolean): Boolean {

        return when (val v = child(key).value) {

            is Boolean -> v

            is Number -> v.toInt() != 0

            is String -> !(v.equals("false", true) || v == "0")

            else -> default
        }
    }

    private fun DataSnapshot.readInt(key: String, default: Int): Int {

        return when (val v = child(key).value) {

            is Number -> v.toInt()

            is String -> v.trim().toIntOrNull() ?: default

            else -> default
        }
    }

    private fun parsePackages(snapshot: DataSnapshot): List<PackageItem> {

        val result = mutableListOf<PackageItem>()

        for (child in snapshot.child("packages").children) {

            val id = child.key ?: continue

            if (!child.readBool("enabled", true)) {
                continue
            }

            result.add(
                PackageItem(
                    id = id,
                    name = child.readString("name") ?: id,
                    logo = child.readString("logo") ?: "",
                    enabled = true,
                    order = child.readInt("order", 999)
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun parseChannels(snapshot: DataSnapshot): List<Channel> {

        val result = mutableListOf<Channel>()

        for (child in snapshot.child("channels").children) {

            val name = child.readString("name") ?: continue

            val group = child.readString("group") ?: continue

            if (!child.readBool("enabled", true)) {
                continue
            }

            result.add(
                Channel(
                    name = name,
                    group = group,
                    url = child.readString("url") ?: "",
                    logo = child.readString("logo") ?: "",
                    enabled = true,
                    order = child.readInt("order", 999),
                    userAgent =
                        child.readString("userAgent")
                            ?: child.readString("user_agent")
                            ?: "",
                    referer = child.readString("referer") ?: ""
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun belongs(channel: Channel, pkg: PackageItem): Boolean =
        if (pkg.id == FAV_ID) {
            isFavorite(channel)
        } else {
            channel.group.equals(pkg.id, ignoreCase = true) ||
                    channel.group.equals(pkg.name, ignoreCase = true)
        }

    // =========================
    // Render lists
    // =========================

    private fun showListMessage(list: LinearLayout, title: String, sub: String) {

        list.removeAllViews()

        val box = LinearLayout(this)

        box.orientation = LinearLayout.VERTICAL

        box.gravity = Gravity.CENTER

        box.setPadding(dp(12), dp(36), dp(12), dp(36))

        box.addView(
            label(title, 16f, white, true, Gravity.CENTER, 2),
            lp(matchParent, wrap)
        )

        box.addView(
            label(sub, 12f, gray, false, Gravity.CENTER, 3),
            lp(matchParent, wrap).apply {
                topMargin = dp(6)
            }
        )

        list.addView(box, lp(matchParent, wrap))
    }

    private fun renderPackages() {

        val focusedId =
            packageRows
                .firstOrNull { it.card.hasFocus() }
                ?.item
                ?.id

        packagesList.removeAllViews()
        packageRows.clear()

        packages.forEachIndexed { index, item ->

            val card = createPackageCard(item, index)

            card.chosen = item.id == selectedPackage

            card.onFocused = {
                if (moveState == null) {
                    scheduleSelect(item.id)
                }
            }

            if (item.id != FAV_ID) {
                card.setOnLongClickListener {
                    showPackageMenu(item)
                    true
                }
            }

            card.setOnClickListener {

                mainHandler.removeCallbacks(selectRunnable)

                pendingPackageId = null

                selectPackage(item.id)

                focusChannelsColumn()
            }

            packagesList.addView(
                card,
                lp(matchParent, dp(66)).apply {
                    setMargins(0, dp(5), 0, dp(5))
                }
            )

            packageRows.add(PackageRow(item, card))
        }

        packagesHeader.setCount(packages.size)

        if (focusedId != null) {

            packageRows
                .firstOrNull { it.item.id == focusedId }
                ?.card
                ?.requestFocus()
        }

        refreshMovingVisual()
    }

    private val selectRunnable = Runnable {

        val id = pendingPackageId

        pendingPackageId = null

        if (id != null) {
            selectPackage(id)
        }
    }

    private fun scheduleSelect(id: String) {

        pendingPackageId = id

        mainHandler.removeCallbacks(selectRunnable)

        mainHandler.postDelayed(selectRunnable, 180)
    }

    private fun flushPendingSelect() {

        mainHandler.removeCallbacks(selectRunnable)

        val id = pendingPackageId

        pendingPackageId = null

        if (id != null) {
            selectPackage(id)
        }
    }

    private fun selectPackage(id: String, force: Boolean = false) {

        if (!force && id == selectedPackage) {
            return
        }

        selectedPackage = id

        packageRows.forEach {
            it.card.chosen = it.item.id == id
        }

        renderChannels()
    }

    private fun renderChannels(keepScroll: Boolean = false) {

        val restoreIndex = channelRows.indexOfFirst { it.card.hasFocus() }

        channelsList.removeAllViews()
        channelRows.clear()

        lastFocusedChannel = null

        val pkgIndex = packages.indexOfFirst { it.id == selectedPackage }

        val pkg = packages.getOrNull(pkgIndex)

        channelsHeader.setSubtitle(pkg?.name ?: "CHANNELS")

        visibleChannels =
            if (pkg == null) {
                emptyList()
            } else {

                val base =
                    channels
                        .filter { belongs(it, pkg) }
                        .sortedBy { it.order }

                val saved = loadChannelOrder(pkg.id)

                if (saved.isEmpty()) {
                    base
                } else {
                    val idx = saved.withIndex().associate { it.value to it.index }
                    base.sortedBy { idx[channelKey(it)] ?: Int.MAX_VALUE }
                }
            }

        channelsHeader.setCount(visibleChannels.size)

        if (!keepScroll) {
            channelScroll.scrollTo(0, 0)
        }

        if (visibleChannels.isEmpty()) {

            showListMessage(
                channelsList,
                Txt.NO_CHANNELS,
                Txt.NO_CHANNELS_SUB
            )

            return
        }

        val accent = pkg?.let { accentOfPackage(it) } ?: accentFor(0)

        visibleChannels.forEachIndexed { i, channel ->

            val row = createChannelRow(channel, i + 1, accent)

            row.setPlaying(channel == playingChannel)

            channelsList.addView(
                row.card,
                lp(matchParent, dp(66)).apply {
                    setMargins(0, dp(5), 0, dp(5))
                }
            )

            channelRows.add(row)
        }

        if (restoreIndex >= 0) {

            channelRows
                .getOrNull(minOf(restoreIndex, channelRows.size - 1))
                ?.card
                ?.requestFocus()
        }

        refreshMovingVisual()
    }

    // =========================
    // Playback
    // =========================

    private fun playChannel(channel: Channel) {

        val source = parseSource(channel.url)

        if (source.url.isBlank()) {

            Toast.makeText(this, Txt.NO_URL, Toast.LENGTH_SHORT).show()

            return
        }

        retryCount = 0
        liveWindowRetries = 0
        finalPlaybackError = false
        playbackStarted = false
        firstError = null

        mainHandler.removeCallbacks(retryRunnable)
        mainHandler.removeCallbacks(hideWebStatusRunnable)

        updateQuality(0)

        playingChannel = channel

        playingList = visibleChannels

        prefs.edit().putString("last_channel", channelKey(channel)).apply()

        updatePlayingMarks()

        updateNowPlaying(channel)

        // ---- YouTube / Facebook -> WebView ----
        if (isWebSource(source.url)) {

            currentSource = null

            playWeb(channel, source.url)

            return
        }

        // ---- Normal stream -> ExoPlayer ----
        stopWeb()

        currentSource = source

        attempts = attemptsFor(source.url)

        attemptIndex = 0

        showStatus(
            "\u062c\u0627\u0631\u064a \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629\u2026",
            "${channel.name}\n\u0627\u0644\u0645\u0635\u062f\u0631: ${attempts[0].label}",
            false
        )

        startCurrentAttempt(channel)
    }

    private fun showDetailedPlaybackError(
        error: PlaybackException?,
        exception: Throwable?
    ) {

        val message =
            if (error != null) {

                buildPlaybackErrorMessage(error)

            } else {

                val cause = exception?.cause

                val text =
                    exception?.message
                        ?: cause?.message
                        ?: exception?.javaClass?.simpleName
                        ?: "\u062e\u0637\u0623 \u063a\u064a\u0631 \u0645\u0639\u0631\u0648\u0641"

                "\u062e\u0637\u0623 \u062f\u0627\u062e\u0644\u064a\n${shortenError(text)}"
            }

        showStatus("\u062a\u0639\u0630\u0631 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629", message, true)
    }

    private fun updatePlayingMarks() {

        channelRows.forEach {
            it.setPlaying(it.channel == playingChannel)
        }
    }

    private fun updateNowPlaying(channel: Channel) {

        fitName(nowTitle, channel.name, 16, 9)

        nowSub.text = realPackageName(channel)

        livePill.visibility =
            if (isVodChannel(channel)) View.GONE else View.VISIBLE
    }

    private fun zap(delta: Int) {

        val list = playingList

        if (list.isEmpty()) {
            return
        }

        var index = list.indexOf(playingChannel)

        if (index < 0) {
            index = if (delta > 0) -1 else 0
        }

        repeat(list.size) {

            index = (index + delta + list.size) % list.size

            val candidate = list[index]

            if (candidate.url.isNotBlank()) {

                lastFocusedChannel = candidate

                playChannel(candidate)

                showZapBanner(candidate)

                return
            }
        }
    }

    private val hideZapRunnable = Runnable {

        zapBanner.animate()
            .alpha(0f)
            .setDuration(220)
            .withEndAction {
                zapBanner.visibility = View.GONE
            }
            .start()
    }

    private fun neighborOf(list: List<Channel>, index: Int, delta: Int): Channel? {

        if (list.size < 2) {
            return null
        }

        var i = index

        repeat(list.size) {

            i = (i + delta + list.size) % list.size

            val c = list[i]

            if (i != index && c.url.isNotBlank()) {
                return c
            }
        }

        return null
    }

    private fun showZapBanner(channel: Channel) {

        val list =
            if (playingList.contains(channel)) playingList else listOf(channel)

        val index = list.indexOf(channel)

        val prev = neighborOf(list, index, -1)

        val next = neighborOf(list, index, 1)

        fun pkgName(c: Channel): String =
            realPackageName(c)

        bannerCur.bind(channel, index + 1, pkgName(channel))

        bannerCur.setQuality(qualityText, qualityColor)

        if (prev != null) {
            bannerPrev.bind(prev, list.indexOf(prev) + 1, "")
            bannerPrev.visibility = View.VISIBLE
        } else {
            bannerPrev.visibility = View.INVISIBLE
        }

        if (next != null) {
            bannerNext.bind(next, list.indexOf(next) + 1, "")
            bannerNext.visibility = View.VISIBLE
        } else {
            bannerNext.visibility = View.INVISIBLE
        }

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        if (zapBanner.visibility != View.VISIBLE) {
            zapBanner.alpha = 0f
            zapBanner.visibility = View.VISIBLE
        }

        zapBanner.animate().alpha(1f).setDuration(180).start()

        mainHandler.postDelayed(hideZapRunnable, 4200)
    }

    private fun buildZapBanner() {

        zapBanner = LinearLayout(this)

        zapBanner.orientation = LinearLayout.HORIZONTAL

        zapBanner.gravity = Gravity.CENTER_VERTICAL

        zapBanner.visibility = View.GONE

        bannerPrev = BannerCard(false, accentFor(1))

        bannerCur = BannerCard(true, accentFor(0))

        bannerNext = BannerCard(false, accentFor(2))

        zapBanner.addView(
            bannerPrev,
            lp(0, dp(64), 0.27f).apply { setMargins(0, 0, dp(10), 0) }
        )

        zapBanner.addView(bannerCur, lp(0, dp(92), 0.46f))

        zapBanner.addView(
            bannerNext,
            lp(0, dp(64), 0.27f).apply { setMargins(dp(10), 0, 0, 0) }
        )

        fullscreenContainer.addView(
            zapBanner,
            FrameLayout.LayoutParams(matchParent, wrap, Gravity.BOTTOM).apply {
                setMargins(dp(48), 0, dp(48), dp(40))
            }
        )
    }

    // =========================
    // Quality badge
    // =========================

    private fun buildQualityBadge() {

        qualityBadge = TextView(this)

        qualityBadge.textSize = 11f

        qualityBadge.setTextColor(white)

        qualityBadge.typeface = Typeface.DEFAULT_BOLD

        qualityBadge.gravity = Gravity.CENTER

        qualityBadge.setPadding(dp(8), dp(3), dp(8), dp(3))

        qualityBadge.visibility = View.GONE
    }

    private fun updateQuality(height: Int) {

        val (text, color) =
            when {
                height >= 2160 -> "4K UHD" to Color.rgb(250, 204, 21)
                height >= 1440 -> "2K QHD" to Color.rgb(250, 204, 21)
                height >= 1080 -> "FHD 1080p" to Color.rgb(52, 211, 153)
                height >= 720 -> "HD 720p" to cyan
                height >= 480 -> "SD 480p" to Color.rgb(255, 183, 77)
                height > 0 -> "SD ${height}p" to Color.rgb(255, 120, 100)
                else -> "" to Color.WHITE
            }

        qualityText = text

        qualityColor = color

        if (text.isEmpty() || playingWeb) {

            qualityBadge.visibility = View.GONE

        } else {

            qualityBadge.text = text

            qualityBadge.setTextColor(color)

            qualityBadge.background =
                GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.argb(190, 8, 10, 28))
                    setStroke(dp(1), color)
                }

            qualityBadge.visibility = View.VISIBLE
        }

        if (::bannerCur.isInitialized) {
            bannerCur.setQuality(qualityText, qualityColor)
        }
    }

    // =========================
    // Weather + date
    // =========================

    private val weatherRunnable = object : Runnable {
        override fun run() {
            fetchWeather()
            mainHandler.postDelayed(this, 30 * 60 * 1000L)
        }
    }

    private val dateRunnable = object : Runnable {
        override fun run() {
            updateDate()
            mainHandler.postDelayed(this, 30 * 1000L)
        }
    }

    private fun updateDate() {

        try {

            val format =
                SimpleDateFormat(
                    "EEEE d MMMM yyyy",
                    Locale.forLanguageTag("ar-IQ-u-nu-latn")
                )

            dateView.text = format.format(Date())

        } catch (_: Exception) {
        }
    }

    private fun weatherEmoji(code: Int): String =
        when (code) {
            0 -> "\u2600\uFE0F"
            1 -> "\uD83C\uDF24\uFE0F"
            2 -> "\u26C5"
            3 -> "\u2601\uFE0F"
            45, 48 -> "\uD83C\uDF2B\uFE0F"
            in 51..57 -> "\uD83C\uDF26\uFE0F"
            in 61..67 -> "\uD83C\uDF27\uFE0F"
            in 71..77 -> "\u2744\uFE0F"
            in 80..82 -> "\uD83C\uDF27\uFE0F"
            85, 86 -> "\u2744\uFE0F"
            in 95..99 -> "\u26C8\uFE0F"
            else -> "\u2601\uFE0F"
        }

    private fun fetchWeather() {

        val lat = weatherLat

        val lon = weatherLon

        try {

            ioExecutor.execute {

                var connection: HttpURLConnection? = null

                try {

                    val api =
                        "https://api.open-meteo.com/v1/forecast" +
                                "?latitude=$lat&longitude=$lon" +
                                "&current=temperature_2m,weather_code&timezone=auto"

                    connection = URL(api).openConnection() as HttpURLConnection

                    connection.connectTimeout = 8000

                    connection.readTimeout = 8000

                    if (connection.responseCode !in 200..299) {
                        return@execute
                    }

                    val text =
                        connection.inputStream
                            .use { it.readBytes() }
                            .toString(Charsets.UTF_8)

                    val current = JSONObject(text).getJSONObject("current")

                    val temp = current.getDouble("temperature_2m")

                    val code = current.getInt("weather_code")

                    mainHandler.post {
                        weatherTemp.text = "${Math.round(temp)}\u00B0"
                        weatherIcon.text = weatherEmoji(code)
                        weatherCityView.text = weatherCity
                    }

                } catch (_: Exception) {

                } finally {

                    connection?.disconnect()
                }
            }

        } catch (_: Exception) {
        }
    }

    // optional Firebase settings: settings/weather_lat, weather_lon, weather_city
    private fun readWeatherSettings(snapshot: DataSnapshot) {

        val settings = snapshot.child("settings")

        val lat = settings.child("weather_lat").value?.toString()?.toDoubleOrNull()

        val lon = settings.child("weather_lon").value?.toString()?.toDoubleOrNull()

        val city = settings.readString("weather_city")

        var changed = false

        if (lat != null && lon != null && (lat != weatherLat || lon != weatherLon)) {
            weatherLat = lat
            weatherLon = lon
            changed = true
        }

        if (city != null && city != weatherCity) {
            weatherCity = city
            weatherCityView.text = city
        }

        if (changed) {
            fetchWeather()
        }
    }

    // =========================
    // Favorites / custom order
    // =========================

    private fun channelKey(channel: Channel): String =
        channel.group.lowercase(Locale.US) + "|" + channel.name

    private fun isFavorite(channel: Channel): Boolean =
        favorites.contains(channelKey(channel))

    private fun isVodChannel(channel: Channel): Boolean =
        channel.url.isNotBlank() &&
                detectKind(parseSource(channel.url).url) == StreamKind.MP4

    private fun accentOfPackage(pkg: PackageItem): Accent {

        if (pkg.id == FAV_ID) {
            return Accent(Color.rgb(250, 204, 21), Color.rgb(249, 115, 22))
        }

        val i = basePackages.indexOfFirst { it.id == pkg.id }

        return accentFor(if (i < 0) 0 else i)
    }

    private fun realPackageName(channel: Channel): String =
        packages
            .firstOrNull { it.id != FAV_ID && belongs(channel, it) }
            ?.name
            ?: channel.group

    // builds the displayed packages list: favorites first, then the user's order
    private fun rebuildPackages() {

        val saved =
            (prefs.getString("order_pkgs", "") ?: "")
                .split("\n")
                .filter { it.isNotBlank() }

        val idx = saved.withIndex().associate { it.value to it.index }

        val ordered = basePackages.sortedBy { idx[it.id] ?: Int.MAX_VALUE }

        val hasFavorites = channels.any { isFavorite(it) }

        packages =
            if (hasFavorites) {
                listOf(PackageItem(FAV_ID, "المفضلة", "", true, -1)) + ordered
            } else {
                ordered
            }
    }

    private fun loadChannelOrder(pkgId: String): List<String> =
        (prefs.getString("order_ch_$pkgId", "") ?: "")
            .split("\n")
            .filter { it.isNotBlank() }

    private fun saveChannelOrder(pkgId: String, list: List<Channel>) {
        prefs.edit()
            .putString("order_ch_$pkgId", list.joinToString("\n") { channelKey(it) })
            .apply()
    }

    private fun toggleFavorite(channel: Channel) {

        val key = channelKey(channel)

        val added =
            if (favorites.contains(key)) {
                favorites.remove(key)
                false
            } else {
                favorites.add(key)
                true
            }

        prefs.edit().putStringSet("favorites", HashSet(favorites)).apply()

        rebuildPackages()

        renderPackages()

        if (selectedPackage == FAV_ID) {

            if (packages.none { it.id == FAV_ID }) {

                val first = packages.firstOrNull()

                if (first != null) {
                    selectPackage(first.id, true)
                } else {
                    selectedPackage = ""
                    renderChannels()
                }

            } else {
                renderChannels(true)
            }
        }

        if (currentFocus == null && !isFullscreen) {
            focusSelectedPackage()
        }

        Toast.makeText(
            this,
            if (added) "★ تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة",
            Toast.LENGTH_SHORT
        ).show()
    }

    // resume the last watched channel automatically
    private fun playLastChannel() {

        val key = prefs.getString("last_channel", null) ?: return

        val channel =
            channels.firstOrNull { channelKey(it) == key && it.url.isNotBlank() }
                ?: return

        val pkg =
            packages.firstOrNull { it.id != FAV_ID && belongs(channel, it) }

        if (pkg != null) {
            selectPackage(pkg.id, true)
        }

        playChannel(channel)
    }

    // =========================
    // Reorder mode
    // =========================

    private fun startMove(isPackage: Boolean, id: String) {

        moveState = MoveState(isPackage, id)

        refreshMovingVisual()

        Toast.makeText(
            this,
            "وضع النقل: أعلى / أسفل للتحريك  •  OK للتثبيت",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun endMove() {

        moveState = null

        refreshMovingVisual()

        Toast.makeText(this, "تم حفظ الترتيب", Toast.LENGTH_SHORT).show()
    }

    private fun refreshMovingVisual() {

        val s = moveState

        packageRows.forEach {
            it.card.moving = s != null && s.isPackage && s.id == it.item.id
        }

        channelRows.forEach {
            it.card.moving = s != null && !s.isPackage && s.id == channelKey(it.channel)
        }
    }

    private fun handleMoveKey(event: KeyEvent): Boolean {

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    moveStep(-1)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    moveStep(1)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    endMove()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun moveStep(delta: Int) {

        val s = moveState ?: return

        if (s.isPackage) {

            val list = packages.filter { it.id != FAV_ID }.toMutableList()

            val i = list.indexOfFirst { it.id == s.id }

            val j = i + delta

            if (i < 0 || j < 0 || j >= list.size) {
                return
            }

            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp

            prefs.edit()
                .putString("order_pkgs", list.joinToString("\n") { it.id })
                .apply()

            rebuildPackages()

            renderPackages()

            packageRows.firstOrNull { it.item.id == s.id }?.card?.requestFocus()

        } else {

            val list = visibleChannels.toMutableList()

            val i = list.indexOfFirst { channelKey(it) == s.id }

            val j = i + delta

            if (i < 0 || j < 0 || j >= list.size) {
                return
            }

            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp

            saveChannelOrder(selectedPackage, list)

            renderChannels(true)

            val pc = playingChannel

            if (pc != null && visibleChannels.contains(pc)) {
                playingList = visibleChannels
            }

            channelRows
                .firstOrNull { channelKey(it.channel) == s.id }
                ?.card
                ?.requestFocus()
        }

        refreshMovingVisual()
    }

    // =========================
    // Long-press menu
    // =========================

    private fun menuOpen(): Boolean =
        ::menuOverlay.isInitialized && menuOverlay.visibility == View.VISIBLE

    private fun buildMenuOverlay() {

        menuOverlay = FrameLayout(this)

        menuOverlay.setBackgroundColor(Color.argb(175, 4, 6, 18))

        menuOverlay.isClickable = true

        menuOverlay.visibility = View.GONE

        val box = LinearLayout(this)

        box.orientation = LinearLayout.VERTICAL

        box.setPadding(dp(18), dp(18), dp(18), dp(16))

        box.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(24, 20, 60), Color.rgb(10, 14, 34))
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), withAlpha(cyan, 120))
            }

        menuTitle = label("", 18f, white, true, Gravity.CENTER, 2)

        menuList = LinearLayout(this)

        menuList.orientation = LinearLayout.VERTICAL

        box.addView(menuTitle, lp(matchParent, wrap))

        box.addView(
            menuList,
            lp(matchParent, wrap).apply { topMargin = dp(12) }
        )

        menuOverlay.addView(
            box,
            FrameLayout.LayoutParams(dp(380), wrap, Gravity.CENTER)
        )

        root.addView(menuOverlay, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private fun menuItem(text: String, accent: Accent): TextView {

        val tv = label(text, 16f, white, true, Gravity.CENTER)

        tv.setPadding(dp(16), 0, dp(16), 0)

        tv.isFocusable = true

        tv.isFocusableInTouchMode = true

        fun style(focused: Boolean) {

            tv.background =
                if (focused) {
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(
                            mix(accent.start, bgMid, 0.62f),
                            mix(accent.end, bgMid, 0.62f)
                        )
                    ).apply {
                        cornerRadius = dp(16).toFloat()
                        setStroke(dp(2), Color.WHITE)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(Color.argb(24, 255, 255, 255))
                        setStroke(dp(1), Color.argb(30, 255, 255, 255))
                    }
                }
        }

        style(false)

        tv.setOnFocusChangeListener { _, focused -> style(focused) }

        return tv
    }

    private fun showMenu(title: String, options: List<Pair<String, () -> Unit>>) {

        menuReturnFocus = currentFocus

        menuTitle.text = title

        menuList.removeAllViews()

        var first: View? = null

        options.forEachIndexed { i, option ->

            val item = menuItem(option.first, accentFor(i))

            item.setOnClickListener {
                closeMenu()
                option.second()
            }

            menuList.addView(
                item,
                lp(matchParent, dp(54)).apply { topMargin = dp(6) }
            )

            if (first == null) {
                first = item
            }
        }

        // keep the D-pad focus inside the menu
        normalScreen.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        menuOverlay.visibility = View.VISIBLE

        first?.requestFocus()
    }

    private fun closeMenu() {

        menuOverlay.visibility = View.GONE

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        menuReturnFocus?.requestFocus()

        menuReturnFocus = null
    }

    private fun showChannelMenu(channel: Channel) {

        if (isFullscreen || moveState != null) {
            return
        }

        val fav = isFavorite(channel)

        showMenu(
            channel.name,
            listOf(
                (if (fav) "★  إزالة من المفضلة" else "☆  إضافة إلى المفضلة") to
                        { toggleFavorite(channel) },
                "↕  نقل القناة" to
                        { startMove(false, channelKey(channel)) },
                "إلغاء" to {}
            )
        )
    }

    private fun showPackageMenu(item: PackageItem) {

        if (isFullscreen || moveState != null) {
            return
        }

        showMenu(
            item.name,
            listOf(
                "↕  نقل الباقة" to { startMove(true, item.id) },
                "إلغاء" to {}
            )
        )
    }

    // =========================
    // Splash screen
    // =========================

    private val splashRunnable = object : Runnable {
        override fun run() {

            val elapsed = SystemClock.elapsedRealtime() - splashStart

            if (dataReady || elapsed >= 6000) {
                hideSplash()
            } else {
                mainHandler.postDelayed(this, 200)
            }
        }
    }

    private fun hideSplash() {

        if (!splashVisible) {
            return
        }

        splashVisible = false

        splash.animate()
            .alpha(0f)
            .setDuration(380)
            .withEndAction {
                splash.visibility = View.GONE
            }
            .start()
    }

    private fun buildSplash() {

        splash = FrameLayout(this)

        splash.isClickable = true

        splash.addView(
            AmbientBackground(),
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        val col = LinearLayout(this)

        col.orientation = LinearLayout.VERTICAL

        col.gravity = Gravity.CENTER_HORIZONTAL

        val mark = TextView(this)

        mark.text = "N"
        mark.textSize = 56f
        mark.setTextColor(white)
        mark.typeface = Typeface.DEFAULT_BOLD
        mark.gravity = Gravity.CENTER

        mark.background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(cyan, violet)
            ).apply {
                cornerRadius = dp(34).toFloat()
            }

        col.addView(mark, lp(dp(116), dp(116)))

        val brand = label("NIYATI", 34f, white, true, Gravity.CENTER)

        brand.letterSpacing = 0.22f

        col.addView(
            brand,
            lp(wrap, wrap).apply { topMargin = dp(22) }
        )

        val sport = label("SPORTS IPTV", 13f, cyan, true, Gravity.CENTER)

        sport.letterSpacing = 0.4f

        col.addView(sport, lp(wrap, wrap))

        val track = FrameLayout(this)

        track.background =
            GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(Color.argb(40, 255, 255, 255))
            }

        val thumb = View(this)

        thumb.background =
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(cyan, violet)
            ).apply {
                cornerRadius = dp(2).toFloat()
            }

        track.addView(thumb, FrameLayout.LayoutParams(dp(60), dp(4)))

        col.addView(
            track,
            lp(dp(180), dp(4)).apply { topMargin = dp(38) }
        )

        splash.addView(col, FrameLayout.LayoutParams(wrap, wrap, Gravity.CENTER))

        root.addView(splash, FrameLayout.LayoutParams(matchParent, matchParent))

        // animations
        mark.startAnimation(
            ScaleAnimation(
                0.94f, 1.06f, 0.94f, 1.06f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 900
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
        )

        thumb.startAnimation(
            TranslateAnimation(
                -dp(60).toFloat(),
                dp(180).toFloat(),
                0f,
                0f
            ).apply {
                duration = 1100
                repeatCount = Animation.INFINITE
            }
        )

        col.alpha = 0f

        col.translationY = dp(18).toFloat()

        col.animate().alpha(1f).translationY(0f).setDuration(650).start()
    }

    // =========================
    // Focus helpers
    // =========================

    private fun columnOf(view: View?): Int {

        var v: View? = view

        while (v != null) {

            if (v === packagesList) {
                return 0
            }

            if (v === channelsList) {
                return 1
            }

            if (v === playerFrame) {
                return 2
            }

            v = v.parent as? View
        }

        return -1
    }

    private fun focusSelectedPackage() {

        val row =
            packageRows.firstOrNull { it.item.id == selectedPackage }
                ?: packageRows.firstOrNull()

        row?.card?.requestFocus()
    }

    private fun focusChannelsColumn() {

        val target =
            channelRows.firstOrNull { it.channel == lastFocusedChannel }
                ?: channelRows.firstOrNull { it.channel == playingChannel }
                ?: channelRows.firstOrNull()

        if (target != null) {
            target.card.requestFocus()
        } else {
            focusSelectedPackage()
        }
    }

    // =========================
    // D-PAD
    // =========================

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        // splash screen swallows keys
        if (splashVisible) {
            return true
        }

        // long-press menu
        if (menuOpen()) {

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    closeMenu()
                }
                return true
            }

            return super.dispatchKeyEvent(event)
        }

        // reorder mode
        if (moveState != null) {
            return handleMoveKey(event)
        }

        if (isFullscreen) {
            return handleFullscreenKey(event)
        }

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        val key = event.keyCode

        // yellow button = favorite
        if (key == KeyEvent.KEYCODE_PROG_YELLOW) {

            val target =
                if (columnOf(currentFocus) == 1) lastFocusedChannel else playingChannel

            if (target != null) {
                toggleFavorite(target)
            }

            return true
        }

        val isDpad =
            key == KeyEvent.KEYCODE_DPAD_LEFT ||
                    key == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    key == KeyEvent.KEYCODE_DPAD_UP ||
                    key == KeyEvent.KEYCODE_DPAD_DOWN

        if (!isDpad) {
            return super.dispatchKeyEvent(event)
        }

        val column = columnOf(currentFocus)

        if (column == -1) {

            if (packageRows.isNotEmpty()) {
                focusSelectedPackage()
                return true
            }

            return super.dispatchKeyEvent(event)
        }

        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {

            if (column == 0) {

                flushPendingSelect()

                if (channelRows.isEmpty()) {
                    playerFrame.requestFocus()
                } else {
                    focusChannelsColumn()
                }

                return true
            }

            if (column == 1) {
                playerFrame.requestFocus()
                return true
            }

            return true
        }

        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {

            if (column == 2) {
                focusChannelsColumn()
                return true
            }

            if (column == 1) {
                focusSelectedPackage()
                return true
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun handleFullscreenKey(event: KeyEvent): Boolean {

        if (!playingWeb && playerView.isControllerFullyVisible) {
            return super.dispatchKeyEvent(event)
        }

        if (event.keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                playingChannel?.let { toggleFavorite(it) }
            }

            return true
        }

        val delta =
            when (event.keyCode) {

                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_CHANNEL_DOWN -> -1

                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_CHANNEL_UP -> 1

                else -> 0
            }

        if (delta != 0) {

            if (
                event.action == KeyEvent.ACTION_DOWN &&
                event.repeatCount == 0
            ) {
                zap(delta)
            }

            return true
        }

        if (
            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER
        ) {

            // web mode: let the page handle OK (play / pause)
            if (playingWeb) {
                return super.dispatchKeyEvent(event)
            }

            if (event.action == KeyEvent.ACTION_DOWN) {
                playerView.showController()
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {

        if (isFullscreen) {
            exitFullscreen()
            return
        }

        when (columnOf(currentFocus)) {

            2 -> {
                focusChannelsColumn()
                return
            }

            1 -> {
                focusSelectedPackage()
                return
            }
        }

        val now = System.currentTimeMillis()

        if (now - backPressedAt < 2000) {

            super.onBackPressed()

        } else {

            backPressedAt = now

            Toast.makeText(
                this,
                "\u0627\u0636\u063a\u0637 \u0631\u062c\u0648\u0639 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649 \u0644\u0644\u062e\u0631\u0648\u062c",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // Fullscreen
    // =========================

    private fun enterFullscreen() {

        if (isFullscreen) {
            return
        }

        isFullscreen = true

        moveVideoTo(fullscreenContainer)

        normalScreen.visibility = View.GONE

        fullscreenContainer.visibility = View.VISIBLE

        playerView.useController = !playingWeb

        playerView.controllerShowTimeoutMs = 4000

        if (playingWeb) {

            playerView.isFocusable = false

            webView.isFocusable = true

            webView.isFocusableInTouchMode = true

            webView.requestFocus()

        } else {

            playerView.isFocusable = true

            playerView.isFocusableInTouchMode = true

            playerView.requestFocus()
        }

        hideSystemBars()

        playingChannel?.let {
            showZapBanner(it)
        }
    }

    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        isFullscreen = false

        playerView.hideController()

        playerView.useController = false

        playerView.isFocusable = false

        webView.isFocusable = false

        webView.isFocusableInTouchMode = false

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        moveVideoTo(playerFrame)

        fullscreenContainer.visibility = View.GONE

        normalScreen.visibility = View.VISIBLE

        showSystemBars()

        playerFrame.requestFocus()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.let {

                it.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.show(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )

        } else {

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // =========================
    // Logo download
    // =========================

    private fun downloadBitmap(urlString: String): Bitmap? {

        var connection: HttpURLConnection? = null

        return try {

            connection = URL(urlString).openConnection() as HttpURLConnection

            connection.connectTimeout = 6000

            connection.readTimeout = 8000

            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                return null
            }

            val bytes = connection.inputStream.use { it.readBytes() }

            val bounds =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sample = 1

            while (
                bounds.outWidth / sample > 256 ||
                bounds.outHeight / sample > 256
            ) {
                sample *= 2
            }

            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        } catch (_: Exception) {

            null

        } finally {

            connection?.disconnect()
        }
    }

    // =========================
    // Lifecycle
    // =========================

    override fun onStart() {

        super.onStart()

        if (::webView.isInitialized) {
            webView.onResume()
        }

        if (resumeOnStart && ::player.isInitialized && !playingWeb) {

            if (player.isCurrentMediaItemLive) {
                player.seekToDefaultPosition()
            }

            player.playWhenReady = true
        }
    }

    override fun onStop() {

        super.onStop()

        if (::webView.isInitialized) {
            webView.onPause()
        }

        if (::player.isInitialized) {

            resumeOnStart =
                player.playWhenReady &&
                        player.playbackState != Player.STATE_IDLE

            player.pause()
        }
    }

    override fun onDestroy() {

        mainHandler.removeCallbacksAndMessages(null)

        dataListener?.let {
            rootRef.removeEventListener(it)
        }

        ioExecutor.shutdownNow()

        if (::webView.isInitialized) {

            (webView.parent as? ViewGroup)?.removeView(webView)

            webView.stopLoading()

            webView.destroy()
        }

        if (::player.isInitialized) {
            player.release()
        }

        super.onDestroy()
    }
}

// =========================
// Arabic texts
// =========================

private object Txt {

    const val DB_ERROR = "\u062a\u0639\u0630\u0631 \u0627\u0644\u0627\u062a\u0635\u0627\u0644 \u0628\u0642\u0627\u0639\u062f\u0629 \u0627\u0644\u0628\u064a\u0627\u0646\u0627\u062a"

    const val ERR_SUB = "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0631\u0627\u0628\u0637 \u0627\u0644\u0628\u062b \u0623\u0648 \u0645\u0646 \u0627\u062a\u0635\u0627\u0644 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a"

    const val ERR_TITLE = "\u062a\u0639\u0630\u0631 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629"

    const val HINT = "اضغط OK لملء الشاشة  •  اضغط مطولاً على القناة للمفضلة والنقل"

    const val IDLE_SUB = "\u062a\u0646\u0642\u0651\u0644 \u0628\u0627\u0644\u0623\u0633\u0647\u0645 \u0628\u064a\u0646 \u0627\u0644\u0628\u0627\u0642\u0627\u062a \u0648\u0627\u0644\u0642\u0646\u0648\u0627\u062a"

    const val IDLE_TITLE = "\u0627\u062e\u062a\u0631 \u0642\u0646\u0627\u0629 \u0644\u0644\u0645\u0634\u0627\u0647\u062f\u0629"

    const val LOAD_FAIL_SUB = "\u0633\u064a\u062a\u0645 \u0627\u0644\u062a\u062d\u062f\u064a\u062b \u062a\u0644\u0642\u0627\u0626\u064a\u0627\u064b \u0639\u0646\u062f \u0639\u0648\u062f\u0629 \u0627\u0644\u0627\u062a\u0635\u0627\u0644"

    const val LOAD_FAIL_TITLE = "\u062a\u0639\u0630\u0631 \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0628\u064a\u0627\u0646\u0627\u062a"

    const val NO_CHANNELS = "\u0644\u0627 \u062a\u0648\u062c\u062f \u0642\u0646\u0648\u0627\u062a"

    const val NO_CHANNELS_SUB = "\u0644\u0647\u0630\u0647 \u0627\u0644\u0628\u0627\u0642\u0629 \u062d\u0627\u0644\u064a\u0627\u064b"

    const val NO_URL = "\u0647\u0630\u0647 \u0627\u0644\u0642\u0646\u0627\u0629 \u0644\u0627 \u062a\u062d\u062a\u0648\u064a \u0639\u0644\u0649 \u0631\u0627\u0628\u0637 \u0628\u062b \u062d\u0627\u0644\u064a\u0627\u064b"
}
