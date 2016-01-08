package com.soundcloud.android.offline;

import static com.soundcloud.android.playlists.PlaylistWithTracksTests.createPlaylistWithTracks;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.PlaylistWithTracks;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OfflineContentOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Collection<Urn> LIKED_TRACKS = singletonList(TRACK_URN_1);

    @Mock private StoreDownloadUpdatesCommand storeDownloadUpdatesCommand;
    @Mock private LoadTracksWithStalePoliciesCommand loadTracksWithStalePolicies;
    @Mock private OfflineContentStorage offlineContentStorage;
    @Mock private PolicyOperations policyOperations;
    @Mock private LoadExpectedContentCommand loadExpectedContentCommand;
    @Mock private LoadOfflineContentUpdatesCommand loadOfflineContentUpdatesCommand;
    @Mock private OfflineServiceInitiator serviceInitiator;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private CollectionOperations collectionOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private ChangeResult changeResult;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    @Mock private Action1<Object> startServiceAction;

    private OfflineContentOperations operations;
    private TestEventBus eventBus;
    private TestSubscriber<ChangeResult> subscriber;
    private List<PlaylistWithTracks> playlistsWithTracks;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new TestSubscriber<>();

        when(serviceInitiator.action1Start()).thenReturn(startServiceAction);
        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.just(LIKED_TRACKS));
        when(policyOperations.updatePolicies(anyListOf(Urn.class))).thenReturn(Observable.<Void>just(null));
        when(changeResult.success()).thenReturn(true);
        when(offlineContentStorage.deleteLikedTrackCollection()).thenReturn(Observable.<ChangeResult>empty());

        final Urn offlinePlaylist = Urn.forPlaylist(112233L);
        final List<Urn> offlinePlaylists = singletonList(offlinePlaylist);
        playlistsWithTracks = singletonList(createPlaylistWithTracks(offlinePlaylist));
        when(offlineContentStorage.loadOfflinePlaylists()).thenReturn(Observable.just(offlinePlaylists));
        when(playlistOperations.playlists(offlinePlaylists)).thenReturn(Observable.just(playlistsWithTracks));

        operations = new OfflineContentOperations(
                storeDownloadUpdatesCommand,
                loadTracksWithStalePolicies,
                clearTrackDownloadsCommand,
                eventBus,
                offlineContentStorage,
                policyOperations,
                loadExpectedContentCommand,
                loadOfflineContentUpdatesCommand,
                serviceInitiator,
                featureOperations,
                trackDownloadsStorage,
                playlistOperations,
                collectionOperations,
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
    public void loadOfflineContentUpdatesSyncThePlaylistsTracks() {
        final PublishSubject<List<PlaylistWithTracks>> subject = PublishSubject.create();
        final List<Urn> offlineCollections = singletonList(Urn.forPlaylist(123L));

        when(offlineContentStorage.loadOfflinePlaylists()).thenReturn(Observable.just(offlineCollections));
        when(playlistOperations.playlists(offlineCollections)).thenReturn(subject);

        operations.loadOfflineContentUpdates().subscribe();

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void loadOfflineContentStoresContentUpdates() throws Exception {
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.<Collection<Urn>>just(Collections.<Urn>emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(loadExpectedContentCommand.toObservable(playlistsWithTracks)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentUpdates));

        operations.loadOfflineContentUpdates().subscribe();

        verify(storeDownloadUpdatesCommand).call(offlineContentUpdates);
    }

    @Test
    public void loadOfflineContentReturnsContentUpdates() throws Exception {
        final TestSubscriber<OfflineContentUpdates> subscriber = new TestSubscriber<>();
        final ExpectedOfflineContent downloadRequests = getExpectedOfflineContent();
        final OfflineContentUpdates offlineContentUpdates = mock(OfflineContentUpdates.class);

        when(loadTracksWithStalePolicies.toObservable(null)).thenReturn(Observable.<Collection<Urn>>just(Collections.<Urn>emptyList()));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));
        when(loadExpectedContentCommand.toObservable(playlistsWithTracks)).thenReturn(Observable.just(downloadRequests));
        when(loadOfflineContentUpdatesCommand.toObservable(downloadRequests)).thenReturn(Observable.just(offlineContentUpdates));

        operations.loadOfflineContentUpdates().subscribe(subscriber);

        subscriber.assertValue(offlineContentUpdates);
    }

    @Test
    public void setPlaylistAvailableOfflineStartsService() {
        final Urn playlist1 = Urn.forPlaylist(123L);
        final Urn playlist2 = Urn.forPlaylist(456L);
        final List<Urn> expectedOfflinePlaylists = newArrayList(playlist1, playlist2);
        when(offlineContentStorage.storeLikedTrackCollection()).thenReturn(Observable.just(new ChangeResult(1)));
        when(collectionOperations.myPlaylists()).thenReturn(Observable.just(Arrays.asList(createPlaylistItem(playlist1), createPlaylistItem(playlist2))));
        when(offlineContentStorage.addOfflinePlaylists(expectedOfflinePlaylists)).thenReturn(Observable.just(new TxnResult()));

        operations.enableOfflineCollection().subscribe();

        verify(startServiceAction).call(anyObject());
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
        when(offlineContentStorage.removePlaylistFromOffline(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe(subscriber);

        subscriber.assertCompleted();
    }

    @Test
    public void makePlaylistAvailableOfflineStartsService() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.storeAsOfflinePlaylist(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistAvailableOffline(playlistUrn).subscribe();

        verify(startServiceAction).call(anyObject());
    }

    @Test
    public void makePlaylistUnavailableOfflineStartsService() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(offlineContentStorage.removePlaylistFromOffline(playlistUrn)).thenReturn(Observable.just(changeResult));

        operations.makePlaylistUnavailableOffline(playlistUrn).subscribe();

        verify(startServiceAction).call(anyObject());
    }

    @Test
    public void clearOfflineContentStartsService() {
        List<Urn> removed = Arrays.asList(Urn.forTrack(123), Urn.forPlaylist(1234));
        when(clearTrackDownloadsCommand.toObservable(null)).thenReturn(Observable.just(removed));
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(true));

        operations.clearOfflineContent().subscribe();

        verify(startServiceAction).call(anyObject());
    }

    @Test
    public void getLikedTracksOfflineStateReturnsNoOfflineWhenOfflineLikedTrackAreDisabled() {
        final TestSubscriber<OfflineState> subscriber = new TestSubscriber<>();
        when(offlineContentStorage.isOfflineLikesEnabled()).thenReturn(Observable.just(false));

        operations.getLikedTracksOfflineStateFromStorage().subscribe(subscriber);

        subscriber.assertValue(OfflineState.NOT_OFFLINE);
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

    private ExpectedOfflineContent getExpectedOfflineContent() {
        return new ExpectedOfflineContent(Collections.<DownloadRequest>emptyList(), Collections.<PlaylistWithTracks>emptyList(), false, Collections.<Urn>emptyList());
    }

    public PlaylistItem createPlaylistItem(Urn urn) {
        return new PlaylistItem(PropertySet.from(PlayableProperty.URN.bind(urn)));
    }
}
