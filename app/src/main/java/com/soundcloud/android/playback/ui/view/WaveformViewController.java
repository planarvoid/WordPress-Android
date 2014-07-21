package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;
import static com.soundcloud.android.playback.ui.progress.ScrubController.ScrubControllerFactory;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrollXHelper;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.waveform.WaveformResult;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.graphics.Bitmap;
import android.util.Pair;
import android.view.View;

public class WaveformViewController implements ScrubController.OnScrubListener, ProgressAware, WaveformView.OnWidthChangedListener {

    private final WaveformView waveformView;
    private final Scheduler graphicsScheduler;
    private final float waveformWidthRatio;
    private final ProgressController leftProgressController;
    private final ProgressController rightProgressController;
    private final ProgressController dragProgressController;

    private final ScrubController scrubController;

    private TranslateXHelper leftProgressHelper;
    private TranslateXHelper rightProgressHelper;

    private Observable<WaveformResult> waveformResultObservable;
    private Subscription waveformSubscription = Subscriptions.empty();

    private int adjustedWidth;
    private boolean suppressProgress;
    private boolean playSessionIsActive;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();

    WaveformViewController(WaveformView waveform,
                           ProgressAnimationControllerFactory animationControllerFactory,
                           final ScrubControllerFactory scrubControllerFactory, Scheduler graphicsScheduler){
        this.waveformView = waveform;
        this.graphicsScheduler = graphicsScheduler;
        this.waveformWidthRatio = waveform.getWidthRatio();
        this.scrubController = scrubControllerFactory.create(waveformView.getDragViewHolder());

        waveformView.setOnWidthChangedListener(this);
        scrubController.addScrubListener(this);

        leftProgressController = animationControllerFactory.create(waveformView.getLeftWaveform());
        rightProgressController = animationControllerFactory.create(waveformView.getRightWaveform());
        dragProgressController = animationControllerFactory.create(waveformView.getDragViewHolder());

        waveformView.showLoading();
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
        if (suppressProgress) {
            cancelProgressAnimations();
        }
        if (newScrubState == SCRUB_STATE_CANCELLED && playSessionIsActive) {
            startProgressAnimations(latestProgress);
        }
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        leftProgressHelper.setValueFromProportion(playSessionIsActive ?
                waveformView.getLeftWaveform() : waveformView.getLeftLine(), scrubPosition);
        rightProgressHelper.setValueFromProportion(playSessionIsActive ?
                waveformView.getRightWaveform() : waveformView.getRightLine(), scrubPosition);
    }

    public void setProgress(PlaybackProgress progress) {
        latestProgress = progress;
        if (!progress.isEmpty()) {
            scrubController.setDuration(progress.getDuration());
        }
        if (!suppressProgress) {
            leftProgressController.setPlaybackProgress(progress);
            rightProgressController.setPlaybackProgress(progress);
            dragProgressController.setPlaybackProgress(progress);

            if (!playSessionIsActive){
                waveformView.showIdleLinesAtWaveformPositions();
            }
        }
    }

    @Override
    public void onWaveformViewWidthChanged(int newWidth) {
        adjustedWidth = (int) (waveformWidthRatio * newWidth);
        waveformView.setWaveformWidths(adjustedWidth);

        final int middle = newWidth / 2;
        waveformView.setWaveformTranslations(middle, 0);

        leftProgressHelper = new TranslateXHelper(middle, middle - adjustedWidth);
        leftProgressController.setHelper(leftProgressHelper);

        rightProgressHelper = new TranslateXHelper(0, -adjustedWidth);
        rightProgressController.setHelper(rightProgressHelper);

        final ScrollXHelper dragProgressHelper = new ScrollXHelper(0, adjustedWidth);
        dragProgressController.setHelper(dragProgressHelper);
        scrubController.setProgressHelper(dragProgressHelper);

        if (waveformResultObservable != null) {
            createWaveforms(waveformResultObservable);
        }
    }

    public void displayWaveform(Observable<WaveformResult> waveformResultObservable) {
        waveformView.showLoading();
        this.waveformResultObservable = waveformResultObservable;
        if (adjustedWidth > 0) {
            createWaveforms(waveformResultObservable);
        }
    }

    public void reset() {
        waveformSubscription.unsubscribe(); // Matthias, help test this
        waveformResultObservable = null;
        waveformView.showLoading();
    }

    public void showPlayingState(PlaybackProgress progress) {
        playSessionIsActive = true;
        waveformView.showExpandedWaveform();
        if (!suppressProgress) {
            startProgressAnimations(progress);
        }
    }

    public void showBufferingState() {
        playSessionIsActive = true;
        waveformView.showExpandedWaveform();
        cancelProgressAnimations();
    }

    public void showIdleState() {
        playSessionIsActive = false;
        // must happen in order to retain translation values for nine-old-androids on pre-honeycomb
        waveformView.showIdleLinesAtWaveformPositions();
        waveformView.showCollapsedWaveform();
        cancelProgressAnimations();
    }

    public void setWaveformVisibility(boolean visible) {
        waveformView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setDuration(long duration) {
        scrubController.setDuration(duration);
    }

    private void startProgressAnimations(PlaybackProgress progress) {
        leftProgressController.startProgressAnimation(progress);
        rightProgressController.startProgressAnimation(progress);
        dragProgressController.startProgressAnimation(progress);
    }

    private void cancelProgressAnimations() {
        leftProgressController.cancelProgressAnimation();
        rightProgressController.cancelProgressAnimation();
        dragProgressController.cancelProgressAnimation();
    }

    public void addScrubListener(ScrubController.OnScrubListener listener){
        scrubController.addScrubListener(listener);
    }

    private void createWaveforms(final Observable<WaveformResult> waveformResultObservable) {
        waveformSubscription.unsubscribe();
        waveformSubscription = waveformResultObservable
                .subscribeOn(graphicsScheduler)
                .map(createWaveformsFunc())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new WaveformSubscriber());
    }

    private Func1<WaveformResult, Pair<Bitmap, Bitmap>> createWaveformsFunc() {
        return new Func1<WaveformResult, Pair<Bitmap, Bitmap>>() {
            @Override
            public Pair<Bitmap, Bitmap> call(WaveformResult waveformResult) {
                return waveformView.createWaveforms(waveformResult.getWaveformData(), adjustedWidth);
            }
        };
    }

    private class WaveformSubscriber extends DefaultSubscriber<Pair<Bitmap, Bitmap>> {
        @Override
        public void onNext(Pair<Bitmap, Bitmap> bitmaps) {
            waveformView.setWaveformBitmaps(bitmaps);
            if (playSessionIsActive){
                waveformView.showExpandedWaveform();
            }
        }
    }

}
