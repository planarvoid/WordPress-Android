package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdOverlayProperty;
import com.soundcloud.android.ads.InterstitialProperty;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.VisibleForTesting;

import java.util.List;

public final class AdOverlayTrackingEvent extends TrackingEvent {
    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String TYPE_INTERSTITIAL = "interstitial";
    public static final String TYPE_AUDIO_AD = "audio_ad";
    public static final String TYPE_LEAVE_BEHIND = "leave_behind";

    private final List<String> trackingUrls;

    private AdOverlayTrackingEvent(long timeStamp, String kindImpression, PropertySet adMetaData, Urn monetizableTrack, Urn user, List<String> trackingUrls, @Nullable TrackSourceInfo trackSourceInfo) {
        super(kindImpression, timeStamp);
        this.trackingUrls = trackingUrls;

        put(AdTrackingKeys.KEY_USER_URN, user.toString());
        put(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN, monetizableTrack.toString());
        put(AdTrackingKeys.KEY_AD_ARTWORK_URL, adMetaData.get(AdOverlayProperty.IMAGE_URL));
        put(AdTrackingKeys.KEY_CLICK_THROUGH_URL, adMetaData.get(AdOverlayProperty.CLICK_THROUGH_URL).toString());
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, getNonNullOriginScreenValue(trackSourceInfo));

        if (adMetaData.contains(LeaveBehindProperty.LEAVE_BEHIND_URN)) {
            put(AdTrackingKeys.KEY_AD_URN, adMetaData.get(LeaveBehindProperty.LEAVE_BEHIND_URN));
            put(AdTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_AUDIO_AD);
            put(AdTrackingKeys.KEY_AD_TYPE, TYPE_LEAVE_BEHIND);
        } else if (adMetaData.contains(InterstitialProperty.INTERSTITIAL_URN)) {
            put(AdTrackingKeys.KEY_AD_URN, adMetaData.get(InterstitialProperty.INTERSTITIAL_URN));
            put(AdTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_INTERSTITIAL);
            put(AdTrackingKeys.KEY_AD_TYPE, TYPE_INTERSTITIAL);
        }

        if (adMetaData.contains(LeaveBehindProperty.AUDIO_AD_TRACK_URN)){
            put(AdTrackingKeys.KEY_AD_TRACK_URN, adMetaData.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN).toString());
            put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, adMetaData.get(LeaveBehindProperty.AUDIO_AD_TRACK_URN).toString());
        } else {
            put(AdTrackingKeys.KEY_AD_TRACK_URN, monetizableTrack.toString());
        }
    }

    private String getNonNullOriginScreenValue(@Nullable TrackSourceInfo trackSourceInfo) {
        if (trackSourceInfo != null) {
            return trackSourceInfo.getOriginScreen();
        }
        return ScTextUtils.EMPTY_STRING;
    }

    public static AdOverlayTrackingEvent forClick(PropertySet adMetaData, Urn track, Urn user, @Nullable TrackSourceInfo sourceInfo) {
        return forClick(System.currentTimeMillis(), adMetaData, track, user, sourceInfo);
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    public static AdOverlayTrackingEvent forImpression(PropertySet adMetaData, Urn track, Urn user, @Nullable TrackSourceInfo sourceInfo) {
        return forImpression(System.currentTimeMillis(), adMetaData, track, user, sourceInfo);
    }

    @VisibleForTesting
    public static AdOverlayTrackingEvent forImpression(long timeStamp, PropertySet adMetaData, Urn track, Urn user, TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adMetaData.get(LeaveBehindProperty.TRACKING_IMPRESSION_URLS);
        return new AdOverlayTrackingEvent(
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
    public static AdOverlayTrackingEvent forClick(long timestamp, PropertySet adMetaData, Urn track, Urn user, TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adMetaData.get(LeaveBehindProperty.TRACKING_CLICK_URLS);
        return new AdOverlayTrackingEvent(
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
