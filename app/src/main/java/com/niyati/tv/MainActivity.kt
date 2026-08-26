package com.niyati.tv

import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(8, 11, 18))

        val title = TextView(this)
        title.text = "Niyati TV"
        title.textSize = 24f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER_VERTICAL
        title.setPadding(25, 20, 25, 20)

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75.dp()
            )
        )

        playerView = PlayerView(this)
        playerView.useController = true
        playerView.setBackgroundColor(Color.BLACK)

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                260.dp()
            )
        )

        val channelButton = TextView(this)
        channelButton.text = "▶  اختبار beIN SPORT 1HD"
        channelButton.textSize = 18f
        channelButton.setTextColor(Color.WHITE)
        channelButton.gravity = Gravity.CENTER
        channelButton.setBackgroundColor(Color.rgb(233, 21, 66))
        channelButton.setPadding(20, 20, 20, 20)

        channelButton.setOnClickListener {

            playChannel(
                "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"
            )

        }

        root.addView(
            channelButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70.dp()
            ).apply {
                setMargins(20, 20, 20, 20)
            }
        )

        setContentView(root)
    }

    private fun playChannel(url: String) {

        if (player == null) {

            player = ExoPlayer.Builder(this)
                .build()

            playerView.player = player

        }

        val mediaItem =
            MediaItem.fromUri(Uri.parse(url))

        player?.setMediaItem(mediaItem)

        player?.prepare()

        player?.playWhenReady = true
    }

    override fun onStop() {

        super.onStop()

        player?.release()

        player = null
    }

    private fun Int.dp(): Int {

        return (
            this *
            resources.displayMetrics.density
        ).toInt()
    }
}
