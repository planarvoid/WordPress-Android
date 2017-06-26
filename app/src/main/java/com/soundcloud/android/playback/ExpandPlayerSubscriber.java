package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

/**
 * Should be deleted after RxJava2 migration.
 *
 * @deprecated Use {@link ExpandPlayerObserver} instead.
 */
@Deprecated
public class ExpandPlayerSubscriber extends DefaultSubscriber<PlaybackResult> {

    private final EventBus eventBus;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public ExpandPlayerSubscriber(EventBus eventBus,
                                  PlaybackFeedbackHelper playbackFeedbackHelper,
                                  PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
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
