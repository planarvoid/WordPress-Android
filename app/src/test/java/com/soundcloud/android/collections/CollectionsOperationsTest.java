package com.soundcloud.android.collections;

import static com.soundcloud.android.collections.CollectionsOperations.PLAYLIST_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CollectionsOperationsTest extends AndroidUnitTest {

    private CollectionsOperations operations;

    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private PlaylistLikesStorage playlistLikeStorage;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;

    private TestSubscriber<MyCollections> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new CollectionsOperations(Schedulers.immediate(), playlistPostStorage, playlistLikeStorage, loadLikedTrackUrnsCommand);
        when(loadLikedTrackUrnsCommand.toObservable())
                .thenReturn(Observable.just(Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L))));
    }

    @Test
    public void collectionsReturnsSortedPostedPlaylists() throws Exception {
        final PropertySet postedPlaylist1 = getPostedPlaylist(Urn.forPlaylist(1L), new Date(1));
        final PropertySet postedPlaylist2 = getPostedPlaylist(Urn.forPlaylist(2L), new Date(3));

        final Observable<List<PropertySet>> postedPlaylists = Observable.just(Arrays.asList(postedPlaylist1, postedPlaylist2));
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(postedPlaylists);
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));

        operations.collections().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikesCount()).isEqualTo(2);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsSortedLikedPlaylists() throws Exception {
        final PropertySet likedPlaylist1 = getLikedPlaylist(Urn.forPlaylist(3L), new Date(2));
        final PropertySet likedPlaylist2 = getLikedPlaylist(Urn.forPlaylist(4L), new Date(4));

        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1, likedPlaylist2));
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(likedPlaylists);
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));

        operations.collections().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikesCount()).isEqualTo(2);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(likedPlaylist1)
        ));
    }

    @Test
    public void collectionsReturnsSortedPostedAndLikedPlaylists() throws Exception {
        final PropertySet postedPlaylist1 = getPostedPlaylist(Urn.forPlaylist(1L), new Date(1));
        final PropertySet postedPlaylist2 = getPostedPlaylist(Urn.forPlaylist(2L), new Date(3));
        final PropertySet likedPlaylist1 = getLikedPlaylist(Urn.forPlaylist(3L), new Date(2));
        final PropertySet likedPlaylist2 = getLikedPlaylist(Urn.forPlaylist(4L), new Date(4));

        final Observable<List<PropertySet>> postedPlaylists = Observable.just(Arrays.asList(postedPlaylist1, postedPlaylist2));
        when(playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(postedPlaylists);

        final Observable<List<PropertySet>> likedPlaylists = Observable.just(Arrays.asList(likedPlaylist1, likedPlaylist2));
        when(playlistLikeStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)).thenReturn(likedPlaylists);

        operations.collections().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
        assertThat(subscriber.getOnNextEvents().get(0).getLikesCount()).isEqualTo(2);
        assertThat(subscriber.getOnNextEvents().get(0).getPlaylistItems()).isEqualTo(Arrays.asList(
                PlaylistItem.from(likedPlaylist2),
                PlaylistItem.from(postedPlaylist2),
                PlaylistItem.from(likedPlaylist1),
                PlaylistItem.from(postedPlaylist1)
        ));
    }

    private PropertySet getPostedPlaylist(Urn urn, Date postedAt) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(urn),
                PostProperty.CREATED_AT.bind(postedAt)
        );
    }

    private PropertySet getLikedPlaylist(Urn urn, Date likedAt) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(urn),
                LikeProperty.CREATED_AT.bind(likedAt)
        );
    }
}

