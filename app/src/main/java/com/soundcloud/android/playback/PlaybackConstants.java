package com.soundcloud.android.playback;

public interface PlaybackConstants {
    long PROGRESS_DELAY_MS = 500L;

    // Video playback constants
    String MIME_TYPE_AVC = "video/avc";
    String MIME_TYPE_MP4 = "video/mp4";

    int MAX_BITRATE_KBPS_WIFI = 4000;
    int MAX_BITRATE_KBPS_4G = 2000;
    int MAX_BITRATE_KPBS_3G = 750;
    int MAX_BITRATE_KBPS_2G = 250;

    int RESOLUTION_PX_1080P = 1080;
    int RESOLUTION_PX_720P = 720;
    int RESOLUTION_PX_480P = 480;
    int RESOLUTION_PX_360P = 360;
}
