package com.soundcloud.android.events;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

public final class UpgradeFunnelEvent extends TrackingEvent {

    public static final String KIND_UPSELL_IMPRESSION = "upsell_impression";
    public static final String KIND_RESUBSCRIBE_IMPRESSION = "resub_impression";
    public static final String KIND_UPSELL_CLICK = "upsell_click";
    public static final String KIND_RESUBSCRIBE_CLICK = "resub_click";
    public static final String KIND_UPGRADE_SUCCESS = "upgrade_complete";

    public static final String ID_WHY_ADS = "why_ads";
    public static final String ID_PLAYER = "player";
    public static final String ID_SETTINGS = "settings";
    public static final String ID_SETTINGS_UPGRADE = "upgrade";
    public static final String ID_LIKES = "likes";
    public static final String ID_SEARCH_RESULTS = "search_results";
    public static final String ID_SEARCH_RESULTS_GO = "search_go";
    public static final String ID_PLAYLIST_ITEM = "playlist_item";
    public static final String ID_PLAYLIST_PAGE = "playlist_page";
    public static final String ID_PLAYLIST_OVERFLOW = "playlist_overflow";
    public static final String ID_STREAM = "stream";
    public static final String ID_PLAYLIST_TRACKS = "playlist_tracks";
    public static final String ID_UPGRADE_BUTTON = "upgrade_button";
    public static final String ID_UPGRADE_PROMO = "upgrade_promo";
    public static final String ID_RESUBSCRIBE_BUTTON = "resubscribe_button";

    public static final String KEY_ID = "upgrade_funnel_id";
    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_PAGE_URN = "page_urn";

    private UpgradeFunnelEvent(@NotNull String kind) {
        super(kind);
    }

    private UpgradeFunnelEvent(@NotNull String kind, String id) {
        this(kind);
        put(KEY_ID, id);
    }

