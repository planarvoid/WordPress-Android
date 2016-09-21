package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;

public class AdDeliveryEvent extends TrackingEvent {

    public static final String AD_DELIVERED_KIND = "AD_DELIVERED";

    public final Urn adUrn;
    public final String adRequestId;
    public final boolean inForeground;
    public final boolean playerVisible;

    private AdDeliveryEvent(String kind, Urn monetizableUrn, Urn adUrn, String adRequestId,
                            boolean playerVisible, boolean inForeground) {
        super(kind);
        this.put(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN, monetizableUrn.toString());
        this.adUrn = adUrn;
        this.adRequestId = adRequestId;
        this.inForeground = inForeground;
        this.playerVisible = playerVisible;
    }

    public static AdDeliveryEvent adDelivered(Urn monetizableUrn, Urn adUrn, String adRequestId,
                                              boolean playerVisible, boolean inForeground) {
        return new AdDeliveryEvent(AD_DELIVERED_KIND,
                                   monetizableUrn,
                                   adUrn,
                                   adRequestId,
                                   playerVisible,
                                   inForeground);
    }
}
