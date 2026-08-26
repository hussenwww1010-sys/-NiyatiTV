/* =========================================
   NIYATI TV - WEB PLAYER
   MPEG-TS / HLS PLAYER
========================================= */

let mpegPlayer = null;
let hlsPlayer = null;
let reconnectTimer = null;
let currentUrl = null;
let reconnectAttempts = 0;

const video = document.getElementById("video");
const playerBox = document.getElementById("player");


/* =========================================
   LOAD MPEG-TS LIBRARY
========================================= */

function loadMpegTS() {

    return new Promise((resolve, reject) => {

        if (window.mpegts) {
            resolve();
            return;
        }

        const script = document.createElement("script");

        script.src =
            "https://cdn.jsdelivr.net/npm/mpegts.js@1.8.0/dist/mpegts.min.js";

        script.onload = () => resolve();

        script.onerror = () =>
            reject(
                new Error("تعذر تحميل مشغل MPEG-TS")
            );

        document.head.appendChild(script);

    });

}


/* =========================================
   LOAD HLS
========================================= */

function loadHLS() {

    return new Promise((resolve, reject) => {

        if (window.Hls) {
            resolve();
            return;
        }

        const script = document.createElement("script");

        script.src =
            "https://cdn.jsdelivr.net/npm/hls.js@1.6.15/dist/hls.min.js";

        script.onload = () => resolve();

        script.onerror = () =>
            reject(
                new Error("تعذر تحميل مشغل HLS")
            );

        document.head.appendChild(script);

    });

}


/* =========================================
   STOP CURRENT PLAYER
========================================= */

function stopChannel() {

    clearTimeout(reconnectTimer);

    reconnectTimer = null;

    reconnectAttempts = 0;


    /* MPEG-TS */

    if (mpegPlayer) {

        try {
            mpegPlayer.pause();
        } catch (e) {}

        try {
            mpegPlayer.unload();
        } catch (e) {}

        try {
            mpegPlayer.detachMediaElement();
        } catch (e) {}

        try {
            mpegPlayer.destroy();
        } catch (e) {}

        mpegPlayer = null;

    }


    /* HLS */

    if (hlsPlayer) {

        try {
            hlsPlayer.destroy();
        } catch (e) {}

        hlsPlayer = null;

    }


    if (video) {

        video.pause();

        video.removeAttribute("src");

        video.removeAttribute("poster");

        video.load();

    }

}


/* =========================================
   PLAY CHANNEL
========================================= */

async function playChannel(url) {

    if (!url) {

        alert("رابط القناة غير موجود");

        return;

    }


    currentUrl = url;

    reconnectAttempts = 0;


    stopChannel();


    try {

        /*
         * HLS
         */

        if (
            url.toLowerCase().includes(".m3u8")
        ) {

            await playHLS(url);

            return;

        }


        /*
         * MPEG-TS
         */

        await playMPEGTS(url);

    }

    catch (error) {

        console.error(
            "NIYATI PLAYER ERROR:",
            error
        );

        showPlayerMessage(
            "تعذر تشغيل البث"
        );

    }

}


/* =========================================
   MPEG-TS
========================================= */

async function playMPEGTS(url) {

    await loadMpegTS();


    if (
        !window.mpegts ||
        !mpegts.isSupported()
    ) {

        /*
         * fallback
         */

        video.src = url;

        video.load();

        await video.play();

        return;

    }


    mpegPlayer =
        mpegts.createPlayer(

            {
                type: "mpegts",

                url: url,

                isLive: true

            },

            {

                enableWorker: true,

                lazyLoad: false,

                autoCleanupSourceBuffer: true,

                liveBufferLatencyChasing: true,

                liveSync: false,

                stashInitialSize: 384 * 1024,

                seekType: "range",

                deferLoadAfterSourceOpen: false

            }

        );


    mpegPlayer.attachMediaElement(
        video
    );


    /*
     * ERROR HANDLING
     */

    mpegPlayer.on(
        mpegts.Events.ERROR,
        function(errorType, errorDetail, errorInfo) {

            console.error(
                "MPEG-TS ERROR:",
                errorType,
                errorDetail,
                errorInfo
            );

            reconnect();

        }
    );


    /*
     * BUFFERING
     */

    mpegPlayer.on(
        mpegts.Events.STATISTICS_INFO,
        function() {

            reconnectAttempts = 0;

        }
    );


    /*
     * START
     */

    mpegPlayer.load();


    try {

        await mpegPlayer.play();

    }

    catch (error) {

        console.log(
            "Autoplay blocked:",
            error
        );

        showPlayerMessage(
            "اضغط على الشاشة لتشغيل القناة"
        );

    }

}


/* =========================================
   HLS PLAYER
========================================= */

