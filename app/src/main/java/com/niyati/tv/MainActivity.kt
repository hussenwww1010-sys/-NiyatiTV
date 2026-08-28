package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

data class Channel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : Activity() {

    private var exoPlayer: ExoPlayer? = null
    private var isFullscreen = false
    private var currentScreen = ScreenState.CATEGORIES
    
    private var currentGroup = ""
    private var currentChannelIndex = -1

    private val visibleChannels = mutableListOf<Channel>()
    private val channelButtons = mutableListOf<View>()
    private val categoryButtons = mutableListOf<View>()

    private lateinit var root: FrameLayout
    private lateinit var categoriesView: LinearLayout
    private lateinit var channelsView: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var categoriesGrid: GridLayout
    private lateinit var channelsGrid: GridLayout
    private lateinit var categoryTitle: TextView
    private lateinit var channelTitle: TextView
    private lateinit var playerOverlayControl: LinearLayout
    private lateinit var playingChannelTitle: TextView

    enum class ScreenState { CATEGORIES, CHANNELS, PLAYER }

    // Palette الألوان الجديدة المستوحاة من الشعار والواجهة (Modern Neon Blue Theme)
    private val bgMain = Color.parseColor("#060B14")
    private val bgCard = Color.parseColor("#0F172A")
    private val bgCardFocus = Color.parseColor("#1E293B")
    private val primaryCyan = Color.parseColor("#00E5FF")
    private val primaryBlue = Color.parseColor("#0284C7")
    private val textWhite = Color.parseColor("#FFFFFF")
    private val textMuted = Color.parseColor("#94A3B8")
    private val glassOverlay = Color.parseColor("#CC0F172A")

