package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.PlayQueueFixtures.getPlayQueueItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayQueueAdapterTest extends AndroidUnitTest {

    private PlayQueueAdapter adapter;

    @Mock private TrackPlayQueueItemRenderer trackPlayQueueItemRenderer;
    @Mock private HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer;
    @Mock private PlayQueueAdapter.NowPlayingListener nowPlayingListener;

    private final TrackPlayQueueUIItem playQueueItem1 = getPlayQueueItem(1);
    private final TrackPlayQueueUIItem playQueueItem2 = getPlayQueueItem(2);
    private final TrackPlayQueueUIItem playQueueItem3 = getPlayQueueItem(3);

    @Before
    public void setUp() throws Exception {
        adapter = new PlayQueueAdapter(trackPlayQueueItemRenderer, headerPlayQueueItemRenderer);

        adapter.addItem(playQueueItem1);
        adapter.addItem(playQueueItem2);
        adapter.addItem(playQueueItem3);
    }

    @Test
    public void updateNowPlayingSetsDraggableStateOnItems() {
        adapter.updateNowPlaying(1, true);

        assertThat(playQueueItem1.getPlayState()).isEqualTo(TrackPlayQueueUIItem.PlayState.PLAYED);
        assertThat(playQueueItem2.getPlayState()).isEqualTo(TrackPlayQueueUIItem.PlayState.PLAYING);
        assertThat(playQueueItem3.getPlayState()).isEqualTo(TrackPlayQueueUIItem.PlayState.COMING_UP);
    }

    @Test
    public void updateRepeatModeStateOnItems() {
        adapter.updateInRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        assertThat(playQueueItem1.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem2.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem3.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
    }

    @Test
    public void notifyNowPlayingListenerWhenPlayStateChangedToPlaying() {
        adapter.setNowPlayingChangedListener(nowPlayingListener);

        adapter.updateNowPlaying(0, true);

        verify(nowPlayingListener).onNowPlayingChanged(playQueueItem1);
    }

    @Test
    public void doNotNotifyNowPlayingListenerWhenPlayStateChangedToPlayingAndNotifyFlagSetToFalse() {
        adapter.setNowPlayingChangedListener(nowPlayingListener);

        adapter.updateNowPlaying(0, false);

        verify(nowPlayingListener, never()).onNowPlayingChanged(playQueueItem1);
    }


}
