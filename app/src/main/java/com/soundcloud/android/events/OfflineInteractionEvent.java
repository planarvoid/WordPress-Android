package com.soundcloud.android.events;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;

public class OfflineInteractionEvent extends LegacyTrackingEvent {

    public static final String KIND_ONBOARDING_START = "offline_sync_onboarding::start";
    public static final String KIND_ONBOARDING_DISMISS = "offline_sync_onboarding::dismiss";
    public static final String KIND_ONBOARDING_AUTOMATIC_SYNC = "offline_sync_onboarding::automatic_collection_sync";
    public static final String KIND_ONBOARDING_MANUAL_SYNC = "offline_sync_onboarding::manual_sync";

    public static final String KIND_WIFI_SYNC_ENABLE = "only_sync_over_wifi::enable";
    public static final String KIND_WIFI_SYNC_DISABLE = "only_sync_over_wifi::disable";
    public static final String KIND_COLLECTION_SYNC_ENABLE = "automatic_collection_sync::enable";
    public static final String KIND_COLLECTION_SYNC_DISABLE = "automatic_collection_sync::disable";
    public static final String KIND_LIMIT_BELOW_USAGE = "offline_storage::limit_below_usage";

    public static final String KIND_OFFLINE_PLAYLIST_ADD = "playlist_to_offline::add";
    public static final String KIND_OFFLINE_PLAYLIST_REMOVE = "playlist_to_offline::remove";
    public static final String KIND_OFFLINE_LIKES_ADD = "automatic_likes_sync::enable";
    public static final String KIND_OFFLINE_LIKES_REMOVE = "automatic_likes_sync::disable";

    private static final String KEY_PAGE_NAME = "page_name";
    private static final String KEY_CLICK_OBJECT = "click_object";

    private OfflineInteractionEvent(@NotNull String kind, String pageName) {
        super(kind);
        put(KEY_PAGE_NAME, pageName);
    }

    private OfflineInteractionEvent(@NotNull String kind) {
        super(kind);
    }

    public static OfflineInteractionEvent fromOnboardingStart() {
        return new OfflineInteractionEvent(KIND_ONBOARDING_START);
    }

    public static OfflineInteractionEvent fromOnboardingDismiss() {
        return new OfflineInteractionEvent(KIND_ONBOARDING_DISMISS);
    }

    public static OfflineInteractionEvent fromOnboardingWithAutomaticSync() {
        return new OfflineInteractionEvent(KIND_ONBOARDING_AUTOMATIC_SYNC);
    }

    public static OfflineInteractionEvent fromOnboardingWithManualSync() {
        return new OfflineInteractionEvent(KIND_ONBOARDING_MANUAL_SYNC);
    }

    public static OfflineInteractionEvent forOnlyWifiOverWifiToggle(boolean wifiOnlySyncEnabled) {
        return new OfflineInteractionEvent(wifiOnlySyncEnabled ?
                                           KIND_WIFI_SYNC_ENABLE : KIND_WIFI_SYNC_DISABLE);
    }

    public static OfflineInteractionEvent fromRemoveOfflineLikes(String pageName) {
        return new OfflineInteractionEvent(KIND_OFFLINE_LIKES_REMOVE, pageName);
    }

    public static OfflineInteractionEvent fromEnableOfflineLikes(String pageName) {
        return new OfflineInteractionEvent(KIND_OFFLINE_LIKES_ADD, pageName);
    }

    public static OfflineInteractionEvent fromEnableCollectionSync(String pageName) {
        return new OfflineInteractionEvent(KIND_COLLECTION_SYNC_ENABLE, pageName);
    }

    public static OfflineInteractionEvent fromDisableCollectionSync(String pageName) {
        return new OfflineInteractionEvent(KIND_COLLECTION_SYNC_DISABLE, pageName);
    }

    public static OfflineInteractionEvent fromDisableCollectionSync(String pageName, Optional<Urn> entityUrn) {
        final OfflineInteractionEvent event = new OfflineInteractionEvent(KIND_COLLECTION_SYNC_DISABLE, pageName);
        if (entityUrn.isPresent()) {
            event.put(KEY_CLICK_OBJECT, entityUrn.get().toString());
        }
        return event;
    }

    public static OfflineInteractionEvent fromRemoveOfflinePlaylist(String pageName, @NonNull Urn playlistUrn,
                                                                    PromotedSourceInfo promotedSourceInfo) {
        return new OfflineInteractionEvent(KIND_OFFLINE_PLAYLIST_REMOVE, pageName)
                .<OfflineInteractionEvent>put(KEY_CLICK_OBJECT, playlistUrn.toString())
                .putPromotedSourceInfo(promotedSourceInfo);
    }

    public static OfflineInteractionEvent fromAddOfflinePlaylist(String pageName, @NonNull Urn playlistUrn,
                                                                 PromotedSourceInfo promotedSourceInfo) {
        return new OfflineInteractionEvent(KIND_OFFLINE_PLAYLIST_ADD, pageName)
                .<OfflineInteractionEvent>put(KEY_CLICK_OBJECT, playlistUrn.toString())
                .putPromotedSourceInfo(promotedSourceInfo);
    }

    public static OfflineInteractionEvent forStorageBelowLimitImpression() {
        return new OfflineInteractionEvent(KIND_LIMIT_BELOW_USAGE, Screen.SETTINGS_OFFLINE.get());
    }

    private OfflineInteractionEvent putPromotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
        if (promotedSourceInfo != null) {
            put(PlayableTrackingKeys.KEY_AD_URN, promotedSourceInfo.getAdUrn());
            put(PlayableTrackingKeys.KEY_MONETIZATION_TYPE, UIEvent.MonetizationType.PROMOTED.toString());
            if (promotedSourceInfo.getPromoterUrn().isPresent()) {
                put(PlayableTrackingKeys.KEY_PROMOTER_URN, promotedSourceInfo.getPromoterUrn().get().toString());
            }
        }
        return this;
    }

    public String getPageName() {
        return get(KEY_PAGE_NAME);
    }

    public String getClickObject() {
        return get(KEY_CLICK_OBJECT);
    }

}
