package com.soundcloud.android.configuration;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUpdateLightCycleTest {

    private ConfigurationUpdateLightCycle lightCycle;

    @Mock private ConfigurationManager configurationManager;
    @Mock private Navigator navigator;
    @Mock private AppCompatActivity activity;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        lightCycle = new ConfigurationUpdateLightCycle(configurationManager, navigator, eventBus);
    }

    @Test
    public void shouldRestartAppForHighTierUpgradeIfPlanUpgradedFromFreeTier() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(Plan.FREE_TIER, Plan.HIGH_TIER));

        verify(navigator).restartForAccountUpgrade(activity);
    }

    @Test
    public void shouldRestartAppForHighTierUpgradeIfPlanUpgradedFromMidTier() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(Plan.MID_TIER, Plan.HIGH_TIER));

        verify(navigator).restartForAccountUpgrade(activity);
    }

    @Test // because we don't know how to handle this yet. Remove this test once we launch mid tier
    public void shouldNotRestartAppForUpgradeIfPlanUpgradedToMidTier() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forUpgrade(Plan.FREE_TIER, Plan.MID_TIER));

        verify(navigator, never()).restartForAccountUpgrade(activity);
    }

    @Test
    public void shouldRestartAppForDowngrade() {
        lightCycle.onStart(activity);

        eventBus.publish(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.forDowngrade(Plan.HIGH_TIER, Plan.FREE_TIER));

        verify(navigator).restartForAccountDowngrade(activity);
    }
}
