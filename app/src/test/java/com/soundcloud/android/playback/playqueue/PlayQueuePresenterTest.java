package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueuePresenterTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayQueueRecyclerItemAdapter adapter;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueArtworkController playerArtworkController;
    @Mock private PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory;
    @Mock private EventBus eventbus;
    private PlayQueuePresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new PlayQueuePresenter(
                adapter,
                playQueueManager,
                playQueueOperations,
                playerArtworkController,
                swipeToRemoveCallbackFactory,
                eventbus
        );
    }

    @Test
    public void returnFalseWhenBeforeCurrentPQItem() {
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenCurrentPQItem() {
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(presenter.isRemovable(3)).isFalse();
    }

    @Test
    public void returnTrueWhenAfterCurrentPQItem() {
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(presenter.isRemovable(4)).isTrue();
    }
}
