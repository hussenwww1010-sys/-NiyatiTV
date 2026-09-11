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
    private lateinit var timeText: TextView

    private var player: ExoPlayer? = null

    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentChannel: Channel? = null

    private var isOverlayVisible = true
    private var isPlayingChannel = false

    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    private val channels = mutableListOf<Channel>()

    // =========================================================
    // COLORS - NIYATI SPORTS TV MODERN THEME
    // =========================================================

    private val BG = Color.parseColor("#04070D")
    private val PANEL_BG = Color.parseColor("#F00A0F1A")
    private val CARD_BG = Color.parseColor("#121926")
    private val CARD_FOCUS_BG = Color.parseColor("#00E5FF")
    
    private val WHITE = Color.WHITE
    private val TEXT_PRIMARY = Color.parseColor("#F1F5F9")
    private val TEXT_SECONDARY = Color.parseColor("#94A3B8")

    private val ACCENT_CYAN = Color.parseColor("#00E5FF")
    private val ACCENT_BLUE = Color.parseColor("#0284C7")
    
    private val STATUS_GREEN = Color.parseColor("#10B981")
    private val STATUS_RED = Color.parseColor("#EF4444")

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

    private fun updateTime() {
        if (::timeText.isInitialized) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = sdf.format(Date())
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
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
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
                            statusTextSafe("جاري الاتصال بالبث المباشر...")
                        }
                        Player.STATE_READY -> {
                            isPlayingChannel = true
                            statusTextSafe("البث مستقر  • 1080p 60fps")
                        }
                        Player.STATE_ENDED -> {
                            reconnectChannel()
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isPlayingChannel = false
                    statusTextSafe("فشل الاتصال - جاري إعادة المحاولة...")
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

    private fun playChannel(channel: Channel, index: Int) {

        currentChannel = channel
        currentChannelIndex = index
        currentGroup = channel.group

        updateCurrentInfo()

        val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))

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

    private fun reconnectChannel() {
        val channel = currentChannel ?: return
        handler.postDelayed({
            if (!isFinishing) {
                val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
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

        root.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20f), dp(18f), dp(20f), dp(18f))
            background = roundedBackground(PANEL_BG, 24f, Color.parseColor("#1E293B"), 1)
            elevation = dp(16f).toFloat()
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val panelWidth = when {
            screenWidth < dp(600f) -> (screenWidth * 0.94f).toInt()
            screenWidth < dp(1000f) -> (screenWidth * 0.82f).toInt()
            else -> dp(760f)
        }

        val panelHeight = when {
            screenWidth < dp(600f) -> FrameLayout.LayoutParams.MATCH_PARENT
            else -> dp(540f)
        }

        // -----------------------------------------------------
        // HEADER BAR
        // -----------------------------------------------------

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(6f), dp(0f), dp(6f), dp(14f))
        }

        val brandBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = text("NIYATI", 22f, WHITE, true).apply {
            letterSpacing = 0.15f
        }

        val tv = text("SPORTS IPTV", 10f, ACCENT_CYAN, true).apply {
            letterSpacing = 0.20f
        }

        brandBox.addView(brand)
        brandBox.addView(tv)

        header.addView(
            brandBox,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        timeText = text("--:--", 16f, WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10f), 0, dp(10f), 0)
        }
        header.addView(timeText)

        liveBadge = text("● LIVE", 11f, WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12f), 0, dp(12f), 0)
            background = roundedBackground(Color.parseColor("#15803D"), 50f, STATUS_GREEN, 1)
        }

        header.addView(
            liveBadge,
            LinearLayout.LayoutParams(dp(80f), dp(32f)).apply {
                marginStart = dp(10f)
            }
        )

        overlay.addView(header)

        // -----------------------------------------------------
        // CURRENT CHANNEL INFO PANEL
        // -----------------------------------------------------

        val currentBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
            background = roundedBackground(Color.parseColor("#0F172A"), 16f, Color.parseColor("#334155"), 1)
        }

        currentGroupText = text("البث الرياضي المباشر", 11f, ACCENT_CYAN, true)
        currentBar.addView(currentGroupText)

        currentChannelText = text("اختر قناة للبدء", 18f, WHITE, true)
        currentBar.addView(currentChannelText)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        statusText = text("جاهز للبث", 11f, TEXT_SECONDARY)
        statusRow.addView(statusText, LinearLayout.LayoutParams(0, dp(22f), 1f))

        channelCountText = text("0 قناة", 11f, TEXT_SECONDARY)
        channelCountText.gravity = Gravity.END
        statusRow.addView(channelCountText, LinearLayout.LayoutParams(dp(90f), dp(22f)))

        currentBar.addView(statusRow)

        overlay.addView(
            currentBar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(14f)
            }
        )

        // -----------------------------------------------------
        // CONTENT AREA
        // -----------------------------------------------------

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // PACKAGES COLUMN
        val packagesBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(0f), dp(0f), dp(6f), dp(0f))
        }

        val packagesTitle = text("الباقات الرياضية", 14f, TEXT_SECONDARY, true).apply {
            setPadding(dp(4f), 0, dp(4f), dp(6f))
        }
        packagesBox.addView(packagesTitle)

        packageScroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        packageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        packageScroll.addView(packageLayout)
        packagesBox.addView(packageScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        content.addView(packagesBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.40f))

        // CHANNELS COLUMN
        val channelsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6f), dp(0f), dp(0f), dp(0f))
        }

        val channelTitle = text("القنوات المتاحة", 14f, TEXT_SECONDARY, true).apply {
            setPadding(dp(4f), 0, dp(4f), dp(6f))
        }
        channelsBox.addView(channelTitle)

        channelScroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        channelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        channelScroll.addView(channelLayout)
        channelsBox.addView(channelScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        content.addView(channelsBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.60f))

        overlay.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // -----------------------------------------------------
        // FOOTER
        // -----------------------------------------------------

        val footer = text("▲ ▼ التنقل   •   OK التشغيل   •   BACK القائمة / الإغلاق", 10f, TEXT_SECONDARY).apply {
            gravity = Gravity.CENTER
        }

        overlay.addView(
            footer,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(26f)).apply {
                topMargin = dp(8f)
            }
        )

        root.addView(
            overlay,
            FrameLayout.LayoutParams(panelWidth, panelHeight, Gravity.CENTER)
        )

        setContentView(root)
        buildPackages()
    }

    // =========================================================
    // BUILD PACKAGES
    // =========================================================

    private fun buildPackages() {
        packageLayout.removeAllViews()
        val groups = channels.map { it.group }.distinct()

        groups.forEachIndexed { index, group ->
            val groupChannels = channels.filter { it.group == group }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                isFocusable = true
                isFocusableInTouchMode = true
                setPadding(dp(10f), 0, dp(10f), 0)
                background = roundedBackground(CARD_BG, 12f)
            }

            val icon = TextView(this).apply {
                text = packageIcon(group)
                textSize = 16f
                gravity = Gravity.CENTER
            }

            card.addView(icon, LinearLayout.LayoutParams(dp(32f), dp(40f)))

            val name = text(packageDisplayName(group), 13f, TEXT_PRIMARY, true).apply {
                maxLines = 1
            }

            card.addView(name, LinearLayout.LayoutParams(0, dp(40f), 1f))

            val count = text(groupChannels.size.toString(), 10f, TEXT_SECONDARY, true).apply {
                gravity = Gravity.CENTER
            }

            card.addView(count, LinearLayout.LayoutParams(dp(28f), dp(24f)))

            card.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46f)
            ).apply {
                bottomMargin = dp(6f)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.background = roundedBackground(ACCENT_BLUE, 12f, ACCENT_CYAN, 2)
                    name.setTextColor(WHITE)
                } else {
                    val selected = group == currentGroup
                    view.background = roundedBackground(
                        if (selected) Color.parseColor("#1E293B") else CARD_BG,
                        12f,
                        if (selected) ACCENT_CYAN else Color.TRANSPARENT,
                        if (selected) 1 else 0
                    )
                    name.setTextColor(if (selected) ACCENT_CYAN else TEXT_PRIMARY)
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
                card.post { card.requestFocus() }
            }
        }
    }

    // =========================================================
    // SHOW CHANNELS
    // =========================================================

    private fun showChannels(group: String) {

        currentGroup = group
        val groupChannels = channels.filter { it.group == group }

        currentGroupText.text = packageDisplayName(group)
        channelCountText.text = "${groupChannels.size} قناة"

        channelLayout.removeAllViews()

        groupChannels.forEachIndexed { index, channel ->
            val isPlaying = channel == currentChannel

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                isFocusable = true
                isFocusableInTouchMode = true
                setPadding(dp(12f), 0, dp(12f), 0)
                background = roundedBackground(
                    if (isPlaying) Color.parseColor("#065F46") else CARD_BG,
                    12f,
                    if (isPlaying) STATUS_GREEN else Color.TRANSPARENT,
                    if (isPlaying) 1 else 0
                )
            }

            val number = text(
                String.format("%02d", index + 1),
                11f,
                if (isPlaying) STATUS_GREEN else TEXT_SECONDARY,
                true
            ).apply { gravity = Gravity.CENTER }

            card.addView(number, LinearLayout.LayoutParams(dp(30f), dp(42f)))

            val name = text(
                if (isPlaying) "▶ ${channel.name}" else channel.name,
                13f,
                if (isPlaying) WHITE else TEXT_PRIMARY,
                true
            ).apply { maxLines = 1 }

            card.addView(name, LinearLayout.LayoutParams(0, dp(42f), 1f))

            val hd = text(
                if (channel.name.contains("4K", true)) "4K" else "HD",
                9f,
                if (isPlaying) WHITE else ACCENT_CYAN,
                true
            ).apply {
                gravity = Gravity.CENTER
                background = roundedBackground(Color.parseColor("#0F172A"), 6f)
            }

            card.addView(hd, LinearLayout.LayoutParams(dp(34f), dp(22f)))

            card.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f)
            ).apply {
                bottomMargin = dp(6f)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.background = roundedBackground(ACCENT_BLUE, 12f, ACCENT_CYAN, 2)
                    name.setTextColor(WHITE)
                    number.setTextColor(WHITE)
                } else {
                    view.background = roundedBackground(
                        if (isPlaying) Color.parseColor("#065F46") else CARD_BG,
                        12f,
                        if (isPlaying) STATUS_GREEN else Color.TRANSPARENT,
                        if (isPlaying) 1 else 0
                    )
                    name.setTextColor(if (isPlaying) STATUS_GREEN else TEXT_PRIMARY)
                    number.setTextColor(if (isPlaying) STATUS_GREEN else TEXT_SECONDARY)
                }
            }

            card.setOnClickListener {
                val realIndex = channels.indexOf(channel)
                playChannel(channel, realIndex)
            }

            channelLayout.addView(card)

            if (index == 0 && isOverlayVisible) {
                card.post { card.requestFocus() }
            }
        }
    }

    private fun refreshChannelCards() {
        if (currentGroup.isNotEmpty()) {
            showChannels(currentGroup)
        }
    }

    private fun updateCurrentInfo() {
        val channel = currentChannel ?: run {
            currentChannelText.text = "اختر قناة للبدء"
            currentGroupText.text = "البث الرياضي"
            statusText.text = "جاهز للبث"
            return
        }

        currentChannelText.text = channel.name
        currentGroupText.text = packageDisplayName(channel.group)
        statusText.text = "● بث مباشر الآن"
    }

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

    private fun packageDisplayName(group: String): String {
        return when (group) {
            "ALWAN SPORT" -> "ألوان سبورت"
            "beIN SPORTS" -> "beIN SPORTS"
            "beIN SPORTS VIP" -> "beIN SPORTS VIP"
            "THAMANYA" -> "ثمانية"
            "ALKASS SPORT" -> "الكأس الرياضية"
            "AD SPORT" -> "أبوظبي الرياضية"
            "DUBAI SPORT" -> "دبي الرياضية"
            "IRAQIA SPORT" -> "العراقية الرياضية"
            else -> group
        }
    }

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

    private fun showOverlay() {
        isOverlayVisible = true
        overlay.visibility = View.VISIBLE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        updateCurrentInfo()

        if (channelLayout.childCount > 0) {
            var focused = false
            for (i in 0 until channelLayout.childCount) {
                val child = channelLayout.getChildAt(i)
                if (currentChannel != null && channels.filter { it.group == currentGroup }.getOrNull(i) == currentChannel) {
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

    private fun nextChannel() {
        val groupChannels = channels.filter { it.group == currentGroup }
        if (groupChannels.isEmpty()) return
        val currentPosition = groupChannels.indexOf(currentChannel)
        val nextPosition = if (currentPosition < groupChannels.lastIndex) currentPosition + 1 else 0
        val next = groupChannels[nextPosition]
        playChannel(next, channels.indexOf(next))
    }

    private fun previousChannel() {
        val groupChannels = channels.filter { it.group == currentGroup }
        if (groupChannels.isEmpty()) return
        val currentPosition = groupChannels.indexOf(currentChannel)
        val previousPosition = if (currentPosition > 0) currentPosition - 1 else groupChannels.lastIndex
        val previous = groupChannels[previousPosition]
        playChannel(previous, channels.indexOf(previous))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
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
                }
                return true
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

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun loadChannelsData() {
        channels.clear()

        // ALWAN SPORT
        channels.add(Channel("ALWAN SPORT 1 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859098&extension=ts"))
        channels.add(Channel("ALWAN SPORT 2 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859097&extension=ts"))
        channels.add(Channel("ALWAN SPORT 3 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859096&extension=ts"))
        channels.add(Channel("ALWAN SPORT 4 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859095&extension=ts"))
        channels.add(Channel("ALWAN SPORT 5 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859094&extension=ts"))
        channels.add(Channel("ALWAN SPORT 6 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859093&extension=ts"))

        // beIN SPORTS
        channels.add(Channel("beIN SPORT 1 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"))
        channels.add(Channel("beIN SPORT 2 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330438&extension=ts"))
        channels.add(Channel("beIN SPORT 3 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411381&extension=ts"))
        channels.add(Channel("beIN SPORT 4 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411380&extension=ts"))
        channels.add(Channel("beIN SPORT 5 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411379&extension=ts"))
        channels.add(Channel("beIN SPORT 6 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411378&extension=ts"))
        channels.add(Channel("beIN SPORT 7 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411377&extension=ts"))
        channels.add(Channel("beIN SPORT 8 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411376&extension=ts"))
        channels.add(Channel("beIN SPORT 9 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411375&extension=ts"))

        // beIN SPORTS VIP
        channels.add(Channel("beIN SPORT 1 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660413&extension=ts"))
        channels.add(Channel("beIN SPORT 2 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660411&extension=ts"))
        channels.add(Channel("beIN SPORT 3 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660409&extension=ts"))
        channels.add(Channel("beIN SPORT 4 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660407&extension=ts"))
        channels.add(Channel("beIN SPORT 5 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660405&extension=ts"))
        channels.add(Channel("beIN SPORT 6 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660403&extension=ts"))
        channels.add(Channel("beIN SPORT 7 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660401&extension=ts"))
        channels.add(Channel("beIN SPORT 8 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660399&extension=ts"))
        channels.add(Channel("beIN SPORT 9 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660397&extension=ts"))

        // THAMANYA
        channels.add(Channel("THAMANYA 1 HD", "THAMANYA", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936356&extension=ts"))
        channels.add(Channel("THAMANYA 2 HD", "THAMANYA", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936355&extension=ts"))
        channels.add(Channel("THAMANYA 3 HD", "THAMANYA", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936354&extension=ts"))

        // ALKASS SPORT
        channels.add(Channel("ALKASS SPORT 1 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591593&extension=ts"))
        channels.add(Channel("ALKASS SPORT 2 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591591&extension=ts"))
        channels.add(Channel("ALKASS SPORT 3 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787903&extension=ts"))
        channels.add(Channel("ALKASS SPORT 4 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591589&extension=ts"))
        channels.add(Channel("ALKASS SPORT 5 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591587&extension=ts"))
        channels.add(Channel("ALKASS SPORT 6 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787906&extension=ts"))

        // AD SPORT
        channels.add(Channel("AD SPORT 1 HD", "AD SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993336&extension=ts"))
        channels.add(Channel("AD SPORT 2 HD", "AD SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993337&extension=ts"))

        // DUBAI SPORT
        channels.add(Channel("DUBAI SPORT 1 HD", "DUBAI SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8086&extension=ts"))
        channels.add(Channel("DUBAI SPORT 2 HD", "DUBAI SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=84251&extension=ts"))
        channels.add(Channel("DUBAI SPORT 3 HD", "DUBAI SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591579&extension=ts"))

        // IRAQIA SPORT
        channels.add(Channel("IRAQIA SPORT HD", "IRAQIA SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"))
    }
}