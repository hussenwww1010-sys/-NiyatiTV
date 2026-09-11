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

    // ============================================================
    // PLAYER
    // ============================================================

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    // ============================================================
    // UI
    // ============================================================

    private lateinit var overlay: LinearLayout
    private lateinit var packageScroll: HorizontalScrollView
    private lateinit var channelScroll: HorizontalScrollView
    private lateinit var packageLayout: LinearLayout
    private lateinit var channelLayout: LinearLayout

    private lateinit var logoView: ImageView
    private lateinit var currentChannelText: TextView
    private lateinit var currentGroupText: TextView
    private lateinit var statusText: TextView
    private lateinit var timeText: TextView
    private lateinit var channelCountText: TextView

    private var isOverlayVisible = true
    private var isPlayingChannel = false

    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentChannel: Channel? = null

    private val handler = Handler(Looper.getMainLooper())

    private val channels = mutableListOf<Channel>()

    // ============================================================
    // LUXURY COLORS
    // ============================================================

    private val BG = Color.parseColor("#030407")

    private val BLACK = Color.parseColor("#000000")
    private val BLACK_SOFT = Color.parseColor("#08090D")

    private val GOLD = Color.parseColor("#D6A84F")
    private val GOLD_LIGHT = Color.parseColor("#F4D58A")
    private val GOLD_DARK = Color.parseColor("#8E6525")

    private val SILVER = Color.parseColor("#D9DCE2")
    private val SILVER_SOFT = Color.parseColor("#A6ACB7")

    private val WHITE = Color.WHITE
    private val TEXT_PRIMARY = Color.parseColor("#F7F7F5")
    private val TEXT_SECONDARY = Color.parseColor("#9EA4AE")

    private val CARD = Color.parseColor("#101217")
    private val CARD_2 = Color.parseColor("#151820")

    private val GREEN = Color.parseColor("#22C55E")
    private val RED = Color.parseColor("#EF4444")

    // ============================================================
    // TIME
    // ============================================================

    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    // ============================================================
    // ON CREATE
    // ============================================================

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
            val firstGroup = channels.first().group
            showChannels(firstGroup)
        }

        handler.post(timeRunnable)
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    // ============================================================
    // BACKGROUND HELPERS
    // ============================================================

    private fun roundedBackground(
        color: Int,
        radius: Float = 14f,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidth: Int = 0
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            setColor(color)

            cornerRadius = dp(radius).toFloat()

            if (strokeColor != Color.TRANSPARENT && strokeWidth > 0) {

                setStroke(
                    dp(strokeWidth.toFloat()),
                    strokeColor
                )
            }
        }
    }

    private fun gradientBackground(
        colors: IntArray,
        orientation: GradientDrawable.Orientation =
            GradientDrawable.Orientation.TOP_BOTTOM
    ): GradientDrawable {

        return GradientDrawable(
            orientation,
            colors
        ).apply {
            cornerRadius = dp(22f).toFloat()
        }
    }

    // ============================================================
    // TEXT
    // ============================================================

    private fun text(
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

    // ============================================================
    // PLAYER
    // ============================================================

    private fun createPlayer() {

        player = ExoPlayer.Builder(this).build()

        playerView = PlayerView(this).apply {

            player = this@MainActivity.player

            useController = false

            /*
             * RESIZE_MODE_ZOOM لملء الشاشة بالكامل عند العرض بدون حواف سوداء.
             */
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

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

                override fun onPlaybackStateChanged(state: Int) {

                    when (state) {

                        Player.STATE_BUFFERING -> {

                            statusTextSafe(
                                "جاري الاتصال بالبث المباشر..."
                            )
                        }

                        Player.STATE_READY -> {

                            isPlayingChannel = true

                            statusTextSafe(
                                "● البث المباشر مستقر"
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
                        "فشل الاتصال — جاري إعادة المحاولة..."
                    )

                    reconnectChannel()
                }
            }
        )
    }

    // ============================================================
    // SAFE STATUS
    // ============================================================

    private fun statusTextSafe(value: String) {

        if (::statusText.isInitialized) {

            statusText.text = value
        }
    }

    // ============================================================
    // PLAY CHANNEL
    // ============================================================

    private fun playChannel(
        channel: Channel,
        index: Int
    ) {

        currentChannel = channel

        currentChannelIndex = index

        currentGroup = channel.group

        updateCurrentInfo()

        val mediaItem = MediaItem.fromUri(
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

        /*
         * عند اختيار القناة، يتم الدخول فوراً إلى وضع ملء الشاشة.
         */
        hideOverlay()
    }

    // ============================================================
    // RECONNECT
    // ============================================================

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

    // ============================================================
    // CREATE INTERFACE (عمودي: الباقات -> القنوات -> المشغل)
    // ============================================================

    private fun createInterface() {

        val root = FrameLayout(this).apply {

            setBackgroundColor(BG)
        }

        // --------------------------------------------------------
        // PLAYER (خلفية بملء الشاشة دائماً)
        // --------------------------------------------------------

        root.addView(

            playerView,

            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // --------------------------------------------------------
        // MAIN OVERLAY
        // --------------------------------------------------------

        overlay = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(22f),
                dp(18f),
                dp(22f),
                dp(18f)
            )

            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#E6030407"),
                    Color.parseColor("#F0030407"),
                    Color.parseColor("#FA080A0F")
                )
            )
        }

        // ========================================================
        // TOP BAR
        // ========================================================

        val topBar = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
        }

        logoView = ImageView(this).apply {

            setImageResource(
                R.drawable.niyati_logo
            )

            scaleType =
                ImageView.ScaleType.FIT_CENTER

            contentDescription = "NIYATI TV"
        }

        topBar.addView(

            logoView,

            LinearLayout.LayoutParams(
                dp(58f),
                dp(58f)
            )
        )

        val brandBox = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL

            setPadding(
                dp(10f),
                0,
                0,
                0
            )
        }

        val brand = text(
            "NIYATI",
            20f,
            WHITE,
            true
        ).apply {

            letterSpacing = 0.18f
        }

        val brandSub = text(
            "SPORTS • LIVE TV",
            8f,
            GOLD_LIGHT,
            true
        ).apply {

            letterSpacing = 0.18f
        }

        brandBox.addView(brand)

        brandBox.addView(brandSub)

        topBar.addView(

            brandBox,

            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val liveBox = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER

            setPadding(
                dp(12f),
                0,
                dp(12f),
                0
            )

            background =
                roundedBackground(
                    Color.parseColor("#80110D0A"),
                    50f,
                    GOLD_DARK,
                    1
                )
        }

        val liveDot = text(
            "●",
            10f,
            GREEN,
            true
        )

        val liveText = text(
            " LIVE",
            10f,
            SILVER,
            true
        )

        liveBox.addView(liveDot)

        liveBox.addView(liveText)

        topBar.addView(

            liveBox,

            LinearLayout.LayoutParams(
                dp(74f),
                dp(34f)
            ).apply {

                marginStart = dp(8f)
            }
        )

        timeText = text(
            "--:--",
            15f,
            WHITE,
            true
        ).apply {

            gravity = Gravity.CENTER

            setPadding(
                dp(10f),
                0,
                dp(4f),
                0
            )
        }

        topBar.addView(

            timeText,

            LinearLayout.LayoutParams(
                dp(62f),
                dp(40f)
            )
        )

        overlay.addView(

            topBar,

            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60f)
            )
        )

        // ========================================================
        // 1. PACKAGES SECTION (الباقات أولاً)
        // ========================================================

        val packageHeader =
            createSectionHeader(
                "الباقات الرياضية",
                "اختر الباقة"
            )

        overlay.addView(

            packageHeader,

            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34f)
            ).apply {
                topMargin = dp(8f)
            }
        )

        packageScroll =
            HorizontalScrollView(this).apply {

                isHorizontalScrollBarEnabled = false

                overScrollMode =
                    View.OVER_SCROLL_NEVER

                clipToPadding = false

                setPadding(
                    dp(2f),
                    0,
                    dp(2f),
                    dp(4f)
                )
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
                dp(68f)
            ).apply {

                bottomMargin = dp(8f)
            }
        )

        // ========================================================
        // 2. CHANNELS SECTION (القنوات ثانياً)
        // ========================================================

        val channelHeader =
            createSectionHeader(
                "القنوات",
                "اضغط OK للتشغيل بملء الشاشة"
            )

        overlay.addView(

            channelHeader,

            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34f)
            )
        )

        channelScroll =
            HorizontalScrollView(this).apply {

                isHorizontalScrollBarEnabled = false

                overScrollMode =
                    View.OVER_SCROLL_NEVER

                clipToPadding = false

                setPadding(
                    dp(2f),
                    0,
                    dp(2f),
                    0
                )
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
                dp(98f)
            ).apply {

                bottomMargin = dp(12f)
            }
        )

        // ========================================================
        // 3. PLAYER INFO / HERO (معلومات المشغل ثالثاً)
        // ========================================================

        val hero = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(18f),
                dp(10f),
                dp(18f),
                dp(10f)
            )

            background =
                gradientBackground(
                    intArrayOf(
                        Color.parseColor("#D916171C"),
                        Color.parseColor("#B30B0D12")
                    ),
                    GradientDrawable.Orientation.LEFT_RIGHT
                )
        }

        val heroInfo = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER_VERTICAL
        }

        currentGroupText = text(
            "البث الرياضي المباشر",
            10f,
            GOLD_LIGHT,
            true
        )

        currentGroupText.setPadding(
            0,
            0,
            0,
            dp(2f)
        )

        heroInfo.addView(
            currentGroupText
        )

        currentChannelText = text(
            "اختر قناة للبدء",
            18f,
            WHITE,
            true
        )

        currentChannelText.maxLines = 1

        heroInfo.addView(
            currentChannelText
        )

        statusText = text(
            "جاهز للبث",
            10f,
            TEXT_SECONDARY
        )

        heroInfo.addView(
            statusText
        )

        hero.addView(

            heroInfo,

            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        channelCountText = text(
            "0 قناة",
            10f,
            SILVER_SOFT,
            true
        ).apply {

            gravity = Gravity.CENTER

            background =
                roundedBackground(
                    Color.parseColor("#201D1A15"),
                    12f,
                    Color.parseColor("#664F3A1A"),
                    1
                )

            setPadding(
                dp(12f),
                0,
                dp(12f),
                0
            )
        }

        hero.addView(

            channelCountText,

            LinearLayout.LayoutParams(
                dp(76f),
                dp(34f)
            )
        )

        overlay.addView(

            hero,

            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70f)
            ).apply {

                bottomMargin = dp(10f)
            }
        )

        // ========================================================
        // BOTTOM FOOTER
        // ========================================================

        val footer = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
        }

        val footerText = text(
            "▲ ▼ التنقل   •   OK تشغيل ملء الشاشة   •   BACK خروج / القائمة",
            9f,
            TEXT_SECONDARY,
            false
        ).apply {

            gravity = Gravity.CENTER
        }

        footer.addView(
            footerText
        )

        overlay.addView(

            footer,

            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(24f)
            )
        )

        // ========================================================
        // ADD OVERLAY
        // ========================================================

        root.addView(

            overlay,

            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        buildPackages()
    }

    // ============================================================
    // SECTION HEADER
    // ============================================================

    private fun createSectionHeader(
        title: String,
        subtitle: String
    ): LinearLayout {

        val box = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL

            layoutDirection =
                View.LAYOUT_DIRECTION_RTL
        }

        val titleBox = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER_VERTICAL
        }

        val line = View(this).apply {

            setBackgroundColor(GOLD)
        }

        titleBox.addView(

            line,

            LinearLayout.LayoutParams(
                dp(3f),
                dp(18f)
            ).apply {

                marginStart = dp(8f)
            }
        )

        val titleText = text(
            title,
            13f,
            WHITE,
            true
        )

        titleBox.addView(
            titleText
        )

        box.addView(

            titleBox,

            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        val subText = text(
            subtitle,
            9f,
            TEXT_SECONDARY
        ).apply {

            gravity = Gravity.CENTER
        }

        box.addView(
            subText
        )

        return box
    }

    // ============================================================
    // PACKAGES
    // ============================================================

    private fun buildPackages() {

        packageLayout.removeAllViews()

        val groups =
            channels
                .map { it.group }
                .distinct()

        groups.forEachIndexed { index, group ->

            val groupChannels =
                channels.filter {
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
                        dp(12f),
                        dp(6f),
                        dp(12f),
                        dp(6f)
                    )

                    background =
                        roundedBackground(
                            CARD,
                            14f,
                            Color.TRANSPARENT,
                            0
                        )

                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        defaultFocusHighlightEnabled = false
                    }
                }

            val icon = text(
                packageIcon(group),
                16f,
                GOLD_LIGHT,
                true
            ).apply {

                gravity = Gravity.CENTER
            }

            card.addView(

                icon,

                LinearLayout.LayoutParams(
                    dp(30f),
                    dp(24f)
                )
            )

            val name = text(
                packageDisplayName(group),
                10f,
                TEXT_PRIMARY,
                true
            ).apply {

                gravity = Gravity.CENTER

                maxLines = 1
            }

            card.addView(

                name,

                LinearLayout.LayoutParams(
                    dp(110f),
                    dp(20f)
                )
            )

            val count = text(
                "${groupChannels.size} قناة",
                8f,
                TEXT_SECONDARY
            ).apply {

                gravity = Gravity.CENTER
            }

            card.addView(

                count,

                LinearLayout.LayoutParams(
                    dp(110f),
                    dp(16f)
                )
            )

            card.layoutParams =
                LinearLayout.LayoutParams(
                    dp(120f),
                    dp(62f)
                ).apply {

                    marginStart = dp(4f)

                    marginEnd = dp(4f)
                }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                if (hasFocus) {

                    view.background =
                        roundedBackground(
                            Color.parseColor("#3A2A12"),
                            14f,
                            GOLD,
                            2
                        )

                    name.setTextColor(
                        GOLD_LIGHT
                    )

                    icon.setTextColor(
                        GOLD_LIGHT
                    )

                    view.scaleX = 1.035f

                    view.scaleY = 1.035f

                } else {

                    val selected =
                        group == currentGroup

                    view.background =
                        roundedBackground(
                            if (selected)
                                Color.parseColor("#211A10")
                            else
                                CARD,
                            14f,
                            if (selected)
                                GOLD_DARK
                            else
                                Color.TRANSPARENT,
                            if (selected) 1 else 0
                        )

                    name.setTextColor(
                        if (selected)
                            GOLD_LIGHT
                        else
                            TEXT_PRIMARY
                    )

                    icon.setTextColor(
                        if (selected)
                            GOLD
                        else
                            SILVER
                    )

                    view.scaleX = 1f

                    view.scaleY = 1f
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

    // ============================================================
    // SHOW CHANNELS
    // ============================================================

    private fun showChannels(group: String) {

        currentGroup = group

        val groupChannels =
            channels.filter {
                it.group == group
            }

        currentGroupText.text =
            packageDisplayName(group)

        channelCountText.text =
            "${groupChannels.size} قناة"

        channelLayout.removeAllViews()

        groupChannels.forEachIndexed {
                index,
                channel ->

            val isPlaying =
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
                        dp(6f),
                        dp(10f),
                        dp(6f)
                    )

                    background =
                        roundedBackground(
                            if (isPlaying)
                                Color.parseColor("#302216")
                            else
                                CARD_2,
                            14f,
                            if (isPlaying)
                                GOLD_DARK
                            else
                                Color.TRANSPARENT,
                            if (isPlaying) 1 else 0
                        )

                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        defaultFocusHighlightEnabled = false
                    }
                }

            val topRow =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL
                }

            val quality = text(
                if (
                    channel.name.contains(
                        "4K",
                        true
                    )
                ) {
                    "4K"
                } else {
                    "HD"
                },
                8f,
                if (isPlaying)
                    GOLD_LIGHT
                else
                    GOLD,
                true
            ).apply {

                gravity = Gravity.CENTER

                background =
                    roundedBackground(
                        Color.parseColor("#251D12"),
                        7f
                    )
            }

            topRow.addView(

                quality,

                LinearLayout.LayoutParams(
                    dp(32f),
                    dp(18f)
                )
            )

            val number = text(
                String.format(
                    Locale.getDefault(),
                    "%02d",
                    index + 1
                ),
                9f,
                SILVER_SOFT,
                true
            ).apply {

                gravity = Gravity.CENTER
            }

            topRow.addView(

                number,

                LinearLayout.LayoutParams(
                    0,
                    dp(18f),
                    1f
                )
            )

            val live = text(
                "LIVE",
                7f,
                GREEN,
                true
            ).apply {

                gravity = Gravity.CENTER
            }

            topRow.addView(

                live,

                LinearLayout.LayoutParams(
                    dp(34f),
                    dp(18f)
                )
            )

            card.addView(

                topRow,

                LinearLayout.LayoutParams(
                    dp(150f),
                    dp(20f)
                )
            )

            val name = text(
                if (isPlaying)
                    "▶ ${channel.name}"
                else
                    channel.name,
                11f,
                if (isPlaying)
                    GOLD_LIGHT
                else
                    TEXT_PRIMARY,
                true
            ).apply {

                gravity = Gravity.CENTER

                maxLines = 1
            }

            card.addView(

                name,

                LinearLayout.LayoutParams(
                    dp(150f),
                    dp(26f)
                )
            )

            val smallStatus = text(
                if (isPlaying)
                    "يتم التشغيل الآن"
                else
                    "بث مباشر",
                7f,
                if (isPlaying)
                    GOLD
                else
                    TEXT_SECONDARY
            ).apply {

                gravity = Gravity.CENTER
            }

            card.addView(

                smallStatus,

                LinearLayout.LayoutParams(
                    dp(150f),
                    dp(14f)
                )
            )

            card.layoutParams =
                LinearLayout.LayoutParams(
                    dp(160f),
                    dp(82f)
                ).apply {

                    marginStart = dp(4f)

                    marginEnd = dp(4f)
                }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                if (hasFocus) {

                    view.background =
                        roundedBackground(
                            Color.parseColor("#3A2A12"),
                            14f,
                            GOLD,
                            2
                        )

                    name.setTextColor(
                        WHITE
                    )

                    number.setTextColor(
                        GOLD_LIGHT
                    )

                    view.scaleX = 1.035f

                    view.scaleY = 1.035f

                } else {

                    view.background =
                        roundedBackground(
                            if (isPlaying)
                                Color.parseColor("#302216")
                            else
                                CARD_2,
                            14f,
                            if (isPlaying)
                                GOLD_DARK
                            else
                                Color.TRANSPARENT,
                            if (isPlaying) 1 else 0
                        )

                    name.setTextColor(
                        if (isPlaying)
                            GOLD_LIGHT
                        else
                            TEXT_PRIMARY
                    )

                    number.setTextColor(
                        SILVER_SOFT
                    )

                    view.scaleX = 1f

                    view.scaleY = 1f
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

            if (
                index == 0 &&
                isOverlayVisible
            ) {

                card.post {

                    card.requestFocus()
                }
            }
        }
    }

    // ============================================================
    // REFRESH CHANNEL CARDS
    // ============================================================

    private fun refreshChannelCards() {

        if (currentGroup.isNotEmpty()) {

            showChannels(
                currentGroup
            )
        }
    }

    // ============================================================
    // CURRENT INFO
    // ============================================================

    private fun updateCurrentInfo() {

        val channel =
            currentChannel
                ?: run {

                    currentChannelText.text =
                        "اختر قناة للبدء"

                    currentGroupText.text =
                        "البث الرياضي المباشر"

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

    // ============================================================
    // PACKAGE ICON
    // ============================================================

    private fun packageIcon(
        group: String
    ): String {

        return when (group) {

            "ALWAN SPORT" -> "✦"

            "beIN SPORTS" -> "◈"

            "beIN SPORTS VIP" -> "♛"

            "THAMANYA" -> "8"

            "ALKASS SPORT" -> "◆"

            "AD SPORT" -> "A"

            "DUBAI SPORT" -> "D"

            "IRAQIA SPORT" -> "I"

            else -> "N"
        }
    }

    // ============================================================
    // PACKAGE DISPLAY NAME
    // ============================================================

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
                "الكأس الرياضية"

            "AD SPORT" ->
                "أبوظبي الرياضية"

            "DUBAI SPORT" ->
                "دبي الرياضية"

            "IRAQIA SPORT" ->
                "العراقية الرياضية"

            else ->
                group
        }
    }

    // ============================================================
    // HIDE OVERLAY
    // ============================================================

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

    // ============================================================
    // SHOW OVERLAY
    // ============================================================

    private fun showOverlay() {

        isOverlayVisible = true

        overlay.visibility =
            View.VISIBLE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        updateCurrentInfo()

        focusCurrentChannel()
    }

    // ============================================================
    // FOCUS CURRENT CHANNEL
    // ============================================================

    private fun focusCurrentChannel() {

        if (channelLayout.childCount == 0) {
            return
        }

        val groupChannels =
            channels.filter {
                it.group == currentGroup
            }

        val currentPosition =
            groupChannels.indexOf(
                currentChannel
            )

        if (
            currentPosition >= 0 &&
            currentPosition <
            channelLayout.childCount
        ) {

            channelLayout
                .getChildAt(currentPosition)
                .requestFocus()

        } else {

            channelLayout
                .getChildAt(0)
                .requestFocus()
        }
    }

    // ============================================================
    // NEXT CHANNEL
    // ============================================================

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
            ) {

                currentPosition + 1

            } else {

                0
            }

        val next =
            groupChannels[nextPosition]

        playChannel(
            next,
            channels.indexOf(next)
        )
    }

    // ============================================================
    // PREVIOUS CHANNEL
    // ============================================================

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
            if (currentPosition > 0) {

                currentPosition - 1

            } else {

                groupChannels.lastIndex
            }

        val previous =
            groupChannels[previousPosition]

        playChannel(
            previous,
            channels.indexOf(previous)
        )
    }

    // ============================================================
    // REMOTE / KEYBOARD CONTROL & EXIT FIXED
    // ============================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action !=
            KeyEvent.ACTION_DOWN
        ) {

            return super.dispatchKeyEvent(
                event
            )
        }

        when (event.keyCode) {

            // ----------------------------------------------------
            // OK
            // ----------------------------------------------------

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }
            }

            // ----------------------------------------------------
            // BACK (تم إصلاح الخروج من التطبيق هنا)
            // ----------------------------------------------------

            KeyEvent.KEYCODE_BACK -> {

                /*
                 * إذا كانت الواجهة مخفية (الشاشة كاملة للمشغل):
                 * نُعيد إظهار القائمة.
                 *
                 * إذا كانت القائمة ظاهرة بالفعل:
                 * نقوم بالخروج من التطبيق عبر finish().
                 */

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                } else {

                    finish()

                    return true
                }
            }

            // ----------------------------------------------------
            // RIGHT
            // ----------------------------------------------------

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (!isOverlayVisible) {

                    nextChannel()

                    return true
                }
            }

            // ----------------------------------------------------
            // LEFT
            // ----------------------------------------------------

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (!isOverlayVisible) {

                    previousChannel()

                    return true
                }
            }

            // ----------------------------------------------------
            // UP / DOWN
            // ----------------------------------------------------

            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

    // ============================================================
    // TIME
    // ============================================================

    private fun updateTime() {

        if (
            ::timeText.isInitialized
        ) {

            val sdf =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            timeText.text =
                sdf.format(Date())
        }
    }

    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    // ============================================================
    // PAUSE
    // ============================================================

    override fun onPause() {

        super.onPause()

        player?.pause()
    }

    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        player?.release()

        player = null

        super.onDestroy()
    }

    // ============================================================
    // CHANNEL DATA
    // ============================================================

    private fun loadChannelsData() {

        channels.clear()

        // ========================================================
        // ALWAN SPORT
        // ========================================================

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

        // ========================================================
        // beIN SPORTS
        // ========================================================

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

        // ========================================================
        // beIN SPORTS VIP
        // ========================================================

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

        // ========================================================
        // THAMANYA
        // ========================================================

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

        // ========================================================
        // ALKASS SPORT
        // ========================================================

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

        // ========================================================
        // AD SPORT
        // ========================================================

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

        // ========================================================
        // DUBAI SPORT
        // ========================================================

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

        // ========================================================
        // IRAQIA SPORT
        // ========================================================

        channels.add(
            Channel(
                "IRAQIA SPORT HD",
                "IRAQIA SPORT",
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"
            )
        )
    }
}
