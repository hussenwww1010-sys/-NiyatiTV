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

    // ============================================================
    // CHANNEL DATABASE
    // ============================================================

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

    private val channels = listOf(

        // ========================================================
        // BEIN TOD
        // ========================================================

        c("beIN Tod 4K", "beIN TOD", "460835"),

        c("beIN Sport Tod 1", "beIN TOD", "460836"),
        c("beIN Sport Tod 2", "beIN TOD", "460837"),
        c("beIN Sport Tod 3", "beIN TOD", "460838"),
        c("beIN Sport Tod 4", "beIN TOD", "460839"),
        c("beIN Sport Tod 5", "beIN TOD", "460840"),
        c("beIN Sport Tod 6", "beIN TOD", "460841"),
        c("beIN Sport Tod 7", "beIN TOD", "460842"),
        c("beIN Sport Tod 8", "beIN TOD", "460843"),
        c("beIN Sport Tod 9", "beIN TOD", "460844"),

        c("beIN Sport Tod English 1", "beIN TOD", "460845"),
        c("beIN Sport Tod English 2", "beIN TOD", "460846"),

        c("beIN Sport Tod Extra 1", "beIN TOD", "460847"),
        c("beIN Sport Tod Extra 2", "beIN TOD", "460848"),
        c("beIN Sport Tod Extra 3", "beIN TOD", "460849"),
        c("beIN Sport Tod Extra 4", "beIN TOD", "460850"),
        c("beIN Sport Tod Extra 5", "beIN TOD", "460851"),
        c("beIN Sport Tod Extra 6", "beIN TOD", "460852"),
        c("beIN Sport Tod Extra 7", "beIN TOD", "460853"),
        c("beIN Sport Tod Extra 8", "beIN TOD", "460854"),
        c("beIN Sport Tod Extra 9", "beIN TOD", "460855"),

        // ========================================================
        // BEIN SPORTS
        // ========================================================

        c("beIN Sport Global 4K", "beIN SPORTS", "22186"),
        c("beIN Sport News 4K", "beIN SPORTS", "318230"),

        c("beIN Sport 1 4K", "beIN SPORTS", "318197"),
        c("beIN1 H265", "beIN SPORTS", "391094"),

        c("beIN Sport 2 4K", "beIN SPORTS", "318198"),
        c("beIN2 H265", "beIN SPORTS", "391095"),

        c("beIN Sport 3 4K", "beIN SPORTS", "318199"),
        c("beIN3 H265", "beIN SPORTS", "391096"),

        c("beIN Sport 4 4K", "beIN SPORTS", "440580"),
        c("beIN4 H265", "beIN SPORTS", "391097"),

        c("beIN Sport 5 4K", "beIN SPORTS", "318201"),
        c("beIN5 H265", "beIN SPORTS", "391098"),

        c("beIN Sport 6 4K", "beIN SPORTS", "318202"),
        c("beIN6 H265", "beIN SPORTS", "391099"),

        c("beIN Sport 7 4K", "beIN SPORTS", "318203"),
        c("beIN7 H265", "beIN SPORTS", "391100"),

        c("beIN Sport 8 4K", "beIN SPORTS", "318204"),
        c("beIN8 H265", "beIN SPORTS", "391101"),

        c("beIN Sport 9 4K", "beIN SPORTS", "318205"),
        c("beIN9 H265", "beIN SPORTS", "391102"),

        c("beIN Sport English 1 4K", "beIN SPORTS", "319495"),
        c("beIN Sport English 2 4K", "beIN SPORTS", "319496"),

        c("beIN Sport French 1 4K", "beIN SPORTS", "319497"),
        c("beIN Sport French 2 4K", "beIN SPORTS", "319498"),

        c("beIN Sport NBA 4K", "beIN SPORTS", "319499"),

        c("beIN Global HD", "beIN SPORTS", "442220"),
        c("beIN Sport News HD", "beIN SPORTS", "443146"),

        c("beIN Sport 1 HD", "beIN SPORTS", "325793"),
        c("beIN Sport 2 HD", "beIN SPORTS", "325794"),
        c("beIN Sport 3 HD", "beIN SPORTS", "325795"),
        c("beIN Sport 4 HD", "beIN SPORTS", "325796"),
        c("beIN Sport 5 HD", "beIN SPORTS", "325797"),
        c("beIN Sport 6 HD", "beIN SPORTS", "325798"),
        c("beIN Sport 7 HD", "beIN SPORTS", "325799"),
        c("beIN Sport 8 HD", "beIN SPORTS", "325800"),
        c("beIN Sport 9 HD", "beIN SPORTS", "325801"),

        c("beIN Sport 1 HD English", "beIN SPORTS", "318217"),
        c("beIN Sport 2 HD English", "beIN SPORTS", "318218"),

        c("beIN Sport 1 HD Frensh", "beIN SPORTS", "319437"),
        c("beIN Sport 2 HD Frensh", "beIN SPORTS", "319438"),

        c("beIN Sport NBA HD", "beIN SPORTS", "318219"),

        c("beIN Sport 1 SD", "beIN SPORTS", "325803"),
        c("beIN Sport 2 SD", "beIN SPORTS", "325804"),
        c("beIN Sport 3 SD", "beIN SPORTS", "325805"),
        c("beIN Sport 4 SD", "beIN SPORTS", "325806"),
        c("beIN Sport 5 SD", "beIN SPORTS", "325807"),
        c("beIN Sport 6 SD", "beIN SPORTS", "325808"),
        c("beIN Sport 7 SD", "beIN SPORTS", "325809"),
        c("beIN Sport 8 SD", "beIN SPORTS", "325810"),
        c("beIN Sport 9 SD", "beIN SPORTS", "325811"),

        c("beIN Sport English 1 SD", "beIN SPORTS", "319425"),
        c("beIN Sport English 2 SD", "beIN SPORTS", "319426"),

        c("beIN Sport French 1 SD", "beIN SPORTS", "319427"),
        c("beIN Sport French 2 SD", "beIN SPORTS", "319428"),

        // ========================================================
        // BEIN XTRA
        // ========================================================

        c("beIN Sport XTRA 1 4K", "beIN XTRA", "325790"),
        c("beIN Sport XTRA 2 4K", "beIN XTRA", "319487"),
        c("beIN Sport XTRA 3 4K", "beIN XTRA", "319488"),
        c("beIN Sport XTRA 4 4K", "beIN XTRA", "440569"),
        c("beIN Sport XTRA 5 4K", "beIN XTRA", "440570"),
        c("beIN Sport XTRA 6 4K", "beIN XTRA", "440571"),
        c("beIN Sport XTRA 7 4K", "beIN XTRA", "447243"),
        c("beIN Sport XTRA 8 4K", "beIN XTRA", "447244"),
        c("beIN Sport XTRA 9 4K", "beIN XTRA", "447245"),

        c("beIN Sport Xtra 1 HD", "beIN XTRA", "325802"),
        c("beIN Sport Xtra 2 HD", "beIN XTRA", "319435"),
        c("beIN Sport Xtra 3 HD", "beIN XTRA", "319436"),
        c("beIN Sport Xtra 4 HD", "beIN XTRA", "440572"),
        c("beIN Sport Xtra 5 HD", "beIN XTRA", "440573"),
        c("beIN Sport Xtra 6 HD", "beIN XTRA", "440574"),
        c("beIN Sport Xtra 7 HD", "beIN XTRA", "447246"),
        c("beIN Sport Xtra 8 HD", "beIN XTRA", "447247"),
        c("beIN Sport Xtra 9 HD", "beIN XTRA", "447248"),

        c("beIN Sport Xtra 1 SD", "beIN XTRA", "325812"),
        c("beIN Sport Xtra 2 SD", "beIN XTRA", "319423"),
        c("beIN Sport Xtra 3 SD", "beIN XTRA", "319424"),
        c("beIN Sport Xtra 4 SD", "beIN XTRA", "440575"),
        c("beIN Sport Xtra 5 SD", "beIN XTRA", "440576"),
        c("beIN Sport Xtra 6 SD", "beIN XTRA", "440577"),
        c("beIN Sport Xtra 7 SD", "beIN XTRA", "447249"),
        c("beIN Sport Xtra 8 SD", "beIN XTRA", "447250"),
        c("beIN Sport Xtra 9 SD", "beIN XTRA", "447251"),

        // ========================================================
        // AL RABIAA
        // ========================================================

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

        // ========================================================
        // ALKASS
        // ========================================================

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

        // ========================================================
        // SAUDI SPORTS
        // ========================================================

        c("KSA Sport 1 4K", "SAUDI SPORTS", "97805"),
        c("KSA Sport 2 4K", "SAUDI SPORTS", "97806"),
        c("KSA Sport 3 4K", "SAUDI SPORTS", "97807"),
        c("SAUDUA NOW", "SAUDI SPORTS", "97808"),
        c("SAUDI 24 SPORT HD", "SAUDI SPORTS", "100470"),
        c("STC SPORT 1 HD", "SAUDI SPORTS", "421391"),
        c("STC SPORT 2 HD", "SAUDI SPORTS", "421392"),
        c("STC SPORT 3 HD", "SAUDI SPORTS", "420903"),
        c("STC SPORT 4 HD", "SAUDI SPORTS", "433178"),

        // ========================================================
        // GULF / ARAB SPORTS
        // ========================================================

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

        // ========================================================
        // AD SPORTS
        // ========================================================

        c("AD SPORTS 1 HD", "AD SPORTS", "326053"),
        c("AD SPORTS 2 HD", "AD SPORTS", "326054"),
        c("AD Sport Asia 1 HD", "AD SPORTS", "244188"),
        c("AD Sport Asia 2 HD", "AD SPORTS", "244191"),

        // ========================================================
        // ALWAN SPORT
        // ========================================================

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

        // ========================================================
        // CRICKET
        // ========================================================

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

        // ========================================================
        // STAR SPORTS
        // ========================================================

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

        // ========================================================
        // FAJER TV
        // ========================================================

        c("Fajer TV 1", "FAJER TV", "463532"),
        c("Fajer TV 2", "FAJER TV", "463533"),
        c("Fajer TV 3", "FAJER TV", "463534"),
        c("Fajer TV 4", "FAJER TV", "463535"),
        c("Faher TV 5", "FAJER TV", "463536"),

        // ========================================================
        // KURDISTAN SPORTS
        // ========================================================

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

        // ========================================================
        // SHAHID SPORT
        // ========================================================

        c("Shahid Spot1 4K", "SHAHID SPORT", "430911"),
        c("Shahid Spot2 4K", "SHAHID SPORT", "430912"),
        c("Shahid Spot3 4K", "SHAHID SPORT", "430913"),
        c("Shahid Spot4 4K", "SHAHID SPORT", "430914"),
        c("Shahid Spot5 4K", "SHAHID SPORT", "430915"),

        // ========================================================
        // SHASHA
        // ========================================================

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

        buildInterface()
    }

    // ============================================================
    // INTERFACE
    // ============================================================

    private fun buildInterface() {

        root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(backgroundColor)

        // --------------------------------------------------------
        // TOP BAR
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // PLAYER
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // NOW PLAYING
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // BOTTOM AREA
        // --------------------------------------------------------

        bottomArea = LinearLayout(this)

        bottomArea.orientation =
            LinearLayout.HORIZONTAL

        bottomArea.setBackgroundColor(
            panelColor
        )

        // --------------------------------------------------------
        // PACKAGES
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // CHANNELS
        // --------------------------------------------------------

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

    // ============================================================
    // PACKAGES
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
                    i in 0 until
                            packagesLayout.childCount
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
                    i in 0 until
                            channelsLayout.childCount
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

    // ============================================================
    // PACKAGE BUTTON
    // ============================================================

    private fun createPackageButton(
        name: String,
        selected: Boolean
    ): TextView {

        val button = TextView(this)

        button.text = name
        button.textSize = 17f
        button.setTextColor(textColor)

        button.gravity =
            Gravity.CENTER_VERTICAL

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

    // ============================================================
    // PLAY CHANNEL
    // ============================================================

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

                    // أول ضغطة ترجع من fullscreen
                    if (fullscreen) {

                        exitFullscreen()

                        return true
                    }

                    // إذا توجد قناة تعمل،
                    // زر Back يوقف القناة ولا يغلق التطبيق
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

                    // OK داخل المشغل = fullscreen
                    if (playerView.hasFocus()) {

                        toggleFullscreen()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroy() {

        exoPlayer?.release()

        exoPlayer = null

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
                resources.displayMetrics.density
            ).toInt()
    }
}
