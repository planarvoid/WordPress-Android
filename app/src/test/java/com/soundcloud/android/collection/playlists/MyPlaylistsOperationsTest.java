package com.soundcloud.android.collection.playlists;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MyPlaylistsOperationsTest extends AndroidUnitTest {

    private MyPlaylistsOperations operations;

    @Mock private SyncInitiatorBridge syncInitiator;
    @Mock private CollectionOptionsStorage collectionOptionsStorage;
    @Mock private FeatureFlags featureFlags;
    @Mock private PlaylistLikesStorage playlistLikesStorage;
    @Mock private PlaylistPostStorage playlistPostStorage;

    private TestSubscriber<List<PlaylistItem>> subscriber = new TestSubscriber<>();

    private PropertySet postedPlaylist1;
    private PropertySet postedPlaylist2;
    private PropertySet likedPlaylist1;
    private PropertySet likedPlaylist2;
    private PropertySet likedPlaylist3Offline;

    @Before
    public void setUp() throws Exception {
        operations = new MyPlaylistsOperations(
                syncInitiator,
                playlistLikesStorage,
                playlistPostStorage,
                Schedulers.immediate());

        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Observable.just(true));
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Observable.just(true));
        postedPlaylist1 = getPostedPlaylist(Urn.forPlaylist(1L), new Date(1), "apple");
        postedPlaylist2 = getPostedPlaylist(Urn.forPlaylist(2L), new Date(3), "banana");
        likedPlaylist1 = getLikedPlaylist(Urn.forPlaylist(3L), new Date(2), "cherry");
        likedPlaylist2 = getLikedPlaylist(Urn.forPlaylist(4L), new Date(4), "doughnut");
        likedPlaylist3Offline = getLikedPlaylistOffline(Urn.forPlaylist(5L),
                                                        new Date(5),
                                                        "eclair",
                                                        OfflineState.DOWNLOADED);

        when(playlistPostStorage.loadPostedPlaylists(any(Integer.class), eq(Long.MAX_VALUE)))
                .thenReturn(Observable.just(Arrays.asList(postedPlaylist1, postedPlaylist2)));

        when(playlistLikesStorage.loadLikedPlaylists(any(Integer.class), eq(Long.MAX_VALUE)))
                .thenReturn(Observable.just(Arrays.asList(likedPlaylist1, likedPlaylist2, likedPlaylist3Offline)));
    }

    @Test
    public void myPlaylistsReturnsPostedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void myPlaylistsReturnsLikedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(likedPlaylist1)
        ));
    }

    @Test
    public void myPlaylistsReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void myPlaylistsReturnsUniquePostedAndLikedPlaylists() throws Exception {
        PropertySet likedPlaylist3 = getLikedPlaylist(postedPlaylist1.get(EntityProperty.URN), new Date(6), "pepper");
        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1,
                                                                                           likedPlaylist2,
                                                                                           likedPlaylist3,
                                                                                           likedPlaylist3Offline));
        when(playlistLikesStorage.loadLikedPlaylists(any(Integer.class), eq(Long.MAX_VALUE)))
                .thenReturn(likedPlaylists);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3),
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1)
        ));
    }

    @Test
    public void myPlaylistsWithoutFiltersReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(false).build();

        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void myPlaylistsReturnsPostedAndLikedPlaylistsSortedByTitle() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true)
                                                         .sortByTitle(true).build();

        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(postedPlaylist1),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(likedPlaylist3Offline)
        ));
    }

    @Test
    public void myPlaylistsReturnsOfflineOnly() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showOfflineOnly(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(singletonList(
                PlaylistItem.from(likedPlaylist3Offline)));
    }

    @Test
    public void myPlaylistsSyncsBeforeReturningIfNeverSyncedBefore() throws Exception {
        final PublishSubject<Void> subject = PublishSubject.create();
        when(syncInitiator.refreshMyPostedAndLikedPlaylists()).thenReturn(subject);

        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Observable.just(false));

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();

        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(null);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void refreshAndLoadPlaylistsReturnsMyPlaylistsAfterSync() throws Exception {
        final PublishSubject<Void> subject = PublishSubject.create();
        when(syncInitiator.refreshMyPostedAndLikedPlaylists()).thenReturn(subject);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.refreshAndLoadPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();

        subject.onNext(null);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);

        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist3Offline),
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
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

    private PropertySet getLikedPlaylistOffline(Urn urn, Date date, String title, OfflineState state) {
        return getLikedPlaylist(urn, date, title)
                .put(OfflineProperty.OFFLINE_STATE, state);
    }

}
