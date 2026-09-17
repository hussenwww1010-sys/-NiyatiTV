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

    // ==========================================================
    // UI COLORS
    // ==========================================================

    private val bgPrimary = Color.parseColor("#090C10")
    private val bgSecondary = Color.parseColor("#0D1117")
    private val bgCard = Color.parseColor("#161B22")
    private val accentColor = Color.parseColor("#00E5FF")
    private val accentHover = Color.parseColor("#1F2937")
    private val telegramBlue = Color.parseColor("#24A1DE")

    private val textWhite = Color.WHITE
    private val textMuted = Color.parseColor("#8B949E")
    private val statusGreen = Color.parseColor("#00E676")
    private val strokeColor = Color.parseColor("#21262D")

    private val telegramUrl = "https://t.me/NAITI_Tv"

    // ==========================================================
    // CHANNELS — PLAY SPORTS 2
    // 39 CHANNELS
    // ==========================================================

    private val channels = mutableListOf<Channel>().apply {

        // ======================================================
        // BEIN SPORTS — SOURCE 1
        // ======================================================

        val beinSource1 = "┃AR┃ BEIN SPORTS HD"

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 1 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330437&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 2 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1330438&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 3 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411381&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 4 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411380&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 5 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411379&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 6 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411378&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 7 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411377&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 8 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411376&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 9 HD",
                group = beinSource1,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1411375&extension=ts"
            )
        )

        // ======================================================
        // BEIN SPORTS — SOURCE 2
        // ======================================================

        val beinSource2 = "┃AR┃ BEIN SPORTS HD 2"

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 1 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660413&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 2 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660411&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 3 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660409&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 4 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660407&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 5 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660405&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 6 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660403&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 7 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660401&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 8 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660399&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ beIN SPORT 9 HD",
                group = beinSource2,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1660397&extension=ts"
            )
        )

        // ======================================================
        // ALWAN SPORTS
        // ======================================================

        val alwan = "┃AR┃ ALWAN SPORTS"

        add(
            Channel(
                name = "┃AR┃ ALWAN SPORT 1 HD",
                group = alwan,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859098&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALWAN SPORT 2 HD",
                group = alwan,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859097&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALWAN SPORT 3 HD",
                group = alwan,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859096&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALWAN SPORT 4 HD",
                group = alwan,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859095&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALWAN SPORT 5 HD",
                group = alwan,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859094&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALWAN SPORT 6 HD",
                group = alwan,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1859093&extension=ts"
            )
        )

        // ======================================================
        // THAMANYA
        // ======================================================

        val thamanya = "┃AR┃ THAMANYA"

        add(
            Channel(
                name = "┃AR┃ THAMANYA 1 HD",
                group = thamanya,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936356&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ THAMANYA 2 HD",
                group = thamanya,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936355&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ THAMANYA 3 HD",
                group = thamanya,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=1936354&extension=ts"
            )
        )

        // ======================================================
        // ALKASS SPORTS
        // ======================================================

        val alkass = "┃AR┃ ALKASS SPORTS"

        add(
            Channel(
                name = "┃AR┃ ALKASS SPORT 1 HD",
                group = alkass,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591593&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALKASS SPORT 2 HD",
                group = alkass,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591591&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALKASS SPORT 3 HD",
                group = alkass,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787903&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALKASS SPORT 4 HD",
                group = alkass,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591589&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALKASS SPORT 5 HD",
                group = alkass,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591587&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ ALKASS SPORT 6 HD",
                group = alkass,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=787906&extension=ts"
            )
        )

        // ======================================================
        // AD SPORTS
        // ======================================================

        val adSport = "┃AR┃ AD SPORTS"

        add(
            Channel(
                name = "┃AR┃ AD SPORT 1 HD",
                group = adSport,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993336&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ AD SPORT 2 HD",
                group = adSport,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=993337&extension=ts"
            )
        )

        // ======================================================
        // DUBAI SPORTS
        // ======================================================

        val dubaiSport = "┃AR┃ DUBAI SPORTS"

        add(
            Channel(
                name = "┃AR┃ DUBAI SPORT 1 HD",
                group = dubaiSport,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8086&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ DUBAI SPORT 2 HD",
                group = dubaiSport,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=84251&extension=ts"
            )
        )

        add(
            Channel(
                name = "┃AR┃ DUBAI SPORT 3 HD",
                group = dubaiSport,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=591579&extension=ts"
            )
        )

        // ======================================================
        // IRAQIA SPORTS
        // ======================================================

        val iraqiaSport = "┃AR┃ IRAQIA SPORTS"

        add(
            Channel(
                name = "┃AR┃ IRAQIA SPORT HD",
                group = iraqiaSport,
                url = "http://103.176.90.24/play/live.php?mac=00:1A:79:00:3A:F8&stream=8116&extension=ts"
            )
        )
    }

    // ==========================================================
    // CREATE
    // ==========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.statusBarColor = bgPrimary
        window.navigationBarColor = bgPrimary

        buildInterface()
        showWelcomeDialog()
    }

    // ==========================================================
    // WELCOME
    // ==========================================================

    private fun showWelcomeDialog() {

        val builder = AlertDialog.Builder(this)

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

        val dialog = builder.create()
        dialog.show()
    }

    // ==========================================================
    // BUILD INTERFACE
    // ==========================================================

    private fun buildInterface() {

        root = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setBackgroundColor(bgPrimary)

            layoutDirection =
                View.LAYOUT_DIRECTION_LTR
        }

        createTopBar()

        mainContent = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            setBackgroundColor(bgPrimary)
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

        topBar = LinearLayout(this).apply {

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

            setBackgroundColor(bgSecondary)
        }

        val logoBox = FrameLayout(this).apply {

            background =
                GradientDrawable().apply {

                    setColor(accentColor)

                    cornerRadius =
                        dp(12).toFloat()
                }
        }

        val logoText = TextView(this).apply {

            text = "NT"

            textSize = 15f

            gravity =
                Gravity.CENTER

            setTextColor(bgPrimary)

            setTypeface(
                Typeface.DEFAULT_BOLD
            )
        }

        logoBox.addView(
            logoText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        topBar.addView(
            logoBox,
            LinearLayout.LayoutParams(
                dp(40),
                dp(40)
            )
        )

        val brandText = TextView(this).apply {

            text = "  NAITI TV"

            textSize = 18f

            setTextColor(textWhite)

            setTypeface(
                Typeface.DEFAULT_BOLD
            )
        }

        topBar.addView(brandText)

        val spacer = View(this)

        topBar.addView(
            spacer,
            LinearLayout.LayoutParams(
                0,
                1,
                1f
            )
        )

        val telegramBtn = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER

            setPadding(
                dp(12),
                dp(6),
                dp(12),
                dp(6)
            )

            isFocusable = true
            isFocusableInTouchMode = true

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

        val tgIcon = TextView(this).apply {

            text = "✈ "

            textSize = 12f

            setTextColor(textWhite)
        }

        val tgText = TextView(this).apply {

            text = "Telegram"

            textSize = 11f

            setTextColor(textWhite)

            setTypeface(
                Typeface.DEFAULT_BOLD
            )
        }

        telegramBtn.addView(tgIcon)
        telegramBtn.addView(tgText)

        val tgParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                setMargins(
                    0,
                    0,
                    dp(12),
                    0
                )
            }

        topBar.addView(
            telegramBtn,
            tgParams
        )

        val liveBadge = LinearLayout(this).apply {

            orientation =
                LinearLayout.HORIZONTAL

            gravity =
                Gravity.CENTER

            setPadding(
                dp(14),
                dp(6),
                dp(14),
                dp(6)
            )

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.parseColor(
                            "#1A00E676"
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

        val liveDot = TextView(this).apply {

            text = "● "

            textSize = 10f

            setTextColor(statusGreen)
        }

        val liveText = TextView(this).apply {

            text = "LIVE"

            textSize = 11f

            setTextColor(textWhite)

            setTypeface(
                Typeface.DEFAULT_BOLD
            )
        }

        liveBadge.addView(liveDot)
        liveBadge.addView(liveText)

        topBar.addView(liveBadge)

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

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(telegramUrl)
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "تعذر فتح رابط التليجرام",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==========================================================
    // PACKAGES COLUMN
    // ==========================================================

    private fun createPackageColumn() {

        val col = LinearLayout(this).apply {

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

        val scroll = ScrollView(this).apply {

            isVerticalScrollBarEnabled =
                false

            isFocusable = false
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

        val col = LinearLayout(this).apply {

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

        val scroll = ScrollView(this).apply {

            isVerticalScrollBarEnabled =
                false

            isFocusable = false
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
    // PLAYER
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

        playerContainer =
            FrameLayout(this).apply {

                background =
                    GradientDrawable().apply {

                        setColor(Color.BLACK)

                        cornerRadius =
                            dp(20).toFloat()

                        setStroke(
                            dp(1),
                            strokeColor
                        )
                    }

                clipToOutline = true
            }

        playerView =
            PlayerView(this).apply {

                useController = false

                setBackgroundColor(
                    Color.BLACK
                )

                resizeMode =
                    AspectRatioFrameLayout
                        .RESIZE_MODE_FILL

                isFocusable = true

                isFocusableInTouchMode =
                    true

                isClickable = true

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

        val watermark =
            TextView(this).apply {

                text = "NAITI TV"

                textSize = 10f

                setTextColor(
                    Color.parseColor(
                        "#80FFFFFF"
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

        playerColumn.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.65f
            )
        )

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

                textSize = 16f

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

                textSize = 12f

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

                progress = 100

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

                topMargin = dp(15)
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
    // HEADER
    // ==========================================================

    private fun createColumnHeader(
        title: String
    ): TextView {

        return TextView(this).apply {

            text = title

            textSize = 13f

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

                currentGroup = group

                updatePackageSelection()

                loadChannels(group)
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
                    dp(48)
                ).apply {

                    bottomMargin = dp(8)
                }
            )

            packageButtons.add(card)
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
                    dp(16),
                    0
                )

                background =
                    createCardDrawable(
                        false,
                        isSelected
                    )

                isFocusable = true

                isFocusableInTouchMode =
                    true
            }

        val nameTv =
            TextView(this).apply {

                text =
                    packageDisplayName(
                        name
                    )

                textSize = 13f

                setTextColor(
                    if (isSelected)
                        textWhite
                    else
                        textMuted
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )
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

                textSize = 10f

                setTextColor(
                    if (isSelected)
                        textWhite
                    else
                        textMuted
                )

                setPadding(
                    dp(8),
                    dp(3),
                    dp(8),
                    dp(3)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            if (isSelected)
                                Color.parseColor(
                                    "#33FFFFFF"
                                )
                            else
                                Color.parseColor(
                                    "#15FFFFFF"
                                )
                        )

                        cornerRadius =
                            dp(12).toFloat()
                    }
            }

        layout.addView(badge)

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
                "beIN SPORTS HD 🏆"

            "┃AR┃ BEIN SPORTS HD 2" ->
                "beIN SPORTS HD 2 🏆"

            "┃AR┃ ALWAN SPORTS" ->
                "ALWAN SPORTS 🎨"

            "┃AR┃ THAMANYA" ->
                "THAMANYA 📺"

            "┃AR┃ ALKASS SPORTS" ->
                "ALKASS SPORTS 🏆"

            "┃AR┃ AD SPORTS" ->
                "AD SPORTS 🏆"

            "┃AR┃ DUBAI SPORTS" ->
                "DUBAI SPORTS 📺"

            "┃AR┃ IRAQIA SPORTS" ->
                "IRAQIA SPORTS 🇮🇶"

            else ->
                name
        }
    }

    // ==========================================================
    // UPDATE PACKAGE SELECTION
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
                packagesLayout.getChildAt(i)
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

            val tv =
                child.getChildAt(0)
                        as? TextView

            tv?.setTextColor(
                if (isSelected)
                    textWhite
                else
                    textMuted
            )

            val badge =
                child.getChildAt(1)
                        as? TextView

            badge?.setTextColor(
                if (isSelected)
                    textWhite
                else
                    textMuted
            )

            badge?.background =
                GradientDrawable().apply {

                    setColor(
                        if (isSelected)
                            Color.parseColor(
                                "#33FFFFFF"
                            )
                        else
                            Color.parseColor(
                                "#15FFFFFF"
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

        currentChannelIndex = -1

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
                    index == currentChannelIndex
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
                    createCardDrawable(
                        hasFocus,
                        currentChannelIndex ==
                                index
                    )
            }

            channelsLayout.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(50)
                ).apply {

                    bottomMargin = dp(8)
                }
            )

            channelButtons.add(card)
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
                    createCardDrawable(
                        false,
                        isSelected
                    )

                isFocusable = true

                isFocusableInTouchMode =
                    true
            }

        val iconBox =
            TextView(this).apply {

                text = "TV"

                textSize = 9f

                setTextColor(
                    if (isSelected)
                        bgPrimary
                    else
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
                            if (isSelected)
                                textWhite
                            else
                                bgPrimary
                        )

                        cornerRadius =
                            dp(12).toFloat()
                    }
            }

        card.addView(
            iconBox,
            LinearLayout.LayoutParams(
                dp(32),
                dp(32)
            )
        )

        val name =
            TextView(this).apply {

                text =
                    "  ${channel.name}"

                textSize = 12f

                setTextColor(
                    textWhite
                )

                setTypeface(
                    Typeface.DEFAULT_BOLD
                )

                maxLines = 1
            }

        card.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val quality =
            TextView(this).apply {

                text = when {

                    channel.name.contains(
                        "UHD",
                        true
                    ) ||
                            channel.name.contains(
                                "4K",
                                true
                            ) ->
                        "UHD"

                    channel.name.contains(
                        "FHD",
                        true
                    ) ||
                            channel.name.contains(
                                "1080p",
                                true
                            ) ->
                        "FHD"

                    channel.name.contains(
                        "HD",
                        true
                    ) ->
                        "HD"

                    else ->
                        "SD"
                }

                textSize = 8f

                setTextColor(
                    if (isSelected)
                        bgPrimary
                    else
                        accentColor
                )

                setPadding(
                    dp(8),
                    dp(3),
                    dp(8),
                    dp(3)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            if (isSelected)
                                textWhite
                            else
                                Color.parseColor(
                                    "#1500E5FF"
                                )
                        )

                        cornerRadius =
                            dp(8).toFloat()
                    }
            }

        card.addView(
            quality
        )

        return card
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
                channelsLayout.getChildAt(i)
                        as? LinearLayout
                    ?: continue

            val isSelected =
                i == currentChannelIndex

            child.background =
                createCardDrawable(
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
                            textWhite
                        else
                            bgPrimary
                    )

                    cornerRadius =
                        dp(12).toFloat()
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
                            textWhite
                        else
                            Color.parseColor(
                                "#1500E5FF"
                            )
                    )

                    cornerRadius =
                        dp(8).toFloat()
                }
        }
    }

    // ==========================================================
    // CARD DRAWABLE
    // ==========================================================

    private fun createCardDrawable(
        hasFocus: Boolean,
        isSelected: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(24).toFloat()

            if (hasFocus) {

                setColor(
                    accentHover
                )

                setStroke(
                    dp(2),
                    accentColor
                )

            } else if (isSelected) {

                setColor(
                    accentColor
                )

                setStroke(
                    dp(0),
                    Color.TRANSPARENT
                )

            } else {

                setColor(
                    bgCard
                )

                setStroke(
                    dp(1),
                    Color.TRANSPARENT
                )
            }
        }
    }

    // ==========================================================
    // EXO PLAYER
    // ==========================================================

    private fun initExoPlayer() {

        if (exoPlayer == null) {

            exoPlayer =
                ExoPlayer.Builder(this)
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
            channel.name

        epgSub.text =
            "Now Playing: Live Broadcast"

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

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "خطأ في تشغيل القناة",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun enterFullscreen() {

        if (fullscreen)
            return

        fullscreen = true

        topBar.visibility =
            View.GONE

        mainContent.getChildAt(0)
            .visibility =
            View.GONE

        mainContent.getChildAt(1)
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
            playerContainer.layoutParams
                    as LinearLayout.LayoutParams

        params.height =
            LinearLayout.LayoutParams.MATCH_PARENT

        params.weight = 1f

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

            @Suppress("DEPRECATION")

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

        fullscreen = false

        topBar.visibility =
            View.VISIBLE

        mainContent.getChildAt(0)
            .visibility =
            View.VISIBLE

        mainContent.getChildAt(1)
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

                setColor(Color.BLACK)

                cornerRadius =
                    dp(20).toFloat()

                setStroke(
                    dp(1),
                    strokeColor
                )
            }

        val params =
            playerContainer.layoutParams
                    as LinearLayout.LayoutParams

        params.height = 0

        params.weight = 0.65f

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

            @Suppress("DEPRECATION")

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
    // REMOTE / KEYBOARD
    // ==========================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_BACK -> {

                    if (fullscreen) {

                        exitFullscreen()

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (playerView.hasFocus()) {

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

            playChannel(it)
        }
    }

    override fun onStop() {

        super.onStop()

        playerView.player = null

        exoPlayer?.release()

        exoPlayer = null
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