package com.soundcloud.android.events;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PromotedTrackingEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String TYPE_PROMOTED = "promoted";

    private final List<String> trackingUrls;

    private PromotedTrackingEvent(@NotNull String kind, long timeStamp, String adUrn, String itemUrn,
                                  Optional<Urn> promoterUrn, List<String> trackingUrls, String screen) {
        super(kind, timeStamp);
        this.trackingUrls = trackingUrls;

        put(AdTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_PROMOTED);
        put(AdTrackingKeys.KEY_AD_URN, adUrn);
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, screen);
        put(AdTrackingKeys.KEY_AD_TRACK_URN, itemUrn);
        if (promoterUrn.isPresent()) {
            put(AdTrackingKeys.KEY_PROMOTER_URN, promoterUrn.get().toString());
        }
    }

    public static PromotedTrackingEvent forPromoterClick(PromotedListItem promotedItem, String screen) {
        PromotedTrackingEvent click = basePromotedEvent(KIND_CLICK, promotedItem, promotedItem.getPromoterClickUrls(), screen);
        click.put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, promotedItem.getEntityUrn().toString());
        click.put(AdTrackingKeys.KEY_CLICK_TARGET_URN, promotedItem.getPromoterUrn().get().toString());
        return click;
    }

    public static PromotedTrackingEvent forItemClick(PromotedListItem promotedItem, String screen) {
        PromotedTrackingEvent click = basePromotedEvent(KIND_CLICK, promotedItem, promotedItem.getClickUrls(), screen);
        click.put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, promotedItem.getEntityUrn().toString());
        click.put(AdTrackingKeys.KEY_CLICK_TARGET_URN, promotedItem.getEntityUrn().toString());
        return click;
    }

    public static PromotedTrackingEvent forImpression(PromotedListItem promotedItem, String screen) {
        return basePromotedEvent(KIND_IMPRESSION, promotedItem, promotedItem.getImpressionUrls(), screen);
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    @NotNull
    private static PromotedTrackingEvent basePromotedEvent(String kind, PromotedListItem promotedItem,
                                                           List<String> trackingUrls, String screen) {
        return basePromotedEvent(kind, PromotedSourceInfo.fromItem(promotedItem), trackingUrls, screen);
    }

    @NotNull
    private static PromotedTrackingEvent basePromotedEvent(String kind, PromotedSourceInfo promotedSource,
                                                           List<String> trackingUrls, String screen) {
        return new PromotedTrackingEvent(kind,
                System.currentTimeMillis(),
                promotedSource.getAdUrn(),
                promotedSource.getPromotedItemUrn().toString(),
                promotedSource.getPromoterUrn(),
                trackingUrls,
                screen
        );
    }
}
