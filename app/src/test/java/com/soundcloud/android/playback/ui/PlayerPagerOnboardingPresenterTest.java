package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlayerPagerOnboardingPresenterTest {
    private final TestEventBus eventBus = new TestEventBus();

    @Mock private PlayerPagerOnboardingStorage storage;
    @Mock private PlayerFragment fragment;
    @Mock private PlayerTrackPager pager;
    @Mock private IntroductoryOverlayOperations introductoryOverlayOperations;
    @Mock private CastConnectionHelper castConnectionHelper;

    private PlayerPagerOnboardingPresenter presenter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(fragment.getPlayerPager()).thenReturn(pager);
        when(pager.getChildCount()).thenReturn(2);
        when(castConnectionHelper.isCasting()).thenReturn(false);
        presenter = new PlayerPagerOnboardingPresenter(
                storage,
                introductoryOverlayOperations,
                castConnectionHelper,
                eventBus
        );
    }

    @Test
    public void showSkipOnboardingWhenPlayerExpands() {
        when(introductoryOverlayOperations.wasOverlayShown(IntroductoryOverlayKey.PLAY_QUEUE)).thenReturn(true);

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
    public void doNotShowSkipWhenItHasBeenDisplayed3Times() {
        when(introductoryOverlayOperations.wasOverlayShown(IntroductoryOverlayKey.PLAY_QUEUE)).thenReturn(true);
        when(storage.numberOfOnboardingRun()).thenReturn(3);
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }

    @Test
    public void doNotShowSkipOnboardingWhenOnlyOnePage() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }

    @Test
    public void doNotShowSkipOnboardingWhenPlayQueueOverlayIsPending() {
        presenter.onResume(fragment);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }

    @Test
    public void showSkipOnboardingWhenPlayQueueOverlayIsNotPending() {
        when(introductoryOverlayOperations.wasOverlayShown(IntroductoryOverlayKey.PLAY_QUEUE)).thenReturn(true);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager).beginFakeDrag();
    }

    @Test
    public void doNotShowSkipOnboardingWhenPlayQueueOverlayWasPending() {
        when(introductoryOverlayOperations.wasOverlayShown(IntroductoryOverlayKey.PLAY_QUEUE))
                .thenReturn(false)
                .thenReturn(true);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }

    @Test
    public void showSkipOnboardingWhenCastingAndIntroOverlayWasAlreadyShown() {
        when(castConnectionHelper.isCasting()).thenReturn(true);

        presenter.onResume(fragment);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager).beginFakeDrag();
    }

    @Test
    public void doNotCrashIfPagerWasNotFound() {
        when(fragment.getPlayerPager()).thenReturn(null);
        presenter.onResume(fragment);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        verify(pager, never()).beginFakeDrag();
    }

}
