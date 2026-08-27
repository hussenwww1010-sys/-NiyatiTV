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

    private var exoPlayer: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView

    private lateinit var packageScroll: HorizontalScrollView
    private lateinit var packagesLayout: LinearLayout

    private lateinit var channelScroll: ScrollView
    private lateinit var channelsLayout: LinearLayout

    private lateinit var nowPlaying: TextView
    private lateinit var fullscreenButton: TextView
    private lateinit var statusText: TextView

    private var fullscreen = false
    private var currentGroup = ""
    private var currentChannel: Channel? = null

    private val backgroundColor = Color.rgb(5, 8, 14)
    private val surfaceColor = Color.rgb(9, 14, 22)
    private val cardColor = Color.rgb(18, 25, 37)
    private val cardFocusColor = Color.rgb(218, 24, 55)
    private val accentColor = Color.rgb(238, 31, 63)
    private val textColor = Color.WHITE
    private val secondaryTextColor = Color.rgb(155, 165, 180)

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
            base + id + ".ts"
        )
    }

    // ============================================================
    // CHANNEL DATABASE
    // ============================================================

    private val channels = listOf(

        // BEIN TOD
        c("beIN Tod 4K", "BEIN TOD", "460835"),
        c("beIN Sport Tod 1", "BEIN TOD", "460836"),
        c("beIN Sport Tod 2", "BEIN TOD", "460837"),
        c("beIN Sport Tod 3", "BEIN TOD", "460838"),
        c("beIN Sport Tod 4", "BEIN TOD", "460839"),
        c("beIN Sport Tod 5", "BEIN TOD", "460840"),
        c("beIN Sport Tod 6", "BEIN TOD", "460841"),
        c("beIN Sport Tod 7", "BEIN TOD", "460842"),
        c("beIN Sport Tod 8", "BEIN TOD", "460843"),
        c("beIN Sport Tod 9", "BEIN TOD", "460844"),
        c("beIN Sport Tod English 1", "BEIN TOD", "460845"),
        c("beIN Sport Tod English 2", "BEIN TOD", "460846"),
        c("beIN Sport Tod Extra 1", "BEIN TOD", "460847"),
        c("beIN Sport Tod Extra 2", "BEIN TOD", "460848"),
        c("beIN Sport Tod Extra 3", "BEIN TOD", "460849"),
        c("beIN Sport Tod Extra 4", "BEIN TOD", "460850"),
        c("beIN Sport Tod Extra 5", "BEIN TOD", "460851"),
        c("beIN Sport Tod Extra 6", "BEIN TOD", "460852"),
        c("beIN Sport Tod Extra 7", "BEIN TOD", "460853"),
        c("beIN Sport Tod Extra 8", "BEIN TOD", "460854"),
        c("beIN Sport Tod Extra 9", "BEIN TOD", "460855"),

        // BEIN SPORTS
        c("beIN Sport Global 4K", "BEIN SPORTS", "22186"),
        c("beIN Sport News 4K", "BEIN SPORTS", "318230"),
        c("beIN Sport 1 4K", "BEIN SPORTS", "318197"),
        c("beIN1 H265", "BEIN SPORTS", "391094"),
        c("beIN Sport 2 4K", "BEIN SPORTS", "318198"),
        c("beIN2 H265", "BEIN SPORTS", "391095"),
        c("beIN Sport 3 4K", "BEIN SPORTS", "318199"),
        c("beIN3 H265", "BEIN SPORTS", "391096"),
        c("beIN Sport 4 4K", "BEIN SPORTS", "440580"),
        c("beIN4 H265", "BEIN SPORTS", "391097"),
        c("beIN Sport 5 4K", "BEIN SPORTS", "318201"),
        c("beIN5 H265", "BEIN SPORTS", "391098"),
        c("beIN Sport 6 4K", "BEIN SPORTS", "318202"),
        c("beIN6 H265", "BEIN SPORTS", "391099"),
        c("beIN Sport 7 4K", "BEIN SPORTS", "318203"),
        c("beIN7 H265", "BEIN SPORTS", "391100"),
        c("beIN Sport 8 4K", "BEIN SPORTS", "318204"),
        c("beIN8 H265", "BEIN SPORTS", "391101"),
        c("beIN Sport 9 4K", "BEIN SPORTS", "318205"),
        c("beIN9 H265", "BEIN SPORTS", "391102"),
        c("beIN Sport English 1 4K", "BEIN SPORTS", "319495"),
        c("beIN Sport English 2 4K", "BEIN SPORTS", "319496"),
        c("beIN Sport French 1 4K", "BEIN SPORTS", "319497"),
        c("beIN Sport French 2 4K", "BEIN SPORTS", "319498"),
        c("beIN Sport NBA 4K", "BEIN SPORTS", "319499"),
        c("beIN Global HD", "BEIN SPORTS", "442220"),
        c("beIN Sport News HD", "BEIN SPORTS", "443146"),
        c("beIN Sport 1 HD", "BEIN SPORTS", "325793"),
        c("beIN Sport 2 HD", "BEIN SPORTS", "325794"),
        c("beIN Sport 3 HD", "BEIN SPORTS", "325795"),
        c("beIN Sport 4 HD", "BEIN SPORTS", "325796"),
        c("beIN Sport 5 HD", "BEIN SPORTS", "325797"),
        c("beIN Sport 6 HD", "BEIN SPORTS", "325798"),
        c("beIN Sport 7 HD", "BEIN SPORTS", "325799"),
        c("beIN Sport 8 HD", "BEIN SPORTS", "325800"),
        c("beIN Sport 9 HD", "BEIN SPORTS", "325801"),
        c("beIN Sport 1 HD English", "BEIN SPORTS", "318217"),
        c("beIN Sport 2 HD English", "BEIN SPORTS", "318218"),
        c("beIN Sport 1 HD Frensh", "BEIN SPORTS", "319437"),
        c("beIN Sport 2 HD Frensh", "BEIN SPORTS", "319438"),
        c("beIN Sport NBA HD", "BEIN SPORTS", "318219"),
        c("beIN Sport 1 SD", "BEIN SPORTS", "325803"),
        c("beIN Sport 2 SD", "BEIN SPORTS", "325804"),
        c("beIN Sport 3 SD", "BEIN SPORTS", "325805"),
        c("beIN Sport 4 SD", "BEIN SPORTS", "325806"),
        c("beIN Sport 5 SD", "BEIN SPORTS", "325807"),
        c("beIN Sport 6 SD", "BEIN SPORTS", "325808"),
        c("beIN Sport 7 SD", "BEIN SPORTS", "325809"),
        c("beIN Sport 8 SD", "BEIN SPORTS", "325810"),
        c("beIN Sport 9 SD", "BEIN SPORTS", "325811"),
        c("beIN Sport English 1 SD", "BEIN SPORTS", "319425"),
        c("beIN Sport English 2 SD", "BEIN SPORTS", "319426"),
        c("beIN Sport French 1 SD", "BEIN SPORTS", "319427"),
        c("beIN Sport French 2 SD", "BEIN SPORTS", "319428"),

        // BEIN XTRA
        c("beIN Sport XTRA 1 4K", "BEIN XTRA", "325790"),
        c("beIN Sport XTRA 2 4K", "BEIN XTRA", "319487"),
        c("beIN Sport XTRA 3 4K", "BEIN XTRA", "319488"),
        c("beIN Sport XTRA 4 4K", "BEIN XTRA", "440569"),
        c("beIN Sport XTRA 5 4K", "BEIN XTRA", "440570"),
        c("beIN Sport XTRA 6 4K", "BEIN XTRA", "440571"),
        c("beIN Sport XTRA 7 4K", "BEIN XTRA", "447243"),
        c("beIN Sport XTRA 8 4K", "BEIN XTRA", "447244"),
        c("beIN Sport XTRA 9 4K", "BEIN XTRA", "447245"),
        c("beIN Sport Xtra 1 HD", "BEIN XTRA", "325802"),
        c("beIN Sport Xtra 2 HD", "BEIN XTRA", "319435"),
        c("beIN Sport Xtra 3 HD", "BEIN XTRA", "319436"),
        c("beIN Sport Xtra 4 HD", "BEIN XTRA", "440572"),
        c("beIN Sport Xtra 5 HD", "BEIN XTRA", "440573"),
        c("beIN Sport Xtra 6 HD", "BEIN XTRA", "440574"),
        c("beIN Sport Xtra 7 HD", "BEIN XTRA", "447246"),
        c("beIN Sport Xtra 8 HD", "BEIN XTRA", "447247"),
        c("beIN Sport Xtra 9 HD", "BEIN XTRA", "447248"),
        c("beIN Sport Xtra 1 SD", "BEIN XTRA", "325812"),
        c("beIN Sport Xtra 2 SD", "BEIN XTRA", "319423"),
        c("beIN Sport Xtra 3 SD", "BEIN XTRA", "319424"),
        c("beIN Sport Xtra 4 SD", "BEIN XTRA", "440575"),
        c("beIN Sport Xtra 5 SD", "BEIN XTRA", "440576"),
        c("beIN Sport Xtra 6 SD", "BEIN XTRA", "440577"),
        c("beIN Sport Xtra 7 SD", "BEIN XTRA", "447249"),
        c("beIN Sport Xtra 8 SD", "BEIN XTRA", "447250"),
        c("beIN Sport Xtra 9 SD", "BEIN XTRA", "447251"),

        // AL RABIAA
        c("AL RABIAA SPORT 1", "AL RABIAA", "371931"),
        c("AL RABIAA SPORT 1+", "AL RABIAA", "371933"),
        c("AL RABIAA SPORT 2", "AL RABIAA", "371932"),
        c("Rabiaa Sport +2", "AL RABIAA", "434565"),
        c("AL RABIAA TV 4K", "AL RABIAA", "371939"),
        c("AL RABIAA MOVIES", "AL RABIAA", "371934"),
        c("Rabiaa Variety", "AL RABIAA", "434566"),
        c("Njoom Al Rabiaa", "AL RABIAA", "434567"),
        c("AL RABIAA SERIES", "AL RABIAA", "371935"),
        c("AL RABIAA GEO", "AL RABIAA", "371936"),
        c("AL RABIAA QURAN", "AL RABIAA", "371937"),
        c("AL RABIAA MUSICA", "AL RABIAA", "371938"),

        // ALKASS
        c("Alkass 1 HD", "ALKASS", "96214"),
        c("Alkass 2 HD", "ALKASS", "96215"),
        c("Alkass 3 HD", "ALKASS", "278068"),
        c("Alkass 4 HD", "ALKASS", "96216"),
        c("Alkass 5 HD", "ALKASS", "96217"),
        c("Alkass 6 HD", "ALKASS", "211523"),
        c("Alkass 7 HD", "ALKASS", "379828"),
        c("Alkass 8 HD", "ALKASS", "379829"),
        c("Alkass 9 HD", "ALKASS", "393991"),
        c("Alkass 10 HD", "ALKASS", "393992"),

        // SAUDI SPORTS
        c("KSA Sport 1 4K", "SAUDI SPORTS", "97805"),
        c("KSA Sport 2 4K", "SAUDI SPORTS", "97806"),
        c("KSA Sport 3 4K", "SAUDI SPORTS", "97807"),
        c("SAUDUA NOW", "SAUDI SPORTS", "97808"),
        c("SAUDI 24 SPORT HD", "SAUDI SPORTS", "100470"),
        c("STC SPORT 1 HD", "SAUDI SPORTS", "421391"),
        c("STC SPORT 2 HD", "SAUDI SPORTS", "421392"),
        c("STC SPORT 3 HD", "SAUDI SPORTS", "420903"),
        c("STC SPORT 4 HD", "SAUDI SPORTS", "433178"),

        // GULF SPORTS
        c("Dubai Sport 1 HD", "GULF SPORTS", "97813"),
        c("Dubai Sport 2 HD", "GULF SPORTS", "97814"),
        c("Dubai Racing 1 HD", "GULF SPORTS", "97816"),
        c("On Sport HD 1", "GULF SPORTS", "97820"),
        c("ON SPORTS MAX 4K", "GULF SPORTS", "97821"),
        c("AR: ON TIME SPORT FM", "GULF SPORTS", "399432"),
        c("ON SPORTS PLUS HD", "GULF SPORTS", "97825"),
        c("Oman Sport HD", "GULF SPORTS", "97877"),
        c("KUWAIT SPORT 4K", "GULF SPORTS", "97826"),
        c("BAHRAIN SPORT 1 HD", "GULF SPORTS", "66383"),
        c("YAS SPORT HD", "GULF SPORTS", "328659"),
        c("ALRABIAA SPORT 4K", "GULF SPORTS", "328660"),
        c("LIBYA SPORT 2 4K", "GULF SPORTS", "328661"),
        c("BAHRAIN SPORT 2 HD", "GULF SPORTS", "328662"),
        c("KUWAIT SPORT PLUS 4K", "GULF SPORTS", "328663"),
        c("PALASTINE SPORT 4K", "GULF SPORTS", "328664"),
        c("Iraqia Sport", "GULF SPORTS", "107038"),
        c("ufm radio", "GULF SPORTS", "267050"),
        c("Sharjah Sport HD", "GULF SPORTS", "141797"),
        c("Libya Sport 1 TV", "GULF SPORTS", "97818"),
        c("Jordan Sport TV", "GULF SPORTS", "109699"),
        c("Zamalik", "GULF SPORTS", "97822"),
        c("Nile Sport", "GULF SPORTS", "97824"),
        c("Al Ahly TV", "GULF SPORTS", "97823"),
        c("PalestineSport", "GULF SPORTS", "417306"),

        // AD SPORTS
        c("AD SPORTS 1 HD", "AD SPORTS", "326053"),
        c("AD SPORTS 2 HD", "AD SPORTS", "326054"),
        c("AD Sport Asia 1 HD", "AD SPORTS", "244188"),
        c("AD Sport Asia 2 HD", "AD SPORTS", "244191"),

        // ALWAN SPORT
        c("Alwan Sport 1 4K", "ALWAN SPORT", "418111"),
        c("Alwan Sport 1 HD", "ALWAN SPORT", "418112"),
        c("Alwan Sport 1 SD", "ALWAN SPORT", "418113"),
        c("Alwan Sport 2 4K", "ALWAN SPORT", "418114"),
        c("Alwan Sport 2 HD", "ALWAN SPORT", "418115"),
        c("Alwan Sport 2 SD", "ALWAN SPORT", "418116"),
        c("Alwan Sport 3 4K", "ALWAN SPORT", "418117"),
        c("Alwan Sport 3 HD", "ALWAN SPORT", "418118"),
        c("Alwan Sport 3 SD", "ALWAN SPORT", "418119"),
        c("Alwan Sport 4 4K", "ALWAN SPORT", "418120"),
        c("Alwan Sport 4 HD", "ALWAN SPORT", "418121"),
        c("Alwan Sport 4 SD", "ALWAN SPORT", "418122"),
        c("Alwan Sport 5 4K", "ALWAN SPORT", "418123"),
        c("Alwan Sport 5 HD", "ALWAN SPORT", "418124"),
        c("Alwan Sport 5 SD", "ALWAN SPORT", "418125"),
        c("Alwan Sport 6 4K", "ALWAN SPORT", "418126"),
        c("Alwan Sport 6 HD", "ALWAN SPORT", "418127"),
        c("Alwan Sport 6 SD", "ALWAN SPORT", "418128"),
        c("Alwan Sport 7 4K", "ALWAN SPORT", "433739"),
        c("Alwan Sport 8 4K", "ALWAN SPORT", "433740"),
        c("Alwan Sport 9 4K", "ALWAN SPORT", "433741"),
        c("Alwan Sport 10 4K", "ALWAN SPORT", "433742"),

        // CRICKET
        c("DS: SS Cricket HD", "CRICKET", "362434"),
        c("UK: SKY SPORTS CRICKET HD", "CRICKET", "376914"),
        c("UK: ASTRO CRICKET", "CRICKET", "376935"),
        c("UK: CRICKET LIVE 3HD", "CRICKET", "376934"),
        c("UK: CRICKET LIVE 2HD", "CRICKET", "376933"),
        c("UK: CRICKET LIVE 1HD", "CRICKET", "376932"),
        c("VIP UK: SkySport Cricket HD", "CRICKET", "376852"),
        c("UK: HUB SPORTS 4", "CRICKET", "377011"),
        c("UK: HUB SPORTS 3", "CRICKET", "377010"),
        c("BD: T SPORTS HD", "CRICKET", "397831"),
        c("PK: FAST SPORTS FHD", "CRICKET", "380185"),
        c("PK: PTV SPORTS HD", "CRICKET", "380189"),
        c("PK: Ten Sports HD", "CRICKET", "380193"),
        c("PK: PTV SPORTS", "CRICKET", "379173"),

        // STAR SPORTS
        c("IN: Star Sports 1 FHD", "STAR SPORTS", "387564"),
        c("IN: Star Sports 1 Hindi FHD", "STAR SPORTS", "387565"),
        c("IN: Star Sports 2 FHD", "STAR SPORTS", "387566"),
        c("IN: Star Sports Select 1 FHD", "STAR SPORTS", "387568"),
        c("IN: Star Sports Select 2 FHD", "STAR SPORTS", "387569"),
        c("IN: Star Sports 1 Eng HD", "STAR SPORTS", "387722"),
        c("IN: Star Sports 2 Eng HD", "STAR SPORTS", "387723"),
        c("IN: Star Sports 3 Eng HD", "STAR SPORTS", "387724"),
        c("IN: Star Sports Select 1 Eng HD", "STAR SPORTS", "387725"),
        c("IN: Star Sports Select 2 Eng HD", "STAR SPORTS", "387726"),
        c("IN: Willow Cricket HD", "STAR SPORTS", "387766"),
        c("IN: Ten Sports", "STAR SPORTS", "387788"),
        c("IN: Star Sports 1 Hindi HD", "STAR SPORTS", "387909"),
        c("IN: STAR SPORTS SELECT 2", "STAR SPORTS", "364779"),
        c("IN: STAR SPORTS SELECT 1", "STAR SPORTS", "364780"),
        c("IN: STAR SPORTS 3", "STAR SPORTS", "364781"),
        c("IN: STAR SPORTS 2", "STAR SPORTS", "364782"),
        c("IN: STAR SPORTS 1", "STAR SPORTS", "364783"),
        c("IN: STAR SPORTS 1 TAMIL", "STAR SPORTS", "364864"),
        c("USA | Willow Cricket HD", "STAR SPORTS", "386666"),
        c("USA | Willow Cricket Extra", "STAR SPORTS", "386665"),

        // FAJER TV
        c("Fajer TV 1", "FAJER TV", "463532"),
        c("Fajer TV 2", "FAJER TV", "463533"),
        c("Fajer TV 3", "FAJER TV", "463534"),
        c("Fajer TV 4", "FAJER TV", "463535"),
        c("Faher TV 5", "FAJER TV", "463536"),

        // KURDISTAN SPORTS
        c("KU: Duhok Sport", "KURDISTAN SPORTS", "358226"),
        c("KU: LD Sport", "KURDISTAN SPORTS", "358229"),
        c("KU: See Sport 1", "KURDISTAN SPORTS", "358222"),
        c("KU: See Sport 2", "KURDISTAN SPORTS", "358223"),
        c("KU: See Sport 3", "KURDISTAN SPORTS", "358224"),
        c("KU: Ava Sport", "KURDISTAN SPORTS", "358221"),
        c("KU: Aro Sport", "KURDISTAN SPORTS", "358225"),
        c("KU: 4 Sport", "KURDISTAN SPORTS", "358227"),
        c("KU: Astera Sport", "KURDISTAN SPORTS", "358228"),
        c("KU: NRT Sport", "KURDISTAN SPORTS", "358220"),
        c("KU: Kurdistan Sport", "KURDISTAN SPORTS", "358219"),
        c("KU: Dasinya Sport", "KURDISTAN SPORTS", "358230"),
        c("KU: MTV Sport", "KURDISTAN SPORTS", "358231"),
        c("KU: Aso Sport", "KURDISTAN SPORTS", "358232"),
        c("KU: Newline Sport", "KURDISTAN SPORTS", "358233"),
        c("KU: MMN SPORT", "KURDISTAN SPORTS", "358234"),
        c("KU: NUBAR SPORT", "KURDISTAN SPORTS", "358235"),
        c("KU: SIMA SPORT", "KURDISTAN SPORTS", "358236"),
        c("KU: Zaxo Sport", "KURDISTAN SPORTS", "358237"),
        c("KU: LD SPORT CHEAK", "KURDISTAN SPORTS", "358238"),
        c("KU: Delal Sport", "KURDISTAN SPORTS", "358239"),

        // SHAHID SPORT
        c("Shahid Spot1 4K", "SHAHID SPORT", "430911"),
        c("Shahid Spot2 4K", "SHAHID SPORT", "430912"),
        c("Shahid Spot3 4K", "SHAHID SPORT", "430913"),
        c("Shahid Spot4 4K", "SHAHID SPORT", "430914"),
        c("Shahid Spot5 4K", "SHAHID SPORT", "430915"),

        // SHASHA
        c("Shasha 1 TV 4K", "SHASHA", "348400"),
        c("Shasha 2 TV 4K", "SHASHA", "244079"),
        c("Shasha 3 TV 4K", "SHASHA", "443029")
    )

    // ============================================================
    // CREATE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        buildInterface()
    }

    // ============================================================
    // MAIN INTERFACE
    // ============================================================

    private fun buildInterface() {

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(backgroundColor)

        // ========================================================
        // TOP BAR
        // ========================================================

        topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL

        topBar.setPadding(
            dp(30),
            dp(8),
            dp(30),
            dp(8)
        )

        topBar.setBackgroundColor(surfaceColor)

        // LOGO
        val logoBox = LinearLayout(this)
        logoBox.orientation = LinearLayout.HORIZONTAL
        logoBox.gravity = Gravity.CENTER_VERTICAL

        val logoMark = TextView(this)

        logoMark.text = "NT"
        logoMark.textSize = 22f
        logoMark.setTextColor(Color.WHITE)
        logoMark.gravity = Gravity.CENTER
        logoMark.typeface = Typeface.DEFAULT_BOLD

        logoMark.background = roundedBackground(
            accentColor,
            14
        )

        logoBox.addView(
            logoMark,
            LinearLayout.LayoutParams(
                dp(50),
                dp(42)
            )
        )

        val logoText = TextView(this)

        logoText.text = "NIYATI TV"
        logoText.textSize = 22f
        logoText.setTextColor(textColor)
        logoText.typeface = Typeface.DEFAULT_BOLD
        logoText.gravity = Gravity.CENTER_VERTICAL

        logoText.setPadding(
            dp(12),
            0,
            0,
            0
        )

        logoBox.addView(
            logoText,
            LinearLayout.LayoutParams(
                -2,
                dp(42)
            )
        )

        topBar.addView(
            logoBox,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        // LIVE INDICATOR
        val liveBox = LinearLayout(this)

        liveBox.orientation = LinearLayout.HORIZONTAL
        liveBox.gravity = Gravity.CENTER
        liveBox.setPadding(
            dp(16),
            0,
            dp(16),
            0
        )

        liveBox.background = roundedBackground(
            Color.rgb(35, 22, 28),
            30
        )

        val liveDot = TextView(this)
        liveDot.text = "●"
        liveDot.textSize = 13f
        liveDot.setTextColor(accentColor)

        liveBox.addView(
            liveDot,
            LinearLayout.LayoutParams(
                -2,
                -2
            )
        )

        val liveText = TextView(this)
        liveText.text = "LIVE"
        liveText.textSize = 13f
        liveText.setTextColor(textColor)
        liveText.typeface = Typeface.DEFAULT_BOLD

        liveText.setPadding(
            dp(6),
            0,
            0,
            0
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
                dp(100),
                dp(38)
            )
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        // ========================================================
        // PLAYER
        // ========================================================

        playerContainer = FrameLayout(this)
        playerContainer.setBackgroundColor(Color.BLACK)

        playerView = PlayerView(this)

        playerView.useController = true
        playerView.setBackgroundColor(Color.BLACK)
        playerView.isFocusable = true
        playerView.isFocusableInTouchMode = true

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        // PLAYER TITLE OVERLAY
        statusText = TextView(this)

        statusText.text = "NIYATI TV"
        statusText.textSize = 15f
        statusText.setTextColor(Color.WHITE)
        statusText.typeface = Typeface.DEFAULT_BOLD
        statusText.gravity = Gravity.CENTER_VERTICAL

        statusText.setPadding(
            dp(16),
            0,
            dp(16),
            0
        )

        statusText.background = roundedBackground(
            Color.argb(170, 0, 0, 0),
            20
        )

        val statusParams =
            FrameLayout.LayoutParams(
                dp(170),
                dp(40)
            )

        statusParams.gravity =
            Gravity.TOP or Gravity.START

        statusParams.setMargins(
            dp(18),
            dp(18),
            0,
            0
        )

        playerContainer.addView(
            statusText,
            statusParams
        )

        // FULLSCREEN BUTTON
        fullscreenButton = TextView(this)

        fullscreenButton.text = "⛶"
        fullscreenButton.textSize = 26f
        fullscreenButton.setTextColor(Color.WHITE)
        fullscreenButton.gravity = Gravity.CENTER
        fullscreenButton.isFocusable = true
        fullscreenButton.isFocusableInTouchMode = true

        fullscreenButton.background = roundedBackground(
            Color.argb(200, 15, 15, 15),
            18
        )

        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        val fullParams =
            FrameLayout.LayoutParams(
                dp(58),
                dp(58)
            )

        fullParams.gravity =
            Gravity.BOTTOM or Gravity.END

        fullParams.setMargins(
            0,
            0,
            dp(22),
            dp(22)
        )

        playerContainer.addView(
            fullscreenButton,
            fullParams
        )

        root.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.52f
            )
        )

        // ========================================================
        // NOW PLAYING
        // ========================================================

        nowPlaying = TextView(this)

        nowPlaying.text =
            "  اختر قناة لبدء المشاهدة"

        nowPlaying.textSize = 16f
        nowPlaying.setTextColor(textColor)
        nowPlaying.typeface = Typeface.DEFAULT_BOLD
        nowPlaying.gravity = Gravity.CENTER_VERTICAL

        nowPlaying.setPadding(
            dp(22),
            0,
            dp(22),
            0
        )

        nowPlaying.background = roundedBackground(
            surfaceColor,
            0
        )

        root.addView(
            nowPlaying,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            )
        )

        // ========================================================
        // PACKAGE TITLE
        // ========================================================

        val packageTitle = TextView(this)

        packageTitle.text = "  الباقات"
        packageTitle.textSize = 17f
        packageTitle.setTextColor(textColor)
        packageTitle.typeface = Typeface.DEFAULT_BOLD
        packageTitle.gravity = Gravity.CENTER_VERTICAL

        packageTitle.setPadding(
            dp(20),
            0,
            0,
            0
        )

        root.addView(
            packageTitle,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        // ========================================================
        // PACKAGES HORIZONTAL
        // ========================================================

        packageScroll = HorizontalScrollView(this)

        packageScroll.isHorizontalScrollBarEnabled = false
        packageScroll.isFocusable = false

        packagesLayout = LinearLayout(this)
        packagesLayout.orientation = LinearLayout.HORIZONTAL
        packagesLayout.gravity = Gravity.CENTER_VERTICAL

        packagesLayout.setPadding(
            dp(18),
            dp(4),
            dp(18),
            dp(8)
        )

        packageScroll.addView(
            packagesLayout,
            HorizontalScrollView.LayoutParams(
                -2,
                -1
            )
        )

        root.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        // ========================================================
        // CHANNEL TITLE
        // ========================================================

        val channelTitle = TextView(this)

        channelTitle.text = "  القنوات"
        channelTitle.textSize = 17f
        channelTitle.setTextColor(textColor)
        channelTitle.typeface = Typeface.DEFAULT_BOLD
        channelTitle.gravity = Gravity.CENTER_VERTICAL

        root.addView(
            channelTitle,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        // ========================================================
        // CHANNELS
        // ========================================================

        channelScroll = ScrollView(this)

        channelScroll.isVerticalScrollBarEnabled = false

        channelsLayout = LinearLayout(this)

        channelsLayout.orientation =
            LinearLayout.VERTICAL

        channelsLayout.setPadding(
            dp(18),
            dp(4),
            dp(18),
            dp(24)
        )

        channelScroll.addView(
            channelsLayout,
            ScrollView.LayoutParams(
                -1,
                -2
            )
        )

        root.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                0.48f
            )
        )

        setContentView(root)

        loadPackages()
    }

    // ============================================================
    // PACKAGES
    // ============================================================

    private fun loadPackages() {

        packagesLayout.removeAllViews()

        val groups =
            channels
                .map { it.group }
                .distinct()

        if (groups.isEmpty()) {
            return
        }

        currentGroup = groups.first()

        groups.forEachIndexed { index, group ->

            val button =
                createPackageButton(
                    group,
                    index == 0
                )

            button.setOnClickListener {

                currentGroup = group

                updatePackageSelection(button)

                loadChannels(group)
            }

            packagesLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    dp(175),
                    dp(52)
                ).apply {
                    setMargins(
                        dp(5),
                        0,
                        dp(5),
                        0
                    )
                }
            )
        }

        loadChannels(groups.first())
    }

    private fun updatePackageSelection(
        selectedButton: TextView
    ) {

        for (
            i in 0 until packagesLayout.childCount
        ) {

            val child =
                packagesLayout.getChildAt(i)

            if (child is TextView) {

                child.background =
                    roundedBackground(
                        cardColor,
                        16
                    )
            }
        }

        selectedButton.background =
            roundedBackground(
                cardFocusColor,
                16
            )
    }

    // ============================================================
    // CHANNELS
    // ============================================================

    private fun loadChannels(
        group: String
    ) {

        channelsLayout.removeAllViews()

        val filtered =
            channels.filter {
                it.group == group
            }

        filtered.forEachIndexed { index, channel ->

            val button =
                createChannelButton(
                    channel,
                    index == 0
                )

            button.setOnClickListener {

                updateChannelSelection(
                    button
                )

                playChannel(channel)
            }

            channelsLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    -1,
                    dp(68)
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

    private fun updateChannelSelection(
        selectedButton: TextView
    ) {

        for (
            i in 0 until channelsLayout.childCount
        ) {

            val child =
                channelsLayout.getChildAt(i)

            if (child is TextView) {

                child.background =
                    roundedBackground(
                        cardColor,
                        18
                    )
            }
        }

        selectedButton.background =
            roundedBackground(
                cardFocusColor,
                18
            )
    }

    // ============================================================
    // PACKAGE BUTTON
    // ============================================================

    private fun createPackageButton(
        name: String,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        button.text = name
        button.textSize = 15f
        button.setTextColor(textColor)
        button.typeface = Typeface.DEFAULT_BOLD
        button.gravity = Gravity.CENTER
        button.isFocusable = true
        button.isFocusableInTouchMode = true

        button.background =
            roundedBackground(
                if (selected)
                    cardFocusColor
                else
                    cardColor,
                16
            )

        button.setOnFocusChangeListener {
                view,
                hasFocus ->

            if (hasFocus) {

                view.background =
                    roundedBackground(
                        cardFocusColor,
                        16
                    )

            } else if (
                currentGroup != name
            ) {

                view.background =
                    roundedBackground(
                        cardColor,
                        16
                    )
            }
        }

        return button
    }

    // ============================================================
    // CHANNEL BUTTON
    // ============================================================

    private fun createChannelButton(
        channel: Channel,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        val number =
            channels.indexOf(channel) + 1

        button.text =
            String.format(
                "%03d     %s                                      ▶",
                number,
                channel.name
            )

        button.textSize = 16f
        button.setTextColor(textColor)
        button.typeface = Typeface.DEFAULT_BOLD
        button.gravity = Gravity.CENTER_VERTICAL

        button.setPadding(
            dp(22),
            0,
            dp(22),
            0
        )

        button.isFocusable = true
        button.isFocusableInTouchMode = true

        button.background =
            roundedBackground(
                if (selected)
                    cardFocusColor
                else
                    cardColor,
                18
            )

        button.setOnFocusChangeListener {
                view,
                hasFocus ->

            if (hasFocus) {

                view.background =
                    roundedBackground(
                        cardFocusColor,
                        18
                    )

            } else {

                view.background =
                    roundedBackground(
                        cardColor,
                        18
                    )
            }
        }

        return button
    }

    // ============================================================
    // PLAY CHANNEL
    // ============================================================

    private fun playChannel(
        channel: Channel
    ) {

        currentChannel = channel

        nowPlaying.text =
            "  ▶  ${channel.name}"

        statusText.text =
            "  ${channel.name}"

        try {

            if (exoPlayer == null) {

                exoPlayer =
                    ExoPlayer.Builder(this)
                        .build()

                playerView.player =
                    exoPlayer

                exoPlayer?.addListener(
                    object : Player.Listener {

                        override fun onPlaybackStateChanged(
                            playbackState: Int
                        ) {

                            when (playbackState) {

                                Player.STATE_BUFFERING -> {

                                    statusText.text =
                                        "  جاري تحميل ${channel.name}"
                                }

                                Player.STATE_READY -> {

                                    statusText.text =
                                        "  ${channel.name}"
                                }
                            }
                        }

                        override fun onPlayerError(
                            error: PlaybackException
                        ) {

                            statusText.text =
                                "  تعذر تشغيل القناة"

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

        fullscreen = true

        topBar.visibility =
            View.GONE

        packageScroll.visibility =
            View.GONE

        channelsLayout.visibility =
            View.GONE

        nowPlaying.visibility =
            View.GONE

        fullscreenButton.visibility =
            View.GONE

        statusText.visibility =
            View.GONE

        channelScroll.visibility =
            View.GONE

        val params =
            playerContainer.layoutParams

        params.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.height =
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

        packageScroll.visibility =
            View.VISIBLE

        channelsLayout.visibility =
            View.VISIBLE

        nowPlaying.visibility =
            View.VISIBLE

        fullscreenButton.visibility =
            View.VISIBLE

        statusText.visibility =
            View.VISIBLE

        channelScroll.visibility =
            View.VISIBLE

        val params =
            playerContainer.layoutParams

        params.width =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.height = 0

        playerContainer.layoutParams =
            params

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

            when (event.keyCode) {

                KeyEvent.KEYCODE_BACK -> {

                    if (fullscreen) {

                        exitFullscreen()

                        return true
                    }

                    if (
                        exoPlayer != null
                    ) {

                        exoPlayer?.stop()

                        currentChannel = null

                        nowPlaying.text =
                            "  اختر قناة لبدء المشاهدة"

                        statusText.text =
                            "NIYATI TV"

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (fullscreen) {

                        return super.dispatchKeyEvent(
                            event
                        )
                    }

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

    // ============================================================
    // BACKUP: TV DPAD
    // ============================================================

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent
    ): Boolean {

        if (
            fullscreen &&
            keyCode == KeyEvent.KEYCODE_BACK
        ) {

            exitFullscreen()

            return true
        }

        return super.onKeyDown(
            keyCode,
            event
        )
    }

    // ============================================================
    // DRAWABLE
    // ============================================================

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {

        val drawable =
            GradientDrawable()

        drawable.setColor(color)

        drawable.cornerRadius =
            dp(radius).toFloat()

        return drawable
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources.displayMetrics.density
            ).toInt()
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroy() {

        exoPlayer?.release()

        exoPlayer = null

        super.onDestroy()
    }
}
