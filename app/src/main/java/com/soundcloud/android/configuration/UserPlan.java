package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class UserPlan {

    public final Plan currentPlan;
    public final List<Plan> upsells;

    @JsonCreator
    public UserPlan(@JsonProperty("id") String id,
                    @JsonProperty("upsells") List<String> upsells) {
        this.currentPlan = Plan.fromId(id);
        this.upsells = Plan.fromIds(upsells);
    }

}
