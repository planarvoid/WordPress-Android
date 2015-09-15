package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class PlayPublisherTest extends AndroidUnitTest {

    public static final Urn TRACK_URN = Urn.forTrack(123);
    private PlayPublisher playPublisher;

    @Mock private GcmStorage gcmStorage;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private ApiClientRx apiClient;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        playPublisher = new PlayPublisher(gcmStorage, dateProvider, eventBus, Schedulers.immediate(), apiClient);
        playPublisher.subscribe();

        when(gcmStorage.getToken()).thenReturn("token");
        when(dateProvider.getTime()).thenReturn(123L);
    }

    @Test
    public void playEventCausesPlayPublishApiRequest() {
        final PublishSubject<ApiResponse> apiResponseSubject = PublishSubject.create();
        when(apiClient.response(argThat(isPublicApiRequestTo("POST", "/tpub")
                .withContent(new PlayPublisher.Payload("token", 123L, TRACK_URN))))).thenReturn(apiResponseSubject);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN));

        assertThat(apiResponseSubject.hasObservers()).isTrue();
    }

    @Test
    public void bufferingEventDoesNotCausePlayPublishApiRequest() {
        final PublishSubject<ApiResponse> apiResponseSubject = PublishSubject.create();
        when(apiClient.response(any(ApiRequest.class))).thenReturn(apiResponseSubject);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, TRACK_URN));

        assertThat(apiResponseSubject.hasObservers()).isFalse();
    }

    @Test
    public void idleEventDoesNotCausePlayPublishApiRequest() {
        final PublishSubject<ApiResponse> apiResponseSubject = PublishSubject.create();
        when(apiClient.response(any(ApiRequest.class))).thenReturn(apiResponseSubject);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, TRACK_URN));

        assertThat(apiResponseSubject.hasObservers()).isFalse();
    }
}
