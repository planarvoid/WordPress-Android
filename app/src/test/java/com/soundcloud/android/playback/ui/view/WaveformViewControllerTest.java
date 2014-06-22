package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.android.waveform.WaveformResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import android.graphics.Bitmap;
import android.util.Pair;
import android.widget.ImageView;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class WaveformViewControllerTest {

    private  static final float WAVEFORM_WIDTH_RATIO = 2.0f;

    private WaveformViewController waveformViewController;

    @Mock
    private ProgressAnimationControllerFactory progressAnimationControllerFactory;
    @Mock
    private WaveformView waveformView;
    @Mock
    private ImageView leftWaveform;
    @Mock
    private ImageView rightWaveform;
    @Mock
    private ProgressController leftAnimationController;
    @Mock
    private ProgressController rightAnimationController;
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
        when(waveformView.getLeftWaveform()).thenReturn(leftWaveform);
        when(waveformView.getRightWaveform()).thenReturn(rightWaveform);
        when(progressAnimationControllerFactory.create(same(leftWaveform))).thenReturn(leftAnimationController);
        when(progressAnimationControllerFactory.create(same(rightWaveform))).thenReturn(rightAnimationController);
        when(waveformResult.getWaveformData()).thenReturn(waveformData);

        waveformViewController = new WaveformViewControllerFactory(progressAnimationControllerFactory, new Provider<Scheduler>() {
            @Override
            public Scheduler get() {
                return Schedulers.immediate();
            }
        }).create(waveformView, WAVEFORM_WIDTH_RATIO);
    }

    @Test
    public void constructorShowsLoadingDrawablesByDefault() {
        verify(waveformView).showLoading();
    }

    @Test
    public void showPlayingStateStartsProgressAnimations() {
        waveformViewController.showPlayingState(playbackProgress);
        verify(leftAnimationController).startProgressAnimation(playbackProgress);
        verify(leftAnimationController).startProgressAnimation(playbackProgress);
    }

    @Test
    public void showPlayingStateHidesIdleLines() {
        waveformViewController.showPlayingState(playbackProgress);
        verify(waveformView).hideIdleLines();
    }

    @Test
    public void showPlayingStateDoesNotScaleUpWaveformIfAlreadyPlaying() {
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.showPlayingState(playbackProgress);
        verify(waveformView).scaleUpWaveforms(); // only one time
    }

    @Test
    public void showPlayingStateScalesUpWaveformsIfIdle() {
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.showIdleState();
        waveformViewController.showPlayingState(playbackProgress);
        verify(waveformView, times(2)).scaleUpWaveforms();
    }

    @Test
    public void showIdleStateCancelsProgressAnimations() {
        waveformViewController.showIdleState();
        verify(leftAnimationController).cancelProgressAnimation();
        verify(rightAnimationController).cancelProgressAnimation();
    }

    @Test
    public void showIdleStateCallsShowIdleLinesOnWaveformView() {
        waveformViewController.showIdleState();
        verify(waveformView).showIdleLinesAtWaveformPositions();
    }

    @Test
    public void showIdleStateDoesNotCallScaleDownWaveformIfAlreadyIdle() {
        waveformViewController.showIdleState();
        verify(waveformView, never()).scaleDownWaveforms();
    }

    @Test
    public void showIdleStateCallsScaleDownWaveform() {
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.showIdleState();
        verify(waveformView).scaleDownWaveforms();
    }

    @Test
    public void setProgressSetsProgressOnAnimationControllers() {
        waveformViewController.setProgress(playbackProgress);
        verify(leftAnimationController).setPlaybackProgress(playbackProgress);
        verify(rightAnimationController).setPlaybackProgress(playbackProgress);
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
    public void displayWaveformAfterSettingWidthScalesUpWaveformViewIfPlaying() {
        waveformViewController.showPlayingState(playbackProgress);
        waveformViewController.onWaveformViewWidthChanged(500);
        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.displayWaveform(Observable.just(waveformResult));
        verify(waveformView, times(2)).scaleUpWaveforms(); // once for playing, once after loading
    }

    @Test
    public void onWaveformWidthWithWaveformResultSetsWaveformOnWaveformView() {
        waveformViewController.displayWaveform(Observable.just(waveformResult));

        final Pair<Bitmap, Bitmap> bitmapPair = new Pair<Bitmap, Bitmap>(bitmap, bitmap);
        when(waveformView.createWaveforms(waveformData, 1000)).thenReturn(bitmapPair);
        waveformViewController.onWaveformViewWidthChanged(500);
        verify(waveformView).setWaveformBitmaps(bitmapPair);
    }
}