package com.soundcloud.android.configuration;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Plan {

    FREE_TIER("none"),
    MID_TIER("mid_tier"),
    HIGH_TIER("high_tier");

    public final String planId;

    Plan(String planId) {
        this.planId = planId;
    }

    @Override
    public String toString() {
        return planId;
    }

    public boolean isUpgradeFrom(@NonNull Plan existingPlan) {
        return this.compareTo(existingPlan) > 0;
    }

    public boolean isDowngradeFrom(@NonNull Plan existingPlan) {
        return this.compareTo(existingPlan) < 0;
    }

    public static Plan fromId(String planId) {
        switch (planId) {
            case "mid_tier":
                return MID_TIER;
            case "high_tier":
                return HIGH_TIER;
            default:
                return FREE_TIER;
        }
    }

    public static List<Plan> fromIds(Collection<String> planIds) {
        final List<Plan> plans = new ArrayList<>(planIds.size());
        for (String id : planIds) {
            plans.add(Plan.fromId(id));
        }
        return plans;
    }

    public static Set<String> toIds(Collection<Plan> plans) {
        final HashSet<String> ids = new HashSet<>(plans.size());
        for (Plan plan : plans) {
            ids.add(plan.planId);
        }
        return ids;
    }
}
