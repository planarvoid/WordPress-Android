package com.soundcloud.android.configuration;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class PlanChangeOperationsTest {

    private PlanChangeOperations operations;

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private PlaySessionController playSessionController;

    private TestEventBus eventBus = new TestEventBus();
    private TestSubscriber<Object> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        when(offlineContentOperations.resetOfflineFeature()).thenReturn(Observable.just(null));
        operations = new PlanChangeOperations(configurationOperations, policyOperations, playSessionController, offlineContentOperations, eventBus);
    }

    @Test
    public void downgradeShouldAwaitUpdatedConfigurationAndPolicies() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertNoErrors();
    }

    @Test
    public void downgradeShouldPublishPolicyUpdateEvent() {
        Urn track1 = Urn.forTrack(123L);
        Urn track2 = Urn.forTrack(456L);
        when(policyOperations.refreshedTrackPolicies()).thenReturn(Observable.just(Arrays.asList(track1, track2)));
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        final PolicyUpdateEvent event = eventBus.lastEventOn(EventQueue.POLICY_UPDATES);
        assertThat(event.getTracks()).containsExactly(track1, track2);
    }

    @Test
    public void downgradeShouldClearDownloadedTracksOnCompletion() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);
    }

    @Test
    public void downgradeShouldResetPlaybackServiceOnSubscription() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.never());
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void downgradeShouldResetPendingPlanChangeFlagsOnSuccess() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void downgradeShouldNotResetPendingPlanChangeFlagsOnNetworkErrors() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.error(new IOException()));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
    }

    @Test
    public void downgradeShouldResetPendingPlanChangeFlagsOnOtherError() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.error(new Exception()));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldAwaitConfigurationAndPoliciesWhenPlanChangePending() {
        when(configurationOperations.isPendingUpgrade()).thenReturn(true);
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        subscriber.assertValue(singletonList(Urn.forTrack(123L)));
    }

    @Test
    public void upgradeShouldAwaitConfigurationAndPoliciesWhenNoPlanChangePending() {
        when(configurationOperations.isPendingUpgrade()).thenReturn(false);
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        subscriber.assertValue(singletonList(Urn.forTrack(123L)));
    }

    @Test
    public void upgradeShouldResetPlaybackServiceOnSubscriptionWhenNoPlanChangePending() {
        when(configurationOperations.isPendingUpgrade()).thenReturn(false);
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.never());
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.never());

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void upgradeShouldResetPlaybackServiceOnSubscriptionWhenPlanChangePending() {
        when(configurationOperations.isPendingUpgrade()).thenReturn(true);
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.never());

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void upgradeShouldResetPendingPlanChangeFlagsOnSuccess() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldNotResetPendingPlanChangeFlagsOnNetworkErrors() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.error(new IOException()));

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldResetPendingPlanChangeFlagsOnOtherErrors() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.error(new Exception()));

        operations.awaitAccountUpgrade(Plan.HIGH_TIER).subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void downgradeToFreeResetsOfflineFeature() {
        final PublishSubject<Void> clearObservable = PublishSubject.create();
        when(configurationOperations.awaitConfigurationFromPendingPlanChange()).thenReturn(Observable.just(TestConfiguration.free()));
        when(offlineContentOperations.resetOfflineFeature()).thenReturn(clearObservable);
        when(policyOperations.refreshedTrackPolicies()).thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        assertThat(clearObservable.hasObservers()).isTrue();
    }

    @Test
    public void downgradeToMidTierDoesNotClearOfflineContent() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange()).thenReturn(Observable.just(TestConfiguration.midTier()));
        when(policyOperations.refreshedTrackPolicies()).thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(offlineContentOperations, never()).resetOfflineFeature();
    }

}
