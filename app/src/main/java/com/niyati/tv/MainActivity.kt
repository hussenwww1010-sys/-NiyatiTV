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

    private lateinit var playerView: PlayerView
    private lateinit var packageLayout: LinearLayout
    private lateinit var channelLayout: LinearLayout
    private lateinit var centerLayout: LinearLayout
    private lateinit var headerTextView: TextView

    private var player: ExoPlayer? = null
    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentChannel: Channel? = null

    private var isFullscreen = false
    private var isPlayingChannel = false

    private val handler = Handler(Looper.getMainLooper())
    private val channels = mutableListOf<Channel>()

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

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    // دوال إنشاء الخلفيات ذات الحواف الدائرية
    private fun createCardBackground(bgColor: Int, strokeColor: Int = Color.TRANSPARENT, radiusDp: Float = 10f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = dpToPx(radiusDp).toFloat()
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dpToPx(2f), strokeColor)
            }
        }
    }

    private fun createPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView = PlayerView(this).apply {
            player = this@MainActivity.player
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isPlayingChannel = true
                } else if (state == Player.STATE_ENDED) {
                    reconnectChannel()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@MainActivity,
                    "تعذر تشغيل القناة، جاري إعادة الاتصال...",
                    Toast.LENGTH_SHORT
                ).show()
                reconnectChannel()
            }
        })
    }

    private fun playChannel(channel: Channel, index: Int) {
        currentChannel = channel
        currentChannelIndex = index
        currentGroup = channel.group

        val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
        player?.apply {
            stop()
            clearMediaItems()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
        isPlayingChannel = true
        showChannels(currentGroup) // إعادة تحديث القائمة لإبراز القناة الشغالة
        enterFullscreen()
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

    private fun createInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0B0E14")) // خلفية داكنة فخمة
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // --- 1. قائمة الباقات الجانبية ---
        val packageScrollView = ScrollView(this).apply {
            isFillViewport = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        }
        packageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(dpToPx(10f), dpToPx(16f), dpToPx(10f), dpToPx(16f))
            setBackgroundColor(Color.parseColor("#121620"))
        }
        packageScrollView.addView(
            packageLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            packageScrollView,
            LinearLayout.LayoutParams(dpToPx(220f), LinearLayout.LayoutParams.MATCH_PARENT)
        )

        // --- 2. المنطقة الوسطى (المشغل + القنوات) ---
        centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12f), dpToPx(12f), dpToPx(12f), dpToPx(12f))
        }

        // الشريط العلوي (Header)
        headerTextView = TextView(this).apply {
            text = "📺 الباقات الرياضية"
            textSize = 17f
            setTextColor(Color.parseColor("#00E5FF"))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dpToPx(8f), 0, dpToPx(8f), dpToPx(10f))
        }
        centerLayout.addView(headerTextView)

        // حاوية مشغل الفيديو
        val playerContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = createCardBackground(Color.BLACK, Color.parseColor("#1E2638"), 12f)
            setPadding(dpToPx(2f), dpToPx(2f), dpToPx(2f), dpToPx(2f))
            addView(
                playerView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        centerLayout.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(250f)
            ).apply { bottomMargin = dpToPx(12f) }
        )

        // قائمة القنوات
        val channelScrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        channelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(4f), 0, dpToPx(4f))
        }
        channelScrollView.addView(
            channelLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        centerLayout.addView(
            channelScrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            centerLayout,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        )

        setContentView(root)
        buildPackages()
    }

    private fun buildPackages() {
        packageLayout.removeAllViews()
        val groups = channels.map { it.group }.distinct()

        groups.forEachIndexed { index, group ->
            val title = packageDisplayName(group)
            val text = TextView(this).apply {
                this.text = title
                textSize = 14f
                setTextColor(Color.parseColor("#C5CEE0"))
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(dpToPx(14f), 0, dpToPx(14f), 0)
                isFocusable = true
                isFocusableInTouchMode = true
                background = createCardBackground(Color.TRANSPARENT, radiusDp = 8f)
                
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(46f)
                ).apply { bottomMargin = dpToPx(6f) }

                setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        view.background = createCardBackground(Color.parseColor("#1D60C2"), Color.parseColor("#00E5FF"), 8f)
                        setTextColor(Color.WHITE)
                    } else {
                        val isSelectedGroup = group == currentGroup
                        view.background = createCardBackground(
                            if (isSelectedGroup) Color.parseColor("#1A233A") else Color.TRANSPARENT,
                            radiusDp = 8f
                        )
                        setTextColor(if (isSelectedGroup) Color.parseColor("#00E5FF") else Color.parseColor("#C5CEE0"))
                    }
                }
                setOnClickListener { showChannels(group) }
            }
            packageLayout.addView(text)
            if (index == 0) text.requestFocus()
        }
    }

    private fun showChannels(group: String) {
        currentGroup = group
        headerTextView.text = "📺 ${packageDisplayName(group)}"
        channelLayout.removeAllViews()

        val groupChannels = channels.filter { it.group == group }
        groupChannels.forEachIndexed { index, channel ->
            val isCurrentPlaying = channel == currentChannel

            val text = TextView(this).apply {
                this.text = if (isCurrentPlaying) "▶  ${channel.name}" else channel.name
                textSize = 14f
                setTextColor(if (isCurrentPlaying) Color.parseColor("#00FF66") else Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, if (isCurrentPlaying) Typeface.BOLD_ITALIC else Typeface.BOLD)
                setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
                isFocusable = true
                isFocusableInTouchMode = true

                val normalBg = if (isCurrentPlaying) Color.parseColor("#152D24") else Color.parseColor("#161B26")
                val strokeBg = if (isCurrentPlaying) Color.parseColor("#00FF66") else Color.TRANSPARENT
                background = createCardBackground(normalBg, strokeBg, 8f)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(44f)
                ).apply { bottomMargin = dpToPx(6f) }

                setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        view.background = createCardBackground(Color.parseColor("#2563EB"), Color.WHITE, 8f)
                        setTextColor(Color.WHITE)
                    } else {
                        view.background = createCardBackground(normalBg, strokeBg, 8f)
                        setTextColor(if (isCurrentPlaying) Color.parseColor("#00FF66") else Color.WHITE)
                    }
                }
                setOnClickListener {
                    val realIndex = channels.indexOf(channel)
                    playChannel(channel, realIndex)
                }
            }
            channelLayout.addView(text)
            if (index == 0 && !isFullscreen) text.requestFocus()
        }
    }

    private fun packageDisplayName(group: String): String {
        return when (group) {
            "ALWAN SPORT" -> "🎨 ألوان سبورت"
            "beIN SPORTS" -> "⚽ beIN SPORTS"
            "beIN SPORTS VIP" -> "👑 beIN SPORTS VIP"
            "THAMANYA" -> "8️⃣ ثمانية"
            "ALKASS SPORT" -> "🏆 الكأس"
            "AD SPORT" -> "🇦🇪 أبوظبي الرياضية"
            "DUBAI SPORT" -> "🏙️ دبي الرياضية"
            "IRAQIA SPORT" -> "🇮🇶 العراقية الرياضية"
            else -> group
        }
    }

    private fun enterFullscreen() {
        if (isFullscreen) return
        isFullscreen = true

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        packageLayout.parent?.let { (it as View).visibility = View.GONE }
        channelLayout.parent?.let { (it as View).visibility = View.GONE }
        headerTextView.visibility = View.GONE
        playerView.requestFocus()
    }

    private fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        packageLayout.parent?.let { (it as View).visibility = View.VISIBLE }
        channelLayout.parent?.let { (it as View).visibility = View.VISIBLE }
        headerTextView.visibility = View.VISIBLE
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
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (isFullscreen) {
                    playerView.showController()
                    return true
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isFullscreen) {
                    exitFullscreen()
                    return true
                }
                if (isPlayingChannel) {
                    player?.stop()
                    isPlayingChannel = false
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isFullscreen) {
                    nextChannel()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isFullscreen) {
                    previousChannel()
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

        // =========================
        // ALWAN SPORT
        // =========================
        channels.add(Channel("ALWAN SPORT 1 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859098&extension=ts"))
        channels.add(Channel("ALWAN SPORT 2 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859097&extension=ts"))
        channels.add(Channel("ALWAN SPORT 3 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859096&extension=ts"))
        channels.add(Channel("ALWAN SPORT 4 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859095&extension=ts"))
        channels.add(Channel("ALWAN SPORT 5 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859094&extension=ts"))
        channels.add(Channel("ALWAN SPORT 6 HD", "ALWAN SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859093&extension=ts"))

        // =========================
        // beIN SPORTS
        // =========================
        channels.add(Channel("beIN SPORT 1 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"))
        channels.add(Channel("beIN SPORT 2 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330438&extension=ts"))
        channels.add(Channel("beIN SPORT 3 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411381&extension=ts"))
        channels.add(Channel("beIN SPORT 4 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411380&extension=ts"))
        channels.add(Channel("beIN SPORT 5 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411379&extension=ts"))
        channels.add(Channel("beIN SPORT 6 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411378&extension=ts"))
        channels.add(Channel("beIN SPORT 7 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411377&extension=ts"))
        channels.add(Channel("beIN SPORT 8 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411376&extension=ts"))
        channels.add(Channel("beIN SPORT 9 HD", "beIN SPORTS", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411375&extension=ts"))

        // =========================
        // beIN SPORTS VIP
        // =========================
        channels.add(Channel("beIN SPORT 1 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660413&extension=ts"))
        channels.add(Channel("beIN SPORT 2 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660411&extension=ts"))
        channels.add(Channel("beIN SPORT 3 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660409&extension=ts"))
        channels.add(Channel("beIN SPORT 4 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660407&extension=ts"))
        channels.add(Channel("beIN SPORT 5 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660405&extension=ts"))
        channels.add(Channel("beIN SPORT 6 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660403&extension=ts"))
        channels.add(Channel("beIN SPORT 7 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660401&extension=ts"))
        channels.add(Channel("beIN SPORT 8 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660399&extension=ts"))
        channels.add(Channel("beIN SPORT 9 HD", "beIN SPORTS VIP", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660397&extension=ts"))

        // =========================
        // THAMANYA
        // =========================
        channels.add(Channel("THAMANYA 1 HD", "THAMANYA", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936356&extension=ts"))
        channels.add(Channel("THAMANYA 2 HD", "THAMANYA", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936355&extension=ts"))
        channels.add(Channel("THAMANYA 3 HD", "THAMANYA", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936354&extension=ts"))

        // =========================
        // ALKASS SPORT
        // =========================
        channels.add(Channel("ALKASS SPORT 1 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591593&extension=ts"))
        channels.add(Channel("ALKASS SPORT 2 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591591&extension=ts"))
        channels.add(Channel("ALKASS SPORT 3 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787903&extension=ts"))
        channels.add(Channel("ALKASS SPORT 4 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591589&extension=ts"))
        channels.add(Channel("ALKASS SPORT 5 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591587&extension=ts"))
        channels.add(Channel("ALKASS SPORT 6 HD", "ALKASS SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787906&extension=ts"))

        // =========================
        // AD SPORT
        // =========================
        channels.add(Channel("AD SPORT 1 HD", "AD SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993336&extension=ts"))
        channels.add(Channel("AD SPORT 2 HD", "AD SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993337&extension=ts"))

        // =========================
        // DUBAI SPORT
        // =========================
        channels.add(Channel("DUBAI SPORT 1 HD", "DUBAI SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8086&extension=ts"))
        channels.add(Channel("DUBAI SPORT 2 HD", "DUBAI SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=84251&extension=ts"))
        channels.add(Channel("DUBAI SPORT 3 HD", "DUBAI SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591579&extension=ts"))

        // =========================
        // IRAQIA SPORT
        // =========================
        channels.add(Channel("IRAQIA SPORT HD", "IRAQIA SPORT", "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"))
    }
}
