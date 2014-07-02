package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.PlayerArtworkController.PlayerArtworkControllerFactory;
import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class PlayerArtworkControllerTest {
    private PlayerArtworkController playerArtworkController;

    @Mock private ProgressAnimationControllerFactory animationControllerFactory;
    @Mock private PlayerArtworkView playerArtworkView;
    @Mock private ProgressController progressController;
    @Mock private ImageView wrappedImageView;
    @Mock private View artworkIdleOverlay;
    @Mock private PlaybackProgress playbackProgress;

    @Before
    public void setUp() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB); // 9 old Androids
        when(playerArtworkView.findViewById(R.id.artwork_image_view)).thenReturn(wrappedImageView);
        when(animationControllerFactory.create(wrappedImageView)).thenReturn(progressController);
        when(playerArtworkView.findViewById(R.id.artwork_overlay)).thenReturn(artworkIdleOverlay);
        playerArtworkController = new PlayerArtworkControllerFactory(animationControllerFactory).create(playerArtworkView);
    }

    @Test
    public void showPlayingStateStartsProgressAnimationWithProgressArtument() {
        playerArtworkController.showPlayingState(playbackProgress);
        verify(progressController).startProgressAnimation(playbackProgress);
    }

    @Test
    public void showPlayingStateDoesNotStartProgressAnimationIfScrubbing() {
        playerArtworkController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        playerArtworkController.showPlayingState(playbackProgress);
        verify(progressController, never()).startProgressAnimation(any(PlaybackProgress.class));
    }

    @Test
    public void scrubStateCancelledStartsProgressAnimationFromLastPositionIfPlaying() {
        playerArtworkController.showPlayingState(playbackProgress);
        PlaybackProgress latest = new PlaybackProgress(5, 10);

        playerArtworkController.setProgress(latest);
        playerArtworkController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(progressController).startProgressAnimation(latest);
    }

    @Test
    public void scrubStateCancelledDoesNotStartAnimationsIfNotPlaying() {
        playerArtworkController.scrubStateChanged(SCRUB_STATE_CANCELLED);
        verify(progressController, never()).startProgressAnimation(any(PlaybackProgress.class));
    }

    @Test
    public void showSessionActiveStateCancelsExistingAnimations() {
        playerArtworkController.showSessionActiveState();
        verify(progressController).cancelProgressAnimation();
    }

    @Test
    public void showIdleStateCancelsProgressAnimation() {
        playerArtworkController.showIdleState();
        verify(progressController).cancelProgressAnimation();
    }

    @Test
     public void setProgressSetsProgressOnController() {
        playerArtworkController.setProgress(playbackProgress);
        verify(progressController).setPlaybackProgress(playbackProgress);
    }

    @Test
    public void setProgressDoesNotSetProgressOnControllerWhileScrubbing() {
        playerArtworkController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        playerArtworkController.setProgress(playbackProgress);
        verify(progressController, never()).setPlaybackProgress(any(PlaybackProgress.class));
    }

    @Test
    public void scrubbingStateCancelsProgressAnimations() {
        playerArtworkController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        verify(progressController).cancelProgressAnimation();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void displayScrubPositionUsesHelperToSetImageViewPosition() {
        when(wrappedImageView.getMeasuredWidth()).thenReturn(20);
        when(playerArtworkView.getWidth()).thenReturn(10);
        playerArtworkController.onArtworkSizeChanged();
        playerArtworkController.displayScrubPosition(.5f);
        verify(wrappedImageView).setTranslationX(-5);
    }
}