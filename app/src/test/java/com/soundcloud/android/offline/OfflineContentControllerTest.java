package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentChangedEvent.requested;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collections;

public class OfflineContentControllerTest extends AndroidUnitTest {

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final Urn PLAYLIST = Urn.forPlaylist(123L);
    private static final Void SIGNAL = null;

    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private PlaylistWithTracks playlistWithTracks;
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
        controller = new OfflineContentController(eventBus, settingsStorage, serviceInitiator, offlineContentOperations);
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

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startsOfflineSyncWhenATrackIsLiked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, true, 1));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenATrackIsUnliked() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(TRACK, false, 1));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void startsOfflineSyncWhenLikeSyncingUpdatedTheLikes() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, true));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenLikeSyncingDidNotUpdateTheLiked() {
        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_TRACK_LIKES, false));

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
    public void doesNotStartOfflineSyncWhenWifiOnlySyncWasEnabled() {
        controller.subscribe();

        wifiOnlyToggleSetting.onNext(true);

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startsOfflineSyncWhenTrackAddedToPlaylistMarkedAsAvailableOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenTrackAddedToNonOfflinePlaylist() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST, 1));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startOfflineSyncWhenPlaylistMarkedAsOfflineSyncedAndChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void doesNotStartOfflineSyncWhenPlaylistMarkedAsOfflineSyncedButNoChanged() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();

        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, false, PLAYLIST));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void doesNotStartOfflineSyncOnSyncResultEventForPlaylistNotMarkedAsOffline() {
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        startServiceSubscriber.assertNoValues();
    }

    @Test
    public void startOfflineSyncOnPolicyUpdateEvent() {
        controller.subscribe();

        eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.create(Collections.singletonList(Urn.forTrack(123L))));

        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void addOfflinePlaylistOnCreationWhenOfflineCollectionEnabled() {
        final PublishSubject<Void> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(PLAYLIST)).thenReturn(makeAvailableOffline);
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromPlaylistCreated(PLAYLIST));

        startServiceSubscriber.assertValueCount(0);
        makeAvailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void addOfflinePlaylistOnLikeWhenOfflineCollectionEnabled() {
        final PublishSubject<Void> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(PLAYLIST)).thenReturn(makeAvailableOffline);
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(PLAYLIST, true, 1));

        startServiceSubscriber.assertValueCount(0);
        makeAvailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }
    
    @Test
    public void addOfflinePlaylistOnSyncWhenOfflineCollectionEnabled() {
        final PublishSubject<Void> makeAvailableOffline = PublishSubject.create();
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(PLAYLIST)).thenReturn(makeAvailableOffline);
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

        startServiceSubscriber.assertValueCount(0);
        makeAvailableOffline.onNext(null);
        startServiceSubscriber.assertValueCount(1);
    }

    @Test
    public void dontAddAgainOfflinePlaylistOnLikeWhenOfflineCollectionEnabled() {
        // Note : If not, this would trigger an inifinite loop.
        when(offlineContentOperations.isOfflineCollectionEnabled()).thenReturn(true);
        when(offlineContentOperations.makePlaylistAvailableOffline(PLAYLIST)).thenReturn(Observable.<Void>empty());
        when(offlineContentOperations.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));

        controller.subscribe();
        eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, PLAYLIST));

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
