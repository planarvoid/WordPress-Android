package com.soundcloud.android.policies;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.List;

public class FetchPoliciesCommandTest extends AndroidUnitTest {

    private FetchPoliciesCommand command;

    private final List<String> body = singletonList("soundcloud:playlists:1");
    private final PolicyInfo policy = new PolicyInfo("soundcloud:playlists:1", true, "Allow", false);
    private final ModelCollection<PolicyInfo> policies = new ModelCollection<>(singletonList(policy));

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() {
        this.command = new FetchPoliciesCommand(apiClient);
    }

    @Test
    public void fetchesPoliciesForGivenTracks() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.POLICIES.path()).withContent(body)), any(TypeToken.class)))
                .thenReturn(policies);

        Collection<PolicyInfo> result = command.with(singletonList(Urn.forPlaylist(1))).call();

        assertThat(result).contains(policy);
    }
}
