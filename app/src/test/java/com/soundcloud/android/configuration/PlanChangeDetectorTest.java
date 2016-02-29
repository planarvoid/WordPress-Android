package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlanChangeDetectorTest extends AndroidUnitTest {

    private PlanChangeDetector planChangeDetector;

    private TestEventBus eventBus = new TestEventBus();
    @Mock private FeatureOperations featureOperations;
    @Mock private ConfigurationSettingsStorage configurationSettingsStorage;

    @Before
    public void setUp() throws Exception {
        planChangeDetector = new PlanChangeDetector(eventBus, featureOperations, configurationSettingsStorage);
        when(configurationSettingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.UNDEFINED);
        when(configurationSettingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.UNDEFINED);
    }

    @Test
    public void persistsPlanDowngrades() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        verify(configurationSettingsStorage).storePendingPlanDowngrade(Plan.FREE_TIER);
    }

    @Test
    public void persistsPlanUpgrades() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.HIGH_TIER);

        verify(configurationSettingsStorage).storePendingPlanUpgrade(Plan.HIGH_TIER);
    }

    @Test
    public void doesNotPersistPlanChangesIfPlanIsTheSame() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        verify(configurationSettingsStorage, never()).storePendingPlanUpgrade(any(Plan.class));
        verify(configurationSettingsStorage, never()).storePendingPlanDowngrade(any(Plan.class));
    }

    @Test
    public void publishesPlanUpgrades() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.HIGH_TIER);

        UserPlanChangedEvent event = eventBus.lastEventOn(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.class);
        assertThat(event.isUpgrade()).isTrue();
        assertThat(event.oldPlan).isEqualTo(Plan.FREE_TIER);
        assertThat(event.newPlan).isEqualTo(Plan.HIGH_TIER);
    }

    @Test
    public void publishesPlanDowngrades() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        UserPlanChangedEvent event = eventBus.lastEventOn(EventQueue.USER_PLAN_CHANGE, UserPlanChangedEvent.class);
        assertThat(event.isDowngrade()).isTrue();
        assertThat(event.oldPlan).isEqualTo(Plan.HIGH_TIER);
        assertThat(event.newPlan).isEqualTo(Plan.FREE_TIER);
    }

    @Test
    public void doesNotPublishEventIfPlanDoesNotChange() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        eventBus.verifyNoEventsOn(EventQueue.USER_PLAN_CHANGE);
    }

    @Test
    public void isNoOpWhenPlanDowngradeAlreadySignalled() {
        when(configurationSettingsStorage.getPendingPlanDowngrade()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        verify(configurationSettingsStorage, never()).storePendingPlanDowngrade(any(Plan.class));
        eventBus.verifyNoEventsOn(EventQueue.USER_PLAN_CHANGE);
    }

    @Test
    public void isNoOpWhenPlanUpgradeAlreadySignalled() {
        when(configurationSettingsStorage.getPendingPlanUpgrade()).thenReturn(Plan.HIGH_TIER);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.HIGH_TIER);

        verify(configurationSettingsStorage, never()).storePendingPlanDowngrade(any(Plan.class));
        eventBus.verifyNoEventsOn(EventQueue.USER_PLAN_CHANGE);
    }
}
