package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.PlaylistPostOperations.PAGE_SIZE;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistPostOperationsTest {

    private PlaylistPostOperations operations;
    private List<PropertySet> postedPlaylists;

    @Mock private Observer<List<PropertySet>> observer;
    @Mock private LoadPostedPlaylistsCommand loadPostedPlaylistsCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;
    @Mock private PlaylistStorage playlistStorage;

    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new PlaylistPostOperations(
                playlistStorage,
                loadPostedPlaylistsCommand,
                syncInitiator,
                scheduler, networkConnectionHelper);

        postedPlaylists = Arrays.asList(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());

        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void postedPlaylistsReturnsPostedPlaylistsFromStorage() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());

        operations.postedPlaylists().subscribe(observer);

        verify(observer).onNext(postedPlaylists);
        verify(observer).onCompleted();
    }

    @Test
    public void postedPlaylistsRequestsUpdatesFromSyncerWhenOnWifi() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator).requestPlaylistSync(postedPlaylists);
    }

    @Test
    public void postedPlaylistsDoesNotRequestsUpdatesFromSyncerWhenOffWifi() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(false);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(postedPlaylists);
    }

    @Test
    public void postedPlaylistsRequestsDoesNotUpdateEmptyListFromSyncer() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(anyList());
    }

    @Test
    public void trackPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(firstPage), Observable.<List<PropertySet>>never());
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());

        operations.postedPlaylistsPager().page(operations.postedPlaylists()).subscribe(observer);
        operations.postedPlaylistsPager().next();

        final ChronologicalQueryParams params = loadPostedPlaylistsCommand.getInput();
        expect(params.getTimestamp()).toEqual(firstPage.get(PAGE_SIZE - 1).get(PlaylistProperty.CREATED_AT).getTime());
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.syncPlaylistPosts()).thenReturn(Observable.<SyncResult>empty());

        operations.postedPlaylistsPager().page(operations.postedPlaylists()).subscribe(observer);
        operations.postedPlaylistsPager().next();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(postedPlaylists);
        inOrder.verify(observer).onCompleted();
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        when(loadPostedPlaylistsCommand.toObservable()).thenReturn(Observable.just(postedPlaylists));
        when(syncInitiator.refreshPostedPlaylists()).thenReturn(Observable.just(true));

        operations.updatedPostedPlaylists().subscribe(observer);

        InOrder inOrder = inOrder(observer, syncInitiator);
        inOrder.verify(syncInitiator).refreshPostedPlaylists();
        inOrder.verify(observer).onNext(postedPlaylists);
        inOrder.verify(observer).onCompleted();
    }


    private List<PropertySet> createPageOfPostedPlaylists(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        }
        return page;
    }
}
