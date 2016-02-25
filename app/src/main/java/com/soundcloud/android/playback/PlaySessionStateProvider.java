package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player.StateTransition;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the current play session state. Can be queried for recent state, recent progress, and info about the current
 * item being played back.
 */

@Singleton
public class PlaySessionStateProvider {

    private final Map<Urn, PlaybackProgress> progressMap = new HashMap<>();
    private final Func1<StateTransition, Boolean> ignoreDefaultStateFilter = new Func1<StateTransition, Boolean>() {
        @Override
        public Boolean call(StateTransition stateTransition) {
            return !StateTransition.DEFAULT.equals(stateTransition);
        }
    };
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;

    private StateTransition lastStateTransition = StateTransition.DEFAULT;
    private Urn currentPlayingUrn; // the urn of the item that is currently loaded in the playback service

    @Inject
    public PlaySessionStateProvider(EventBus eventBus, PlayQueueManager playQueueManager) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber());
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM,  new PlayQueueItemSubscriber());
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(ignoreDefaultStateFilter)
                .subscribe(new PlayStateSubscriber());
    }

    public boolean isPlayingCurrentPlayQueueItem(){
        final PlayQueueItem playQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return !playQueueItem.isEmpty() && isPlayingItem(playQueueItem.getUrn());
    }

    private boolean isPlayingItem(Urn itemUrn) {
        return currentPlayingUrn != null && currentPlayingUrn.equals(itemUrn);
    }

    public boolean isPlaying() {
        return lastStateTransition.playSessionIsActive();
    }

    public boolean isInErrorState() {
        return lastStateTransition.wasError();
    }

    public PlaybackProgress getLastProgressEvent() {
        return getLastProgressForItem(currentPlayingUrn);
    }

    public PlaybackProgress getLastProgressEventForCurrentPlayQueueItem() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return getLastProgressForItem(currentPlayQueueItem.getUrn());
    }

    public PlaybackProgress getLastProgressForItem(Urn urn) {
        if (hasLastKnownProgress(urn)){
            return progressMap.get(urn);
        } else if (playQueueManager.wasLastSavedItem(urn)) {
            return new PlaybackProgress(playQueueManager.getLastSavedProgressPosition(), Consts.NOT_SET);
        } else {
            return PlaybackProgress.empty();
        }
    }

    public boolean hasLastKnownProgress(Urn itemUrn) {
        return progressMap.containsKey(itemUrn);
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            final boolean isItemChange = currentPlayingUrn != null &&
                    !stateTransition.isForUrn(currentPlayingUrn);

            if (isItemChange && stateTransition.playSessionIsActive()) {
                progressMap.clear();
            }

            lastStateTransition = stateTransition;
            currentPlayingUrn = stateTransition.getUrn();

            if (stateTransition.getProgress().isDurationValid()) {
                progressMap.put(currentPlayingUrn, stateTransition.getProgress());
            }

            if (playingNewItemFromBeginning(stateTransition, isItemChange) || playbackStoppedMidSession(stateTransition)) {
                final long lastValidProgress = getLastProgressForItem(currentPlayingUrn).getPosition();
                playQueueManager.saveCurrentProgress(stateTransition.playbackEnded() ? 0 : lastValidProgress);
            }
        }
    }

    private boolean playbackStoppedMidSession(StateTransition stateTransition) {
        return (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete());
    }

    private boolean playingNewItemFromBeginning(StateTransition stateTransition, boolean isItemChange) {
        return isItemChange && !playQueueManager.wasLastSavedItem(stateTransition.getUrn());
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            progressMap.put(progress.getUrn(), progress.getPlaybackProgress());
        }
    }

    private class PlayQueueItemSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            if (lastStateTransition.playSessionIsActive()) {
                progressMap.clear();
            }
        }
    }
}
