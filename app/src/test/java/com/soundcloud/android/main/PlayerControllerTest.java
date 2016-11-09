package com.soundcloud.android.main;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.ads.AdOrientationController;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.ads.AdViewabilityController;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.snackbar.FeedbackController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

public class PlayerControllerTest extends AndroidUnitTest {

    private PlayerController playerController;

    @Mock private SlidingPlayerController slidingPlayerController;
    @Mock private AdPlayerController adPlayerController;
    @Mock private AdOrientationController adOrienetationController;
    @Mock private AdViewabilityController adViewabilityController;
    @Mock private FeedbackController feedbackController;
    @Mock private AppCompatActivity appCompatActivity;

    @Before
    public void setUp() throws Exception {
        playerController = new PlayerController(slidingPlayerController,
                                                adPlayerController,
                                                adOrienetationController,
                                                adViewabilityController,
                                                feedbackController);
    }

    @Test
    public void onResumeRegistersWithFeedbackController() {
        playerController.onResume(appCompatActivity);

        verify(feedbackController).register(appCompatActivity, slidingPlayerController);
    }

    @Test
    public void onPauseClearsFeedbackController() {
        playerController.onPause(appCompatActivity);

        verify(feedbackController).clear();
    }
}
