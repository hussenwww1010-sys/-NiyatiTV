package com.niyati.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    // ==========================================================
    // PLAYER
    // ==========================================================

    private var exoPlayer: ExoPlayer? = null

    private var fullscreen = false

    private var currentGroup = ""

    private var currentChannelIndex = -1

    private var currentSelectedChannel: Channel? = null

    // 0 = FIT
    // 1 = CROP
    // 2 = FILL
    private var zoomMode = 0

    // ==========================================================
    // CHANNEL DATA
    // ==========================================================

    private val visibleChannels =
        mutableListOf<Channel>()

    private val channelButtons =
        mutableListOf<View>()

    private val packageButtons =
        mutableListOf<View>()

    // ==========================================================
    // UI
    // ==========================================================

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

    // ==========================================================
    // PLAYER OVERLAYS
    // ==========================================================

    private lateinit var playerControls: LinearLayout

    private lateinit var zoomButton: TextView

    private lateinit var fullscreenButton: TextView

    private lateinit var channelInfoOverlay: LinearLayout

    private lateinit var overlayChannelName: TextView

    private lateinit var overlayChannelStatus: TextView

    // ==========================================================
    // HANDLER
    // ==========================================================

    private val overlayHandler =
        Handler(Looper.getMainLooper())

    private val hideOverlayRunnable =
        Runnable {

            if (::channelInfoOverlay.isInitialized) {

                channelInfoOverlay.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction {

                        channelInfoOverlay.visibility =
                            View.GONE

                    }
                    .start()
            }
        }

    // ==========================================================
    // COLORS
    // ==========================================================

    private val bgPrimary =
        Color.parseColor("#070A0F")

    private val bgSecondary =
        Color.parseColor("#0C1119")

    private val bgCard =
        Color.parseColor("#121A26")

    private val accentColor =
        Color.parseColor("#00E5FF")

    private val accentBlue =
        Color.parseColor("#0284C7")

    private val accentPurple =
        Color.parseColor("#7C3AED")

    private val accentHover =
        Color.parseColor("#182638")

    private val telegramBlue =
        Color.parseColor("#24A1DE")

    private val textWhite =
        Color.WHITE

    private val textMuted =
        Color.parseColor("#8B949E")

    private val statusGreen =
        Color.parseColor("#00E676")

    private val strokeColor =
        Color.parseColor("#202B3A")

    private val telegramUrl =
        "https://t.me/NAITI_Tv"

    // ==========================================================
    // CHANNELS
    // ==========================================================

    private val channels =
        mutableListOf<Channel>().apply {

            // ==================================================
            // BEIN SPORTS SOURCE 1
            // ==================================================

            val beinSource1 =
                "┃AR┃ BEIN SPORTS HD"

            add(
                Channel(
                    "┃AR┃ beIN SPORT 1 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 2 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330438&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 3 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411381&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 4 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411380&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 5 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411379&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 6 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411378&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 7 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411377&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 8 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411376&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 9 HD",
                    beinSource1,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411375&extension=ts"
                )
            )

            // ==================================================
            // BEIN SPORTS SOURCE 2
            // ==================================================

            val beinSource2 =
                "┃AR┃ BEIN SPORTS HD 2"

            add(
                Channel(
                    "┃AR┃ beIN SPORT 1 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660413&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 2 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660411&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 3 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660409&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 4 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660407&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 5 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660405&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 6 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660403&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 7 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660401&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 8 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660399&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ beIN SPORT 9 HD",
                    beinSource2,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660397&extension=ts"
                )
            )

            // ==================================================
            // ALWAN SPORTS
            // ==================================================

            val alwan =
                "┃AR┃ ALWAN SPORTS"

            add(
                Channel(
                    "┃AR┃ ALWAN SPORT 1 HD",
                    alwan,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859098&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALWAN SPORT 2 HD",
                    alwan,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859097&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALWAN SPORT 3 HD",
                    alwan,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859096&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALWAN SPORT 4 HD",
                    alwan,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859095&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALWAN SPORT 5 HD",
                    alwan,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859094&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALWAN SPORT 6 HD",
                    alwan,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859093&extension=ts"
                )
            )

            // ==================================================
            // THAMANYA
            // ==================================================

            val thamanya =
                "┃AR┃ THAMANYA"

            add(
                Channel(
                    "┃AR┃ THAMANYA 1 HD",
                    thamanya,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936356&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ THAMANYA 2 HD",
                    thamanya,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936355&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ THAMANYA 3 HD",
                    thamanya,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936354&extension=ts"
                )
            )

            // ==================================================
            // ALKASS
            // ==================================================

            val alkass =
                "┃AR┃ ALKASS SPORTS"

            add(
                Channel(
                    "┃AR┃ ALKASS SPORT 1 HD",
                    alkass,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591593&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALKASS SPORT 2 HD",
                    alkass,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591591&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALKASS SPORT 3 HD",
                    alkass,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787903&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALKASS SPORT 4 HD",
                    alkass,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591589&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALKASS SPORT 5 HD",
                    alkass,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591587&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ ALKASS SPORT 6 HD",
                    alkass,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787906&extension=ts"
                )
            )

            // ==================================================
            // AD SPORTS
            // ==================================================

            val adSport =
                "┃AR┃ AD SPORTS"

            add(
                Channel(
                    "┃AR┃ AD SPORT 1 HD",
                    adSport,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993336&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ AD SPORT 2 HD",
                    adSport,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993337&extension=ts"
                )
            )

            // ==================================================
            // DUBAI SPORTS
            // ==================================================

            val dubaiSport =
                "┃AR┃ DUBAI SPORTS"

            add(
                Channel(
                    "┃AR┃ DUBAI SPORT 1 HD",
                    dubaiSport,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8086&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ DUBAI SPORT 2 HD",
                    dubaiSport,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=84251&extension=ts"
                )
            )

            add(
                Channel(
                    "┃AR┃ DUBAI SPORT 3 HD",
                    dubaiSport,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591579&extension=ts"
                )
            )

            // ==================================================
            // IRAQIA
            // ==================================================

            val iraqiaSport =
                "┃AR┃ IRAQIA SPORTS"

            add(
                Channel(
                    "┃AR┃ IRAQIA SPORT HD",
                    iraqiaSport,
                    "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"
                )
            )
        }

    // ==========================================================
    // ON CREATE
    // ==========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.statusBarColor =
            bgPrimary

        window.navigationBarColor =
            bgPrimary

        buildInterface()

        showWelcomeDialog()
    }

    // ==========================================================
    // WELCOME
    // ==========================================================

    private fun showWelcomeDialog() {

        val builder =
            AlertDialog.Builder(this)

        builder.setTitle(
            "أهلاً بك في تطبيق NAITI TV 📺"
        )

        builder.setMessage(
            "استمتع بمشاهدة أحدث القنوات الرياضية والترفيهية بأعلى جودة وبث مباشر سلس بدون تقطيع!\n\n" +
                    "يمكنك الانضمام إلى قناتنا على التليجرام لمتابعة التحديثات والدعم الفني."
        )

        builder.setPositiveButton(
            "ابدأ المشاهدة"
        ) { dialog, _ ->

            dialog.dismiss()
        }

        builder.setNeutralButton(
            "قناة التليجرام"
        ) { _, _ ->

            openTelegramChannel()
        }

        val dialog =
            builder.create()

        dialog.show()
    }

    // ==========================================================
    // BUILD INTERFACE
    // ==========================================================

    private fun buildInterface() {

        root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    bgPrimary
                )

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR
            }

        createTopBar()

        mainContent =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                setBackgroundColor(
                    bgPrimary
                )
            }

        createPackageColumn()

        createChannelColumn()

        createPlayerColumn()

        root.addView(
            mainContent,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        loadPackages()
    }

    // ==========================================================
    // TOP BAR
    // ==========================================================

    private fun createTopBar() {

        topBar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(25),
                    dp(10),
                    dp(25),
                    dp(10)
                )

                setBackgroundColor(
                    bgSecondary
                )
            }

        // ======================================================
        // BRAND — NO IMAGE LOGO
        // ======================================================

        val brandText =
            TextView(this).apply {

                text =
                    "NAITI TV"

                textSize =
                    19f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                letterSpacing =
                    0.08f
            }

        topBar.addView(
            brandText
        )

        val spacer =
            View(this)

        topBar.addView(
            spacer,
            LinearLayout.LayoutParams(
                0,
                1,
                1f
            )
        )

        // ======================================================
        // TELEGRAM
        // ======================================================

        val telegramBtn =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(12),
                    dp(7),
                    dp(12),
                    dp(7)
                )

                isFocusable = true

                isFocusableInTouchMode =
                    true

                background =
                    GradientDrawable().apply {

                        setColor(
                            telegramBlue
                        )

                        cornerRadius =
                            dp(20).toFloat()
                    }

                setOnClickListener {

                    openTelegramChannel()
                }
            }

        val tgIcon =
            TextView(this).apply {

                text =
                    "✈"

                textSize =
                    12f

                setTextColor(
                    textWhite
                )
            }

        val tgText =
            TextView(this).apply {

                text =
                    "  Telegram"

                textSize =
                    11f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )
            }

        telegramBtn.addView(
            tgIcon
        )

        telegramBtn.addView(
            tgText
        )

        topBar.addView(
            telegramBtn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // ======================================================
        // LIVE BADGE
        // ======================================================

        val liveBadge =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(14),
                    dp(7),
                    dp(14),
                    dp(7)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.parseColor(
                                "#1400E676"
                            )
                        )

                        cornerRadius =
                            dp(20).toFloat()

                        setStroke(
                            dp(1),
                            statusGreen
                        )
                    }
            }

        val liveDot =
            TextView(this).apply {

                text =
                    "● "

                textSize =
                    9f

                setTextColor(
                    statusGreen
                )
            }

        val liveText =
            TextView(this).apply {

                text =
                    "LIVE"

                textSize =
                    10f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )
            }

        liveBadge.addView(
            liveDot
        )

        liveBadge.addView(
            liveText
        )

        val liveParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                leftMargin =
                    dp(12)
            }

        topBar.addView(
            liveBadge,
            liveParams
        )

        root.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )
    }

    // ==========================================================
    // TELEGRAM
    // ==========================================================

    private fun openTelegramChannel() {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        telegramUrl
                    )
                )

            startActivity(
                intent
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "تعذر فتح رابط التليجرام",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==========================================================
    // PACKAGE COLUMN
    // ==========================================================

    private fun createPackageColumn() {

        val col =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    bgSecondary
                )
            }

        col.addView(
            createColumnHeader(
                "PACKAGES"
            )
        )

        val scroll =
            ScrollView(this).apply {

                isVerticalScrollBarEnabled =
                    false

                isFocusable =
                    false
            }

        packagesLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )
            }

        scroll.addView(
            packagesLayout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        col.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        mainContent.addView(
            col,
            LinearLayout.LayoutParams(
                dp(220),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    // ==========================================================
    // CHANNEL COLUMN
    // ==========================================================

    private fun createChannelColumn() {

        val col =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    bgSecondary
                )
            }

        col.addView(
            createColumnHeader(
                "AVAILABLE CHANNELS"
            )
        )

        val scroll =
            ScrollView(this).apply {

                isVerticalScrollBarEnabled =
                    false

                isFocusable =
                    false
            }

        channelsLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )
            }

        scroll.addView(
            channelsLayout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        col.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        mainContent.addView(
            col,
            LinearLayout.LayoutParams(
                dp(280),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    // ==========================================================
    // PLAYER COLUMN
    // ==========================================================

    private fun createPlayerColumn() {

        playerColumn =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(15),
                    dp(20),
                    dp(20)
                )

                setBackgroundColor(
                    bgPrimary
                )
            }

        // ======================================================
        // PLAYER CONTAINER
        // ======================================================

        playerContainer =
            FrameLayout(this).apply {

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.BLACK
                        )

                        cornerRadius =
                            dp(20).toFloat()

                        setStroke(
                            dp(1),
                            strokeColor
                        )
                    }

                clipToOutline =
                    true
            }

        // ======================================================
        // PLAYER VIEW
        // ======================================================

        playerView =
            PlayerView(this).apply {

                useController =
                    false

                setBackgroundColor(
                    Color.BLACK
                )

                resizeMode =
                    AspectRatioFrameLayout
                        .RESIZE_MODE_FIT

                isFocusable =
                    true

                isFocusableInTouchMode =
                    true

                isClickable =
                    true

                setOnFocusChangeListener {
                        _,
                        hasFocus ->

                    if (!fullscreen) {

                        playerContainer.background =
                            GradientDrawable().apply {

                                setColor(
                                    Color.BLACK
                                )

                                cornerRadius =
                                    dp(20).toFloat()

                                setStroke(
                                    dp(2),
                                    if (hasFocus)
                                        accentColor
                                    else
                                        strokeColor
                                )
                            }
                    }
                }

                setOnClickListener {

                    toggleFullscreen()
                }
            }

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ======================================================
        // WATERMARK
        // ======================================================

        val watermark =
            TextView(this).apply {

                text =
                    "NAITI TV"

                textSize =
                    10f

                setTextColor(
                    Color.parseColor(
                        "#90FFFFFF"
                    )
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                setPadding(
                    dp(10),
                    dp(5),
                    dp(10),
                    dp(5)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.parseColor(
                                "#40000000"
                            )
                        )

                        cornerRadius =
                            dp(8).toFloat()
                    }
            }

        val wmParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.START

                setMargins(
                    dp(12),
                    dp(12),
                    0,
                    0
                )
            }

        playerContainer.addView(
            watermark,
            wmParams
        )

        // ======================================================
        // PLAYER CONTROL BAR
        // ======================================================

        createPlayerControls()

        // ======================================================
        // CHANNEL INFORMATION OVERLAY
        // ======================================================

        createChannelInfoOverlay()

        // ======================================================
        // PLAYER SIZE
        // ======================================================

        playerColumn.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.65f
            )
        )

        // ======================================================
        // INFO PANEL
        // ======================================================

        epgContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(16),
                    dp(20),
                    dp(16)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            bgSecondary
                        )

                        cornerRadius =
                            dp(20).toFloat()

                        setStroke(
                            dp(1),
                            strokeColor
                        )
                    }
            }

        epgTitle =
            TextView(this).apply {

                text =
                    "Select a channel to play"

                textSize =
                    16f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )
            }

        epgContainer.addView(
            epgTitle
        )

        epgSub =
            TextView(this).apply {

                text =
                    "Live Stream Ready"

                textSize =
                    12f

                setTextColor(
                    textMuted
                )

                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(12)
                )
            }

        epgContainer.addView(
            epgSub
        )

        val progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {

                progress =
                    100

                progressDrawable.setTint(
                    accentColor
                )
            }

        epgContainer.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(6)
            )
        )

        playerColumn.addView(
            epgContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.35f
            ).apply {

                topMargin =
                    dp(15)
            }
        )

        mainContent.addView(
            playerColumn,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )
    }

    // ==========================================================
    // PLAYER CONTROLS
    // ==========================================================

    private fun createPlayerControls() {

        playerControls =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.parseColor(
                                "#CC080D14"
                            )
                        )

                        cornerRadius =
                            dp(18).toFloat()

                        setStroke(
                            dp(1),
                            Color.parseColor(
                                "#4000E5FF"
                            )
                        )
                    }
            }

        // ======================================================
        // ZOOM BUTTON
        // ======================================================

        zoomButton =
            createPlayerButton(
                "⛶",
                "FIT"
            )

        zoomButton.setOnClickListener {

            cycleZoomMode()
        }

        playerControls.addView(
            zoomButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(42)
            ).apply {

                rightMargin =
                    dp(8)
            }
        )

        // ======================================================
        // FULLSCREEN BUTTON
        // ======================================================

        fullscreenButton =
            createPlayerButton(
                "⛶",
                "FULL"
            )

        fullscreenButton.setOnClickListener {

            toggleFullscreen()
        }

        playerControls.addView(
            fullscreenButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(42)
            )
        )

        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.BOTTOM or Gravity.END

                setMargins(
                    0,
                    0,
                    dp(18),
                    dp(18)
                )
            }

        playerContainer.addView(
            playerControls,
            params
        )
    }

    // ==========================================================
    // PLAYER BUTTON
    // ==========================================================

    private fun createPlayerButton(
        icon: String,
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text =
                "$icon\n$text"

            textSize =
                8f

            gravity =
                Gravity.CENTER

            setTextColor(
                textWhite
            )

            setTypeface(
                Typeface.DEFAULT_BOLD
            )

            isClickable =
                true

            isFocusable =
                false

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.parseColor(
                            "#33253646"
                        )
                    )

                    cornerRadius =
                        dp(13).toFloat()

                    setStroke(
                        dp(1),
                        Color.parseColor(
                            "#5500E5FF"
                        )
                    )
                }
        }
    }

    // ==========================================================
    // ZOOM
    // ==========================================================

    private fun cycleZoomMode() {

        zoomMode++

        if (zoomMode > 2)
            zoomMode = 0

        when (zoomMode) {

            0 -> {

                playerView.resizeMode =
                    AspectRatioFrameLayout
                        .RESIZE_MODE_FIT

                zoomButton.text =
                    "⛶\nFIT"
            }

            1 -> {

                playerView.resizeMode =
                    AspectRatioFrameLayout
                        .RESIZE_MODE_ZOOM

                zoomButton.text =
                    "🔍\nCROP"
            }

            2 -> {

                playerView.resizeMode =
                    AspectRatioFrameLayout
                        .RESIZE_MODE_FILL

                zoomButton.text =
                    "↔\nFILL"
            }
        }
    }

    // ==========================================================
    // CHANNEL INFO OVERLAY
    // ==========================================================

    private fun createChannelInfoOverlay() {

        channelInfoOverlay =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(18),
                    dp(12),
                    dp(22),
                    dp(12)
                )

                alpha =
                    0f

                visibility =
                    View.GONE

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.parseColor(
                                "#E60A1018"
                            )
                        )

                        cornerRadius =
                            dp(18).toFloat()

                        setStroke(
                            dp(1),
                            Color.parseColor(
                                "#9900E5FF"
                            )
                        )
                    }
            }

        overlayChannelName =
            TextView(this).apply {

                text =
                    ""

                textSize =
                    17f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                maxLines =
                    1
            }

        channelInfoOverlay.addView(
            overlayChannelName
        )

        overlayChannelStatus =
            TextView(this).apply {

                text =
                    "●  LIVE"

                textSize =
                    10f

                setTextColor(
                    statusGreen
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        channelInfoOverlay.addView(
            overlayChannelStatus
        )

        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.CENTER

            }

        playerContainer.addView(
            channelInfoOverlay,
            params
        )
    }

    // ==========================================================
    // SHOW CHANNEL INFO
    // ==========================================================

    private fun showChannelInfo(
        channel: Channel
    ) {

        overlayHandler.removeCallbacks(
            hideOverlayRunnable
        )

        overlayChannelName.text =
            cleanChannelName(
                channel.name
            )

        overlayChannelStatus.text =
            "●  LIVE  •  ${packageDisplayName(channel.group)}"

        channelInfoOverlay.visibility =
            View.VISIBLE

        channelInfoOverlay.alpha =
            0f

        channelInfoOverlay.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        overlayHandler.postDelayed(
            hideOverlayRunnable,
            3000
        )
    }

    // ==========================================================
    // CLEAN CHANNEL NAME
    // ==========================================================

    private fun cleanChannelName(
        name: String
    ): String {

        return name
            .replace("┃AR┃", "")
            .trim()
    }

    // ==========================================================
    // COLUMN HEADER
    // ==========================================================

    private fun createColumnHeader(
        title: String
    ): TextView {

        return TextView(this).apply {

            text =
                title

            textSize =
                13f

            setTextColor(
                textMuted
            )

            setTypeface(
                Typeface.DEFAULT_BOLD
            )

            setPadding(
                dp(20),
                dp(18),
                dp(20),
                dp(12)
            )

            letterSpacing =
                0.04f

            background =
                GradientDrawable().apply {

                    setColor(
                        bgSecondary
                    )
                }
        }
    }

    // ==========================================================
    // LOAD PACKAGES
    // ==========================================================

    private fun loadPackages() {

        packagesLayout.removeAllViews()

        packageButtons.clear()

        val groups =
            channels.map {
                it.group
            }.distinct()

        if (groups.isEmpty())
            return

        currentGroup =
            groups.first()

        groups.forEach { group ->

            val count =
                channels.count {
                    it.group == group
                }

            val card =
                createPackageCard(
                    group,
                    count,
                    group == currentGroup
                )

            card.setOnClickListener {

                currentGroup =
                    group

                updatePackageSelection()

                loadChannels(
                    group
                )
            }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                view.background =
                    createCardDrawable(
                        hasFocus,
                        currentGroup == group
                    )
            }

            packagesLayout.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
                ).apply {

                    bottomMargin =
                        dp(9)
                }
            )

            packageButtons.add(
                card
            )
        }

        loadChannels(
            groups.first()
        )
    }

    // ==========================================================
    // PACKAGE CARD
    // ==========================================================

    private fun createPackageCard(
        name: String,
        count: Int,
        isSelected: Boolean
    ): LinearLayout {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(16),
                    0,
                    dp(14),
                    0
                )

                background =
                    createCardDrawable(
                        false,
                        isSelected
                    )

                isFocusable =
                    true

                isFocusableInTouchMode =
                    true
            }

        val accentLine =
            View(this).apply {

                background =
                    GradientDrawable().apply {

                        setColor(
                            if (isSelected)
                                textWhite
                            else
                                accentColor
                        )

                        cornerRadius =
                            dp(4).toFloat()
                    }
            }

        layout.addView(
            accentLine,
            LinearLayout.LayoutParams(
                dp(4),
                dp(26)
            ).apply {

                rightMargin =
                    dp(10)
            }
        )

        val nameTv =
            TextView(this).apply {

                text =
                    packageDisplayName(
                        name
                    )

                textSize =
                    12f

                setTextColor(
                    if (isSelected)
                        bgPrimary
                    else
                        textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                maxLines =
                    1
            }

        layout.addView(
            nameTv,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val badge =
            TextView(this).apply {

                text =
                    count.toString()

                textSize =
                    9f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    if (isSelected)
                        bgPrimary
                    else
                        accentColor
                )

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            if (isSelected)
                                Color.WHITE
                            else
                                Color.parseColor(
                                    "#1400E5FF"
                                )
                        )

                        cornerRadius =
                            dp(12).toFloat()
                    }
            }

        layout.addView(
            badge
        )

        return layout
    }

    // ==========================================================
    // PACKAGE NAMES
    // ==========================================================

    private fun packageDisplayName(
        name: String
    ): String {

        return when (name) {

            "┃AR┃ BEIN SPORTS HD" ->
                "beIN SPORTS HD"

            "┃AR┃ BEIN SPORTS HD 2" ->
                "beIN SPORTS HD 2"

            "┃AR┃ ALWAN SPORTS" ->
                "ALWAN SPORTS"

            "┃AR┃ THAMANYA" ->
                "THAMANYA"

            "┃AR┃ ALKASS SPORTS" ->
                "ALKASS SPORTS"

            "┃AR┃ AD SPORTS" ->
                "AD SPORTS"

            "┃AR┃ DUBAI SPORTS" ->
                "DUBAI SPORTS"

            "┃AR┃ IRAQIA SPORTS" ->
                "IRAQIA SPORTS"

            else ->
                name
                    .replace("┃AR┃", "")
                    .trim()
        }
    }

    // ==========================================================
    // UPDATE PACKAGE
    // ==========================================================

    private fun updatePackageSelection() {

        val groups =
            channels.map {
                it.group
            }.distinct()

        for (
            i in 0 until packagesLayout.childCount
        ) {

            val child =
                packagesLayout
                    .getChildAt(i)
                    as? LinearLayout
                    ?: continue

            val isSelected =
                groups.getOrNull(i) ==
                        currentGroup

            child.background =
                createCardDrawable(
                    child.hasFocus(),
                    isSelected
                )

            val accentLine =
                child.getChildAt(0)

            if (accentLine != null) {

                accentLine.background =
                    GradientDrawable().apply {

                        setColor(
                            if (isSelected)
                                textWhite
                            else
                                accentColor
                        )

                        cornerRadius =
                            dp(4).toFloat()
                    }
            }

            val tv =
                child.getChildAt(1)
                    as? TextView

            tv?.setTextColor(
                if (isSelected)
                    bgPrimary
                else
                    textWhite
            )

            val badge =
                child.getChildAt(2)
                    as? TextView

            badge?.setTextColor(
                if (isSelected)
                    bgPrimary
                else
                    accentColor
            )

            badge?.background =
                GradientDrawable().apply {

                    setColor(
                        if (isSelected)
                            Color.WHITE
                        else
                            Color.parseColor(
                                "#1400E5FF"
                            )
                    )

                    cornerRadius =
                        dp(12).toFloat()
                }
        }
    }

    // ==========================================================
    // LOAD CHANNELS
    // ==========================================================

    private fun loadChannels(
        group: String
    ) {

        channelsLayout.removeAllViews()

        channelButtons.clear()

        visibleChannels.clear()

        currentChannelIndex =
            -1

        val filtered =
            channels.filter {
                it.group == group
            }

        visibleChannels.addAll(
            filtered
        )

        filtered.forEachIndexed {
                index,
                channel ->

            val card =
                createChannelCard(
                    channel,
                    false
                )

            card.setOnClickListener {

                currentChannelIndex =
                    index

                updateChannelSelection(
                    card
                )

                playChannel(
                    channel
                )
            }

            card.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                view.background =
                    createChannelDrawable(
                        hasFocus,
                        currentChannelIndex ==
                                index
                    )
            }

            channelsLayout.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54)
                ).apply {

                    bottomMargin =
                        dp(8)
                }
            )

            channelButtons.add(
                card
            )
        }
    }

    // ==========================================================
    // CHANNEL CARD
    // ==========================================================

    private fun createChannelCard(
        channel: Channel,
        isSelected: Boolean
    ): LinearLayout {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(12),
                    0,
                    dp(12),
                    0
                )

                background =
                    createChannelDrawable(
                        false,
                        isSelected
                    )

                isFocusable =
                    true

                isFocusableInTouchMode =
                    true
            }

        // ======================================================
        // TV ICON
        // ======================================================

        val iconBox =
            TextView(this).apply {

                text =
                    "TV"

                textSize =
                    8f

                setTextColor(
                    accentColor
                )

                gravity =
                    Gravity.CENTER

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.parseColor(
                                "#101923"
                            )
                        )

                        cornerRadius =
                            dp(12).toFloat()

                        setStroke(
                            dp(1),
                            Color.parseColor(
                                "#3000E5FF"
                            )
                        )
                    }
            }

        card.addView(
            iconBox,
            LinearLayout.LayoutParams(
                dp(34),
                dp(34)
            ).apply {

                rightMargin =
                    dp(9)
            }
        )

        // ======================================================
        // NAME
        // ======================================================

        val name =
            TextView(this).apply {

                text =
                    cleanChannelName(
                        channel.name
                    )

                textSize =
                    12f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                maxLines =
                    1

                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

        card.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // ======================================================
        // QUALITY
        // ======================================================

        val quality =
            TextView(this).apply {

                text =
                    getQuality(
                        channel.name
                    )

                textSize =
                    8f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    accentColor
                )

                setPadding(
                    dp(8),
                    dp(4),
                    dp(8),
                    dp(4)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.parseColor(
                                "#1400E5FF"
                            )
                        )

                        cornerRadius =
                            dp(9).toFloat()
                    }
            }

        card.addView(
            quality
        )

        return card
    }

    // ==========================================================
    // QUALITY
    // ==========================================================

    private fun getQuality(
        name: String
    ): String {

        return when {

            name.contains(
                "UHD",
                true
            ) ||
                    name.contains(
                        "4K",
                        true
                    ) ->
                "UHD"

            name.contains(
                "FHD",
                true
            ) ||
                    name.contains(
                        "1080",
                        true
                    ) ->
                "FHD"

            name.contains(
                "HD",
                true
            ) ->
                "HD"

            else ->
                "SD"
        }
    }

    // ==========================================================
    // CHANNEL DRAWABLE
    // ==========================================================

    private fun createChannelDrawable(
        hasFocus: Boolean,
        isSelected: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(27).toFloat()

            when {

                hasFocus -> {

                    setColor(
                        Color.parseColor(
                            "#162A3A"
                        )
                    )

                    setStroke(
                        dp(2),
                        accentColor
                    )
                }

                isSelected -> {

                    setColor(
                        Color.parseColor(
                            "#00D7F0"
                        )
                    )

                    setStroke(
                        dp(1),
                        Color.parseColor(
                            "#80FFFFFF"
                        )
                    )
                }

                else -> {

                    setColor(
                        bgCard
                    )

                    setStroke(
                        dp(1),
                        Color.parseColor(
                            "#152B3A"
                        )
                    )
                }
            }
        }
    }

    // ==========================================================
    // UPDATE CHANNEL SELECTION
    // ==========================================================

    private fun updateChannelSelection(
        selectedView: View
    ) {

        for (
            i in 0 until channelsLayout.childCount
        ) {

            val child =
                channelsLayout
                    .getChildAt(i)
                    as? LinearLayout
                    ?: continue

            val isSelected =
                i == currentChannelIndex

            child.background =
                createChannelDrawable(
                    child.hasFocus(),
                    isSelected
                )

            val iconBox =
                child.getChildAt(0)
                    as? TextView

            iconBox?.setTextColor(
                if (isSelected)
                    bgPrimary
                else
                    accentColor
            )

            iconBox?.background =
                GradientDrawable().apply {

                    setColor(
                        if (isSelected)
                            Color.WHITE
                        else
                            Color.parseColor(
                                "#101923"
                            )
                    )

                    cornerRadius =
                        dp(12).toFloat()

                    setStroke(
                        dp(1),
                        if (isSelected)
                            Color.WHITE
                        else
                            Color.parseColor(
                                "#3000E5FF"
                            )
                    )
                }

            val quality =
                child.getChildAt(2)
                    as? TextView

            quality?.setTextColor(
                if (isSelected)
                    bgPrimary
                else
                    accentColor
            )

            quality?.background =
                GradientDrawable().apply {

                    setColor(
                        if (isSelected)
                            Color.WHITE
                        else
                            Color.parseColor(
                                "#1400E5FF"
                            )
                    )

                    cornerRadius =
                        dp(9).toFloat()
                }
        }
    }

    // ==========================================================
    // PACKAGE DRAWABLE
    // ==========================================================

    private fun createCardDrawable(
        hasFocus: Boolean,
        isSelected: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(24).toFloat()

            when {

                hasFocus -> {

                    setColor(
                        accentHover
                    )

                    setStroke(
                        dp(2),
                        accentColor
                    )
                }

                isSelected -> {

                    setColor(
                        accentColor
                    )

                    setStroke(
                        dp(1),
                        Color.parseColor(
                            "#80FFFFFF"
                        )
                    )
                }

                else -> {

                    setColor(
                        bgCard
                    )

                    setStroke(
                        dp(1),
                        Color.parseColor(
                            "#152B3A"
                        )
                    )
                }
            }
        }
    }

    // ==========================================================
    // EXO PLAYER
    // ==========================================================

    private fun initExoPlayer() {

        if (exoPlayer == null) {

            exoPlayer =
                ExoPlayer.Builder(
                    this
                )
                    .build()
                    .apply {

                        addListener(
                            object :
                                Player.Listener {

                                override fun
                                onPlayerError(
                                    error:
                                    PlaybackException
                                ) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "تعذر تشغيل هذه القناة حالياً",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

            playerView.player =
                exoPlayer
        }
    }

    // ==========================================================
    // PLAY CHANNEL
    // ==========================================================

    private fun playChannel(
        channel: Channel
    ) {

        currentSelectedChannel =
            channel

        epgTitle.text =
            cleanChannelName(
                channel.name
            )

        epgSub.text =
            "●  Live Broadcast  •  ${getQuality(channel.name)}"

        showChannelInfo(
            channel
        )

        try {

            initExoPlayer()

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

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "خطأ في تشغيل القناة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==========================================================
    // NEXT CHANNEL
    // ==========================================================

    private fun nextChannel() {

        if (visibleChannels.isEmpty())
            return

        if (currentChannelIndex < 0) {

            currentChannelIndex =
                0

        } else {

            currentChannelIndex++

            if (
                currentChannelIndex >=
                visibleChannels.size
            ) {

                currentChannelIndex =
                    0
            }
        }

        val channel =
            visibleChannels[
                currentChannelIndex
            ]

        updateChannelSelection(
            channelButtons[
                currentChannelIndex
            ]
        )

        channelButtons
            .getOrNull(
                currentChannelIndex
            )
            ?.requestFocus()

        playChannel(
            channel
        )
    }

    // ==========================================================
    // PREVIOUS CHANNEL
    // ==========================================================

    private fun previousChannel() {

        if (visibleChannels.isEmpty())
            return

        if (currentChannelIndex < 0) {

            currentChannelIndex =
                visibleChannels.size - 1

        } else {

            currentChannelIndex--

            if (currentChannelIndex < 0) {

                currentChannelIndex =
                    visibleChannels.size - 1
            }
        }

        val channel =
            visibleChannels[
                currentChannelIndex
            ]

        updateChannelSelection(
            channelButtons[
                currentChannelIndex
            ]
        )

        channelButtons
            .getOrNull(
                currentChannelIndex
            )
            ?.requestFocus()

        playChannel(
            channel
        )
    }

    // ==========================================================
    // FULLSCREEN
    // ==========================================================

    private fun toggleFullscreen() {

        if (fullscreen)
            exitFullscreen()
        else
            enterFullscreen()
    }

    // ==========================================================
    // ENTER FULLSCREEN
    // ==========================================================

    private fun enterFullscreen() {

        if (fullscreen)
            return

        fullscreen =
            true

        topBar.visibility =
            View.GONE

        mainContent
            .getChildAt(0)
            .visibility =
            View.GONE

        mainContent
            .getChildAt(1)
            .visibility =
            View.GONE

        epgContainer.visibility =
            View.GONE

        playerColumn.setPadding(
            0,
            0,
            0,
            0
        )

        playerContainer.background =
            null

        val params =
            playerContainer
                .layoutParams
                as LinearLayout.LayoutParams

        params.height =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.weight =
            1f

        playerContainer.layoutParams =
            params

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {

            window.insetsController?.let {

                it.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                it.systemBarsBehavior =
                    WindowInsetsController
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            @Suppress(
                "DEPRECATION"
            )

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        playerView.requestFocus()
    }

    // ==========================================================
    // EXIT FULLSCREEN
    // ==========================================================

    private fun exitFullscreen() {

        if (!fullscreen)
            return

        fullscreen =
            false

        topBar.visibility =
            View.VISIBLE

        mainContent
            .getChildAt(0)
            .visibility =
            View.VISIBLE

        mainContent
            .getChildAt(1)
            .visibility =
            View.VISIBLE

        epgContainer.visibility =
            View.VISIBLE

        playerColumn.setPadding(
            dp(20),
            dp(15),
            dp(20),
            dp(20)
        )

        playerContainer.background =
            GradientDrawable().apply {

                setColor(
                    Color.BLACK
                )

                cornerRadius =
                    dp(20).toFloat()

                setStroke(
                    dp(1),
                    strokeColor
                )
            }

        val params =
            playerContainer
                .layoutParams
                as LinearLayout.LayoutParams

        params.height =
            0

        params.weight =
            0.65f

        playerContainer.layoutParams =
            params

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {

            window.insetsController?.show(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )

        } else {

            @Suppress(
                "DEPRECATION"
            )

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        if (
            currentChannelIndex >= 0
        ) {

            channelButtons
                .getOrNull(
                    currentChannelIndex
                )
                ?.requestFocus()

        } else {

            playerView.requestFocus()
        }
    }

    // ==========================================================
    // REMOTE CONTROL
    // ==========================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            when (
                event.keyCode
            ) {

                // ==================================================
                // BACK
                // ==================================================

                KeyEvent.KEYCODE_BACK -> {

                    if (fullscreen) {

                        exitFullscreen()

                        return true
                    }
                }

                // ==================================================
                // UP = PREVIOUS CHANNEL
                // ==================================================

                KeyEvent.KEYCODE_DPAD_UP -> {

                    if (
                        playerView.hasFocus()
                    ) {

                        previousChannel()

                        return true
                    }
                }

                // ==================================================
                // DOWN = NEXT CHANNEL
                // ==================================================

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    if (
                        playerView.hasFocus()
                    ) {

                        nextChannel()

                        return true
                    }
                }

                // ==================================================
                // CENTER / ENTER
                // ==================================================

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

        return super.dispatchKeyEvent(
            event
        )
    }

    // ==========================================================
    // LIFECYCLE
    // ==========================================================

    override fun onStart() {

        super.onStart()

        currentSelectedChannel?.let {

            playChannel(
                it
            )
        }
    }

    override fun onStop() {

        super.onStop()

        overlayHandler.removeCallbacks(
            hideOverlayRunnable
        )

        playerView.player =
            null

        exoPlayer?.release()

        exoPlayer =
            null
    }

    // ==========================================================
    // DP
    // ==========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}