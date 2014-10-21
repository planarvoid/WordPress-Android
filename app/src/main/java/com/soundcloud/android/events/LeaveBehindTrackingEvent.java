package com.soundcloud.android.events;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.propeller.PropertySet;

import java.util.List;

public class LeaveBehindTrackingEvent extends TrackingEvent {
    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    private final List<String> trackingUrls;

    LeaveBehindTrackingEvent(long timeStamp, String kindImpression, PropertySet properties, Urn track, Urn user, List<String> trackingUrls, TrackSourceInfo trackSourceInfo) {
        super(kindImpression, timeStamp);
        this.trackingUrls = trackingUrls;

        put(AdTrackingKeys.KEY_USER_URN, user.toString());
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, track.toString());

        put(AdTrackingKeys.KEY_AD_URN, properties.get(LeaveBehindProperty.AD_URN).toString());
        put(AdTrackingKeys.KEY_AD_TRACK_URN, properties.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN).toString());

        put(AdTrackingKeys.KEY_AD_ARTWORK_URL, properties.get(LeaveBehindProperty.IMAGE_URL).toString());
        put(AdTrackingKeys.KEY_CLICK_THROUGH_URN, properties.get(LeaveBehindProperty.CLICK_THROUGH_URL).toString());
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, trackSourceInfo.getOriginScreen());
    }



    public static LeaveBehindTrackingEvent forClick(PropertySet adMetaData, Urn track, Urn user, TrackSourceInfo sourceInfo) {
        return forClick(System.currentTimeMillis(), adMetaData, track, user, sourceInfo);
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    public static LeaveBehindTrackingEvent forImpression(PropertySet adMetaData, Urn track, Urn user, TrackSourceInfo sourceInfo) {
        return forImpression(System.currentTimeMillis(), adMetaData, track, user, sourceInfo);
    }

    @VisibleForTesting
    public static LeaveBehindTrackingEvent forImpression(long timeStamp, PropertySet adMetaData, Urn track, Urn user, TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adMetaData.get(LeaveBehindProperty.TRACKING_IMPRESSION_URLS);
        return new LeaveBehindTrackingEvent(
                timeStamp,
                KIND_IMPRESSION,
                adMetaData,
                track,
                user,
                trackingUrls,
                sourceInfo
        );
    }

    @VisibleForTesting
    public static LeaveBehindTrackingEvent forClick(long timestamp, PropertySet adMetaData, Urn track, Urn user, TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adMetaData.get(LeaveBehindProperty.TRACKING_CLICK_URLS);
        return new LeaveBehindTrackingEvent(
                timestamp,
                KIND_CLICK,
                adMetaData,
                track,
                user,
                trackingUrls,
                sourceInfo
        );
    }
}
