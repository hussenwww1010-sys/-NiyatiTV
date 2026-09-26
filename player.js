let mpegPlayer = null;
let hlsPlayer = null;
let dashPlayer = null;
let currentUrl = null;
let retryTimer = null;
let retryCount = 0;

const MAX_RETRIES = 5;
const RETRY_DELAY = 3000;

const video = document.getElementById("video");

/* =========================================================
   STOP EVERYTHING
========================================================= */

function stopChannel() {

    clearTimeout(retryTimer);
    retryTimer = null;

    try {
        if (mpegPlayer) {
            mpegPlayer.pause();
            mpegPlayer.unload();
            mpegPlayer.detachMediaElement();
            mpegPlayer.destroy();
        }
    } catch (e) {}

    mpegPlayer = null;

    try {
        if (hlsPlayer) {
            hlsPlayer.destroy();
        }
    } catch (e) {}

    hlsPlayer = null;

    try {
        if (dashPlayer) {
            dashPlayer.reset();
        }
    } catch (e) {}

    dashPlayer = null;

    if (video) {
        try {
            video.pause();
            video.removeAttribute("src");
            video.load();
        } catch (e) {}
    }
}


/* =========================================================
   LOAD EXTERNAL SCRIPT
========================================================= */

function loadScript(src) {

    return new Promise((resolve, reject) => {

        const existing = document.querySelector(
            `script[src="${src}"]`
        );

        if (existing) {

            if (
                src.includes("hls.js") &&
                window.Hls
            ) {
                resolve();
                return;
            }

            if (
                src.includes("mpegts.js") &&
                window.mpegts
            ) {
                resolve();
                return;
            }

            if (
                src.includes("dash.all") &&
                window.dashjs
            ) {
                resolve();
                return;
            }

            existing.addEventListener(
                "load",
                resolve,
                { once: true }
            );

            existing.addEventListener(
                "error",
                reject,
                { once: true }
            );

            return;
        }

        const script = document.createElement("script");

        script.src = src;
        script.async = true;

        script.onload = resolve;
        script.onerror = () =>
            reject(
                new Error(
                    "تعذر تحميل مكتبة المشغل"
                )
            );

        document.head.appendChild(script);
    });
}


/* =========================================================
   DETECT STREAM TYPE
========================================================= */

function detectStreamType(url) {

    if (!url) {
        return "unknown";
    }

    const cleanUrl =
        url
            .split("#")[0]
            .split("?")[0]
            .toLowerCase();

    /* HLS */

    if (
        cleanUrl.endsWith(".m3u8") ||
        url.toLowerCase().includes(".m3u8?")
    ) {
        return "hls";
    }


    /* MPEG DASH */

    if (
        cleanUrl.endsWith(".mpd") ||
        url.toLowerCase().includes(".mpd?")
    ) {
        return "dash";
    }


    /* MPEG TS */

    if (
        cleanUrl.endsWith(".ts") ||
        cleanUrl.endsWith(".mpegts") ||
        cleanUrl.endsWith(".m2ts")
    ) {
        return "mpegts";
    }


    /* MP4 */

    if (
        cleanUrl.endsWith(".mp4") ||
        cleanUrl.endsWith(".m4v")
    ) {
        return "mp4";
    }


    /* WebM */

    if (
        cleanUrl.endsWith(".webm")
    ) {
        return "webm";
    }


    /* OGG */

    if (
        cleanUrl.endsWith(".ogv") ||
        cleanUrl.endsWith(".ogg")
    ) {
        return "ogg";
    }


    /* RTMP */

    if (
        url.toLowerCase().startsWith("rtmp://")
    ) {
        return "rtmp";
    }


    /* RTSP */

    if (
        url.toLowerCase().startsWith("rtsp://")
    ) {
        return "rtsp";
    }


    /* Unknown */

    return "unknown";
}


/* =========================================================
   NATIVE VIDEO
========================================================= */

