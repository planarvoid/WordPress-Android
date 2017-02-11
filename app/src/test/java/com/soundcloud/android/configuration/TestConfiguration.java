package com.soundcloud.android.configuration;

import com.soundcloud.java.optional.Optional;

import java.util.Collections;

public final class TestConfiguration {

    private TestConfiguration() {}

    public static Configuration highTier() {
        return buildWithPlan(Plan.HIGH_TIER.planId);
    }

    public static Configuration midTier() {
        return buildWithPlan(Plan.MID_TIER.planId);
    }

    public static Configuration free() {
        return buildWithPlan(Plan.FREE_TIER.planId);
    }

    private static Configuration buildWithPlan(String plan) {
        final UserPlan userPlan = new UserPlan(plan, true, Optional.absent(), Collections.emptyList());
        return Configuration.builder().userPlan(userPlan).build();
    }

}
