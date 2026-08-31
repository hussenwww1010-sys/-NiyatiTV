package com.niyati.tv

import android.app.Activity
import android.content.Context
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
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
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
    val url: String,
    val viewers: String = "12.33M",
    val quality: String = "4K HDR"
)

class MainActivity : Activity() {

    private var exoPlayer: ExoPlayer? = null
    private var fullscreen = false
    private var currentGroup = "beIN TOD"
    private var currentChannelIndex = -1

    private lateinit var root: LinearLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsGrid: GridLayout
    private lateinit var featuredLayout: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var epgTitle: TextView
    private lateinit var epgSub: TextView

    // Exact Gold & Bronze Metallic Colors
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
        return Channel(name = name, group = group, url = "$base$id.ts", viewers = "${(10..20).random()}.${(10..99).random()}M", quality = "4K HDR")
    }

    private val channels = mutableListOf<Channel>().apply {
        // beIN TOD
        add(c("beIN Tod 4K", "beIN TOD ⚽", "460835"))
        for (i in 1..9) add(c("beIN Sport Tod $i", "beIN TOD ⚽", "${460835 + i}"))

        // BEIN SPORTS
        add(c("beIN Sport Global 4K", "beIN SPORTS 🏆", "22186"))
        add(c("beIN Sport News 4K", "beIN SPORTS 🏆", "318230"))
        val bein4k = listOf(318197, 318198, 318199, 440580, 318201, 318202, 318203, 318204, 318205)
        for (i in 1..9) {
            bein4k.getOrNull(i - 1)?.let { add(c("beIN Sport $i 4K", "beIN SPORTS 🏆", it.toString())) }
        }

        // BEIN XTRA
        for (i in 1..9) add(c("beIN Sport XTRA $i 4K", "beIN XTRA 🔥", "${325790 + i}"))

        // OTHER PACKAGES
        listOf("12 الرابعة الرياضية", "10 الكأس QATAR", "9 السعودية الرياضية", "25 قنوات الخليج", "4 أبوظبي الرياضية").forEach { pkg ->
            for (i in 1..6) add(c("قناة $i", pkg, "22186"))
        }
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
            setPadding(dp(15), dp(15), dp(15), dp(10))
        }

        // 1. Left Section: Packages Column
        createPackagesColumn()

        // 2. Middle Section: Premium Channels (Grid Layout)
        createChannelsGridSection()

        // 3. Right Section: Player & EPG
        createPlayerSection()

        root.addView(mainContent, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 4. Bottom Section: Featured Content Carousel
        createFeaturedContentSection()

        setContentView(root)
        loadPackages()
    }

    private fun createPackagesColumn() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // Logo
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

        col.addView(scroll, LinearLayout.LayoutParams(dp(200), 0, 1f))
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
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Live Badge Top Right
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

        // Player Container
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

        // EPG Gold Card
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
            text = "beIN Sport Tod 2"
            textSize = 14f
            setTextColor(textWhite)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        epg.addView(epgTitle)

        epgSub = TextView(this).apply {
            text = "4K HDR | Audio: English / Arabic               يعرض الآن: بث مباشر القناة"
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

        // Action Buttons
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

    private fun createFeaturedContentSection() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), 0, dp(15), dp(10))
        }

        val header = TextView(this).apply {
            text = "Featured Content"
            textSize = 12f
            setTextColor(goldText)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, 0, 0, dp(6))
        }
        container.addView(header)

        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        featuredLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        for (i in 1..8) {
            val card = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    setColor(bgCardDark)
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), strokeBronze)
                }
                isFocusable = true
            }
            val txt = TextView(this).apply {
                text = "MOVIE $i"
                textSize = 10f
                setTextColor(goldText)
                gravity = Gravity.CENTER
            }
            card.addView(txt, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            featuredLayout.addView(card, LinearLayout.LayoutParams(dp(100), dp(55)).apply {
                rightMargin = dp(8)
            })
        }

        scroll.addView(featuredLayout)
        container.addView(scroll)

        root.addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
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
                textSize = 12f
                setTextColor(textWhite)
                setTypeface(Typeface.DEFAULT_BOLD)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            btn.addView(txt)

            packagesLayout.addView(btn, LinearLayout.LayoutParams(dp(180), dp(45)).apply {
                bottomMargin = dp(8)
            })
        }

        loadChannelsGrid(groups.first())
    }

    private fun loadChannelsGrid(group: String) {
        channelsGrid.removeAllViews()
        val filtered = channels.filter { it.group == group }

        filtered.forEachIndexed { index, channel ->
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

            // Thumbnail Simulation
            val thumb = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1B202A"))
                    cornerRadius = dp(6).toFloat()
                }
            }

            val badge = TextView(this).apply {
                text = "4K HDR   👁 ${channel.viewers}"
                textSize = 7f
                setTextColor(goldText)
                setPadding(dp(4), dp(2), dp(4), dp(2))
            }
            thumb.addView(badge)

            card.addView(thumb, LinearLayout.LayoutParams(dp(135), dp(65)))

            val name = TextView(this).apply {
                text = channel.name
                textSize = 10f
                setTextColor(textWhite)
                setTypeface(Typeface.DEFAULT_BOLD)
                setPadding(0, dp(4), 0, 0)
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
        featuredLayout.parent.requestLayout()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
