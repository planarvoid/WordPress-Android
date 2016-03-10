package com.soundcloud.android.configuration;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlanChangeOperationsTest {

    private PlanChangeOperations operations;

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private PlaySessionController playSessionController;

    private TestSubscriber<Object> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        when(offlineContentOperations.resetOfflineFeature()).thenReturn(Observable.<Void>just(null));
        operations = new PlanChangeOperations(configurationOperations,
                policyOperations, playSessionController, offlineContentOperations);
    }

    @Test
    public void downgradeShouldAwaitUpdatedConfigurationAndPolicies() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertNoErrors();
    }

    @Test
    public void downgradeShouldClearDownloadedTracksOnCompletion() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);
    }

    @Test
    public void downgradeShouldResetPlaybackServiceOnSubscription() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>never());
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
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void downgradeShouldNotResetPendingPlanChangeFlagsOnNetworkErrors() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>error(new IOException()));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
    }

    @Test
    public void downgradeShouldResetPendingPlanChangeFlagsOnOtherError() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>error(new Exception()));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldAwaitConfigurationAndPoliciesWhenPlanChangePending() {
        when(configurationOperations.isPendingHighTierUpgrade()).thenReturn(true);
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountUpgrade().subscribe(subscriber);

        subscriber.assertValue(singletonList(Urn.forTrack(123)));
    }

    @Test
    public void upgradeShouldAwaitConfigurationAndPoliciesWhenNoPlanChangePending() {
        when(configurationOperations.isPendingHighTierUpgrade()).thenReturn(false);
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountUpgrade().subscribe(subscriber);

        subscriber.assertValue(singletonList(Urn.forTrack(123)));
    }

    @Test
    public void upgradeShouldResetPlaybackServiceOnSubscriptionWhenNoPlanChangePending() {
        when(configurationOperations.isPendingHighTierUpgrade()).thenReturn(false);
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.<Configuration>never());
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>never());

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void upgradeShouldResetPlaybackServiceOnSubscriptionWhenPlanChangePending() {
        when(configurationOperations.isPendingHighTierUpgrade()).thenReturn(true);
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>never());

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(playSessionController).resetPlaySession();
    }

    @Test
    public void upgradeShouldResetPendingPlanChangeFlagsOnSuccess() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldNotResetPendingPlanChangeFlagsOnNetworkErrors() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.<Configuration>error(new IOException()));

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
    }

    @Test
    public void upgradeShouldResetPendingPlanChangeFlagsOnOtherErrors() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.<Configuration>error(new Exception()));

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void downgradeResetsOfflineFeature() {
        final PublishSubject<Void> clearObservable = PublishSubject.create();
        when(configurationOperations.awaitConfigurationFromPendingPlanChange()).thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(offlineContentOperations.resetOfflineFeature()).thenReturn(clearObservable);
        when(policyOperations.refreshedTrackPolicies()).thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        assertThat(clearObservable.hasObservers()).isTrue();
    }
}
