package com.soundcloud.android.configuration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class PlanTest {

    @Test
    public void shouldMapPlanBasedOnServerSideId() {
        assertThat(Plan.fromId("none")).isEqualTo(Plan.FREE_TIER);
        assertThat(Plan.fromId("mid_tier")).isEqualTo(Plan.MID_TIER);
        assertThat(Plan.fromId("high_tier")).isEqualTo(Plan.HIGH_TIER);
    }

    @Test
    public void shouldMapToUndefinedForUnhandledPlans() {
        assertThat(Plan.fromId("unknown plan")).isEqualTo(Plan.UNDEFINED);
    }

    @Test
    public void shouldMapToUndefinedForNull() {
        assertThat(Plan.fromId(null)).isEqualTo(Plan.UNDEFINED);
    }

    @Test
    public void shouldMapToPlanCollectionFromIds() {
        assertThat(Plan.fromIds(asList("unknown", "mid_tier", "high_tier")))
                .containsExactly(Plan.MID_TIER, Plan.HIGH_TIER);
    }

    @Test
    public void shouldMapToPlanIdsFromPlans() {
        assertThat(Plan.toIds(asList(Plan.MID_TIER, Plan.HIGH_TIER))).containsOnly("mid_tier", "high_tier");
    }

    @Test
    public void shouldIgnoreUndefinedWhenMappingToPlanIds() {
        assertThat(Plan.toIds(asList(Plan.UNDEFINED, Plan.MID_TIER, Plan.HIGH_TIER)))
                .containsOnly("mid_tier", "high_tier");
    }

    @Test
    public void shouldBeComparableBasedOnPlanType() {
        final List<Plan> plans = asList(Plan.HIGH_TIER, Plan.FREE_TIER, Plan.UNDEFINED, Plan.MID_TIER);
        Collections.sort(plans);
        assertThat(plans).containsExactly(Plan.UNDEFINED, Plan.FREE_TIER, Plan.MID_TIER, Plan.HIGH_TIER);
    }

    @Test
    public void shouldIndicateDowngradeToFreeTier() {
        assertThat(Plan.FREE_TIER.isDowngradeFrom(Plan.UNDEFINED)).isFalse();
        assertThat(Plan.FREE_TIER.isDowngradeFrom(Plan.FREE_TIER)).isFalse();
        assertThat(Plan.FREE_TIER.isDowngradeFrom(Plan.MID_TIER)).isTrue();
        assertThat(Plan.FREE_TIER.isDowngradeFrom(Plan.HIGH_TIER)).isTrue();
    }

    @Test
    public void shouldIndicateDowngradeToMidTier() {
        assertThat(Plan.MID_TIER.isDowngradeFrom(Plan.UNDEFINED)).isFalse();
        assertThat(Plan.MID_TIER.isDowngradeFrom(Plan.FREE_TIER)).isFalse();
        assertThat(Plan.MID_TIER.isDowngradeFrom(Plan.MID_TIER)).isFalse();
        assertThat(Plan.MID_TIER.isDowngradeFrom(Plan.HIGH_TIER)).isTrue();
    }

    @Test
    public void shouldNeverIndicateHighTierAsDowngrade() {
        assertThat(Plan.HIGH_TIER.isDowngradeFrom(Plan.UNDEFINED)).isFalse();
        assertThat(Plan.HIGH_TIER.isDowngradeFrom(Plan.FREE_TIER)).isFalse();
        assertThat(Plan.HIGH_TIER.isDowngradeFrom(Plan.MID_TIER)).isFalse();
        assertThat(Plan.HIGH_TIER.isDowngradeFrom(Plan.HIGH_TIER)).isFalse();
    }

    @Test
    public void shouldIndicateUpgradeToMidTier() {
        assertThat(Plan.MID_TIER.isUpgradeFrom(Plan.UNDEFINED)).isFalse();
        assertThat(Plan.MID_TIER.isUpgradeFrom(Plan.FREE_TIER)).isTrue();
        assertThat(Plan.MID_TIER.isUpgradeFrom(Plan.MID_TIER)).isFalse();
        assertThat(Plan.MID_TIER.isUpgradeFrom(Plan.HIGH_TIER)).isFalse();
    }

    @Test
    public void shouldIndicateUpgradeToHighTier() {
        assertThat(Plan.HIGH_TIER.isUpgradeFrom(Plan.UNDEFINED)).isFalse();
        assertThat(Plan.HIGH_TIER.isUpgradeFrom(Plan.FREE_TIER)).isTrue();
        assertThat(Plan.HIGH_TIER.isUpgradeFrom(Plan.MID_TIER)).isTrue();
        assertThat(Plan.HIGH_TIER.isUpgradeFrom(Plan.HIGH_TIER)).isFalse();
    }

    @Test
    public void shouldNeverIndicateFreeTierAsUpgrade() {
        assertThat(Plan.FREE_TIER.isUpgradeFrom(Plan.UNDEFINED)).isFalse();
        assertThat(Plan.FREE_TIER.isUpgradeFrom(Plan.FREE_TIER)).isFalse();
        assertThat(Plan.FREE_TIER.isUpgradeFrom(Plan.MID_TIER)).isFalse();
        assertThat(Plan.FREE_TIER.isUpgradeFrom(Plan.HIGH_TIER)).isFalse();
    }
}
