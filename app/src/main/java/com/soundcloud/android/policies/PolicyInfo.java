package com.soundcloud.android.policies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

class PolicyInfo {

    private final Urn trackUrn;
    private final boolean monetizable;
    private final String policy;
    private final boolean syncable;

    @JsonCreator
    public PolicyInfo(@JsonProperty("urn") String trackUrn, @JsonProperty("monetizable") boolean monetizable,
                      @JsonProperty("policy") String policy, @JsonProperty("syncable") boolean syncable) {
        this(new Urn(trackUrn), monetizable, policy, syncable);
    }

    public PolicyInfo(Urn trackUrn, boolean monetizable, String policy, boolean syncable) {
        this.trackUrn = trackUrn;
        this.monetizable = monetizable;
        this.policy = policy;
        this.syncable = syncable;
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

    public boolean isSyncable() {
        return syncable;
    }

    public String toString() {
        return Objects.toStringHelper(this)
                .add("trackUrn", trackUrn)
                .add("monetizable", monetizable)
                .add("policy", policy)
                .add("syncable", syncable).toString();
    }

}
