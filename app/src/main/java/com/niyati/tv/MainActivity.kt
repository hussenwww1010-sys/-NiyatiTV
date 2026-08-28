package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
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
    private var fullscreen = false
    private var currentGroup = ""
    private var currentChannelIndex = -1

    private val visibleChannels = mutableListOf<Channel>()
    private val channelButtons = mutableListOf<View>()

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var nowPlaying: TextView
    private lateinit var packageArea: LinearLayout
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelArea: LinearLayout
    private lateinit var channelsLayout: LinearLayout
    private lateinit var fullscreenButton: TextView

    // ألوان لوحة التحكم الجديدة (Dark Luxury / Gold-Accent)
    private val bgMain = Color.parseColor("#090C15")
    private val bgPanel = Color.parseColor("#121725")
    private val bgCard = Color.parseColor("#1B2234")
    private val bgCardFocus = Color.parseColor("#2A344D")
    
    private val primaryGold = Color.parseColor("#E5A93C")
    private val primaryGoldDark = Color.parseColor("#B37F20")
    
    private val textWhite = Color.parseColor("#FFFFFF")
    private val textMuted = Color.parseColor("#8E99B0")
    private val statusGreen = Color.parseColor("#00E676")

    private val base = "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun c(name: String, group: String, id: String): Channel {
        return Channel(
            name = name,
            group = group,
            url = "$base$id.ts"
        )
    }

    private val channels = mutableListOf<Channel>().apply {

        // BEIN TOD
        add(c("beIN Tod 4K", "BEIN TOD", "460835"))
        for (i in 1..9) {
            add(c("beIN Sport Tod $i", "BEIN TOD", "${460835 + i}"))
        }
        add(c("beIN Sport Tod English 1", "BEIN TOD", "460845"))
        add(c("beIN Sport Tod English 2", "BEIN TOD", "460846"))
        for (i in 1..9) {
            add(c("beIN Sport Tod Extra $i", "BEIN TOD", "${460846 + i}"))
        }

        // BEIN SPORTS
        add(c("beIN Sport Global 4K", "BEIN SPORTS", "22186"))
        add(c("beIN Sport News 4K", "BEIN SPORTS", "318230"))

        val bein4k = listOf(318197, 318198, 318199, 440580, 318201, 318202, 318203, 318204, 318205)
        for (i in 1..9) {
            bein4k.getOrNull(i - 1)?.let { id ->
                add(c("beIN Sport $i 4K", "BEIN SPORTS", id.toString()))
            }
            add(c("beIN$i H265", "BEIN SPORTS", "${391093 + i}"))
        }

        add(c("beIN Sport English 1 4K", "BEIN SPORTS", "319495"))
        add(c("beIN Sport English 2 4K", "BEIN SPORTS", "319496"))
        add(c("beIN Sport French 1 4K", "BEIN SPORTS", "319497"))
        add(c("beIN Sport French 2 4K", "BEIN SPORTS", "319498"))
        add(c("beIN Sport NBA 4K", "BEIN SPORTS", "319499"))

        add(c("beIN Global HD", "BEIN SPORTS", "442220"))
        add(c("beIN Sport News HD", "BEIN SPORTS", "443146"))

        for (i in 1..9) {
            add(c("beIN Sport $i HD", "BEIN SPORTS", "${325792 + i}"))
        }

        add(c("beIN Sport 1 HD English", "BEIN SPORTS", "318217"))
        add(c("beIN Sport 2 HD English", "BEIN SPORTS", "318218"))
        add(c("beIN Sport 1 HD French", "BEIN SPORTS", "319437"))
        add(c("beIN Sport 2 HD French", "BEIN SPORTS", "319438"))
        add(c("beIN Sport NBA HD", "BEIN SPORTS", "318219"))

        for (i in 1..9) {
            add(c("beIN Sport $i SD", "BEIN SPORTS", "${325802 + i}"))
        }

        add(c("beIN Sport English 1 SD", "BEIN SPORTS", "319425"))
        add(c("beIN Sport English 2 SD", "BEIN SPORTS", "319426"))
        add(c("beIN Sport French 1 SD", "BEIN SPORTS", "319427"))
        add(c("beIN Sport French 2 SD", "BEIN SPORTS", "319428"))

        // BEIN XTRA
        val xtra4k = listOf(325790, 319487, 319488, 440569, 440570, 440571, 447243, 447244, 447245)
        val xtraHd = listOf(325802, 319435, 319436, 440572, 440573, 440574, 447246, 447247, 447248)
        val xtraSd = listOf(325812, 319423, 319424, 440575, 440576, 440577, 447249, 447250, 447251)

        for (i in 1..9) {
            xtra4k.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i 4K", "BEIN XTRA", it.toString())) }
            xtraHd.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i HD", "BEIN XTRA", it.toString())) }
            xtraSd.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i SD", "BEIN XTRA", it.toString())) }
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
        ).forEach { add(c(it.first, "AL RABIAA", it.second)) }

        // ALKASS
        val alkassIds = listOf(96214, 96215, 278068, 96216, 96217, 211523, 379828, 379829, 393991, 393992)
        for (i in 1..10) {
            alkassIds.getOrNull(i - 1)?.let { add(c("Alkass $i HD", "ALKASS", it.toString())) }
        }

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
        ).forEach { add(c(it.first, "SAUDI SPORTS", it.second)) }

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
        ).forEach { add(c(it.first, "GULF SPORTS", it.second)) }

        // AD SPORTS
        listOf(
            "AD SPORTS 1 HD" to "326053",
            "AD SPORTS 2 HD" to "326054",
            "AD Sport Asia 1 HD" to "244188",
            "AD Sport Asia 2 HD" to "244191"
        ).forEach { add(c(it.first, "AD SPORTS", it.second)) }

        // ALWAN SPORT
        val alwan = listOf(
            418111, 418112, 418113, 418114, 418115, 418116,
            418117, 418118, 418119, 418120, 418121, 418122,
            418123, 418124, 418125, 418126, 418127, 418128
        )

        for (i in 1..6) {
            val x = (i - 1) * 3
            alwan.getOrNull(x)?.let { add(c("Alwan Sport $i 4K", "ALWAN SPORT", it.toString())) }
            alwan.getOrNull(x + 1)?.let { add(c("Alwan Sport $i HD", "ALWAN SPORT", it.toString())) }
            alwan.getOrNull(x + 2)?.let { add(c("Alwan Sport $i SD", "ALWAN SPORT", it.toString())) }
        }

        listOf(
            "Alwan Sport 7 4K" to "433739",
            "Alwan Sport 8 4K" to "433740",
            "Alwan Sport 9 4K" to "433741",
            "Alwan Sport 10 4K" to "433742"
        ).forEach { add(c(it.first, "ALWAN SPORT", it.second)) }

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
        ).forEach { add(c(it.first, "CRICKET", it.second)) }

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
        ).forEach { add(c(it.first, "STAR SPORTS", it.second)) }

        // FAJER TV
        listOf(
            "Fajer TV 1" to "463532",
            "Fajer TV 2" to "463533",
            "Fajer TV 3" to "463534",
            "Fajer TV 4" to "463535",
            "Fajer TV 5" to "463536"
        ).forEach { add(c(it.first, "FAJER TV", it.second)) }

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
        ).forEach { add(c(it.first, "KURDISTAN SPORTS", it.second)) }

        // SHAHID SPORT
        for (i in 1..5) {
            add(c("Shahid Sport $i 4K", "SHAHID SPORT", "${430910 + i}"))
        }

        // SHASHA
        listOf(
            "Shasha 1 TV 4K" to "348400",
            "Shasha 2 TV 4K" to "244079",
            "Shasha 3 TV 4K" to "443029"
        ).forEach { add(c(it.first, "SHASHA", it.second)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bgMain
        window.navigationBarColor = bgMain
        buildInterface()
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgMain)
        }

        createTopBar()
        createPlayer()
        createNowPlaying()
        createPackageArea()
        createChannelArea()

        setContentView(root)
        loadPackages()
    }

    private fun createTopBar() {
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
            setBackgroundColor(bgMain)
        }

        val logoBox = FrameLayout(this).apply {
            val draw: Drawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(primaryGold, primaryGoldDark)
            ).apply { cornerRadius = dp(12).toFloat() }
            background = draw
        }

        val logo = TextView(this).apply {
            text = "⚽"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        logoBox.addView(logo, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        topBar.addView(logoBox, LinearLayout.LayoutParams(dp(46), dp(46)))

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }

        val brandTitle = TextView(this).apply {
            text = "NIYATI SPORTS"
            textSize = 20f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        brand.addView(brandTitle, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val brandSub = TextView(this).apply {
            text = "ULTIMATE FOOTBALL HUB"
            textSize = 10f
            setTextColor(primaryGold)
            letterSpacing = 0.1f
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        brand.addView(brandSub, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        topBar.addView(brand, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        val liveBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(4), dp(12), dp(4))
            val draw: Drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2638"))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.parseColor("#2E3A54"))
            }
            background = draw
        }

        val liveDot = TextView(this).apply {
            text = "●"
            textSize = 10f
            setTextColor(statusGreen)
            gravity = Gravity.CENTER
        }

        liveBadge.addView(liveDot, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val liveText = TextView(this).apply {
            text = " LIVE HD"
            textSize = 11f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        liveBadge.addView(liveText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        topBar.addView(liveBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(topBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(66)))
    }

    private fun createPlayer() {
        playerContainer = FrameLayout(this).apply {
            val draw: Drawable = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(16).toFloat()
            }
            background = draw
            clipToOutline = true
        }

        val playerMarginLayout = LinearLayout(this).apply {
            setPadding(dp(16), 0, dp(16), 0)
            addView(playerContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
        }

        playerView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        playerContainer.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val watermark = TextView(this).apply {
            text = "NIYATI TV"
            textSize = 12f
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            val draw: Drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#40000000"))
                cornerRadius = dp(8).toFloat()
            }
            background = draw
        }

        val wmParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(dp(16), dp(16), 0, 0)
        }
        playerContainer.addView(watermark, wmParams)

        fullscreenButton = TextView(this).apply {
            text = "⛶ تكبير الشاشة"
            textSize = 12f
            setTextColor(textWhite)
            gravity = Gravity.CENTER
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            isFocusable = true
            isFocusableInTouchMode = true

            val draw: Drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#B0000000"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#44FFFFFF"))
            }
            background = draw

            setOnClickListener { toggleFullscreen() }
        }

        val fullscreenParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, dp(16), dp(16))
        }

        playerContainer.addView(fullscreenButton, fullscreenParams)

        root.addView(playerMarginLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240)))
    }

    private fun createNowPlaying() {
        nowPlaying = TextView(this).apply {
            text = "اختر قناة لبدء البث المباشر ⚽"
            textSize = 13f
            setTextColor(primaryGold)
            gravity = Gravity.CENTER
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        root.addView(nowPlaying, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun createPackageArea() {
        packageArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgMain)
            setPadding(0, dp(4), 0, dp(4))
        }

        val packageScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFocusable = false
        }

        packagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }

        packageScroll.addView(packagesLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        packageArea.addView(packageScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(packageArea, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun createChannelArea() {
        channelArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgPanel)
            setPadding(0, dp(8), 0, 0)
        }

        val title = TextView(this).apply {
            text = "قنوات البث المباشر"
            textSize = 14f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(20), dp(4), dp(20), dp(8))
        }

        channelArea.addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val channelScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isFocusable = false
        }

        channelsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(24))
        }

        channelScroll.addView(channelsLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        channelArea.addView(channelScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(channelArea, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun loadPackages() {
        packagesLayout.removeAllViews()
        val groups = channels.map { it.group }.distinct()

        if (groups.isEmpty()) return
        currentGroup = groups.first()

        groups.forEachIndexed { index, group ->
            val card = createPackageCard(group, index == 0)

            card.setOnClickListener {
                currentGroup = group
                updatePackageSelection()
                loadChannels(group)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                val bgDrawable: Drawable = if (hasFocus) {
                    createCardBackground(bgCardFocus, true)
                } else {
                    createCardBackground(
                        if (currentGroup == group) primaryGold else bgCard,
                        currentGroup == group
                    )
                }
                view.background = bgDrawable
            }

            packagesLayout.addView(
                card,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            )
        }

        loadChannels(groups.first())
    }

    private fun createPackageCard(name: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = packageDisplayName(name)
            textSize = 12f
            setTextColor(if (selected) bgMain else textWhite)
            gravity = Gravity.CENTER
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(18), 0, dp(18), 0)

            val draw: Drawable = createCardBackground(
                if (selected) primaryGold else bgCard,
                selected
            )
            background = draw

            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    private fun packageDisplayName(name: String): String {
        return when (name) {
            "BEIN TOD" -> "beIN TOD ⚽"
            "BEIN SPORTS" -> "beIN SPORTS 🏆"
            "BEIN XTRA" -> "beIN XTRA 🔥"
            "AL RABIAA" -> "الرابعة الرياضية"
            "ALKASS" -> "الكأس QATAR"
            "SAUDI SPORTS" -> "السعودية الرياضية"
            "GULF SPORTS" -> "قنوات الخليج"
            "AD SPORTS" -> "أبوظبي الرياضية"
            "ALWAN SPORT" -> "ألوان سبورت"
            "CRICKET" -> "الكريكت"
            "STAR SPORTS" -> "Star Sports"
            "FAJER TV" -> "الفجر TV"
            "KURDISTAN SPORTS" -> "كوردستان سبورت"
            "SHAHID SPORT" -> "شاهد رياضة"
            "SHASHA" -> "شاشة"
            else -> name
        }
    }

    private fun updatePackageSelection() {
        val groups = channels.map { it.group }.distinct()

        for (i in 0 until packagesLayout.childCount) {
            val child = packagesLayout.getChildAt(i) as? TextView ?: continue
            val group = groups.getOrNull(i)

            val isSelected = (group == currentGroup)
            child.setTextColor(if (isSelected) bgMain else textWhite)

            val bgDrawable: Drawable = createCardBackground(
                if (isSelected) primaryGold else bgCard,
                isSelected
            )
            child.background = bgDrawable
        }
    }

    private fun loadChannels(group: String) {
        channelsLayout.removeAllViews()
        channelButtons.clear()
        visibleChannels.clear()

        currentChannelIndex = -1

        val filtered = channels.filter { it.group == group }
        visibleChannels.addAll(filtered)

        var row: LinearLayout? = null

        filtered.forEachIndexed { index, channel ->
            if (index % 2 == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                channelsLayout.addView(
                    row,
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(8))
                    }
                )
            }

            val button = createChannelCard(channel)

            button.setOnClickListener {
                currentChannelIndex = index
                updateChannelSelection(button)
                playChannel(channel)
            }

            button.setOnFocusChangeListener { view, hasFocus ->
                val bgDrawable: Drawable = if (hasFocus) {
                    createChannelBackground(bgCardFocus, true)
                } else {
                    createChannelBackground(
                        if (currentChannelIndex == index) primaryGoldDark else bgCard,
                        currentChannelIndex == index
                    )
                }
                view.background = bgDrawable
            }

            row?.addView(
                button,
                LinearLayout.LayoutParams(0, dp(60), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            )

            channelButtons.add(button)

            if (index == 0) {
                button.post { button.requestFocus() }
            }
        }
    }

    private fun createChannelCard(channel: Channel): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            val draw: Drawable = createChannelBackground(bgCard, false)
            background = draw
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val icon = TextView(this).apply {
            text = "▶"
            textSize = 10f
            setTextColor(primaryGold)
            gravity = Gravity.CENTER
            val draw: Drawable = GradientDrawable().apply {
                setColor(Color.parseColor("#26324A"))
                cornerRadius = dp(8).toFloat()
            }
            background = draw
        }

        card.addView(icon, LinearLayout.LayoutParams(dp(32), dp(32)))

        val textArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
        }

        val name = TextView(this).apply {
            text = channel.name
            textSize = 12f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
            maxLines = 1
        }

        textArea.addView(name, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        card.addView(textArea, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        return card
    }

    private fun updateChannelSelection(selected: View) {
        for (button in channelButtons) {
            val bgDrawable: Drawable = createChannelBackground(bgCard, false)
            button.background = bgDrawable
        }
        val bgDrawable: Drawable = createChannelBackground(primaryGoldDark, true)
        selected.background = bgDrawable
    }

    private fun moveChannel(direction: Int) {
        if (visibleChannels.isEmpty()) return

        if (currentChannelIndex < 0) {
            currentChannelIndex = if (direction > 0) 0 else visibleChannels.lastIndex
        } else {
            currentChannelIndex += direction
            if (currentChannelIndex < 0) currentChannelIndex = visibleChannels.lastIndex
            if (currentChannelIndex > visibleChannels.lastIndex) currentChannelIndex = 0
        }

        val channel = visibleChannels[currentChannelIndex]
        val button = channelButtons.getOrNull(currentChannelIndex)

        if (button != null) {
            updateChannelSelection(button)
            button.requestFocus()
            button.post {
                button.requestRectangleOnScreen(Rect(0, 0, button.width, button.height), true)
            }
        }

        playChannel(channel)
    }

    private fun createCardBackground(color: Int, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(20).toFloat()
            if (selected) setStroke(dp(2), primaryGold)
        }
    }

    private fun createChannelBackground(color: Int, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
            if (selected) setStroke(dp(2), primaryGold)
        }
    }

    private fun playChannel(channel: Channel) {
        nowPlaying.text = "جاري العرض الآن: ${channel.name}"

        try {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(this).build()
                playerView.player = exoPlayer
                exoPlayer?.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(this@MainActivity, "تعذر تشغيل هذه القناة حالياً", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في تشغيل القناة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFullscreen() {
        if (fullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {
        if (fullscreen) return
        fullscreen = true

        topBar.visibility = View.GONE
        nowPlaying.visibility = View.GONE
        packageArea.visibility = View.GONE
        channelArea.visibility = View.GONE

        val params = playerContainer.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        params.height = LinearLayout.LayoutParams.MATCH_PARENT
        params.weight = 0f
        playerContainer.layoutParams = params

        fullscreenButton.visibility = View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }

        playerView.requestFocus()
    }

    private fun exitFullscreen() {
        if (!fullscreen) return
        fullscreen = false

        topBar.visibility = View.VISIBLE
        nowPlaying.visibility = View.VISIBLE
        packageArea.visibility = View.VISIBLE
        channelArea.visibility = View.VISIBLE

        val params = playerContainer.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240))

        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        params.height = dp(240)
        params.weight = 0f
        playerContainer.layoutParams = params

        fullscreenButton.visibility = View.VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        if (currentChannelIndex >= 0) {
            channelButtons.getOrNull(currentChannelIndex)?.requestFocus()
        } else {
            playerView.requestFocus()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (visibleChannels.isNotEmpty() && (playerView.hasFocus() || channelButtons.any { it.hasFocus() })) {
                        moveChannel(-1)
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (visibleChannels.isNotEmpty() && playerView.hasFocus()) {
                        moveChannel(1)
                        return true
                    }
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (fullscreen) {
                        exitFullscreen()
                        return true
                    }

                    if (exoPlayer != null) {
                        exoPlayer?.stop()
                        nowPlaying.text = "اختر قناة لبدء البث المباشر ⚽"
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (playerView.hasFocus()) {
                        toggleFullscreen()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        playerView.player = null
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
