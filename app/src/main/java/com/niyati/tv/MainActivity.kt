package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
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
import androidx.media3.ui.PlayerView

data class Channel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : Activity() {

    private var exoPlayer: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var bottomArea: LinearLayout
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsLayout: LinearLayout
    private lateinit var nowPlaying: TextView
    private lateinit var fullscreenButton: TextView

    private var fullscreen = false
    private var currentGroup = ""

    private val backgroundColor = Color.rgb(6, 10, 17)
    private val panelColor = Color.rgb(11, 17, 27)
    private val cardColor = Color.rgb(20, 29, 43)
    private val selectedColor = Color.rgb(225, 22, 55)
    private val textColor = Color.WHITE
    private val secondaryText = Color.rgb(170, 180, 195)

    /*
     * القنوات الحالية
     *
     * نترك روابط القنوات التي كانت عندك.
     * تستطيع إضافة بقية قنواتك بنفس الطريقة لاحقاً.
     */
    private val channels = listOf(

        Channel(
            "beIN SPORT 1HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"
        ),

        Channel(
            "beIN SPORT 2HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330438&extension=ts"
        ),

        Channel(
            "beIN SPORT 3HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411381&extension=ts"
        ),

        Channel(
            "beIN SPORT 4HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411380&extension=ts"
        ),

        Channel(
            "beIN SPORT 5HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411379&extension=ts"
        ),

        Channel(
            "beIN SPORT 6HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411378&extension=ts"
        ),

        Channel(
            "beIN SPORT 7HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411377&extension=ts"
        ),

        Channel(
            "beIN SPORT 8HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411376&extension=ts"
        ),

        Channel(
            "beIN SPORT 9HD",
            "beIN SPORTS",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411375&extension=ts"
        ),

        Channel(
            "ALWAN SPORT 1HD",
            "ALWAN SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859098&extension=ts"
        ),

        Channel(
            "ALWAN SPORT 2HD",
            "ALWAN SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859097&extension=ts"
        ),

        Channel(
            "ALWAN SPORT 3HD",
            "ALWAN SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859096&extension=ts"
        ),

        Channel(
            "ALWAN SPORT 4HD",
            "ALWAN SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859095&extension=ts"
        ),

        Channel(
            "ALWAN SPORT 5HD",
            "ALWAN SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859094&extension=ts"
        ),

        Channel(
            "ALWAN SPORT 6HD",
            "ALWAN SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859093&extension=ts"
        ),

        Channel(
            "THAMANYA 1HD",
            "THAMANYA",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936356&extension=ts"
        ),

        Channel(
            "THAMANYA 2HD",
            "THAMANYA",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936355&extension=ts"
        ),

        Channel(
            "THAMANYA 3HD",
            "THAMANYA",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936354&extension=ts"
        ),

        Channel(
            "ALKASS SPORT 1HD",
            "ALKASS SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591593&extension=ts"
        ),

        Channel(
            "ALKASS SPORT 2HD",
            "ALKASS SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591591&extension=ts"
        ),

        Channel(
            "ALKASS SPORT 3HD",
            "ALKASS SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787903&extension=ts"
        ),

        Channel(
            "ALKASS SPORT 4HD",
            "ALKASS SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591589&extension=ts"
        ),

        Channel(
            "ALKASS SPORT 5HD",
            "ALKASS SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591587&extension=ts"
        ),

        Channel(
            "ALKASS SPORT 6HD",
            "ALKASS SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787906&extension=ts"
        ),

        Channel(
            "AD SPORT 1HD",
            "AD SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993336&extension=ts"
        ),

        Channel(
            "AD SPORT 2HD",
            "AD SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993337&extension=ts"
        ),

        Channel(
            "DUBAI SPORT 1HD",
            "DUBAI SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8086&extension=ts"
        ),

        Channel(
            "DUBAI SPORT 2HD",
            "DUBAI SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=84251&extension=ts"
        ),

        Channel(
            "DUBAI SPORT 3HD",
            "DUBAI SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591579&extension=ts"
        ),

        Channel(
            "IRAQIA SPORT HD",
            "IRAQIA SPORT",
            "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        buildInterface()
    }

    private fun buildInterface() {

        root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(backgroundColor)

        // ==============================
        // TOP BAR
        // ==============================

        topBar = LinearLayout(this)

        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(
            dp(24),
            0,
            dp(24),
            0
        )

        topBar.setBackgroundColor(
            Color.rgb(8, 14, 24)
        )

        val logo = TextView(this)

        logo.text = "NIYATI TV"
        logo.textSize = 25f
        logo.setTextColor(textColor)
        logo.setTypeface(
            null,
            Typeface.BOLD
        )
        logo.gravity =
            Gravity.CENTER_VERTICAL

        topBar.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        val live = TextView(this)

        live.text = "LIVE"
        live.textSize = 14f
        live.setTextColor(textColor)
        live.setTypeface(
            null,
            Typeface.BOLD
        )
        live.gravity = Gravity.CENTER

        live.setBackgroundColor(
            selectedColor
        )

        topBar.addView(
            live,
            LinearLayout.LayoutParams(
                dp(70),
                dp(38)
            )
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                -1,
                dp(64)
            )
        )

        // ==============================
        // PLAYER
        // ==============================

        playerContainer = FrameLayout(this)

        playerContainer.setBackgroundColor(
            Color.BLACK
        )

        playerView = PlayerView(this)

        playerView.useController = true
        playerView.setBackgroundColor(Color.BLACK)
        playerView.isFocusable = true

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        fullscreenButton = TextView(this)

        fullscreenButton.text = "⛶"
        fullscreenButton.textSize = 28f
        fullscreenButton.setTextColor(textColor)
        fullscreenButton.gravity = Gravity.CENTER

        fullscreenButton.setBackgroundColor(
            Color.argb(
                190,
                0,
                0,
                0
            )
        )

        fullscreenButton.isFocusable = true

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        val fullscreenParams =
            FrameLayout.LayoutParams(
                dp(60),
                dp(60)
            )

        fullscreenParams.gravity =
            Gravity.BOTTOM or Gravity.END

        fullscreenParams.setMargins(
            0,
            0,
            dp(18),
            dp(18)
        )

        playerContainer.addView(
            fullscreenButton,
            fullscreenParams
        )

        root.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.56f
            )
        )

        // ==============================
        // NOW PLAYING
        // ==============================

        nowPlaying = TextView(this)

        nowPlaying.text =
            "اختر قناة لبدء المشاهدة"

        nowPlaying.textSize = 17f
        nowPlaying.setTextColor(textColor)

        nowPlaying.gravity =
            Gravity.CENTER_VERTICAL

        nowPlaying.setPadding(
            dp(22),
            0,
            dp(22),
            0
        )

        nowPlaying.setBackgroundColor(
            panelColor
        )

        root.addView(
            nowPlaying,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        // ==============================
        // BOTTOM
        // ==============================

        bottomArea = LinearLayout(this)

        bottomArea.orientation =
            LinearLayout.HORIZONTAL

        bottomArea.setBackgroundColor(
            panelColor
        )

        // PACKAGES

        val packageScroll =
            ScrollView(this)

        packagesLayout =
            LinearLayout(this)

        packagesLayout.orientation =
            LinearLayout.VERTICAL

        packagesLayout.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        packageScroll.addView(
            packagesLayout
        )

        bottomArea.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                dp(250),
                -1
            )
        )

        // CHANNELS

        val channelScroll =
            ScrollView(this)

        channelsLayout =
            LinearLayout(this)

        channelsLayout.orientation =
            LinearLayout.VERTICAL

        channelsLayout.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        channelScroll.addView(
            channelsLayout
        )

        bottomArea.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        root.addView(
            bottomArea,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.44f
            )
        )

        setContentView(root)

        loadPackages()
    }

    // ==============================
    // PACKAGES
    // ==============================

    private fun loadPackages() {

        packagesLayout.removeAllViews()

        val groups =
            channels.map {
                it.group
            }.distinct()

        if (groups.isEmpty()) {
            return
        }

        currentGroup = groups[0]

        groups.forEachIndexed {
                index,
                group ->

            val button =
                createPackageButton(
                    group,
                    index == 0
                )

            button.setOnClickListener {

                currentGroup = group

                for (
                    i in 0 until packagesLayout.childCount
                ) {

                    packagesLayout
                        .getChildAt(i)
                        .setBackgroundColor(
                            cardColor
                        )
                }

                button.setBackgroundColor(
                    selectedColor
                )

                loadChannels(group)
            }

            packagesLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    dp(60)
                ).apply {
                    setMargins(
                        0,
                        0,
                        0,
                        dp(8)
                    )
                }
            )
        }

        loadChannels(groups[0])
    }

    // ==============================
    // CHANNELS
    // ==============================

    private fun loadChannels(
        group: String
    ) {

        channelsLayout.removeAllViews()

        val filtered =
            channels.filter {
                it.group == group
            }

        filtered.forEachIndexed {
                index,
                channel ->

            val button =
                createChannelButton(
                    channel,
                    index == 0
                )

            button.setOnClickListener {

                for (
                    i in 0 until channelsLayout.childCount
                ) {

                    channelsLayout
                        .getChildAt(i)
                        .setBackgroundColor(
                            cardColor
                        )
                }

                button.setBackgroundColor(
                    selectedColor
                )

                playChannel(channel)
            }

            channelsLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    dp(62)
                ).apply {
                    setMargins(
                        0,
                        0,
                        0,
                        dp(8)
                    )
                }
            )

            if (index == 0) {
                button.requestFocus()
            }
        }
    }

    // ==============================
    // PACKAGE BUTTON
    // ==============================

    private fun createPackageButton(
        name: String,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        button.text = name
        button.textSize = 17f
        button.setTextColor(textColor)
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(
            dp(18),
            0,
            dp(10),
            0
        )

        button.setBackgroundColor(
            if (selected) {
                selectedColor
            } else {
                cardColor
            }
        )

        button.isFocusable = true
        button.isFocusableInTouchMode = true

        button.setOnFocusChangeListener {
                view,
                hasFocus ->

            if (hasFocus) {
                view.setBackgroundColor(
                    selectedColor
                )
            }
        }

        return button
    }

    // ==============================
    // CHANNEL BUTTON
    // ==============================

    private fun createChannelButton(
        channel: Channel,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        val number =
            channels.indexOf(channel) + 1

        button.text =
            "$number    ${channel.name}                         ▶"

        button.textSize = 17f
        button.setTextColor(textColor)

        button.gravity =
            Gravity.CENTER_VERTICAL

        button.setPadding(
            dp(18),
            0,
            dp(18),
            0
        )

        button.setBackgroundColor(
            if (selected) {
                selectedColor
            } else {
                cardColor
            }
        )

        button.isFocusable = true
        button.isFocusableInTouchMode = true

        button.setOnFocusChangeListener {
                view,
                hasFocus ->

            if (hasFocus) {

                view.setBackgroundColor(
                    selectedColor
                )

            }
        }

        return button
    }

    // ==============================
    // PLAY
    // ==============================

    private fun playChannel(
        channel: Channel
    ) {

        nowPlaying.text =
            "▶  ${channel.name}"

        try {

            if (exoPlayer == null) {

                exoPlayer =
                    ExoPlayer.Builder(this)
                        .build()

                playerView.player =
                    exoPlayer

                exoPlayer?.addListener(
                    object : Player.Listener {

                        override fun onPlayerError(
                            error: PlaybackException
                        ) {

                            Toast.makeText(
                                this@MainActivity,
                                "تعذر تشغيل القناة",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }

            val mediaItem =
                MediaItem.fromUri(
                    Uri.parse(channel.url)
                )

            exoPlayer?.setMediaItem(
                mediaItem
            )

            exoPlayer?.prepare()

            exoPlayer?.playWhenReady =
                true

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "خطأ في تشغيل القناة",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==============================
    // FULLSCREEN
    // ==============================

    private fun toggleFullscreen() {

        if (fullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {

        fullscreen = true

        topBar.visibility =
            View.GONE

        nowPlaying.visibility =
            View.GONE

        bottomArea.visibility =
            View.GONE

        val params =
            playerContainer.layoutParams

        params.height =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        playerContainer.layoutParams =
            params

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun exitFullscreen() {

        fullscreen = false

        topBar.visibility =
            View.VISIBLE

        nowPlaying.visibility =
            View.VISIBLE

        bottomArea.visibility =
            View.VISIBLE

        val params =
            playerContainer.layoutParams

        params.height = 0

        params.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        playerContainer.layoutParams =
            params

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // ==============================
    // REMOTE
    // ==============================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_UP
        ) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_BACK -> {

                    if (fullscreen) {

                        exitFullscreen()

                        return true
                    }

                    if (
                        exoPlayer != null &&
                        exoPlayer?.isPlaying == true
                    ) {

                        exoPlayer?.stop()

                        nowPlaying.text =
                            "اختر قناة لبدء المشاهدة"

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER -> {

                    if (
                        playerView.hasFocus()
                    ) {

                        toggleFullscreen()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // ==============================
    // CLEANUP
    // ==============================

    override fun onDestroy() {

        exoPlayer?.release()

        exoPlayer = null

        super.onDestroy()
    }

    // ==============================
    // DP
    // ==============================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }
}
