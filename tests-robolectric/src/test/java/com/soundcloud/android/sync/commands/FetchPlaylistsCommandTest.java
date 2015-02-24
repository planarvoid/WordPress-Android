package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class FetchPlaylistsCommandTest {

    private FetchPlaylistsCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchPlaylistsCommand(apiClient);
    }

    @Test
    public void shouldResolveUrnsToFullTracksViaApiMobile() throws Exception {
        Map body = new HashMap();
        body.put("urns", Arrays.asList("soundcloud:playlists:1", "soundcloud:playlists:2"));
        ModelCollection<ApiPlaylist> tracks = new ModelCollection<>();
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_FETCH.path()).withContent(body)))).thenReturn(tracks);
        ModelCollection<ApiPlaylist> result = command.with(Arrays.asList(Urn.forPlaylist(1), Urn.forPlaylist(2))).call();
        expect(result).toBe(tracks);
    }

}