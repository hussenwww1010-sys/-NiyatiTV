let mpegPlayer = null;
let hlsPlayer = null;
let currentUrl = null;

const video = document.getElementById("video");

function stopChannel() {
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

    if (video) {
        video.pause();
        video.removeAttribute("src");
        video.load();
    }
}

function loadScript(src) {
    return new Promise((resolve, reject) => {
        const old = document.querySelector(`script[src="${src}"]`);

        if (old) {
            if (window.mpegts || window.Hls) {
                resolve();
            } else {
                old.addEventListener("load", resolve, {once:true});
                old.addEventListener("error", reject, {once:true});
            }
            return;
        }

        const script = document.createElement("script");
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
}

async function playChannel(url) {

    stopChannel();

    currentUrl = url;

    if (!video || !url) {
        return;
    }

    try {

        /* HLS */

        if (/\.m3u8(\?|$)/i.test(url)) {

            await loadScript(
                "https://cdn.jsdelivr.net/npm/hls.js@1.6.15/dist/hls.min.js"
            );

            if (video.canPlayType("application/vnd.apple.mpegurl")) {

                video.src = url;
                video.load();
                await video.play();

                return;
            }

            if (window.Hls && Hls.isSupported()) {

                hlsPlayer = new Hls({
                    enableWorker: true,
                    lowLatencyMode: true,
                    backBufferLength: 30
                });

                hlsPlayer.loadSource(url);
                hlsPlayer.attachMedia(video);

                hlsPlayer.on(
                    Hls.Events.MANIFEST_PARSED,
                    () => video.play().catch(() => {})
                );

                hlsPlayer.on(
                    Hls.Events.ERROR,
                    (event, data) => {

                        if (data.fatal) {
                            console.error("HLS error:", data);
                        }

                    }
                );

                return;
            }

            throw new Error("HLS غير مدعوم");
        }


        /* MPEG-TS */

        await loadScript(
            "https://cdn.jsdelivr.net/npm/mpegts.js@1.8.0/dist/mpegts.min.js"
        );

        if (!window.mpegts || !mpegts.isSupported()) {
            throw new Error(
                "المتصفح لا يدعم MPEG-TS عبر MSE"
            );
        }

        mpegPlayer = mpegts.createPlayer(
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
                stashInitialSize: 384 * 1024
            }
        );

        mpegPlayer.attachMediaElement(video);

        mpegPlayer.on(
            mpegts.Events.ERROR,
            (type, detail, info) => {
                console.error(
                    "MPEG-TS error:",
                    type,
                    detail,
                    info
                );
            }
        );

        mpegPlayer.load();

        await mpegPlayer.play();

    } catch (error) {

        console.error(
            "Niyati TV Player:",
            error
        );

        alert(
            "تعذر تشغيل البث من المتصفح.\n\n" +
            "قد يكون مصدر البث لا يسمح باتصالات المتصفح " +
            "أو يستخدم HTTP/CORS غير مسموح به."
        );
    }
}
