package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.playback.playqueue.PlayQueueFixtures.getHeaderPlayQueueUiItem;
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

    @Mock private TrackPlayQueueItemRenderer trackRenderer;
    @Mock private HeaderPlayQueueItemRenderer headerRenderer;
    @Mock private MagicBoxPlayQueueItemRenderer magicBoxRenderer;
    @Mock private PlayQueueAdapter.NowPlayingListener nowPlayingListener;

    private final HeaderPlayQueueUIItem headerPlayQueueItem1 = getHeaderPlayQueueUiItem();
    private final HeaderPlayQueueUIItem headerPlayQueueItem2 = getHeaderPlayQueueUiItem();
    private final TrackPlayQueueUIItem playQueueItem1 = getPlayQueueItem(1);
    private final TrackPlayQueueUIItem playQueueItem2 = getPlayQueueItem(2);
    private final TrackPlayQueueUIItem playQueueItem3 = getPlayQueueItem(3);

    @Before
    public void setUp() throws Exception {
        adapter = new PlayQueueAdapter(trackRenderer, headerRenderer, magicBoxRenderer);

        playQueueItem2.setPlayState(PlayState.PLAYING);

        adapter.addItem(headerPlayQueueItem1);
        adapter.addItem(playQueueItem1);
        adapter.addItem(playQueueItem2);
        adapter.addItem(headerPlayQueueItem2);
        adapter.addItem(playQueueItem3);
    }

    @Test
    public void updateNowPlayingSetsDraggableStateOnItems() {
        adapter.updateNowPlaying(2, true);

        assertThat(headerPlayQueueItem1.getPlayState()).isEqualTo(PlayState.PLAYING);
        assertThat(headerPlayQueueItem2.getPlayState()).isEqualTo(PlayState.COMING_UP);
        assertThat(playQueueItem1.getPlayState()).isEqualTo(PlayState.PLAYED);
        assertThat(playQueueItem2.getPlayState()).isEqualTo(PlayState.PLAYING);
        assertThat(playQueueItem3.getPlayState()).isEqualTo(PlayState.COMING_UP);
    }

    @Test
    public void updateRepeatModeStateOnItems() {
        adapter.updateInRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);


        assertThat(headerPlayQueueItem1.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(headerPlayQueueItem2.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem1.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem2.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem3.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
    }

    @Test
    public void notifyNowPlayingListenerWhenPlayStateChangedToPlaying() {
        adapter.setNowPlayingChangedListener(nowPlayingListener);

        adapter.updateNowPlaying(1, true);

        verify(nowPlayingListener).onNowPlayingChanged(playQueueItem1);
    }

    @Test
    public void doNotNotifyNowPlayingListenerWhenPlayStateChangedToPlayingAndNotifyFlagSetToFalse() {
        adapter.setNowPlayingChangedListener(nowPlayingListener);

        adapter.updateNowPlaying(1, false);

        verify(nowPlayingListener, never()).onNowPlayingChanged(playQueueItem1);
    }

    @Test
    public void doNotUpdateWhenUpdatingTheSameTrack() {
        adapter.setNowPlayingChangedListener(nowPlayingListener);

        adapter.updateNowPlaying(2, true);

        verify(nowPlayingListener, never()).onNowPlayingChanged(playQueueItem1);
    }


}