async function playNative(url, mimeType = "") {

    if (!video) {
        throw new Error("عنصر الفيديو غير موجود");
    }

    return new Promise((resolve, reject) => {

        let finished = false;

        const cleanup = () => {

            video.removeEventListener(
                "loadedmetadata",
                success
            );

            video.removeEventListener(
                "canplay",
                success
            );

            video.removeEventListener(
                "error",
                failed
            );
        };

        const success = () => {

            if (finished) return;

            finished = true;

            cleanup();

            video.play()
                .catch(() => {});

            resolve();
        };


        const failed = () => {

            if (finished) return;

            finished = true;

            cleanup();

            reject(
                new Error(
                    "المتصفح رفض تشغيل مصدر الفيديو"
                )
            );
        };


        video.addEventListener(
            "loadedmetadata",
            success
        );

        video.addEventListener(
            "canplay",
            success
        );

        video.addEventListener(
            "error",
            failed
        );


        if (mimeType) {

            video.setAttribute(
                "type",
                mimeType
            );
        }


        video.src = url;

        video.load();

    });
}


/* =========================================================
   HLS
========================================================= */

async function playHLS(url) {

    await loadScript(
        "https://cdn.jsdelivr.net/npm/hls.js@1.6.15/dist/hls.min.js"
    );


    /* Safari / iPhone / native HLS */

    if (
        video.canPlayType(
            "application/vnd.apple.mpegurl"
        )
    ) {

        await playNative(
            url,
            "application/vnd.apple.mpegurl"
        );

        return;
    }


    /* HLS.js */

    if (
        window.Hls &&
        Hls.isSupported()
    ) {

        hlsPlayer = new Hls({

            enableWorker: true,

            lowLatencyMode: true,

            backBufferLength: 30,

            maxBufferLength: 30,

            maxMaxBufferLength: 60,

            liveSyncDurationCount: 3,

            liveMaxLatencyDurationCount: 6,

            enableWebVTT: true,

            enableIMSC1: true,

            enableCEA708Captions: true
        });


        hlsPlayer.loadSource(url);

        hlsPlayer.attachMedia(video);


        hlsPlayer.on(
            Hls.Events.MANIFEST_PARSED,
            () => {

                video.play()
                    .catch(() => {});

            }
        );


        hlsPlayer.on(
            Hls.Events.ERROR,
            (event, data) => {

                console.error(
                    "HLS:",
                    data
                );


                if (!data.fatal) {
                    return;
                }


                switch (data.type) {

                    case Hls.ErrorTypes.NETWORK_ERROR:

                        console.warn(
                            "HLS network error — reconnecting"
                        );

                        try {
                            hlsPlayer.startLoad();
                        } catch (e) {}

                        break;


                    case Hls.ErrorTypes.MEDIA_ERROR:

                        console.warn(
                            "HLS media error — recovering"
                        );

                        try {
                            hlsPlayer.recoverMediaError();
                        } catch (e) {}

                        break;


                    default:

                        scheduleRetry(
                            url
                        );

                        break;
                }

            }
        );

        return;
    }


    throw new Error(
        "هذا المتصفح لا يدعم HLS"
    );
}


/* =========================================================
   MPEG DASH
========================================================= */

async function playDASH(url) {

    await loadScript(
        "https://cdn.jsdelivr.net/npm/dashjs@5.0.0/dist/modern/umd/dash.all.min.js"
    );


    if (
        !window.dashjs
    ) {
        throw new Error(
            "DASH.js غير متوفر"
        );
    }


    dashPlayer =
        dashjs.MediaPlayer().create();


    dashPlayer.initialize(
        video,
        url,
        true
    );


    dashPlayer.updateSettings({

        streaming: {

            buffer: {

                fastSwitchEnabled: true,

                stableBufferTime: 12,

                bufferTimeAtTopQuality: 20,

                bufferTimeAtTopQualityLongForm: 30
            },

            lowLatencyEnabled: true
        }

    });


    dashPlayer.on(
        dashjs.MediaPlayer.events.ERROR,
        (error) => {

            console.error(
                "DASH error:",
                error
            );

        }
    );
}


