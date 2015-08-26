package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.MyProfileOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
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
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private LikesStorage likesStorage;
    @Mock private UserAssociationStorage userAssociationStorage;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<PropertySet>> subscriber;

    @Before
    public void setUp() throws Exception {
        operations = new MyProfileOperations(likesStorage, postStorage, playlistPostStorage, syncStateStorage, syncInitiator,
                networkConnectionHelper, userAssociationStorage, scheduler);


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
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() {
        final List<PropertySet> firstPage = createPageOfPostedTracks(PAGE_SIZE);
        final List<PropertySet> secondPage = createPageOfPostedTracks(1);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PostProperty.CREATED_AT).getTime();
        when(syncStateStorage.hasSyncedBefore(SyncContent.MySounds)).thenReturn(Observable.just(true));
        when(postStorage.loadPosts(PAGE_SIZE, time)).thenReturn(Observable.just(secondPage));

        operations.postsPagingFunction().call(firstPage).subscribe(subscriber);

        subscriber.assertValues(secondPage);
    }

    @Test
    public void postPagerFinishesIfLastPageIncomplete() {
        assertThat(operations.postsPagingFunction().call(posts)).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedPostsReloadsPostedPostsAfterSyncWithChange() {
        when(postStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(posts));
        when(syncInitiator.refreshPosts()).thenReturn(Observable.just(true));

        operations.updatedPosts().subscribe(subscriber);

        subscriber.assertValue(posts);
    }

    @Test
    public void syncAndLoadLikesWhenInitialLikesLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfLikes(PAGE_SIZE);
        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.just(true));

        operations.pagedLikes().subscribe(subscriber);

        subscriber.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyLikesResultsWithEmptyResults() {
        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.just(true));

        operations.pagedLikes().subscribe(subscriber);

        subscriber.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedLikesReturnsLikesFromStorage() {
        final List<PropertySet> pageOfLikes = createPageOfLikes(2);

        when(likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(pageOfLikes));
        when(syncInitiator.refreshLikes()).thenReturn(Observable.<Boolean>empty());

        operations.pagedLikes().subscribe(subscriber);

        subscriber.assertValue(pageOfLikes);
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

        operations.updatedLikes().subscribe(subscriber);

        subscriber.assertValue(pageOfLikes);
    }

    @Test
    public void syncAndLoadPlaylistsWhenInitialPlaylistLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPlaylists(PAGE_SIZE);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.pagedPlaylistItems().subscribe(subscriber);

        subscriber.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPlaylistsResultsWithEmptyResults() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.pagedPlaylistItems().subscribe(subscriber);

        subscriber.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedPlaylistItemsReturnsPlaylistItemsFromStorage() {
        final List<PropertySet> playlists = createPageOfPlaylists(2);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(playlists));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.pagedPlaylistItems().subscribe(subscriber);

        subscriber.assertValue(playlists);
    }

    @Test
    public void playlistPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() {
        final List<PropertySet> firstPage = createPageOfPlaylists(PAGE_SIZE);
        final List<PropertySet> secondPage = createPageOfPlaylists(1);

        final long time = firstPage.get(PAGE_SIZE - 1).get(PostProperty.CREATED_AT).getTime();
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, time)).thenReturn(Observable.just(secondPage));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.playlistPagingFunction().call(firstPage).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(secondPage));
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

        operations.updatedPlaylists().subscribe(subscriber);

        subscriber.assertValue(playlists);
    }

    @Test
    public void syncAndLoadFollowingsWhenInitialFollowingsLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.loadFollowingsUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(followingsUrn));
        when(userAssociationStorage.loadFollowings(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshFollowings()).thenReturn(Observable.just(true));
        when(syncInitiator.syncUsers(followingsUrn)).thenReturn(Observable.just(SyncResult.success("success", true)));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(firstPage);
    }

    @Test
    public void syncAndLoadEmptyFollowingsResultsWithEmptyResults() {
        when(userAssociationStorage.loadFollowingsUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(Collections.<Urn>emptyList()));
        when(userAssociationStorage.loadFollowings(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshFollowings()).thenReturn(Observable.just(true));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(Collections.<PropertySet>emptyList());
    }

    @Test
    public void pagedFollowingsReturnsFollowingsFromStorage() {
        final List<PropertySet> pageOfFollowings = createPageOfFollowings(2);
        final List<Urn> urns = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));
        when(userAssociationStorage.loadFollowingsUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(urns));
        when(userAssociationStorage.loadFollowings(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(pageOfFollowings));
        when(syncInitiator.refreshFollowings()).thenReturn(Observable.<Boolean>empty());
        when(syncInitiator.syncUsers(urns)).thenReturn(Observable.just(SyncResult.success("success", true)));

        operations.pagedFollowings().subscribe(subscriber);

        subscriber.assertValue(pageOfFollowings);
    }

    @Test
    public void followingsPagerLoadsNextPageUsingPositionOfLastItemOfPreviousPage() {
        final List<PropertySet> firstPage = createPageOfFollowings(PAGE_SIZE);
        final List<PropertySet> secondPage = createPageOfFollowings(1);
        final List<Urn> followingsUrn = pageOfUrns(firstPage);
        final long position = firstPage.get(PAGE_SIZE - 1).get(UserAssociationProperty.POSITION);

        when(userAssociationStorage.loadFollowingsUrns(PAGE_SIZE, position)).thenReturn(Observable.just(followingsUrn));
        when(userAssociationStorage.loadFollowings(PAGE_SIZE, position)).thenReturn(Observable.just(secondPage));

        when(syncInitiator.refreshFollowings()).thenReturn(Observable.<Boolean>empty());
        when(syncInitiator.syncUsers(followingsUrn)).thenReturn(Observable.just(SyncResult.success("success", true)));

        operations.followingsPagingFunction().call(firstPage).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(secondPage));
    }

    @Test
    public void followingsPagerFinishesIfLastPageIncomplete() {
        assertThat(operations.followingsPagingFunction().call(createPageOfFollowings(PAGE_SIZE - 1))).isEqualTo(Pager.finish());
    }

    @Test
    public void updatedFollowingsReloadsFollowingsAfterSyncWithChange() {
        final List<PropertySet> pageOfFollowings = createPageOfFollowings(2);
        final List<Urn> followingsUrn = Arrays.asList(Urn.forUser(123L), Urn.forUser(124L));

        when(userAssociationStorage.loadFollowingsUrns(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(followingsUrn));
        when(userAssociationStorage.loadFollowings(PAGE_SIZE, Consts.NOT_SET)).thenReturn(Observable.just(pageOfFollowings));
        when(syncInitiator.refreshFollowings()).thenReturn(Observable.just(true));
        when(syncInitiator.syncUsers(followingsUrn)).thenReturn(Observable.just(SyncResult.success("success", true)));

        operations.updatedFollowings().subscribe(subscriber);

        subscriber.assertValue(pageOfFollowings);
    }

    private List<PropertySet> createPageOfFollowings(int size) {
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(TestPropertySets.expectedFollowingForFollowingsScreen(i));
        }
        return page;
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

    private List<Urn> pageOfUrns(List<PropertySet> propertySets) {
        List<Urn> page = new ArrayList<>(propertySets.size());
        for (PropertySet propertySet : propertySets) {
            page.add(propertySet.get(UserProperty.URN));
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