package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class UserPlan {

    final Plan currentPlan;
    final List<Upsell> planUpsells;

    @JsonCreator
    public UserPlan(@JsonProperty("id") String id,
                    @JsonProperty("plan_upsells") List<Upsell> upsells) {
        this.currentPlan = Plan.fromId(id);
        this.planUpsells = Collections.unmodifiableList(upsells);
    }

    public static class Upsell {

        public final String id;
        final int trialDays;

        @JsonCreator
        public Upsell(@JsonProperty("id") String id,
                      @JsonProperty("trial_days") int trialDays) {
            this.id = id;
            this.trialDays = trialDays;
        }
    }

}
