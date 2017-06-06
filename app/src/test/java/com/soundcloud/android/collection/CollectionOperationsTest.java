package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.configuration.experiments.PlaylistAndAlbumsPreviewsExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollectionOperationsTest extends AndroidUnitTest {

    private CollectionOperations operations;

    @Mock private SyncInitiatorBridge syncInitiator;
    @Mock private MyPlaylistsOperations myPlaylistsOperations;
    @Mock private LoadLikedTrackPreviewsCommand loadLikedTrackPreviewsCommand;
    @Mock private StationsOperations stationsOperations;
    @Mock private CollectionOptionsStorage collectionOptionsStorage;
    @Mock private OfflineStateOperations offlineStateOperations;
    @Mock private PlayHistoryOperations playHistoryOperations;
    @Mock private RecentlyPlayedOperations recentlyPlayedOperations;
    @Mock private PlaylistAndAlbumsPreviewsExperiment playlistAndAlbumsPreviewsExperiment;

    private List<LikedTrackPreview> trackPreviews = Arrays.asList(
            LikedTrackPreview.create(Urn.forTrack(1L), "http://image/url1"),
            LikedTrackPreview.create(Urn.forTrack(2L), "http://image/url2")
    );

    private List<TrackItem> playHistory = Arrays.asList(
            ModelFixtures.trackItem(),
            ModelFixtures.trackItem()
    );

    private List<RecentlyPlayedPlayableItem> recentlyPlayed = Arrays.asList(
            new RecentlyPlayedPlayableItem(Urn.forPlaylist(123L),
                                           Optional.absent(),
                                           "title 1",
                                           10,
                                           false,
                                           Optional.absent(),
                                           false,
                                           false,
                                           1000),
            new RecentlyPlayedPlayableItem(Urn.forTrackStation(234L),
                                           Optional.absent(),
                                           "title 2",
                                           0,
                                           false,
                                           Optional.absent(),
                                           false,
                                           false,
                                           1000)
    );


    private final TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        operations = new CollectionOperations(
                eventBus,
                Schedulers.trampoline(),
                loadLikedTrackPreviewsCommand,
                syncInitiator,
                stationsOperations,
                collectionOptionsStorage,
                offlineStateOperations,
                playHistoryOperations,
                recentlyPlayedOperations,
                myPlaylistsOperations,
                ModelFixtures.entityItemCreator(),
                playlistAndAlbumsPreviewsExperiment);

        when(offlineStateOperations.loadLikedTracksOfflineState()).thenReturn(Observable.just(OfflineState.NOT_OFFLINE));
        when(loadLikedTrackPreviewsCommand.toObservable(null)).thenReturn(rx.Observable.just(trackPreviews));
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Single.just(true));
        when(stationsOperations.collection(StationsCollectionsTypes.LIKED))
                .thenReturn(Observable.just(StationFixtures.getStation(Urn.forTrackStation(123L))));
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL))
                .thenReturn(Maybe.just(singletonList(Playlist.from(ModelFixtures.create(ApiPlaylist.class)))));
        when(playHistoryOperations.playHistory(3)).thenReturn(Observable.just(playHistory));
        when(recentlyPlayedOperations.recentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS))
                .thenReturn(Observable.just(recentlyPlayed));
    }

    @Test
    public void onCollectionChangedWhenTrackLikeEventFires() {
        TestObserver testObserver = operations.onCollectionChanged().test();

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forTrack(1), true, 1));

        testObserver.assertValueCount(1);
    }

    @Test
    public void onCollectionChangedWhenTrackUnlikeEventFires() {
        TestObserver testObserver = operations.onCollectionChanged().test();

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forTrack(1), false, 1));

        testObserver.assertValueCount(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistLikeEventFires() {
        TestObserver testObserver = operations.onCollectionChanged().test();

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forPlaylist(1), true, 1));

        testObserver.assertValueCount(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistUnlikeEventFires() {
        TestObserver testObserver = operations.onCollectionChanged().test();

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forPlaylist(1), true, 1));

        testObserver.assertValueCount(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistCreatedEventFires() {
        TestObserver testObserver = operations.onCollectionChanged().test();

        final Urn localPlaylist = Urn.newLocalPlaylist();
        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityCreated(localPlaylist));

        testObserver.assertValueCount(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistPushedEventFires() {
        TestObserver testObserver = operations.onCollectionChanged().test();

        final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistPushedToServer(Urn.forPlaylist(4), Playlist.from(apiPlaylist)));

        testObserver.assertValueCount(1);
    }

    @Test
    public void collectionsShouldReturnAnErrorWhenAllCollectionsFailedToLoad() {
        final RuntimeException exception = new RuntimeException("Test");

        when(loadLikedTrackPreviewsCommand.toObservable(null)).thenReturn(rx.Observable.error(exception));
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL)).thenReturn(Maybe.error(exception));

        when(stationsOperations.collection(StationsCollectionsTypes.LIKED)).thenReturn(Observable.error(
                exception));

        when(playHistoryOperations.playHistory(3)).thenReturn(
                Observable.error(exception));

        when(recentlyPlayedOperations.recentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS)).thenReturn(
                Observable.error(exception));

        MyCollection collection = operations.collections().test()
                                              .assertValueCount(1)
                                              .values().get(0);
        assertThat(collection.getLikes().trackPreviews()).isEqualTo(Collections.emptyList());
        assertThat(collection.getPlaylistAndAlbums().get()).isEqualTo(Collections.emptyList());
        assertThat(collection.getStations()).isEqualTo(Collections.emptyList());
        assertThat(collection.hasError()).isTrue();
    }

    @Test
    public void collectionsShouldReturnAValueWhenAtLeastOneCollectionSucceededToLoad() {
        final RuntimeException exception = new RuntimeException("Test");

        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL))
                .thenReturn(Maybe.error(exception));
        when(stationsOperations.collection(StationsCollectionsTypes.LIKED))
                .thenReturn(Observable.error(exception));

        MyCollection collection = operations.collections().test()
                                              .assertValueCount(1)
                                              .values().get(0);
        assertThat(collection.getLikes().trackPreviews()).isEqualTo(trackPreviews);
        assertThat(collection.getPlaylistAndAlbums().get()).isEqualTo(Collections.emptyList());
        assertThat(collection.getStations()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void collectionsShouldSeparatePlaylistsAndAlbumsIfUserIsIntoABTest() {
        when(playlistAndAlbumsPreviewsExperiment.isEnabled()).thenReturn(true);

        Playlist playlist1 = ModelFixtures.playlistBuilder(Playlist.from(ModelFixtures.create(ApiPlaylist.class))).isAlbum(false).build();
        Playlist playlist2 = ModelFixtures.playlistBuilder(Playlist.from(ModelFixtures.create(ApiPlaylist.class))).isAlbum(false).build();
        Playlist album1 = ModelFixtures.playlistBuilder(Playlist.from(ModelFixtures.create(ApiPlaylist.class))).isAlbum(true).build();
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL))
                .thenReturn(Maybe.just(Arrays.asList(playlist1, playlist2, album1)));

        MyCollection collection = operations.collections().test()
                                              .assertValueCount(1)
                                              .values().get(0);

        assertThat(collection.getPlaylistAndAlbums().isPresent()).isFalse();
        assertThat(collection.getPlaylists().isPresent()).isTrue();
        assertThat(collection.getAlbums().isPresent()).isTrue();
        assertThat(collection.getPlaylists().get().size()).isEqualTo(2);
        assertThat(collection.getAlbums().get().size()).isEqualTo(1);
    }

    @Test
    public void collectionsShouldKeepPlaylistsAndAlbumsTogetherIfUserIsNotIntoAbTest() {
        when(playlistAndAlbumsPreviewsExperiment.isEnabled()).thenReturn(false);

        Playlist playlist1 = ModelFixtures.playlistBuilder(Playlist.from(ModelFixtures.create(ApiPlaylist.class))).isAlbum(false).build();
        Playlist playlist2 = ModelFixtures.playlistBuilder(Playlist.from(ModelFixtures.create(ApiPlaylist.class))).isAlbum(false).build();
        Playlist album1 = ModelFixtures.playlistBuilder(Playlist.from(ModelFixtures.create(ApiPlaylist.class))).isAlbum(true).build();
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL))
                .thenReturn(Maybe.just(Arrays.asList(playlist1, playlist2, album1)));

        MyCollection collection = operations.collections().test()
                                              .assertValueCount(1)
                                              .values().get(0);
        assertThat(collection.getPlaylistAndAlbums().isPresent()).isTrue();
        assertThat(collection.getPlaylists().isPresent()).isFalse();
        assertThat(collection.getAlbums().isPresent()).isFalse();
        assertThat(collection.getPlaylistAndAlbums().get().size()).isEqualTo(3);
    }

    @Test
    public void collectionsSyncsBeforeReturningIfNeverSyncedBefore() throws Exception {
        CompletableSubject subject = CompletableSubject.create();

        when(syncInitiator.refreshLikedTracks()).thenReturn(subject);
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Single.just(false));

        TestObserver<MyCollection> testObserver = operations.collections().test();

        testObserver.assertNoValues();

        subject.onComplete();

        List<LikedTrackPreview> previews = testObserver.assertValueCount(1)
                                                                 .values().get(0)
                                                                 .getLikes()
                                                                 .trackPreviews();
        assertThat(previews).isEqualTo(trackPreviews);
    }

}
