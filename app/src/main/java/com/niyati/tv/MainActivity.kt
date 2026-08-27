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
    private var fullscreen = false
    private var currentGroup = ""

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var bottomArea: LinearLayout
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsLayout: LinearLayout
    private lateinit var nowPlaying: TextView
    private lateinit var fullscreenButton: TextView

    private val background = Color.rgb(5, 8, 14)
    private val topColor = Color.rgb(8, 13, 22)
    private val panelColor = Color.rgb(10, 16, 26)
    private val cardColor = Color.rgb(18, 27, 40)
    private val cardFocus = Color.rgb(32, 45, 64)
    private val accent = Color.rgb(226, 25, 58)
    private val white = Color.WHITE
    private val gray = Color.rgb(155, 166, 182)

    private val base =
        "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun c(
        name: String,
        group: String,
        id: String
    ): Channel {
        return Channel(
            name,
            group,
            "$base$id.ts"
        )
    }

    /*
     * ============================================================
     * CHANNEL DATABASE
     * ============================================================
     */

    private val channels = mutableListOf<Channel>().apply {

        // BEIN TOD
        add(c("beIN Tod 4K", "beIN TOD", "460835"))
        for (i in 1..9)
            add(c("beIN Sport Tod $i", "beIN TOD", "${460835 + i}"))

        add(c("beIN Sport Tod English 1", "beIN TOD", "460845"))
        add(c("beIN Sport Tod English 2", "beIN TOD", "460846"))

        for (i in 1..9)
            add(c("beIN Sport Tod Extra $i", "beIN TOD", "${460846 + i}"))

        // BEIN SPORTS
        add(c("beIN Sport Global 4K", "beIN SPORTS", "22186"))
        add(c("beIN Sport News 4K", "beIN SPORTS", "318230"))

        val bein4k = listOf(
            318197, 318198, 318199, 440580,
            318201, 318202, 318203, 318204, 318205
        )

        for (i in 1..9) {
            add(c("beIN Sport $i 4K", "beIN SPORTS", bein4k[i - 1].toString()))
            add(c("beIN$i H265", "beIN SPORTS", "${391093 + i}"))
        }

        add(c("beIN Sport English 1 4K", "beIN SPORTS", "319495"))
        add(c("beIN Sport English 2 4K", "beIN SPORTS", "319496"))
        add(c("beIN Sport French 1 4K", "beIN SPORTS", "319497"))
        add(c("beIN Sport French 2 4K", "beIN SPORTS", "319498"))
        add(c("beIN Sport NBA 4K", "beIN SPORTS", "319499"))
        add(c("beIN Global HD", "beIN SPORTS", "442220"))
        add(c("beIN Sport News HD", "beIN SPORTS", "443146"))

        for (i in 1..9)
            add(c("beIN Sport $i HD", "BEIN SPORTS", "${325792 + i}"))

        add(c("beIN Sport 1 HD English", "beIN SPORTS", "318217"))
        add(c("beIN Sport 2 HD English", "beIN SPORTS", "318218"))
        add(c("beIN Sport 1 HD Frensh", "beIN SPORTS", "319437"))
        add(c("beIN Sport 2 HD Frensh", "beIN SPORTS", "319438"))
        add(c("beIN Sport NBA HD", "beIN SPORTS", "318219"))

        for (i in 1..9)
            add(c("beIN Sport $i SD", "beIN SPORTS", "${325802 + i}"))

        add(c("beIN Sport English 1 SD", "beIN SPORTS", "319425"))
        add(c("beIN Sport English 2 SD", "beIN SPORTS", "319426"))
        add(c("beIN Sport French 1 SD", "beIN SPORTS", "319427"))
        add(c("beIN Sport French 2 SD", "beIN SPORTS", "319428"))

        // BEIN XTRA
        val xtra4k = listOf(
            325790, 319487, 319488, 440569, 440570,
            440571, 447243, 447244, 447245
        )

        val xtraHd = listOf(
            325802, 319435, 319436, 440572, 440573,
            440574, 447246, 447247, 447248
        )

        val xtraSd = listOf(
            325812, 319423, 319424, 440575, 440576,
            440577, 447249, 447250, 447251
        )

        for (i in 1..9) {
            add(c("beIN Sport XTRA $i 4K", "BEIN XTRA", xtra4k[i - 1].toString()))
            add(c("beIN Sport Xtra $i HD", "BEIN XTRA", xtraHd[i - 1].toString()))
            add(c("beIN Sport Xtra $i SD", "BEIN XTRA", xtraSd[i - 1].toString()))
        }

        // AL RABIAA
        listOf(
            "AL RABIAA SPORT 1" to "371931",
            "AL RABIAA SPORT 1+" to "371933",
            "AL RABIAA SPORT 2" to "371932",
            "Rabiaa Sport +2" to "434565",
            "AL RABIAA TV 4K" to "371939",
            "AL RABIAA MOVIES" to "371934",
            "Rabiaa Variety" to "434566",
            "Njoom Al Rabiaa" to "434567",
            "AL RABIAA SERIES" to "371935",
            "AL RABIAA GEO" to "371936",
            "AL RABIAA QURAN" to "371937",
            "AL RABIAA MUSICA" to "371938"
        ).forEach {
            add(c(it.first, "AL RABIAA", it.second))
        }

        // ALKASS
        val alkassIds = listOf(
            96214, 96215, 278068, 96216, 96217,
            211523, 379828, 379829, 393991, 393992
        )

        for (i in 1..10)
            add(c("Alkass $i HD", "ALKASS", alkassIds[i - 1].toString()))

        // SAUDI SPORTS
        listOf(
            "KSA Sport 1 4K" to "97805",
            "KSA Sport 2 4K" to "97806",
            "KSA Sport 3 4K" to "97807",
            "SAUDUA NOW" to "97808",
            "SAUDI 24 SPORT HD" to "100470",
            "STC SPORT 1 HD" to "421391",
            "STC SPORT 2 HD" to "421392",
            "STC SPORT 3 HD" to "420903",
            "STC SPORT 4 HD" to "433178"
        ).forEach {
            add(c(it.first, "SAUDI SPORTS", it.second))
        }

        // GULF SPORTS
        listOf(
            "Dubai Sport 1 HD" to "97813",
            "Dubai Sport 2 HD" to "97814",
            "Dubai Racing 1 HD" to "97816",
            "On Sport HD 1" to "97820",
            "ON SPORTS MAX 4K" to "97821",
            "AR: ON TIME SPORT FM" to "399432",
            "ON SPORTS PLUS HD" to "97825",
            "Oman Sport HD" to "97877",
            "KUWAIT SPORT 4K" to "97826",
            "BAHRAIN SPORT 1 HD" to "66383",
            "YAS SPORT HD" to "328659",
            "ALRABIAA SPORT 4K" to "328660",
            "LIBYA SPORT 2 4K" to "328661",
            "BAHRAIN SPORT 2 HD" to "328662",
            "KUWAIT SPORT PLUS 4K" to "328663",
            "PALASTINE SPORT 4K" to "328664",
            "Iraqia Sport" to "107038",
            "ufm radio" to "267050",
            "Sharjah Sport HD" to "141797",
            "Libya Sport 1 TV" to "97818",
            "Jordan Sport TV" to "109699",
            "Zamalik" to "97822",
            "Nile Sport" to "97824",
            "Al Ahly TV" to "97823",
            "PalestineSport" to "417306"
        ).forEach {
            add(c(it.first, "GULF SPORTS", it.second))
        }

        // AD SPORTS
        listOf(
            "AD SPORTS 1 HD" to "326053",
            "AD SPORTS 2 HD" to "326054",
            "AD Sport Asia 1 HD" to "244188",
            "AD Sport Asia 2 HD" to "244191"
        ).forEach {
            add(c(it.first, "AD SPORTS", it.second))
        }

        // ALWAN SPORT
        val alwan = listOf(
            418111, 418112, 418113,
            418114, 418115, 418116,
            418117, 418118, 418119,
            418120, 418121, 418122,
            418123, 418124, 418125,
            418126, 418127, 418128
        )

        for (i in 1..6) {
            val x = (i - 1) * 3
            add(c("Alwan Sport $i 4K", "ALWAN SPORT", alwan[x].toString()))
            add(c("Alwan Sport $i HD", "ALWAN SPORT", alwan[x + 1].toString()))
            add(c("Alwan Sport $i SD", "ALWAN SPORT", alwan[x + 2].toString()))
        }

        listOf(
            "Alwan Sport 7 4K" to "433739",
            "Alwan Sport 8 4K" to "433740",
            "Alwan Sport 9 4K" to "433741",
            "Alwan Sport 10 4K" to "433742"
        ).forEach {
            add(c(it.first, "ALWAN SPORT", it.second))
        }

        // CRICKET
        listOf(
            "DS: SS Cricket HD" to "362434",
            "UK: SKY SPORTS CRICKET HD" to "376914",
            "UK: ASTRO CRICKET" to "376935",
            "UK: CRICKET LIVE 3HD" to "376934",
            "UK: CRICKET LIVE 2HD" to "376933",
            "UK: CRICKET LIVE 1HD" to "376932",
            "VIP UK: SkySport Cricket HD" to "376852",
            "UK: HUB SPORTS 4" to "377011",
            "UK: HUB SPORTS 3" to "377010",
            "BD: T SPORTS HD" to "397831",
            "PK: FAST SPORTS FHD" to "380185",
            "PK: PTV SPORTS HD" to "380189",
            "PK: Ten Sports HD" to "380193",
            "PK: PTV SPORTS" to "379173"
        ).forEach {
            add(c(it.first, "CRICKET", it.second))
        }

        // STAR SPORTS
        listOf(
            "IN: Star Sports 1 FHD" to "387564",
            "IN: Star Sports 1 Hindi FHD" to "387565",
            "IN: Star Sports 2 FHD" to "387566",
            "IN: Star Sports Select 1 FHD" to "387568",
            "IN: Star Sports Select 2 FHD" to "387569",
            "IN: Star Sports 1 Eng HD" to "387722",
            "IN: Star Sports 2 Eng HD" to "387723",
            "IN: Star Sports 3 Eng HD" to "387724",
            "IN: Star Sports Select 1 Eng HD" to "387725",
            "IN: Star Sports Select 2 Eng HD" to "387726",
            "IN: Willow Cricket HD" to "387766",
            "IN: Ten Sports" to "387788",
            "IN: Star Sports 1 Hindi HD" to "387909",
            "IN: STAR SPORTS SELECT 2" to "364779",
            "IN: STAR SPORTS SELECT 1" to "364780",
            "IN: STAR SPORTS 3" to "364781",
            "IN: STAR SPORTS 2" to "364782",
            "IN: STAR SPORTS 1" to "364783",
            "IN: STAR SPORTS 1 TAMIL" to "364864",
            "USA | Willow Cricket HD" to "386666",
            "USA | Willow Cricket Extra" to "386665"
        ).forEach {
            add(c(it.first, "STAR SPORTS", it.second))
        }

        // FAJER TV
        listOf(
            "Fajer TV 1" to "463532",
            "Fajer TV 2" to "463533",
            "Fajer TV 3" to "463534",
            "Fajer TV 4" to "463535",
            "Faher TV 5" to "463536"
        ).forEach {
            add(c(it.first, "FAJER TV", it.second))
        }

        // KURDISTAN SPORTS
        listOf(
            "KU: Duhok Sport" to "358226",
            "KU: LD Sport" to "358229",
            "KU: See Sport 1" to "358222",
            "KU: See Sport 2" to "358223",
            "KU: See Sport 3" to "358224",
            "KU: Ava Sport" to "358221",
            "KU: Aro Sport" to "358225",
            "KU: 4 Sport" to "358227",
            "KU: Astera Sport" to "358228",
            "KU: NRT Sport" to "358220",
            "KU: Kurdistan Sport" to "358219",
            "KU: Dasinya Sport" to "358230",
            "KU: MTV Sport" to "358231",
            "KU: Aso Sport" to "358232",
            "KU: Newline Sport" to "358233",
            "KU: MMN SPORT" to "358234",
            "KU: NUBAR SPORT" to "358235",
            "KU: SIMA SPORT" to "358236",
            "KU: Zaxo Sport" to "358237",
            "KU: LD SPORT CHEAK" to "358238",
            "KU: Delal Sport" to "358239"
        ).forEach {
            add(c(it.first, "KURDISTAN SPORTS", it.second))
        }

        // SHAHID SPORT
        for (i in 1..5)
            add(c("Shahid Spot$i 4K", "SHAHID SPORT", "${430910 + i}"))

        // SHASHA
        listOf(
            "Shasha 1 TV 4K" to "348400",
            "Shasha 2 TV 4K" to "244079",
            "Shasha 3 TV 4K" to "443029"
        ).forEach {
            add(c(it.first, "SHASHA", it.second))
        }
    }

    /*
     * ============================================================
     * CREATE
     * ============================================================
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        buildInterface()
    }

    /*
     * ============================================================
     * MAIN INTERFACE
     * ============================================================
     */

    private fun buildInterface() {

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(background)

        createTopBar()
        createPlayer()
        createNowPlaying()
        createChannelArea()

        setContentView(root)

        loadPackages()
    }

    /*
     * ============================================================
     * TOP BAR
     * ============================================================
     */

    private fun createTopBar() {

        topBar = LinearLayout(this)

        topBar.orientation =
            LinearLayout.HORIZONTAL

        topBar.gravity =
            Gravity.CENTER_VERTICAL

        topBar.setPadding(
            dp(22),
            dp(8),
            dp(22),
            dp(8)
        )

        topBar.setBackgroundColor(topColor)

        /*
         * LOGO
         */

        val logo = TextView(this)

        logo.text = "NT"

        logo.textSize = 19f

        logo.setTextColor(white)

        logo.gravity = Gravity.CENTER

        logo.setTypeface(
            null,
            Typeface.BOLD
        )

        logo.setBackgroundColor(accent)

        topBar.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        /*
         * BRAND
         */

        val brand = LinearLayout(this)

        brand.orientation =
            LinearLayout.VERTICAL

        brand.gravity =
            Gravity.CENTER_VERTICAL

        brand.setPadding(
            dp(13),
            0,
            0,
            0
        )

        val title = TextView(this)

        title.text = "NIYATI TV"

        title.textSize = 21f

        title.setTextColor(white)

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        val subtitle = TextView(this)

        subtitle.text =
            "LIVE SPORTS & TV"

        subtitle.textSize = 10f

        subtitle.setTextColor(gray)

        brand.addView(title)

        brand.addView(subtitle)

        topBar.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        /*
         * LIVE INDICATOR
         */

        val liveBox = LinearLayout(this)

        liveBox.orientation =
            LinearLayout.HORIZONTAL

        liveBox.gravity =
            Gravity.CENTER

        liveBox.setPadding(
            dp(14),
            0,
            dp(14),
            0
        )

        val liveDot = TextView(this)

        liveDot.text = "●"

        liveDot.textSize = 12f

        liveDot.setTextColor(accent)

        val liveText = TextView(this)

        liveText.text = " LIVE"

        liveText.textSize = 13f

        liveText.setTextColor(white)

        liveText.setTypeface(
            null,
            Typeface.BOLD
        )

        liveBox.addView(liveDot)

        liveBox.addView(liveText)

        topBar.addView(
            liveBox,
            LinearLayout.LayoutParams(
                dp(90),
                dp(40)
            )
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                -1,
                dp(68)
            )
        )
    }

    /*
     * ============================================================
     * PLAYER
     * ============================================================
     */

    private fun createPlayer() {

        playerContainer = FrameLayout(this)

        playerContainer.setBackgroundColor(Color.BLACK)

        playerView = PlayerView(this)

        playerView.useController = false

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

        fullscreenButton.textSize = 27f

        fullscreenButton.setTextColor(white)

        fullscreenButton.gravity =
            Gravity.CENTER

        fullscreenButton.setBackgroundColor(
            Color.argb(190, 0, 0, 0)
        )

        fullscreenButton.isFocusable = true

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        val fp =
            FrameLayout.LayoutParams(
                dp(58),
                dp(58)
            )

        fp.gravity =
            Gravity.BOTTOM or Gravity.END

        fp.setMargins(
            0,
            0,
            dp(18),
            dp(16)
        )

        playerContainer.addView(
            fullscreenButton,
            fp
        )

        root.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.55f
            )
        )
    }

    /*
     * ============================================================
     * NOW PLAYING
     * ============================================================
     */

    private fun createNowPlaying() {

        nowPlaying = TextView(this)

        nowPlaying.text =
            "●  اختر قناة لبدء المشاهدة"

        nowPlaying.textSize = 15f

        nowPlaying.setTextColor(white)

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
                dp(50)
            )
        )
    }

    /*
     * ============================================================
     * CHANNEL AREA
     * ============================================================
     */

    private fun createChannelArea() {

        bottomArea = LinearLayout(this)

        bottomArea.orientation =
            LinearLayout.HORIZONTAL

        bottomArea.setBackgroundColor(
            panelColor
        )

        /*
         * PACKAGES
         */

        val packageScroll =
            ScrollView(this)

        packageScroll.isFocusable = false

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
                dp(235),
                -1
            )
        )

        /*
         * CHANNELS
         */

        val channelScroll =
            ScrollView(this)

        channelScroll.isFocusable = false

        channelsLayout =
            LinearLayout(this)

        channelsLayout.orientation =
            LinearLayout.VERTICAL

        channelsLayout.setPadding(
            dp(14),
            dp(12),
            dp(14),
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
                0.45f
            )
        )
    }

    /*
     * ============================================================
     * PACKAGES
     * ============================================================
     */

    private fun loadPackages() {

        packagesLayout.removeAllViews()

        val groups =
            channels
                .map { it.group }
                .distinct()

        if (groups.isEmpty())
            return

        currentGroup = groups.first()

        groups.forEachIndexed { index, group ->

            val button =
                createPackageButton(
                    group,
                    index == 0
                )

            button.setOnClickListener {

                currentGroup = group

                updatePackageFocus(button)

                loadChannels(group)
            }

            button.setOnFocusChangeListener { view, focus ->

                if (focus) {

                    view.setBackgroundColor(
                        cardFocus
                    )
                } else {

                    if (currentGroup ==
                        group
                    ) {

                        view.setBackgroundColor(
                            accent
                        )

                    } else {

                        view.setBackgroundColor(
                            cardColor
                        )
                    }
                }
            }

            packagesLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    dp(55)
                ).apply {
                    setMargins(
                        0,
                        0,
                        0,
                        dp(7)
                    )
                }
            )
        }

        loadChannels(groups.first())
    }

    private fun updatePackageFocus(
        selected: View
    ) {

        for (
            i in 0 until
                    packagesLayout.childCount
        ) {

            packagesLayout
                .getChildAt(i)
                .setBackgroundColor(
                    cardColor
                )
        }

        selected.setBackgroundColor(
            accent
        )
    }

    /*
     * ============================================================
     * PACKAGE CARD
     * ============================================================
     */

    private fun createPackageButton(
        name: String,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        button.text = name

        button.textSize = 15f

        button.setTextColor(white)

        button.gravity =
            Gravity.CENTER_VERTICAL

        button.setPadding(
            dp(16),
            0,
            dp(10),
            0
        )

        button.setTypeface(
            null,
            Typeface.BOLD
        )

        button.setBackgroundColor(
            if (selected)
                accent
            else
                cardColor
        )

        button.isFocusable = true

        button.isFocusableInTouchMode = true

        return button
    }

    /*
     * ============================================================
     * CHANNELS
     * ============================================================
     */

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

                updateChannelFocus(
                    button
                )

                playChannel(channel)
            }

            button.setOnFocusChangeListener {
                    view,
                    focus ->

                if (focus) {

                    view.setBackgroundColor(
                        cardFocus
                    )

                } else {

                    view.setBackgroundColor(
                        cardColor
                    )
                }
            }

            channelsLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    dp(58)
                ).apply {

                    setMargins(
                        0,
                        0,
                        0,
                        dp(7)
                    )
                }
            )

            if (index == 0) {

                button.post {
                    button.requestFocus()
                }
            }
        }
    }

    private fun updateChannelFocus(
        selected: View
    ) {

        for (
            i in 0 until
                    channelsLayout.childCount
        ) {

            channelsLayout
                .getChildAt(i)
                .setBackgroundColor(
                    cardColor
                )
        }

        selected.setBackgroundColor(
            accent
        )
    }

    /*
     * ============================================================
     * CHANNEL CARD
     * ============================================================
     */

    private fun createChannelButton(
        channel: Channel,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        val number =
            channels.indexOf(channel) + 1

        button.text =
            String.format(
                "%03d     %s     ›",
                number,
                channel.name
            )

        button.textSize = 15f

        button.setTextColor(white)

        button.gravity =
            Gravity.CENTER_VERTICAL

        button.setPadding(
            dp(18),
            0,
            dp(18),
            0
        )

        button.setTypeface(
            null,
            Typeface.BOLD
        )

        button.setBackgroundColor(
            if (selected)
                accent
            else
                cardColor
        )

        button.isFocusable = true

        button.isFocusableInTouchMode = true

        return button
    }

    /*
     * ============================================================
     * PLAY CHANNEL
     * ============================================================
     */

    private fun playChannel(
        channel: Channel
    ) {

        nowPlaying.text =
            "●  الآن يعمل: ${channel.name}"

        try {

            if (exoPlayer == null) {

                exoPlayer =
                    ExoPlayer
                        .Builder(this)
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

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "خطأ في تشغيل القناة",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * ============================================================
     * FULLSCREEN
     * ============================================================
     */

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
            playerContainer
                .layoutParams as LinearLayout.LayoutParams

        params.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.height = 0

        params.weight = 1f

        playerContainer.layoutParams =
            params

        fullscreenButton.visibility =
            View.GONE

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
            playerContainer
                .layoutParams as LinearLayout.LayoutParams

        params.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.height = 0

        params.weight = 0.55f

        playerContainer.layoutParams =
            params

        fullscreenButton.visibility =
            View.VISIBLE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    /*
     * ============================================================
     * REMOTE CONTROL
     * ============================================================
     */

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_UP
        ) {

            when (
                event.keyCode
            ) {

                KeyEvent.KEYCODE_BACK -> {

                    if (fullscreen) {

                        exitFullscreen()

                        return true
                    }

                    if (
                        exoPlayer != null
                    ) {

                        exoPlayer?.stop()

                        nowPlaying.text =
                            "●  اختر قناة لبدء المشاهدة"

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

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

    /*
     * ============================================================
     * LIFECYCLE
     * ============================================================
     */

    override fun onDestroy() {

        exoPlayer?.release()

        exoPlayer = null

        super.onDestroy()
    }

    /*
     * ============================================================
     * DP
     * ============================================================
     */

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources
                    .displayMetrics
                    .density
            ).toInt()
    }
}
