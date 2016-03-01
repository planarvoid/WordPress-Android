package com.soundcloud.android.configuration;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.ClearTrackDownloadsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlanChangeOperationsTest {

    private PlanChangeOperations operations;

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private PlaybackServiceInitiator playbackServiceInitiator;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    private TestSubscriber<Object> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new PlanChangeOperations(configurationOperations,
                policyOperations, playbackServiceInitiator, clearTrackDownloadsCommand);
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

        verify(clearTrackDownloadsCommand).call(null);
    }

    @Test
    public void downgradeShouldResetPlaybackServiceOnSubscription() {
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>never());
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(playbackServiceInitiator).resetPlaybackService();
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
    public void downgradeShouldNotResetPendingPlanChangeFlagsOnError() {
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>error(new Exception()));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
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

        verify(playbackServiceInitiator).resetPlaybackService();
    }

    @Test
    public void upgradeShouldResetPlaybackServiceOnSubscriptionWhenPlanChangePending() {
        when(configurationOperations.isPendingHighTierUpgrade()).thenReturn(true);
        when(configurationOperations.awaitConfigurationFromPendingPlanChange())
                .thenReturn(Observable.just(ModelFixtures.create(Configuration.class)));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>never());

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(playbackServiceInitiator).resetPlaybackService();
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
    public void upgradeShouldNotResetPendingPlanChangeFlagsOnError() {
        when(configurationOperations.awaitConfigurationWithPlan(Plan.HIGH_TIER))
                .thenReturn(Observable.<Configuration>error(new Exception()));

        operations.awaitAccountUpgrade().subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
    }

}
