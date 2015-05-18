package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentControllerTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private OfflinePlaylistStorage playlistStorage;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaylistWithTracks playlistWithTracks;

    private OfflineContentController controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineLikeToggleSetting;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikeToggleSetting = PublishSubject.create();

        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(settingsStorage.getOfflineLikedTracksStatusChange()).thenReturn(offlineLikeToggleSetting);
        when(playlistOperations.playlist(PLAYLIST)).thenReturn(Observable.just(playlistWithTracks));

        controller = new OfflineContentController(Robolectric.application, eventBus, settingsStorage, playlistStorage, playlistOperations, Schedulers.immediate());
    }

    @Test
    public void stopServiceWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();

        expect(wasServiceStopped()).toBeTrue();
    }

    @Test
    public void startsServiceWhenOfflineLikeToggleEnabled() {
        controller.subscribe();

        offlineLikeToggleSetting.onNext(true);

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsUnavailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, false));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTheFeatureIsDisabled() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expectNoInteractionWithService();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, true, 1));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, false, 1));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        expectNoInteractionWithService();
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.subscribe();
        controller.unsubscribe();

        offlineLikeToggleSetting.onNext(true);

        expect(wasServiceStarted()).toBeFalse();
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        expect(wasServiceStarted()).toBeTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        expect(wasServiceStarted()).toBeFalse();
    }

    @Test
    public void syncAndLoadPlaylistOnPlaylistIsMarkedForOfflineEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        verify(playlistOperations).playlist(PLAYLIST);
    }

    private void expectNoInteractionWithService() {
        final Intent startService = Robolectric.getShadowApplication().peekNextStartedService();
        expect(startService).toBeNull();
    }

    private boolean wasServiceStarted() {
        final Intent intent = Robolectric.getShadowApplication().peekNextStartedService();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_START) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }

    private boolean wasServiceStopped() {
        final Intent intent = Robolectric.getShadowApplication().peekNextStartedService();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_STOP) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }
}
