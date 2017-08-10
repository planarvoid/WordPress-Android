package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentUpdates.builder;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
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
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OfflineContentOperationsTest {

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
    @Mock private Consumer<Object> startServiceConsumer;
    @Mock private Consumer<Object> scheduleCleanupConsumer;
    @Mock private LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    @Mock private OfflineContentScheduler serviceScheduler;
    @Mock private ResetOfflineContentCommand resetOfflineContentCommand;
    @Mock private IntroductoryOverlayOperations introductoryOverlayOperations;

    private OfflineContentOperations operations;
    private TestEventBusV2 eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBusV2();

        when(serviceInitiator.startFromUserConsumer()).thenReturn(startServiceConsumer);
        when(serviceScheduler.scheduleCleanupConsumer()).thenReturn(scheduleCleanupConsumer);
        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(LIKED_TRACKS));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                rx.Observable.just(Collections.emptyList()));

        final Urn offlinePlaylist = Urn.forPlaylist(112233L);
        final List<Urn> offlinePlaylists = singletonList(offlinePlaylist);
        when(loadOfflinePlaylistsCommand.toSingle()).thenReturn(Single.just(offlinePlaylists));

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
                Schedulers.trampoline(),
                introductoryOverlayOperations);
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(new ArrayList<>()));
        operations.updateOfflineContentStalePolicies().subscribe();

        verifyZeroInteractions(policyOperations);
    }

    @Test
    public void updateStalePoliciesRequestsPolicyUpdatesFromPolicyOperations() {
        final List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(tracks));

        operations.updateOfflineContentStalePolicies().subscribe();

        verify(policyOperations).updatePolicies(tracks);
    }

    @Test
    public void loadOfflineContentStoresContentUpdates() throws Exception {
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(Collections.emptyList()));
        when(loadExpectedContentCommand.toSingle()).thenReturn(Single.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toSingle(downloadRequests)).thenReturn(Single.just(offlineContentUpdates));

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

        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(Collections.emptyList()));
        when(loadExpectedContentCommand.toSingle()).thenReturn(Single.just(expectedContent));
        when(loadOfflineContentUpdatesCommand.toSingle(expectedContent)).thenReturn(Single.just(updates));

        operations.loadOfflineContentUpdates().subscribe();

        verify(publisher).publishRemoved(updates.tracksToRemove());
        verify(publisher).publishDownloaded(updates.tracksToRestore());
        verify(publisher).publishUnavailable(updates.unavailableTracks());
    }

    @Test
    public void loadOfflineContentReturnsContentUpdates() throws Exception {
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(Collections.emptyList()));
        when(loadExpectedContentCommand.toSingle()).thenReturn(Single.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toSingle(downloadRequests)).thenReturn(Single.just(
                offlineContentUpdates));

        final TestObserver<OfflineContentUpdates> observer = operations.loadOfflineContentUpdates().test();

        observer.assertValue(offlineContentUpdates);
    }

    @Test
    public void enableOfflineCollectionStartsService() throws Exception {
        final Urn playlist1 = Urn.forPlaylist(123L);
        final Urn playlist2 = Urn.forPlaylist(456L);
        final List<Urn> expectedOfflinePlaylists = newArrayList(playlist1, playlist2);
        when(offlineContentStorage.addLikedTrackCollection()).thenReturn(Observable.just(new ChangeResult(1)));
        when(collectionOperations.myPlaylists()).thenReturn(Maybe.just(Arrays.asList(createPlaylistItem(playlist1),
                                                                                     createPlaylistItem(playlist2))));
        when(offlineContentStorage.resetOfflinePlaylists(expectedOfflinePlaylists)).thenReturn(Observable.just(new TxnResult()));
        final SingleSubject<SyncJobResult> refreshSubject = SingleSubject.create();
        when(syncInitiatorBridge.refreshMyPlaylists()).thenReturn(refreshSubject);

        operations.enableOfflineCollection().subscribe();

        verify(startServiceConsumer).accept(any());
        verify(introductoryOverlayOperations).setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true);
        assertThat(refreshSubject.hasObservers()).isTrue();
    }

    @Test
    public void makePlaylistAvailableOfflineStoresAsOfflineContent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        List<Urn> playlists = singletonList(playlistUrn);
        when(offlineContentStorage.storeAsOfflinePlaylists(playlists)).thenReturn(Observable.just(txnResult));

        operations.makePlaylistAvailableOffline(playlistUrn).test().assertValueCount(1).assertComplete();
    }

    @Test
    public void makePlaylistUnavailableOfflineRemovesOfflineContentPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Observable.just(
                changeResult));

        final TestObserver<RxSignal> observer = operations.makePlaylistUnavailableOffline(playlistUrn).test();

        observer.assertValueCount(1);
        observer.assertComplete();
    }

    @Test
    public void makePlaylistAvailableOfflinePublishesEntityStateChange() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final List<Urn> playlists = singletonList(playlistUrn);

        when(offlineContentStorage.storeAsOfflinePlaylists(playlists)).thenReturn(Observable.just(txnResult));

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

        when(offlineContentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn))).thenReturn(Observable.just(txnResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe();

        verify(syncInitiator).syncPlaylistsAndForget(Lists.newArrayList(playlistUrn));
    }

    @Test
    public void makePlaylistAvailableOfflineStartsServiceAndIgnoreSyncErrors() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        when(offlineContentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn))).thenReturn(Observable.just(txnResult));

        operations.makePlaylistAvailableOffline(playlistUrn)
                  .test()
                  .assertNoErrors();
    }

    @Test
    public void makePlaylistUnavailableOfflineStartsService() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Observable.just(
                changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        verify(startServiceConsumer).accept(any());
    }

    @Test
    public void makePlaylistUnavailableOfflineScheduleFilesCleaUp() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Observable.just(
                changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        verify(scheduleCleanupConsumer).accept(any());
    }


    @Test
    public void clearOfflineContentStartsService() throws Exception {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearOfflineContentCommand.toSingle()).thenReturn(Single.just(removed));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Single.just(true));

        operations.clearOfflineContent().subscribe();

        verify(startServiceConsumer).accept(any());
    }

    @Test
    public void resetOfflineContentStartsService() throws Exception {
        OfflineContentLocation location = OfflineContentLocation.DEVICE_STORAGE;
        List<Urn> reset = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(resetOfflineContentCommand.toSingle(location)).thenReturn(Single.just(reset));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Single.just(true));

        operations.resetOfflineContent(location).subscribe();

        verify(startServiceConsumer).accept(any());
    }

    @Test
    public void resetOfflineFeature() throws Exception {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearOfflineContentCommand.toSingle()).thenReturn(Single.just(removed));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Single.just(true));

        operations.disableOfflineFeature().subscribe();

        verify(startServiceConsumer).accept(any());
        verify(offlineContentStorage).removeOfflineCollection();
    }

    @Test
    public void loadOfflineContentUpdatesDoesNotFailWhenPoliciesFailedToUpdate() {
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                rx.Observable.error(new RuntimeException("Test exception")));
        final ExpectedOfflineContent expectedOfflineContent = mock(ExpectedOfflineContent.class);
        when(loadExpectedContentCommand.toSingle()).thenReturn(Single.just(expectedOfflineContent));
        when(loadOfflineContentUpdatesCommand.toSingle(expectedOfflineContent)).thenReturn(Single.just(mock(OfflineContentUpdates.class)));
        final TestObserver<OfflineContentUpdates> observer = operations.loadOfflineContentUpdates().test();

        observer.assertComplete();
        observer.assertNoErrors();
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
    public void disableOfflineLikedTracksStartsService() throws Exception {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Observable.just(changeResult));
        operations.disableOfflineLikedTracks().subscribe();

        verify(startServiceConsumer).accept(any());
    }

    @Test
    public void disableOfflineLikedTracksScheduleFilesCleanUp() throws Exception {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Observable.just(changeResult));
        operations.disableOfflineLikedTracks().subscribe();

        verify(scheduleCleanupConsumer).accept(any());
    }

    private ExpectedOfflineContent getExpectedOfflineContent() {
        return mock(ExpectedOfflineContent.class);
    }

    public Playlist createPlaylistItem(Urn urn) {
        return ModelFixtures.playlistBuilder().urn(urn).build();
    }
}
