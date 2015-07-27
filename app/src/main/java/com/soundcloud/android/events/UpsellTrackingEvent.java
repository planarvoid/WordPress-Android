package com.soundcloud.android.events;

import com.soundcloud.android.analytics.TrackingCode;
import org.jetbrains.annotations.NotNull;

public final class UpsellTrackingEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String KEY_TCODE = "tcode";

    private UpsellTrackingEvent(@NotNull String kind, int trackingCodeUrn) {
        super(kind, System.currentTimeMillis());
        put(KEY_TCODE, toUrn(trackingCodeUrn));
    }

    public static UpsellTrackingEvent forWhyAdsImpression() {
        return new UpsellTrackingEvent(KIND_IMPRESSION, TrackingCode.UPSELL_WHY_ADS);
    }

    public static UpsellTrackingEvent forWhyAdsClick() {
        return new UpsellTrackingEvent(KIND_CLICK, TrackingCode.UPSELL_WHY_ADS);
    }

    public static UpsellTrackingEvent forNavImpression() {
        return new UpsellTrackingEvent(KIND_IMPRESSION, TrackingCode.UPSELL_NAV);
    }

    public static UpsellTrackingEvent forNavClick() {
        return new UpsellTrackingEvent(KIND_CLICK, TrackingCode.UPSELL_NAV);
    }

    public static UpsellTrackingEvent forSettingsImpression() {
        return new UpsellTrackingEvent(KIND_IMPRESSION, TrackingCode.UPSELL_SETTINGS);
    }

    public static UpsellTrackingEvent forSettingsClick() {
        return new UpsellTrackingEvent(KIND_CLICK, TrackingCode.UPSELL_SETTINGS);
    }

    public static UpsellTrackingEvent forLikesImpression() {
        return new UpsellTrackingEvent(KIND_IMPRESSION, TrackingCode.UPSELL_LIKES);
    }

    public static UpsellTrackingEvent forLikesClick() {
        return new UpsellTrackingEvent(KIND_CLICK, TrackingCode.UPSELL_LIKES);
    }

    public static UpsellTrackingEvent forPlaylistItemImpression() {
        return new UpsellTrackingEvent(KIND_IMPRESSION, TrackingCode.UPSELL_PLAYLIST_ITEM);
    }

    public static UpsellTrackingEvent forPlaylistItemClick() {
        return new UpsellTrackingEvent(KIND_CLICK, TrackingCode.UPSELL_PLAYLIST_ITEM);
    }

    public static UpsellTrackingEvent forPlaylistPageImpression() {
        return new UpsellTrackingEvent(KIND_IMPRESSION, TrackingCode.UPSELL_PLAYLIST_PAGE);
    }

    public static UpsellTrackingEvent forPlaylistPageClick() {
        return new UpsellTrackingEvent(KIND_CLICK, TrackingCode.UPSELL_PLAYLIST_PAGE);
    }

    private static String toUrn(int trackingCode) {
        return "soundcloud:tcode:" + trackingCode;
    }

}
