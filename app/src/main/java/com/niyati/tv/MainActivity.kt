package com.niyati.tv

import android.app.Activity
import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    val url: String,
    val viewers: String = "12.3K",
    val quality: String = "4K HDR"
)

class MainActivity : Activity() {

    private var exoPlayer: ExoPlayer? = null
    private var fullscreen = false
    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentSelectedChannel: Channel? = null

    private val visibleChannels = mutableListOf<Channel>()
    private val channelButtons = mutableListOf<View>()
    private val packageButtons = mutableListOf<View>()

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var playerColumn: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var epgContainer: LinearLayout
    private lateinit var epgTitle: TextView
    private lateinit var epgSub: TextView
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsLayout: LinearLayout

    // Luxury Dark & Gold Color Palette
    private val bgPrimary = Color.parseColor("#08090C")
    private val bgSecondary = Color.parseColor("#101216")
    private val bgCard = Color.parseColor("#181B22")
    private val bgCardSelected = Color.parseColor("#2A2215")
    
    // Metallic Gold Accents
    private val goldAccent = Color.parseColor("#D4AF37")
    private val goldGlow = Color.parseColor("#F3E5AB")
    private val goldDark = Color.parseColor("#AA7C11")
    private val strokeGold = Color.parseColor("#4A3B18")
    private val strokeColor = Color.parseColor("#222630")

    private val textWhite = Color.parseColor("#F5F5F7")
    private val textMuted = Color.parseColor("#8E95A2")
    private val statusGreen = Color.parseColor("#00E676")
    private val telegramBlue = Color.parseColor("#24A1DE")

