package com.soundcloud.android.configuration;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;

import java.io.IOException;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class PlanChangeOperationsTest {

    private PlanChangeOperations operations;

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private PendingPlanOperations pendingPlanOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private PlaySessionController playSessionController;

    private TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        when(offlineContentOperations.disableOfflineFeature()).thenReturn(Single.just(RxSignal.SIGNAL));
        operations = new PlanChangeOperations(configurationOperations, pendingPlanOperations, policyOperations, playSessionController, offlineContentOperations, eventBus);
    }

    @Test
    public void downgradeShouldAwaitUpdatedConfigurationAndPolicies() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        final TestObserver<Object> subscriber = operations.awaitAccountDowngrade().test();

        subscriber.assertValueCount(1);
        subscriber.assertNoErrors();
    }

    @Test
    public void downgradeShouldPublishPolicyUpdateEvent() {
        Urn track1 = Urn.forTrack(123L);
        Urn track2 = Urn.forTrack(456L);
        when(policyOperations.refreshedTrackPolicies()).thenReturn(Observable.just(Arrays.asList(track1, track2)));
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().test();

        final PolicyUpdateEvent event = eventBus.lastEventOn(EventQueue.POLICY_UPDATES);
        assertThat(event.getTracks()).containsExactly(track1, track2);
    }

    @Test
    public void downgradeShouldClearDownloadedTracksOnCompletion() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().test();
    }

    @Test
    public void downgradeShouldResetPlaybackServiceOnSubscription() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.never());
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().test();

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void downgradeShouldResetPendingPlanChangeFlagsOnSuccess() {
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountDowngrade().test();

        verify(pendingPlanOperations).clearPendingPlanChanges();
    }

    @Test
    public void downgradeShouldNotResetPendingPlanChangeFlagsOnNetworkErrors() {
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.error(new IOException()));

        operations.awaitAccountDowngrade().test();

        verify(pendingPlanOperations, never()).clearPendingPlanChanges();
    }

    @Test
    public void downgradeShouldResetPendingPlanChangeFlagsOnOtherError() {
        when(configurationOperations.awaitConfigurationFromPendingDowngrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.error(new Exception()));

        operations.awaitAccountDowngrade().test();

        verify(pendingPlanOperations).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldAwaitConfigurationAndPolicies() {
        when(configurationOperations.awaitConfigurationFromPendingUpgrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        final TestObserver<Object> subscriber = operations.awaitAccountUpgrade().test();

        subscriber.assertValue(singletonList(Urn.forTrack(123L)));
    }

    @Test
    public void upgradeShouldResetPlaybackServiceOnSubscription() {
        when(configurationOperations.awaitConfigurationFromPendingUpgrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.never());

        operations.awaitAccountUpgrade().test();

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void upgradeShouldResetPendingPlanChangeFlagsOnSuccess() {
        when(configurationOperations.awaitConfigurationFromPendingUpgrade())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountUpgrade().test();

        verify(pendingPlanOperations).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldNotResetPendingPlanChangeFlagsOnNetworkErrors() {
        when(configurationOperations.awaitConfigurationFromPendingUpgrade())
                .thenReturn(Observable.error(new IOException()));

        operations.awaitAccountUpgrade().test();

        verify(pendingPlanOperations, never()).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldResetPendingPlanChangeFlagsOnOtherErrors() {
        when(configurationOperations.awaitConfigurationFromPendingUpgrade())
                .thenReturn(Observable.error(new Exception()));

        operations.awaitAccountUpgrade().test();

        verify(pendingPlanOperations).clearPendingPlanChanges();
    }

    @Test
    public void downgradeToFreeResetsOfflineFeature() {
        final SingleSubject<RxSignal> clearObservable = SingleSubject.create();
        when(configurationOperations.awaitConfigurationFromPendingDowngrade()).thenReturn(Observable.just(TestConfiguration.free()));
        when(offlineContentOperations.disableOfflineFeature()).thenReturn(clearObservable);

        operations.awaitAccountDowngrade().test();

        assertThat(clearObservable.hasObservers()).isTrue();
    }

    @Test
    public void downgradeToMidTierDoesNotClearOfflineContent() {
        when(configurationOperations.awaitConfigurationFromPendingDowngrade()).thenReturn(Observable.just(TestConfiguration.midTier()));
        when(policyOperations.refreshedTrackPolicies()).thenReturn(Observable.just(singletonList(Urn.forTrack(123L))));

        operations.awaitAccountDowngrade().test();

        verify(offlineContentOperations, never()).disableOfflineFeature();
    }

}
