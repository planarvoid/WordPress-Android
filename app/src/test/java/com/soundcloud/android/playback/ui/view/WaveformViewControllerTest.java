package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import com.soundcloud.android.view.WaveformScrollView;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformOperations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

public class WaveformViewControllerTest extends AndroidUnitTest {

    private static final float WAVEFORM_WIDTH_RATIO = 2.0f;
    private static final long PLAYABLE_DURATION = 1000;
    private static final long FULL_DURATION = 2000;
    private final PlaybackProgress playbackProgress = TestPlaybackProgress.getPlaybackProgress(10, 100);

    private WaveformViewController waveformViewController;

    @Mock private ScrubController.Factory scrubControllerFactory;
    @Mock private ScrubController scrubController;
    @Mock private ProgressController.Factory progressAnimationControllerFactory;
    @Mock private WaveformView waveformView;
    @Mock private WaveformCanvas leftWaveform;
    @Mock private WaveformCanvas rightWaveform;
    @Mock private ImageView leftLine;
    @Mock private ImageView rightLine;
    @Mock private WaveformScrollView dragViewHolder;
    @Mock private ProgressController leftAnimationController;
    @Mock private ProgressController rightAnimationController;
    @Mock private ProgressController dragAnimationController;
    @Mock private WaveformData waveformData;
    @Mock private Bitmap bitmap;
    @Mock private WaveformOperations waveformOperations;
    @Mock private AdOverlayController adOverlayController;

    @Before
    public void setUp() throws Exception {
        when(waveformView.getLeftWaveform()).thenReturn(leftWaveform);
        when(waveformView.getRightWaveform()).thenReturn(rightWaveform);
        when(waveformView.getLeftLine()).thenReturn(leftLine);
        when(waveformView.getRightLine()).thenReturn(rightLine);
        when(waveformView.getDragViewHolder()).thenReturn(dragViewHolder);
        when(waveformView.getWidthRatio()).thenReturn(WAVEFORM_WIDTH_RATIO);

        when(progressAnimationControllerFactory.create(same(leftWaveform))).thenReturn(leftAnimationController);
        when(progressAnimationControllerFactory.create(same(rightWaveform))).thenReturn(rightAnimationController);
        when(progressAnimationControllerFactory.create(same(dragViewHolder))).thenReturn(dragAnimationController);
        when(scrubControllerFactory.create(dragViewHolder)).thenReturn(scrubController);

        when(adOverlayController.isNotVisible()).thenReturn(true);

        waveformViewController = new WaveformViewController.Factory(scrubControllerFactory,
                                                                    progressAnimationControllerFactory).create(waveformView);
    }

