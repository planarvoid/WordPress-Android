package com.soundcloud.android.downgrade;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.ClearTrackDownloadsCommand;
import com.soundcloud.android.policies.PolicyOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DowngradeProgressOperationsTest {

    private DowngradeProgressOperations operations;

    @Mock private ConfigurationOperations configurationOperations;
    @Mock private PolicyOperations policyOperations;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    private TestSubscriber<Object> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new DowngradeProgressOperations(configurationOperations,
                policyOperations, clearTrackDownloadsCommand);
    }

    @Test
    public void shouldClearDownloadedTracksAndAwaitUpdatedPolicies() {
        when(clearTrackDownloadsCommand.toObservable(null))
                .thenReturn(Observable.<List<Urn>>just(null));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertNoErrors();
    }

    @Test
    public void shouldResetPendingPlanChangeFlagsOnSuccess() {
        when(clearTrackDownloadsCommand.toObservable(null))
                .thenReturn(Observable.<List<Urn>>just(null));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.just(singletonList(Urn.forTrack(123))));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations).clearPendingPlanChanges();
    }

    @Test
    public void shouldNotResetPendingPlanChangeFlagsOnError() {
        when(clearTrackDownloadsCommand.toObservable(null))
                .thenReturn(Observable.<List<Urn>>just(null));
        when(policyOperations.refreshedTrackPolicies())
                .thenReturn(Observable.<List<Urn>>error(new Exception()));

        operations.awaitAccountDowngrade().subscribe(subscriber);

        verify(configurationOperations, never()).clearPendingPlanChanges();
    }

}
