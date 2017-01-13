package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PendingPlanOperationsTest {

    @Mock private ConfigurationSettingsStorage settingsStorage;

    private PendingPlanOperations operations;

    @Before
    public void setUp() throws Exception {
        when(settingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.UNDEFINED);
        when(settingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.UNDEFINED);
        operations = new PendingPlanOperations(settingsStorage);
    }

    @Test
    public void planChangePendingIfUpgradePending() {
        when(settingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.MID_TIER);

        assertThat(operations.hasPendingPlanChange()).isTrue();
    }

    @Test
    public void planChangePendingIfDowngradePending() {
        when(settingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.FREE_TIER);

        assertThat(operations.hasPendingPlanChange()).isTrue();
    }

    @Test
    public void noPlanChangePendingIfNoUpgradeOrDowngradeSet() {
        assertThat(operations.hasPendingPlanChange()).isFalse();
    }

    @Test
    public void hasPendingHighTierUpgrade() {
        when(settingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.HIGH_TIER);
        assertThat(operations.isPendingUpgrade()).isTrue();
    }

    @Test
    public void hasPendingHMidTierUpgrade() {
        when(settingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.MID_TIER);
        assertThat(operations.isPendingUpgrade()).isTrue();
    }

    @Test
    public void hasNoPendingUpgrade() {
        when(settingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.UNDEFINED);
        assertThat(operations.isPendingUpgrade()).isFalse();
    }

    @Test
    public void hasPendingFreeDowngrade() {
        when(settingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.FREE_TIER);
        assertThat(operations.isPendingDowngrade()).isTrue();
    }

    @Test
    public void hasPendingMidTierDowngrade() {
        when(settingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.MID_TIER);
        assertThat(operations.isPendingDowngrade()).isTrue();
    }

    @Test
    public void hasNoPendingDowngrade() {
        when(settingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.UNDEFINED);
        assertThat(operations.isPendingDowngrade()).isFalse();
    }

}