package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

data class Channel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : Activity() {

    private var exoPlayer: ExoPlayer? = null
    private var fullscreen = false
    private var currentGroup = "beIN SPORTS 🏆"

    private lateinit var root: LinearLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsGrid: GridLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var epgTitle: TextView
    private lateinit var epgSub: TextView

    private val bgDark = Color.parseColor("#0B0C0E")
    private val bgCardDark = Color.parseColor("#14161B")
    private val bronzeGoldGradientStart = Color.parseColor("#8A6635")
    private val bronzeGoldGradientEnd = Color.parseColor("#3A2A14")
    private val goldText = Color.parseColor("#D4AF37")
    private val strokeBronze = Color.parseColor("#5A4525")
    private val textWhite = Color.parseColor("#E6E8EC")
    private val textMuted = Color.parseColor("#8C929E")

    private val base = "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun c(name: String, group: String, id: String): Channel {
        return Channel(name = name, group = group, url = "$base$id.ts")
    }

    private fun customChannel(name: String, group: String, url: String): Channel {
        return Channel(name = name, group = group, url = url)
    }

    private val channels = mutableListOf<Channel>().apply {
        // 1. BEIN SPORTS
        val gBein = "beIN SPORTS 🏆"
        add(c("beIN Sport Global 4K", gBein, "22186"))
        add(c("beIN Sport News 4K", gBein, "318230"))
        val bein4k = listOf(318197, 318198, 318199, 440580, 318201, 318202, 318203, 318204, 318205)
        for (i in 1..9) {
            bein4k.getOrNull(i - 1)?.let { add(c("beIN Sport $i 4K", gBein, it.toString())) }
            add(c("beIN$i H265", gBein, "${391093 + i}"))
        }
        add(c("beIN Sport English 1 4K", gBein, "319495"))
        add(c("beIN Sport English 2 4K", gBein, "319496"))
        add(c("beIN Sport French 1 4K", gBein, "319497"))
        add(c("beIN Sport French 2 4K", gBein, "319498"))
        add(c("beIN Sport NBA 4K", gBein, "319499"))
        add(c("beIN Global HD", gBein, "442220"))
        add(c("beIN Sport News HD", gBein, "443146"))
        for (i in 1..9) add(c("beIN Sport $i HD", gBein, "${325792 + i}"))
        add(c("beIN Sport 1 HD English", gBein, "318217"))
        add(c("beIN Sport 2 HD English", gBein, "318218"))
        add(c("beIN Sport 1 HD French", gBein, "319437"))
        add(c("beIN Sport 2 HD French", gBein, "319438"))
        add(c("beIN Sport NBA HD", gBein, "318219"))
        for (i in 1..9) add(c("beIN Sport $i SD", gBein, "${325802 + i}"))

        // 2. BEIN TOD
        val gTod = "beIN TOD ⚽"
        add(c("beIN Tod 4K", gTod, "460835"))
        for (i in 1..9) add(c("beIN Sport Tod $i", gTod, "${460835 + i}"))
        add(c("beIN Sport Tod English 1", gTod, "460845"))
        add(c("beIN Sport Tod English 2", gTod, "460846"))
        for (i in 1..9) add(c("beIN Sport Tod Extra $i", gTod, "${460846 + i}"))

        // 3. ALWAN SPORT
        val gAlwan = "Alwan Sport 🎨"
        val alwan = listOf(418111, 418112, 418113, 418114, 418115, 418116, 418117, 418118, 418119, 418120, 418121, 418122, 418123, 418124, 418125, 418126, 418127, 418128)
        for (i in 1..6) {
            val x = (i - 1) * 3
            alwan.getOrNull(x)?.let { add(c("Alwan Sport $i 4K", gAlwan, it.toString())) }
            alwan.getOrNull(x + 1)?.let { add(c("Alwan Sport $i HD", gAlwan, it.toString())) }
            alwan.getOrNull(x + 2)?.let { add(c("Alwan Sport $i SD", gAlwan, it.toString())) }
        }
        listOf("7" to "433739", "8" to "433740", "9" to "433741", "10" to "433742").forEach {
            add(c("Alwan Sport ${it.first} 4K", gAlwan, it.second))
        }

        // 4. BEIN XTRA
        val gXtra = "beIN XTRA 🔥"
        val xtra4k = listOf(325790, 319487, 319488, 440569, 440570, 440571, 447243, 447244, 447245)
        for (i in 1..9) {
            xtra4k.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i 4K", gXtra, it.toString())) }
        }

        // 5. AL RABIAA
        val gRabiaa = "12 الرابعة الرياضية"
        listOf(
            "AL RABIAA SPORT 1" to "371931", "AL RABIAA SPORT 1+" to "371933",
            "AL RABIAA SPORT 2" to "371932", "Rabiaa Sport +2" to "434565",
            "AL RABIAA TV 4K" to "371939", "AL RABIAA MOVIES" to "371934",
            "Rabiaa Variety" to "434566", "Njoom Al Rabiaa" to "434567",
            "AL RABIAA SERIES" to "371935", "AL RABIAA GEO" to "371936",
            "AL RABIAA QURAN" to "371937", "AL RABIAA MUSICA" to "371938"
        ).forEach { add(c(it.first, gRabiaa, it.second)) }

        // 6. ALKASS
        val gKass = "10 الكأس QATAR"
        val alkassIds = listOf(96214, 96215, 278068, 96216, 96217, 211523, 379828, 379829, 393991, 393992)
        for (i in 1..10) {
            alkassIds.getOrNull(i - 1)?.let { add(c("Alkass $i HD", gKass, it.toString())) }
        }

        // 7. SAUDI SPORTS
        val gSaudi = "9 السعودية الرياضية"
        listOf(
            "KSA Sport 1 4K" to "97805", "KSA Sport 2 4K" to "97806",
            "KSA Sport 3 4K" to "97807", "SAUDUA NOW" to "97808",
            "SAUDI 24 SPORT HD" to "100470", "STC SPORT 1 HD" to "421391",
            "STC SPORT 2 HD" to "421392", "STC SPORT 3 HD" to "420903",
            "STC SPORT 4 HD" to "433178"
        ).forEach { add(c(it.first, gSaudi, it.second)) }

        // 8. AD SPORTS
        val gAd = "4 أبوظبي الرياضية"
        listOf(
            "AD SPORTS 1 HD" to "326053", "AD SPORTS 2 HD" to "326054",
            "AD Sport Asia 1 HD" to "244188", "AD Sport Asia 2 HD" to "244191"
        ).forEach { add(c(it.first, gAd, it.second)) }

        // 9. GULF SPORTS
        val gGulf = "25 قنوات الخليج"
        listOf(
            "Dubai Sport 1 HD" to "97813", "Dubai Sport 2 HD" to "97814",
            "Dubai Racing 1 HD" to "97816", "On Sport HD 1" to "97820",
            "ON SPORTS MAX 4K" to "97821", "AR: ON TIME SPORT FM" to "399432",
            "ON SPORTS PLUS HD" to "97825", "Oman Sport HD" to "97877",
            "KUWAIT SPORT 4K" to "97826", "BAHRAIN SPORT 1 HD" to "66383",
            "YAS SPORT HD" to "328659", "ALRABIAA SPORT 4K" to "328660",
            "LIBYA SPORT 2 4K" to "328661", "BAHRAIN SPORT 2 HD" to "328662",
            "KUWAIT SPORT PLUS 4K" to "328663", "PALASTINE SPORT 4K" to "328664",
            "Iraqia Sport" to "107038", "ufm radio" to "267050",
            "Sharjah Sport HD" to "141797", "Libya Sport 1 TV" to "97818",
            "Jordan Sport TV" to "109699", "Zamalik" to "97822",
            "Nile Sport" to "97824", "Al Ahly TV" to "97823",
            "PalestineSport" to "417306"
        ).forEach { add(c(it.first, gGulf, it.second)) }

        // 10. SHAHID SPORT
        val gShahid = "Shahid Sport"
        for (i in 1..5) add(c("Shahid Sport $i 4K", gShahid, "${430910 + i}"))

        // 11. FAJER TV
        val gFajer = "Fajer TV"
        for (i in 1..5) add(c("Fajer TV $i", gFajer, "${463531 + i}"))

        // 12. KURDISTAN SPORTS
        val gKurd = "Kurdistan Sports"
        listOf(
            "KU: Duhok Sport" to "358226", "KU: LD Sport" to "358229",
            "KU: See Sport 1" to "358222", "KU: See Sport 2" to "358223",
            "KU: See Sport 3" to "358224", "KU: Ava Sport" to "358221",
            "KU: Aro Sport" to "358225", "KU: 4 Sport" to "358227",
            "KU: Astera Sport" to "358228", "KU: NRT Sport" to "358220",
            "KU: Kurdistan Sport" to "358219", "KU: Dasinya Sport" to "358230",
            "KU: MTV Sport" to "358231", "KU: Aso Sport" to "358232",
            "KU: Newline Sport" to "358233", "KU: MMN SPORT" to "358234",
            "KU: NUBAR SPORT" to "358235", "KU: SIMA SPORT" to "358236",
            "KU: Zaxo Sport" to "358237", "KU: LD SPORT CHEAK" to "358238",
            "KU: Delal Sport" to "358239"
        ).forEach { add(c(it.first, gKurd, it.second)) }

        // 13. SHASHA
        val gShasha = "Shasha TV"
        listOf("Shasha 1 TV 4K" to "348400", "Shasha 2 TV 4K" to "244079", "Shasha 3 TV 4K" to "443029").forEach {
            add(c(it.first, gShasha, it.second))
        }

        // 14. DRAMA & MBC
        val gDrama = "Drama & MBC 🎬"
        listOf(
            "Drama TV" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=316966&extension=ts",
            "Maraya TV" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=316957&extension=ts",
            "MBC 1" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120314&extension=ts",
            "MBC 2" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120313&extension=ts",
            "MBC 3" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120312&extension=ts",
            "MBC 4" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120311&extension=ts",
            "MBC ACTION" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120308&extension=ts",
            "MBC MAX FHD" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=23077&extension=ts",
            "MBC DRAMA" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120306&extension=ts",
            "MBC MASR 1 FHD" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120303&extension=ts",
            "MBC MASR 2 FHD" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=120302&extension=ts",
            "MBC IRAQ FHD" to "http://4kpro2.com:8789/play/live.php?mac=00:1A:79:FB:74:61&stream=84196&extension=ts"
        ).forEach { add(customChannel(it.first, gDrama, it.second)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bgDark

        buildGoldInterface()
    }

    private fun buildGoldInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgDark)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        createPackagesColumn()
        createChannelsGridSection()
        createPlayerSection()

        root.addView(mainContent, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))

        setContentView(root)
        loadPackages()
    }

    private fun createPackagesColumn() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val logoBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(15))
        }
        val logoIcon = TextView(this).apply {
            text = "N"
            textSize = 20f
            setTextColor(goldText)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), goldText)
                setColor(bgCardDark)
            }
        }
        logoBox.addView(logoIcon, LinearLayout.LayoutParams(dp(42), dp(42)))

        val brandText = TextView(this).apply {
            text = "  NIYATI TV"
            textSize = 18f
            setTextColor(goldText)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        logoBox.addView(brandText)
        col.addView(logoBox)

        val header = TextView(this).apply {
            text = "الباقات"
            textSize = 14f
            setTextColor(goldText)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, 0, 0, dp(10))
        }
        col.addView(header)

        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        packagesLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(packagesLayout)

        col.addView(scroll, LinearLayout.LayoutParams(dp(180), 0, 1f))
        mainContent.addView(col)
    }

    private fun createChannelsGridSection() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), 0, dp(15), 0)
        }

        val header = TextView(this).apply {
            text = "القنوات المتاحة   Premium Channels"
            textSize = 14f
            setTextColor(goldText)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, 0, 0, dp(10))
        }
        col.addView(header)

        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        channelsGrid = GridLayout(this).apply {
            columnCount = 2
            orientation = GridLayout.HORIZONTAL
        }
        scroll.addView(channelsGrid)

        col.addView(scroll, LinearLayout.LayoutParams(dp(320), LinearLayout.LayoutParams.MATCH_PARENT))
        mainContent.addView(col)
    }

    private fun createPlayerSection() {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val topBadgeBar = LinearLayout(this).apply {
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(8))
        }
        val liveBadge = TextView(this).apply {
            text = "● مباشر"
            textSize = 11f
            setTextColor(Color.parseColor("#00E676"))
            setPadding(dp(12), dp(4), dp(12), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#102B1E"))
                cornerRadius = dp(15).toFloat()
                setStroke(dp(1), Color.parseColor("#00E676"))
            }
        }
        topBadgeBar.addView(liveBadge)
        col.addView(topBadgeBar)

        playerContainer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(2), bronzeGoldGradientStart)
            }
            clipToOutline = true
        }

        playerView = PlayerView(this).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            isFocusable = true
            setOnClickListener { toggleFullscreen() }
        }
        playerContainer.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        col.addView(playerContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val epg = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(bgCardDark)
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), strokeBronze)
            }
        }

        epgTitle = TextView(this).apply {
            text = "اختر قناة لبدء العرض"
            textSize = 14f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        epg.addView(epgTitle)

        epgSub = TextView(this).apply {
            text = "Audio: English / Arabic               يعرض الآن: بث مباشر القناة"
            textSize = 10f
            setTextColor(textMuted)
            setPadding(0, dp(4), 0, dp(6))
        }
        epg.addView(epgSub)

        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progress = 65
            progressDrawable.setTint(goldText)
        }
        epg.addView(pb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        listOf("📊 Match Stats", "🔄 Switch Angle", "🔔 Fetch Stat", "👁 Switch Angle").forEach { txt ->
            val btn = TextView(this).apply {
                text = txt
                textSize = 9f
                setTextColor(textWhite)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1F222A"))
                    cornerRadius = dp(6).toFloat()
                }
            }
            actions.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
        epg.addView(actions)

        col.addView(epg, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        mainContent.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
    }

    private fun loadPackages() {
        packagesLayout.removeAllViews()
        val groups = channels.map { it.group }.distinct()

        groups.forEach { group ->
            val btn = FrameLayout(this).apply {
                background = createGoldBronzeDrawable(group == currentGroup)
                isFocusable = true
                setOnClickListener {
                    currentGroup = group
                    loadPackages()
                    loadChannelsGrid(group)
                }
            }

            val txt = TextView(this).apply {
                text = group
                textSize = 11f
                setTextColor(textWhite)
                setTypeface(Typeface.DEFAULT_BOLD)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            btn.addView(txt)

            packagesLayout.addView(btn, LinearLayout.LayoutParams(dp(165), dp(45)).apply {
                bottomMargin = dp(8)
            })
        }

        loadChannelsGrid(groups.first())
    }

    private fun loadChannelsGrid(group: String) {
        channelsGrid.removeAllViews()
        val filtered = channels.filter { it.group == group }

        filtered.forEachIndexed { _, channel ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(6), dp(6), dp(6), dp(6))
                background = GradientDrawable().apply {
                    setColor(bgCardDark)
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), strokeBronze)
                }
                isFocusable = true
                setOnClickListener {
                    playChannel(channel)
                }
            }

            val thumb = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1B202A"))
                    cornerRadius = dp(6).toFloat()
                }
            }

            card.addView(thumb, LinearLayout.LayoutParams(dp(135), dp(65)))

            val name = TextView(this).apply {
                text = channel.name
                textSize = 10f
                setTextColor(textWhite)
                setTypeface(Typeface.DEFAULT_BOLD)
                setPadding(0, dp(6), 0, 0)
                maxLines = 1
            }
            card.addView(name)

            val param = GridLayout.LayoutParams().apply {
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            channelsGrid.addView(card, param)
        }
    }

    private fun createGoldBronzeDrawable(isSelected: Boolean): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (isSelected) intArrayOf(bronzeGoldGradientStart, bronzeGoldGradientEnd)
            else intArrayOf(bgCardDark, bgCardDark)
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), if (isSelected) goldText else strokeBronze)
        }
    }

    private fun playChannel(channel: Channel) {
        epgTitle.text = channel.name
        try {
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(this).build()
                playerView.player = exoPlayer
            }
            exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)))
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في البث", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen
        mainContent.getChildAt(0).visibility = if (fullscreen) View.GONE else View.VISIBLE
        mainContent.getChildAt(1).visibility = if (fullscreen) View.GONE else View.VISIBLE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
