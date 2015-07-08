package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
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

public class OfflineContentControllerTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private OfflinePlaylistStorage playlistStorage;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaylistWithTracks playlistWithTracks;
    @Mock private Context context;

    private OfflineContentController controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineLikeToggleSetting;
    private PublishSubject<Boolean> wifiOnlyToggleSetting;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikeToggleSetting = PublishSubject.create();
        wifiOnlyToggleSetting = PublishSubject.create();

        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(settingsStorage.getWifiOnlyOfflineSyncStateChange()).thenReturn(wifiOnlyToggleSetting);
        when(settingsStorage.getOfflineLikedTracksStatusChange()).thenReturn(offlineLikeToggleSetting);
        when(playlistOperations.playlist(PLAYLIST)).thenReturn(Observable.just(playlistWithTracks));

        controller = new OfflineContentController(context, eventBus, settingsStorage, playlistStorage, playlistOperations, Schedulers.immediate());
    }

    @Test
    public void stopServiceWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();

        assertThat(wasServiceStopped()).isTrue();
    }

    @Test
    public void startsServiceWhenOfflineLikeToggleEnabled() {
        controller.subscribe();

        offlineLikeToggleSetting.onNext(true);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsUnavailableOffline() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, false));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTheFeatureIsDisabled() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, true, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, false, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.subscribe();
        controller.unsubscribe();

        offlineLikeToggleSetting.onNext(true);

        assertThat(wasServiceStarted()).isFalse();
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncSettingWasDisabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(false);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenWifiOnlySyncWasEnabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(true);

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void syncAndLoadPlaylistOnPlaylistIsMarkedForOfflineEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        verify(playlistOperations).playlist(PLAYLIST);
    }

    @Test
    public void startOfflineSyncWhenPlaylistMarkedAsOfflineSyncedAndChanged() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenPlaylistMarkedAsOfflineSyncedButNoChanged() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, false, PLAYLIST));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void doesNotStartOfflineSyncOnSyncResultEventForPlaylistNotMarkedAsOffline() {
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));
        controller.subscribe();

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
