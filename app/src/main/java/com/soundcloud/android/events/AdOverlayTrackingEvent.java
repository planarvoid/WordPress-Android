package com.soundcloud.android.events;

import com.soundcloud.android.ads.InterstitialAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.VisibleForTesting;

import java.util.List;

public final class AdOverlayTrackingEvent extends LegacyTrackingEvent {
    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String TYPE_INTERSTITIAL = "interstitial";
    public static final String TYPE_AUDIO_AD = "audio_ad";
    public static final String TYPE_LEAVE_BEHIND = "leave_behind";

    private final List<String> trackingUrls;

    private AdOverlayTrackingEvent(long timeStamp,
                                   String kindImpression,
                                   OverlayAdData adData,
                                   Urn monetizableTrack,
                                   Urn user,
                                   List<String> trackingUrls,
                                   @Nullable TrackSourceInfo trackSourceInfo) {
        super(kindImpression, timeStamp);
        this.trackingUrls = trackingUrls;

        put(PlayableTrackingKeys.KEY_USER_URN, user.toString());
        put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, monetizableTrack.toString());
        put(PlayableTrackingKeys.KEY_AD_ARTWORK_URL, adData.getImageUrl());
        put(PlayableTrackingKeys.KEY_CLICK_THROUGH_URL, adData.getClickthroughUrl().toString());
        put(PlayableTrackingKeys.KEY_ORIGIN_SCREEN, getNonNullOriginScreenValue(trackSourceInfo));
        put(PlayableTrackingKeys.KEY_AD_URN, adData.getAdUrn().toString());

        if (adData instanceof LeaveBehindAd) {
            put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_AUDIO_AD);
            put(PlayableTrackingKeys.KEY_AD_TYPE, TYPE_LEAVE_BEHIND);
            put(PlayableTrackingKeys.KEY_AD_TRACK_URN, ((LeaveBehindAd) adData).getAudioAdUrn().toString());
            put(PlayableTrackingKeys.KEY_CLICK_OBJECT_URN, ((LeaveBehindAd) adData).getAudioAdUrn().toString());
        } else if (adData instanceof InterstitialAd) {
            put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_INTERSTITIAL);
            put(PlayableTrackingKeys.KEY_AD_TYPE, TYPE_INTERSTITIAL);
            put(PlayableTrackingKeys.KEY_AD_TRACK_URN, monetizableTrack.toString());
        }
    }

    private String getNonNullOriginScreenValue(@Nullable TrackSourceInfo trackSourceInfo) {
        if (trackSourceInfo != null) {
            return trackSourceInfo.getOriginScreen();
        }
        return Strings.EMPTY;
    }

    public static AdOverlayTrackingEvent forClick(OverlayAdData adData,
                                                  Urn track,
                                                  Urn user,
                                                  @Nullable TrackSourceInfo sourceInfo) {
        return forClick(System.currentTimeMillis(), adData, track, user, sourceInfo);
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    public static AdOverlayTrackingEvent forImpression(OverlayAdData adData,
                                                       Urn track,
                                                       Urn user,
                                                       @Nullable TrackSourceInfo sourceInfo) {
        return forImpression(System.currentTimeMillis(), adData, track, user, sourceInfo);
    }

    @VisibleForTesting
    public static AdOverlayTrackingEvent forImpression(long timeStamp,
                                                       OverlayAdData adData,
                                                       Urn track,
                                                       Urn user,
                                                       TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adData.getImpressionUrls();
        return new AdOverlayTrackingEvent(
                timeStamp,
                KIND_IMPRESSION,
                adData,
                track,
                user,
                trackingUrls,
                sourceInfo
        );
    }

    @VisibleForTesting
    public static AdOverlayTrackingEvent forClick(long timestamp,
                                                  OverlayAdData adData,
                                                  Urn track,
                                                  Urn user,
                                                  TrackSourceInfo sourceInfo) {
        final List<String> trackingUrls = adData.getClickUrls();
        return new AdOverlayTrackingEvent(
                timestamp,
                KIND_CLICK,
                adData,
                track,
                user,
                trackingUrls,
                sourceInfo
        );
    }
}
