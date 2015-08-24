package com.soundcloud.android.events;

import com.soundcloud.android.analytics.TrackingCode;
import org.jetbrains.annotations.NotNull;

public final class UpgradeTrackingEvent extends TrackingEvent {

    public static final String KIND_UPSELL_IMPRESSION = "upsell_impression";
    public static final String KIND_UPSELL_CLICK = "upsell_click";
    public static final String KIND_UPGRADE_SUCCESS = "upgrade_complete";

    public static final String KEY_TCODE = "tcode";

    private UpgradeTrackingEvent(@NotNull String kind) {
        super(kind, System.currentTimeMillis());
    }

    private UpgradeTrackingEvent(@NotNull String kind, int trackingCodeUrn) {
        this(kind);
        put(KEY_TCODE, toTrackingCodeUrn(trackingCodeUrn));
    }

    public static UpgradeTrackingEvent forWhyAdsImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_WHY_ADS);
    }

    public static UpgradeTrackingEvent forWhyAdsClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_WHY_ADS);
    }

    public static UpgradeTrackingEvent forNavImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_NAV);
    }

    public static UpgradeTrackingEvent forNavClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_NAV);
    }

    public static UpgradeTrackingEvent forSettingsImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_SETTINGS);
    }

    public static UpgradeTrackingEvent forSettingsClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_SETTINGS);
    }

    public static UpgradeTrackingEvent forLikesImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_LIKES);
    }

    public static UpgradeTrackingEvent forLikesClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_LIKES);
    }

    public static UpgradeTrackingEvent forPlaylistItemImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_PLAYLIST_ITEM);
    }

    public static UpgradeTrackingEvent forPlaylistItemClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_PLAYLIST_ITEM);
    }

    public static UpgradeTrackingEvent forPlaylistPageImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_PLAYLIST_PAGE);
    }

    public static UpgradeTrackingEvent forPlaylistPageClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_PLAYLIST_PAGE);
    }

    public static UpgradeTrackingEvent forUpgradeButtonImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPGRADE_BUTTON);
    }

    public static UpgradeTrackingEvent forUpgradeButtonClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPGRADE_BUTTON);
    }

    public static UpgradeTrackingEvent forUpgradeSuccess() {
        return new UpgradeTrackingEvent(KIND_UPGRADE_SUCCESS);
    }

    private static String toTrackingCodeUrn(int trackingCode) {
        return "soundcloud:tcode:" + trackingCode;
    }

    @Override
    public String toString() {
        return kind + " : " + get(KEY_TCODE);
    }

}