/* =========================================================
   MPEG TS
========================================================= */

async function playMPEGTS(url) {

    await loadScript(
        "https://cdn.jsdelivr.net/npm/mpegts.js@1.8.0/dist/mpegts.min.js"
    );


    if (
        !window.mpegts ||
        !mpegts.isSupported()
    ) {

        throw new Error(
            "المتصفح لا يدعم MPEG-TS عبر MSE"
        );
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

                liveBufferLatencyMaxLatency: 3,

                liveBufferLatencyMinRemain: 1,

                stashInitialSize:
                    384 * 1024
            }

        );


    mpegPlayer.attachMediaElement(
        video
    );


    mpegPlayer.on(
        mpegts.Events.ERROR,
        (
            type,
            detail,
            info
        ) => {

            console.error(
                "MPEG-TS error:",
                type,
                detail,
                info
            );


            scheduleRetry(
                url
            );

        }
    );


    mpegPlayer.load();

    await mpegPlayer.play();
}


/* =========================================================
   MP4
========================================================= */

async function playMP4(url) {

    await playNative(
        url,
        "video/mp4"
    );
}


/* =========================================================
   WEBM
========================================================= */

async function playWebM(url) {

    await playNative(
        url,
        "video/webm"
    );
}


/* =========================================================
   OGG
========================================================= */

async function playOGG(url) {

    await playNative(
        url,
        "video/ogg"
    );
}


/* =========================================================
   RETRY
========================================================= */

function scheduleRetry(url) {

    if (
        retryCount >= MAX_RETRIES
    ) {

        console.error(
            "Maximum retries reached"
        );

        showPlayerError(
            "انقطع البث بعد عدة محاولات."
        );

        return;
    }


    retryCount++;


    clearTimeout(
        retryTimer
    );


    console.log(
        `إعادة المحاولة ${retryCount}/${MAX_RETRIES}`
    );


    retryTimer =
        setTimeout(
            () => {

                playChannel(
                    url,
                    true
                );

            },
            RETRY_DELAY
        );
}


/* =========================================================
   MAIN PLAYER
========================================================= */

async function playChannel(
    url,
    isRetry = false
) {

    if (!url) {

        showPlayerError(
            "رابط البث فارغ."
        );

        return;
    }


    if (!isRetry) {
        retryCount = 0;
    }


    stopChannel();


    currentUrl = url;


    if (!video) {

        console.error(
            "video element not found"
        );

        return;
    }


    const type =
        detectStreamType(url);


    console.log(
        "NIYATI TV STREAM:",
        type,
        url
    );


    try {

        switch (type) {


            /* ================= HLS ================= */

            case "hls":

                await playHLS(
                    url
                );

                break;


            /* ================= DASH ================= */

            case "dash":

                await playDASH(
                    url
                );

                break;


            /* ================= MPEG TS ================= */

            case "mpegts":

                await playMPEGTS(
                    url
                );

                break;


            /* ================= MP4 ================= */

            case "mp4":

                await playMP4(
                    url
                );

                break;


            /* ================= WEBM ================= */

            case "webm":

                await playWebM(
                    url
                );

                break;


            /* ================= OGG ================= */

            case "ogg":

                await playOGG(
                    url
                );

                break;


            /* ================= RTMP ================= */

            case "rtmp":

                throw new Error(
                    "RTMP غير مدعوم مباشرة داخل المتصفح الحديث."
                );


            /* ================= RTSP ================= */

            case "rtsp":

                throw new Error(
                    "RTSP غير مدعوم مباشرة داخل عنصر HTML5 Video."
                );


            /* ================= UNKNOWN ================= */

            default:

                /*
                 * إذا الرابط غير معروف،
                 * نجرب أولاً HLS،
                 * وإذا فشل نجرب MPEG-TS،
                 * وبعدها Native Video.
                 */

                try {

                    await playHLS(
                        url
                    );

                } catch (hlsError) {

                    console.warn(
                        "HLS failed:",
                        hlsError
                    );


                    stopChannel();


                    try {

                        await playMPEGTS(
                            url
                        );

                    } catch (tsError) {

                        console.warn(
                            "MPEG-TS failed:",
                            tsError
                        );


                        stopChannel();


                        await playNative(
                            url
                        );
                    }
                }

                break;
        }


        console.log(
            "NIYATI TV: Playback started"
        );


        retryCount = 0;


    } catch (error) {

        console.error(
            "NIYATI TV Player Error:",
            error
        );


        scheduleRetry(
            url
        );


        showPlayerError(
            getReadableError(
                error,
                type
            )
        );
    }
}


