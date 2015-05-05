package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.ClearTrackDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadExpectedContentCommand;
import com.soundcloud.android.offline.commands.LoadOfflineContentUpdatesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.StoreDownloadUpdatesCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
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
    private static final Collection<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN_1);
    private static final WriteResult WRITE_RESULT_SUCCESS = new WriteResultStub(true);

    @Mock private StoreDownloadUpdatesCommand storeDownloadUpdatesCommand;
    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private OfflineSettingsStorage settingsStorage;
    @Mock private OfflinePlaylistStorage playlistStorage;
    @Mock private PolicyOperations policyOperations;
    @Mock private LoadExpectedContentCommand loadExpectedContentCommand;
    @Mock private LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    @Mock private OfflineTracksStorage offlineTracksStorage;
    @Mock private ChangeResult changeResult;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    @Mock private SecureFileStorage secureFileStorage;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;
    private TestSubscriber<Object> subscriber;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new TestSubscriber<>();

        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.just(LIKED_TRACKS));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>just(null));
        when(changeResult.success()).thenReturn(true);

        operations = new OfflineContentOperations(
                storeDownloadUpdatesCommand,
                loadTracksWithStalePolicies,
                clearTrackDownloadsCommand,
                settingsStorage,
                eventBus,
                playlistStorage,
                policyOperations,
                loadExpectedContentCommand,
                loadOfflineContentUpdatesCommand,
                offlineTracksStorage,
                secureFileStorage,
                Schedulers.immediate());
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(new ArrayList<Urn>()));
        operations.updateOfflineContentStalePolicies().subscribe(subscriber);

        verifyZeroInteractions(policyOperations);
    }

    @Test
    public void updateStalePoliciesRequestsPolicyUpdatesFromPolicyOperations() {
        final List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(tracks));

        operations.updateOfflineContentStalePolicies().subscribe();

        verify(policyOperations).updatePolicies(tracks);
    }

    @Test
    public void loadOfflineContentStoresContentUpdates() throws Exception {
        final Collection<DownloadRequest> downloadRequests = Collections.emptyList();
        final OfflineContentRequests offlineContentRequests = mock(OfflineContentRequests.class);

        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(Collections.<Urn>emptyList()));
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentRequests));

        operations.loadOfflineContentUpdates().subscribe(new TestObserver<OfflineContentRequests>());

        verify(storeDownloadUpdatesCommand).call(offlineContentRequests);
    }

    @Test
    public void loadOfflineContentReturnsContentUpdates() throws Exception {
        final Collection<DownloadRequest> downloadRequests = Collections.emptyList();
        final OfflineContentRequests offlineContentRequests = mock(OfflineContentRequests.class);

        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<Collection<Urn>>just(Collections.<Urn>emptyList()));
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentRequests));

        final TestObserver<OfflineContentRequests> observer = new TestObserver<>();
        operations.loadOfflineContentUpdates().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(offlineContentRequests);
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
        expect(eventPropertySet.get(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE)).toEqual(true);
    }

    @Test
    public void makePlaylistUnavailableOfflineEmitsUnmarkedForOfflineEntityChangeEvent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(playlistStorage.removeFromOfflinePlaylists(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE)).toEqual(false);
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

    @Test
    public void getLikedTracksDownloadStateReturnsNoOfflineWhenOfflineLikedTrackNotEnabled() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(false);

        final TestObserver<DownloadState> observer = new TestObserver<>();
        operations.getLikedTracksDownloadStateFromStorage().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.NO_OFFLINE);
    }

    @Test
    public void getLikedTracksDownloadStateReturnsRequestedWhenPendingRequestsExists() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(offlineTracksStorage.pendingLikedTracksUrns()).thenReturn(Observable.just(Arrays.asList(TRACK_URN_1)));
        final TestObserver<DownloadState> observer = new TestObserver<>();
        operations.getLikedTracksDownloadStateFromStorage().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.REQUESTED);
    }

    @Test
    public void getLikedTracksDownloadStateReturnsDownloadedWhenNoPendingRequest() {
        when(settingsStorage.isOfflineLikedTracksEnabled()).thenReturn(true);
        when(offlineTracksStorage.pendingLikedTracksUrns()).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        final TestObserver<DownloadState> observer = new TestObserver<>();
        operations.getLikedTracksDownloadStateFromStorage().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(DownloadState.DOWNLOADED);
    }

    @Test
    public void tryToUpdateAndLoadLastPoliciesUpdateTimeFetchThePolicies() {
        final TestObserver<Long> observer = new TestObserver<>();
        final TestObservables.MockObservable<Void> fetchingObservable = TestObservables.just(null);

        when(offlineTracksStorage.getLastPolicyUpdate()).thenReturn(Observable.<Long>empty());
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(fetchingObservable);

        operations.tryToUpdateAndLoadLastPoliciesUpdateTime().subscribe(observer);

        expect(fetchingObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void tryToUpdateAndLoadLastPoliciesUpdateTimeReturnsLastUpdateWhenFetchFailed() {
        final TestObserver<Long> observer = new TestObserver<>();
        when(offlineTracksStorage.getLastPolicyUpdate()).thenReturn(Observable.just(12344567L));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>error(new RuntimeException("Test exception")));

        operations.tryToUpdateAndLoadLastPoliciesUpdateTime().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(12344567L);
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    @Test
    public void tryToUpdateAndLoadLastPoliciesUpdateTimeReturnsLastUpdateWhenFetchSucceeded() {
        final TestObserver<Long> observer = new TestObserver<>();
        when(offlineTracksStorage.getLastPolicyUpdate()).thenReturn(Observable.just(12344567L));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>just(null));

        operations.tryToUpdateAndLoadLastPoliciesUpdateTime().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(12344567L);
        expect(observer.getOnCompletedEvents()).toNumber(1);
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
