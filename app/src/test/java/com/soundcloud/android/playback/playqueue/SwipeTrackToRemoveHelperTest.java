package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlayQueueManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SwipeTrackToRemoveHelperTest {

    @Mock PlayQueueManager playQueueManager;
    private PlayQueuePresenter.SwipeTrackToRemoveHelper swipeTrackToRemoveHelper;

    @Before
    public void setUp() throws Exception {
        swipeTrackToRemoveHelper = new PlayQueuePresenter.SwipeTrackToRemoveHelper(playQueueManager);
    }

    @Test
    public void returnFalseWhenBeforeCurrentPQItem() {
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(swipeTrackToRemoveHelper.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenCurrentPQItem() {
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(swipeTrackToRemoveHelper.isRemovable(3)).isFalse();
    }

    @Test
    public void returnTrueWhenAfterCurrentPQItem() {
        when(playQueueManager.getCurrentTrackPosition()).thenReturn(3);

        assertThat(swipeTrackToRemoveHelper.isRemovable(4)).isTrue();
    }
}
