package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.events.UpgradeFunnelEvent;

class TrackingCode {

    private static final int UPSELL_WHY_ADS = 1006;
    private static final int UPSELL_SETTINGS = 1007;
    private static final int UPSELL_SETTINGS_UPGRADE = 1008;
    private static final int UPSELL_LIKES = 1009;
    private static final int UPSELL_PLAYLIST_ITEM = 1011;
    private static final int UPSELL_PLAYLIST_PAGE = 1012;
    private static final int UPSELL_STREAM = 1027;
    private static final int UPSELL_COLLECTION = 1052;
    private static final int UPSELL_PLAYER = 1017;
    private static final int UPSELL_SEARCH_RESULTS = 1025;
    private static final int UPSELL_SEARCH_PREMIUM_RESULTS = 1026;
    private static final int UPSELL_PLAYLIST_TRACKS = 1042;
    private static final int UPSELL_PLAYLIST_OVERFLOW = 1048;
    private static final int UPGRADE_BUTTON = 3002;
    private static final int RESUBSCRIBE_BUTTON = 4002;
    private static final int UPGRADE_PROMO = 4007;

    static String fromEventId(String eventId) {
        switch (eventId) {
            case UpgradeFunnelEvent.ID_WHY_ADS:
                return toUrn(UPSELL_WHY_ADS);
            case UpgradeFunnelEvent.ID_PLAYER:
                return toUrn(UPSELL_PLAYER);
            case UpgradeFunnelEvent.ID_SETTINGS:
                return toUrn(UPSELL_SETTINGS);
            case UpgradeFunnelEvent.ID_SETTINGS_UPGRADE:
                return toUrn(UPSELL_SETTINGS_UPGRADE);
            case UpgradeFunnelEvent.ID_LIKES:
                return toUrn(UPSELL_LIKES);
            case UpgradeFunnelEvent.ID_SEARCH_RESULTS:
                return toUrn(UPSELL_SEARCH_RESULTS);
            case UpgradeFunnelEvent.ID_SEARCH_RESULTS_GO:
                return toUrn(UPSELL_SEARCH_PREMIUM_RESULTS);
            case UpgradeFunnelEvent.ID_PLAYLIST_ITEM:
                return toUrn(UPSELL_PLAYLIST_ITEM);
            case UpgradeFunnelEvent.ID_PLAYLIST_PAGE:
                return toUrn(UPSELL_PLAYLIST_PAGE);
            case UpgradeFunnelEvent.ID_STREAM:
                return toUrn(UPSELL_STREAM);
            case UpgradeFunnelEvent.ID_COLLECTION:
                return toUrn(UPSELL_COLLECTION);
            case UpgradeFunnelEvent.ID_PLAYLIST_TRACKS:
                return toUrn(UPSELL_PLAYLIST_TRACKS);
            case UpgradeFunnelEvent.ID_UPGRADE_BUTTON:
                return toUrn(UPGRADE_BUTTON);
            case UpgradeFunnelEvent.ID_RESUBSCRIBE_BUTTON:
                return toUrn(RESUBSCRIBE_BUTTON);
            case UpgradeFunnelEvent.ID_UPGRADE_PROMO:
                return toUrn(UPGRADE_PROMO);
            case UpgradeFunnelEvent.ID_PLAYLIST_OVERFLOW:
                return toUrn(UPSELL_PLAYLIST_OVERFLOW);
            default:
                throw new IllegalArgumentException("Tracking event not recognised: " + eventId);
        }
    }

    private static String toUrn(int code) {
        return "soundcloud:tcode:" + code;
    }

}
