package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;

public class MidTierTrackEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "Impression";
    public static final String KIND_CLICK = "Click";

    private final Urn trackUrn;
    private final String pageName;

    private MidTierTrackEvent(String kind, Urn trackUrn, String pageName){
        this(kind, trackUrn, pageName, System.currentTimeMillis());
    }

    private MidTierTrackEvent(String kind, Urn trackUrn, String pageName, long timestamp) {
        super(kind, timestamp);
        this.trackUrn = trackUrn;
        this.pageName = pageName;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public String getPageName() {
        return pageName;
    }

    public static MidTierTrackEvent forImpression(Urn trackUrn, String pageName){
        return new MidTierTrackEvent(KIND_IMPRESSION, trackUrn, pageName);
    }

    public static MidTierTrackEvent forClick(Urn trackUrn, String pageName) {
        return new MidTierTrackEvent(KIND_CLICK, trackUrn, pageName);
    }
}
