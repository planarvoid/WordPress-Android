package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackUrn;

public class PolicyInfo {

    private final TrackUrn trackUrn;
    private final boolean monetizable;
    private final String policy;

    @JsonCreator
    public PolicyInfo(@JsonProperty("urn") String trackUrn, @JsonProperty("monetizable") boolean monetizable,
                      @JsonProperty("policy") String policy) {
        this((TrackUrn) Urn.parse(trackUrn), monetizable, policy);
    }

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
