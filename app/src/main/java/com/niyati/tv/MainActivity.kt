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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : Activity() {

    // =========================
    // Firebase
    // =========================

    private val firebaseUrl =
        "https://niyati-tv-default-rtdb.europe-west1.firebasedatabase.app"

    private lateinit var database: FirebaseDatabase
    private lateinit var rootRef: com.google.firebase.database.DatabaseReference

    // =========================
    // Player
    // =========================

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    // =========================
    // Main containers
    // =========================

    private lateinit var root: FrameLayout
    private lateinit var normalScreen: LinearLayout
    private lateinit var contentRow: LinearLayout

    private lateinit var packagesPanel: LinearLayout
    private lateinit var channelsPanel: LinearLayout
    private lateinit var playerPanel: LinearLayout

    private lateinit var packagesList: LinearLayout
    private lateinit var channelsList: LinearLayout

    private lateinit var playerContainer: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private var isFullscreen = false

    // =========================
    // Data
    // =========================

    data class Channel(
        val name: String,
        val group: String,
        val url: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    private val packages = mutableListOf<PackageItem>()
    private val channels = mutableListOf<Channel>()

    private var selectedPackage = ""

    // =========================
    // Colors
    // =========================

    private val bg = Color.rgb(4, 7, 13)
    private val panel = Color.rgb(13, 19, 31)
    private val card = Color.rgb(18, 25, 38)

    private val cyan = Color.rgb(0, 229, 255)
    private val blue = Color.rgb(2, 132, 199)

    private val white = Color.WHITE
    private val gray = Color.rgb(145, 158, 175)

    // =========================
    // onCreate
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        player = ExoPlayer.Builder(this).build()

        database = FirebaseDatabase.getInstance(firebaseUrl)
        rootRef = database.reference

        buildInterface()

        loadFirebaseData()
    }

    // =========================
    // Build interface
    // =========================

    private fun buildInterface() {

        root = FrameLayout(this)
        root.setBackgroundColor(bg)

        // =========================
        // Normal screen
        // =========================

        normalScreen = LinearLayout(this)
        normalScreen.orientation = LinearLayout.VERTICAL
        normalScreen.setBackgroundColor(bg)

        root.addView(
            normalScreen,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // =========================
        // Header
        // =========================

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.layoutDirection = View.LAYOUT_DIRECTION_LTR

        header.setPadding(
            dp(28),
            dp(12),
            dp(28),
            dp(12)
        )

        header.setBackgroundColor(bg)

        val brand = TextView(this)

        brand.text = "NIYATI"
        brand.textSize = 25f
        brand.setTextColor(white)
        brand.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        header.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val sportText = TextView(this)

        sportText.text = "SPORTS IPTV"
        sportText.textSize = 13f
        sportText.setTextColor(cyan)
        sportText.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        header.addView(sportText)

        normalScreen.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
            )
        )

        // =========================
        // Three columns
        // =========================

        contentRow = LinearLayout(this)

        contentRow.orientation =
            LinearLayout.HORIZONTAL

        contentRow.layoutDirection =
            View.LAYOUT_DIRECTION_LTR

        contentRow.setPadding(
            dp(14),
            dp(8),
            dp(14),
            dp(14)
        )

        normalScreen.addView(
            contentRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // =========================
        // Packages
        // =========================

        packagesPanel = createPanel()

        packagesPanel.addView(
            createSectionTitle(
                "الباقات",
                "PACKAGES"
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val packageScroll = ScrollView(this)

        packageScroll.isFillViewport = true

        packagesList = LinearLayout(this)

        packagesList.orientation =
            LinearLayout.VERTICAL

        packagesList.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(10)
        )

        packageScroll.addView(packagesList)

        packagesPanel.addView(
            packageScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        contentRow.addView(
            packagesPanel,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.23f
            )
        )

        // =========================
        // Channels
        // =========================

        channelsPanel = createPanel()

        channelsPanel.addView(
            createSectionTitle(
                "القنوات",
                "CHANNELS"
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val channelScroll = ScrollView(this)

        channelScroll.isFillViewport = true

        channelsList = LinearLayout(this)

        channelsList.orientation =
            LinearLayout.VERTICAL

        channelsList.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(10)
        )

        channelScroll.addView(channelsList)

        channelsPanel.addView(
            channelScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        contentRow.addView(
            channelsPanel,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.30f
            )
        )

        // =========================
        // Player
        // =========================

        playerPanel = createPanel()

        playerPanel.addView(
            createSectionTitle(
                "المشغل",
                "PLAYER"
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        playerContainer = FrameLayout(this)

        playerContainer.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        playerView = PlayerView(this)

        playerView.useController = true

        playerView.setShowBuffering(
            PlayerView.SHOW_BUFFERING_WHEN_PLAYING
        )

        playerView.setBackgroundColor(
            Color.BLACK
        )

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
            )
        )

        val hint = TextView(this)

        hint.text =
            "اضغط OK على المشغل لملء الشاشة"

        hint.textSize = 12f
        hint.setTextColor(gray)
        hint.gravity = Gravity.CENTER

        playerPanel.addView(
            hint,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        contentRow.addView(
            playerPanel,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.47f
            )
        )

        // =========================
        // Fullscreen container
        // =========================

        fullscreenContainer = FrameLayout(this)

        fullscreenContainer.setBackgroundColor(
            Color.BLACK
        )

        fullscreenContainer.visibility =
            View.GONE

        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        packagesPanel.requestFocus()
    }

    // =========================
    // Panel
    // =========================

    private fun createPanel(): LinearLayout {

        val layout = LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setBackgroundColor(panel)

        layout.setPadding(
            dp(5),
            dp(5),
            dp(5),
            dp(5)
        )

        layout.isFocusable = true
        layout.isFocusableInTouchMode = true

        applyFocusBackground(layout)

        layout.setOnFocusChangeListener { view, hasFocus ->
            updateFocus(view, hasFocus)
        }

        return layout
    }

    // =========================
    // Section title
    // =========================

    private fun createSectionTitle(
        arabic: String,
        english: String
    ): LinearLayout {

        val box = LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        box.gravity =
            Gravity.CENTER_VERTICAL

        box.setPadding(
            dp(16),
            0,
            dp(16),
            0
        )

        val title = TextView(this)

        title.text = arabic
        title.textSize = 19f
        title.setTextColor(white)

        title.setTypeface(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        title.gravity = Gravity.RIGHT

        box.addView(title)

        val sub = TextView(this)

        sub.text = english
        sub.textSize = 10f
        sub.setTextColor(cyan)
        sub.gravity = Gravity.RIGHT

        box.addView(sub)

        return box
    }

    // =========================
    // Firebase
    // =========================

    private fun loadFirebaseData() {

        rootRef.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    loadPackagesFromFirebase(snapshot)

                    loadChannelsFromFirebase(snapshot)

                    showPackages()

                    if (packages.isNotEmpty()) {

                        selectedPackage =
                            packages.first().id

                        showChannelsForPackage(
                            selectedPackage
                        )
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "تعذر الاتصال بقاعدة البيانات",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // =========================
    // Load packages
    // =========================

    private fun loadPackagesFromFirebase(
        snapshot: DataSnapshot
    ) {

        packages.clear()

        val packagesSnapshot =
            snapshot.child("packages")

        for (child in packagesSnapshot.children) {

            val id =
                child.key ?: continue

            val enabled =
                child.child("enabled")
                    .getValue(Boolean::class.java)
                    ?: true

            if (!enabled) continue

            val name =
                child.child("name")
                    .getValue(String::class.java)
                    ?: id

            val logo =
                child.child("logo")
                    .getValue(String::class.java)
                    ?: ""

            val order =
                child.child("order")
                    .getValue(Int::class.java)
                    ?: 999

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

    // =========================
    // Load channels
    // =========================

    private fun loadChannelsFromFirebase(
        snapshot: DataSnapshot
    ) {

        channels.clear()

        val channelSnapshot =
            snapshot.child("channels")

        for (child in channelSnapshot.children) {

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
                    .getValue(Boolean::class.java)
                    ?: true

            val order =
                child.child("order")
                    .getValue(Int::class.java)
                    ?: 999

            if (!enabled) continue

            channels.add(
                Channel(
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

    // =========================
    // Show packages
    // =========================

    private fun showPackages() {

        packagesList.removeAllViews()

        for (item in packages) {

            val packageCard =
                createPackageCard(item)

            packagesList.addView(
                packageCard,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(62)
                ).apply {

                    setMargins(
                        0,
                        dp(5),
                        0,
                        dp(5)
                    )
                }
            )
        }
    }

    // =========================
    // Package card
    // =========================

    private fun createPackageCard(
        item: PackageItem
    ): TextView {

        val cardView = TextView(this)

        cardView.text = item.name
        cardView.textSize = 16f
        cardView.setTextColor(white)

        cardView.gravity =
            Gravity.CENTER

        cardView.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        cardView.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(5)
        )

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true

        applyFocusBackground(cardView)

        cardView.setOnClickListener {

            selectedPackage =
                item.id

            showChannelsForPackage(
                item.id
            )

            cardView.requestFocus()
        }

        cardView.setOnFocusChangeListener { view, hasFocus ->

            updateFocus(
                view,
                hasFocus
            )
        }

        return cardView
    }

    // =========================
    // Show channels
    // =========================

    private fun showChannelsForPackage(
        packageId: String
    ) {

        channelsList.removeAllViews()

        val filtered =
            channels
                .filter {
                    it.group.equals(
                        packageId,
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.order
                }

        if (filtered.isEmpty()) {

            val empty = TextView(this)

            empty.text =
                "لا توجد قنوات\nلهذه الباقة حالياً"

            empty.textSize = 15f
            empty.setTextColor(gray)

            empty.gravity =
                Gravity.CENTER

            empty.setPadding(
                dp(15),
                dp(30),
                dp(15),
                dp(30)
            )

            channelsList.addView(
                empty,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(120)
                )
            )

            return
        }

        for (channel in filtered) {

            val channelCard =
                createChannelCard(channel)

            channelsList.addView(
                channelCard,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(65)
                ).apply {

                    setMargins(
                        0,
                        dp(5),
                        0,
                        dp(5)
                    )
                }
            )
        }

        if (channelsList.childCount > 0) {

            channelsList
                .getChildAt(0)
                .requestFocus()
        }
    }

    // =========================
    // Channel card
    // =========================

    private fun createChannelCard(
        channel: Channel
    ): TextView {

        val channelCard = TextView(this)

        channelCard.text =
            channel.name

        channelCard.textSize = 15f
        channelCard.setTextColor(white)

        channelCard.gravity =
            Gravity.CENTER_VERTICAL or
                    Gravity.RIGHT

        channelCard.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

        channelCard.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        channelCard.isFocusable = true
        channelCard.isFocusableInTouchMode = true

        applyFocusBackground(channelCard)

        channelCard.setOnClickListener {

            playChannel(channel)

            playerPanel.requestFocus()
        }

        channelCard.setOnFocusChangeListener { view, hasFocus ->

            updateFocus(
                view,
                hasFocus
            )
        }

        return channelCard
    }

    // =========================
    // Play channel
    // =========================

    private fun playChannel(
        channel: Channel
    ) {

        if (channel.url.isBlank()) {

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

            player.setMediaItem(mediaItem)

            player.prepare()

            player.playWhenReady = true

            playerView.player = player

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "تعذر تشغيل القناة",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // Focus background
    // =========================

    private fun applyFocusBackground(
        view: View
    ) {

        view.background =
            GradientDrawable().apply {

                cornerRadius =
                    dp(10).toFloat()

                setColor(card)

                setStroke(
                    dp(1),
                    Color.TRANSPARENT
                )
            }
    }

    private fun updateFocus(
        view: View,
        hasFocus: Boolean
    ) {

        view.background =
            GradientDrawable().apply {

                cornerRadius =
                    dp(10).toFloat()

                setColor(
                    if (hasFocus) {
                        Color.rgb(
                            15,
                            40,
                            50
                        )
                    } else {
                        card
                    }
                )

                setStroke(
                    dp(
                        if (hasFocus) {
                            2
                        } else {
                            1
                        }
                    ),
                    if (hasFocus) {
                        cyan
                    } else {
                        Color.TRANSPARENT
                    }
                )
            }
    }

    // =========================
    // D-PAD
    // =========================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action !=
            KeyEvent.ACTION_DOWN
        ) {
            return super.dispatchKeyEvent(event)
        }

        // =========================
        // Fullscreen
        // =========================

        if (isFullscreen) {

            if (
                event.keyCode ==
                KeyEvent.KEYCODE_BACK
            ) {

                exitFullscreen()

                return true
            }

            return super.dispatchKeyEvent(event)
        }

        // =========================
        // OK on player
        // =========================

        if (
            (
                event.keyCode ==
                    KeyEvent.KEYCODE_DPAD_CENTER ||
                event.keyCode ==
                    KeyEvent.KEYCODE_ENTER
            ) &&
            isPlayerFocused()
        ) {

            enterFullscreen()

            return true
        }

        // =========================
        // LEFT
        // =========================

        if (
            event.keyCode ==
            KeyEvent.KEYCODE_DPAD_LEFT
        ) {

            if (isChannelsFocused()) {

                packagesPanel.requestFocus()

                return true
            }

            if (isPlayerFocused()) {

                channelsPanel.requestFocus()

                return true
            }
        }

        // =========================
        // RIGHT
        // =========================

        if (
            event.keyCode ==
            KeyEvent.KEYCODE_DPAD_RIGHT
        ) {

            if (isPackagesFocused()) {

                channelsPanel.requestFocus()

                return true
            }

            if (isChannelsFocused()) {

                playerPanel.requestFocus()

                return true
            }
        }

        // =========================
        // BACK
        // =========================

        if (
            event.keyCode ==
            KeyEvent.KEYCODE_BACK
        ) {

            if (isPlayerFocused()) {

                channelsPanel.requestFocus()

                return true
            }

            if (isChannelsFocused()) {

                packagesPanel.requestFocus()

                return true
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    // =========================
    // Focus detection
    // =========================

    private fun isPackagesFocused(): Boolean {

        return packagesPanel.hasFocus() ||
                isDescendantFocused(
                    packagesPanel
                )
    }

    private fun isChannelsFocused(): Boolean {

        return channelsPanel.hasFocus() ||
                isDescendantFocused(
                    channelsPanel
                )
    }

    private fun isPlayerFocused(): Boolean {

        return playerPanel.hasFocus() ||
                isDescendantFocused(
                    playerPanel
                )
    }

    private fun isDescendantFocused(
        parent: ViewGroup
    ): Boolean {

        for (
            i in 0 until parent.childCount
        ) {

            val child =
                parent.getChildAt(i)

            if (child.hasFocus()) {
                return true
            }

            if (
                child is ViewGroup &&
                isDescendantFocused(child)
            ) {

                return true
            }
        }

        return false
    }

    // =========================
    // Enter fullscreen
    // =========================

    private fun enterFullscreen() {

        if (isFullscreen) return

        isFullscreen = true

        playerContainer.removeView(
            playerView
        )

        normalScreen.visibility =
            View.GONE

        fullscreenContainer.visibility =
            View.VISIBLE

        fullscreenContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        playerView.requestFocus()

        hideSystemBars()
    }

    // =========================
    // Exit fullscreen
    // =========================

    private fun exitFullscreen() {

        if (!isFullscreen) return

        isFullscreen = false

        fullscreenContainer.removeView(
            playerView
        )

        fullscreenContainer.visibility =
            View.GONE

        playerContainer.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        normalScreen.visibility =
            View.VISIBLE

        showSystemBars()

        playerPanel.requestFocus()
    }

    // =========================
    // Hide system bars
    // =========================

    private fun hideSystemBars() {

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
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // =========================
    // Show system bars
    // =========================

    private fun showSystemBars() {

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
    }

    // =========================
    // Lifecycle
    // =========================

    override fun onStart() {

        super.onStart()

        if (::player.isInitialized) {

            player.playWhenReady = true
        }
    }

    override fun onStop() {

        super.onStop()

        if (::player.isInitialized) {

            player.pause()
        }
    }

    override fun onDestroy() {

        if (::player.isInitialized) {

            player.release()
        }

        super.onDestroy()
    }

    // =========================
    // DP helper
    // =========================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources
                        .displayMetrics
                        .density
            ).toInt()
    }
}
