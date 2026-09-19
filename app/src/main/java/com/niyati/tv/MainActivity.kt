package com.niyati.tv

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
import android.text.TextUtils
import android.util.LruCache
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    private lateinit var zapOverlay: TextView

    // =========================
    // Data
    // =========================

    data class Channel(
        val name: String,
        val group: String,
        val url: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    data class Accent(val start: Int, val end: Int)

    private class PackageRow(val item: PackageItem, val card: TvCard)

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
    private var isFullscreen = false
    private var resumeOnStart = false
    private var retryCount = 0
    private var backPressedAt = 0L

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
        Accent(Color.rgb(0, 229, 255), Color.rgb(41, 121, 255)),     // cyan -> blue
        Accent(Color.rgb(192, 132, 252), Color.rgb(236, 72, 153)),   // violet -> pink
        Accent(Color.rgb(255, 183, 77), Color.rgb(244, 63, 94)),     // amber -> red
        Accent(Color.rgb(52, 211, 153), Color.rgb(6, 182, 212)),     // green -> teal
        Accent(Color.rgb(250, 204, 21), Color.rgb(249, 115, 22)),    // yellow -> orange
        Accent(Color.rgb(129, 140, 248), Color.rgb(56, 189, 248))    // indigo -> sky
    )

    private fun accentFor(index: Int): Accent =
        palette[((index % palette.size) + palette.size) % palette.size]

    // =========================
    // Helpers
    // =========================

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(3)

    private val imageCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
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

    private fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
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
        if (bold) tv.typeface = Typeface.DEFAULT_BOLD
        return tv
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

        database = FirebaseDatabase.getInstance(firebaseUrl)
        rootRef = database.reference

        buildInterface()

        startFirebase()
    }

    // =========================
    // Player setup
    // =========================

    private val retryRunnable = Runnable {
        player.prepare()
        player.playWhenReady = true
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    retryCount = 0
                    hideStatus()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlayerError()
            }
        })
    }

    private fun handlePlayerError() {
        val channel = playingChannel ?: return

        if (retryCount < 2) {
            retryCount++
            showStatus("جاري إعادة المحاولة…", channel.name, false)
            mainHandler.removeCallbacks(retryRunnable)
            mainHandler.postDelayed(retryRunnable, 2000)
        } else {
            showStatus(
‎                "تعذر تشغيل القناة",
‎                "تحقق من رابط البث أو من اتصال الإنترنت",
                true
            )
            livePill.visibility = View.GONE
        }
    }

    // =========================
    // Build interface
    // =========================

    private fun buildInterface() {

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

        packagesHeader = SectionHeader("الباقات", "PACKAGES", accentFor(0))
        packagesPanel.addView(packagesHeader, lp(matchParent, dp(64)))

        val packageScroll = createScroll()
        packagesList = createList()
        packageScroll.addView(packagesList)
        packagesPanel.addView(packageScroll, lp(matchParent, 0, 1f))

        contentRow.addView(packagesPanel, panelParams(0.23f))

        // ---- Channels ----

        val channelsPanel = createPanel()

        channelsHeader = SectionHeader("القنوات", "CHANNELS", accentFor(1))
        channelsPanel.addView(channelsHeader, lp(matchParent, dp(64)))

        channelScroll = createScroll()
        channelsList = createList()
        channelScroll.addView(channelsList)
        channelsPanel.addView(channelScroll, lp(matchParent, 0, 1f))

        contentRow.addView(channelsPanel, panelParams(0.30f))

        // ---- Player ----

        val playerPanel = createPanel()

        playerPanel.addView(
            SectionHeader("المشغل", "PLAYER", accentFor(2), false),
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
        playerFrame.setOnClickListener { enterFullscreen() }

        playerPanel.addView(
            playerFrame,
            lp(matchParent, 0, 1f).apply { setMargins(dp(6), dp(4), dp(6), 0) }
        )

        playerPanel.addView(
            buildInfoBar(),
            lp(matchParent, dp(60)).apply { setMargins(dp(6), dp(8), dp(6), 0) }
        )

        val hint = label(
‎            "اضغط OK لملء الشاشة  •  أعلى / أسفل لتبديل القناة",
            11f,
            gray,
            false,
            Gravity.CENTER
        )
        playerPanel.addView(hint, lp(matchParent, dp(30)))

        contentRow.addView(playerPanel, panelParams(0.47f))

        // ---- Video + status overlay ----

        playerView = PlayerView(this)
        playerView.useController = false
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        playerView.setBackgroundColor(Color.BLACK)
        playerView.isFocusable = false
        playerView.player = player

        buildStatusOverlay()
        moveVideoTo(playerFrame)
        showStatus(
‎            "اختر قناة للمشاهدة",
‎            "تنقّل بالأسهم بين الباقات والقنوات",
            false
        )

        // ---- Fullscreen ----

        fullscreenContainer = FrameLayout(this)
        fullscreenContainer.setBackgroundColor(Color.BLACK)
        fullscreenContainer.visibility = View.GONE

        zapOverlay = TextView(this)
        zapOverlay.textSize = 20f
        zapOverlay.setTextColor(white)
        zapOverlay.typeface = Typeface.DEFAULT_BOLD
        zapOverlay.setPadding(dp(20), dp(10), dp(20), dp(10))
        zapOverlay.visibility = View.GONE
        zapOverlay.background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Color.argb(190, 8, 10, 28))
            setStroke(dp(1), withAlpha(cyan, 140))
        }
        fullscreenContainer.addView(
            zapOverlay,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.RIGHT)
                .apply { setMargins(dp(40), dp(32), dp(40), 0) }
        )

        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        setContentView(root)
    }

    private fun panelParams(weight: Float): LinearLayout.LayoutParams =
        lp(0, matchParent, weight).apply { setMargins(dp(7), 0, dp(7), 0) }

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
        mark.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(cyan, violet)
        ).apply { cornerRadius = dp(14).toFloat() }
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

        val clock = TextClock(this)
        clock.format12Hour = "hh:mm a"
        clock.format24Hour = "hh:mm a"
        clock.textSize = 20f
        clock.setTextColor(white)
        clock.typeface = Typeface.DEFAULT_BOLD
        header.addView(
            clock,
            lp(wrap, wrap).apply { setMargins(dp(18), 0, 0, 0) }
        )

        return header
    }

    private fun createLivePill(): LinearLayout {

        val pill = LinearLayout(this)
        pill.orientation = LinearLayout.HORIZONTAL
        pill.gravity = Gravity.CENTER_VERTICAL
        pill.setPadding(dp(10), dp(4), dp(12), dp(4))
        pill.background = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(Color.argb(46, 255, 59, 92))
            setStroke(dp(1), Color.argb(150, 255, 59, 92))
        }

        val dot = View(this)
        dot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.rgb(255, 59, 92))
        }
        val blink = AlphaAnimation(1f, 0.2f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        dot.startAnimation(blink)
        pill.addView(dot, lp(dp(8), dp(8)))

        val text = label("LIVE", 11f, white, true, Gravity.CENTER)
        text.letterSpacing = 0.12f
        pill.addView(text, lp(wrap, wrap).apply { setMargins(dp(7), 0, 0, 0) })

        return pill
    }

    private fun buildInfoBar(): LinearLayout {

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(14), 0, dp(14), 0)
        bar.background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.argb(22, 255, 255, 255))
            setStroke(dp(1), Color.argb(26, 255, 255, 255))
        }

        livePill = createLivePill()
        livePill.visibility = View.GONE
        bar.addView(livePill, lp(wrap, wrap))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_VERTICAL

        nowTitle = label("لم يتم اختيار قناة", 16f, white, true)
        nowSub = label("اختر قناة من القائمة", 12f, gray)

        col.addView(nowTitle, lp(matchParent, wrap))
        col.addView(nowSub, lp(matchParent, wrap))

        bar.addView(col, lp(0, wrap, 1f))

        return bar
    }

    private fun buildStatusOverlay() {

        statusOverlay = LinearLayout(this)
        statusOverlay.orientation = LinearLayout.VERTICAL
        statusOverlay.gravity = Gravity.CENTER
        statusOverlay.setPadding(dp(16), dp(16), dp(16), dp(16))
        statusOverlay.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(12, 16, 38), Color.rgb(7, 9, 22))
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
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
                .apply { leftMargin = dp(4) }
        )
        statusIcon.addView(
            statusBang,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        statusOverlay.addView(statusIcon, lp(dp(58), dp(58)))

        statusTitle = label("", 17f, white, true, Gravity.CENTER, 2)
        statusSub = label("", 12f, gray, false, Gravity.CENTER, 3)

        statusOverlay.addView(
            statusTitle,
            lp(matchParent, wrap).apply { topMargin = dp(14) }
        )
        statusOverlay.addView(
            statusSub,
            lp(matchParent, wrap).apply { topMargin = dp(4) }
        )
    }

    private fun showStatus(title: String, sub: String, error: Boolean) {
        statusTitle.text = title
        statusSub.text = sub

        val colors = if (error) {
            intArrayOf(Color.rgb(255, 82, 82), Color.rgb(244, 63, 94))
        } else {
            intArrayOf(cyan, violet)
        }
        statusIcon.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        ).apply { shape = GradientDrawable.OVAL }

        statusPlay.visibility = if (error) View.GONE else View.VISIBLE
        statusBang.visibility = if (error) View.VISIBLE else View.GONE

        statusOverlay.visibility = View.VISIBLE
    }

    private fun hideStatus() {
        statusOverlay.visibility = View.GONE
    }

    private fun moveVideoTo(container: FrameLayout) {
        (playerView.parent as? ViewGroup)?.removeView(playerView)
        (statusOverlay.parent as? ViewGroup)?.removeView(statusOverlay)

        container.addView(
            playerView,
            0,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )
        container.addView(
            statusOverlay,
            1,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )
    }

    private fun applyPlayerFrameBackground(focused: Boolean) {
        playerFrame.background = GradientDrawable().apply {
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
        panel.background = GradientDrawable(
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

    /** Dark gradient with two soft colored glows. */
    private inner class AmbientBackground : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var base: Shader? = null
        private var glowA: Shader? = null
        private var glowB: Shader? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            val fw = w.toFloat()
            val fh = h.toFloat()

            base = LinearGradient(
                0f, 0f, fw, fh,
                intArrayOf(bgTop, bgMid, bgBottom),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )

            glowA = RadialGradient(
                fw * 0.88f, fh * 0.08f, fw * 0.55f,
                intArrayOf(Color.argb(95, 168, 85, 247), Color.argb(0, 168, 85, 247)),
                null,
                Shader.TileMode.CLAMP
            )

            glowB = RadialGradient(
                fw * 0.08f, fh * 0.98f, fw * 0.5f,
                intArrayOf(Color.argb(80, 0, 229, 255), Color.argb(0, 0, 229, 255)),
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

    /** Small filled triangle used as the "play" glyph. */
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

    /** Section title: Arabic title, small subtitle, count chip and accent bar. */
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
            countChip.background = GradientDrawable().apply {
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
            bar.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(accent.start, accent.end)
            ).apply { cornerRadius = dp(2).toFloat() }
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

    /** Rounded badge that shows the logo, or the first letter until it loads. */
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
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(accent.start, accent.end)
            ).apply { cornerRadius = radius }
            image.visibility = View.GONE
            letter.visibility = View.VISIBLE
        }

        private fun showImage(bitmap: Bitmap) {
            image.setImageBitmap(bitmap)
            image.visibility = View.VISIBLE
            letter.visibility = View.GONE
            background = GradientDrawable().apply {
                cornerRadius = radius
                setColor(Color.argb(242, 250, 250, 255))
            }
        }

        fun bind(name: String, url: String) {
            currentUrl = url
            letter.text = name.trim().take(1).uppercase()
            showFallback()

            if (!url.startsWith("http", ignoreCase = true)) return

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
                            if (currentUrl == url) showImage(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                // executor already shut down
            }
        }
    }

    /**
     * Base card for packages and channels.
     * Normal: glass. Chosen: tinted with accent. Focused: bright accent gradient + white border + scale.
     */
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
            texts.addView(nameView, LinearLayout.LayoutParams(matchParent, wrap))
            texts.addView(
                subView,
                LinearLayout.LayoutParams(matchParent, wrap).apply { topMargin = dp(2) }
            )
            addView(texts, LinearLayout.LayoutParams(0, matchParent, 1f))

            addView(badge, LinearLayout.LayoutParams(dp(badgeSizeDp), dp(badgeSizeDp)))

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
            animate().scaleX(scale).scaleY(scale).setDuration(130).start()

            refresh()

            if (gainFocus) onFocused?.invoke()
        }

        private fun refresh() {
            val d: GradientDrawable

            if (focusedNow) {
                d = GradientDrawable(
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

        val accent = accentFor(index)
        val card = TvCard(accent, 44, 18)

        val count = channels.count { belongs(it, item) }

        card.nameView.text = item.name
        card.subView.text = "$count قناة"
        card.badge.bind(item.name, item.logo)

        val dot = View(this)
        dot.background = GradientDrawable().apply {
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
            (dot.background as GradientDrawable)
                .setColor(if (focused) Color.WHITE else accent.start)
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

        card.nameView.text = channel.name
        card.badge.bind(channel.name, channel.logo)

        val numberView = label(
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

            card.subView.text = when {
                !hasUrl -> "غير متاح حالياً"
                playing -> "يعرض الآن"
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

        card.stateListener = { _, _ -> applyState() }
        applyState()

        card.onFocused = { lastFocusedChannel = channel }

        card.setOnClickListener {
            if (playingChannel == channel && player.playbackState != Player.STATE_IDLE) {
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
            showListMessage(packagesList, "لا يوجد اتصال", "تحقق من الإنترنت")
            showListMessage(
                channelsList,
‎                "تعذر تحميل البيانات",
‎                "سيتم التحديث تلقائياً عند عودة الاتصال"
            )
        }
    }

    private fun startFirebase() {

        showListMessage(packagesList, "جاري التحميل", "يرجى الانتظار")
        showListMessage(channelsList, "جاري تحميل القنوات", "يرجى الانتظار")

        mainHandler.postDelayed(loadTimeout, 12000)

        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                onDataLoaded(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                mainHandler.removeCallbacks(loadTimeout)

                Toast.makeText(
                    this@MainActivity,
‎                    "تعذر الاتصال بقاعدة البيانات",
                    Toast.LENGTH_LONG
                ).show()

                if (!dataReady) {
                    showListMessage(packagesList, "تعذر الاتصال", "تحقق من الإعدادات")
                    showListMessage(channelsList, "تعذر الاتصال", "بقاعدة البيانات")
                }
            }
        }

        dataListener = listener
        rootRef.addValueEventListener(listener)
    }

    private fun onDataLoaded(snapshot: DataSnapshot) {

        mainHandler.removeCallbacks(loadTimeout)

        val newChannels = parseChannels(snapshot)
        var newPackages = parsePackages(snapshot)

        // If no packages node exists, build packages from channel groups.
        if (newPackages.isEmpty() && newChannels.isNotEmpty()) {
            newPackages = newChannels
                .map { it.group }
                .distinct()
                .mapIndexed { i, g -> PackageItem(g, g, "", true, i) }
        }

        // Nothing changed -> don't rebuild (keeps focus and scroll position).
        if (dataReady && newPackages == packages && newChannels == channels) return

        dataReady = true
        packages = newPackages
        channels = newChannels

        renderPackages()

        val keep = packages.firstOrNull { it.id == selectedPackage }
            ?: packages.firstOrNull()

        if (keep != null) {
            selectPackage(keep.id, true)
        } else {
            selectedPackage = ""
            renderChannels()
        }

        if (currentFocus == null && !isFullscreen) {
            focusSelectedPackage()
        }
    }

    private fun DataSnapshot.readString(key: String): String? =
        child(key).value?.toString()?.trim()?.takeIf { it.isNotEmpty() }

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

            if (!child.readBool("enabled", true)) continue

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

            if (!child.readBool("enabled", true)) continue

            result.add(
                Channel(
                    name = name,
                    group = group,
                    url = child.readString("url") ?: "",
                    logo = child.readString("logo") ?: "",
                    enabled = true,
                    order = child.readInt("order", 999)
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun belongs(channel: Channel, pkg: PackageItem): Boolean =
        channel.group.equals(pkg.id, ignoreCase = true) ||
                channel.group.equals(pkg.name, ignoreCase = true)

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
            lp(matchParent, wrap).apply { topMargin = dp(6) }
        )

        list.addView(box, lp(matchParent, wrap))
    }

    private fun renderPackages() {

        val focusedId = packageRows.firstOrNull { it.card.hasFocus() }?.item?.id

        packagesList.removeAllViews()
        packageRows.clear()

        packages.forEachIndexed { index, item ->

            val card = createPackageCard(item, index)
            card.chosen = item.id == selectedPackage

            card.onFocused = { scheduleSelect(item.id) }

            card.setOnClickListener {
                mainHandler.removeCallbacks(selectRunnable)
                pendingPackageId = null
                selectPackage(item.id)
                focusChannelsColumn()
            }

            packagesList.addView(
                card,
                lp(matchParent, dp(66)).apply { setMargins(0, dp(5), 0, dp(5)) }
            )

            packageRows.add(PackageRow(item, card))
        }

        packagesHeader.setCount(packages.size)

        if (focusedId != null) {
            packageRows.firstOrNull { it.item.id == focusedId }?.card?.requestFocus()
        }
    }

    private val selectRunnable = Runnable {
        val id = pendingPackageId
        pendingPackageId = null
        if (id != null) selectPackage(id)
    }

    /** Selecting on focus (with a tiny delay) makes browsing packages fast. */
    private fun scheduleSelect(id: String) {
        pendingPackageId = id
        mainHandler.removeCallbacks(selectRunnable)
        mainHandler.postDelayed(selectRunnable, 180)
    }

    private fun flushPendingSelect() {
        mainHandler.removeCallbacks(selectRunnable)
        val id = pendingPackageId
        pendingPackageId = null
        if (id != null) selectPackage(id)
    }

    private fun selectPackage(id: String, force: Boolean = false) {

        if (!force && id == selectedPackage) return

        selectedPackage = id

        packageRows.forEach { it.card.chosen = it.item.id == id }

        renderChannels()
    }

    private fun renderChannels() {

        val restoreIndex = channelRows.indexOfFirst { it.card.hasFocus() }

        channelsList.removeAllViews()
        channelRows.clear()
        lastFocusedChannel = null

        val pkgIndex = packages.indexOfFirst { it.id == selectedPackage }
        val pkg = packages.getOrNull(pkgIndex)

        channelsHeader.setSubtitle(pkg?.name ?: "CHANNELS")

        visibleChannels =
            if (pkg == null) emptyList()
            else channels.filter { belongs(it, pkg) }.sortedBy { it.order }

        channelsHeader.setCount(visibleChannels.size)
        channelScroll.scrollTo(0, 0)

        if (visibleChannels.isEmpty()) {
            showListMessage(
                channelsList,
‎                "لا توجد قنوات",
‎                "لهذه الباقة حالياً"
            )
            return
        }

        val accent = accentFor(pkgIndex)

        visibleChannels.forEachIndexed { i, channel ->

            val row = createChannelRow(channel, i + 1, accent)
            row.setPlaying(channel == playingChannel)

            channelsList.addView(
                row.card,
                lp(matchParent, dp(66)).apply { setMargins(0, dp(5), 0, dp(5)) }
            )

            channelRows.add(row)
        }

        if (restoreIndex >= 0) {
            channelRows
                .getOrNull(minOf(restoreIndex, channelRows.size - 1))
                ?.card
                ?.requestFocus()
        }
    }

    // =========================
    // Playback
    // =========================

    private fun playChannel(channel: Channel) {

        if (channel.url.isBlank()) {
            Toast.makeText(
                this,
‎                "هذه القناة لا تحتوي على رابط بث حالياً",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            retryCount = 0
            mainHandler.removeCallbacks(retryRunnable)

            playingChannel = channel
            playingList = visibleChannels

            updatePlayingMarks()
            updateNowPlaying(channel)

            showStatus("جاري تشغيل القناة…", channel.name, false)

            player.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)))
            player.prepare()
            player.playWhenReady = true

        } catch (e: Exception) {
            showStatus(
‎                "تعذر تشغيل القناة",
‎                "تحقق من رابط البث أو من اتصال الإنترنت",
                true
            )
            livePill.visibility = View.GONE
        }
    }

    private fun updatePlayingMarks() {
        channelRows.forEach { it.setPlaying(it.channel == playingChannel) }
    }

    private fun updateNowPlaying(channel: Channel) {
        nowTitle.text = channel.name
        nowSub.text = packages.firstOrNull { belongs(channel, it) }?.name ?: channel.group
        livePill.visibility = View.VISIBLE
    }

    /** Switch to the previous / next playable channel (used in fullscreen). */
    private fun zap(delta: Int) {

        val list = playingList
        if (list.isEmpty()) return

        var index = list.indexOf(playingChannel)
        if (index < 0) index = if (delta > 0) -1 else 0

        repeat(list.size) {
            index = (index + delta + list.size) % list.size
            val candidate = list[index]

            if (candidate.url.isNotBlank()) {
                lastFocusedChannel = candidate
                playChannel(candidate)
                showZapOverlay(
                    String.format(Locale.US, "%02d", index + 1) + "  " + candidate.name
                )
                return
            }
        }
    }

    private val hideZapRunnable = Runnable { zapOverlay.visibility = View.GONE }

    private fun showZapOverlay(text: String) {
        zapOverlay.text = text
        zapOverlay.visibility = View.VISIBLE
        mainHandler.removeCallbacks(hideZapRunnable)
        mainHandler.postDelayed(hideZapRunnable, 2600)
    }

    // =========================
    // Focus helpers
    // =========================

    private fun columnOf(view: View?): Int {
        var v: View? = view
        while (v != null) {
            if (v === packagesList) return 0
            if (v === channelsList) return 1
            if (v === playerFrame) return 2
            v = v.parent as? View
        }
        return -1
    }

    private fun focusSelectedPackage() {
        val row = packageRows.firstOrNull { it.item.id == selectedPackage }
            ?: packageRows.firstOrNull()
        row?.card?.requestFocus()
    }

    private fun focusChannelsColumn() {
        val target = channelRows.firstOrNull { it.channel == lastFocusedChannel }
            ?: channelRows.firstOrNull { it.channel == playingChannel }
            ?: channelRows.firstOrNull()

        if (target != null) target.card.requestFocus() else focusSelectedPackage()
    }

    // =========================
    // D-PAD
    // =========================

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (isFullscreen) return handleFullscreenKey(event)

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        val key = event.keyCode

        val isDpad = key == KeyEvent.KEYCODE_DPAD_LEFT ||
                key == KeyEvent.KEYCODE_DPAD_RIGHT ||
                key == KeyEvent.KEYCODE_DPAD_UP ||
                key == KeyEvent.KEYCODE_DPAD_DOWN

        if (!isDpad) return super.dispatchKeyEvent(event)

        val column = columnOf(currentFocus)

        // Nothing focused: start from the packages column.
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
                if (channelRows.isEmpty()) playerFrame.requestFocus() else focusChannelsColumn()
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

        // Controller visible: let it handle its own buttons.
        if (playerView.isControllerFullyVisible) {
            return super.dispatchKeyEvent(event)
        }

        val delta = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> -1
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_UP -> 1
            else -> 0
        }

        if (delta != 0) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                zap(delta)
            }
            return true
        }

        if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) playerView.showController()
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

        // Packages column: press back twice to exit.
        val now = System.currentTimeMillis()
        if (now - backPressedAt < 2000) {
            super.onBackPressed()
        } else {
            backPressedAt = now
            Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================
    // Fullscreen
    // =========================

    private fun enterFullscreen() {

        if (isFullscreen) return

        isFullscreen = true

        moveVideoTo(fullscreenContainer)

        normalScreen.visibility = View.GONE
        fullscreenContainer.visibility = View.VISIBLE

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 4000
        playerView.isFocusable = true
        playerView.isFocusableInTouchMode = true
        playerView.requestFocus()

        hideSystemBars()

        playingChannel?.let { showZapOverlay(it.name) }
    }

    private fun exitFullscreen() {

        if (!isFullscreen) return

        isFullscreen = false

        playerView.hideController()
        playerView.useController = false
        playerView.isFocusable = false

        mainHandler.removeCallbacks(hideZapRunnable)
        zapOverlay.visibility = View.GONE

        moveVideoTo(playerFrame)

        fullscreenContainer.visibility = View.GONE
        normalScreen.visibility = View.VISIBLE

        showSystemBars()

        playerFrame.requestFocus()
    }

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

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun showSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.show(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )

        } else {

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // =========================
    // Logo download (no extra libraries needed)
    // =========================

    private fun downloadBitmap(urlString: String): Bitmap? {

        var connection: HttpURLConnection? = null

        return try {
            connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) return null

            val bytes = connection.inputStream.use { it.readBytes() }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sample = 1
            while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) {
                sample *= 2
            }

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        } catch (e: Exception) {
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

        if (resumeOnStart && ::player.isInitialized) {
            if (player.isCurrentMediaItemLive) player.seekToDefaultPosition()
            player.playWhenReady = true
        }
    }

    override fun onStop() {

        super.onStop()

        if (::player.isInitialized) {
            resumeOnStart =
                player.playWhenReady && player.playbackState != Player.STATE_IDLE
            player.pause()
        }
    }

    override fun onDestroy() {

        mainHandler.removeCallbacksAndMessages(null)

        dataListener?.let { rootRef.removeEventListener(it) }

        ioExecutor.shutdownNow()

        if (::player.isInitialized) {
            player.release()
        }

        super.onDestroy()
    }
}
