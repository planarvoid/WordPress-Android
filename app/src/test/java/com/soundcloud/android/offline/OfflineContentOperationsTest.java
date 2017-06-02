package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentUpdates.builder;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OfflineContentOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Collection<Urn> LIKED_TRACKS = singletonList(TRACK_URN_1);

    @Mock private StoreDownloadUpdatesCommand storeDownloadUpdatesCommand;
    @Mock private OfflineStatePublisher publisher;
    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private OfflineContentStorage offlineContentStorage;
    @Mock private PolicyOperations policyOperations;
    @Mock private LoadExpectedContentCommand loadExpectedContentCommand;
    @Mock private LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    @Mock private OfflineServiceInitiator serviceInitiator;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private CollectionOperations collectionOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private TxnResult txnResult;
    @Mock private ChangeResult changeResult;
    @Mock private ClearOfflineContentCommand clearOfflineContentCommand;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Action1<Object> startServiceAction;
    @Mock private Action1<Object> scheduleCleanupAction;
    @Mock private LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    @Mock private OfflineContentScheduler serviceScheduler;
    @Mock private ResetOfflineContentCommand resetOfflineContentCommand;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();

        when(serviceInitiator.startFromUserAction()).thenReturn(startServiceAction);
        when(serviceScheduler.scheduleCleanupAction()).thenReturn(scheduleCleanupAction);
        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(LIKED_TRACKS));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                Observable.just(Collections.emptyList()));
        when(txnResult.success()).thenReturn(true);

        final Urn offlinePlaylist = Urn.forPlaylist(112233L);
        final List<Urn> offlinePlaylists = singletonList(offlinePlaylist);
        when(loadOfflinePlaylistsCommand.toObservable(null)).thenReturn(Observable.just(offlinePlaylists));

        operations = new OfflineContentOperations(
                storeDownloadUpdatesCommand,
                publisher,
                loadTracksWithStalePolicies,
                clearOfflineContentCommand,
                resetOfflineContentCommand,
                eventBus,
                offlineContentStorage,
                policyOperations,
                loadExpectedContentCommand,
                loadOfflineContentUpdatesCommand,
                serviceInitiator,
                serviceScheduler,
                syncInitiatorBridge,
                syncInitiator,
                featureOperations,
                trackDownloadsStorage,
                collectionOperations,
                loadOfflinePlaylistsCommand,
                Schedulers.immediate());
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(new ArrayList<>()));
        operations.updateOfflineContentStalePolicies().subscribe();

        verifyZeroInteractions(policyOperations);
    }

    @Test
    public void updateStalePoliciesRequestsPolicyUpdatesFromPolicyOperations() {
        final List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(tracks));

        operations.updateOfflineContentStalePolicies().subscribe();

        verify(policyOperations).updatePolicies(tracks);
    }

    @Test
    public void loadOfflineContentStoresContentUpdates() throws Exception {
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(Collections.emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Observable.just(true));
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentUpdates));

        operations.loadOfflineContentUpdates().subscribe();

        verify(storeDownloadUpdatesCommand).call(offlineContentUpdates);
    }

    @Test
    public void loadOfflineContentStoresContentUpdatesPublishesContentUpdates() {
        final ExpectedOfflineContent expectedContent= getExpectedOfflineContent();
        final DownloadRequest downloadRequests = mock(DownloadRequest.class);
        final OfflineContentUpdates updates = builder()
                .tracksToRemove(singletonList(Urn.forTrack(1L)))
                .tracksToRestore(singletonList(Urn.forTrack(2L)))
                .unavailableTracks(singletonList(Urn.forTrack(3L)))
                .tracksToDownload(singletonList(downloadRequests))
                .build();

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(Collections.emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Observable.just(true));
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(expectedContent));
        when(loadOfflineContentUpdatesCommand.toObservable(expectedContent)).thenReturn(Observable.just(updates));

        operations.loadOfflineContentUpdates().subscribe();

        verify(publisher).publishRemoved(updates.tracksToRemove());
        verify(publisher).publishDownloaded(updates.tracksToRestore());
        verify(publisher).publishUnavailable(updates.unavailableTracks());
    }

    @Test
    public void loadOfflineContentReturnsContentUpdates() throws Exception {
        final TestSubscriber<OfflineContentUpdates> subscriber = new TestSubscriber<>();
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(Collections.emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Observable.just(true));
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(
                offlineContentUpdates));

        operations.loadOfflineContentUpdates().subscribe(subscriber);

        subscriber.assertValue(offlineContentUpdates);
    }

    @Test
    public void enableOfflineCollectionStartsService() {
        final Urn playlist1 = Urn.forPlaylist(123L);
        final Urn playlist2 = Urn.forPlaylist(456L);
        final List<Urn> expectedOfflinePlaylists = newArrayList(playlist1, playlist2);
        when(offlineContentStorage.addLikedTrackCollection()).thenReturn(Observable.just(new ChangeResult(1)));
        when(collectionOperations.myPlaylists()).thenReturn(Maybe.just(Arrays.asList(createPlaylistItem(playlist1),
                                                                                     createPlaylistItem(playlist2))));
        when(offlineContentStorage.resetOfflinePlaylists(expectedOfflinePlaylists)).thenReturn(Observable.just(new TxnResult()));
        final PublishSubject<Void> refreshSubject = PublishSubject.create();
        when(syncInitiatorBridge.refreshMyPlaylists()).thenReturn(refreshSubject);

        operations.enableOfflineCollection().subscribe();

        verify(startServiceAction).call(any());
        assertThat(refreshSubject.hasObservers()).isTrue();
    }

    @Test
    public void makePlaylistAvailableOfflineStoresAsOfflineContent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        List<Urn> playlists = singletonList(playlistUrn);
        when(offlineContentStorage.storeAsOfflinePlaylists(playlists)).thenReturn(Observable.just(txnResult));
        when(syncInitiator.syncPlaylists(playlists)).thenReturn(Observable.just(SyncJobResult.success("blah", true)));

        operations.makePlaylistAvailableOffline(playlistUrn).test().assertValueCount(1).assertCompleted();
    }

    @Test
    public void makePlaylistUnavailableOfflineRemovesOfflineContentPlaylist() {
        TestSubscriber<Void> subscriber = new TestSubscriber<>();
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Observable.just(
                changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(subscriber);

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
    }

    @Test
    public void makePlaylistAvailableOfflinePublishesEntityStateChange() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final List<Urn> playlists = singletonList(playlistUrn);

        when(offlineContentStorage.storeAsOfflinePlaylists(playlists)).thenReturn(Observable.just(txnResult));
        when(syncInitiator.syncPlaylists(playlists)).thenReturn(Observable.empty());

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event).isEqualTo(PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsMarkedForDownload(playlists));
    }

    @Test
    public void makePlaylistUnAvailableOfflinePublishesEntityStateChange() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        List<Urn> playlists = singletonList(playlistUrn);
        when(offlineContentStorage.removePlaylistsFromOffline(playlists)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event).isEqualTo(PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload(playlists));
    }

    @Test
    public void makePlaylistAvailableOfflineStartsService() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final PublishSubject<SyncJobResult> sync = PublishSubject.create();

        when(offlineContentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn))).thenReturn(Observable.just(txnResult));
        when(syncInitiator.syncPlaylists(singletonList(playlistUrn))).thenReturn(sync);

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe();

        assertThat(sync.hasObservers()).isTrue();
    }

    @Test
    public void makePlaylistAvailableOfflineStartsServiceAndIgnoreSyncErrors() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        when(offlineContentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn))).thenReturn(Observable.just(txnResult));
        when(syncInitiator.syncPlaylists(singletonList(playlistUrn))).thenReturn(Observable.error(new UnknownHostException("Sync error")));

        operations.makePlaylistAvailableOffline(playlistUrn)
                  .test()
                  .assertNoErrors();
    }

    @Test
    public void makePlaylistUnavailableOfflineStartsService() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Observable.just(
                changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        verify(startServiceAction).call(any());
    }

    @Test
    public void makePlaylistUnavailableOfflineScheduleFilesCleaUp() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Observable.just(
                changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        verify(scheduleCleanupAction).call(any());
    }


    @Test
    public void clearOfflineContentStartsService() {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearOfflineContentCommand.toObservable(null)).thenReturn(Observable.just(removed));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Observable.just(true));

        operations.clearOfflineContent().subscribe();

        verify(startServiceAction).call(any());
    }

    @Test
    public void resetOfflineContentStartsService() {
        OfflineContentLocation location = OfflineContentLocation.DEVICE_STORAGE;
        List<Urn> reset = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(resetOfflineContentCommand.toObservable(location)).thenReturn(Observable.just(reset));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Observable.just(true));

        operations.resetOfflineContent(location).subscribe();

        verify(startServiceAction).call(any());
    }

    @Test
    public void resetOfflineFeature() {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearOfflineContentCommand.toObservable(null)).thenReturn(Observable.just(removed));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Observable.just(true));

        operations.disableOfflineFeature().subscribe();

        verify(startServiceAction).call(any());
        verify(offlineContentStorage).removeOfflineCollection();
    }

    @Test
    public void loadOfflineContentUpdatesDoesNotFailWhenPoliciesFailedToUpdate() {
        final TestSubscriber<OfflineContentUpdates> subscriber = new TestSubscriber<>();

        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                Observable.error(new RuntimeException("Test exception")));
        operations.loadOfflineContentUpdates().subscribe(subscriber);

        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void disableOfflineLikedTracksSendAnEvent() {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Observable.just(changeResult));

        operations.disableOfflineLikedTracks().subscribe();

        final OfflineContentChangedEvent event = eventBus.lastEventOn(EventQueue.OFFLINE_CONTENT_CHANGED);
        assertThat(event.state).isEqualTo(OfflineState.NOT_OFFLINE);
        assertThat(event.isLikedTrackCollection).isEqualTo(true);
    }

    @Test
    public void disableOfflineLikedTracksStartsService() {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Observable.just(changeResult));
        operations.disableOfflineLikedTracks().subscribe();

        verify(startServiceAction).call(any());
    }

    @Test
    public void disableOfflineLikedTracksScheduleFilesCleanUp() {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Observable.just(changeResult));
        operations.disableOfflineLikedTracks().subscribe();

        verify(scheduleCleanupAction).call(any());
    }

    private ExpectedOfflineContent getExpectedOfflineContent() {
        return mock(ExpectedOfflineContent.class);
    }

    public Playlist createPlaylistItem(Urn urn) {
        return ModelFixtures.playlistBuilder().urn(urn).build();
    }
}
