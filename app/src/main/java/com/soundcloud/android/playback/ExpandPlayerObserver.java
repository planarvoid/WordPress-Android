package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DefaultObserver;

import javax.inject.Inject;

public class ExpandPlayerObserver extends DefaultObserver<PlaybackResult> {

    private final EventBusV2 eventBus;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public ExpandPlayerObserver(EventBusV2 eventBus,
                                PlaybackFeedbackHelper playbackFeedbackHelper,
                                PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onNext(@NonNull PlaybackResult playbackResult) {
        if (playbackResult.isSuccess()) {
            expandPlayer();
        } else {
            onPlaybackError();
            playbackFeedbackHelper.showFeedbackOnPlaybackError(playbackResult.getErrorReason());
        }
    }

    @Override
    public void onError(@NonNull Throwable e) {}

    @Override
    public void onComplete() {}

    private void onPlaybackError() {
        clearMeasuring();
    }

    protected void expandPlayer() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }

    private void clearMeasuring() {
        performanceMetricsEngine.clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER);
    }

}
