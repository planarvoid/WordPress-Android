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
    @Mock private PendingPlanOperations pendingPlanOperations;

    @Before
    public void setUp() throws Exception {
        planChangeDetector = new PlanChangeDetector(eventBus, featureOperations, pendingPlanOperations);
        when(pendingPlanOperations.hasPendingPlanChange()).thenReturn(false);
    }

    @Test
    public void persistsPlanDowngrades() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        verify(pendingPlanOperations).setPendingDowngrade(Plan.FREE_TIER);
    }

    @Test
    public void persistsPlanUpgrades() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.HIGH_TIER);

        verify(pendingPlanOperations).setPendingUpgrade(Plan.HIGH_TIER);
    }

    @Test
    public void doesNotPersistPlanChangesIfPlanIsTheSame() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        verify(pendingPlanOperations, never()).setPendingUpgrade(any(Plan.class));
        verify(pendingPlanOperations, never()).setPendingDowngrade(any(Plan.class));
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
    public void isNoOpWhenPlanChangeIsAlreadySignalled() {
        when(pendingPlanOperations.hasPendingPlanChange()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);

        planChangeDetector.handleRemotePlan(Plan.FREE_TIER);

        verify(pendingPlanOperations, never()).setPendingDowngrade(any(Plan.class));
        verify(pendingPlanOperations, never()).setPendingUpgrade(any(Plan.class));
        eventBus.verifyNoEventsOn(EventQueue.USER_PLAN_CHANGE);
    }

}
