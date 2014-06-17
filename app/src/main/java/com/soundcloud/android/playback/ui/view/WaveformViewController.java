package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ProgressController.ProgressAnimationControllerFactory;

import com.soundcloud.android.events.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.waveform.WaveformResult;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import android.graphics.Bitmap;
import android.util.Pair;

import javax.inject.Inject;
import javax.inject.Provider;

class WaveformViewController {

    private final WaveformView waveformView;
    private final float waveformWidthRatio;
    private final ProgressController leftProgressController;
    private final ProgressController rightProgressController;
    private final Provider<Scheduler> waveformScheduler;

    private TranslateXHelper leftProgressHelper;
    private TranslateXHelper rightProgressHelper;

    private Observable<WaveformResult> waveformResultObservable;
    private Subscription waveformSubscription = Subscriptions.empty();

    private boolean inPlayingState;
    private int adjustedWidth;

    private WaveformViewController(WaveformView waveform,
                                   ProgressAnimationControllerFactory animationControllerFactory,
                                   float waveformWidthRatio,
                                   Provider<Scheduler> waveformScheduler){
        this.waveformView = waveform;
        this.waveformWidthRatio = waveformWidthRatio;
        this.waveformScheduler = waveformScheduler;
        leftProgressController = animationControllerFactory.create(waveformView.getLeftWaveform());
        rightProgressController = animationControllerFactory.create(waveformView.getRightWaveform());
        waveformView.showLoading();
    }

    public void showPlayingState(PlaybackProgress progress) {
        leftProgressController.startProgressAnimation(progress);
        rightProgressController.startProgressAnimation(progress);
        waveformView.hideIdleLines();

        if (!inPlayingState){
            inPlayingState = true;
            waveformView.scaleUpWaveforms();
        }

    }

    public void showIdleState() {
        leftProgressController.cancelProgressAnimation();
        rightProgressController.cancelProgressAnimation();
        waveformView.showIdleLinesAtWaveformPositions();

        if (inPlayingState){
            inPlayingState = false;
            waveformView.scaleDownWaveforms();
        }
    }

    public void setProgress(PlaybackProgress progress) {
        leftProgressController.setPlaybackProgress(progress);
        rightProgressController.setPlaybackProgress(progress);
    }

    public void onWaveformViewWidthChanged(int w) {
        adjustedWidth = (int) (waveformWidthRatio * w);
        waveformView.setWaveformWidths(adjustedWidth);

        final int middle = w / 2;
        waveformView.setWaveformTranslations(middle, 0);

        leftProgressHelper = new TranslateXHelper(middle, middle - adjustedWidth);
        leftProgressController.setHelper(leftProgressHelper);

        rightProgressHelper = new TranslateXHelper(0, -adjustedWidth);
        rightProgressController.setHelper(rightProgressHelper);

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

    private void createWaveforms(Observable<WaveformResult> waveformResultObservable) {
        waveformSubscription.unsubscribe();
        waveformSubscription = waveformResultObservable
                .subscribeOn(waveformScheduler.get()) // TODO : look at the scheduling here
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
            if (inPlayingState){
                waveformView.scaleUpWaveforms();
            }
        }
    }

    static class WaveformViewControllerFactory {
        private final ProgressAnimationControllerFactory animationControllerFactory;
        private final Provider<Scheduler> waveformScheduler;

        @Inject
        WaveformViewControllerFactory(ProgressAnimationControllerFactory animationControllerFactory) {
            this(animationControllerFactory, new Provider<Scheduler>(){
                @Override
                public Scheduler get() {
                    return Schedulers.newThread();
                }
            });
        }
        WaveformViewControllerFactory(ProgressAnimationControllerFactory animationControllerFactory,
                                      Provider<Scheduler> waveformScheduler) {
            this.animationControllerFactory = animationControllerFactory;
            this.waveformScheduler = waveformScheduler;
        }
        WaveformViewController create(WaveformView waveformView, float waveformWidthRatio){
            return new WaveformViewController(waveformView, animationControllerFactory,
                    waveformWidthRatio, waveformScheduler);
        }
    }
}
