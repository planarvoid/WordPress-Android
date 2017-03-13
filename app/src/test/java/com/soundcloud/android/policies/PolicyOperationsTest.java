package com.soundcloud.android.policies;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateFailureEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class PolicyOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);

    private PolicyOperations operations;

    @Mock private ClearTableCommand clearTableCommand;
    @Mock private UpdatePoliciesCommand updatePoliciesCommand;
    @Mock private TxnResult writeResult;
    @Mock private LoadPolicyUpdateTimeCommand policyUpdateTimeCommand;
    @Mock private PolicyStorage policyStorage;

    private final List<Urn> tracks = singletonList(TRACK_URN);
    private ApiPolicyInfo apiPolicyInfo = ModelFixtures.apiPolicyInfo(TRACK_URN);
    private TestSubscriber<List<Urn>> observer = new TestSubscriber<>();
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        operations = new PolicyOperations(clearTableCommand, updatePoliciesCommand,
                                          policyUpdateTimeCommand, Schedulers.immediate(), policyStorage, eventBus);
    }

    @Test
    public void updatePoliciesFetchesAndStoresPoliciesInPlace() {
        Collection<ApiPolicyInfo> policies = singletonList(apiPolicyInfo);
        when(updatePoliciesCommand.toObservable(tracks)).thenReturn(just(policies));

        Collection<ApiPolicyInfo> policyInfos = operations.updatePolicies(tracks).toBlocking().single();

        assertThat(policyInfos).isEqualTo(policies);
    }

    @Test
    public void filtersMonetizableTracks() {
        when(updatePoliciesCommand.toObservable(asList(TRACK_URN, TRACK_URN2)))
                .thenReturn(just(createNotMonetizablePolicy(TRACK_URN2)));

        operations.filterMonetizableTracks(asList(TRACK_URN, TRACK_URN2)).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(TRACK_URN2);
    }

    @Test
    public void updateTrackPoliciesFetchesAndStorePoliciesForLoadedTracks() throws Exception {
        List<Urn> tracks = asList(TRACK_URN, TRACK_URN2);
        Collection<ApiPolicyInfo> policies = asList(
                ModelFixtures.apiPolicyInfo(TRACK_URN, true, ApiPolicyInfo.MONETIZE, false),
                ModelFixtures.apiPolicyInfo(TRACK_URN2, false, ApiPolicyInfo.ALLOW, false)
        );

        when(policyStorage.loadTracksForPolicyUpdate()).thenReturn(tracks);
        when(updatePoliciesCommand.call(tracks)).thenReturn(policies);
        when(writeResult.success()).thenReturn(true);

        List<Urn> result = operations.updateTrackPolicies();
        assertThat(result).containsAll(tracks);

        verify(updatePoliciesCommand).call(tracks);
    }

    @Test
    public void updateTrackPoliciesDoesNotClearPoliciesTable() throws Exception {
        List<Urn> tracks = asList(TRACK_URN, TRACK_URN2);
        Collection<ApiPolicyInfo> policies = asList(
                ModelFixtures.apiPolicyInfo(TRACK_URN, true, ApiPolicyInfo.MONETIZE, false),
                ModelFixtures.apiPolicyInfo(TRACK_URN2, false, ApiPolicyInfo.ALLOW, false)
        );

        when(policyStorage.loadTracksForPolicyUpdate()).thenReturn(tracks);
        when(updatePoliciesCommand.call(tracks)).thenReturn(policies);
        when(writeResult.success()).thenReturn(true);

        operations.updateTrackPolicies();

        verify(clearTableCommand, never()).call(Tables.TrackPolicies.TABLE);
    }

    @Test
    public void updateTrackPoliciesReturnEmptyListWhenPolicyFetchFailed() throws Exception {
        List<Urn> tracks = asList(TRACK_URN, TRACK_URN2);
        when(policyStorage.loadTracksForPolicyUpdate()).thenReturn(tracks);
        when(updatePoliciesCommand.call(tracks)).thenThrow(new RuntimeException("expected exception"));

        List<Urn> result = operations.updateTrackPolicies();
        assertThat(result).isEmpty();
    }

    @Test
    public void updateTrackPoliciesReportsFetchFailures() {
        List<Urn> tracks = asList(TRACK_URN, TRACK_URN2);
        when(policyStorage.loadTracksForPolicyUpdate()).thenReturn(tracks);
        when(updatePoliciesCommand.call(tracks)).thenThrow(new PolicyUpdateFailure(new IOException()));

        operations.updateTrackPolicies();

        PolicyUpdateFailureEvent failureEvent =
                eventBus.lastEventOn(EventQueue.TRACKING, PolicyUpdateFailureEvent.class);
        assertThat(failureEvent.reason()).isEqualTo(PolicyUpdateFailureEvent.Reason.KIND_POLICY_FETCH_FAILED);
    }

    @Test
    public void updateTrackPoliciesReportsInsertFailures() {
        List<Urn> tracks = asList(TRACK_URN, TRACK_URN2);
        when(policyStorage.loadTracksForPolicyUpdate()).thenReturn(tracks);
        when(updatePoliciesCommand.call(tracks)).thenThrow(
                new PolicyUpdateFailure(new PropellerWriteException("faild", new Exception())));

        operations.updateTrackPolicies();

        PolicyUpdateFailureEvent failureEvent =
                eventBus.lastEventOn(EventQueue.TRACKING, PolicyUpdateFailureEvent.class);
        assertThat(failureEvent.reason()).isEqualTo(PolicyUpdateFailureEvent.Reason.KIND_POLICY_WRITE_FAILED);
    }

    @Test
    public void refreshedTrackPoliciesWipesAndFetchesAndStoresPoliciesForLoadedTracks() throws Exception {
        List<Urn> tracks = asList(TRACK_URN, TRACK_URN2);
        Collection<ApiPolicyInfo> policies = asList(
                ModelFixtures.apiPolicyInfo(TRACK_URN, true, ApiPolicyInfo.MONETIZE, false),
                ModelFixtures.apiPolicyInfo(TRACK_URN2, false, ApiPolicyInfo.ALLOW, false)
        );
        when(policyStorage.tracksForPolicyUpdate()).thenReturn(just(tracks));
        when(updatePoliciesCommand.toObservable(tracks)).thenReturn(just(policies));

        operations.refreshedTrackPolicies().subscribe(observer);

        observer.assertValue(tracks);
        verify(clearTableCommand).call(Tables.TrackPolicies.TABLE);
    }

    @Test
    public void refreshedTrackPoliciesForwardsErrorsFromPolicyFetch() throws Exception {
        final RuntimeException exception = new RuntimeException();
        when(policyStorage.tracksForPolicyUpdate()).thenReturn(Observable.error(exception));

        operations.refreshedTrackPolicies().subscribe(observer);

        observer.assertError(exception);
    }

    private Collection<ApiPolicyInfo> createNotMonetizablePolicy(Urn trackUrn) {
        return singletonList(ModelFixtures.apiPolicyInfo(trackUrn, false, ApiPolicyInfo.ALLOW, false));
    }
}