    public static UpgradeFunnelEvent forWhyAdsImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_WHY_ADS);
    }

    public static UpgradeFunnelEvent forWhyAdsClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_WHY_ADS);
    }

    public static UpgradeFunnelEvent forPlayerImpression(Urn trackUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_PLAYER)
                .put(KEY_PAGE_NAME, Screen.PLAYER_MAIN.get())
                .put(KEY_PAGE_URN, trackUrn.toString());
    }

    public static UpgradeFunnelEvent forPlayerClick(Urn trackUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_PLAYER)
                .put(KEY_PAGE_NAME, Screen.PLAYER_MAIN.get())
                .put(KEY_PAGE_URN, trackUrn.toString());
    }

    public static UpgradeFunnelEvent forSettingsClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_SETTINGS)
                .put(KEY_PAGE_NAME, Screen.SETTINGS_MAIN.get());
    }

    public static UpgradeFunnelEvent forSettingsImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_SETTINGS)
                .put(KEY_PAGE_NAME, Screen.SETTINGS_MAIN.get());
    }

    public static UpgradeFunnelEvent forUpgradeFromSettingsClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_SETTINGS_UPGRADE)
                .put(KEY_PAGE_NAME, Screen.SETTINGS_OFFLINE.get());
    }

    public static UpgradeFunnelEvent forUpgradeFromSettingsImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_SETTINGS_UPGRADE)
                .put(KEY_PAGE_NAME, Screen.SETTINGS_OFFLINE.get());
    }

    public static UpgradeFunnelEvent forLikesImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_LIKES)
                .put(KEY_PAGE_NAME, Screen.LIKES.get());
    }

    public static UpgradeFunnelEvent forLikesClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_LIKES)
                .put(KEY_PAGE_NAME, Screen.LIKES.get());
    }

    public static UpgradeFunnelEvent forSearchResultsImpression(Screen screen) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_SEARCH_RESULTS)
                .put(KEY_PAGE_NAME, screen.get());
    }

    public static UpgradeFunnelEvent forSearchResultsClick(Screen screen) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_SEARCH_RESULTS)
                .put(KEY_PAGE_NAME, screen.get());
    }

    public static UpgradeFunnelEvent forSearchPremiumResultsImpression(Screen screen) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_SEARCH_RESULTS_GO)
                .put(KEY_PAGE_NAME, screen.get());
    }

    public static UpgradeFunnelEvent forSearchPremiumResultsClick(Screen screen) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_SEARCH_RESULTS_GO)
                .put(KEY_PAGE_NAME, screen.get());
    }

    public static UpgradeFunnelEvent forPlaylistItemImpression(String screen, Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_PLAYLIST_ITEM)
                .put(KEY_PAGE_NAME, screen)
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeFunnelEvent forPlaylistItemClick(String screen, Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_PLAYLIST_ITEM)
                .put(KEY_PAGE_NAME, screen)
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeFunnelEvent forPlaylistPageImpression(Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_PLAYLIST_PAGE)
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get())
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeFunnelEvent forPlaylistPageClick(Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_PLAYLIST_PAGE)
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get())
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeFunnelEvent forPlaylistOverflowImpression(Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_PLAYLIST_OVERFLOW)
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get())
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeFunnelEvent forPlaylistOverflowClick(Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_PLAYLIST_OVERFLOW)
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get())
                .put(KEY_PAGE_URN, playlistUrn.toString());
    }

    public static UpgradeFunnelEvent forStreamImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_STREAM)
                .put(KEY_PAGE_NAME, Screen.STREAM.get());
    }

    public static UpgradeFunnelEvent forStreamClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_STREAM)
                .put(KEY_PAGE_NAME, Screen.STREAM.get());
    }

    public static UpgradeFunnelEvent forPlaylistTracksImpression(Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_PLAYLIST_TRACKS)
                .put(KEY_PAGE_URN, playlistUrn.toString())
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get());
    }

    public static UpgradeFunnelEvent forPlaylistTracksClick(Urn playlistUrn) {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_PLAYLIST_TRACKS)
                .put(KEY_PAGE_URN, playlistUrn.toString())
                .put(KEY_PAGE_NAME, Screen.PLAYLIST_DETAILS.get());
    }

    public static UpgradeFunnelEvent forUpgradeButtonImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_UPGRADE_BUTTON)
                .put(KEY_PAGE_NAME, Screen.CONVERSION.get());
    }

    public static UpgradeFunnelEvent forUpgradeButtonClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_UPGRADE_BUTTON)
                .put(KEY_PAGE_NAME, Screen.CONVERSION.get());
    }

    public static UpgradeFunnelEvent forUpgradePromoImpression() {
        return new UpgradeFunnelEvent(KIND_UPSELL_IMPRESSION, ID_UPGRADE_PROMO)
                .put(KEY_PAGE_NAME, Screen.CONVERSION.get());
    }

    public static UpgradeFunnelEvent forUpgradePromoClick() {
        return new UpgradeFunnelEvent(KIND_UPSELL_CLICK, ID_UPGRADE_PROMO)
                .put(KEY_PAGE_NAME, Screen.CONVERSION.get());
    }

    public static UpgradeFunnelEvent forUpgradeSuccess() {
        return new UpgradeFunnelEvent(KIND_UPGRADE_SUCCESS);
    }

    public static UpgradeFunnelEvent forResubscribeImpression() {
        return new UpgradeFunnelEvent(KIND_RESUBSCRIBE_IMPRESSION, ID_RESUBSCRIBE_BUTTON)
                .put(KEY_PAGE_NAME, Screen.OFFLINE_OFFBOARDING.get());
    }

    public static UpgradeFunnelEvent forResubscribeClick() {
        return new UpgradeFunnelEvent(KIND_RESUBSCRIBE_CLICK, ID_RESUBSCRIBE_BUTTON)
                .put(KEY_PAGE_NAME, Screen.OFFLINE_OFFBOARDING.get());
    }

    public boolean isImpression() {
        return kind.equals(KIND_UPSELL_IMPRESSION)
                || kind.equals(KIND_RESUBSCRIBE_IMPRESSION);
    }

}
