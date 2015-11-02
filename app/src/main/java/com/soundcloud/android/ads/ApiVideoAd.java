package com.soundcloud.android.ads;

import android.net.Uri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.List;

class ApiVideoAd implements PropertySetSource {
    private final String adUrn;
    private final List<ApiVideoSource> videoSources;
    private final ApiCompanionAd visualAd;
    private final ApiVideoTracking videoTracking;

    @JsonCreator
    public ApiVideoAd(@JsonProperty("urn") String adUrn,
                      @JsonProperty("video_sources") List<ApiVideoSource> videoSources,
                      @JsonProperty("video_tracking") ApiVideoTracking videoTracking,
                      @JsonProperty("visual_ad") ApiCompanionAd visualAd) {
        this.adUrn = adUrn;
        this.videoSources = videoSources;
        this.visualAd = visualAd;
        this.videoTracking = videoTracking;
    }

    public Optional<ApiCompanionAd> getVisualAd() {
       return (visualAd != null) ? Optional.of(visualAd) : Optional.<ApiCompanionAd>absent();
    }

    public List<ApiVideoSource> getVideoSources() {
        return videoSources;
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                VideoAdProperty.AD_URN.bind(adUrn),
                VideoAdProperty.AD_TYPE.bind(AdProperty.AD_TYPE_VIDEO),
                VideoAdProperty.COMPANION_URN.bind(visualAd.urn),
                VideoAdProperty.ARTWORK.bind(Uri.parse(visualAd.imageUrl)),
                VideoAdProperty.CLICK_THROUGH_LINK.bind(Uri.parse(visualAd.clickthroughUrl)),
                VideoAdProperty.DEFAULT_TEXT_COLOR.bind(visualAd.displayProperties.defaultTextColor),
                VideoAdProperty.DEFAULT_BACKGROUND_COLOR.bind(visualAd.displayProperties.defaultBackgroundColor),
                VideoAdProperty.PRESSED_TEXT_COLOR.bind(visualAd.displayProperties.pressedTextColor),
                VideoAdProperty.PRESSED_BACKGROUND_COLOR.bind(visualAd.displayProperties.pressedBackgroundColor),
                VideoAdProperty.FOCUSED_TEXT_COLOR.bind(visualAd.displayProperties.focusedTextColor),
                VideoAdProperty.FOCUSED_BACKGROUND_COLOR.bind(visualAd.displayProperties.focusedBackgroundColor),
                VideoAdProperty.AD_CLICKTHROUGH_URLS.bind(visualAd.trackingClickUrls),
                VideoAdProperty.AD_COMPANION_DISPLAY_IMPRESSION_URLS.bind(visualAd.trackingImpressionUrls)
            ).merge(videoTracking.toPropertySet());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("urn", adUrn)
                .add("videoSources", videoSources)
                .add("visualAd", visualAd)
                .toString();
    }
}