    @Test
    public void showPlayingStateDoesNotStartProgressAnimationsWithoutSettingDuration() {
        waveformViewController.showPlayingState(playbackProgress);
        verify(leftAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(rightAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(dragAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void showPlayingStateStartsProgressAnimationsAfterSettingDuration() {
        waveformViewController.setDurations(PLAYABLE_DURATION, FULL_DURATION);
        waveformViewController.showPlayingState(playbackProgress);
        verify(leftAnimationController).startProgressAnimation(playbackProgress, FULL_DURATION);
        verify(rightAnimationController).startProgressAnimation(playbackProgress, FULL_DURATION);
        verify(dragAnimationController).startProgressAnimation(playbackProgress, FULL_DURATION);
    }

    @Test
    public void showPlayingStateDoesNotStartProgressAnimationsIfScrubbing() {
        waveformViewController.setDurations(PLAYABLE_DURATION, FULL_DURATION);
        waveformViewController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        waveformViewController.showPlayingState(playbackProgress);
        verify(leftAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(rightAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(dragAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void scrubStateCancelledStartsProgressAnimationsFromLastPositionIfPlaying() {
        waveformViewController.setDurations(PLAYABLE_DURATION, FULL_DURATION);
        waveformViewController.showPlayingState(playbackProgress);
        PlaybackProgress latest = TestPlaybackProgress.getPlaybackProgress(5, 10);

        waveformViewController.setProgress(latest);
        waveformViewController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(leftAnimationController).startProgressAnimation(latest, FULL_DURATION);
        verify(rightAnimationController).startProgressAnimation(latest, FULL_DURATION);
        verify(dragAnimationController).startProgressAnimation(latest, FULL_DURATION);
    }

    @Test
    public void scrubStateCancelledDoesntStartProgressAnimationsFromLastPositionIfBuffering() {
        waveformViewController.setDurations(PLAYABLE_DURATION, FULL_DURATION);
        waveformViewController.showBufferingState();
        PlaybackProgress latest = TestPlaybackProgress.getPlaybackProgress(5, 10);

        waveformViewController.setProgress(latest);
        waveformViewController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(leftAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(rightAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(dragAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void scrubStateCancelledDoesNotStartAnimationsIfNotPlaying() {
        waveformViewController.setDurations(PLAYABLE_DURATION, FULL_DURATION);
        waveformViewController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(leftAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(rightAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
        verify(dragAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void showPlayingStateCallsShowExpandedOnWaveform() {
        waveformViewController.showPlayingState(playbackProgress);
        verify(waveformView).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void showBufferingStateCancelsProgressAnimations() {
        waveformViewController.showBufferingState();
        verify(leftAnimationController).cancelProgressAnimation();
        verify(rightAnimationController).cancelProgressAnimation();
        verify(dragAnimationController).cancelProgressAnimation();
    }

    @Test
    public void showBufferingStateCallsShowExpandedOnWaveform() {
        waveformViewController.showBufferingState();
        verify(waveformView).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void showIdleStateCancelsProgressAnimationsAfterShowingIdleLines() {
        waveformViewController.showIdleState();

        InOrder leftInOrder = Mockito.inOrder(waveformView, leftAnimationController);
        InOrder rightInOrder = Mockito.inOrder(waveformView, rightAnimationController);

        leftInOrder.verify(waveformView).showIdleLinesAtWaveformPositions();
        leftInOrder.verify(leftAnimationController).cancelProgressAnimation();

        rightInOrder.verify(waveformView).showIdleLinesAtWaveformPositions();
        rightInOrder.verify(rightAnimationController).cancelProgressAnimation();

        verify(dragAnimationController).cancelProgressAnimation();
    }

    @Test
    public void showIdleStateShowsWaveformsAtPositions() {
        waveformViewController.showIdleState();
        verify(leftAnimationController).cancelProgressAnimation();
        verify(rightAnimationController).cancelProgressAnimation();
        verify(dragAnimationController).cancelProgressAnimation();
    }

    @Test
    public void showIdleStateCallsShowCollapsedOnWaveform() {
        waveformViewController.showIdleState();
        verify(waveformView).showCollapsedWaveform();
    }

    @Test
    public void setProgressSetsProgressOnAnimationControllers() {
        waveformViewController.setDurations(1, 2);
        waveformViewController.setProgress(playbackProgress);
        verify(leftAnimationController).setPlaybackProgress(playbackProgress, 2);
        verify(rightAnimationController).setPlaybackProgress(playbackProgress, 2);
        verify(dragAnimationController).setPlaybackProgress(playbackProgress, 2);
    }

    @Test
    public void clearProgressResetsAnimationControllers() {
        waveformViewController.clearProgress();

        verify(leftAnimationController).reset();
        verify(rightAnimationController).reset();
        verify(dragAnimationController).reset();
    }

    @Test
    public void setDurationsSetsDurationOnScrubController() {
        waveformViewController.setDurations(123, 456);
        verify(scrubController).setFullDuration(456);
    }

    @Test
    public void setProgressDoesNotSetDurationOnScrubControllerIfProgressEmpty() {
        waveformViewController.setProgress(PlaybackProgress.empty());
        verify(scrubController, never()).setFullDuration(anyLong());
    }

    @Test
    public void setProgressCallsShowIdleLinesAtWaveformPositionWhenPlaystateNotActiveAndDurationSet() {
        waveformViewController.setDurations(PLAYABLE_DURATION, FULL_DURATION);
        waveformViewController.setProgress(playbackProgress);
        verify(waveformView).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void setProgressDoesNotCallShowIdleLinesAtWaveformPositionWhenPlaystateNotActiveAndDurationNotSet() {
        waveformViewController.setProgress(playbackProgress);
        verify(waveformView, never()).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void setProgressDoesNotSetProgressOnAnimationControllersIfScrubbing() {
        waveformViewController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        waveformViewController.setProgress(playbackProgress);
        verify(leftAnimationController, never()).setPlaybackProgress(any(PlaybackProgress.class), anyLong());
        verify(rightAnimationController, never()).setPlaybackProgress(any(PlaybackProgress.class), anyLong());
        verify(dragAnimationController, never()).setPlaybackProgress(any(PlaybackProgress.class), anyLong());
    }

    @Test
    public void onWaveformWidthChangedConfiguresWaveformsToWidthTimesRatio() {
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformWidths(1000, 1);
    }

    @Test
    public void onWaveformWidthSetsWaveformTranslationsToHalfWidthAndZero() {
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformTranslations(250, 0);
    }

    @Test
    public void onWaveformWidthSetsLeftAnimationBoundsToMiddleAndMiddleMinusWidth() {
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(leftAnimationController).setHelper(eq(new TranslateXHelper(250, -750)));
    }

    @Test
    public void onWaveformWidthSetsRightAnimationBoundsToZeroAndNegativeWidth() {
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(rightAnimationController).setHelper(eq(new TranslateXHelper(0, -1000)));
    }

    @Test
    public void displayWaveformWhenNotExpandedAfterSettingWidthDoesNotSetWaveformsOnWaveformView() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.setWaveform(Observable.just(waveformData), true);
        verify(waveformView, never()).setWaveformData(waveformData, 1000, 1.0f);
    }

    @Test
    public void displayWaveformWhenExpandedAndBackgroundAfterSettingWidthDoesNotSetWaveformsOnWaveformView() {
        waveformViewController.setExpanded();
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.setWaveform(Observable.just(waveformData), false);

        verify(waveformView, never()).setWaveformData(waveformData, 1000, 1.0f);
    }

    @Test
    public void displayWaveformWhenExpandedAndForegroundAfterSettingWidthSetsWaveformsOnWaveformView() {
        waveformViewController.setExpanded();
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.setWaveform(Observable.just(waveformData), true);

        verify(waveformView).setWaveformData(waveformData, 1000, 1.0f);
    }

    @Test
    public void displayWaveformWhenExpandedAfterSettingWidthShowsExpandedWaveformIfPlaySessionActive() {
        waveformViewController.setExpanded();
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.onForeground();
        waveformViewController.setWaveform(Observable.just(waveformData), true);

        verify(waveformView).showIdleLinesAtWaveformPositions(); // on state change
        verify(waveformView).showExpandedWaveform(); // on waveform loaded
    }

    @Test
    public void onWaveformWidthWhenExpandedWithWaveformResultSetsWaveformOnWaveformView() {
        waveformViewController.setExpanded();
        waveformViewController.onForeground();
        waveformViewController.setWaveform(Observable.just(waveformData), true);
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformData(waveformData, 1000, 1.0f);
    }

    @Test
    public void onWaveformWidthWhenNotExpandedWithWaveformResultDoesNotSetWaveform() {
        waveformViewController.onPlayerSlide(.99f);
        waveformViewController.setWaveform(Observable.just(waveformData), true);
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView, never()).setWaveformData(waveformData, 1000, 1.0f);
    }

    @Test
    public void setExpandedWhenNotAllowedToBeShownWithWaveformResultDoesNotMakeWaveformVisible() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.onForeground();
        waveformViewController.setWaveform(Observable.just(waveformData), true);
        waveformViewController.hide();
        when(adOverlayController.isNotVisible()).thenReturn(false);

        waveformViewController.setExpanded();

        verify(waveformView).setWaveformData(waveformData, 1000, 1.0f);
        verify(waveformView, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void setExpandedWhenAllowedToBeShownWithWaveformResultMakesWaveformVisible() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.onForeground();
        waveformViewController.setWaveform(Observable.just(waveformData), true);
        when(adOverlayController.isNotVisible()).thenReturn(false);

        waveformViewController.setExpanded();

        verify(waveformView).setWaveformData(waveformData, 1000, 1.0f);
        verify(waveformView).setVisibility(View.VISIBLE);
    }

    @Test
    public void onPlayerSlideWithGreaterThanZeroShowsWaveform() {
        waveformViewController.onPlayerSlide(Float.MIN_VALUE);
        verify(waveformView).setVisibility(View.VISIBLE);
    }

    @Test
    public void onPlayerSlideDoesNotSetWaveformVisibleWhenSetToHide() {
        waveformViewController.hide();

        waveformViewController.onPlayerSlide(Float.MIN_VALUE);

        verify(waveformView, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void onPlayerSlideWithZeroHidesWaveform() {
        waveformViewController.onPlayerSlide(0);
        verify(waveformView).setVisibility(View.GONE);
    }

    @Test
    public void setCollapsedSetsVisibilityToGone() {
        waveformViewController.setCollapsed();
        verify(waveformView).setVisibility(View.GONE);
    }

    @Test
    public void addScrubListenerAddsScrubListenerToController() {
        final ScrubController.OnScrubListener listener = mock(ScrubController.OnScrubListener.class);
        waveformViewController.addScrubListener(listener);
        verify(scrubController).addScrubListener(listener);
    }

    @Test
    public void onForegroundCausesWaveformCreation() {
        waveformViewController.setWaveform(Observable.just(waveformData), true);
        waveformViewController.onBackground(); // setWaveform() is assumed to be called in foreground
        waveformViewController.setExpanded();

        waveformViewController.onForeground();

        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformData(waveformData, 1000, 1.0f);
    }

    @Test
    public void showSetsWaveformVisibleIfExpanded() {
        waveformViewController.show();
        verify(waveformView).setVisibility(View.VISIBLE);
    }

    @Test
    public void showDoesNotSetWaveformVisibleIfCollapsed() {
        waveformViewController.setCollapsed();

        waveformViewController.show();

        verify(waveformView, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void hideSetsWaveformGone() {
        waveformViewController.hide();
        verify(waveformView).setVisibility(View.GONE);
    }

    @Test
    public void displayScrubPositionSetsScrubPositionOnWaveforms() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.displayScrubPosition(.5f, 0);
        verify(leftWaveform).setTranslationX(-250f);
        verify(rightWaveform).setTranslationX(-500f);
    }

    @Test
    public void displayScrubPositionSetsScrubPositionOnWaveformsIfPlaySessionNotActive() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showIdleState();
        waveformViewController.displayScrubPosition(.5f, 0);
        verify(leftWaveform).setTranslationX(-250f);
        verify(rightWaveform).setTranslationX(-500f);
    }

    @Test
    public void displayScrubPositionSetsScrubPositionOnLinesIfPlaySessionActive() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showIdleState();
        waveformViewController.displayScrubPosition(.5f, 0);
        verify(leftLine).setTranslationX(-250f);
        verify(rightLine).setTranslationX(-500f);
    }

    @Test
    public void resetRemovesReferenceToPreviousObservable() {
        PublishSubject<WaveformData> waveformDataObservable = PublishSubject.create();
        waveformViewController.setWaveform(waveformDataObservable, true);

        waveformViewController.reset();
        waveformViewController.onWaveformViewWidthChanged(500);

        assertThat(waveformDataObservable.hasObservers()).isFalse();
    }
}
