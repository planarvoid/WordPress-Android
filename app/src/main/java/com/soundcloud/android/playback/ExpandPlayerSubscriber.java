package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {

    private final EventBus eventBus;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final MiniplayerExperiment miniplayerExperiment;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus,
                                  PlaybackFeedbackHelper playbackFeedbackHelper,
                                  MiniplayerExperiment miniplayerExperiment,
                                  PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.miniplayerExperiment = miniplayerExperiment;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onNext(PlaybackResult result) {
        if (result.isSuccess()) {
            expandPlayer();
        } else {
            onPlaybackError();
            playbackFeedbackHelper.showFeedbackOnPlaybackError(result.getErrorReason());
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
        performanceMetricsEngine.clearMeasuring(MetricType.TIME_TO_EXPAND_PLAYER);
    }

}
