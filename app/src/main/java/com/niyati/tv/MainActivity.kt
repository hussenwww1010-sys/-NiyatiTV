package com.niyati.tv

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

data class Channel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : Activity() {

    private lateinit var playerView: PlayerView
    private lateinit var packageLayout: LinearLayout
    private lateinit var channelLayout: LinearLayout

    private var player: ExoPlayer? = null

    private var currentGroup = ""
    private var currentChannelIndex = -1
    private var currentChannel: Channel? = null

    private var isFullscreen = false
    private var isPlayingChannel = false

    private val handler = Handler(Looper.getMainLooper())

    // ============================================================
    // القنوات الجديدة فقط
    // ============================================================

    private val channels = mutableListOf<Channel>().apply {

        // ========================================================
        // ALWAN
        // ========================================================

        add(Channel("ألوان Aflam 1 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641630.ts"))

        add(Channel("ألوان Aflam 2 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641631.ts"))

        add(Channel("ألوان Aflam 3 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641632.ts"))

        add(Channel("ألوان Aflam 4 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641633.ts"))

        add(Channel("ألوان Aghani 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641638.ts"))

        add(Channel("ألوان Alwathaeqye 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641635.ts"))

        add(Channel("ألوان Anime 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641642.ts"))

        add(Channel("ألوان Atfal 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641636.ts"))

        add(Channel("ألوان Bollywood 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641639.ts"))

        add(Channel("ألوان F1 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641616.ts"))

        add(Channel("ألوان Korea 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641640.ts"))

        add(Channel("ألوان Mosalsalat 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641637.ts"))

        add(Channel("ألوان Mosalsalat+ 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641641.ts"))

        add(Channel("ألوان Quran 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641643.ts"))

        add(Channel("ألوان Sport 1 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641605.ts"))

        add(Channel("ألوان Sport 1 HD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641618.ts"))

        add(Channel("ألوان Sport 1 SD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641624.ts"))

        add(Channel("ألوان Sport 2 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641606.ts"))

        add(Channel("ألوان Sport 2 HD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641619.ts"))

        add(Channel("ألوان Sport 2 SD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641625.ts"))

        add(Channel("ألوان Sport 3 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641607.ts"))

        add(Channel("ألوان Sport 3 HD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641620.ts"))

        add(Channel("ألوان Sport 3 SD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641626.ts"))

        add(Channel("ألوان Sport 4 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641608.ts"))

        add(Channel("ألوان Sport 4 HD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641621.ts"))

        add(Channel("ألوان Sport 4 SD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641627.ts"))

        add(Channel("ألوان Sport 5 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641609.ts"))

        add(Channel("ألوان Sport 5 HD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641622.ts"))

        add(Channel("ألوان Sport 5 SD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641628.ts"))

        add(Channel("ألوان Sport 6 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641610.ts"))

        add(Channel("ألوان Sport 6 HD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641623.ts"))

        add(Channel("ألوان Sport 6 SD", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641629.ts"))

        add(Channel("ألوان Sport 7 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641611.ts"))

        add(Channel("ألوان Sport 8 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641612.ts"))

        add(Channel("ألوان Sport 9 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641613.ts"))

        add(Channel("ألوان Sport 10 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641614.ts"))

        add(Channel("ألوان Turkey 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641634.ts"))

        add(Channel("ألوان UFC 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641615.ts"))

        add(Channel("ألوان WWE 4K", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1641617.ts"))

        add(Channel("|AR| Zee ألوان TV", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/355100.ts"))

        add(Channel("|AR| Zee ألوان TV", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1058045.ts"))

        add(Channel("|UAE| Zee ألوان TV", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1261.ts"))

        add(Channel("|UAE| Zee ألوان TV", "Alwan",
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1058351.ts"))


        // ========================================================
        // الدوري الإيطالي
        // ========================================================

        val serieA = "الدوري الإيطالي - Serie A"

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295337.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295336.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295340.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295339.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295338.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295343.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295342.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295341.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/516008.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1235772.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1235777.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/295335.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1756834.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1756835.ts"))

        add(Channel("الدوري الإيطالي - مباراة/قناة 1", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1585392.ts"))

        add(Channel("الدوري الإيطالي - مباراة/قناة 2", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1585393.ts"))

        add(Channel("الدوري الإيطالي - مباراة/قناة 3", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1585394.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630725.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630726.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630727.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/160254.ts"))

        add(Channel("الدوري الإيطالي", serieA,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/41060.ts"))


        // ========================================================
        // STARZPLAY
        // ========================================================

        val starz = "StarzPlay"

        add(Channel("CAR - Starz Comedy", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/297244.ts"))

        add(Channel("ES| Starz Encore", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/598487.ts"))

        add(Channel("|AR| ستارز بلاي / AD SPORTS 1", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/254310.ts"))

        add(Channel("|AR| ستارز بلاي / AD SPORTS 1 +", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1173275.ts"))

        add(Channel("|AR| ستارز بلاي / AD SPORTS 2", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/254309.ts"))

        add(Channel("|AR| ستارز بلاي / AD SPORTS 2 +", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1173276.ts"))

        add(Channel("|AR| ستارز بلاي / AD SPORTS 3 ᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/252862.ts"))

        add(Channel("|AR| ستارز بلاي / AD SPORTS 3+", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/252720.ts"))

        add(Channel("|AR| ستارز بلاي /AD PREMIER 3 ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/314102.ts"))

        add(Channel("|LAM| Starz Black ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29399.ts"))

        add(Channel("|LAM| Starz Cinema ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29400.ts"))

        add(Channel("|LAM| Starz Comedy ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29401.ts"))

        add(Channel("|LAM| Starz East ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29402.ts"))

        add(Channel("|LAM| Starz Edge ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29403.ts"))

        add(Channel("|LAM| Starz Encore Action", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29404.ts"))

        add(Channel("|LAM| Starz Encore Black ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29405.ts"))

        add(Channel("|LAM| Starz Encore Classic ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29406.ts"))

        add(Channel("|LAM| Starz Encore Family ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29408.ts"))

        add(Channel("|LAM| Starz Encore Suspense ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29409.ts"))

        add(Channel("|LAM| Starz Encore Western ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29410.ts"))

        add(Channel("|LAM| Starz Encore ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29407.ts"))

        add(Channel("|LAM| Starz Family ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29412.ts"))

        add(Channel("|LAM| Starz Kids Family ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29413.ts"))

        add(Channel("|LAM| Starz West ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29414.ts"))

        add(Channel("|LAM| Starz ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/29411.ts"))

        add(Channel("|US| STARZ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/60725.ts"))

        add(Channel("|US| STARZ CINEMA ᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/11161.ts"))

        add(Channel("|US| STARZ COMEDY", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/60729.ts"))

        add(Channel("|US| STARZ EAST", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/258828.ts"))

        add(Channel("|US| STARZ EDGE", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/60726.ts"))

        add(Channel("|US| STARZ ENCORE ACTION ᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/258829.ts"))

        add(Channel("|US| STARZ ENCORE BLACK", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/60730.ts"))

        add(Channel("|US| STARZ ENCORE FAMILY ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/76978.ts"))

        add(Channel("|US| STARZ ENCORE SUSPENSE", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/258830.ts"))

        add(Channel("|US| STARZ ENCORE WEST ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/76975.ts"))

        add(Channel("|US| STARZ ENCORE WESTERNS ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/76977.ts"))

        add(Channel("|US| STARZ ENCORE WESTERNS ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/11160.ts"))

        add(Channel("|US| STARZ ENCORE ᴴᴰ EAST", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/76979.ts"))

        add(Channel("|US| STARZ KIDS & FAMILY", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/60727.ts"))

        add(Channel("|US| STARZ KIDS & FAMILY ᵁᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/76976.ts"))

        add(Channel("|US| STARZ WEST", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/60728.ts"))

        add(Channel("|US| STARZ ᴴᴰ", starz,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/11159.ts"))


        // ========================================================
        // SHAHID
        // ========================================================

        val shahid = "Shahid"

        add(Channel("|AR| ALJAZEERA 360 شاهد على العصر", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1785072.ts"))

        add(Channel("|AR| ALJAZEERA 360 شاهد على العصر 2", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1785073.ts"))

        add(Channel("|AR| شاهد Al Hayba", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/513091.ts"))

        add(Channel("|AR| شاهد AL HAYBA ᴴᴰ", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/260380.ts"))

        add(Channel("|AR| شاهد AL HUFFRA", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/260371.ts"))

        add(Channel("|AR| شاهد Bab Al_Hara", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/513090.ts"))

        add(Channel("|AR| شاهد KORIA", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/260367.ts"))

        add(Channel("|AR| شاهد Naser Al Qassaby", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/513098.ts"))

        add(Channel("|AR| شاهد زينة و عزيزة", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/292929.ts"))

        add(Channel("|DOC|شاهد Documentary", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/309027.ts"))

        add(Channel("|MA| شاهد أولاد الدرب", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/292926.ts"))

        add(Channel("|MA| شاهد جروح", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/292927.ts"))

        add(Channel("|MA| شاهد دابا تزيان", shahid,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/292928.ts"))

        for (i in 1..18) {
            val id = 265709 + i
            add(
                Channel(
                    "|AR| شاهد CINEMA $i ᵁHD",
                    shahid,
                    "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/$id.ts"
                )
            )
        }

        for (i in 1..10) {
            val id = 1585036 + i
            add(
                Channel(
                    "|AR| شاهد PPV $i",
                    shahid,
                    "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/$id.ts"
                )
            )
        }


        // ========================================================
        // الكأس
        // ========================================================

        val kass = "الكأس"

        add(Channel("|AR| الكأس 1 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/22837.ts"))

        add(Channel("|AR| الكأس 2 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/22838.ts"))

        add(Channel("|AR| الكأس 3 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/22839.ts"))

        add(Channel("|AR| الكأس 4 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/22840.ts"))

        add(Channel("|AR| الكأس 5 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/22841.ts"))

        add(Channel("|AR| الكأس 6 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/22842.ts"))

        add(Channel("|AR| الكأس 7 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/28271.ts"))

        add(Channel("|AR| الكأس 8 ᴴᴰ", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/38857.ts"))

        add(Channel("الكأس 01", kass,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/201460.ts"))


        // ========================================================
        // الرابعة العراقية
        // ========================================================

        val rabiaa = "الرابعة العراقية - Al Rabiaa"

        add(Channel("الرابعة 1", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1142953.ts"))

        add(Channel("SP الرابعة Sports 1+", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1299808.ts"))

        add(Channel("الرابعة 2", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1142954.ts"))

        add(Channel("SP الرابعة Sports 2+", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1299809.ts"))

        add(Channel("الرابعة 3", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1142955.ts"))

        add(Channel("الرابعة 4", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1142956.ts"))

        add(Channel("|IRAQ| الرابعة Iraq", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/192402.ts"))

        add(Channel("|MA| Arrabiaa HD", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/7994.ts"))

        add(Channel("|MA| Arrabiaa HD", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1058152.ts"))

        add(Channel("|MA| Arrabiaa HD (SP)", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1291013.ts"))

        add(Channel("|MA| ARRABIAA Tnt", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/8220.ts"))

        add(Channel("|MA| Arrabiaa Tnt", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1058153.ts"))

        add(Channel("|MA| Arrabiaa UHD", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/49556.ts"))

        add(Channel("|SPO| الرابعة Sport", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/201255.ts"))

        add(Channel("|SPO| الرابعة sport", rabiaa,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/249482.ts"))


        // ========================================================
        // POST SPORT
        // ========================================================

        val post = "Post Sport"

        add(Channel("بوست سبورت 1", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630675.ts"))

        add(Channel("بوست سبورت 2", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630676.ts"))

        add(Channel("بوست سبورت 3", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630677.ts"))

        add(Channel("بوست سبورت 4", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630678.ts"))

        add(Channel("بوست سبورت 5", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630679.ts"))

        add(Channel("بوست سبورت 6", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630680.ts"))

        add(Channel("بوست TV", post,
            "http://m3u.drm-26.com:80/live/Mkdtv1_061261/4STJysUc/1630681.ts"))
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

        createPlayer()
        createInterface()

        if (channels.isNotEmpty()) {
            val firstGroup = channels.first().group
            showChannels(firstGroup)
        }
    }


    // ============================================================
    // PLAYER
    // ============================================================

    private fun createPlayer() {

        player = ExoPlayer.Builder(this).build()

        playerView = PlayerView(this)

        playerView.player = player

        playerView.useController = true

        playerView.setShowBuffering(
            PlayerView.SHOW_BUFFERING_WHEN_PLAYING
        )

        player?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {

                when (state) {

                    Player.STATE_READY -> {
                        isPlayingChannel = true
                    }

                    Player.STATE_BUFFERING -> {
                        // buffering
                    }

                    Player.STATE_ENDED -> {
                        reconnectChannel()
                    }

                    Player.STATE_IDLE -> {
                        // idle
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {

                Toast.makeText(
                    this@MainActivity,
                    "تعذر تشغيل القناة، جاري إعادة الاتصال...",
                    Toast.LENGTH_SHORT
                ).show()

                reconnectChannel()
            }
        })
    }


    private fun playChannel(channel: Channel, index: Int) {

        currentChannel = channel
        currentChannelIndex = index
        currentGroup = channel.group

        val mediaItem = MediaItem.fromUri(
            Uri.parse(channel.url)
        )

        player?.apply {

            stop()

            clearMediaItems()

            setMediaItem(mediaItem)

            prepare()

            playWhenReady = true
        }

        isPlayingChannel = true
    }


    private fun reconnectChannel() {

        val channel = currentChannel ?: return

        handler.postDelayed({

            if (!isFinishing) {

                val mediaItem = MediaItem.fromUri(
                    Uri.parse(channel.url)
                )

                player?.apply {

                    stop()

                    clearMediaItems()

                    setMediaItem(mediaItem)

                    prepare()

                    playWhenReady = true
                }
            }

        }, 1500)
    }


    // ============================================================
    // INTERFACE
    // ============================================================

    private fun createInterface() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.HORIZONTAL

        root.setBackgroundColor(
            Color.rgb(8, 10, 16)
        )

        // --------------------------------------------------------
        // PACKAGES
        // --------------------------------------------------------

        packageLayout = LinearLayout(this)

        packageLayout.orientation = LinearLayout.VERTICAL

        packageLayout.gravity = Gravity.TOP

        packageLayout.setPadding(
            15,
            25,
            10,
            20
        )

        packageLayout.setBackgroundColor(
            Color.rgb(13, 16, 24)
        )

        val packageParams = LinearLayout.LayoutParams(
            260,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        root.addView(
            packageLayout,
            packageParams
        )


        // --------------------------------------------------------
        // CENTER
        // --------------------------------------------------------

        val center = LinearLayout(this)

        center.orientation = LinearLayout.VERTICAL

        center.setPadding(
            10,
            10,
            10,
            10
        )

        // PLAYER

        val playerContainer = LinearLayout(this)

        playerContainer.gravity = Gravity.CENTER

        playerContainer.setBackgroundColor(
            Color.BLACK
        )

        playerContainer.addView(
            playerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                420
            )
        )

        center.addView(
            playerContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                430
            )
        )


        // --------------------------------------------------------
        // CHANNELS
        // --------------------------------------------------------

        channelLayout = LinearLayout(this)

        channelLayout.orientation = LinearLayout.VERTICAL

        channelLayout.setPadding(
            10,
            10,
            10,
            10
        )

        center.addView(
            channelLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(
            center,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        setContentView(root)

        buildPackages()
    }


    // ============================================================
    // PACKAGES
    // ============================================================

    private fun buildPackages() {

        packageLayout.removeAllViews()

        val groups = channels
            .map { it.group }
            .distinct()

        groups.forEachIndexed { index, group ->

            val title = packageDisplayName(group)

            val text = TextView(this)

            text.text = title

            text.textSize = 18f

            text.setTextColor(Color.WHITE)

            text.gravity = Gravity.CENTER_VERTICAL

            text.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            text.setPadding(
                20,
                0,
                15,
                0
            )

            text.isFocusable = true

            text.isFocusableInTouchMode = true

            text.setBackgroundColor(
                Color.TRANSPARENT
            )

            text.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    65
                ).apply {
                    bottomMargin = 5
                }

            text.setOnFocusChangeListener { view, hasFocus ->

                if (hasFocus) {

                    view.setBackgroundColor(
                        Color.rgb(35, 90, 170)
                    )

                    view.setPadding(
                        28,
                        0,
                        15,
                        0
                    )

                } else {

                    view.setBackgroundColor(
                        Color.TRANSPARENT
                    )

                    view.setPadding(
                        20,
                        0,
                        15,
                        0
                    )
                }
            }

            text.setOnClickListener {

                showChannels(group)
            }

            packageLayout.addView(text)

            if (index == 0) {
                text.requestFocus()
            }
        }
    }


    // ============================================================
    // CHANNELS
    // ============================================================

    private fun showChannels(group: String) {

        currentGroup = group

        channelLayout.removeAllViews()

        val groupChannels = channels.filter {
            it.group == group
        }

        groupChannels.forEachIndexed { index, channel ->

            val text = TextView(this)

            text.text = channel.name

            text.textSize = 17f

            text.setTextColor(Color.WHITE)

            text.gravity = Gravity.CENTER_VERTICAL

            text.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            text.setPadding(
                20,
                0,
                20,
                0
            )

            text.isFocusable = true

            text.isFocusableInTouchMode = true

            text.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    60
                ).apply {
                    bottomMargin = 5
                }

            text.setBackgroundColor(
                Color.rgb(18, 22, 32)
            )

            text.setOnFocusChangeListener { view, hasFocus ->

                if (hasFocus) {

                    view.setBackgroundColor(
                        Color.rgb(35, 90, 170)
                    )

                } else {

                    view.setBackgroundColor(
                        Color.rgb(18, 22, 32)
                    )
                }
            }

            text.setOnClickListener {

                val realIndex = channels.indexOf(channel)

                playChannel(
                    channel,
                    realIndex
                )
            }

            channelLayout.addView(text)

            if (index == 0) {
                text.requestFocus()
            }
        })
    }


    // ============================================================
    // PACKAGE NAMES
    // ============================================================

    private fun packageDisplayName(group: String): String {

        return when (group) {

            "Alwan" ->
                "🎨 ألوان"

            "الدوري الإيطالي - Serie A" ->
                "🇮🇹 الدوري الإيطالي"

            "StarzPlay" ->
                "⭐ StarzPlay"

            "Shahid" ->
                "🟢 شاهد"

            "الكأس" ->
                "🏆 الكأس"

            "الرابعة العراقية - Al Rabiaa" ->
                "🇮🇶 الرابعة العراقية"

            "Post Sport" ->
                "⚽ Post Sport"

            else ->
                group
        }
    }


    // ============================================================
    // FULLSCREEN
    // ============================================================

    private fun enterFullscreen() {

        if (isFullscreen) return

        isFullscreen = true

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        playerView.layoutParams =
            playerView.layoutParams.apply {
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }

        playerView.requestFocus()
    }


    private fun exitFullscreen() {

        if (!isFullscreen) return

        isFullscreen = false

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        playerView.layoutParams =
            playerView.layoutParams.apply {
                height = 420
            }

        playerView.requestFocus()
    }


    // ============================================================
    // NEXT / PREVIOUS CHANNEL
    // ============================================================

    private fun nextChannel() {

        val groupChannels = channels.filter {
            it.group == currentGroup
        }

        if (groupChannels.isEmpty()) return

        val currentPosition =
            groupChannels.indexOf(currentChannel)

        val nextPosition =
            if (currentPosition < groupChannels.lastIndex)
                currentPosition + 1
            else
                0

        val next = groupChannels[nextPosition]

        val realIndex = channels.indexOf(next)

        playChannel(
            next,
            realIndex
        )
    }


    private fun previousChannel() {

        val groupChannels = channels.filter {
            it.group == currentGroup
        }

        if (groupChannels.isEmpty()) return

        val currentPosition =
            groupChannels.indexOf(currentChannel)

        val previousPosition =
            if (currentPosition > 0)
                currentPosition - 1
            else
                groupChannels.lastIndex

        val previous =
            groupChannels[previousPosition]

        val realIndex =
            channels.indexOf(previous)

        playChannel(
            previous,
            realIndex
        )
    }


    // ============================================================
    // REMOTE CONTROL
    // ============================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (isFullscreen) {

                    playerView.showController()

                    return true
                }
            }

            KeyEvent.KEYCODE_BACK -> {

                if (isFullscreen) {

                    exitFullscreen()

                    return true
                }

                if (isPlayingChannel) {

                    player?.stop()

                    isPlayingChannel = false

                    return true
                }

                return super.dispatchKeyEvent(event)
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (isFullscreen) {

                    nextChannel()

                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (isFullscreen) {

                    previousChannel()

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }


    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }


    // ============================================================
    // PAUSE
    // ============================================================

    override fun onPause() {

        super.onPause()

        player?.pause()
    }


    // ============================================================
    // DESTROY
    // ============================================================

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        player?.release()

        player = null

        super.onDestroy()
    }
}
