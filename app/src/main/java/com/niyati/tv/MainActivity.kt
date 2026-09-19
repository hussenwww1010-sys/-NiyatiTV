package com.niyati.tv

import android.app.Activity
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase

class MainActivity : Activity() {

    // =========================================================
    // COLORS
    // =========================================================

    private val bg = Color.rgb(5, 8, 14)
    private val panel = Color.rgb(11, 16, 25)
    private val card = Color.rgb(18, 25, 38)
    private val cardSelected = Color.rgb(24, 38, 54)

    private val cyan = Color.rgb(0, 229, 255)
    private val cyanSoft = Color.rgb(90, 238, 255)

    private val white = Color.rgb(245, 248, 252)
    private val textSecondary = Color.rgb(145, 158, 177)
    private val textMuted = Color.rgb(91, 105, 125)

    private val green = Color.rgb(16, 185, 129)
    private val red = Color.rgb(239, 68, 68)

    // =========================================================
    // FIREBASE
    // =========================================================

    private val databaseUrl =
        "https://niyati-tv-default-rtdb.europe-west1.firebasedatabase.app"

    private val database by lazy {
        FirebaseDatabase.getInstance(databaseUrl).reference
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private var playerReady = false
    private var isFullscreen = false

    // =========================================================
    // ROOT UI
    // =========================================================

    private lateinit var root: FrameLayout
    private lateinit var normalScreen: LinearLayout
    private lateinit var contentRow: LinearLayout

    private lateinit var packagesPanel: LinearLayout
    private lateinit var channelsPanel: LinearLayout
    private lateinit var playerPanel: LinearLayout

    private lateinit var packagesScroll: ScrollView
    private lateinit var channelsScroll: ScrollView

    private lateinit var packagesList: LinearLayout
    private lateinit var channelsList: LinearLayout

    private lateinit var playerContainer: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private lateinit var channelTitle: TextView
    private lateinit var playerTitle: TextView
    private lateinit var playerStatus: TextView
    private lateinit var selectedPackageLabel: TextView

    // =========================================================
    // DATA
    // =========================================================

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String = "",
        val enabled: Boolean = true,
        val order: Int = 0
    )

    data class Channel(
        val id: String,
        val name: String,
        val group: String,
        val url: String,
        val logo: String = "",
        val enabled: Boolean = true,
        val order: Int = 0
    )

    private val packages = mutableListOf<PackageItem>()
    private val allChannels = mutableListOf<Channel>()

    private var selectedPackage: PackageItem? = null
    private var selectedChannel: Channel? = null

    // =========================================================
    // FOCUS STATE
    // =========================================================

    // 0 = packages
    // 1 = channels
    // 2 = player
    private var currentSection = 0

    private var selectedPackageIndex = 0
    private var selectedChannelIndex = 0

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        hideSystemBars()

        createPlayer()

        createInterface()

