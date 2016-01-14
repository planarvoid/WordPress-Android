package com.soundcloud.android.offline;

import static com.soundcloud.android.playlists.PlaylistWithTracksTests.createPlaylistWithTracks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OfflineContentControllerTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);
    private static final Void SIGNAL = null;

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private PlaylistWithTracks playlistWithTracks;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private CollectionOperations collectionOperations;
    @Mock private Context context;

    private OfflineContentController controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> offlineLikeToggle;
    private PublishSubject<Boolean> wifiOnlyToggleSetting;
    private PublishSubject<Void> onCollectionChanged;
    private PublishSubject<Boolean> offlineCollectionStateChanges;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        offlineLikeToggle = PublishSubject.create();
        wifiOnlyToggleSetting = PublishSubject.create();
        onCollectionChanged = PublishSubject.create();
        offlineCollectionStateChanges = PublishSubject.create();

        when(settingsStorage.getWifiOnlyOfflineSyncStateChange()).thenReturn(wifiOnlyToggleSetting);
        when(offlineContentOperations.getOfflineLikedTracksStatusChanges()).thenReturn(offlineLikeToggle);
        when(playlistOperations.playlists(Collections.singleton(PLAYLIST))).thenReturn(Observable.just(Collections.singletonList(playlistWithTracks)));
        when(collectionOperations.onCollectionChanged()).thenReturn(onCollectionChanged);
        when(collectionOperations.myPlaylists()).thenReturn(Observable.<List<PlaylistItem>>empty());
        when(offlineContentOperations.getOfflineCollectionStateChanges()).thenReturn(offlineCollectionStateChanges);
        when(offlineContentOperations.setOfflinePlaylists(anyList())).thenReturn(Observable.empty());
        controller = new OfflineContentController(context, eventBus, settingsStorage,
                playlistOperations, offlineContentOperations, collectionOperations, Schedulers.immediate());

    }

    @Test
    public void startsServiceWhenSubscribes() {
        controller.subscribe();

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void stopServiceWhenUnsubscribed() {
        subscribe();

        controller.unsubscribe();

        assertThat(wasServiceStopped()).isTrue();
    }

    @Test
    public void startsServiceWhenOfflineLikeToggleEnabled() {
        subscribe();

        offlineLikeToggle.onNext(true);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOffline() {
        subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsUnavailableOffline() {
        subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, false));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikesChangedButOfflineLikesAreDisabled() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(false));

        subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, true, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, false, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        subscribe();

        controller.unsubscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLikesMarkedForOffline(true));
        onCollectionChanged.onNext(SIGNAL);

        assertThat(wasServiceStarted()).isFalse();
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncSettingWasDisabled() {
        subscribe();

        wifiOnlyToggleSetting.onNext(false);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenWifiOnlySyncWasEnabled() {
        subscribe();

        wifiOnlyToggleSetting.onNext(true);

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void syncAndLoadPlaylistOnPlaylistIsMarkedForOfflineEvent() {
        subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));

        verify(playlistOperations).playlists(Collections.singleton(PLAYLIST));
    }

    @Test
    public void startOfflineSyncWhenPlaylistMarkedAsOfflineSyncedAndChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void doesNotStartOfflineSyncWhenPlaylistMarkedAsOfflineSyncedButNoChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, false, PLAYLIST));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void doesNotStartOfflineSyncOnSyncResultEventForPlaylistNotMarkedAsOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        verify(context, never()).startService(any(Intent.class));
    }

    @Test
    public void startOfflineSyncOnPolicyUpdateEvent() {
        subscribe();

        eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.success(Collections.singletonList(Urn.forTrack(123L))));

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void markPlaylistOfflineWhenPlaylistCreated() {
        final List<Urn> playlistsCollection = Arrays.asList(Urn.forPlaylist(123L), Urn.forPlaylist(456L));

        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        setPlaylistsCollection(playlistsCollection);

        subscribe();
        onCollectionChanged.onNext(SIGNAL);

        verify(offlineContentOperations).setOfflinePlaylists(playlistsCollection);
    }

    @Test
    public void nopOpWhenPlaylistCreatedButOfflineCollectionDisabled() {
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(false);

        subscribe();
        onCollectionChanged.onNext(SIGNAL);

        verify(offlineContentOperations, never()).setOfflinePlaylists(anyList());
    }

    @Test
    public void startOfflineServiceWhenCollectionStateChangedIsTrue() {
        setPlaylistsCollection(Urn.forPlaylist(123L));

        subscribe();
        offlineCollectionStateChanges.onNext(true);

        assertThat(wasServiceStarted()).isTrue();
    }

    @Test
    public void ignoreOfflineCollectionStateChangedWhenFalse() {
        setPlaylistsCollection(Urn.forPlaylist(123L));

        subscribe();
        offlineCollectionStateChanges.onNext(false);

        verify(context, never()).startService(any(Intent.class));
    }

    private void subscribe() {
        controller.subscribe();
        reset(context);
    }

    private void setPlaylistsCollection(Urn... urns) {
        setPlaylistsCollection(Arrays.asList(urns));
    }

    private void setPlaylistsCollection(List<Urn> playlists) {
        final List<PlaylistItem> playlistsItemCollection = new ArrayList<>();
        for (Urn urn : playlists) {
            playlistsItemCollection.add(createPlaylistItem(urn));
        }
        when(collectionOperations.myPlaylists()).thenReturn(Observable.just(playlistsItemCollection));

        final List<PlaylistWithTracks> playlistsWithTracksCollection = new ArrayList<>();
        for (Urn urn : playlists) {
            playlistsWithTracksCollection.add(createPlaylistWithTracks(urn));
        }
        when(playlistOperations.playlists(playlists)).thenReturn(Observable.just(playlistsWithTracksCollection));


        when(offlineContentOperations.setOfflinePlaylists(playlists)).thenReturn(Observable.<Void>just(null));
    }

    private PlaylistItem createPlaylistItem(Urn urn) {
        return PlaylistItem.from(PropertySet.from(EntityProperty.URN.bind(urn)));
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
