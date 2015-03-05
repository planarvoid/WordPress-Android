package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.PlaylistPostOperations.PAGE_SIZE;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

@RunWith(SoundCloudTestRunner.class)
public class PlaylistPostOperationsTest {

    private PlaylistPostOperations operations;
    private List<PropertySet> postedPlaylists;

    @Mock private LegacyLoadPostedPlaylistsCommand loadPostedPlaylistsCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;

    private Scheduler scheduler = Schedulers.immediate();
    private TestObserver<List<PropertySet>> observer;

    @Before
    public void setUp() throws Exception {
        operations = new PlaylistPostOperations(
                loadPostedPlaylistsCommand,
                syncInitiator,
                scheduler, networkConnectionHelper);

        postedPlaylists = Arrays.asList(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        observer = new TestObserver<>();

        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void syncAndLoadPlaylistPostsWhenInitialPlaylistPostsLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(Collections.<PropertySet>emptyList()), Observable.just(firstPage));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPlaylistPostsResultsWithEmptyResults() throws Exception {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(Collections.<PropertySet>emptyList());
    }

    @Test
    public void postedPlaylistsReturnsPostedPlaylistsFromStorage() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void postedPlaylistsRequestsUpdatesFromSyncerWhenOnWifi() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator).requestPlaylistSync(postedPlaylists);
    }

    @Test
    public void postedPlaylistsDoesNotRequestsUpdatesFromSyncerWhenOffWifi() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(false);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(postedPlaylists);
    }

    @Test
    public void postedPlaylistsRequestsDoesNotUpdateEmptyListFromSyncer() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(anyList());
    }

    @Test
    public void trackPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(firstPage), Observable.<List<PropertySet>>never());
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylistsPager().page(operations.postedPlaylists()).subscribe(observer);
        operations.postedPlaylistsPager().next();

        final ChronologicalQueryParams params = loadPostedPlaylistsCommand.getInput();
        expect(params.getTimestamp()).toEqual(firstPage.get(PAGE_SIZE - 1).get(PlaylistProperty.CREATED_AT).getTime());
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylistsPager().page(operations.postedPlaylists()).subscribe(observer);
        operations.postedPlaylistsPager().next();

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.updatedPostedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    private void expectObserverOnNextEventToEqual(List<PropertySet> firstPage) {
        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toEqual(firstPage);
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    private List<PropertySet> createPageOfPostedPlaylists(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        }
        return page;
    }
}
