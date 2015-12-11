package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OfflineContentOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Collection<Urn> LIKED_TRACKS = Collections.singletonList(TRACK_URN_1);

    @Mock private StoreDownloadUpdatesCommand storeDownloadUpdatesCommand;
    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private OfflineContentStorage offlineContentStorage;
    @Mock private PolicyOperations policyOperations;
    @Mock private LoadExpectedContentCommand loadExpectedContentCommand;
    @Mock private LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private FeatureOperations featureOperations;
    @Mock private ChangeResult changeResult;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;
    private TestSubscriber<Urn> subscriber;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new TestSubscriber<>();

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(LIKED_TRACKS));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>just(null));
        when(changeResult.success()).thenReturn(true);
        when(offlineContentStorage.storeOfflineLikesDisabled()).thenReturn(Observable.<ChangeResult>empty());

        operations = new OfflineContentOperations(
                storeDownloadUpdatesCommand,
                loadTracksWithStalePolicies,
                clearTrackDownloadsCommand,
                eventBus,
                offlineContentStorage,
                policyOperations,
                loadExpectedContentCommand,
                loadOfflineContentUpdatesCommand,
                featureOperations,
                trackDownloadsStorage,
                Schedulers.immediate());
    }

    @Test
    public void doesNotRequestPolicyUpdatesWhenAllPoliciesAreUpToDate() {
        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.<Collection<Urn>>just(new ArrayList<Urn>()));
        operations.updateOfflineContentStalePolicies().subscribe();

        verifyZeroInteractions(policyOperations);
    }

    @Test
    public void updateStalePoliciesRequestsPolicyUpdatesFromPolicyOperations() {
        final List<Urn> tracks = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(124L));
        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.<Collection<Urn>>just(tracks));

        operations.updateOfflineContentStalePolicies().subscribe();

        verify(policyOperations).updatePolicies(tracks);
    }

    @Test
    public void loadOfflineContentStoresContentUpdates() throws Exception {
        final Collection<DownloadRequest> downloadRequests = Collections.emptyList();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.<Collection<Urn>>just(Collections.<Urn>emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentUpdates));

        operations.loadOfflineContentUpdates().subscribe();

        verify(storeDownloadUpdatesCommand).call(offlineContentUpdates);
    }

    @Test
    public void loadOfflineContentReturnsContentUpdates() throws Exception {
        final TestSubscriber<OfflineContentUpdates> subscriber = new TestSubscriber<>();
        final Collection<DownloadRequest> downloadRequests = Collections.emptyList();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.<Collection<Urn>>just(Collections.<Urn>emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(loadExpectedContentCommand.toObservable(null)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentUpdates));

        operations.loadOfflineContentUpdates().subscribe(subscriber);

        subscriber.assertValue(offlineContentUpdates);
    }

    @Test
    public void makePlaylistAvailableOfflineStoresAsOfflineContent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.storeAsOfflinePlaylist(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe(subscriber);

        subscriber.assertCompleted();
    }

    @Test
    public void makePlaylistUnavailableOfflineRemovesOfflineContentPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removeFromOfflinePlaylists(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(subscriber);

        subscriber.assertCompleted();
    }

    @Test
    public void makePlaylistAvailableOfflineEmitsMarkedForOfflineEntityChangeEvent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.storeAsOfflinePlaylist(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe();

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        assertThat(eventPropertySet.get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(eventPropertySet.get(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE)).isEqualTo(true);
    }

    @Test
    public void makePlaylistUnavailableOfflineEmitsUnmarkedForOfflineEntityChangeEvent() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removeFromOfflinePlaylists(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        PropertySet eventPropertySet = event.getChangeMap().get(playlistUrn);
        assertThat(eventPropertySet.get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(eventPropertySet.get(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE)).isEqualTo(false);
    }

    @Test
    public void clearOfflineContentClearsTrackDownloads() {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearTrackDownloadsCommand.toObservable(null)).thenReturn(Observable.just(removed));

        final TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        operations.clearOfflineContent().subscribe(subscriber);

        subscriber.assertValue(removed);
    }

    @Test
    public void clearOfflineContentPublishesOfflineContentRemovedEvent() {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearTrackDownloadsCommand.toObservable(null)).thenReturn(Observable.just(removed));

        operations.clearOfflineContent().subscribe();

        CurrentDownloadEvent publishedEvent = eventBus.lastEventOn(EventQueue.CURRENT_DOWNLOAD);
        assertThat(publishedEvent.kind).isEqualTo(OfflineState.NO_OFFLINE);
        assertThat(publishedEvent.entities).contains(Urn.forTrack(123), Urn.forPlaylist(1234));
    }

    @Test
    public void getLikedTracksOfflineStateReturnsNoOfflineWhenOfflineLikedTrackAreDisabled() {
        final TestSubscriber<OfflineState> subscriber = new TestSubscriber<>();
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(false));

        operations.getLikedTracksOfflineStateFromStorage().subscribe(subscriber);

        subscriber.assertValue(OfflineState.NO_OFFLINE);
    }

    @Test
    public void getLikedTracksOfflineStateReturnsStateFromStorageWhenOfflineLikedTracksAreEnabled() {
        final TestSubscriber<OfflineState> subscriber = new TestSubscriber<>();
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(trackDownloadsStorage.getLikesOfflineState()).thenReturn(Observable.just(OfflineState.REQUESTED));

        operations.getLikedTracksOfflineStateFromStorage().subscribe(subscriber);

        subscriber.assertValue(OfflineState.REQUESTED);
    }

    @Test
    public void loadOfflineContentUpdatesDoesNotFailWhenPoliciesFailedToUpdate() {
        final TestSubscriber<OfflineContentUpdates> subscriber = new TestSubscriber<>();

        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>error(new RuntimeException("Test exception")));
        operations.loadOfflineContentUpdates().subscribe(subscriber);

        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

}
