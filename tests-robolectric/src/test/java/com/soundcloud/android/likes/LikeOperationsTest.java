package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.likes.LikeOperations.PAGE_SIZE;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LikeOperationsTest {

    private LikeOperations operations;

    @Mock private Observer<List<PropertySet>> observer;
    @Mock private LoadLikedTracksCommand loadLikedTracksCommand;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    @Mock private LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    @Mock private UpdateLikeCommand storeLikeCommand;
    @Mock private SyncInitiator syncInitiator;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private Action0 requestSystemSyncAction;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new LikeOperations(loadLikedTracksCommand,
                loadLikedTrackUrnsCommand,
                loadLikedPlaylistsCommand,
                storeLikeCommand,
                syncInitiator,
                eventBus,
                scheduler, networkConnectionHelper);
        when(storeLikeCommand.toObservable()).thenReturn(
                Observable.just(TestPropertySets.likedTrack(Urn.forTrack(123))));
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
    }

    @Test
    public void likedTracksReturnsLikedTracksFromStorage() {
        List<PropertySet> likedTracks = Arrays.asList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(likedTracks));
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedTracks().subscribe(observer);

        verify(observer).onNext(likedTracks);
        verify(observer).onCompleted();
    }

    @Test
    public void likedTracksRequestsUpdatesFromSyncer() {
        List<PropertySet> likedTracks = Arrays.asList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(likedTracks));
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.<SyncResult>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator).requestTracksSync(likedTracks);
    }

    @Test
    public void likedTracksDoesNotRequestsUpdatesFromSyncerWhenOffWifi() {
        List<PropertySet> likedTracks = Arrays.asList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(likedTracks));
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator, never()).requestTracksSync(likedTracks);
    }

    @Test
    public void likedTracksRequestsDoesNotUpdateEmptyListFromSyncer() {
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedTracks().subscribe(observer);

        verify(syncInitiator, never()).requestTracksSync(anyList());
    }

    @Test
    public void trackPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfTrackLikes(PAGE_SIZE);
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(firstPage), Observable.<List<PropertySet>>never());
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedTracksPager().page(operations.likedTracks()).subscribe(observer);
        operations.likedTracksPager().next();

        final ChronologicalQueryParams params = loadLikedTracksCommand.getInput();
        expect(params.getTimestamp()).toEqual(firstPage.get(PAGE_SIZE - 1).get(LikeProperty.CREATED_AT).getTime());
    }

    @Test
    public void trackPagerFinishesIfLastPageIncomplete() throws Exception {

        final List<PropertySet> firstPage = Arrays.asList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(firstPage));
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedTracksPager().page(operations.likedTracks()).subscribe(observer);
        operations.likedTracksPager().next();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(observer).onCompleted();
    }

    @Test
    public void updatedLikedTracksReloadsLikedTracksAfterSyncWithChange() {
        List<PropertySet> likedTracks = Arrays.asList(TestPropertySets.expectedLikedTrackForLikesScreen());
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(likedTracks));
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.just(SyncResult.success("any intent action", true)));

        operations.updatedLikedTracks().subscribe(observer);

        InOrder inOrder = inOrder(observer, syncInitiator);
        inOrder.verify(syncInitiator).syncTrackLikes();
        inOrder.verify(observer).onNext(likedTracks);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void likedPlaylistsReturnsLikedTracksFromStorage() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylists().subscribe(observer);

        verify(observer).onNext(likedPlaylists);
        verify(observer).onCompleted();
    }

    @Test
    public void likedPlaylistsRequestsUpdatesFromSyncer() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        operations.likedPlaylists().subscribe(observer);

        verify(syncInitiator).requestPlaylistSync(likedPlaylists);
    }

    @Test
    public void likedPlaylistsDoesNotRequestUpdatesFromSyncerWhenOffWifi() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(likedPlaylists);
    }

    @Test
    public void likedPlaylistsDoesNotUpdateEmptyPageWithSyncer() {
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(Collections.<PropertySet>emptyList()));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylists().subscribe(observer);

        verify(syncInitiator, never()).requestPlaylistSync(anyList());
    }

    @Test
    public void playlistPagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> firstPage = createPageOfPlaylistLikes(PAGE_SIZE);
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(firstPage), Observable.<List<PropertySet>>never());
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylistsPager().page(operations.likedPlaylists()).subscribe(observer);
        operations.likedPlaylistsPager().next();

        final ChronologicalQueryParams params = loadLikedPlaylistsCommand.getInput();
        expect(params.getTimestamp()).toEqual(firstPage.get(PAGE_SIZE - 1).get(LikeProperty.CREATED_AT).getTime());
    }

    @Test
    public void playlistPagerFinishesIfLastPageIncomplete() throws Exception {

        final List<PropertySet> firstPage = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(firstPage));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.<SyncResult>empty());

        operations.likedPlaylistsPager().page(operations.likedPlaylists()).subscribe(observer);
        operations.likedPlaylistsPager().next();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(observer).onCompleted();
    }

    @Test
    public void updatedLikedPlaylistsReloadsLikedPlaylistsAfterSyncWithChange() {
        List<PropertySet> likedPlaylists = Arrays.asList(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        when(loadLikedPlaylistsCommand.toObservable()).thenReturn(Observable.just(likedPlaylists));
        when(syncInitiator.syncPlaylistLikes()).thenReturn(Observable.just(SyncResult.success("any intent action", true)));

        operations.updatedLikedPlaylists().subscribe(observer);

        InOrder inOrder = inOrder(observer, syncInitiator);
        inOrder.verify(syncInitiator).syncPlaylistLikes();
        inOrder.verify(observer).onNext(likedPlaylists);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void toggleToggleLikeAddsNewLike() {
        final PropertySet track = TestPropertySets.unlikedTrack(Urn.forTrack(123L));
        operations.addLike(track).subscribe();

        verify(storeLikeCommand).toObservable();
        final PropertySet input = storeLikeCommand.getInput();
        expect(input.contains(LikeProperty.ADDED_AT)).toBeTrue();
        expect(input.contains(LikeProperty.CREATED_AT)).toBeTrue();
        expect(input.get(PlayableProperty.IS_LIKED)).toBeTrue();
    }

    @Test
    public void toggleToggleLikeRemovesLike() {
        final PropertySet track = TestPropertySets.likedTrack(Urn.forTrack(123L));

        operations.removeLike(track).subscribe();

        verify(storeLikeCommand).toObservable();
        final PropertySet input = storeLikeCommand.getInput();
        expect(input.contains(LikeProperty.REMOVED_AT)).toBeTrue();
        expect(input.contains(LikeProperty.CREATED_AT)).toBeTrue();
        expect(input.get(PlayableProperty.IS_LIKED)).toBeFalse();
    }

    @Test
    public void togglingLikePublishesPlayableChangedEvent() {
        final PropertySet track = TestPropertySets.likedTrack(Urn.forTrack(123L));

        operations.addLike(track).subscribe();

        EntityStateChangedEvent event = eventBus.firstEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getNextUrn()).toEqual(track.get(PlayableProperty.URN));
        expect(event.getNextChangeSet().contains(PlayableProperty.IS_LIKED)).toBeTrue();
        expect(event.getNextChangeSet().contains(PlayableProperty.LIKES_COUNT)).toBeTrue();
    }

    @Test
    public void togglingLikeRequestsSystemSync() {
        final PropertySet track = TestPropertySets.likedTrack(Urn.forTrack(123L));

        operations.addLike(track).subscribe();

        verify(requestSystemSyncAction).call();
    }


    private List<PropertySet> createPageOfTrackLikes(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedLikedTrackForLikesScreen());
        }
        return page;
    }

    private List<PropertySet> createPageOfPlaylistLikes(int size){
        List<PropertySet> page = new ArrayList<>(size);
        for (int i = 0; i < size; i++){
            page.add(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen());
        }
        return page;
    }
}