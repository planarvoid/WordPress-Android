package com.soundcloud.android.playback.ui;

import android.content.res.Resources;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.java.collections.PropertySet;

public class VideoPlayerAd extends PlayerAd {

    private final VideoAd videoData;

    VideoPlayerAd(VideoAd videoAd, PropertySet source) {
        super(videoAd, source);
        this.videoData = videoAd;
    }

    boolean isVerticalVideo() {
        return videoData.isVerticalVideo();
    }

    boolean isLetterboxVideo() {
        return !videoData.isVerticalVideo();
    }

    VideoSource getFirstSource() {
        return videoData.getFirstSource();
    }

    float getVideoProportion() {
        final VideoSource source = getFirstSource();
        return (float) source.getWidth() / (float) source.getHeight();
    }

    @Override
    String getCallToActionButtonText(Resources resources) {
        return resources.getString(R.string.ads_call_to_action);
    }

}
