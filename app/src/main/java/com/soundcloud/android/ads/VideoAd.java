package com.soundcloud.android.ads;

import android.net.Uri;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;

import java.util.List;

@AutoValue
public abstract class VideoAd extends PlayerAdData {

    private static VideoAd create(ApiVideoAd apiVideoAd) {
        final ApiVideoTracking videoTracking = apiVideoAd.getVideoTracking();
        return new AutoValue_VideoAd(
                apiVideoAd.getAdUrn(),
                videoTracking.impressionUrls,
                videoTracking.finishUrls,
                videoTracking.skipUrls,
                VisualAdDisplayProperties.create(apiVideoAd.getDisplayProperties()),
                Lists.transform(apiVideoAd.getVideoSources(), ApiVideoSource.toVideoSource),
                Uri.parse(apiVideoAd.getClickThroughUrl()),
                videoTracking.startUrls,
                videoTracking.firstQuartileUrls,
                videoTracking.secondQuartileUrls,
                videoTracking.thirdQuartileUrls,
                videoTracking.pauseUrls,
                videoTracking.resumeUrls,
                videoTracking.fullScreenUrls,
                videoTracking.exitFullScreenUrls
        );
    }

    public static VideoAd create(ApiVideoAd apiVideoAd, Urn monetizableTrackUrn) {
        VideoAd videoAd = create(apiVideoAd);
        videoAd.setMonetizableTrackUrn(monetizableTrackUrn);
        return videoAd;
    }

    public abstract List<VideoSource> getVideoSources();

    public abstract Uri getClickThroughUrl();

    public abstract List<String> getStartUrls();

    public abstract List<String> getFirstQuartileUrls();

    public abstract List<String> getSecondQuartileUrls();

    public abstract List<String> getThirdQuartileUrls();

    public abstract List<String> getPauseUrls();

    public abstract List<String> getResumeUrls();

    public abstract List<String> getFullScreenUrls();

    public abstract List<String> getExitFullScreenUrls();

    public VideoSource getFirstSource() {
        return getVideoSources().get(0);
    }

    public boolean isVerticalVideo() {
        final VideoSource source = getFirstSource();
        return source.getHeight() > source.getWidth();
    }

}
