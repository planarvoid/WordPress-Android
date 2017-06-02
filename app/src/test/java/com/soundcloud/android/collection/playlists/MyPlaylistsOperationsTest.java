package com.soundcloud.android.collection.playlists;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MyPlaylistsOperationsTest extends AndroidUnitTest {

    private MyPlaylistsOperations operations;

    @Mock private SyncInitiatorBridge syncInitiator;
    @Mock private PlaylistLikesStorage playlistLikesStorage;
    @Mock private PlaylistPostStorage playlistPostStorage;

    private Playlist postedPlaylist1;
    private Playlist postedPlaylist2;
    private Playlist album1;
    private Playlist likedPlaylist1;
    private Playlist likedPlaylist2;
    private Playlist likedPlaylist3Offline;
    private PlaylistAssociation postedPlaylistAssociation1;
    private PlaylistAssociation postedPlaylistAssociation2;
    private PlaylistAssociation albumAssociation1;
    private PlaylistAssociation likedPlaylistAssociation1;
    private PlaylistAssociation likedPlaylistAssociation2;
    private PlaylistAssociation likedPlaylistAssociation3Offline;

    @Before
    public void setUp() throws Exception {
        operations = new MyPlaylistsOperations(
                syncInitiator,
                playlistLikesStorage,
                playlistPostStorage,
                Schedulers.trampoline());

        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Single.just(true));
        when(syncInitiator.hasSyncedTrackLikesBefore()).thenReturn(Single.just(true));


        postedPlaylist1 = getPlaylistItem(Urn.forPlaylist(1L), "apple");
        postedPlaylist2 = getPlaylistItem(Urn.forPlaylist(2L), "banana");
        likedPlaylist1 = getPlaylistItem(Urn.forPlaylist(3L), "cherry");
        likedPlaylist2 = getPlaylistItem(Urn.forPlaylist(4L), "doughnut");
        likedPlaylist3Offline = getLikedPlaylistOffline(Urn.forPlaylist(5L), "eclair", OfflineState.DOWNLOADED);
        album1 = getAlbum(Urn.forPlaylist(6L), "froyo");

        postedPlaylistAssociation1 = getAssociatedPlaylist(postedPlaylist1, new Date(1));
        postedPlaylistAssociation2 = getAssociatedPlaylist(postedPlaylist2, new Date(3));
        likedPlaylistAssociation1 = getAssociatedPlaylist(likedPlaylist1, new Date(2));
        likedPlaylistAssociation2 = getAssociatedPlaylist(likedPlaylist2, new Date(4));
        likedPlaylistAssociation3Offline = getAssociatedPlaylist(likedPlaylist3Offline, new Date(5));
        albumAssociation1 = getAssociatedPlaylist(album1, new Date(6));

        when(playlistPostStorage.loadPostedPlaylists(any(Integer.class), eq(Long.MAX_VALUE)))
                .thenReturn(Observable.just(
                        Arrays.asList(
                                postedPlaylistAssociation1,
                                postedPlaylistAssociation2,
                                albumAssociation1
                        )));

        when(playlistLikesStorage.loadLikedPlaylists(any(Integer.class), eq(Long.MAX_VALUE)))
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
        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(Arrays.asList(
                album1,
                postedPlaylist2,
                postedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsLikedPlaylists() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(false).showLikes(true).build();
        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(Arrays.asList(
                likedPlaylist3Offline,
                likedPlaylist2,
                likedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsOnlyAlbums() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().entities(PlaylistsOptions.Entities.ALBUMS).build();
        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(singletonList(
                album1
        ));
    }

    @Test
    public void myPlaylistsReturnsPostedAndLikedPlaylistsSortedByCreationDate() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(Arrays.asList(
                album1,
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
        ));
    }

    @Test
    public void myPlaylistsReturnsUniquePostedAndLikedPlaylists() throws Exception {
        final Playlist likedPlaylist3 = getPlaylistItem(postedPlaylist1.urn(), "pepper");
        PlaylistAssociation likedPlaylistAssociation3 = getAssociatedPlaylist(likedPlaylist3, new Date(6));
        final Observable<List<PlaylistAssociation>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylistAssociation1,
                                                                                                   likedPlaylistAssociation2,
                                                                                                   likedPlaylistAssociation3,
                                                                                                   likedPlaylistAssociation3Offline));
        when(playlistLikesStorage.loadLikedPlaylists(any(Integer.class), eq(Long.MAX_VALUE))).thenReturn(likedPlaylists);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(Arrays.asList(
                album1,
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

        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(Arrays.asList(
                album1,
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

        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(Arrays.asList(
                postedPlaylist1,
                postedPlaylist2,
                likedPlaylist1,
                likedPlaylist2,
                likedPlaylist3Offline,
                album1
        ));
    }

    @Test
    public void myPlaylistsReturnsOfflineOnly() throws Exception {
        final PlaylistsOptions options = PlaylistsOptions.builder().showOfflineOnly(true).build();
        operations.myPlaylists(options).test()
                  .assertValueCount(1)
                  .assertValues(singletonList(
                likedPlaylist3Offline));
    }

    @Test
    public void myPlaylistsSyncsBeforeReturningIfNeverSyncedBefore() throws Exception {
        final CompletableSubject subject = CompletableSubject.create();
        when(syncInitiator.refreshMyPostedAndLikedPlaylists()).thenReturn(subject);

        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Single.just(false));

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();

        TestObserver<List<Playlist>> testObserver = operations.myPlaylists(options).test();

         testObserver.assertNoValues();

        subject.onComplete();

        testObserver.assertValueCount(1);
        testObserver.assertValue(Arrays.asList(
                album1,
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
        ));
    }

    @Test
    public void refreshAndLoadPlaylistsReturnsMyPlaylistsAfterSync() throws Exception {
        final CompletableSubject subject = CompletableSubject.create();
        when(syncInitiator.refreshMyPostedAndLikedPlaylists()).thenReturn(subject);

        final PlaylistsOptions options = PlaylistsOptions.builder().showPosts(true).showLikes(true).build();
        TestObserver testObserver = operations.refreshAndLoadPlaylists(options).test();

        testObserver.assertNoValues();

        subject.onComplete();

        testObserver.assertValueCount(1);

        testObserver.assertValue(Arrays.asList(
                album1,
                likedPlaylist3Offline,
                likedPlaylist2,
                postedPlaylist2,
                likedPlaylist1,
                postedPlaylist1
        ));
    }

    private Playlist getPlaylistItem(Urn urn, String title) {
        return getPlaylistBuilder(urn, title).build();
    }

    private Playlist getAlbum(Urn urn, String title) {
        return getPlaylistBuilder(urn, title).isAlbum(true).build();
    }

    private Playlist.Builder getPlaylistBuilder(Urn urn, String title) {
        return ModelFixtures.playlistBuilder().urn(urn).title(title);
    }

    private PlaylistAssociation getAssociatedPlaylist(Playlist playlistItem, Date postedAt) {
        return PlaylistAssociation.create(playlistItem, postedAt);
    }

    private Playlist getLikedPlaylistOffline(Urn urn, String title, OfflineState state) {
        return getPlaylistBuilder(urn, title).offlineState(state).build();
    }
}
