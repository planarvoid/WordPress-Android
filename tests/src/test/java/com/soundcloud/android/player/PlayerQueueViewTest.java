package com.soundcloud.android.player;

import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.playback.PlayQueue;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerQueueViewTest {

    PlayerQueueView playerQueueView;

    @Mock
    PlayQueue playQueue;
    @Mock
    PlayerTrackView playerTrackView;
    @Mock
    EmptyListView emptyListView;

    @Before
    public void setUp() throws Exception {
        playerQueueView = new PlayerQueueView(Robolectric.application) {
            @Override
            protected PlayerTrackView createPlayerTrackView(Context context) {
                return playerTrackView;
            }

            @Override
            protected EmptyListView createEmptyListView(Context context) {
                return emptyListView;
            }
        };

        // TODO, remove with RL2. cant addView on a mock successfully yet
        when(playerTrackView.getParent()).thenReturn(playerQueueView);
        when(emptyListView.getParent()).thenReturn(playerQueueView);
    }

//    @Test
//    public void shouldCreateNewPlayerTrackViewFromPlayQueueItem() throws Exception {
//        final Track track = new Track(123L);
//        final PlayQueueItem playQueueItem = new PlayQueueItem(Observable.just(track), 0);
//
//        playerQueueView.setPlayQueueItem(playQueueItem, false);
//        verify(playerTrackView).setPlayQueueItem(playQueueItem);
//
//    }
//
//    @Test
//    public void shouldSetWaitingStateOnEmptyView() throws Exception {
//        final PlayQueueItem playQueueItem = PlayQueueItem.empty(0);
//        //when(playQueueManager.isLoading()).thenReturn(true);
//
//        playerQueueView.setPlayQueueItem(playQueueItem, false);
//        verify(emptyListView).setStatus(EmptyListView.Status.WAITING);
//    }
//
//    @Test
//    public void shouldSetUnknownErrorStateOnEmptyView() throws Exception {
//        final PlayQueueItem playQueueItem = PlayQueueItem.empty(0);
//        //when(playQueueManager.isLoading()).thenReturn(false);
//        //when(playQueueManager.lastLoadFailed()).thenReturn(true);
//
//        playerQueueView.setPlayQueueItem(playQueueItem, false);
//        verify(emptyListView).setStatus(EmptyListView.Status.ERROR);
//    }
//
//    @Test
//    public void shouldReturnEmptyViewWhenTrackIsNull() throws Exception {
//        final PlayQueueItem playQueueItem = PlayQueueItem.empty(0);
//        playerQueueView.setPlayQueueItem(playQueueItem, false);
//        verifyZeroInteractions(playerTrackView);
//    }
//
//    @Test
//    public void shouldSetCommentingPosition() throws Exception {
//        final Track track = new Track(123L);
//        final PlayQueueItem playQueueItem = new PlayQueueItem(Observable.just(track), 0);
//
//        playerQueueView.setPlayQueueItem(playQueueItem, true);
//        verify(playerTrackView).setCommentMode(true);
//    }
}
