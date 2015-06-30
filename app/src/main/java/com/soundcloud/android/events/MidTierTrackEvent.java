package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;

public class MidTierTrackEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "Impression";

    private final Urn trackUrn;

    private MidTierTrackEvent(String kind, Urn trackUrn){
        this(kind, trackUrn, System.currentTimeMillis());
    }

    private MidTierTrackEvent(String kind, Urn trackUrn, long timestamp) {
        super(kind, timestamp);
        this.trackUrn = trackUrn;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public static MidTierTrackEvent forImpression(Urn trackUrn){
        return new MidTierTrackEvent(KIND_IMPRESSION, trackUrn);
    }
}
