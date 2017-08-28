package com.soundcloud.android.playback.flipper;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;

import javax.inject.Inject;

class PerformanceReporter {

    private final EventBusV2 eventBus;
    private final AccountOperations accountOperations;
    private final ConnectionHelper connectionHelper;

    @Inject
    public PerformanceReporter(EventBusV2 eventBus,
                               AccountOperations accountOperations,
                               ConnectionHelper connectionHelper) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
        this.connectionHelper = connectionHelper;
    }

    public void report(PlaybackItem playbackItem, AudioPerformanceEvent audioPerformanceEvent, PlayerType playerType) {
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

    private PlaybackPerformanceEvent createPlaybackPerformanceEvent(PlaybackItem playbackItem, AudioPerformanceEvent event, PlayerType playerType) {
        String eventType = event.getType();
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
                .metricValue(event.getLatency())
                .protocol(PlaybackProtocol.fromValue(event.getProtocol()))
                .playerType(playerType)
                .connectionType(connectionHelper.getCurrentConnectionType())
                .cdnHost(event.getCdnHost())
                .format(event.getFormat())
                .bitrate(event.getBitRate())
                .userUrn(accountOperations.getLoggedInUserUrn())
                .details(Optional.fromNullable(event.getDetailsJson()))
                .build();
    }

    private void reportPerformanceEvent(PlaybackPerformanceEvent event) {
        eventBus.publish(EventQueue.PLAYBACK_PERFORMANCE, event);
    }
}
