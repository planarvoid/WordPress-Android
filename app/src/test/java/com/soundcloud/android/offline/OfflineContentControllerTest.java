package com.soundcloud.android.offline;

import static com.soundcloud.android.events.PlaylistEntityChangedEvent.fromPlaylistEdited;
import static com.soundcloud.android.events.PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist;
import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class OfflineContentControllerTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);
    private static final Void SIGNAL = null;

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineServiceInitiator serviceInitiator;

    private OfflineContentController controller;
    private TestEventBusV2 eventBus;
    private io.reactivex.subjects.PublishSubject<Boolean> wifiOnlyToggleSetting;
    private PublishSubject<Void> onCollectionChanged;
    private TestObserver<Object> startServiceObserver;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBusV2();
        wifiOnlyToggleSetting = PublishSubject.create();
        onCollectionChanged = PublishSubject.create();

        startServiceObserver = new TestObserver<>();
        when(serviceInitiator.startObserver()).thenReturn(new TestDefaultObserver());
        when(settingsStorage.getWifiOnlyOfflineSyncStateChange()).thenReturn(wifiOnlyToggleSetting);
        controller = new OfflineContentController(eventBus,
                                                  settingsStorage,
                                                  serviceInitiator,
                                                  offlineContentOperations);
    }

    @Test
    public void startsServiceWhenSubscribes() {
        controller.subscribe();

        verify(serviceInitiator).start();
    }

    @Test
    public void stopServiceWhenUnsubscribed() throws Exception {
        controller.subscribe();

        controller.dispose();

        verify(serviceInitiator).stop();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikesChangedButOfflineLikesAreDisabled() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.TRACK_LIKES.name(), true));

        startServiceObserver.assertNoValues();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Single.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(TRACK, true, 1));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Single.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(TRACK, false, 1));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.TRACK_LIKES.name(), false));

        startServiceObserver.assertNoValues();
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.subscribe();

        controller.dispose();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(Collections.emptyList(), true));
        onCollectionChanged.onNext(SIGNAL);

        startServiceObserver.assertNoValues();
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncSettingWasDisabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(false);

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncWasEnabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(true);

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void restartsOfflineSyncWhenConnectionChanges() {
        controller.subscribe();

        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.OFFLINE);

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOfflineIsEdited() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Single.just(true));
        Playlist editedPlaylist = ModelFixtures.playlistBuilder().urn(PLAYLIST).build();

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, fromPlaylistEdited(editedPlaylist));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Single.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, fromTrackAddedToPlaylist(PLAYLIST, 1));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Single.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYLIST_CHANGED, fromTrackAddedToPlaylist(PLAYLIST, 1));

        startServiceObserver.assertNoValues();
    }

    @Test
    public void startOfflineSyncWhenPlaylistMarkedAsOfflineSyncedAndChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Single.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), true, PLAYLIST));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenPlaylistMarkedAsOfflineSyncedButNoChanged() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), false, PLAYLIST));

        startServiceObserver.assertNoValues();
    }

    @Test
    public void doesNotStartOfflineSyncOnSyncResultEventForPlaylistNotMarkedAsOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Single.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), true, PLAYLIST));

        startServiceObserver.assertNoValues();
    }

    @Test
    public void startOfflineSyncOnPolicyUpdateEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.create(singletonList(Urn.forTrack(123L))));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void removePlaylistFromOfflineContentWhenDeleted() {
        Urn playlist1 = Urn.forPlaylist(123L);
        Urn playlist2 = Urn.forPlaylist(456L);

        PublishSubject<RxSignal> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(Sets.newHashSet(playlist1, playlist2))).thenReturn(
                makePlaylistUnavailableOffline);

        controller.subscribe();
        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntitiesDeleted(Sets.newHashSet(playlist1, playlist2)));

        startServiceObserver.assertNoValues();
        makePlaylistUnavailableOffline.onNext(RxSignal.SIGNAL);
        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void removePlaylistFromOfflineContentWhenUnliked() {
        Urn playlist1 = Urn.forPlaylist(123L);
        Urn playlist2 = Urn.forPlaylist(456L);

        PublishSubject<RxSignal> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(Sets.newHashSet(playlist1, playlist2))).thenReturn(
                makePlaylistUnavailableOffline);

        controller.subscribe();
        Map<Urn, LikesStatusEvent.LikeStatus> likes = new HashMap<>(2);
        likes.put(playlist1, LikesStatusEvent.LikeStatus.create(playlist1, false));
        likes.put(playlist2, LikesStatusEvent.LikeStatus.create(playlist2, false));
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(likes));

        startServiceObserver.assertNoValues();
        makePlaylistUnavailableOffline.onNext(RxSignal.SIGNAL);
        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void addOfflinePlaylistOnCreationWhenOfflineCollectionEnabled() {
        PublishSubject<RxSignal> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(Sets.newHashSet(PLAYLIST))).thenReturn(
                makeAvailableOffline);

        controller.subscribe();

        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityCreated(PLAYLIST));

        startServiceObserver.assertValueCount(0);
        makeAvailableOffline.onNext(RxSignal.SIGNAL);
        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void addOfflinePlaylistOnLikeWhenOfflineCollectionEnabled() {
        PublishSubject<RxSignal> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(Sets.newHashSet(PLAYLIST))).thenReturn(
                makeAvailableOffline);

        controller.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(PLAYLIST, true, 1));

        startServiceObserver.assertValueCount(0);
        makeAvailableOffline.onNext(RxSignal.SIGNAL);
        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void dontAddAgainOfflinePlaylistOnLikeWhenOfflineCollectionEnabled() {
        // Note : If not, this would trigger an inifinite loop.
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Single.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), true, PLAYLIST));

        startServiceObserver.assertValueCount(1);
    }

    @Test
    public void nopOpWhenPlaylistCreatedButOfflineCollectionDisabled() {
        controller.subscribe();
        onCollectionChanged.onNext(SIGNAL);

        startServiceObserver.assertNoValues();
    }

    private class TestDefaultObserver extends DefaultObserver<Object> {
        public void onNext(Object object) {
            super.onNext(object);
            startServiceObserver.onNext(object);
        }

        public void onError(Throwable throwable) {
            super.onError(throwable);
            startServiceObserver.onError(throwable);
        }

        public void onComplete() {
            super.onComplete();
            startServiceObserver.onComplete();
        }
    }
}
