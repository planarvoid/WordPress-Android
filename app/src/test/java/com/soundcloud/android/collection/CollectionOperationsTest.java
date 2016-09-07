package com.soundcloud.android.collection;

import static com.soundcloud.android.collection.CollectionOperations.PLAYLIST_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
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
import java.util.Date;
import java.util.List;

public class CollectionOperationsTest extends AndroidUnitTest {

    private CollectionOperations operations;

    @Mock private SyncInitiatorBridge syncInitiator;
    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private PlaylistLikesStorage playlistLikeStorage;
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

    private PropertySet postedPlaylist1;
    private PropertySet postedPlaylist2;
    private PropertySet likedPlaylist1;
    private PropertySet likedPlaylist2;
    private PropertySet likedPlaylist3Offline;
    private TestEventBus eventBus;
    private TestSubscriber<Object> collectionChangedSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        operations = new CollectionOperations(
                eventBus,
                Schedulers.immediate(),
                playlistPostStorage,
                playlistLikeStorage,
                loadLikedTrackPreviewsCommand,
                syncInitiator,
                stationsOperations,
                collectionOptionsStorage,
                offlineStateOperations,
                playHistoryOperations,
                recentlyPlayedOperations, featureFlags);

        when(offlineStateOperations.loadLikedTracksOfflineState()).thenReturn(Observable.just(OfflineState.NOT_OFFLINE));
        when(loadLikedTrackPreviewsCommand.toObservable(null)).thenReturn(Observable.just(trackPreviews));
        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Observable.just(true));
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Observable.just(true));
        when(stationsOperations.collection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.just(StationFixtures.getStation(
                Urn.forTrackStation(123L))));
        when(stationsOperations.sync()).thenReturn(Observable.just(SyncJobResult.success("stations sync", true)));
        postedPlaylist1 = getPostedPlaylist(Urn.forPlaylist(1L), new Date(1), "apple");
        postedPlaylist2 = getPostedPlaylist(Urn.forPlaylist(2L), new Date(3), "banana");
        likedPlaylist1 = getLikedPlaylist(Urn.forPlaylist(3L), new Date(2), "cherry");
        likedPlaylist2 = getLikedPlaylist(Urn.forPlaylist(4L), new Date(4), "doughnut");
        likedPlaylist3Offline = getLikedPlaylistOffline(Urn.forPlaylist(5L),
                                                        new Date(5),
                                                        "eclair",
                                                        OfflineState.DOWNLOADED);

        final Observable<List<PropertySet>> postedPlaylists = Observable.just(Arrays.asList(postedPlaylist1,
                                                                                            postedPlaylist2));
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(postedPlaylists);

        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1,
                                                                                           likedPlaylist2,
                                                                                           likedPlaylist3Offline));
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(likedPlaylists);
    }

    private PropertySet getLikedPlaylistOffline(Urn urn, Date date, String title, OfflineState state) {
        return getLikedPlaylist(urn, date, title)
                .put(OfflineProperty.OFFLINE_STATE, state);
    }

    @Test
    public void onCollectionChangedWhenTrackLikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(1), true, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenTrackUnlikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(1), false, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistLikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(Urn.forPlaylist(1), true, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistUnlikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromLike(Urn.forPlaylist(1), false, 1));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistCreatedEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        final Urn localPlaylist = Urn.newLocalPlaylist();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromEntityCreated(localPlaylist));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedWhenPlaylistPushedEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        final PropertySet playlist = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromPlaylistPushedToServer(Urn.forPlaylist(4), playlist));

        assertThat(collectionChangedSubscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void collectionsShouldReturnAnErrorWhenAllCollectionsFailedToLoad() {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();

        final RuntimeException exception = new RuntimeException("Test");
        when(loadLikedTrackPreviewsCommand.toObservable(null)).thenReturn(Observable.<List<LikedTrackPreview>>error(
                exception));
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT,
                                                     Long.MAX_VALUE)).thenReturn(Observable.<List<PropertySet>>error(
                exception));
        when(stationsOperations.collection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.<StationRecord>error(
                exception));

        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        MyCollection collection = subscriber.getOnNextEvents().get(0);
        assertThat(collection.getLikes().getTrackPreviews()).isEqualTo(Collections.emptyList());
        assertThat(collection.getPlaylistItems()).isEqualTo(Collections.emptyList());
        assertThat(collection.getStations()).isEqualTo(Collections.emptyList());
        assertThat(collection.hasError()).isTrue();
    }

    @Test
    public void collectionsShouldReturnAValueWhenAtLeastOneCollectionSucceededToLoad() {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();

        final RuntimeException exception = new RuntimeException("Test");
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT,
                                                     Long.MAX_VALUE)).thenReturn(Observable.<List<PropertySet>>error(
                exception));
        when(stationsOperations.collection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.<StationRecord>error(
                exception));

        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        MyCollection collection = subscriber.getOnNextEvents().get(0);
        assertThat(collection.getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(collection.getPlaylistItems()).isEqualTo(Collections.emptyList());
        assertThat(collection.getStations()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void collectionsReturnsPostedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsLikedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(likedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsUniquePostedAndLikedPlaylists() throws Exception {
        PropertySet likedPlaylist3 = getLikedPlaylist(postedPlaylist1.get(EntityProperty.URN), new Date(6), "pepper");
        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1,
                                                                                           likedPlaylist2,
                                                                                           likedPlaylist3,
                                                                                           likedPlaylist3Offline));
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(likedPlaylists);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3),
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1)
        ));
    }

    @Test
    public void collectionsWithoutFiltersReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(false).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsPostedAndLikedPlaylistsSortedByTitle() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true)
                                                         .sortByTitle(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(postedPlaylist1),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(likedPlaylist3Offline)
        ));
    }

    @Test
    public void collectionsReturnsOfflineOnly() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showOfflineOnly(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Collections.singletonList(
                PlaylistItem.from(likedPlaylist3Offline)));
    }

    @Test
    public void collectionsSyncsBeforeReturningIfNeverSyncedBefore() throws Exception {
        final PublishSubject<Void> subject = PublishSubject.create();
        when(syncInitiator.refreshLikedTracks()).thenReturn(subject);
        when(syncInitiator.refreshMyPostedAndLikedPlaylists()).thenReturn(subject);

        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Observable.just(false));
        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Observable.just(false));

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(null);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void updatedCollectionsReturnsMyCollectionsAfterSync() throws Exception {
        final PublishSubject<Void> subject = PublishSubject.create();
        when(syncInitiator.refreshLikedTracks()).thenReturn(subject);
        when(syncInitiator.refreshMyPostedAndLikedPlaylists()).thenReturn(subject);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.updatedCollections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(null);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        MyCollection collection = subscriber.getOnNextEvents().get(0);

        assertThat(collection.getLikes().getTrackPreviews()).isEqualTo(trackPreviews);
        assertThat(collection.getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void onCollectionChangedShouldSendAnEventWhenStations() {
        final TestSubscriber<Object> subscriber = new TestSubscriber<>();
        operations.onCollectionChanged().subscribe(subscriber);

        eventBus.publish(EventQueue.SYNC_RESULT, SyncJobResult.success(Syncable.RECENT_STATIONS.name(), true));

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void onCollectionChangedShouldSendAnEventWhenAPlaylistIsMarkedForDownload() {
        final TestSubscriber<Object> subscriber = new TestSubscriber<>();
        operations.onCollectionChanged().subscribe(subscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromPlaylistsMarkedForDownload(Collections.singletonList(Urn.forPlaylist(
                                 123))));

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
    }

    private PropertySet getPostedPlaylist(Urn urn, Date postedAt, String title) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(urn),
                PlaylistProperty.TITLE.bind(title),
                PostProperty.CREATED_AT.bind(postedAt)
        );
    }

    private PropertySet getLikedPlaylist(Urn urn, Date likedAt, String title) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(urn),
                PlaylistProperty.TITLE.bind(title),
                LikeProperty.CREATED_AT.bind(likedAt)
        );
    }
}
