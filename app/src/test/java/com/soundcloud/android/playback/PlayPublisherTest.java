package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.Schedulers;

public class PlayPublisherTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123);

    @SuppressWarnings("FieldCanBeLocal")
    private PlayPublisher playPublisher;

    @Mock private GcmStorage gcmStorage;
    @Mock private ApiClient apiClient;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        playPublisher = new PlayPublisher(resources(),
                                          gcmStorage,
                                          new TestDateProvider(123L),
                                          eventBus,
                                          Schedulers.immediate(),
                                          apiClient);
        playPublisher.subscribe();

        when(gcmStorage.getToken()).thenReturn("token");
    }

    @Test
    public void playEventCausesPlayPublishApiRequest() {
        when(apiClient.fetchResponse(any(ApiRequest.class))).thenReturn(new ApiResponse(null, 200, "body"));

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         TestPlayStates.wrap(new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         TRACK_URN, 0, 0)));

        verify(apiClient).fetchResponse(argThat(isPublicApiRequestTo("POST",
                                                                                     "/tpub").withContent(new PlayPublisher.Payload(
                resources().getString(R.string.gcm_gateway_id),
                "token",
                123L,
                TRACK_URN))));
    }

    @Test
    public void bufferingEventDoesNotCausePlayPublishApiRequest() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         TestPlayStates.wrap(new PlaybackStateTransition(PlaybackState.BUFFERING,
                                                                         PlayStateReason.NONE,
                                                                         TRACK_URN, 0, 0)));

        verify(apiClient, never()).fetchResponse(any(ApiRequest.class));
    }

    @Test
    public void idleEventDoesNotCausePlayPublishApiRequest() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         TestPlayStates.wrap(new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.NONE,
                                                                         TRACK_URN, 0, 0)));

        verify(apiClient, never()).fetchResponse(any(ApiRequest.class));
    }
}
