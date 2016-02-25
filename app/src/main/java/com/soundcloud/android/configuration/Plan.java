package com.soundcloud.android.configuration;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Plan {

    UNDEFINED("undefined"),
    FREE_TIER("none"),
    MID_TIER("mid_tier"),
    HIGH_TIER("high_tier");

    public final String planId;

    Plan(String planId) {
        this.planId = planId;
    }

    public boolean isUpgradeFrom(@NonNull Plan existingPlan) {
        return this != UNDEFINED && existingPlan != UNDEFINED && this.compareTo(existingPlan) > 0;
    }

    public boolean isDowngradeFrom(@NonNull Plan existingPlan) {
        return this != UNDEFINED && existingPlan != UNDEFINED && this.compareTo(existingPlan) < 0;
    }

    public static Plan fromId(@Nullable String planId) {
        if (planId == null) {
            return UNDEFINED;
        }
        switch (planId) {
            case "none":
                return FREE_TIER;
            case "mid_tier":
                return MID_TIER;
            case "high_tier":
                return HIGH_TIER;
            default:
                return UNDEFINED;
        }
    }

    public static List<Plan> fromIds(Collection<String> planIds) {
        final List<Plan> plans = new ArrayList<>(planIds.size());
        for (String id : planIds) {
            final Plan plan = Plan.fromId(id);
            if (plan != UNDEFINED) {
                plans.add(plan);
            }
        }
        return plans;
    }

    public static Set<String> toIds(Collection<Plan> plans) {
        final HashSet<String> ids = new HashSet<>(plans.size());
        for (Plan plan : plans) {
            if (plan != UNDEFINED) {
                ids.add(plan.planId);
            }
        }
        return ids;
    }
}
