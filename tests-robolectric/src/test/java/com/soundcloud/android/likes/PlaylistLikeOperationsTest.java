package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.likes.LikeOperations.PAGE_SIZE;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistLikeOperationsTest {

    private PlaylistLikeOperations operations;

    @Mock private Observer<List<PropertySet>> observer;
    @Mock private PlaylistLikesStorage storage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new PlaylistLikeOperations(
                storage, syncInitiator,
                eventBus,
                scheduler, networkConnectionHelper);
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
     public void syncAndLoadPlaylistLikesWhenInitialPlaylistLoadReturnsEmptyList() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.just(SyncResult.success("action", false)));

        operations.likedPlaylists().subscribe(observer);

        verify(observer).onNext(likedPlaylists);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotSyncPlaylistOnEmptySecondPage() {
        final List<PropertySet> firstPage = createPageOfPlaylistLikes(PAGE_SIZE);
        when(storage.loadLikedPlaylists(anyInt(), anyLong())).thenReturn(Observable.just(firstPage), Observable.just(Collections.<PropertySet>emptyList()));
        final PublishSubject<SyncResult> syncObservable = PublishSubject.create();
        when(syncInitiator.syncPlaylistLikes()).thenReturn(syncObservable);

        final Pager.PagingFunction<List<PropertySet>> listPager = operations.pagingFunction();
        listPager.call(firstPage).subscribe(observer);

        expect(syncObservable.hasObservers()).toBeFalse();
    }

    @Test
    public void likedPlaylistsReturnsLikedPlaylistsFromStorage() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylists().subscribe(observer);

        verify(observer).onNext(likedPlaylists);
        verify(observer).onCompleted();
    }

    @Test
    public void likedPlaylistsRequestsUpdatesFromSyncer() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.likedPlaylists().subscribe(observer);

        verify(syncInitiator).requestPlaylistSync(likedPlaylists);
    }

    @Test
    public void likedPlaylistsDoesNotRequestUpdatesFromSyncerWhenOffWifi() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(likedPlaylists);
    }

    @Test
    public void likedPlaylistsDoesNotUpdateEmptyPageWithSyncer() {
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(anyList());
    }

    @Test
    public void playlistPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPlaylistLikes(PAGE_SIZE);
        final long lastTimestampOfFirstPage = firstPage.get(firstPage.size() - 1).get(LikeProperty.CREATED_AT).getTime();
        when(storage.loadLikedPlaylists(anyInt(), anyLong())).thenReturn(Observable.just(firstPage));

        operations.pagingFunction().call(firstPage).subscribe(observer);

        InOrder inOrder = inOrder(storage);
        inOrder.verify(storage).loadLikedPlaylists(PAGE_SIZE, lastTimestampOfFirstPage);
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        final List<PropertySet> firstPage = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        expect(operations.pagingFunction().call(firstPage)).toBe(Pager.<List<PropertySet>>finish());
    }

    @Test
    public void updatedLikedPlaylistsReloadsLikedPlaylistsAfterSyncWithChange() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.just(SyncResult.success("any intent action", true)));

        operations.updatedLikedPlaylists().subscribe(observer);

        InOrder inOrder = inOrder(observer, syncInitiator);
        inOrder.verify(syncInitiator).syncPlaylistLikes();
        inOrder.verify(observer).onNext(likedPlaylists);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void syncAndLoadEmptyPlaylistLikesResults() {
        when(storage.loadLikedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.just(SyncResult.success("action", false)));

        operations.likedPlaylists().subscribe(observer);

        verify(observer).onNext(Collections.<PropertySet>emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void onPlaylistLikedEventReturnsPlaylistInfoFromLike() throws Exception {
        final PropertySet likedPlaylist = TestPropertySets.expectedLikedPlaylistForPlaylistsScreen();
        final Urn playlistUrn = likedPlaylist.get(PlaylistProperty.URN);
        when(storage.loadLikedPlaylist(playlistUrn)).thenReturn(Observable.just(likedPlaylist));

        final TestObserver<PropertySet> observer = new TestObserver<>();
        operations.onPlaylistLiked().subscribe(observer);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(playlistUrn, true, 5));

        expect(observer.getOnNextEvents()).toContainExactly(likedPlaylist);
    }

    @Test
    public void onPlaylistUnlikedEventReturnsUnlikedPlaylistUrn() throws Exception {
        final Urn unlikedPlaylistUrn = Urn.forPlaylist(123L);
        final TestObserver<Urn> observer = new TestObserver<>();
        operations.onPlaylistUnliked().subscribe(observer);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(unlikedPlaylistUrn, false, 5));

        expect(observer.getOnNextEvents()).toContainExactly(unlikedPlaylistUrn);
    }

    private List<PropertySet> createPageOfPlaylistLikes(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        }
        return page;
    }
}