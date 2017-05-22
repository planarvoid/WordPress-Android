package com.soundcloud.android.playback.flipper;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.flippernative.api.audio_performance;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

class PerformanceReporter {

    private final EventBus eventBus;
    private final AccountOperations accountOperations;
    private final ConnectionHelper connectionHelper;

    @Inject
    public PerformanceReporter(EventBus eventBus,
                               AccountOperations accountOperations,
                               ConnectionHelper connectionHelper) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.connectionHelper = connectionHelper;
    }

    public void report(PlaybackItem playbackItem, audio_performance audioPerformanceEvent, PlayerType playerType) {
        if (allowPerformanceMeasureEvent(playbackItem)) {
            reportPerformanceEvent(createPlaybackPerformanceEvent(playbackItem, audioPerformanceEvent, playerType));
        }
    }

    private boolean allowPerformanceMeasureEvent(PlaybackItem playbackItem) {
        return !isAd(playbackItem);
    }

    private boolean isAd(PlaybackItem playbackItem) {
        return playbackItem != null && playbackItem.getPlaybackType() == PlaybackType.AUDIO_AD;
    }

    private PlaybackPerformanceEvent createPlaybackPerformanceEvent(PlaybackItem playbackItem, audio_performance event, PlayerType playerType) {
        String eventType = event.getType().const_get_value();
        PlaybackPerformanceEvent.Builder builder;

        switch (eventType) {
            case "play":
                builder = PlaybackPerformanceEvent.timeToPlay(playbackItem.getPlaybackType());
                break;
            case "seek":
                builder = PlaybackPerformanceEvent.timeToSeek();
                break;
            case "cacheUsage":
                builder = PlaybackPerformanceEvent.cacheUsagePercent();
                break;
            case "playlist":
                builder = PlaybackPerformanceEvent.timeToPlaylist();
                break;
            default:
                throw new IllegalArgumentException("Unexpected performance metric : " + eventType);
        }

        return builder
                .metricValue(event.getLatency().const_get_value())
                .protocol(PlaybackProtocol.fromValue(event.getProtocol().const_get_value()))
                .playerType(playerType)
                .connectionType(connectionHelper.getCurrentConnectionType())
                .cdnHost(event.getHost().const_get_value())
                .format(event.getFormat().const_get_value())
                .bitrate((int) event.getBitrate().const_get_value())
                .userUrn(accountOperations.getLoggedInUserUrn())
                .details(Optional.fromNullable(event.getDetails().get_value().toJson()))
                .build();
    }

    private void reportPerformanceEvent(PlaybackPerformanceEvent event) {
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }
}
