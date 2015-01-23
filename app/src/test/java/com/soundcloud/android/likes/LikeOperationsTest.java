package com.soundcloud.android.likes;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LikeOperationsTest {

    private LikeOperations operations;

    @Mock private Observer<List<PropertySet>> observer;
    @Mock private LoadLikedTracksCommand loadLikedTracksCommand;
    @Mock private LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;
    @Mock private LoadLikedPlaylistsCommand loadLikedPlaylistsCommand;
    @Mock private SyncInitiator syncInitiator;

    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        operations = new LikeOperations(loadLikedTracksCommand, loadLikedTrackUrnsCommand, loadLikedPlaylistsCommand,
                syncInitiator, scheduler);
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

}