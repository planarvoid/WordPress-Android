package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import javax.inject.Provider;

public class PlayerArtworkControllerTest extends AndroidUnitTest {
    private static final long FULL_DURATION = 10000L;
    private PlayerArtworkController playerArtworkController;

    @Mock private ProgressController.Factory animationControllerFactory;
    @Mock private PlayerTrackArtworkView playerTrackArtworkView;
    @Mock private ProgressController progressController;
    @Mock private ImageView artworkOverlayImage;
    @Mock private View artworkIdleOverlay;
    @Mock private ImageView wrappedImageView;
    @Mock private PlaybackProgress playbackProgress;
    @Mock private PlayerArtworkLoader playerArtworkLoader;
    @Mock private ViewVisibilityProvider viewVisibilityProvider;
    @Mock private ImageResource imageResource;

    private Provider<PlayerArtworkLoader> playerArtworkLoaderProvider;
    private View artworkHolder;

    @Before
    public void setUp() throws Exception {
        artworkHolder = new FrameLayout(context());

        when(playerTrackArtworkView.getArtworkHolder()).thenReturn(artworkHolder);
        when(playerTrackArtworkView.getWrappedImageView()).thenReturn(wrappedImageView);
        when(playerTrackArtworkView.getImageOverlay()).thenReturn(artworkOverlayImage);
        when(playerTrackArtworkView.findViewById(R.id.artwork_image_view)).thenReturn(wrappedImageView);
        when(playerTrackArtworkView.findViewById(R.id.artwork_overlay)).thenReturn(artworkIdleOverlay);
        when(playerTrackArtworkView.findViewById(R.id.artwork_overlay_image)).thenReturn(artworkOverlayImage);
        when(playerTrackArtworkView.findViewById(R.id.artwork_holder)).thenReturn(artworkHolder);
        when(animationControllerFactory.create(artworkHolder)).thenReturn(progressController);
        playerArtworkLoaderProvider = () -> playerArtworkLoader;

        playerArtworkController = new PlayerArtworkController.Factory(animationControllerFactory,
                                                                      playerArtworkLoaderProvider).create(
                playerTrackArtworkView);
    }

    @Test
    public void showPlayingStateDoesNotStartProgressAnimationWithoutDuration() {
        playerArtworkController.setPlayState(TestPlayStates.playing(), true);
        verify(progressController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void showPlayingStateStartsProgressAnimationWithProgressArgument() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.setPlayState(TestPlayStates.playing(), true);
        verify(progressController).startProgressAnimation(any(PlaybackProgress.class), eq(FULL_DURATION));
    }

    @Test
    public void showPlayingStateDoesNotStartProgressAnimationIfScrubbing() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        playerArtworkController.setPlayState(TestPlayStates.playing(), true);
        verify(progressController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void setDurationAfterShowPlayingStateStartsProgressAnimations() {
        playerArtworkController.setPlayState(TestPlayStates.playing(1, 2), true);
        playerArtworkController.setFullDuration(FULL_DURATION);
        verify(progressController).startProgressAnimation(any(PlaybackProgress.class), eq(FULL_DURATION));
    }

    @Test
    public void scrubStateCancelledStartsProgressAnimationFromLastPositionIfPlaying() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.setPlayState(TestPlayStates.playing(), true);
        PlaybackProgress latest = new PlaybackProgress(5, 10, TestPlayStates.URN);

        playerArtworkController.setProgress(latest);
        playerArtworkController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(progressController).startProgressAnimation(latest, FULL_DURATION);
    }

    @Test
    public void scrubStateCancelledDoesNotStartAnimationsIfNotPlaying() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.scrubStateChanged(SCRUB_STATE_CANCELLED);
        verify(progressController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void showIdleStateCancelsProgressAnimation() {
        playerArtworkController.setPlayState(TestPlayStates.idle(), true);
        verify(progressController).cancelProgressAnimation();
    }

    @Test
    public void showIdleStateWithProgressDoesNotSetProgressIfDurationNotSet() {
        playerArtworkController.setPlayState(TestPlayStates.idle(), true);

        verify(progressController, never()).setPlaybackProgress(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void showIdleStateWithProgressSetsProgressIfDurationSet() {
        playerArtworkController.setFullDuration(FULL_DURATION);

        final PlaybackProgress playbackProgress = new PlaybackProgress(1, 2, TestPlayStates.URN);
        playerArtworkController.setProgress(playbackProgress);

        verify(progressController).setPlaybackProgress(playbackProgress, FULL_DURATION);
    }

    @Test
    public void showIdleStateWithEmptyProgressDoesNotSetProgress() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.setPlayState(TestPlayStates.idle(1, 2), true);

        verify(progressController, never()).setPlaybackProgress(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void setProgressSetsProgressOnControllerIfDurationSet() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.setProgress(playbackProgress);
        verify(progressController).setPlaybackProgress(playbackProgress, FULL_DURATION);
    }

    @Test
    public void clearProgressSetsProgressOnControllerToEmptyIfDurationSet() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.setProgress(playbackProgress);
        playerArtworkController.clearProgress();
        verify(progressController).setPlaybackProgress(PlaybackProgress.empty(), FULL_DURATION);
    }

    @Test
    public void setProgressDoesNotSetProgressOnControllerWhileScrubbing() {
        playerArtworkController.setFullDuration(FULL_DURATION);
        playerArtworkController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        playerArtworkController.setProgress(playbackProgress);
        verify(progressController, never()).setPlaybackProgress(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void scrubbingStateCancelsProgressAnimations() {
        playerArtworkController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        verify(progressController).cancelProgressAnimation();
    }

    @Test
    public void clearClearsWrappedImageView() {
        playerArtworkController.clear();

        verify(wrappedImageView).setImageDrawable(null);
    }

    @Test
    public void clearClearsImageOverlay() {
        playerArtworkController.clear();
        verify(artworkOverlayImage).setImageDrawable(null);
    }

    @Test
    public void displayScrubPositionUsesHelperToSetImageViewPosition() {
        when(wrappedImageView.getMeasuredWidth()).thenReturn(20);
        when(playerTrackArtworkView.getWidth()).thenReturn(10);
        playerArtworkController.onArtworkSizeChanged();
        playerArtworkController.displayScrubPosition(.5f, .5f);
        assertThat(artworkHolder.getTranslationX()).isEqualTo(-5F);
    }

    @Test
    public void loadArtworkDisplaysArtworkThroughImageOperations() throws Exception {
        final Urn urn = Urn.forTrack(123L);
        when(wrappedImageView.getResources()).thenReturn(resources());
        when(wrappedImageView.getContext()).thenReturn(context());

        playerArtworkController.loadArtwork(imageResource, true, viewVisibilityProvider);
        verify(playerArtworkLoader).loadArtwork(same(imageResource),
                                                same(wrappedImageView),
                                                same(artworkOverlayImage),
                                                eq(true),
                                                same(viewVisibilityProvider));
    }
}
