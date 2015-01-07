package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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
import rx.subjects.PublishSubject;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentControllerTest {

    @Mock private TrackDownloadsStorage storage;
    @Mock private OfflineContentOperations operations;

    private OfflineContentController controller;

    private EventBus eventBus;
    private final Urn TRACK_URN = Urn.forTrack(123L);
    private PublishSubject<Boolean> offlineLikesSyncObservable;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikesSyncObservable = PublishSubject.create();
        when(operations.isLikesOfflineSyncEnabled()).thenReturn(true);
        when(operations.getSettingsStatus()).thenReturn(offlineLikesSyncObservable.asObservable());
        when(operations.updateOfflineLikes()).thenReturn(Observable.just(new TxnResult()));
        controller = new OfflineContentController(eventBus, operations, Robolectric.application);
    }

    @Test
    public void enqueueTracksWhenOfflineLikesEnabled() {
        controller.subscribe();

        verify(operations).updateOfflineLikes();
    }

    @Test
    public void doesNotEnqueueTracksWhenOfflineLikesEnabled() {
        when(operations.isLikesOfflineSyncEnabled()).thenReturn(false);

        controller.subscribe();

        verify(operations).updateOfflineLikes();
    }

    @Test
    public void enqueueTracksAfterPlayableChangedFromLikeEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        verifyUpdateAfterSubscription();
    }

    @Test
    public void doesNotEnqueueTracksWhenOfflineSyncOfLikesIsDisabled() {
        when(operations.isLikesOfflineSyncEnabled()).thenReturn(false);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        verifyNeverUpdateAfterSubscription();
    }

    @Test
    public void doesNotEnqueueTrackAfterPlayableChangedFromRepostEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createIgnoredEvent());

        verifyNeverUpdateAfterSubscription();
    }

    @Test
    public void stopsListeningToLikedTracksChangesWhenOfflineLikesNotEnabled() {
        controller.subscribe();
        offlineLikesSyncObservable.onNext(false);
        reset(operations);

        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        verify(operations, never()).updateOfflineLikes();
    }

    @Test
    public void likeUpdateStarsOfflineContentService() {
        when(operations.updateOfflineLikes()).thenReturn(Observable.just(new TxnResult()));

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService.getAction()).toEqual("action_download_tracks");
        expect(startService.getComponent().getClassName()).toEqual(OfflineContentService.class.getCanonicalName());
    }

    private Observable<TxnResult> verifyUpdateAfterSubscription() {
        return verify(operations, times(2)).updateOfflineLikes();
    }

    private Observable<TxnResult> verifyNeverUpdateAfterSubscription() {
        return verify(operations, times(1)).updateOfflineLikes();
    }

    private PlayableUpdatedEvent createIgnoredEvent() {
        return PlayableUpdatedEvent.forRepost(TRACK_URN, true, 10);
    }

    private PlayableUpdatedEvent createLikeEvent() {
        return PlayableUpdatedEvent.forLike(TRACK_URN, true, 10);
    }
}