    private val base = "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun c(name: String, group: String, id: String): Channel {
        return Channel(name = name, group = group, url = "$base$id.ts")
    }

    private val channels = mutableListOf<Channel>().apply {
        // BEIN TOD
        add(c("beIN Tod 4K", "BEIN TOD", "460835"))
        for (i in 1..9) { add(c("beIN Sport Tod $i", "BEIN TOD", "${460835 + i}")) }
        add(c("beIN Sport Tod English 1", "BEIN TOD", "460845"))
        add(c("beIN Sport Tod English 2", "BEIN TOD", "460846"))
        for (i in 1..9) { add(c("beIN Sport Tod Extra $i", "BEIN TOD", "${460846 + i}")) }

        // BEIN SPORTS
        add(c("beIN Sport Global 4K", "BEIN SPORTS", "22186"))
        add(c("beIN Sport News 4K", "BEIN SPORTS", "318230"))
        val bein4k = listOf(318197, 318198, 318199, 440580, 318201, 318202, 318203, 318204, 318205)
        for (i in 1..9) {
            bein4k.getOrNull(i - 1)?.let { id -> add(c("beIN Sport $i 4K", "BEIN SPORTS", id.toString())) }
            add(c("beIN$i H265", "BEIN SPORTS", "${391093 + i}"))
        }
        add(c("beIN Sport English 1 4K", "BEIN SPORTS", "319495"))
        add(c("beIN Sport English 2 4K", "BEIN SPORTS", "319496"))
        add(c("beIN Sport French 1 4K", "BEIN SPORTS", "319497"))
        add(c("beIN Sport French 2 4K", "BEIN SPORTS", "319498"))
        add(c("beIN Sport NBA 4K", "BEIN SPORTS", "319499"))
        add(c("beIN Global HD", "BEIN SPORTS", "442220"))
        add(c("beIN Sport News HD", "BEIN SPORTS", "443146"))
        for (i in 1..9) { add(c("beIN Sport $i HD", "BEIN SPORTS", "${325792 + i}")) }

        // AL RABIAA
        listOf(
            "AL RABIAA SPORT 1" to "371931", "AL RABIAA SPORT 1+" to "371933",
            "AL RABIAA SPORT 2" to "371932", "Rabiaa Sport +2" to "434565",
            "AL RABIAA TV 4K" to "371939", "AL RABIAA MOVIES" to "371934"
        ).forEach { add(c(it.first, "AL RABIAA", it.second)) }

        // ALKASS
        val alkassIds = listOf(96214, 96215, 278068, 96216, 96217, 211523, 379828, 379829, 393991, 393992)
        for (i in 1..10) { alkassIds.getOrNull(i - 1)?.let { add(c("Alkass $i HD", "ALKASS", it.toString())) } }

        // SAUDI SPORTS
        listOf(
            "KSA Sport 1 4K" to "97805", "KSA Sport 2 4K" to "97806",
            "STC SPORT 1 HD" to "421391", "STC SPORT 2 HD" to "421392"
        ).forEach { add(c(it.first, "SAUDI SPORTS", it.second)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bgMain
        window.navigationBarColor = bgMain
        
        buildUI()
        showCategoriesView()
    }

    private fun buildUI() {
        root = FrameLayout(this).apply {
            setBackgroundColor(bgMain)
        }

        // 1. شاشة الباقات الرئيسية
        categoriesView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(24), dp(28), dp(24))
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(20))
        }

        val appLogoText = TextView(this).apply {
            text = "NIYATI • TV"
            textSize = 26f
            setTextColor(primaryCyan)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        headerLayout.addView(appLogoText)

        categoryTitle = TextView(this).apply {
            text = "  |  قائمة الباقات المتاحة"
            textSize = 18f
            setTextColor(textMuted)
        }
        headerLayout.addView(categoryTitle)
        categoriesView.addView(headerLayout)

        val catScrollView = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        categoriesGrid = GridLayout(this).apply {
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        catScrollView.addView(categoriesGrid, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        categoriesView.addView(catScrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 2. شاشة القنوات
        channelsView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(24), dp(28), dp(24))
            visibility = View.GONE
        }

        channelTitle = TextView(this).apply {
            text = "القنوات"
            textSize = 22f
            setTextColor(primaryCyan)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, 0, 0, dp(20))
        }
        channelsView.addView(channelTitle)

        val chScrollView = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        channelsGrid = GridLayout(this).apply {
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        chScrollView.addView(channelsGrid, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        channelsView.addView(chScrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 3. مشغل الفيديو وعناصر التحكم
        playerContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        
        playerView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        playerContainer.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // overlay controls
        playerOverlayControl = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = createShapeDrawable(glassOverlay, dp(16), primaryCyan)
        }

        playingChannelTitle = TextView(this).apply {
            text = "جاري التشغيل..."
            textSize = 16f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        val fullscreenBtn = TextView(this).apply {
            text = " ⛶ ملء الشاشة "
            textSize = 14f
            setTextColor(primaryCyan)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = createShapeDrawable(bgCard, dp(8), primaryCyan)
            isFocusable = true
            setOnClickListener { toggleFullscreenVideo() }
        }

        playerOverlayControl.addView(playingChannelTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        playerOverlayControl.addView(fullscreenBtn)

        val overlayParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM
            setMargins(dp(32), 0, dp(32), dp(32))
        }
        playerContainer.addView(playerOverlayControl, overlayParams)

        root.addView(categoriesView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(channelsView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(playerContainer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        setContentView(root)
        populateCategories()
    }

    private fun populateCategories() {
        categoriesGrid.removeAllViews()
        categoryButtons.clear()

        val groups = channels.map { it.group }.distinct()
        val screenWidth = resources.displayMetrics.widthPixels - dp(56)
        val itemWidth = (screenWidth - dp(32)) / 3

        groups.forEachIndexed { index, groupName ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(20), dp(16), dp(20))
                background = createShapeDrawable(bgCard, dp(14))
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val icon = TextView(this).apply {
                text = "📺"
                textSize = 22f
                gravity = Gravity.CENTER
            }

            val name = TextView(this).apply {
                text = packageDisplayName(groupName)
                textSize = 15f
                setTextColor(textWhite)
                setTypeface(Typeface.DEFAULT_BOLD)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }

            card.addView(icon)
            card.addView(name)

            card.setOnFocusChangeListener { _, hasFocus ->
                card.background = if (hasFocus) createShapeDrawable(bgCardFocus, dp(14), primaryCyan) else createShapeDrawable(bgCard, dp(14))
                card.scaleX = if (hasFocus) 1.04f else 1.0f
                card.scaleY = if (hasFocus) 1.04f else 1.0f
            }

            card.setOnClickListener {
                currentGroup = groupName
                showChannelsView(groupName)
            }

            val params = GridLayout.LayoutParams().apply {
                width = itemWidth
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }

            categoriesGrid.addView(card, params)
            categoryButtons.add(card)

            if (index == 0) card.post { card.requestFocus() }
        }
    }

    private fun showCategoriesView() {
        currentScreen = ScreenState.CATEGORIES
        categoriesView.visibility = View.VISIBLE
        channelsView.visibility = View.GONE
        playerContainer.visibility = View.GONE
        exitFullscreenSystemUI()

        if (categoryButtons.isNotEmpty()) {
            categoryButtons[0].post { categoryButtons[0].requestFocus() }
        }
    }

    private fun showChannelsView(groupName: String) {
        currentScreen = ScreenState.CHANNELS
        categoriesView.visibility = View.GONE
        channelsView.visibility = View.VISIBLE
        playerContainer.visibility = View.GONE
        channelTitle.text = "باقة: ${packageDisplayName(groupName)}"

        channelsGrid.removeAllViews()
        channelButtons.clear()
        visibleChannels.clear()

        val filtered = channels.filter { it.group == groupName }
        visibleChannels.addAll(filtered)

        val screenWidth = resources.displayMetrics.widthPixels - dp(56)
        val itemWidth = (screenWidth - dp(32)) / 3

        filtered.forEachIndexed { index, channel ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = createShapeDrawable(bgCard, dp(12))
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val icon = TextView(this).apply {
                text = "▶"
                textSize = 12f
                setTextColor(primaryCyan)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = createShapeDrawable(Color.parseColor("#1E293B"), dp(6))
            }

            val name = TextView(this).apply {
                text = channel.name
                textSize = 14f
                setTextColor(textWhite)
                setTypeface(Typeface.DEFAULT_BOLD)
                maxLines = 1
                setPadding(dp(10), 0, 0, 0)
            }

            card.addView(icon)
            card.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            card.setOnFocusChangeListener { _, hasFocus ->
                card.background = if (hasFocus) createShapeDrawable(bgCardFocus, dp(12), primaryCyan) else createShapeDrawable(bgCard, dp(12))
                card.scaleX = if (hasFocus) 1.03f else 1.0f
                card.scaleY = if (hasFocus) 1.03f else 1.0f
            }

            card.setOnClickListener {
                currentChannelIndex = index
                playChannelAndFullscreen(channel)
            }

            val params = GridLayout.LayoutParams().apply {
                width = itemWidth
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }

            channelsGrid.addView(card, params)
            channelButtons.add(card)

            if (index == 0) card.post { card.requestFocus() }
        }
    }

    private fun playChannelAndFullscreen(channel: Channel) {
        currentScreen = ScreenState.PLAYER
        categoriesView.visibility = View.GONE
        channelsView.visibility = View.GONE
        playerContainer.visibility = View.VISIBLE
        playingChannelTitle.text = "البث المباشر: ${channel.name}"

        enterFullscreenSystemUI()

        try {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(this).build()
                playerView.player = exoPlayer
                exoPlayer?.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(this@MainActivity, "خطأ في تشغيل القناة", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

        } catch (e: Exception) {
            Toast.makeText(this, "حدث خطأ أثناء تشغيل البث", Toast.LENGTH_SHORT).show()
        }

        playerView.post { playerView.requestFocus() }
    }

    private fun toggleFullscreenVideo() {
        if (playerView.resizeMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL) {
            playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            Toast.makeText(this, "العرض الأصلي", Toast.LENGTH_SHORT).show()
        } else {
            playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            Toast.makeText(this, "تنسيق ملء الشاشة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveChannelInPlayer(direction: Int) {
        if (visibleChannels.isEmpty()) return

        currentChannelIndex += direction
        if (currentChannelIndex < 0) currentChannelIndex = visibleChannels.lastIndex
        if (currentChannelIndex > visibleChannels.lastIndex) currentChannelIndex = 0

        val nextChannel = visibleChannels[currentChannelIndex]
        Toast.makeText(this, "القناة: ${nextChannel.name}", Toast.LENGTH_SHORT).show()
        playChannelAndFullscreen(nextChannel)
    }

    private fun enterFullscreenSystemUI() {
        isFullscreen = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun exitFullscreenSystemUI() {
        isFullscreen = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    when (currentScreen) {
                        ScreenState.PLAYER -> {
                            exoPlayer?.stop()
                            showChannelsView(currentGroup)
                            return true
                        }
                        ScreenState.CHANNELS -> {
                            showCategoriesView()
                            return true
                        }
                        ScreenState.CATEGORIES -> {
                            finish()
                            return true
                        }
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (currentScreen == ScreenState.PLAYER) {
                        moveChannelInPlayer(-1)
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (currentScreen == ScreenState.PLAYER) {
                        moveChannelInPlayer(1)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun createShapeDrawable(bgColor: Int, radiusDp: Int, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dp(radiusDp).toFloat()
            strokeColor?.let { setStroke(dp(2), it) }
        }
    }

    private fun packageDisplayName(name: String): String {
        return when (name) {
            "BEIN TOD" -> "beIN TOD"
            "BEIN SPORTS" -> "beIN SPORTS"
            "BEIN XTRA" -> "beIN XTRA"
            "AL RABIAA" -> "الرابعة الرياضية"
            "ALKASS" -> "الكأس"
            "SAUDI SPORTS" -> "السعودية الرياضية"
            "GULF SPORTS" -> "قنوات الخليج"
            "AD SPORTS" -> "أبوظبي الرياضية"
            "ALWAN SPORT" -> "ألوان سبورت"
            "CRICKET" -> "الكريكت"
            "STAR SPORTS" -> "Star Sports"
            "FAJER TV" -> "الفجر TV"
            "KURDISTAN SPORTS" -> "كوردستان سبورت"
            "SHAHID SPORT" -> "شاهد رياضة"
            "SHASHA" -> "شاشة"
            else -> name
        }
    }

    override fun onStop() {
        super.onStop()
        playerView.player = null
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
