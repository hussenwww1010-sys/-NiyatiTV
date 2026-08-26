let currentVideo = null;

function playChannel(url) {

    const video = document.getElementById("video");

    if (!video || !url) {
        return;
    }

    currentVideo = url;

    video.pause();

    video.removeAttribute("src");

    video.load();

    video.src = url;

    video.controls = true;

    video.autoplay = true;

    video.load();

    video.play().catch(function(error) {

        console.log(
            "Playback waiting for user interaction:",
            error
        );

    });

}


function stopChannel() {

    const video = document.getElementById("video");

    if (!video) {
        return;
    }

    video.pause();

    video.removeAttribute("src");

    video.load();

    currentVideo = null;

}
