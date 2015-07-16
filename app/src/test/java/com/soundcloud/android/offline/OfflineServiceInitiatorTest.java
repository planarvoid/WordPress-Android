package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.content.Intent;

public class OfflineServiceInitiatorTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaylistWithTracks playlistWithTracks;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private Context context;

    private OfflineServiceInitiator controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineLikeToggle;
    private PublishSubject<Boolean> wifiOnlyToggleSetting;


    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikeToggle = PublishSubject.create();
        wifiOnlyToggleSetting = PublishSubject.create();

        when(settingsStorage.getWifiOnlyOfflineSyncStateChange()).thenReturn(wifiOnlyToggleSetting);
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(offlineLikeToggle);
        when(playlistOperations.playlist(PLAYLIST)).thenReturn(Observable.just(playlistWithTracks));

        controller = new OfflineServiceInitiator(context, eventBus, settingsStorage,
                playlistOperations, offlineContentOperations, Schedulers.immediate());

        controller.subscribe();
        reset(context); // required as we start on subscribe
    }

    @Test
    public void startsServiceWhenSubscribes() {
        controller.unsubscribe();
        reset(context);

        controller.subscribe();

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void stopServiceWhenUnsubscribed() {
        controller.unsubscribe();

        assertThat(wasServiceStopped()).isTrue();
    }

    @Test
    public void startsServiceWhenOfflineLikeToggleEnabled() {
        offlineLikeToggle.onNext(true);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOffline() {
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsUnavailableOffline() {
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, false));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikesChangedButOfflineLikesAreDisabled() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(false));

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, true, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, false, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.unsubscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLikesMarkedForOffline(true));

        assertThat(wasServiceStarted()).isFalse();
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncSettingWasDisabled() {
        wifiOnlyToggleSetting.onNext(false);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenWifiOnlySyncWasEnabled() {
        wifiOnlyToggleSetting.onNext(true);

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void syncAndLoadPlaylistOnPlaylistIsMarkedForOfflineEvent() {
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        verify(playlistOperations).playlist(PLAYLIST);
    }

    @Test
    public void startOfflineSyncWhenPlaylistMarkedAsOfflineSyncedAndChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenPlaylistMarkedAsOfflineSyncedButNoChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, false, PLAYLIST));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void doesNotStartOfflineSyncOnSyncResultEventForPlaylistNotMarkedAsOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        verify(context, never()).startService(any(Intent.class));
    }

    private Intent captureStartServiceIntent() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context).startService(captor.capture());

        return captor.getValue();
    }

    private boolean wasServiceStarted() {
        final Intent intent = captureStartServiceIntent();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_START) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }

    private boolean wasServiceStopped() {
        final Intent intent = captureStartServiceIntent();
        return intent != null &&
                intent.getAction().equals(OfflineContentService.ACTION_STOP) &&
                intent.getComponent().getClassName().equals(OfflineContentService.class.getCanonicalName());
    }
}