    private val telegramUrl = "https://t.me/NAITI_Tv"
    private val base = "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun c(name: String, group: String, id: String, viewers: String = "15.4M", quality: String = "4K HDR"): Channel {
        return Channel(name = name, group = group, url = "$base$id.ts", viewers = viewers, quality = quality)
    }

    private fun customChannel(name: String, group: String, url: String): Channel {
        return Channel(name = name, group = group, url = url, viewers = "8.9K", quality = "FHD")
    }

    private val channels = mutableListOf<Channel>().apply {
        // 1. BEIN SPORTS
        add(c("beIN Sport Global 4K", "BEIN SPORTS", "22186", "25.4M", "4K HDR"))
        add(c("beIN Sport News 4K", "BEIN SPORTS", "318230", "12.1M", "4K"))
        val bein4k = listOf(318197, 318198, 318199, 440580, 318201, 318202, 318203, 318204, 318205)
        for (i in 1..9) {
            bein4k.getOrNull(i - 1)?.let { id ->
                add(c("beIN Sport $i 4K", "BEIN SPORTS", id.toString(), "${10 + i}.2M", "4K HDR"))
            }
        }

        // 2. BEIN TOD
        add(c("beIN Tod 4K", "BEIN TOD", "460835", "18.5M", "4K HDR"))
        for (i in 1..9) add(c("beIN Sport Tod $i", "BEIN TOD", "${460835 + i}", "${8 + i}.3M", "1080p"))

        // 3. ALWAN SPORT
        for (i in 1..6) {
            add(c("Alwan Sport $i 4K", "ALWAN SPORT", "${418111 + i}", "9.1M", "4K"))
        }

        // 4. SAUDI SPORTS
        listOf(
            "KSA Sport 1 4K" to "97805", "KSA Sport 2 4K" to "97806",
            "STC SPORT 1 HD" to "421391", "STC SPORT 2 HD" to "421392"
        ).forEach { add(c(it.first, "SAUDI SPORTS", it.second, "14.2M", "4K HDR")) }

        // 5. DRAMA & MBC
        listOf(
            "MBC 1" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120314&extension=ts",
            "MBC ACTION" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120308&extension=ts"
        ).forEach { add(customChannel(it.first, "DRAMA & MBC", it.second)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bgPrimary
        window.navigationBarColor = bgPrimary

        buildInterface()
        showWelcomeDialog()
    }

    private fun showWelcomeDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(28), dp(28), dp(28))
            background = GradientDrawable().apply {
                setColor(bgCard)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(2), goldAccent)
            }
        }

        val titleText = TextView(this).apply {
            text = "👑 NIYATI TV LUXURY"
            textSize = 22f
            setTextColor(goldAccent)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
        }
        dialogView.addView(titleText)

        val subTitleText = TextView(this).apply {
            text = "مرحباً بك في النسخة الفاخرة الجديدة. بوابتك لمشاهدة المباريات والقنوات الرياضية بأعلى جودة 4K HDR وبث مباشر بدون تقطيع."
            textSize = 13f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.3f)
        }
        val subParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(12), 0, dp(24))
        }
        dialogView.addView(subTitleText, subParams)

        val startBtn = TextView(this).apply {
            text = "دخول التطبيق الفاخر"
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = GradientDrawable().apply {
                setColor(goldAccent)
                cornerRadius = dp(14).toFloat()
            }
        }
        dialogView.addView(startBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)))

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        startBtn.setOnClickListener { dialog.dismiss() }
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgPrimary)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        createTopBar()

        mainContent = LinearLayout(this).apply {
            orientation = if (isTvDevice()) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setBackgroundColor(bgPrimary)
        }

        if (isTvDevice()) {
            createPackageColumn()
            createChannelColumn()
            createPlayerColumn()
        } else {
            createPlayerColumn()
            createPackageColumn()
            createChannelColumn()
        }

        root.addView(mainContent, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        loadPackages()
    }

    private fun createTopBar() {
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(25), dp(10), dp(25), dp(10))
            setBackgroundColor(bgSecondary)
        }

        // Luxury Logo Box
        val logoBox = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(2), goldAccent)
            }
        }

        val logoText = TextView(this).apply {
            text = "N"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(goldAccent)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        logoBox.addView(logoText, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        topBar.addView(logoBox, LinearLayout.LayoutParams(dp(38), dp(38)))

        val brandText = TextView(this).apply {
            text = "  NIYATI TV"
            textSize = 18f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        topBar.addView(brandText)

        val spacer = View(this)
        topBar.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))

        // Live Badge
        val liveBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A00E676"))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), statusGreen)
            }
        }

        val liveDot = TextView(this).apply {
            text = "● "
            textSize = 10f
            setTextColor(statusGreen)
        }

        val liveText = TextView(this).apply {
            text = "مباشر"
            textSize = 11f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        liveBadge.addView(liveDot)
        liveBadge.addView(liveText)
        topBar.addView(liveBadge)

        root.addView(topBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)))
    }

    private fun createPackageColumn() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgSecondary)
        }

        col.addView(createColumnHeader("الباقات"))

        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        packagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        scroll.addView(packagesLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val width = if (isTvDevice()) dp(220) else LinearLayout.LayoutParams.MATCH_PARENT
        val height = if (isTvDevice()) LinearLayout.LayoutParams.MATCH_PARENT else dp(130)
        mainContent.addView(col, LinearLayout.LayoutParams(width, height))
    }

    private fun createChannelColumn() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgSecondary)
        }

        col.addView(createColumnHeader("القنوات المتاحة"))

        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        channelsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        scroll.addView(channelsLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val width = if (isTvDevice()) dp(290) else LinearLayout.LayoutParams.MATCH_PARENT
        val height = if (isTvDevice()) LinearLayout.LayoutParams.MATCH_PARENT else 0
        val weight = if (isTvDevice()) 0f else 1f

        mainContent.addView(col, LinearLayout.LayoutParams(width, height, weight))
    }

    private fun createPlayerColumn() {
        playerColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
            setBackgroundColor(bgPrimary)
        }

        // Player Video Frame
        playerContainer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(2), strokeGold)
            }
            clipToOutline = true
        }

        playerView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(Color.BLACK)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            isFocusable = true
            isClickable = true

            setOnFocusChangeListener { _, hasFocus ->
                if (!fullscreen) {
                    playerContainer.background = GradientDrawable().apply {
                        setColor(Color.BLACK)
                        cornerRadius = dp(16).toFloat()
                        setStroke(dp(2), if (hasFocus) goldAccent else strokeGold)
                    }
                }
            }

            setOnClickListener { toggleFullscreen() }
        }

        playerContainer.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val watermark = TextView(this).apply {
            text = "4K HDR"
            textSize = 9f
            setTextColor(goldAccent)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#99000000"))
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), goldAccent)
            }
        }

        val wmParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, dp(12), dp(12), 0)
        }
        playerContainer.addView(watermark, wmParams)

        val playerHeight = if (isTvDevice()) 0 else dp(220)
        val playerWeight = if (isTvDevice()) 0.65f else 0f

        playerColumn.addView(playerContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, playerHeight, playerWeight))

        // Luxury EPG & Smart Controls Bar
        epgContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(bgSecondary)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), strokeGold)
            }
        }

        epgTitle = TextView(this).apply {
            text = "اختر قناة لبدء العرض"
            textSize = 16f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        epgContainer.addView(epgTitle)

        epgSub = TextView(this).apply {
            text = "يعرض الآن: بث مباشر | Audio: Arabic / English"
            textSize = 11f
            setTextColor(textMuted)
            setPadding(0, dp(2), 0, dp(8))
        }
        epgContainer.addView(epgSub)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progress = 70
            progressDrawable.setTint(goldAccent)
        }
        epgContainer.addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)))

        // Interactive Options Bar (Smart Controls)
        val controlsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }

        listOf("📊 Match Stats", "🔄 Switch Angle", "🎙 Audio Track").forEach { option ->
            val optBtn = TextView(this).apply {
                text = option
                textSize = 10f
                setTextColor(textWhite)
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = GradientDrawable().apply {
                    setColor(bgCard)
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), strokeColor)
                }
            }
            controlsBar.addView(optBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = dp(8)
            })
        }

        epgContainer.addView(controlsBar)

        playerColumn.addView(epgContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        val mainPlayerWidth = if (isTvDevice()) 0 else LinearLayout.LayoutParams.MATCH_PARENT
        val mainPlayerHeight = if (isTvDevice()) LinearLayout.LayoutParams.MATCH_PARENT else LinearLayout.LayoutParams.WRAP_CONTENT
        val mainPlayerWeight = if (isTvDevice()) 1f else 0f

        mainContent.addView(playerColumn, LinearLayout.LayoutParams(mainPlayerWidth, mainPlayerHeight, mainPlayerWeight))
    }

    private fun createColumnHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(goldAccent)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
    }

    private fun loadPackages() {
        packagesLayout.removeAllViews()
        packageButtons.clear()

        val groups = channels.map { it.group }.distinct()
        if (groups.isEmpty()) return

        currentGroup = groups.first()

        groups.forEach { group ->
            val card = createPackageCard(group, group == currentGroup)

            card.setOnClickListener {
                currentGroup = group
                updatePackageSelection()
                loadChannels(group)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                view.background = createCardDrawable(hasFocus, currentGroup == group)
            }

            packagesLayout.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                bottomMargin = dp(8)
            })

            packageButtons.add(card)
        }

        loadChannels(groups.first())
    }

    private fun createPackageCard(name: String, isSelected: Boolean): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = createCardDrawable(false, isSelected)
            isFocusable = true
        }

        val nameTv = TextView(this).apply {
            text = name
            textSize = 12f
            setTextColor(if (isSelected) goldGlow else textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        layout.addView(nameTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return layout
    }

    private fun updatePackageSelection() {
        val groups = channels.map { it.group }.distinct()
        for (i in 0 until packagesLayout.childCount) {
            val child = packagesLayout.getChildAt(i) as? LinearLayout ?: continue
            val isSelected = groups.getOrNull(i) == currentGroup
            child.background = createCardDrawable(child.hasFocus(), isSelected)
        }
    }

    private fun loadChannels(group: String) {
        channelsLayout.removeAllViews()
        channelButtons.clear()
        visibleChannels.clear()

        val filtered = channels.filter { it.group == group }
        visibleChannels.addAll(filtered)

        filtered.forEachIndexed { index, channel ->
            val card = createChannelCard(channel, index == currentChannelIndex)

            card.setOnClickListener {
                currentChannelIndex = index
                updateChannelSelection()
                playChannel(channel)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                view.background = createCardDrawable(hasFocus, currentChannelIndex == index)
            }

            channelsLayout.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply {
                bottomMargin = dp(8)
            })

            channelButtons.add(card)
        }
    }

    private fun createChannelCard(channel: Channel, isSelected: Boolean): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = createCardDrawable(false, isSelected)
            isFocusable = true
        }

        val qualityBadge = TextView(this).apply {
            text = channel.quality
            textSize = 8f
            setTextColor(Color.BLACK)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(6), dp(3), dp(6), dp(3))
            background = GradientDrawable().apply {
                setColor(goldAccent)
                cornerRadius = dp(6).toFloat()
            }
        }
        card.addView(qualityBadge)

        val name = TextView(this).apply {
            text = "  ${channel.name}"
            textSize = 12f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
            maxLines = 1
        }
        card.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val viewers = TextView(this).apply {
            text = "👁 ${channel.viewers}"
            textSize = 9f
            setTextColor(textMuted)
        }
        card.addView(viewers)

        return card
    }

    private fun updateChannelSelection() {
        for (i in 0 until channelsLayout.childCount) {
            val child = channelsLayout.getChildAt(i) as? LinearLayout ?: continue
            val isSelected = (i == currentChannelIndex)
            child.background = createCardDrawable(child.hasFocus(), isSelected)
        }
    }

    private fun createCardDrawable(hasFocus: Boolean, isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            when {
                hasFocus -> {
                    setColor(bgCardSelected)
                    setStroke(dp(2), goldAccent)
                }
                isSelected -> {
                    setColor(bgCardSelected)
                    setStroke(dp(1), goldDark)
                }
                else -> {
                    setColor(bgCard)
                    setStroke(dp(1), strokeColor)
                }
            }
        }
    }

    private fun initExoPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(this@MainActivity, "تعذر تشغيل البث المباشر", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            playerView.player = exoPlayer
        }
    }

    private fun playChannel(channel: Channel) {
        currentSelectedChannel = channel
        epgTitle.text = channel.name
        epgSub.text = "يعرض الآن: بث مباشر بدقة ${channel.quality}"

        try {
            initExoPlayer()
            val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في تشغيل القناة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFullscreen() {
        if (fullscreen) exitFullscreen() else enterFullscreen()
    }

    private fun enterFullscreen() {
        if (fullscreen) return
        fullscreen = true

        topBar.visibility = View.GONE
        if (isTvDevice()) {
            mainContent.getChildAt(0).visibility = View.GONE
            mainContent.getChildAt(1).visibility = View.GONE
        } else {
            mainContent.getChildAt(1).visibility = View.GONE
            mainContent.getChildAt(2).visibility = View.GONE
        }

        epgContainer.visibility = View.GONE
        playerColumn.setPadding(0, 0, 0, 0)

        val params = playerContainer.layoutParams as LinearLayout.LayoutParams
        params.height = LinearLayout.LayoutParams.MATCH_PARENT
        params.weight = 1f
        playerContainer.layoutParams = params

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
    }

    private fun exitFullscreen() {
        if (!fullscreen) return
        fullscreen = false

        topBar.visibility = View.VISIBLE
        if (isTvDevice()) {
            mainContent.getChildAt(0).visibility = View.VISIBLE
            mainContent.getChildAt(1).visibility = View.VISIBLE
        } else {
            mainContent.getChildAt(1).visibility = View.VISIBLE
            mainContent.getChildAt(2).visibility = View.VISIBLE
        }

        epgContainer.visibility = View.VISIBLE
        playerColumn.setPadding(dp(15), dp(10), dp(15), dp(10))

        val params = playerContainer.layoutParams as LinearLayout.LayoutParams
        params.height = if (isTvDevice()) 0 else dp(220)
        params.weight = if (isTvDevice()) 0.65f else 0f
        playerContainer.layoutParams = params
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && fullscreen) {
                exitFullscreen()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
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
