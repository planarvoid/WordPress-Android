package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class UserPlan {

    public final String id;
    public final List<String> upsells;

    @JsonCreator
    public UserPlan(@JsonProperty("id") String id,
                    @JsonProperty("upsells") List<String> upsells) {
        this.id = id;
        this.upsells = upsells;
    }

}
