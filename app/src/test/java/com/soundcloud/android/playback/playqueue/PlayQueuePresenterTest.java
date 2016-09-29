package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueuePresenterTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayQueueAdapter adapter;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueArtworkController playerArtworkController;
    @Mock private PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory;
    @Mock private EventBus eventbus;
    @Mock private Context context;
    @Mock private PlayQueueUIItem item;
    @Mock private PlayQueueUIItemMapper playQueueUIItemMapper;
    @Mock private FeedbackController feedbackController;

    private PlayQueuePresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new PlayQueuePresenter(
                adapter,
                playQueueManager,
                playQueueOperations,
                playerArtworkController,
                swipeToRemoveCallbackFactory,
                eventbus,
                context,
                feedbackController,
                playQueueUIItemMapper);
        when(adapter.getItem(anyInt())).thenReturn(item);
        when(item.isTrack()).thenReturn(true);
    }

    @Test
    public void returnFalseWhenBeforeCurrentPQItem() {
        when(adapter.getQueuePosition(2)).thenReturn(2);
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenCurrentPQItem() {
        when(adapter.getQueuePosition(3)).thenReturn(3);
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(presenter.isRemovable(3)).isFalse();
    }

    @Test
    public void returnTrueWhenAfterCurrentPQItem() {
        when(adapter.getQueuePosition(4)).thenReturn(4);
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(presenter.isRemovable(4)).isTrue();
    }

}
