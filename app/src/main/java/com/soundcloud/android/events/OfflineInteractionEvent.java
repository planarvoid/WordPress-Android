package com.soundcloud.android.events;

import static com.soundcloud.android.events.OfflineInteractionEvent.Kind.KIND_COLLECTION_SYNC_DISABLE;
import static com.soundcloud.android.events.OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_ADD;
import static com.soundcloud.android.events.OfflineInteractionEvent.Kind.KIND_OFFLINE_PLAYLIST_REMOVE;
import static com.soundcloud.android.events.OfflineInteractionEvent.Kind.KIND_WIFI_SYNC_DISABLE;
import static com.soundcloud.android.events.OfflineInteractionEvent.Kind.KIND_WIFI_SYNC_ENABLE;
import static com.soundcloud.android.events.OfflineInteractionEvent.EventName.IMPRESSION;
import static java.lang.Boolean.TRUE;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.NonNull;

@AutoValue
public abstract class OfflineInteractionEvent extends NewTrackingEvent {
    private static final String CATEGORY = "consumer_subs";

    public enum EventName {
        IMPRESSION("impression"), CLICK("click");
        private final String key;

        EventName(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public enum Kind {
        KIND_ONBOARDING_START("offline_sync_onboarding::start"),
        KIND_ONBOARDING_DISMISS("offline_sync_onboarding::dismiss"),
        KIND_ONBOARDING_AUTOMATIC_SYNC("offline_sync_onboarding::automatic_collection_sync"),
        KIND_ONBOARDING_MANUAL_SYNC("offline_sync_onboarding::manual_sync"),
        KIND_WIFI_SYNC_ENABLE("only_sync_over_wifi::enable"),
        KIND_WIFI_SYNC_DISABLE("only_sync_over_wifi::disable"),
        KIND_COLLECTION_SYNC_ENABLE("automatic_collection_sync::enable"),
        KIND_COLLECTION_SYNC_DISABLE("automatic_collection_sync::disable"),
        KIND_LIMIT_BELOW_USAGE("offline_storage::limit_below_usage"),
        KIND_OFFLINE_PLAYLIST_ADD("playlist_to_offline::add"),
        KIND_OFFLINE_PLAYLIST_REMOVE("playlist_to_offline::remove"),
        KIND_OFFLINE_LIKES_ADD("automatic_likes_sync::enable"),
        KIND_OFFLINE_LIKES_REMOVE("automatic_likes_sync::disable");
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public enum Context {
        LIKES_CONTEXT("likes"),
        PLAYLIST_CONTEXT("playlist"),
        ALL_CONTEXT("all");
        private final String key;

        Context(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public abstract EventName eventName();

    public abstract Optional<String> impressionCategory();

    public abstract Optional<String> clickCategory();

    public abstract Optional<Kind> impressionName();

    public abstract Optional<Kind> clickName();

    public abstract Optional<String> pageName();

    public abstract Optional<Urn> clickObject();

    public abstract Optional<String> adUrn();

    public abstract Optional<UIEvent.MonetizationType> monetizationType();

    public abstract Optional<Urn> promoterUrn();

    //region Appboy fields
    public abstract Optional<Context> context();

    public abstract Optional<Boolean> isEnabled();
    //endregion

    public static OfflineInteractionEvent fromOnboardingStart() {
        return clickEventBuilder(Kind.KIND_ONBOARDING_START).build();
    }

    public static OfflineInteractionEvent fromOnboardingDismiss() {
        return clickEventBuilder(Kind.KIND_ONBOARDING_DISMISS).build();
    }

    public static OfflineInteractionEvent fromOnboardingWithAutomaticSync() {
        return clickEventBuilder(Kind.KIND_ONBOARDING_AUTOMATIC_SYNC).build();
    }

    public static OfflineInteractionEvent fromOnboardingWithManualSync() {
        return clickEventBuilder(Kind.KIND_ONBOARDING_MANUAL_SYNC).build();
    }

    public static OfflineInteractionEvent forOnlyWifiOverWifiToggle(boolean wifiOnlySyncEnabled) {
        return clickEventBuilder(wifiOnlySyncEnabled ? KIND_WIFI_SYNC_ENABLE : KIND_WIFI_SYNC_DISABLE).build();
    }

    public static OfflineInteractionEvent fromRemoveOfflineLikes(String pageName) {
        return clickEventBuilder(Kind.KIND_OFFLINE_LIKES_REMOVE).context(Optional.of(Context.LIKES_CONTEXT)).isEnabled(Optional.of(false)).pageName(Optional.of(pageName)).build();
    }

    public static OfflineInteractionEvent fromEnableOfflineLikes(String pageName) {
        return clickEventBuilder(Kind.KIND_OFFLINE_LIKES_ADD).context(Optional.of(Context.LIKES_CONTEXT)).isEnabled(Optional.of(true)).pageName(Optional.of(pageName)).build();
    }

    public static OfflineInteractionEvent fromEnableCollectionSync(String pageName) {
        return clickEventBuilder(Kind.KIND_COLLECTION_SYNC_ENABLE).context(Optional.of(Context.ALL_CONTEXT)).isEnabled(Optional.of(true)).pageName(Optional.of(pageName)).build();
    }

    public static OfflineInteractionEvent fromDisableCollectionSync(String pageName) {
        return clickEventBuilder(Kind.KIND_COLLECTION_SYNC_DISABLE).context(Optional.of(Context.ALL_CONTEXT)).isEnabled(Optional.of(false)).pageName(Optional.of(pageName)).build();
    }

    public static OfflineInteractionEvent fromDisableCollectionSync(String pageName, Optional<Urn> entityUrn) {
        return clickEventBuilder(KIND_COLLECTION_SYNC_DISABLE).context(Optional.of(Context.ALL_CONTEXT)).isEnabled(Optional.of(false)).pageName(Optional.of(pageName)).clickObject(entityUrn).build();
    }

    public static OfflineInteractionEvent fromRemoveOfflinePlaylist(String pageName, @NonNull Urn playlistUrn,
                                                                    PromotedSourceInfo promotedSourceInfo) {
        return clickEventBuilder(KIND_OFFLINE_PLAYLIST_REMOVE).context(Optional.of(Context.PLAYLIST_CONTEXT))
                                                              .isEnabled(Optional.of(false))
                                                              .pageName(Optional.of(pageName))
                                                              .clickObject(Optional.of(playlistUrn))
                                                              .promotedSourceInfo(promotedSourceInfo)
                                                              .build();
    }

    public static OfflineInteractionEvent fromAddOfflinePlaylist(String pageName, @NonNull Urn playlistUrn,
                                                                 PromotedSourceInfo promotedSourceInfo) {
        return clickEventBuilder(KIND_OFFLINE_PLAYLIST_ADD).context(Optional.of(Context.PLAYLIST_CONTEXT))
                                                           .isEnabled(Optional.of(TRUE))
                                                           .pageName(Optional.of(pageName))
                                                           .clickObject(Optional.of(playlistUrn))
                                                           .promotedSourceInfo(promotedSourceInfo)
                                                           .build();
    }

    public static OfflineInteractionEvent forStorageBelowLimitImpression() {
        return builder(IMPRESSION).impressionCategory(Optional.of(CATEGORY)).impressionName(Optional.of(Kind.KIND_LIMIT_BELOW_USAGE)).pageName(Optional.of(Screen.SETTINGS_OFFLINE.get())).build();
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_OfflineInteractionEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    private static OfflineInteractionEvent.Builder clickEventBuilder(Kind kind) {
        return builder(EventName.CLICK).clickCategory(Optional.of(CATEGORY)).clickName(Optional.of(kind));
    }

    private static OfflineInteractionEvent.Builder builder(EventName eventName) {
        return new AutoValue_OfflineInteractionEvent.Builder().id(defaultId())
                                                              .timestamp(defaultTimestamp())
                                                              .referringEvent(Optional.absent())
                                                              .eventName(eventName)
                                                              .impressionCategory(Optional.absent())
                                                              .clickCategory(Optional.absent())
                                                              .impressionName(Optional.absent())
                                                              .clickName(Optional.absent())
                                                              .pageName(Optional.absent())
                                                              .clickObject(Optional.absent())
                                                              .adUrn(Optional.absent())
                                                              .monetizationType(Optional.absent())
                                                              .monetizationType(Optional.absent())
                                                              .promoterUrn(Optional.absent())
                                                              .context(Optional.absent())
                                                              .isEnabled(Optional.absent());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder eventName(EventName eventName);

        public abstract Builder pageName(Optional<String> pageName);

        public abstract Builder impressionCategory(Optional<String> impressionCategory);

        public abstract Builder clickCategory(Optional<String> clickCategory);

        public abstract Builder impressionName(Optional<Kind> impressionName);

        public abstract Builder clickName(Optional<Kind> clickName);

        public abstract Builder clickObject(Optional<Urn> clickObject);

        public abstract Builder adUrn(Optional<String> adUrn);

        public abstract Builder monetizationType(Optional<UIEvent.MonetizationType> monetizationType);

        public abstract Builder promoterUrn(Optional<Urn> promoterUrn);

        public abstract Builder context(Optional<Context> context);

        public abstract Builder isEnabled(Optional<Boolean> isEnabled);

        public Builder promotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
            if (promotedSourceInfo != null) {
                adUrn(Optional.of(promotedSourceInfo.getAdUrn()));
                monetizationType(Optional.of(UIEvent.MonetizationType.PROMOTED));
                promoterUrn(promotedSourceInfo.getPromoterUrn());
            }
            return this;
        }

        public abstract OfflineInteractionEvent build();
    }
}
