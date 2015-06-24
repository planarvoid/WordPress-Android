package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class FetchPlaylistWithTracksCommandTest {

    private static final Urn URN = Urn.forPlaylist(123L);

    private FetchPlaylistWithTracksCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchPlaylistWithTracksCommand(apiClient);
    }

    @Test
    public void shouldResolveUrnsToFullTracksViaApiMobile() throws Exception {
        ApiPlaylistWithTracks playlistWithTracks = ModelFixtures.apiPlaylistWithNoTracks();

        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.PLAYLIST_WITH_TRACKS.path(URN))), isA(TypeToken.class)))
                .thenReturn(playlistWithTracks);

        expect(command.with(URN).call()).toBe(playlistWithTracks);
    }


}