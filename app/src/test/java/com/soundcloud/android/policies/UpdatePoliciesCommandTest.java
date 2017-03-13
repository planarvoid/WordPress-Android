package com.soundcloud.android.policies;

import static com.soundcloud.android.policies.UpdatePoliciesCommand.BATCH_SIZE;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
import com.soundcloud.android.utils.Sleeper;
import com.soundcloud.android.utils.TryWithBackOff;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpdatePoliciesCommandTest extends AndroidUnitTest {

    private UpdatePoliciesCommand command;

    private final Urn trackUrn = Urn.forTrack(123L);
    private final List<String> body = singletonList(trackUrn.toString());
    private final ApiPolicyInfo policy = ModelFixtures.apiPolicyInfo(trackUrn);
    private final List<ApiPolicyInfo> policyCollection = singletonList(policy);
    private final ModelCollection<ApiPolicyInfo> policiesResponse = new ModelCollection<>(policyCollection);

    @Mock private ApiClient apiClient;
    @Mock private StorePoliciesCommand storePoliciesCommand;
    @Mock private Sleeper sleeper;

    @Before
    public void setUp() {
        final TryWithBackOff.Factory factory = new TryWithBackOff.Factory(sleeper);
        this.command = new UpdatePoliciesCommand(apiClient, storePoliciesCommand,
                                                 factory.create(0, TimeUnit.SECONDS, 0, 1));
    }

    @Test
    public void shouldRequestAndStorePoliciesInBatchesOf300() throws Exception {
        ModelCollection<ApiPolicyInfo> batch1Response = new ModelCollection<>(nCopies(BATCH_SIZE, policy));
        ModelCollection<ApiPolicyInfo> batch2Response = new ModelCollection<>(nCopies(20, policy));
        Collection<ApiPolicyInfo> expectedResult = expectBatchFetchWith(batch1Response, batch2Response);
        when(storePoliciesCommand.call(batch1Response)).thenReturn(TestStorageResults.successfulTransaction());
        when(storePoliciesCommand.call(batch2Response)).thenReturn(TestStorageResults.successfulTransaction());

        Collection<ApiPolicyInfo> result = command.call(nCopies(BATCH_SIZE + 20, trackUrn));

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test(expected = PolicyUpdateFailure.class)
    public void shouldEscalateFetchFailures() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), any(TypeToken.class)))
                .thenThrow(new IOException());

        command.call(singleton(trackUrn));
    }

    @Test(expected = PolicyUpdateFailure.class)
    public void shouldEscalateInsertFailures() throws Exception {
        whenSingleBatchFetched();
        when(storePoliciesCommand.call(policiesResponse)).thenReturn(TestStorageResults.failedTransaction());

        command.call(singleton(trackUrn));
    }

    private void whenSingleBatchFetched() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.POLICIES.path()).withContent(body)), any(TypeToken.class)))
                .thenReturn(policiesResponse);
    }

    private Collection<ApiPolicyInfo> expectBatchFetchWith(ModelCollection<ApiPolicyInfo> batch1Response,
                                                           ModelCollection<ApiPolicyInfo> batch2Response)
            throws Exception {
        List<String> batch1Input = nCopies(BATCH_SIZE, trackUrn.toString());
        List<String> batch2Input = nCopies(20, trackUrn.toString());
        Collection<ApiPolicyInfo> expectedResult = new ArrayList<>();
        expectedResult.addAll(batch1Response.getCollection());
        expectedResult.addAll(batch2Response.getCollection());
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.POLICIES.path()).withContent(batch1Input)), any(TypeToken.class)))
                .thenReturn(batch1Response);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.POLICIES.path()).withContent(batch2Input)), any(TypeToken.class)))
                .thenReturn(batch2Response);
        return expectedResult;
    }
}
