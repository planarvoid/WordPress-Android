package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;
import static com.soundcloud.android.playback.ui.progress.ScrubController.ScrubControllerFactory;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.ListenableHorizontalScrollView;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.android.waveform.WaveformResult;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class WaveformViewControllerTest {

    private  static final float WAVEFORM_WIDTH_RATIO = 2.0f;

    private WaveformViewController waveformViewController;

    @Mock
    private ScrubControllerFactory scrubControllerFactory;
    @Mock
    private ScrubController scrubController;
    @Mock
    private ProgressAnimationControllerFactory progressAnimationControllerFactory;
    @Mock
    private WaveformView waveformView;
    @Mock
    private ImageView leftWaveform;
    @Mock
    private ImageView rightWaveform;
    @Mock
    private ImageView leftLine;
    @Mock
    private ImageView rightLine;
    @Mock
    private ListenableHorizontalScrollView dragViewHolder;
    @Mock
    private ProgressController leftAnimationController;
    @Mock
    private ProgressController rightAnimationController;
    @Mock
    private ProgressController dragAnimationController;
    @Mock
    private PlaybackProgress playbackProgress;
    @Mock
    private WaveformResult waveformResult;
    @Mock
    private WaveformData waveformData;
    @Mock
    private Bitmap bitmap;
    @Mock
    private WaveformOperations waveformOperations;

    @Before
    public void setUp() throws Exception {
        // for nine-old-androids
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK", String.valueOf(Build.VERSION_CODES.HONEYCOMB));

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

        waveformViewController = new WaveformViewControllerFactory(scrubControllerFactory, progressAnimationControllerFactory,
                new Provider<Scheduler>() {
            @Override
            public Scheduler get() {
                return Schedulers.immediate();
            }
        }).create(waveformView);
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
    public void showPlayingStateDoesNotStartsProgressAnimationsIfScrubbing() {
        waveformViewController.scrubStateChanged(ScrubController.SCRUB_STATE_SCRUBBING);
        waveformViewController.showPlayingState(playbackProgress);
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
    public void showIdleStateCancelsProgressAnimations() {
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
    public void displayWaveformAfterSettingWidthSetsWaveformsOnWaveformView() {
        waveformViewController.onWaveformViewWidthChanged(500);
        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView).setWaveformBitmaps(bitmapPair);
    }

    @Test
    public void displayWaveformAfterSettingWidthShowsExpandedWaveformIfPlaySessionActive() {
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.onWaveformViewWidthChanged(500);
        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView, times(2)).showExpandedWaveform(); // once for playing, once after loading
    }

    @Test
    public void onWaveformWidthWithWaveformResultSetsWaveformOnWaveformView() {
        waveformViewController.displayWaveform(Observable.just(waveformResult));

        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformBitmaps(bitmapPair);
    }

    @Test
    public void setWaveformVisibilityWithTrueSetsVisibilityToVisible() {
        waveformViewController.setWaveformVisibility(true);
        verify(waveformView).setVisibility(View.VISIBLE);
    }

    @Test
    public void setWaveformVisibilityWithFalseSetsVisibilityToGone() {
        waveformViewController.setWaveformVisibility(false);
        verify(waveformView).setVisibility(View.GONE);
    }

    @Test
    public void addScrubListenerAddsScrubListenerToController() {
        final ScrubController.OnScrubListener listener = Mockito.mock(ScrubController.OnScrubListener.class);
        waveformViewController.addScrubListener(listener);
        verify(scrubController).addScrubListener(listener);
    }

    @Test
    public void displayScrubPositionSetsScrubPositionOnWaveformsIfPlaySessionActive() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.displayScrubPosition(.5f);
        verify(leftWaveform).setTranslationX(-250f);
        verify(rightWaveform).setTranslationX(-500f);
    }

    @Test
    public void displayScrubPositionSetsScrubPositionOnLinesIfPlaySessionActive() {
        waveformViewController.onWaveformViewWidthChanged(500);
        waveformViewController.showIdleState();
        waveformViewController.displayScrubPosition(.5f);
        verify(leftLine).setTranslationX(-250f);
        verify(rightLine).setTranslationX(-500f);
    }
}