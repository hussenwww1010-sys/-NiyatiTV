package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
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
            setBackgroundColor(Color.rgb(8, 10, 16))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val packageScrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        packageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(dpToPx(12f), dpToPx(16f), dpToPx(12f), dpToPx(16f))
            setBackgroundColor(Color.rgb(13, 16, 24))
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
            LinearLayout.LayoutParams(dpToPx(240f), LinearLayout.LayoutParams.MATCH_PARENT)
        )

        centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8f), dpToPx(8f), dpToPx(8f), dpToPx(8f))
        }

        val playerContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
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
                dpToPx(260f)
            )
        )

        val channelScrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        channelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8f), dpToPx(8f), dpToPx(8f), dpToPx(8f))
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
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(48f)
                ).apply {
                    bottomMargin = dpToPx(4f)
                }
                setOnFocusChangeListener { view, hasFocus ->
                    view.setBackgroundColor(
                        if (hasFocus) Color.rgb(35, 90, 170) else Color.TRANSPARENT
                    )
                }
                setOnClickListener { showChannels(group) }
            }
            packageLayout.addView(text)
            if (index == 0) text.requestFocus()
        }
    }

    private fun showChannels(group: String) {
        currentGroup = group
        channelLayout.removeAllViews()

        val groupChannels = channels.filter { it.group == group }
        groupChannels.forEachIndexed { index, channel ->
            val text = TextView(this).apply {
                this.text = channel.name
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
                isFocusable = true
                isFocusableInTouchMode = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(44f)
                ).apply {
                    bottomMargin = dpToPx(4f)
                }
                setBackgroundColor(Color.rgb(18, 22, 32))
                setOnFocusChangeListener { view, hasFocus ->
                    view.setBackgroundColor(
                        if (hasFocus) Color.rgb(35, 90, 170) else Color.rgb(18, 22, 32)
                    )
                }
                setOnClickListener {
                    val realIndex = channels.indexOf(channel)
                    playChannel(channel, realIndex)
                }
            }
            channelLayout.addView(text)
            if (index == 0) text.requestFocus()
        }
    }

    private fun packageDisplayName(group: String): String {
        return when (group) {
            "Alwan" -> "🎨 ألوان"
            "الدوري الإيطالي - Serie A" -> "🇮🇹 الدوري الإيطالي"
            "StarzPlay" -> "⭐ StarzPlay"
            "Shahid" -> "🟢 شاهد"
            "الكأس" -> "🏆 الكأس"
            "الرابعة العراقية - Al Rabiaa" -> "🇮🇶 الرابعة العراقية"
            "Post Sport" -> "⚽ Post Sport"
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
        playerView.requestFocus()
    }

    private fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        packageLayout.parent?.let { (it as View).visibility = View.VISIBLE }
        channelLayout.parent?.let { (it as View).visibility = View.VISIBLE }
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
        val server = "http://m3u.drm-26.com:80/live/Mkdtv1_061261/123456/"

        // Alwan Network
        channels.add(Channel("ALWAN SPORT 1 4K", "Alwan", "${server}1001.ts"))
        channels.add(Channel("ALWAN SPORT 2 4K", "Alwan", "${server}1002.ts"))
        channels.add(Channel("ALWAN SPORT 3 4K", "Alwan", "${server}1003.ts"))
        channels.add(Channel("ALWAN SPORT 4 4K", "Alwan", "${server}1004.ts"))
        channels.add(Channel("ALWAN SPORT 1 HD", "Alwan", "${server}1005.ts"))
        channels.add(Channel("ALWAN SPORT 2 HD", "Alwan", "${server}1006.ts"))
        channels.add(Channel("ALWAN SPORT 3 HD", "Alwan", "${server}1007.ts"))
        channels.add(Channel("ALWAN SPORT 4 HD", "Alwan", "${server}1008.ts"))
        channels.add(Channel("ALWAN SPORT 1 SD", "Alwan", "${server}1009.ts"))
        channels.add(Channel("ALWAN SPORT 2 SD", "Alwan", "${server}1010.ts"))
        channels.add(Channel("ALWAN MOVIES 4K", "Alwan", "${server}1011.ts"))
        channels.add(Channel("ALWAN CINEMA 4K", "Alwan", "${server}1012.ts"))
        channels.add(Channel("ALWAN ANIME 4K", "Alwan", "${server}1013.ts"))
        channels.add(Channel("ALWAN KIDS 4K", "Alwan", "${server}1014.ts"))
        channels.add(Channel("ALWAN DOCUMENTARY 4K", "Alwan", "${server}1015.ts"))
        channels.add(Channel("ALWAN UFC 4K", "Alwan", "${server}1016.ts"))
        channels.add(Channel("ALWAN F1 4K", "Alwan", "${server}1017.ts"))
        channels.add(Channel("ALWAN SERIES 1 4K", "Alwan", "${server}1018.ts"))
        channels.add(Channel("ALWAN SERIES 2 4K", "Alwan", "${server}1019.ts"))

        // Serie A
        channels.add(Channel("Serie A Pass 1 HD", "الدوري الإيطالي - Serie A", "${server}2001.ts"))
        channels.add(Channel("Serie A Pass 2 HD", "الدوري الإيطالي - Serie A", "${server}2002.ts"))
        channels.add(Channel("Serie A Pass 3 HD", "الدوري الإيطالي - Serie A", "${server}2003.ts"))
        channels.add(Channel("Serie A Matchday 1", "الدوري الإيطالي - Serie A", "${server}2004.ts"))
        channels.add(Channel("Serie A Matchday 2", "الدوري الإيطالي - Serie A", "${server}2005.ts"))

        // StarzPlay & AD Sports
        channels.add(Channel("AD SPORTS 1 HD", "StarzPlay", "${server}3001.ts"))
        channels.add(Channel("AD SPORTS 2 HD", "StarzPlay", "${server}3002.ts"))
        channels.add(Channel("AD SPORTS 3 HD", "StarzPlay", "${server}3003.ts"))
        channels.add(Channel("AD SPORTS PREMIUM 1 HD", "StarzPlay", "${server}3004.ts"))
        channels.add(Channel("StarzPlay Action", "StarzPlay", "${server}3005.ts"))
        channels.add(Channel("StarzPlay Cinema", "StarzPlay", "${server}3006.ts"))

        // Shahid Network
        channels.add(Channel("Shahid Drama HD", "Shahid", "${server}4001.ts"))
        channels.add(Channel("Shahid Cinema HD", "Shahid", "${server}4002.ts"))
        channels.add(Channel("Shahid Series HD", "Shahid", "${server}4003.ts"))
        channels.add(Channel("Shahid Events Live 1", "Shahid", "${server}4004.ts"))
        channels.add(Channel("Shahid Events Live 2", "Shahid", "${server}4005.ts"))

        // Alkass
        channels.add(Channel("Alkass One HD", "الكأس", "${server}5001.ts"))
        channels.add(Channel("Alkass Two HD", "الكأس", "${server}5002.ts"))
        channels.add(Channel("Alkass Three HD", "الكأس", "${server}5003.ts"))
        channels.add(Channel("Alkass Four HD", "الكأس", "${server}5004.ts"))
        channels.add(Channel("Alkass Five HD", "الكأس", "${server}5005.ts"))
        channels.add(Channel("Alkass Six HD", "الكأس", "${server}5006.ts"))
        channels.add(Channel("Alkass Seven HD", "الكأس", "${server}5007.ts"))
        channels.add(Channel("Alkass Eight HD", "الكأس", "${server}5008.ts"))

        // Al Rabiaa
        channels.add(Channel("Al Rabiaa TV HD", "الرابعة العراقية - Al Rabiaa", "${server}6001.ts"))
        channels.add(Channel("Al Rabiaa Sport 1 HD", "الرابعة العراقية - Al Rabiaa", "${server}6002.ts"))
        channels.add(Channel("Al Rabiaa Sport 2 HD", "الرابعة العراقية - Al Rabiaa", "${server}6003.ts"))

        // Post Sport
        channels.add(Channel("Post Sport Main", "Post Sport", "${server}7001.ts"))
        channels.add(Channel("Post Sport 1 HD", "Post Sport", "${server}7002.ts"))
        channels.add(Channel("Post Sport 2 HD", "Post Sport", "${server}7003.ts"))
        channels.add(Channel("Post Sport 3 HD", "Post Sport", "${server}7004.ts"))
        channels.add(Channel("Post Sport 4 HD", "Post Sport", "${server}7005.ts"))
        channels.add(Channel("Post Sport 5 HD", "Post Sport", "${server}7006.ts"))
        channels.add(Channel("Post Sport 6 HD", "Post Sport", "${server}7007.ts"))
    }
}
