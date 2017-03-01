package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class VideoAd extends PlayableAdData implements ExpirableAd {

    public static VideoAd create(ApiVideoAd apiVideoAd, long createdAt) {
        final ApiAdTracking videoTracking = apiVideoAd.getVideoTracking();
        return new AutoValue_VideoAd(
                apiVideoAd.getAdUrn(),
                apiVideoAd.getCallToActionButtonText(),
                videoTracking.impressionUrls,
                videoTracking.startUrls,
                videoTracking.finishUrls,
                videoTracking.skipUrls,
                videoTracking.firstQuartileUrls,
                videoTracking.secondQuartileUrls,
                videoTracking.thirdQuartileUrls,
                videoTracking.pauseUrls,
                videoTracking.resumeUrls,
                videoTracking.clickUrls,
                apiVideoAd.isSkippable(),
                Optional.of(VisualAdDisplayProperties.create(apiVideoAd.getDisplayProperties())),
                apiVideoAd.getTitle(),
                createdAt,
                apiVideoAd.getExpiryInMins(),
                Lists.transform(apiVideoAd.getVideoSources(), VideoAdSource::create),
                apiVideoAd.getClickThroughUrl(),
                videoTracking.fullScreenUrls,
                videoTracking.exitFullScreenUrls
        );
    }

    public static VideoAd create(ApiVideoAd apiVideoAd, long createdAt, Urn monetizableTrackUrn) {
        VideoAd videoAd = create(apiVideoAd, createdAt);
        videoAd.setMonetizableTrackUrn(monetizableTrackUrn);
        return videoAd;
    }

    public abstract Optional<String> getTitle();

    public abstract long getCreatedAt();

    public abstract int getExpiryInMins();

    public abstract List<VideoAdSource> getVideoSources();

    public abstract String getClickThroughUrl();

    public abstract List<String> getFullScreenUrls();

    public abstract List<String> getExitFullScreenUrls();

    public VideoAdSource getFirstSource() {
        return getVideoSources().get(0);
    }

    public boolean isVerticalVideo() {
        final VideoAdSource source = getFirstSource();
        return source.getHeight() > source.getWidth();
    }
}
