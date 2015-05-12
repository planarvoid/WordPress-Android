package com.soundcloud.android.events;

import com.google.common.base.Optional;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PromotedTrackEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String TYPE_PROMOTED = "promoted";

    private final List<String> trackingUrls;

    private PromotedTrackEvent(@NotNull String kind, long timeStamp, String adUrn, String trackUrn,
                               Optional<Urn> promoterUrn, List<String> trackingUrls, String screen) {
        super(kind, timeStamp);
        this.trackingUrls = trackingUrls;

        put(AdTrackingKeys.KEY_MONETIZATION_TYPE, TYPE_PROMOTED);
        put(AdTrackingKeys.KEY_AD_URN, adUrn);
        put(AdTrackingKeys.KEY_ORIGIN_SCREEN, screen);
        put(AdTrackingKeys.KEY_AD_TRACK_URN, trackUrn);
        if (promoterUrn.isPresent()) {
            put(AdTrackingKeys.KEY_PROMOTER_URN, promoterUrn.get().toString());
        }
    }

    public static PromotedTrackEvent forPromoterClick(PromotedTrackItem promotedTrack, String screen) {
        PromotedTrackEvent click = basePromotedEvent(KIND_CLICK, promotedTrack, promotedTrack.getPromoterClickUrls(), screen);
        click.put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, promotedTrack.getEntityUrn().toString());
        click.put(AdTrackingKeys.KEY_CLICK_TARGET_URN, promotedTrack.getPromoterUrn().get().toString());
        return click;
    }

    public static PromotedTrackEvent forTrackClick(PromotedTrackItem promotedTrack, String screen) {
        PromotedTrackEvent click = basePromotedEvent(KIND_CLICK, promotedTrack, promotedTrack.getClickUrls(), screen);
        click.put(AdTrackingKeys.KEY_CLICK_OBJECT_URN, promotedTrack.getEntityUrn().toString());
        click.put(AdTrackingKeys.KEY_CLICK_TARGET_URN, promotedTrack.getEntityUrn().toString());
        return click;
    }

    public static PromotedTrackEvent forImpression(PromotedTrackItem promotedTrack, String screen) {
        return basePromotedEvent(KIND_IMPRESSION, promotedTrack, promotedTrack.getImpressionUrls(), screen);
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    @NotNull
    private static PromotedTrackEvent basePromotedEvent(String kind, PromotedTrackItem promotedTrack,
                                                        List<String> trackingUrls, String screen) {
        return new PromotedTrackEvent(kind,
                System.currentTimeMillis(),
                promotedTrack.getAdUrn(),
                promotedTrack.getEntityUrn().toString(),
                promotedTrack.getPromoterUrn(),
                trackingUrls,
                screen
        );
    }

}
