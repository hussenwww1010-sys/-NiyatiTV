package com.niyati.tv

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : Activity() {

    // ============================================================
    // DATA
    // ============================================================

    data class Channel(
        val name: String,
        val group: String,
        val url: String,
        val logo: String = "",
        val enabled: Boolean = true,
        val order: Long = 0
    )

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String = "",
        val enabled: Boolean = true,
        val order: Long = 0
    )

    private val channels = mutableListOf<Channel>()
    private val packages = mutableListOf<PackageItem>()

    // ============================================================
    // FIREBASE
    // ============================================================

    private val firebaseDatabase =
        FirebaseDatabase.getInstance(
            "https://niyati-tv-default-rtdb.europe-west1.firebasedatabase.app"
        )

    private val firebaseRoot =
        firebaseDatabase.reference

    private var firebaseListener: ValueEventListener? = null

    // ============================================================
    // PLAYER
    // ============================================================

    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView

    private var currentChannel: Channel? = null
    private var currentChannelIndex = -1

    private var isFullscreen = false
    private var isPlayerReady = false

    // ============================================================
    // UI
    // ============================================================

    private lateinit var rootLayout: LinearLayout
    private lateinit var packagesLayout: LinearLayout
    private lateinit var channelsLayout: LinearLayout
    private lateinit var playerContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var channelTitle: TextView

    private lateinit var loadingProgress: ProgressBar

    private var selectedPackageId: String? = null

    // ============================================================
    // COLORS
    // ============================================================

    private val bgPrimary = Color.rgb(4, 7, 13)
    private val bgSecondary = Color.rgb(9, 13, 21)
    private val cardColor = Color.rgb(18, 25, 38)

    private val cyan = Color.rgb(0, 229, 255)
    private val blue = Color.rgb(2, 132, 199)

    private val white = Color.WHITE
    private val gray = Color.rgb(160, 170, 185)

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.statusBarColor = bgPrimary
        window.navigationBarColor = bgPrimary

        buildInterface()

        initExoPlayer()

        loadFirebaseData()

        showWelcomeDialog()
    }

    // ============================================================
    // BUILD INTERFACE
    // ============================================================

    private fun buildInterface() {

        rootLayout = LinearLayout(this)

        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setBackgroundColor(bgPrimary)

        rootLayout.layoutDirection = View.LAYOUT_DIRECTION_RTL

        setContentView(rootLayout)

        // --------------------------------------------------------
        // TOP BAR
        // --------------------------------------------------------

        val topBar = LinearLayout(this)

        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL
        topBar.setPadding(20, 18, 20, 18)

        topBar.setBackgroundColor(bgSecondary)

        val brand = TextView(this)

        brand.text = "NIYATI TV"
        brand.textSize = 22f
        brand.setTextColor(white)
        brand.setTypeface(null, Typeface.BOLD)

        val brandParams =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )

        topBar.addView(brand, brandParams)

        statusText = TextView(this)

        statusText.text = "جاري الاتصال..."
        statusText.textSize = 12f
        statusText.setTextColor(gray)

        topBar.addView(statusText)

        rootLayout.addView(
            topBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // --------------------------------------------------------
        // MAIN CONTENT
        // --------------------------------------------------------

        val mainScroll = ScrollView(this)

        mainScroll.isFillViewport = true

        val mainLayout = LinearLayout(this)

        mainLayout.orientation = LinearLayout.VERTICAL

        mainLayout.setPadding(12, 12, 12, 20)

        // --------------------------------------------------------
        // PLAYER
        // --------------------------------------------------------

        playerContainer = LinearLayout(this)

        playerContainer.orientation = LinearLayout.VERTICAL

        playerContainer.setBackgroundColor(Color.BLACK)

        playerView = PlayerView(this)

        playerView.useController = true
        playerView.setShowBuffering(
            PlayerView.SHOW_BUFFERING_WHEN_PLAYING
        )

        playerView.setBackgroundColor(Color.BLACK)

        playerContainer.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            )
        )

        loadingProgress = ProgressBar(this)

        loadingProgress.visibility = View.GONE

        playerContainer.addView(
            loadingProgress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        mainLayout.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // --------------------------------------------------------
        // CURRENT CHANNEL
        // --------------------------------------------------------

        channelTitle = TextView(this)

        channelTitle.text = "اختر قناة للبدء"
        channelTitle.textSize = 18f
        channelTitle.setTextColor(white)
        channelTitle.setTypeface(null, Typeface.BOLD)

        channelTitle.gravity = Gravity.CENTER

        channelTitle.setPadding(10, 18, 10, 18)

        mainLayout.addView(channelTitle)

        // --------------------------------------------------------
        // PACKAGES TITLE
        // --------------------------------------------------------

        val packagesTitle = makeSectionTitle("الباقات")

        mainLayout.addView(packagesTitle)

        // --------------------------------------------------------
        // PACKAGES
        // --------------------------------------------------------

        packagesLayout = LinearLayout(this)

        packagesLayout.orientation = LinearLayout.HORIZONTAL
        packagesLayout.gravity = Gravity.CENTER_VERTICAL

        packagesLayout.setPadding(0, 5, 0, 10)

        mainLayout.addView(
            packagesLayout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // --------------------------------------------------------
        // CHANNELS TITLE
        // --------------------------------------------------------

        val channelsTitle = makeSectionTitle("القنوات")

        mainLayout.addView(channelsTitle)

        // --------------------------------------------------------
        // CHANNELS
        // --------------------------------------------------------

        channelsLayout = LinearLayout(this)

        channelsLayout.orientation = LinearLayout.VERTICAL

        channelsLayout.setPadding(0, 5, 0, 20)

        mainLayout.addView(
            channelsLayout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        mainScroll.addView(mainLayout)

        rootLayout.addView(
            mainScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    // ============================================================
    // SECTION TITLE
    // ============================================================

    private fun makeSectionTitle(
        text: String
    ): TextView {

        val title = TextView(this)

        title.text = text
        title.textSize = 18f
        title.setTextColor(white)

        title.setTypeface(null, Typeface.BOLD)

        title.gravity = Gravity.RIGHT

        title.setPadding(8, 18, 8, 10)

        return title
    }

    // ============================================================
    // FIREBASE
    // ============================================================

    private fun loadFirebaseData() {

        statusText.text = "جاري تحميل البيانات..."

        firebaseListener?.let {
            firebaseRoot.removeEventListener(it)
        }

        firebaseListener =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    loadPackagesFromFirebase(
                        snapshot.child("packages")
                    )

                    loadChannelsFromFirebase(
                        snapshot.child("channels")
                    )

                    renderPackages()

                    if (packages.isNotEmpty()) {

                        selectPackage(
                            packages.first().id
                        )

                    } else {

                        channelsLayout.removeAllViews()

                        addEmptyMessage(
                            channelsLayout,
                            "لا توجد باقات حالياً"
                        )
                    }

                    statusText.text =
                        "متصل • ${channels.size} قناة"

                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    statusText.text = "فشل الاتصال"

                    Toast.makeText(
                        this@MainActivity,
                        "تعذر الاتصال بقاعدة البيانات",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        firebaseRoot.addListenerForSingleValueEvent(
            firebaseListener!!
        )
    }

    // ============================================================
    // LOAD PACKAGES
    // ============================================================

    private fun loadPackagesFromFirebase(
        snapshot: DataSnapshot
    ) {

        packages.clear()

        for (packageSnapshot in snapshot.children) {

            val id = packageSnapshot.key
                ?: continue

            val enabled =
                packageSnapshot
                    .child("enabled")
                    .getValue(Boolean::class.java)
                    ?: true

            if (!enabled) {
                continue
            }

            val name =
                packageSnapshot
                    .child("name")
                    .getValue(String::class.java)
                    .orEmpty()

            val logo =
                packageSnapshot
                    .child("logo")
                    .getValue(String::class.java)
                    .orEmpty()

            val order =
                packageSnapshot
                    .child("order")
                    .getValue(Long::class.java)
                    ?: 0L

            if (name.isBlank()) {
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

    // ============================================================
    // LOAD CHANNELS
    // ============================================================

    private fun loadChannelsFromFirebase(
        snapshot: DataSnapshot
    ) {

        channels.clear()

        for (channelSnapshot in snapshot.children) {

            val enabled =
                channelSnapshot
                    .child("enabled")
                    .getValue(Boolean::class.java)
                    ?: true

            if (!enabled) {
                continue
            }

            val name =
                channelSnapshot
                    .child("name")
                    .getValue(String::class.java)
                    .orEmpty()

            val group =
                channelSnapshot
                    .child("group")
                    .getValue(String::class.java)
                    .orEmpty()

            val url =
                channelSnapshot
                    .child("url")
                    .getValue(String::class.java)
                    .orEmpty()

            val logo =
                channelSnapshot
                    .child("logo")
                    .getValue(String::class.java)
                    .orEmpty()

            val order =
                channelSnapshot
                    .child("order")
                    .getValue(Long::class.java)
                    ?: 0L

            if (
                name.isBlank() ||
                group.isBlank() ||
                url.isBlank()
            ) {
                continue
            }

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

    // ============================================================
    // RENDER PACKAGES
    // ============================================================

    private fun renderPackages() {

        packagesLayout.removeAllViews()

        if (packages.isEmpty()) {

            addEmptyMessage(
                packagesLayout,
                "لا توجد باقات"
            )

            return
        }

        for (packageItem in packages) {

            val button =
                Button(this)

            button.text =
                packageItem.name

            button.textSize = 14f

            button.setTextColor(white)

            button.isAllCaps = false

            button.background =
                roundedBackground(
                    cardColor,
                    14
                )

            button.setPadding(
                22,
                12,
                22,
                12
            )

            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(55)
                )

            params.setMargins(
                5,
                5,
                5,
                5
            )

            button.layoutParams = params

            button.isFocusable = true
            button.isFocusableInTouchMode = false

            button.setOnFocusChangeListener { view, hasFocus ->

                if (hasFocus) {

                    view.background =
                        roundedBackground(
                            cyan,
                            14
                        )

                    button.setTextColor(
                        Color.BLACK
                    )

                } else {

                    view.background =
                        roundedBackground(
                            cardColor,
                            14
                        )

                    button.setTextColor(
                        white
                    )
                }
            }

            button.setOnClickListener {

                selectPackage(
                    packageItem.id
                )
            }

            button.setOnKeyListener { _, keyCode, event ->

                if (
                    event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                ) {

                    selectPackage(
                        packageItem.id
                    )

                    true

                } else {
                    false
                }
            }

            packagesLayout.addView(button)
        }
    }

    // ============================================================
    // SELECT PACKAGE
    // ============================================================

    private fun selectPackage(
        packageId: String
    ) {

        selectedPackageId = packageId

        renderChannels(
            packageId
        )
    }

    // ============================================================
    // RENDER CHANNELS
    // ============================================================

    private fun renderChannels(
        packageId: String
    ) {

        channelsLayout.removeAllViews()

        val packageChannels =
            channels
                .filter {
                    it.group == packageId
                }
                .sortedBy {
                    it.order
                }

        if (packageChannels.isEmpty()) {

            addEmptyMessage(
                channelsLayout,
                "لا توجد قنوات بهذه الباقة"
            )

            return
        }

        for (
            index in packageChannels.indices
        ) {

            val channel =
                packageChannels[index]

            val card =
                createChannelCard(
                    channel,
                    index,
                    packageChannels
                )

            channelsLayout.addView(card)
        }
    }

    // ============================================================
    // CHANNEL CARD
    // ============================================================

    private fun createChannelCard(
        channel: Channel,
        index: Int,
        packageChannels: List<Channel>
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.HORIZONTAL

        card.gravity =
            Gravity.CENTER_VERTICAL

        card.setPadding(
            16,
            12,
            16,
            12
        )

        card.background =
            roundedBackground(
                cardColor,
                14
            )

        card.isFocusable = true
        card.isClickable = true

        val number =
            TextView(this)

        number.text =
            "${index + 1}"

        number.textSize = 15f
        number.setTextColor(cyan)

        number.gravity =
            Gravity.CENTER

        val numberParams =
            LinearLayout.LayoutParams(
                dp(40),
                dp(40)
            )

        card.addView(
            number,
            numberParams
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        info.gravity =
            Gravity.CENTER_VERTICAL

        info.setPadding(
            14,
            0,
            14,
            0
        )

        val name =
            TextView(this)

        name.text =
            channel.name

        name.textSize = 16f

        name.setTextColor(white)

        name.setTypeface(
            null,
            Typeface.BOLD
        )

        info.addView(name)

        val quality =
            TextView(this)

        quality.text =
            "اضغط OK للتشغيل"

        quality.textSize = 11f

        quality.setTextColor(gray)

        quality.setPadding(
            0,
            4,
            0,
            0
        )

        info.addView(quality)

        card.addView(
            info,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val play =
            TextView(this)

        play.text = "▶"

        play.textSize = 20f

        play.setTextColor(cyan)

        play.gravity =
            Gravity.CENTER

        card.addView(
            play,
            LinearLayout.LayoutParams(
                dp(45),
                dp(45)
            )
        )

        card.setOnFocusChangeListener { view, hasFocus ->

            if (hasFocus) {

                view.background =
                    roundedBackground(
                        cyan,
                        14
                    )

                name.setTextColor(
                    Color.BLACK
                )

                quality.setTextColor(
                    Color.DKGRAY
                )

                number.setTextColor(
                    Color.BLACK
                )

                play.setTextColor(
                    Color.BLACK
                )

            } else {

                view.background =
                    roundedBackground(
                        cardColor,
                        14
                    )

                name.setTextColor(
                    white
                )

                quality.setTextColor(
                    gray
                )

                number.setTextColor(
                    cyan
                )

                play.setTextColor(
                    cyan
                )
            }
        }

        card.setOnClickListener {

            playChannel(
                channel
            )
        }

        card.setOnKeyListener { _, keyCode, event ->

            if (
                event.action == KeyEvent.ACTION_DOWN
            ) {

                when (keyCode) {

                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {

                        playChannel(
                            channel
                        )

                        true
                    }

                    else -> false
                }

            } else {
                false
            }
        }

        return card
    }

    // ============================================================
    // PLAYER INITIALIZATION
    // ============================================================

    private fun initExoPlayer() {

        player =
            ExoPlayer.Builder(this)
                .build()

        playerView.player =
            player

        player?.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    when (playbackState) {

                        Player.STATE_BUFFERING -> {

                            loadingProgress.visibility =
                                View.VISIBLE

                            statusText.text =
                                "جاري التحميل..."
                        }

                        Player.STATE_READY -> {

                            loadingProgress.visibility =
                                View.GONE

                            isPlayerReady = true

                            statusText.text =
                                "يعمل الآن"
                        }

                        Player.STATE_ENDED -> {

                            loadingProgress.visibility =
                                View.GONE

                            statusText.text =
                                "انتهى البث"
                        }

                        Player.STATE_IDLE -> {

                            loadingProgress.visibility =
                                View.GONE
                        }
                    }
                }

                override fun onPlayerError(
                    error: androidx.media3.common.PlaybackException
                ) {

                    loadingProgress.visibility =
                        View.GONE

                    statusText.text =
                        "تعذر تشغيل القناة"

                    Toast.makeText(
                        this@MainActivity,
                        "تعذر تشغيل القناة",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // ============================================================
    // PLAY CHANNEL
    // ============================================================

    private fun playChannel(
        channel: Channel
    ) {

        currentChannel =
            channel

        currentChannelIndex =
            channels.indexOf(channel)

        channelTitle.text =
            channel.name

        loadingProgress.visibility =
            View.VISIBLE

        statusText.text =
            "جاري تشغيل ${channel.name}"

        try {

            val mediaItem =
                MediaItem.fromUri(
                    Uri.parse(channel.url)
                )

            player?.setMediaItem(
                mediaItem
            )

            player?.prepare()

            player?.playWhenReady = true

            isPlayerReady = false

        } catch (
            e: Exception
        ) {

            loadingProgress.visibility =
                View.GONE

            statusText.text =
                "خطأ في الرابط"

            Toast.makeText(
                this,
                "رابط القناة غير صالح",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // NEXT CHANNEL
    // ============================================================

    private fun playNextChannel() {

        if (channels.isEmpty()) {
            return
        }

        val packageId =
            selectedPackageId

        val list =
            if (packageId != null) {

                channels.filter {
                    it.group == packageId
                }

            } else {

                channels
            }

        if (list.isEmpty()) {
            return
        }

        val current =
            currentChannel

        val currentIndex =
            list.indexOf(current)

        val nextIndex =
            if (
                currentIndex < 0 ||
                currentIndex >= list.lastIndex
            ) {
                0
            } else {
                currentIndex + 1
            }

        playChannel(
            list[nextIndex]
        )
    }

    // ============================================================
    // PREVIOUS CHANNEL
    // ============================================================

    private fun playPreviousChannel() {

        if (channels.isEmpty()) {
            return
        }

        val packageId =
            selectedPackageId

        val list =
            if (packageId != null) {

                channels.filter {
                    it.group == packageId
                }

            } else {

                channels
            }

        if (list.isEmpty()) {
            return
        }

        val current =
            currentChannel

        val currentIndex =
            list.indexOf(current)

        val previousIndex =
            if (
                currentIndex <= 0
            ) {
                list.lastIndex
            } else {
                currentIndex - 1
            }

        playChannel(
            list[previousIndex]
        )
    }

    // ============================================================
    // FULLSCREEN
    // ============================================================

    private fun enterFullscreen() {

        isFullscreen = true

        if (Build.VERSION.SDK_INT >= 30) {

            window.insetsController?.let {

                it.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            @Suppress("DEPRECATION")

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        playerContainer.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        playerView.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        playerView.useController = true
    }

    private fun exitFullscreen() {

        isFullscreen = false

        if (Build.VERSION.SDK_INT >= 30) {

            window.insetsController?.show(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )

        } else {

            @Suppress("DEPRECATION")

            window.decorView.systemUiVisibility = 0
        }

        playerContainer.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            )

        playerView.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            )
    }

    // ============================================================
    // REMOTE CONTROL
    // ============================================================

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {

        if (isFullscreen) {

            when (keyCode) {

                KeyEvent.KEYCODE_BACK -> {

                    exitFullscreen()

                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {

                    playPreviousChannel()

                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    playNextChannel()

                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (
                        player?.isPlaying == true
                    ) {

                        player?.pause()

                    } else {

                        player?.play()
                    }

                    return true
                }
            }
        }

        return super.onKeyDown(
            keyCode,
            event
        )
    }

    // ============================================================
    // BACK
    // ============================================================

    override fun onBackPressed() {

        if (isFullscreen) {

            exitFullscreen()

            return
        }

        super.onBackPressed()
    }

    // ============================================================
    // WELCOME
    // ============================================================

    private fun showWelcomeDialog() {

        AlertDialog.Builder(this)
            .setTitle("أهلاً بك في Niyati TV 📺")
            .setMessage(
                "استمتع بمشاهدة القنوات الرياضية."
            )
            .setPositiveButton(
                "ابدأ المشاهدة",
                null
            )
            .show()
    }

    // ============================================================
    // EMPTY MESSAGE
    // ============================================================

    private fun addEmptyMessage(
        parent: ViewGroup,
        text: String
    ) {

        val message =
            TextView(this)

        message.text = text

        message.textSize = 14f

        message.setTextColor(gray)

        message.gravity =
            Gravity.CENTER

        message.setPadding(
            20,
            30,
            20,
            30
        )

        parent.addView(
            message,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    // ============================================================
    // BACKGROUND
    // ============================================================

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                dp(radius).toFloat()
        }
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources.displayMetrics.density
            ).toInt()
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onStart() {

        super.onStart()

        if (
            player != null &&
            currentChannel != null
        ) {

            player?.playWhenReady = true
        }
    }

    override fun onStop() {

        super.onStop()

        player?.pause()
    }

    override fun onDestroy() {

        firebaseListener?.let {

            firebaseRoot.removeEventListener(
                it
            )
        }

        player?.release()

        player = null

        super.onDestroy()
    }
}
