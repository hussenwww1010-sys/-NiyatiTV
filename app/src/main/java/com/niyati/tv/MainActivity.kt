Package com.niyati.tv

import android.app.Activity
import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentSelectedChannel: Channel? = null

    private val visibleChannels = mutableListOf<Channel>()
    private val channelButtons = mutableListOf<View>()
    private val packageButtons = mutableListOf<View>()

    private lateinit var root: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var playerColumn: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var epgContainer: LinearLayout
    private lateinit var epgTitle: TextView
    private lateinit var epgSub: TextView
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsLayout: LinearLayout

    // UI Colors
    private val bgPrimary = Color.parseColor("#090C10")
    private val bgSecondary = Color.parseColor("#0D1117")
    private val bgCard = Color.parseColor("#161B22")
    private val accentColor = Color.parseColor("#00E5FF") // Turquoise Accent
    private val accentHover = Color.parseColor("#1F2937")
    private val telegramBlue = Color.parseColor("#24A1DE") // Official Telegram Color

    private val textWhite = Color.WHITE
    private val textMuted = Color.parseColor("#8B949E")
    private val statusGreen = Color.parseColor("#00E676")
    private val strokeColor = Color.parseColor("#21262D")

    private val telegramUrl = "https://t.me/NAITI_Tv"

    private val base = "http://xxtv.me:8080/live/1219624801985519/2036793881828746/"

    private fun isTvDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun c(name: String, group: String, id: String): Channel {
        return Channel(name = name, group = group, url = "$base$id.ts")
    }

    private fun customChannel(name: String, group: String, url: String): Channel {
        return Channel(name = name, group = group, url = url)
    }

    private val channels = mutableListOf<Channel>().apply {
        // 1. BEIN SPORTS (الباقة الأولى)
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

        for (i in 1..9) add(c("beIN Sport $i HD", "BEIN SPORTS", "${325792 + i}"))

        add(c("beIN Sport 1 HD English", "BEIN SPORTS", "318217"))
        add(c("beIN Sport 2 HD English", "BEIN SPORTS", "318218"))
        add(c("beIN Sport 1 HD French", "BEIN SPORTS", "319437"))
        add(c("beIN Sport 2 HD French", "BEIN SPORTS", "319438"))
        add(c("beIN Sport NBA HD", "BEIN SPORTS", "318219"))

        for (i in 1..9) add(c("beIN Sport $i SD", "BEIN SPORTS", "${325802 + i}"))

        add(c("beIN Sport English 1 SD", "BEIN SPORTS", "319425"))
        add(c("beIN Sport English 2 SD", "BEIN SPORTS", "319426"))
        add(c("beIN Sport French 1 SD", "BEIN SPORTS", "319427"))
        add(c("beIN Sport French 2 SD", "BEIN SPORTS", "319428"))

        // 2. BEIN TOD (الباقة الثانية)
        add(c("beIN Tod 4K", "BEIN TOD", "460835"))
        for (i in 1..9) add(c("beIN Sport Tod $i", "BEIN TOD", "${460835 + i}"))
        add(c("beIN Sport Tod English 1", "BEIN TOD", "460845"))
        add(c("beIN Sport Tod English 2", "BEIN TOD", "460846"))
        for (i in 1..9) add(c("beIN Sport Tod Extra $i", "BEIN TOD", "${460846 + i}"))

        // 3. ALWAN SPORT (الباقة الثالثة)
        val alwan = listOf(418111, 418112, 418113, 418114, 418115, 418116, 418117, 418118, 418119, 418120, 418121, 418122, 418123, 418124, 418125, 418126, 418127, 418128)
        for (i in 1..6) {
            val x = (i - 1) * 3
            alwan.getOrNull(x)?.let { add(c("Alwan Sport $i 4K", "ALWAN SPORT", it.toString())) }
            alwan.getOrNull(x + 1)?.let { add(c("Alwan Sport $i HD", "ALWAN SPORT", it.toString())) }
            alwan.getOrNull(x + 2)?.let { add(c("Alwan Sport $i SD", "ALWAN SPORT", it.toString())) }
        }
        listOf(
            "Alwan Sport 7 4K" to "433739", "Alwan Sport 8 4K" to "433740",
            "Alwan Sport 9 4K" to "433741", "Alwan Sport 10 4K" to "433742"
        ).forEach { add(c(it.first, "ALWAN SPORT", it.second)) }

        // 4. BEIN XTRA
        val xtra4k = listOf(325790, 319487, 319488, 440569, 440570, 440571, 447243, 447244, 447245)
        val xtraHd = listOf(325812, 319435, 319436, 440572, 440573, 440574, 447246, 447247, 447248)
        val xtraSd = listOf(325822, 319423, 319424, 440575, 440576, 440577, 447249, 447250, 447251)

        for (i in 1..9) {
            xtra4k.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i 4K", "BEIN XTRA", it.toString())) }
            xtraHd.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i HD", "BEIN XTRA", it.toString())) }
            xtraSd.getOrNull(i - 1)?.let { add(c("beIN Sport XTRA $i SD", "BEIN XTRA", it.toString())) }
        }

        // 5. AL RABIAA
        listOf(
            "AL RABIAA SPORT 1" to "371931", "AL RABIAA SPORT 1+" to "371933",
            "AL RABIAA SPORT 2" to "371932", "Rabiaa Sport +2" to "434565",
            "AL RABIAA TV 4K" to "371939", "AL RABIAA MOVIES" to "371934",
            "Rabiaa Variety" to "434566", "Njoom Al Rabiaa" to "434567",
            "AL RABIAA SERIES" to "371935", "AL RABIAA GEO" to "371936",
            "AL RABIAA QURAN" to "371937", "AL RABIAA MUSICA" to "371938"
        ).forEach { add(c(it.first, "AL RABIAA", it.second)) }

        // 6. ALKASS
        val alkassIds = listOf(96214, 96215, 278068, 96216, 96217, 211523, 379828, 379829, 393991, 393992)
        for (i in 1..10) {
            alkassIds.getOrNull(i - 1)?.let { add(c("Alkass $i HD", "ALKASS", it.toString())) }
        }

        // 7. SAUDI SPORTS
        listOf(
            "KSA Sport 1 4K" to "97805", "KSA Sport 2 4K" to "97806",
            "KSA Sport 3 4K" to "97807", "SAUDUA NOW" to "97808",
            "SAUDI 24 SPORT HD" to "100470", "STC SPORT 1 HD" to "421391",
            "STC SPORT 2 HD" to "421392", "STC SPORT 3 HD" to "420903",
            "STC SPORT 4 HD" to "433178"
        ).forEach { add(c(it.first, "SAUDI SPORTS", it.second)) }

        // 8. AD SPORTS
        listOf(
            "AD SPORTS 1 HD" to "326053", "AD SPORTS 2 HD" to "326054",
            "AD Sport Asia 1 HD" to "244188", "AD Sport Asia 2 HD" to "244191"
        ).forEach { add(c(it.first, "AD SPORTS", it.second)) }

        // 9. GULF SPORTS
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
        ).forEach { add(c(it.first, "GULF SPORTS", it.second)) }

        // 10. SHAHID SPORT
        for (i in 1..5) add(c("Shahid Sport $i 4K", "SHAHID SPORT", "${430910 + i}"))

        // 11. FAJER TV
        listOf(
            "Fajer TV 1" to "463532", "Fajer TV 2" to "463533",
            "Fajer TV 3" to "463534", "Fajer TV 4" to "463535",
            "Fajer TV 5" to "463536"
        ).forEach { add(c(it.first, "FAJER TV", it.second)) }

        // 12. KURDISTAN SPORTS
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
        ).forEach { add(c(it.first, "KURDISTAN SPORTS", it.second)) }

        // 13. SHASHA
        listOf(
            "Shasha 1 TV 4K" to "348400", "Shasha 2 TV 4K" to "244079",
            "Shasha 3 TV 4K" to "443029"
        ).forEach { add(c(it.first, "SHASHA", it.second)) }

        // 14. DRAMA & MBC
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
        ).forEach { add(customChannel(it.first, "DRAMA & MBC", it.second)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bgPrimary
        window.navigationBarColor = bgPrimary

        buildInterface()
        showWelcomeDialog()
    }

    private fun showWelcomeDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            background = GradientDrawable().apply {
                setColor(bgCard)
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), strokeColor)
            }
        }

        val iconContainer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A00E676"))
                shape = GradientDrawable.OVAL
                setStroke(dp(2), statusGreen)
            }
        }

        val iconText = TextView(this).apply {
            text = "📺"
            textSize = 28f
            gravity = Gravity.CENTER
        }
        iconContainer.addView(iconText, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val iconParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
            bottomMargin = dp(16)
        }
        dialogView.addView(iconContainer, iconParams)

        val titleText = TextView(this).apply {
            text = "مرحباً بك في NAITI TV"
            textSize = 20f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
        }
        dialogView.addView(titleText)

        val subTitleText = TextView(this).apply {
            text = "استمتع بمشاهدة أحدث القنوات الرياضية والترفيهية بأعلى جودة وبث مباشر سلس بدون تقطيع!"
            textSize = 13f
            setTextColor(textMuted)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
        }
        val subParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(10), 0, dp(24))
        }
        dialogView.addView(subTitleText, subParams)

        val startBtn = TextView(this).apply {
            text = "ابدأ المشاهدة الآن"
            textSize = 14f
            setTextColor(bgPrimary)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = dp(16).toFloat()
            }
        }
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
            bottomMargin = dp(10)
        }
        dialogView.addView(startBtn, btnParams)

        val telegramBtn = TextView(this).apply {
            text = "الانضمام لقناة التليجرام ✈"
            textSize = 13f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = GradientDrawable().apply {
                setColor(bgSecondary)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#363B42"))
            }
        }
        dialogView.addView(telegramBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        startBtn.setOnClickListener {
            dialog.dismiss()
        }

        telegramBtn.setOnClickListener {
            openTelegramChannel()
            dialog.dismiss()
        }
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgPrimary)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        createTopBar()

        mainContent = LinearLayout(this).apply {
            orientation = if (isTvDevice()) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setBackgroundColor(bgPrimary)
        }

        if (isTvDevice()) {
            createPackageColumn()
            createChannelColumn()
            createPlayerColumn()
        } else {
            createPlayerColumn()
            createPackageColumn()
            createChannelColumn()
        }

        root.addView(mainContent, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        loadPackages()
    }

    private fun createTopBar() {
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(25), dp(10), dp(25), dp(10))
            setBackgroundColor(bgSecondary)
        }

        val logoBox = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = dp(12).toFloat()
            }
        }

        val logoText = TextView(this).apply {
            text = "NT"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(bgPrimary)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        logoBox.addView(logoText, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        topBar.addView(logoBox, LinearLayout.LayoutParams(dp(40), dp(40)))

        val brandText = TextView(this).apply {
            text = "  NAITI TV"
            textSize = 18f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        topBar.addView(brandText)

        val spacer = View(this)
        topBar.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))

        // Telegram Button
        val telegramBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isFocusable = true
            isFocusableInTouchMode = true
            background = GradientDrawable().apply {
                setColor(telegramBlue)
                cornerRadius = dp(20).toFloat()
            }
            setOnClickListener { openTelegramChannel() }
        }

        val tgIcon = TextView(this).apply {
            text = "✈ "
            textSize = 12f
            setTextColor(textWhite)
        }

        val tgText = TextView(this).apply {
            text = "Telegram"
            textSize = 11f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        telegramBtn.addView(tgIcon)
        telegramBtn.addView(tgText)

        val tgParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, dp(12), 0)
        }
        topBar.addView(telegramBtn, tgParams)

        // Live Badge
        val liveBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A00E676"))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), statusGreen)
            }
        }

        val liveDot = TextView(this).apply {
            text = "● "
            textSize = 10f
            setTextColor(statusGreen)
        }

        val liveText = TextView(this).apply {
            text = "LIVE"
            textSize = 11f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        liveBadge.addView(liveDot)
        liveBadge.addView(liveText)
        topBar.addView(liveBadge)

        root.addView(topBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)))
    }

    private fun openTelegramChannel() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح رابط التليجرام", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPackageColumn() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgSecondary)
        }

        col.addView(createColumnHeader("PACKAGES"))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isFocusable = false
        }

        packagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        scroll.addView(packagesLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val width = if (isTvDevice()) dp(220) else LinearLayout.LayoutParams.MATCH_PARENT
        val height = if (isTvDevice()) LinearLayout.LayoutParams.MATCH_PARENT else dp(130)
        mainContent.addView(col, LinearLayout.LayoutParams(width, height))
    }

    private fun createChannelColumn() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgSecondary)
        }

        col.addView(createColumnHeader("AVAILABLE CHANNELS"))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isFocusable = false
        }

        channelsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        scroll.addView(channelsLayout, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val width = if (isTvDevice()) dp(280) else LinearLayout.LayoutParams.MATCH_PARENT
        val height = if (isTvDevice()) LinearLayout.LayoutParams.MATCH_PARENT else 0
        val weight = if (isTvDevice()) 0f else 1f

        mainContent.addView(col, LinearLayout.LayoutParams(width, height, weight))
    }

    private fun createPlayerColumn() {
        playerColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
            setBackgroundColor(bgPrimary)
        }

        playerContainer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), strokeColor)
            }
            clipToOutline = true
        }

        playerView = PlayerView(this).apply {
            useController = false
            setBackgroundColor(Color.BLACK)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            setOnFocusChangeListener { _, hasFocus ->
                if (!fullscreen) {
                    playerContainer.background = GradientDrawable().apply {
                        setColor(Color.BLACK)
                        cornerRadius = dp(20).toFloat()
                        setStroke(dp(2), if (hasFocus) accentColor else strokeColor)
                    }
                }
            }

            setOnClickListener { toggleFullscreen() }
        }

        playerContainer.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val watermark = TextView(this).apply {
            text = "NAITI TV"
            textSize = 10f
            setTextColor(Color.parseColor("#80FFFFFF"))
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#40000000"))
                cornerRadius = dp(8).toFloat()
            }
        }

        val wmParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(dp(12), dp(12), 0, 0)
        }

        playerContainer.addView(watermark, wmParams)

        val playerHeight = if (isTvDevice()) 0 else dp(210)
        val playerWeight = if (isTvDevice()) 0.65f else 0f

        playerColumn.addView(playerContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, playerHeight, playerWeight))

        epgContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
            background = GradientDrawable().apply {
                setColor(bgSecondary)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), strokeColor)
            }
        }

        epgTitle = TextView(this).apply {
            text = "Select a channel to play"
            textSize = 15f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        epgContainer.addView(epgTitle)

        epgSub = TextView(this).apply {
            text = "Live Stream Ready"
            textSize = 11f
            setTextColor(textMuted)
            setPadding(0, dp(2), 0, dp(8))
        }
        epgContainer.addView(epgSub)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progress = 100
            progressDrawable.setTint(accentColor)
        }

        epgContainer.addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(4)))

        val epgHeight = if (isTvDevice()) 0 else LinearLayout.LayoutParams.WRAP_CONTENT
        val epgWeight = if (isTvDevice()) 0.35f else 0f

        playerColumn.addView(epgContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, epgHeight, epgWeight).apply {
            topMargin = dp(10)
        })

        val mainPlayerWidth = if (isTvDevice()) 0 else LinearLayout.LayoutParams.MATCH_PARENT
        val mainPlayerHeight = if (isTvDevice()) LinearLayout.LayoutParams.MATCH_PARENT else LinearLayout.LayoutParams.WRAP_CONTENT
        val mainPlayerWeight = if (isTvDevice()) 1f else 0f

        mainContent.addView(playerColumn, LinearLayout.LayoutParams(mainPlayerWidth, mainPlayerHeight, mainPlayerWeight))
    }

    private fun createColumnHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(textMuted)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(20), dp(14), dp(20), dp(10))
            background = GradientDrawable().apply { setColor(bgSecondary) }
        }
    }

    private fun loadPackages() {
        packagesLayout.removeAllViews()
        packageButtons.clear()

        val groups = channels.map { it.group }.distinct()
        if (groups.isEmpty()) return

        currentGroup = groups.first()

        groups.forEach { group ->
            val count = channels.count { it.group == group }
            val card = createPackageCard(group, count, group == currentGroup)

            card.setOnClickListener {
                currentGroup = group
                updatePackageSelection()
                loadChannels(group)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                view.background = createCardDrawable(hasFocus, currentGroup == group)
            }

            packagesLayout.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                bottomMargin = dp(8)
            })

            packageButtons.add(card)
        }

        loadChannels(groups.first())
    }

    private fun createPackageCard(name: String, count: Int, isSelected: Boolean): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            background = createCardDrawable(false, isSelected)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val nameTv = TextView(this).apply {
            text = packageDisplayName(name)
            textSize = 13f
            setTextColor(if (isSelected) textWhite else textMuted)
            setTypeface(Typeface.DEFAULT_BOLD)
        }

        layout.addView(nameTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val badge = TextView(this).apply {
            text = count.toString()
            textSize = 10f
            setTextColor(if (isSelected) textWhite else textMuted)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = GradientDrawable().apply {
                setColor(if (isSelected) Color.parseColor("#33FFFFFF") else Color.parseColor("#15FFFFFF"))
                cornerRadius = dp(12).toFloat()
            }
        }

        layout.addView(badge)
        return layout
    }

    private fun packageDisplayName(name: String): String {
        return when (name) {
            "BEIN SPORTS" -> "beIN SPORTS 🏆"
            "BEIN TOD" -> "beIN TOD ⚽"
            "ALWAN SPORT" -> "Alwan Sport 🎨"
            "BEIN XTRA" -> "beIN XTRA 🔥"
            "AL RABIAA" -> "Al Rabiaa Sport"
            "ALKASS" -> "Alkass Qatar"
            "SAUDI SPORTS" -> "Saudi Sports"
            "AD SPORTS" -> "Abu Dhabi Sports"
            "GULF SPORTS" -> "Gulf Sports"
            "SHAHID SPORT" -> "Shahid Sport"
            "FAJER TV" -> "Fajer TV"
            "KURDISTAN SPORTS" -> "Kurdistan Sports"
            "SHASHA" -> "Shasha TV"
            "DRAMA & MBC" -> "Drama & MBC 🎬"
            else -> name
        }
    }

    private fun updatePackageSelection() {
        val groups = channels.map { it.group }.distinct()
        for (i in 0 until packagesLayout.childCount) {
            val child = packagesLayout.getChildAt(i) as? LinearLayout ?: continue
            val isSelected = groups.getOrNull(i) == currentGroup
            child.background = createCardDrawable(child.hasFocus(), isSelected)

            val tv = child.getChildAt(0) as? TextView
            tv?.setTextColor(if (isSelected) textWhite else textMuted)

            val badge = child.getChildAt(1) as? TextView
            badge?.setTextColor(if (isSelected) textWhite else textMuted)
            badge?.background = GradientDrawable().apply {
                setColor(if (isSelected) Color.parseColor("#33FFFFFF") else Color.parseColor("#15FFFFFF"))
                cornerRadius = dp(12).toFloat()
            }
        }
    }

    private fun loadChannels(group: String) {
        channelsLayout.removeAllViews()
        channelButtons.clear()
        visibleChannels.clear()
        currentChannelIndex = -1

        val filtered = channels.filter { it.group == group }
        visibleChannels.addAll(filtered)

        filtered.forEachIndexed { index, channel ->
            val card = createChannelCard(channel, index == currentChannelIndex)

            card.setOnClickListener {
                currentChannelIndex = index
                updateChannelSelection(card)
                playChannel(channel)
            }

            card.setOnFocusChangeListener { view, hasFocus ->
                view.background = createCardDrawable(hasFocus, currentChannelIndex == index)
            }

            channelsLayout.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply {
                bottomMargin = dp(8)
            })

            channelButtons.add(card)
        }
    }

    private fun createChannelCard(channel: Channel, isSelected: Boolean): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            background = createCardDrawable(false, isSelected)
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val iconBox = TextView(this).apply {
            text = "TV"
            textSize = 9f
            setTextColor(if (isSelected) bgPrimary else accentColor)
            gravity = Gravity.CENTER
            setTypeface(Typeface.DEFAULT_BOLD)
            background = GradientDrawable().apply {
                setColor(if (isSelected) textWhite else bgPrimary)
                cornerRadius = dp(12).toFloat()
            }
        }

        card.addView(iconBox, LinearLayout.LayoutParams(dp(32), dp(32)))

        val name = TextView(this).apply {
            text = "  ${channel.name}"
            textSize = 12f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
            maxLines = 1
        }

        card.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val quality = TextView(this).apply {
            text = when {
                channel.name.contains("4K", true) -> "4K"
                channel.name.contains("FHD", true) -> "FHD"
                channel.name.contains("HD", true) -> "HD"
                else -> "SD"
            }
            textSize = 8f
            setTextColor(if (isSelected) bgPrimary else accentColor)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = GradientDrawable().apply {
                setColor(if (isSelected) textWhite else Color.parseColor("#1500E5FF"))
                cornerRadius = dp(8).toFloat()
            }
        }

        card.addView(quality)
        return card
    }

    private fun updateChannelSelection(selectedView: View) {
        for (i in 0 until channelsLayout.childCount) {
            val child = channelsLayout.getChildAt(i) as? LinearLayout ?: continue
            val isSelected = (i == currentChannelIndex)
            child.background = createCardDrawable(child.hasFocus(), isSelected)

            val iconBox = child.getChildAt(0) as? TextView
            iconBox?.setTextColor(if (isSelected) bgPrimary else accentColor)
            iconBox?.background = GradientDrawable().apply {
                setColor(if (isSelected) textWhite else bgPrimary)
                cornerRadius = dp(12).toFloat()
            }

            val quality = child.getChildAt(2) as? TextView
            quality?.setTextColor(if (isSelected) bgPrimary else accentColor)
            quality?.background = GradientDrawable().apply {
                setColor(if (isSelected) textWhite else Color.parseColor("#1500E5FF"))
                cornerRadius = dp(8).toFloat()
            }
        }
    }

    private fun createCardDrawable(hasFocus: Boolean, isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            if (hasFocus) {
                setColor(accentHover)
                setStroke(dp(2), accentColor)
            } else if (isSelected) {
                setColor(accentColor)
                setStroke(dp(0), Color.TRANSPARENT)
            } else {
                setColor(bgCard)
                setStroke(dp(1), Color.TRANSPARENT)
            }
        }
    }

    private fun initExoPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(this@MainActivity, "تعذر تشغيل هذه القناة حالياً", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            playerView.player = exoPlayer
        }
    }

    private fun playChannel(channel: Channel) {
        currentSelectedChannel = channel
        epgTitle.text = channel.name
        epgSub.text = "Now Playing: Live Broadcast"

        try {
            initExoPlayer()

            val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في تشغيل القناة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFullscreen() {
        if (fullscreen) exitFullscreen() else enterFullscreen()
    }

    private fun enterFullscreen() {
        if (fullscreen) return
        fullscreen = true

        topBar.visibility = View.GONE

        if (isTvDevice()) {
            mainContent.getChildAt(0).visibility = View.GONE
            mainContent.getChildAt(1).visibility = View.GONE
        } else {
            mainContent.getChildAt(1).visibility = View.GONE
            mainContent.getChildAt(2).visibility = View.GONE
        }

        epgContainer.visibility = View.GONE

        playerColumn.setPadding(0, 0, 0, 0)
        playerContainer.background = null

        val params = playerContainer.layoutParams as LinearLayout.LayoutParams
        params.height = LinearLayout.LayoutParams.MATCH_PARENT
        params.weight = 1f
        playerContainer.layoutParams = params

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        playerView.requestFocus()
    }

    private fun exitFullscreen() {
        if (!fullscreen) return
        fullscreen = false

        topBar.visibility = View.VISIBLE

        if (isTvDevice()) {
            mainContent.getChildAt(0).visibility = View.VISIBLE
            mainContent.getChildAt(1).visibility = View.VISIBLE
        } else {
            mainContent.getChildAt(1).visibility = View.VISIBLE
            mainContent.getChildAt(2).visibility = View.VISIBLE
        }

        epgContainer.visibility = View.VISIBLE

        playerColumn.setPadding(dp(15), dp(10), dp(15), dp(10))
        playerContainer.background = GradientDrawable().apply {
            setColor(Color.BLACK)
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), strokeColor)
        }

        val params = playerContainer.layoutParams as LinearLayout.LayoutParams
        params.height = if (isTvDevice()) 0 else dp(210)
        params.weight = if (isTvDevice()) 0.65f else 0f
        playerContainer.layoutParams = params

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
                KeyEvent.KEYCODE_BACK -> {
                    if (fullscreen) {
                        exitFullscreen()
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

    override fun onStart() {
        super.onStart()
        currentSelectedChannel?.let { playChannel(it) }
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
