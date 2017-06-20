package com.soundcloud.android.introductoryoverlay;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.GoOnboardingTooltipEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class IntroductoryOverlayPresenterTest extends AndroidUnitTest {
    private static final String FAKE_KEY = "key";
    private Optional<TrackingEvent> EVENT = Optional.of(GoOnboardingTooltipEvent.forListenOfflineLikes());

    @Mock private IntroductoryOverlayOperations operations;
    @Mock private EventBusV2 eventBus;
    @Mock private View overlayTargetView;

    private IntroductoryOverlayPresenter presenter;
    private IntroductoryOverlay introductoryOverlay;

    @Before
    public void setUp() {
        when(overlayTargetView.getContext()).thenReturn(activity());

        presenter = new IntroductoryOverlayPresenter(operations, resources(), eventBus);
        introductoryOverlay = IntroductoryOverlay.builder()
                                                 .overlayKey(FAKE_KEY)
                                                 .targetView(overlayTargetView)
                                                 .title(R.string.cast_introductory_overlay_title)
                                                 .description(R.string.cast_introductory_overlay_description)
                                                 .build();
    }

    @Test
    public void noOpIfOverlayWasAlreadyShown() {
        when(operations.wasOverlayShown(FAKE_KEY)).thenReturn(true);

        presenter.showIfNeeded(introductoryOverlay);

        verify(operations, never()).setOverlayShown(FAKE_KEY);
    }

    @Test
    public void markOverlayAsShownAfterFirstTimeShown() {
        when(operations.wasOverlayShown(FAKE_KEY)).thenReturn(false);

        presenter.showIfNeeded(introductoryOverlay);

        verify(operations).setOverlayShown(FAKE_KEY);
    }

    @Test
    public void noOpIfEventNotPresent() {
        when(operations.wasOverlayShown(FAKE_KEY)).thenReturn(false);

        presenter.showIfNeeded(introductoryOverlay);

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void sendEventIfPresent() {
        introductoryOverlay = IntroductoryOverlay.builder()
                                                 .overlayKey(FAKE_KEY)
                                                 .targetView(overlayTargetView)
                                                 .title(R.string.overlay_listen_offline_likes_title)
                                                 .description(R.string.overlay_listen_offline_likes_description)
                                                 .event(EVENT)
                                                 .build();
        when(operations.wasOverlayShown(FAKE_KEY)).thenReturn(false);

        presenter.showIfNeeded(introductoryOverlay);

        verify(eventBus).publish(EventQueue.TRACKING, EVENT.get());
    }
}
