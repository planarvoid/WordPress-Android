package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.tracks.TrackItem;

import android.content.res.Resources;

public class VideoPlayerAd extends PlayerAd {

    private final VideoAd videoData;

    VideoPlayerAd(VideoAd videoAd) {
        super(videoAd, TrackItem.EMPTY);
        this.videoData = videoAd;
    }

    boolean isVerticalVideo() {
        return videoData.isVerticalVideo();
    }

    boolean isLetterboxVideo() {
        return !videoData.isVerticalVideo();
    }

    VideoAdSource getFirstSource() {
        return videoData.getFirstSource();
    }

    float getVideoProportion() {
        final VideoAdSource source = getFirstSource();
        return (float) source.getWidth() / (float) source.getHeight();
    }

    @Override
    String getCallToActionButtonText(Resources resources) {
        return resources.getString(R.string.ads_call_to_action);
    }

}
