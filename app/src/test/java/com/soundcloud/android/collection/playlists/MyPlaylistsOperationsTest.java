package com.soundcloud.android.collection.playlists;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.posts.PostsStorage;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TestOfflinePropertiesProvider;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Maps;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MyPlaylistsOperationsTest {

    private MyPlaylistsOperations operations;

    @Mock private SyncInitiatorBridge syncInitiator;
    @Mock private PostsStorage postsStorage;
    @Mock private LikesStorage likesStorage;
    @Mock private PlaylistRepository playlistRepository;

    private Playlist postedPlaylist1;
    private Playlist postedPlaylist2;
    private Playlist album1;
    private Playlist likedPlaylist1;
    private Playlist likedPlaylist2;
    private Playlist likedPlaylist3Offline;
    private PlaylistAssociation likedPlaylistAssociation1;
    private PlaylistAssociation likedPlaylistAssociation2;
    private PlaylistAssociation likedPlaylistAssociation3Offline;

    @Before
    public void setUp() throws Exception {
        when(syncInitiator.hasSyncedLikedAndPostedPlaylistsBefore()).thenReturn(Single.just(true));

        postedPlaylist1 = getPlaylistItem(Urn.forPlaylist(1L), "apple");
        postedPlaylist2 = getPlaylistItem(Urn.forPlaylist(2L), "banana");
        likedPlaylist1 = getPlaylistItem(Urn.forPlaylist(3L), "cherry");
        likedPlaylist2 = getPlaylistItem(Urn.forPlaylist(4L), "doughnut");
        likedPlaylist3Offline = getLikedPlaylistOffline(Urn.forPlaylist(5L), "eclair", OfflineState.DOWNLOADED);
        album1 = getAlbum(Urn.forPlaylist(6L), "froyo");

        PlaylistAssociation postedPlaylistAssociation1 = getAssociatedPlaylist(postedPlaylist1, new Date(1));
        PlaylistAssociation postedPlaylistAssociation2 = getAssociatedPlaylist(postedPlaylist2, new Date(3));
        likedPlaylistAssociation1 = getAssociatedPlaylist(likedPlaylist1, new Date(2));
        likedPlaylistAssociation2 = getAssociatedPlaylist(likedPlaylist2, new Date(4));
        likedPlaylistAssociation3Offline = getAssociatedPlaylist(likedPlaylist3Offline, new Date(5));
        PlaylistAssociation albumAssociation1 = getAssociatedPlaylist(album1, new Date(6));

        mockLikes(Arrays.asList(
                likedPlaylistAssociation1,
                likedPlaylistAssociation2,
                likedPlaylistAssociation3Offline
        ));
        mockPosts(Arrays.asList(
                postedPlaylistAssociation1,
                postedPlaylistAssociation2,
                albumAssociation1
        ));

        operations = new MyPlaylistsOperations(
                syncInitiator,
                postsStorage,
                likesStorage,
                playlistRepository,
                TestOfflinePropertiesProvider.fromOfflinePlaylist(likedPlaylist3Offline.urn()),
                Schedulers.trampoline());

    }

    private void mockLikes(List<PlaylistAssociation> likedPlaylists) {

        when(likesStorage.loadPlaylistLikes(eq(Long.MAX_VALUE), any(Integer.class)))
                .thenReturn(Single.just(Lists.transform(likedPlaylists, PlaylistAssociation::getAssociation)));
        when(playlistRepository.withUrns(Lists.transform(likedPlaylists, PlaylistAssociation::getTargetUrn)))
                .thenReturn(Single.just(Maps.asMap(Lists.transform(likedPlaylists, PlaylistAssociation::getPlaylist), Playlist::urn)));
    }

    private void mockPosts(List<PlaylistAssociation> postedPlaylists) {
        when(postsStorage.loadPostedPlaylists(any(Integer.class), eq(Long.MAX_VALUE)))
                .thenReturn(Single.just(Lists.transform(postedPlaylists, PlaylistAssociation::getAssociation)));

        when(playlistRepository.withUrns(Lists.transform(postedPlaylists, PlaylistAssociation::getTargetUrn)))
                .thenReturn(Single.just(Maps.asMap(Lists.transform(postedPlaylists, PlaylistAssociation::getPlaylist), Playlist::urn)));
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
        mockLikes(Arrays.asList(likedPlaylistAssociation1,
                                likedPlaylistAssociation2,
                                likedPlaylistAssociation3,
                                likedPlaylistAssociation3Offline));

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
        return PlaylistFixtures.playlistBuilder().urn(urn).title(title);
    }

    private PlaylistAssociation getAssociatedPlaylist(Playlist playlistItem, Date postedAt) {
        return PlaylistAssociation.create(playlistItem, new Association(playlistItem.urn(), postedAt));
    }

    private Playlist getLikedPlaylistOffline(Urn urn, String title, OfflineState state) {
        return getPlaylistBuilder(urn, title).build();
    }
}
