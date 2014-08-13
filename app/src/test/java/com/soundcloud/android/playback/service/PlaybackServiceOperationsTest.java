package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackServiceOperationsTest {

    private PlaybackServiceOperations playbackServiceOperations;
    private PublicApiTrack track;

    @Mock
    private HttpProperties httpProperties;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private Token token;

    @Before
    public void setUp() throws Exception {
        playbackServiceOperations = new PlaybackServiceOperations(accountOperations, httpProperties);
        track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenBuilding(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        playbackServiceOperations.buildHLSUrlForTrack(Urn.forTrack(123L));
    }

    @Test
    public void shouldBuildHLSUrlForTrackBasedOnTrackURN() {
        PublicApiTrack mockTrack = mock(PublicApiTrack.class);
        when(mockTrack.getUrn()).thenReturn(Urn.forTrack(123));
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        token.access = "access";
        when(httpProperties.getPrivateApiHostWithHttpScheme()).thenReturn("https://somehost/path");

        expect(playbackServiceOperations
                .buildHLSUrlForTrack(Urn.forTrack(123L)))
                .toEqual("https://somehost/path/tracks/soundcloud:sounds:123/streams/hls?oauth_token=access");
    }
}