package com.soundcloud.android.policies;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.apache.maven.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PolicyOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);

    private PolicyOperations operations;

    @Mock private FetchPoliciesCommand fetchPoliciesCommand;
    @Mock private StorePoliciesCommand storePoliciesCommand;
    @Mock private LoadTracksForPolicyUpdateCommand loadTracksForPolicyUpdateCommand;
    @Mock private WriteResult writeResult;

    private final List<Urn> tracks = Collections.singletonList(TRACK_URN);
    private ApiPolicyInfo apiPolicyInfo = ModelFixtures.apiPolicyInfo(TRACK_URN);
    private TestSubscriber<List<Urn>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new PolicyOperations(fetchPoliciesCommand, storePoliciesCommand, loadTracksForPolicyUpdateCommand, Schedulers.immediate());

        when(fetchPoliciesCommand.toObservable())
                .thenReturn(Observable.<Collection<ApiPolicyInfo>>just(Collections.singletonList(apiPolicyInfo)));

        observer = new TestSubscriber<>();
    }

    @Test
    public void updatePoliciesFetchesPolicies() {
        operations.updatePolicies(tracks).subscribe();

        verify(fetchPoliciesCommand).toObservable();
        assertThat(fetchPoliciesCommand.getInput()).containsExactly(TRACK_URN);
    }

    @Test
    public void updatePoliciesStoresPolicies() {
        Collection<ApiPolicyInfo> policies = Collections.singletonList(apiPolicyInfo);
        when(fetchPoliciesCommand.toObservable()).thenReturn(Observable.just(policies));

        operations.updatePolicies(tracks).subscribe();

        verify(storePoliciesCommand).call(policies);
    }

    @Test
    public void filtersMonetizableTracks() {
        when(fetchPoliciesCommand.toObservable()).thenReturn(Observable.just(createNotMonetizablePolicy(TRACK_URN2)));

        operations.filterMonetizableTracks(newArrayList(TRACK_URN, TRACK_URN2)).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(TRACK_URN2);
    }

    @Test
    public void filteringMonetizableTracksStoresTheFetchedPolicies() {
        Collection<ApiPolicyInfo> policies = Arrays.asList(
               ModelFixtures.apiPolicyInfo(TRACK_URN, true, ApiPolicyInfo.MONETIZE, false),
                ModelFixtures.apiPolicyInfo(TRACK_URN2, false, ApiPolicyInfo.ALLOW, false)
        );

        when(fetchPoliciesCommand.toObservable()).thenReturn(Observable.just((policies)));

        operations.filterMonetizableTracks(newArrayList(TRACK_URN, TRACK_URN2)).subscribe(observer);

        verify(storePoliciesCommand).call(policies);
    }

    @Test
    public void updateTrackPoliciesFetchesAndStorePoliciesForLoadedTracks() throws Exception {
        List<Urn> tracks = Arrays.asList(TRACK_URN, TRACK_URN2);
        Collection<PolicyInfo> policies = newArrayList(
                new PolicyInfo(TRACK_URN, true, PolicyInfo.MONETIZE, false),
                new PolicyInfo(TRACK_URN2, false, PolicyInfo.ALLOW, false));

        when(loadTracksForPolicyUpdateCommand.call(null)).thenReturn(tracks);
        when(fetchPoliciesCommand.call()).thenReturn(policies);
        when(writeResult.success()).thenReturn(true);
        when(storePoliciesCommand.call()).thenReturn(writeResult);

        List<Urn> result = operations.updateTrackPolicies();
        assertThat(result).containsAll(tracks);
        assertThat(storePoliciesCommand.getInput()).containsAll(policies);
        assertThat(fetchPoliciesCommand.getInput()).containsAll(tracks);

        verify(loadTracksForPolicyUpdateCommand).call(null);
        verify(fetchPoliciesCommand).call();
        verify(storePoliciesCommand).call();
    }

    @Test
    public void updateTrackPoliciesReturnEmptyListWhenPolicyFetchFailed() throws Exception {
        List<Urn> tracks = Arrays.asList(TRACK_URN, TRACK_URN2);
        when(loadTracksForPolicyUpdateCommand.call(null)).thenReturn(tracks);
        when(fetchPoliciesCommand.call()).thenThrow(new IOException("expected exception"));

        List<Urn> result = operations.updateTrackPolicies();
        assertThat(result).isEmpty();
    }

    private Collection<PolicyInfo> createNotMonetizablePolicy(Urn trackUrn) {
        return Collections.singletonList(new PolicyInfo(trackUrn, false, PolicyInfo.ALLOW, false));
    }

    private Collection<ApiPolicyInfo> createNotMonetizablePolicy(Urn trackUrn) {
        return Collections.singletonList(ModelFixtures.apiPolicyInfo(trackUrn, false, ApiPolicyInfo.ALLOW, false));
    }
}
