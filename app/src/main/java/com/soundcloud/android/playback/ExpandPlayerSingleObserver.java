package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.rx.eventbus.EventBusV2;

import javax.inject.Inject;

public class ExpandPlayerSingleObserver extends DefaultSingleObserver<PlaybackResult> {

    private final EventBusV2 eventBus;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public ExpandPlayerSingleObserver(EventBusV2 eventBus,
                                      PlaybackFeedbackHelper playbackFeedbackHelper,
                                      PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onSuccess(PlaybackResult result) {
        if (result.isSuccess()) {
            expandPlayer();
        } else {
            onPlaybackError();
            playbackFeedbackHelper.showFeedbackOnPlaybackError(result.getErrorReason());
        }
        super.onSuccess(result);
    }

    protected void onPlaybackError() {
        clearMeasuring();
    }

    protected void expandPlayer() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }

    private void clearMeasuring() {
        performanceMetricsEngine.clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER);
    }

}
