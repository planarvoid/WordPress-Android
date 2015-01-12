package com.soundcloud.android.sync.commands;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.ApiTrackCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class FetchTracksCommandTest {

    private FetchTracksCommand command;

    @Mock private ApiClient apiClient;

    @Before
    public void setup() {
        command = new FetchTracksCommand(apiClient);
    }

    @Test
    public void shouldResolveUrnsToFullTracksViaApiMobile() throws Exception {
        Map body = new HashMap();
        body.put("urns", Arrays.asList("soundcloud:tracks:1", "soundcloud:tracks:2"));
        ApiTrackCollection tracks = new ApiTrackCollection();
        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("POST", ApiEndpoints.TRACKS_FETCH.path()).withContent(body)))).thenReturn(tracks);
        ApiTrackCollection result = command.with(Arrays.asList(Urn.forTrack(1), Urn.forTrack(2))).call();
        expect(result).toBe(tracks);
    }

}