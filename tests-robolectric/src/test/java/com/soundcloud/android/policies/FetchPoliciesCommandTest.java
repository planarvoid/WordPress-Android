package com.soundcloud.android.policies;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class FetchPoliciesCommandTest {

    private FetchPoliciesCommand command;

    private final List<String> body = Arrays.asList("soundcloud:playlists:1");
    private final PolicyInfo policy = new PolicyInfo("soundcloud:playlists:1", true, "Allow", false);
    private final ModelCollection<PolicyInfo> policies = new ModelCollection<>(Arrays.asList(policy));

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() {
        this.command = new FetchPoliciesCommand(apiClient);
    }

    @Test
    public void fetchesPoliciesForGivenTracks() throws Exception {
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.POLICIES.path()).withContent(body)))).thenReturn(policies);

        List<PolicyInfo> result = command.with(Arrays.asList(Urn.forPlaylist(1))).call();

        expect(result).toContainExactly(policy);
    }
}
