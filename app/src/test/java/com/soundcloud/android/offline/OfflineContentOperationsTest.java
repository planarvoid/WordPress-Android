package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentUpdates.builder;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import com.squareup.sqlbrite2.BriteDatabase;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OfflineContentOperationsTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Collection<Urn> LIKED_TRACKS = singletonList(TRACK_URN_1);

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
    @Mock private Action scheduleCleanupConsumer;
    @Mock private OfflineContentScheduler serviceScheduler;
    @Mock private IntroductoryOverlayOperations introductoryOverlayOperations;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private TrackOfflineStateProvider trackOfflineStateProvider;
    @Mock private SecureFileStorage secureFileStorage;

    private OfflineContentOperations operations;
    private TestEventBusV2 eventBus;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBusV2();

        when(serviceScheduler.actionScheduleCleanupConsumer()).thenReturn(scheduleCleanupConsumer);
        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(LIKED_TRACKS));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                rx.Observable.just(Collections.emptyList()));

        final Urn offlinePlaylist = Urn.forPlaylist(112233L);
        final List<Urn> offlinePlaylists = singletonList(offlinePlaylist);
        when(offlineContentStorage.getOfflinePlaylists()).thenReturn(Single.just(offlinePlaylists));

        operations = new OfflineContentOperations(
                publisher,
                loadTracksWithStalePolicies,
                clearOfflineContentCommand,
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
                Schedulers.trampoline(),
                introductoryOverlayOperations,
                offlineSettingsStorage,
                trackOfflineStateProvider,
                secureFileStorage);
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(new ArrayList<>()));
        operations.updateOfflineContentStalePolicies().test().assertComplete();

        verifyZeroInteractions(policyOperations);
    }

    @Test
    public void updateStalePoliciesRequestsPolicyUpdatesFromPolicyOperations() {
        final List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(tracks));

        operations.updateOfflineContentStalePolicies().test().assertComplete();

        verify(policyOperations).updatePolicies(tracks);
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
        when(trackDownloadsStorage.writeUpdates(updates)).thenReturn(Single.just(mock(BriteDatabase.Transaction.class)));

        operations.loadOfflineContentUpdates().test().assertComplete();

        verify(publisher).publishRemoved(updates.tracksToRemove());
        verify(publisher).publishDownloaded(updates.tracksToRestore());
        verify(publisher).publishUnavailable(updates.unavailableTracks());
    }

    @Test
    public void loadOfflineContentFailsFastOnUpdatePublishFailure() {
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
        when(trackDownloadsStorage.writeUpdates(updates)).thenReturn(Single.error(IOException::new));

        operations.loadOfflineContentUpdates().test().assertError(IOException.class);

        verify(publisher, never()).publishRemoved(anyCollection());
        verify(publisher, never()).publishDownloaded(anyCollection());
        verify(publisher, never()).publishUnavailable(anyCollection());
    }

    @Test
    public void loadOfflineContentReturnsContentUpdates() throws Exception {
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toSingle()).thenReturn(Single.just(Collections.emptyList()));
        when(loadExpectedContentCommand.toSingle()).thenReturn(Single.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toSingle(downloadRequests)).thenReturn(Single.just(offlineContentUpdates));
        when(trackDownloadsStorage.writeUpdates(offlineContentUpdates)).thenReturn(Single.just(mock(BriteDatabase.Transaction.class)));

        final TestObserver<OfflineContentUpdates> observer = operations.loadOfflineContentUpdates().test();

        observer.assertValue(offlineContentUpdates);
    }

    @Test
    public void enableOfflineCollectionStartsService() throws Exception {
        final Urn playlist1 = Urn.forPlaylist(123L);
        final Urn playlist2 = Urn.forPlaylist(456L);
        final List<Urn> expectedOfflinePlaylists = newArrayList(playlist1, playlist2);
        when(offlineContentStorage.addLikedTrackCollection()).thenReturn(Completable.complete());
        when(collectionOperations.myPlaylists()).thenReturn(Maybe.just(Arrays.asList(createPlaylistItem(playlist1),
                                                                                     createPlaylistItem(playlist2))));
        when(offlineContentStorage.resetOfflinePlaylists(expectedOfflinePlaylists)).thenReturn(Completable.complete());
        final SingleSubject<SyncJobResult> refreshSubject = SingleSubject.create();
        when(syncInitiatorBridge.refreshMyPlaylists()).thenReturn(refreshSubject);

        operations.enableOfflineCollection().test();

        verify(serviceInitiator).startFromUserConsumer();
        verify(introductoryOverlayOperations).setOverlayShown(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES, true);
        assertThat(refreshSubject.hasObservers()).isTrue();
    }

    @Test
    public void makePlaylistAvailableOfflineStoresAsOfflineContent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        List<Urn> playlists = singletonList(playlistUrn);
        when(offlineContentStorage.storeAsOfflinePlaylists(playlists)).thenReturn(Completable.complete());

        operations.makePlaylistAvailableOffline(playlistUrn).test().assertComplete();
    }

    @Test
    public void makePlaylistUnavailableOfflineRemovesOfflineContentPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Completable.complete());

        TestObserver<Void> observer = operations.makePlaylistUnavailableOffline(playlistUrn).test();
        observer.assertComplete();
    }

    @Test
    public void makePlaylistAvailableOfflinePublishesEntityStateChange() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final List<Urn> playlists = singletonList(playlistUrn);

        when(offlineContentStorage.storeAsOfflinePlaylists(playlists)).thenReturn(Completable.complete());

        operations.makePlaylistAvailableOffline(playlistUrn).test().assertComplete();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event).isEqualTo(PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsMarkedForDownload(playlists));
    }

    @Test
    public void makePlaylistUnAvailableOfflinePublishesEntityStateChange() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        List<Urn> playlists = singletonList(playlistUrn);
        when(offlineContentStorage.removePlaylistsFromOffline(playlists)).thenReturn(Completable.complete());

        operations.makePlaylistUnavailableOffline(playlistUrn).test().assertComplete();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event).isEqualTo(PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload(playlists));
    }

    @Test
    public void makePlaylistAvailableOfflineStartsService() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        when(offlineContentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn))).thenReturn(Completable.complete());

        operations.makePlaylistAvailableOffline(playlistUrn).test().assertComplete();

        verify(syncInitiator).syncPlaylistsAndForget(Lists.newArrayList(playlistUrn));
    }

    @Test
    public void makePlaylistAvailableOfflineStartsServiceAndIgnoreSyncErrors() {
        final Urn playlistUrn = Urn.forPlaylist(123L);

        when(offlineContentStorage.storeAsOfflinePlaylists(singletonList(playlistUrn))).thenReturn(Completable.complete());

        operations.makePlaylistAvailableOffline(playlistUrn)
                  .test()
                  .assertNoErrors();
    }

    @Test
    public void makePlaylistUnavailableOfflineStartsService() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Completable.complete());

        operations.makePlaylistUnavailableOffline(playlistUrn).test().assertComplete();

        verify(serviceInitiator).startFromUserConsumer();
    }

    @Test
    public void makePlaylistUnavailableOfflineScheduleFilesCleaUp() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistsFromOffline(singletonList(playlistUrn))).thenReturn(Completable.complete());

        operations.makePlaylistUnavailableOffline(playlistUrn).test().assertComplete();

        verify(scheduleCleanupConsumer).run();
    }


    @Test
    public void clearOfflineContentStartsService() throws Exception {
        when(clearOfflineContentCommand.toSingle()).thenReturn(Single.just(true));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Single.just(true));

        operations.clearOfflineContent().test().assertComplete();

        verify(serviceInitiator).startFromUserConsumer();
    }

    @Test
    public void resetOfflineContentStartsService() throws Exception {
        OfflineContentLocation location = OfflineContentLocation.DEVICE_STORAGE;

        when(trackDownloadsStorage.getResetTracksToRequested()).thenReturn(Completable.complete());
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Single.just(true));

        operations.resetOfflineContent(location).test().assertComplete();

        verify(serviceInitiator).startFromUserConsumer();

        verify(trackOfflineStateProvider).clear();
        verify(secureFileStorage).deleteAllTracks();
        verify(offlineSettingsStorage).setOfflineContentLocation(location);
        verify(secureFileStorage).updateOfflineDir();
    }

    @Test
    public void resetOfflineFeature() throws Exception {
        when(clearOfflineContentCommand.toSingle()).thenReturn(Single.just(true));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(io.reactivex.Single.just(true));

        operations.disableOfflineFeature().test().assertComplete();

        verify(serviceInitiator).startFromUserConsumer();
        verify(offlineSettingsStorage).removeOfflineCollection();
    }

    @Test
    public void loadOfflineContentUpdatesDoesNotFailWhenPoliciesFailedToUpdate() {
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(
                rx.Observable.error(new RuntimeException("Test exception")));
        final ExpectedOfflineContent expectedOfflineContent = mock(ExpectedOfflineContent.class);
        when(loadExpectedContentCommand.toSingle()).thenReturn(Single.just(expectedOfflineContent));

        OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);
        when(loadOfflineContentUpdatesCommand.toSingle(expectedOfflineContent)).thenReturn(Single.just(offlineContentUpdates));
        when(trackDownloadsStorage.writeUpdates(offlineContentUpdates)).thenReturn(Single.just(mock(BriteDatabase.Transaction.class)));
        final TestObserver<OfflineContentUpdates> observer = operations.loadOfflineContentUpdates().test();

        observer.assertComplete();
        observer.assertNoErrors();
    }

    @Test
    public void disableOfflineLikedTracksSendAnEvent() {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Completable.complete());

        operations.disableOfflineLikedTracks().test().assertComplete();

        final OfflineContentChangedEvent event = eventBus.lastEventOn(EventQueue.OFFLINE_CONTENT_CHANGED);
        assertThat(event.state).isEqualTo(OfflineState.NOT_OFFLINE);
        assertThat(event.isLikedTrackCollection).isEqualTo(true);
    }

    @Test
    public void disableOfflineLikedTracksStartsService() throws Exception {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Completable.complete());
        operations.disableOfflineLikedTracks().test().assertComplete();

        verify(serviceInitiator).startFromUserConsumer();
    }

    @Test
    public void disableOfflineLikedTracksScheduleFilesCleanUp() throws Exception {
        when(offlineContentStorage.removeLikedTrackCollection()).thenReturn(Completable.complete());
        operations.disableOfflineLikedTracks().test().assertComplete();

        verify(scheduleCleanupConsumer).run();
    }

    private ExpectedOfflineContent getExpectedOfflineContent() {
        return mock(ExpectedOfflineContent.class);
    }

    public Playlist createPlaylistItem(Urn urn) {
        return PlaylistFixtures.playlistBuilder().urn(urn).build();
    }
}
