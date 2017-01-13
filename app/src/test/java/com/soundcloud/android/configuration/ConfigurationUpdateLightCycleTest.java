package com.soundcloud.android.configuration;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUpdateLightCycleTest {

    private ConfigurationUpdateLightCycle lightCycle;

    @Mock private ConfigurationManager configurationManager;
    @Mock private PendingPlanOperations pendingPlanOperations;
    @Mock private Navigator navigator;
    @Mock private AppCompatActivity activity;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        lightCycle = new ConfigurationUpdateLightCycle(configurationManager, pendingPlanOperations, navigator, eventBus);
    }

    @Test
    public void shouldResetAppForHighTierUpgradeIfPlanUpgradedFromFreeTier() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(Plan.FREE_TIER, Plan.HIGH_TIER));

        verify(navigator).resetForAccountUpgrade(activity);
    }

    @Test
    public void shouldResetAppForHighTierUpgradeIfPlanUpgradedFromMidTier() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(Plan.MID_TIER, Plan.HIGH_TIER));

        verify(navigator).resetForAccountUpgrade(activity);
    }

    @Test
    public void shouldRestartAppForUpgradeIfPlanUpgradedToMidTier() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(Plan.FREE_TIER, Plan.MID_TIER));

        verify(navigator).resetForAccountUpgrade(activity);
    }

    @Test
    public void shouldResetAppForDowngrade() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forDowngrade(Plan.HIGH_TIER, Plan.FREE_TIER));

        verify(navigator).resetForAccountDowngrade(activity);
    }

}
