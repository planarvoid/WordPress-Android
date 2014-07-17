package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class WaveformViewControllerFactory {
    private final ProgressController.ProgressAnimationControllerFactory animationControllerFactory;
    private final Scheduler scheduler;
    private final ScrubController.ScrubControllerFactory scrubControllerFactory;

    @Inject
    WaveformViewControllerFactory(ScrubController.ScrubControllerFactory scrubControllerFactory,
                                  ProgressController.ProgressAnimationControllerFactory animationControllerFactory,
                                  @Named("GraphicsScheduler") Scheduler scheduler) {
        this.scrubControllerFactory = scrubControllerFactory;
        this.animationControllerFactory = animationControllerFactory;
        this.scheduler = scheduler;
    }
    public WaveformViewController create(WaveformView waveformView){
        return new WaveformViewController(waveformView, animationControllerFactory,scrubControllerFactory, scheduler);
    }
}