async function playHLS(url) {

    await loadHLS();


    /*
     * Native HLS
     */

    if (
        video.canPlayType(
            "application/vnd.apple.mpegurl"
        )
    ) {

        video.src = url;

        video.load();

        try {

            await video.play();

        }

        catch (error) {

            console.log(error);

        }

        return;

    }


    /*
     * HLS.js
     */

    if (
        window.Hls &&
        Hls.isSupported()
    ) {

        hlsPlayer =
            new Hls({

                enableWorker: true,

                lowLatencyMode: true,

                backBufferLength: 30,

                maxBufferLength: 10,

                maxMaxBufferLength: 20,

                liveSyncDurationCount: 3,

                liveMaxLatencyDurationCount: 6

            });


        hlsPlayer.loadSource(url);


        hlsPlayer.attachMedia(
            video
        );


        hlsPlayer.on(
            Hls.Events.MANIFEST_PARSED,
            function() {

                video.play()
                    .catch(
                        error =>
                            console.log(error)
                    );

            }
        );


        hlsPlayer.on(
            Hls.Events.ERROR,
            function(
                event,
                data
            ) {

                console.error(
                    "HLS ERROR:",
                    data
                );


                if (
                    data.fatal
                ) {

                    switch (
                        data.type
                    ) {

                        case Hls.ErrorTypes.NETWORK_ERROR:

                            hlsPlayer.startLoad();

                            break;


                        case Hls.ErrorTypes.MEDIA_ERROR:

                            hlsPlayer.recoverMediaError();

                            break;


                        default:

                            reconnect();

                            break;

                    }

                }

            }
        );

        return;

    }


    throw new Error(
        "HLS غير مدعوم"
    );

}


/* =========================================
   AUTO RECONNECT
========================================= */

function reconnect() {

    if (!currentUrl) {
        return;
    }


    if (
        reconnectTimer
    ) {

        return;

    }


    reconnectAttempts++;


    /*
     * حد أقصى للمحاولات
     */

    if (
        reconnectAttempts > 8
    ) {

        showPlayerMessage(
            "تعذر الاتصال بالبث"
        );

        return;

    }


    showPlayerMessage(
        "جاري إعادة الاتصال بالبث..."
    );


    const delay =
        Math.min(
            2000 * reconnectAttempts,
            10000
        );


    reconnectTimer =
        setTimeout(

            async function() {

                reconnectTimer = null;

                try {

                    await playChannel(
                        currentUrl
                    );

                }

                catch (error) {

                    console.error(
                        error
                    );

                }

            },

            delay

        );

}


/* =========================================
   VIDEO EVENTS
========================================= */

if (video) {


    video.addEventListener(
        "waiting",
        function() {

            console.log(
                "NIYATI: buffering..."
            );

        }
    );


    video.addEventListener(
        "playing",
        function() {

            reconnectAttempts = 0;

            hidePlayerMessage();

        }
    );


    video.addEventListener(
        "error",
        function() {

            console.error(
                "HTML VIDEO ERROR",
                video.error
            );

        }
    );

}


/* =========================================
   PLAYER MESSAGE
========================================= */

function showPlayerMessage(message) {

    let messageBox =
        document.getElementById(
            "niyatiPlayerMessage"
        );


    if (!messageBox) {

        messageBox =
            document.createElement(
                "div"
            );


        messageBox.id =
            "niyatiPlayerMessage";


        messageBox.style.position =
            "absolute";


        messageBox.style.left =
            "50%";


        messageBox.style.top =
            "50%";


        messageBox.style.transform =
            "translate(-50%,-50%)";


        messageBox.style.zIndex =
            "10001";


        messageBox.style.background =
            "rgba(0,0,0,.85)";


        messageBox.style.color =
            "white";


        messageBox.style.padding =
            "18px 25px";


        messageBox.style.borderRadius =
            "14px";


        messageBox.style.fontSize =
            "15px";


        messageBox.style.textAlign =
            "center";


        playerBox.appendChild(
            messageBox
        );

    }


    messageBox.textContent =
        message;


    messageBox.style.display =
        "block";

}


/* =========================================
   HIDE MESSAGE
========================================= */

function hidePlayerMessage() {

    const box =
        document.getElementById(
            "niyatiPlayerMessage"
        );


    if (box) {

        box.style.display =
            "none";

    }

}


/* =========================================
   FULLSCREEN
========================================= */

function enterFullscreen() {

    if (!playerBox) {
        return;
    }


    if (
        playerBox.requestFullscreen
    ) {

        playerBox.requestFullscreen();

    }

    else if (
        video.webkitEnterFullscreen
    ) {

        video.webkitEnterFullscreen();

    }

}


/* =========================================
   VIDEO CLICK
========================================= */

if (video) {

    video.addEventListener(
        "dblclick",
        function() {

            enterFullscreen();

        }
    );

}


/* =========================================
   BACK / ESC
========================================= */

document.addEventListener(
    "keydown",
    function(event) {

        if (
            event.key === "Escape" ||
            event.key === "Backspace"
        ) {

            if (
                playerBox &&
                playerBox.style.display === "flex"
            ) {

                if (
                    document.fullscreenElement
                ) {

                    document.exitFullscreen();

                    event.preventDefault();

                    return;

                }

                stopChannel();

            }

        }

    }
);
