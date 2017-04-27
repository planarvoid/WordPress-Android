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

    public static Configuration deviceConflict() {
        return Configuration.builder()
                            .userPlan(new UserPlan(Plan.HIGH_TIER.planId, true, Optional.absent(), Collections.emptyList()))
                            .deviceManagement(new DeviceManagement(false, true))
                            .build();
    }

    private static Configuration buildWithPlan(String plan) {
        return Configuration.builder()
                            .userPlan(new UserPlan(plan, true, Optional.absent(), Collections.emptyList()))
                            .deviceManagement(new DeviceManagement(true, false))
                            .build();
    }

}
