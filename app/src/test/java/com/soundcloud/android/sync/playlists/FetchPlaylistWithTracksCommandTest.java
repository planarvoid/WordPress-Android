package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FetchPlaylistWithTracksCommandTest extends AndroidUnitTest {

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

        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.PLAYLIST_WITH_TRACKS.path(URN))), isA(TypeToken.class)))
                .thenReturn(playlistWithTracks);

        assertThat(command.with(URN).call()).isSameAs(playlistWithTracks);
    }
}
