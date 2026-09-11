package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Channel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : Activity() {

    // =========================================================
    // PLAYER
    // =========================================================

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    // =========================================================
    // ROOT UI
    // =========================================================

    private lateinit var overlay: LinearLayout

    private lateinit var packageScroll: HorizontalScrollView
    private lateinit var packageLayout: LinearLayout

    private lateinit var channelScroll: HorizontalScrollView
    private lateinit var channelLayout: LinearLayout

    private lateinit var currentChannelText: TextView
    private lateinit var currentGroupText: TextView
    private lateinit var statusText: TextView
    private lateinit var timeText: TextView

    private lateinit var liveDot: TextView

    private var currentGroup = ""
    private var currentChannel: Channel? = null
    private var currentChannelIndex = -1

    private var isOverlayVisible = true
    private var isPlayingChannel = false

    private val handler = Handler(Looper.getMainLooper())

    private val timeRunnable = object : Runnable {
        override fun run() {
            if (::timeText.isInitialized) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeText.text = sdf.format(Date())
            }

            handler.postDelayed(this, 1000)
        }
    }

    private val channels = mutableListOf<Channel>()

    // =========================================================
    // COLORS
    // =========================================================

    private val BLACK = Color.parseColor("#05070B")

    private val GLASS = Color.parseColor("#D90A0F17")

    private val CARD = Color.parseColor("#E6111721")
    private val CARD_DARK = Color.parseColor("#D90B1018")

    private val WHITE = Color.WHITE
    private val TEXT = Color.parseColor("#F8FAFC")
    private val MUTED = Color.parseColor("#94A3B8")

    private val CYAN = Color.parseColor("#00E5FF")
    private val BLUE = Color.parseColor("#1677FF")

    private val GREEN = Color.parseColor("#16C784")
    private val RED = Color.parseColor("#EF4444")

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        loadChannelsData()

        createPlayer()

        createInterface()

        if (channels.isNotEmpty()) {
            showChannels(channels.first().group)
        }

        handler.post(timeRunnable)
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun background(
        color: Int,
        radius: Float = 16f,
        stroke: Int = Color.TRANSPARENT,
        strokeWidth: Int = 0
    ): GradientDrawable {

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (stroke != Color.TRANSPARENT && strokeWidth > 0) {
                setStroke(
                    dp(strokeWidth.toFloat()),
                    stroke
                )
            }
        }
    }

    private fun tv(
        value: String,
        size: Float,
        color: Int = WHITE,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {

            text = value

            textSize = size

            setTextColor(color)

            gravity = Gravity.CENTER_VERTICAL

            if (bold) {
                setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            }
        }
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private fun createPlayer() {

        player = ExoPlayer.Builder(this).build()

        playerView = PlayerView(this).apply {

            player = this@MainActivity.player

            useController = false

            resizeMode =
                AspectRatioFrameLayout.RESIZE_MODE_FILL

            setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
            )

            setBackgroundColor(Color.BLACK)

            setOnClickListener {

                if (isOverlayVisible) {
                    hideOverlay()
                } else {
                    showOverlay()
                }
            }
        }

        player?.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(
                    state: Int
                ) {

                    when (state) {

                        Player.STATE_BUFFERING -> {

                            statusTextSafe(
                                "جاري الاتصال بالبث..."
                            )
                        }

                        Player.STATE_READY -> {

                            isPlayingChannel = true

                            statusTextSafe(
                                "البث مباشر ومستقر"
                            )
                        }

                        Player.STATE_ENDED -> {

                            reconnectChannel()
                        }
                    }
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    isPlayingChannel = false

                    statusTextSafe(
                        "فشل الاتصال • إعادة الاتصال..."
                    )

                    reconnectChannel()
                }
            }
        )
    }

    private fun statusTextSafe(value: String) {

        if (::statusText.isInitialized) {
            statusText.text = value
        }
    }

    // =========================================================
    // PLAY CHANNEL
    // =========================================================

    private fun playChannel(
        channel: Channel,
        index: Int
    ) {

        currentChannel = channel

        currentChannelIndex = index

        currentGroup = channel.group

        updateCurrentInfo()

        val mediaItem =
            MediaItem.fromUri(
                Uri.parse(channel.url)
            )

        player?.apply {

            stop()

            clearMediaItems()

            setMediaItem(mediaItem)

            prepare()

            playWhenReady = true
        }

        isPlayingChannel = true

        refreshChannelCards()

        hideOverlay()
    }

    // =========================================================
    // RECONNECT
    // =========================================================

    private fun reconnectChannel() {

        val channel = currentChannel ?: return

        handler.postDelayed({

            if (!isFinishing) {

                val mediaItem =
                    MediaItem.fromUri(
                        Uri.parse(channel.url)
                    )

                player?.apply {

                    stop()

                    clearMediaItems()

                    setMediaItem(mediaItem)

                    prepare()

                    playWhenReady = true
                }
            }

        }, 1500)
    }

    // =========================================================
    // NEW INTERFACE
    // =========================================================

    private fun createInterface() {

        val root = FrameLayout(this).apply {
            setBackgroundColor(BLACK)
        }

        // PLAYER
        root.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // MAIN OVERLAY
        overlay = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(18f),
                dp(16f),
                dp(18f),
                dp(16f)
            )

            background =
                background(
                    GLASS,
                    28f
                )

            elevation =
                dp(20f).toFloat()
        }

        // =====================================================
        // TOP BAR
        // =====================================================

        val topBar = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
        }

        // BRAND
        val brandBox =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val brand =
            tv(
                "NIYATI",
                25f,
                WHITE,
                true
            ).apply {

                letterSpacing = 0.18f
            }

        val subtitle =
            tv(
                "SPORTS",
                9f,
                CYAN,
                true
            ).apply {

                letterSpacing = 0.35f
            }

        brandBox.addView(brand)

        brandBox.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(18f)
            )
        )

        topBar.addView(
            brandBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // CURRENT CHANNEL
        val topChannel =
            tv(
                "البث الرياضي",
                12f,
                MUTED,
                true
            ).apply {

                gravity = Gravity.CENTER
            }

        topBar.addView(
            topChannel,
            LinearLayout.LayoutParams(
                0,
                dp(38f),
                1f
            )
        )

        // TIME
        timeText =
            tv(
                "--:--",
                16f,
                WHITE,
                true
            ).apply {

                gravity = Gravity.CENTER

                background =
                    background(
                        Color.parseColor("#66111A24"),
                        14f
                    )

                setPadding(
                    dp(14f),
                    0,
                    dp(14f),
                    0
                )
            }

        topBar.addView(
            timeText,
            LinearLayout.LayoutParams(
                dp(80f),
                dp(38f)
            )
        )

        // LIVE
        liveDot =
            tv(
                "●  LIVE",
                10f,
                WHITE,
                true
            ).apply {

                gravity = Gravity.CENTER

                background =
                    background(
                        Color.parseColor("#B5163A2B"),
                        30f,
                        GREEN,
                        1
                    )
            }

        topBar.addView(
            liveDot,
            LinearLayout.LayoutParams(
                dp(80f),
                dp(34f)
            ).apply {

                marginStart =
                    dp(8f)
            }
        )

        overlay.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f)
            )
        )

        // =====================================================
        // CURRENT CHANNEL CARD
        // =====================================================

        val currentCard =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(16f),
                    dp(8f),
                    dp(16f),
                    dp(8f)
                )

                background =
                    background(
                        Color.parseColor("#B3091018"),
                        20f,
                        Color.parseColor("#334155"),
                        1
                    )
            }

        val channelIcon =
            TextView(this).apply {

                text = "▶"

                textSize = 20f

                setTextColor(CYAN)

                gravity = Gravity.CENTER

                background =
                    background(
                        Color.parseColor("#221E293B"),
                        14f
                    )
            }

        currentCard.addView(
            channelIcon,
            LinearLayout.LayoutParams(
                dp(46f),
                dp(46f)
            )
        )

        val currentInfo =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(12f),
                    0,
                    dp(12f),
                    0
                )
            }

        currentGroupText =
            tv(
                "اختر باقة",
                10f,
                CYAN,
                true
            )

        currentChannelText =
            tv(
                "اختر قناة للبدء",
                17f,
                WHITE,
                true
            )

        statusText =
            tv(
                "جاهز للبث",
                9f,
                MUTED
            )

        currentInfo.addView(
            currentGroupText
        )

        currentInfo.addView(
            currentChannelText
        )

        currentInfo.addView(
            statusText
        )

        currentCard.addView(
            currentInfo,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        val watch =
            tv(
                "مشاهدة",
                11f,
                WHITE,
                true
            ).apply {

                gravity = Gravity.CENTER

                background =
                    background(
                        BLUE,
                        14f
                    )
            }

        watch.setOnClickListener {

            if (currentChannel != null) {
                hideOverlay()
            }
        }

        currentCard.addView(
            watch,
            LinearLayout.LayoutParams(
                dp(85f),
                dp(40f)
            )
        )

        overlay.addView(
            currentCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68f)
            ).apply {

                topMargin = dp(10f)

                bottomMargin = dp(12f)
            }
        )

        // =====================================================
        // PACKAGES TITLE
        // =====================================================

        val packageHeader =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        val packageTitle =
            tv(
                "الباقات الرياضية",
                15f,
                WHITE,
                true
            )

        packageHeader.addView(
            packageTitle,
            LinearLayout.LayoutParams(
                0,
                dp(30f),
                1f
            )
        )

        val packageHint =
            tv(
                "اختر الباقة",
                9f,
                MUTED
            ).apply {

                gravity = Gravity.CENTER
            }

        packageHeader.addView(
            packageHint,
            LinearLayout.LayoutParams(
                dp(90f),
                dp(30f)
            )
        )

        overlay.addView(
            packageHeader
        )

        // =====================================================
        // PACKAGES HORIZONTAL
        // =====================================================

        packageScroll =
            HorizontalScrollView(this).apply {

                isHorizontalScrollBarEnabled = false

                overScrollMode =
                    View.OVER_SCROLL_NEVER

                isFillViewport = false
            }

        packageLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        packageScroll.addView(
            packageLayout
        )

        overlay.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72f)
            ).apply {

                bottomMargin = dp(12f)
            }
        )

        // =====================================================
        // CHANNEL TITLE
        // =====================================================

        val channelHeader =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        val channelTitle =
            tv(
                "القنوات",
                15f,
                WHITE,
                true
            )

        channelHeader.addView(
            channelTitle,
            LinearLayout.LayoutParams(
                0,
                dp(30f),
                1f
            )
        )

        val channelHint =
            tv(
                "اضغط OK للتشغيل",
                9f,
                MUTED
            ).apply {

                gravity = Gravity.CENTER
            }

        channelHeader.addView(
            channelHint,
            LinearLayout.LayoutParams(
                dp(110f),
                dp(30f)
            )
        )

        overlay.addView(
            channelHeader
        )

        // =====================================================
        // CHANNELS HORIZONTAL
        // =====================================================

        channelScroll =
            HorizontalScrollView(this).apply {

                isHorizontalScrollBarEnabled = false

                overScrollMode =
                    View.OVER_SCROLL_NEVER

                isFillViewport = false
            }

        channelLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        channelScroll.addView(
            channelLayout
        )

        overlay.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // =====================================================
        // FOOTER
        // =====================================================

        val footer =
            tv(
                "← → القنوات    ↑ ↓ الباقات    OK تشغيل    BACK رجوع",
                9f,
                MUTED
            ).apply {

                gravity = Gravity.CENTER
            }

        overlay.addView(
            footer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(25f)
            )
        )

        // =====================================================
        // SIZE
        // =====================================================

        val width =
            resources.displayMetrics.widthPixels

        val height =
            resources.displayMetrics.heightPixels

        val portrait =
            height > width

        val overlayWidth =
            if (portrait) {
                (width * 0.96f).toInt()
            } else {
                (width * 0.94f).toInt()
            }

        val overlayHeight =
            if (portrait) {
                (height * 0.78f).toInt()
            } else {
                (height * 0.78f).toInt()
            }

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                overlayWidth,
                overlayHeight,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {

                bottomMargin =
                    if (portrait) dp(10f)
                    else dp(18f)
            }
        )

        setContentView(root)

        buildPackages()
    }

    // =========================================================
    // BUILD PACKAGES
    // =========================================================

    private fun buildPackages() {

        packageLayout.removeAllViews()

        val groups =
            channels
                .map { it.group }
                .distinct()

        groups.forEachIndexed { index, group ->

            val count =
                channels.count {
                    it.group == group
                }

            val card =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    isFocusable = true

                    isFocusableInTouchMode = true

                    setPadding(
                        dp(14f),
                        dp(5f),
                        dp(14f),
                        dp(5f)
                    )

                    background =
                        background(
                            if (group == currentGroup)
                                Color.parseColor("#2230AFFF")
                            else CARD,
                            18f,
                            if (group == currentGroup)
                                CYAN
                            else Color.TRANSPARENT,
                            if (group == currentGroup)
                                1
                            else 0
                        )
                }

            val icon =
                tv(
                    packageIcon(group),
                    21f,
                    WHITE,
                    true
                ).apply {

                    gravity = Gravity.CENTER
                }

            card.addView(
                icon,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(28f)
                )
            )

            val name =
                tv(
                    packageDisplayName(group),
                    11f,
                    TEXT,
                    true
                ).apply {

                    gravity = Gravity.CENTER

                    maxLines = 1
                }

            card.addView(
                name,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(25f)
                )
            )

            val countText =
                tv(
                    "$count قناة",
                    8f,
                    MUTED,
                    true
                ).apply {

                    gravity = Gravity.CENTER
                }

            card.addView(
                countText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(17f)
                )
            )

            card.layoutParams =
                LinearLayout.LayoutParams(
                    dp(125f),
                    dp(70f)
                ).apply {

                    marginStart = dp(5f)
                    marginEnd = dp(5f)
                }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                if (hasFocus) {

                    view.background =
                        background(
                            BLUE,
                            18f,
                            CYAN,
                            2
                        )

                    name.setTextColor(WHITE)

                } else {

                    val selected =
                        group == currentGroup

                    view.background =
                        background(
                            if (selected)
                                Color.parseColor("#2230AFFF")
                            else CARD,
                            18f,
                            if (selected)
                                CYAN
                            else Color.TRANSPARENT,
                            if (selected) 1 else 0
                        )

                    name.setTextColor(
                        if (selected)
                            CYAN
                        else TEXT
                    )
                }
            }

            card.setOnClickListener {

                showChannels(group)

                if (channelLayout.childCount > 0) {

                    channelLayout
                        .getChildAt(0)
                        .requestFocus()
                }
            }

            packageLayout.addView(card)

            if (index == 0) {

                card.post {

                    card.requestFocus()
                }
            }
        }
    }

    // =========================================================
    // SHOW CHANNELS
    // =========================================================

    private fun showChannels(group: String) {

        currentGroup = group

        val groupChannels =
            channels.filter {
                it.group == group
            }

        currentGroupText.text =
            packageDisplayName(group)

        channelLayout.removeAllViews()

        groupChannels.forEachIndexed {
                index,
                channel ->

            val playing =
                channel == currentChannel

            val card =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    isFocusable = true

                    isFocusableInTouchMode = true

                    setPadding(
                        dp(10f),
                        dp(8f),
                        dp(10f),
                        dp(8f)
                    )

                    background =
                        background(
                            if (playing)
                                Color.parseColor("#153B7A5A")
                            else CARD_DARK,
                            18f,
                            if (playing)
                                GREEN
                            else Color.TRANSPARENT,
                            if (playing) 1 else 0
                        )
                }

            // CHANNEL NUMBER
            val number =
                tv(
                    String.format(
                        Locale.getDefault(),
                        "%02d",
                        index + 1
                    ),
                    12f,
                    if (playing)
                        GREEN
                    else MUTED,
                    true
                ).apply {

                    gravity = Gravity.CENTER

                    background =
                        background(
                            Color.parseColor("#33111822"),
                            10f
                        )
                }

            card.addView(
                number,
                LinearLayout.LayoutParams(
                    dp(42f),
                    dp(28f)
                )
            )

            // ICON
            val icon =
                tv(
                    if (playing)
                        "▶"
                    else "TV",
                    14f,
                    if (playing)
                        GREEN
                    else CYAN,
                    true
                ).apply {

                    gravity = Gravity.CENTER
                }

            card.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(48f),
                    dp(35f)
                )
            )

            // NAME
            val name =
                tv(
                    channel.name,
                    11f,
                    if (playing)
                        WHITE
                    else TEXT,
                    true
                ).apply {

                    gravity = Gravity.CENTER

                    maxLines = 2
                }

            card.addView(
                name,
                LinearLayout.LayoutParams(
                    dp(130f),
                    dp(38f)
                )
            )

            // QUALITY
            val quality =
                tv(
                    if (
                        channel.name.contains(
                            "4K",
                            true
                        )
                    )
                        "4K"
                    else
                        "HD",
                    8f,
                    CYAN,
                    true
                ).apply {

                    gravity = Gravity.CENTER

                    background =
                        background(
                            Color.parseColor("#33111C29"),
                            8f
                        )
                }

            card.addView(
                quality,
                LinearLayout.LayoutParams(
                    dp(40f),
                    dp(20f)
                )
            )

            card.layoutParams =
                LinearLayout.LayoutParams(
                    dp(160f),
                    dp(155f)
                ).apply {

                    marginStart = dp(5f)
                    marginEnd = dp(5f)
                }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                if (hasFocus) {

                    view.background =
                        background(
                            BLUE,
                            18f,
                            CYAN,
                            2
                        )

                    name.setTextColor(WHITE)

                } else {

                    view.background =
                        background(
                            if (playing)
                                Color.parseColor(
                                    "#153B7A5A"
                                )
                            else CARD_DARK,
                            18f,
                            if (playing)
                                GREEN
                            else Color.TRANSPARENT,
                            if (playing) 1 else 0
                        )

                    name.setTextColor(
                        if (playing)
                            GREEN
                        else TEXT
                    )
                }
            }

            card.setOnClickListener {

                val realIndex =
                    channels.indexOf(channel)

                playChannel(
                    channel,
                    realIndex
                )
            }

            channelLayout.addView(card)
        }

        // focus first channel
        if (
            channelLayout.childCount > 0 &&
            isOverlayVisible
        ) {

            channelLayout
                .getChildAt(0)
                .post {

                    channelLayout
                        .getChildAt(0)
                        .requestFocus()
                }
        }
    }

    // =========================================================
    // REFRESH
    // =========================================================

    private fun refreshChannelCards() {

        if (currentGroup.isNotEmpty()) {

            showChannels(currentGroup)
        }
    }

    // =========================================================
    // CURRENT CHANNEL
    // =========================================================

    private fun updateCurrentInfo() {

        val channel =
            currentChannel

        if (channel == null) {

            currentChannelText.text =
                "اختر قناة للبدء"

            currentGroupText.text =
                "البث الرياضي"

            statusText.text =
                "جاهز للبث"

            return
        }

        currentChannelText.text =
            channel.name

        currentGroupText.text =
            packageDisplayName(
                channel.group
            )

        statusText.text =
            "● بث مباشر الآن"
    }

    // =========================================================
    // PACKAGE ICON
    // =========================================================

    private fun packageIcon(
        group: String
    ): String {

        return when (group) {

            "ALWAN SPORT" -> "🎨"

            "beIN SPORTS" -> "⚽"

            "beIN SPORTS VIP" -> "👑"

            "THAMANYA" -> "8️⃣"

            "ALKASS SPORT" -> "🏆"

            "AD SPORT" -> "🇦🇪"

            "DUBAI SPORT" -> "🏙️"

            "IRAQIA SPORT" -> "🇮🇶"

            else -> "📺"
        }
    }

    // =========================================================
    // PACKAGE NAMES
    // =========================================================

    private fun packageDisplayName(
        group: String
    ): String {

        return when (group) {

            "ALWAN SPORT" ->
                "ألوان سبورت"

            "beIN SPORTS" ->
                "beIN SPORTS"

            "beIN SPORTS VIP" ->
                "beIN SPORTS VIP"

            "THAMANYA" ->
                "ثمانية"

            "ALKASS SPORT" ->
                "الكأس"

            "AD SPORT" ->
                "أبوظبي"

            "DUBAI SPORT" ->
                "دبي الرياضية"

            "IRAQIA SPORT" ->
                "العراقية"

            else ->
                group
        }
    }

    // =========================================================
    // FULLSCREEN PLAYER
    // =========================================================

    private fun hideOverlay() {

        isOverlayVisible = false

        overlay.visibility =
            View.GONE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // =========================================================
    // SHOW UI
    // =========================================================

    private fun showOverlay() {

        isOverlayVisible = true

        overlay.visibility =
            View.VISIBLE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        updateCurrentInfo()

        refreshChannelCards()
    }

    // =========================================================
    // NEXT CHANNEL
    // =========================================================

    private fun nextChannel() {

        val groupChannels =
            channels.filter {
                it.group == currentGroup
            }

        if (groupChannels.isEmpty()) {
            return
        }

        val currentPosition =
            groupChannels.indexOf(
                currentChannel
            )

        val nextPosition =
            if (
                currentPosition <
                groupChannels.lastIndex
            )
                currentPosition + 1
            else
                0

        val next =
            groupChannels[nextPosition]

        playChannel(
            next,
            channels.indexOf(next)
        )
    }

    // =========================================================
    // PREVIOUS CHANNEL
    // =========================================================

    private fun previousChannel() {

        val groupChannels =
            channels.filter {
                it.group == currentGroup
            }

        if (groupChannels.isEmpty()) {
            return
        }

        val currentPosition =
            groupChannels.indexOf(
                currentChannel
            )

        val previousPosition =
            if (currentPosition > 0)
                currentPosition - 1
            else
                groupChannels.lastIndex

        val previous =
            groupChannels[previousPosition]

        playChannel(
            previous,
            channels.indexOf(previous)
        )
    }

    // =========================================================
    // REMOTE CONTROL
    // =========================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action !=
            KeyEvent.ACTION_DOWN
        ) {
            return super.dispatchKeyEvent(event)
        }

        when (event.keyCode) {

            // OK
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }
            }

            // BACK
            KeyEvent.KEYCODE_BACK -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }

                return true
            }

            // RIGHT
            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (!isOverlayVisible) {

                    nextChannel()

                    return true
                }
            }

            // LEFT
            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (!isOverlayVisible) {

                    previousChannel()

                    return true
                }
            }

            // UP / DOWN
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    // =========================================================
    // PAUSE
    // =========================================================

    override fun onPause() {

        super.onPause()

        player?.pause()
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        player?.release()

        player = null

        super.onDestroy()
    }

    // =========================================================
    // CHANNEL DATA
    // =========================================================

    private fun loadChannelsData() {

        channels.clear()

        // =====================================================
        // ALWAN SPORT
        // =====================================================

        channels.add(
            Channel(
                "ALWAN SPORT 1 HD",
                "ALWAN SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859098&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALWAN SPORT 2 HD",
                "ALWAN SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859097&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALWAN SPORT 3 HD",
                "ALWAN SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859096&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALWAN SPORT 4 HD",
                "ALWAN SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859095&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALWAN SPORT 5 HD",
                "ALWAN SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859094&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALWAN SPORT 6 HD",
                "ALWAN SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859093&extension=ts"
            )
        )

        // =====================================================
        // BEIN SPORTS
        // =====================================================

        channels.add(
            Channel(
                "beIN SPORT 1 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 2 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330438&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 3 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411381&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 4 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411380&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 5 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411379&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 6 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411378&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 7 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411377&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 8 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411376&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 9 HD",
                "beIN SPORTS",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411375&extension=ts"
            )
        )

        // =====================================================
        // BEIN VIP
        // =====================================================

        channels.add(
            Channel(
                "beIN SPORT 1 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660413&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 2 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660411&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 3 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660409&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 4 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660407&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 5 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660405&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 6 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660403&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 7 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660401&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 8 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660399&extension=ts"
            )
        )

        channels.add(
            Channel(
                "beIN SPORT 9 HD",
                "beIN SPORTS VIP",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660397&extension=ts"
            )
        )

        // =====================================================
        // THAMANYA
        // =====================================================

        channels.add(
            Channel(
                "THAMANYA 1 HD",
                "THAMANYA",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936356&extension=ts"
            )
        )

        channels.add(
            Channel(
                "THAMANYA 2 HD",
                "THAMANYA",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936355&extension=ts"
            )
        )

        channels.add(
            Channel(
                "THAMANYA 3 HD",
                "THAMANYA",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936354&extension=ts"
            )
        )

        // =====================================================
        // ALKASS
        // =====================================================

        channels.add(
            Channel(
                "ALKASS SPORT 1 HD",
                "ALKASS SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591593&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALKASS SPORT 2 HD",
                "ALKASS SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591591&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALKASS SPORT 3 HD",
                "ALKASS SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787903&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALKASS SPORT 4 HD",
                "ALKASS SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591589&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALKASS SPORT 5 HD",
                "ALKASS SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591587&extension=ts"
            )
        )

        channels.add(
            Channel(
                "ALKASS SPORT 6 HD",
                "ALKASS SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787906&extension=ts"
            )
        )

        // =====================================================
        // AD SPORT
        // =====================================================

        channels.add(
            Channel(
                "AD SPORT 1 HD",
                "AD SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993336&extension=ts"
            )
        )

        channels.add(
            Channel(
                "AD SPORT 2 HD",
                "AD SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993337&extension=ts"
            )
        )

        // =====================================================
        // DUBAI SPORT
        // =====================================================

        channels.add(
            Channel(
                "DUBAI SPORT 1 HD",
                "DUBAI SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8086&extension=ts"
            )
        )

        channels.add(
            Channel(
                "DUBAI SPORT 2 HD",
                "DUBAI SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=84251&extension=ts"
            )
        )

        channels.add(
            Channel(
                "DUBAI SPORT 3 HD",
                "DUBAI SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591579&extension=ts"
            )
        )

        // =====================================================
        // IRAQIA SPORT
        // =====================================================

        channels.add(
            Channel(
                "IRAQIA SPORT HD",
                "IRAQIA SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"
            )
        )
    }
}