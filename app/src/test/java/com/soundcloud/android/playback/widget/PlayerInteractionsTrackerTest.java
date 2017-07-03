package com.soundcloud.android.playback.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaybackActionSource;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlayerInteractionsTrackerTest {

    @Mock EventTracker eventTracker;
    @Captor ArgumentCaptor<UIEvent> uiEventArgumentCaptor;

    private PlayerInteractionsTracker playerInteractionsTracker;

    @Before
    public void setUp() throws Exception {
        playerInteractionsTracker = new PlayerInteractionsTracker(eventTracker);
    }

    @Test
    public void onSwipeForwardShouldTrackClickEvent() {
        playerInteractionsTracker.swipeForward(PlaybackActionSource.FULL);

        assertEvent(UIEvent.ClickName.SWIPE_FORWARD, UIEvent.PlayerInterface.FULLSCREEN);
    }

    @Test
    public void onSwipeBackwardShouldTrackClickEvent() {
        playerInteractionsTracker.swipeBackward(PlaybackActionSource.FULL);

        assertEvent(UIEvent.ClickName.SWIPE_BACKWARD, UIEvent.PlayerInterface.FULLSCREEN);
    }

    @Test
    public void onClickForwardShouldTrackClickEvent() {
        playerInteractionsTracker.clickForward(PlaybackActionSource.FULL);

        assertEvent(UIEvent.ClickName.CLICK_FORWARD, UIEvent.PlayerInterface.FULLSCREEN);
    }

    @Test
    public void onClickBackwardShouldTrackClickEvent() {
        playerInteractionsTracker.clickBackward(PlaybackActionSource.FULL);

        assertEvent(UIEvent.ClickName.CLICK_BACKWARD, UIEvent.PlayerInterface.FULLSCREEN);
    }

    private void assertEvent(UIEvent.ClickName clickName, UIEvent.PlayerInterface playerInterface) {
        verify(eventTracker).trackClick(uiEventArgumentCaptor.capture());

        UIEvent uiEvent = uiEventArgumentCaptor.getValue();

        assertThat(uiEvent.clickName().get()).isEqualTo(clickName);
        assertThat(uiEvent.playerInterface().get()).isEqualTo(playerInterface);
        assertThat(uiEvent.clickCategory().get()).isEqualTo(UIEvent.ClickCategory.PLAYER);
    }
}
