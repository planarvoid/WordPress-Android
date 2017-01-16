package com.soundcloud.android.events;

import static com.soundcloud.android.events.PromotedTrackingEvent.ImpressionName.PROMOTED_PLAYLIST;
import static com.soundcloud.android.events.PromotedTrackingEvent.ImpressionName.PROMOTED_TRACK;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoValue
public abstract class PromotedTrackingEvent extends NewTrackingEvent {

    public static final String CLICK_NAME = "item_navigation";
    public enum ImpressionName {

        PROMOTED_PLAYLIST("promoted_playlist"),
        PROMOTED_TRACK("promoted_track");
        private final String key;

        ImpressionName(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public enum Kind {
        KIND_IMPRESSION("impression"),
        KIND_CLICK("click");
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    private static final String TYPE_PROMOTED = "promoted";

    public abstract Kind kind();

    public abstract List<String> trackingUrls();

    public abstract String monetizationType();

    public abstract String adUrn();

    public abstract String originScreen();

    public abstract Optional<Urn> impressionObject();

    public abstract Optional<ImpressionName> impressionName();

    public abstract Optional<Urn> promoterUrn();

    public abstract Optional<Urn> clickObject();

    public abstract Optional<Urn> clickTarget();

    public abstract Optional<String> clickName();

    private static PromotedTrackingEvent.Builder create(Kind kind, String adUrn, Optional<Urn> promoterUrn, List<String> trackingUrls, String screen) {
        return new AutoValue_PromotedTrackingEvent.Builder().id(defaultId())
                                                            .timestamp(defaultTimestamp())
                                                            .referringEvent(Optional.absent())
                                                            .kind(kind)
                                                            .trackingUrls(trackingUrls)
                                                            .monetizationType(TYPE_PROMOTED)
                                                            .adUrn(adUrn)
                                                            .originScreen(screen)
                                                            .impressionObject(Optional.absent())
                                                            .impressionName(Optional.absent())
                                                            .promoterUrn(promoterUrn)
                                                            .clickObject(Optional.absent())
                                                            .clickTarget(Optional.absent())
                                                            .clickName(Optional.absent());
    }

    public static PromotedTrackingEvent forPromoterClick(PromotedListItem promotedItem, String screen) {
        return basePromotedEvent(Kind.KIND_CLICK, promotedItem, promotedItem.getPromoterClickUrls(), screen)
                .clickObject(Optional.of(promotedItem.getUrn()))
                .clickTarget(Optional.of(promotedItem.getPromoterUrn().get()))
                .clickName(Optional.of(CLICK_NAME))
                .build();
    }

    public static PromotedTrackingEvent forItemClick(PromotedListItem promotedItem, String screen) {
        return basePromotedEvent(Kind.KIND_CLICK, promotedItem, promotedItem.getClickUrls(), screen)
                .clickObject(Optional.of(promotedItem.getUrn()))
                .clickTarget(Optional.of(promotedItem.getUrn()))
                .clickName(Optional.of(CLICK_NAME))
                .build();
    }

    public static PromotedTrackingEvent forImpression(PromotedListItem promotedItem, String screen) {
        final Urn promotedItemUrn = promotedItem.getUrn();
        final ImpressionName impressionName = promotedItemUrn.isPlaylist() ? PROMOTED_PLAYLIST : PROMOTED_TRACK;
        return basePromotedEvent(Kind.KIND_IMPRESSION, promotedItem, promotedItem.getImpressionUrls(), screen).impressionObject(Optional.of(promotedItemUrn))
                                                                                                              .impressionName(Optional.of(impressionName))
                                                                                                              .build();
    }

    @NotNull
    private static PromotedTrackingEvent.Builder basePromotedEvent(Kind kind, PromotedListItem promotedItem,
                                                                   List<String> trackingUrls, String screen) {
        return PromotedTrackingEvent.create(kind, promotedItem.getAdUrn(), promotedItem.getPromoterUrn(), trackingUrls, screen);
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return null;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder timestamp(long timestamp);

        abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        abstract Builder kind(Kind kind);

        abstract Builder trackingUrls(List<String> trackingUrls);

        abstract Builder monetizationType(String monetizationType);

        abstract Builder adUrn(String adUrn);

        abstract Builder originScreen(String originScreen);

        abstract Builder impressionObject(Optional<Urn> impressionObject);

        abstract Builder impressionName(Optional<ImpressionName> impressionName);

        abstract Builder promoterUrn(Optional<Urn> promoterUrn);

        abstract Builder clickObject(Optional<Urn> clickObject);

        abstract Builder clickTarget(Optional<Urn> clickTarget);

        abstract Builder clickName(Optional<String> clickName);

        public abstract PromotedTrackingEvent build();
    }
}
