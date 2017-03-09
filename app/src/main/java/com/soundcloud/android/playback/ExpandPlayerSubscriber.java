package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {

    private final EventBus eventBus;
    private final PlaybackToastHelper playbackToastHelper;
    private final MiniplayerExperiment miniplayerExperiment;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus,
                                  PlaybackToastHelper playbackToastHelper,
                                  MiniplayerExperiment miniplayerExperiment,
                                  PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.playbackToastHelper = playbackToastHelper;
        this.miniplayerExperiment = miniplayerExperiment;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onNext(PlaybackResult result) {
        if (result.isSuccess()) {
            expandPlayer();
        } else {
            onPlaybackError();
            playbackToastHelper.showToastOnPlaybackError(result.getErrorReason());
        }
    }

    protected void onPlaybackError(){
        clearMeasuring();
    }

    protected void expandPlayer() {
        if (miniplayerExperiment.canExpandPlayer()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        } else {
            clearMeasuring();
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        }
    }

    private void clearMeasuring() {
        performanceMetricsEngine.clearMeasuring(MetricType.EXTENDED_TIME_TO_PLAY);
    }

}
