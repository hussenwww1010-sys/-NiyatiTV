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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

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

    // =========================================================
    // MAIN UI
    // =========================================================

    private lateinit var overlay: LinearLayout
    private lateinit var packageLayout: LinearLayout
    private lateinit var channelLayout: LinearLayout

    private lateinit var packageScroll: ScrollView
    private lateinit var channelScroll: ScrollView

    private lateinit var currentChannelText: TextView
    private lateinit var currentGroupText: TextView
    private lateinit var channelCountText: TextView
    private lateinit var liveBadge: TextView
    private lateinit var statusText: TextView

    private var player: ExoPlayer? = null

    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentChannel: Channel? = null

    private var isOverlayVisible = true
    private var isPlayingChannel = false

    private val handler = Handler(Looper.getMainLooper())

    private val channels = mutableListOf<Channel>()

    // =========================================================
    // COLORS - NIYATI TV PREMIUM
    // =========================================================

    private val BG = Color.parseColor("#070A0F")
    private val PANEL = Color.parseColor("#E90C1119")
    private val CARD = Color.parseColor("#D9141B26")
    private val CARD_DARK = Color.parseColor("#C90E141D")

    private val WHITE = Color.WHITE
    private val TEXT = Color.parseColor("#E8EDF5")
    private val MUTED = Color.parseColor("#8D99AA")

    private val CYAN = Color.parseColor("#00D9FF")
    private val CYAN_DARK = Color.parseColor("#008EB0")

    private val GREEN = Color.parseColor("#35E58A")
    private val RED = Color.parseColor("#FF4D5A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        loadChannelsData()
        createPlayer()
        createInterface()

        if (channels.isNotEmpty()) {
            val firstGroup = channels.first().group
            showChannels(firstGroup)
        }
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
                setStroke(dp(strokeWidth.toFloat()), strokeColor)
            }
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int = WHITE,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }

            gravity = Gravity.CENTER_VERTICAL
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

            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

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
                            statusTextSafe("جاري الاتصال...")
                        }

                        Player.STATE_READY -> {
                            isPlayingChannel = true
                            statusTextSafe("البث مستقر")
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

                    statusTextSafe("إعادة الاتصال بالبث...")

                    Toast.makeText(
                        this@MainActivity,
                        "تعذر تشغيل القناة، جاري إعادة الاتصال...",
                        Toast.LENGTH_SHORT
                    ).show()

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

        hideOverlay()
    }

    // =========================================================
    // RECONNECT
    // =========================================================

    private fun reconnectChannel() {

        val channel = currentChannel ?: return

        handler.postDelayed({

            if (!isFinishing) {

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
            }

        }, 1500)
    }

    // =========================================================
    // MAIN INTERFACE
    // =========================================================

    private fun createInterface() {

        val root = FrameLayout(this).apply {
            setBackgroundColor(BG)
        }

        // -----------------------------------------------------
        // PLAYER - FULL BACKGROUND
        // -----------------------------------------------------

        root.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // -----------------------------------------------------
        // DARK GRADIENT-LIKE PANEL
        // -----------------------------------------------------

        overlay = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(16f),
                dp(14f),
                dp(16f),
                dp(14f)
            )

            background = roundedBackground(
                PANEL,
                22f,
                Color.parseColor("#2633414D"),
                1
            )

            elevation = dp(12f).toFloat()
        }

        // -----------------------------------------------------
        // RESPONSIVE WIDTH
        // -----------------------------------------------------

        val screenWidth = resources.displayMetrics.widthPixels

        val panelWidth = when {

            screenWidth < dp(600f) ->
                (screenWidth * 0.94f).toInt()

            screenWidth < dp(1000f) ->
                (screenWidth * 0.78f).toInt()

            else ->
                dp(720f)
        }

        val panelHeight = when {

            screenWidth < dp(600f) ->
                FrameLayout.LayoutParams.MATCH_PARENT

            else ->
                dp(520f)
        }

        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        val header = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(8f),
                dp(4f),
                dp(8f),
                dp(12f)
            )
        }

        // BRAND

        val brandBox = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = text(
            "NIYATI",
            22f,
            WHITE,
            true
        )

        brand.letterSpacing = 0.12f

        brandBox.addView(
            brand,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(30f)
            )
        )

        val tv = text(
            "SPORTS TV",
            10f,
            CYAN,
            true
        )

        tv.letterSpacing = 0.18f

        brandBox.addView(
            tv,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(20f)
            )
        )

        header.addView(
            brandBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // LIVE BADGE

        liveBadge = text(
            "●  LIVE",
            11f,
            WHITE,
            true
        ).apply {

            gravity = Gravity.CENTER

            setPadding(
                dp(13f),
                0,
                dp(13f),
                0
            )

            background = roundedBackground(
                Color.parseColor("#CC12301F"),
                50f,
                GREEN,
                1
            )
        }

        header.addView(
            liveBadge,
            LinearLayout.LayoutParams(
                dp(85f),
                dp(34f)
            ).apply {
                marginStart = dp(8f)
            }
        )

        overlay.addView(header)

        // -----------------------------------------------------
        // CURRENT CHANNEL BAR
        // -----------------------------------------------------

        val currentBar = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            layoutDirection = View.LAYOUT_DIRECTION_RTL

            setPadding(
                dp(15f),
                dp(12f),
                dp(15f),
                dp(12f)
            )

            background = roundedBackground(
                Color.parseColor("#C90B111A"),
                16f,
                Color.parseColor("#26384959"),
                1
            )
        }

        currentGroupText = text(
            "البث الرياضي",
            11f,
            CYAN,
            true
        )

        currentBar.addView(
            currentGroupText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(20f)
            )
        )

        currentChannelText = text(
            "اختر قناة للبدء",
            17f,
            WHITE,
            true
        )

        currentBar.addView(
            currentChannelText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30f)
            )
        )

        val statusRow = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        statusText = text(
            "جاهز للبث",
            10f,
            MUTED
        )

        statusRow.addView(
            statusText,
            LinearLayout.LayoutParams(
                0,
                dp(22f),
                1f
            )
        )

        channelCountText = text(
            "0 قناة",
            10f,
            MUTED
        )

        channelCountText.gravity = Gravity.CENTER

        statusRow.addView(
            channelCountText,
            LinearLayout.LayoutParams(
                dp(90f),
                dp(22f)
            )
        )

        currentBar.addView(
            statusRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22f)
            )
        )

        overlay.addView(
            currentBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(90f)
            ).apply {
                bottomMargin = dp(12f)
            }
        )

        // -----------------------------------------------------
        // CONTENT
        // -----------------------------------------------------

        val content = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // -----------------------------------------------------
        // PACKAGES
        // -----------------------------------------------------

        val packagesBox = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(0f),
                dp(0f),
                dp(8f),
                dp(0f)
            )
        }

        val packagesTitle = text(
            "الباقات",
            15f,
            WHITE,
            true
        )

        packagesTitle.setPadding(
            dp(4f),
            0,
            dp(4f),
            dp(8f)
        )

        packagesBox.addView(
            packagesTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34f)
            )
        )

        packageScroll = ScrollView(this).apply {

            isFillViewport = true

            overScrollMode = View.OVER_SCROLL_NEVER
        }

        packageLayout = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
        }

        packageScroll.addView(packageLayout)

        packagesBox.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        content.addView(
            packagesBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.38f
            )
        )

        // -----------------------------------------------------
        // CHANNELS
        // -----------------------------------------------------

        val channelsBox = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(8f),
                dp(0f),
                dp(0f),
                dp(0f)
            )
        }

        val channelTitle = text(
            "القنوات",
            15f,
            WHITE,
            true
        )

        channelTitle.setPadding(
            dp(4f),
            0,
            dp(4f),
            dp(8f)
        )

        channelsBox.addView(
            channelTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34f)
            )
        )

        channelScroll = ScrollView(this).apply {

            isFillViewport = true

            overScrollMode = View.OVER_SCROLL_NEVER
        }

        channelLayout = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
        }

        channelScroll.addView(channelLayout)

        channelsBox.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        content.addView(
            channelsBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.62f
            )
        )

        overlay.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // -----------------------------------------------------
        // FOOTER
        // -----------------------------------------------------

        val footer = text(
            "↑ ↓ تنقل   •   OK تشغيل   •   BACK إظهار القائمة",
            10f,
            MUTED
        )

        footer.gravity = Gravity.CENTER

        overlay.addView(
            footer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30f)
            )
        )

        // -----------------------------------------------------
        // ADD PANEL
        // -----------------------------------------------------

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                panelWidth,
                panelHeight,
                Gravity.CENTER
            )
        )

        setContentView(root)

        buildPackages()
    }

    // =========================================================
    // BUILD PACKAGES
    // =========================================================

    private fun buildPackages() {

        packageLayout.removeAllViews()

        val groups = channels
            .map { it.group }
            .distinct()

        groups.forEachIndexed { index, group ->

            val groupChannels =
                channels.filter { it.group == group }

            val card = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER_VERTICAL

                layoutDirection = View.LAYOUT_DIRECTION_RTL

                isFocusable = true

                isFocusableInTouchMode = true

                setPadding(
                    dp(10f),
                    0,
                    dp(10f),
                    0
                )

                background = roundedBackground(
                    CARD_DARK,
                    13f
                )
            }

            // ICON

            val icon = TextView(this).apply {

                text = packageIcon(group)

                textSize = 18f

                gravity = Gravity.CENTER

                setTextColor(WHITE)
            }

            card.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(38f),
                    dp(42f)
                )
            )

            // NAME

            val name = text(
                packageDisplayName(group),
                13f,
                TEXT,
                true
            )

            name.maxLines = 1

            card.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    dp(42f),
                    1f
                )
            )

            // COUNT

            val count = text(
                groupChannels.size.toString(),
                10f,
                MUTED,
                true
            )

            count.gravity = Gravity.CENTER

            card.addView(
                count,
                LinearLayout.LayoutParams(
                    dp(32f),
                    dp(25f)
                )
            )

            card.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48f)
                ).apply {

                    bottomMargin = dp(7f)
                }

            card.setOnFocusChangeListener { view, hasFocus ->

                if (hasFocus) {

                    view.background =
                        roundedBackground(
                            Color.parseColor("#CC083344"),
                            13f,
                            CYAN,
                            1
                        )

                    name.setTextColor(WHITE)

                } else {

                    val selected =
                        group == currentGroup

                    view.background =
                        roundedBackground(
                            if (selected)
                                Color.parseColor("#C9152730")
                            else
                                CARD_DARK,
                            13f,
                            if (selected)
                                Color.parseColor("#176A7A")
                            else
                                Color.TRANSPARENT,
                            if (selected) 1 else 0
                        )

                    name.setTextColor(
                        if (selected)
                            CYAN
                        else
                            TEXT
                    )
                }
            }

            card.setOnClickListener {

                showChannels(group)

                if (channelLayout.childCount > 0) {
                    channelLayout.getChildAt(0).requestFocus()
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

        channelCountText.text =
            "${groupChannels.size} قناة"

        channelLayout.removeAllViews()

        groupChannels.forEachIndexed { index, channel ->

            val isPlaying =
                channel == currentChannel

            val card = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                gravity = Gravity.CENTER_VERTICAL

                layoutDirection = View.LAYOUT_DIRECTION_RTL

                isFocusable = true

                isFocusableInTouchMode = true

                setPadding(
                    dp(12f),
                    0,
                    dp(12f),
                    0
                )

                background =
                    roundedBackground(
                        if (isPlaying)
                            Color.parseColor("#CC103C2C")
                        else
                            CARD,
                        13f,
                        if (isPlaying)
                            GREEN
                        else
                            Color.TRANSPARENT,
                        if (isPlaying) 1 else 0
                    )
            }

            // CHANNEL NUMBER

            val number = text(
                String.format(
                    "%02d",
                    index + 1
                ),
                11f,
                if (isPlaying)
                    GREEN
                else
                    MUTED,
                true
            )

            number.gravity = Gravity.CENTER

            card.addView(
                number,
                LinearLayout.LayoutParams(
                    dp(35f),
                    dp(44f)
                )
            )

            // CHANNEL NAME

            val name = text(
                if (isPlaying)
                    "▶  ${channel.name}"
                else
                    channel.name,
                13f,
                if (isPlaying)
                    GREEN
                else
                    TEXT,
                true
            )

            name.maxLines = 1

            card.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    dp(44f),
                    1f
                )
            )

            // HD

            val hd = text(
                if (
                    channel.name.contains(
                        "4K",
                        true
                    )
                ) "4K"
                else "HD",
                9f,
                if (isPlaying)
                    GREEN
                else
                    CYAN,
                true
            )

            hd.gravity = Gravity.CENTER

            hd.background =
                roundedBackground(
                    Color.parseColor("#261D3340"),
                    8f
                )

            card.addView(
                hd,
                LinearLayout.LayoutParams(
                    dp(38f),
                    dp(23f)
                )
            )

            card.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(50f)
                ).apply {

                    bottomMargin = dp(7f)
                }

            card.setOnFocusChangeListener { view, hasFocus ->

                if (hasFocus) {

                    view.background =
                        roundedBackground(
                            Color.parseColor("#CC07566B"),
                            13f,
                            CYAN,
                            1
                        )

                    name.setTextColor(WHITE)
                    number.setTextColor(WHITE)

                } else {

                    view.background =
                        roundedBackground(
                            if (isPlaying)
                                Color.parseColor("#CC103C2C")
                            else
                                CARD,
                            13f,
                            if (isPlaying)
                                GREEN
                            else
                                Color.TRANSPARENT,
                            if (isPlaying) 1 else 0
                        )

                    name.setTextColor(
                        if (isPlaying)
                            GREEN
                        else
                            TEXT
                    )

                    number.setTextColor(
                        if (isPlaying)
                            GREEN
                        else
                            MUTED
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

    // =========================================================
    // REFRESH CHANNEL CARDS
    // =========================================================

    private fun refreshChannelCards() {

        if (currentGroup.isNotEmpty()) {
            showChannels(currentGroup)
        }
    }

    // =========================================================
    // CURRENT CHANNEL INFO
    // =========================================================

    private fun updateCurrentInfo() {

        val channel = currentChannel

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
            packageDisplayName(channel.group)

        statusText.text =
            "● بث مباشر"
    }

    // =========================================================
    // PACKAGE ICONS
    // =========================================================

    private fun packageIcon(group: String): String {

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
    // PACKAGE DISPLAY NAMES
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

    // =========================================================
    // HIDE OVERLAY
    // =========================================================

    private fun hideOverlay() {

        isOverlayVisible = false

        overlay.visibility = View.GONE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // =========================================================
    // SHOW OVERLAY
    // =========================================================

    private fun showOverlay() {

        isOverlayVisible = true

        overlay.visibility = View.VISIBLE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        updateCurrentInfo()

        if (channelLayout.childCount > 0) {

            var focused = false

            for (i in 0 until channelLayout.childCount) {

                val child =
                    channelLayout.getChildAt(i)

                if (
                    currentChannel != null &&
                    channels.filter {
                        it.group == currentGroup
                    }.getOrNull(i) == currentChannel
                ) {

                    child.requestFocus()

                    focused = true

                    break
                }
            }

            if (!focused) {
                channelLayout.getChildAt(0).requestFocus()
            }
        }
    }

    // =========================================================
    // NEXT CHANNEL
    // =========================================================

    private fun nextChannel() {

        val groupChannels =
            channels.filter {
                it.group == currentGroup
            }

        if (groupChannels.isEmpty()) return

        val currentPosition =
            groupChannels.indexOf(currentChannel)

        val nextPosition =
            if (
                currentPosition < groupChannels.lastIndex
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

    // =========================================================
    // PREVIOUS CHANNEL
    // =========================================================

    private fun previousChannel() {

        val groupChannels =
            channels.filter {
                it.group == currentGroup
            }

        if (groupChannels.isEmpty()) return

        val currentPosition =
            groupChannels.indexOf(currentChannel)

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

            // -------------------------------------------------
            // OK
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }
            }

            // -------------------------------------------------
            // BACK
            // -------------------------------------------------

            KeyEvent.KEYCODE_BACK -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }

                // لا نخلي زر Back يغلق التطبيق مباشرة
                // إذا المستخدم داخل القائمة، نخليه يبقى بالتطبيق.

                return true
            }

            // -------------------------------------------------
            // RIGHT
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (!isOverlayVisible) {

                    nextChannel()

                    return true
                }
            }

            // -------------------------------------------------
            // LEFT
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (!isOverlayVisible) {

                    previousChannel()

                    return true
                }
            }

            // -------------------------------------------------
            // UP
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_UP -> {

                if (!isOverlayVisible) {

                    showOverlay()

                    return true
                }
            }

            // -------------------------------------------------
            // DOWN
            // -------------------------------------------------

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
        // beIN SPORTS
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
        // beIN SPORTS VIP
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
        // ALKASS SPORT
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
