package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.view.ListenableHorizontalScrollView;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.android.waveform.WaveformResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.schedulers.Schedulers;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class WaveformViewControllerTest {

    private static final float WAVEFORM_WIDTH_RATIO = 2.0f;
    private final PlaybackProgress playbackProgress = new PlaybackProgress(10, 100);

    private WaveformViewController waveformViewController;

    @Mock private ScrubController.Factory scrubControllerFactory;
    @Mock private ScrubController scrubController;
    @Mock private ProgressController.Factory progressAnimationControllerFactory;
    @Mock private WaveformView waveformView;
    @Mock private ImageView leftWaveform;
    @Mock private ImageView rightWaveform;
    @Mock private ImageView leftLine;
    @Mock private ImageView rightLine;
    @Mock private ListenableHorizontalScrollView dragViewHolder;
    @Mock private ProgressController leftAnimationController;
    @Mock private ProgressController rightAnimationController;
    @Mock private ProgressController dragAnimationController;
    @Mock private WaveformResult waveformResult;
    @Mock private WaveformData waveformData;
    @Mock private Bitmap bitmap;
    @Mock private WaveformOperations waveformOperations;

    @Before
    public void setUp() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB); // 9 old Androids

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

        when(waveformResult.getWaveformData()).thenReturn(waveformData);

        waveformViewController = new WaveformViewController.Factory(scrubControllerFactory, progressAnimationControllerFactory,
               Schedulers.immediate()).create(waveformView);
    }

    @Test
    public void constructorShowsLoadingDrawablesByDefault() {
        verify(waveformView).showLoading();
    }

    @Test
    public void showPlayingStateStartsProgressAnimations() {
        waveformViewController.showPlayingState(playbackProgress);
        verify(leftAnimationController).startProgressAnimation(playbackProgress);
        verify(rightAnimationController).startProgressAnimation(playbackProgress);
        verify(dragAnimationController).startProgressAnimation(playbackProgress);
    }

    @Test
    public void showPlayingStateDoesNotStartProgressAnimationsIfScrubbing() {
        waveformViewController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        waveformViewController.showPlayingState(playbackProgress);
        verify(leftAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class));
        verify(rightAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class));
        verify(dragAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class));
    }

    @Test
    public void scrubStateCancelledStartsProgressAnimationsFromLastPositionIfPlaying() {
        waveformViewController.showPlayingState(playbackProgress);
        PlaybackProgress latest = new PlaybackProgress(5, 10);

        waveformViewController.setProgress(latest);
        waveformViewController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(leftAnimationController).startProgressAnimation(latest);
        verify(rightAnimationController).startProgressAnimation(latest);
        verify(dragAnimationController).startProgressAnimation(latest);
    }

    @Test
    public void scrubStateCancelledDoesNotStartAnimationsIfNotPlaying() {
        waveformViewController.scrubStateChanged(SCRUB_STATE_CANCELLED);

        verify(leftAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class));
        verify(rightAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class));
        verify(dragAnimationController, never()).startProgressAnimation(any(PlaybackProgress.class));
    }

    @Test
    public void showPlayingStateCallsShowExpandedOnWaveform() {
        waveformViewController.showPlayingState(playbackProgress);
        verify(waveformView).showExpandedWaveform();
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
        verify(waveformView).showExpandedWaveform();
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
        waveformViewController.setProgress(playbackProgress);
        verify(leftAnimationController).setPlaybackProgress(playbackProgress);
        verify(rightAnimationController).setPlaybackProgress(playbackProgress);
        verify(dragAnimationController).setPlaybackProgress(playbackProgress);
    }

    @Test
    public void setProgressSetsDurationOnScrubController() {
        waveformViewController.setProgress(playbackProgress);
        verify(scrubController).setDuration(playbackProgress.getDuration());
    }

    @Test
    public void setProgressDoesNotSetDurationOnScrubControllerIfProgressEmpty() {
        waveformViewController.setProgress(PlaybackProgress.empty());
        verify(scrubController, never()).setDuration(anyLong());
    }

    @Test
    public void setProgressCallsShowIdleLinesAtWaveformPositionWhenPlaystateNotActive() {
        waveformViewController.setProgress(playbackProgress);
        verify(waveformView).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void setProgressDoesNotCallShowIdleLinesAtWaveformPositionWhenPlaystateActive() {
        waveformViewController.showBufferingState();
        waveformViewController.setProgress(playbackProgress);
        verify(waveformView, never()).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void setProgressDoesNotSetProgressOnAnimationControllersIfScrubbing() {
        waveformViewController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        waveformViewController.setProgress(playbackProgress);
        verify(leftAnimationController, never()).setPlaybackProgress(any(PlaybackProgress.class));
        verify(rightAnimationController, never()).setPlaybackProgress(any(PlaybackProgress.class));
        verify(dragAnimationController, never()).setPlaybackProgress(any(PlaybackProgress.class));
    }

    @Test
    public void onWaveformWidthChangedConfiguresWaveformsToWidthTimesRatio() {
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformWidths(1000);
    }

    @Test
    public void onWaveformWidthSetsWaveformTranslationsToHalfWidthAndZero(){
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformTranslations(250, 0);
    }

    @Test
    public void onWaveformWidthSetsLeftAnimationBoundsToMiddleAndMiddleMinusWidth(){
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(leftAnimationController).setHelper(eq(new TranslateXHelper(250, -750)));

    }

    @Test
    public void onWaveformWidthSetsRightAnimationBoundsToZeroAndNegativeWidth(){
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(rightAnimationController).setHelper(eq(new TranslateXHelper(0, -1000)));
    }

    @Test
    public void displayWaveformShowsLoading() {
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView, times(2)).showLoading(); // once from constructor
    }

    @Test
    public void displayWaveformWhenNotExpandedAfterSettingWidthDoesNotSetWaveformsOnWaveformView() {
        waveformViewController.onWaveformViewWidthChanged(500);
        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(any(WaveformData.class), anyInt())).thenReturn(bitmapPair);
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView, never()).setWaveformBitmaps(any(Pair.class));
    }

    @Test
    public void displayWaveformWhenExpandedAfterSettingWidthSetsWaveformsOnWaveformView() {
        waveformViewController.setExpanded();
        waveformViewController.onWaveformViewWidthChanged(500);
        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView).setWaveformBitmaps(bitmapPair);
    }

    @Test
    public void displayWaveformWhenExpandedAfterSettingWidthShowsExpandedWaveformIfPlaySessionActive() {
        waveformViewController.setExpanded();
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.onWaveformViewWidthChanged(500);
        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView, times(2)).showExpandedWaveform(); // once for playing, once after loading
    }

    @Test
    public void resetRemovesReferenceToPreviousObservable() {
        TestObservables.MockObservable<WaveformResult> waveformResultObservable = TestObservables.emptyObservable();
        waveformViewController.displayWaveform(waveformResultObservable);

        waveformViewController.reset();
        waveformViewController.onWaveformViewWidthChanged(500);

        expect(waveformResultObservable.subscribedTo()).toBeFalse();
    }

    @Test
    public void resetCallsShowLoadingOnWaveformView() {
        waveformViewController.reset();

        verify(waveformView, times(2)).showLoading(); // once in the constructor
    }

    @Test
    public void onWaveformWidthWhenExpandedWithWaveformResultSetsWaveformOnWaveformView() {
        waveformViewController.setExpanded();
        waveformViewController.displayWaveform(Observable.just(waveformResult));

        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformBitmaps(bitmapPair);
    }

    @Test
    public void onWaveformWidthWhenNotExpandedWithWaveformResultDoesNotSetWaveform() {
        waveformViewController.onPlayerSlide(.99f);
        waveformViewController.displayWaveform(Observable.just(waveformResult));

        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(any(WaveformData.class), anyInt())).thenReturn(bitmapPair);
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView, never()).setWaveformBitmaps(any(Pair.class));
    }

    @Test
    public void onPlayerSlideWithGreaterThanZeroShowsWaveform() {
        waveformViewController.onPlayerSlide(Float.MIN_VALUE);
        verify(waveformView).setVisibility(View.VISIBLE);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void displayScrubPositionSetsScrubPositionOnWaveformsIfPlaySessionActive() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.displayScrubPosition(.5f);
        verify(leftWaveform).setTranslationX(-250f);
        verify(rightWaveform).setTranslationX(-500f);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void displayScrubPositionSetsScrubPositionOnLinesIfPlaySessionActive() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showIdleState();
        waveformViewController.displayScrubPosition(.5f);
        verify(leftLine).setTranslationX(-250f);
        verify(rightLine).setTranslationX(-500f);
    }
}