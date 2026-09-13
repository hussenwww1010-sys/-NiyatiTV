package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

    // ============================================================
    // PLAYER
    // ============================================================

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    // ============================================================
    // ROOT / UI
    // ============================================================

    private lateinit var root: FrameLayout
    private lateinit var overlay: LinearLayout

    private lateinit var packageScroll: HorizontalScrollView
    private lateinit var packageLayout: LinearLayout

    private lateinit var channelScroll: HorizontalScrollView
    private lateinit var channelLayout: LinearLayout

    private lateinit var logoView: ImageView
    private lateinit var titleText: TextView
    private lateinit var currentChannelText: TextView
    private lateinit var currentGroupText: TextView
    private lateinit var statusText: TextView
    private lateinit var timeText: TextView

    private var isOverlayVisible = true
    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentChannel: Channel? = null

    private val channels = mutableListOf<Channel>()

    private val handler = Handler(Looper.getMainLooper())

    // ============================================================
    // COLORS
    // ============================================================

    private val BG = Color.parseColor("#030407")
    private val BLACK = Color.BLACK

    private val GOLD = Color.parseColor("#D6A84F")
    private val GOLD_LIGHT = Color.parseColor("#F4D58A")
    private val GOLD_DARK = Color.parseColor("#8E6525")

    private val WHITE = Color.WHITE
    private val TEXT_PRIMARY = Color.parseColor("#F7F7F5")
    private val TEXT_SECONDARY = Color.parseColor("#9EA4AE")

    private val CARD = Color.parseColor("#101217")
    private val CARD_2 = Color.parseColor("#171A21")

    private val GREEN = Color.parseColor("#22C55E")
    private val RED = Color.parseColor("#EF4444")

    // ============================================================
    // TIME
    // ============================================================

    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        loadChannelsData()
        createPlayer()
        createInterface()

        if (channels.isNotEmpty()) {
            currentGroup = channels.first().group
            showChannels(currentGroup)
        }

        handler.post(timeRunnable)
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }

    private fun roundedBackground(
        color: Int,
        radius: Float = 14f,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidth: Int = 0
    ): GradientDrawable {

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (
                strokeColor != Color.TRANSPARENT &&
                strokeWidth > 0
            ) {
                setStroke(
                    dp(strokeWidth.toFloat()),
                    strokeColor
                )
            }
        }
    }

    private fun gradientBackground(
        colors: IntArray,
        orientation: GradientDrawable.Orientation =
            GradientDrawable.Orientation.TOP_BOTTOM
    ): GradientDrawable {

        return GradientDrawable(
            orientation,
            colors
        ).apply {
            cornerRadius = dp(18f).toFloat()
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int = WHITE,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {

            text = value
            textSize = size
            setTextColor(color)

            gravity = Gravity.CENTER_VERTICAL

            if (bold) {
                setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
            }
        }
    }

    // ============================================================
    // CHANNEL DATA
    // ============================================================

    private fun addChannel(
        name: String,
        group: String,
        url: String
    ) {
        channels.add(
            Channel(
                name = name,
                group = group,
                url = url
            )
        )
    }

    private fun addSts(
        name: String,
        group: String,
        id: Int
    ) {

        val url =
            "http://sts.mydroon.com:8080/9127491274/QDXDbuLzFDYaYbD/$id"

        addChannel(
            name,
            group,
            url
        )
    }

    /*
     * مهم:
     * ضع هنا بداية رابط 103.176.90.24 كما هو موجود عندك
     * من ملف M3U.
     *
     * مثال الشكل فقط:
     *
     * http://103.176.90.24/play/live.php?mac=XXXX&stream=
     *
     * لا تغير stream ID الذي يتم إضافته بعد ذلك.
     */
    private val SOURCE_103_BASE =
        "http://103.176.90.24/play/live.php?mac=YOUR_MAC&stream="

    private fun add103(
        name: String,
        group: String,
        streamId: Int
    ) {

        val url =
            "${SOURCE_103_BASE}${streamId}&extension=ts"

        addChannel(
            name,
            group,
            url
        )
    }

    private fun loadChannelsData() {

        channels.clear()

        // ========================================================
        // 1 - SOLO SPORTS
        // ========================================================

        val solo = "SOLO SPORTS"

        addSts("SOLO SPORT HDR 4K", solo, 249999)

        addSts("SOLO SPORT 1 FHD", solo, 250000)
        addSts("SOLO SPORT 1 HEVC", solo, 250001)
        addSts("SOLO SPORT 1 HD", solo, 250002)
        addSts("SOLO SPORT 1 SD", solo, 250003)

        addSts("SOLO SPORT 2 FHD", solo, 250004)
        addSts("SOLO SPORT 2 HEVC", solo, 250005)
        addSts("SOLO SPORT 2 HD", solo, 250006)
        addSts("SOLO SPORT 2 SD", solo, 250007)

        addSts("SOLO SPORT 3 FHD", solo, 250008)
        addSts("SOLO SPORT 3 HEVC", solo, 250009)
        addSts("SOLO SPORT 3 HD", solo, 250010)
        addSts("SOLO SPORT 3 SD", solo, 250011)

        addSts("SOLO SPORT 4 FHD", solo, 250012)
        addSts("SOLO SPORT 4 HEVC", solo, 250013)
        addSts("SOLO SPORT 4 HD", solo, 250014)
        addSts("SOLO SPORT 4 SD", solo, 250015)

        addSts("SOLO SPORT 5 FHD", solo, 250016)
        addSts("SOLO SPORT 5 HEVC", solo, 250017)
        addSts("SOLO SPORT 5 HD", solo, 250018)
        addSts("SOLO SPORT 5 SD", solo, 250019)

        addSts("SOLO SPORT UFC FHD", solo, 250020)
        addSts("SOLO SPORT UFC HEVC", solo, 250021)
        addSts("SOLO SPORT UFC HD", solo, 250022)
        addSts("SOLO SPORT UFC SD", solo, 250023)

        addSts("SOLO SPORT F1 FHD", solo, 250024)
        addSts("SOLO SPORT F1 HEVC", solo, 250025)
        addSts("SOLO SPORT F1 HD", solo, 250026)
        addSts("SOLO SPORT F1 SD", solo, 250027)

        addSts("SOLO SPORT WWE FHD", solo, 250028)
        addSts("SOLO SPORT WWE HEVC", solo, 250029)
        addSts("SOLO SPORT WWE HD", solo, 250030)
        addSts("SOLO SPORT WWE SD", solo, 250031)

        // ========================================================
        // 2 - ALWAN SPORTS
        // ========================================================

        val alwan = "ALWAN SPORTS"

        addSts("ALWAN SPORT 1 4K", alwan, 248386)
        addSts("ALWAN SPORT 1 HD", alwan, 248387)
        addSts("ALWAN SPORT 1 SD", alwan, 248388)

        addSts("ALWAN SPORT 2 4K", alwan, 248389)
        addSts("ALWAN SPORT 2 HD", alwan, 248390)
        addSts("ALWAN SPORT 2 SD", alwan, 248391)

        addSts("ALWAN SPORT 3 4K", alwan, 248392)
        addSts("ALWAN SPORT 3 HD", alwan, 248393)
        addSts("ALWAN SPORT 3 SD", alwan, 248394)

        addSts("ALWAN SPORT 4 HD", alwan, 248395)
        addSts("ALWAN SPORT 4 SD", alwan, 248396)

        addSts("ALWAN SPORT 5 HD", alwan, 248397)
        addSts("ALWAN SPORT 5 SD", alwan, 248398)

        addSts("ALWAN SPORT 6 HD", alwan, 248399)
        addSts("ALWAN SPORT 6 SD", alwan, 248400)

        // ========================================================
        // 3 - beIN SPORTS
        // ========================================================

        val bein = "beIN SPORTS"

        add103("beIN SPORT 1 HD", bein, 1330437)
        add103("beIN SPORT 2 HD", bein, 1330438)
        add103("beIN SPORT 3 HD", bein, 1411381)
        add103("beIN SPORT 4 HD", bein, 1411380)
        add103("beIN SPORT 5 HD", bein, 1411379)
        add103("beIN SPORT 6 HD", bein, 1411378)
        add103("beIN SPORT 7 HD", bein, 1411377)
        add103("beIN SPORT 8 HD", bein, 1411376)
        add103("beIN SPORT 9 HD", bein, 1411375)

        add103("beIN SPORT 1 HD • 2", bein, 1660413)
        add103("beIN SPORT 2 HD • 2", bein, 1660411)
        add103("beIN SPORT 3 HD • 2", bein, 1660409)
        add103("beIN SPORT 4 HD • 2", bein, 1660407)
        add103("beIN SPORT 5 HD • 2", bein, 1660405)
        add103("beIN SPORT 6 HD • 2", bein, 1660403)
        add103("beIN SPORT 7 HD • 2", bein, 1660401)
        add103("beIN SPORT 8 HD • 2", bein, 1660399)
        add103("beIN SPORT 9 HD • 2", bein, 1660397)

        // ========================================================
        // 4 - ALWAN SPORT
        // ========================================================

        add103("ALWAN SPORT 1 HD", alwan, 1859098)
        add103("ALWAN SPORT 2 HD", alwan, 1859097)
        add103("ALWAN SPORT 3 HD", alwan, 1859096)
        add103("ALWAN SPORT 4 HD", alwan, 1859095)
        add103("ALWAN SPORT 5 HD", alwan, 1859094)
        add103("ALWAN SPORT 6 HD", alwan, 1859093)

        // ========================================================
        // 5 - THMANYAH
        // ========================================================

        val thamanyah = "THMANYAH"

        add103("THAMANYA 1 HD", thamanyah, 1936356)
        add103("THAMANYA 2 HD", thamanyah, 1936355)
        add103("THAMANYA 3 HD", thamanyah, 1936354)

        // ========================================================
        // 6 - AL KASS SPORTS
        // ========================================================

        val alkass = "AL KASS SPORTS"

        add103("ALKASS SPORT 1 HD", alkass, 591593)
        add103("ALKASS SPORT 2 HD", alkass, 591591)
        add103("ALKASS SPORT 3 HD", alkass, 787903)
        add103("ALKASS SPORT 4 HD", alkass, 591589)
        add103("ALKASS SPORT 5 HD", alkass, 591587)
        add103("ALKASS SPORT 6 HD", alkass, 787906)

        // ========================================================
        // 7 - AD SPORTS
        // ========================================================

        val ad = "AD SPORTS"

        add103("AD SPORT 1 HD", ad, 993336)
        add103("AD SPORT 2 HD", ad, 993337)

        // ========================================================
        // 8 - DUBAI SPORTS
        // ========================================================

        val dubai = "DUBAI SPORTS"

        add103("DUBAI SPORT 1 HD", dubai, 8086)
        add103("DUBAI SPORT 2 HD", dubai, 84251)
        add103("DUBAI SPORT 3 HD", dubai, 591579)

        // ========================================================
        // 9 - IRAQ SPORTS
        // ========================================================

        val iraq = "IRAQ SPORTS"

        add103("IRAQIA SPORT HD", iraq, 8116)
    }

    // ============================================================
    // PLAYER
    // ============================================================

    private fun createPlayer() {

        player =
            ExoPlayer.Builder(this)
                .build()

        playerView =
            PlayerView(this).apply {

                player =
                    this@MainActivity.player

                useController = false

                resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_FIT

                setShowBuffering(
                    PlayerView.SHOW_BUFFERING_WHEN_PLAYING
                )

                setBackgroundColor(
                    Color.BLACK
                )

                setOnClickListener {

                    if (isOverlayVisible) {
                        hideOverlay()
                    } else {
                        showOverlay()
                    }
                }
            }

        player?.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(
                    state: Int
                ) {

                    when (state) {

                        Player.STATE_BUFFERING -> {

                            statusTextSafe(
                                "جاري الاتصال بالبث المباشر..."
                            )
                        }

                        Player.STATE_READY -> {

                            statusTextSafe(
                                "● البث المباشر مستقر"
                            )
                        }

                        Player.STATE_ENDED -> {

                            reconnectChannel()
                        }
                    }
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    statusTextSafe(
                        "فشل الاتصال — إعادة المحاولة..."
                    )

                    reconnectChannel()
                }
            }
        )
    }

    // ============================================================
    // PLAY CHANNEL
    // ============================================================

    private fun playChannel(
        channel: Channel,
        index: Int
    ) {

        currentChannel =
            channel

        currentChannelIndex =
            index

        currentGroup =
            channel.group

        updateCurrentInfo()

        statusTextSafe(
            "جاري تشغيل ${channel.name}"
        )

        val mediaItem =
            MediaItem.fromUri(
                Uri.parse(channel.url)
            )

        player?.apply {

            stop()

            clearMediaItems()

            setMediaItem(
                mediaItem
            )

            prepare()

            playWhenReady = true
        }

        refreshChannelCards()

        hideOverlay()
    }

    // ============================================================
    // RECONNECT
    // ============================================================

    private fun reconnectChannel() {

        val channel =
            currentChannel
                ?: return

        handler.postDelayed({

            if (isFinishing) {
                return@postDelayed
            }

            val mediaItem =
                MediaItem.fromUri(
                    Uri.parse(channel.url)
                )

            player?.apply {

                stop()

                clearMediaItems()

                setMediaItem(
                    mediaItem
                )

                prepare()

                playWhenReady = true
            }

        }, 1500)
    }

    // ============================================================
    // MAIN INTERFACE
    // ============================================================

    private fun createInterface() {

        root =
            FrameLayout(this).apply {

                setBackgroundColor(
                    BG
                )
            }

        root.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ========================================================
        // OVERLAY
        // ========================================================

        overlay =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(18f),
                    dp(14f),
                    dp(18f),
                    dp(14f)
                )

                background =
                    GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(
                            Color.parseColor("#E8030407"),
                            Color.parseColor("#D9030407"),
                            Color.parseColor("#A0030407"),
                            Color.parseColor("#52030407")
                        )
                    )
            }

        // ========================================================
        // TOP BAR
        // ========================================================

        val topBar =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_LTR

                setPadding(
                    dp(4f),
                    0,
                    dp(4f),
                    dp(4f)
                )
            }

        logoView =
            ImageView(this).apply {

                setImageResource(
                    R.drawable.niyati_logo
                )

                scaleType =
                    ImageView.ScaleType.CENTER_INSIDE
            }

        topBar.addView(
            logoView,
            LinearLayout.LayoutParams(
                dp(52f),
                dp(52f)
            )
        )

        val brand =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(10f),
                    0,
                    dp(10f),
                    0
                )
            }

        titleText =
            makeText(
                "NIYATI TV",
                19f,
                GOLD_LIGHT,
                true
            )

        brand.addView(
            titleText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(27f)
            )
        )

        val sub =
            makeText(
                "LIVE SPORTS",
                10f,
                TEXT_SECONDARY,
                true
            )

        brand.addView(
            sub,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(20f)
            )
        )

        topBar.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val live =
            makeText(
                "● LIVE",
                11f,
                WHITE,
                true
            ).apply {

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(14f),
                    0,
                    dp(14f),
                    0
                )

                background =
                    roundedBackground(
                        Color.parseColor("#B31A1D24"),
                        30f,
                        GOLD_DARK,
                        1
                    )
            }

        topBar.addView(
            live,
            LinearLayout.LayoutParams(
                dp(82f),
                dp(36f)
            )
        )

        timeText =
            makeText(
                "",
                13f,
                TEXT_SECONDARY,
                true
            ).apply {

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(12f),
                    0,
                    dp(12f),
                    0
                )
            }

        topBar.addView(
            timeText,
            LinearLayout.LayoutParams(
                dp(85f),
                dp(40f)
            )
        )

        overlay.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56f)
            )
        )

        // ========================================================
        // PLAYER SPACER / INFO
        // ========================================================

        val playerInfo =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                setPadding(
                    dp(12f),
                    dp(5f),
                    dp(12f),
                    dp(5f)
                )
            }

        currentGroupText =
            makeText(
                "اختر باقة",
                11f,
                GOLD,
                true
            )

        currentChannelText =
            makeText(
                "اختر قناة للبدء",
                18f,
                WHITE,
                true
            )

        statusText =
            makeText(
                "جاهز للبث",
                10f,
                TEXT_SECONDARY,
                false
            )

        playerInfo.addView(
            currentGroupText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(21f)
            )
        )

        playerInfo.addView(
            currentChannelText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28f)
            )
        )

        playerInfo.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(20f)
            )
        )

        overlay.addView(
            playerInfo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72f)
            )
        )

        // ========================================================
        // PACKAGE TITLE
        // ========================================================

        val packageTitle =
            makeText(
                "الباقات الرياضية",
                14f,
                WHITE,
                true
            ).apply {

                setPadding(
                    dp(4f),
                    dp(3f),
                    dp(4f),
                    dp(3f)
                )
            }

        overlay.addView(
            packageTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30f)
            )
        )

        // ========================================================
        // PACKAGES
        // ========================================================

        packageScroll =
            HorizontalScrollView(this).apply {

                isHorizontalScrollBarEnabled =
                    false

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }

        packageLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        packageScroll.addView(
            packageLayout
        )

        overlay.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70f)
            )
        )

        // ========================================================
        // CHANNEL TITLE
        // ========================================================

        val channelTitle =
            makeText(
                "القنوات",
                14f,
                WHITE,
                true
            ).apply {

                setPadding(
                    dp(4f),
                    dp(5f),
                    dp(4f),
                    dp(2f)
                )
            }

        overlay.addView(
            channelTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(32f)
            )
        )

        // ========================================================
        // CHANNELS
        // ========================================================

        channelScroll =
            HorizontalScrollView(this).apply {

                isHorizontalScrollBarEnabled =
                    false

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                overScrollMode =
                    View.OVER_SCROLL_NEVER
            }

        channelLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        channelScroll.addView(
            channelLayout
        )

        overlay.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(88f)
            )
        )

        // ========================================================
        // FOOTER
        // ========================================================

        val footer =
            makeText(
                "اضغط على القناة للتشغيل • OK ملء الشاشة • ← → تغيير القناة",
                10f,
                TEXT_SECONDARY,
                false
            ).apply {

                gravity =
                    Gravity.CENTER

                background =
                    roundedBackground(
                        Color.parseColor("#66101217"),
                        12f
                    )
            }

        overlay.addView(
            footer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34f)
            )
        )

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(
            root
        )

        buildPackages()
    }

    // ============================================================
    // PACKAGES
    // ============================================================

    private fun buildPackages() {

        packageLayout.removeAllViews()

        val groups =
            channels
                .map { it.group }
                .distinct()

        groups.forEachIndexed { index, group ->

            val count =
                channels.count {
                    it.group == group
                }

            val card =
                LinearLayout(this).apply {

                    id =
                        View.generateViewId()

                    orientation =
                        LinearLayout.VERTICAL

                    gravity =
                        Gravity.CENTER

                    layoutDirection =
                        View.LAYOUT_DIRECTION_RTL

                    isFocusable =
                        true

                    isClickable =
                        true

                    setPadding(
                        dp(15f),
                        dp(5f),
                        dp(15f),
                        dp(5f)
                    )

                    background =
                        roundedBackground(
                            CARD,
                            16f,
                            Color.parseColor("#292D35"),
                            1
                        )

                    setOnClickListener {

                        currentGroup =
                            group

                        showChannels(
                            group
                        )

                        updatePackageFocus()
                    }

                    setOnFocusChangeListener { view, focused ->

                        if (focused) {

                            view.background =
                                roundedBackground(
                                    Color.parseColor("#241B0C"),
                                    16f,
                                    GOLD,
                                    2
                                )

                        } else {

                            view.background =
                                roundedBackground(
                                    CARD,
                                    16f,
                                    Color.parseColor("#292D35"),
                                    1
                                )
                        }
                    }
                }

            val number =
                makeText(
                    String.format(
                        "%02d",
                        index + 1
                    ),
                    18f,
                    GOLD_LIGHT,
                    true
                ).apply {

                    gravity =
                        Gravity.CENTER
                }

            card.addView(
                number,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(25f)
                )
            )

            val name =
                makeText(
                    group,
                    11f,
                    WHITE,
                    true
                ).apply {

                    gravity =
                        Gravity.CENTER
                }

            card.addView(
                name,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(24f)
                )
            )

            val total =
                makeText(
                    "$count قناة",
                    9f,
                    TEXT_SECONDARY,
                    false
                ).apply {

                    gravity =
                        Gravity.CENTER
                }

            card.addView(
                total,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(18f)
                )
            )

            packageLayout.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(145f),
                    dp(68f)
                ).apply {

                    setMargins(
                        dp(5f),
                        0,
                        dp(5f),
                        0
                    )
                }
            )
        }

        updatePackageFocus()
    }

    // ============================================================
    // SHOW CHANNELS
    // ============================================================

    private fun showChannels(
        group: String
    ) {

        channelLayout.removeAllViews()

        val groupChannels =
            channels.filter {
                it.group == group
            }

        groupChannels.forEachIndexed { index, channel ->

            val realIndex =
                channels.indexOf(
                    channel
                )

            val card =
                createChannelCard(
                    channel,
                    index,
                    realIndex
                )

            channelLayout.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(155f),
                    dp(82f)
                ).apply {

                    setMargins(
                        dp(5f),
                        0,
                        dp(5f),
                        0
                    )
                }
            )
        }

        channelScroll.post {

            channelScroll.fullScroll(
                HorizontalScrollView.FOCUS_RIGHT
            )
        }
    }

    // ============================================================
    // CHANNEL CARD
    // ============================================================

    private fun createChannelCard(
        channel: Channel,
        position: Int,
        realIndex: Int
    ): View {

        val card =
            LinearLayout(this).apply {

                id =
                    View.generateViewId()

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL

                isFocusable =
                    true

                isClickable =
                    true

                setPadding(
                    dp(10f),
                    dp(5f),
                    dp(10f),
                    dp(5f)
                )

                background =
                    roundedBackground(
                        CARD_2,
                        14f,
                        Color.parseColor("#292D35"),
                        1
                    )

                setOnClickListener {

                    playChannel(
                        channel,
                        realIndex
                    )
                }

                setOnFocusChangeListener { view, focused ->

                    if (focused) {

                        view.background =
                            roundedBackground(
                                Color.parseColor("#261D0D"),
                                14f,
                                GOLD,
                                2
                            )

                    } else {

                        val selected =
                            currentChannelIndex ==
                                    realIndex

                        view.background =
                            roundedBackground(
                                if (selected) {
                                    Color.parseColor(
                                        "#211A0D"
                                    )
                                } else {
                                    CARD_2
                                },
                                14f,
                                if (selected) {
                                    GOLD
                                } else {
                                    Color.parseColor(
                                        "#292D35"
                                    )
                                },
                                1
                            )
                    }
                }
            }

        val top =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

        val live =
            makeText(
                "LIVE",
                8f,
                WHITE,
                true
            ).apply {

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(6f),
                    0,
                    dp(6f),
                    0
                )

                background =
                    roundedBackground(
                        Color.parseColor("#9922C55E"),
                        8f
                    )
            }

        top.addView(
            live,
            LinearLayout.LayoutParams(
                dp(37f),
                dp(19f)
            )
        )

        val quality =
            getQuality(
                channel.name
            )

        val qualityText =
            makeText(
                quality,
                8f,
                GOLD_LIGHT,
                true
            ).apply {

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(5f),
                    0,
                    dp(5f),
                    0
                )

                background =
                    roundedBackground(
                        Color.parseColor("#331F1708"),
                        8f,
                        GOLD_DARK,
                        1
                    )
            }

        top.addView(
            qualityText,
            LinearLayout.LayoutParams(
                dp(42f),
                dp(19f)
            ).apply {

                setMargins(
                    dp(4f),
                    0,
                    0,
                    0
                )
            }
        )

        card.addView(
            top,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(21f)
            )
        )

        val name =
            makeText(
                channel.name,
                11f,
                WHITE,
                true
            ).apply {

                gravity =
                    Gravity.CENTER

                maxLines = 1

                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

        card.addView(
            name,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(29f)
            )
        )

        val number =
            makeText(
                "#${position + 1}",
                9f,
                TEXT_SECONDARY,
                false
            ).apply {

                gravity =
                    Gravity.CENTER
            }

        card.addView(
            number,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(18f)
            )
        )

        return card
    }

    // ============================================================
    // QUALITY
    // ============================================================

    private fun getQuality(
        name: String
    ): String {

        return when {

            name.contains(
                "4K",
                true
            ) -> "4K"

            name.contains(
                "FHD",
                true
            ) -> "FHD"

            name.contains(
                "HEVC",
                true
            ) -> "HEVC"

            name.contains(
                "HD",
                true
            ) -> "HD"

            name.contains(
                "SD",
                true
            ) -> "SD"

            else -> "LIVE"
        }
    }

    // ============================================================
    // CURRENT INFO
    // ============================================================

    private fun updateCurrentInfo() {

        val channel =
            currentChannel
                ?: return

        currentGroupText.text =
            channel.group

        currentChannelText.text =
            channel.name
    }

    // ============================================================
    // REFRESH CHANNEL CARDS
    // ============================================================

    private fun refreshChannelCards() {

        if (currentGroup.isNotEmpty()) {

            showChannels(
                currentGroup
            )
        }
    }

    // ============================================================
    // PACKAGE FOCUS
    // ============================================================

    private fun updatePackageFocus() {

        for (
            i in 0 until packageLayout.childCount
        ) {

            val view =
                packageLayout.getChildAt(i)

            val group =
                channels
                    .map { it.group }
                    .distinct()
                    .getOrNull(i)
                    ?: continue

            val selected =
                group == currentGroup

            view.background =
                roundedBackground(
                    if (selected) {
                        Color.parseColor(
                            "#261D0D"
                        )
                    } else {
                        CARD
                    },
                    16f,
                    if (selected) {
                        GOLD
                    } else {
                        Color.parseColor(
                            "#292D35"
                        )
                    },
                    if (selected) 2 else 1
                )
        }
    }

    // ============================================================
    // STATUS
    // ============================================================

    private fun statusTextSafe(
        value: String
    ) {

        if (
            ::statusText.isInitialized
        ) {

            statusText.text =
                value
        }
    }

    // ============================================================
    // TIME
    // ============================================================

    private fun updateTime() {

        if (
            !::timeText.isInitialized
        ) {
            return
        }

        val sdf =
            java.text.SimpleDateFormat(
                "HH:mm",
                java.util.Locale("ar", "IQ")
            )

        timeText.text =
            sdf.format(
                java.util.Date()
            )
    }

    // ============================================================
    // OVERLAY
    // ============================================================

    private fun hideOverlay() {

        isOverlayVisible =
            false

        overlay.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {

                overlay.visibility =
                    View.GONE
            }
            .start()

        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    private fun showOverlay() {

        isOverlayVisible =
            true

        overlay.visibility =
            View.VISIBLE

        overlay.alpha =
            0f

        overlay.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    // ============================================================
    // DPAD / REMOTE
    // ============================================================

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

                KeyEvent.KEYCODE_DPAD_UP -> {

                    if (!isOverlayVisible) {

                        showOverlay()

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    if (!isOverlayVisible) {

                        showOverlay()

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    if (!isOverlayVisible) {

                        playNextChannel(
                            previous = true
                        )

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    if (!isOverlayVisible) {

                        playNextChannel(
                            previous = false
                        )

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (!isOverlayVisible) {

                        showOverlay()

                        return true
                    }
                }

                KeyEvent.KEYCODE_BACK -> {

                    if (!isOverlayVisible) {

                        showOverlay()

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
    // NEXT / PREVIOUS CHANNEL
    // ============================================================

    private fun playNextChannel(
        previous: Boolean
    ) {

        if (
            currentGroup.isEmpty()
        ) {
            return
        }

        val groupChannels =
            channels.filter {
                it.group == currentGroup
            }

        if (
            groupChannels.isEmpty()
        ) {
            return
        }

        var position =
            groupChannels.indexOfFirst {
                it == currentChannel
            }

        if (position < 0) {
            position = 0
        }

        position =
            if (previous) {
                position - 1
            } else {
                position + 1
            }

        if (position < 0) {
            position =
                groupChannels.lastIndex
        }

        if (
            position >
            groupChannels.lastIndex
        ) {
            position = 0
        }

        val channel =
            groupChannels[position]

        val realIndex =
            channels.indexOf(
                channel
            )

        playChannel(
            channel,
            realIndex
        )
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onPause() {

        super.onPause()

        if (
            isFinishing
        ) {

            player?.release()

            player = null
        }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            timeRunnable
        )

        player?.release()

        player = null

        super.onDestroy()
    }
}