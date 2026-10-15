package com.niyati.tv

import android.annotation.SuppressLint
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.TextUtils
import android.util.LruCache
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
class MainActivity : Activity() {

    // =========================
    // Firebase
    // =========================

    private val firebaseUrl =
        "https://niyati-tv-default-rtdb.europe-west1.firebasedatabase.app"

    private lateinit var database: FirebaseDatabase
    private lateinit var rootRef: DatabaseReference
    private var dataListener: ValueEventListener? = null

    // =========================
    // Player
    // =========================

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    // =========================
    // Web player (YouTube / Facebook)
    // =========================

    private lateinit var webView: WebView

    // true while the current channel is played inside the WebView
    private var playingWeb = false

    private val desktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

    // =========================
    // Views
    // =========================

    private lateinit var root: FrameLayout
    private lateinit var normalScreen: LinearLayout
    private lateinit var packagesList: LinearLayout
    private lateinit var channelsList: LinearLayout
    private lateinit var channelScroll: ScrollView
    private lateinit var packagesHeader: SectionHeader
    private lateinit var channelsHeader: SectionHeader
    private lateinit var playerFrame: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private lateinit var statusOverlay: LinearLayout
    private lateinit var statusIcon: FrameLayout
    private lateinit var statusPlay: PlayIcon
    private lateinit var statusBang: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView

    private lateinit var nowTitle: TextView
    private lateinit var nowSub: TextView
    private lateinit var livePill: LinearLayout
    // receiver-style channel banner (fullscreen)
    private lateinit var zapBanner: LinearLayout
    private lateinit var bannerPrev: BannerCard
    private lateinit var bannerCur: BannerCard
    private lateinit var bannerNext: BannerCard

    // quality badge on the video
    private lateinit var qualityBadge: TextView
    private var qualityText = ""
    private var qualityColor = Color.WHITE

    // header: weather + date
    private lateinit var weatherIcon: TextView
    private lateinit var weatherTemp: TextView
    private lateinit var weatherCityView: TextView
    private lateinit var dateView: TextView
    private var weatherLat = 33.3152
    private var weatherLon = 44.3661
    private var weatherCity = "بغداد"

    // =========================
    // Data
    // =========================

    data class Channel(
        val name: String,
        val group: String,
        val url: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int,
        val userAgent: String = "",
        val referer: String = "",
        val backups: List<String> = emptyList(),
        val isNew: Boolean = false,
        val audios: List<AudioItem> = emptyList(),
        val epgId: String = "",
        val isVod: Boolean = false
    )

    // one program of the TV guide
    data class Program(
        val start: Long,
        val end: Long,
        val title: String
    )

    // movies and series (library)
    data class Episode(
        val season: Int,
        val number: Int,
        val title: String,
        val url: String,
        val duration: Int,
        val thumb: String,
        val overview: String
    )

    data class VodItem(
        val id: String,
        val series: Boolean,
        val title: String,
        val poster: String,
        val backdrop: String,
        val url: String,
        val genre: String,
        val year: String,
        val rating: String,
        val duration: Int,
        val overview: String,
        val isNew: Boolean,
        val featured: Boolean,
        val order: Int,
        val episodes: List<Episode>
    )

    private class VodPlay(val item: VodItem, val episode: Episode?) {
        val key: String
            get() = if (episode == null) item.id else "${item.id}_s${episode.season}e${episode.number}"
    }

    // an extra audio channel (second commentator) added to a channel from the dashboard
    data class AudioItem(
        val name: String,
        val url: String
    )

    data class MatchItem(
        val home: String,
        val away: String,
        val league: String,
        val time: String,
        val date: String,
        val score: String,
        val status: String,
        val channel: String,
        val order: Int,
        val homeLogo: String = "",
        val awayLogo: String = ""
    )

    data class PackageItem(
        val id: String,
        val name: String,
        val logo: String,
        val enabled: Boolean,
        val order: Int
    )

    data class Accent(
        val start: Int,
        val end: Int
    )

    private class PackageRow(
        val item: PackageItem,
        val card: TvCard
    )

    private class ChannelRow(
        val channel: Channel,
        val card: TvCard,
        val setPlaying: (Boolean) -> Unit
    )

    private var packages: List<PackageItem> = emptyList()
    private var channels: List<Channel> = emptyList()
    private var visibleChannels: List<Channel> = emptyList()

    private val packageRows = mutableListOf<PackageRow>()
    private val channelRows = mutableListOf<ChannelRow>()

    private var selectedPackage = ""
    private var pendingPackageId: String? = null
    private var playingChannel: Channel? = null
    private var playingList: List<Channel> = emptyList()
    private var lastFocusedChannel: Channel? = null

    private var dataReady = false

    // ---- favorites / custom order ----
    private val FAV_ID = "__favorites__"
    private val gold = Color.rgb(250, 204, 21)
    private val favorites = mutableSetOf<String>()
    private var basePackages: List<PackageItem> = emptyList()
    private var autoPlayDone = false

    private val prefs by lazy {
        getSharedPreferences("niyati_prefs", MODE_PRIVATE)
    }

    private class MoveState(val isPackage: Boolean, val id: String)

    private var moveState: MoveState? = null

    // ---- long-press menu ----
    private lateinit var menuOverlay: FrameLayout
    private lateinit var menuTitle: TextView
    private lateinit var menuList: LinearLayout
    private var menuReturnFocus: View? = null

    // ---- splash ----
    private lateinit var splash: FrameLayout
    private var splashVisible = true
    private var splashStart = 0L

    // ---- glass channel panel (fullscreen) ----
    private lateinit var channelPanel: LinearLayout
    private lateinit var panelList: LinearLayout
    private lateinit var panelScroll: ScrollView
    private lateinit var panelSub: TextView
    private lateinit var panelQualityBtn: TextView

    private class QualityOption(
        val height: Int,
        val bitrate: Int,
        val group: Tracks.Group,
        val index: Int
    )
    private var isFullscreen = false
    private var resumeOnStart = false

    private var retryCount = 0
    private var backPressedAt = 0L

    // prevents retry after the final error is shown
    private var finalPlaybackError = false

    // =========================
    // Colors
    // =========================

    // theme colors (set by applyTheme in onCreate)
    private var bgTop = Color.rgb(6, 9, 24)
    private var bgMid = Color.rgb(17, 10, 44)
    private var bgBottom = Color.rgb(4, 20, 36)

    private var cyan = Color.rgb(0, 229, 255)
    private var violet = Color.rgb(168, 85, 247)
    private val white = Color.WHITE
    private val dimWhite = Color.argb(235, 255, 255, 255)
    private val gray = Color.rgb(148, 163, 184)

    private var palette = listOf(
        Accent(Color.rgb(0, 229, 255), Color.rgb(41, 121, 255)),
        Accent(Color.rgb(192, 132, 252), Color.rgb(236, 72, 153)),
        Accent(Color.rgb(255, 183, 77), Color.rgb(244, 63, 94)),
        Accent(Color.rgb(52, 211, 153), Color.rgb(6, 182, 212)),
        Accent(Color.rgb(250, 204, 21), Color.rgb(249, 115, 22)),
        Accent(Color.rgb(129, 140, 248), Color.rgb(56, 189, 248))
    )

    private fun accentFor(index: Int): Accent =
        palette[((index % palette.size) + palette.size) % palette.size]

    // =========================
    // Themes
    // =========================

    private class Theme(
        val name: String,
        val bgTop: Int,
        val bgMid: Int,
        val bgBottom: Int,
        val primary: Int,
        val secondary: Int,
        val palette: List<Accent>
    )

    private fun acc(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int) =
        Accent(Color.rgb(r1, g1, b1), Color.rgb(r2, g2, b2))

    private val themes: List<Theme> = listOf(
        Theme(
            "أزرق ليلي",
            Color.rgb(6, 9, 24), Color.rgb(17, 10, 44), Color.rgb(4, 20, 36),
            Color.rgb(0, 229, 255), Color.rgb(168, 85, 247),
            listOf(
                acc(0, 229, 255, 41, 121, 255),
                acc(192, 132, 252, 236, 72, 153),
                acc(255, 183, 77, 244, 63, 94),
                acc(52, 211, 153, 6, 182, 212),
                acc(250, 204, 21, 249, 115, 22),
                acc(129, 140, 248, 56, 189, 248)
            )
        ),
        Theme(
            "أخضر الملعب",
            Color.rgb(3, 18, 12), Color.rgb(6, 34, 22), Color.rgb(2, 22, 28),
            Color.rgb(52, 211, 153), Color.rgb(250, 204, 21),
            listOf(
                acc(52, 211, 153, 6, 182, 212),
                acc(163, 230, 53, 34, 197, 94),
                acc(250, 204, 21, 249, 115, 22),
                acc(45, 212, 191, 59, 130, 246),
                acc(132, 204, 22, 21, 128, 61),
                acc(110, 231, 183, 16, 185, 129)
            )
        ),
        Theme(
            "ذهبي ملكي",
            Color.rgb(14, 10, 4), Color.rgb(36, 22, 8), Color.rgb(20, 10, 6),
            Color.rgb(250, 204, 21), Color.rgb(249, 115, 22),
            listOf(
                acc(250, 204, 21, 249, 115, 22),
                acc(251, 191, 36, 217, 119, 6),
                acc(253, 224, 71, 234, 179, 8),
                acc(245, 158, 11, 180, 83, 9),
                acc(252, 211, 77, 251, 146, 60),
                acc(254, 240, 138, 245, 158, 11)
            )
        ),
        Theme(
            "أحمر ناري",
            Color.rgb(20, 5, 10), Color.rgb(44, 8, 22), Color.rgb(22, 6, 10),
            Color.rgb(255, 99, 120), Color.rgb(249, 115, 22),
            listOf(
                acc(255, 99, 120, 244, 63, 94),
                acc(251, 146, 60, 239, 68, 68),
                acc(244, 114, 182, 225, 29, 72),
                acc(252, 165, 165, 220, 38, 38),
                acc(253, 186, 116, 234, 88, 12),
                acc(251, 113, 133, 190, 18, 60)
            )
        ),
        Theme(
            "بنفسجي",
            Color.rgb(10, 6, 26), Color.rgb(30, 10, 60), Color.rgb(14, 8, 40),
            Color.rgb(192, 132, 252), Color.rgb(236, 72, 153),
            listOf(
                acc(192, 132, 252, 236, 72, 153),
                acc(129, 140, 248, 168, 85, 247),
                acc(232, 121, 249, 147, 51, 234),
                acc(165, 180, 252, 99, 102, 241),
                acc(244, 114, 182, 168, 85, 247),
                acc(196, 181, 253, 124, 58, 237)
            )
        )
    )

    private var themeIndex = 0

    private fun applyTheme(index: Int) {

        val t = themes[index]

        bgTop = t.bgTop
        bgMid = t.bgMid
        bgBottom = t.bgBottom
        cyan = t.primary
        violet = t.secondary
        palette = t.palette
    }

    // ---- settings ----
    private var previewEnabled = true
    private var dynBgEnabled = true

    // ---- dynamic background / preview ----
    private lateinit var ambient: AmbientBackground
    private val tintCache = HashMap<String, Int>()
    private var tintToken = 0
    private var previewTarget: Channel? = null

    // ---- fallback links ----
    private var sourceIndex = 0
    private var playToken = 0

    // ---- remote control (maintenance / announcement / update) ----
    private var blocked = false
    private var updateShown = false
    private var noticeKey = ""
    private var noticeDismiss: (() -> Unit)? = null
    private var noticeReturnFocus: View? = null
    private lateinit var noticeOverlay: FrameLayout
    private lateinit var noticeIcon: TextView
    private lateinit var noticeTitle: TextView
    private lateinit var noticeMessage: TextView
    private lateinit var noticeButtons: LinearLayout
    private lateinit var blockOverlay: FrameLayout
    private lateinit var blockIcon: TextView
    private lateinit var blockTitle: TextView
    private lateinit var blockMessage: TextView

    // ---- today's matches ticker ----
    private var matches: List<MatchItem> = emptyList()
    private var tickerIndex = -1
    private lateinit var tickerChip: LedFrame
    private lateinit var tickerHome: TextView
    private lateinit var tickerAway: TextView
    private lateinit var tickerMid: TextView
    private lateinit var tickerHomeLogo: LogoBadge
    private lateinit var playerGlow: GlowFrame
    private var glowToken = 0

    // ---- phone / touch ----
    private var phoneMode = false
    private lateinit var touchControls: LinearLayout
    private lateinit var touchFavIcon: LineIconView
    private lateinit var touchServerBtn: View
    private lateinit var touchAudioBtn: View
    private lateinit var hudTop: LinearLayout
    private lateinit var hudLogo: LogoBadge
    private lateinit var hudName: TextView
    private lateinit var hudSub: TextView
    private lateinit var hudLive: TextView
    private lateinit var hudQuality: TextView
    private lateinit var hudSeekRow: LinearLayout
    private lateinit var hudSeek: SeekBarView
    private lateinit var hudTimeCur: TextView
    private lateinit var hudTimeTotal: TextView
    private lateinit var hudPlayIcon: LineIconView

    // line icons (24 x 24 grid)
    private val ICON_PREV = "M19 5L9 12l10 7zM5 5v14"
    private val ICON_STAR = "M12 3l2.8 6 6.2.7-4.7 4.3 1.4 6.5-5.7-3.3-5.7 3.3 1.4-6.5L3 9.7 9.2 9z"
    private val ICON_SERVER = "M4 5h16v5H4zM4 14h16v5H4zM8 7.5h.01M8 16.5h.01"
    private val ICON_QUALITY = "M4 7h8M16 7h4M4 17h4M12 17h8M14 4v6M10 14v6"
    private val ICON_ASPECT = "M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"
    private val ICON_FLAG = "M5 21V4M5 4h12l-2.5 4L17 12H5"
    private val ICON_LIST = "M4 6h16M4 12h16M4 18h10"
    private val ICON_AUDIO = "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3zM6 11a6 6 0 0 0 12 0M12 17v4"
    private val ICON_EXIT = "M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5"
    private val ICON_PLAY = "M7 4l13 8-13 8z"
    private val ICON_PAUSE = "M8 5v14M16 5v14"
    private val ICON_BACK10 = "M11 17l-5-5 5-5M18 17l-5-5 5-5"
    private val ICON_FWD10 = "M13 17l5-5-5-5M6 17l5-5-5-5"
    private val hudVodButtons = mutableListOf<View>()
    private var hudFirstButton: View? = null
    private var epgMap: Map<String, List<Program>> = emptyMap()
    private val timeFormat by lazy { SimpleDateFormat("HH:mm", Locale.US) }

    // ---- extra audio (commentators) ----
    private class AudioOption(val label: String, val group: Tracks.Group, val index: Int)

    private var audioPlayer: ExoPlayer? = null
    private var externalAudio = -1

    // ---- servers (the channel links) ----
    private lateinit var stripTitle: TextView
    private lateinit var stripHint: TextView
    private var stripMode = 0          // 0 = quality, 1 = servers
    private var autoFallback = false   // automatic switch to the next server when a link dies
    private lateinit var tickerAwayLogo: LogoBadge
    private var tickerMatch: MatchItem? = null

    // ---- Firebase parts (only the nodes the app needs, not the whole database) ----
    private val parts = HashMap<String, DataSnapshot>()
    private val listeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    // ---- in-app update ----
    private var downloadingUpdate = false

    // ---- sections (live / movies / series) ----
    private var currentSection = 0
    private lateinit var liveHost: LinearLayout
    private lateinit var vodHost: FrameLayout
    private var sectionBarView: View? = null
    private val sectionTabViews = mutableListOf<LinearLayout>()
    private var movies: List<VodItem> = emptyList()
    private var seriesList: List<VodItem> = emptyList()
    private var vodGenre = ""
    private lateinit var libGenres: LinearLayout
    private lateinit var libGenreHeader: SectionHeader
    private lateinit var libScroll: ScrollView
    private lateinit var libContent: LinearLayout
    private var libFirstFocus: View? = null
    private lateinit var detailOverlay: FrameLayout
    private lateinit var detailBackdrop: ImageView
    private lateinit var detailCol: LinearLayout
    private lateinit var detailScroll: ScrollView
    private var detailItem: VodItem? = null
    private var detailSeason = 1
    private var detailPrimary: View? = null
    private var detailReturnFocus: View? = null
    private var vodPlay: VodPlay? = null
    private var pendingSeek = 0L
    private var liveBeforeVod: Channel? = null
    private val posterCache =
        object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
    private var lightMode = false
    private val dayFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    private val dateTextFormat by lazy {
        SimpleDateFormat("EEEE d MMMM yyyy", Locale.forLanguageTag("ar-IQ-u-nu-latn"))
    }
    private var uaIndex = 0
    private var aspectMode = 0   // 0 = original, 1 = stretch, 2 = zoom
    private lateinit var noticeBox: LedFrame
    private lateinit var menuBox: LedFrame
    private lateinit var statusRing: SpinnerRing
    private lateinit var statusRetry: TextView
    private var noticeCancelable = true
    private lateinit var noticeProgressTrack: LinearLayout
    private lateinit var noticeProgressFill: View
    private lateinit var noticeProgressRest: View

    // ---- activation codes ----
    private var requireCode = false
    private var gated = false
    private var serverOffset = 0L
    private var activeCode = ""
    private var codeExpiresAt = 0L
    private var gateMessage = "أدخل كود التفعيل للمتابعة"
    private var expiryWarned = false
    private var codeRef: DatabaseReference? = null
    private var codeListener: ValueEventListener? = null
    private lateinit var codeOverlay: FrameLayout
    private lateinit var codeMessage: TextView
    private lateinit var codeInput: EditText
    private lateinit var codeStatus: TextView
    private lateinit var codeCancelBtn: TextView

    // ---- statistics ----
    private var deviceIdCache = ""
    private var adminDevices: Set<String> = emptySet()
    private var watchChannel: Channel? = null
    private var watchStart = 0L
    private lateinit var tickerLine1: TextView
    private lateinit var tickerLine2: TextView

    // ---- number keys / previous channel ----
    private val numberBuffer = StringBuilder()
    private var previousChannel: Channel? = null
    private lateinit var numberOverlay: TextView

    // ---- connection ----
    private var online = true
    private lateinit var offlineBanner: TextView
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    // ---- ui feedback ----
    private var uiSound = true
    private var lastNavKeyAt = 0L

    // ---- quality strip (separate small overlay, auto hides) ----
    private lateinit var qualityStrip: LinearLayout
    private lateinit var qualityStripChips: LinearLayout

    // ---- multi view ----
    private class Tile(
        val channel: Channel,
        val player: ExoPlayer,
        val http: DefaultHttpDataSource.Factory,
        val frame: FrameLayout,
        val nameLabel: TextView,
        val status: TextView
    ) {
        var sourceIdx = 0
        var started = false
    }

    private val tiles = mutableListOf<Tile>()
    private val multiList = mutableListOf<Channel>()
    private var multiActive = false
    private var multiSel = 0
    private lateinit var multiContainer: FrameLayout
    private lateinit var multiGrid: LinearLayout
    private lateinit var multiHint: TextView

    // =========================
    // Helpers
    // =========================

    private val mainHandler = Handler(Looper.getMainLooper())

    private val ioExecutor: ExecutorService = Executors.newFixedThreadPool(3)

    private val imageCache =
        object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }

    private val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun lp(w: Int, h: Int, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h, weight)

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun mix(a: Int, b: Int, t: Float): Int =
        Color.rgb(
            (Color.red(a) * t + Color.red(b) * (1 - t)).toInt(),
            (Color.green(a) * t + Color.green(b) * (1 - t)).toInt(),
            (Color.blue(a) * t + Color.blue(b) * (1 - t)).toInt()
        )

    private fun label(
        text: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.RIGHT,
        lines: Int = 1
    ): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = sizeSp
        tv.setTextColor(color)
        tv.gravity = gravity
        tv.maxLines = lines
        tv.ellipsize = TextUtils.TruncateAt.END
        tv.textDirection = View.TEXT_DIRECTION_ANY_RTL
        if (bold) {
            tv.typeface = Typeface.DEFAULT_BOLD
        }
        return tv
    }

    // shrink the text automatically until the whole name is visible
    private fun setupFit(tv: TextView, maxSp: Int, minSp: Int) {
        if (Build.VERSION.SDK_INT >= 26) {
            tv.setAutoSizeTextTypeUniformWithConfiguration(
                minSp,
                maxSp,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
    }

    private fun fitName(tv: TextView, text: String, maxSp: Int, minSp: Int) {
        tv.text = text
        if (Build.VERSION.SDK_INT < 26) {
            val size =
                if (text.length <= 16) {
                    maxSp.toFloat()
                } else {
                    (maxSp * 16f / text.length).coerceAtLeast(minSp.toFloat())
                }
            tv.textSize = size
        }
    }

    // =========================
    // onCreate
    // =========================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // keep the cookies of the stream servers (like the other players do)
        try {
            java.net.CookieHandler.setDefault(
                java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL)
            )
        } catch (_: Exception) {
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        themeIndex = prefs.getInt("theme", 0).coerceIn(0, themes.size - 1)

        applyTheme(themeIndex)

        previewEnabled = prefs.getBoolean("preview", true)

        dynBgEnabled = prefs.getBoolean("dyn_bg", true)

        uiSound = prefs.getBoolean("ui_sound", true)

        autoFallback = prefs.getBoolean("auto_fallback", false)

        aspectMode = prefs.getInt("aspect", 0).coerceIn(0, 2)

        lightMode =
            if (prefs.contains("light_mode")) {
                prefs.getBoolean("light_mode", false)
            } else {
                (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
            }

        val skipSplash = prefs.getBoolean("skip_splash_once", false)

        if (skipSplash) {
            prefs.edit().putBoolean("skip_splash_once", false).apply()
        }

        phoneMode =
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN) &&
                    !packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) &&
                    !isTvDevice() &&
                    resources.configuration.smallestScreenWidthDp < 600

        // phone: portrait (landscape only for fullscreen), TV / tablet: landscape
        requestedOrientation =
            if (phoneMode) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

        window.statusBarColor = bgTop
        window.navigationBarColor = bgBottom

        initPlayer()

        initWebView()

        database = FirebaseDatabase.getInstance(firebaseUrl)

        // local cache: the lists open instantly, even with a slow / no connection
        try {
            database.setPersistenceEnabled(true)
        } catch (_: Exception) {
        }

        rootRef = database.reference

        // keep only these nodes in the local cache (never the codes / stats / presence)
        for (node in listOf("settings", "packages", "channels", "matches", "epg", "movies", "series")) {
            try {
                rootRef.child(node).keepSynced(true)
            } catch (_: Exception) {
            }
        }

        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())

        buildInterface()

        registerNetworkWatcher()

        splashStart = SystemClock.elapsedRealtime()

        if (skipSplash) {
            splashVisible = false
            splash.visibility = View.GONE
        } else {
            mainHandler.postDelayed(splashRunnable, 1800)
        }

        startFirebase()

        startPresence()

        activeCode = prefs.getString("code", "") ?: ""

        codeExpiresAt = prefs.getLong("code_exp", 0L)

        if (activeCode.isNotEmpty()) {
            attachCodeListener(activeCode)
        }

        cleanOldUpdates()

        mainHandler.post(weatherRunnable)

        mainHandler.post(dateRunnable)

        mainHandler.post(tickerRunnable)

        mainHandler.postDelayed(epgRunnable, 60_000L)
    }

    // =========================
    // Player setup
    // =========================

    private enum class StreamKind(
        val label: String,
        val mime: String?
    ) {
        AUTO("\u062a\u0644\u0642\u0627\u0626\u064a", null),
        HLS("HLS / M3U8", MimeTypes.APPLICATION_M3U8),
        DASH("DASH / MPD", MimeTypes.APPLICATION_MPD),
        SS("Smooth Streaming", MimeTypes.APPLICATION_SS),
        TS("MPEG-TS", MimeTypes.VIDEO_MP2T),
        MP4("MP4", MimeTypes.VIDEO_MP4),
        RTSP("RTSP", MimeTypes.APPLICATION_RTSP)
    }

    private data class Source(
        val url: String,
        val headers: Map<String, String>
    )

    private lateinit var httpFactory: DefaultHttpDataSource.Factory

    private var currentSource: Source? = null
    private var attempts: List<StreamKind> = listOf(StreamKind.AUTO)
    private var attemptIndex = 0
    private var firstError: PlaybackException? = null
    private var liveWindowRetries = 0
    private var playbackStarted = false

    private var defaultUserAgent =
        "Mozilla/5.0 (Linux; Android 13; TV) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

    // errors meaning the guessed format is wrong (we try another format)
    private val formatErrors = setOf(
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED
    )

    private val retryRunnable = Runnable {
        if (finalPlaybackError || playingChannel == null || playingWeb) {
            return@Runnable
        }
        try {
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            finalPlaybackError = true

            val ch = playingChannel

            if (ch != null && tryNextSource(ch)) {
                return@Runnable
            }

            showDetailedPlaybackError(null, e)
        }
    }

    private fun initPlayer() {

        // HTTP / HTTPS + redirects + per-channel headers
        httpFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent(defaultUserAgent)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

        // DefaultDataSource = http + https + rtmp + udp + file + content
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        // helps live TS streams (avoids black screen with audio)
        val extractorsFactory =
            DefaultExtractorsFactory()
                .setTsExtractorFlags(
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                )

        val mediaSourceFactory =
            DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
                .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(8))

        // fall back to another decoder if the primary one fails
        val renderersFactory =
            DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                )

        // buffer size: 0 = fast start, 1 = balanced (default), 2 = most stable
        val loadControl =
            when (prefs.getInt("buffer_mode", 1)) {
                0 -> DefaultLoadControl.Builder().setBufferDurationsMs(3000, 30000, 700, 2000)
                2 -> DefaultLoadControl.Builder().setBufferDurationsMs(20000, 60000, 2500, 8000)
                else -> DefaultLoadControl.Builder().setBufferDurationsMs(10000, 50000, 1500, 5000)
            }
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

        player =
            ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()

        player.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {

                    // the WebView is in charge right now, ignore ExoPlayer events
                    if (playingWeb) {
                        return
                    }

                    when (playbackState) {

                        Player.STATE_BUFFERING -> {
                            // watchdog: a long buffering after the start = a dead connection
                            mainHandler.removeCallbacks(stallRunnable)

                            if (playbackStarted) {
                                mainHandler.postDelayed(stallRunnable, 12000)
                            }

                            // full-screen status only at the start of playback,
                            // after that PlayerView shows its own buffering spinner
                            if (
                                playingChannel != null &&
                                !finalPlaybackError &&
                                !playbackStarted
                            ) {
                                showStatus(
                                    "\u062c\u0627\u0631\u064a \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0628\u062b\u2026",
                                    playingChannel?.name ?: "",
                                    false
                                )
                            }
                        }

                        Player.STATE_READY -> {
                            retryCount = 0
                            liveWindowRetries = 0
                            finalPlaybackError = false
                            playbackStarted = true

                            mainHandler.removeCallbacks(stallRunnable)

                            // resume a movie / episode where it stopped
                            if (pendingSeek > 0L) {
                                player.seekTo(pendingSeek)
                                pendingSeek = 0L
                            }

                            hideStatus()

                            playingChannel?.let {
                                if (uaIndex > 0) {
                                    prefs.edit().putInt("ua_" + channelKey(it), uaIndex).apply()
                                }
                            }

                            livePill.visibility =
                                if (playingChannel?.let { isVodChannel(it) } == true) {
                                    View.GONE
                                } else {
                                    View.VISIBLE
                                }
                        }

                        Player.STATE_ENDED -> {
                            if (playingChannel != null && !finalPlaybackError) {
                                val vod = playingChannel?.let { isVodChannel(it) } == true

                                if (vod) {
                                    onVodEnded()
                                } else {
                                    reconnectLiveStream()
                                }
                            }
                        }

                        else -> {
                        }
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (!playingWeb) {
                        updateQuality(videoSize.height)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (playingWeb) {
                        return
                    }
                    handlePlayerError(error)
                }
            }
        )
    }

    // =========================
    // Web player (YouTube / Facebook)
    // =========================

    private val hideWebStatusRunnable = Runnable {
        if (playingWeb && !finalPlaybackError) {
            hideStatus()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {

        webView = WebView(this)

        webView.setBackgroundColor(Color.BLACK)

        // becomes focusable only in fullscreen
        webView.isFocusable = false

        webView.visibility = View.GONE

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            userAgentString = desktopUserAgent
        }

        webView.webChromeClient = WebChromeClient()

        webView.webViewClient =
            object : WebViewClient() {

                // block navigation away from the embedded player
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = request.isForMainFrame

                override fun onPageFinished(view: WebView?, url: String?) {

                    if (
                        playingWeb &&
                        url != null &&
                        !url.startsWith("about:")
                    ) {
                        mainHandler.removeCallbacks(hideWebStatusRunnable)
                        mainHandler.postDelayed(hideWebStatusRunnable, 1500)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {

                    if (playingWeb && request?.isForMainFrame == true) {

                        finalPlaybackError = true

                        livePill.visibility = View.GONE

                        val failed = playingChannel

                        if (failed != null && tryNextSource(failed)) {
                            return
                        }

                        showStatus(
                            "\u062a\u0639\u0630\u0631 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629",
                            "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u062a\u0635\u0627\u0644 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a \u0623\u0648 \u0645\u0646 \u0627\u0644\u0631\u0627\u0628\u0637",
                            true
                        )
                    }
                }
            }
    }

    private fun hostOf(url: String): String? =
        try {
            Uri.parse(url.trim()).host?.lowercase(Locale.US)
        } catch (_: Exception) {
            null
        }

    private fun isYoutubeHost(host: String?): Boolean =
        host != null &&
                (host == "youtu.be" ||
                        host == "youtube.com" ||
                        host.endsWith(".youtube.com") ||
                        host == "youtube-nocookie.com" ||
                        host.endsWith(".youtube-nocookie.com"))

    private fun isFacebookHost(host: String?): Boolean =
        host != null &&
                (host == "fb.watch" ||
                        host == "facebook.com" ||
                        host.endsWith(".facebook.com") ||
                        host == "fb.com" ||
                        host.endsWith(".fb.com"))

    // true when the link is a YouTube / Facebook page (not a direct stream)
    private fun isWebSource(url: String): Boolean {

        val lowerPath =
            url.trim()
                .lowercase(Locale.US)
                .substringBefore("#")
                .substringBefore("?")

        if (
            lowerPath.endsWith(".m3u8") ||
            lowerPath.endsWith(".mpd") ||
            lowerPath.endsWith(".ts") ||
            lowerPath.endsWith(".mp4")
        ) {
            return false
        }

        val host = hostOf(url)

        return isYoutubeHost(host) || isFacebookHost(host)
    }

    private fun youtubeEmbedUrl(url: String): String? {

        val uri = Uri.parse(url.trim())

        val host = uri.host?.lowercase(Locale.US)

        val segs = uri.pathSegments

        val params = "autoplay=1&playsinline=1&rel=0&modestbranding=1"

        // channel live: youtube.com/channel/UCxxxx/live
        if (segs.size >= 2 && segs[0] == "channel") {
            return "https://www.youtube.com/embed/live_stream?channel=${segs[1]}&$params"
        }

        var id: String? = null

        if (host == "youtu.be") {

            id = segs.firstOrNull()

        } else {

            val v = uri.getQueryParameter("v")

            if (!v.isNullOrBlank()) {
                id = v
            } else if (
                segs.size >= 2 &&
                segs[0] in setOf("embed", "live", "shorts", "v")
            ) {
                id = segs[1]
            }
        }

        if (!id.isNullOrBlank()) {
            return "https://www.youtube.com/embed/$id?$params"
        }

        val list = uri.getQueryParameter("list")

        if (!list.isNullOrBlank()) {
            return "https://www.youtube.com/embed/videoseries?list=$list&$params"
        }

        return null
    }

    private fun facebookEmbedUrl(url: String): String =
        "https://www.facebook.com/plugins/video.php" +
                "?href=${Uri.encode(url.trim())}" +
                "&show_text=false&autoplay=true&allowfullscreen=true&width=1280"

    private fun buildEmbedHtml(embedUrl: String): String {

        val safe = embedUrl.replace("&", "&amp;")

        return "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<style>" +
                "html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden}" +
                "iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}" +
                "</style></head><body>" +
                "<iframe src=\"$safe\" " +
                "allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture\" " +
                "allowfullscreen></iframe>" +
                "</body></html>"
    }

    private fun playWeb(channel: Channel, rawUrl: String) {

        val host = hostOf(rawUrl)

        val isYoutube = isYoutubeHost(host)

        val embedUrl =
            if (isYoutube) youtubeEmbedUrl(rawUrl) else facebookEmbedUrl(rawUrl)

        val baseUrl =
            if (isYoutube) "https://www.youtube.com" else "https://www.facebook.com"

        val sourceName = if (isYoutube) "YouTube" else "Facebook"

        // stop ExoPlayer, the WebView takes over
        try {
            player.stop()
            player.clearMediaItems()
        } catch (_: Exception) {
        }

        playingWeb = true

        if (embedUrl == null) {

            finalPlaybackError = true

            if (tryNextSource(channel)) {
                return
            }

            livePill.visibility = View.GONE

            webView.visibility = View.GONE

            showStatus(
                "\u0631\u0627\u0628\u0637 \u063a\u064a\u0631 \u0645\u062f\u0639\u0648\u0645",
                "\u062a\u0639\u0630\u0651\u0631 \u0627\u0633\u062a\u062e\u0631\u0627\u062c \u0645\u0639\u0631\u0651\u0641 \u0627\u0644\u0641\u064a\u062f\u064a\u0648 \u0645\u0646 \u0631\u0627\u0628\u0637 $sourceName",
                true
            )

            return
        }

        showStatus(
            "\u062c\u0627\u0631\u064a \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629\u2026",
            "${channel.name}\n\u0627\u0644\u0645\u0635\u062f\u0631: $sourceName",
            false
        )

        webView.visibility = View.VISIBLE

        webView.onResume()

        webView.loadDataWithBaseURL(
            baseUrl,
            buildEmbedHtml(embedUrl),
            "text/html",
            "utf-8",
            null
        )

        livePill.visibility = View.VISIBLE
    }

    private fun stopWeb() {

        mainHandler.removeCallbacks(hideWebStatusRunnable)

        if (playingWeb) {
            playingWeb = false
            webView.loadUrl("about:blank")
        }

        webView.visibility = View.GONE
    }

    // =========================
    // Source parsing / detection
    // =========================

    /*
     * Supports the common Kodi format used in IPTV lists:
     * http://server/live/1.m3u8|User-Agent=VLC&Referer=http://site.com/
     */
    private fun parseSource(raw: String): Source {

        val trimmed = raw.trim()

        val idx = trimmed.indexOf('|')

        if (idx < 0) {
            return Source(trimmed, emptyMap())
        }

        val url = trimmed.substring(0, idx).trim()

        val map = linkedMapOf<String, String>()

        trimmed
            .substring(idx + 1)
            .split('&')
            .forEach { part ->
                val eq = part.indexOf('=')
                if (eq > 0) {
                    val k = Uri.decode(part.substring(0, eq)).trim()
                    val v = Uri.decode(part.substring(eq + 1)).trim()
                    if (k.isNotEmpty()) {
                        map[k] = v
                    }
                }
            }

        return Source(url, map)
    }

    private fun detectKind(rawUrl: String): StreamKind {

        val lower =
            rawUrl
                .trim()
                .lowercase(Locale.US)
                .substringBefore("#")

        val path = lower.substringBefore("?")

        return when {

            lower.startsWith("rtsp://") || lower.startsWith("rtsps://") ->
                StreamKind.RTSP

            path.endsWith(".m3u8") || path.contains(".m3u8/") ->
                StreamKind.HLS

            path.endsWith(".mpd") ->
                StreamKind.DASH

            path.endsWith(".ism") ||
                    path.endsWith(".isml") ||
                    path.contains(".ism/manifest") ||
                    path.contains(".isml/manifest") ->
                StreamKind.SS

            path.endsWith(".mp4") || path.endsWith(".m4v") || path.endsWith(".mov") ->
                StreamKind.MP4

            path.endsWith(".ts") ->
                StreamKind.TS

            // extension inside the query, e.g. play.php?file=abc.m3u8
            lower.contains(".m3u8") ->
                StreamKind.HLS

            lower.contains(".mpd") ->
                StreamKind.DASH

            lower.contains(".mp4") ->
                StreamKind.MP4

            else ->
                StreamKind.AUTO
        }
    }

    /*
     * Attempt order:
     * - URL without extension (e.g. Xtream): auto -> HLS -> TS -> DASH
     * - URL with a clear extension: that format -> auto
     */
    private fun attemptsFor(url: String): List<StreamKind> =
        when (val kind = detectKind(url)) {

            StreamKind.AUTO ->
                listOf(
                    StreamKind.AUTO,
                    StreamKind.HLS,
                    StreamKind.TS,
                    StreamKind.DASH
                )

            StreamKind.RTSP,
            StreamKind.SS ->
                listOf(kind)

            else ->
                listOf(kind, StreamKind.AUTO)
        }

    private fun createMediaItem(url: String, kind: StreamKind): MediaItem {

        val builder = MediaItem.Builder().setUri(Uri.parse(url))

        kind.mime?.let {
            builder.setMimeType(it)
        }

        return builder.build()
    }

    private fun applyRequestHeaders(
        channel: Channel,
        source: Source,
        factory: DefaultHttpDataSource.Factory = httpFactory,
        uaIdx: Int = uaIndex
    ) {

        val headers =
            linkedMapOf(
                "Accept" to "*/*",
                "Accept-Language" to "ar-IQ,ar;q=0.9,en;q=0.8"
            )

        var userAgent = uaOptions(channel).getOrElse(uaIdx) { defaultUserAgent }

        if (channel.referer.isNotBlank()) {

            headers["Referer"] = channel.referer

            val ref = Uri.parse(channel.referer)

            if (ref.scheme != null && ref.authority != null) {
                headers["Origin"] = "${ref.scheme}://${ref.authority}"
            }
        }

        // headers inside the URL (after |) override the others
        source.headers.forEach { (k, v) ->
            if (k.equals("user-agent", ignoreCase = true)) {
                userAgent = v
            } else {
                headers[k] = v
            }
        }

        factory.setUserAgent(userAgent)
        factory.setDefaultRequestProperties(headers)
    }

    private fun startCurrentAttempt(channel: Channel) {

        val source = currentSource ?: return

        val kind =
            attempts.getOrElse(attemptIndex) {
                StreamKind.AUTO
            }

        try {

            if (attemptIndex > 0) {
                showStatus(
                    "جاري التحضير…",
                    channel.name,
                    false
                )
            }

            applyRequestHeaders(channel, source)

            player.setMediaItem(createMediaItem(source.url, kind))

            player.prepare()

            player.playWhenReady = true

        } catch (e: Exception) {

            finalPlaybackError = true

            livePill.visibility = View.GONE

            if (tryNextSource(channel)) {
                return
            }

            showDetailedPlaybackError(null, e)
        }
    }

    // =========================
    // Playback error handling
    // =========================

    private fun handlePlayerError(error: PlaybackException) {

        val channel = playingChannel

        val detailedMessage = buildPlaybackErrorMessage(error)

        android.util.Log.e(
            "NIYATI_PLAYER",
            "Channel=${channel?.name} " +
                    "URL=${channel?.url} " +
                    "Code=${error.errorCodeName} " +
                    "Attempt=${attempts.getOrNull(attemptIndex)?.label}\n" +
                    detailedMessage,
            error
        )

        if (channel == null) {
            return
        }

        // 1) live window passed: seek to the newest point instead of failing
        if (
            error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW &&
            liveWindowRetries < 3
        ) {
            liveWindowRetries++
            player.seekToDefaultPosition()
            player.prepare()
            return
        }

        // 2) wrong format: try the next one (only before playback started)
        if (
            error.errorCode in formatErrors &&
            !playbackStarted &&
            attemptIndex + 1 < attempts.size
        ) {
            if (firstError == null) {
                firstError = error
            }
            attemptIndex++
            startCurrentAttempt(channel)
            return
        }

        // 3a) the server refuses this "client": try another User-Agent (as the other players do)
        val refused = findHttpException(error)

        if (refused != null && refused.responseCode in listOf(401, 403) && !finalPlaybackError) {

            if (uaIndex + 1 < uaOptions(channel).size) {

                uaIndex++

                val token = playToken

                showStatus("جاري تجربة طريقة اتصال أخرى…", "", false)

                mainHandler.removeCallbacks(retryRunnable)

                mainHandler.postDelayed(
                    {
                        if (token == playToken && !finalPlaybackError) {
                            startSource(channel, sourceIndex, false)
                        }
                    },
                    700
                )

                return
            }
        }

        // 3) network/server errors: retry
        if (retryCount < 10 && !finalPlaybackError) {

            retryCount++

            showStatus(
                "جاري إعادة الاتصال…",
                friendlyError(error).first + "  •  المحاولة $retryCount من 10",
                false
            )

            mainHandler.removeCallbacks(retryRunnable)

            mainHandler.postDelayed(
                retryRunnable,
                if (refused != null && refused.responseCode in listOf(401, 403, 429)) {
                    4000L
                } else {
                    minOf(1500L * retryCount, 6000L)
                }
            )

            return
        }

        finalPlaybackError = true

        livePill.visibility = View.GONE

        // dead link: switch to the backup link if the channel has one
        if (tryNextSource(channel)) {
            return
        }

        val shown =
            if (error.errorCode in formatErrors) {
                firstError ?: error
            } else {
                error
            }

        val tried =
            if (attempts.size > 1) {
                "\n\u0627\u0644\u0635\u064a\u063a \u0627\u0644\u0645\u062c\u0631\u0651\u0628\u0629: " +
                        attempts
                            .take(attemptIndex + 1)
                            .joinToString(" \u060c ") { it.label }
            } else {
                ""
            }

        val friendly = friendlyError(shown)

        if (!channel.isVod) {
            reportChannel(channel, friendly.first, true)
        }

        showStatus(
            friendly.first,
            friendly.second + serverHint(),
            true
        )
    }

    private fun buildPlaybackErrorMessage(error: PlaybackException): String {

        val lines = mutableListOf<String>()

        lines.add("\u0646\u0648\u0639 \u0627\u0644\u062e\u0637\u0623: ${error.errorCodeName}")

        val httpException = findHttpException(error)

        if (httpException != null) {

            val code = httpException.responseCode

            val responseMessage = httpException.responseMessage

            lines.add(
                "HTTP: $code" +
                        (if (!responseMessage.isNullOrBlank()) " $responseMessage" else "")
            )

            when (code) {
                401 -> lines.add("\u0627\u0644\u062e\u0627\u062f\u0645 \u064a\u0637\u0644\u0628 \u0645\u0635\u0627\u062f\u0642\u0629 \u0623\u0648 \u0635\u0644\u0627\u062d\u064a\u0629.")
                403 -> lines.add("\u0627\u0644\u062e\u0627\u062f\u0645 \u0631\u0641\u0636 \u0627\u0644\u0637\u0644\u0628 403 (\u062c\u0631\u0628 Referer \u0623\u0648 User-Agent).")
                404 -> lines.add("\u0627\u0644\u0631\u0627\u0628\u0637 \u063a\u064a\u0631 \u0645\u0648\u062c\u0648\u062f 404.")
                429 -> lines.add("\u0627\u0644\u062e\u0627\u062f\u0645 \u0631\u0641\u0636 \u0627\u0644\u0637\u0644\u0628\u0627\u062a \u0645\u0624\u0642\u062a\u0627\u064b 429.")
                in 500..599 -> lines.add("\u0645\u0634\u0643\u0644\u0629 \u0645\u0646 \u062e\u0627\u062f\u0645 \u0627\u0644\u0628\u062b.")
            }
        }

        val dataSourceException = findDataSourceException(error)

        if (dataSourceException != null) {
            lines.add("DataSource: ${dataSourceException.reason}")
        }

        val cause = error.cause

        if (cause != null) {

            val simpleName = cause.javaClass.simpleName

            val message = cause.message

            if (!message.isNullOrBlank()) {
                lines.add("$simpleName: " + shortenError(message))
            } else {
                lines.add("\u0627\u0644\u0633\u0628\u0628: $simpleName")
            }
        }

        if (lines.size == 1) {
            lines.add("\u0627\u0644\u0633\u0628\u0628: ${error.message ?: "\u063a\u064a\u0631 \u0645\u0639\u0631\u0648\u0641"}")
        }

        return lines.joinToString("\n")
    }

    private fun findHttpException(
        error: PlaybackException
    ): HttpDataSource.InvalidResponseCodeException? {

        var current: Throwable? = error

        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException) {
                return current
            }
            current = current.cause
        }

        return null
    }

    private fun findDataSourceException(
        error: PlaybackException
    ): DataSourceException? {

        var current: Throwable? = error

        while (current != null) {
            if (current is DataSourceException) {
                return current
            }
            current = current.cause
        }

        return null
    }

    private fun shortenError(text: String): String {

        val cleaned = text.replace("\n", " ").trim()

        return if (cleaned.length > 180) {
            cleaned.take(177) + "..."
        } else {
            cleaned
        }
    }

    // =========================
    // Build interface
    // =========================

    private fun buildInterface() {

        buildQualityBadge()

        root = FrameLayout(this)

        root.layoutDirection = View.LAYOUT_DIRECTION_LTR

        ambient = AmbientBackground()

        root.addView(
            ambient,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        normalScreen = LinearLayout(this)

        normalScreen.orientation = LinearLayout.VERTICAL

        root.addView(
            normalScreen,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        if (phoneMode) {

            buildPhoneContent()

        } else {

        normalScreen.addView(buildHeader(), lp(matchParent, dp(78)))

        normalScreen.addView(buildSectionBar(), lp(matchParent, wrap))

        val contentRow = LinearLayout(this)

        contentRow.orientation = LinearLayout.HORIZONTAL

        contentRow.setPadding(dp(18), dp(4), dp(18), dp(16))

        normalScreen.addView(contentRow, lp(matchParent, 0, 1f))

        liveHost = contentRow

        buildVodHost()

        // ---- Packages ----

        val packagesPanel = createPanel()

        packagesHeader = SectionHeader("\u0627\u0644\u0628\u0627\u0642\u0627\u062a", "PACKAGES", accentFor(0))

        packagesPanel.addView(packagesHeader, lp(matchParent, dp(64)))

        val packageScroll = createScroll()

        packagesList = createList()

        packageScroll.addView(packagesList)

        packagesPanel.addView(packageScroll, lp(matchParent, 0, 1f))

        contentRow.addView(packagesPanel, panelParams(0.23f))

        // ---- Channels ----

        val channelsPanel = createPanel()

        channelsHeader = SectionHeader("\u0627\u0644\u0642\u0646\u0648\u0627\u062a", "CHANNELS", accentFor(1))

        channelsPanel.addView(channelsHeader, lp(matchParent, dp(64)))

        channelScroll = createScroll()

        channelsList = createList()

        channelScroll.addView(channelsList)

        channelsPanel.addView(channelScroll, lp(matchParent, 0, 1f))

        contentRow.addView(channelsPanel, panelParams(0.30f))

        // ---- Player ----

        val playerPanel = createPanel()

        playerPanel.addView(
            SectionHeader("\u0627\u0644\u0645\u0634\u063a\u0644", "PLAYER", accentFor(2), false),
            lp(matchParent, dp(64))
        )

        val glowFrame = GlowFrame()

        playerFrame = glowFrame

        playerGlow = glowFrame

        playerFrame.setPadding(dp(4), dp(4), dp(4), dp(4))

        playerFrame.isFocusable = true

        playerFrame.isFocusableInTouchMode = !phoneMode

        playerFrame.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        applyPlayerFrameBackground(false)

        playerFrame.setOnFocusChangeListener { _, hasFocus ->
            applyPlayerFrameBackground(hasFocus)
        }

        playerFrame.setOnClickListener {
            enterFullscreen()
        }

        playerFrame.setOnLongClickListener {
            showPlayerOptionsMenu()
            true
        }

        playerPanel.addView(
            playerFrame,
            lp(matchParent, 0, 1f).apply {
                setMargins(dp(6), dp(4), dp(6), 0)
            }
        )

        playerPanel.addView(
            buildInfoBar(),
            lp(matchParent, dp(60)).apply {
                setMargins(dp(6), dp(8), dp(6), 0)
            }
        )

        playerPanel.addView(buildHintBar(), lp(matchParent, dp(30)))

        contentRow.addView(playerPanel, panelParams(0.47f))

        }

        // ---- Video + status ----

        playerView = PlayerView(this)

        playerView.useController = false

        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

        playerView.setBackgroundColor(Color.BLACK)

        playerView.isFocusable = false

        playerView.player = player

        playerView.setKeepContentOnPlayerReset(true)

        applyAspect()

        buildStatusOverlay()

        moveVideoTo(playerFrame)

        showStatus(Txt.IDLE_TITLE, Txt.IDLE_SUB, false)

        // ---- Fullscreen ----

        fullscreenContainer = FrameLayout(this)

        fullscreenContainer.setBackgroundColor(Color.BLACK)

        fullscreenContainer.visibility = View.GONE

        buildZapBanner()

        buildChannelPanel()

        buildQualityStrip()

        buildTouchControls()

        root.addView(
            fullscreenContainer,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        buildMultiView()

        buildNumberOverlay()

        buildOfflineBanner()

        buildDetailOverlay()

        buildMenuOverlay()

        buildNoticeOverlay()

        buildBlockOverlay()

        buildCodeOverlay()

        buildSplash()

        setContentView(root)
    }

    // App logo: put logo.png in res/drawable. Without it the "N" mark is used.
    private fun createLogoMark(letterSp: Float, radiusDp: Int): View {

        val id = resources.getIdentifier("logo", "drawable", packageName)

        if (id != 0) {

            val image = ImageView(this)

            image.setImageResource(id)

            image.scaleType = ImageView.ScaleType.FIT_CENTER

            return image
        }

        val mark = TextView(this)

        mark.text = "N"

        mark.textSize = letterSp

        mark.setTextColor(white)

        mark.typeface = Typeface.DEFAULT_BOLD

        mark.gravity = Gravity.CENTER

        mark.background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(cyan, violet)
            ).apply {
                cornerRadius = dp(radiusDp).toFloat()
            }

        return mark
    }

    private fun panelParams(weight: Float): LinearLayout.LayoutParams =
        lp(0, matchParent, weight).apply {
            setMargins(dp(7), 0, dp(7), 0)
        }

    private fun buildHeader(): View {

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL

        header.gravity = Gravity.CENTER_VERTICAL

        header.setPadding(dp(34), dp(8), dp(34), dp(6))

        val logoBox = ShineLayout()

        logoBox.addView(
            createLogoMark(22f, 14),
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        header.addView(logoBox, lp(dp(48), dp(48)))

        val brandCol = LinearLayout(this)

        brandCol.orientation = LinearLayout.VERTICAL

        brandCol.setPadding(dp(12), 0, 0, 0)

        val brand = label("NIYATI", 22f, white, true, Gravity.LEFT)

        brand.letterSpacing = 0.14f

        val sport = label("SPORTS IPTV", 10.5f, cyan, true, Gravity.LEFT)

        sport.letterSpacing = 0.28f

        brandCol.addView(brand, lp(wrap, wrap))

        brandCol.addView(sport, lp(wrap, wrap))

        header.addView(brandCol, lp(wrap, wrap))

        header.addView(
            buildTicker(),
            lp(0, wrap, 1f).apply {
                setMargins(dp(24), 0, dp(24), 0)
            }
        )

        header.addView(createLivePill(), lp(wrap, wrap))

        // ---- weather ----
        val weatherChip = LinearLayout(this)

        weatherChip.orientation = LinearLayout.HORIZONTAL

        weatherChip.gravity = Gravity.CENTER_VERTICAL

        weatherChip.setPadding(dp(12), dp(3), dp(14), dp(3))

        weatherChip.background =
            GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(22, 255, 255, 255))
                setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }

        weatherIcon = TextView(this)

        weatherIcon.text = "\u2601\uFE0F"

        weatherIcon.textSize = 22f

        weatherIcon.gravity = Gravity.CENTER

        weatherChip.addView(weatherIcon, lp(wrap, wrap))

        val weatherCol = LinearLayout(this)

        weatherCol.orientation = LinearLayout.VERTICAL

        weatherCol.setPadding(dp(8), 0, 0, 0)

        weatherTemp = label("--\u00B0", 17f, white, true, Gravity.LEFT)

        weatherCityView = label(weatherCity, 10f, gray, false, Gravity.LEFT)

        weatherCol.addView(weatherTemp, lp(wrap, wrap))

        weatherCol.addView(weatherCityView, lp(wrap, wrap))

        weatherChip.addView(weatherCol, lp(wrap, wrap))

        header.addView(
            weatherChip,
            lp(wrap, wrap).apply {
                setMargins(dp(16), 0, 0, 0)
            }
        )

        // ---- clock + date ----
        val timeCol = LinearLayout(this)

        timeCol.orientation = LinearLayout.VERTICAL

        timeCol.gravity = Gravity.RIGHT

        val clock = TextClock(this)

        clock.format12Hour = "hh:mm a"

        clock.format24Hour = "hh:mm a"

        clock.textSize = 20f

        clock.setTextColor(white)

        clock.typeface = Typeface.DEFAULT_BOLD

        clock.gravity = Gravity.RIGHT

        timeCol.addView(clock, lp(wrap, wrap))

        dateView = label("", 11f, gray, false, Gravity.RIGHT)

        timeCol.addView(dateView, lp(wrap, wrap))

        header.addView(
            timeCol,
            lp(wrap, wrap).apply {
                setMargins(dp(18), 0, 0, 0)
            }
        )

        // ---- settings button ----
        val settingsBtn = TextView(this)

        settingsBtn.text = "⚙"

        settingsBtn.textSize = 22f

        settingsBtn.setTextColor(white)

        settingsBtn.gravity = Gravity.CENTER

        settingsBtn.isFocusable = true

        settingsBtn.isFocusableInTouchMode = !phoneMode

        fun styleSettings(focused: Boolean) {
            settingsBtn.background =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (focused) {
                        setColor(withAlpha(cyan, 110))
                        setStroke(dp(2), Color.WHITE)
                    } else {
                        setColor(Color.argb(22, 255, 255, 255))
                        setStroke(dp(1), Color.argb(40, 255, 255, 255))
                    }
                }
        }

        styleSettings(false)

        settingsBtn.setOnFocusChangeListener { _, focused -> styleSettings(focused) }

        settingsBtn.setOnClickListener {
            showSettingsMenu()
        }

        header.addView(
            settingsBtn,
            lp(dp(44), dp(44)).apply {
                setMargins(dp(16), 0, 0, 0)
            }
        )

        return header
    }

    private fun createLivePill(): LinearLayout {

        val pill = LinearLayout(this)

        pill.orientation = LinearLayout.HORIZONTAL

        pill.gravity = Gravity.CENTER_VERTICAL

        pill.setPadding(dp(10), dp(4), dp(12), dp(4))

        pill.background =
            GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(46, 255, 59, 92))
                setStroke(dp(1), Color.argb(150, 255, 59, 92))
            }

        val dot = View(this)

        dot.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(255, 59, 92))
            }

        val blink =
            AlphaAnimation(1f, 0.2f).apply {
                duration = 800
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }

        dot.startAnimation(blink)

        pill.addView(dot, lp(dp(8), dp(8)))

        val text = label("LIVE", 11f, white, true, Gravity.CENTER)

        text.letterSpacing = 0.12f

        pill.addView(
            text,
            lp(wrap, wrap).apply {
                setMargins(dp(7), 0, 0, 0)
            }
        )

        return pill
    }

    private fun buildInfoBar(): LinearLayout {

        val bar = LinearLayout(this)

        bar.orientation = LinearLayout.HORIZONTAL

        bar.gravity = Gravity.CENTER_VERTICAL

        bar.setPadding(dp(14), 0, dp(14), 0)

        bar.background =
            GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(22, 255, 255, 255))
                setStroke(dp(1), Color.argb(26, 255, 255, 255))
            }

        livePill = createLivePill()

        livePill.visibility = View.GONE

        bar.addView(livePill, lp(wrap, wrap))

        bar.addView(
            qualityBadge,
            lp(wrap, wrap).apply {
                setMargins(dp(8), 0, 0, 0)
            }
        )

        val col = LinearLayout(this)

        col.orientation = LinearLayout.VERTICAL

        col.gravity = Gravity.CENTER_VERTICAL

        nowTitle = label("\u0644\u0645 \u064a\u062a\u0645 \u0627\u062e\u062a\u064a\u0627\u0631 \u0642\u0646\u0627\u0629", 16f, white, true)

        nowSub = label("\u0627\u062e\u062a\u0631 \u0642\u0646\u0627\u0629 \u0645\u0646 \u0627\u0644\u0642\u0627\u0626\u0645\u0629", 12f, gray)

        setupFit(nowTitle, 16, 9)

        nowTitle.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

        col.addView(nowTitle, lp(matchParent, dp(24)))

        col.addView(nowSub, lp(matchParent, wrap))

        bar.addView(col, lp(0, wrap, 1f))

        return bar
    }

    private fun buildStatusOverlay() {

        statusOverlay = LinearLayout(this)

        statusOverlay.orientation = LinearLayout.VERTICAL

        statusOverlay.gravity = Gravity.CENTER

        statusOverlay.setPadding(dp(16), dp(16), dp(16), dp(16))

        statusOverlay.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    mix(violet, bgTop, 0.16f),
                    Color.rgb(7, 9, 22)
                )
            )

        // animated ring around the icon
        statusIcon = FrameLayout(this)

        statusRing = SpinnerRing()

        statusIcon.addView(statusRing, FrameLayout.LayoutParams(matchParent, matchParent))

        val disc = FrameLayout(this)

        statusPlay = PlayIcon(Color.WHITE)

        statusBang = TextView(this)

        statusBang.text = "!"
        statusBang.textSize = 26f
        statusBang.setTextColor(white)
        statusBang.typeface = Typeface.DEFAULT_BOLD
        statusBang.gravity = Gravity.CENTER
        statusBang.visibility = View.GONE

        disc.addView(
            statusPlay,
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER).apply {
                leftMargin = dp(4)
            }
        )

        disc.addView(
            statusBang,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        statusIcon.addView(
            disc,
            FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER)
        )

        // keep the icon tag for showStatus (the colored disc)
        statusIcon.tag = disc

        statusOverlay.addView(statusIcon, lp(dp(84), dp(84)))

        statusTitle = label("", 17f, white, true, Gravity.CENTER, 2)

        statusSub = label("", 12.5f, gray, false, Gravity.CENTER, 3)

        statusOverlay.addView(
            statusTitle,
            lp(matchParent, wrap).apply {
                topMargin = dp(12)
            }
        )

        statusOverlay.addView(
            statusSub,
            lp(matchParent, wrap).apply {
                topMargin = dp(4)
            }
        )

        statusRetry = menuItem("إعادة المحاولة", accentFor(0))

        statusRetry.visibility = View.GONE

        statusRetry.setOnClickListener {
            retryPlayback()
        }

        statusOverlay.addView(
            statusRetry,
            lp(dp(190), dp(44)).apply {
                topMargin = dp(14)
            }
        )
    }

    private fun showStatus(title: String, sub: String, error: Boolean) {

        val idle = title == Txt.IDLE_TITLE

        statusTitle.text = title

        statusSub.text = sub

        statusSub.visibility = if (sub.isBlank()) View.GONE else View.VISIBLE

        val colors =
            if (error) {
                intArrayOf(Color.rgb(255, 82, 82), Color.rgb(244, 63, 94))
            } else {
                intArrayOf(cyan, violet)
            }

        (statusIcon.tag as? View)?.background =
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                colors
            ).apply {
                shape = GradientDrawable.OVAL
            }

        statusRing.setMode(if (error) 1 else if (idle) 2 else 0)

        statusPlay.visibility = if (error) View.GONE else View.VISIBLE

        statusBang.visibility = if (error) View.VISIBLE else View.GONE

        statusRetry.visibility = if (error) View.VISIBLE else View.GONE

        statusOverlay.visibility = View.VISIBLE

        if (error && ::qualityBadge.isInitialized) {
            qualityBadge.visibility = View.GONE
        }
    }

    // try the current channel again from the start
    private fun retryPlayback() {

        val channel = playingChannel ?: return

        playToken++

        retryCount = 0

        startSource(channel, savedServerIndex(channel))
    }

    // short Arabic message instead of the technical error codes
    private fun friendlyError(error: PlaybackException?): Pair<String, String> {

        val http = if (error != null) findHttpException(error) else null

        if (http != null) {

            return when (http.responseCode) {

                401, 403 -> Pair("السيرفر رفض الاتصال", "قد يكون الاشتراك أو الرابط غير صالح")

                404 -> Pair("القناة غير متوفرة حالياً", "الرابط غير موجود على السيرفر")

                429 -> Pair("السيرفر مشغول", "كثرة الطلبات، جرّب بعد قليل")

                511 -> Pair("تعذر الوصول للسيرفر", "قد تحجب شبكتك هذا الرابط، جرّب شبكة أخرى")

                in 500..599 -> Pair("السيرفر لا يستجيب حالياً", "المشكلة من مصدر البث، جرّب بعد قليل")

                else -> Pair("تعذر تشغيل القناة", "استجابة غير متوقعة من السيرفر")
            }
        }

        return when (error?.errorCode) {

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                Pair("لا يوجد اتصال بالسيرفر", "تحقق من الإنترنت")

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                Pair("صيغة البث غير مدعومة", "جرّب سيرفراً آخر")

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                Pair("جهازك لا يدعم هذا البث", "")

            else -> Pair("تعذر تشغيل القناة", "حاول مرة أخرى بعد قليل")
        }
    }

    private fun hideStatus() {
        statusOverlay.visibility = View.GONE
    }

    private fun moveVideoTo(container: FrameLayout) {

        (playerView.parent as? ViewGroup)?.removeView(playerView)

        (webView.parent as? ViewGroup)?.removeView(webView)

        (statusOverlay.parent as? ViewGroup)?.removeView(statusOverlay)

        // order: ExoPlayer video, then WebView, then the status overlay on top
        container.addView(
            playerView,
            0,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        container.addView(
            webView,
            1,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        container.addView(
            statusOverlay,
            2,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )
    }

    private fun applyPlayerFrameBackground(focused: Boolean) {

        playerFrame.background =
            GradientDrawable().apply {

                cornerRadius = dp(10).toFloat()

                setColor(Color.BLACK)

                setStroke(
                    if (focused) dp(3) else dp(1),
                    if (focused) cyan else Color.argb(45, 255, 255, 255)
                )
            }
    }

    private fun createPanel(): LinearLayout {

        val panel = LinearLayout(this)

        panel.orientation = LinearLayout.VERTICAL

        panel.setPadding(dp(4), dp(6), dp(4), dp(6))

        panel.clipChildren = false

        panel.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.argb(30, 255, 255, 255),
                    Color.argb(12, 255, 255, 255)
                )
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.argb(38, 255, 255, 255))
            }

        return panel
    }

    private fun createScroll(): ScrollView {

        val scroll = ScrollView(this)

        scroll.isFillViewport = true

        scroll.isVerticalScrollBarEnabled = false

        scroll.overScrollMode = View.OVER_SCROLL_NEVER

        scroll.clipToPadding = false

        scroll.clipChildren = false

        return scroll
    }

    private fun createList(): LinearLayout {

        val list = LinearLayout(this)

        list.orientation = LinearLayout.VERTICAL

        list.setPadding(dp(10), dp(4), dp(10), dp(10))

        list.clipChildren = false

        return list
    }

    // =========================
    // Custom views
    // =========================

    private inner class AmbientBackground : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var base: Shader? = null
        private var glowA: Shader? = null
        private var glowB: Shader? = null
        private var tintShader: Shader? = null

        private var curColor = Color.BLACK
        private var curStrength = 0f
        private var anim: ValueAnimator? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

            super.onSizeChanged(w, h, oldw, oldh)

            val fw = w.toFloat()

            val fh = h.toFloat()

            base =
                LinearGradient(
                    0f,
                    0f,
                    fw,
                    fh,
                    intArrayOf(bgTop, bgMid, bgBottom),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )

            glowA =
                RadialGradient(
                    fw * 0.88f,
                    fh * 0.08f,
                    fw * 0.55f,
                    intArrayOf(withAlpha(violet, 95), withAlpha(violet, 0)),
                    null,
                    Shader.TileMode.CLAMP
                )

            glowB =
                RadialGradient(
                    fw * 0.08f,
                    fh * 0.98f,
                    fw * 0.5f,
                    intArrayOf(withAlpha(cyan, 80), withAlpha(cyan, 0)),
                    null,
                    Shader.TileMode.CLAMP
                )

            rebuildTint()
        }

        private fun rebuildTint() {

            if (width == 0 || height == 0) {
                return
            }

            val alpha = (135 * curStrength).toInt()

            tintShader =
                RadialGradient(
                    width * 0.70f,
                    height * 0.48f,
                    width * 0.68f,
                    intArrayOf(withAlpha(curColor, alpha), withAlpha(curColor, 0)),
                    null,
                    Shader.TileMode.CLAMP
                )
        }

        // null = fade the tint out
        fun setTint(color: Int?) {

            anim?.cancel()

            val fromColor = curColor

            val fromStrength = curStrength

            val toColor = color ?: curColor

            val toStrength = if (color == null) 0f else 1f

            val evaluator = ArgbEvaluator()

            val a = ValueAnimator.ofFloat(0f, 1f)

            a.duration = 700

            a.addUpdateListener {

                val t = it.animatedValue as Float

                curColor = evaluator.evaluate(t, fromColor, toColor) as Int

                curStrength = fromStrength + (toStrength - fromStrength) * t

                rebuildTint()

                invalidate()
            }

            anim = a

            a.start()
        }

        override fun onDraw(canvas: Canvas) {

            val w = width.toFloat()

            val h = height.toFloat()

            paint.shader = base
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = glowA
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.shader = glowB
            canvas.drawRect(0f, 0f, w, h, paint)

            if (curStrength > 0.01f) {
                paint.shader = tintShader
                canvas.drawRect(0f, 0f, w, h, paint)
            }
        }
    }

    private inner class PlayIcon(tint: Int) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val path = Path()

        init {
            paint.color = tint
        }

        fun setTint(color: Int) {
            paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {

            val w = width.toFloat()

            val h = height.toFloat()

            path.reset()

            path.moveTo(w * 0.2f, h * 0.08f)

            path.lineTo(w * 0.95f, h * 0.5f)

            path.lineTo(w * 0.2f, h * 0.92f)

            path.close()

            canvas.drawPath(path, paint)
        }
    }

    private inner class SectionHeader(
        arabic: String,
        english: String,
        accent: Accent,
        showCount: Boolean = true
    ) : LinearLayout(this@MainActivity) {

        private val countChip = TextView(this@MainActivity)

        private val subtitle = label(english, 10.5f, accent.start)

        init {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(dp(14), 0, dp(14), 0)

            countChip.textSize = 12f

            countChip.setTextColor(white)

            countChip.typeface = Typeface.DEFAULT_BOLD

            countChip.gravity = Gravity.CENTER

            countChip.minWidth = dp(30)

            countChip.setPadding(dp(10), dp(4), dp(10), dp(4))

            countChip.background =
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(withAlpha(accent.start, 60))
                    setStroke(dp(1), withAlpha(accent.start, 140))
                }

            countChip.visibility = if (showCount) View.VISIBLE else View.GONE

            addView(countChip, LinearLayout.LayoutParams(wrap, wrap))

            val col = LinearLayout(this@MainActivity)

            col.orientation = LinearLayout.VERTICAL

            col.gravity = Gravity.CENTER_VERTICAL

            col.setPadding(dp(10), 0, dp(12), 0)

            col.addView(
                label(arabic, 19f, white, true),
                LinearLayout.LayoutParams(matchParent, wrap)
            )

            subtitle.letterSpacing = 0.15f

            col.addView(subtitle, LinearLayout.LayoutParams(matchParent, wrap))

            addView(col, LinearLayout.LayoutParams(0, wrap, 1f))

            val bar = View(this@MainActivity)

            bar.background =
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(accent.start, accent.end)
                ).apply {
                    cornerRadius = dp(2).toFloat()
                }

            addView(bar, LinearLayout.LayoutParams(dp(4), dp(36)))
        }

        fun setCount(count: Int) {
            countChip.text = count.toString()
        }

        fun setSubtitle(text: String) {

            subtitle.text = text

            val hasArabic = text.any { it in '\u0600'..'\u06FF' }

            subtitle.letterSpacing = if (hasArabic) 0f else 0.15f
        }
    }

    private inner class LogoBadge(
        sizeDp: Int,
        private val accent: Accent
    ) : FrameLayout(this@MainActivity) {

        private val image = ImageView(this@MainActivity)

        private val letter = TextView(this@MainActivity)

        private val radius = dp(sizeDp / 3).toFloat()

        private var currentUrl = ""

        init {

            image.scaleType = ImageView.ScaleType.FIT_CENTER

            image.setPadding(dp(5), dp(5), dp(5), dp(5))

            image.visibility = View.GONE

            letter.textSize = sizeDp * 0.42f

            letter.setTextColor(Color.WHITE)

            letter.typeface = Typeface.DEFAULT_BOLD

            letter.gravity = Gravity.CENTER

            addView(image, FrameLayout.LayoutParams(matchParent, matchParent))

            addView(letter, FrameLayout.LayoutParams(matchParent, matchParent))

            showFallback()
        }

        private fun showFallback() {

            background =
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(accent.start, accent.end)
                ).apply {
                    cornerRadius = radius
                }

            image.visibility = View.GONE

            letter.visibility = View.VISIBLE
        }

        private fun showImage(bitmap: Bitmap) {

            image.setImageBitmap(bitmap)

            image.visibility = View.VISIBLE

            letter.visibility = View.GONE

            background =
                GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(Color.argb(242, 250, 250, 255))
                }
        }

        fun bind(name: String, url: String) {

            currentUrl = url

            letter.text = name.trim().take(1).uppercase()

            showFallback()

            if (!url.startsWith("http", ignoreCase = true)) {
                return
            }

            val cached = imageCache.get(url)

            if (cached != null) {
                showImage(cached)
                return
            }

            try {

                ioExecutor.execute {

                    val bitmap = downloadBitmap(url)

                    if (bitmap != null) {

                        imageCache.put(url, bitmap)

                        post {
                            if (currentUrl == url) {
                                showImage(bitmap)
                            }
                        }
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    private inner class BannerCard(
        private val big: Boolean,
        private val accent: Accent
    ) : LinearLayout(this@MainActivity) {

        private val numberView =
            label("", if (big) 24f else 13f, white, true, Gravity.CENTER)

        private val badge = LogoBadge(if (big) 58 else 38, accent)

        private val nameView = label("", if (big) 22f else 15f, white, true)

        private val subView = label("", 12f, gray)

        private val qualityChip = TextView(this@MainActivity)

        init {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(dp(12), 0, dp(14), 0)

            alpha = if (big) 1f else 0.7f

            background =
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        if (big) mix(accent.start, bgTop, 0.35f) else Color.argb(215, 12, 16, 38),
                        if (big) mix(accent.end, bgTop, 0.35f) else Color.argb(215, 8, 10, 28)
                    )
                ).apply {
                    cornerRadius = dp(if (big) 26 else 20).toFloat()
                    setStroke(
                        if (big) dp(2) else dp(1),
                        if (big) Color.WHITE else Color.argb(60, 255, 255, 255)
                    )
                }

            addView(numberView, LinearLayout.LayoutParams(dp(if (big) 52 else 34), matchParent))

            val texts = LinearLayout(this@MainActivity)

            texts.orientation = LinearLayout.VERTICAL

            texts.gravity = Gravity.CENTER_VERTICAL

            texts.setPadding(dp(6), 0, dp(10), 0)

            setupFit(nameView, if (big) 22 else 15, 10)

            nameView.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

            texts.addView(
                nameView,
                LinearLayout.LayoutParams(matchParent, dp(if (big) 30 else 22))
            )

            if (big) {

                val row = LinearLayout(this@MainActivity)

                row.orientation = LinearLayout.HORIZONTAL

                row.gravity = Gravity.CENTER_VERTICAL

                qualityChip.textSize = 11f

                qualityChip.typeface = Typeface.DEFAULT_BOLD

                qualityChip.gravity = Gravity.CENTER

                qualityChip.setPadding(dp(8), dp(2), dp(8), dp(2))

                qualityChip.visibility = View.GONE

                row.addView(qualityChip, LinearLayout.LayoutParams(wrap, wrap))

                row.addView(
                    subView,
                    LinearLayout.LayoutParams(0, wrap, 1f).apply {
                        setMargins(dp(8), 0, 0, 0)
                    }
                )

                texts.addView(row, LinearLayout.LayoutParams(matchParent, wrap))
            }

            addView(texts, LinearLayout.LayoutParams(0, matchParent, 1f))

            addView(badge, LinearLayout.LayoutParams(dp(if (big) 58 else 38), dp(if (big) 58 else 38)))
        }

        fun bind(channel: Channel, number: Int, sub: String) {

            numberView.text = String.format(Locale.US, "%02d", number)

            fitName(nameView, channel.name, if (big) 22 else 15, 10)

            subView.text = sub

            badge.bind(channel.name, channel.logo)
        }

        fun setQuality(text: String, color: Int) {

            if (!big) {
                return
            }

            if (text.isEmpty()) {

                qualityChip.visibility = View.GONE

            } else {

                qualityChip.text = text

                qualityChip.setTextColor(color)

                qualityChip.background =
                    GradientDrawable().apply {
                        cornerRadius = dp(8).toFloat()
                        setColor(Color.argb(150, 8, 10, 28))
                        setStroke(dp(1), color)
                    }

                qualityChip.visibility = View.VISIBLE
            }
        }
    }

    private inner class TvCard(
        val accent: Accent,
        badgeSizeDp: Int,
        leadWidthDp: Int
    ) : LedFrame() {

        val lead = FrameLayout(this@MainActivity)

        val badge = LogoBadge(badgeSizeDp, accent)

        val nameView = label("", 16f, white, true)

        val subView = label("", 11.5f, gray)

        var stateListener: ((Boolean, Boolean) -> Unit)? = null

        var onFocused: (() -> Unit)? = null

        var focusedNow = false
            private set

        var chosen = false
            set(value) {
                field = value
                refresh()
            }

        var moving = false
            set(value) {
                field = value
                refresh()
            }

        init {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(dp(10), 0, dp(12), 0)

            isFocusable = true

            isFocusableInTouchMode = !phoneMode

            clipChildren = false

            // LED light runs around the card that has the focus
            cornerDp = 18

            drawBase = false

            setLed(false)

            setColors(accent.start, accent.end)

            addView(lead, LinearLayout.LayoutParams(dp(leadWidthDp), matchParent))

            val texts = LinearLayout(this@MainActivity)

            texts.orientation = LinearLayout.VERTICAL

            texts.gravity = Gravity.CENTER_VERTICAL

            texts.setPadding(dp(8), 0, dp(12), 0)

            setupFit(nameView, 16, 9)

            nameView.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

            texts.addView(nameView, LinearLayout.LayoutParams(matchParent, dp(24)))

            texts.addView(
                subView,
                LinearLayout.LayoutParams(matchParent, wrap).apply {
                    topMargin = dp(2)
                }
            )

            addView(texts, LinearLayout.LayoutParams(0, matchParent, 1f))

            addView(
                badge,
                LinearLayout.LayoutParams(dp(badgeSizeDp), dp(badgeSizeDp))
            )

            refresh()
        }

        override fun onFocusChanged(
            gainFocus: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {

            super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

            focusedNow = gainFocus

            setLed(gainFocus)

            val scale = if (gainFocus) 1.04f else 1f

            animate()
                .scaleX(scale)
                .scaleY(scale)
                .setStartDelay(0)
                .setDuration(130)
                .start()

            refresh()

            if (gainFocus) {
                playNavFeedback(this)
                onFocused?.invoke()
            }
        }

        private fun refresh() {

            val d: GradientDrawable

            if (moving) {

                d =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(withAlpha(gold, 150), withAlpha(Color.rgb(249, 115, 22), 150))
                    )

                d.setStroke(dp(3), gold)

            } else if (focusedNow) {

                d =
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(
                            mix(accent.start, bgMid, 0.62f),
                            mix(accent.end, bgMid, 0.62f)
                        )
                    )

                d.setStroke(dp(1), Color.argb(110, 255, 255, 255))

            } else if (chosen) {

                d = GradientDrawable()

                d.setColor(withAlpha(accent.start, 46))

                d.setStroke(dp(1), withAlpha(accent.start, 190))

            } else {

                d = GradientDrawable()

                d.setColor(Color.argb(20, 255, 255, 255))

                d.setStroke(dp(1), Color.argb(26, 255, 255, 255))
            }

            d.cornerRadius = dp(18).toFloat()

            background = d

            nameView.setTextColor(white)

            subView.setTextColor(if (focusedNow) dimWhite else gray)

            stateListener?.invoke(focusedNow, chosen)
        }
    }

    // =========================
    // Card factories
    // =========================

    private fun createPackageCard(item: PackageItem, index: Int): TvCard {

        val accent = accentOfPackage(item)

        val card = TvCard(accent, 44, 18)

        val count = channels.count { belongs(it, item) }

        fitName(card.nameView, item.name, 16, 9)

        card.subView.text = "$count \u0642\u0646\u0627\u0629"

        if (item.id == FAV_ID) {
            card.badge.bind("★", "")
        } else {
            card.badge.bind(item.name, item.logo)
        }

        val dot = View(this)

        dot.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent.start)
            }

        dot.visibility = View.INVISIBLE

        card.lead.addView(
            dot,
            FrameLayout.LayoutParams(dp(8), dp(8), Gravity.CENTER)
        )

        card.stateListener = { focused, chosen ->

            dot.visibility = if (chosen) View.VISIBLE else View.INVISIBLE

            val dotBg = dot.background as GradientDrawable

            dotBg.setColor(
                if (focused) Color.WHITE else accent.start
            )
        }

        return card
    }

    private fun createChannelRow(
        channel: Channel,
        number: Int,
        accent: Accent
    ): ChannelRow {

        val card = TvCard(accent, 46, 34)

        val hasUrl = channel.url.isNotBlank()

        val isVod = hasUrl && isVodChannel(channel)

        fitName(card.nameView, channel.name, 16, 9)

        card.badge.bind(channel.name, channel.logo)

        val numberView =
            label(
                String.format(Locale.US, "%02d", number),
                12f,
                gray,
                true,
                Gravity.CENTER
            )

        val playIcon = EqualizerView(accent.start)

        playIcon.visibility = View.GONE

        card.lead.addView(
            numberView,
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        card.lead.addView(
            playIcon,
            FrameLayout.LayoutParams(dp(20), dp(18), Gravity.CENTER)
        )

        if (channel.isNew) {

            val newBadge = TextView(this)

            newBadge.text = "جديد"

            newBadge.textSize = 8f

            newBadge.typeface = Typeface.DEFAULT_BOLD

            newBadge.setTextColor(Color.rgb(40, 24, 0))

            newBadge.setPadding(dp(4), dp(1), dp(4), dp(1))

            newBadge.background =
                GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(gold)
                }

            card.lead.addView(
                newBadge,
                FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                    bottomMargin = dp(7)
                }
            )
        }

        if (isFavorite(channel)) {

            val star = TextView(this)

            star.text = "★"

            star.textSize = 11f

            star.setTextColor(gold)

            card.lead.addView(
                star,
                FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                    topMargin = dp(6)
                }
            )
        }

        var playing = false

        fun applyState() {

            val focused = card.focusedNow

            numberView.visibility = if (playing) View.GONE else View.VISIBLE

            playIcon.visibility = if (playing) View.VISIBLE else View.GONE

            playIcon.setTint(if (focused) Color.WHITE else accent.start)

            numberView.setTextColor(if (focused) dimWhite else gray)

            // a match of today on this channel replaces "live broadcast"
            val match = if (hasUrl) matchForChannel(channel) else null

            val epg = if (hasUrl) epgNowText(channel) else null

            card.subView.text =
                when {
                    !hasUrl -> "غير متاح حالياً"
                    match != null -> matchSubText(match)
                    epg != null -> epg
                    playing -> "يعرض الآن"
                    isVod -> "فيديو"
                    else -> "بث مباشر"
                }

            card.subView.setTextColor(
                when {
                    focused -> dimWhite
                    match != null -> if (isLiveMatch(match)) Color.rgb(255, 99, 120) else gold
                    epg != null -> accent.start
                    playing -> accent.start
                    else -> gray
                }
            )

            card.alpha = if (hasUrl) 1f else 0.55f
        }

        card.stateListener = { _, _ ->
            applyState()
        }

        applyState()

        card.onFocused = {
            lastFocusedChannel = channel

            requestTint(channel.logo, accent.start)

            schedulePreview(channel)
        }

        card.setOnLongClickListener {
            showChannelMenu(channel)
            true
        }

        card.setOnClickListener {

            val alreadyPlaying =
                playingChannel == channel &&
                        (playingWeb || player.playbackState != Player.STATE_IDLE)

            if (alreadyPlaying) {
                enterFullscreen()
            } else {
                playChannel(channel)
            }
        }

        return ChannelRow(channel, card) { value ->
            playing = value
            applyState()
        }
    }

    // =========================
    // Firebase
    // =========================

    private val loadTimeout = Runnable {

        if (!dataReady) {

            showListMessage(packagesList, "\u0644\u0627 \u064a\u0648\u062c\u062f \u0627\u062a\u0635\u0627\u0644", "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a")

            showListMessage(
                channelsList,
                Txt.LOAD_FAIL_TITLE,
                Txt.LOAD_FAIL_SUB
            )
        }
    }

    private fun startFirebase() {

        showSkeleton(packagesList, 6)

        showSkeleton(channelsList, 7)

        mainHandler.postDelayed(loadTimeout, 12000)

        for (key in listOf("settings", "packages", "channels", "matches", "epg", "movies", "series")) {

            val listener =
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        parts[key] = snapshot
                        onDataLoaded()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onDataError()
                    }
                }

            val ref = rootRef.child(key)

            listeners.add(Pair(ref, listener))

            ref.addValueEventListener(listener)
        }
    }

    private fun onDataError() {

        mainHandler.removeCallbacks(loadTimeout)

        Toast.makeText(this, Txt.DB_ERROR, Toast.LENGTH_LONG).show()

        if (!dataReady) {

            showListMessage(packagesList, "تعذر الاتصال", "تحقق من الإعدادات")

            showListMessage(channelsList, "تعذر الاتصال", "بقاعدة البيانات")
        }
    }

    private fun onDataLoaded() {

        val settingsPart = parts["settings"]

        readWeatherSettings(settingsPart)

        applyRemoteSettings(settingsPart)

        updateMatches(parts["matches"])

        updateEpg(parts["epg"])

        updateVod(parts["movies"], parts["series"])

        if (!parts.containsKey("channels") && !parts.containsKey("packages")) {
            return
        }

        mainHandler.removeCallbacks(loadTimeout)

        val newChannels = parseChannels(parts["channels"])

        var newPackages = parsePackages(parts["packages"])

        if (newPackages.isEmpty() && newChannels.isNotEmpty()) {

            newPackages =
                newChannels
                    .map { it.group }
                    .distinct()
                    .mapIndexed { i, g ->
                        PackageItem(g, g, "", true, i)
                    }
        }

        if (
            dataReady &&
            newPackages == basePackages &&
            newChannels == channels
        ) {
            return
        }

        dataReady = true

        basePackages = newPackages

        channels = newChannels

        rebuildPackages()

        renderPackages()

        val keep =
            packages.firstOrNull { it.id == selectedPackage }
                ?: packages.firstOrNull { it.id != FAV_ID }
                ?: packages.firstOrNull()

        if (keep != null) {

            selectPackage(keep.id, true)

        } else {

            selectedPackage = ""

            renderChannels()
        }

        if (!autoPlayDone) {
            autoPlayDone = true
            playLastChannel()
        }

        if (currentFocus == null && !isFullscreen) {
            focusSelectedPackage()
        }
    }

    private fun DataSnapshot.readString(key: String): String? =
        child(key)
            .value
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun DataSnapshot.readBool(key: String, default: Boolean): Boolean {

        return when (val v = child(key).value) {

            is Boolean -> v

            is Number -> v.toInt() != 0

            is String -> !(v.equals("false", true) || v == "0")

            else -> default
        }
    }

    private fun DataSnapshot.readInt(key: String, default: Int): Int {

        return when (val v = child(key).value) {

            is Number -> v.toInt()

            is String -> v.trim().toIntOrNull() ?: default

            else -> default
        }
    }

    private fun parsePackages(snapshot: DataSnapshot?): List<PackageItem> {

        val result = mutableListOf<PackageItem>()

        for (child in snapshot?.children ?: emptyList()) {

            val id = child.key ?: continue

            if (!child.readBool("enabled", true)) {
                continue
            }

            result.add(
                PackageItem(
                    id = id,
                    name = child.readString("name") ?: id,
                    logo = child.readString("logo") ?: "",
                    enabled = true,
                    order = child.readInt("order", 999)
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun parseChannels(snapshot: DataSnapshot?): List<Channel> {

        val result = mutableListOf<Channel>()

        for (child in snapshot?.children ?: emptyList()) {

            val name = child.readString("name") ?: continue

            val group = child.readString("group") ?: continue

            if (!child.readBool("enabled", true)) {
                continue
            }

            result.add(
                Channel(
                    name = name,
                    group = group,
                    url = child.readString("url") ?: "",
                    logo = child.readString("logo") ?: "",
                    enabled = true,
                    order = child.readInt("order", 999),
                    userAgent =
                        child.readString("userAgent")
                            ?: child.readString("user_agent")
                            ?: "",
                    referer = child.readString("referer") ?: "",
                    backups = collectBackups(child),
                    isNew = isNewChannel(child),
                    audios = collectAudios(child),
                    epgId = child.readString("epgId") ?: ""
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun belongs(channel: Channel, pkg: PackageItem): Boolean =
        if (pkg.id == FAV_ID) {
            isFavorite(channel)
        } else {
            channel.group.equals(pkg.id, ignoreCase = true) ||
                    channel.group.equals(pkg.name, ignoreCase = true)
        }

    // =========================
    // Render lists
    // =========================

    private fun showListMessage(list: LinearLayout, title: String, sub: String) {

        clearList(list)

        val box = LinearLayout(this)

        box.orientation = LinearLayout.VERTICAL

        box.gravity = Gravity.CENTER

        box.setPadding(dp(12), dp(36), dp(12), dp(36))

        box.addView(
            label(title, 16f, white, true, Gravity.CENTER, 2),
            lp(matchParent, wrap)
        )

        box.addView(
            label(sub, 12f, gray, false, Gravity.CENTER, 3),
            lp(matchParent, wrap).apply {
                topMargin = dp(6)
            }
        )

        list.addView(box, lp(matchParent, wrap))
    }

    private fun renderPackages() {

        val focusedId =
            packageRows
                .firstOrNull { it.card.hasFocus() }
                ?.item
                ?.id

        clearList(packagesList)
        packageRows.clear()

        packages.forEachIndexed { index, item ->

            val card = createPackageCard(item, index)

            card.chosen = item.id == selectedPackage

            card.onFocused = {
                if (moveState == null) {
                    scheduleSelect(item.id)

                    cancelPreview()

                    requestTint(item.logo, accentOfPackage(item).start)
                }
            }

            if (item.id != FAV_ID) {
                card.setOnLongClickListener {
                    showPackageMenu(item)
                    true
                }
            }

            card.setOnClickListener {

                mainHandler.removeCallbacks(selectRunnable)

                pendingPackageId = null

                selectPackage(item.id)

                focusChannelsColumn()
            }

            packagesList.addView(
                card,
                if (phoneMode) {
                    lp(dp(172), dp(60)).apply { setMargins(dp(5), 0, dp(5), 0) }
                } else {
                    lp(matchParent, dp(66)).apply { setMargins(0, dp(5), 0, dp(5)) }
                }
            )

            packageRows.add(PackageRow(item, card))
        }

        packagesHeader.setCount(packages.size)

        if (focusedId != null) {

            packageRows
                .firstOrNull { it.item.id == focusedId }
                ?.card
                ?.requestFocus()
        }

        refreshMovingVisual()
    }

    private val selectRunnable = Runnable {

        val id = pendingPackageId

        pendingPackageId = null

        if (id != null) {
            selectPackage(id)
        }
    }

    private fun scheduleSelect(id: String) {

        pendingPackageId = id

        mainHandler.removeCallbacks(selectRunnable)

        mainHandler.postDelayed(selectRunnable, 180)
    }

    private fun flushPendingSelect() {

        mainHandler.removeCallbacks(selectRunnable)

        val id = pendingPackageId

        pendingPackageId = null

        if (id != null) {
            selectPackage(id)
        }
    }

    private fun selectPackage(id: String, force: Boolean = false) {

        if (!force && id == selectedPackage) {
            return
        }

        selectedPackage = id

        packageRows.forEach {
            it.card.chosen = it.item.id == id
        }

        renderChannels()
    }

    private fun renderChannels(keepScroll: Boolean = false) {

        val restoreIndex = channelRows.indexOfFirst { it.card.hasFocus() }

        clearList(channelsList)
        channelRows.clear()

        lastFocusedChannel = null

        val pkgIndex = packages.indexOfFirst { it.id == selectedPackage }

        val pkg = packages.getOrNull(pkgIndex)

        channelsHeader.setSubtitle(pkg?.name ?: "CHANNELS")

        visibleChannels =
            if (pkg == null) {
                emptyList()
            } else {

                val base =
                    channels
                        .filter { belongs(it, pkg) }
                        .sortedBy { it.order }

                val saved = loadChannelOrder(pkg.id)

                if (saved.isEmpty()) {
                    base
                } else {
                    val idx = saved.withIndex().associate { it.value to it.index }
                    base.sortedBy { idx[channelKey(it)] ?: Int.MAX_VALUE }
                }
            }

        channelsHeader.setCount(visibleChannels.size)

        if (!keepScroll) {
            channelScroll.scrollTo(0, 0)
        }

        if (visibleChannels.isEmpty()) {

            showListMessage(
                channelsList,
                Txt.NO_CHANNELS,
                Txt.NO_CHANNELS_SUB
            )

            return
        }

        val accent = pkg?.let { accentOfPackage(it) } ?: accentFor(0)

        visibleChannels.forEachIndexed { i, channel ->

            val row = createChannelRow(channel, i + 1, accent)

            row.setPlaying(channel == playingChannel)

            channelsList.addView(
                row.card,
                lp(matchParent, dp(66)).apply {
                    setMargins(0, dp(5), 0, dp(5))
                }
            )

            channelRows.add(row)

            // the cards slide in one after another
            if (!keepScroll && i < 10) {

                val targetAlpha = row.card.alpha

                row.card.alpha = 0f

                row.card.translationX = dp(40).toFloat()

                row.card.animate()
                    .alpha(targetAlpha)
                    .translationX(0f)
                    .setStartDelay(i * 45L)
                    .setDuration(260)
                    .start()
            }
        }

        if (restoreIndex >= 0) {

            channelRows
                .getOrNull(minOf(restoreIndex, channelRows.size - 1))
                ?.card
                ?.requestFocus()
        }

        refreshMovingVisual()
    }

    // =========================
    // Playback
    // =========================

    private fun sourcesOf(channel: Channel): List<String> =
        (listOf(channel.url) + channel.backups)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun playChannel(channel: Channel, preview: Boolean = false) {

        if (blocked || gated) {
            return
        }

        if (sourcesOf(channel).isEmpty()) {

            Toast.makeText(this, Txt.NO_URL, Toast.LENGTH_SHORT).show()

            return
        }

        playToken++

        mainHandler.removeCallbacks(retryRunnable)
        mainHandler.removeCallbacks(hideWebStatusRunnable)

        updateQuality(0)

        val old = playingChannel

        if (old != null && old != channel && !old.isVod && !channel.isVod) {
            previousChannel = old
        }

        // a new channel starts with its original sound
        stopExternalAudio()

        playingChannel = channel

        playingList = visibleChannels

        beginWatch(channel)

        updatePresenceChannel()

        if (!preview && !channel.isVod) {
            prefs.edit().putString("last_channel", channelKey(channel)).apply()
        }

        updatePlayingMarks()

        updateNowPlaying(channel)

        if (!preview) {
            requestTint(channel.logo, cyan)
        }

        requestPlayerGlow(channel.logo)

        startSource(channel, savedServerIndex(channel))
    }

    // starts one of the channel links (0 = main link, 1.. = backups)
    private fun startSource(channel: Channel, index: Int, resetUa: Boolean = true) {

        val raw = sourcesOf(channel).getOrNull(index) ?: return

        val source = parseSource(raw)

        sourceIndex = index

        retryCount = 0
        liveWindowRetries = 0
        finalPlaybackError = false
        playbackStarted = false
        firstError = null

        if (resetUa) {
            uaIndex = savedUaIndex(channel)
        }

        mainHandler.removeCallbacks(retryRunnable)
        mainHandler.removeCallbacks(hideWebStatusRunnable)

        if (index > 0) {
            nowSub.text = nowSubText(channel) + "  •  السيرفر ${index + 1}"
        }

        // ---- YouTube / Facebook -> WebView ----
        if (isWebSource(source.url)) {

            currentSource = null

            playWeb(channel, source.url)

            applyControllerMode()

            return
        }

        // ---- Normal stream -> ExoPlayer ----
        stopWeb()

        // every new link starts with automatic quality
        try {
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .build()
        } catch (_: Exception) {
        }

        applyControllerMode()

        currentSource = source

        attempts = attemptsFor(source.url)

        attemptIndex = 0

        showStatus(
            "جاري تشغيل القناة…",
            channel.name + (if (index > 0) "  •  السيرفر ${index + 1}" else ""),
            false
        )

        startCurrentAttempt(channel)
    }

    // the current link is dead: move to the next backup link (if any)
    private fun tryNextSource(channel: Channel): Boolean {

        // the viewer chooses the server himself (can be enabled in the settings)
        if (!autoFallback) {
            return false
        }

        // offline: keep the error, the stream restarts when the connection is back
        if (!online) {
            return false
        }

        val urls = sourcesOf(channel)

        val next = sourceIndex + 1

        if (next >= urls.size) {
            return false
        }

        val token = playToken

        showStatus(
            "تعذر تشغيل السيرفر ${next}",
            "جاري التحويل إلى السيرفر ${next + 1} من ${urls.size}",
            false
        )

        mainHandler.postDelayed(
            {
                if (token == playToken && !isFinishing && !blocked) {
                    startSource(channel, next)
                }
            },
            700
        )

        return true
    }

    private fun collectBackups(child: DataSnapshot): List<String> {

        val list = mutableListOf<String>()

        for (key in listOf("url2", "url3", "url4", "url5", "backupUrl", "backup_url")) {
            child.readString(key)?.let { list.add(it) }
        }

        for (b in child.child("backups").children) {
            b.value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { list.add(it) }
        }

        return list
    }

    private fun showDetailedPlaybackError(
        error: PlaybackException?,
        exception: Throwable?
    ) {

        val message =
            if (error != null) {

                buildPlaybackErrorMessage(error)

            } else {

                val cause = exception?.cause

                val text =
                    exception?.message
                        ?: cause?.message
                        ?: exception?.javaClass?.simpleName
                        ?: "\u062e\u0637\u0623 \u063a\u064a\u0631 \u0645\u0639\u0631\u0648\u0641"

                "\u062e\u0637\u0623 \u062f\u0627\u062e\u0644\u064a\n${shortenError(text)}"
            }

        val friendly =
            if (error != null) {
                friendlyError(error)
            } else {
                Pair("تعذر تشغيل القناة", "حدث خطأ غير متوقع")
            }

        showStatus(friendly.first, friendly.second + serverHint(), true)
    }

    private fun updatePlayingMarks() {

        channelRows.forEach {
            it.setPlaying(it.channel == playingChannel)
        }
    }

    private fun updateNowPlaying(channel: Channel) {

        fitName(nowTitle, channel.name, 16, 9)

        nowSub.text = nowSubText(channel)

        livePill.visibility =
            if (isVodChannel(channel)) View.GONE else View.VISIBLE
    }

    private fun zap(delta: Int) {

        val list = playingList

        if (list.isEmpty()) {
            return
        }

        var index = list.indexOf(playingChannel)

        if (index < 0) {
            index = if (delta > 0) -1 else 0
        }

        repeat(list.size) {

            index = (index + delta + list.size) % list.size

            val candidate = list[index]

            if (candidate.url.isNotBlank()) {

                lastFocusedChannel = candidate

                playChannel(candidate)

                showZapBanner(candidate)

                return
            }
        }
    }

    private val hideZapRunnable = Runnable {

        zapBanner.animate()
            .alpha(0f)
            .setDuration(220)
            .withEndAction {
                zapBanner.visibility = View.GONE
            }
            .start()
    }

    private fun neighborOf(list: List<Channel>, index: Int, delta: Int): Channel? {

        if (list.size < 2) {
            return null
        }

        var i = index

        repeat(list.size) {

            i = (i + delta + list.size) % list.size

            val c = list[i]

            if (i != index && c.url.isNotBlank()) {
                return c
            }
        }

        return null
    }

    private fun showZapBanner(channel: Channel) {

        val list =
            if (playingList.contains(channel)) playingList else listOf(channel)

        val index = list.indexOf(channel)

        val prev = neighborOf(list, index, -1)

        val next = neighborOf(list, index, 1)

        fun pkgName(c: Channel): String =
            realPackageName(c)

        bannerCur.bind(channel, index + 1, epgNowText(channel) ?: pkgName(channel))

        bannerCur.setQuality(qualityText, qualityColor)

        if (prev != null) {
            bannerPrev.bind(prev, list.indexOf(prev) + 1, "")
            bannerPrev.visibility = View.VISIBLE
        } else {
            bannerPrev.visibility = View.INVISIBLE
        }

        if (next != null) {
            bannerNext.bind(next, list.indexOf(next) + 1, "")
            bannerNext.visibility = View.VISIBLE
        } else {
            bannerNext.visibility = View.INVISIBLE
        }

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        if (zapBanner.visibility != View.VISIBLE) {
            zapBanner.alpha = 0f
            zapBanner.visibility = View.VISIBLE
        }

        zapBanner.animate().alpha(1f).setDuration(180).start()

        mainHandler.postDelayed(hideZapRunnable, 4200)
    }

    private fun buildZapBanner() {

        zapBanner = LinearLayout(this)

        zapBanner.orientation = LinearLayout.HORIZONTAL

        zapBanner.gravity = Gravity.CENTER_VERTICAL

        zapBanner.visibility = View.GONE

        bannerPrev = BannerCard(false, accentFor(1))

        bannerCur = BannerCard(true, accentFor(0))

        bannerNext = BannerCard(false, accentFor(2))

        zapBanner.addView(
            bannerPrev,
            lp(0, dp(64), 0.27f).apply { setMargins(0, 0, dp(10), 0) }
        )

        zapBanner.addView(bannerCur, lp(0, dp(92), 0.46f))

        zapBanner.addView(
            bannerNext,
            lp(0, dp(64), 0.27f).apply { setMargins(dp(10), 0, 0, 0) }
        )

        fullscreenContainer.addView(
            zapBanner,
            FrameLayout.LayoutParams(matchParent, wrap, Gravity.BOTTOM).apply {
                setMargins(dp(48), 0, dp(48), dp(40))
            }
        )
    }

    // =========================
    // Quality badge
    // =========================

    private fun buildQualityBadge() {

        qualityBadge = TextView(this)

        qualityBadge.textSize = 11f

        qualityBadge.setTextColor(white)

        qualityBadge.typeface = Typeface.DEFAULT_BOLD

        qualityBadge.gravity = Gravity.CENTER

        qualityBadge.setPadding(dp(8), dp(3), dp(8), dp(3))

        qualityBadge.visibility = View.GONE
    }

    private fun updateQuality(height: Int) {

        val (text, color) =
            when {
                height >= 2160 -> "4K UHD" to Color.rgb(250, 204, 21)
                height >= 1440 -> "2K QHD" to Color.rgb(250, 204, 21)
                height >= 1080 -> "FHD 1080p" to Color.rgb(52, 211, 153)
                height >= 720 -> "HD 720p" to cyan
                height >= 480 -> "SD 480p" to Color.rgb(255, 183, 77)
                height > 0 -> "SD ${height}p" to Color.rgb(255, 120, 100)
                else -> "" to Color.WHITE
            }

        qualityText = text

        qualityColor = color

        if (text.isEmpty() || playingWeb) {

            qualityBadge.visibility = View.GONE

        } else {

            qualityBadge.text = text

            qualityBadge.setTextColor(color)

            qualityBadge.background =
                GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.argb(190, 8, 10, 28))
                    setStroke(dp(1), color)
                }

            qualityBadge.visibility = View.VISIBLE
        }

        if (::bannerCur.isInitialized) {
            bannerCur.setQuality(qualityText, qualityColor)
        }
    }

    // =========================
    // Weather + date
    // =========================

    private val weatherRunnable = object : Runnable {
        override fun run() {
            fetchWeather()
            mainHandler.postDelayed(this, 30 * 60 * 1000L)
        }
    }

    private val dateRunnable = object : Runnable {
        override fun run() {
            updateDate()
            mainHandler.postDelayed(this, 30 * 1000L)
        }
    }

    private fun updateDate() {

        try {

            dateView.text = dateTextFormat.format(Date())

        } catch (_: Exception) {
        }
    }

    private fun weatherEmoji(code: Int): String =
        when (code) {
            0 -> "\u2600\uFE0F"
            1 -> "\uD83C\uDF24\uFE0F"
            2 -> "\u26C5"
            3 -> "\u2601\uFE0F"
            45, 48 -> "\uD83C\uDF2B\uFE0F"
            in 51..57 -> "\uD83C\uDF26\uFE0F"
            in 61..67 -> "\uD83C\uDF27\uFE0F"
            in 71..77 -> "\u2744\uFE0F"
            in 80..82 -> "\uD83C\uDF27\uFE0F"
            85, 86 -> "\u2744\uFE0F"
            in 95..99 -> "\u26C8\uFE0F"
            else -> "\u2601\uFE0F"
        }

    private fun fetchWeather() {

        val lat = weatherLat

        val lon = weatherLon

        try {

            ioExecutor.execute {

                var connection: HttpURLConnection? = null

                try {

                    val api =
                        "https://api.open-meteo.com/v1/forecast" +
                                "?latitude=$lat&longitude=$lon" +
                                "&current=temperature_2m,weather_code&timezone=auto"

                    connection = URL(api).openConnection() as HttpURLConnection

                    connection.connectTimeout = 8000

                    connection.readTimeout = 8000

                    if (connection.responseCode !in 200..299) {
                        return@execute
                    }

                    val text =
                        connection.inputStream
                            .use { it.readBytes() }
                            .toString(Charsets.UTF_8)

                    val current = JSONObject(text).getJSONObject("current")

                    val temp = current.getDouble("temperature_2m")

                    val code = current.getInt("weather_code")

                    mainHandler.post {
                        weatherTemp.text = "${Math.round(temp)}\u00B0"
                        weatherIcon.text = weatherEmoji(code)
                        weatherCityView.text = weatherCity
                    }

                } catch (_: Exception) {

                } finally {

                    connection?.disconnect()
                }
            }

        } catch (_: Exception) {
        }
    }

    // optional Firebase settings: settings/weather_lat, weather_lon, weather_city
    private fun readWeatherSettings(snapshot: DataSnapshot?) {

        val settings = snapshot ?: return

        val lat = settings.child("weather_lat").value?.toString()?.toDoubleOrNull()

        val lon = settings.child("weather_lon").value?.toString()?.toDoubleOrNull()

        val city = settings.readString("weather_city")

        var changed = false

        if (lat != null && lon != null && (lat != weatherLat || lon != weatherLon)) {
            weatherLat = lat
            weatherLon = lon
            changed = true
        }

        if (city != null && city != weatherCity) {
            weatherCity = city
            weatherCityView.text = city
        }

        if (changed) {
            fetchWeather()
        }
    }

    // =========================
    // Favorites / custom order
    // =========================

    private fun channelKey(channel: Channel): String =
        channel.group.lowercase(Locale.US) + "|" + channel.name

    private fun isFavorite(channel: Channel): Boolean =
        favorites.contains(channelKey(channel))

    private fun isVodChannel(channel: Channel): Boolean =
        channel.isVod ||
                (channel.url.isNotBlank() &&
                        detectKind(parseSource(channel.url).url) == StreamKind.MP4)

    private fun accentOfPackage(pkg: PackageItem): Accent {

        if (pkg.id == FAV_ID) {
            return Accent(Color.rgb(250, 204, 21), Color.rgb(249, 115, 22))
        }

        val i = basePackages.indexOfFirst { it.id == pkg.id }

        return accentFor(if (i < 0) 0 else i)
    }

    private fun realPackageName(channel: Channel): String =
        packages
            .firstOrNull { it.id != FAV_ID && belongs(channel, it) }
            ?.name
            ?: channel.group

    // builds the displayed packages list: favorites first, then the user's order
    private fun rebuildPackages() {

        val saved =
            (prefs.getString("order_pkgs", "") ?: "")
                .split("\n")
                .filter { it.isNotBlank() }

        val idx = saved.withIndex().associate { it.value to it.index }

        val ordered = basePackages.sortedBy { idx[it.id] ?: Int.MAX_VALUE }

        val hasFavorites = channels.any { isFavorite(it) }

        packages =
            if (hasFavorites) {
                listOf(PackageItem(FAV_ID, "المفضلة", "", true, -1)) + ordered
            } else {
                ordered
            }
    }

    private fun loadChannelOrder(pkgId: String): List<String> =
        (prefs.getString("order_ch_$pkgId", "") ?: "")
            .split("\n")
            .filter { it.isNotBlank() }

    private fun saveChannelOrder(pkgId: String, list: List<Channel>) {
        prefs.edit()
            .putString("order_ch_$pkgId", list.joinToString("\n") { channelKey(it) })
            .apply()
    }

    private fun toggleFavorite(channel: Channel) {

        val key = channelKey(channel)

        val added =
            if (favorites.contains(key)) {
                favorites.remove(key)
                false
            } else {
                favorites.add(key)
                true
            }

        prefs.edit().putStringSet("favorites", HashSet(favorites)).apply()

        rebuildPackages()

        renderPackages()

        if (selectedPackage == FAV_ID) {

            if (packages.none { it.id == FAV_ID }) {

                val first = packages.firstOrNull()

                if (first != null) {
                    selectPackage(first.id, true)
                } else {
                    selectedPackage = ""
                    renderChannels()
                }

            } else {
                renderChannels(true)
            }

        } else {

            // the gold star on the channel row
            renderChannels(true)
        }

        if (currentFocus == null && !isFullscreen) {
            focusSelectedPackage()
        }

        Toast.makeText(
            this,
            if (added) "★ تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة",
            Toast.LENGTH_SHORT
        ).show()
    }

    // resume the last watched channel automatically
    private fun playLastChannel() {

        val key = prefs.getString("last_channel", null) ?: return

        val channel =
            channels.firstOrNull { channelKey(it) == key && it.url.isNotBlank() }
                ?: return

        val pkg =
            packages.firstOrNull { it.id != FAV_ID && belongs(channel, it) }

        if (pkg != null) {
            selectPackage(pkg.id, true)
        }

        playChannel(channel)
    }

    // =========================
    // Reorder mode
    // =========================

    private fun startMove(isPackage: Boolean, id: String) {

        moveState = MoveState(isPackage, id)

        refreshMovingVisual()

        Toast.makeText(
            this,
            "وضع النقل: أعلى / أسفل للتحريك  •  OK للتثبيت",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun endMove() {

        moveState = null

        refreshMovingVisual()

        Toast.makeText(this, "تم حفظ الترتيب", Toast.LENGTH_SHORT).show()
    }

    private fun refreshMovingVisual() {

        val s = moveState

        packageRows.forEach {
            it.card.moving = s != null && s.isPackage && s.id == it.item.id
        }

        channelRows.forEach {
            it.card.moving = s != null && !s.isPackage && s.id == channelKey(it.channel)
        }
    }

    private fun handleMoveKey(event: KeyEvent): Boolean {

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    moveStep(-1)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    moveStep(1)
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    endMove()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun moveStep(delta: Int) {

        val s = moveState ?: return

        if (s.isPackage) {

            val list = packages.filter { it.id != FAV_ID }.toMutableList()

            val i = list.indexOfFirst { it.id == s.id }

            val j = i + delta

            if (i < 0 || j < 0 || j >= list.size) {
                return
            }

            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp

            prefs.edit()
                .putString("order_pkgs", list.joinToString("\n") { it.id })
                .apply()

            rebuildPackages()

            renderPackages()

            packageRows.firstOrNull { it.item.id == s.id }?.card?.requestFocus()

        } else {

            val list = visibleChannels.toMutableList()

            val i = list.indexOfFirst { channelKey(it) == s.id }

            val j = i + delta

            if (i < 0 || j < 0 || j >= list.size) {
                return
            }

            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp

            saveChannelOrder(selectedPackage, list)

            renderChannels(true)

            val pc = playingChannel

            if (pc != null && visibleChannels.contains(pc)) {
                playingList = visibleChannels
            }

            channelRows
                .firstOrNull { channelKey(it.channel) == s.id }
                ?.card
                ?.requestFocus()
        }

        refreshMovingVisual()
    }

    // =========================
    // Long-press menu
    // =========================

    private fun menuOpen(): Boolean =
        ::menuOverlay.isInitialized && menuOverlay.visibility == View.VISIBLE

    private fun buildMenuOverlay() {

        menuOverlay = FrameLayout(this)

        menuOverlay.setBackgroundColor(Color.argb(175, 4, 6, 18))

        menuOverlay.isClickable = true

        menuOverlay.visibility = View.GONE

        val box = LedFrame()

        menuBox = box

        box.cornerDp = 26

        box.setColors(cyan, violet)

        box.orientation = LinearLayout.VERTICAL

        box.setPadding(dp(20), dp(20), dp(20), dp(18))

        box.background = fancyCardBackground(26)

        menuTitle = label("", 18f, white, true, Gravity.CENTER, 2)

        menuList = LinearLayout(this)

        menuList.orientation = LinearLayout.VERTICAL

        box.addView(menuTitle, lp(matchParent, wrap))

        val menuScroll = ScrollView(this)

        menuScroll.isVerticalScrollBarEnabled = false

        menuScroll.addView(menuList)

        box.addView(
            menuScroll,
            lp(matchParent, wrap).apply { topMargin = dp(12) }
        )

        menuOverlay.addView(
            box,
            FrameLayout.LayoutParams(boxWidth(380), wrap, Gravity.CENTER)
        )

        root.addView(menuOverlay, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private fun menuItem(text: String, accent: Accent): TextView {

        val tv = label(text, 16f, white, true, Gravity.CENTER)

        tv.setPadding(dp(16), 0, dp(16), 0)

        tv.isFocusable = true

        tv.isFocusableInTouchMode = !phoneMode

        fun style(focused: Boolean) {

            tv.background =
                if (focused) {
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(
                            mix(accent.start, bgMid, 0.62f),
                            mix(accent.end, bgMid, 0.62f)
                        )
                    ).apply {
                        cornerRadius = dp(16).toFloat()
                        setStroke(dp(2), Color.WHITE)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(Color.argb(24, 255, 255, 255))
                        setStroke(dp(1), Color.argb(30, 255, 255, 255))
                    }
                }
        }

        style(false)

        tv.setOnFocusChangeListener { _, focused -> style(focused) }

        return tv
    }

    private fun showMenu(title: String, options: List<Pair<String, () -> Unit>>) {

        menuReturnFocus = currentFocus

        menuTitle.text = title

        menuList.removeAllViews()

        (menuBox.layoutParams as FrameLayout.LayoutParams).width = boxWidth(380)

        menuBox.requestLayout()

        var first: View? = null

        options.forEachIndexed { i, option ->

            val item = menuItem(option.first, accentFor(i))

            item.setOnClickListener {
                closeMenu()
                option.second()
            }

            menuList.addView(
                item,
                lp(matchParent, dp(48)).apply { topMargin = dp(6) }
            )

            if (first == null) {
                first = item
            }
        }

        // keep the D-pad focus inside the menu
        normalScreen.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        menuOverlay.visibility = View.VISIBLE

        first?.requestFocus()
    }

    private fun closeMenu() {

        menuOverlay.visibility = View.GONE

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        menuReturnFocus?.requestFocus()

        menuReturnFocus = null
    }

    private fun showChannelMenu(channel: Channel) {

        if (isFullscreen || moveState != null) {
            return
        }

        val fav = isFavorite(channel)

        val items = mutableListOf<Pair<String, () -> Unit>>()

        items.add(
            (if (fav) "★  إزالة من المفضلة" else "☆  إضافة إلى المفضلة") to
                    { toggleFavorite(channel) }
        )

        if (!phoneMode) {
            items.add("↕  نقل القناة" to { startMove(false, channelKey(channel)) })
        }

        if (multiList.contains(channel)) {
            items.add("➖  إزالة من المشاهدة المتعددة" to { removeFromMulti(channel) })
        } else {
            items.add("➕  إضافة للمشاهدة المتعددة (${multiList.size}/4)" to { addToMulti(channel) })
        }

        if (multiList.size >= 2) {
            items.add("🔲  بدء المشاهدة المتعددة (${multiList.size})" to { enterMultiView() })
        }

        items.add("إلغاء" to {})

        showMenu(channel.name, items)
    }

    private fun showPackageMenu(item: PackageItem) {

        if (isFullscreen || moveState != null || phoneMode) {
            return
        }

        showMenu(
            item.name,
            listOf(
                "↕  نقل الباقة" to { startMove(true, item.id) },
                "إلغاء" to {}
            )
        )
    }

    // =========================
    // Splash screen
    // =========================

    private val splashRunnable = object : Runnable {
        override fun run() {

            val elapsed = SystemClock.elapsedRealtime() - splashStart

            if (dataReady || elapsed >= 6000) {
                hideSplash()
            } else {
                mainHandler.postDelayed(this, 200)
            }
        }
    }

    private fun hideSplash() {

        if (!splashVisible) {
            return
        }

        splashVisible = false

        splash.animate()
            .alpha(0f)
            .setDuration(380)
            .withEndAction {
                splash.visibility = View.GONE
            }
            .start()
    }

    private fun buildSplash() {

        splash = FrameLayout(this)

        splash.isClickable = true

        splash.addView(
            AmbientBackground(),
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        val col = LinearLayout(this)

        col.orientation = LinearLayout.VERTICAL

        col.gravity = Gravity.CENTER_HORIZONTAL

        val mark = createLogoMark(56f, 34)

        col.addView(mark, lp(dp(116), dp(116)))

        val brand = label("NIYATI", 34f, white, true, Gravity.CENTER)

        brand.letterSpacing = 0.22f

        col.addView(
            brand,
            lp(wrap, wrap).apply { topMargin = dp(22) }
        )

        val sport = label("SPORTS IPTV", 13f, cyan, true, Gravity.CENTER)

        sport.letterSpacing = 0.4f

        col.addView(sport, lp(wrap, wrap))

        val track = FrameLayout(this)

        track.background =
            GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(Color.argb(40, 255, 255, 255))
            }

        val thumb = View(this)

        thumb.background =
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(cyan, violet)
            ).apply {
                cornerRadius = dp(2).toFloat()
            }

        track.addView(thumb, FrameLayout.LayoutParams(dp(60), dp(4)))

        col.addView(
            track,
            lp(dp(180), dp(4)).apply { topMargin = dp(38) }
        )

        splash.addView(col, FrameLayout.LayoutParams(wrap, wrap, Gravity.CENTER))

        root.addView(splash, FrameLayout.LayoutParams(matchParent, matchParent))

        // animations
        mark.startAnimation(
            ScaleAnimation(
                0.94f, 1.06f, 0.94f, 1.06f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 900
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
        )

        thumb.startAnimation(
            TranslateAnimation(
                -dp(60).toFloat(),
                dp(180).toFloat(),
                0f,
                0f
            ).apply {
                duration = 1100
                repeatCount = Animation.INFINITE
            }
        )

        col.alpha = 0f

        col.translationY = dp(18).toFloat()

        col.animate().alpha(1f).translationY(0f).setDuration(650).start()
    }

    // =========================
    // Quality selection
    // =========================

    private fun tierName(h: Int): String =
        when {
            h >= 2160 -> "4K"
            h >= 1080 -> "FHD"
            h >= 720 -> "HD"
            else -> "SD"
        }

    private fun collectQualities(): List<QualityOption> {

        val best = LinkedHashMap<Int, QualityOption>()

        for (g in player.currentTracks.groups) {

            if (g.type != C.TRACK_TYPE_VIDEO) {
                continue
            }

            for (i in 0 until g.length) {

                if (!g.isTrackSupported(i)) {
                    continue
                }

                val f = g.getTrackFormat(i)

                if (f.height <= 0) {
                    continue
                }

                val old = best[f.height]

                if (old == null || f.bitrate > old.bitrate) {
                    best[f.height] = QualityOption(f.height, f.bitrate, g, i)
                }
            }
        }

        return best.values.sortedByDescending { it.height }
    }

    private fun isManualQuality(): Boolean =
        player.trackSelectionParameters.overrides.values
            .any { it.type == C.TRACK_TYPE_VIDEO }

    private fun qualityButtonText(): String {

        if (playingWeb) {
            return "الجودة"
        }

        val h = player.videoSize.height

        return "الجودة: " + if (isManualQuality() && h > 0) "${h}p" else "تلقائي"
    }

    private fun setQuality(option: QualityOption?) {

        val builder = player.trackSelectionParameters.buildUpon()

        if (option == null) {
            builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            builder.setOverrideForType(
                TrackSelectionOverride(option.group.mediaTrackGroup, option.index)
            )
        }

        player.trackSelectionParameters = builder.build()

        if (::panelQualityBtn.isInitialized) {
            panelQualityBtn.text =
                if (option == null) "الجودة: تلقائي" else "الجودة: ${option.height}p"
        }

        Toast.makeText(
            this,
            if (option == null) "الجودة: تلقائي" else "الجودة: ${option.height}p ${tierName(option.height)}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showQualityMenu() {

        if (playingChannel == null) {
            return
        }

        if (playingWeb) {
            Toast.makeText(
                this,
                "الجودة غير متاحة لقنوات يوتيوب / فيسبوك",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val options = collectQualities()

        if (options.size < 2) {
            Toast.makeText(
                this,
                if (options.isEmpty()) "انتظر حتى يبدأ البث" else "هذه القناة بجودة واحدة فقط",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val manual = isManualQuality()

        val currentHeight = player.videoSize.height

        val items = mutableListOf<Pair<String, () -> Unit>>()

        items.add(
            ((if (!manual) "✓  " else "") + "تلقائي (AUTO)") to { setQuality(null) }
        )

        options.forEach { o ->

            val mbps =
                if (o.bitrate > 0) {
                    String.format(Locale.US, "  •  %.1f Mbps", o.bitrate / 1_000_000f)
                } else {
                    ""
                }

            val selected = manual && o.height == currentHeight

            items.add(
                ((if (selected) "✓  " else "") +
                        "${o.height}p  ${tierName(o.height)}$mbps") to { setQuality(o) }
            )
        }

        items.add("إلغاء" to {})

        showMenu("جودة البث", items)
    }

    // PlayerView controller only for videos (mp4): live channels use the glass list
    private fun applyControllerMode() {

        val vod = playingChannel?.let { isVodChannel(it) } == true

        playerView.useController = false
    }

    // =========================
    // Glass channel panel (fullscreen)
    // =========================

    private fun panelOpen(): Boolean =
        ::channelPanel.isInitialized && channelPanel.visibility == View.VISIBLE

    private val hidePanelRunnable = Runnable {

        if (menuOpen()) {
            startPanelTimer()
        } else {
            closeChannelPanel()
        }
    }

    private fun startPanelTimer() {

        mainHandler.removeCallbacks(hidePanelRunnable)

        mainHandler.postDelayed(hidePanelRunnable, 7000)
    }

    private fun buildChannelPanel() {

        channelPanel = LinearLayout(this)

        channelPanel.orientation = LinearLayout.VERTICAL

        channelPanel.setPadding(dp(8), dp(8), dp(8), dp(8))

        channelPanel.visibility = View.GONE

        // glass: very transparent so the picture stays visible
        channelPanel.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.argb(120, 20, 22, 58),
                    Color.argb(95, 8, 10, 28)
                )
            ).apply {
                cornerRadius = dp(26).toFloat()
                setStroke(dp(1), Color.argb(70, 255, 255, 255))
            }

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL

        header.gravity = Gravity.CENTER_VERTICAL

        header.setPadding(dp(8), 0, dp(8), 0)

        val titleCol = LinearLayout(this)

        titleCol.orientation = LinearLayout.VERTICAL

        titleCol.setPadding(dp(10), 0, 0, 0)

        titleCol.addView(label("القنوات", 18f, white, true), lp(matchParent, wrap))

        panelSub = label("", 11f, cyan)

        titleCol.addView(panelSub, lp(matchParent, wrap))

        header.addView(titleCol, lp(0, wrap, 1f))

        channelPanel.addView(header, lp(matchParent, dp(56)))

        panelScroll = createScroll()

        panelList = createList()

        panelScroll.addView(panelList)

        channelPanel.addView(panelScroll, lp(matchParent, 0, 1f))

        fullscreenContainer.addView(
            channelPanel,
            FrameLayout.LayoutParams(dp(340), matchParent, Gravity.RIGHT).apply {
                setMargins(0, dp(24), dp(24), dp(24))
            }
        )
    }

    private fun openChannelPanel() {

        val current = playingChannel ?: return

        if (panelOpen()) {
            return
        }

        closeQualityStrip(true)

        val list = if (playingList.isNotEmpty()) playingList else visibleChannels

        if (list.isEmpty()) {
            return
        }

        // hide the other overlays
        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        playerView.hideController()

        // the video must not steal the focus while the list is open
        playerView.isFocusable = false
        playerView.isFocusableInTouchMode = false
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false

        panelSub.text = realPackageName(current)

        panelList.removeAllViews()

        val pkg = packages.firstOrNull { it.id != FAV_ID && belongs(current, it) }

        val accent = pkg?.let { accentOfPackage(it) } ?: accentFor(0)

        var target: View? = null

        list.forEachIndexed { i, channel ->

            val row = createChannelRow(channel, i + 1, accent)

            row.setPlaying(channel == playingChannel)

            row.card.setOnClickListener {

                closeChannelPanel()

                if (channel != playingChannel) {
                    playChannel(channel)
                    showZapBanner(channel)
                }
            }

            panelList.addView(
                row.card,
                lp(matchParent, dp(60)).apply {
                    setMargins(0, dp(4), 0, dp(4))
                }
            )

            if (channel == playingChannel) {
                target = row.card
            }
        }

        channelPanel.animate().cancel()

        channelPanel.alpha = 0f

        channelPanel.translationX = dp(36).toFloat()

        channelPanel.visibility = View.VISIBLE

        channelPanel.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(200)
            .start()

        channelPanel.post {
            (target ?: panelList.getChildAt(0))?.requestFocus()
        }

        startPanelTimer()
    }

    private fun closeChannelPanel(immediate: Boolean = false) {

        mainHandler.removeCallbacks(hidePanelRunnable)

        if (!panelOpen()) {
            return
        }

        if (playingWeb) {
            webView.isFocusable = true
            webView.isFocusableInTouchMode = !phoneMode
        } else {
            playerView.isFocusable = true
            playerView.isFocusableInTouchMode = !phoneMode
        }

        channelPanel.animate().cancel()

        if (immediate) {

            channelPanel.visibility = View.GONE

        } else {

            channelPanel.animate()
                .alpha(0f)
                .translationX(dp(36).toFloat())
                .setDuration(180)
                .withEndAction {
                    channelPanel.visibility = View.GONE
                }
                .start()
        }

        if (isFullscreen) {
            if (playingWeb) {
                webView.requestFocus()
            } else {
                playerView.requestFocus()
            }
        }
    }

    private fun handlePanelKey(event: KeyEvent): Boolean {

        startPanelTimer()

        if (event.keyCode == KeyEvent.KEYCODE_BACK) {

            if (event.action == KeyEvent.ACTION_DOWN) {
                closeChannelPanel()
            }

            return true
        }

        if (
            event.keyCode == KeyEvent.KEYCODE_MENU ||
            event.keyCode == KeyEvent.KEYCODE_PROG_BLUE
        ) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                closeChannelPanel(true)
                openQualityStrip()
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    // =========================
    // Dynamic background (color of the focused logo)
    // =========================

    private fun dominantColor(bitmap: Bitmap): Int {

        val small = Bitmap.createScaledBitmap(bitmap, 24, 24, true)

        val hsv = FloatArray(3)

        val weight = FloatArray(12)
        val rs = FloatArray(12)
        val gs = FloatArray(12)
        val bs = FloatArray(12)

        for (y in 0 until 24) {

            for (x in 0 until 24) {

                val p = small.getPixel(x, y)

                if (Color.alpha(p) < 140) {
                    continue
                }

                Color.colorToHSV(p, hsv)

                // ignore white / gray / black pixels
                if (hsv[1] < 0.28f || hsv[2] < 0.25f) {
                    continue
                }

                val w = hsv[1] * hsv[2]

                val bucket = (hsv[0] / 30f).toInt().coerceIn(0, 11)

                weight[bucket] += w
                rs[bucket] += Color.red(p) * w
                gs[bucket] += Color.green(p) * w
                bs[bucket] += Color.blue(p) * w
            }
        }

        var best = -1

        var bestWeight = 0f

        for (i in 0 until 12) {
            if (weight[i] > bestWeight) {
                bestWeight = weight[i]
                best = i
            }
        }

        if (best < 0 || bestWeight < 1.5f) {
            return 0
        }

        return Color.rgb(
            (rs[best] / weight[best]).toInt(),
            (gs[best] / weight[best]).toInt(),
            (bs[best] / weight[best]).toInt()
        )
    }

    private fun requestTint(logo: String, fallback: Int) {

        if (!::ambient.isInitialized || !dynBgEnabled || isFullscreen) {
            return
        }

        tintToken++

        doTint(logo, fallback, tintToken, true)
    }

    private fun doTint(logo: String, fallback: Int, token: Int, retry: Boolean) {

        if (token != tintToken) {
            return
        }

        if (logo.isNotBlank()) {

            val cached = tintCache[logo]

            if (cached != null) {
                ambient.setTint(if (cached == 0) fallback else cached)
                return
            }

            val bitmap = imageCache.get(logo)

            if (bitmap != null) {

                val color = dominantColor(bitmap)

                tintCache[logo] = color

                ambient.setTint(if (color == 0) fallback else color)

                return
            }

            // the logo is still downloading: try once more shortly
            if (retry) {

                mainHandler.postDelayed(
                    { doTint(logo, fallback, token, false) },
                    700
                )

                return
            }
        }

        ambient.setTint(fallback)
    }

    // =========================
    // Auto preview (2 seconds on a channel)
    // =========================

    private val previewRunnable = Runnable {

        val channel = previewTarget ?: return@Runnable

        previewTarget = null

        if (isFullscreen || menuOpen() || moveState != null || splashVisible) {
            return@Runnable
        }

        // still resting on the same channel in the channels column
        if (lastFocusedChannel == channel && columnOf(currentFocus) == 1) {
            playChannel(channel, true)
        }
    }

    private fun cancelPreview() {

        mainHandler.removeCallbacks(previewRunnable)

        previewTarget = null
    }

    private fun schedulePreview(channel: Channel) {

        cancelPreview()

        if (
            !previewEnabled ||
            isFullscreen ||
            moveState != null ||
            menuOpen() ||
            channel.url.isBlank() ||
            channel == playingChannel
        ) {
            return
        }

        // do not auto-start heavy web players (YouTube / Facebook)
        if (isWebSource(parseSource(channel.url).url)) {
            return
        }

        previewTarget = channel

        mainHandler.postDelayed(previewRunnable, 2000)
    }

    // =========================
    // Settings menu
    // =========================

    private fun showSettingsMenu() {

        if (isFullscreen || moveState != null || menuOpen()) {
            return
        }

        val views = mutableListOf<View>()

        var first: View? = null

        fun section(title: String) {
            views.add(settingsSection(title))
        }

        fun row(
            icon: String,
            title: String,
            value: String,
            toggle: (() -> Boolean)?,
            initial: Boolean,
            action: (() -> Unit)?
        ) {

            val view = settingRow(icon, title, value, toggle, initial, action)

            views.add(view)

            if (first == null) {
                first = view
            }
        }

        section("المظهر")

        row("🎨", "الثيم", themes[themeIndex].name, null, false) { showThemeMenu() }

        row("✨", "الخلفية المتحركة", "", { toggleDynamicBackground(); dynBgEnabled }, dynBgEnabled, null)

        section("التشغيل")

        row("📶", "استقرار البث", bufferModeName(), null, false) { cycleBufferMode() }

        row("🖥", "التبديل التلقائي للسيرفر", "", { toggleAutoFallback(); autoFallback }, autoFallback, null)

        row("👁", "المعاينة التلقائية", "", { togglePreview(); previewEnabled }, previewEnabled, null)

        row("🔲", "المشاهدة المتعددة", "${multiList.size}/4", null, false) { showMultiMenu() }

        row("📐", "أبعاد الشاشة", aspectName(), null, false) { showAspectMenu() }

        row("⚡", "وضع الأداء", if (lightMode) "خفيف" else "عادي", null, false) { toggleLightMode() }

        section("التنقل")

        row("🔔", "أصوات التنقل", "", { toggleUiSound(); uiSound }, uiSound, null)

        section("الحساب والجهاز")

        if (requireCode || activeCode.isNotEmpty()) {
            row("🔑", "الاشتراك", subscriptionText(), null, false) {
                showCodeOverlay("أدخل الكود الجديد", false)
            }
        }

        row("🆔", "معرّف الجهاز", deviceId(), null, false) {
            Toast.makeText(this, "معرّف الجهاز: ${deviceId()}", Toast.LENGTH_LONG).show()
        }

        row("ℹ️", "الإصدار", versionLabel(), null, false) {
            Toast.makeText(this, "رقم الإصدار: ${versionLabel()}", Toast.LENGTH_LONG).show()
        }

        row("🔏", "بصمة التوقيع", signatureFingerprint(), null, false) {
            Toast.makeText(
                this,
                "إذا تغيرت هذه البصمة بين نسختين، فالتحديث لن يثبت فوق القديمة",
                Toast.LENGTH_LONG
            ).show()
        }

        if (!isTvDevice()) {
            row("📺", "مشاركة الشاشة", "", null, false) { openScreenShare() }
        }

        if (adminDevices.contains(deviceId())) {
            row("📊", "الإحصائيات", "", null, false) { showStatsScreen() }
        }

        val close = menuItem("إغلاق", accentFor(2))

        close.setOnClickListener { closeMenu() }

        close.layoutParams = lp(matchParent, dp(48)).apply { topMargin = dp(14) }

        views.add(close)

        showMenuViews("الإعدادات", views, first, 440)
    }

    private fun settingsSection(title: String): TextView {

        val header = label(title, 12.5f, cyan, true, Gravity.RIGHT)

        header.letterSpacing = 0.08f

        header.layoutParams = lp(matchParent, wrap).apply { setMargins(dp(8), dp(14), dp(8), dp(2)) }

        return header
    }

    // one line of the settings: icon | title | value or on/off switch
    private fun settingRow(
        icon: String,
        title: String,
        value: String,
        toggle: (() -> Boolean)?,
        initial: Boolean,
        action: (() -> Unit)?
    ): View {

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL

        row.gravity = Gravity.CENTER_VERTICAL

        row.setPadding(dp(14), 0, dp(14), 0)

        row.isFocusable = true

        row.isFocusableInTouchMode = !phoneMode

        row.isClickable = true

        row.layoutParams = lp(matchParent, dp(58)).apply { topMargin = dp(6) }

        // value / switch (left)
        val pill = TextView(this)

        pill.textSize = 12.5f

        pill.typeface = Typeface.DEFAULT_BOLD

        pill.gravity = Gravity.CENTER

        pill.setPadding(dp(12), dp(3), dp(12), dp(3))

        fun stylePill(on: Boolean) {

            if (toggle != null) {

                pill.text = if (on) "مفعّل" else "متوقف"

                pill.setTextColor(if (on) Color.rgb(8, 24, 20) else gray)

                pill.background =
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(if (on) Color.rgb(52, 211, 153) else Color.argb(40, 255, 255, 255))
                    }

            } else {

                pill.text = if (value.isBlank()) "›" else "$value  ›"

                pill.setTextColor(dimWhite)

                pill.background =
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(Color.argb(34, 255, 255, 255))
                    }
            }
        }

        stylePill(initial)

        row.addView(pill, lp(wrap, wrap))

        row.addView(
            label(title, 16f, white, true),
            lp(0, wrap, 1f).apply { setMargins(dp(12), 0, dp(12), 0) }
        )

        val iconView = TextView(this)

        iconView.text = icon

        iconView.textSize = 19f

        iconView.gravity = Gravity.CENTER

        iconView.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(36, 255, 255, 255))
            }

        row.addView(iconView, lp(dp(40), dp(40)))

        fun style(focused: Boolean) {

            row.background =
                if (focused) {
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(mix(cyan, bgMid, 0.5f), mix(violet, bgMid, 0.5f))
                    ).apply {
                        cornerRadius = dp(18).toFloat()
                        setStroke(dp(2), Color.WHITE)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(18).toFloat()
                        setColor(Color.argb(22, 255, 255, 255))
                        setStroke(dp(1), Color.argb(28, 255, 255, 255))
                    }
                }
        }

        style(false)

        row.setOnFocusChangeListener { _, focused -> style(focused) }

        row.setOnClickListener {

            if (toggle != null) {

                stylePill(toggle())

            } else {

                closeMenu()

                action?.invoke()
            }
        }

        return row
    }

    // like showMenu but with ready made rows
    private fun showMenuViews(title: String, views: List<View>, first: View?, widthDp: Int) {

        menuReturnFocus = currentFocus

        menuTitle.text = title

        menuList.removeAllViews()

        views.forEach { menuList.addView(it) }

        (menuBox.layoutParams as FrameLayout.LayoutParams).width = boxWidth(widthDp)

        menuBox.requestLayout()

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        menuOverlay.visibility = View.VISIBLE

        first?.requestFocus()
    }

    private fun showThemeMenu() {

        val items = mutableListOf<Pair<String, () -> Unit>>()

        themes.forEachIndexed { i, theme ->
            items.add(
                ((if (i == themeIndex) "✓  " else "") + theme.name) to { chooseTheme(i) }
            )
        }

        items.add("إلغاء" to {})

        showMenu("اختر الثيم", items)
    }

    // the whole interface is rebuilt with the new colors
    private fun chooseTheme(index: Int) {

        if (index == themeIndex) {
            return
        }

        prefs.edit()
            .putInt("theme", index)
            .putBoolean("skip_splash_once", true)
            .apply()

        recreate()
    }

    private fun toggleDynamicBackground() {

        dynBgEnabled = !dynBgEnabled

        prefs.edit().putBoolean("dyn_bg", dynBgEnabled).apply()

        if (dynBgEnabled) {

            val target = lastFocusedChannel ?: playingChannel

            if (target != null) {
                requestTint(target.logo, cyan)
            }

        } else {
            ambient.setTint(null)
        }

        Toast.makeText(
            this,
            if (dynBgEnabled) "الخلفية المتحركة: مفعّلة" else "الخلفية المتحركة: متوقفة",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun togglePreview() {

        previewEnabled = !previewEnabled

        prefs.edit().putBoolean("preview", previewEnabled).apply()

        if (!previewEnabled) {
            cancelPreview()
        }

        Toast.makeText(
            this,
            if (previewEnabled) "المعاينة التلقائية: مفعّلة" else "المعاينة التلقائية: متوقفة",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================
    // Remote control (Firebase settings)
    // =========================

    @Suppress("DEPRECATION")
    private fun currentVersionCode(): Long =
        try {
            val info = packageManager.getPackageInfo(packageName, 0)

            if (Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
        } catch (_: Exception) {
            0L
        }

    private fun applyRemoteSettings(snapshot: DataSnapshot?) {

        val st = snapshot ?: return

        val appEnabled = st.readBool("appEnabled", true)

        val maintenance = st.readBool("maintenance", false)

        val annEnabled = st.readBool("announcementEnabled", false)

        val annTitle = st.readString("announcementTitle") ?: ""

        val annMessage = st.readString("announcementMessage") ?: ""

        val latest = st.readInt("latestVersion", 0)

        val updateUrl = st.readString("updateUrl") ?: ""

        val updateNotes = st.readString("updateNotes") ?: ""

        val updateSha = st.readString("updateSha256") ?: ""

        val force =
            st.readBool("forceUpdate", false) ||
                    currentVersionCode() < st.readInt("minVersion", 0)

        requireCode = st.readBool("requireCode", false)

        st.readString("userAgent")?.let { defaultUserAgent = it }

        adminDevices =
            st.child("adminDevices").children
                .filter { it.value == true }
                .mapNotNull { it.key }
                .toSet()

        // 1) app disabled or under maintenance
        if (!appEnabled || maintenance) {

            showBlock(
                if (!appEnabled) "⛔" else "🛠",
                if (!appEnabled) "التطبيق متوقف حالياً" else "التطبيق تحت الصيانة",
                annMessage.ifBlank { "نعمل على تحسين الخدمة، يرجى المحاولة لاحقاً" }
            )

            return
        }

        hideBlock()

        // activation code required: nothing else until the code is valid
        updateGate()

        if (gated) {
            return
        }

        // a forced update comes before everything else
        if (force && latest > currentVersionCode() && updateUrl.isNotBlank()) {
            showUpdateNotice(latest, updateUrl, updateNotes, updateSha, true)
            return
        }

        // 2) announcement (shown once for every new message)
        if (annEnabled && (annTitle.isNotBlank() || annMessage.isNotBlank())) {

            val key = "ann|$annTitle|$annMessage"

            val seen = prefs.getString("notice_seen", "") == key

            if (!seen && noticeKey != key) {

                showNotice(
                    key,
                    "📢",
                    annTitle.ifBlank { "إعلان" },
                    annMessage,
                    listOf("حسناً" to { markNoticeSeen(key) })
                ) { markNoticeSeen(key) }
            }

            return
        }

        if (noticeOpen() && noticeKey.startsWith("ann|")) {
            hideNotice()
        }

        // 3) new version available (downloaded and installed inside the app)
        if (latest > currentVersionCode() && updateUrl.isNotBlank() && !updateShown) {

            updateShown = true

            showUpdateNotice(latest, updateUrl, updateNotes, updateSha, false)
        }
    }

    private fun markNoticeSeen(key: String) {
        prefs.edit().putString("notice_seen", key).apply()
    }

    private fun openUpdateUrl(url: String) {

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح رابط التحديث", Toast.LENGTH_LONG).show()
        }
    }

    private fun noticeOpen(): Boolean =
        ::noticeOverlay.isInitialized && noticeOverlay.visibility == View.VISIBLE

    private fun buildNoticeOverlay() {

        noticeOverlay = FrameLayout(this)

        noticeOverlay.setBackgroundColor(Color.argb(200, 4, 6, 18))

        noticeOverlay.isClickable = true

        noticeOverlay.visibility = View.GONE

        val box = LedFrame()

        noticeBox = box

        box.cornerDp = 28

        box.orientation = LinearLayout.VERTICAL

        box.gravity = Gravity.CENTER_HORIZONTAL

        box.setPadding(dp(28), dp(26), dp(28), dp(24))

        box.background = fancyCardBackground(28)

        noticeIcon = TextView(this)

        noticeIcon.textSize = 38f

        noticeIcon.gravity = Gravity.CENTER

        noticeIcon.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(40, 255, 255, 255))
                setStroke(dp(1), Color.argb(100, 255, 255, 255))
            }

        box.addView(noticeIcon, lp(dp(78), dp(78)))

        noticeTitle = label("", 22f, white, true, Gravity.CENTER, 2)

        box.addView(
            noticeTitle,
            lp(matchParent, wrap).apply { topMargin = dp(10) }
        )

        noticeMessage = label("", 15f, dimWhite, false, Gravity.CENTER, 9)

        box.addView(
            noticeMessage,
            lp(matchParent, wrap).apply { topMargin = dp(10) }
        )

        // download progress (in-app update)
        noticeProgressTrack = LinearLayout(this)

        noticeProgressTrack.orientation = LinearLayout.HORIZONTAL

        noticeProgressTrack.visibility = View.GONE

        noticeProgressTrack.background =
            GradientDrawable().apply {
                cornerRadius = dp(5).toFloat()
                setColor(Color.argb(40, 255, 255, 255))
            }

        noticeProgressFill = View(this)

        noticeProgressFill.background =
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(cyan, violet)
            ).apply {
                cornerRadius = dp(5).toFloat()
            }

        noticeProgressRest = View(this)

        noticeProgressTrack.addView(noticeProgressFill, LinearLayout.LayoutParams(0, matchParent, 0f))

        noticeProgressTrack.addView(noticeProgressRest, LinearLayout.LayoutParams(0, matchParent, 100f))

        box.addView(
            noticeProgressTrack,
            lp(matchParent, dp(10)).apply { topMargin = dp(18) }
        )

        noticeButtons = LinearLayout(this)

        noticeButtons.orientation = LinearLayout.HORIZONTAL

        noticeButtons.gravity = Gravity.CENTER

        box.addView(
            noticeButtons,
            lp(matchParent, wrap).apply { topMargin = dp(20) }
        )

        noticeOverlay.addView(
            box,
            FrameLayout.LayoutParams(boxWidth(520), wrap, Gravity.CENTER)
        )

        root.addView(noticeOverlay, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private fun showNotice(
        key: String,
        icon: String,
        title: String,
        message: String,
        buttons: List<Pair<String, () -> Unit>>,
        cancelable: Boolean = true,
        keepOpenFirst: Boolean = false,
        onDismiss: () -> Unit
    ) {

        if (menuOpen()) {
            closeMenu()
        }

        noticeCancelable = cancelable

        when {
            key.startsWith("upd|") ->
                noticeBox.setColors(Color.rgb(255, 196, 61), Color.rgb(255, 120, 60))
            key.startsWith("ann|") ->
                noticeBox.setColors(cyan, violet)
            else ->
                noticeBox.setColors(Color.rgb(52, 211, 153), cyan)
        }

        noticeProgressTrack.visibility = View.GONE

        noticeButtons.visibility = View.VISIBLE

        noticeKey = key

        noticeDismiss = onDismiss

        noticeReturnFocus = currentFocus

        noticeIcon.text = icon

        noticeTitle.text = title

        noticeMessage.text = message

        noticeMessage.visibility = if (message.isBlank()) View.GONE else View.VISIBLE

        noticeButtons.removeAllViews()

        var first: View? = null

        buttons.forEachIndexed { i, button ->

            val item = menuItem(button.first, accentFor(i))

            item.setOnClickListener {

                if (keepOpenFirst && i == 0) {
                    button.second()
                } else {
                    hideNotice()
                    button.second()
                }
            }

            val params =
                if (buttons.size == 1) {
                    lp(dp(220), dp(48))
                } else {
                    lp(0, dp(48), 1f)
                }

            params.setMargins(dp(6), 0, dp(6), 0)

            noticeButtons.addView(item, params)

            if (first == null) {
                first = item
            }
        }

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        noticeOverlay.visibility = View.VISIBLE

        first?.requestFocus()
    }

    private fun hideNotice() {

        if (!noticeOpen()) {
            return
        }

        noticeOverlay.visibility = View.GONE

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        noticeReturnFocus?.requestFocus()

        noticeReturnFocus = null
    }

    private fun buildBlockOverlay() {

        blockOverlay = FrameLayout(this)

        blockOverlay.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgTop, bgMid, bgBottom)
            )

        blockOverlay.isClickable = true

        blockOverlay.visibility = View.GONE

        val col = LedFrame()

        col.cornerDp = 30

        col.setColors(Color.rgb(255, 59, 92), Color.rgb(255, 159, 67))

        col.orientation = LinearLayout.VERTICAL

        col.gravity = Gravity.CENTER_HORIZONTAL

        col.setPadding(dp(30), dp(28), dp(30), dp(28))

        col.background = fancyCardBackground(30)

        blockIcon = TextView(this)

        blockIcon.textSize = 64f

        blockIcon.gravity = Gravity.CENTER

        col.addView(blockIcon, lp(wrap, wrap))

        blockTitle = label("", 28f, white, true, Gravity.CENTER, 2)

        col.addView(
            blockTitle,
            lp(matchParent, wrap).apply { topMargin = dp(16) }
        )

        blockMessage = label("", 16f, gray, false, Gravity.CENTER, 6)

        col.addView(
            blockMessage,
            lp(matchParent, wrap).apply { topMargin = dp(10) }
        )

        blockOverlay.addView(
            col,
            FrameLayout.LayoutParams(boxWidth(560), wrap, Gravity.CENTER)
        )

        root.addView(blockOverlay, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private fun showBlock(icon: String, title: String, message: String) {

        if (!blocked) {

            blocked = true

            stopExternalAudio()

            if (multiActive) {
                exitMultiView(false)
            }

            // stop everything that is playing
            try {
                player.stop()
                player.clearMediaItems()
            } catch (_: Exception) {
            }

            mainHandler.removeCallbacks(retryRunnable)

            stopWeb()
        }

        blockIcon.text = icon

        blockTitle.text = title

        blockMessage.text = message

        if (noticeOpen()) {
            hideNotice()
        }

        if (menuOpen()) {
            closeMenu()
        }

        blockOverlay.visibility = View.VISIBLE
    }

    private fun hideBlock() {

        if (!blocked) {
            return
        }

        blocked = false

        blockOverlay.visibility = View.GONE

        playingChannel?.let { playChannel(it) }
    }

    // =========================
    // Today's matches ticker
    // =========================

    private val tickerRunnable = object : Runnable {
        override fun run() {
            showNextMatch()
            mainHandler.postDelayed(this, 4500)
        }
    }

    private fun buildTicker(): View {

        // the box always takes the free space of the header
        val box = FrameLayout(this)

        val chip = LedFrame()

        chip.orientation = LinearLayout.VERTICAL

        chip.gravity = Gravity.CENTER

        chip.setPadding(dp(24), dp(7), dp(24), dp(7))

        chip.alpha = 0f

        chip.visibility = View.GONE

        chip.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(215, 24, 28, 70), Color.argb(240, 8, 10, 28))
            ).apply {
                cornerRadius = dp(22).toFloat()
            }

        tickerChip = chip

        // tap (or OK with the remote) = open the channel of this match
        chip.isClickable = true

        chip.isFocusable = true

        chip.isFocusableInTouchMode = !phoneMode

        chip.setOnClickListener {

            chip.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(80)
                .withEndAction {
                    chip.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                .start()

            tickerMatch?.let { openMatchChannel(it) }
        }

        chip.setOnFocusChangeListener { _, focused ->
            val scale = if (focused) 1.06f else 1f
            chip.animate().scaleX(scale).scaleY(scale).setDuration(130).start()
        }

        // left -> right: away team | time or score | home team (Arabic reading order)
        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL

        row.gravity = Gravity.CENTER_VERTICAL

        tickerAwayLogo = LogoBadge(30, accentFor(2))

        tickerHomeLogo = LogoBadge(30, accentFor(0))

        tickerAway = label("", 17f, white, true, Gravity.CENTER)

        tickerHome = label("", 17f, white, true, Gravity.CENTER)

        tickerMid = label("", 17f, Color.rgb(8, 14, 30), true, Gravity.CENTER)

        tickerMid.setPadding(dp(14), dp(2), dp(14), dp(2))

        row.addView(tickerAwayLogo, lp(dp(30), dp(30)).apply { setMargins(0, 0, dp(8), 0) })

        row.addView(tickerAway, lp(wrap, wrap))

        row.addView(tickerMid, lp(wrap, wrap).apply { setMargins(dp(14), 0, dp(14), 0) })

        row.addView(tickerHome, lp(wrap, wrap))

        row.addView(tickerHomeLogo, lp(dp(30), dp(30)).apply { setMargins(dp(8), 0, 0, 0) })

        chip.addView(row, lp(wrap, wrap))

        tickerLine2 = label("", 10.5f, gray, false, Gravity.CENTER)

        chip.addView(tickerLine2, lp(wrap, wrap).apply { topMargin = dp(1) })

        box.addView(
            chip,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.CENTER)
        )

        return box
    }

    private fun parseMatches(snapshot: DataSnapshot?): List<MatchItem> {

        val result = mutableListOf<MatchItem>()

        for (child in snapshot?.children ?: emptyList()) {

            if (!child.readBool("enabled", true)) {
                continue
            }

            val home = child.readString("home") ?: continue

            val away = child.readString("away") ?: continue

            result.add(
                MatchItem(
                    home = home,
                    away = away,
                    league = child.readString("league") ?: "",
                    time = child.readString("time") ?: "",
                    date = child.readString("date") ?: "",
                    score = child.readString("score") ?: "",
                    status = child.readString("status") ?: "",
                    channel = child.readString("channel") ?: "",
                    order = child.readInt("order", 999),
                    homeLogo = child.readString("homeLogo") ?: "",
                    awayLogo = child.readString("awayLogo") ?: ""
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun updateMatches(snapshot: DataSnapshot?) {

        val newMatches = parseMatches(snapshot)

        if (newMatches != matches) {

            matches = newMatches

            tickerIndex = -1

            showNextMatch()

            refreshMatchViews()
        }
    }

    // only today's matches (a match without a date counts as today)
    private fun todayMatches(): List<MatchItem> {

        val today = dayFormat.format(Date())

        return matches.filter { it.date.isBlank() || it.date == today }
    }

    private class MatchView(val mid: String, val line2: String, val live: Boolean)

    private fun matchView(m: MatchItem): MatchView {

        val status = m.status.trim().lowercase(Locale.US)

        val live = status == "live" || status == "مباشر"

        val finished = status == "ft" || status == "finished" || status == "end" || status == "انتهت"

        val middle =
            if (live || finished) {
                m.score.ifBlank { "vs" }
            } else {
                m.time.ifBlank { "vs" }
            }

        val parts = mutableListOf<String>()

        if (live) {
            parts.add("🔴 مباشر")
        } else if (finished) {
            parts.add("انتهت")
        }

        if (m.league.isNotBlank()) {
            parts.add(m.league)
        }

        if (m.channel.isNotBlank()) {
            parts.add("📺 ${m.channel}")
        }

        return MatchView(middle, parts.joinToString("  •  "), live)
    }

    private fun showNextMatch() {

        val list = todayMatches()

        if (list.isEmpty()) {
            tickerChip.animate().cancel()
            tickerChip.visibility = View.GONE
            return
        }

        tickerChip.visibility = View.VISIBLE

        tickerIndex = (tickerIndex + 1) % list.size

        val match = list[tickerIndex]

        val view = matchView(match)

        tickerChip.animate().cancel()

        tickerChip.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {

                tickerMatch = match

                tickerHome.text = match.home

                tickerAway.text = match.away

                tickerMid.text = view.mid

                tickerMid.setTextColor(if (view.live) Color.WHITE else Color.rgb(8, 14, 30))

                tickerMid.background =
                    GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(if (view.live) Color.rgb(255, 59, 92) else cyan)
                    }

                // team crests only when a logo link exists (homeLogo / awayLogo)
                tickerHomeLogo.visibility =
                    if (match.homeLogo.isNotBlank()) View.VISIBLE else View.GONE

                tickerAwayLogo.visibility =
                    if (match.awayLogo.isNotBlank()) View.VISIBLE else View.GONE

                if (match.homeLogo.isNotBlank()) {
                    tickerHomeLogo.bind(match.home, match.homeLogo)
                }

                if (match.awayLogo.isNotBlank()) {
                    tickerAwayLogo.bind(match.away, match.awayLogo)
                }

                tickerLine2.text = view.line2

                tickerLine2.visibility =
                    if (view.line2.isBlank()) View.GONE else View.VISIBLE

                // live match: red / orange light, otherwise the theme colors
                if (view.live) {
                    tickerChip.setColors(Color.rgb(255, 59, 92), Color.rgb(255, 159, 67))
                } else {
                    tickerChip.setColors(cyan, violet)
                }

                tickerChip.animate().alpha(1f).setDuration(260).start()
            }
            .start()
    }

    // Rounded card with two glowing "LED" lights that run around the border
    private open inner class LedFrame : LinearLayout(this@MainActivity) {

        private val rect = RectF()
        private val path = Path()
        private val segment = Path()
        private val measure = PathMeasure()
        private val pos = FloatArray(2)

        private var length = 0f
        private var progress = 0f
        private var anim: ValueAnimator? = null
        private var ledOn = true
        private var attached = false

        private var colorA = Color.CYAN
        private var colorB = Color.MAGENTA

        var cornerDp = 22
        var drawBase = true

        private val basePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = dp(1).toFloat()
                color = Color.argb(70, 255, 255, 255)
            }

        private val glowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = dp(8).toFloat()
            }

        private val corePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = dp(3).toFloat()
            }

        private val headPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }

        init {
            setWillNotDraw(false)
        }

        fun setColors(a: Int, b: Int) {
            colorA = a
            colorB = b
            invalidate()
        }

        // the light only runs while it is on (saves the CPU of the idle cards)
        fun setLed(on: Boolean) {

            if (ledOn == on) {
                return
            }

            ledOn = on

            if (attached) {
                if (on && isShown) startAnim() else stopAnim()
            }

            invalidate()
        }

        private fun startAnim() {

            if (anim != null || lightMode) {
                return
            }

            val a = ValueAnimator.ofFloat(0f, 1f)

            a.duration = 3000

            a.repeatCount = ValueAnimator.INFINITE

            a.interpolator = android.view.animation.LinearInterpolator()

            a.addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }

            anim = a

            a.start()
        }

        private fun stopAnim() {
            anim?.cancel()
            anim = null
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

            super.onSizeChanged(w, h, oldw, oldh)

            val inset = dp(2).toFloat()

            rect.set(inset, inset, w - inset, h - inset)

            val radius = minOf(h / 2f, dp(cornerDp).toFloat())

            path.reset()

            path.addRoundRect(rect, radius, radius, Path.Direction.CW)

            measure.setPath(path, false)

            length = measure.length
        }

        override fun onAttachedToWindow() {

            super.onAttachedToWindow()

            attached = true

            if (ledOn && isShown) {
                startAnim()
            }
        }

        override fun onDetachedFromWindow() {

            attached = false

            stopAnim()

            super.onDetachedFromWindow()
        }

        override fun onVisibilityChanged(changedView: View, visibility: Int) {

            super.onVisibilityChanged(changedView, visibility)

            if (attached && ledOn) {
                if (isShown) startAnim() else stopAnim()
            }
        }

        private fun drawRange(canvas: Canvas, from: Float, to: Float, paint: Paint) {

            var a = from

            var b = to

            if (a < 0f) {
                a += length
                b += length
            }

            if (b > length) {
                part(canvas, a, length, paint)
                part(canvas, 0f, b - length, paint)
            } else {
                part(canvas, a, b, paint)
            }
        }

        private fun part(canvas: Canvas, from: Float, to: Float, paint: Paint) {

            if (to <= from) {
                return
            }

            segment.reset()

            measure.getSegment(from, to, segment, true)

            canvas.drawPath(segment, paint)
        }

        // a bright head with a fading tail
        private fun comet(canvas: Canvas, fraction: Float, color: Int) {

            val head = fraction * length

            val tail = length * 0.30f

            val steps = 10

            for (i in 0 until steps) {

                val fade = 1f - i / steps.toFloat()

                val alpha = fade * fade

                val end = head - tail * i / steps

                val start = head - tail * (i + 1) / steps

                glowPaint.color = withAlpha(color, (80 * alpha).toInt())

                drawRange(canvas, start, end, glowPaint)

                corePaint.color = withAlpha(color, (255 * alpha).toInt())

                drawRange(canvas, start, end, corePaint)
            }

            measure.getPosTan(head, pos, null)

            canvas.drawCircle(pos[0], pos[1], dp(2).toFloat() + 0.5f, headPaint)
        }

        override fun onDraw(canvas: Canvas) {

            super.onDraw(canvas)

            if (!ledOn || length <= 0f) {
                return
            }

            if (drawBase) {
                canvas.drawPath(path, basePaint)
            }

            // light mode: a still glowing border instead of the running lights
            if (lightMode) {
                corePaint.color = withAlpha(colorA, 170)
                canvas.drawPath(path, corePaint)
                return
            }

            comet(canvas, progress, colorA)

            comet(canvas, (progress + 0.5f) % 1f, colorB)
        }
    }

    // Animated sound bars for the channel that is playing
    private inner class EqualizerView(tint: Int) : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
        private val bar = RectF()
        private var t = 0f
        private var anim: ValueAnimator? = null

        fun setTint(color: Int) {
            paint.color = color
            invalidate()
        }

        private fun refreshAnim() {

            if (!lightMode && isShown && isAttachedToWindow) {

                if (anim == null) {

                    val a = ValueAnimator.ofFloat(0f, 6.2832f)

                    a.duration = 850

                    a.repeatCount = ValueAnimator.INFINITE

                    a.interpolator = android.view.animation.LinearInterpolator()

                    a.addUpdateListener {
                        t = it.animatedValue as Float
                        invalidate()
                    }

                    anim = a

                    a.start()
                }

            } else {
                anim?.cancel()
                anim = null
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            refreshAnim()
        }

        override fun onDetachedFromWindow() {
            anim?.cancel()
            anim = null
            super.onDetachedFromWindow()
        }

        override fun onVisibilityChanged(changedView: View, visibility: Int) {
            super.onVisibilityChanged(changedView, visibility)
            refreshAnim()
        }

        override fun onDraw(canvas: Canvas) {

            val w = width.toFloat()

            val h = height.toFloat()

            val count = 4

            val gap = w * 0.12f

            val barW = (w - gap * (count - 1)) / count

            for (i in 0 until count) {

                val level = 0.3f + 0.7f * (0.5f + 0.5f * sin(t + i * 1.7f))

                val barH = h * level

                val left = i * (barW + gap)

                bar.set(left, h - barH, left + barW, h)

                canvas.drawRoundRect(bar, barW / 2, barW / 2, paint)
            }
        }
    }

    // Soft glow around the player in the color of the channel logo
    private inner class GlowFrame : FrameLayout(this@MainActivity) {

        private val rect = RectF()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private var glow = 0
        private var breath = 1f
        private var breathAnim: ValueAnimator? = null
        private var colorAnim: ValueAnimator? = null

        init {
            setWillNotDraw(false)
        }

        fun setGlow(target: Int) {

            colorAnim?.cancel()

            val from = if (glow == 0) target else glow

            val evaluator = ArgbEvaluator()

            val a = ValueAnimator.ofFloat(0f, 1f)

            a.duration = 500

            a.addUpdateListener {
                glow = evaluator.evaluate(it.animatedFraction, from, target) as Int
                invalidate()
            }

            colorAnim = a

            a.start()
        }

        override fun onAttachedToWindow() {

            super.onAttachedToWindow()

            if (lightMode) {
                breath = 1f
                return
            }

            val a = ValueAnimator.ofFloat(0.6f, 1f)

            a.duration = 1800

            a.repeatMode = ValueAnimator.REVERSE

            a.repeatCount = ValueAnimator.INFINITE

            a.addUpdateListener {
                breath = it.animatedValue as Float
                invalidate()
            }

            breathAnim = a

            a.start()
        }

        override fun onDetachedFromWindow() {

            breathAnim?.cancel()

            colorAnim?.cancel()

            super.onDetachedFromWindow()
        }

        override fun dispatchDraw(canvas: Canvas) {

            super.dispatchDraw(canvas)

            if (glow == 0) {
                return
            }

            val radius = dp(10).toFloat()

            val widths = floatArrayOf(dp(18).toFloat(), dp(12).toFloat(), dp(7).toFloat(), dp(3).toFloat())

            val alphas = intArrayOf(16, 30, 58, 130)

            for (i in widths.indices) {

                val w = widths[i]

                paint.strokeWidth = w

                paint.color = withAlpha(glow, (alphas[i] * breath).toInt())

                rect.set(w / 2, w / 2, width - w / 2, height - w / 2)

                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }
    }

    // A light band passes over the app logo every few seconds
    private inner class ShineLayout : FrameLayout(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val clip = Path()
        private var p = 0f
        private var anim: ValueAnimator? = null

        init {
            setWillNotDraw(false)
        }

        override fun onAttachedToWindow() {

            super.onAttachedToWindow()

            if (lightMode) {
                return
            }

            val a = ValueAnimator.ofFloat(0f, 1f)

            a.duration = 5200

            a.repeatCount = ValueAnimator.INFINITE

            a.interpolator = android.view.animation.LinearInterpolator()

            a.addUpdateListener {
                p = it.animatedValue as Float
                if (p < 0.32f) {
                    invalidate()
                }
            }

            anim = a

            a.start()
        }

        override fun onDetachedFromWindow() {
            anim?.cancel()
            super.onDetachedFromWindow()
        }

        override fun dispatchDraw(canvas: Canvas) {

            super.dispatchDraw(canvas)

            if (p >= 0.28f) {
                return
            }

            val t = p / 0.28f

            val w = width.toFloat()

            val h = height.toFloat()

            val bandW = w * 0.55f

            val x = -bandW + t * (w + bandW * 2)

            clip.reset()

            clip.addRoundRect(0f, 0f, w, h, dp(14).toFloat(), dp(14).toFloat(), Path.Direction.CW)

            canvas.save()

            canvas.clipPath(clip)

            paint.shader =
                LinearGradient(
                    x,
                    0f,
                    x + bandW,
                    0f,
                    intArrayOf(
                        Color.argb(0, 255, 255, 255),
                        Color.argb(150, 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )

            canvas.skew(-0.3f, 0f)

            canvas.drawRect(0f, 0f, w + h, h, paint)

            canvas.restore()
        }
    }

    // glow color of the player = dominant color of the channel logo
    private fun requestPlayerGlow(logo: String) {

        if (!::playerGlow.isInitialized) {
            return
        }

        glowToken++

        doGlow(logo, glowToken, true)
    }

    private fun doGlow(logo: String, token: Int, retry: Boolean) {

        if (token != glowToken) {
            return
        }

        if (logo.isNotBlank()) {

            val cached = tintCache[logo]

            if (cached != null) {
                playerGlow.setGlow(if (cached == 0) cyan else cached)
                return
            }

            val bitmap = imageCache.get(logo)

            if (bitmap != null) {

                val color = dominantColor(bitmap)

                tintCache[logo] = color

                playerGlow.setGlow(if (color == 0) cyan else color)

                return
            }

            if (retry) {
                mainHandler.postDelayed({ doGlow(logo, token, false) }, 800)
                return
            }
        }

        playerGlow.setGlow(cyan)
    }

    // =========================
    // Matches linked to channels
    // =========================

    private fun normName(text: String): String =
        text.lowercase(Locale.US).replace(" ", "").replace("-", "")

    private fun isLiveMatch(m: MatchItem): Boolean {

        val status = m.status.trim().lowercase(Locale.US)

        return status == "live" || status == "مباشر"
    }

    private fun isFinishedMatch(m: MatchItem): Boolean {

        val status = m.status.trim().lowercase(Locale.US)

        return status == "ft" || status == "finished" || status == "end" || status == "انتهت"
    }

    // today's match that is shown on this channel (live ones first)
    private fun matchForChannel(channel: Channel): MatchItem? {

        val name = normName(channel.name)

        val found =
            todayMatches().filter { m ->
                val c = normName(m.channel)
                c.length >= 4 && (c == name || name.contains(c) || c.contains(name))
            }

        return found.firstOrNull { isLiveMatch(it) } ?: found.firstOrNull()
    }

    private fun matchSubText(m: MatchItem): String =
        when {
            isLiveMatch(m) -> "🔴 ${m.home}  ${m.score.ifBlank { "vs" }}  ${m.away}"
            isFinishedMatch(m) -> "${m.home}  ${m.score.ifBlank { "vs" }}  ${m.away}  •  انتهت"
            else -> "⏰ ${m.time.ifBlank { "--:--" }}  ${m.home} × ${m.away}"
        }

    private fun nowSubText(channel: Channel): String {

        if (channel.isVod) {

            val episode = vodPlay?.episode

            return if (episode != null) {
                "الموسم ${episode.season} • الحلقة ${episode.number}"
            } else {
                "فيلم"
            }
        }

        val base = realPackageName(channel)

        val m = matchForChannel(channel)

        if (m == null) {

            val guide = epgLine(channel)

            return if (guide != null) "$guide  •  $base" else base
        }

        val middle =
            if (isLiveMatch(m) || isFinishedMatch(m)) {
                m.score.ifBlank { "vs" }
            } else {
                m.time.ifBlank { "vs" }
            }

        return "${m.home}  $middle  ${m.away}  •  $base"
    }

    private fun refreshMatchViews() {

        channelRows.forEach { it.setPlaying(it.channel == playingChannel) }

        playingChannel?.let { nowSub.text = nowSubText(it) }
    }

    // =========================
    // Hint bar with keycaps
    // =========================

    private fun buildHintBar(): View {

        val bar = LinearLayout(this)

        bar.orientation = LinearLayout.HORIZONTAL

        bar.gravity = Gravity.CENTER

        fun add(key: String, text: String) {

            val cap = TextView(this)

            cap.text = key

            cap.textSize = 10.5f

            cap.typeface = Typeface.DEFAULT_BOLD

            cap.setTextColor(white)

            cap.gravity = Gravity.CENTER

            cap.setPadding(dp(8), dp(1), dp(8), dp(1))

            cap.background =
                GradientDrawable().apply {
                    cornerRadius = dp(7).toFloat()
                    setColor(Color.argb(26, 255, 255, 255))
                    setStroke(dp(1), Color.argb(120, 255, 255, 255))
                }

            bar.addView(cap, lp(wrap, wrap))

            bar.addView(
                label(text, 11f, gray, false, Gravity.CENTER),
                lp(wrap, wrap).apply { setMargins(dp(6), 0, dp(16), 0) }
            )
        }

        add("OK", "ملء الشاشة")

        add("OK مطولاً", "خيارات")

        add("0-9", "رقم القناة")

        return bar
    }

    // =========================
    // Skeleton loading
    // =========================

    // A view that still runs an infinite animation is NOT removed visually by
    // removeAllViews() (it stays drawn as a "disappearing child"), so stop it first.
    private fun clearList(list: LinearLayout) {

        for (i in 0 until list.childCount) {
            list.getChildAt(i).clearAnimation()
        }

        list.removeAllViews()
    }

    private fun showSkeleton(list: LinearLayout, rows: Int) {

        clearList(list)

        for (i in 0 until rows) {

            val row = LinearLayout(this)

            row.orientation = LinearLayout.HORIZONTAL

            row.gravity = Gravity.CENTER_VERTICAL

            row.setPadding(dp(14), 0, dp(14), 0)

            row.background =
                GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(Color.argb(20, 255, 255, 255))
                    setStroke(dp(1), Color.argb(26, 255, 255, 255))
                }

            val bars = LinearLayout(this)

            bars.orientation = LinearLayout.VERTICAL

            bars.gravity = Gravity.RIGHT

            bars.addView(skeletonBar(), lp(dp(130), dp(12)))

            bars.addView(
                skeletonBar(),
                lp(dp(76), dp(9)).apply { topMargin = dp(8) }
            )

            row.addView(bars, lp(0, wrap, 1f))

            val logo = View(this)

            logo.background =
                GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.argb(40, 255, 255, 255))
                }

            row.addView(
                logo,
                lp(dp(44), dp(44)).apply { setMargins(dp(10), 0, 0, 0) }
            )

            row.startAnimation(
                AlphaAnimation(1f, 0.35f).apply {
                    duration = 900
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                    startOffset = i * 120L
                }
            )

            list.addView(
                row,
                lp(matchParent, dp(66)).apply { setMargins(0, dp(5), 0, dp(5)) }
            )
        }
    }

    private fun skeletonBar(): View {

        val bar = View(this)

        bar.background =
            GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.argb(46, 255, 255, 255))
            }

        return bar
    }

    // =========================
    // Sound / haptic feedback
    // =========================

    private fun playNavFeedback(view: View) {

        if (!uiSound) {
            return
        }

        // only when the user just pressed a D-pad key (not on re-render)
        if (SystemClock.uptimeMillis() - lastNavKeyAt > 300) {
            return
        }

        try {
            view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN)
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
        }
    }

    private fun toggleUiSound() {

        uiSound = !uiSound

        prefs.edit().putBoolean("ui_sound", uiSound).apply()

        Toast.makeText(
            this,
            if (uiSound) "أصوات التنقل: مفعّلة" else "أصوات التنقل: متوقفة",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================
    // NEW badge
    // =========================

    // a channel is "new" when new=true or it was added (addedAt) in the last 7 days
    private fun isNewChannel(child: DataSnapshot): Boolean {

        if (child.readBool("new", false)) {
            return true
        }

        val added = child.readString("addedAt") ?: return false

        val millis: Long =
            added.toLongOrNull()?.let { if (it < 100000000000L) it * 1000 else it }
                ?: (try {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(added)?.time
                } catch (_: Exception) {
                    null
                })
                ?: return false

        val age = System.currentTimeMillis() - millis

        return age in 0..(7L * 24 * 60 * 60 * 1000)
    }

    // =========================
    // Number keys / previous channel
    // =========================

    private val numberRunnable = Runnable {
        commitNumber()
    }

    private fun buildNumberOverlay() {

        numberOverlay = TextView(this)

        numberOverlay.textSize = 40f

        numberOverlay.setTextColor(white)

        numberOverlay.typeface = Typeface.DEFAULT_BOLD

        numberOverlay.gravity = Gravity.CENTER

        numberOverlay.setPadding(dp(24), dp(8), dp(24), dp(8))

        numberOverlay.visibility = View.GONE

        numberOverlay.background =
            GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb(190, 8, 10, 28))
                setStroke(dp(2), withAlpha(cyan, 160))
            }

        root.addView(
            numberOverlay,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.RIGHT).apply {
                setMargins(0, dp(30), dp(40), 0)
            }
        )
    }

    private fun numberTargetList(): List<Channel> =
        if (isFullscreen && playingList.isNotEmpty()) playingList else visibleChannels

    private fun handleSpecialKeys(event: KeyEvent): Boolean {

        val code = event.keyCode

        val digit =
            when (code) {
                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> code - KeyEvent.KEYCODE_0
                in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 ->
                    code - KeyEvent.KEYCODE_NUMPAD_0
                else -> -1
            }

        if (digit >= 0) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                enterDigit(digit)
            }

            return true
        }

        if (code == KeyEvent.KEYCODE_TV_ZOOM_MODE) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                cycleAspect()
            }

            return true
        }

        if (code == KeyEvent.KEYCODE_PROG_RED) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {

                if (isFullscreen) {
                    closeChannelPanel(true)
                    openServerStrip()
                } else {
                    showServerMenu()
                }
            }

            return true
        }

        if (
            code == KeyEvent.KEYCODE_LAST_CHANNEL ||
            code == KeyEvent.KEYCODE_PROG_GREEN
        ) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                goPreviousChannel()
            }

            return true
        }

        return false
    }

    private fun enterDigit(digit: Int) {

        if (blocked || splashVisible) {
            return
        }

        if (numberBuffer.length >= 3) {
            numberBuffer.setLength(0)
        }

        numberBuffer.append(digit)

        numberOverlay.text = numberBuffer.toString()

        numberOverlay.visibility = View.VISIBLE

        mainHandler.removeCallbacks(numberRunnable)

        val value = numberBuffer.toString().toInt()

        // the number can not get longer (e.g. "5" when there are only 18 channels)
        if (value * 10 > numberTargetList().size) {
            commitNumber()
        } else {
            mainHandler.postDelayed(numberRunnable, 1500)
        }
    }

    private fun commitNumber() {

        mainHandler.removeCallbacks(numberRunnable)

        val text = numberBuffer.toString()

        numberBuffer.setLength(0)

        numberOverlay.visibility = View.GONE

        val n = text.toIntOrNull() ?: return

        val target = numberTargetList().getOrNull(n - 1)

        if (target == null || sourcesOf(target).isEmpty()) {
            Toast.makeText(this, "لا توجد قناة بالرقم $n", Toast.LENGTH_SHORT).show()
            return
        }

        playChannel(target)

        if (isFullscreen) {
            showZapBanner(target)
        } else {
            channelRows.firstOrNull { it.channel == target }?.card?.requestFocus()
        }
    }

    private fun goPreviousChannel() {

        val prev = previousChannel

        if (prev == null) {
            Toast.makeText(this, "لا توجد قناة سابقة", Toast.LENGTH_SHORT).show()
            return
        }

        playChannel(prev)

        if (isFullscreen) {
            showZapBanner(prev)
        } else {
            channelRows.firstOrNull { it.channel == prev }?.card?.requestFocus()
        }
    }

    // =========================
    // Connection watcher
    // =========================

    private val offlineRunnable = Runnable {

        online = false

        setBannerStyle(false)

        offlineBanner.text = "⚠  لا يوجد اتصال بالإنترنت  •  سيعود البث تلقائياً"

        mainHandler.removeCallbacks(hideBannerRunnable)

        offlineBanner.visibility = View.VISIBLE
    }

    private val hideBannerRunnable = Runnable {
        offlineBanner.visibility = View.GONE
    }

    private fun setBannerStyle(ok: Boolean) {

        offlineBanner.background =
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                if (ok) {
                    intArrayOf(Color.rgb(16, 150, 100), Color.rgb(6, 120, 120))
                } else {
                    intArrayOf(Color.rgb(220, 60, 70), Color.rgb(200, 90, 40))
                }
            ).apply {
                cornerRadius = dp(22).toFloat()
            }
    }

    private fun buildOfflineBanner() {

        offlineBanner = TextView(this)

        offlineBanner.textSize = 15f

        offlineBanner.setTextColor(white)

        offlineBanner.typeface = Typeface.DEFAULT_BOLD

        offlineBanner.gravity = Gravity.CENTER

        offlineBanner.setPadding(dp(24), dp(8), dp(24), dp(8))

        offlineBanner.visibility = View.GONE

        setBannerStyle(false)

        root.addView(
            offlineBanner,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(8)
            }
        )
    }

    // needs android.permission.ACCESS_NETWORK_STATE in the manifest
    private fun registerNetworkWatcher() {

        try {

            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

            val callback =
                object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(network: Network) {
                        mainHandler.post { onNetworkChanged(true) }
                    }

                    override fun onLost(network: Network) {
                        mainHandler.post { onNetworkChanged(false) }
                    }
                }

            cm.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                callback
            )

            netCallback = callback

        } catch (_: Exception) {
        }
    }

    private fun onNetworkChanged(available: Boolean) {

        if (!::offlineBanner.isInitialized) {
            return
        }

        if (!available) {

            // wait a moment: the network may only be switching (wifi <-> ethernet)
            mainHandler.removeCallbacks(offlineRunnable)

            mainHandler.postDelayed(offlineRunnable, 2500)

            return
        }

        mainHandler.removeCallbacks(offlineRunnable)

        if (!online) {

            online = true

            setBannerStyle(true)

            offlineBanner.text = "✔  عاد الاتصال"

            offlineBanner.visibility = View.VISIBLE

            mainHandler.removeCallbacks(hideBannerRunnable)

            mainHandler.postDelayed(hideBannerRunnable, 2500)

            retryAfterReconnect()

            fetchWeather()
        }
    }

    private fun retryAfterReconnect() {

        if (blocked || gated) {
            return
        }

        if (multiActive) {

            tiles.forEach {
                if (it.player.playbackState != Player.STATE_READY) {
                    it.sourceIdx = 0
                    startTile(it)
                }
            }

            return
        }

        val channel = playingChannel ?: return

        if (externalAudio >= 0) {
            playExternalAudio(externalAudio)
        }

        val healthy = !playingWeb && player.playbackState == Player.STATE_READY

        if (!healthy) {
            startSource(channel, savedServerIndex(channel))
        }
    }

    // =========================
    // Quality strip (small, separate, auto hides)
    // =========================

    private val hideQualityRunnable = Runnable {
        closeQualityStrip()
    }

    private fun qualityStripOpen(): Boolean =
        ::qualityStrip.isInitialized && qualityStrip.visibility == View.VISIBLE

    private fun buildQualityStrip() {

        qualityStrip = LinearLayout(this)

        qualityStrip.orientation = LinearLayout.HORIZONTAL

        qualityStrip.gravity = Gravity.CENTER_VERTICAL

        qualityStrip.setPadding(dp(16), dp(8), dp(12), dp(8))

        qualityStrip.visibility = View.GONE

        qualityStrip.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(150, 20, 22, 58), Color.argb(120, 8, 10, 28))
            ).apply {
                cornerRadius = dp(30).toFloat()
                setStroke(dp(1), Color.argb(80, 255, 255, 255))
            }

        stripTitle = label("الجودة", 13f, cyan, true, Gravity.CENTER)

        qualityStrip.addView(
            stripTitle,
            lp(wrap, wrap).apply { setMargins(0, 0, dp(12), 0) }
        )

        qualityStripChips = LinearLayout(this)

        qualityStripChips.orientation = LinearLayout.HORIZONTAL

        qualityStrip.addView(qualityStripChips, lp(wrap, wrap))

        stripHint = label("", 11f, gray, false, Gravity.CENTER)

        qualityStrip.addView(
            stripHint,
            lp(wrap, wrap).apply { setMargins(dp(14), 0, dp(4), 0) }
        )

        fullscreenContainer.addView(
            qualityStrip,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(26)
            }
        )
    }

    private fun setVideoFocusable(enabled: Boolean) {

        if (!enabled) {

            playerView.isFocusable = false
            playerView.isFocusableInTouchMode = false
            webView.isFocusable = false
            webView.isFocusableInTouchMode = false

            return
        }

        if (playingWeb) {
            webView.isFocusable = true
            webView.isFocusableInTouchMode = !phoneMode
        } else {
            playerView.isFocusable = true
            playerView.isFocusableInTouchMode = !phoneMode
        }
    }

    private fun openQualityStrip() {

        if (playingChannel == null || !isFullscreen) {
            return
        }

        if (playingWeb) {
            Toast.makeText(
                this,
                "الجودة غير متاحة لقنوات يوتيوب / فيسبوك",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val options = collectQualities()

        if (options.size < 2) {
            Toast.makeText(
                this,
                if (options.isEmpty()) "انتظر حتى يبدأ البث" else "هذه القناة بجودة واحدة فقط",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // keep the picture clean: hide the other overlays
        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        val manual = isManualQuality()

        val currentHeight = player.videoSize.height

        qualityStripChips.removeAllViews()

        stripMode = 0

        stripTitle.text = "الجودة"

        stripHint.text = if (stripModeCount() > 1) "▲▼ تبديل" else ""

        var target: View? = null

        fun addChip(text: String, selected: Boolean, action: () -> Unit) {

            val chip = menuItem((if (selected) "✓ " else "") + text, accentFor(0))

            chip.setPadding(dp(16), 0, dp(16), 0)

            chip.setOnClickListener {
                closeQualityStrip()
                action()
            }

            qualityStripChips.addView(
                chip,
                lp(wrap, dp(40)).apply { setMargins(dp(4), 0, dp(4), 0) }
            )

            if (selected || target == null) {
                target = chip
            }
        }

        addChip("تلقائي", !manual) { setQuality(null) }

        options.forEach { option ->
            addChip(
                "${option.height}p",
                manual && option.height == currentHeight
            ) { setQuality(option) }
        }

        setVideoFocusable(false)

        qualityStrip.animate().cancel()

        qualityStrip.alpha = 0f

        qualityStrip.translationY = -dp(14).toFloat()

        qualityStrip.visibility = View.VISIBLE

        qualityStrip.animate().alpha(1f).translationY(0f).setDuration(180).start()

        qualityStrip.post {
            target?.requestFocus()
        }

        mainHandler.removeCallbacks(hideQualityRunnable)

        mainHandler.postDelayed(hideQualityRunnable, 5000)
    }

    private fun closeQualityStrip(immediate: Boolean = false) {

        mainHandler.removeCallbacks(hideQualityRunnable)

        if (!qualityStripOpen()) {
            return
        }

        setVideoFocusable(true)

        qualityStrip.animate().cancel()

        if (immediate) {

            qualityStrip.visibility = View.GONE

        } else {

            qualityStrip.animate()
                .alpha(0f)
                .translationY(-dp(14).toFloat())
                .setDuration(160)
                .withEndAction {
                    qualityStrip.visibility = View.GONE
                }
                .start()
        }

        if (isFullscreen) {
            if (playingWeb) {
                webView.requestFocus()
            } else {
                playerView.requestFocus()
            }
        }
    }

    private fun handleQualityStripKey(event: KeyEvent): Boolean {

        // every key restarts the 5 second timer
        mainHandler.removeCallbacks(hideQualityRunnable)

        mainHandler.postDelayed(hideQualityRunnable, 5000)

        when (event.keyCode) {

            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_PROG_BLUE -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    closeQualityStrip()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    cycleStrip()
                }

                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // =========================
    // Multi view (split screen)
    // =========================

    private fun tileSources(channel: Channel): List<String> =
        sourcesOf(channel).filter { !isWebSource(parseSource(it).url) }

    private fun addToMulti(channel: Channel) {

        if (multiList.size >= 4) {
            Toast.makeText(this, "الحد الأقصى 4 قنوات", Toast.LENGTH_SHORT).show()
            return
        }

        if (tileSources(channel).isEmpty()) {
            Toast.makeText(
                this,
                "هذه القناة لا تدعم المشاهدة المتعددة",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        multiList.add(channel)

        Toast.makeText(
            this,
            if (multiList.size >= 2) {
                "تمت الإضافة (${multiList.size}/4)  •  اضغط مطولاً لبدء المشاهدة"
            } else {
                "تمت الإضافة (${multiList.size}/4)  •  أضف قناة أخرى"
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun removeFromMulti(channel: Channel) {

        multiList.remove(channel)

        Toast.makeText(
            this,
            "تمت الإزالة (${multiList.size}/4)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showMultiMenu() {

        val items = mutableListOf<Pair<String, () -> Unit>>()

        if (multiList.size >= 2) {
            items.add("▶  بدء المشاهدة المتعددة (${multiList.size} قنوات)" to { enterMultiView() })
        }

        if (multiList.isNotEmpty()) {
            items.add(
                "🗑  مسح القائمة" to {
                    multiList.clear()
                    Toast.makeText(this, "تم مسح القائمة", Toast.LENGTH_SHORT).show()
                }
            )
        }

        items.add("إلغاء" to {})

        showMenu(
            if (multiList.isEmpty()) {
                "اضغط مطولاً على أي قناة ثم اختر إضافة للمشاهدة المتعددة"
            } else {
                multiList.joinToString("  •  ") { it.name }
            },
            items
        )
    }

    private fun buildMultiView() {

        multiContainer = FrameLayout(this)

        multiContainer.setBackgroundColor(Color.BLACK)

        multiContainer.visibility = View.GONE

        multiGrid = LinearLayout(this)

        multiContainer.addView(multiGrid, FrameLayout.LayoutParams(matchParent, matchParent))

        multiHint = label(
            if (phoneMode) {
                "اضغط على مربع لتبديل الصوت   •   ضغط مطول لتكبيره"
            } else {
                "◄ ► ▲ ▼  تبديل الصوت   •   OK  تكبير القناة   •   رجوع  خروج"
            },
            13f,
            white,
            true,
            Gravity.CENTER
        )

        multiHint.setPadding(dp(20), dp(8), dp(20), dp(8))

        multiHint.visibility = View.GONE

        multiHint.background =
            GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(190, 8, 10, 28))
                setStroke(dp(1), withAlpha(cyan, 140))
            }

        multiContainer.addView(
            multiHint,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(26)
            }
        )

        val closeButton = TextView(this)

        closeButton.text = "✕"

        closeButton.textSize = 18f

        closeButton.setTextColor(white)

        closeButton.gravity = Gravity.CENTER

        closeButton.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(190, 8, 10, 28))
                setStroke(dp(1), Color.argb(120, 255, 255, 255))
            }

        closeButton.setOnClickListener {
            exitMultiView()
        }

        multiContainer.addView(
            closeButton,
            FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.RIGHT).apply {
                setMargins(0, dp(14), dp(14), 0)
            }
        )

        root.addView(multiContainer, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private val hideMultiHintRunnable = Runnable {
        multiHint.animate().alpha(0f).setDuration(250).withEndAction {
            multiHint.visibility = View.GONE
        }.start()
    }

    private fun showMultiHint() {

        multiHint.animate().cancel()

        multiHint.alpha = 1f

        multiHint.visibility = View.VISIBLE

        mainHandler.removeCallbacks(hideMultiHintRunnable)

        mainHandler.postDelayed(hideMultiHintRunnable, 6000)
    }

    private fun createTile(channel: Channel): Tile {

        val http =
            DefaultHttpDataSource.Factory()
                .setUserAgent(defaultUserAgent)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

        val extractors =
            DefaultExtractorsFactory()
                .setTsExtractorFlags(
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                )

        val tilePlayer =
            ExoPlayer.Builder(
                this,
                DefaultRenderersFactory(this).setEnableDecoderFallback(true)
            )
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http), extractors)
                )
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(3000, 20000, 700, 2000)
                        .build()
                )
                .build()

        val frame = FrameLayout(this)

        frame.setPadding(dp(4), dp(4), dp(4), dp(4))

        val video = PlayerView(this)

        video.useController = false

        video.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

        video.setBackgroundColor(Color.BLACK)

        video.isFocusable = false

        video.player = tilePlayer

        frame.addView(video, FrameLayout.LayoutParams(matchParent, matchParent))

        val status = label("جاري التحميل…", 14f, white, true, Gravity.CENTER)

        frame.addView(status, FrameLayout.LayoutParams(wrap, wrap, Gravity.CENTER))

        val name = label(channel.name, 12f, white, true, Gravity.LEFT)

        name.setPadding(dp(10), dp(3), dp(10), dp(3))

        frame.addView(
            name,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.LEFT).apply {
                setMargins(dp(10), dp(10), 0, 0)
            }
        )

        val tile = Tile(channel, tilePlayer, http, frame, name, status)

        // touch: tap = take the sound, long press = fullscreen
        frame.setOnClickListener {

            val i = tiles.indexOfFirst { it === tile }

            if (i >= 0) {
                multiSel = i
                refreshTileSelection()
            }
        }

        frame.setOnLongClickListener {

            val i = tiles.indexOfFirst { it === tile }

            if (i >= 0) {
                multiSel = i
                maximizeSelectedTile()
            }

            true
        }

        tilePlayer.addListener(
            object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {

                    if (playbackState == Player.STATE_READY) {

                        tile.started = true

                        tile.status.visibility = View.GONE

                    } else if (playbackState == Player.STATE_BUFFERING && !tile.started) {

                        tile.status.text = "جاري التحميل…"

                        tile.status.visibility = View.VISIBLE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    tileFailed(tile)
                }
            }
        )

        return tile
    }

    private fun startTile(tile: Tile) {

        val raw = tileSources(tile.channel).getOrNull(tile.sourceIdx) ?: return

        val source = parseSource(raw)

        applyRequestHeaders(tile.channel, source, tile.http, 0)

        tile.status.text = "جاري التحميل…"

        tile.status.visibility = View.VISIBLE

        try {

            tile.player.setMediaItem(createMediaItem(source.url, detectKind(source.url)))

            tile.player.prepare()

            tile.player.playWhenReady = true

        } catch (_: Exception) {
            tileFailed(tile)
        }
    }

    // dead link: try the next backup link, otherwise show the error on the tile
    private fun tileFailed(tile: Tile) {

        if (tile.sourceIdx + 1 < tileSources(tile.channel).size && online) {

            tile.sourceIdx++

            startTile(tile)

            return
        }

        tile.status.text = "تعذر تشغيل القناة"

        tile.status.visibility = View.VISIBLE
    }

    private fun enterMultiView() {

        if (multiList.size < 2) {
            Toast.makeText(this, "أضف قناتين على الأقل", Toast.LENGTH_SHORT).show()
            return
        }

        if (blocked || gated || multiActive) {
            return
        }

        cancelPreview()

        closeChannelPanel(true)

        closeQualityStrip(true)

        // free the decoder of the normal player
        try {
            player.stop()
            player.clearMediaItems()
        } catch (_: Exception) {
        }

        mainHandler.removeCallbacks(retryRunnable)

        stopWeb()

        playToken++

        multiActive = true

        setPhoneOrientation(true)

        stopExternalAudio()

        tiles.clear()

        multiGrid.removeAllViews()

        multiList.take(4).forEach { tiles.add(createTile(it)) }

        fun cell(tile: Tile, vertical: Boolean): LinearLayout.LayoutParams =
            if (vertical) lp(matchParent, 0, 1f) else lp(0, matchParent, 1f)

        when (tiles.size) {

            2 -> {

                multiGrid.orientation = LinearLayout.HORIZONTAL

                tiles.forEach { multiGrid.addView(it.frame, cell(it, false)) }
            }

            3 -> {

                multiGrid.orientation = LinearLayout.HORIZONTAL

                multiGrid.addView(tiles[0].frame, lp(0, matchParent, 2f))

                val column = LinearLayout(this)

                column.orientation = LinearLayout.VERTICAL

                column.addView(tiles[1].frame, lp(matchParent, 0, 1f))

                column.addView(tiles[2].frame, lp(matchParent, 0, 1f))

                multiGrid.addView(column, lp(0, matchParent, 1f))
            }

            else -> {

                multiGrid.orientation = LinearLayout.VERTICAL

                val top = LinearLayout(this)

                top.orientation = LinearLayout.HORIZONTAL

                top.addView(tiles[0].frame, lp(0, matchParent, 1f))

                top.addView(tiles[1].frame, lp(0, matchParent, 1f))

                val bottom = LinearLayout(this)

                bottom.orientation = LinearLayout.HORIZONTAL

                bottom.addView(tiles[2].frame, lp(0, matchParent, 1f))

                bottom.addView(tiles[3].frame, lp(0, matchParent, 1f))

                multiGrid.addView(top, lp(matchParent, 0, 1f))

                multiGrid.addView(bottom, lp(matchParent, 0, 1f))
            }
        }

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        multiContainer.visibility = View.VISIBLE

        multiSel = 0

        refreshTileSelection()

        tiles.forEach { startTile(it) }

        hideSystemBars()

        showMultiHint()
    }

    private fun exitMultiView(restoreMain: Boolean = true) {

        if (!multiActive) {
            return
        }

        multiActive = false

        setPhoneOrientation(false)

        mainHandler.removeCallbacks(hideMultiHintRunnable)

        tiles.forEach {
            try {
                it.player.release()
            } catch (_: Exception) {
            }
        }

        tiles.clear()

        multiGrid.removeAllViews()

        multiContainer.visibility = View.GONE

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        fullscreenContainer.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        showSystemBars()

        if (restoreMain && !blocked) {
            playingChannel?.let { playChannel(it) }
        }

        focusChannelsColumn()
    }

    // sound only from the selected tile, every tile keeps playing
    private fun refreshTileSelection() {

        tiles.forEachIndexed { i, tile ->

            val selected = i == multiSel

            tile.frame.background =
                GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.BLACK)
                    setStroke(
                        if (selected) dp(4) else dp(1),
                        if (selected) cyan else Color.argb(60, 255, 255, 255)
                    )
                }

            tile.nameLabel.text = (if (selected) "🔊 " else "🔇 ") + tile.channel.name

            tile.nameLabel.background =
                GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(if (selected) withAlpha(cyan, 210) else Color.argb(170, 8, 10, 28))
                }

            tile.nameLabel.setTextColor(if (selected) Color.BLACK else white)

            tile.player.volume = if (selected) 1f else 0f

            // the muted tiles use a lower quality to spare the device
            try {

                val params = tile.player.trackSelectionParameters.buildUpon()

                if (selected) {
                    params.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                } else if (tiles.size >= 3) {
                    params.setMaxVideoSize(854, 480)
                } else {
                    params.setMaxVideoSize(1280, 720)
                }

                tile.player.trackSelectionParameters = params.build()

            } catch (_: Exception) {
            }
        }
    }

    private fun tileCenter(tile: Tile): Pair<Float, Float> {

        val location = IntArray(2)

        tile.frame.getLocationOnScreen(location)

        return Pair(
            location[0] + tile.frame.width / 2f,
            location[1] + tile.frame.height / 2f
        )
    }

    private fun moveMultiSelection(dx: Int, dy: Int) {

        if (tiles.size < 2) {
            return
        }

        val from = tileCenter(tiles[multiSel])

        var best = -1

        var bestScore = Float.MAX_VALUE

        tiles.forEachIndexed { i, tile ->

            if (i == multiSel) {
                return@forEachIndexed
            }

            val c = tileCenter(tile)

            val ddx = c.first - from.first

            val ddy = c.second - from.second

            val inDirection =
                when {
                    dx > 0 -> ddx > 10
                    dx < 0 -> ddx < -10
                    dy > 0 -> ddy > 10
                    else -> ddy < -10
                }

            if (!inDirection) {
                return@forEachIndexed
            }

            val score =
                if (dx != 0) abs(ddx) + 2 * abs(ddy) else abs(ddy) + 2 * abs(ddx)

            if (score < bestScore) {
                bestScore = score
                best = i
            }
        }

        // nothing in that direction: cycle through the tiles
        multiSel =
            if (best >= 0) {
                best
            } else {
                val step = if (dx > 0 || dy > 0) 1 else -1
                (multiSel + step + tiles.size) % tiles.size
            }

        refreshTileSelection()
    }

    private fun handleMultiKey(event: KeyEvent): Boolean {

        val code = event.keyCode

        val isDown = event.action == KeyEvent.ACTION_DOWN

        when (code) {

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isDown) moveMultiSelection(-1, 0)
                if (isDown) showMultiHint()
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isDown) moveMultiSelection(1, 0)
                if (isDown) showMultiHint()
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isDown) moveMultiSelection(0, -1)
                if (isDown) showMultiHint()
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isDown) moveMultiSelection(0, 1)
                if (isDown) showMultiHint()
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (isDown && event.repeatCount == 0) {
                    maximizeSelectedTile()
                }
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                if (isDown && event.repeatCount == 0) {
                    exitMultiView()
                }
                return true
            }

            // 1..4 = jump straight to a tile
            in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_4 -> {

                if (isDown && event.repeatCount == 0) {

                    val index = code - KeyEvent.KEYCODE_1

                    if (index < tiles.size) {
                        multiSel = index
                        refreshTileSelection()
                    }
                }

                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // OK: leave the split screen and play the selected channel fullscreen
    private fun maximizeSelectedTile() {

        val channel = tiles.getOrNull(multiSel)?.channel ?: return

        val others = ArrayList(multiList)

        exitMultiView(false)

        playChannel(channel)

        // up / down now zaps between the channels of the split screen
        playingList = others

        enterFullscreen()
    }

    // =========================
    // Screen sharing (phone -> TV)
    // =========================

    private fun isTvDevice(): Boolean =
        try {
            val manager =
                getSystemService(UI_MODE_SERVICE) as android.app.UiModeManager

            manager.currentModeType ==
                    android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

        } catch (_: Exception) {
            false
        }

    // opens the system cast / mirroring screen: the phone screen goes to the TV
    private fun openScreenShare() {

        val actions =
            listOf(
                "android.settings.CAST_SETTINGS",
                "android.settings.WIFI_DISPLAY_SETTINGS"
            )

        for (action in actions) {

            try {
                startActivity(Intent(action))
                return
            } catch (_: Exception) {
            }
        }

        Toast.makeText(
            this,
            "افتح إعدادات الهاتف واختر مشاركة الشاشة (Cast / Smart View)",
            Toast.LENGTH_LONG
        ).show()
    }

    // =========================
    // Servers (the links of a channel)
    // =========================

    private fun savedServerIndex(channel: Channel): Int {

        val count = sourcesOf(channel).size

        val saved = prefs.getInt("srv_" + channelKey(channel), 0)

        return if (saved in 0 until count) saved else 0
    }

    private fun serverHint(): String {

        val channel = playingChannel ?: return ""

        return if (sourcesOf(channel).size > 1) {
            "\nيمكنك تجربة سيرفر آخر من خيارات المشغل"
        } else {
            ""
        }
    }

    private fun switchServer(index: Int) {

        val channel = playingChannel ?: return

        if (index !in sourcesOf(channel).indices) {
            return
        }

        if (index == sourceIndex && !finalPlaybackError) {
            Toast.makeText(this, "أنت على السيرفر ${index + 1} حالياً", Toast.LENGTH_SHORT).show()
            return
        }

        // next time this channel opens on the server the viewer chose
        prefs.edit().putInt("srv_" + channelKey(channel), index).apply()

        playToken++

        startSource(channel, index)

        Toast.makeText(this, "السيرفر ${index + 1}", Toast.LENGTH_SHORT).show()
    }

    // fullscreen: small strip at the top, hides after a few seconds
    private fun openServerStrip() {

        val channel = playingChannel ?: return

        if (!isFullscreen) {
            return
        }

        val count = sourcesOf(channel).size

        if (count < 2) {
            Toast.makeText(this, "هذه القناة لها سيرفر واحد فقط", Toast.LENGTH_SHORT).show()
            return
        }

        closeChannelPanel(true)

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        stripMode = 1

        stripTitle.text = "السيرفر"

        stripHint.text = if (stripModeCount() > 1) "▲▼ تبديل" else ""

        qualityStripChips.removeAllViews()

        var target: View? = null

        for (i in 0 until count) {

            val selected = i == sourceIndex

            val chip = menuItem((if (selected) "✓ " else "") + "السيرفر ${i + 1}", accentFor(0))

            chip.setPadding(dp(16), 0, dp(16), 0)

            chip.setOnClickListener {
                closeQualityStrip()
                switchServer(i)
            }

            qualityStripChips.addView(
                chip,
                lp(wrap, dp(40)).apply { setMargins(dp(4), 0, dp(4), 0) }
            )

            if (selected || target == null) {
                target = chip
            }
        }

        setVideoFocusable(false)

        qualityStrip.animate().cancel()

        qualityStrip.alpha = 0f

        qualityStrip.translationY = -dp(14).toFloat()

        qualityStrip.visibility = View.VISIBLE

        qualityStrip.animate().alpha(1f).translationY(0f).setDuration(180).start()

        qualityStrip.post {
            target?.requestFocus()
        }

        mainHandler.removeCallbacks(hideQualityRunnable)

        mainHandler.postDelayed(hideQualityRunnable, 5000)
    }

    // normal screen: long press on the player
    private fun showPlayerOptionsMenu() {

        val channel = playingChannel ?: return

        val items = mutableListOf<Pair<String, () -> Unit>>()

        items.add("🎚  الجودة" to { showQualityMenu() })

        items.add("📐  أبعاد الشاشة (${aspectName()})" to { showAspectMenu() })

        items.add("⚠  إبلاغ عن مشكلة" to { showReportMenu() })

        if (audioAvailable()) {
            items.add("🎙  الصوت (المعلق)" to { showAudioMenu() })
        }

        val count = sourcesOf(channel).size

        if (count > 1) {
            items.add("🖥  السيرفر (${sourceIndex + 1}/$count)" to { showServerMenu() })
        }

        items.add("إلغاء" to {})

        showMenu("خيارات المشغل", items)
    }

    private fun showServerMenu() {

        val channel = playingChannel ?: return

        val count = sourcesOf(channel).size

        if (count < 2) {
            Toast.makeText(this, "هذه القناة لها سيرفر واحد فقط", Toast.LENGTH_SHORT).show()
            return
        }

        val items = mutableListOf<Pair<String, () -> Unit>>()

        for (i in 0 until count) {
            items.add(
                ((if (i == sourceIndex) "✓  " else "") + "السيرفر ${i + 1}") to { switchServer(i) }
            )
        }

        items.add("إلغاء" to {})

        showMenu("اختر السيرفر", items)
    }

    private fun toggleAutoFallback() {

        autoFallback = !autoFallback

        prefs.edit().putBoolean("auto_fallback", autoFallback).apply()

        Toast.makeText(
            this,
            if (autoFallback) "التبديل التلقائي للسيرفر: مفعّل" else "التبديل التلقائي للسيرفر: متوقف",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================
    // Phone layout (portrait)
    // =========================

    private fun boxWidth(maxDp: Int): Int =
        minOf(dp(maxDp), resources.displayMetrics.widthPixels - dp(24))

    private fun setPhoneOrientation(landscape: Boolean) {

        if (!phoneMode) {
            return
        }

        requestedOrientation =
            if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
    }

    private fun buildPhoneHeader(): View {

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL

        header.gravity = Gravity.CENTER_VERTICAL

        header.setPadding(dp(12), dp(6), dp(12), dp(4))

        val logoBox = ShineLayout()

        logoBox.addView(
            createLogoMark(18f, 12),
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        header.addView(logoBox, lp(dp(40), dp(40)))

        val brandCol = LinearLayout(this)

        brandCol.orientation = LinearLayout.VERTICAL

        brandCol.setPadding(dp(10), 0, 0, 0)

        val brand = label("NIYATI", 18f, white, true, Gravity.LEFT)

        brand.letterSpacing = 0.14f

        val sport = label("SPORTS IPTV", 8.5f, cyan, true, Gravity.LEFT)

        sport.letterSpacing = 0.25f

        brandCol.addView(brand, lp(matchParent, wrap))

        brandCol.addView(sport, lp(matchParent, wrap))

        header.addView(brandCol, lp(0, wrap, 1f))

        // weather
        weatherIcon = TextView(this)

        weatherIcon.text = "☁️"

        weatherIcon.textSize = 17f

        weatherTemp = label("--°", 15f, white, true, Gravity.LEFT)

        weatherCityView = label(weatherCity, 9f, gray, false, Gravity.LEFT)

        val chip = LinearLayout(this)

        chip.orientation = LinearLayout.HORIZONTAL

        chip.gravity = Gravity.CENTER_VERTICAL

        chip.setPadding(dp(8), dp(2), dp(10), dp(2))

        chip.background =
            GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(22, 255, 255, 255))
                setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }

        chip.addView(weatherIcon, lp(wrap, wrap))

        chip.addView(weatherTemp, lp(wrap, wrap).apply { setMargins(dp(4), 0, 0, 0) })

        header.addView(chip, lp(wrap, wrap).apply { setMargins(dp(6), 0, 0, 0) })

        // clock + date
        val timeCol = LinearLayout(this)

        timeCol.orientation = LinearLayout.VERTICAL

        timeCol.gravity = Gravity.RIGHT

        val clock = TextClock(this)

        clock.format12Hour = "hh:mm a"

        clock.format24Hour = "hh:mm a"

        clock.textSize = 14f

        clock.setTextColor(white)

        clock.typeface = Typeface.DEFAULT_BOLD

        clock.gravity = Gravity.RIGHT

        dateView = label("", 9f, gray, false, Gravity.RIGHT)

        timeCol.addView(clock, lp(wrap, wrap))

        timeCol.addView(dateView, lp(wrap, wrap))

        header.addView(timeCol, lp(wrap, wrap).apply { setMargins(dp(8), 0, 0, 0) })

        // settings
        val settingsBtn = TextView(this)

        settingsBtn.text = "⚙"

        settingsBtn.textSize = 18f

        settingsBtn.setTextColor(white)

        settingsBtn.gravity = Gravity.CENTER

        settingsBtn.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(22, 255, 255, 255))
                setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }

        settingsBtn.setOnClickListener {
            showSettingsMenu()
        }

        header.addView(settingsBtn, lp(dp(38), dp(38)).apply { setMargins(dp(8), 0, 0, 0) })

        return header
    }

    // phone: header, matches card, player 16:9, info bar, packages row, channels list
    private fun buildPhoneContent() {

        normalScreen.addView(buildPhoneHeader(), lp(matchParent, dp(56)))

        normalScreen.addView(buildSectionBar(), lp(matchParent, wrap))

        val live = LinearLayout(this)

        live.orientation = LinearLayout.VERTICAL

        liveHost = live

        normalScreen.addView(live, lp(matchParent, 0, 1f))

        buildVodHost()

        live.addView(
            buildTicker(),
            lp(matchParent, wrap).apply { setMargins(dp(10), dp(2), dp(10), dp(4)) }
        )

        // ---- player (16:9, the full width) ----
        val glowFrame = GlowFrame()

        playerFrame = glowFrame

        playerGlow = glowFrame

        playerFrame.setPadding(dp(3), dp(3), dp(3), dp(3))

        playerFrame.isFocusable = false

        applyPlayerFrameBackground(false)

        playerFrame.setOnClickListener {
            enterFullscreen()
        }

        playerFrame.setOnLongClickListener {
            showPlayerOptionsMenu()
            true
        }

        val playerWidth = resources.displayMetrics.widthPixels - dp(16)

        live.addView(
            playerFrame,
            lp(matchParent, playerWidth * 9 / 16).apply { setMargins(dp(8), dp(2), dp(8), 0) }
        )

        live.addView(
            buildInfoBar(),
            lp(matchParent, dp(56)).apply { setMargins(dp(8), dp(6), dp(8), 0) }
        )

        // ---- packages: a horizontal row ----
        packagesHeader = SectionHeader("الباقات", "PACKAGES", accentFor(0))

        val packageScroll = HorizontalScrollView(this)

        packageScroll.isHorizontalScrollBarEnabled = false

        packageScroll.overScrollMode = View.OVER_SCROLL_NEVER

        packagesList = createList()

        packagesList.orientation = LinearLayout.HORIZONTAL

        packagesList.setPadding(dp(5), dp(8), dp(5), dp(2))

        packageScroll.addView(packagesList)

        live.addView(packageScroll, lp(matchParent, dp(78)))

        // ---- channels: the vertical list ----
        channelsHeader = SectionHeader("القنوات", "CHANNELS", accentFor(1))

        live.addView(channelsHeader, lp(matchParent, dp(52)))

        channelScroll = createScroll()

        channelsList = createList()

        channelScroll.addView(channelsList)

        live.addView(channelScroll, lp(matchParent, 0, 1f))
    }

    // =========================
    // Touch buttons (fullscreen)
    // =========================

    private val hideTouchRunnable = Runnable {
        hideTouchControls()
    }

    private fun resetHudTimer() {
        mainHandler.removeCallbacks(hideTouchRunnable)
        mainHandler.postDelayed(hideTouchRunnable, 6000)
    }

    private fun touchControlsVisible(): Boolean =
        ::touchControls.isInitialized && touchControls.visibility == View.VISIBLE

    private fun hudStates(radiusDp: Int, base: Int): android.graphics.drawable.StateListDrawable {

        val pressed =
            GradientDrawable().apply {
                cornerRadius = dp(radiusDp).toFloat()
                setColor(withAlpha(cyan, 100))
            }

        val lit =
            GradientDrawable().apply {
                cornerRadius = dp(radiusDp).toFloat()
                setColor(withAlpha(cyan, 110))
                setStroke(dp(2), Color.WHITE)
            }

        val states = android.graphics.drawable.StateListDrawable()

        states.addState(intArrayOf(android.R.attr.state_pressed), pressed)

        states.addState(intArrayOf(android.R.attr.state_focused), lit)

        states.addState(
            intArrayOf(),
            GradientDrawable().apply {
                cornerRadius = dp(radiusDp).toFloat()
                setColor(base)
            }
        )

        return states
    }

    private fun touchButton(
        icon: String,
        text: String,
        keepOpen: Boolean = false,
        action: () -> Unit
    ): LinearLayout {

        val button = LinearLayout(this)

        button.orientation = LinearLayout.VERTICAL

        button.gravity = Gravity.CENTER

        button.setPadding(dp(8), dp(6), dp(8), dp(6))

        button.isClickable = true

        button.isFocusable = true

        button.isFocusableInTouchMode = !phoneMode

        button.background = hudStates(16, Color.TRANSPARENT)

        val iconView = TextView(this)

        iconView.text = icon

        iconView.textSize = 21f

        iconView.gravity = Gravity.CENTER

        iconView.setTextColor(white)

        button.addView(iconView, lp(wrap, wrap))

        button.addView(label(text, 10.5f, dimWhite, false, Gravity.CENTER), lp(wrap, wrap))

        button.setOnClickListener {

            if (keepOpen) {
                resetHudTimer()
            } else {
                hideTouchControls(true)
            }

            action()
        }

        return button
    }

    private fun roundIcon(iconPath: String, action: () -> Unit): View {

        val button = FrameLayout(this)

        val icon = LineIconView()

        icon.setIcon(iconPath)

        button.addView(icon, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))

        button.isClickable = true

        button.isFocusable = true

        button.isFocusableInTouchMode = !phoneMode

        button.background = hudStates(23, Color.argb(70, 255, 255, 255))

        button.setOnClickListener {
            hideTouchControls(true)
            action()
        }

        return button
    }

    // thin line icon drawn from SVG path data (24 x 24)
    private inner class LineIconView : View(this@MainActivity) {

        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = Color.WHITE
            }

        private var path: Path? = null
        private var source = ""
        private val scaled = Path()
        private val matrix = Matrix()
        private var filled = false

        fun setIcon(d: String) {

            if (d == source) {
                return
            }

            source = d

            path =
                try {
                    androidx.core.graphics.PathParser.createPathFromPathData(d)
                } catch (_: Exception) {
                    null
                }

            invalidate()
        }

        fun setFilled(value: Boolean) {
            filled = value
            invalidate()
        }

        fun setTint(color: Int) {
            paint.color = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {

            val p = path ?: return

            val scale = width / 24f

            matrix.setScale(scale, scale)

            p.transform(matrix, scaled)

            paint.strokeWidth = 1.9f * scale

            paint.style = if (filled) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE

            canvas.drawPath(scaled, paint)
        }
    }

    // floating round button: the focused one grows and shows its name
    private fun circleButton(
        iconPath: String,
        text: String,
        keepOpen: Boolean = false,
        action: () -> Unit
    ): LinearLayout {

        val box = LinearLayout(this)

        box.orientation = LinearLayout.VERTICAL

        box.gravity = Gravity.CENTER_HORIZONTAL

        box.clipChildren = false

        box.isClickable = true

        box.isFocusable = true

        box.isFocusableInTouchMode = !phoneMode

        val size = if (phoneMode) 48 else 62

        val circle = FrameLayout(this)

        val icon = LineIconView()

        icon.setIcon(iconPath)

        circle.addView(
            icon,
            FrameLayout.LayoutParams(dp(if (phoneMode) 24 else 28), dp(if (phoneMode) 24 else 28), Gravity.CENTER)
        )

        box.addView(circle, lp(dp(size), dp(size)))

        val name = label(text, if (phoneMode) 10.5f else 12.5f, white, true, Gravity.CENTER)

        name.setPadding(dp(12), dp(1), dp(12), dp(1))

        box.addView(name, lp(wrap, wrap).apply { topMargin = dp(8) })

        fun style(focused: Boolean) {

            circle.background =
                if (focused) {
                    GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(cyan, violet)
                    ).apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(2), Color.WHITE)
                    }
                } else {
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.argb(190, 14, 16, 44))
                        setStroke(dp(1), Color.argb(64, 255, 255, 255))
                    }
                }

            val scale = if (focused) 1.25f else 1f

            circle.animate().scaleX(scale).scaleY(scale).setStartDelay(0).setDuration(130).start()

            // on a touch screen the names stay visible, on the TV only the focused one shows its name
            if (phoneMode) {

                name.background = null

            } else {

                name.visibility = if (focused) View.VISIBLE else View.INVISIBLE

                name.background =
                    GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(Color.argb(220, 6, 10, 30))
                        setStroke(dp(1), withAlpha(cyan, 150))
                    }
            }

            if (Build.VERSION.SDK_INT >= 28) {
                circle.elevation = dp(if (focused) 12 else 4).toFloat()
                circle.outlineSpotShadowColor = if (focused) cyan else Color.BLACK
                circle.outlineAmbientShadowColor = if (focused) cyan else Color.BLACK
            }
        }

        style(false)

        box.setOnFocusChangeListener { _, focused -> style(focused) }

        box.setOnClickListener {

            if (keepOpen) {
                resetHudTimer()
            } else {
                hideTouchControls(true)
            }

            action()
        }

        box.tag = icon

        return box
    }

    // seek bar with the buffered part and a round handle (videos)
    private inner class SeekBarView : View(this@MainActivity) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private var progress = 0f
        private var buffered = 0f
        private var dragging = false

        var onSeek: ((Float) -> Unit)? = null

        fun setPosition(p: Float, b: Float) {

            if (!dragging) {
                progress = p.coerceIn(0f, 1f)
            }

            buffered = b.coerceIn(0f, 1f)

            invalidate()
        }

        override fun onDraw(canvas: Canvas) {

            val w = width.toFloat()

            val h = height.toFloat()

            val track = dp(5).toFloat()

            val top = h / 2 - track / 2

            paint.shader = null

            paint.color = Color.argb(60, 255, 255, 255)

            rect.set(0f, top, w, top + track)

            canvas.drawRoundRect(rect, track / 2, track / 2, paint)

            paint.color = Color.argb(115, 255, 255, 255)

            rect.set(0f, top, w * buffered, top + track)

            canvas.drawRoundRect(rect, track / 2, track / 2, paint)

            paint.shader = LinearGradient(0f, 0f, maxOf(w, 1f), 0f, cyan, violet, Shader.TileMode.CLAMP)

            rect.set(0f, top, w * progress, top + track)

            canvas.drawRoundRect(rect, track / 2, track / 2, paint)

            paint.shader = null

            paint.color = Color.WHITE

            canvas.drawCircle(w * progress, h / 2, dp(if (dragging) 10 else 8).toFloat(), paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    dragging = true
                    parent.requestDisallowInterceptTouchEvent(true)
                    progress = (event.x / width).coerceIn(0f, 1f)
                    resetHudTimer()
                    invalidate()
                }

                MotionEvent.ACTION_MOVE -> {
                    progress = (event.x / width).coerceIn(0f, 1f)
                    invalidate()
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    onSeek?.invoke(progress)
                    resetHudTimer()
                    invalidate()
                }
            }

            return true
        }
    }

    private fun buildTouchControls() {

        // ---------- top bar: exit, clock, channel info ----------
        hudTop = LinearLayout(this)

        hudTop.orientation = LinearLayout.HORIZONTAL

        hudTop.gravity = Gravity.CENTER_VERTICAL

        hudTop.setPadding(dp(18), dp(14), dp(18), dp(34))

        hudTop.visibility = View.GONE

        hudTop.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(225, 4, 6, 18), Color.argb(0, 4, 6, 18))
            )

        hudTop.addView(roundIcon(ICON_EXIT) { exitFullscreen() }, lp(dp(46), dp(46)))

        val clock = TextClock(this)

        clock.format12Hour = "hh:mm a"

        clock.format24Hour = "hh:mm a"

        clock.textSize = 15f

        clock.setTextColor(white)

        clock.typeface = Typeface.DEFAULT_BOLD

        hudTop.addView(clock, lp(wrap, wrap).apply { setMargins(dp(14), 0, 0, 0) })

        hudTop.addView(View(this), lp(0, 1, 1f))

        hudLive = label("● مباشر", 12f, white, true, Gravity.CENTER)

        hudLive.setPadding(dp(12), dp(3), dp(12), dp(3))

        hudLive.background =
            GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.argb(200, 220, 38, 38))
            }

        hudTop.addView(hudLive, lp(wrap, wrap).apply { setMargins(0, 0, dp(8), 0) })

        hudQuality = label("", 12f, cyan, true, Gravity.CENTER)

        hudQuality.setPadding(dp(10), dp(3), dp(10), dp(3))

        hudQuality.background =
            GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.argb(150, 8, 10, 28))
                setStroke(dp(1), cyan)
            }

        hudTop.addView(hudQuality, lp(wrap, wrap).apply { setMargins(0, 0, dp(12), 0) })

        val info = LinearLayout(this)

        info.orientation = LinearLayout.VERTICAL

        hudName = label("", 18f, white, true)

        setupFit(hudName, 18, 11)

        hudName.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL

        hudSub = label("", 12f, dimWhite)

        info.addView(hudName, lp(dp(300), dp(26)))

        info.addView(hudSub, lp(dp(300), wrap))

        hudTop.addView(info, lp(wrap, wrap).apply { setMargins(0, 0, dp(10), 0) })

        hudLogo = LogoBadge(48, accentFor(0))

        hudTop.addView(hudLogo, lp(dp(48), dp(48)))

        fullscreenContainer.addView(
            hudTop,
            FrameLayout.LayoutParams(matchParent, wrap, Gravity.TOP)
        )

        // ---------- bottom: floating round buttons (+ a seek bar for videos) ----------
        val card = LinearLayout(this)

        touchControls = card

        card.orientation = LinearLayout.VERTICAL

        card.gravity = Gravity.CENTER_HORIZONTAL

        card.clipChildren = false

        card.setPadding(dp(8), dp(4), dp(8), dp(2))

        card.visibility = View.GONE

        // seek row (videos only) in its own glass capsule
        hudSeekRow = LinearLayout(this)

        hudSeekRow.orientation = LinearLayout.HORIZONTAL

        hudSeekRow.gravity = Gravity.CENTER_VERTICAL

        hudSeekRow.minimumWidth = dp(if (phoneMode) 420 else 560)

        hudSeekRow.setPadding(dp(18), dp(4), dp(18), dp(4))

        hudSeekRow.visibility = View.GONE

        hudSeekRow.background =
            GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(Color.argb(200, 10, 12, 34))
                setStroke(dp(1), Color.argb(70, 255, 255, 255))
            }

        hudTimeCur = label("0:00", 12f, white, true, Gravity.LEFT)

        hudSeek = SeekBarView()

        hudSeek.onSeek = { fraction ->
            val duration = player.duration
            if (duration > 0) {
                player.seekTo((fraction * duration).toLong())
            }
        }

        hudTimeTotal = label("0:00", 12f, white, true, Gravity.RIGHT)

        hudSeekRow.addView(hudTimeCur, lp(dp(54), wrap))

        hudSeekRow.addView(hudSeek, lp(0, dp(30), 1f))

        hudSeekRow.addView(hudTimeTotal, lp(dp(54), wrap))

        card.addView(hudSeekRow, lp(wrap, wrap).apply { bottomMargin = dp(18) })

        // the round buttons
        val buttons = LinearLayout(this)

        buttons.orientation = LinearLayout.HORIZONTAL

        buttons.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        buttons.clipChildren = false

        val gap = if (phoneMode) 4 else 10

        fun slot() = lp(wrap, wrap).apply { setMargins(dp(gap), 0, dp(gap), 0) }

        hudVodButtons.clear()

        val back = circleButton(ICON_BACK10, "10 ث", true) { seekBy(-10000L) }

        val play = circleButton(ICON_PAUSE, "تشغيل", true) { togglePlayPause() }

        hudPlayIcon = play.tag as LineIconView

        val forward = circleButton(ICON_FWD10, "10 ث", true) { seekBy(10000L) }

        hudVodButtons.add(back)
        hudVodButtons.add(play)
        hudVodButtons.add(forward)

        hudVodButtons.forEach {
            it.visibility = View.GONE
            buttons.addView(it, slot())
        }

        buttons.addView(circleButton(ICON_PREV, "السابقة") { goPreviousChannel() }, slot())

        val fav = circleButton(ICON_STAR, "المفضلة") {
            playingChannel?.let { toggleFavorite(it) }
        }

        touchFavIcon = fav.tag as LineIconView

        buttons.addView(fav, slot())

        touchAudioBtn = circleButton(ICON_AUDIO, "الصوت") { openAudioStrip() }

        buttons.addView(touchAudioBtn, slot())

        touchServerBtn = circleButton(ICON_SERVER, "السيرفر") { openServerStrip() }

        buttons.addView(touchServerBtn, slot())

        buttons.addView(circleButton(ICON_QUALITY, "الجودة") { openQualityStrip() }, slot())

        buttons.addView(circleButton(ICON_ASPECT, "الأبعاد") { openAspectStrip() }, slot())

        buttons.addView(circleButton(ICON_FLAG, "إبلاغ") { showReportMenu() }, slot())

        val channelsButton = circleButton(ICON_LIST, "القنوات") { openChannelPanel() }

        hudFirstButton = channelsButton

        buttons.addView(channelsButton, slot())

        card.addView(buttons, lp(wrap, wrap))

        fullscreenContainer.addView(
            card,
            FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(26)
            }
        )
    }

    private fun fmtTime(ms: Long): String {

        val total = (ms / 1000).coerceAtLeast(0)

        val h = total / 3600

        val m = (total % 3600) / 60

        val sec = total % 60

        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        } else {
            String.format(Locale.US, "%d:%02d", m, sec)
        }
    }

    private fun togglePlayPause() {

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.playWhenReady = true
            return
        }

        player.playWhenReady = !player.playWhenReady
    }

    private fun seekBy(deltaMs: Long) {

        val duration = player.duration

        val target = player.currentPosition + deltaMs

        player.seekTo(if (duration > 0) target.coerceIn(0L, duration) else maxOf(0L, target))
    }

    // double tap on the left / right half = 10 seconds (videos)
    private fun handleDoubleTap(x: Float) {

        val channel = playingChannel ?: return

        if (playingWeb || !isVodChannel(channel) || panelOpen() || menuOpen()) {
            return
        }

        val back = x < resources.displayMetrics.widthPixels / 2f

        seekBy(if (back) -10000L else 10000L)

        Toast.makeText(this, if (back) "⏪ 10 ثوانٍ" else "⏩ 10 ثوانٍ", Toast.LENGTH_SHORT).show()
    }

    private val hudTickRunnable = object : Runnable {

        override fun run() {

            if (!touchControlsVisible()) {
                return
            }

            val channel = playingChannel

            if (channel != null && !playingWeb && isVodChannel(channel)) {

                val duration = player.duration

                if (duration > 0) {

                    hudSeek.setPosition(
                        player.currentPosition.toFloat() / duration,
                        player.bufferedPosition.toFloat() / duration
                    )

                    hudTimeCur.text = fmtTime(player.currentPosition)

                    hudTimeTotal.text = fmtTime(duration)
                }

                hudPlayIcon.setIcon(
                    if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) ICON_PAUSE else ICON_PLAY
                )
            }

            mainHandler.postDelayed(this, 500)
        }
    }

    private fun showTouchControls() {

        val channel = playingChannel ?: return

        if (!isFullscreen) {
            return
        }

        val vod = isVodChannel(channel)

        // keep the picture clean
        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        // top bar: the channel and what is on now
        fitName(hudName, channel.name, 18, 11)

        hudSub.text = epgLine(channel) ?: nowSubText(channel)

        hudLogo.bind(channel.name, channel.logo)

        hudLive.visibility = if (vod) View.GONE else View.VISIBLE

        hudQuality.text = qualityText

        hudQuality.visibility = if (qualityText.isEmpty()) View.GONE else View.VISIBLE

        // bottom card
        val favorite = isFavorite(channel)

        touchFavIcon.setFilled(favorite)

        touchFavIcon.setTint(if (favorite) gold else white)

        hudVodButtons.forEach { it.visibility = if (vod) View.VISIBLE else View.GONE }

        hudSeekRow.visibility = if (vod) View.VISIBLE else View.GONE

        touchServerBtn.visibility = if (serversAvailable()) View.VISIBLE else View.GONE

        touchAudioBtn.visibility = if (audioAvailable()) View.VISIBLE else View.GONE

        if (!phoneMode) {
            setVideoFocusable(false)
        }

        hudTop.animate().cancel()

        hudTop.alpha = 0f

        hudTop.visibility = View.VISIBLE

        hudTop.animate().alpha(1f).setDuration(160).start()

        touchControls.animate().cancel()

        touchControls.alpha = 0f

        touchControls.visibility = View.VISIBLE

        touchControls.animate().alpha(1f).setDuration(160).start()

        mainHandler.removeCallbacks(hudTickRunnable)

        mainHandler.post(hudTickRunnable)

        resetHudTimer()

        if (!phoneMode) {
            touchControls.post { hudFirstButton?.requestFocus() }
        }
    }

    private fun hideTouchControls(immediate: Boolean = false) {

        mainHandler.removeCallbacks(hideTouchRunnable)

        mainHandler.removeCallbacks(hudTickRunnable)

        if (!touchControlsVisible()) {
            return
        }

        hudTop.animate().cancel()

        touchControls.animate().cancel()

        if (immediate) {

            hudTop.visibility = View.GONE

            touchControls.visibility = View.GONE

        } else {

            hudTop.animate().alpha(0f).setDuration(180).withEndAction { hudTop.visibility = View.GONE }.start()

            touchControls.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction { touchControls.visibility = View.GONE }
                .start()
        }

        if (!phoneMode && isFullscreen) {

            setVideoFocusable(true)

            if (!panelOpen() && !qualityStripOpen()) {
                if (playingWeb) webView.requestFocus() else playerView.requestFocus()
            }
        }
    }

    private fun handleHudKey(event: KeyEvent): Boolean {

        resetHudTimer()

        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_MENU) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                hideTouchControls()
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun hitView(view: View, x: Float, y: Float): Boolean {

        val location = IntArray(2)

        view.getLocationOnScreen(location)

        return x >= location[0] && x <= location[0] + view.width &&
                y >= location[1] && y <= location[1] + view.height
    }

    private fun handleScreenTap(x: Float, y: Float) {

        if (menuOpen() || noticeOpen() || multiActive) {
            return
        }

        // tap outside an open list / strip closes it
        if (panelOpen()) {
            if (!hitView(channelPanel, x, y)) {
                closeChannelPanel()
            }
            return
        }

        if (qualityStripOpen()) {
            if (!hitView(qualityStrip, x, y)) {
                closeQualityStrip()
            }
            return
        }

        if (touchControlsVisible()) {
            if (!hitView(touchControls, x, y) && !hitView(hudTop, x, y)) {
                hideTouchControls()
            }
            return
        }

        showTouchControls()
    }

    // swipe up = next channel, swipe down = previous channel
    private fun handleFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float) {

        if (e1 == null || menuOpen() || noticeOpen() || multiActive || panelOpen() ||
            qualityStripOpen() || touchControlsVisible()
        ) {
            return
        }

        val dy = e2.y - e1.y

        val dx = e2.x - e1.x

        if (abs(velocityY) > 1200 && abs(dy) > dp(90) && abs(dy) > 2 * abs(dx)) {
            zap(if (dy < 0) 1 else -1)
        }
    }

    // =========================
    // Audio: commentators
    // =========================

    private fun collectAudios(child: DataSnapshot): List<AudioItem> {

        val list = mutableListOf<AudioItem>()

        for (a in child.child("audios").children) {

            val url = a.readString("url") ?: continue

            list.add(AudioItem(a.readString("name") ?: "تعليق ${list.size + 1}", url))
        }

        return list
    }

    // audio tracks inside the stream (e.g. Arabic / English commentary)
    private fun collectEmbeddedAudio(): List<AudioOption> {

        val out = mutableListOf<AudioOption>()

        for (g in player.currentTracks.groups) {

            if (g.type != C.TRACK_TYPE_AUDIO) {
                continue
            }

            for (i in 0 until g.length) {

                if (!g.isTrackSupported(i)) {
                    continue
                }

                val f = g.getTrackFormat(i)

                val language = f.language

                var name =
                    f.label
                        ?: language?.let { l ->
                            Locale(l).getDisplayLanguage(Locale("ar")).ifBlank { l }
                        }
                        ?: "مسار ${out.size + 1}"

                if (out.any { it.label == name }) {
                    name = "$name ${out.size + 1}"
                }

                out.add(AudioOption(name, g, i))
            }
        }

        return out
    }

    private fun serversAvailable(): Boolean =
        playingChannel?.let { sourcesOf(it).size > 1 } == true

    private fun audioAvailable(): Boolean =
        !playingWeb &&
                (playingChannel?.audios?.isNotEmpty() == true || collectEmbeddedAudio().size >= 2)

    private fun stripModeCount(): Int =
        1 + (if (serversAvailable()) 1 else 0) + (if (audioAvailable()) 1 else 0) + (if (!playingWeb) 1 else 0)

    // up / down: quality -> servers -> audio
    private fun cycleStrip() {

        val modes = mutableListOf(0)

        if (serversAvailable()) {
            modes.add(1)
        }

        if (audioAvailable()) {
            modes.add(2)
        }

        if (!playingWeb) {
            modes.add(3)
        }

        if (modes.size < 2) {
            return
        }

        val next = modes[(modes.indexOf(stripMode).coerceAtLeast(0) + 1) % modes.size]

        when (next) {
            0 -> openQualityStrip()
            1 -> openServerStrip()
            2 -> openAudioStrip()
            else -> openAspectStrip()
        }
    }

    private fun ensureAudioPlayer(): ExoPlayer {

        audioPlayer?.let { return it }

        val p = ExoPlayer.Builder(this).build()

        p.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(
                        this@MainActivity,
                        "تعذر تشغيل القناة الصوتية",
                        Toast.LENGTH_LONG
                    ).show()
                    stopExternalAudio()
                }
            }
        )

        audioPlayer = p

        return p
    }

    // the picture stays muted, the sound comes from the commentary stream
    private fun playExternalAudio(index: Int) {

        val item = playingChannel?.audios?.getOrNull(index) ?: return

        val p = ensureAudioPlayer()

        val source = parseSource(item.url)

        p.setMediaItem(createMediaItem(source.url, detectKind(source.url)))

        p.prepare()

        p.volume = 1f

        p.playWhenReady = true

        player.volume = 0f

        externalAudio = index
    }

    private fun stopExternalAudio() {

        externalAudio = -1

        audioPlayer?.let {
            try {
                it.stop()
                it.clearMediaItems()
            } catch (_: Exception) {
            }
        }

        if (::player.isInitialized) {
            player.volume = 1f
        }
    }

    private fun selectOriginalAudio() {

        stopExternalAudio()

        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .build()

        Toast.makeText(this, "الصوت: الأصلي", Toast.LENGTH_SHORT).show()
    }

    private fun selectEmbeddedAudio(option: AudioOption) {

        stopExternalAudio()

        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
                .build()

        Toast.makeText(this, "الصوت: ${option.label}", Toast.LENGTH_SHORT).show()
    }

    private fun selectExternalAudio(index: Int) {

        playExternalAudio(index)

        Toast.makeText(
            this,
            "الصوت: ${playingChannel?.audios?.getOrNull(index)?.name ?: ""}",
            Toast.LENGTH_SHORT
        ).show()
    }

    // the commentary can be ahead of / behind the picture
    private fun syncExternalAudio(deltaMs: Long) {

        val p = audioPlayer ?: return

        p.seekTo(maxOf(0L, p.currentPosition + deltaMs))

        Toast.makeText(
            this,
            if (deltaMs < 0) "تأخير الصوت ${-deltaMs / 1000} ث" else "تقديم الصوت ${deltaMs / 1000} ث",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openAudioStrip() {

        val channel = playingChannel ?: return

        if (!isFullscreen) {
            return
        }

        val embedded = collectEmbeddedAudio()

        if (playingWeb || (embedded.size < 2 && channel.audios.isEmpty())) {
            Toast.makeText(this, "لا توجد قنوات صوتية أخرى لهذه القناة", Toast.LENGTH_SHORT).show()
            return
        }

        closeChannelPanel(true)

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        stripMode = 2

        stripTitle.text = "الصوت"

        stripHint.text = if (stripModeCount() > 1) "▲▼ تبديل" else ""

        qualityStripChips.removeAllViews()

        var target: View? = null

        fun addChip(text: String, selected: Boolean, closeAfter: Boolean, action: () -> Unit) {

            val chip = menuItem((if (selected) "✓ " else "") + text, accentFor(0))

            chip.setPadding(dp(14), 0, dp(14), 0)

            chip.setOnClickListener {

                if (closeAfter) {
                    closeQualityStrip()
                } else {
                    mainHandler.removeCallbacks(hideQualityRunnable)
                    mainHandler.postDelayed(hideQualityRunnable, 5000)
                }

                action()
            }

            qualityStripChips.addView(
                chip,
                lp(wrap, dp(40)).apply { setMargins(dp(4), 0, dp(4), 0) }
            )

            if (selected || target == null) {
                target = chip
            }
        }

        if (embedded.size >= 2) {

            embedded.forEach { option ->
                addChip(option.label, externalAudio < 0 && option.group.isTrackSelected(option.index), true) {
                    selectEmbeddedAudio(option)
                }
            }

        } else {
            addChip("الأصلي", externalAudio < 0, true) { selectOriginalAudio() }
        }

        channel.audios.forEachIndexed { i, item ->
            addChip("🎙 " + item.name, externalAudio == i, true) { selectExternalAudio(i) }
        }

        if (externalAudio >= 0) {
            addChip("⏪", false, false) { syncExternalAudio(-1000) }
            addChip("⏩", false, false) { syncExternalAudio(1000) }
        }

        setVideoFocusable(false)

        qualityStrip.animate().cancel()

        qualityStrip.alpha = 0f

        qualityStrip.translationY = -dp(14).toFloat()

        qualityStrip.visibility = View.VISIBLE

        qualityStrip.animate().alpha(1f).translationY(0f).setDuration(180).start()

        qualityStrip.post {
            target?.requestFocus()
        }

        mainHandler.removeCallbacks(hideQualityRunnable)

        mainHandler.postDelayed(hideQualityRunnable, 5000)
    }

    // normal screen: the same choices as a menu
    private fun showAudioMenu() {

        val channel = playingChannel ?: return

        val embedded = collectEmbeddedAudio()

        if (embedded.size < 2 && channel.audios.isEmpty()) {
            Toast.makeText(this, "لا توجد قنوات صوتية أخرى لهذه القناة", Toast.LENGTH_SHORT).show()
            return
        }

        val items = mutableListOf<Pair<String, () -> Unit>>()

        if (embedded.size >= 2) {

            embedded.forEach { option ->
                items.add(
                    ((if (externalAudio < 0 && option.group.isTrackSelected(option.index)) "✓  " else "") +
                            option.label) to { selectEmbeddedAudio(option) }
                )
            }

        } else {
            items.add(((if (externalAudio < 0) "✓  " else "") + "الأصلي") to { selectOriginalAudio() })
        }

        channel.audios.forEachIndexed { i, item ->
            items.add(
                ((if (externalAudio == i) "✓  " else "") + "🎙  " + item.name) to
                        { selectExternalAudio(i) }
            )
        }

        if (externalAudio >= 0) {
            items.add("⏪  تأخير الصوت ثانية" to { syncExternalAudio(-1000) })
            items.add("⏩  تقديم الصوت ثانية" to { syncExternalAudio(1000) })
        }

        items.add("إلغاء" to {})

        showMenu("اختر الصوت", items)
    }

    // =========================
    // Match card -> its channel
    // =========================

    private fun channelForMatch(m: MatchItem): Channel? {

        val wanted = normName(m.channel)

        if (wanted.length < 4) {
            return null
        }

        val usable = channels.filter { sourcesOf(it).isNotEmpty() }

        return usable.firstOrNull { normName(it.name) == wanted }
            ?: usable.firstOrNull {
                val name = normName(it.name)
                name.contains(wanted) || wanted.contains(name)
            }
    }

    // selects its package, plays the channel and scrolls the list to it
    private fun openMatchChannel(m: MatchItem) {

        if (blocked || gated || multiActive || isFullscreen) {
            return
        }

        val channel = channelForMatch(m)

        if (channel == null) {
            Toast.makeText(this, "لا توجد قناة مرتبطة بهذه المباراة", Toast.LENGTH_SHORT).show()
            return
        }

        val pkg = packages.firstOrNull { it.id != FAV_ID && belongs(channel, it) }

        if (pkg != null && pkg.id != selectedPackage) {
            selectPackage(pkg.id, true)
        }

        playChannel(channel)

        val row = channelRows.firstOrNull { it.channel == channel }

        if (row != null) {

            channelScroll.post {
                channelScroll.smoothScrollTo(0, maxOf(0, row.card.top - dp(8)))
            }

            if (!phoneMode) {
                row.card.requestFocus()
            }
        }

        Toast.makeText(this, "▶  ${channel.name}", Toast.LENGTH_SHORT).show()
    }

    // =========================
    // In-app update
    // =========================

    private fun cleanOldUpdates() {

        try {
            File(cacheDir, "updates").deleteRecursively()
        } catch (_: Exception) {
        }
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls()

    private fun askInstallPermission() {

        noticeMessage.text = "فعّل خيار (السماح بتثبيت التطبيقات) لهذا التطبيق، ثم ارجع واضغط تحديث الآن."

        try {

            startActivity(
                Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "فعّل (مصادر غير معروفة) من إعدادات الأمان بالجهاز",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showUpdateNotice(version: Int, url: String, notes: String, sha: String, force: Boolean) {

        if (downloadingUpdate) {
            return
        }

        val key = "upd|$version"

        if (noticeOpen() && noticeKey == key) {
            return
        }

        val buttons = mutableListOf<Pair<String, () -> Unit>>()

        buttons.add("تحديث الآن" to { beginUpdate(url, sha, version) })

        if (!force) {
            buttons.add("لاحقاً" to {})
        }

        showNotice(
            key,
            "⬆️",
            if (force) "تحديث إجباري" else "تحديث جديد متوفر",
            notes.ifBlank { "يتوفر إصدار أحدث من التطبيق. سيتم تحميله وتثبيته من داخل التطبيق." },
            buttons,
            !force,
            true
        ) {}
    }

    private fun setUpdateProgress(percent: Int, text: String? = null) {

        noticeProgressTrack.visibility = View.VISIBLE

        (noticeProgressFill.layoutParams as LinearLayout.LayoutParams).weight = percent.toFloat()

        (noticeProgressRest.layoutParams as LinearLayout.LayoutParams).weight = (100 - percent).toFloat()

        noticeProgressTrack.requestLayout()

        noticeMessage.text = text ?: "جاري تحميل التحديث…  $percent%"
    }

    // the buttons of the update window (the window stays open)
    private fun setUpdateButtons(buttons: List<Pair<String, () -> Unit>>) {

        noticeButtons.removeAllViews()

        noticeButtons.visibility = View.VISIBLE

        var first: View? = null

        buttons.forEachIndexed { i, button ->

            val item = menuItem(button.first, accentFor(i))

            item.setOnClickListener { button.second() }

            noticeButtons.addView(
                item,
                lp(0, dp(48), 1f).apply { setMargins(dp(6), 0, dp(6), 0) }
            )

            if (first == null) {
                first = item
            }
        }

        first?.requestFocus()
    }

    private fun beginUpdate(url: String, sha: String, version: Int) {

        if (downloadingUpdate) {
            return
        }

        if (!canInstallPackages()) {
            askInstallPermission()
            return
        }

        downloadingUpdate = true

        noticeButtons.visibility = View.GONE

        setUpdateProgress(0)

        Thread {

            var connection: HttpURLConnection? = null

            try {

                val dir = File(cacheDir, "updates")

                dir.mkdirs()

                val file = File(dir, "niyati-$version.apk")

                if (file.exists()) {
                    file.delete()
                }

                connection = URL(url).openConnection() as HttpURLConnection

                connection.connectTimeout = 15000

                connection.readTimeout = 30000

                connection.instanceFollowRedirects = true

                connection.setRequestProperty("User-Agent", "NiyatiTV")

                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw Exception("HTTP ${connection.responseCode}")
                }

                val total = connection.contentLengthLong

                var done = 0L

                var lastReport = -1L

                connection.inputStream.use { input ->

                    file.outputStream().use { output ->

                        val buffer = ByteArray(64 * 1024)

                        var read = input.read(buffer)

                        while (read >= 0) {

                            output.write(buffer, 0, read)

                            done += read

                            val report = if (total > 0) done * 100 / total else done / (512 * 1024)

                            if (report != lastReport) {

                                lastReport = report

                                mainHandler.post {
                                    if (total > 0) {
                                        setUpdateProgress(report.toInt())
                                    } else {
                                        setUpdateProgress(0, "جاري تحميل التحديث…  ${done / 1048576} MB")
                                    }
                                }
                            }

                            read = input.read(buffer)
                        }
                    }
                }

                if (total > 0 && file.length() != total) {
                    throw Exception("size")
                }

                // optional security check: the SHA-256 written in Firebase must match
                if (sha.isNotBlank()) {

                    val digest = MessageDigest.getInstance("SHA-256")

                    file.inputStream().use { input ->

                        val buffer = ByteArray(64 * 1024)

                        var read = input.read(buffer)

                        while (read > 0) {
                            digest.update(buffer, 0, read)
                            read = input.read(buffer)
                        }
                    }

                    val hex = digest.digest().joinToString("") { "%02x".format(it) }

                    if (!hex.equals(sha.trim(), true)) {
                        throw Exception("sha")
                    }
                }

                mainHandler.post { onUpdateDownloaded(file) }

            } catch (_: Exception) {

                mainHandler.post { onUpdateFailed(url, sha, version) }

            } finally {

                connection?.disconnect()
            }

        }.start()
    }

    private fun onUpdateFailed(url: String, sha: String, version: Int) {

        downloadingUpdate = false

        noticeProgressTrack.visibility = View.GONE

        noticeMessage.text = "تعذر تحميل التحديث. تحقق من الاتصال وحاول مرة أخرى."

        val buttons = mutableListOf<Pair<String, () -> Unit>>()

        buttons.add("إعادة المحاولة" to { beginUpdate(url, sha, version) })

        if (noticeCancelable) {
            buttons.add("إلغاء" to { hideNotice() })
        }

        setUpdateButtons(buttons)
    }

    private fun onUpdateDownloaded(file: File) {

        downloadingUpdate = false

        noticeProgressTrack.visibility = View.GONE

        val problem = checkApk(file)

        noticeMessage.text =
            problem ?: "اكتمل التحميل. اضغط تثبيت لإكمال التحديث."

        val buttons = mutableListOf<Pair<String, () -> Unit>>()

        buttons.add((if (problem != null) "محاولة التثبيت" else "تثبيت الآن") to { installApk(file) })

        if (noticeCancelable) {
            buttons.add("إغلاق" to { hideNotice() })
        }

        setUpdateButtons(buttons)

        // a known problem is explained instead of the unclear "app not installed"
        if (problem == null) {
            installApk(file)
        }
    }

    private fun installApk(file: File) {

        try {

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            startActivity(intent)

        } catch (_: Exception) {

            Toast.makeText(this, "تعذر فتح شاشة التثبيت", Toast.LENGTH_LONG).show()
        }
    }

    // =========================
    // Activation codes
    // =========================

    private val dayMillis = 24L * 60 * 60 * 1000

    private fun serverNow(): Long = System.currentTimeMillis() + serverOffset

    private fun deviceId(): String {

        if (deviceIdCache.isNotEmpty()) {
            return deviceIdCache
        }

        var id = prefs.getString("device_id", "") ?: ""

        if (id.isEmpty()) {

            val raw =
                try {
                    android.provider.Settings.Secure.getString(
                        contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                } catch (_: Exception) {
                    null
                } ?: java.util.UUID.randomUUID().toString()

            val digest = MessageDigest.getInstance("SHA-256").digest((raw + "niyati").toByteArray())

            id = digest.joinToString("") { "%02x".format(it) }.take(12)

            prefs.edit().putString("device_id", id).apply()
        }

        deviceIdCache = id

        return id
    }

    private fun codeValid(): Boolean =
        activeCode.isNotEmpty() && (codeExpiresAt == 0L || serverNow() < codeExpiresAt)

    private fun subscriptionText(): String {

        if (activeCode.isEmpty()) {
            return "غير مفعّل"
        }

        if (codeExpiresAt == 0L) {
            return "غير محدود"
        }

        val days = (codeExpiresAt - serverNow() + dayMillis - 1) / dayMillis

        return if (days <= 0) "منتهي" else "متبقي $days يوم"
    }

    private fun updateGate() {

        if (!requireCode || codeValid()) {

            if (gated) {
                hideCodeOverlay(true)
            }

            return
        }

        if (!gated) {
            showCodeOverlay(gateMessage, true)
        }
    }

    private fun codeGateOpen(): Boolean =
        ::codeOverlay.isInitialized && codeOverlay.visibility == View.VISIBLE

    private fun buildCodeOverlay() {

        codeOverlay = FrameLayout(this)

        codeOverlay.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgTop, bgMid, bgBottom)
            )

        codeOverlay.isClickable = true

        codeOverlay.visibility = View.GONE

        val col = LedFrame()

        col.cornerDp = 28

        col.setColors(cyan, violet)

        col.orientation = LinearLayout.VERTICAL

        col.gravity = Gravity.CENTER_HORIZONTAL

        col.setPadding(dp(26), dp(24), dp(26), dp(22))

        col.background = fancyCardBackground(28)

        val icon = TextView(this)

        icon.text = "🔑"

        icon.textSize = 46f

        icon.gravity = Gravity.CENTER

        col.addView(icon, lp(wrap, wrap))

        col.addView(
            label("تفعيل الاشتراك", 24f, white, true, Gravity.CENTER),
            lp(matchParent, wrap).apply { topMargin = dp(8) }
        )

        codeMessage = label("", 15f, gray, false, Gravity.CENTER, 3)

        col.addView(codeMessage, lp(matchParent, wrap).apply { topMargin = dp(6) })

        codeInput = EditText(this)

        codeInput.setSingleLine(true)

        codeInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        codeInput.imeOptions = EditorInfo.IME_ACTION_DONE

        codeInput.hint = "XXXX-XXXX"

        codeInput.setHintTextColor(gray)

        codeInput.setTextColor(white)

        codeInput.textSize = 22f

        codeInput.gravity = Gravity.CENTER

        codeInput.typeface = Typeface.DEFAULT_BOLD

        codeInput.letterSpacing = 0.12f

        codeInput.layoutDirection = View.LAYOUT_DIRECTION_LTR

        codeInput.textDirection = View.TEXT_DIRECTION_LTR

        codeInput.background =
            GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.argb(40, 255, 255, 255))
                setStroke(dp(1), Color.argb(130, 255, 255, 255))
            }

        codeInput.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                activateCode(codeInput.text.toString())
                true
            } else {
                false
            }
        }

        col.addView(
            codeInput,
            lp(matchParent, dp(58)).apply { topMargin = dp(16) }
        )

        codeStatus = label("", 14f, Color.rgb(255, 120, 130), true, Gravity.CENTER, 2)

        col.addView(codeStatus, lp(matchParent, wrap).apply { topMargin = dp(10) })

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL

        row.gravity = Gravity.CENTER

        val activate = menuItem("تفعيل", accentFor(0))

        activate.setOnClickListener { activateCode(codeInput.text.toString()) }

        codeCancelBtn = menuItem("خروج", accentFor(2))

        row.addView(activate, lp(0, dp(50), 1f).apply { setMargins(dp(6), 0, dp(6), 0) })

        row.addView(codeCancelBtn, lp(0, dp(50), 1f).apply { setMargins(dp(6), 0, dp(6), 0) })

        col.addView(row, lp(matchParent, wrap).apply { topMargin = dp(14) })

        codeOverlay.addView(col, FrameLayout.LayoutParams(boxWidth(520), wrap, Gravity.CENTER))

        root.addView(codeOverlay, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    // required = true: the app is locked until a valid code is entered
    private fun showCodeOverlay(message: String, required: Boolean) {

        if (required) {

            gated = true

            // stop everything that is playing
            try {
                player.stop()
                player.clearMediaItems()
            } catch (_: Exception) {
            }

            mainHandler.removeCallbacks(retryRunnable)

            stopWeb()

            stopExternalAudio()

            flushWatch()

            if (multiActive) {
                exitMultiView(false)
            }

            if (isFullscreen) {
                exitFullscreen()
            }
        }

        if (noticeOpen()) {
            hideNotice()
        }

        if (menuOpen()) {
            closeMenu()
        }

        codeMessage.text = message

        codeStatus.text = ""

        codeInput.setText("")

        codeCancelBtn.text = if (required) "خروج" else "إلغاء"

        codeCancelBtn.setOnClickListener {
            if (required) finish() else hideCodeOverlay(false)
        }

        codeOverlay.visibility = View.VISIBLE

        codeInput.requestFocus()

        codeInput.postDelayed({ showKeyboard() }, 300)
    }

    private fun hideCodeOverlay(resume: Boolean) {

        gated = false

        hideKeyboard()

        codeOverlay.visibility = View.GONE

        if (resume) {
            playingChannel?.let { playChannel(it) }
        }
    }

    private fun showKeyboard() {

        try {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(codeInput, InputMethodManager.SHOW_IMPLICIT)
        } catch (_: Exception) {
        }
    }

    private fun hideKeyboard() {

        try {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(codeInput.windowToken, 0)
        } catch (_: Exception) {
        }
    }

    private fun activateCode(input: String) {

        val code = input.trim().uppercase(Locale.US).filter { it.isLetterOrDigit() || it == '-' || it == '_' }

        if (code.length < 4) {
            codeStatus.setTextColor(Color.rgb(255, 120, 130))
            codeStatus.text = "اكتب كوداً صحيحاً"
            return
        }

        codeStatus.setTextColor(gray)

        codeStatus.text = "جاري التحقق…"

        hideKeyboard()

        rootRef.child("codes").child(code).addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    evaluateCode(code, snapshot, true)
                }

                override fun onCancelled(error: DatabaseError) {
                    codeStatus.setTextColor(Color.rgb(255, 120, 130))
                    codeStatus.text = "تعذر الاتصال، تحقق من الإنترنت"
                }
            }
        )
    }

    private fun parseDateEnd(text: String): Long? =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(text)?.time?.plus(dayMillis - 1)
        } catch (_: Exception) {
            null
        }

    // codes/<CODE>: enabled, days or expires (yyyy-MM-dd) or expiresAt, maxDevices, devices/*
    private fun evaluateCode(code: String, snap: DataSnapshot, interactive: Boolean) {

        fun fail(message: String) {

            if (interactive) {

                codeStatus.setTextColor(Color.rgb(255, 120, 130))

                codeStatus.text = message

            } else {

                revokeCode(message)
            }
        }

        if (!snap.exists()) {
            fail("الكود غير صحيح")
            return
        }

        if (!snap.readBool("enabled", true)) {
            fail("هذا الكود موقوف")
            return
        }

        val now = serverNow()

        var expires: Long? = (snap.child("expiresAt").value as? Number)?.toLong()

        if (expires == null) {
            expires = snap.readString("expires")?.let { parseDateEnd(it) }
        }

        val days = snap.readInt("days", 0)

        // the days start counting at the first use
        if (expires == null && days > 0) {

            val newExpire = now + days * dayMillis

            expires = newExpire

            snap.ref.updateChildren(
                mapOf<String, Any>("activatedAt" to now, "expiresAt" to newExpire)
            )
        }

        if (expires != null && now > expires) {
            fail("انتهى اشتراك هذا الكود")
            return
        }

        val id = deviceId()

        val devices = snap.child("devices")

        if (!devices.hasChild(id) && devices.childrenCount >= snap.readInt("maxDevices", 1)) {
            fail("هذا الكود مستخدم على جهاز آخر")
            return
        }

        val seen = (devices.child(id).value as? Number)?.toLong() ?: 0L

        if (seen == 0L || now - seen > 3_600_000L) {
            snap.ref.child("devices").child(id).setValue(now)
        }

        // accepted
        activeCode = code

        codeExpiresAt = expires ?: 0L

        prefs.edit().putString("code", code).putLong("code_exp", codeExpiresAt).apply()

        if (interactive) {

            codeStatus.setTextColor(Color.rgb(80, 220, 150))

            codeStatus.text = "✔ تم التفعيل"

            attachCodeListener(code)

            Toast.makeText(this, "تم تفعيل الاشتراك: ${subscriptionText()}", Toast.LENGTH_LONG).show()

            mainHandler.postDelayed({
                if (codeValid()) {
                    hideCodeOverlay(true)
                }
            }, 600)
        }

        if (!expiryWarned && expires != null && expires - now < 3 * dayMillis) {

            expiryWarned = true

            Toast.makeText(this, "ينتهي اشتراكك قريباً: ${subscriptionText()}", Toast.LENGTH_LONG).show()
        }

        if (!interactive) {
            updateGate()
        }
    }

    private fun revokeCode(message: String) {

        detachCodeListener()

        activeCode = ""

        codeExpiresAt = 0L

        prefs.edit().remove("code").remove("code_exp").apply()

        gateMessage = message

        updateGate()
    }

    private fun attachCodeListener(code: String) {

        detachCodeListener()

        val ref = rootRef.child("codes").child(code)

        val listener =
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    evaluateCode(code, snapshot, false)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            }

        codeRef = ref

        codeListener = listener

        ref.addValueEventListener(listener)
    }

    private fun detachCodeListener() {

        codeListener?.let { codeRef?.removeEventListener(it) }

        codeListener = null

        codeRef = null
    }

    // =========================
    // Statistics
    // =========================

    private fun todayKey(): String =
        dayFormat.format(Date(serverNow()))

    private fun statKey(channel: Channel): String =
        channel.name.map { if (it in ".$#[]/") '_' else it }.joinToString("").take(80)

    // online viewers: /presence/<device> exists while the app is connected
    private fun startPresence() {

        try {

            database.getReference(".info/serverTimeOffset").addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        serverOffset = (snapshot.value as? Number)?.toLong() ?: 0L
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )

            val ref = rootRef.child("presence").child(deviceId())

            database.getReference(".info/connected").addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        if (snapshot.value == true) {

                            ref.onDisconnect().removeValue()

                            ref.setValue(
                                mapOf<String, Any>(
                                    "ts" to ServerValue.TIMESTAMP,
                                    "ch" to (playingChannel?.name ?: "")
                                )
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )

        } catch (_: Exception) {
        }
    }

    private fun updatePresenceChannel() {

        try {
            rootRef.child("presence").child(deviceId()).child("ch").setValue(playingChannel?.name ?: "")
        } catch (_: Exception) {
        }
    }

    private fun beginWatch(channel: Channel) {

        flushWatch()

        watchChannel = channel

        watchStart = SystemClock.elapsedRealtime()
    }

    // a watch of 30 seconds or more counts as one view
    private fun flushWatch() {

        val channel = watchChannel ?: return

        watchChannel = null

        val seconds = (SystemClock.elapsedRealtime() - watchStart) / 1000

        if (seconds < 30 || !online) {
            return
        }

        try {

            rootRef.child("stats").child("channels").child(statKey(channel)).updateChildren(
                mapOf<String, Any>(
                    "name" to channel.name,
                    "views" to ServerValue.increment(1),
                    "seconds" to ServerValue.increment(seconds)
                )
            )

            val day = rootRef.child("stats").child("daily").child(todayKey())

            day.child("views").setValue(ServerValue.increment(1))

            if (prefs.getString("stats_day", "") != todayKey()) {

                day.child("users").child(deviceId()).setValue(true)

                prefs.edit().putString("stats_day", todayKey()).apply()
            }

        } catch (_: Exception) {
        }
    }

    private fun readOnce(ref: DatabaseReference, done: (DataSnapshot?) -> Unit) {

        ref.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    done(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    done(null)
                }
            }
        )
    }

    // only for the devices listed in settings/adminDevices
    private fun showStatsScreen() {

        Toast.makeText(this, "جاري تحميل الإحصائيات…", Toast.LENGTH_SHORT).show()

        readOnce(rootRef.child("presence")) { presence ->

            readOnce(rootRef.child("stats").child("channels")) { channelStats ->

                readOnce(rootRef.child("stats").child("daily").child(todayKey()).child("users")) { users ->

                    val lines = mutableListOf<String>()

                    lines.add("🟢 المتصلون الآن: ${presence?.childrenCount ?: 0}")

                    lines.add("👥 مستخدمو اليوم: ${users?.childrenCount ?: 0}")

                    val top =
                        (channelStats?.children ?: emptyList())
                            .map {
                                Triple(
                                    it.readString("name") ?: (it.key ?: "-"),
                                    it.readInt("views", 0),
                                    it.readInt("seconds", 0)
                                )
                            }
                            .sortedByDescending { it.second }
                            .take(6)

                    lines.add("أكثر القنوات مشاهدة:")

                    if (top.isEmpty()) {
                        lines.add("لا توجد بيانات بعد")
                    }

                    top.forEachIndexed { i, t ->
                        lines.add("${i + 1}. ${t.first} — ${t.second} مشاهدة · ${t.third / 60} د")
                    }

                    showNotice(
                        "stats",
                        "📊",
                        "الإحصائيات",
                        lines.joinToString("\n"),
                        listOf("حسناً" to {})
                    ) {}
                }
            }
        }
    }

    // =========================
    // Stream stability
    // =========================

    private fun bufferModeName(): String =
        when (prefs.getInt("buffer_mode", 1)) {
            0 -> "سريع"
            2 -> "مستقر"
            else -> "متوازن"
        }

    // fast -> balanced -> stable (the player is rebuilt, the app restarts the screen)
    private fun cycleBufferMode() {

        val next = (prefs.getInt("buffer_mode", 1) + 1) % 3

        prefs.edit()
            .putInt("buffer_mode", next)
            .putBoolean("skip_splash_once", true)
            .apply()

        recreate()
    }

    // the server ended / cut the live stream: load it again at once, without a message
    private fun reconnectLiveStream() {

        val channel = playingChannel ?: return

        if (retryCount >= 10) {

            finalPlaybackError = true

            livePill.visibility = View.GONE

            showStatus("انقطع البث", "تعذر إعادة الاتصال" + serverHint(), true)

            return
        }

        retryCount++

        // the first tries are silent: the last picture stays on the screen
        val silent = retryCount <= 3

        if (!silent) {
            showStatus("جاري إعادة الاتصال…", "المحاولة $retryCount من 10", false)
        }

        mainHandler.removeCallbacks(retryRunnable)

        val token = playToken

        mainHandler.postDelayed(
            {
                if (token == playToken && !finalPlaybackError && playingChannel == channel && !playingWeb) {
                    // a fresh start of the same link (a new playlist / token)
                    startCurrentAttempt(channel)
                }
            },
            if (silent) 100L else minOf(1500L * retryCount, 6000L)
        )
    }

    // watchdog: buffering for 12 seconds = the connection is dead, start again
    private val stallRunnable = Runnable {

        val channel = playingChannel

        if (
            channel != null &&
            !finalPlaybackError &&
            !playingWeb &&
            !multiActive &&
            player.playbackState == Player.STATE_BUFFERING
        ) {
            reconnectLiveStream()
        }
    }

    // =========================
    // Shared card style + player ring
    // =========================

    // the background of every card (notice, menu, code, maintenance)
    private fun fancyCardBackground(radiusDp: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                withAlpha(mix(violet, bgTop, 0.32f), 246),
                withAlpha(mix(bgMid, bgTop, 0.7f), 250),
                withAlpha(bgBottom, 250)
            )
        ).apply {
            cornerRadius = dp(radiusDp).toFloat()
        }

    // mode: 0 = loading (turning), 1 = error (red), 2 = idle (quiet ring)
    private inner class SpinnerRing : View(this@MainActivity) {

        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = dp(4).toFloat()
                strokeCap = Paint.Cap.ROUND
            }

        private val rect = RectF()
        private var angle = 0f
        private var mode = 0
        private var anim: ValueAnimator? = null

        fun setMode(value: Int) {
            mode = value
            refreshAnim()
            invalidate()
        }

        private fun refreshAnim() {

            if (mode == 0 && isShown && isAttachedToWindow) {

                if (anim == null) {

                    val a = ValueAnimator.ofFloat(0f, 360f)

                    a.duration = 1100

                    a.repeatCount = ValueAnimator.INFINITE

                    a.interpolator = android.view.animation.LinearInterpolator()

                    a.addUpdateListener {
                        angle = it.animatedValue as Float
                        invalidate()
                    }

                    anim = a

                    a.start()
                }

            } else {
                anim?.cancel()
                anim = null
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            refreshAnim()
        }

        override fun onDetachedFromWindow() {
            anim?.cancel()
            anim = null
            super.onDetachedFromWindow()
        }

        override fun onVisibilityChanged(changedView: View, visibility: Int) {
            super.onVisibilityChanged(changedView, visibility)
            refreshAnim()
        }

        override fun onDraw(canvas: Canvas) {

            val inset = paint.strokeWidth

            rect.set(inset, inset, width - inset, height - inset)

            paint.color =
                when (mode) {
                    1 -> Color.argb(190, 255, 82, 82)
                    2 -> Color.argb(46, 255, 255, 255)
                    else -> Color.argb(34, 255, 255, 255)
                }

            canvas.drawOval(rect, paint)

            if (mode == 0) {

                paint.color = cyan

                canvas.drawArc(rect, angle, 110f, false, paint)

                paint.color = violet

                canvas.drawArc(rect, angle + 180f, 70f, false, paint)
            }
        }
    }

    // =========================
    // Screen size (aspect ratio)
    // =========================

    private val aspectNames = listOf("أصلي", "تمديد", "تكبير")

    private fun aspectName(): String = aspectNames[aspectMode]

    private fun applyAspect() {

        playerView.resizeMode =
            when (aspectMode) {
                1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
    }

    private fun setAspect(mode: Int) {

        aspectMode = mode

        prefs.edit().putInt("aspect", mode).apply()

        applyAspect()
    }

    // original -> stretch -> zoom (remote zoom key)
    private fun cycleAspect() {

        if (playingWeb) {
            return
        }

        setAspect((aspectMode + 1) % 3)

        Toast.makeText(this, "أبعاد الشاشة: ${aspectName()}", Toast.LENGTH_SHORT).show()
    }

    private fun showAspectMenu() {

        val items = mutableListOf<Pair<String, () -> Unit>>()

        aspectNames.forEachIndexed { i, name ->
            items.add(
                ((if (i == aspectMode) "✓  " else "") + name) to {
                    setAspect(i)
                    Toast.makeText(this, "أبعاد الشاشة: $name", Toast.LENGTH_SHORT).show()
                }
            )
        }

        items.add("إلغاء" to {})

        showMenu("أبعاد الشاشة", items)
    }

    // fullscreen: the chips stay open so the effect can be seen at once
    private fun openAspectStrip() {

        if (!isFullscreen) {
            return
        }

        if (playingWeb) {
            Toast.makeText(this, "الأبعاد غير متاحة لقنوات يوتيوب / فيسبوك", Toast.LENGTH_SHORT).show()
            return
        }

        closeChannelPanel(true)

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        stripMode = 3

        stripTitle.text = "الأبعاد"

        stripHint.text = if (stripModeCount() > 1) "▲▼ تبديل" else ""

        qualityStripChips.removeAllViews()

        val chips = mutableListOf<TextView>()

        var target: View? = null

        aspectNames.forEachIndexed { i, name ->

            val chip = menuItem((if (i == aspectMode) "✓ " else "") + name, accentFor(0))

            chip.setPadding(dp(16), 0, dp(16), 0)

            chip.setOnClickListener {

                setAspect(i)

                chips.forEachIndexed { j, c ->
                    c.text = (if (j == aspectMode) "✓ " else "") + aspectNames[j]
                }

                mainHandler.removeCallbacks(hideQualityRunnable)

                mainHandler.postDelayed(hideQualityRunnable, 5000)
            }

            chips.add(chip)

            qualityStripChips.addView(
                chip,
                lp(wrap, dp(40)).apply { setMargins(dp(4), 0, dp(4), 0) }
            )

            if (i == aspectMode) {
                target = chip
            }
        }

        setVideoFocusable(false)

        qualityStrip.animate().cancel()

        qualityStrip.alpha = 0f

        qualityStrip.translationY = -dp(14).toFloat()

        qualityStrip.visibility = View.VISIBLE

        qualityStrip.animate().alpha(1f).translationY(0f).setDuration(180).start()

        qualityStrip.post {
            target?.requestFocus()
        }

        mainHandler.removeCallbacks(hideQualityRunnable)

        mainHandler.postDelayed(hideQualityRunnable, 5000)
    }

    // =========================
    // Client names (User-Agent)
    // =========================

    private val altUserAgents =
        listOf("VLC/3.0.18 LibVLC/3.0.18", "Lavf/60.3.100", "IPTVSmartersPro/1.1.1")

    // the channel's own User-Agent (or the default), then common player names
    private fun uaOptions(channel: Channel): List<String> =
        (listOf(channel.userAgent.ifBlank { defaultUserAgent }) + altUserAgents).distinct()

    private fun savedUaIndex(channel: Channel): Int {

        val saved = prefs.getInt("ua_" + channelKey(channel), 0)

        return if (saved in uaOptions(channel).indices) saved else 0
    }

    // light mode = no running lights / glow (helps weak TVs to play smoothly)
    private fun toggleLightMode() {

        prefs.edit()
            .putBoolean("light_mode", !lightMode)
            .putBoolean("skip_splash_once", true)
            .apply()

        recreate()
    }

    // =========================
    // Version + signature (to find why an update does not install)
    // =========================

    private fun versionLabel(): String =
        try {
            "${packageManager.getPackageInfo(packageName, 0).versionName} (${currentVersionCode()})"
        } catch (_: Exception) {
            "?"
        }

    // first bytes of the SHA-256 of the signing certificate: must be the same in every build
    @Suppress("DEPRECATION")
    private fun signatureFingerprint(): String =
        try {

            val signature =
                if (Build.VERSION.SDK_INT >= 28) {
                    packageManager
                        .getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                        .signingInfo
                        ?.apkContentsSigners
                        ?.firstOrNull()
                } else {
                    packageManager
                        .getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                        .signatures
                        ?.firstOrNull()
                }

            if (signature == null) {
                "?"
            } else {
                MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())
                    .take(4)
                    .joinToString(":") { "%02X".format(it) }
            }

        } catch (_: Exception) {
            "?"
        }

    // =========================
    // Reports: "this channel does not work"
    // =========================

    private fun showReportMenu() {

        val channel = playingChannel ?: return

        val items = mutableListOf<Pair<String, () -> Unit>>()

        for (reason in listOf("لا يوجد بث", "الصورة تتقطع", "لا يوجد صوت", "القناة غير صحيحة")) {
            items.add(reason to { reportChannel(channel, reason, false) })
        }

        items.add("إلغاء" to {})

        showMenu("ما المشكلة؟", items)
    }

    // /reports/<channel>: count (viewers), auto (the app itself could not play it), reason, last
    private fun reportChannel(channel: Channel, reason: String, auto: Boolean) {

        val key = statKey(channel)

        val prefKey = (if (auto) "arep_" else "rep_") + key

        val now = System.currentTimeMillis()

        // not more than one report per channel every 10 minutes (30 for the automatic ones)
        if (now - prefs.getLong(prefKey, 0L) < (if (auto) 30 else 10) * 60_000L) {

            if (!auto) {
                Toast.makeText(this, "تم إرسال بلاغك قبل قليل، شكراً", Toast.LENGTH_SHORT).show()
            }

            return
        }

        prefs.edit().putLong(prefKey, now).apply()

        try {

            rootRef.child("reports").child(key).updateChildren(
                mapOf<String, Any>(
                    "name" to channel.name,
                    (if (auto) "auto" else "count") to ServerValue.increment(1),
                    "last" to ServerValue.TIMESTAMP,
                    "reason" to reason
                )
            )

        } catch (_: Exception) {
        }

        if (!auto) {
            Toast.makeText(this, "شكراً، تم إرسال البلاغ", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================
    // TV guide (EPG): /epg/<epgId>/<startMillis> = { e: endMillis, t: title }
    // =========================

    private val epgRunnable = object : Runnable {

        override fun run() {
            refreshMatchViews()
            mainHandler.postDelayed(this, 60_000L)
        }
    }

    private fun parseEpg(node: DataSnapshot?): Map<String, List<Program>> {

        val result = HashMap<String, List<Program>>()

        for (channelNode in node?.children ?: emptyList()) {

            val key = channelNode.key ?: continue

            val list = mutableListOf<Program>()

            for (p in channelNode.children) {

                val start: Long =
                    p.key?.toLongOrNull()
                        ?: (p.child("s").value as? Number)?.toLong()
                        ?: continue

                val end = (p.child("e").value as? Number)?.toLong() ?: continue

                val title = p.child("t").value?.toString() ?: continue

                list.add(Program(start, end, title))
            }

            result[key] = list.sortedBy { it.start }
        }

        return result
    }

    private fun updateEpg(node: DataSnapshot?) {

        val parsed = parseEpg(node)

        if (parsed != epgMap) {
            epgMap = parsed
            refreshMatchViews()
        }
    }

    private fun epgKeyOf(channel: Channel): String =
        channel.epgId.ifBlank { statKey(channel) }

    private fun epgNow(channel: Channel): Program? {

        val now = serverNow()

        return epgMap[epgKeyOf(channel)]?.firstOrNull { it.start <= now && now < it.end }
    }

    private fun epgNext(channel: Channel): Program? {

        val now = serverNow()

        return epgMap[epgKeyOf(channel)]?.firstOrNull { it.start > now }
    }

    private fun epgNowText(channel: Channel): String? =
        epgNow(channel)?.let { "الآن: ${it.title}" }

    // "Now: X  •  Next: Y 21:00"
    private fun epgLine(channel: Channel): String? {

        val now = epgNow(channel)

        val next = epgNext(channel)

        if (now == null && next == null) {
            return null
        }

        val parts = mutableListOf<String>()

        if (now != null) {
            parts.add("الآن: ${now.title}")
        }

        if (next != null) {
            parts.add("التالي: ${next.title} ${timeFormat.format(Date(next.start))}")
        }

        return parts.joinToString("  •  ")
    }

    // =========================
    // Sections: live / movies / series
    // =========================

    private fun genresOf(item: VodItem): List<String> =
        item.genre.split(",", "،").map { it.trim() }.filter { it.isNotEmpty() }

    private fun parseVod(node: DataSnapshot?, series: Boolean): List<VodItem> {

        val result = mutableListOf<VodItem>()

        for (c in node?.children ?: emptyList()) {

            val id = c.key ?: continue

            if (!c.readBool("enabled", true)) {
                continue
            }

            val title = c.readString("title") ?: continue

            val episodes = mutableListOf<Episode>()

            for (e in c.child("episodes").children) {

                val url = e.readString("url") ?: continue

                val number = e.readInt("e", episodes.size + 1)

                episodes.add(
                    Episode(
                        season = e.readInt("s", 1),
                        number = number,
                        title = e.readString("title") ?: "الحلقة $number",
                        url = url,
                        duration = e.readInt("duration", 0),
                        thumb = e.readString("thumb") ?: "",
                        overview = e.readString("overview") ?: ""
                    )
                )
            }

            result.add(
                VodItem(
                    id = id,
                    series = series,
                    title = title,
                    poster = c.readString("poster") ?: "",
                    backdrop = c.readString("backdrop") ?: "",
                    url = c.readString("url") ?: "",
                    genre = c.readString("genre") ?: "",
                    year = c.readString("year") ?: "",
                    rating = c.readString("rating") ?: "",
                    duration = c.readInt("duration", 0),
                    overview = c.readString("overview") ?: "",
                    isNew = c.readBool("new", false),
                    featured = c.readBool("featured", false),
                    order = c.readInt("order", 999),
                    episodes = episodes.sortedWith(compareBy({ it.season }, { it.number }))
                )
            )
        }

        return result.sortedBy { it.order }
    }

    private fun updateVod(moviesNode: DataSnapshot?, seriesNode: DataSnapshot?) {

        val newMovies = parseVod(moviesNode, false)

        val newSeries = parseVod(seriesNode, true)

        if (newMovies != movies || newSeries != seriesList) {

            movies = newMovies

            seriesList = newSeries

            if (currentSection != 0 && ::libContent.isInitialized) {
                renderLibrary()
            }
        }
    }

    // ---- section bar ----

    private fun isInSectionBar(view: View?): Boolean {

        val bar = sectionBarView ?: return false

        var v: View? = view

        while (v != null) {
            if (v === bar) {
                return true
            }
            v = v.parent as? View
        }

        return false
    }

    private fun buildSectionBar(): View {

        val holder = LinearLayout(this)

        holder.orientation = LinearLayout.HORIZONTAL

        holder.gravity = Gravity.CENTER

        holder.setPadding(dp(18), dp(2), dp(18), dp(8))

        val pill = LinearLayout(this)

        pill.orientation = LinearLayout.HORIZONTAL

        pill.setPadding(dp(5), dp(4), dp(5), dp(4))

        pill.background =
            GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.argb(26, 255, 255, 255))
                setStroke(dp(1), Color.argb(40, 255, 255, 255))
            }

        sectionTabViews.clear()

        val defs =
            listOf(
                Triple("مباشر", "M3 6h18v11H3zM8 21h8", 0),
                Triple("أفلام", "M3 5h18v14H3zM7 5v14M17 5v14M3 10h4M3 14h4M17 10h4M17 14h4", 1),
                Triple("مسلسلات", "M5 8h14v12H5zM8 5h8M10.5 2.5h3", 2)
            )

        defs.forEach { (name, path, index) ->

            val tab = LinearLayout(this)

            tab.orientation = LinearLayout.HORIZONTAL

            tab.gravity = Gravity.CENTER

            tab.setPadding(dp(22), 0, dp(22), 0)

            tab.isClickable = true

            tab.isFocusable = true

            tab.isFocusableInTouchMode = !phoneMode

            val icon = LineIconView()

            icon.setIcon(path)

            tab.addView(icon, lp(dp(20), dp(20)))

            tab.addView(
                label(name, 16f, white, true, Gravity.CENTER),
                lp(wrap, wrap).apply { setMargins(dp(8), 0, 0, 0) }
            )

            tab.tag = index

            tab.setOnClickListener { switchSection(index) }

            tab.setOnFocusChangeListener { _, _ -> refreshTabs() }

            pill.addView(tab, lp(wrap, dp(44)))

            sectionTabViews.add(tab)
        }

        holder.addView(pill, lp(wrap, wrap))

        sectionBarView = holder

        refreshTabs()

        return holder
    }

    private fun refreshTabs() {

        sectionTabViews.forEach { tab ->

            val selected = (tab.tag as Int) == currentSection

            val focused = tab.hasFocus()

            tab.background =
                when {

                    selected ->
                        GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            intArrayOf(cyan, violet)
                        ).apply {
                            cornerRadius = dp(22).toFloat()
                            if (focused) {
                                setStroke(dp(2), Color.WHITE)
                            }
                        }

                    focused ->
                        GradientDrawable().apply {
                            cornerRadius = dp(22).toFloat()
                            setColor(Color.argb(50, 255, 255, 255))
                            setStroke(dp(2), Color.WHITE)
                        }

                    else -> null
                }
        }
    }

    private fun switchSection(index: Int) {

        if (index == currentSection || isFullscreen || multiActive || blocked || gated) {
            return
        }

        currentSection = index

        refreshTabs()

        liveHost.visibility = if (index == 0) View.VISIBLE else View.GONE

        vodHost.visibility = if (index == 0) View.GONE else View.VISIBLE

        if (index == 0) {

            if (playingChannel != null && !playingWeb && vodPlay == null) {
                player.playWhenReady = true
            }

            audioPlayer?.playWhenReady = externalAudio >= 0

            focusChannelsColumn()

        } else {

            cancelPreview()

            // the live picture is paused while the library is open
            try {
                player.playWhenReady = false
            } catch (_: Exception) {
            }

            audioPlayer?.playWhenReady = false

            vodGenre = ""

            renderLibrary()

            focusLibrary()
        }
    }

    private fun focusLibrary() {

        libScroll.post {
            libFirstFocus?.requestFocus()
        }
    }

    // ---- images ----

    private fun downloadImage(urlString: String, maxPx: Int): Bitmap? {

        var connection: HttpURLConnection? = null

        return try {

            connection = URL(urlString).openConnection() as HttpURLConnection

            connection.connectTimeout = 8000

            connection.readTimeout = 12000

            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                return null
            }

            val bytes = connection.inputStream.use { it.readBytes() }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sample = 1

            while (bounds.outWidth / sample > maxPx || bounds.outHeight / sample > maxPx) {
                sample *= 2
            }

            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        } catch (_: Exception) {

            null

        } finally {

            connection?.disconnect()
        }
    }

    private fun loadImage(view: ImageView, url: String, maxPx: Int) {

        if (!url.startsWith("http", ignoreCase = true)) {
            return
        }

        val key = "$maxPx|$url"

        posterCache.get(key)?.let {
            view.setImageBitmap(it)
            return
        }

        view.tag = key

        try {

            ioExecutor.execute {

                val bitmap = downloadImage(url, maxPx)

                if (bitmap != null) {

                    posterCache.put(key, bitmap)

                    mainHandler.post {
                        if (view.tag == key) {
                            view.setImageBitmap(bitmap)
                        }
                    }
                }
            }

        } catch (_: Exception) {
        }
    }

    private fun placeholderFor(title: String): GradientDrawable {

        val accent = accentFor(Math.abs(title.hashCode()) % 6)

        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(mix(accent.start, bgTop, 0.45f), mix(accent.end, bgTop, 0.35f))
        )
    }

    private fun roundOutline(view: View, radiusDp: Int) {

        view.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, dp(radiusDp).toFloat())
                }
            }

        view.clipToOutline = true
    }

    // ---- progress (resume) ----

    private fun vodKey(item: VodItem, ep: Episode?): String =
        if (ep == null) item.id else "${item.id}_s${ep.season}e${ep.number}"

    private fun savedPos(key: String): Long = prefs.getLong("vp_$key", 0L)

    private fun progressOf(key: String): Float {

        val duration = prefs.getLong("vd_$key", 0L)

        return if (duration > 0) (savedPos(key).toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    private fun isFinished(key: String): Boolean = progressOf(key) >= 0.92f

    private fun nextEpisode(item: VodItem, ep: Episode): Episode? =
        item.episodes.getOrNull(item.episodes.indexOf(ep) + 1)

    // what "continue" means: the movie / episode where the viewer stopped, or the next episode
    private fun resumeTarget(item: VodItem): Pair<Episode?, Long>? {

        if (!item.series) {

            val pos = savedPos(item.id)

            return if (pos > 15000 && !isFinished(item.id)) Pair(null, pos) else null
        }

        val last = prefs.getString("lastEp_${item.id}", null) ?: return null

        val parts = last.split("x")

        val season = parts.getOrNull(0)?.toIntOrNull() ?: return null

        val number = parts.getOrNull(1)?.toIntOrNull() ?: return null

        val episode =
            item.episodes.firstOrNull { it.season == season && it.number == number } ?: return null

        val key = vodKey(item, episode)

        if (savedPos(key) > 15000 && !isFinished(key)) {
            return Pair(episode, savedPos(key))
        }

        val next = nextEpisode(item, episode) ?: return null

        return Pair(next, 0L)
    }

    // ---- library page ----

    private fun buildVodHost() {

        vodHost = FrameLayout(this)

        vodHost.visibility = View.GONE

        normalScreen.addView(vodHost, lp(matchParent, 0, 1f))

        val page = LinearLayout(this)

        page.orientation = if (phoneMode) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        val side = if (phoneMode) 8 else 18

        page.setPadding(dp(side), dp(4), dp(side), dp(if (phoneMode) 4 else 16))

        libGenres = createList()

        libGenreHeader = SectionHeader("التصنيفات", "GENRES", accentFor(0))

        if (phoneMode) {

            val chips = HorizontalScrollView(this)

            chips.isHorizontalScrollBarEnabled = false

            libGenres.orientation = LinearLayout.HORIZONTAL

            libGenres.setPadding(dp(4), dp(4), dp(4), dp(4))

            chips.addView(libGenres)

            page.addView(chips, lp(matchParent, dp(54)))

        } else {

            val panel = createPanel()

            panel.addView(libGenreHeader, lp(matchParent, dp(64)))

            val genreScroll = createScroll()

            genreScroll.addView(libGenres)

            panel.addView(genreScroll, lp(matchParent, 0, 1f))

            page.addView(panel, panelParams(0.24f))
        }

        libScroll = createScroll()

        libContent = LinearLayout(this)

        libContent.orientation = LinearLayout.VERTICAL

        libContent.setPadding(dp(if (phoneMode) 4 else 10), dp(6), dp(if (phoneMode) 4 else 10), dp(18))

        libScroll.addView(libContent)

        if (phoneMode) {

            page.addView(libScroll, lp(matchParent, 0, 1f))

        } else {

            val panel = createPanel()

            panel.addView(libScroll, lp(matchParent, 0, 1f))

            page.addView(panel, panelParams(0.76f))
        }

        vodHost.addView(page, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private fun genreRow(name: String, count: Int, selected: Boolean): View {

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL

        row.gravity = Gravity.CENTER_VERTICAL

        row.setPadding(dp(16), 0, dp(16), 0)

        row.isClickable = true

        row.isFocusable = true

        row.isFocusableInTouchMode = !phoneMode

        row.tag = name

        row.addView(label("$count", 13f, if (selected) white else gray, true, Gravity.LEFT), lp(wrap, wrap))

        row.addView(
            label(name, if (phoneMode) 15f else 17f, white, true),
            if (phoneMode) {
                lp(wrap, wrap).apply { setMargins(dp(10), 0, 0, 0) }
            } else {
                lp(0, wrap, 1f).apply { setMargins(dp(10), 0, 0, 0) }
            }
        )

        row.layoutParams =
            if (phoneMode) {
                lp(wrap, dp(40)).apply { setMargins(dp(4), 0, dp(4), 0) }
            } else {
                lp(matchParent, dp(54)).apply { setMargins(0, dp(4), 0, dp(4)) }
            }

        fun style(focused: Boolean) {

            row.background =
                when {

                    focused ->
                        GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            intArrayOf(mix(cyan, bgMid, 0.55f), mix(violet, bgMid, 0.55f))
                        ).apply {
                            cornerRadius = dp(18).toFloat()
                            setStroke(dp(2), Color.WHITE)
                        }

                    selected ->
                        GradientDrawable().apply {
                            cornerRadius = dp(18).toFloat()
                            setColor(withAlpha(cyan, 60))
                            setStroke(dp(1), withAlpha(cyan, 170))
                        }

                    else ->
                        GradientDrawable().apply {
                            cornerRadius = dp(18).toFloat()
                            setColor(Color.argb(22, 255, 255, 255))
                            setStroke(dp(1), Color.argb(28, 255, 255, 255))
                        }
                }
        }

        style(false)

        row.setOnFocusChangeListener { _, focused -> style(focused) }

        row.setOnClickListener {

            vodGenre = if (name == "الكل") "" else name

            renderLibrary()

            for (i in 0 until libGenres.childCount) {
                val child = libGenres.getChildAt(i)
                if (child.tag == name) {
                    child.requestFocus()
                }
            }
        }

        return row
    }

    private fun sectionTitle(text: String): TextView {

        val title = label(text, if (phoneMode) 17f else 20f, white, true)

        title.setPadding(dp(8), dp(16), dp(8), dp(8))

        return title
    }

    private fun actionButton(text: String, primary: Boolean, onClick: () -> Unit): TextView {

        val button = label(text, if (phoneMode) 14f else 16f, if (primary) Color.rgb(8, 14, 30) else white, true, Gravity.CENTER)

        button.setPadding(dp(22), 0, dp(22), 0)

        button.isClickable = true

        button.isFocusable = true

        button.isFocusableInTouchMode = !phoneMode

        fun style(focused: Boolean) {

            button.background =
                GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    if (primary) intArrayOf(cyan, mix(cyan, violet, 0.5f)) else intArrayOf(Color.argb(60, 255, 255, 255), Color.argb(40, 255, 255, 255))
                ).apply {
                    cornerRadius = dp(22).toFloat()
                    setStroke(dp(if (focused) 3 else 1), if (focused) Color.WHITE else Color.argb(70, 255, 255, 255))
                }

            button.animate().scaleX(if (focused) 1.06f else 1f).scaleY(if (focused) 1.06f else 1f).setDuration(120).start()
        }

        style(false)

        button.setOnFocusChangeListener { _, focused -> style(focused) }

        button.setOnClickListener { onClick() }

        return button
    }

    private fun posterView(
        item: VodItem,
        wDp: Int,
        hDp: Int,
        badge: String,
        progress: Float,
        onClick: () -> Unit
    ): FrameLayout {

        val frame = FrameLayout(this)

        frame.isClickable = true

        frame.isFocusable = true

        frame.isFocusableInTouchMode = !phoneMode

        frame.background = placeholderFor(item.title)

        roundOutline(frame, 14)

        val image = ImageView(this)

        image.scaleType = ImageView.ScaleType.CENTER_CROP

        frame.addView(image, FrameLayout.LayoutParams(matchParent, matchParent))

        loadImage(image, item.poster, 360)

        val scrim = LinearLayout(this)

        scrim.orientation = LinearLayout.VERTICAL

        scrim.setPadding(dp(8), dp(26), dp(8), dp(if (progress > 0f) 12 else 8))

        scrim.background =
            GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(Color.argb(235, 4, 6, 18), Color.argb(0, 4, 6, 18))
            )

        scrim.addView(label(item.title, if (phoneMode) 11.5f else 12.5f, white, true, Gravity.CENTER, 2), lp(matchParent, wrap))

        if (badge.isNotEmpty()) {
            scrim.addView(label(badge, 10.5f, cyan, true, Gravity.CENTER), lp(matchParent, wrap))
        }

        frame.addView(scrim, FrameLayout.LayoutParams(matchParent, wrap, Gravity.BOTTOM))

        if (item.rating.isNotBlank()) {

            val rating = label("★ ${item.rating}", 11f, gold, true, Gravity.CENTER)

            rating.setPadding(dp(7), dp(1), dp(7), dp(1))

            rating.background =
                GradientDrawable().apply {
                    cornerRadius = dp(9).toFloat()
                    setColor(Color.argb(190, 4, 6, 18))
                }

            frame.addView(
                rating,
                FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.LEFT).apply { setMargins(dp(7), dp(7), 0, 0) }
            )
        }

        if (item.isNew) {

            val tag = label("جديد", 10.5f, Color.rgb(40, 24, 0), true, Gravity.CENTER)

            tag.setPadding(dp(7), dp(1), dp(7), dp(1))

            tag.background =
                GradientDrawable().apply {
                    cornerRadius = dp(9).toFloat()
                    setColor(gold)
                }

            frame.addView(
                tag,
                FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.RIGHT).apply { setMargins(0, dp(7), dp(7), 0) }
            )
        }

        if (progress > 0f) {

            val track = LinearLayout(this)

            track.orientation = LinearLayout.HORIZONTAL

            track.setBackgroundColor(Color.argb(120, 255, 255, 255))

            val fill = View(this)

            fill.setBackgroundColor(cyan)

            track.addView(fill, LinearLayout.LayoutParams(0, matchParent, progress.coerceIn(0.03f, 1f)))

            track.addView(View(this), LinearLayout.LayoutParams(0, matchParent, 1f - progress.coerceIn(0.03f, 1f)))

            frame.addView(track, FrameLayout.LayoutParams(matchParent, dp(4), Gravity.BOTTOM))
        }

        fun style(focused: Boolean) {

            frame.foreground =
                if (focused) {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setStroke(dp(3), Color.WHITE)
                    }
                } else {
                    null
                }

            frame.animate()
                .scaleX(if (focused) 1.07f else 1f)
                .scaleY(if (focused) 1.07f else 1f)
                .setDuration(130)
                .start()

            frame.elevation = dp(if (focused) 14 else 2).toFloat()

            if (Build.VERSION.SDK_INT >= 28) {
                frame.outlineSpotShadowColor = if (focused) cyan else Color.BLACK
            }
        }

        frame.setOnFocusChangeListener { _, focused -> style(focused) }

        frame.setOnClickListener { onClick() }

        frame.layoutParams = lp(dp(wDp), dp(hDp)).apply { setMargins(dp(7), dp(8), dp(7), dp(8)) }

        return frame
    }

    private fun heroBanner(item: VodItem, contentWidth: Int): View {

        val hero = FrameLayout(this)

        hero.background = placeholderFor(item.title)

        roundOutline(hero, 22)

        val backdrop = ImageView(this)

        backdrop.scaleType = ImageView.ScaleType.CENTER_CROP

        hero.addView(backdrop, FrameLayout.LayoutParams(matchParent, matchParent))

        loadImage(backdrop, item.backdrop.ifBlank { item.poster }, 960)

        val scrim = View(this)

        scrim.background =
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.argb(20, 4, 6, 18), Color.argb(238, 4, 6, 18))
            )

        hero.addView(scrim, FrameLayout.LayoutParams(matchParent, matchParent))

        val column = LinearLayout(this)

        column.orientation = LinearLayout.VERTICAL

        column.gravity = Gravity.CENTER_VERTICAL

        column.setPadding(dp(24), dp(14), dp(24), dp(14))

        val meta =
            listOf(genresOf(item).firstOrNull() ?: "", item.year, if (item.rating.isNotBlank()) "★ ${item.rating}" else "")
                .filter { it.isNotBlank() }
                .joinToString("  •  ")

        if (meta.isNotEmpty()) {
            column.addView(label(meta, 13f, cyan, true), lp(matchParent, wrap))
        }

        column.addView(
            label(item.title, if (phoneMode) 21f else 28f, white, true, Gravity.RIGHT, 2),
            lp(matchParent, wrap).apply { topMargin = dp(4) }
        )

        if (item.overview.isNotBlank()) {
            column.addView(
                label(item.overview, 13f, dimWhite, false, Gravity.RIGHT, if (phoneMode) 2 else 3),
                lp(matchParent, wrap).apply { topMargin = dp(6) }
            )
        }

        val buttons = LinearLayout(this)

        buttons.orientation = LinearLayout.HORIZONTAL

        buttons.gravity = Gravity.RIGHT

        val details = actionButton("التفاصيل", false) { showDetail(item) }

        val play =
            actionButton(
                if (item.series) "▶  ابدأ المشاهدة" else "▶  تشغيل",
                true
            ) { playBest(item) }

        libFirstFocus = play

        buttons.addView(details, lp(wrap, dp(if (phoneMode) 38 else 44)).apply { setMargins(dp(8), 0, 0, 0) })

        buttons.addView(play, lp(wrap, dp(if (phoneMode) 38 else 44)))

        column.addView(buttons, lp(matchParent, wrap).apply { topMargin = dp(12) })

        hero.addView(
            column,
            FrameLayout.LayoutParams((contentWidth * (if (phoneMode) 0.9f else 0.62f)).toInt(), matchParent, Gravity.RIGHT)
        )

        return hero
    }

    private fun renderLibrary() {

        if (!::libContent.isInitialized) {
            return
        }

        val series = currentSection == 2

        val all = if (series) seriesList else movies

        val posterW = if (phoneMode) 104 else 128

        val posterH = posterW * 3 / 2

        val screenW = resources.displayMetrics.widthPixels

        val contentW =
            if (phoneMode) {
                screenW - dp(24)
            } else {
                ((screenW - dp(36) - dp(28)) * 0.76f).toInt() - dp(40)
            }

        // genres
        clearList(libGenres)

        val genres = all.flatMap { genresOf(it) }.distinct()

        libGenres.addView(genreRow("الكل", all.size, vodGenre.isEmpty()))

        genres.forEach { g ->
            libGenres.addView(genreRow(g, all.count { g in genresOf(it) }, vodGenre == g))
        }

        libGenreHeader.setCount(genres.size + 1)

        val shown = if (vodGenre.isEmpty()) all else all.filter { vodGenre in genresOf(it) }

        libContent.removeAllViews()

        libFirstFocus = null

        if (shown.isEmpty()) {

            libContent.addView(
                label(
                    if (all.isEmpty()) "لا توجد أعمال بعد، تضاف من لوحة التحكم" else "لا توجد أعمال بهذا التصنيف",
                    16f,
                    gray,
                    true,
                    Gravity.CENTER,
                    3
                ),
                lp(matchParent, wrap).apply { setMargins(dp(12), dp(60), dp(12), 0) }
            )

            return
        }

        // featured
        val hero = shown.firstOrNull { it.featured } ?: shown.first()

        libContent.addView(
            heroBanner(hero, contentW),
            lp(matchParent, dp(if (phoneMode) 200 else 250)).apply { setMargins(dp(4), dp(4), dp(4), dp(6)) }
        )

        // continue watching
        val resumable = if (vodGenre.isEmpty()) all.filter { resumeTarget(it) != null } else emptyList()

        if (resumable.isNotEmpty()) {

            libContent.addView(sectionTitle("أكمل المشاهدة"))

            val scroller = HorizontalScrollView(this)

            scroller.isHorizontalScrollBarEnabled = false

            scroller.clipChildren = false

            val row = LinearLayout(this)

            row.orientation = LinearLayout.HORIZONTAL

            row.clipChildren = false

            resumable.forEach { item ->

                val target = resumeTarget(item)

                val episode = target?.first

                val progress = progressOf(vodKey(item, episode))

                row.addView(
                    posterView(
                        item,
                        posterW,
                        posterH,
                        if (episode != null) "الموسم ${episode.season} • الحلقة ${episode.number}" else "",
                        progress
                    ) { playBest(item) }
                )
            }

            scroller.addView(row)

            libContent.addView(scroller, lp(matchParent, wrap))
        }

        // everything
        libContent.addView(
            sectionTitle(
                when {
                    vodGenre.isNotEmpty() -> vodGenre
                    series -> "كل المسلسلات"
                    else -> "كل الأفلام"
                }
            )
        )

        val perRow = maxOf(2, contentW / (dp(posterW) + dp(14)))

        shown.chunked(perRow).forEach { chunk ->

            val row = LinearLayout(this)

            row.orientation = LinearLayout.HORIZONTAL

            row.clipChildren = false

            chunk.forEach { item ->
                row.addView(posterView(item, posterW, posterH, "", 0f) { showDetail(item) })
            }

            libContent.addView(row, lp(matchParent, wrap))
        }
    }

    // ---- details page ----

    private fun detailOpen(): Boolean =
        ::detailOverlay.isInitialized && detailOverlay.visibility == View.VISIBLE

    private fun buildDetailOverlay() {

        detailOverlay = FrameLayout(this)

        detailOverlay.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgTop, bgMid, bgBottom)
            )

        detailOverlay.isClickable = true

        detailOverlay.visibility = View.GONE

        detailBackdrop = ImageView(this)

        detailBackdrop.scaleType = ImageView.ScaleType.CENTER_CROP

        detailBackdrop.alpha = 0.5f

        val backdropHeight = dp(if (phoneMode) 230 else 320)

        detailOverlay.addView(detailBackdrop, FrameLayout.LayoutParams(matchParent, backdropHeight))

        val fade = View(this)

        fade.background =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(0, 0, 0, 0), bgMid)
            )

        detailOverlay.addView(fade, FrameLayout.LayoutParams(matchParent, backdropHeight))

        detailScroll = ScrollView(this)

        detailScroll.isVerticalScrollBarEnabled = false

        detailCol = LinearLayout(this)

        detailCol.orientation = LinearLayout.VERTICAL

        detailCol.setPadding(dp(if (phoneMode) 14 else 34), dp(if (phoneMode) 56 else 40), dp(if (phoneMode) 14 else 34), dp(30))

        detailScroll.addView(detailCol)

        detailOverlay.addView(detailScroll, FrameLayout.LayoutParams(matchParent, matchParent))

        val close = TextView(this)

        close.text = "✕"

        close.textSize = 18f

        close.setTextColor(white)

        close.gravity = Gravity.CENTER

        close.isClickable = true

        close.isFocusable = true

        close.isFocusableInTouchMode = !phoneMode

        close.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(190, 8, 10, 28))
                setStroke(dp(1), Color.argb(120, 255, 255, 255))
            }

        close.setOnClickListener { closeDetail() }

        detailOverlay.addView(
            close,
            FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.LEFT).apply { setMargins(dp(14), dp(14), 0, 0) }
        )

        root.addView(detailOverlay, FrameLayout.LayoutParams(matchParent, matchParent))
    }

    private fun showDetail(item: VodItem) {

        detailItem = item

        val target = resumeTarget(item)

        detailSeason = target?.first?.season ?: item.episodes.firstOrNull()?.season ?: 1

        detailReturnFocus = currentFocus

        renderDetail()

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        detailOverlay.visibility = View.VISIBLE

        detailScroll.scrollTo(0, 0)

        detailScroll.post { detailPrimary?.requestFocus() }
    }

    private fun closeDetail() {

        if (!detailOpen()) {
            return
        }

        detailOverlay.visibility = View.GONE

        normalScreen.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

        detailReturnFocus?.requestFocus()

        detailReturnFocus = null
    }

    private fun renderDetail() {

        val item = detailItem ?: return

        detailCol.removeAllViews()

        detailPrimary = null

        detailBackdrop.setImageDrawable(null)

        loadImage(detailBackdrop, item.backdrop.ifBlank { item.poster }, 960)

        val posterW = if (phoneMode) 110 else 160

        val posterH = posterW * 3 / 2

        // poster + information
        val top = LinearLayout(this)

        top.orientation = LinearLayout.HORIZONTAL

        val info = LinearLayout(this)

        info.orientation = LinearLayout.VERTICAL

        info.setPadding(dp(8), 0, dp(16), 0)

        info.addView(
            label(item.title, if (phoneMode) 21f else 32f, white, true, Gravity.RIGHT, 3),
            lp(matchParent, wrap)
        )

        val facts =
            listOf(
                genresOf(item).joinToString("، "),
                item.year,
                if (item.duration > 0) "${item.duration} د" else "",
                if (item.rating.isNotBlank()) "★ ${item.rating}" else "",
                if (item.series) "${item.episodes.map { it.season }.distinct().size} مواسم" else ""
            ).filter { it.isNotBlank() }.joinToString("  •  ")

        if (facts.isNotEmpty()) {
            info.addView(label(facts, 14f, cyan, true), lp(matchParent, wrap).apply { topMargin = dp(6) })
        }

        if (item.overview.isNotBlank()) {
            info.addView(
                label(item.overview, 14f, dimWhite, false, Gravity.RIGHT, 7),
                lp(matchParent, wrap).apply { topMargin = dp(10) }
            )
        }

        val buttons = LinearLayout(this)

        buttons.orientation = if (phoneMode) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        buttons.gravity = Gravity.RIGHT

        val target = resumeTarget(item)

        val resumeEpisode: Episode? = target?.first

        val resumePos: Long = target?.second ?: 0L

        val primary: TextView? =
            if (!item.series) {

                if (item.url.isBlank()) {
                    null
                } else if (target != null) {
                    actionButton("▶  أكمل من ${fmtTime(target.second)}", true) { playVod(item, null, target.second) }
                } else {
                    actionButton("▶  تشغيل", true) { playVod(item, null, 0L) }
                }

            } else if (resumeEpisode != null) {

                actionButton(
                    "▶  أكمل: الموسم ${resumeEpisode.season} الحلقة ${resumeEpisode.number}",
                    true
                ) { playVod(item, resumeEpisode, resumePos) }

            } else {

                val first = item.episodes.firstOrNull()

                if (first != null) actionButton("▶  ابدأ المشاهدة", true) { playVod(item, first, 0L) } else null
            }

        if (!item.series && target != null) {
            val restart = actionButton("من البداية", false) { playVod(item, null, 0L) }
            buttons.addView(restart, lp(wrap, dp(44)).apply { setMargins(dp(8), dp(4), 0, dp(4)) })
        }

        if (primary != null) {
            buttons.addView(primary, lp(wrap, dp(44)).apply { setMargins(0, dp(4), 0, dp(4)) })
            detailPrimary = primary
        }

        info.addView(buttons, lp(matchParent, wrap).apply { topMargin = dp(14) })

        val posterFrame = FrameLayout(this)

        posterFrame.background = placeholderFor(item.title)

        roundOutline(posterFrame, 16)

        val posterImage = ImageView(this)

        posterImage.scaleType = ImageView.ScaleType.CENTER_CROP

        posterFrame.addView(posterImage, FrameLayout.LayoutParams(matchParent, matchParent))

        loadImage(posterImage, item.poster, 480)

        top.addView(info, lp(0, wrap, 1f))

        top.addView(posterFrame, lp(dp(posterW), dp(posterH)))

        detailCol.addView(top, lp(matchParent, wrap))

        // seasons + episodes (series)
        if (item.series && item.episodes.isNotEmpty()) {

            val seasons = item.episodes.map { it.season }.distinct().sorted()

            if (detailSeason !in seasons) {
                detailSeason = seasons.first()
            }

            val chips = HorizontalScrollView(this)

            chips.isHorizontalScrollBarEnabled = false

            val chipRow = LinearLayout(this)

            chipRow.orientation = LinearLayout.HORIZONTAL

            chipRow.gravity = Gravity.RIGHT

            seasons.reversed().forEach { season ->

                val chip = actionButton("الموسم $season", season == detailSeason) {
                    detailSeason = season
                    renderDetail()
                }

                chipRow.addView(chip, lp(wrap, dp(40)).apply { setMargins(dp(5), 0, dp(5), 0) })
            }

            chips.addView(chipRow, FrameLayout.LayoutParams(wrap, wrap))

            detailCol.addView(chips, lp(matchParent, wrap).apply { topMargin = dp(22) })

            item.episodes.filter { it.season == detailSeason }.forEach { episode ->
                detailCol.addView(episodeRow(item, episode))
            }
        }
    }

    private fun episodeRow(item: VodItem, ep: Episode): View {

        val key = vodKey(item, ep)

        val progress = progressOf(key)

        val done = isFinished(key)

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL

        row.gravity = Gravity.CENTER_VERTICAL

        row.setPadding(dp(14), 0, dp(10), 0)

        row.isClickable = true

        row.isFocusable = true

        row.isFocusableInTouchMode = !phoneMode

        row.layoutParams = lp(matchParent, dp(88)).apply { topMargin = dp(8) }

        // play icon (left)
        val playCircle = FrameLayout(this)

        playCircle.background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(50, 255, 255, 255))
            }

        val playIcon = LineIconView()

        playIcon.setIcon(if (done) "M5 13l4 4L19 7" else ICON_PLAY)

        playCircle.addView(playIcon, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))

        row.addView(playCircle, lp(dp(40), dp(40)))

        // texts
        val texts = LinearLayout(this)

        texts.orientation = LinearLayout.VERTICAL

        texts.setPadding(dp(12), 0, dp(12), 0)

        texts.addView(label(ep.title, 16f, white, true), lp(matchParent, wrap))

        texts.addView(
            label(
                "الحلقة ${ep.number}" + (if (ep.duration > 0) "  •  ${ep.duration} د" else "") + (if (done) "  •  تمت المشاهدة" else ""),
                12f,
                gray
            ),
            lp(matchParent, wrap)
        )

        if (progress > 0f && !done) {

            val track = LinearLayout(this)

            track.orientation = LinearLayout.HORIZONTAL

            track.background =
                GradientDrawable().apply {
                    cornerRadius = dp(2).toFloat()
                    setColor(Color.argb(60, 255, 255, 255))
                }

            track.addView(
                View(this).apply { setBackgroundColor(cyan) },
                LinearLayout.LayoutParams(0, matchParent, progress.coerceIn(0.03f, 1f))
            )

            track.addView(View(this), LinearLayout.LayoutParams(0, matchParent, 1f - progress.coerceIn(0.03f, 1f)))

            texts.addView(track, lp(matchParent, dp(4)).apply { topMargin = dp(8) })
        }

        row.addView(texts, lp(0, wrap, 1f))

        // thumbnail (right)
        val thumb = FrameLayout(this)

        thumb.background = placeholderFor(item.title + ep.number)

        roundOutline(thumb, 12)

        val thumbImage = ImageView(this)

        thumbImage.scaleType = ImageView.ScaleType.CENTER_CROP

        thumb.addView(thumbImage, FrameLayout.LayoutParams(matchParent, matchParent))

        loadImage(thumbImage, ep.thumb, 300)

        thumb.addView(
            label("${ep.number}", 18f, white, true, Gravity.CENTER),
            FrameLayout.LayoutParams(matchParent, matchParent)
        )

        row.addView(thumb, lp(dp(if (phoneMode) 96 else 128), dp(if (phoneMode) 54 else 72)))

        fun style(focused: Boolean) {

            row.background =
                if (focused) {
                    GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(mix(cyan, bgMid, 0.5f), mix(violet, bgMid, 0.5f))
                    ).apply {
                        cornerRadius = dp(18).toFloat()
                        setStroke(dp(2), Color.WHITE)
                    }
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(18).toFloat()
                        setColor(Color.argb(26, 255, 255, 255))
                        setStroke(dp(1), Color.argb(30, 255, 255, 255))
                    }
                }
        }

        style(false)

        row.setOnFocusChangeListener { _, focused -> style(focused) }

        row.setOnClickListener {
            playVod(item, ep, if (!done && progress > 0f) savedPos(key) else 0L)
        }

        return row
    }

    // ---- playing ----

    // the saved place, or the beginning
    private fun playBest(item: VodItem) {

        val target = resumeTarget(item)

        if (target != null) {

            playVod(item, target.first, target.second)

        } else if (!item.series) {

            playVod(item, null, 0L)

        } else {

            val first = item.episodes.firstOrNull()

            if (first != null) {
                playVod(item, first, 0L)
            } else {
                Toast.makeText(this, "لا توجد حلقات بعد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playVod(item: VodItem, ep: Episode?, startMs: Long) {

        if (blocked || gated) {
            return
        }

        val url = ep?.url ?: item.url

        if (url.isBlank()) {
            Toast.makeText(this, "لا يوجد رابط لهذا العمل", Toast.LENGTH_SHORT).show()
            return
        }

        if (vodPlay == null) {
            liveBeforeVod = playingChannel?.takeIf { !it.isVod } ?: liveBeforeVod
        }

        val name =
            if (ep != null) "${item.title} · الموسم ${ep.season} الحلقة ${ep.number}" else item.title

        val channel =
            Channel(
                name = name,
                group = "vod",
                url = url,
                logo = item.poster,
                enabled = true,
                order = 0,
                isVod = true
            )

        val play = VodPlay(item, ep)

        vodPlay = play

        pendingSeek = if (startMs > 15000L) startMs else 0L

        if (ep != null) {
            prefs.edit().putString("lastEp_${item.id}", "${ep.season}x${ep.number}").apply()
        }

        closeDetail()

        playChannel(channel)

        playingList = listOf(channel)

        enterFullscreen()

        mainHandler.removeCallbacks(vodSaveRunnable)

        mainHandler.postDelayed(vodSaveRunnable, 10_000L)
    }

    private val vodSaveRunnable = object : Runnable {

        override fun run() {

            saveVodProgress()

            if (vodPlay != null) {
                mainHandler.postDelayed(this, 10_000L)
            }
        }
    }

    private fun saveVodProgress() {

        val play = vodPlay ?: return

        if (playingWeb || player.duration <= 0 || player.currentPosition <= 0) {
            return
        }

        prefs.edit()
            .putLong("vp_" + play.key, player.currentPosition)
            .putLong("vd_" + play.key, player.duration)
            .apply()
    }

    // the video ended: next episode at once, or the end screen
    private fun onVodEnded() {

        val play = vodPlay

        if (play == null) {
            showStatus("انتهى الفيديو", "اضغط رجوع للعودة", true)
            return
        }

        val duration = player.duration

        if (duration > 0) {
            prefs.edit()
                .putLong("vp_" + play.key, duration)
                .putLong("vd_" + play.key, duration)
                .apply()
        }

        val episode = play.episode

        if (episode != null) {

            val next = nextEpisode(play.item, episode)

            if (next != null) {

                Toast.makeText(this, "الحلقة التالية: ${next.title}", Toast.LENGTH_LONG).show()

                playVod(play.item, next, 0L)

                return
            }
        }

        showStatus("انتهى العرض", "اضغط رجوع للعودة", true)
    }

    // back from the fullscreen: save the place, stop, and give the screen back
    private fun leaveVod() {

        saveVodProgress()

        mainHandler.removeCallbacks(vodSaveRunnable)

        try {
            player.stop()
            player.clearMediaItems()
        } catch (_: Exception) {
        }

        vodPlay = null

        pendingSeek = 0L

        playingChannel = liveBeforeVod

        if (currentSection == 0) {

            liveBeforeVod?.let { playChannel(it) }

        } else {

            renderLibrary()

            focusLibrary()
        }
    }

    // =========================
    // Check the update file before installing it
    // =========================

    private fun signatureSet(info: android.content.pm.PackageInfo?): Set<String> {

        if (info == null) {
            return emptySet()
        }

        val signatures =
            if (Build.VERSION.SDK_INT >= 28) {
                info.signingInfo?.apkContentsSigners?.toList()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.toList()
            }

        return (signatures ?: emptyList())
            .map { signature ->
                MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
            .toSet()
    }

    // returns the reason why Android would refuse this update (null = looks fine)
    @Suppress("DEPRECATION")
    private fun checkApk(file: File): String? {

        val flags =
            if (Build.VERSION.SDK_INT >= 28) {
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                android.content.pm.PackageManager.GET_SIGNATURES
            }

        val archive =
            try {
                packageManager.getPackageArchiveInfo(file.path, flags)
            } catch (_: Exception) {
                null
            } ?: return "ملف التحديث تالف أو غير صالح. أعد تحميله."

        if (archive.packageName != packageName) {
            return "ملف التحديث لتطبيق مختلف (${archive.packageName})."
        }

        val newCode =
            if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()

        val current = currentVersionCode()

        if (newCode <= current) {
            return "رقم إصدار التحديث ($newCode) لا يزيد عن المثبت ($current). ارفع versionCode ثم أعد بناء الملف."
        }

        val installed =
            try {
                packageManager.getPackageInfo(packageName, flags)
            } catch (_: Exception) {
                null
            }

        val newSigners = signatureSet(archive)

        val oldSigners = signatureSet(installed)

        if (newSigners.isNotEmpty() && oldSigners.isNotEmpty() && newSigners.intersect(oldSigners).isEmpty()) {
            return "التحديث موقّع بمفتاح مختلف عن النسخة المثبتة، فلن يُثبَّت فوقها. احذف التطبيق ثم ثبّت التحديث يدوياً مرة واحدة، وبعدها تعمل كل التحديثات."
        }

        return null
    }

    // =========================
    // Focus helpers
    // =========================

    private fun columnOf(view: View?): Int {

        var v: View? = view

        while (v != null) {

            if (v === packagesList) {
                return 0
            }

            if (v === channelsList) {
                return 1
            }

            if (v === playerFrame) {
                return 2
            }

            v = v.parent as? View
        }

        return -1
    }

    private fun focusSelectedPackage() {

        val row =
            packageRows.firstOrNull { it.item.id == selectedPackage }
                ?: packageRows.firstOrNull()

        row?.card?.requestFocus()
    }

    private fun focusChannelsColumn() {

        val target =
            channelRows.firstOrNull { it.channel == lastFocusedChannel }
                ?: channelRows.firstOrNull { it.channel == playingChannel }
                ?: channelRows.firstOrNull()

        if (target != null) {
            target.card.requestFocus()
        } else {
            focusSelectedPackage()
        }
    }

    // =========================
    // D-PAD
    // =========================

    // a tap on the screen (fullscreen) shows the touch buttons, a swipe up / down changes the channel
    private val gestureDetector by lazy {
        GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    handleScreenTap(e.rawX, e.rawY)
                    return false
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    handleDoubleTap(e.rawX)
                    return false
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    handleFling(e1, e2, velocityX, velocityY)
                    return false
                }
            }
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        if (isFullscreen && !blocked && !splashVisible && !multiActive) {
            gestureDetector.onTouchEvent(ev)
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        // remember the last D-pad press (used for the navigation sound)
        if (
            event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
        ) {
            lastNavKeyAt = SystemClock.uptimeMillis()
        }

        // splash screen swallows keys
        if (splashVisible) {
            return true
        }

        // app disabled / maintenance: nothing works except exit
        if (blocked) {

            if (
                event.keyCode == KeyEvent.KEYCODE_BACK &&
                event.action == KeyEvent.ACTION_DOWN &&
                event.repeatCount == 0
            ) {

                val now = System.currentTimeMillis()

                if (now - backPressedAt < 2000) {
                    finish()
                } else {
                    backPressedAt = now
                    Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
                }
            }

            return true
        }

        // activation code screen
        if (codeGateOpen()) {

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {

                    if (!gated) {

                        hideCodeOverlay(false)

                    } else {

                        val now = System.currentTimeMillis()

                        if (now - backPressedAt < 2000) {
                            finish()
                        } else {
                            backPressedAt = now
                            Toast.makeText(this, "اضغط رجوع مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                return true
            }

            return super.dispatchKeyEvent(event)
        }

        // announcement / update notice
        if (noticeOpen()) {

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN && noticeCancelable && !downloadingUpdate) {
                    hideNotice()
                    noticeDismiss?.invoke()
                }
                return true
            }

            return super.dispatchKeyEvent(event)
        }

        // movie / series details
        if (detailOpen()) {

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    closeDetail()
                }

                return true
            }

            return super.dispatchKeyEvent(event)
        }

        // split screen (multi view)
        if (multiActive) {
            return handleMultiKey(event)
        }

        // long-press menu
        if (menuOpen()) {

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    closeMenu()
                }
                return true
            }

            return super.dispatchKeyEvent(event)
        }

        // number keys / previous channel
        if (moveState == null && (currentSection == 0 || isFullscreen) && handleSpecialKeys(event)) {
            return true
        }

        // player panel on the TV: the D-pad moves between its buttons
        if (touchControlsVisible() && !phoneMode && isFullscreen) {
            return handleHudKey(event)
        }

        // quality strip
        if (qualityStripOpen()) {
            return handleQualityStripKey(event)
        }

        // glass channel panel (fullscreen)
        if (panelOpen()) {
            return handlePanelKey(event)
        }

        // reorder mode
        if (moveState != null) {
            return handleMoveKey(event)
        }

        if (isFullscreen) {
            return handleFullscreenKey(event)
        }

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        val key = event.keyCode

        // menu button = settings
        if (key == KeyEvent.KEYCODE_MENU) {
            showSettingsMenu()
            return true
        }

        // yellow button = favorite
        if (key == KeyEvent.KEYCODE_PROG_YELLOW) {

            val target =
                if (columnOf(currentFocus) == 1) lastFocusedChannel else playingChannel

            if (target != null) {
                toggleFavorite(target)
            }

            return true
        }

        val isDpad =
            key == KeyEvent.KEYCODE_DPAD_LEFT ||
                    key == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    key == KeyEvent.KEYCODE_DPAD_UP ||
                    key == KeyEvent.KEYCODE_DPAD_DOWN

        if (!isDpad) {
            return super.dispatchKeyEvent(event)
        }

        // movies / series pages and the section bar use the normal focus search
        if (currentSection != 0 || isInSectionBar(currentFocus)) {
            return super.dispatchKeyEvent(event)
        }

        val column = columnOf(currentFocus)

        if (column == -1) {

            if (packageRows.isNotEmpty()) {
                focusSelectedPackage()
                return true
            }

            return super.dispatchKeyEvent(event)
        }

        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {

            if (column == 0) {

                flushPendingSelect()

                if (channelRows.isEmpty()) {
                    playerFrame.requestFocus()
                } else {
                    focusChannelsColumn()
                }

                return true
            }

            if (column == 1) {
                playerFrame.requestFocus()
                return true
            }

            return true
        }

        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {

            if (column == 2) {
                focusChannelsColumn()
                return true
            }

            if (column == 1) {
                focusSelectedPackage()
                return true
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    private fun handleFullscreenKey(event: KeyEvent): Boolean {

        if (!playingWeb && playerView.isControllerFullyVisible) {
            return super.dispatchKeyEvent(event)
        }

        // menu = the player panel (channel info, seek bar, buttons)
        if (event.keyCode == KeyEvent.KEYCODE_MENU) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                showTouchControls()
            }

            return true
        }

        if (event.keyCode == KeyEvent.KEYCODE_PROG_BLUE) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                openQualityStrip()
            }

            return true
        }

        // video: left / right = 10 seconds back / forward
        if (
            (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) &&
            !playingWeb &&
            playingChannel?.let { isVodChannel(it) } == true
        ) {

            if (event.action == KeyEvent.ACTION_DOWN) {
                seekBy(if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -10000L else 10000L)
                showTouchControls()
            }

            return true
        }

        // left / right on a live channel = quality strip
        if (
            (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) &&
            !playingWeb &&
            playingChannel?.let { isVodChannel(it) } == false
        ) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                openQualityStrip()
            }

            return true
        }

        if (event.keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {

            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                playingChannel?.let { toggleFavorite(it) }
            }

            return true
        }

        val delta =
            when (event.keyCode) {

                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_CHANNEL_DOWN -> -1

                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_CHANNEL_UP -> 1

                else -> 0
            }

        if (delta != 0) {

            if (
                event.action == KeyEvent.ACTION_DOWN &&
                event.repeatCount == 0
            ) {
                zap(delta)
            }

            return true
        }

        if (
            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER
        ) {

            // OK = open the glass channel list
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {

                if (!playingWeb && playingChannel?.let { isVodChannel(it) } == true) {
                    togglePlayPause()
                    showTouchControls()
                } else {
                    openChannelPanel()
                }
            }

            return true
        }

        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {

        if (isFullscreen) {
            exitFullscreen()
            return
        }

        if (detailOpen()) {
            closeDetail()
            return
        }

        if (currentSection != 0) {
            switchSection(0)
            return
        }

        when (columnOf(currentFocus)) {

            2 -> {
                focusChannelsColumn()
                return
            }

            1 -> {
                focusSelectedPackage()
                return
            }
        }

        val now = System.currentTimeMillis()

        if (now - backPressedAt < 2000) {

            super.onBackPressed()

        } else {

            backPressedAt = now

            Toast.makeText(
                this,
                "\u0627\u0636\u063a\u0637 \u0631\u062c\u0648\u0639 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649 \u0644\u0644\u062e\u0631\u0648\u062c",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // Fullscreen
    // =========================

    private fun enterFullscreen() {

        if (isFullscreen) {
            return
        }

        isFullscreen = true

        setPhoneOrientation(true)

        cancelPreview()

        playingChannel?.let {
            if (!it.isVod) {
                prefs.edit().putString("last_channel", channelKey(it)).apply()
            }
        }

        moveVideoTo(fullscreenContainer)

        normalScreen.visibility = View.GONE

        fullscreenContainer.visibility = View.VISIBLE

        applyControllerMode()

        playerView.controllerShowTimeoutMs = 4000

        if (playingWeb) {

            playerView.isFocusable = false

            webView.isFocusable = true

            webView.isFocusableInTouchMode = !phoneMode

            webView.requestFocus()

        } else {

            playerView.isFocusable = true

            playerView.isFocusableInTouchMode = !phoneMode

            playerView.requestFocus()
        }

        hideSystemBars()

        playingChannel?.let {
            showZapBanner(it)
        }
    }

    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        isFullscreen = false

        setPhoneOrientation(false)

        hideTouchControls(true)

        closeChannelPanel(true)

        closeQualityStrip(true)

        playerView.hideController()

        playerView.useController = false

        playerView.isFocusable = false

        webView.isFocusable = false

        webView.isFocusableInTouchMode = false

        mainHandler.removeCallbacks(hideZapRunnable)

        zapBanner.animate().cancel()

        zapBanner.visibility = View.GONE

        moveVideoTo(playerFrame)

        fullscreenContainer.visibility = View.GONE

        normalScreen.visibility = View.VISIBLE

        showSystemBars()

        playerFrame.requestFocus()

        if (vodPlay != null) {
            leaveVod()
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.let {

                it.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        } else {

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    @Suppress("DEPRECATION")
    private fun showSystemBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.insetsController?.show(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )

        } else {

            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    // =========================
    // Logo download
    // =========================

    private fun downloadBitmap(urlString: String): Bitmap? {

        var connection: HttpURLConnection? = null

        return try {

            connection = URL(urlString).openConnection() as HttpURLConnection

            connection.connectTimeout = 6000

            connection.readTimeout = 8000

            connection.instanceFollowRedirects = true

            if (connection.responseCode !in 200..299) {
                return null
            }

            val bytes = connection.inputStream.use { it.readBytes() }

            val bounds =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sample = 1

            while (
                bounds.outWidth / sample > 256 ||
                bounds.outHeight / sample > 256
            ) {
                sample *= 2
            }

            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        } catch (_: Exception) {

            null

        } finally {

            connection?.disconnect()
        }
    }

    // =========================
    // Lifecycle
    // =========================

    override fun onStart() {

        super.onStart()

        if (::webView.isInitialized) {
            webView.onResume()
        }

        if (multiActive) {
            tiles.forEach { it.player.playWhenReady = true }
        }

        if (externalAudio >= 0) {
            audioPlayer?.playWhenReady = true
        }

        playingChannel?.let {
            if (!multiActive && !blocked && !gated) {
                beginWatch(it)
            }
        }

        if (resumeOnStart && ::player.isInitialized && !playingWeb) {

            if (player.isCurrentMediaItemLive) {
                player.seekToDefaultPosition()
            }

            player.playWhenReady = true
        }
    }

    override fun onStop() {

        super.onStop()

        if (::webView.isInitialized) {
            webView.onPause()
        }

        tiles.forEach { it.player.pause() }

        audioPlayer?.pause()

        flushWatch()

        if (::player.isInitialized) {

            resumeOnStart =
                player.playWhenReady &&
                        player.playbackState != Player.STATE_IDLE

            player.pause()
        }
    }

    override fun onDestroy() {

        mainHandler.removeCallbacksAndMessages(null)

        listeners.forEach { it.first.removeEventListener(it.second) }

        listeners.clear()

        detachCodeListener()

        flushWatch()

        ioExecutor.shutdownNow()

        if (::webView.isInitialized) {

            (webView.parent as? ViewGroup)?.removeView(webView)

            webView.stopLoading()

            webView.destroy()
        }

        tiles.forEach {
            try {
                it.player.release()
            } catch (_: Exception) {
            }
        }

        tiles.clear()

        audioPlayer?.release()

        audioPlayer = null

        try {
            netCallback?.let {
                (getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager)
                    .unregisterNetworkCallback(it)
            }
        } catch (_: Exception) {
        }

        if (::player.isInitialized) {
            player.release()
        }

        super.onDestroy()
    }
}

// =========================
// Arabic texts
// =========================

private object Txt {

    const val DB_ERROR = "\u062a\u0639\u0630\u0631 \u0627\u0644\u0627\u062a\u0635\u0627\u0644 \u0628\u0642\u0627\u0639\u062f\u0629 \u0627\u0644\u0628\u064a\u0627\u0646\u0627\u062a"

    const val ERR_SUB = "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0631\u0627\u0628\u0637 \u0627\u0644\u0628\u062b \u0623\u0648 \u0645\u0646 \u0627\u062a\u0635\u0627\u0644 \u0627\u0644\u0625\u0646\u062a\u0631\u0646\u062a"

    const val ERR_TITLE = "\u062a\u0639\u0630\u0631 \u062a\u0634\u063a\u064a\u0644 \u0627\u0644\u0642\u0646\u0627\u0629"

    const val HINT = "اضغط OK لملء الشاشة  •  اضغط مطولاً على القناة للمفضلة والنقل"

    const val IDLE_SUB = "\u062a\u0646\u0642\u0651\u0644 \u0628\u0627\u0644\u0623\u0633\u0647\u0645 \u0628\u064a\u0646 \u0627\u0644\u0628\u0627\u0642\u0627\u062a \u0648\u0627\u0644\u0642\u0646\u0648\u0627\u062a"

    const val IDLE_TITLE = "\u0627\u062e\u062a\u0631 \u0642\u0646\u0627\u0629 \u0644\u0644\u0645\u0634\u0627\u0647\u062f\u0629"

    const val LOAD_FAIL_SUB = "\u0633\u064a\u062a\u0645 \u0627\u0644\u062a\u062d\u062f\u064a\u062b \u062a\u0644\u0642\u0627\u0626\u064a\u0627\u064b \u0639\u0646\u062f \u0639\u0648\u062f\u0629 \u0627\u0644\u0627\u062a\u0635\u0627\u0644"

    const val LOAD_FAIL_TITLE = "\u062a\u0639\u0630\u0631 \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0628\u064a\u0627\u0646\u0627\u062a"

    const val NO_CHANNELS = "\u0644\u0627 \u062a\u0648\u062c\u062f \u0642\u0646\u0648\u0627\u062a"

    const val NO_CHANNELS_SUB = "\u0644\u0647\u0630\u0647 \u0627\u0644\u0628\u0627\u0642\u0629 \u062d\u0627\u0644\u064a\u0627\u064b"

    const val NO_URL = "\u0647\u0630\u0647 \u0627\u0644\u0642\u0646\u0627\u0629 \u0644\u0627 \u062a\u062d\u062a\u0648\u064a \u0639\u0644\u0649 \u0631\u0627\u0628\u0637 \u0628\u062b \u062d\u0627\u0644\u064a\u0627\u064b"
}
