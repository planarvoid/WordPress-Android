package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayPublisherTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123);

    @SuppressWarnings("FieldCanBeLocal")
    private PlayPublisher playPublisher;

    @Mock private GcmStorage gcmStorage;
    @Mock private ApiClientRxV2 apiClient;

    @Before
    public void setUp() throws Exception {
        playPublisher = new PlayPublisher(resources(),
                                          gcmStorage,
                                          new TestDateProvider(123L),
                                          Schedulers.trampoline(),
                                          apiClient);

        when(gcmStorage.getToken()).thenReturn("token");
    }

    @Test
    public void playEventCausesPlayPublishApiRequest() {
        when(apiClient.response(any(ApiRequest.class))).thenReturn(Single.just(new ApiResponse(null, 200, "body")));

        playPublisher.onPlaybackStateChanged(TestPlayStates.wrap(new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                                             PlayStateReason.NONE,
                                                                                             TRACK_URN, 0, 0)));

        verify(apiClient).response(argThat(isPublicApiRequestTo("POST", "/tpub").withContent(new PlayPublisher.Payload(
                resources().getString(R.string.gcm_gateway_id),
                "token",
                123L,
                TRACK_URN))));
    }
}
