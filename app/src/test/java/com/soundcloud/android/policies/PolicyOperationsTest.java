package com.soundcloud.android.policies;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
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

import java.util.Arrays;
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
    private ApiPolicyInfo apiPolicyInfo = ModelFixtures.apiPolicyInfo(TRACK_URN);
    private TestSubscriber<List<Urn>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new PolicyOperations(fetchPoliciesCommand, storePoliciesCommand, Schedulers.immediate());

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

    private Collection<ApiPolicyInfo> createNotMonetizablePolicy(Urn trackUrn) {
        return Collections.singletonList(ModelFixtures.apiPolicyInfo(trackUrn, false, ApiPolicyInfo.ALLOW, false));
    }
}
