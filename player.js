let currentVideo = null;

function playChannel(url) {
    const player = document.getElementById("video");

    if (!player || !url) return;

    currentVideo = url;

    player.src = url;
    player.controls = true;

    player.play().catch(() => {
        console.log("Playback requires user interaction");
    });
}

function stopChannel() {
    const player = document.getElementById("video");

    if (!player) return;

    player.pause();
    player.removeAttribute("src");
    player.load();

    currentVideo = null;
}
