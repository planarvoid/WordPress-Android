package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.PlaybackProgressEvent;

import javax.annotation.Nullable;
import javax.inject.Inject;

class PlaybackAnalyticsController {

    private final TrackSessionAnalyticsDispatcher trackAnalyticsDispatcher;
    private final AdSessionAnalyticsDispatcher adAnalyticsDispatcher;
    private final PlayQueueManager playQueueManager;

    private PlaybackStateTransition previousTransition = PlaybackStateTransition.DEFAULT;
    @Nullable private PlaybackItem previousItem;

    @Inject
    public PlaybackAnalyticsController(TrackSessionAnalyticsDispatcher trackAnalyticsDispatcher,
                                       AdSessionAnalyticsDispatcher adAnalyticsController, PlayQueueManager playQueueManager) {
        this.trackAnalyticsDispatcher = trackAnalyticsDispatcher;
        this.adAnalyticsDispatcher = adAnalyticsController;
        this.playQueueManager = playQueueManager;
    }

    public void onStateTransition(PlaybackItem currentItem, PlaybackStateTransition newTransition) {
        final PlaybackAnalyticsDispatcher dispatcher = dispatcherForItem(currentItem);
        final boolean isNewItem = !newTransition.getUrn().equals(previousTransition.getUrn());
        resetPromotedSourceIfNeeded(isNewItem);

        if (wasLastItemSkippedWhilePlaying(isNewItem)) {
            dispatcherForItem(previousItem).onSkipTransition(previousTransition);
        }

        if (newTransition.isPlayerPlaying()) {
            dispatcher.onPlayTransition(newTransition, isNewItem);
        } else {
            dispatcher.onStopTransition(newTransition, isNewItem);
        }

        previousTransition = newTransition;
        previousItem = currentItem;
    }

    private void resetPromotedSourceIfNeeded(boolean isNewItem) {
        // Reset shared promoted source info so that individual plays send correct start events
        if (isNewItem) {
            final PromotedSourceInfo promotedSource = playQueueManager.getCurrentPlaySessionSource().getPromotedSourceInfo();
            if (promotedSource != null) {
                promotedSource.resetPlaybackStarted();
            }
        }
    }

    public void onProgressEvent(PlaybackItem currentItem, PlaybackProgressEvent playbackProgress) {
        dispatcherForItem(currentItem).onProgressEvent(playbackProgress);
    }

    private boolean wasLastItemSkippedWhilePlaying(boolean isNewItem) {
        return isNewItem && previousItem != null && previousTransition.isPlayerPlaying();
    }

    private PlaybackAnalyticsDispatcher dispatcherForItem(PlaybackItem currentItem) {
        return isAd(currentItem) ? adAnalyticsDispatcher : trackAnalyticsDispatcher;
    }

    private boolean isAd(PlaybackItem item) {
        return item.getPlaybackType() == PlaybackType.AUDIO_AD
                || item.getPlaybackType() == PlaybackType.VIDEO_AD;
    }

}
