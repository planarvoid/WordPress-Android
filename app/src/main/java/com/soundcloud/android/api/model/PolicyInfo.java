package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soundcloud.android.tracks.TrackUrn;

public class PolicyInfo {

    private final TrackUrn trackUrn;
    private final boolean monetizable;
    private final String policy;

    @JsonCreator
    public PolicyInfo(TrackUrn trackUrn, boolean monetizable, String policy) {
        this.trackUrn = trackUrn;
        this.monetizable = monetizable;
        this.policy = policy;
    }

    public TrackUrn getTrackUrn() {
        return trackUrn;
    }

    public boolean isMonetizable() {
        return monetizable;
    }

    public String getPolicy() {
        return policy;
    }
}
