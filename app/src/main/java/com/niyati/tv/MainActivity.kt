package com.niyati.tv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    private val red = Color.rgb(233, 21, 66)
    private val background = Color.rgb(8, 11, 18)
    private val card = Color.rgb(18, 23, 33)

    private val testChannel =
        "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(background)
        root.setPadding(20, 20, 20, 20)

        val title = TextView(this)

        title.text = "NIYATI TV"
        title.textSize = 30f
        title.setTextColor(Color.WHITE)
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER_VERTICAL

        root.addView(
            title,
            LinearLayout.LayoutParams(-1, 70)
        )

        val playerTitle = TextView(this)

        playerTitle.text = "المشغل"
        playerTitle.textSize = 20f
        playerTitle.setTextColor(Color.WHITE)
        playerTitle.gravity = Gravity.CENTER_VERTICAL

        root.addView(
            playerTitle,
            LinearLayout.LayoutParams(-1, 50)
        )

        playerView = PlayerView(this)

        playerView.setBackgroundColor(Color.BLACK)
        playerView.useController = true

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val channel = TextView(this)

        channel.text = "▶  تشغيل beIN SPORT 1HD"
        channel.textSize = 20f
        channel.setTextColor(Color.WHITE)
        channel.gravity = Gravity.CENTER
        channel.setBackgroundColor(red)
        channel.isFocusable = true

        root.addView(
            channel,
            LinearLayout.LayoutParams(-1, 70)
        )

        channel.setOnClickListener {
            playChannel(testChannel)
        }

        setContentView(root)

        channel.requestFocus()
    }

    private fun playChannel(url: String) {

        if (player == null) {

            player = ExoPlayer.Builder(this)
                .build()

            playerView.player = player
        }

        val mediaItem = MediaItem.fromUri(
            Uri.parse(url)
        )

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onStop() {

        super.onStop()

        player?.release()
        player = null
    }
}
