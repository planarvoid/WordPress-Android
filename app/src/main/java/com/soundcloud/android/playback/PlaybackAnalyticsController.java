package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.PlaybackProgressEvent;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class PlaybackAnalyticsController {

    private final TrackSessionAnalyticsDispatcher trackAnalyticsDispatcher;
    private final AdSessionAnalyticsDispatcher adAnalyticsDispatcher;
    private final PlayQueueManager playQueueManager;

    private PlaybackProgress lastProgressCheckpoint = PlaybackProgress.empty();
    private PlayStateEvent playStateEvent = PlayStateEvent.DEFAULT;
    @Nullable private PlaybackItem previousItem;

    @Inject
    public PlaybackAnalyticsController(TrackSessionAnalyticsDispatcher trackAnalyticsDispatcher,
                                       AdSessionAnalyticsDispatcher adAnalyticsController,
                                       PlayQueueManager playQueueManager) {
        this.trackAnalyticsDispatcher = trackAnalyticsDispatcher;
        this.adAnalyticsDispatcher = adAnalyticsController;
        this.playQueueManager = playQueueManager;
    }

    public void onStateTransition(PlaybackItem currentItem, PlayStateEvent playState) {
        final PlaybackAnalyticsDispatcher dispatcher = dispatcherForItem(currentItem);
        final boolean isNewItem = !playState.getPlayingItemUrn().equals(playStateEvent.getPlayingItemUrn());
        if (isNewItem) {
            onPlaybackItemChange();
        }

        if (wasLastItemSkippedWhilePlaying(isNewItem)) {
            dispatcherForItem(previousItem).onSkipTransition(playStateEvent);
        }

        if (playState.isPlayerPlaying()) {
            dispatcher.onPlayTransition(currentItem, playState, isNewItem);
        } else {
            dispatcher.onStopTransition(playState, isNewItem);
        }

        playStateEvent = playState;
        previousItem = currentItem;
    }

    public void onProgressEvent(PlaybackItem currentItem, PlaybackProgressEvent playbackProgress) {
        final PlaybackProgress currentProgress = playbackProgress.getPlaybackProgress();

        if (currentProgress.getPosition() >= lastProgressCheckpoint.getPosition() + checkpointIntervalForItem(
                currentItem)) {
            dispatcherForItem(currentItem).onProgressCheckpoint(playStateEvent, playbackProgress);
            lastProgressCheckpoint = currentProgress;
        }
        dispatcherForItem(currentItem).onProgressEvent(playbackProgress);
    }

    private void onPlaybackItemChange() {
        // Reset shared promoted source info so that individual plays send correct start events
        final PromotedSourceInfo promotedSource = playQueueManager.getCurrentPlaySessionSource()
                                                                  .getPromotedSourceInfo();
        if (promotedSource != null) {
            promotedSource.resetPlaybackStarted();
        }
        lastProgressCheckpoint = PlaybackProgress.empty();
    }

    private boolean wasLastItemSkippedWhilePlaying(boolean isNewItem) {
        return isNewItem && previousItem != null && playStateEvent.isPlayerPlaying();
    }

    private PlaybackAnalyticsDispatcher dispatcherForItem(PlaybackItem currentItem) {
        return isAd(currentItem) ? adAnalyticsDispatcher : trackAnalyticsDispatcher;
    }

    private long checkpointIntervalForItem(PlaybackItem currentItem) {
        return isAd(currentItem)
               ? AdSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL
               : TrackSessionAnalyticsDispatcher.CHECKPOINT_INTERVAL;
    }

    private boolean isAd(PlaybackItem item) {
        return item.getPlaybackType() == PlaybackType.AUDIO_AD
                || item.getPlaybackType() == PlaybackType.VIDEO_AD;
    }

}
