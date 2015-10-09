package com.soundcloud.android.events;

public final class AttributionEvent extends TrackingEvent {

    public static final String NETWORK = "network";
    public static final String CAMPAIGN = "campaign";
    public static final String ADGROUP = "adgroup";
    public static final String CREATIVE = "creative";

    public AttributionEvent(String network, String campaign, String adGroup, String creative) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        put(NETWORK, network);
        put(CAMPAIGN, campaign);
        put(ADGROUP, adGroup);
        put(CREATIVE, creative);
    }
}
