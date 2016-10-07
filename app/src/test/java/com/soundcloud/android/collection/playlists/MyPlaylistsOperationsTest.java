package com.soundcloud.android.collection.playlists;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.CollectionOptionsStorage;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistAssociation;
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

    private PlaylistItem postedPlaylist1;
    private PlaylistItem postedPlaylist2;
    private PlaylistItem likedPlaylist1;
    private PlaylistItem likedPlaylist2;
    private PlaylistItem likedPlaylist3Offline;

    private PlaylistAssociation postedPlaylistAssociation1;
    private PlaylistAssociation postedPlaylistAssociation2;
    private PlaylistAssociation likedPlaylistAssociation1;
    private PlaylistAssociation likedPlaylistAssociation2;
    private PlaylistAssociation likedPlaylistAssociation3Offline;

    @Before
    public void setUp() throws Exception {
        operations = new MyPlaylistsOperations(
                syncInitiator,
                playlistLikesStorage,
                playlistPostStorage,
                Schedulers.immediate());

        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Observable.just(true));
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Observable.just(true));


        postedPlaylist1 = getPlaylistItem(Urn.forPlaylist(1L), "apple");
        postedPlaylist2 = getPlaylistItem(Urn.forPlaylist(2L), "banana");
        likedPlaylist1 = getPlaylistItem(Urn.forPlaylist(3L), "cherry");
        likedPlaylist2 = getPlaylistItem(Urn.forPlaylist(4L), "doughnut");
        likedPlaylist3Offline = getLikedPlaylistOffline(Urn.forPlaylist(5L),
                                                        "eclair",
                                                        OfflineState.DOWNLOADED);

        postedPlaylistAssociation1 = getAssociatedPlaylist(postedPlaylist1, new Date(1));
        postedPlaylistAssociation2 = getAssociatedPlaylist(postedPlaylist2, new Date(3));
        likedPlaylistAssociation1 = getAssociatedPlaylist(likedPlaylist1, new Date(2));
        likedPlaylistAssociation2 = getAssociatedPlaylist(likedPlaylist2, new Date(4));
        likedPlaylistAssociation3Offline = getAssociatedPlaylist(likedPlaylist3Offline, new Date(5));

        when(playlistPostStorage.loadPostedPlaylists(any(Integer.class), eq(Long.MAX_VALUE), anyString()))
                .thenReturn(Observable.just(
                        Arrays.asList(
                                postedPlaylistAssociation1,
                                postedPlaylistAssociation2
                        )));

        when(playlistLikesStorage.loadLikedPlaylists(any(Integer.class), eq(Long.MAX_VALUE), anyString()))
                .thenReturn(Observable.just(
                        Arrays.asList(
                                likedPlaylistAssociation1,
                                likedPlaylistAssociation2,
                                likedPlaylistAssociation3Offline
                        )));
    }

    @Test
    public void myPlaylistsReturnsPostedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(false).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                postedPlaylist2,
                postedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsLikedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                likedPlaylist3Offline,
                likedPlaylist2,
                likedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsUniquePostedAndLikedPlaylists() throws Exception {
        final PlaylistItem likedPlaylist3 = getPlaylistItem(postedPlaylist1.getUrn(), "pepper");
        PlaylistAssociation likedPlaylistAssociation3 = getAssociatedPlaylist(likedPlaylist3, new Date(6));
        final Observable<List<PlaylistAssociation>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylistAssociation1,
                                                                                                   likedPlaylistAssociation2,
                                                                                                   likedPlaylistAssociation3,
                                                                                                   likedPlaylistAssociation3Offline));
        when(playlistLikesStorage.loadLikedPlaylists(any(Integer.class), eq(Long.MAX_VALUE), eq("filter")))
                .thenReturn(likedPlaylists);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).textFilter("filter").build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                likedPlaylist3,
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsWithoutFiltersReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(false).build();

        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsPostedAndLikedPlaylistsSortedByTitle() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true)
                                                         .sortByTitle(true).build();

        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(Arrays.asList(
                postedPlaylist1,
                postedPlaylist2,
                likedPlaylist1,
                likedPlaylist2,
                likedPlaylist3Offline
        ));
    }

    @Test
    public void myPlaylistsReturnsOfflineOnly() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showOfflineOnly(true).build();
        operations.myPlaylists(options).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0)).isEqualTo(singletonList(
                likedPlaylist3Offline));
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
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
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
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
        ));
    }

    private PlaylistItem getPlaylistItem(Urn urn, String title) {
        return PlaylistItem.from(PropertySet.from(
                PlaylistProperty.URN.bind(urn),
                PlaylistProperty.TITLE.bind(title)));
    }

    private PlaylistAssociation getAssociatedPlaylist(PlaylistItem playlistItem, Date postedAt) {
        return PlaylistAssociation.create(playlistItem, postedAt);
    }

    private PlaylistItem getLikedPlaylistOffline(Urn urn, String title, OfflineState state) {
        final PlaylistItem playlist = getPlaylistItem(urn, title);
        playlist.getSource().put(OfflineProperty.OFFLINE_STATE, state);
        return playlist;
    }
}
