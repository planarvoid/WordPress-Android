package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class PlaybackAnalyticsController {

    private final TrackSessionAnalyticsDispatcher trackAnalyticsDispatcher;
    private final AdSessionAnalyticsDispatcher adAnalyticsDispatcher;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;

    private PlaybackProgress lastProgressCheckpoint = PlaybackProgress.empty();
    private PlayStateEvent playStateEvent = PlayStateEvent.DEFAULT;
    @Nullable private PlaybackItem previousItem;

    @Inject
    public PlaybackAnalyticsController(TrackSessionAnalyticsDispatcher trackAnalyticsDispatcher,
                                       AdSessionAnalyticsDispatcher adAnalyticsController,
                                       PlayQueueManager playQueueManager,
                                       AdsOperations adsOperations) {
        this.trackAnalyticsDispatcher = trackAnalyticsDispatcher;
        this.adAnalyticsDispatcher = adAnalyticsController;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
    }

    void onStateTransition(PlaybackItem currentItem, PlayStateEvent playState) {
        final PlaybackAnalyticsDispatcher dispatcher = dispatcherForItem(currentItem);
        final boolean isNewItem = !playState.getPlayingItemUrn().equals(playStateEvent.getPlayingItemUrn());
        if (isNewItem) {
            onPlaybackItemChange();
        }

        if (wasLastItemSkippedWhilePlaying(isNewItem)) {
            dispatcherForItem(previousItem).onSkipTransition(playStateEvent);
        }

        if (playState.isPlayerPlaying()) {
            if (isAd(currentItem)) {
                updateAdDispatcherMetaData(currentItem, adsOperations.getCurrentTrackAdData());
            }
            dispatcher.onPlayTransition(playState, isNewItem);
        } else {
            dispatcher.onStopTransition(playState, isNewItem);
        }

        playStateEvent = playState;
        previousItem = currentItem;
    }

    public void onProgressEvent(PlaybackItem currentItem, PlaybackProgressEvent playbackProgress) {
        final PlaybackProgress currentProgress = playbackProgress.getPlaybackProgress();

        final long earliestPositionForCheckpoint = lastProgressCheckpoint.getPosition() + checkpointIntervalForItem(currentItem);
        if (currentProgress.getPosition() >= earliestPositionForCheckpoint) {
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
        final PlaybackType playbackType = item.getPlaybackType();
        return playbackType == PlaybackType.AUDIO_AD || playbackType == PlaybackType.VIDEO_AD;
    }

    private void updateAdDispatcherMetaData(PlaybackItem currentItem, Optional<AdData> adData) {
        if (adData.isPresent() && adData.get().getAdUrn().equals(currentItem.getUrn())) {
            adAnalyticsDispatcher.setAdMetadata((PlayableAdData) adData.get(), playQueueManager.getCurrentTrackSourceInfo());
        } else {
            ErrorUtils.handleSilentException("PlaybackAnalyticsController: Could not set ad data for AdAnalyticsDispatcher", new IllegalStateException());
        }
    }
}
