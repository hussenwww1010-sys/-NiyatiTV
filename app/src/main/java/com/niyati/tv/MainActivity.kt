package com.niyati.tv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
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
    val packageName: String,
    val url: String
)

class MainActivity : Activity() {

    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var packageList: LinearLayout
    private lateinit var channelList: LinearLayout
    private lateinit var selectedChannelText: TextView

    private var currentChannel: Channel? = null
    private var isFullscreen = false

    private val background = Color.rgb(8, 11, 18)
    private val panel = Color.rgb(15, 20, 30)
    private val card = Color.rgb(22, 28, 40)
    private val selected = Color.rgb(233, 21, 66)
    private val white = Color.WHITE
    private val gray = Color.rgb(170, 178, 190)

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

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        buildInterface()
    }

    private fun buildInterface() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(background)

        val header = TextView(this)

        header.text = "NIYATI TV"
        header.textSize = 28f
        header.setTextColor(white)
        header.setTypeface(null, Typeface.BOLD)
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(25, 0, 25, 0)

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                70
            )
        )

        playerView = PlayerView(this)

        playerView.setBackgroundColor(Color.BLACK)
        playerView.useController = true
        playerView.controllerAutoShow = true

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.48f
            )
        )

        selectedChannelText = TextView(this)

        selectedChannelText.text = "اختر قناة"
        selectedChannelText.textSize = 18f
        selectedChannelText.setTextColor(gray)
        selectedChannelText.gravity = Gravity.CENTER

        root.addView(
            selectedChannelText,
            LinearLayout.LayoutParams(
                -1,
                55
            )
        )

        val content = LinearLayout(this)

        content.orientation = LinearLayout.HORIZONTAL
        content.setBackgroundColor(panel)

        val packageScroll = ScrollView(this)

        packageList = LinearLayout(this)

        packageList.orientation = LinearLayout.VERTICAL
        packageList.setPadding(12, 12, 12, 12)

        packageScroll.addView(packageList)

        content.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                250,
                -1
            )
        )

        val channelScroll = ScrollView(this)

        channelList = LinearLayout(this)

        channelList.orientation = LinearLayout.VERTICAL
        channelList.setPadding(12, 12, 12, 12)

        channelScroll.addView(channelList)

        content.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        root.addView(
            content,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.52f
            )
        )

        setContentView(root)

        createPackages()

        val packages = getPackages()

        if (packages.isNotEmpty()) {
            showPackage(packages.first())
        }
    }

    private fun getPackages(): List<String> {

        return channels
            .map { it.packageName }
            .distinct()
    }

    private fun createPackages() {

        packageList.removeAllViews()

        val packages = getPackages()

        packages.forEachIndexed { index, packageName ->

            val button = createButton(
                packageName,
                if (index == 0) selected else card
            )

            button.setOnClickListener {

                selectPackage(button)
                showPackage(packageName)
            }

            packageList.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    65
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
            )

            if (index == 0) {
                button.requestFocus()
            }
        }
    }

    private fun selectPackage(button: TextView) {

        for (i in 0 until packageList.childCount) {

            val child = packageList.getChildAt(i)

            if (child is TextView) {
                child.setBackgroundColor(card)
            }
        }

        button.setBackgroundColor(selected)
    }

    private fun showPackage(packageName: String) {

        channelList.removeAllViews()

        val packageChannels = channels.filter {
            it.packageName == packageName
        }

        packageChannels.forEachIndexed { index, channel ->

            val button = createButton(
                "▶  ${channel.name}",
                if (index == 0) selected else card
            )

            button.setOnClickListener {

                selectChannel(channel)

                for (i in 0 until channelList.childCount) {

                    val child = channelList.getChildAt(i)

                    if (child is TextView) {
                        child.setBackgroundColor(card)
                    }
                }

                button.setBackgroundColor(selected)
            }

            channelList.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    65
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
            )

            if (index == 0) {
                button.requestFocus()
            }
        })
    }

    private fun createButton(
        text: String,
        color: Int
    ): TextView {

        val button = TextView(this)

        button.text = text
        button.textSize = 18f
        button.setTextColor(white)
        button.setBackgroundColor(color)
        button.gravity = Gravity.CENTER_VERTICAL
        button.setPadding(20, 0, 20, 0)

        button.isFocusable = true
        button.isFocusableInTouchMode = true

        button.setOnFocusChangeListener { view, hasFocus ->

            if (hasFocus) {
                view.setBackgroundColor(selected)
            }
        }

        return button
    }

    private fun selectChannel(channel: Channel) {

        currentChannel = channel

        selectedChannelText.text =
            "▶  ${channel.name}"

        playChannel(channel.url)

        playerView.requestFocus()
    }

    private fun playChannel(url: String) {

        try {

            if (player == null) {

                player = ExoPlayer.Builder(this)
                    .build()

                playerView.player = player

                player?.addListener(
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

            val mediaItem = MediaItem.fromUri(
                Uri.parse(url)
            )

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "حدث خطأ في تشغيل القناة",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun enterFullscreen() {

        isFullscreen = true

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun exitFullscreen() {

        isFullscreen = false

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action == KeyEvent.ACTION_UP &&
            event.keyCode == KeyEvent.KEYCODE_BACK
        ) {

            if (isFullscreen) {

                exitFullscreen()

                return true
            }

            if (player?.isPlaying == true) {

                player?.stop()

                playerView.clearFocus()

                val channel = currentChannel

                if (channel != null) {

                    selectedChannelText.text =
                        "توقفت: ${channel.name}"
                }

                return true
            }
        }

        if (
            event.action == KeyEvent.ACTION_UP &&
            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        ) {

            if (playerView.hasFocus()) {

                if (isFullscreen) {
                    exitFullscreen()
                } else {
                    enterFullscreen()
                }

                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {

        super.onStop()

        player?.release()
        player = null
    }
}
