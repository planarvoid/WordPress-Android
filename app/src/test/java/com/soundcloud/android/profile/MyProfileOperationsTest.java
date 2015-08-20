package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyProfileOperationsTest extends AndroidUnitTest {

    private MyProfileOperations operations;
    private List<PropertySet> posts;

    @Mock private PostsStorage postStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private LikesStorage likesStorage;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<PropertySet>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new MyProfileOperations(likesStorage, postStorage, playlistPostStorage, syncInitiator,
                networkConnectionHelper, scheduler);

        posts = Arrays.asList(
                TestPropertySets.expectedPostedPlaylistForPostsScreen(),
                TestPropertySets.expectedPostedTrackForPostsScreen()
        );
        observer = new TestSubscriber<>();
    }

    @Test
    public void postsForPlaybackLoadsTrackPostsFromStorage() {
        final List<PropertySet> trackPosts = createPageOfPostedTracks(PAGE_SIZE);
        when(postStorage.loadPostsForPlayback()).thenReturn(Observable.just(trackPosts));

        operations.postsForPlayback().subscribe(observer);

        observer.assertValue(trackPosts);
    }

    @Test
    public void syncAndLoadPostsWhenInitialPostsLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.pagedPostItems().subscribe(observer);

        observer.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPostsResultsWithEmptyResults() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.pagedPostItems().subscribe(observer);

        observer.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedPostItemsReturnsPostedItemsFromStorage() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.<Boolean>empty());

        operations.pagedPostItems().subscribe(observer);

        observer.assertValue(posts);
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PostProperty.CREATED_AT).getTime();
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(postStorage.loadPosts(PAGE_SIZE, time)).thenReturn(Observable.<List<PropertySet>>never());
        when(syncInitiator.refreshPosts()).thenReturn(Observable.<Boolean>empty());

        operations.postsPagingFunction().call(firstPage);

        verify(postStorage).loadPosts(PAGE_SIZE, time);
    }

    @Test
    public void postPagerFinishesIfLastPageIncomplete() {
        assertThat(operations.postsPagingFunction().call(posts)).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedPostsReloadsPostedPostsAfterSyncWithChange() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.updatedPosts().subscribe(observer);

        observer.assertValue(posts);
    }

    @Test
    public void syncAndLoadLikesWhenInitialLikesLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfLikes(PAGE_SIZE);
        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.just(true));

        operations.pagedLikes().subscribe(observer);

        observer.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyLikesResultsWithEmptyResults() {
        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.just(true));

        operations.pagedLikes().subscribe(observer);

        observer.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedLikesReturnsLikesFromStorage() {
        final List<PropertySet> pageOfLikes = createPageOfLikes(2);

        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(pageOfLikes));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.<Boolean>empty());

        operations.pagedLikes().subscribe(observer);

        observer.assertValue(pageOfLikes);
    }

    @Test
    public void likesPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() {
        final List<PropertySet> firstPage = createPageOfLikes(PAGE_SIZE);
        final long time = firstPage.get(PAGE_SIZE - 1).get(LikeProperty.CREATED_AT).getTime();
        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(likesStorage.loadLikes(PAGE_SIZE, time)).thenReturn(Observable.<List<PropertySet>>never());
        when(syncInitiator.refreshLikes()).thenReturn(Observable.<Boolean>empty());

        operations.likesPagingFunction().call(firstPage);

        verify(likesStorage).loadLikes(PAGE_SIZE, time);
    }

    @Test
    public void likesPagerFinishesIfLastPageIncomplete() {
        assertThat(operations.likesPagingFunction().call(createPageOfLikes(PAGE_SIZE - 1))).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedLikesReloadsLikesAfterSyncWithChange() {
        final List<PropertySet> pageOfLikes = createPageOfLikes(2);
        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(pageOfLikes));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.just(true));

        operations.updatedLikes().subscribe(observer);

        observer.assertValue(pageOfLikes);
    }

    @Test
    public void syncAndLoadPlaylistsWhenInitialPlaylistLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPlaylists(PAGE_SIZE);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.pagedPlaylistItems().subscribe(observer);

        observer.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPlaylistsResultsWithEmptyResults() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.pagedPlaylistItems().subscribe(observer);

        observer.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedPlaylistItemsReturnsPlaylistItemsFromStorage() {
        final List<PropertySet> playlists = createPageOfPlaylists(2);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(playlists));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.pagedPlaylistItems().subscribe(observer);

        observer.assertValue(playlists);
    }

    @Test
    public void playlistPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() {
        final List<PropertySet> firstPage = createPageOfPlaylists(PAGE_SIZE);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PostProperty.CREATED_AT).getTime();
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, time)).thenReturn(Observable.<List<PropertySet>>never());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.playlistPagingFunction().call(firstPage);

        verify(playlistPostStorage).loadPostedPlaylists(PAGE_SIZE, time);
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() {
        assertThat(operations.playlistPagingFunction().call(createPageOfPlaylists(PAGE_SIZE - 1))).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        final List<PropertySet> playlists = createPageOfPlaylists(2);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(playlists));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.updatedPlaylists().subscribe(observer);

        observer.assertValue(playlists);
    }


    private List<PropertySet> createPageOfPlaylists(int size) {
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        }
        return page;
    }

    private List<PropertySet> createPageOfLikes(int size) {
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(TestPropertySets.expectedLikedTrackForLikesScreen());
        }
        return page;
    }

    private List<PropertySet> createPageOfPostedTracks(int size) {
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(TestPropertySets.expectedPostedTrackForPostsScreen());
        }
        return page;
    }

}