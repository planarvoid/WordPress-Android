package com.soundcloud.android.policies;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PolicyOperationsTest {

    private PolicyOperations operations;

    @Mock private FetchPoliciesCommand fetchPoliciesCommand;
    @Mock private StorePoliciesCommand storePoliciesCommand;

    private final Urn urn = Urn.forTrack(123L);
    private final List<Urn> tracks = Arrays.asList(urn);
    private final PolicyInfo policyInfo = new PolicyInfo(urn, true, "No", true);

    @Before
    public void setUp() throws Exception {
        operations = new PolicyOperations(fetchPoliciesCommand, storePoliciesCommand, Schedulers.immediate());
        when(fetchPoliciesCommand.toObservable()).thenReturn(Observable.<Collection<PolicyInfo>>just(Arrays.asList(policyInfo)));
    }

    @Test
    public void fetchAndStorePoliciesFetchesPolicies() {
        operations.fetchAndStorePolicies(tracks).subscribe();

        verify(fetchPoliciesCommand).toObservable();
        expect(fetchPoliciesCommand.getInput()).toContainExactly(urn);
    }

    @Test
    public void fetchAndStorePoliciesStoresPolicies() {
        operations.fetchAndStorePolicies(tracks).subscribe();

        verify(storePoliciesCommand).toObservable();
        expect(storePoliciesCommand.getInput()).toContainExactly(policyInfo);
    }

}
