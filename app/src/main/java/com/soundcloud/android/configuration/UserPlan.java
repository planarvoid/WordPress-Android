package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class UserPlan {

    public final String id;
    public final String upsell;

    @JsonCreator
    public UserPlan(@JsonProperty("id") String id,
                    @JsonProperty("upsell") String upsell) {
        this.id = id;
        this.upsell = upsell;
    }

}
