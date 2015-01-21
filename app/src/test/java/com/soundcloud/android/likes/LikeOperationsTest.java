package com.soundcloud.android.likes;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playlists.LoadLikedPlaylistsCommand;
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
    public void likedTracksReturnsLikedTracksFromStorage() throws Exception {
        List<PropertySet> trackList = Arrays.asList(TestPropertySets.expectedTrackForPlayer());
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(trackList));

        operations.likedTracks().subscribe(observer);

        verify(observer).onNext(trackList);
        verify(observer).onCompleted();
    }

    @Test
    public void updatedLikedTracksReloadsLikedTracksAfterSyncWithChange() throws Exception {
        List<PropertySet> trackList = Arrays.asList(TestPropertySets.expectedTrackForPlayer());
        when(syncInitiator.syncTrackLikes()).thenReturn(Observable.just(SyncResult.success("asdf", true)));
        when(loadLikedTracksCommand.toObservable()).thenReturn(Observable.just(trackList));

        operations.updatedLikedTracks().subscribe(observer);

        InOrder inOrder = inOrder(observer, loadLikedTracksCommand, syncInitiator);
        inOrder.verify(syncInitiator).syncTrackLikes();
        inOrder.verify(loadLikedTracksCommand).toObservable();
        inOrder.verify(observer).onNext(trackList);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}