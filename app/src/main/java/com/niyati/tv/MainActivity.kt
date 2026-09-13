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
// COLORS
// ============================================================
private val BG = Color.parseColor("#030407")
private val BLACK = Color.BLACK
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
// CREATE
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
// BACKGROUND
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
        if (
            strokeColor != Color.TRANSPARENT &&
            strokeWidth > 0
        ) {
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
        resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
// STATUS
// ============================================================
private fun statusTextSafe(value: String) {
    if (::statusText.isInitialized) {
        statusText.text = value
    }
}
// ============================================================
// PLAY
// ============================================================
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
// ============================================================
// RECONNECT
// ============================================================
private fun reconnectChannel() {
    val channel =
        currentChannel ?: return
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
// INTERFACE
// ============================================================
private fun createInterface() {
    val root = FrameLayout(this).apply {
        setBackgroundColor(BG)
    }
    // PLAYER
    root.addView(
        playerView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    )
    // OVERLAY
    overlay = LinearLayout(this).apply {
        orientation =
            LinearLayout.VERTICAL
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
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER_VERTICAL
        layoutDirection =
            View.LAYOUT_DIRECTION_RTL
    }
    logoView = ImageView(this).apply {
        setImageResource(
            R.drawable.niyati_logo
        )
        scaleType =
            ImageView.ScaleType.FIT_CENTER
        contentDescription =
            "NIYATI TV"
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
    liveBox.addView(
        text(
            "●",
            10f,
            GREEN,
            true
        )
    )
    liveBox.addView(
        text(
            " LIVE",
            10f,
            SILVER,
            true
        )
    )
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
        gravity =
            Gravity.CENTER
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
    // PACKAGES
    // ========================================================
    overlay.addView(
        createSectionHeader(
            "الباقات الرياضية",
            "اختر الباقة"
        ),
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
    packageScroll.addView(packageLayout)
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
    // CHANNELS
    // ========================================================
    overlay.addView(
        createSectionHeader(
            "القنوات",
            "اضغط OK للتشغيل بملء الشاشة"
        ),
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
    channelScroll.addView(channelLayout)
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
    // HERO
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
    heroInfo.addView(statusText)
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
        gravity =
            Gravity.CENTER
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
    // FOOTER
    // ========================================================
    val footer = LinearLayout(this).apply {
        orientation =
            LinearLayout.HORIZONTAL
        gravity =
            Gravity.CENTER
        layoutDirection =
            View.LAYOUT_DIRECTION_RTL
    }
    footer.addView(
        text(
            "▲ ▼ التنقل   •   OK تشغيل ملء الشاشة   •   BACK خروج / القائمة",
            9f,
            TEXT_SECONDARY
        ).apply {
            gravity = Gravity.CENTER
        }
    )
    overlay.addView(
        footer,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(24f)
        )
    )
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
    titleBox.addView(
        text(
            title,
            13f,
            WHITE,
            true
        )
    )
    box.addView(
        titleBox,
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )
    )
    box.addView(
        text(
            subtitle,
            9f,
            TEXT_SECONDARY
        ).apply {
            gravity = Gravity.CENTER
        }
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
                        14f
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
        val qualityText =
            when {
                channel.name.contains(
                    "4K",
                    true
                ) -> "4K"
                channel.name.contains(
                    "FHD",
                    true
                ) -> "FHD"
                channel.name.contains(
                    "HEVC",
                    true
                ) -> "HEVC"
                channel.name.contains(
                    "SD",
                    true
                ) -> "SD"
                else -> "HD"
            }
        val quality = text(
            qualityText,
            7f,
            if (isPlaying)
                GOLD_LIGHT
            else
                GOLD,
            true
        ).apply {
            gravity =
                Gravity.CENTER
            background =
                roundedBackground(
                    Color.parseColor("#251D12"),
                    7f
                )
        }
        topRow.addView(
            quality,
            LinearLayout.LayoutParams(
                dp(38f),
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
            10f,
            if (isPlaying)
                GOLD_LIGHT
            else
                TEXT_PRIMARY,
            true
        ).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize =
                android.text.TextUtils.TruncateAt.END
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
                name.setTextColor(WHITE)
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
// REFRESH
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
        "AR| SOLO SPORTS" ->
            "S"
        "AR| ALWAN SPORTS" ->
            "✦"
        "AR| BEIN SPORTS" ->
            "◈"
        "AR| ALRABIAA SPORTS" ->
            "R"
        "AR| AL KASS SPORTS" ->
            "◆"
        "AD| ABU DHABI / STARZPLAY" ->
            "A"
        "AR| THMANYAH" ->
            "8"
        "AR| BUNDESLIGA (Shahid)" ->
            "B"
        else ->
            "N"
    }
}
// ============================================================
// PACKAGE DISPLAY NAME
// ============================================================
private fun packageDisplayName(
    group: String
): String {
    return when (group) {
        "AR| SOLO SPORTS" ->
            "SOLO SPORTS"
        "AR| ALWAN SPORTS" ->
            "ألوان سبورت"
        "AR| BEIN SPORTS" ->
            "beIN SPORTS"
        "AR| ALRABIAA SPORTS" ->
            "الرابعة الرياضية"
        "AR| AL KASS SPORTS" ->
            "الكأس الرياضية"
        "AD| ABU DHABI / STARZPLAY" ->
            "أبوظبي / STARZPLAY"
        "AR| THMANYAH" ->
            "ثمانية"
        "AR| BUNDESLIGA (Shahid)" ->
            "Bundesliga • شاهد"
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
// FOCUS
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
// NEXT
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
// PREVIOUS
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
// REMOTE
// ============================================================
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
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER -> {
            if (!isOverlayVisible) {
                showOverlay()
                return true
            }
        }
        KeyEvent.KEYCODE_BACK -> {
            if (!isOverlayVisible) {
                showOverlay()
                return true
            } else {
                finish()
                return true
            }
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (!isOverlayVisible) {
                nextChannel()
                return true
            }
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
            if (!isOverlayVisible) {
                previousChannel()
                return true
            }
        }
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
// ============================================================
// TIME
// ============================================================
private fun updateTime() {
    if (::timeText.isInitialized) {
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
    handler.removeCallbacksAndMessages(null)
    player?.release()
    player = null
    super.onDestroy()
}
// ============================================================
// CHANNEL DATA
// NEW M3U CHANNELS
// ============================================================
private fun loadChannelsData() {
    channels.clear()
    // ========================================================
    // AR| SOLO SPORTS
    // ========================================================
    val soloLogo =
        "https://scontent.fcmn3-1.fna.fbcdn.net/v/t39.30808-1/536268349_1188643553288426_5194102762508666988_n.jpg"
    add(
        "AR| SOLO SPORT HDR 4K",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/249999"
    )
    add(
        "AR| SOLO SPORT 1 FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250000"
    )
    add(
        "AR| SOLO SPORT 1 HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250001"
    )
    add(
        "AR| SOLO SPORT 1 HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250002"
    )
    add(
        "AR| SOLO SPORT 1 SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250003"
    )
    add(
        "AR| SOLO SPORT 2 FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250004"
    )
    add(
        "AR| SOLO SPORT 2 HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250005"
    )
    add(
        "AR| SOLO SPORT 2 HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250006"
    )
    add(
        "AR| SOLO SPORT 2 SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250007"
    )
    add(
        "AR| SOLO SPORT 3 FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250008"
    )
    add(
        "AR| SOLO SPORT 3 HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250009"
    )
    add(
        "AR| SOLO SPORT 3 HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250010"
    )
    add(
        "AR| SOLO SPORT 3 SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250011"
    )
    add(
        "AR| SOLO SPORT 4 FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250012"
    )
    add(
        "AR| SOLO SPORT 4 HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250013"
    )
    add(
        "AR| SOLO SPORT 4 HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250014"
    )
    add(
        "AR| SOLO SPORT 4 SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250015"
    )
    add(
        "AR| SOLO SPORT 5 FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250016"
    )
    add(
        "AR| SOLO SPORT 5 HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250017"
    )
    add(
        "AR| SOLO SPORT 5 HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250018"
    )
    add(
        "AR| SOLO SPORT 5 SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250019"
    )
    add(
        "AR| SOLO SPORT UFC FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250020"
    )
    add(
        "AR| SOLO SPORT UFC HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250021"
    )
    add(
        "AR| SOLO SPORT UFC HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250022"
    )
    add(
        "AR| SOLO SPORT UFC SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250023"
    )
    add(
        "AR| SOLO SPORT F1 FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250024"
    )
    add(
        "AR| SOLO SPORT F1 HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250025"
    )
    add(
        "AR| SOLO SPORT F1 HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250026"
    )
    add(
        "AR| SOLO SPORT F1 SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250027"
    )
    add(
        "AR| SOLO SPORT WWE FHD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250028"
    )
    add(
        "AR| SOLO SPORT WWE HEVC",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250029"
    )
    add(
        "AR| SOLO SPORT WWE HD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250030"
    )
    add(
        "AR| SOLO SPORT WWE SD",
        "AR| SOLO SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/250031"
    )
    // ========================================================
    // AR| ALWAN SPORTS
    // ========================================================
    add(
        "AR| ALWAN SPORT 1 4K",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248386"
    )
    add(
        "AR| ALWAN SPORT 1 HD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248387"
    )
    add(
        "AR| ALWAN SPORT 1 SD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248388"
    )
    add(
        "AR| ALWAN SPORT 2 4K",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248389"
    )
    add(
        "AR| ALWAN SPORT 2 HD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248390"
    )
    add(
        "AR| ALWAN SPORT 2 SD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248391"
    )
    add(
        "AR| ALWAN SPORT 3 4K",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248392"
    )
    add(
        "AR| ALWAN SPORT 3 HD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248393"
    )
    add(
        "AR| ALWAN SPORT 3 SD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248394"
    )
    add(
        "AR| ALWAN SPORT 4 HD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248395"
    )
    add(
        "AR| ALWAN SPORT 4 SD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248396"
    )
    add(
        "AR| ALWAN SPORT 5 HD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248397"
    )
    add(
        "AR| ALWAN SPORT 5 SD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248398"
    )
    add(
        "AR| ALWAN SPORT 6 HD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248399"
    )
    add(
        "AR| ALWAN SPORT 6 SD",
        "AR| ALWAN SPORTS",
        "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248400"
    )
    // ========================================================
    // AR| BEIN SPORTS
    // ========================================================
    add("AR| BEIN SPORTS HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248344")
    add("AR| BEIN SPORTS SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248345")
    add("AR| BEIN SPORTS NEWS HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248346")
    add("AR| BEIN SPORTS 1 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248347")
    add("AR| BEIN SPORTS 1 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248348")
    add("AR| BEIN SPORTS 2 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248349")
    add("AR| BEIN SPORTS 2 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248350")
    add("AR| BEIN SPORTS 3 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248351")
    add("AR| BEIN SPORTS 3 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248352")
    add("AR| BEIN SPORTS 4 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248353")
    add("AR| BEIN SPORTS 4 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248354")
    add("AR| BEIN SPORTS 5 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248355")
    add("AR| BEIN SPORTS 5 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248356")
    add("AR| BEIN SPORTS 6 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248357")
    add("AR| BEIN SPORTS 6 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248358")
    add("AR| BEIN SPORTS 7 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248359")
    add("AR| BEIN SPORTS 7 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248360")
    add("AR| BEIN SPORTS 8 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248361")
    add("AR| BEIN SPORTS 8 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248362")
    add("AR| BEIN SPORTS 9 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248363")
    add("AR| BEIN SPORTS 9 SD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248364")
    add("AR| BEIN SPORTS ENGLISH 1 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248365")
    add("AR| BEIN SPORTS ENGLISH 2 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248366")
    add("AR| BEIN SPORTS FRENCH 1 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248367")
    add("AR| BEIN SPORTS FRENCH 2 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248368")
    add("AR| BEIN SPORTS NBA HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248369")
    add("AR| BEIN SPORTS XTRA 1 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248370")
    add("AR| BEIN SPORTS XTRA 2 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248371")
    add("AR| BEIN SPORTS XTRA 3 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248372")
    add("AR| BEIN SPORTS AFC 1 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248373")
    add("AR| BEIN SPORTS AFC 2 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248374")
    add("AR| BEIN SPORTS AFC 3 HD", "AR| BEIN SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248375")
    // ========================================================
    // AR| ALRABIAA SPORTS
    // ========================================================
    add("AR| IRAQIA AL RABIAA SPORT 1 HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248401")
    add("AR| IRAQIA AL RABIAA SPORT 2 HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248402")
    add("AR| AL RABIAA NEWS HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248403")
    add("AR| AL RABIAA GEO HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248404")
    add("AR| AL RABIAA MOVIES HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248405")
    add("AR| AL RABIAA MUSIC HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248406")
    add("AR| AL RABIAA NOW HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248407")
    add("AR| AL RABIAA QURAN HD", "AR| ALRABIAA SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248408")
    // ========================================================
    // AR| AL KASS SPORTS
    // ========================================================
    add("AR| AL KASS 1 HD", "AR| AL KASS SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248409")
    add("AR| AL KASS 2 HD", "AR| AL KASS SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248410")
    add("AR| AL KASS 3 HD", "AR| AL KASS SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248411")
    add("AR| AL KASS 4 HD", "AR| AL KASS SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248412")
    add("AR| AL KASS 5 HD", "AR| AL KASS SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248413")
    add("AR| AL KASS 6 HD", "AR| AL KASS SPORTS", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248414")
    // ========================================================
    // AD| ABU DHABI / STARZPLAY
    // ========================================================
    add("AD| ABU DHABI SPORT PREMIUM 1 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248415")
    add("AD| ABU DHABI SPORT PREMIUM 2 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248416")
    add("AD| ABU DHABI SPORT ASIA 1 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248417")
    add("AD| ABU DHABI SPORT ASIA 2 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248418")
    add("AD| STARZPLAY CRICLIFE 1 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248419")
    add("AD| STARZPLAY CRICLIFE 2 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248420")
    add("AD| STARZPLAY GOLF HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248421")
    add("AD| STARZPLAY SPORT 1 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248422")
    add("AD| STARZPLAY SPORT 2 HD", "AD| ABU DHABI / STARZPLAY", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248423")
    // ========================================================
    // AR| THMANYAH
    // ========================================================
    add("AR| THMANYAH 1 FHD", "AR| THMANYAH", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248424")
    add("AR| THMANYAH 1 HD", "AR| THMANYAH", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248425")
    add("AR| THMANYAH 2 FHD", "AR| THMANYAH", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248426")
    add("AR| THMANYAH 2 HD", "AR| THMANYAH", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248427")
    add("AR| THMANYAH 3 FHD", "AR| THMANYAH", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248428")
    add("AR| THMANYAH 3 HD", "AR| THMANYAH", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248429")
    // ========================================================
    // AR| BUNDESLIGA
    // ========================================================
    add("AR| BUNDESLIGA 1 HD", "AR| BUNDESLIGA (Shahid)", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248430")
    add("AR| BUNDESLIGA 2 HD", "AR| BUNDESLIGA (Shahid)", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248431")
    add("AR| BUNDESLIGA 3 HD", "AR| BUNDESLIGA (Shahid)", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248432")
    add("AR| BUNDESLIGA 4 HD", "AR| BUNDESLIGA (Shahid)", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248433")
    add("AR| BUNDESLIGA 5 HD", "AR| BUNDESLIGA (Shahid)", "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/248434")
}
// ============================================================
// ADD CHANNEL HELPER
// ============================================================
private fun add(
    name: String,
    group: String,
    url: String
) {
    channels.add(
        Channel(
            name = name,
            group = group,
            url = url
        )
    )
}

}