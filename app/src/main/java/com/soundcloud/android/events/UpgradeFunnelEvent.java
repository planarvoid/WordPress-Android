package com.soundcloud.android.events;

import static com.soundcloud.android.events.UpgradeFunnelEvent.ClickCategory.CONSUMER_SUBS;
import static com.soundcloud.android.events.UpgradeFunnelEvent.EventName.CLICK;
import static com.soundcloud.android.events.UpgradeFunnelEvent.EventName.IMPRESSION;
import static com.soundcloud.android.events.UpgradeFunnelEvent.Kind.RESUBSCRIBE_CLICK;
import static com.soundcloud.android.events.UpgradeFunnelEvent.Kind.RESUBSCRIBE_IMPRESSION;
import static com.soundcloud.android.events.UpgradeFunnelEvent.Kind.UPGRADE_SUCCESS;
import static com.soundcloud.android.events.UpgradeFunnelEvent.Kind.UPSELL_CLICK;
import static com.soundcloud.android.events.UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

@AutoValue
public abstract class UpgradeFunnelEvent extends TrackingEvent {
    public enum EventName {
        IMPRESSION("impression"),
        CLICK("click");

        final String key;

        EventName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum Kind {
        UPSELL_IMPRESSION("upsell_impression"),
        RESUBSCRIBE_IMPRESSION("resub_impression"),
        UPGRADE_SUCCESS("upgrade_complete"),
        UPSELL_CLICK("upsell_click"),
        RESUBSCRIBE_CLICK("resub_click");

        final String key;

        Kind(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ClickCategory {
        CONSUMER_SUBS("consumer_subs");

        private final String key;

        ClickCategory(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ImpressionName {
        CONSUMER_SUB_AD("consumer_sub_ad"),
        CONSUMER_SUB_UPGRADE_SUCCESS("consumer_sub_upgrade_success"),
        CONSUMER_SUB_RESUBSCRIBE("consumer_sub_resubscribe");

        private final String key;

        ImpressionName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ClickName {
        CONSUMER_SUB_AD("clickthrough::consumer_sub_ad"),
        CONSUMER_SUB_RESUBSCRIBE("clickthrough::consumer_sub_resubscribe");

        private final String key;

        ClickName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum TCode {
        WHY_ADS(1006),
        PLAYER(1017),
        SETTINGS_UPGRADE(1008),
        LIKES(1009),
        SEARCH_RESULTS(1025),
        SEARCH_RESULTS_GO(1026),
        PLAYLIST_ITEM(1011),
        PLAYLIST_PAGE(1012),
        PLAYLIST_OVERFLOW(1048),
        STREAM(1027),
        COLLECTION(1052),
        PLAYLIST_TRACKS(1042),
        CONVERSION_BUY(3002),
        CONVERSION_PROMO(4007),
        RESUBSCRIBE_BUTTON(4002),
        CHOOSER_BUY_MID_TIER(3009),
        CHOOSER_BUY_HIGH_TIER(3011),
        DISCOVERY(1056);

        private final int code;

        TCode(int code) {
            this.code = code;
        }

        public String code() {
            return "soundcloud:tcode:" + code;
        }
    }

    public enum AdjustToken {
        WHY_ADS("1jourb"),
        CONVERSION("b4r6to"),
        PROMO("355p3s"),
        HIGH_TIER_TRACK_PLAYED("lfydid"),
        HIGH_TIER_SEARCH_RESULTS("n3mdeg"),
        STREAM_UPSELL("a7r5gy"),
        SETTINGS("396cnm"),
        PLAYLIST_TRACKS_UPSELL("8a8hir"),
        PLAN_DOWNGRADED("ik01gn"),
        DISCOVERY_UPSELL("3k9mgl");

        private final String adjustToken;

        AdjustToken(String adjustToken) {
            this.adjustToken = adjustToken;
        }

        public String adjustToken() {
            return adjustToken;
        }
    }

    public abstract Kind kind();

    public abstract EventName eventName();

    public abstract Optional<String> pageName();

    public abstract Optional<String> pageUrn();

    public abstract Optional<ClickName> clickName();

    public abstract Optional<ClickCategory> clickCategory();

    public abstract Optional<String> clickObject();

    public abstract Optional<ImpressionName> impressionName();

    public abstract Optional<String> impressionCategory();

    public abstract Optional<String> impressionObject();

    public abstract Optional<AdjustToken> adjustToken();

    @NotNull
    @Override
    public String getKind() {
        return kind().toString();
    }

    private static Builder from(Kind kind) {
        return new AutoValue_UpgradeFunnelEvent.Builder()
                .id(defaultId())
                .timestamp(defaultTimestamp())
                .referringEvent(Optional.absent())
                .kind(kind)
                .pageName(Optional.absent())
                .pageUrn(Optional.absent())
                .clickName(Optional.absent())
                .clickCategory(Optional.absent())
                .clickObject(Optional.absent())
                .impressionName(Optional.absent())
                .impressionCategory(Optional.absent())
                .impressionObject(Optional.absent())
                .adjustToken(Optional.absent());
    }

    private static Builder fromUpsellImpression(TCode tcode) {
        return from(UPSELL_IMPRESSION)
                .eventName(IMPRESSION)
                .impressionName(Optional.of(ImpressionName.CONSUMER_SUB_AD))
                .impressionObject(Optional.of(tcode.code()));
    }

    private static Builder fromUpsellClick(TCode tcode) {
        return from(UPSELL_CLICK)
                .eventName(CLICK)
                .clickName(Optional.of(ClickName.CONSUMER_SUB_AD))
                .clickCategory(Optional.of(CONSUMER_SUBS))
                .clickObject(Optional.of(tcode.code()));
    }

    public static UpgradeFunnelEvent forWhyAdsImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.WHY_ADS)
                                 .adjustToken(Optional.of(AdjustToken.WHY_ADS))
                                 .build();
    }

    public static UpgradeFunnelEvent forWhyAdsClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.WHY_ADS)
                                 .build();
    }

    public static UpgradeFunnelEvent forPlayerImpression(Urn trackUrn) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.PLAYER)
                                 .adjustToken(Optional.of(AdjustToken.HIGH_TIER_TRACK_PLAYED))
                                 .pageName(Optional.of(Screen.PLAYER_MAIN.get()))
                                 .pageUrn(Optional.of(trackUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlayerClick(Urn trackUrn) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.PLAYER)
                                 .pageName(Optional.of(Screen.PLAYER_MAIN.get()))
                                 .pageUrn(Optional.of(trackUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forUpgradeFromSettingsClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.SETTINGS_UPGRADE)
                                 .pageName(Optional.of(Screen.SETTINGS_MAIN.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forUpgradeFromSettingsImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.SETTINGS_UPGRADE)
                                 .adjustToken(Optional.of(AdjustToken.SETTINGS))
                                 .pageName(Optional.of(Screen.SETTINGS_MAIN.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forLikesImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.LIKES)
                                 .pageName(Optional.of(Screen.LIKES.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forLikesClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.LIKES)
                                 .pageName(Optional.of(Screen.LIKES.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forSearchResultsImpression(Screen screen) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.SEARCH_RESULTS)
                                 .pageName(Optional.of(screen.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forSearchResultsClick(Screen screen) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.SEARCH_RESULTS)
                                 .pageName(Optional.of(screen.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forSearchPremiumResultsImpression(Screen screen) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.SEARCH_RESULTS_GO)
                                 .adjustToken(Optional.of(AdjustToken.HIGH_TIER_SEARCH_RESULTS))
                                 .pageName(Optional.of(screen.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forSearchPremiumResultsClick(Screen screen) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.SEARCH_RESULTS_GO)
                                 .pageName(Optional.of(screen.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistItemImpression(String screen, Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.PLAYLIST_ITEM)
                                 .pageName(Optional.of(screen))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistItemClick(String screen, Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.PLAYLIST_ITEM)
                                 .pageName(Optional.of(screen))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistPageImpression(Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.PLAYLIST_PAGE)
                                 .pageName(Optional.of(Screen.PLAYLIST_DETAILS.get()))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistPageClick(Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.PLAYLIST_PAGE)
                                 .pageName(Optional.of(Screen.PLAYLIST_DETAILS.get()))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistOverflowImpression(Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.PLAYLIST_OVERFLOW)
                                 .pageName(Optional.of(Screen.PLAYLIST_DETAILS.get()))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistOverflowClick(Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.PLAYLIST_OVERFLOW)
                                 .pageName(Optional.of(Screen.PLAYLIST_DETAILS.get()))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forStreamImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.STREAM)
                                 .adjustToken(Optional.of(AdjustToken.STREAM_UPSELL))
                                 .pageName(Optional.of(Screen.STREAM.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forStreamClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.STREAM)
                                 .pageName(Optional.of(Screen.STREAM.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forDiscoveryImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.DISCOVERY)
                                 .adjustToken(Optional.of(AdjustToken.DISCOVERY_UPSELL))
                                 .pageName(Optional.of(Screen.SEARCH_MAIN.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forDiscoveryClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.DISCOVERY)
                                 .pageName(Optional.of(Screen.SEARCH_MAIN.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forCollectionImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.COLLECTION)
                                 .pageName(Optional.of(Screen.COLLECTIONS.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forCollectionClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.COLLECTION)
                                 .pageName(Optional.of(Screen.COLLECTIONS.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistTracksImpression(Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.PLAYLIST_TRACKS)
                                 .adjustToken(Optional.of(AdjustToken.PLAYLIST_TRACKS_UPSELL))
                                 .pageName(Optional.of(Screen.PLAYLIST_DETAILS.get()))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forPlaylistTracksClick(Urn playlistUrn) {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.PLAYLIST_TRACKS)
                                 .pageName(Optional.of(Screen.PLAYLIST_DETAILS.get()))
                                 .pageUrn(Optional.of(playlistUrn.toString()))
                                 .build();
    }

    public static UpgradeFunnelEvent forConversionBuyButtonImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.CONVERSION_BUY)
                                 .adjustToken(Optional.of(AdjustToken.CONVERSION))
                                 .pageName(Optional.of(Screen.CONVERSION.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forConversionBuyButtonClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.CONVERSION_BUY)
                                 .pageName(Optional.of(Screen.CONVERSION.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forChooserBuyMidTierImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.CHOOSER_BUY_MID_TIER)
                                 .pageName(Optional.of(Screen.PLAN_CHOICE.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forChooserBuyMidTierClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.CHOOSER_BUY_MID_TIER)
                                 .pageName(Optional.of(Screen.PLAN_CHOICE.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forChooserBuyHighTierImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.CHOOSER_BUY_HIGH_TIER)
                                 .pageName(Optional.of(Screen.PLAN_CHOICE.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forChooserBuyHighTierClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.CHOOSER_BUY_HIGH_TIER)
                                 .pageName(Optional.of(Screen.PLAN_CHOICE.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forConversionPromoImpression() {
        return UpgradeFunnelEvent.fromUpsellImpression(TCode.CONVERSION_PROMO)
                                 .adjustToken(Optional.of(AdjustToken.PROMO))
                                 .pageName(Optional.of(Screen.CONVERSION.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forConversionPromoClick() {
        return UpgradeFunnelEvent.fromUpsellClick(TCode.CONVERSION_PROMO)
                                 .pageName(Optional.of(Screen.CONVERSION.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forUpgradeSuccess() {
        return UpgradeFunnelEvent.from(UPGRADE_SUCCESS)
                                 .eventName(EventName.IMPRESSION)
                                 .impressionName(Optional.of(ImpressionName.CONSUMER_SUB_UPGRADE_SUCCESS))
                                 .build();
    }

    public static UpgradeFunnelEvent forResubscribeImpression() {
        return UpgradeFunnelEvent.from(RESUBSCRIBE_IMPRESSION)
                                 .eventName(EventName.IMPRESSION)
                                 .impressionName(Optional.of(ImpressionName.CONSUMER_SUB_RESUBSCRIBE))
                                 .impressionObject(Optional.of(TCode.RESUBSCRIBE_BUTTON.code()))
                                 .adjustToken(Optional.of(AdjustToken.PLAN_DOWNGRADED))
                                 .pageName(Optional.of(Screen.OFFLINE_OFFBOARDING.get()))
                                 .build();
    }

    public static UpgradeFunnelEvent forResubscribeClick() {
        return UpgradeFunnelEvent.from(RESUBSCRIBE_CLICK)
                                 .eventName(CLICK)
                                 .clickCategory(Optional.of(ClickCategory.CONSUMER_SUBS))
                                 .clickName(Optional.of(ClickName.CONSUMER_SUB_RESUBSCRIBE))
                                 .clickObject(Optional.of(TCode.RESUBSCRIBE_BUTTON.code()))
                                 .pageName(Optional.of(Screen.OFFLINE_OFFBOARDING.get()))
                                 .build();
    }

    @Override
    public UpgradeFunnelEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_UpgradeFunnelEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder timestamp(long timestamp);

        abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        abstract Builder kind(Kind kind);

        abstract Builder eventName(EventName eventName);

        abstract Builder pageName(Optional<String> pageName);

        abstract Builder pageUrn(Optional<String> pageUrn);

        abstract Builder clickName(Optional<ClickName> clickName);

        abstract Builder clickCategory(Optional<ClickCategory> clickCategory);

        abstract Builder clickObject(Optional<String> clickObject);

        abstract Builder impressionName(Optional<ImpressionName> impressionName);

        abstract Builder impressionCategory(Optional<String> impressionCategory);

        abstract Builder impressionObject(Optional<String> impressionObject);

        abstract Builder adjustToken(Optional<AdjustToken> adjustToken);

        abstract UpgradeFunnelEvent build();
    }
}
