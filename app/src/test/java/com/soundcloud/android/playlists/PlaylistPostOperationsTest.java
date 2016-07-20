package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistPostOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistPostOperationsTest extends AndroidUnitTest {

    private PlaylistPostOperations operations;
    private List<PropertySet> postedPlaylists;

    @Mock private PlaylistPostStorage playlistPostStorage;
    @Mock private LegacySyncInitiator legacySyncInitiator;
    @Mock private SyncInitiator syncInitiator;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;
    private TestEventBus eventBus;

    private Scheduler scheduler = Schedulers.immediate();
    private TestObserver<List<PropertySet>> observer;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        operations = new PlaylistPostOperations(
                playlistPostStorage,
                legacySyncInitiator,
                scheduler,
                syncInitiator,
                networkConnectionHelper,
                eventBus);

        postedPlaylists = Collections.singletonList(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        observer = new TestObserver<>();

        when(legacySyncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void syncAndLoadPlaylistPostsWhenInitialPlaylistPostsLoadReturnsEmptyList() {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE,
                                                     Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()),
                                                                                 Observable.just(firstPage));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.just(true));

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(firstPage);
    }

    @Test
    public void syncAndLoadEmptyPlaylistPostsResultsWithEmptyResults() throws Exception {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE,
                                                     Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.just(true));

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(Collections.<PropertySet>emptyList());
    }

    @Test
    public void postedPlaylistsReturnsPostedPlaylistsFromStorage() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(
                postedPlaylists));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void postedPlaylistsRequestsUpdatesFromSyncerWhenOnWifi() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(
                postedPlaylists));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.<Boolean>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator).batchSyncPlaylists(PropertySets.extractUrns(postedPlaylists));
    }

    @Test
    public void postedPlaylistsDoesNotRequestsUpdatesFromSyncerWhenOffWifi() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(
                postedPlaylists));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.<Boolean>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(false);

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).batchSyncPlaylists(anyList());
    }

    @Test
    public void postedPlaylistsRequestsDoesNotUpdateEmptyListFromSyncer() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE,
                                                     Long.MAX_VALUE)).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.postedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).batchSyncPlaylists(anyList());
    }

    @Test
    public void trackPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPostedPlaylists(PAGE_SIZE);
        final long time = firstPage.get(PAGE_SIZE - 1).get(PostProperty.CREATED_AT).getTime();
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(firstPage));
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE,
                                                     time)).thenReturn(Observable.<List<PropertySet>>never());
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.<Boolean>empty());

        operations.pagingFunction().call(firstPage);

        verify(playlistPostStorage).loadPostedPlaylists(PAGE_SIZE, time);
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(
                postedPlaylists));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.<Boolean>empty());

        assertThat(operations.pagingFunction().call(postedPlaylists)).isSameAs(Pager.finish());
    }

    @Test
    public void updatedPostedPlaylistsReloadsPostedPlaylistsAfterSyncWithChange() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(
                postedPlaylists));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.just(true));

        operations.updatedPostedPlaylists().subscribe(observer);

        expectObserverOnNextEventToEqual(postedPlaylists);
    }

    @Test
    public void updatedPostedPlaylistsRequestsUpdatesFromSyncerWhenOnWifi() {
        when(playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)).thenReturn(Observable.just(
                postedPlaylists));
        when(legacySyncInitiator.refreshMyPlaylists()).thenReturn(Observable.just(true));
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.updatedPostedPlaylists().subscribe(observer);

        verify(syncInitiator).batchSyncPlaylists(PropertySets.extractUrns(postedPlaylists));
    }

    @Test
    public void removeShouldRemoveLocalPlaylist() {
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(playlistPostStorage.remove(localPlaylist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(localPlaylist).subscribe();

        verify(playlistPostStorage).remove(localPlaylist);
    }

    @Test
    public void removeShouldMarkForRemovalSyncedPlaylist() {
        final Urn playlist = Urn.forPlaylist(123);
        when(playlistPostStorage.markPendingRemoval(playlist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(playlist).subscribe();

        verify(playlistPostStorage).markPendingRemoval(playlist);
    }

    @Test
    public void removeShouldTriggerMyPlaylistSync() {
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(playlistPostStorage.remove(localPlaylist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(localPlaylist).subscribe();

        verify(legacySyncInitiator).requestSystemSync();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingPlaylist() {
        final Urn playlist = Urn.forPlaylist(213L);
        when(playlistPostStorage.markPendingRemoval(playlist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(playlist).subscribe();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.ENTITY_DELETED);
        assertThat(event.getFirstUrn()).isEqualTo(playlist);
    }

    private void expectObserverOnNextEventToEqual(List<PropertySet> firstPage) {
        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEqualTo(firstPage);
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    private List<PropertySet> createPageOfPostedPlaylists(int size) {
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            page.add(TestPropertySets.expectedPostedPlaylistsForPostedPlaylistsScreen());
        }
        return page;
    }
}
