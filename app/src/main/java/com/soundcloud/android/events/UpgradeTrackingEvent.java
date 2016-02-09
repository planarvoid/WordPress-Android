package com.soundcloud.android.events;

import com.soundcloud.android.analytics.TrackingCode;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

public final class UpgradeTrackingEvent extends TrackingEvent {

    public static final String KIND_UPSELL_IMPRESSION = "upsell_impression";
    public static final String KIND_UPSELL_CLICK = "upsell_click";
    public static final String KIND_UPGRADE_SUCCESS = "upgrade_complete";

    public static final String KEY_TCODE = "tcode";
    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_PAGE_URN = "page_urn";

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

    public static UpgradeTrackingEvent forPlayerImpression(Urn trackUrn) {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_PLAYER)
                .put(KEY_PAGE_NAME, Screen.PLAYER_MAIN.get())
                .put(KEY_PAGE_URN, trackUrn.toString());
    }

    public static UpgradeTrackingEvent forPlayerClick(Urn trackUrn) {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_PLAYER)
                .put(KEY_PAGE_NAME, Screen.PLAYER_MAIN.get())
                .put(KEY_PAGE_URN, trackUrn.toString());
    }

    public static UpgradeTrackingEvent forSettingsClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_SETTINGS)
                .put(KEY_PAGE_NAME, Screen.SETTINGS_OFFLINE.get());
    }

    public static UpgradeTrackingEvent forSettingsImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_SETTINGS)
                .put(KEY_PAGE_NAME, Screen.SETTINGS_OFFLINE.get());
    }

    public static UpgradeTrackingEvent forLikesImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_LIKES)
                .put(KEY_PAGE_NAME, Screen.LIKES.get());
    }

    public static UpgradeTrackingEvent forLikesClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_LIKES)
                .put(KEY_PAGE_NAME, Screen.LIKES.get());
    }

    public static UpgradeTrackingEvent forPlaylistItemImpression(String screen, Urn playlistUrn) {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_PLAYLIST_ITEM)
                .put(KEY_PAGE_NAME, screen)
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeTrackingEvent forPlaylistItemClick(String screen, Urn playlistUrn) {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_PLAYLIST_ITEM)
                .put(KEY_PAGE_NAME, screen)
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeTrackingEvent forPlaylistPageImpression(Urn playlistUrn) {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_PLAYLIST_PAGE)
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get())
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeTrackingEvent forPlaylistPageClick(Urn playlistUrn) {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_PLAYLIST_PAGE)
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get())
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeTrackingEvent forStreamImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPSELL_STREAM)
                .put(KEY_PAGE_NAME, Screen.STREAM.get());
    }

    public static UpgradeTrackingEvent forStreamClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPSELL_STREAM)
                .put(KEY_PAGE_NAME, Screen.STREAM.get());
    }

    public static UpgradeTrackingEvent forUpgradeButtonImpression() {
        return new UpgradeTrackingEvent(KIND_UPSELL_IMPRESSION, TrackingCode.UPGRADE_BUTTON)
                .put(KEY_PAGE_NAME, Screen.CONVERSION.get());
    }

    public static UpgradeTrackingEvent forUpgradeButtonClick() {
        return new UpgradeTrackingEvent(KIND_UPSELL_CLICK, TrackingCode.UPGRADE_BUTTON)
                .put(KEY_PAGE_NAME, Screen.CONVERSION.get());
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