/* =========================================================
   READABLE ERROR
========================================================= */

function getReadableError(
    error,
    type
) {

    const message =
        error &&
        error.message
            ? error.message
            : "";


    if (
        message
            .toLowerCase()
            .includes("cors")
    ) {

        return (
            "البث رفض اتصال المتصفح بسبب CORS."
        );
    }


    if (
        message
            .toLowerCase()
            .includes("network")
    ) {

        return (
            "تعذر الاتصال بسيرفر البث."
        );
    }


    if (
        type === "rtsp"
    ) {

        return (
            "RTSP لا يعمل مباشرة داخل Web Player."
        );
    }


    if (
        type === "rtmp"
    ) {

        return (
            "RTMP لا يعمل مباشرة داخل Web Player."
        );
    }


    return (
        "تعذر تشغيل البث.\n\n" +
        "نوع البث: " +
        type +
        "\n\n" +
        (message || "خطأ غير معروف")
    );
}


/* =========================================================
   PLAYER ERROR UI
========================================================= */

function showPlayerError(
    message
) {

    console.error(
        "NIYATI TV:",
        message
    );


    /*
     * إذا عندك عنصر مخصص للأخطاء
     * باسم playerError راح نستخدمه.
     */

    const errorBox =
        document.getElementById(
            "playerError"
        );


    if (errorBox) {

        errorBox.textContent =
            message;

        errorBox.style.display =
            "block";

        return;
    }


    /*
     * إذا ما موجود عنصر خطأ،
     * ما نستخدم alert حتى ما نزعج المستخدم.
     */

    console.warn(
        message
    );
}


/* =========================================================
   VIDEO EVENTS
========================================================= */

if (video) {


    video.addEventListener(
        "error",
        () => {

            if (
                video.error
            ) {

                console.error(
                    "HTML5 VIDEO ERROR:",
                    video.error.code,
                    video.error.message
                );

                if (
                    currentUrl
                ) {

                    scheduleRetry(
                        currentUrl
                    );
                }
            }

        }
    );


    video.addEventListener(
        "waiting",
        () => {

            console.log(
                "NIYATI TV: Buffering..."
            );

        }
    );


    video.addEventListener(
        "playing",
        () => {

            console.log(
                "NIYATI TV: Playing"
            );

        }
    );


    video.addEventListener(
        "stalled",
        () => {

            console.warn(
                "NIYATI TV: Stream stalled"
            );

            if (
                currentUrl
            ) {

                scheduleRetry(
                    currentUrl
                );
            }

        }
    );


    video.addEventListener(
        "ended",
        () => {

            console.warn(
                "NIYATI TV: Stream ended"
            );

            if (
                currentUrl
            ) {

                scheduleRetry(
                    currentUrl
                );
            }

        }
    );
}


/* =========================================================
   GLOBAL ACCESS
========================================================= */

window.NiyatiPlayer = {

    play: playChannel,

    stop: stopChannel,

    detectType: detectStreamType,

    getCurrentUrl: () =>
        currentUrl

};
