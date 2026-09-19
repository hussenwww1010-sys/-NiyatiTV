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
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
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

    private val BG = Color.rgb(4, 7, 13)
    private val PANEL = Color.rgb(10, 15, 24)
    private val CARD = Color.rgb(17, 24, 36)
    private val CARD_FOCUS = Color.rgb(22, 36, 50)

    private val CYAN = Color.rgb(0, 229, 255)
    private val WHITE = Color.rgb(245, 248, 252)
    private val SECONDARY = Color.rgb(145, 158, 177)
    private val MUTED = Color.rgb(80, 94, 113)

    private val GREEN = Color.rgb(16, 185, 129)
    private val RED = Color.rgb(239, 68, 68)

    // =========================================================
    // FIREBASE
    // =========================================================

    private val databaseUrl =
        "https://niyati-tv-default-rtdb.europe-west1.firebasedatabase.app"

    private lateinit var database: com.google.firebase.database.DatabaseReference

    // =========================================================
    // PLAYER
    // =========================================================

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    private var playerCreated = false
    private var isFullscreen = false

    // =========================================================
    // ROOT
    // =========================================================

    private lateinit var root: FrameLayout
    private lateinit var mainScreen: LinearLayout
    private lateinit var contentRow: LinearLayout

    // =========================================================
    // PACKAGES
    // =========================================================

    private lateinit var packagesPanel: LinearLayout
    private lateinit var packagesList: LinearLayout
    private lateinit var packagesScroll: ScrollView

    // =========================================================
    // CHANNELS
    // =========================================================

    private lateinit var channelsPanel: LinearLayout
    private lateinit var channelsList: LinearLayout
    private lateinit var channelsScroll: ScrollView
    private lateinit var selectedPackageText: TextView

    // =========================================================
    // PLAYER
    // =========================================================

    private lateinit var playerPanel: LinearLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var playerTitle: TextView
    private lateinit var playerStatus: TextView

    // =========================================================
    // FULLSCREEN
    // =========================================================

    private lateinit var fullscreenContainer: FrameLayout

    // =========================================================
    // DATA
    // =========================================================

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    data class Channel(
        val id: String,
        val name: String,
        val group: String,
        val url: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    private val packages = ArrayList<PackageItem>()
    private val channels = ArrayList<Channel>()

    private var selectedPackage: PackageItem? = null
    private var selectedChannel: Channel? = null

    // =========================================================
    // FOCUS
    // =========================================================

    // 0 = packages
    // 1 = channels
    // 2 = player

    private var currentSection = 0

    private var selectedPackageIndex = 0
    private var selectedChannelIndex = 0

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {

            hideSystemBars()

            database =
                FirebaseDatabase
                    .getInstance(databaseUrl)
                    .reference

            createPlayer()

            createUI()

            loadFirebase()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "خطأ في تشغيل التطبيق",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // SYSTEM UI
    // =========================================================

    private fun hideSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false)

            val controller =
                window.insetsController

            if (controller != null) {

                controller.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsController
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

        player =
            ExoPlayer
                .Builder(this)
                .build()

        playerCreated = true

        playerView =
            PlayerView(this)

        playerView.player =
            player

        playerView.useController =
            true

        playerView.setBackgroundColor(
            Color.BLACK
        )

        // اللاعب لا يأخذ التركيز
        playerView.isFocusable = false
        playerView.isFocusableInTouchMode = false
    }

    // =========================================================
    // MAIN UI
    // =========================================================

    private fun createUI() {

        root =
            FrameLayout(this)

        root.setBackgroundColor(
            BG
        )

        setContentView(root)

        // -----------------------------------------------------
        // MAIN SCREEN
        // -----------------------------------------------------

        mainScreen =
            LinearLayout(this)

        mainScreen.orientation =
            LinearLayout.VERTICAL

        mainScreen.setBackgroundColor(
            BG
        )

        mainScreen.isFocusable = false

        root.addView(
            mainScreen,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        createHeader()

        // -----------------------------------------------------
        // CONTENT
        // -----------------------------------------------------

        contentRow =
            LinearLayout(this)

        contentRow.orientation =
            LinearLayout.HORIZONTAL

        contentRow.layoutDirection =
            View.LAYOUT_DIRECTION_LTR

        contentRow.isFocusable = false

        contentRow.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        mainScreen.addView(
            contentRow,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        createPackages()
        createChannels()
        createPlayerPanel()

        // -----------------------------------------------------
        // FULLSCREEN
        // -----------------------------------------------------

        fullscreenContainer =
            FrameLayout(this)

        fullscreenContainer.setBackgroundColor(
            Color.BLACK
        )

        fullscreenContainer.visibility =
            View.GONE

        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )
    }

    // =========================================================
    // HEADER
    // =========================================================

    private fun createHeader() {

        val header =
            LinearLayout(this)

        header.orientation =
            LinearLayout.HORIZONTAL

        header.gravity =
            Gravity.CENTER_VERTICAL

        header.setPadding(
            dp(20),
            dp(8),
            dp(20),
            dp(8)
        )

        header.setBackgroundColor(
            Color.rgb(7, 11, 18)
        )

        mainScreen.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        val brandBox =
            LinearLayout(this)

        brandBox.orientation =
            LinearLayout.VERTICAL

        brandBox.gravity =
            Gravity.CENTER_VERTICAL

        header.addView(
            brandBox,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        val brand =
            TextView(this)

        brand.text =
            "NIYATI"

        brand.textSize =
            21f

        brand.setTextColor(
            WHITE
        )

        brand.typeface =
            Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

        brand.letterSpacing =
            0.12f

        brandBox.addView(
            brand
        )

        val sub =
            TextView(this)

        sub.text =
            "SPORTS IPTV"

        sub.textSize =
            8f

        sub.setTextColor(
            CYAN
        )

        sub.typeface =
            Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

        sub.letterSpacing =
            0.18f

        brandBox.addView(
            sub
        )

        val live =
            TextView(this)

        live.text =
            "●  LIVE"

        live.textSize =
            10f

        live.setTextColor(
            GREEN
        )

        live.gravity =
            Gravity.CENTER

        live.background =
            background(
                Color.rgb(10, 25, 25),
                Color.rgb(25, 70, 65),
                20
            )

        live.setPadding(
            dp(12),
            0,
            dp(12),
            0
        )

        header.addView(
            live,
            LinearLayout.LayoutParams(
                -2,
                dp(32)
            )
        )
    }

    // =========================================================
    // PACKAGES
    // =========================================================

    private fun createPackages() {

        packagesPanel =
            createPanel()

        contentRow.addView(
            packagesPanel,
            LinearLayout.LayoutParams(
                0,
                -1,
                0.23f
            ).apply {
                rightMargin = dp(4)
            }
        )

        val title =
            panelTitle(
                "الباقات",
                "PACKAGES"
            )

        packagesPanel.addView(
            title,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        packagesScroll =
            ScrollView(this)

        packagesScroll.isFocusable =
            false

        packagesScroll.isFocusableInTouchMode =
            false

        packagesScroll.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        packagesList =
            LinearLayout(this)

        packagesList.orientation =
            LinearLayout.VERTICAL

        packagesList.setPadding(
            dp(7),
            dp(4),
            dp(7),
            dp(10)
        )

        packagesScroll.addView(
            packagesList,
            ViewGroup.LayoutParams(
                -1,
                -2
            )
        )

        packagesPanel.addView(
            packagesScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )
    }

    // =========================================================
    // CHANNELS
    // =========================================================

    private fun createChannels() {

        channelsPanel =
            createPanel()

        contentRow.addView(
            channelsPanel,
            LinearLayout.LayoutParams(
                0,
                -1,
                0.32f
            ).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            }
        )

        val titleBox =
            LinearLayout(this)

        titleBox.orientation =
            LinearLayout.VERTICAL

        titleBox.setPadding(
            dp(14),
            dp(9),
            dp(14),
            dp(5)
        )

        channelsPanel.addView(
            titleBox,
            LinearLayout.LayoutParams(
                -1,
                dp(68)
            )
        )

        val title =
            TextView(this)

        title.text =
            "القنوات"

        title.textSize =
            17f

        title.setTextColor(
            WHITE
        )

        title.typeface =
            Typeface.DEFAULT_BOLD

        title.gravity =
            Gravity.RIGHT

        titleBox.addView(
            title
        )

        selectedPackageText =
            TextView(this)

        selectedPackageText.text =
            "اختر باقة"

        selectedPackageText.textSize =
            9f

        selectedPackageText.setTextColor(
            CYAN
        )

        selectedPackageText.gravity =
            Gravity.RIGHT

        titleBox.addView(
            selectedPackageText
        )

        channelsScroll =
            ScrollView(this)

        channelsScroll.isFocusable =
            false

        channelsScroll.isFocusableInTouchMode =
            false

        channelsScroll.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        channelsList =
            LinearLayout(this)

        channelsList.orientation =
            LinearLayout.VERTICAL

        channelsList.setPadding(
            dp(7),
            dp(4),
            dp(7),
            dp(10)
        )

        channelsScroll.addView(
            channelsList,
            ViewGroup.LayoutParams(
                -1,
                -2
            )
        )

        channelsPanel.addView(
            channelsScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )
    }

    // =========================================================
    // PLAYER PANEL
    // =========================================================

    private fun createPlayerPanel() {

        playerPanel =
            createPanel()

        contentRow.addView(
            playerPanel,
            LinearLayout.LayoutParams(
                0,
                -1,
                0.45f
            ).apply {
                leftMargin = dp(4)
            }
        )

        val header =
            LinearLayout(this)

        header.orientation =
            LinearLayout.VERTICAL

        header.gravity =
            Gravity.CENTER_VERTICAL

        header.setPadding(
            dp(14),
            dp(8),
            dp(14),
            dp(5)
        )

        playerPanel.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            )
        )

        playerTitle =
            TextView(this)

        playerTitle.text =
            "المشغل"

        playerTitle.textSize =
            16f

        playerTitle.setTextColor(
            WHITE
        )

        playerTitle.typeface =
            Typeface.DEFAULT_BOLD

        playerTitle.gravity =
            Gravity.RIGHT

        header.addView(
            playerTitle
        )

        playerStatus =
            TextView(this)

        playerStatus.text =
            "اختر قناة للبدء"

        playerStatus.textSize =
            9f

        playerStatus.setTextColor(
            SECONDARY
        )

        playerStatus.gravity =
            Gravity.RIGHT

        header.addView(
            playerStatus
        )

        // -----------------------------------------------------
        // PLAYER CONTAINER
        // -----------------------------------------------------

        playerContainer =
            FrameLayout(this)

        playerContainer.background =
            background(
                Color.BLACK,
                Color.rgb(28, 36, 48),
                14
            )

        playerContainer.setPadding(
            dp(2),
            dp(2),
            dp(2),
            dp(2)
        )

        playerContainer.isFocusable =
            true

        playerContainer.isFocusableInTouchMode =
            true

        // لا نخلي PlayerView يأخذ focus
        playerContainer.descendantFocusability =
            ViewGroup.FOCUS_BLOCK_DESCENDANTS

        playerContainer.setOnFocusChangeListener {
                view,
                focused ->

            currentSection = 2

            updatePlayerFocus(
                view,
                focused
            )
        }

        playerContainer.setOnClickListener {

            enterFullscreen()
        }

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        playerPanel.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            ).apply {
                setMargins(
                    dp(7),
                    dp(2),
                    dp(7),
                    dp(7)
                )
            }
        )
    }

    // =========================================================
    // PANEL
    // =========================================================

    private fun createPanel(): LinearLayout {

        val panel =
            LinearLayout(this)

        panel.orientation =
            LinearLayout.VERTICAL

        panel.background =
            background(
                PANEL,
                Color.rgb(24, 32, 44),
                15
            )

        panel.isFocusable =
            false

        panel.isFocusableInTouchMode =
            false

        panel.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        return panel
    }

    // =========================================================
    // PANEL TITLE
    // =========================================================

    private fun panelTitle(
        arabic: String,
        english: String
    ): LinearLayout {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        box.gravity =
            Gravity.CENTER_VERTICAL

        box.setPadding(
            dp(14),
            dp(8),
            dp(14),
            dp(4)
        )

        val ar =
            TextView(this)

        ar.text =
            arabic

        ar.textSize =
            17f

        ar.setTextColor(
            WHITE
        )

        ar.typeface =
            Typeface.DEFAULT_BOLD

        ar.gravity =
            Gravity.RIGHT

        box.addView(
            ar
        )

        val en =
            TextView(this)

        en.text =
            english

        en.textSize =
            8f

        en.setTextColor(
            CYAN
        )

        en.letterSpacing =
            0.15f

        en.gravity =
            Gravity.RIGHT

        box.addView(
            en
        )

        return box
    }

    // =========================================================
    // FIREBASE
    // =========================================================

    private fun loadFirebase() {

        database.get()
            .addOnSuccessListener { snapshot ->

                try {

                    packages.clear()
                    channels.clear()

                    readPackages(snapshot)
                    readChannels(snapshot)

                    renderPackages()

                    if (packages.isNotEmpty()) {

                        selectedPackageIndex = 0

                        selectPackage(
                            packages[0],
                            false
                        )

                    } else {

                        showNoPackages()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "خطأ بقراءة بيانات Firebase",
                        Toast.LENGTH_LONG
                    ).show()

                    showNoPackages()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "تعذر الاتصال بـ Firebase",
                    Toast.LENGTH_LONG
                ).show()

                showNoPackages()
            }
    }

    // =========================================================
    // READ PACKAGES
    // =========================================================

    private fun readPackages(
        snapshot: DataSnapshot
    ) {

        val node =
            snapshot.child("packages")

        for (child in node.children) {

            val id =
                child.key ?: continue

            val name =
                child.child("name")
                    .getValue(String::class.java)
                    ?: id

            val logo =
                child.child("logo")
                    .getValue(String::class.java)
                    ?: ""

            val enabled =
                child.child("enabled")
                    .value as? Boolean
                    ?: true

            val order =
                (child.child("order").value as? Number)
                    ?.toInt()
                    ?: 0

            if (!enabled) {
                continue
            }

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
    // READ CHANNELS
    // =========================================================

    private fun readChannels(
        snapshot: DataSnapshot
    ) {

        val node =
            snapshot.child("channels")

        for (child in node.children) {

            val id =
                child.key ?: continue

            val name =
                child.child("name")
                    .getValue(String::class.java)
                    ?: continue

            val group =
                child.child("group")
                    .getValue(String::class.java)
                    ?: continue

            val url =
                child.child("url")
                    .getValue(String::class.java)
                    ?: ""

            val logo =
                child.child("logo")
                    .getValue(String::class.java)
                    ?: ""

            val enabled =
                child.child("enabled")
                    .value as? Boolean
                    ?: true

            val order =
                (child.child("order").value as? Number)
                    ?.toInt()
                    ?: 0

            if (!enabled) {
                continue
            }

            channels.add(
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

        channels.sortBy {
            it.order
        }
    }

    // =========================================================
    // RENDER PACKAGES
    // =========================================================

    private fun renderPackages() {

        packagesList.removeAllViews()

        for (i in packages.indices) {

            val view =
                createPackageCard(
                    packages[i],
                    i
                )

            packagesList.addView(
                view
            )
        }
    }

    // =========================================================
    // PACKAGE CARD
    // =========================================================

    private fun createPackageCard(
        item: PackageItem,
        index: Int
    ): View {

        val cardView =
            LinearLayout(this)

        cardView.orientation =
            LinearLayout.HORIZONTAL

        cardView.gravity =
            Gravity.CENTER_VERTICAL

        cardView.setPadding(
            dp(10),
            0,
            dp(8),
            0
        )

        cardView.background =
            background(
                CARD,
                Color.TRANSPARENT,
                11
            )

        cardView.isFocusable =
            true

        cardView.isFocusableInTouchMode =
            true

        cardView.isClickable =
            true

        cardView.id =
            View.generateViewId()

        cardView.setOnFocusChangeListener {
                view,
                focused ->

            if (focused) {

                currentSection = 0
                selectedPackageIndex = index
            }

            updateCardFocus(
                view,
                focused
            )
        }

        cardView.setOnClickListener {

            selectedPackageIndex = index

            selectPackage(
                item,
                true
            )
        }

        // icon
        val icon =
            TextView(this)

        icon.text =
            "▰"

        icon.textSize =
            15f

        icon.setTextColor(
            CYAN
        )

        icon.gravity =
            Gravity.CENTER

        icon.background =
            background(
                Color.rgb(8, 27, 37),
                Color.TRANSPARENT,
                8
            )

        cardView.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(38),
                dp(38)
            ).apply {
                rightMargin = dp(9)
            }
        )

        // name
        val name =
            TextView(this)

        name.text =
            item.name

        name.textSize =
            13f

        name.setTextColor(
            WHITE
        )

        name.typeface =
            Typeface.DEFAULT_BOLD

        name.gravity =
            Gravity.RIGHT

        name.maxLines =
            1

        cardView.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        // arrow
        val arrow =
            TextView(this)

        arrow.text =
            "‹"

        arrow.textSize =
            22f

        arrow.setTextColor(
            MUTED
        )

        arrow.gravity =
            Gravity.CENTER

        cardView.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(22),
                -1
            )
        )

        cardView.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                bottomMargin = dp(6)
            }

        return cardView
    }

    // =========================================================
    // SELECT PACKAGE
    // =========================================================

    private fun selectPackage(
        item: PackageItem,
        moveToChannels: Boolean
    ) {

        selectedPackage =
            item

        selectedChannel =
            null

        selectedChannelIndex =
            0

        selectedPackageText.text =
            item.name

        renderChannels()

        if (moveToChannels) {

            currentSection =
                1

            if (channelsList.childCount > 0) {

                channelsList
                    .getChildAt(0)
                    ?.requestFocus()
            }
        }
    }

    // =========================================================
    // RENDER CHANNELS
    // =========================================================

    private fun renderChannels() {

        channelsList.removeAllViews()

        val group =
            selectedPackage?.id

        if (group.isNullOrBlank()) {

            showNoChannels(
                "اختر باقة"
            )

            return
        }

        val filtered =
            channels.filter {
                it.group.equals(
                    group,
                    ignoreCase = true
                )
            }

        if (filtered.isEmpty()) {

            showNoChannels(
                "لا توجد قنوات داخل هذه الباقة"
            )

            return
        }

        for (i in filtered.indices) {

            channelsList.addView(
                createChannelCard(
                    filtered[i],
                    i
                )
            )
        }
    }

    // =========================================================
    // CHANNEL CARD
    // =========================================================

    private fun createChannelCard(
        channel: Channel,
        index: Int
    ): View {

        val cardView =
            LinearLayout(this)

        cardView.orientation =
            LinearLayout.HORIZONTAL

        cardView.gravity =
            Gravity.CENTER_VERTICAL

        cardView.setPadding(
            dp(8),
            0,
            dp(8),
            0
        )

        cardView.background =
            background(
                CARD,
                Color.TRANSPARENT,
                11
            )

        cardView.isFocusable =
            true

        cardView.isFocusableInTouchMode =
            true

        cardView.isClickable =
            true

        cardView.id =
            View.generateViewId()

        cardView.setOnFocusChangeListener {
                view,
                focused ->

            if (focused) {

                currentSection = 1
                selectedChannelIndex = index
            }

            updateCardFocus(
                view,
                focused
            )
        }

        cardView.setOnClickListener {

            selectedChannelIndex =
                index

            playChannel(
                channel
            )

            playerContainer.requestFocus()
        }

        // number
        val number =
            TextView(this)

        number.text =
            String.format(
                "%02d",
                index + 1
            )

        number.textSize =
            9f

        number.setTextColor(
            MUTED
        )

        number.gravity =
            Gravity.CENTER

        cardView.addView(
            number,
            LinearLayout.LayoutParams(
                dp(28),
                -1
            )
        )

        // play icon
        val icon =
            TextView(this)

        icon.text =
            "▶"

        icon.textSize =
            11f

        icon.setTextColor(
            CYAN
        )

        icon.gravity =
            Gravity.CENTER

        icon.background =
            background(
                Color.rgb(8, 27, 37),
                Color.TRANSPARENT,
                8
            )

        cardView.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(36),
                dp(36)
            ).apply {
                leftMargin = dp(5)
                rightMargin = dp(8)
            }
        )

        // name
        val name =
            TextView(this)

        name.text =
            channel.name

        name.textSize =
            12.5f

        name.setTextColor(
            WHITE
        )

        name.typeface =
            Typeface.DEFAULT_BOLD

        name.gravity =
            Gravity.RIGHT

        name.maxLines =
            1

        cardView.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        // status
        val status =
            TextView(this)

        if (channel.url.isBlank()) {

            status.text =
                "OFF"

            status.setTextColor(
                RED
            )

        } else {

            status.text =
                "HD"

            status.setTextColor(
                GREEN
            )
        }

        status.textSize =
            8f

        status.typeface =
            Typeface.DEFAULT_BOLD

        status.gravity =
            Gravity.CENTER

        cardView.addView(
            status,
            LinearLayout.LayoutParams(
                dp(32),
                -1
            )
        )

        cardView.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                dp(57)
            ).apply {
                bottomMargin = dp(5)
            }

        return cardView
    }

    // =========================================================
    // PLAY
    // =========================================================

    private fun playChannel(
        channel: Channel
    ) {

        selectedChannel =
            channel

        playerTitle.text =
            channel.name

        if (channel.url.isBlank()) {

            playerStatus.text =
                "لا يوجد رابط بث"

            playerStatus.setTextColor(
                RED
            )

            Toast.makeText(
                this,
                "هذه القناة لا تحتوي على رابط بث حالياً",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val mediaItem =
                MediaItem.fromUri(
                    Uri.parse(channel.url)
                )

            player.setMediaItem(
                mediaItem
            )

            player.prepare()

            player.playWhenReady =
                true

            playerStatus.text =
                "جاري التشغيل"

            playerStatus.setTextColor(
                GREEN
            )

        } catch (e: Exception) {

            playerStatus.text =
                "تعذر تشغيل القناة"

            playerStatus.setTextColor(
                RED
            )

            Toast.makeText(
                this,
                "تعذر تشغيل البث",
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
                background(
                    CARD_FOCUS,
                    CYAN,
                    11
                )

            view.scaleX =
                1.015f

            view.scaleY =
                1.015f

        } else {

            view.background =
                background(
                    CARD,
                    Color.TRANSPARENT,
                    11
                )

            view.scaleX =
                1f

            view.scaleY =
                1f
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
                background(
                    Color.BLACK,
                    CYAN,
                    14
                )

        } else {

            view.background =
                background(
                    Color.BLACK,
                    Color.rgb(28, 36, 48),
                    14
                )
        }
    }

    // =========================================================
    // EMPTY
    // =========================================================

    private fun showNoPackages() {

        packagesList.removeAllViews()

        val text =
            TextView(this)

        text.text =
            "لا توجد باقات"

        text.textSize =
            12f

        text.setTextColor(
            SECONDARY
        )

        text.gravity =
            Gravity.CENTER

        packagesList.addView(
            text,
            LinearLayout.LayoutParams(
                -1,
                dp(100)
            )
        )
    }

    private fun showNoChannels(
        message: String
    ) {

        val text =
            TextView(this)

        text.text =
            message

        text.textSize =
            12f

        text.setTextColor(
            SECONDARY
        )

        text.gravity =
            Gravity.CENTER

        text.layoutDirection =
            View.LAYOUT_DIRECTION_RTL

        channelsList.addView(
            text,
            LinearLayout.LayoutParams(
                -1,
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

            return super.dispatchKeyEvent(
                event
            )
        }

        when (event.keyCode) {

            // =================================================
            // RIGHT
            // =================================================

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

            // =================================================
            // LEFT
            // =================================================

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (isFullscreen) {
                    return true
                }

                when (currentSection) {

                    0 -> {

                        return true
                    }

                    1 -> {

                        focusPackage()

                        return true
                    }

                    2 -> {

                        focusChannel()

                        return true
                    }
                }
            }

            // =================================================
            // OK
            // =================================================

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

            // =================================================
            // BACK
            // =================================================

            KeyEvent.KEYCODE_BACK -> {

                if (isFullscreen) {

                    exitFullscreen()

                    return true
                }

                when (currentSection) {

                    2 -> {

                        focusChannel()

                        return true
                    }

                    1 -> {

                        focusPackage()

                        return true
                    }

                    0 -> {

                        // لا يغلق التطبيق
                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

    // =========================================================
    // FOCUS FUNCTIONS
    // =========================================================

    private fun focusFirstChannel() {

        if (channelsList.childCount <= 0) {
            return
        }

        currentSection =
            1

        selectedChannelIndex =
            0

        channelsList
            .getChildAt(0)
            ?.requestFocus()
    }

    private fun focusPlayer() {

        currentSection =
            2

        playerContainer.requestFocus()
    }

    private fun focusPackage() {

        if (packagesList.childCount <= 0) {
            return
        }

        currentSection =
            0

        val index =
            selectedPackageIndex.coerceIn(
                0,
                packagesList.childCount - 1
            )

        packagesList
            .getChildAt(index)
            ?.requestFocus()
    }

    private fun focusChannel() {

        if (channelsList.childCount <= 0) {

            focusPackage()

            return
        }

        currentSection =
            1

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

        isFullscreen =
            true

        mainScreen.visibility =
            View.GONE

        fullscreenContainer.visibility =
            View.VISIBLE

        val oldParent =
            playerView.parent as? ViewGroup

        oldParent?.removeView(
            playerView
        )

        fullscreenContainer.removeAllViews()

        fullscreenContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        playerView.isFocusable =
            false

        playerView.isFocusableInTouchMode =
            false

        hideSystemBars()
    }

    // =========================================================
    // EXIT FULLSCREEN
    // =========================================================

    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        isFullscreen =
            false

        val oldParent =
            playerView.parent as? ViewGroup

        oldParent?.removeView(
            playerView
        )

        fullscreenContainer.removeAllViews()

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        fullscreenContainer.visibility =
            View.GONE

        mainScreen.visibility =
            View.VISIBLE

        hideSystemBars()

        currentSection =
            2

        playerContainer.requestFocus()
    }

    // =========================================================
    // BACK
    // =========================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (isFullscreen) {

            exitFullscreen()

            return
        }

        when (currentSection) {

            2 -> {
                focusChannel()
            }

            1 -> {
                focusPackage()
            }

            0 -> {
                // لا يغلق التطبيق
            }
        }
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onStart() {

        super.onStart()

        if (playerCreated) {
            player.playWhenReady =
                player.playWhenReady
        }
    }

    override fun onStop() {

        if (playerCreated) {

            player.playWhenReady =
                false
        }

        super.onStop()
    }

    override fun onDestroy() {

        if (playerCreated) {

            player.release()
        }

        super.onDestroy()
    }

    // =========================================================
    // BACKGROUND
    // =========================================================

    private fun background(
        color: Int,
        stroke: Int,
        radius: Int
    ): GradientDrawable {

        val drawable =
            GradientDrawable()

        drawable.shape =
            GradientDrawable.RECTANGLE

        drawable.setColor(
            color
        )

        drawable.cornerRadius =
            dp(radius).toFloat()

        if (stroke != Color.TRANSPARENT) {

            drawable.setStroke(
                dp(2),
                stroke
            )
        }

        return drawable
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
        ).toInt()
    }
}