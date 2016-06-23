package com.soundcloud.android.view.snackbar;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Feedback;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;
import android.view.View;

public class FeedbackControllerTest extends AndroidUnitTest {

    private FeedbackController feedbackController;

    @Mock private SlidingPlayerController playerController;
    @Mock private PlayerSnackBarWrapper playerSnackBarWrapper;
    @Mock private TopSnackBarWrapper defaultSnackBarWrapper;
    @Mock private FragmentActivity fragmentActivity;
    @Mock private FeatureFlags featureFlags;

    private EventBus eventBus = new TestEventBus();
    private View playerSnackBarView = new View(context());
    private View activitySnackBarView = new View(context());

    private Feedback feedback = Feedback.create(123);

    @Before
    public void setUp() throws Exception {
        feedbackController = new FeedbackController(playerController,
                                                    playerSnackBarWrapper,
                                                    defaultSnackBarWrapper,
                                                    eventBus,
                                                    featureFlags);
        when(playerController.getSnackbarHolder()).thenReturn(playerSnackBarView);
    }

    @Test
    public void doesNotShowFeedbackWhenPaused() {
        eventBus.publish(EventQueue.SHOW_FEEDBACK, feedback);

        verify(playerSnackBarWrapper, never()).show(any(View.class), any(Feedback.class));
    }

    @Test
    public void showsPlayerSnackBarWhenPlayerExpandedAndResumed() {
        when(playerController.isExpanded()).thenReturn(true);

        feedbackController.onResume(fragmentActivity);

        eventBus.publish(EventQueue.SHOW_FEEDBACK, feedback);

        verify(playerSnackBarWrapper).show(playerSnackBarView, feedback);
        verify(defaultSnackBarWrapper, never()).show(any(View.class), any(Feedback.class));
    }

    @Test
    public void doesNotShowsPlayerSnackBarWhenPlayerExpandedAndResumedThenPaused() {
        when(playerController.isExpanded()).thenReturn(true);

        feedbackController.onResume(fragmentActivity);
        feedbackController.onPause(fragmentActivity);

        eventBus.publish(EventQueue.SHOW_FEEDBACK, feedback);

        verify(playerSnackBarWrapper, never()).show(any(View.class), any(Feedback.class));
        verify(defaultSnackBarWrapper, never()).show(any(View.class), any(Feedback.class));
    }

    @Test
    public void showsActivitySnackBarWhenResumedAndPlayerNotExpanded() {
        when(fragmentActivity.findViewById(R.id.snackbar_holder)).thenReturn(activitySnackBarView);

        feedbackController.onResume(fragmentActivity);

        eventBus.publish(EventQueue.SHOW_FEEDBACK, feedback);

        verify(defaultSnackBarWrapper).show(activitySnackBarView, feedback);
        verify(playerSnackBarWrapper, never()).show(any(View.class), any(Feedback.class));
    }

    @Test
    public void showsActivitySnackBarWithContainerWhenResumedAndPlayerNotExpandedWithNoSnackBarHolderId() {
        when(fragmentActivity.findViewById(R.id.container)).thenReturn(activitySnackBarView);

        feedbackController.onResume(fragmentActivity);

        eventBus.publish(EventQueue.SHOW_FEEDBACK, feedback);

        verify(defaultSnackBarWrapper).show(activitySnackBarView, feedback);
        verify(playerSnackBarWrapper, never()).show(any(View.class), any(Feedback.class));
    }
}
