package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.playback.ui.progress.ProgressController;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import javax.inject.Provider;

public class WaveformViewControllerFactory {

    private final ProgressController.ProgressAnimationControllerFactory animationControllerFactory;
    private final Provider<Scheduler> waveformScheduler;

    @Inject
    WaveformViewControllerFactory(ProgressController.ProgressAnimationControllerFactory animationControllerFactory) {
        this(animationControllerFactory, new Provider<Scheduler>(){
            @Override
            public Scheduler get() {
                return Schedulers.newThread();
            }
        });
    }
    WaveformViewControllerFactory(ProgressController.ProgressAnimationControllerFactory animationControllerFactory,
                                  Provider<Scheduler> waveformScheduler) {
        this.animationControllerFactory = animationControllerFactory;
        this.waveformScheduler = waveformScheduler;
    }
    WaveformViewController create(WaveformView waveformView, float waveformWidthRatio){
        return new WaveformViewController(waveformView, animationControllerFactory,
                waveformWidthRatio, waveformScheduler);
    }
}
