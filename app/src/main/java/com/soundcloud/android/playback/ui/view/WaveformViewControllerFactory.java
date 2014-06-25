package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import javax.inject.Provider;

public class WaveformViewControllerFactory {
    private final ProgressController.ProgressAnimationControllerFactory animationControllerFactory;
    private final Provider<Scheduler> waveformScheduler;
    private final ScrubController.ScrubControllerFactory scrubControllerFactory;

    @Inject
    WaveformViewControllerFactory(ScrubController.ScrubControllerFactory scrubControllerFactory,
                                  ProgressController.ProgressAnimationControllerFactory animationControllerFactory) {
        this(scrubControllerFactory, animationControllerFactory, new Provider<Scheduler>(){
            @Override
            public Scheduler get() {
                return Schedulers.newThread();
            }
        });
    }
    WaveformViewControllerFactory(ScrubController.ScrubControllerFactory scrubControllerFactory,
                                  ProgressController.ProgressAnimationControllerFactory animationControllerFactory,
                                  Provider<Scheduler> waveformScheduler) {
        this.scrubControllerFactory = scrubControllerFactory;
        this.animationControllerFactory = animationControllerFactory;
        this.waveformScheduler = waveformScheduler;
    }
    public WaveformViewController create(WaveformView waveformView){
        return new WaveformViewController(waveformView, animationControllerFactory,
                waveformScheduler, scrubControllerFactory);
    }
}
