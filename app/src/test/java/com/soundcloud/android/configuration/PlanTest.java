package com.soundcloud.android.configuration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PlanTest {

    @Test
    public void shouldMapPlanBasedOnServerSideId() {
        assertThat(Plan.fromId("none")).isEqualTo(Plan.FREE_TIER);
        assertThat(Plan.fromId("mid_tier")).isEqualTo(Plan.MID_TIER);
        assertThat(Plan.fromId("high_tier")).isEqualTo(Plan.HIGH_TIER);
    }

    @Test
    public void shouldMapToFreeTierForUnhandledPlans() {
        assertThat(Plan.fromId("unknown plan")).isEqualTo(Plan.FREE_TIER);
    }

    @Test
    public void shouldMapToPlanCollectionFromIds() {
        assertThat(Plan.fromIds(asList("mid_tier", "high_tier"))).containsExactly(Plan.MID_TIER, Plan.HIGH_TIER);
    }

    @Test
    public void shouldMapToPlanIdsFromPlans() {
        assertThat(Plan.toIds(asList(Plan.MID_TIER, Plan.HIGH_TIER))).containsOnly("mid_tier", "high_tier");
    }
}
