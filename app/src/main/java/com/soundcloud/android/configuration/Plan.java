package com.soundcloud.android.configuration;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.UserPlan.Upsell;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Plan {

    UNDEFINED("undefined", R.string.tier_free),
    FREE_TIER("none", R.string.tier_free),
    MID_TIER("mid_tier", R.string.tier_go),
    HIGH_TIER("high_tier", R.string.tier_plus);

    public final String planId;
    @StringRes public final int tierName;

    Plan(String planId, @StringRes int tierName) {
        this.planId = planId;
        this.tierName = tierName;
    }

    public boolean isUpgradeFrom(@NonNull Plan existingPlan) {
        return this != UNDEFINED && existingPlan != UNDEFINED && this.compareTo(existingPlan) > 0;
    }

    public boolean isDowngradeFrom(@NonNull Plan existingPlan) {
        return this != UNDEFINED && existingPlan != UNDEFINED && this.compareTo(existingPlan) < 0;
    }

    public boolean isGoPlan() {
        return this == MID_TIER || this == HIGH_TIER;
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

    public static List<Plan> fromIds(Collection<String> ids) {
        final List<Plan> plans = new ArrayList<>(ids.size());
        for (String id : ids) {
            final Plan plan = Plan.fromId(id);
            if (plan != UNDEFINED) {
                plans.add(plan);
            }
        }
        return plans;
    }

    public static List<Plan> fromUpsells(Collection<Upsell> upsells) {
        final List<Plan> plans = new ArrayList<>(upsells.size());
        for (Upsell upsell : upsells) {
            final Plan plan = Plan.fromId(upsell.id);
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
