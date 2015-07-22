package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistPostOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistPostOperationsTest extends AndroidUnitTest {

    private PlaylistPostOperations operations;
    private List<PropertySet> postedPlaylists;

    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;

    private Scheduler scheduler = Schedulers.immediate();
    private TestObserver<List<PropertySet>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new PlaylistPostOperations(
                playlistPostStorage,
                syncInitiator,
                scheduler, networkConnectionHelper);

        postedPlaylists = Arrays.asList(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        observer = new TestObserver<>();

        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void syncAndLoadPlaylistPostsWhenInitialPlaylistPostsLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPlaylistPostsResultsWithEmptyResults() throws Exception {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(Collections.<PropertySet>emptyList());
    }

    @Test
    public void postedPlaylistsReturnsPostedPlaylistsFromStorage() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void postedPlaylistsRequestsUpdatesFromSyncerWhenOnWifi() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator).requestPlaylistSync(postedPlaylists);
    }

    @Test
    public void postedPlaylistsDoesNotRequestsUpdatesFromSyncerWhenOffWifi() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(false);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(postedPlaylists);
    }

    @Test
    public void postedPlaylistsRequestsDoesNotUpdateEmptyListFromSyncer() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(anyList());
    }

    @Test
    public void trackPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PlaylistProperty.CREATED_AT).getTime();
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, time)).thenReturn(Observable.<List<PropertySet>>never());
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylistsPager().page(operations.postedPlaylists()).subscribe(observer);
        operations.postedPlaylistsPager().next();

        verify(playlistPostStorage).loadPostedPlaylists(PAGE_SIZE, time);
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylistsPager().page(operations.postedPlaylists()).subscribe(observer);
        operations.postedPlaylistsPager().next();

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.updatedPostedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void updatedPostedPlaylistsRequestsUpdatesFromSyncerWhenOnWifi() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.updatedPostedPlaylists().subscribe(observer);

        verify(syncInitiator).requestPlaylistSync(postedPlaylists);
    }

    private void expectObserverOnNextEventToEqual(List<PropertySet> firstPage) {
        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEqualTo(firstPage);
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    private List<PropertySet> createPageOfPostedPlaylists(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        }
        return page;
    }
}
