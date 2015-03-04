package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.CountOfflineLikesCommand;
import com.soundcloud.android.offline.commands.LoadPendingDownloadsCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand;
import com.soundcloud.android.offline.commands.LoadTracksWithValidPoliciesCommand;
import com.soundcloud.android.offline.commands.RemoveOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.StoreOfflinePlaylistCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsPendingRemovalCommand;
import com.soundcloud.android.offline.commands.UpdateOfflineContentCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
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
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final List<Urn> LIKED_TRACKS = Arrays.asList(TRACK_URN);
    private static final int DOWNLOAD_LIKED_TRACKS_COUNT = 4;

    @Mock private CountOfflineLikesCommand offlineTrackCount;
    @Mock private StoreOfflinePlaylistCommand storeOfflinePlaylist;
    @Mock private RemoveOfflinePlaylistCommand removeOfflinePlaylist;
    @Mock private LoadPendingDownloadsCommand loadPendingDownloads;
    @Mock private UpdateOfflineContentCommand updateOfflineContent;
    @Mock private UpdateContentAsPendingRemovalCommand updateContentAsPendingRemoval;
    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private LoadTracksWithValidPoliciesCommand loadTracksWithValidPolicies;

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

        when(loadPendingDownloads.toObservable()).thenReturn(Observable.<List<DownloadRequest>>empty());
        when(updateOfflineContent.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));
        when(updateContentAsPendingRemoval.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(1)));
        when(offlineTrackCount.toObservable()).thenReturn(Observable.just(DOWNLOAD_LIKED_TRACKS_COUNT));

        operations = new OfflineContentOperations(
                loadTracksWithStalePolicies,
                updateOfflineContent,
                loadPendingDownloads,
                settingsStorage,
                eventBus,
                offlineTrackCount, storeOfflinePlaylist,
                removeOfflinePlaylist,
                policyOperations,
                loadTracksWithValidPolicies);
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toObservable()).thenReturn(Observable.<List<Urn>>just(new ArrayList<Urn>()));
        operations.loadDownloadRequests().subscribe(subscriber);

        verifyZeroInteractions(policyOperations);
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
        TestObserver<Boolean> observer = new TestObserver<>();
        when(storeOfflinePlaylist.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(123L)));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(true);
        expect(storeOfflinePlaylist.getInput()).toBe(playlistUrn);
    }

    @Test
    public void makePlaylistUnavailableOfflineRemovesOfflineContentPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        TestObserver<Boolean> observer = new TestObserver<>();
        ChangeResult deleteResult = mock(ChangeResult.class);
        when(deleteResult.success()).thenReturn(true);
        when(removeOfflinePlaylist.toObservable()).thenReturn(Observable.<WriteResult>just(deleteResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(true);
        expect(removeOfflinePlaylist.getInput()).toBe(playlistUrn);
    }

    @Test
    public void makePlaylistAvailableOfflineEmitsMarkedForOfflineEntityChangeEvent() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(storeOfflinePlaylist.toObservable()).thenReturn(Observable.<WriteResult>just(new InsertResult(123L)));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)).toEqual(true);
    }

    @Test
    public void makePlaylistUnavailableOfflineEmitsUnmarkedForOfflineEntityChangeEvent() throws Exception {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        ChangeResult deleteResult = mock(ChangeResult.class);
        when(deleteResult.success()).thenReturn(true);
        when(removeOfflinePlaylist.toObservable()).thenReturn(Observable.<WriteResult>just(deleteResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(new TestObserver<Boolean>());

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(eventPropertySet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE)).toEqual(false);
    }
}
