package com.soundcloud.android.policies;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PolicyOperationsTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);

    private PolicyOperations operations;

    @Mock private FetchPoliciesCommand fetchPoliciesCommand;
    @Mock private StorePoliciesCommand storePoliciesCommand;

    private final List<Urn> tracks = Collections.singletonList(TRACK_URN);
    private final PolicyInfo policyInfo = new PolicyInfo(TRACK_URN, true, "No", true);
    private TestSubscriber<List<Urn>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new PolicyOperations(fetchPoliciesCommand, storePoliciesCommand, Schedulers.immediate());

        when(fetchPoliciesCommand.toObservable())
                .thenReturn(Observable.<Collection<PolicyInfo>>just(Collections.singletonList(policyInfo)));

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
        operations.updatePolicies(tracks).subscribe();

        assertThat(storePoliciesCommand.getInput()).containsExactly(policyInfo);
        verify(storePoliciesCommand).call();
    }

    @Test
    public void filtersMonetizableTracks() {
        willFetchPolicies(
                createMonetizablePolicy(TRACK_URN),
                createNotMonetizablePolicy(TRACK_URN2));

        operations.filterMonetizableTracks(newArrayList(TRACK_URN, TRACK_URN2)).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).containsExactly(TRACK_URN2);
    }

    @Test
    public void filteringMonetizableTracksStoresTheFetchedPolicies() {
        PolicyInfo[] policies = {createMonetizablePolicy(TRACK_URN), createNotMonetizablePolicy(TRACK_URN2)};
        willFetchPolicies(policies);

        operations.filterMonetizableTracks(newArrayList(TRACK_URN, TRACK_URN2)).subscribe(observer);

        assertThat(storePoliciesCommand.getInput()).containsExactly(policies);
        verify(storePoliciesCommand).call();
    }


    private PolicyInfo createMonetizablePolicy(Urn trackUrn) {
        return new PolicyInfo(trackUrn, true, PolicyInfo.MONETIZE, false);
    }

    private PolicyInfo createNotMonetizablePolicy(Urn trackUrn) {
        return new PolicyInfo(trackUrn, false, PolicyInfo.ALLOW, false);
    }

    private void willFetchPolicies(PolicyInfo... policies) {
        when(fetchPoliciesCommand.toObservable())
                .thenReturn(Observable.<Collection<PolicyInfo>>just(newArrayList(policies)));
    }
}
