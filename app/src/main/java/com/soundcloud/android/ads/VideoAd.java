package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class VideoAd extends PlayerAdData {

    private static VideoAd create(ApiVideoAd apiVideoAd) {
        final ApiAdTracking videoTracking = apiVideoAd.getVideoTracking();
        return new AutoValue_VideoAd(
                apiVideoAd.getAdUrn(),
                videoTracking.impressionUrls,
                videoTracking.startUrls,
                videoTracking.finishUrls,
                videoTracking.skipUrls,
                videoTracking.firstQuartileUrls,
                videoTracking.secondQuartileUrls,
                videoTracking.thirdQuartileUrls,
                videoTracking.pauseUrls,
                videoTracking.resumeUrls,
                apiVideoAd.isSkippable(),
                Optional.of(VisualAdDisplayProperties.create(apiVideoAd.getDisplayProperties())),
                Lists.transform(apiVideoAd.getVideoSources(), ApiVideoSource.toVideoSource),
                apiVideoAd.getClickThroughUrl(),
                videoTracking.clickUrls,
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

    public abstract String getClickThroughUrl();

    public abstract List<String> getClickUrls();

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
