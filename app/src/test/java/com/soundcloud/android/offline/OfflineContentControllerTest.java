package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.propeller.TxnResult;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentControllerTest {

    @Mock private TrackDownloadsStorage storage;
    @Mock private OfflineContentOperations operations;

    private EventBus eventBus;
    private OfflineContentController controller;
    private final Urn TRACK_URN = Urn.forTrack(123L);

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        controller = new OfflineContentController(eventBus, operations, Robolectric.application);
    }

    @Test
    public void enqueueTracksAfterPlayableChangedFromLikeEvent() {
        final PlayableUpdatedEvent event = PlayableUpdatedEvent.forLike(TRACK_URN, true, 10);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);

        verify(operations).updateOfflineLikes();
    }

    @Test
    public void doesNotEnqueueTrackAfterPlayableChangedFromRepostEvent() {
        final PlayableUpdatedEvent event = PlayableUpdatedEvent.forRepost(TRACK_URN, true, 10);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);

        verify(operations, never()).updateOfflineLikes();
    }

    @Test
    public void likeUpdateStarsOfflineContentService() {
        when(operations.updateOfflineLikes()).thenReturn(Observable.just(new TxnResult()));
        final PlayableUpdatedEvent event = PlayableUpdatedEvent.forLike(TRACK_URN, true, 10);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual("action_download_tracks");
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }
}
