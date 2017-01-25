package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlayerPagerOnboardingPresenterTest {
    private final TestEventBus eventBus = new TestEventBus();

    @Mock private PlayerPagerOnboardingStorage storage;
    @Mock private PlayerFragment fragment;
    @Mock private PlayerTrackPager pager;

    private PlayerPagerOnboardingPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(fragment.getPlayerPager()).thenReturn(pager);
        when(pager.getChildCount()).thenReturn(2);
        presenter = new PlayerPagerOnboardingPresenter(
                storage,
                eventBus
        );
    }

    @Test
    public void showSkipOnboardingWhenPlayerExpends() {
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager).beginFakeDrag();
    }

    @Test
    public void doNotShowSkipOnboardingWhenPlayerCollasped() {
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());

        verify(pager, never()).beginFakeDrag();
    }

    @Test
    public void doNotShowSkipOnboardingWhenPlayedIsOffScreen() {
        presenter.onResume(fragment);
        presenter.onPause(fragment);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }

    @Test
    public void showSkipOnboardingOnly3Times() {
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, times(4)).beginFakeDrag();
    }

    @Test
    public void doNotShowSkipOnboardingWhenOnlyOnePage() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }
}
