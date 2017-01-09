package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

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
    @Mock private FeatureFlags featureFlags;

    private TestSubscriber<MyCollection> subscriber = new TestSubscriber<>();
    private List<LikedTrackPreview> trackPreviews = Arrays.asList(
            LikedTrackPreview.create(Urn.forTrack(1L), "http://image/url1"),
            LikedTrackPreview.create(Urn.forTrack(2L), "http://image/url2")
    );

    private List<TrackItem> playHistory = Arrays.asList(
            TrackItem.from(ModelFixtures.create(ApiTrack.class)),
            TrackItem.from(ModelFixtures.create(ApiTrack.class))
    );

    private List<RecentlyPlayedPlayableItem> recentlyPlayed = Arrays.asList(
            new RecentlyPlayedPlayableItem(Urn.forPlaylist(123L),
                                           Optional.<String>absent(),
                                           "title 1",
                                           10,
                                           false,
                                           Optional.<OfflineState>absent(),
                                           1000),
            new RecentlyPlayedPlayableItem(Urn.forTrackStation(234L),
                                           Optional.<String>absent(),
                                           "title 2",
                                           0,
                                           false,
                                           Optional.<OfflineState>absent(),
                                           1000)
    );


    private TestEventBus eventBus;
    private TestSubscriber<Object> collectionChangedSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        operations = new CollectionOperations(
                eventBus,
                Schedulers.immediate(),
                loadLikedTrackPreviewsCommand,
                syncInitiator,
                stationsOperations,
                collectionOptionsStorage,
                offlineStateOperations,
                playHistoryOperations,
                recentlyPlayedOperations,
                myPlaylistsOperations
        );

        when(offlineStateOperations.loadLikedTracksOfflineState()).thenReturn(Observable.just(OfflineState.NOT_OFFLINE));
        when(loadLikedTrackPreviewsCommand.toObservable(null)).thenReturn(Observable.just(trackPreviews));
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Observable.just(true));
        when(stationsOperations.collection(StationsCollectionsTypes.LIKED))
                .thenReturn(Observable.just(StationFixtures.getStation(Urn.forTrackStation(123L))));
        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL))
                .thenReturn(Observable.just(singletonList(PlaylistItem.from(ModelFixtures.create(ApiPlaylist.class)))));
        when(playHistoryOperations.playHistory(3)).thenReturn(Observable.just(playHistory));
        when(recentlyPlayedOperations.recentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS))
                .thenReturn(Observable.just(recentlyPlayed));
    }

    @Test
    public void onCollectionChangedWhenTrackLikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forTrack(1), true, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenTrackUnlikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forTrack(1), false, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistLikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forPlaylist(1), true, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistUnlikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(Urn.forPlaylist(1), true, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistCreatedEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        final Urn localPlaylist = Urn.newLocalPlaylist();
        eventBus.publish(EventQueue.URN_STATE_CHANGED, UrnStateChangedEvent.fromEntityCreated(localPlaylist));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistPushedEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, playlist.toPushedEvent(Urn.forPlaylist(4)));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void collectionsShouldReturnAnErrorWhenAllCollectionsFailedToLoad() {
        final RuntimeException exception = new RuntimeException("Test");

        when(loadLikedTrackPreviewsCommand.toObservable(null)).thenReturn(Observable.<List<LikedTrackPreview>>error(
                exception));

        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL)).thenReturn(
                Observable.<List<PlaylistItem>>error(exception));

        when(stationsOperations.collection(StationsCollectionsTypes.LIKED)).thenReturn(Observable.<StationRecord>error(
                exception));

        when(playHistoryOperations.playHistory(3)).thenReturn(
                Observable.<List<TrackItem>>error(exception));

        when(recentlyPlayedOperations.recentlyPlayed(RecentlyPlayedOperations.CAROUSEL_ITEMS)).thenReturn(
                Observable.<List<RecentlyPlayedPlayableItem>>error(exception));

        operations.collections().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        MyCollection collection = subscriber.getOnNextEvents().get(0);
        assertThat(collection.getLikes().trackPreviews()).isEqualTo(Collections.emptyList());
        assertThat(collection.getPlaylistItems()).isEqualTo(Collections.emptyList());
        assertThat(collection.getStations()).isEqualTo(Collections.emptyList());
        assertThat(collection.hasError()).isTrue();
    }

    @Test
    public void collectionsShouldReturnAValueWhenAtLeastOneCollectionSucceededToLoad() {
        final RuntimeException exception = new RuntimeException("Test");

        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.SHOW_ALL))
                .thenReturn(Observable.<List<PlaylistItem>>error(exception));
        when(stationsOperations.collection(StationsCollectionsTypes.LIKED))
                .thenReturn(Observable.<StationRecord>error(exception));

        operations.collections().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        MyCollection collection = subscriber.getOnNextEvents().get(0);
        assertThat(collection.getLikes().trackPreviews()).isEqualTo(trackPreviews);
        assertThat(collection.getPlaylistItems()).isEqualTo(Collections.emptyList());
        assertThat(collection.getStations()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void collectionsSyncsBeforeReturningIfNeverSyncedBefore() throws Exception {
        final PublishSubject<Void> subject = PublishSubject.create();

        when(syncInitiator.refreshLikedTracks()).thenReturn(subject);
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Observable.just(false));

        operations.collections().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(null);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().trackPreviews()).isEqualTo(trackPreviews);
    }

}
