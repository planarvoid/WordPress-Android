package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;

public class PolicyInfo {

    private final Urn trackUrn;
    private final boolean monetizable;
    private final String policy;

    @JsonCreator
    public PolicyInfo(@JsonProperty("urn") String trackUrn, @JsonProperty("monetizable") boolean monetizable,
                      @JsonProperty("policy") String policy) {
        this(new Urn(trackUrn), monetizable, policy);
    }

    public PolicyInfo(Urn trackUrn, boolean monetizable, String policy) {
        this.trackUrn = trackUrn;
        this.monetizable = monetizable;
        this.policy = policy;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public boolean isMonetizable() {
        return monetizable;
    }

    public String getPolicy() {
        return policy;
    }
}
