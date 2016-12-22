package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PushPlaylistRemovalsCommandTest extends AndroidUnitTest {
    private PushPlaylistRemovalsCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new PushPlaylistRemovalsCommand(apiClient);
    }

    @Test
    public void removesPlaylistTracksAndReturnsUrns() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn track1 = Urn.forTrack(1);
        final Urn track2 = Urn.forTrack(2);
        final List<Urn> input = Arrays.asList(track1, track2);

        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(TestApiResponses.ok());

        Collection<Urn> result = command.with(playlistUrn).with(input).call();

        verify(apiClient).fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.PLAYLIST_REMOVE_TRACK.path(playlistUrn, track1))));
        verify(apiClient).fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.PLAYLIST_REMOVE_TRACK.path(playlistUrn, track2))));
        assertThat(result).isSameAs(input);
    }

    @Test(expected = ApiRequestException.class)
    public void throwsExceptionOnNetworkError() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn track1 = Urn.forTrack(1);
        final List<Urn> input = singletonList(track1);

        when(apiClient.fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.PLAYLIST_REMOVE_TRACK.path(playlistUrn, track1))))).thenReturn(
                TestApiResponses.networkError());

        command.with(playlistUrn).with(input).call();
    }

    @Test(expected = ApiRequestException.class)
    public void throwsExceptionOnServerError() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn track1 = Urn.forTrack(1);
        final List<Urn> input = singletonList(track1);

        when(apiClient.fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.PLAYLIST_REMOVE_TRACK.path(playlistUrn, track1))))).thenReturn(
                TestApiResponses.status(502));

        command.with(playlistUrn).with(input).call();
    }
}
