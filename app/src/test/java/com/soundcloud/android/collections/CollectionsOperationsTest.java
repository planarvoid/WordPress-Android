package com.soundcloud.android.collections;

import static com.soundcloud.android.collections.CollectionsOperations.PLAYLIST_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stations.StationsSyncRequestFactory;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CollectionsOperationsTest extends AndroidUnitTest {

    private CollectionsOperations operations;

    @Mock private SyncStateStorage syncStateStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private PlaylistLikesStorage playlistLikeStorage;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    @Mock private StationsOperations stationsOperations;
    @Mock private CollectionsOptionsStorage collectionsOptionsStorage;

    private TestSubscriber<MyCollections> subscriber = new TestSubscriber<>();
    private List<Urn> likesUrns = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L));

    private PropertySet postedPlaylist1;
    private PropertySet postedPlaylist2;
    private PropertySet likedPlaylist1;
    private PropertySet likedPlaylist2;
    private TestEventBus eventBus;
    private TestSubscriber<EntityStateChangedEvent> collectionChangedSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        operations = new CollectionsOperations(
                eventBus,
                Schedulers.immediate(),
                syncStateStorage,
                playlistPostStorage,
                playlistLikeStorage,
                loadLikedTrackUrnsCommand,
                syncInitiator,
                stationsOperations,
                new FeatureFlags(sharedPreferences("test", Context.MODE_PRIVATE)),
                collectionsOptionsStorage);

        when(loadLikedTrackUrnsCommand.toObservable()).thenReturn(Observable.just(likesUrns));
        when(syncStateStorage.hasSyncedBefore(SyncContent.MyLikes.content.uri)).thenReturn(Observable.just(true));
        when(syncStateStorage.hasSyncedBefore(SyncContent.MyPlaylists.content.uri)).thenReturn(Observable.just(true));
        when(stationsOperations.collection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.just(StationFixtures.getStation(Urn.forTrackStation(123L))));
        when(stationsOperations.sync()).thenReturn(Observable.just(SyncResult.success("stations sync", true)));
        postedPlaylist1 = getPostedPlaylist(Urn.forPlaylist(1L), new Date(1), "apple");
        postedPlaylist2 = getPostedPlaylist(Urn.forPlaylist(2L), new Date(3), "banana");
        likedPlaylist1 = getLikedPlaylist(Urn.forPlaylist(3L), new Date(2), "cherry");
        likedPlaylist2 = getLikedPlaylist(Urn.forPlaylist(4L), new Date(4), "doughnut");

        final Observable<List<PropertySet>> postedPlaylists = Observable.just(Arrays.asList(postedPlaylist1, postedPlaylist2));
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(postedPlaylists);

        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1, likedPlaylist2));
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(likedPlaylists);
    }

    @Test
    public void onCollectionChangedWhenTrackLikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(1), true, 1));

        collectionChangedSubscriber.assertReceivedOnNext(Arrays.asList(EntityStateChangedEvent.fromLike(Urn.forTrack(1), true, 1)));
    }

    @Test
    public void onCollectionChangedWhenTrackUnlikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forTrack(1), false, 1));

        collectionChangedSubscriber.assertReceivedOnNext(Arrays.asList(EntityStateChangedEvent.fromLike(Urn.forTrack(1), false, 1)));
    }

    @Test
    public void onCollectionChangedWhenPlaylistLikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forPlaylist(1), true, 1));

        collectionChangedSubscriber.assertReceivedOnNext(Arrays.asList(EntityStateChangedEvent.fromLike(Urn.forPlaylist(1), true, 1)));
    }

    @Test
    public void onCollectionChangedWhenPlaylistUnlikeEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(Urn.forPlaylist(1), false, 1));

        collectionChangedSubscriber.assertReceivedOnNext(Arrays.asList(EntityStateChangedEvent.fromLike(Urn.forPlaylist(1), false, 1)));
    }

    @Test
    public void onCollectionChangedWhenPlaylistCreatedEventFires() {
        operations.onCollectionChanged().subscribe(collectionChangedSubscriber);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromPlaylistCreated(Urn.forLocalPlaylist(1)));

        collectionChangedSubscriber.assertReceivedOnNext(Arrays.asList(EntityStateChangedEvent.fromPlaylistCreated(Urn.forLocalPlaylist(1))));
    }

    @Test
    public void collectionsShouldReturnAnErrorWhenAllCollectionsFailedToLoad() {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();

        final RuntimeException exception = new RuntimeException("Test");
        when(loadLikedTrackUrnsCommand.toObservable()).thenReturn(Observable.<List<Urn>>error(exception));
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(Observable.<List<PropertySet>>error(exception));
        when(stationsOperations.collection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.<StationRecord>error(exception));

        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(Collections.emptyList());
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Collections.emptyList());
        assertThat(subscriber.getOnNextEvents().get(0).getRecentStations()).isEqualTo(Collections.emptyList());
        assertThat(subscriber.getOnNextEvents().get(0).hasError()).isTrue();
    }

    @Test
    public void collectionsShouldReturnAValueWhenAtLeastOneCollectionSucceededToLoad() {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();

        final RuntimeException exception = new RuntimeException("Test");
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(Observable.<List<PropertySet>>error(exception));
        when(stationsOperations.collection(StationsCollectionsTypes.RECENT)).thenReturn(Observable.<StationRecord>error(exception));

        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Collections.emptyList());
        assertThat(subscriber.getOnNextEvents().get(0).getRecentStations()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void collectionsReturnsPostedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
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
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(likedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsUniquePostedAndLikedPlaylists() throws Exception {
        PropertySet likedPlaylist3 = getLikedPlaylist(postedPlaylist1.get(EntityProperty.URN), new Date(5), "pepper");
        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1, likedPlaylist2, likedPlaylist3));
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(likedPlaylists);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3),
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
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
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
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(postedPlaylist1),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(likedPlaylist2)
        ));
    }

    @Test
    public void collectionsSyncsBeforeReturningIfNeverSyncedBefore() throws Exception {
        final PublishSubject<Boolean> subject = PublishSubject.create();
        when(syncInitiator.refreshLikes()).thenReturn(subject);
        when(syncInitiator.refreshMyPlaylists()).thenReturn(subject);

        when(syncStateStorage.hasSyncedBefore(SyncContent.MyLikes.content.uri)).thenReturn(Observable.just(false));
        when(syncStateStorage.hasSyncedBefore(SyncContent.MyPlaylists.content.uri)).thenReturn(Observable.just(false));

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.collections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(true);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void updatedCollectionsReturnsMyCollectionsAfterSync() throws Exception {
        final PublishSubject<Boolean> subject = PublishSubject.create();
        when(syncInitiator.refreshLikes()).thenReturn(subject);
        when(syncInitiator.refreshMyPlaylists()).thenReturn(subject);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.updatedCollections(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(true);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikes()).isEqualTo(likesUrns);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void onCollectionSyncedShouldSendAnEventWhenPlaylistSynced() {
        final TestSubscriber<SyncResult> subscriber = new TestSubscriber<>();
        operations.onCollectionSynced().subscribe(subscriber);

        final SyncResult syncResult = SyncResult.success(SyncActions.SYNC_PLAYLISTS, true);
        eventBus.publish(EventQueue.SYNC_RESULT, syncResult);

        subscriber.assertValue(syncResult);
    }

    @Test
    public void onCollectionSyncedShouldSendAnEventWhenStations() {
        final TestSubscriber<SyncResult> subscriber = new TestSubscriber<>();
        operations.onCollectionSynced().subscribe(subscriber);

        final SyncResult syncResult = SyncResult.success(StationsSyncRequestFactory.Actions.SYNC_STATIONS, true);
        eventBus.publish(EventQueue.SYNC_RESULT, syncResult);

        subscriber.assertValue(syncResult);
    }

    @Test
    public void onCollectionSyncedShouldNotSendAnEventWhenNoChange() {
        final TestSubscriber<SyncResult> subscriber = new TestSubscriber<>();
        operations.onCollectionSynced().subscribe(subscriber);

        final SyncResult syncResult = SyncResult.success(SyncActions.SYNC_PLAYLISTS, false);
        eventBus.publish(EventQueue.SYNC_RESULT, syncResult);

        subscriber.assertNoValues();
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

