package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
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

public class FetchPlaylistsCommandTest extends AndroidUnitTest {

    private FetchPlaylistsCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchPlaylistsCommand(apiClient, 2);
    }

    @Test
    public void shouldResolveUrnsToFullPlaylistsViaApiMobile() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        final List<Urn> urns = Arrays.asList(playlists.get(0).getUrn(), playlists.get(1).getUrn());

        setupRequest(urns, playlists);

        Collection<ApiPlaylist> result = command.with(urns).call();
        assertThat(result).isEqualTo(playlists);
    }

    @Test
    public void shouldResolveUrnsToFullPlaylistsViaApiMobileInPages() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 3);
        final List<Urn> urns = Arrays.asList(playlists.get(0).getUrn(),
                                             playlists.get(1).getUrn(),
                                             playlists.get(2).getUrn());

        setupRequest(urns.subList(0, 2), playlists.subList(0, 2));
        setupRequest(urns.subList(2, 3), playlists.subList(2, 3));

        Collection<ApiPlaylist> result = command.with(urns).call();
        assertThat(result).isEqualTo(playlists);
    }

    @Test
    public void shouldIgnoreUrnsWithNegativeId() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 1);
        final List<Urn> urns = Collections.singletonList(playlists.get(0).getUrn());

        setupRequest(urns, playlists);

        Collection<ApiPlaylist> result = command
                .with(Arrays.asList(playlists.get(0).getUrn(), Urn.forPlaylist(-100)))
                .call();
        assertThat(result).isEqualTo(playlists);
    }

    @Test
    public void shouldAvoidApiCallWhenAllUrnsAreWithNegativeId() throws Exception {
        final List<Urn> urns = Arrays.asList(Urn.forPlaylist(-100), Urn.forPlaylist(-200));

        command.with(urns).call();

        verifyZeroInteractions(apiClient);
    }

    private void setupRequest(List<Urn> urns, List<ApiPlaylist> playlists) throws Exception {
        Map<String, List<String>> body = new HashMap<>();
        body.put("urns", Urns.toString(urns));

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_FETCH.path()).withContent(body)), isA(TypeToken.class)))
                .thenReturn(new ModelCollection<>(playlists));
    }

}
