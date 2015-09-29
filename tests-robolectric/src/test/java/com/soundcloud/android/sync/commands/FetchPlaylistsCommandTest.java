package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class FetchPlaylistsCommandTest {

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
        expect(result).toEqual(playlists);
    }

    @Test
    public void shouldResolveUrnsToFullPlaylistsViaApiMobileInPages() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 3);
        final List<Urn> urns = Arrays.asList(playlists.get(0).getUrn(), playlists.get(1).getUrn(), playlists.get(2).getUrn());

        setupRequest(urns.subList(0, 2), playlists.subList(0, 2));
        setupRequest(urns.subList(2, 3), playlists.subList(2, 3));

        Collection<ApiPlaylist> result = command.with(urns).call();
        expect(result).toEqual(playlists);
    }

    private void setupRequest(List<Urn> urns, List<ApiPlaylist> playlists) throws Exception {
        Map body = new HashMap();
        body.put("urns", Urns.toString(urns));

        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_FETCH.path()).withContent(body)), isA(TypeToken.class)))
                .thenReturn(new ModelCollection<>(playlists));
    }

}
