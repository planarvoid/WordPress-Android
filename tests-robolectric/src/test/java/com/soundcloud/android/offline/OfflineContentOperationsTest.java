package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.ClearTrackDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadPrioritizedPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.StoreDownloadedCommand;
import com.soundcloud.android.offline.commands.StorePendingDownloadsCommand;
import com.soundcloud.android.offline.commands.StorePendingRemovalsCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN_2 = Urn.forTrack(345L);
    private static final Urn TRACK_URN_3 = Urn.forTrack(678L);
    private static final Collection<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN_1);

    private static final WriteResult WRITE_RESULT_SUCCESS = new WriteResultStub(true);

    @Mock private StorePendingDownloadsCommand storePendingDownloadsCommand;
    @Mock private StorePendingRemovalsCommand storePendingRemovalsCommand;
    @Mock private StoreDownloadedCommand storeDownloadedCommand;
    @Mock private LoadPrioritizedPendingDownloadsCommand loadPrioritizedPendingDownloadsCommand;
    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies;
    @Mock private ChangeResult changeResult;
    @Mock private OfflineTracksStorage offlineTracksStorage;
    @Mock private OfflinePlaylistStorage playlistStorage;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private PolicyOperations policyOperations;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    @Mock private SecureFileStorage secureFileStorage;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;
    private TestSubscriber<Object> subscriber;
    public static final Urn PLAYLIST = Urn.forPlaylist(123L);

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new TestSubscriber<>();

        when(loadTracksWithValidPolicies.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(policyOperations.fetchAndStorePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>just(null));

        when(loadPrioritizedPendingDownloadsCommand.toObservable()).thenReturn(Observable.<List<DownloadRequest>>empty());
        when(offlineTracksStorage.pendingDownloads()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(offlineTracksStorage.pendingRemovals()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(offlineTracksStorage.downloaded()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(changeResult.success()).thenReturn(true);

        operations = new OfflineContentOperations(
                storePendingDownloadsCommand,
                storePendingRemovalsCommand,
                storeDownloadedCommand,
                clearTrackDownloadsCommand,
                loadTracksWithStalePolicies,
                loadPrioritizedPendingDownloadsCommand,
                settingsStorage,
                eventBus,
                playlistStorage,
                policyOperations,
                loadTracksWithValidPolicies,
                offlineTracksStorage,
                secureFileStorage,
                Schedulers.immediate());
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(new ArrayList<Urn>()));
        operations.updateStalePolicies().subscribe(subscriber);

        verifyZeroInteractions(policyOperations);
    }

    @Test
    public void updateStalePoliciesRequestsPolicyUpdatesFromPolicyOperations() {
        final List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(tracks));

        operations.updateStalePolicies().subscribe();

        verify(policyOperations).fetchAndStorePolicies(tracks);
    }

    @Test
    public void updateStalePoliciesChecksOfflineLikesEnabledFlag() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);

        operations.updateStalePolicies().subscribe();

        verify(settingsStorage).isOfflineLikedTracksEnabled();
        expect(loadTracksWithStalePolicies.getInput()).toBeTrue();
    }

    @Test
    public void getNoOfflineLikedTracksWhenOfflineLikedTracksDisabled() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(offlineTracksStorage.pendingLikedTracksUrns()).thenReturn(Observable.<List<Urn>>empty());
        when(settingsStorage.getOfflineLikedTracksStatus()).thenReturn(Observable.just(false));

        operations.getLikedTracksDownloadState().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.NO_OFFLINE);
    }

    @Test
    public void getDownloadingLikedTracksWhenCurrentStartedDownloadIsLiked() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(settingsStorage.getOfflineLikedTracksStatus()).thenReturn(Observable.just(true));
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(offlineTracksStorage.pendingLikedTracksUrns()).thenReturn(Observable.just(Arrays.asList(TRACK_URN_1)));

        operations.getLikedTracksDownloadState().subscribe(observer);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.start(TRACK_URN_1));

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.REQUESTED, DownloadState.DOWNLOADING);
    }

    @Test
    public void getDownloadedLikedTracksWhenNoPendingLikedTracksForDownloadAndOfflineLikesEnabled() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(settingsStorage.getOfflineLikedTracksStatus()).thenReturn(Observable.just(true));
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(offlineTracksStorage.pendingLikedTracksUrns()).thenReturn(Observable.just(Collections.<Urn>emptyList()));

        operations.getLikedTracksDownloadState().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.DOWNLOADED);
    }

    @Test
    public void getRequestedLikedTracksWhenPendingLikedTracksForDownloadAndNotCurrentlyDownloading() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(settingsStorage.getOfflineLikedTracksStatus()).thenReturn(Observable.just(true));
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(offlineTracksStorage.pendingLikedTracksUrns()).thenReturn(Observable.just(Arrays.asList(TRACK_URN_1)));

        operations.getLikedTracksDownloadState().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.REQUESTED);
    }

    @Test
    public void getNoOfflinePlaylistWhenIsNotOfflinePlaylist() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(false));
        when(offlineTracksStorage.pendingPlaylistTracksUrns(PLAYLIST)).thenReturn(Observable.<List<Urn>>empty());

        operations.getPlaylistDownloadState(PLAYLIST).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.NO_OFFLINE);
    }

    @Test
    public void getDownloadingPlaylistWhenCurrentStartedDownloadIsAPlaylistTrack() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        when(offlineTracksStorage.pendingPlaylistTracksUrns(PLAYLIST)).thenReturn(Observable.just(Arrays.asList(TRACK_URN_1)));

        operations.getPlaylistDownloadState(PLAYLIST).subscribe(observer);
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.start(TRACK_URN_1));

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.REQUESTED, DownloadState.DOWNLOADING);
    }

    @Test
    public void getDownloadedPlaylistWhenNoPendingPlaylistTracksAndIsOfflinePlaylist() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        when(offlineTracksStorage.pendingPlaylistTracksUrns(PLAYLIST)).thenReturn(Observable.just(Collections.<Urn>emptyList()));

        operations.getPlaylistDownloadState(PLAYLIST).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.DOWNLOADED);
    }

    @Test
    public void getRequestedPlaylistWhenPendingPlaylistTracksAndNotCurrentlyDownloading() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        when(offlineTracksStorage.pendingPlaylistTracksUrns(PLAYLIST)).thenReturn(Observable.just(Arrays.asList(TRACK_URN_1)));

        operations.getPlaylistDownloadState(PLAYLIST).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.REQUESTED);
    }

    @Test
    public void getDownloadingPlaylistWhenPendingPlaylistTracksAndCurrentlyDownloading() {
        final TestObserver<DownloadState> observer = new TestObserver<>();
        when(playlistStorage.isOfflinePlaylist(PLAYLIST)).thenReturn(Observable.just(true));
        when(offlineTracksStorage.pendingPlaylistTracksUrns(PLAYLIST)).thenReturn(Observable.just(Arrays.asList(TRACK_URN_1)));

        operations.getPlaylistDownloadState(PLAYLIST).subscribe(observer);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromMarkedForOffline(PLAYLIST, true));
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.start(TRACK_URN_1));

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.REQUESTED, DownloadState.DOWNLOADING);
    }

    @Test
    public void loadDownloadRequestsLoadsUpdatedPendingDownloads() {
        final TestObserver<List<DownloadRequest>> observer = new TestObserver<>();
        final List<DownloadRequest> requests = Arrays.asList(new DownloadRequest(Urn.forTrack(123L), "http://"));
        when(loadPrioritizedPendingDownloadsCommand.toObservable()).thenReturn(Observable.just(requests));

        operations.loadDownloadRequests().subscribe(observer);

        expect(observer.getOnCompletedEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toContainExactly(requests.get(0));
    }

    @Test
    public void loadDownloadRequestsEmitsDownloadFinishedEventWhenPendingRemovalAndDownloadedAddedToOfflineContent() {
        actualDownloadedTracks(TRACK_URN_2);
        actualPendingRemovals(TRACK_URN_1, TRACK_URN_2);
        expectedOfflineTracks(TRACK_URN_2);
        when(storeDownloadedCommand.call()).thenReturn(WRITE_RESULT_SUCCESS);

        operations.loadDownloadRequests().subscribe(subscriber);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadFinished(event, TRACK_URN_2);
    }

    @Test
    public void loadDownloadRequestsStoreTracksDownloadedWhenPendingRemovalAndDownloadedAddedToOfflineContent() {
        actualDownloadedTracks(TRACK_URN_1);
        actualPendingRemovals(TRACK_URN_1);
        expectedOfflineTracks(TRACK_URN_1);

        operations.loadDownloadRequests().subscribe(subscriber);

        expect(storeDownloadedCommand.getInput()).toEqual(Arrays.asList(TRACK_URN_1));
    }

    @Test
    public void loadDownloadRequestsEmitsDownloadRemovedEventWhenPendingRemovalFromOfflineContent() {
        actualDownloadedTracks(TRACK_URN_1);
        actualPendingDownloads(TRACK_URN_2);
        expectedOfflineTracks(TRACK_URN_2);
        when(storePendingRemovalsCommand.call()).thenReturn(WRITE_RESULT_SUCCESS);

        operations.loadDownloadRequests().subscribe(subscriber);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expectDownloadsRemoved(event, TRACK_URN_1);
    }

    @Test
    public void loadDownloadRequestsStorePendingRemovalWhenRemovedFromOfflineContent() {
        actualDownloadedTracks(TRACK_URN_1);
        expectedOfflineTracks(TRACK_URN_2);

        operations.loadDownloadRequests().subscribe(subscriber);

        expect(storePendingRemovalsCommand.getInput()).toEqual(Arrays.asList(TRACK_URN_1));
    }

    @Test
    public void loadDownloadRequestsStorePendingDownloadsWhenTracksAddedToOfflineContent() {
        expectedOfflineTracks(TRACK_URN_1, TRACK_URN_2, TRACK_URN_3);
        actualDownloadedTracks(TRACK_URN_1);
        actualPendingDownloads(TRACK_URN_2);

        operations.loadDownloadRequests().subscribe(subscriber);

        expect(storePendingDownloadsCommand.getInput()).toEqual(Arrays.asList(TRACK_URN_3));
    }

    @Test
    public void makePlaylistAvailableOfflineStoresAsOfflineContent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final TestObserver<Boolean> observer = new TestObserver<>();
        when(playlistStorage.storeAsOfflinePlaylist(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(true);
    }

    @Test
    public void makePlaylistUnavailableOfflineRemovesOfflineContentPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final TestObserver<Boolean> observer = new TestObserver<>();
        when(playlistStorage.removeFromOfflinePlaylists(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(true);
    }

    @Test
    public void makePlaylistAvailableOfflineEmitsMarkedForOfflineEntityChangeEvent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.storeAsOfflinePlaylist(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)).toEqual(true);
    }

    @Test
    public void makePlaylistUnavailableOfflineEmitsUnmarkedForOfflineEntityChangeEvent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.removeFromOfflinePlaylists(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)).toEqual(false);
    }

    @Test
    public void clearOfflineContentClearsTrackDownloads() {
        when(clearTrackDownloadsCommand.toObservable(null)).thenReturn(Observable.just(WRITE_RESULT_SUCCESS));

        final TestObserver<WriteResult> observer = new TestObserver<>();
        operations.clearOfflineContent().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(WRITE_RESULT_SUCCESS);
    }

    @Test
    public void clearOfflineContentRemovesOfflineTrackFiles() {
        when(clearTrackDownloadsCommand.toObservable(null)).thenReturn(Observable.just(WRITE_RESULT_SUCCESS));

        operations.clearOfflineContent().subscribe(new TestObserver<WriteResult>());

        verify(secureFileStorage).deleteAllTracks();
    }

    private void actualPendingRemovals(Urn... tracks) {
        when(offlineTracksStorage.pendingRemovals()).thenReturn(Observable.just(Arrays.asList(tracks)));
    }

    private void actualPendingDownloads(Urn... tracks) {
        when(offlineTracksStorage.pendingDownloads()).thenReturn(Observable.just(Arrays.asList(tracks)));
    }

    private void actualDownloadedTracks(Urn... tracks) {
        when(offlineTracksStorage.downloaded()).thenReturn(Observable.just(Arrays.asList(tracks)));
    }

    private void expectedOfflineTracks(Urn... tracks) {
        when(loadTracksWithValidPolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(Arrays.asList(tracks)));
    }

    private void expectDownloadFinished(EntityStateChangedEvent event, Urn urn) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getNextChangeSet().contains(OfflineProperty.DOWNLOADED_AT)).toBeTrue();
        expect(event.getChangeMap().keySet()).toContainExactly(urn);
    }

    private void expectDownloadsRemoved(EntityStateChangedEvent event, Urn... urns) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getChangeMap().keySet()).toContainExactly(urns);
        for (Urn urn : urns) {
            final PropertySet changeSet = event.getChangeMap().get(urn);
            expect(changeSet.contains(OfflineProperty.REMOVED_AT)).toBeTrue();
        }
    }

    private static class WriteResultStub extends WriteResult {
        private final boolean isStubbedSuccess;

        private WriteResultStub(boolean isStubbedSuccess) {
            this.isStubbedSuccess = isStubbedSuccess;
        }

        @Override
        public boolean success() {
            return isStubbedSuccess;
        }
    }

}
