package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestObserver;
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

    private Scheduler scheduler = Schedulers.immediate();
    private TestObserver<List<PropertySet>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new MyProfileOperations(
                postStorage,
                syncInitiator,
                scheduler);

        posts = Arrays.asList(
                TestPropertySets.expectedPostedPlaylistForPostsScreen(),
                TestPropertySets.expectedPostedTrackForPostsScreen()
        );

        observer = new TestObserver<>();
    }

    @Test
    public void postsForPlaybackLoadsTrackPostsFromStorage() {
        final List<PropertySet> trackPosts = createPageOfPostedTracks(PAGE_SIZE);
        when(postStorage.loadPostsForPlayback()).thenReturn(Observable.just(trackPosts));

        operations.postsForPlayback().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(trackPosts);
    }

    @Test
    public void syncAndLoadPostsWhenInitialPostsLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.pagedPostItems().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPostsResultsWithEmptyResults() throws Exception {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.pagedPostItems().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(Collections.<PropertySet>emptyList());
    }

    @Test
    public void postedPlaylistsReturnsPostedPlaylistsFromStorage() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPosts()).thenReturn(Observable.<Boolean>empty());

        operations.pagedPostItems().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(posts);
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PlaylistProperty.CREATED_AT).getTime();
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(postStorage.loadPosts(PAGE_SIZE, time)).thenReturn(Observable.<List<PropertySet>>never());
        when(syncInitiator.refreshPosts()).thenReturn(Observable.<Boolean>empty());

        operations.postsPagingFunction().call(firstPage);

        verify(postStorage).loadPosts(PAGE_SIZE, time);
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        assertThat(operations.postsPagingFunction().call(posts)).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.updatedPosts().subscribe(observer);

        assertThat(observer.getOnNextEvents()).containsExactly(posts);
    }

    private List<PropertySet> createPageOfPostedTracks(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedPostedTrackForPostsScreen());
        }
        return page;
    }

}