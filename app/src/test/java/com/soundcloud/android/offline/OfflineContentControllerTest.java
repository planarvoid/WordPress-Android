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
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OfflineContentControllerTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);
    private static final Void SIGNAL = null;

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineServiceInitiator serviceInitiator;
    @Mock private Action0 startServiceAction;
    @Mock private Action0 stopServiceAction;

    private OfflineContentController controller;
    private TestEventBus eventBus;
    private PublishSubject<Boolean> wifiOnlyToggleSetting;
    private PublishSubject<Void> onCollectionChanged;
    private TestSubscriber<Void> startServiceSubscriber;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        wifiOnlyToggleSetting = PublishSubject.create();
        onCollectionChanged = PublishSubject.create();

        startServiceSubscriber = new TestSubscriber<>();
        when(serviceInitiator.start()).thenReturn(startServiceAction);
        when(serviceInitiator.stop()).thenReturn(stopServiceAction);
        when(serviceInitiator.startSubscriber()).thenReturn(startServiceSubscriber);
        when(settingsStorage.getWifiOnlyOfflineSyncStateChange()).thenReturn(wifiOnlyToggleSetting);
        when(offlineContentOperations.enableOfflineCollection()).thenReturn(Observable.<Void>just(null));
        when(offlineContentOperations.enableOfflineLikedTracks()).thenReturn(Observable.<Void>just(null));
        controller = new OfflineContentController(eventBus,
                                                  settingsStorage,
                                                  serviceInitiator,
                                                  offlineContentOperations);
    }

    @Test
    public void startsServiceWhenSubscribes() {
        controller.subscribe();

        verify(startServiceAction).call();
    }

    @Test
    public void stopServiceWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();

        verify(stopServiceAction).call();
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikesChangedButOfflineLikesAreDisabled() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(false));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.TRACK_LIKES.name(), true));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(TRACK, true, 1));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(TRACK, false, 1));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.TRACK_LIKES.name(), false));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void ignoreStartEventsWhenUnsubscribed() {
        controller.subscribe();

        controller.unsubscribe();

        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(Collections.<Urn>emptyList(), true));
        onCollectionChanged.onNext(SIGNAL);

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncSettingWasDisabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(false);

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenWifiOnlySyncWasEnabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(true);

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void restartsOfflineSyncWhenConnectionChanges() {
        controller.subscribe();

        eventBus.publish(EventQueue.NETWORK_CONNECTION_CHANGED, ConnectionType.OFFLINE);

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenPlaylistMarkedAsAvailableOfflineIsEdited() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        Playlist editedPlaylist = ModelFixtures.playlistBuilder().urn(PLAYLIST).build();

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, fromPlaylistEdited(editedPlaylist));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, fromTrackAddedToPlaylist(PLAYLIST, 1));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYLIST_CHANGED, fromTrackAddedToPlaylist(PLAYLIST, 1));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startOfflineSyncWhenPlaylistMarkedAsOfflineSyncedAndChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), true, PLAYLIST));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenPlaylistMarkedAsOfflineSyncedButNoChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), false, PLAYLIST));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void doesNotStartOfflineSyncOnSyncResultEventForPlaylistNotMarkedAsOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), true, PLAYLIST));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startOfflineSyncOnPolicyUpdateEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.create(singletonList(Urn.forTrack(123L))));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void removePlaylistFromOfflineContentWhenDeleted() {
        final Urn playlist1 = Urn.forPlaylist(123L);
        final Urn playlist2 = Urn.forPlaylist(456L);

        final PublishSubject<Void> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(Arrays.asList(playlist1, playlist2))).thenReturn(
                makePlaylistUnavailableOffline);

        controller.subscribe();
        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntitiesDeleted(Sets.newHashSet(playlist1, playlist2)));

        startServiceSubscriber.assertNoValues();
        makePlaylistUnavailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void removePlaylistFromOfflineContentWhenUnliked() {
        final Urn playlist1 = Urn.forPlaylist(123L);
        final Urn playlist2 = Urn.forPlaylist(456L);

        final PublishSubject<Void> makePlaylistUnavailableOffline = PublishSubject.create();
        when(offlineContentOperations.makePlaylistUnavailableOffline(Arrays.asList(playlist1, playlist2))).thenReturn(
                makePlaylistUnavailableOffline);

        controller.subscribe();
        final Map<Urn, LikesStatusEvent.LikeStatus> likes = new HashMap<>(2);
        likes.put(playlist1, LikesStatusEvent.LikeStatus.create(playlist1, false));
        likes.put(playlist2, LikesStatusEvent.LikeStatus.create(playlist2, false));
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.createFromSync(likes));

        startServiceSubscriber.assertNoValues();
        makePlaylistUnavailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void addOfflinePlaylistOnCreationWhenOfflineCollectionEnabled() {
        final PublishSubject<Void> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(singletonList(PLAYLIST))).thenReturn(
                makeAvailableOffline);

        controller.subscribe();

        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityCreated(PLAYLIST));

        startServiceSubscriber.assertValueCount(0);
        makeAvailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void addOfflinePlaylistOnLikeWhenOfflineCollectionEnabled() {
        final PublishSubject<Void> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(singletonList(PLAYLIST))).thenReturn(
                makeAvailableOffline);

        controller.subscribe();
        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(PLAYLIST, true, 1));

        startServiceSubscriber.assertValueCount(0);
        makeAvailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void dontAddAgainOfflinePlaylistOnLikeWhenOfflineCollectionEnabled() {
        // Note : If not, this would trigger an inifinite loop.
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(singletonList(PLAYLIST))).thenReturn(Observable.<Void>empty());
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.PLAYLIST.name(), true, PLAYLIST));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void nopOpWhenPlaylistCreatedButOfflineCollectionDisabled() {
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(false);

        controller.subscribe();
        onCollectionChanged.onNext(SIGNAL);

        startServiceSubscriber.assertNoValues();
    }

}