        loadFirebaseData()
    }

    // =========================================================
    // SYSTEM BARS
    // =========================================================

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false)

            window.insetsController?.let { controller ->
                controller.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private fun createPlayer() {

        player = ExoPlayer.Builder(this).build()

        playerView = PlayerView(this)

        playerView.player = player

        playerView.useController = true

        playerView.setBackgroundColor(Color.BLACK)

        // مهم جداً:
        // PlayerView نفسه لا يأخذ Focus.
        // الـ playerContainer هو الذي يأخذ Focus.
        playerView.isFocusable = false
        playerView.isFocusableInTouchMode = false

        playerView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    // =========================================================
    // MAIN INTERFACE
    // =========================================================

    private fun createInterface() {

        root = FrameLayout(this)

        root.setBackgroundColor(bg)

        setContentView(root)

        // -----------------------------------------------------
        // NORMAL SCREEN
        // -----------------------------------------------------

        normalScreen = LinearLayout(this)

        normalScreen.orientation = LinearLayout.VERTICAL

        normalScreen.setBackgroundColor(bg)

        normalScreen.isFocusable = false
        normalScreen.isFocusableInTouchMode = false

        root.addView(
            normalScreen,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        createHeader()

        // -----------------------------------------------------
        // CONTENT ROW
        // -----------------------------------------------------

        contentRow = LinearLayout(this)

        contentRow.orientation = LinearLayout.HORIZONTAL

        contentRow.layoutDirection = View.LAYOUT_DIRECTION_LTR

        contentRow.isFocusable = false
        contentRow.isFocusableInTouchMode = false

        contentRow.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        normalScreen.addView(
            contentRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // -----------------------------------------------------
        // PACKAGES
        // -----------------------------------------------------

        createPackagesPanel()

        // -----------------------------------------------------
        // CHANNELS
        // -----------------------------------------------------

        createChannelsPanel()

        // -----------------------------------------------------
        // PLAYER
        // -----------------------------------------------------

        createPlayerPanel()

        // -----------------------------------------------------
        // FULLSCREEN CONTAINER
        // -----------------------------------------------------

        fullscreenContainer = FrameLayout(this)

        fullscreenContainer.setBackgroundColor(Color.BLACK)

        fullscreenContainer.visibility = View.GONE

        fullscreenContainer.isFocusable = false

        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    // =========================================================
    // HEADER
    // =========================================================

    private fun createHeader() {

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL

        header.gravity = Gravity.CENTER_VERTICAL

        header.layoutDirection = View.LAYOUT_DIRECTION_LTR

        header.setPadding(
            dp(22),
            dp(10),
            dp(22),
            dp(8)
        )

        header.setBackgroundColor(Color.rgb(7, 11, 18))

        normalScreen.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
        )

        // -----------------------------------------------------
        // BRAND
        // -----------------------------------------------------

        val brandBox = LinearLayout(this)

        brandBox.orientation = LinearLayout.VERTICAL

        brandBox.gravity = Gravity.CENTER_VERTICAL

        brandBox.isFocusable = false

        header.addView(
            brandBox,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        val brand = TextView(this)

        brand.text = "NIYATI"

        brand.textSize = 22f

        brand.setTextColor(white)

        brand.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        brand.letterSpacing = 0.12f

        brandBox.addView(brand)

        val subtitle = TextView(this)

        subtitle.text = "SPORTS IPTV"

        subtitle.textSize = 9f

        subtitle.setTextColor(cyan)

        subtitle.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        subtitle.letterSpacing = 0.18f

        brandBox.addView(subtitle)

        // -----------------------------------------------------
        // LIVE INDICATOR
        // -----------------------------------------------------

        val liveBox = LinearLayout(this)

        liveBox.orientation = LinearLayout.HORIZONTAL

        liveBox.gravity = Gravity.CENTER

        liveBox.setPadding(
            dp(12),
            0,
            dp(12),
            0
        )

        liveBox.background =
            roundedBackground(
                Color.rgb(12, 25, 25),
                Color.rgb(22, 90, 82),
                20
            )

        val dot = TextView(this)

        dot.text = "●"

        dot.textSize = 10f

        dot.setTextColor(green)

        liveBox.addView(
            dot,
            LinearLayout.LayoutParams(
                dp(18),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val liveText = TextView(this)

        liveText.text = "LIVE"

        liveText.textSize = 10f

        liveText.setTextColor(white)

        liveText.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        liveBox.addView(liveText)

        header.addView(
            liveBox,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(34)
            )
        )
    }

    // =========================================================
    // PACKAGES PANEL
    // =========================================================

    private fun createPackagesPanel() {

        packagesPanel = createPanel()

        contentRow.addView(
            packagesPanel,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.23f
            ).apply {
                marginEnd = dp(5)
            }
        )

        val title = createPanelTitle(
            "الباقات",
            "PACKAGES"
        )

        packagesPanel.addView(title)

        packagesScroll = ScrollView(this)

        packagesScroll.isFocusable = false
        packagesScroll.isFocusableInTouchMode = false
        packagesScroll.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        packagesScroll.isFillViewport = true

        packagesList = LinearLayout(this)

        packagesList.orientation = LinearLayout.VERTICAL

        packagesList.setPadding(
            dp(8),
            dp(4),
            dp(8),
            dp(10)
        )

        packagesList.isFocusable = false

        packagesScroll.addView(
            packagesList,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        packagesPanel.addView(
            packagesScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    // =========================================================
    // CHANNELS PANEL
    // =========================================================

    private fun createChannelsPanel() {

        channelsPanel = createPanel()

        contentRow.addView(
            channelsPanel,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.32f
            ).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        val titleBox = LinearLayout(this)

        titleBox.orientation = LinearLayout.VERTICAL

        titleBox.setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(8)
        )

        titleBox.isFocusable = false

        channelsPanel.addView(
            titleBox,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(68)
            )
        )

        val title = TextView(this)

        title.text = "القنوات"

        title.textSize = 17f

        title.setTextColor(white)

        title.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        title.gravity = Gravity.RIGHT

        title.layoutDirection = View.LAYOUT_DIRECTION_RTL

        titleBox.addView(title)

        channelTitle = title

        selectedPackageLabel = TextView(this)

        selectedPackageLabel.text = "اختر باقة"

        selectedPackageLabel.textSize = 9f

        selectedPackageLabel.setTextColor(cyan)

        selectedPackageLabel.gravity = Gravity.RIGHT

        selectedPackageLabel.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        titleBox.addView(
            selectedPackageLabel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        channelsScroll = ScrollView(this)

        channelsScroll.isFocusable = false
        channelsScroll.isFocusableInTouchMode = false

        channelsScroll.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        channelsScroll.isFillViewport = true

        channelsList = LinearLayout(this)

        channelsList.orientation = LinearLayout.VERTICAL

        channelsList.setPadding(
            dp(8),
            dp(4),
            dp(8),
            dp(10)
        )

        channelsList.isFocusable = false

        channelsScroll.addView(
            channelsList,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        channelsPanel.addView(
            channelsScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    // =========================================================
    // PLAYER PANEL
    // =========================================================

    private fun createPlayerPanel() {

        playerPanel = createPanel()

        contentRow.addView(
            playerPanel,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.45f
            ).apply {
                marginStart = dp(5)
            }
        )

        // -----------------------------------------------------
        // PLAYER HEADER
        // -----------------------------------------------------

        val playerHeader = LinearLayout(this)

        playerHeader.orientation = LinearLayout.HORIZONTAL

        playerHeader.gravity = Gravity.CENTER_VERTICAL

        playerHeader.layoutDirection = View.LAYOUT_DIRECTION_LTR

        playerHeader.setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(8)
        )

        playerHeader.isFocusable = false

        playerPanel.addView(
            playerHeader,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        val titleBox = LinearLayout(this)

        titleBox.orientation = LinearLayout.VERTICAL

        titleBox.gravity = Gravity.CENTER_VERTICAL

        titleBox.isFocusable = false

        playerHeader.addView(
            titleBox,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        playerTitle = TextView(this)

        playerTitle.text = "المشغل"

        playerTitle.textSize = 16f

        playerTitle.setTextColor(white)

        playerTitle.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        playerTitle.gravity = Gravity.RIGHT

        playerTitle.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        titleBox.addView(playerTitle)

        playerStatus = TextView(this)

        playerStatus.text = "اختر قناة للبدء"

        playerStatus.textSize = 9f

        playerStatus.setTextColor(textSecondary)

        playerStatus.gravity = Gravity.RIGHT

        playerStatus.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        titleBox.addView(playerStatus)

        // -----------------------------------------------------
        // PLAYER AREA
        // -----------------------------------------------------

        playerContainer = FrameLayout(this)

        playerContainer.setBackground(
            roundedBackground(
                Color.BLACK,
                Color.rgb(28, 36, 48),
                14
            )
        )

        playerContainer.setPadding(
            dp(2),
            dp(2),
            dp(2),
            dp(2)
        )

        playerContainer.isFocusable = true
        playerContainer.isFocusableInTouchMode = true

        playerContainer.descendantFocusability =
            ViewGroup.FOCUS_BLOCK_DESCENDANTS

        playerContainer.setOnFocusChangeListener { view, hasFocus ->

            currentSection = 2

            updatePlayerFocus(
                view,
                hasFocus
            )
        }

        playerContainer.setOnClickListener {

            if (!isFullscreen) {
                enterFullscreen()
            }
        }

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        playerPanel.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(
                    dp(8),
                    dp(2),
                    dp(8),
                    dp(8)
                )
            }
        )
    }

    // =========================================================
    // PANEL CREATION
    // =========================================================

    private fun createPanel(): LinearLayout {

        val layout = LinearLayout(this)

        layout.orientation = LinearLayout.VERTICAL

        layout.setBackground(
            roundedBackground(
                panel,
                Color.rgb(25, 33, 45),
                16
            )
        )

        // مهم:
        // اللوحة نفسها لا تأخذ Focus.
        layout.isFocusable = false
        layout.isFocusableInTouchMode = false

        layout.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        return layout
    }

    // =========================================================
    // PANEL TITLE
    // =========================================================

    private fun createPanelTitle(
        arabic: String,
        english: String
    ): LinearLayout {

        val box = LinearLayout(this)

        box.orientation = LinearLayout.VERTICAL

        box.gravity = Gravity.CENTER_VERTICAL

        box.setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(6)
        )

        box.isFocusable = false

        val ar = TextView(this)

        ar.text = arabic

        ar.textSize = 17f

        ar.setTextColor(white)

        ar.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        ar.gravity = Gravity.RIGHT

        ar.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        box.addView(ar)

        val en = TextView(this)

        en.text = english

        en.textSize = 8f

        en.setTextColor(cyan)

        en.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        en.letterSpacing = 0.14f

        en.gravity = Gravity.RIGHT

        box.addView(en)

        box.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )

        return box
    }

    // =========================================================
    // FIREBASE
    // =========================================================

    private fun loadFirebaseData() {

        database.get().addOnSuccessListener { snapshot ->

            packages.clear()
            allChannels.clear()

            loadPackagesFromSnapshot(snapshot)

            loadChannelsFromSnapshot(snapshot)

            renderPackages()

            if (packages.isNotEmpty()) {

                selectedPackageIndex = 0

                selectPackage(
                    packages[0],
                    false
                )

            } else {

                showEmptyPackages()
            }

        }.addOnFailureListener {

            Toast.makeText(
                this,
                "تعذر تحميل بيانات التطبيق",
                Toast.LENGTH_LONG
            ).show()

            showEmptyPackages()
        }
    }

    // =========================================================
    // LOAD PACKAGES
    // =========================================================

    private fun loadPackagesFromSnapshot(
        rootSnapshot: DataSnapshot
    ) {

        val packageSnapshot =
            rootSnapshot.child("packages")

        for (item in packageSnapshot.children) {

            val id = item.key ?: continue

            val name =
                item.child("name")
                    .getValue(String::class.java)
                    ?: id

            val logo =
                item.child("logo")
                    .getValue(String::class.java)
                    ?: ""

            val enabled =
                item.child("enabled")
                    .getValue(Boolean::class.java)
                    ?: true

            val order =
                item.child("order")
                    .getValue(Int::class.java)
                    ?: 0

            if (!enabled) continue

            packages.add(
                PackageItem(
                    id = id,
                    name = name,
                    logo = logo,
                    enabled = enabled,
                    order = order
                )
            )
        }

        packages.sortBy {
            it.order
        }
    }

    // =========================================================
    // LOAD CHANNELS
    // =========================================================

    private fun loadChannelsFromSnapshot(
        rootSnapshot: DataSnapshot
    ) {

        val channelSnapshot =
            rootSnapshot.child("channels")

        for (item in channelSnapshot.children) {

            val id = item.key ?: continue

            val name =
                item.child("name")
                    .getValue(String::class.java)
                    ?: continue

            val group =
                item.child("group")
                    .getValue(String::class.java)
                    ?: continue

            val url =
                item.child("url")
                    .getValue(String::class.java)
                    ?: ""

            val logo =
                item.child("logo")
                    .getValue(String::class.java)
                    ?: ""

            val enabled =
                item.child("enabled")
                    .getValue(Boolean::class.java)
                    ?: true

            val order =
                item.child("order")
                    .getValue(Int::class.java)
                    ?: 0

            if (!enabled) continue

            allChannels.add(
                Channel(
                    id = id,
                    name = name,
                    group = group,
                    url = url,
                    logo = logo,
                    enabled = enabled,
                    order = order
                )
            )
        }

        allChannels.sortBy {
            it.order
        }
    }

    // =========================================================
    // RENDER PACKAGES
    // =========================================================

    private fun renderPackages() {

        packagesList.removeAllViews()

        if (packages.isEmpty()) {
            showEmptyPackages()
            return
        }

        for ((index, item) in packages.withIndex()) {

            val cardView =
                createPackageCard(
                    item,
                    index
                )

            packagesList.addView(cardView)
        }
    }

    // =========================================================
    // PACKAGE CARD
    // =========================================================

    private fun createPackageCard(
        item: PackageItem,
        index: Int
    ): View {

        val cardLayout = LinearLayout(this)

        cardLayout.orientation =
            LinearLayout.HORIZONTAL

        cardLayout.gravity =
            Gravity.CENTER_VERTICAL

        cardLayout.layoutDirection =
            View.LAYOUT_DIRECTION_LTR

        cardLayout.setPadding(
            dp(12),
            0,
            dp(10),
            0
        )

        cardLayout.background =
            roundedBackground(
                card,
                Color.TRANSPARENT,
                12
            )

        cardLayout.isFocusable = true
        cardLayout.isFocusableInTouchMode = true
        cardLayout.isClickable = true

        cardLayout.tag = "PACKAGE_CARD"

        cardLayout.id = View.generateViewId()

        // -----------------------------------------------------
        // FOCUS
        // -----------------------------------------------------

        cardLayout.setOnFocusChangeListener { view, hasFocus ->

            if (hasFocus) {

                currentSection = 0

                selectedPackageIndex = index

                updateCardFocus(
                    view,
                    true
                )
            } else {

                updateCardFocus(
                    view,
                    false
                )
            }
        }

        // -----------------------------------------------------
        // CLICK
        // -----------------------------------------------------

        cardLayout.setOnClickListener {

            selectedPackageIndex = index

            selectPackage(
                item,
                true
            )
        }

        // -----------------------------------------------------
        // ICON
        // -----------------------------------------------------

        val icon = TextView(this)

        icon.text = "▰"

        icon.textSize = 16f

        icon.setTextColor(cyan)

        icon.gravity = Gravity.CENTER

        icon.background =
            roundedBackground(
                Color.rgb(10, 28, 38),
                Color.TRANSPARENT,
                9
            )

        cardLayout.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(38),
                dp(38)
            ).apply {
                marginEnd = dp(10)
            }
        )

        // -----------------------------------------------------
        // TEXT
        // -----------------------------------------------------

        val textBox = LinearLayout(this)

        textBox.orientation =
            LinearLayout.VERTICAL

        textBox.gravity =
            Gravity.CENTER_VERTICAL

        textBox.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        val name = TextView(this)

        name.text = item.name

        name.textSize = 14f

        name.setTextColor(white)

        name.typeface =
            Typeface.create("sans-serif", Typeface.BOLD)

        name.gravity = Gravity.RIGHT

        name.maxLines = 1

        textBox.addView(name)

        val small = TextView(this)

        small.text = "SPORTS"

        small.textSize = 7f

        small.setTextColor(textSecondary)

        small.letterSpacing = 0.12f

        small.gravity = Gravity.RIGHT

        textBox.addView(small)

        cardLayout.addView(
            textBox,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        // -----------------------------------------------------
        // ARROW
        // -----------------------------------------------------

        val arrow = TextView(this)

        arrow.text = "‹"

        arrow.textSize = 23f

        arrow.setTextColor(textMuted)

        arrow.gravity = Gravity.CENTER

        cardLayout.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(22),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        cardLayout.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
            ).apply {
                bottomMargin = dp(6)
            }

        return cardLayout
    }

    // =========================================================
    // SELECT PACKAGE
    // =========================================================

    private fun selectPackage(
        packageItem: PackageItem,
        moveToChannels: Boolean
    ) {

        selectedPackage = packageItem

        selectedChannel = null

        selectedChannelIndex = 0

        selectedPackageLabel.text =
            packageItem.name

        renderChannels()

        if (moveToChannels) {

            val first =
                channelsList.getChildAt(0)

            if (first != null) {

                currentSection = 1

                first.requestFocus()

            }
        }
    }

    // =========================================================
    // RENDER CHANNELS
    // =========================================================

    private fun renderChannels() {

        channelsList.removeAllViews()

        val selectedGroup =
            selectedPackage?.id

        val filtered =
            if (selectedGroup.isNullOrBlank()) {
                emptyList()
            } else {
                allChannels.filter {
                    it.group.equals(
                        selectedGroup,
                        ignoreCase = true
                    )
                }
            }

        if (filtered.isEmpty()) {

            val empty =
                TextView(this)

            empty.text =
                if (selectedPackage == null) {
                    "اختر باقة أولاً"
                } else {
                    "لا توجد قنوات داخل هذه الباقة"
                }

            empty.textSize = 12f

            empty.setTextColor(textSecondary)

            empty.gravity = Gravity.CENTER

            empty.layoutDirection =
                View.LAYOUT_DIRECTION_RTL

            channelsList.addView(
                empty,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(100)
                )
            )

            return
        }

        for ((index, channel) in filtered.withIndex()) {

            val cardView =
                createChannelCard(
                    channel,
                    index
                )

            channelsList.addView(cardView)
        }
    }

    // =========================================================
    // CHANNEL CARD
    // =========================================================

    private fun createChannelCard(
        channel: Channel,
        index: Int
    ): View {

        val cardLayout = LinearLayout(this)

        cardLayout.orientation =
            LinearLayout.HORIZONTAL

        cardLayout.gravity =
            Gravity.CENTER_VERTICAL

        cardLayout.layoutDirection =
            View.LAYOUT_DIRECTION_LTR

        cardLayout.setPadding(
            dp(10),
            0,
            dp(10),
            0
        )

        cardLayout.background =
            roundedBackground(
                card,
                Color.TRANSPARENT,
                12
            )

        cardLayout.isFocusable = true
        cardLayout.isFocusableInTouchMode = true
        cardLayout.isClickable = true

        cardLayout.tag = "CHANNEL_CARD"

        cardLayout.id = View.generateViewId()

        // -----------------------------------------------------
        // FOCUS
        // -----------------------------------------------------

        cardLayout.setOnFocusChangeListener { view, hasFocus ->

            if (hasFocus) {

                currentSection = 1

                selectedChannelIndex = index

                updateCardFocus(
                    view,
                    true
                )

            } else {

                updateCardFocus(
                    view,
                    false
                )
            }
        }

        // -----------------------------------------------------
        // CLICK
        // -----------------------------------------------------

        cardLayout.setOnClickListener {

            selectedChannelIndex = index

            playChannel(channel)

            playerContainer.requestFocus()
        }

        // -----------------------------------------------------
        // CHANNEL NUMBER
        // -----------------------------------------------------

        val number = TextView(this)

        number.text =
            String.format(
                "%02d",
                index + 1
            )

        number.textSize = 10f

        number.setTextColor(textMuted)

        number.gravity = Gravity.CENTER

        cardLayout.addView(
            number,
            LinearLayout.LayoutParams(
                dp(30),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // -----------------------------------------------------
        // ICON
        // -----------------------------------------------------

        val icon = TextView(this)

        icon.text = "▶"

        icon.textSize = 12f

        icon.setTextColor(cyan)

        icon.gravity = Gravity.CENTER

        icon.background =
            roundedBackground(
                Color.rgb(8, 29, 38),
                Color.TRANSPARENT,
                9
            )

        cardLayout.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(38),
                dp(38)
            ).apply {
                marginStart = dp(6)
                marginEnd = dp(10)
            }
        )

        // -----------------------------------------------------
        // CHANNEL NAME
        // -----------------------------------------------------

        val name = TextView(this)

        name.text = channel.name

        name.textSize = 13f

        name.setTextColor(white)

        name.typeface =
            Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

        name.gravity = Gravity.RIGHT

        name.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        name.maxLines = 1

        cardLayout.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        val status = TextView(this)

        status.text =
            if (channel.url.isBlank()) {
                "OFF"
            } else {
                "HD"
            }

        status.textSize = 8f

        status.setTextColor(
            if (channel.url.isBlank()) {
                red
            } else {
                green
            }
        )

        status.typeface =
            Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

        status.gravity = Gravity.CENTER

        cardLayout.addView(
            status,
            LinearLayout.LayoutParams(
                dp(34),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        cardLayout.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                bottomMargin = dp(6)
            }

        return cardLayout
    }

    // =========================================================
    // PLAY CHANNEL
    // =========================================================

    private fun playChannel(channel: Channel) {

        selectedChannel = channel

        playerTitle.text =
            channel.name

        if (channel.url.isBlank()) {

            playerStatus.text =
                "لا يوجد رابط بث"

            playerStatus.setTextColor(red)

            Toast.makeText(
                this,
                "هذه القناة لا تحتوي على رابط بث حالياً",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        playerStatus.text =
            "جاري التشغيل"

        playerStatus.setTextColor(green)

        try {

            val mediaItem =
                MediaItem.fromUri(
                    Uri.parse(channel.url)
                )

            player.setMediaItem(mediaItem)

            player.prepare()

            player.playWhenReady = true

            playerReady = true

        } catch (e: Exception) {

            playerStatus.text =
                "تعذر تشغيل القناة"

            playerStatus.setTextColor(red)

            Toast.makeText(
                this,
                "خطأ في تشغيل البث",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // FOCUS CARD
    // =========================================================

    private fun updateCardFocus(
        view: View,
        focused: Boolean
    ) {

        if (focused) {

            view.background =
                roundedBackground(
                    cardSelected,
                    cyan,
                    12
                )

            view.scaleX = 1.015f
            view.scaleY = 1.015f

        } else {

            view.background =
                roundedBackground(
                    card,
                    Color.TRANSPARENT,
                    12
                )

            view.scaleX = 1f
            view.scaleY = 1f
        }
    }

    // =========================================================
    // PLAYER FOCUS
    // =========================================================

    private fun updatePlayerFocus(
        view: View,
        focused: Boolean
    ) {

        if (focused) {

            view.background =
                roundedBackground(
                    Color.BLACK,
                    cyan,
                    14
                )

            view.scaleX = 1.008f
            view.scaleY = 1.008f

        } else {

            view.background =
                roundedBackground(
                    Color.BLACK,
                    Color.rgb(28, 36, 48),
                    14
                )

            view.scaleX = 1f
            view.scaleY = 1f
        }
    }

    // =========================================================
    // EMPTY PACKAGES
    // =========================================================

    private fun showEmptyPackages() {

        packagesList.removeAllViews()

        val text = TextView(this)

        text.text =
            "لا توجد باقات حالياً"

        text.textSize = 12f

        text.setTextColor(textSecondary)

        text.gravity = Gravity.CENTER

        text.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        packagesList.addView(
            text,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(100)
            )
        )
    }

    // =========================================================
    // KEY NAVIGATION
    // =========================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        when (event.keyCode) {

            // -------------------------------------------------
            // RIGHT
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (isFullscreen) {
                    return true
                }

                when (currentSection) {

                    0 -> {

                        focusFirstChannel()

                        return true
                    }

                    1 -> {

                        focusPlayer()

                        return true
                    }

                    2 -> {

                        return true
                    }
                }
            }

            // -------------------------------------------------
            // LEFT
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (isFullscreen) {
                    return true
                }

                when (currentSection) {

                    0 -> {

                        return true
                    }

                    1 -> {

                        focusSelectedPackage()

                        return true
                    }

                    2 -> {

                        focusSelectedChannel()

                        return true
                    }
                }
            }

            // -------------------------------------------------
            // OK / ENTER
            // -------------------------------------------------

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {

                if (isFullscreen) {
                    return true
                }

                if (currentSection == 2) {

                    enterFullscreen()

                    return true
                }
            }

            // -------------------------------------------------
            // BACK
            // -------------------------------------------------

            KeyEvent.KEYCODE_BACK -> {

                if (isFullscreen) {

                    exitFullscreen()

                    return true
                }

                when (currentSection) {

                    2 -> {

                        focusSelectedChannel()

                        return true
                    }

                    1 -> {

                        focusSelectedPackage()

                        return true
                    }

                    0 -> {

                        // لا يغلق التطبيق
                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // =========================================================
    // FOCUS FIRST CHANNEL
    // =========================================================

    private fun focusFirstChannel() {

        if (channelsList.childCount <= 0) {
            return
        }

        currentSection = 1

        selectedChannelIndex = 0

        channelsList.getChildAt(0)?.requestFocus()
    }

    // =========================================================
    // FOCUS PLAYER
    // =========================================================

    private fun focusPlayer() {

        currentSection = 2

        playerContainer.requestFocus()
    }

    // =========================================================
    // FOCUS SELECTED PACKAGE
    // =========================================================

    private fun focusSelectedPackage() {

        if (packagesList.childCount <= 0) {
            return
        }

        currentSection = 0

        val index =
            selectedPackageIndex.coerceIn(
                0,
                packagesList.childCount - 1
            )

        packagesList
            .getChildAt(index)
            ?.requestFocus()
    }

    // =========================================================
    // FOCUS SELECTED CHANNEL
    // =========================================================

    private fun focusSelectedChannel() {

        if (channelsList.childCount <= 0) {

            focusSelectedPackage()

            return
        }

        currentSection = 1

        val index =
            selectedChannelIndex.coerceIn(
                0,
                channelsList.childCount - 1
            )

        channelsList
            .getChildAt(index)
            ?.requestFocus()
    }

    // =========================================================
    // FULLSCREEN
    // =========================================================

    private fun enterFullscreen() {

        if (isFullscreen) {
            return
        }

        isFullscreen = true

        normalScreen.visibility =
            View.GONE

        fullscreenContainer.visibility =
            View.VISIBLE

        // إزالة player من الواجهة القديمة
        val oldParent =
            playerView.parent as? ViewGroup

        oldParent?.removeView(playerView)

        fullscreenContainer.removeAllViews()

        fullscreenContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        hideSystemBars()

        playerView.isFocusable = false
        playerView.isFocusableInTouchMode = false
    }

    // =========================================================
    // EXIT FULLSCREEN
    // =========================================================

    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        isFullscreen = false

        val oldParent =
            playerView.parent as? ViewGroup

        oldParent?.removeView(playerView)

        fullscreenContainer.removeAllViews()

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        fullscreenContainer.visibility =
            View.GONE

        normalScreen.visibility =
            View.VISIBLE

        hideSystemBars()

        currentSection = 2

        playerContainer.requestFocus()
    }

    // =========================================================
    // BACK PRESS
    // =========================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (isFullscreen) {

            exitFullscreen()

            return
        }

        when (currentSection) {

            2 -> focusSelectedChannel()

            1 -> focusSelectedPackage()

            0 -> {
                // التطبيق لا يغلق
            }
        }
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onStart() {

        super.onStart()

        if (::player.isInitialized) {

            player.playWhenReady =
                playerReady
        }
    }

    override fun onStop() {

        super.onStop()

        if (::player.isInitialized) {

            player.playWhenReady = false
        }
    }

    override fun onDestroy() {

        if (::player.isInitialized) {

            player.release()
        }

        super.onDestroy()
    }

    // =========================================================
    // DRAWABLE HELPERS
    // =========================================================

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: Int,
        radiusDp: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            setColor(fillColor)

            cornerRadius =
                dp(radiusDp).toFloat()

            if (strokeColor != Color.TRANSPARENT) {

                setStroke(
                    dp(2),
                    strokeColor
                )
            }
        }
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(value: Int): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}