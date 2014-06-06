package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackServiceOperationsTest {

    private PlaybackServiceOperations playbackServiceOperations;
    private Track track;

    @Mock
    private HttpProperties httpProperties;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private RxHttpClient rxHttpClient;
    @Mock
    private Observer observer;
    @Mock
    private Token token;

    @Before
    public void setUp() throws Exception {
        playbackServiceOperations = new PlaybackServiceOperations(accountOperations, httpProperties, rxHttpClient);
        track = TestHelper.getModelFactory().createModel(Track.class);

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenBuilding(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        playbackServiceOperations.buildHLSUrlForTrack(track);
    }

    @Test
    public void shouldBuildHLSUrlForTrackBasedOnTrackURN() {
        Track mockTrack = mock(Track.class);
        when(mockTrack.getUrn()).thenReturn(Urn.forTrack(123));
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        token.access = "access";
        when(httpProperties.getPrivateApiHostWithHttpScheme()).thenReturn("https://somehost/path");

        expect(playbackServiceOperations
                .buildHLSUrlForTrack(mockTrack))
                .toEqual("https://somehost/path/tracks/soundcloud:sounds:123/streams/hls?oauth_token=access");
    }

    @Test
    public void logPlaycountCallsOnNextWithTrackUrnOnExpectedResponse() throws Exception {
        final Track track = new Track(1L);
        APIResponse response = mock(APIResponse.class);

        when(rxHttpClient.fetchResponse(argThat(isMobileApiRequestTo("POST", "/tracks/soundcloud%3Asounds%3A1/plays")
                .withQueryParam("client_id", "12345")))).thenReturn(Observable.just(response));
        when(httpProperties.getClientId()).thenReturn("12345");
        when(response.getStatusCode()).thenReturn(302);

        playbackServiceOperations.logPlay(track.getUrn()).subscribe(observer);
        verify(observer).onNext(track.getUrn());

    }

}