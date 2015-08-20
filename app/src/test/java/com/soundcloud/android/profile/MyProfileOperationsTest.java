package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
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
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private SyncInitiator syncInitiator;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<PropertySet>> subscriber;

    @Before
    public void setUp() throws Exception {
        operations = new MyProfileOperations(
                postStorage,
                syncStateStorage, syncInitiator,
                scheduler);

        posts = Arrays.asList(
                TestPropertySets.expectedPostedPlaylistForPostsScreen(),
                TestPropertySets.expectedPostedTrackForPostsScreen()
        );

        subscriber = new TestSubscriber<>();
    }

    @Test
    public void postsForPlaybackLoadsTrackPostsFromStorage() {
        final List<PropertySet> trackPosts = createPageOfPostedTracks(PAGE_SIZE);
        when(postStorage.loadPostsForPlayback()).thenReturn(Observable.just(trackPosts));

        operations.postsForPlayback().subscribe(subscriber);

        subscriber.assertValues(trackPosts);
    }


    @Test
    public void postedPlaylistsReturnsPostedPlaylistsFromStorageIfSyncedBefore() {
        when(syncStateStorage.hasSyncedBefore(SyncContent.MySounds)).thenReturn(Observable.just(true));
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));

        operations.pagedPostItems().subscribe(subscriber);

        subscriber.assertValues(posts);
        verify(syncInitiator, never()).refreshPosts();
    }

    @Test
    public void postedPlaylistsReturnsEmptyPostsFromStorageIfSyncedBefore() {
        final List<PropertySet> emptyList = Collections.emptyList();
        when(syncStateStorage.hasSyncedBefore(SyncContent.MySounds)).thenReturn(Observable.just(true));
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(emptyList));

        operations.pagedPostItems().subscribe(subscriber);

        subscriber.assertValues(emptyList);
        verify(syncInitiator, never()).refreshPosts();
    }

    @Test
    public void postedPlaylistsSyncsAndLoadPostsIfNeverSyncedBefore() {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        when(syncStateStorage.hasSyncedBefore(SyncContent.MySounds)).thenReturn(Observable.just(false));
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.pagedPostItems().subscribe(subscriber);

        subscriber.assertValues(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPostsResultsIfNeverSyncedBefore() throws Exception {
        final List<PropertySet> emptyList = Collections.emptyList();
        when(syncStateStorage.hasSyncedBefore(SyncContent.MySounds)).thenReturn(Observable.just(false));
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(emptyList));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.pagedPostItems().subscribe(subscriber);

        subscriber.assertValues(emptyList);
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        final List<PropertySet> secondPage = createPageOfPostedTracks(1);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PostProperty.CREATED_AT).getTime();
        when(syncStateStorage.hasSyncedBefore(SyncContent.MySounds)).thenReturn(Observable.just(true));
        when(postStorage.loadPosts(PAGE_SIZE, time)).thenReturn(Observable.just(secondPage));

        operations.postsPagingFunction().call(firstPage).subscribe(subscriber);

        subscriber.assertValues(secondPage);
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        assertThat(operations.postsPagingFunction().call(posts)).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.updatedPosts().subscribe(subscriber);

        subscriber.assertValues(posts);
    }

    private List<PropertySet> createPageOfPostedTracks(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedPostedTrackForPostsScreen());
        }
        return page;
    }

}