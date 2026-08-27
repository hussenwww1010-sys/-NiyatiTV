package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
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

    // ============================================================
    // PLAYER
    // ============================================================

    private var exoPlayer: ExoPlayer? = null
    private var fullscreen = false
    private var currentGroup = ""

    // ============================================================
    // MAIN VIEWS
    // ============================================================

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout

    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var nowPlaying: TextView

    private lateinit var packageArea: LinearLayout
    private lateinit var packageScroll: HorizontalScrollView
    private lateinit var packagesLayout: LinearLayout

    private lateinit var channelArea: LinearLayout
    private lateinit var channelScroll: ScrollView
    private lateinit var channelsLayout: LinearLayout

    private lateinit var fullscreenButton: TextView

    // ============================================================
    // COLORS
    // ============================================================

    private val background = Color.rgb(5, 8, 15)
    private val topColor = Color.rgb(8, 12, 21)
    private val panelColor = Color.rgb(10, 15, 25)

    private val cardColor = Color.rgb(18, 25, 38)
    private val cardFocus = Color.rgb(42, 51, 70)

    private val accent = Color.rgb(232, 24, 62)
    private val accentDark = Color.rgb(155, 12, 39)

    private val white = Color.WHITE
    private val gray = Color.rgb(151, 162, 178)
    private val green = Color.rgb(42, 214, 126)

    // ============================================================
    // STREAM BASE
    // ============================================================

    private val base =
        "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun c(
        name: String,
        group: String,
        id: String
    ): Channel {

        return Channel(
            name = name,
            group = group,
            url = "$base$id.ts"
        )
    }

    // ============================================================
    // CHANNEL DATABASE
    // ============================================================

    private val channels = mutableListOf<Channel>().apply {

        // ========================================================
        // BEIN TOD
        // ========================================================

        add(c("beIN Tod 4K", "BEIN TOD", "460835"))

        for (i in 1..9) {
            add(
                c(
                    "beIN Sport Tod $i",
                    "BEIN TOD",
                    "${460835 + i}"
                )
            )
        }

        add(
            c(
                "beIN Sport Tod English 1",
                "BEIN TOD",
                "460845"
            )
        )

        add(
            c(
                "beIN Sport Tod English 2",
                "BEIN TOD",
                "460846"
            )
        )

        for (i in 1..9) {
            add(
                c(
                    "beIN Sport Tod Extra $i",
                    "BEIN TOD",
                    "${460846 + i}"
                )
            )
        }

        // ========================================================
        // BEIN SPORTS
        // ========================================================

        add(
            c(
                "beIN Sport Global 4K",
                "BEIN SPORTS",
                "22186"
            )
        )

        add(
            c(
                "beIN Sport News 4K",
                "BEIN SPORTS",
                "318230"
            )
        )

        val bein4k = listOf(
            318197,
            318198,
            318199,
            440580,
            318201,
            318202,
            318203,
            318204,
            318205
        )

        for (i in 1..9) {

            add(
                c(
                    "beIN Sport $i 4K",
                    "BEIN SPORTS",
                    bein4k[i - 1].toString()
                )
            )

            add(
                c(
                    "beIN$i H265",
                    "BEIN SPORTS",
                    "${391093 + i}"
                )
            )
        }

        add(
            c(
                "beIN Sport English 1 4K",
                "BEIN SPORTS",
                "319495"
            )
        )

        add(
            c(
                "beIN Sport English 2 4K",
                "BEIN SPORTS",
                "319496"
            )
        )

        add(
            c(
                "beIN Sport French 1 4K",
                "BEIN SPORTS",
                "319497"
            )
        )

        add(
            c(
                "beIN Sport French 2 4K",
                "BEIN SPORTS",
                "319498"
            )
        )

        add(
            c(
                "beIN Sport NBA 4K",
                "BEIN SPORTS",
                "319499"
            )
        )

        add(
            c(
                "beIN Global HD",
                "BEIN SPORTS",
                "442220"
            )
        )

        add(
            c(
                "beIN Sport News HD",
                "BEIN SPORTS",
                "443146"
            )
        )

        for (i in 1..9) {
            add(
                c(
                    "beIN Sport $i HD",
                    "BEIN SPORTS",
                    "${325792 + i}"
                )
            )
        }

        add(
            c(
                "beIN Sport 1 HD English",
                "BEIN SPORTS",
                "318217"
            )
        )

        add(
            c(
                "beIN Sport 2 HD English",
                "BEIN SPORTS",
                "318218"
            )
        )

        add(
            c(
                "beIN Sport 1 HD French",
                "BEIN SPORTS",
                "319437"
            )
        )

        add(
            c(
                "beIN Sport 2 HD French",
                "BEIN SPORTS",
                "319438"
            )
        )

        add(
            c(
                "beIN Sport NBA HD",
                "BEIN SPORTS",
                "318219"
            )
        )

        for (i in 1..9) {
            add(
                c(
                    "beIN Sport $i SD",
                    "BEIN SPORTS",
                    "${325802 + i}"
                )
            )
        }

        add(
            c(
                "beIN Sport English 1 SD",
                "BEIN SPORTS",
                "319425"
            )
        )

        add(
            c(
                "beIN Sport English 2 SD",
                "BEIN SPORTS",
                "319426"
            )
        )

        add(
            c(
                "beIN Sport French 1 SD",
                "BEIN SPORTS",
                "319427"
            )
        )

        add(
            c(
                "beIN Sport French 2 SD",
                "BEIN SPORTS",
                "319428"
            )
        )

        // ========================================================
        // BEIN XTRA
        // ========================================================

        val xtra4k = listOf(
            325790,
            319487,
            319488,
            440569,
            440570,
            440571,
            447243,
            447244,
            447245
        )

        val xtraHd = listOf(
            325802,
            319435,
            319436,
            440572,
            440573,
            440574,
            447246,
            447247,
            447248
        )

        val xtraSd = listOf(
            325812,
            319423,
            319424,
            440575,
            440576,
            440577,
            447249,
            447250,
            447251
        )

        for (i in 1..9) {

            add(
                c(
                    "beIN Sport XTRA $i 4K",
                    "BEIN XTRA",
                    xtra4k[i - 1].toString()
                )
            )

            add(
                c(
                    "beIN Sport XTRA $i HD",
                    "BEIN XTRA",
                    xtraHd[i - 1].toString()
                )
            )

            add(
                c(
                    "beIN Sport XTRA $i SD",
                    "BEIN XTRA",
                    xtraSd[i - 1].toString()
                )
            )
        }

        // ========================================================
        // AL RABIAA
        // ========================================================

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
            add(
                c(
                    it.first,
                    "AL RABIAA",
                    it.second
                )
            )
        }

        // ========================================================
        // ALKASS
        // ========================================================

        val alkassIds = listOf(
            96214,
            96215,
            278068,
            96216,
            96217,
            211523,
            379828,
            379829,
            393991,
            393992
        )

        for (i in 1..10) {
            add(
                c(
                    "Alkass $i HD",
                    "ALKASS",
                    alkassIds[i - 1].toString()
                )
            )
        }

        // ========================================================
        // SAUDI SPORTS
        // ========================================================

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
            add(
                c(
                    it.first,
                    "SAUDI SPORTS",
                    it.second
                )
            )
        }

        // ========================================================
        // GULF SPORTS
        // ========================================================

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
            add(
                c(
                    it.first,
                    "GULF SPORTS",
                    it.second
                )
            )
        }

        // ========================================================
        // AD SPORTS
        // ========================================================

        listOf(
            "AD SPORTS 1 HD" to "326053",
            "AD SPORTS 2 HD" to "326054",
            "AD Sport Asia 1 HD" to "244188",
            "AD Sport Asia 2 HD" to "244191"
        ).forEach {
            add(
                c(
                    it.first,
                    "AD SPORTS",
                    it.second
                )
            )
        }

        // ========================================================
        // ALWAN SPORT
        // ========================================================

        val alwan = listOf(
            418111,
            418112,
            418113,
            418114,
            418115,
            418116,
            418117,
            418118,
            418119,
            418120,
            418121,
            418122,
            418123,
            418124,
            418125,
            418126,
            418127,
            418128
        )

        for (i in 1..6) {

            val x = (i - 1) * 3

            add(
                c(
                    "Alwan Sport $i 4K",
                    "ALWAN SPORT",
                    alwan[x].toString()
                )
            )

            add(
                c(
                    "Alwan Sport $i HD",
                    "ALWAN SPORT",
                    alwan[x + 1].toString()
                )
            )

            add(
                c(
                    "Alwan Sport $i SD",
                    "ALWAN SPORT",
                    alwan[x + 2].toString()
                )
            )
        }

        listOf(
            "Alwan Sport 7 4K" to "433739",
            "Alwan Sport 8 4K" to "433740",
            "Alwan Sport 9 4K" to "433741",
            "Alwan Sport 10 4K" to "433742"
        ).forEach {
            add(
                c(
                    it.first,
                    "ALWAN SPORT",
                    it.second
                )
            )
        }

        // ========================================================
        // CRICKET
        // ========================================================

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
            add(
                c(
                    it.first,
                    "CRICKET",
                    it.second
                )
            )
        }

        // ========================================================
        // STAR SPORTS
        // ========================================================

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
            add(
                c(
                    it.first,
                    "STAR SPORTS",
                    it.second
                )
            )
        }

        // ========================================================
        // FAJER TV
        // ========================================================

        listOf(
            "Fajer TV 1" to "463532",
            "Fajer TV 2" to "463533",
            "Fajer TV 3" to "463534",
            "Fajer TV 4" to "463535",
            "Fajer TV 5" to "463536"
        ).forEach {
            add(
                c(
                    it.first,
                    "FAJER TV",
                    it.second
                )
            )
        }

        // ========================================================
        // KURDISTAN SPORTS
        // ========================================================

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
            add(
                c(
                    it.first,
                    "KURDISTAN SPORTS",
                    it.second
                )
            )
        }

        // ========================================================
        // SHAHID SPORT
        // ========================================================

        for (i in 1..5) {
            add(
                c(
                    "Shahid Sport $i 4K",
                    "SHAHID SPORT",
                    "${430910 + i}"
                )
            )
        }

        // ========================================================
        // SHASHA
        // ========================================================

        listOf(
            "Shasha 1 TV 4K" to "348400",
            "Shasha 2 TV 4K" to "244079",
            "Shasha 3 TV 4K" to "443029"
        ).forEach {
            add(
                c(
                    it.first,
                    "SHASHA",
                    it.second
                )
            )
        }
    }

    // ============================================================
    // CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.statusBarColor = background
        window.navigationBarColor = background

        buildInterface()
    }

    // ============================================================
    // BUILD INTERFACE
    // ============================================================

    private fun buildInterface() {

        root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setBackgroundColor(
            background
        )

        createTopBar()
        createPlayer()
        createNowPlaying()
        createPackageArea()
        createChannelArea()

        setContentView(root)

        loadPackages()
    }

    // ============================================================
    // TOP BAR
    // ============================================================

    private fun createTopBar() {

        topBar = LinearLayout(this)

        topBar.orientation =
            LinearLayout.HORIZONTAL

        topBar.gravity =
            Gravity.CENTER_VERTICAL

        topBar.setPadding(
            dp(24),
            dp(8),
            dp(24),
            dp(8)
        )

        topBar.setBackgroundColor(
            topColor
        )

        // LOGO

        val logoBox =
            FrameLayout(this)

        val logoBackground =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    accent,
                    accentDark
                )
            )

        logoBackground.cornerRadius =
            dp(14).toFloat()

        logoBox.background =
            logoBackground

        val logo =
            TextView(this)

        logo.text = "NT"
        logo.textSize = 20f
        logo.setTextColor(white)
        logo.gravity = Gravity.CENTER

        logo.setTypeface(
            null,
            Typeface.BOLD
        )

        logoBox.addView(
            logo,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        topBar.addView(
            logoBox,
            LinearLayout.LayoutParams(
                dp(54),
                dp(54)
            )
        )

        // BRAND

        val brand =
            LinearLayout(this)

        brand.orientation =
            LinearLayout.VERTICAL

        brand.gravity =
            Gravity.CENTER_VERTICAL

        brand.setPadding(
            dp(14),
            0,
            0,
            0
        )

        val title =
            TextView(this)

        title.text =
            "NIYATI TV"

        title.textSize =
            22f

        title.setTextColor(
            white
        )

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        val subtitle =
            TextView(this)

        subtitle.text =
            "PREMIUM LIVE TELEVISION"

        subtitle.textSize =
            9f

        subtitle.setTextColor(
            gray
        )

        subtitle.setTypeface(
            null,
            Typeface.BOLD
        )

        brand.addView(
            title,
            LinearLayout.LayoutParams(
                -2,
                dp(30)
            )
        )

        brand.addView(
            subtitle,
            LinearLayout.LayoutParams(
                -2,
                dp(18)
            )
        )

        topBar.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        // LIVE

        val liveBox =
            LinearLayout(this)

        liveBox.orientation =
            LinearLayout.HORIZONTAL

        liveBox.gravity =
            Gravity.CENTER

        val liveBackground =
            GradientDrawable()

        liveBackground.setColor(
            Color.rgb(
                18,
                27,
                38
            )
        )

        liveBackground.cornerRadius =
            dp(18).toFloat()

        liveBox.background =
            liveBackground

        liveBox.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        val dot =
            TextView(this)

        dot.text =
            "●"

        dot.textSize =
            11f

        dot.setTextColor(
            green
        )

        val liveText =
            TextView(this)

        liveText.text =
            " LIVE"

        liveText.textSize =
            12f

        liveText.setTextColor(
            white
        )

        liveText.setTypeface(
            null,
            Typeface.BOLD
        )

        liveBox.addView(
            dot,
            LinearLayout.LayoutParams(
                dp(18),
                -1
            )
        )

        liveBox.addView(
            liveText,
            LinearLayout.LayoutParams(
                -2,
                -1
            )
        )

        topBar.addView(
            liveBox,
            LinearLayout.LayoutParams(
                dp(92),
                dp(40)
            )
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                -1,
                dp(72)
            )
        )
    }

    // ============================================================
    // PLAYER
    // ============================================================

    private fun createPlayer() {

        playerContainer =
            FrameLayout(this)

        playerContainer.setBackgroundColor(
            Color.BLACK
        )

        playerView =
            PlayerView(this)

        playerView.useController =
            false

        playerView.setBackgroundColor(
            Color.BLACK
        )

        playerView.isFocusable =
            true

        playerView.isFocusableInTouchMode =
            true

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        // PLAYER BRAND

        val playerBrand =
            LinearLayout(this)

        playerBrand.orientation =
            LinearLayout.HORIZONTAL

        playerBrand.gravity =
            Gravity.CENTER_VERTICAL

        val brandBg =
            GradientDrawable()

        brandBg.setColor(
            Color.argb(
                180,
                5,
                8,
                15
            )
        )

        brandBg.cornerRadius =
            dp(12).toFloat()

        playerBrand.background =
            brandBg

        playerBrand.setPadding(
            dp(10),
            0,
            dp(14),
            0
        )

        val smallLogo =
            TextView(this)

        smallLogo.text =
            "NT"

        smallLogo.textSize =
            12f

        smallLogo.setTextColor(
            white
        )

        smallLogo.gravity =
            Gravity.CENTER

        smallLogo.setTypeface(
            null,
            Typeface.BOLD
        )

        val smallLogoBg =
            GradientDrawable()

        smallLogoBg.setColor(
            accent
        )

        smallLogoBg.cornerRadius =
            dp(7).toFloat()

        smallLogo.background =
            smallLogoBg

        playerBrand.addView(
            smallLogo,
            LinearLayout.LayoutParams(
                dp(30),
                dp(30)
            )
        )

        val playerBrandText =
            TextView(this)

        playerBrandText.text =
            "  NIYATI TV"

        playerBrandText.textSize =
            12f

        playerBrandText.setTextColor(
            white
        )

        playerBrandText.setTypeface(
            null,
            Typeface.BOLD
        )

        playerBrand.addView(
            playerBrandText,
            LinearLayout.LayoutParams(
                -2,
                dp(30)
            )
        )

        val brandParams =
            FrameLayout.LayoutParams(
                dp(145),
                dp(40)
            )

        brandParams.gravity =
            Gravity.TOP or Gravity.START

        brandParams.setMargins(
            dp(18),
            dp(16),
            0,
            0
        )

        playerContainer.addView(
            playerBrand,
            brandParams
        )

        // FULLSCREEN

        fullscreenButton =
            TextView(this)

        fullscreenButton.text =
            "⛶"

        fullscreenButton.textSize =
            27f

        fullscreenButton.setTextColor(
            white
        )

        fullscreenButton.gravity =
            Gravity.CENTER

        fullscreenButton.setTypeface(
            null,
            Typeface.BOLD
        )

        val fullscreenBg =
            GradientDrawable()

        fullscreenBg.setColor(
            Color.argb(
                190,
                0,
                0,
                0
            )
        )

        fullscreenBg.cornerRadius =
            dp(12).toFloat()

        fullscreenButton.background =
            fullscreenBg

        fullscreenButton.isFocusable =
            true

        fullscreenButton.isFocusableInTouchMode =
            true

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        fullscreenButton.setOnFocusChangeListener {
                view,
                hasFocus ->

            view.background =
                if (hasFocus) {

                    createCardBackground(
                        accent,
                        true
                    )

                } else {

                    fullscreenBg
                }
        }

        val fullscreenParams =
            FrameLayout.LayoutParams(
                dp(58),
                dp(58)
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

        // LIVE LABEL

        val playerInfo =
            TextView(this)

        playerInfo.text =
            "LIVE"

        playerInfo.textSize =
            11f

        playerInfo.setTextColor(
            white
        )

        playerInfo.gravity =
            Gravity.CENTER

        val infoBg =
            GradientDrawable()

        infoBg.setColor(
            accent
        )

        infoBg.cornerRadius =
            dp(8).toFloat()

        playerInfo.background =
            infoBg

        val infoParams =
            FrameLayout.LayoutParams(
                dp(54),
                dp(28)
            )

        infoParams.gravity =
            Gravity.TOP or Gravity.END

        infoParams.setMargins(
            0,
            dp(18),
            dp(18),
            0
        )

        playerContainer.addView(
            playerInfo,
            infoParams
        )

        root.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                -1,
                dp(330)
            )
        )
    }

    // ============================================================
    // NOW PLAYING
    // ============================================================

    private fun createNowPlaying() {

        nowPlaying =
            TextView(this)

        nowPlaying.text =
            "●   اختر قناة لبدء المشاهدة"

        nowPlaying.textSize =
            14f

        nowPlaying.setTextColor(
            white
        )

        nowPlaying.gravity =
            Gravity.CENTER_VERTICAL

        nowPlaying.setPadding(
            dp(24),
            0,
            dp(24),
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
    }

    // ============================================================
    // PACKAGES
    // ============================================================

    private fun createPackageArea() {

        packageArea =
            LinearLayout(this)

        packageArea.orientation =
            LinearLayout.VERTICAL

        packageArea.setBackgroundColor(
            panelColor
        )

        val title =
            TextView(this)

        title.text =
            "  الباقات"

        title.textSize =
            15f

        title.setTextColor(
            white
        )

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        title.gravity =
            Gravity.CENTER_VERTICAL

        packageArea.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                dp(36)
            )
        )

        packageScroll =
            HorizontalScrollView(this)

        packageScroll.isHorizontalScrollBarEnabled =
            false

        packageScroll.isFocusable =
            false

        packagesLayout =
            LinearLayout(this)

        packagesLayout.orientation =
            LinearLayout.HORIZONTAL

        packagesLayout.setPadding(
            dp(14),
            dp(2),
            dp(14),
            dp(8)
        )

        packageScroll.addView(
            packagesLayout,
            HorizontalScrollView.LayoutParams(
                -2,
                -1
            )
        )

        packageArea.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                -1,
                dp(72)
            )
        )

        root.addView(
            packageArea,
            LinearLayout.LayoutParams(
                -1,
                dp(108)
            )
        )
    }

    // ============================================================
    // CHANNEL AREA
    // ============================================================

    private fun createChannelArea() {

        channelArea =
            LinearLayout(this)

        channelArea.orientation =
            LinearLayout.VERTICAL

        channelArea.setBackgroundColor(
            background
        )

        val heading =
            TextView(this)

        heading.text =
            "  القنوات"

        heading.textSize =
            15f

        heading.setTextColor(
            white
        )

        heading.setTypeface(
            null,
            Typeface.BOLD
        )

        heading.gravity =
            Gravity.CENTER_VERTICAL

        channelArea.addView(
            heading,
            LinearLayout.LayoutParams(
                -1,
                dp(38)
            )
        )

        channelScroll =
            ScrollView(this)

        channelScroll.isVerticalScrollBarEnabled =
            false

        channelScroll.isFocusable =
            false

        channelsLayout =
            LinearLayout(this)

        channelsLayout.orientation =
            LinearLayout.VERTICAL

        channelsLayout.setPadding(
            dp(14),
            dp(4),
            dp(14),
            dp(30)
        )

        channelScroll.addView(
            channelsLayout,
            ScrollView.LayoutParams(
                -1,
                -2
            )
        )

        channelArea.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        root.addView(
            channelArea,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )
    }

    // ============================================================
    // LOAD PACKAGES
    // ============================================================

    private fun loadPackages() {

        packagesLayout.removeAllViews()

        val groups =
            channels
                .map {
                    it.group
                }
                .distinct()

        if (groups.isEmpty()) {
            return
        }

        currentGroup =
            groups.first()

        groups.forEachIndexed {
                index,
                group ->

            val card =
                createPackageCard(
                    group,
                    index == 0
                )

            card.setOnClickListener {

                currentGroup =
                    group

                updatePackageSelection(
                    card
                )

                loadChannels(
                    group
                )
            }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                if (hasFocus) {

                    view.background =
                        createCardBackground(
                            cardFocus,
                            true
                        )

                } else {

                    view.background =
                        if (
                            currentGroup ==
                            group
                        ) {

                            createCardBackground(
                                accent,
                                true
                            )

                        } else {

                            createCardBackground(
                                cardColor,
                                false
                            )
                        }
                }
            }

            packagesLayout.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(178),
                    dp(58)
                ).apply {

                    setMargins(
                        dp(4),
                        0,
                        dp(6),
                        0
                    )
                }
            )
        }

        loadChannels(
            groups.first()
        )
    }

    // ============================================================
    // PACKAGE CARD
    // ============================================================

    private fun createPackageCard(
        name: String,
        selected: Boolean
    ): TextView {

        val card =
            TextView(this)

        card.text =
            packageDisplayName(name)

        card.textSize =
            13f

        card.setTextColor(
            white
        )

        card.gravity =
            Gravity.CENTER

        card.setTypeface(
            null,
            Typeface.BOLD
        )

        card.setPadding(
            dp(10),
            0,
            dp(10),
            0
        )

        card.background =
            createCardBackground(
                if (selected)
                    accent
                else
                    cardColor,
                selected
            )

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        return card
    }

    private fun packageDisplayName(
        name: String
    ): String {

        return when (name) {

            "BEIN TOD" ->
                "beIN TOD"

            "BEIN SPORTS" ->
                "beIN SPORTS"

            "BEIN XTRA" ->
                "beIN XTRA"

            "AL RABIAA" ->
                "AL RABIAA"

            "ALKASS" ->
                "ALKASS"

            "SAUDI SPORTS" ->
                "SAUDI SPORTS"

            "GULF SPORTS" ->
                "GULF SPORTS"

            "AD SPORTS" ->
                "AD SPORTS"

            "ALWAN SPORT" ->
                "ALWAN SPORT"

            "CRICKET" ->
                "CRICKET"

            "STAR SPORTS" ->
                "STAR SPORTS"

            "FAJER TV" ->
                "FAJER TV"

            "KURDISTAN SPORTS" ->
                "KURDISTAN SPORTS"

            "SHAHID SPORT" ->
                "SHAHID SPORT"

            "SHASHA" ->
                "SHASHA"

            else ->
                name
        }
    }

    // ============================================================
    // UPDATE PACKAGE
    // ============================================================

    private fun updatePackageSelection(
        selected: View
    ) {

        for (
            i in 0 until packagesLayout.childCount
        ) {

            val child =
                packagesLayout.getChildAt(i)

            child.background =
                createCardBackground(
                    cardColor,
                    false
                )
        }

        selected.background =
            createCardBackground(
                accent,
                true
            )
    }

    // ============================================================
    // LOAD CHANNELS
    // ============================================================

    private fun loadChannels(
        group: String
    ) {

        channelsLayout.removeAllViews()

        val filtered =
            channels.filter {
                it.group == group
            }

        if (filtered.isEmpty()) {

            val empty =
                TextView(this)

            empty.text =
                "لا توجد قنوات في هذه الباقة"

            empty.textSize =
                15f

            empty.setTextColor(
                gray
            )

            empty.gravity =
                Gravity.CENTER

            channelsLayout.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    dp(100)
                )
            )

            return
        }

        var row:
                LinearLayout? = null

        filtered.forEachIndexed {
                index,
                channel ->

            if (index % 4 == 0) {

                row =
                    LinearLayout(this)

                row!!.orientation =
                    LinearLayout.HORIZONTAL

                row!!.gravity =
                    Gravity.CENTER_VERTICAL

                channelsLayout.addView(
                    row,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(78)
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

            val button =
                createChannelCard(
                    channel
                )

            button.setOnClickListener {

                updateChannelSelection(
                    button
                )

                playChannel(
                    channel
                )
            }

            button.setOnFocusChangeListener {
                    view,
                    focus ->

                if (focus) {

                    view.background =
                        createChannelBackground(
                            cardFocus,
                            true
                        )

                } else {

                    view.background =
                        createChannelBackground(
                            cardColor,
                            false
                        )
                }
            }

            row!!.addView(
                button,
                LinearLayout.LayoutParams(
                    dp(255),
                    dp(72)
                ).apply {

                    setMargins(
                        dp(3),
                        0,
                        dp(7),
                        0
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

    // ============================================================
    // CHANNEL CARD
    // ============================================================

    private fun createChannelCard(
        channel: Channel
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.HORIZONTAL

        card.gravity =
            Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(12),
            0,
            dp(12),
            0
        )

        card.background =
            createChannelBackground(
                cardColor,
                false
            )

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        // CHANNEL ICON

        val icon =
            TextView(this)

        icon.text =
            "NT"

        icon.textSize =
            11f

        icon.setTextColor(
            white
        )

        icon.gravity =
            Gravity.CENTER

        icon.setTypeface(
            null,
            Typeface.BOLD
        )

        val iconBg =
            GradientDrawable()

        iconBg.setColor(
            accentDark
        )

        iconBg.cornerRadius =
            dp(8).toFloat()

        icon.background =
            iconBg

        card.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(38),
                dp(38)
            )
        )

        // TEXT

        val textArea =
            LinearLayout(this)

        textArea.orientation =
            LinearLayout.VERTICAL

        textArea.gravity =
            Gravity.CENTER_VERTICAL

        textArea.setPadding(
            dp(10),
            0,
            0,
            0
        )

        val number =
            TextView(this)

        val position =
            channels.indexOf(
                channel
            ) + 1

        number.text =
            String.format(
                "%03d",
                position
            )

        number.textSize =
            9f

        number.setTextColor(
            gray
        )

        number.setTypeface(
            null,
            Typeface.BOLD
        )

        val name =
            TextView(this)

        name.text =
            channel.name

        name.textSize =
            12f

        name.setTextColor(
            white
        )

        name.setTypeface(
            null,
            Typeface.BOLD
        )

        name.maxLines =
            1

        textArea.addView(
            number,
            LinearLayout.LayoutParams(
                -1,
                dp(16)
            )
        )

        textArea.addView(
            name,
            LinearLayout.LayoutParams(
                -1,
                dp(25)
            )
        )

        card.addView(
            textArea,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        // LIVE DOT

        val live =
            TextView(this)

        live.text =
            "●"

        live.textSize =
            9f

        live.setTextColor(
            green
        )

        live.gravity =
            Gravity.CENTER

        card.addView(
            live,
            LinearLayout.LayoutParams(
                dp(20),
                dp(30)
            )
        )

        return card
    }

    // ============================================================
    // UPDATE CHANNEL
    // ============================================================

    private fun updateChannelSelection(
        selected: View
    ) {

        for (
            i in 0 until channelsLayout.childCount
        ) {

            val row =
                channelsLayout.getChildAt(
                    i
                )

            if (
                row is LinearLayout
            ) {

                for (
                    j in 0 until row.childCount
                ) {

                    row.getChildAt(
                        j
                    ).background =
                        createChannelBackground(
                            cardColor,
                            false
                        )
                }
            }
        }

        selected.background =
            createChannelBackground(
                accent,
                true
            )
    }

    // ============================================================
    // BACKGROUNDS
    // ============================================================

    private fun createCardBackground(
        color: Int,
        selected: Boolean
    ): GradientDrawable {

        val drawable =
            GradientDrawable()

        drawable.setColor(
            color
        )

        drawable.cornerRadius =
            dp(12).toFloat()

        if (selected) {

            drawable.setStroke(
                dp(2),
                accent
            )
        }

        return drawable
    }

    private fun createChannelBackground(
        color: Int,
        selected: Boolean
    ): GradientDrawable {

        val drawable =
            GradientDrawable()

        drawable.setColor(
            color
        )

        drawable.cornerRadius =
            dp(12).toFloat()

        if (selected) {

            drawable.setStroke(
                dp(2),
                accent
            )
        }

        return drawable
    }

    // ============================================================
    // PLAY CHANNEL
    // ============================================================

    private fun playChannel(
        channel: Channel
    ) {

        nowPlaying.text =
            "●   الآن يعمل: ${channel.name}"

        try {

            if (exoPlayer == null) {

                exoPlayer =
                    ExoPlayer
                        .Builder(this)
                        .build()

                playerView.player =
                    exoPlayer

                exoPlayer?.addListener(
                    object :
                        Player.Listener {

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
                    Uri.parse(
                        channel.url
                    )
                )

            exoPlayer?.setMediaItem(
                mediaItem
            )

            exoPlayer?.prepare()

            exoPlayer?.playWhenReady =
                true

            playerView.requestFocus()

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

    // ============================================================
    // FULLSCREEN
    // ============================================================

    private fun toggleFullscreen() {

        if (fullscreen) {

            exitFullscreen()

        } else {

            enterFullscreen()
        }
    }

    private fun enterFullscreen() {

        if (fullscreen) {
            return
        }

        fullscreen =
            true

        topBar.visibility =
            View.GONE

        nowPlaying.visibility =
            View.GONE

        packageArea.visibility =
            View.GONE

        channelArea.visibility =
            View.GONE

        val playerParams =
            playerContainer.layoutParams
                as LinearLayout.LayoutParams

        playerParams.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        playerParams.height =
            LinearLayout.LayoutParams.MATCH_PARENT

        playerParams.weight =
            1f

        playerContainer.layoutParams =
            playerParams

        fullscreenButton.visibility =
            View.GONE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun exitFullscreen() {

        if (!fullscreen) {
            return
        }

        fullscreen =
            false

        topBar.visibility =
            View.VISIBLE

        nowPlaying.visibility =
            View.VISIBLE

        packageArea.visibility =
            View.VISIBLE

        channelArea.visibility =
            View.VISIBLE

        val playerParams =
            playerContainer.layoutParams
                as LinearLayout.LayoutParams

        playerParams.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        playerParams.height =
            dp(330)

        playerParams.weight =
            0f

        playerContainer.layoutParams =
            playerParams

        fullscreenButton.visibility =
            View.VISIBLE

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // ============================================================
    // REMOTE CONTROL
    // ============================================================

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

                // ------------------------------------------------
                // BACK
                // ------------------------------------------------

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
                            "●   اختر قناة لبدء المشاهدة"

                        playerView.requestFocus()

                        return true
                    }

                    if (
                        playerView.hasFocus()
                    ) {

                        val firstPackage =
                            packagesLayout
                                .getChildAt(0)

                        if (
                            firstPackage != null
                        ) {

                            firstPackage.requestFocus()

                            return true
                        }
                    }
                }

                // ------------------------------------------------
                // OK
                // ------------------------------------------------

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (
                        playerView.hasFocus()
                    ) {

                        toggleFullscreen()

                        return true
                    }
                }

                // ------------------------------------------------
                // UP
                // ------------------------------------------------

                KeyEvent.KEYCODE_DPAD_UP -> {

                    if (
                        playerView.hasFocus()
                    ) {

                        return super.dispatchKeyEvent(
                            event
                        )
                    }
                }

                // ------------------------------------------------
                // DOWN
                // ------------------------------------------------

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    if (
                        fullscreen
                    ) {

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onPause() {

        super.onPause()

        if (!fullscreen) {

            exoPlayer?.pause()
        }
    }

    override fun onDestroy() {

        exoPlayer?.release()

        exoPlayer =
            null

        super.onDestroy()
    }

    // ============================================================
    // DP
    // ============================================================

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
