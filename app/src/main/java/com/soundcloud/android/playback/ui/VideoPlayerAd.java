package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;

class VideoPlayerAd extends PlayerAd {

    private final VideoAd videoData;

    VideoPlayerAd(VideoAd videoAd) {
        super(videoAd);
        this.videoData = videoAd;
    }

    boolean isVerticalVideo() {
        return videoData.isVerticalVideo();
    }

    boolean isLetterboxVideo() {
        return !videoData.isVerticalVideo();
    }

    VideoAdSource getFirstSource() {
        return videoData.firstVideoSource();
    }

    float getVideoProportion() {
        final VideoAdSource source = getFirstSource();
        return (float) source.width() / (float) source.height();
    }
}
