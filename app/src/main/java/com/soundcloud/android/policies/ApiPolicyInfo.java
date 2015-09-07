package com.soundcloud.android.policies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ApiPolicyInfo {

    // For more information about the possible policies, consult:
    // https://github.com/soundcloud/jvmkit/blob/master/jvmkit/src/main/java/com/soundcloud/jvmkit/policies/ContentPolicy.java
    public static final String ALLOW = "ALLOW";
    public static final String MONETIZE = "MONETIZE";
    public static final String BLOCK = "BLOCK";
    public static final String SNIP = "SNIP";

    @JsonCreator
    public static ApiPolicyInfo create(@JsonProperty("urn") String trackUrn, @JsonProperty("monetizable") boolean monetizable,
                                       @JsonProperty("policy") String policy, @JsonProperty("syncable") boolean syncable,
                                       @JsonProperty("monetization_model") String monetizationModel,
                                       @JsonProperty("sub_mid_tier") boolean subMidTier, @JsonProperty("sub_high_tier") boolean subHighTier) {
            return new AutoValue_ApiPolicyInfo(new Urn(trackUrn), monetizable, policy, syncable, monetizationModel, subMidTier, subHighTier);
    }

    public abstract Urn getUrn();

    public abstract boolean isMonetizable();

    public abstract String getPolicy();

    public abstract boolean isSyncable();

    public abstract String getMonetizationModel();

    public abstract Boolean isSubMidTier();

    public abstract Boolean isSubHighTier();

}
