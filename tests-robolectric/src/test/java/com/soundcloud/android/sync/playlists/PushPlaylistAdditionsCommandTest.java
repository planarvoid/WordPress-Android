package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.api.TestApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class PushPlaylistAdditionsCommandTest {

    private PushPlaylistAdditionsCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new PushPlaylistAdditionsCommand(apiClient);
    }

    @Test
    public void pushesNewPlaylistTracksAndReturnsSuccess() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn track1 = Urn.forTrack(1);
        final Urn track2 = Urn.forTrack(2);
        final List<Urn> input = Arrays.asList(track1, track2);

        when(apiClient.fetchResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.PLAYLIST_ADD_TRACK.path(playlistUrn)).withContent(getBodyFor(track1))))).thenReturn(TestApiResponses.ok());
        when(apiClient.fetchResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.PLAYLIST_ADD_TRACK.path(playlistUrn)).withContent(getBodyFor(track2))))).thenReturn(TestApiResponses.status(404));

        Collection<Urn> result = command.with(playlistUrn).with(input).call();

        expect(result).toContainExactly(track1);
    }

    @Test(expected = ApiRequestException.class)
    public void throwsResponseExceptionOnNetworkError() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn track1 = Urn.forTrack(1);
        final List<Urn> input = Arrays.asList(track1);

        when(apiClient.fetchResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.PLAYLIST_ADD_TRACK.path(playlistUrn)).withContent(getBodyFor(track1))))).thenReturn(TestApiResponses.networkError());

        command.with(playlistUrn).with(input).call();
    }

    @Test(expected = ApiRequestException.class)
    public void throwsResponseExceptionOnServerError() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(1);
        final Urn track1 = Urn.forTrack(1);
        final List<Urn> input = Arrays.asList(track1);

        when(apiClient.fetchResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.PLAYLIST_ADD_TRACK.path(playlistUrn)).withContent(getBodyFor(track1))))).thenReturn(TestApiResponses.status(503));

        command.with(playlistUrn).with(input).call();
    }

    private Map getBodyFor(Urn trackUrn){
        return Collections.singletonMap("track_urn", trackUrn.toString());
    }
}