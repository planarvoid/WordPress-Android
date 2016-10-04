package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchTracksCommandTest extends AndroidUnitTest {

    private FetchTracksCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        command = new FetchTracksCommand(apiClient, 2);
    }

    @Test
    public void shouldResolveUrnsToFullTracksViaApiMobile() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        final List<Urn> urns = Arrays.asList(tracks.get(0).getUrn(), tracks.get(1).getUrn());

        setupRequest(urns, tracks);

        Collection<ApiTrack> result = command.with(urns).call();
        assertThat(result).isEqualTo(tracks);
    }

    @Test
    public void shouldResolveUrnsToFullTracksViaApiMobileInPages() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 3);
        final List<Urn> urns = Arrays.asList(tracks.get(0).getUrn(), tracks.get(1).getUrn(), tracks.get(2).getUrn());

        setupRequest(urns.subList(0, 2), tracks.subList(0, 2));
        setupRequest(urns.subList(2, 3), tracks.subList(2, 3));

        Collection<ApiTrack> result = command.with(urns).call();
        assertThat(result).isEqualTo(tracks);
    }

    @Test
    public void shouldIgnoreUrnsWithNegativeId() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 1);
        final List<Urn> urns = Collections.singletonList(tracks.get(0).getUrn());

        setupRequest(urns, tracks);

        Collection<ApiTrack> result = command
                .with(Arrays.asList(tracks.get(0).getUrn(), Urn.forTrack(-100)))
                .call();
        assertThat(result).isEqualTo(tracks);
    }

    @Test
    public void shouldAvoidApiCallWhenAllUrnsAreWithNegativeId() throws Exception {
        final List<Urn> urns = Arrays.asList(Urn.forTrack(-100), Urn.forTrack(-200));

        command.with(urns).call();

        verifyZeroInteractions(apiClient);
    }

    private void setupRequest(List<Urn> urns, List<ApiTrack> tracks) throws Exception {
        Map<String, List<String>> body = new HashMap<>();
        body.put("urns", Urns.toString(urns));

        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.TRACKS_FETCH.path()).withContent(body)), isA(TypeToken.class)))
                .thenReturn(new ModelCollection<>(tracks));
    }
}
