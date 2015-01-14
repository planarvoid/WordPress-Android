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
    public void enqueueTracksAfterPlayableChangedFromLikeEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        verify(operations).updateOfflineLikes();
    }

    @Test
    public void doesNotEnqueueTracksWhenOfflineSyncOfLikesIsDisabled() {
        when(operations.isLikesOfflineSyncEnabled()).thenReturn(false);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        verify(operations, never()).updateOfflineLikes();

    }

    @Test
    public void doesNotEnqueueTrackAfterPlayableChangedFromRepostEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createIgnoredEvent());

        verify(operations, never()).updateOfflineLikes();
    }

    @Test
    public void updatesOfflineLikesWhenOfflineLikesEnabled() {
        controller.subscribe();
        offlineLikesSyncObservable.onNext(true);

        verify(operations).updateOfflineLikes();
    }

    @Test
    public void updatesOfflineLikesWhenOfflineLikesDisabled() {
        controller.subscribe();
        offlineLikesSyncObservable.onNext(false);

        verify(operations).updateOfflineLikes();
    }

    @Test
    public void stopsListeningToLikedTracksChangesWhenOfflineLikesNotEnabled() {
        when(operations.isLikesOfflineSyncEnabled()).thenReturn(false);
        controller.subscribe();

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

    @Test
    public void ignoreLikesUpdateWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();
        eventBus.publish(EventQueue.PLAYABLE_CHANGED, createLikeEvent());

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }

    @Test
    public void ignoreSettingsUpdateWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();
        offlineLikesSyncObservable.onNext(true);

        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }

    private PlayableUpdatedEvent createIgnoredEvent() {
        return PlayableUpdatedEvent.forRepost(TRACK_URN, true, 10);
    }

    private PlayableUpdatedEvent createLikeEvent() {
        return PlayableUpdatedEvent.forLike(TRACK_URN, true, 10);
    }
}
