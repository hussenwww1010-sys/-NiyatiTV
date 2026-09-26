const channels = [

    // =====================================================
‎    // قنوات تجريبية مجانية ومفتوحة (لاختبار المشغّل فقط)
‎    // كل الروابط أدناه هي بث/محتوى تجريبي عام يوفره
‎    // مالكوه رسميًا للاستخدام والاختبار المفتوح
    // =====================================================

    {
        id: "demo-nasa-tv",
        name: "NASA TV Public",
        package: "تجربة مجانية",
        qualities: [
            {
                name: "HLS",
                url: "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8"
            }
        ]
    },

    {
        id: "demo-bigbuckbunny",
        name: "Big Buck Bunny (Demo)",
        package: "تجربة مجانية",
        qualities: [
            {
                name: "HLS",
                url: "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
            }
        ]
    },

    {
        id: "demo-apple-bipbop",
        name: "Apple HLS Test Stream",
        package: "تجربة مجانية",
        qualities: [
            {
                name: "HLS",
                url: "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
            }
        ]
    },

    {
        id: "demo-sintel",
        name: "Sintel (Blender / CC BY)",
        package: "تجربة مجانية",
        qualities: [
            {
                name: "MP4",
                url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            }
        ]
    }

];
