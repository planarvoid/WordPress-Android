package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.CountOfflineLikesCommand;
import com.soundcloud.android.offline.commands.LoadDownloadedCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsRequestsCommand;
import com.soundcloud.android.offline.commands.LoadPendingRemovalsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.StoreDownloadedCommand;
import com.soundcloud.android.offline.commands.StorePendingDownloadsCommand;
import com.soundcloud.android.offline.commands.StorePendingRemovalsCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackProperty;
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
    private static final int DOWNLOAD_LIKED_TRACKS_COUNT = 4;

    private static final WriteResult WRITE_RESULT_SUCCESS = new WriteResultStub(true);

    @Mock private StorePendingDownloadsCommand storePendingDownloadsCommand;
    @Mock private StorePendingRemovalsCommand storePendingRemovalsCommand;
    @Mock private StoreDownloadedCommand storeDownloadedCommand;
    @Mock private CountOfflineLikesCommand offlineTrackCount;

    @Mock private LoadPendingDownloadsRequestsCommand loadPendingDownloadsRequestsCommand;
    @Mock private LoadPendingDownloadsCommand loadPendingDownloadsCommand;
    @Mock private LoadPendingRemovalsCommand loadPendingRemovalsCommand;
    @Mock private LoadDownloadedCommand loadDownloadedCommand;

    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies;

    @Mock private ChangeResult changeResult;

    @Mock private OfflinePlaylistStorage playlistStorage;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private PolicyOperations policyOperations;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;
    private TestSubscriber<Object> subscriber;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new TestSubscriber<>();

        when(loadTracksWithValidPolicies.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(policyOperations.fetchAndStorePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>just(null));

        when(loadPendingDownloadsRequestsCommand.toObservable()).thenReturn(Observable.<List<DownloadRequest>>empty());
        when(loadPendingDownloadsCommand.toObservable()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(loadPendingRemovalsCommand.toObservable()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(loadDownloadedCommand.toObservable()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(offlineTrackCount.toObservable()).thenReturn(Observable.just(DOWNLOAD_LIKED_TRACKS_COUNT));
        when(changeResult.success()).thenReturn(true);

        operations = new OfflineContentOperations(
                loadDownloadedCommand,
                storePendingDownloadsCommand,
                storePendingRemovalsCommand,
                storeDownloadedCommand,
                loadTracksWithStalePolicies,
                loadPendingRemovalsCommand,
                loadPendingDownloadsRequestsCommand,
                loadPendingDownloadsCommand,
                settingsStorage,
                eventBus,
                offlineTrackCount,
                playlistStorage,
                policyOperations,
                loadTracksWithValidPolicies);
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
    public void loadDownloadRequestsLoadsUpdatedPendingDownloads() {
        final TestObserver<List<DownloadRequest>> observer = new TestObserver<>();
        final List<DownloadRequest> requests = Arrays.asList(new DownloadRequest(Urn.forTrack(123L), "http://"));
        when(loadPendingDownloadsRequestsCommand.toObservable()).thenReturn(Observable.just(requests));

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
    public void callSyncFinishedOnOfflineIdleEvent() {
        TestObserver<Integer> observer = new TestObserver<>();
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.idle());

        operations.onFinishedOrIdleWithDownloadedCount().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DOWNLOAD_LIKED_TRACKS_COUNT);
    }

    @Test
    public void callSyncFinishedOnOfflineSyncStopEvent() {
        TestObserver<Integer> observer = new TestObserver<>();
        eventBus.publish(EventQueue.OFFLINE_CONTENT, OfflineContentEvent.stop());

        operations.onFinishedOrIdleWithDownloadedCount().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DOWNLOAD_LIKED_TRACKS_COUNT);
    }

    @Test
    public void callSyncStartedOnOfflineSyncStartEvent() {
        TestObserver<OfflineContentEvent> observer = new TestObserver<>();
        OfflineContentEvent event = OfflineContentEvent.start();
        eventBus.publish(EventQueue.OFFLINE_CONTENT, event);

        operations.onStarted().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(event);
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
    public void makePlaylistAvailableOfflineEmitsMarkedForOfflineEntityChangeEvent() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.storeAsOfflinePlaylist(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)).toEqual(true);
    }

    @Test
    public void makePlaylistUnavailableOfflineEmitsUnmarkedForOfflineEntityChangeEvent() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.removeFromOfflinePlaylists(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)).toEqual(false);
    }

    private void actualPendingRemovals(Urn... tracks) {
        when(loadPendingRemovalsCommand.toObservable()).thenReturn(Observable.just(Arrays.asList(tracks)));
    }

    private void actualPendingDownloads(Urn... tracks) {
        when(loadPendingDownloadsCommand.toObservable()).thenReturn(Observable.just(Arrays.asList(tracks)));
    }

    private void actualDownloadedTracks(Urn... tracks) {
        when(loadDownloadedCommand.toObservable()).thenReturn(Observable.just(Arrays.asList(tracks)));
    }

    private void expectedOfflineTracks(Urn... tracks) {
        when(loadTracksWithValidPolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(Arrays.asList(tracks)));
    }

    private void expectDownloadFinished(EntityStateChangedEvent event, Urn urn) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getNextChangeSet().get(TrackProperty.OFFLINE_DOWNLOADING)).toBeFalse();
        expect(event.getNextChangeSet().contains(TrackProperty.OFFLINE_DOWNLOADED_AT)).toBeTrue();
        expect(event.getChangeMap().keySet()).toContainExactly(urn);
    }

    private void expectDownloadsRemoved(EntityStateChangedEvent event, Urn... urns) {
        expect(event.getKind()).toBe(EntityStateChangedEvent.DOWNLOAD);
        expect(event.getChangeMap().keySet()).toContainExactly(urns);
        for (Urn urn : urns) {
            final PropertySet changeSet = event.getChangeMap().get(urn);
            expect(changeSet.get(TrackProperty.OFFLINE_DOWNLOADING)).toBeFalse();
            expect(changeSet.contains(TrackProperty.OFFLINE_REMOVED_AT)).toBeTrue();
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
