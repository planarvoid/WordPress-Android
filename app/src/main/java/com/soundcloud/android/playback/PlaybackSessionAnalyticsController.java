package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import javax.inject.Inject;

public class PlaybackSessionAnalyticsController {

    private final EventBus eventBus;
    private final TrackOperations trackOperations;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private PlaybackSessionEvent lastPlayEventData;

    private TrackSourceInfo currentTrackSourceInfo;
    private Playa.StateTransition lastStateTransition = Playa.StateTransition.DEFAULT;
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public PlaybackSessionAnalyticsController(EventBus eventBus, TrackOperations trackOperations,
                                              AccountOperations accountOperations, PlayQueueManager playQueueManager) {
        this.eventBus = eventBus;
        this.trackOperations = trackOperations;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).subscribe(new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            if (lastStateTransition.playSessionIsActive()) {
                if (event.wasNewQueue()){
                    publishStopEvent(lastStateTransition.getProgress(),
                            currentTrackSourceInfo, PlaybackSessionEvent.STOP_REASON_NEW_QUEUE);
                } else {
                    publishStopEvent(lastStateTransition.getProgress(),
                            currentTrackSourceInfo, PlaybackSessionEvent.STOP_REASON_SKIP);
                }
            }
            createTrackObservable(playQueueManager.getCurrentTrackUrn());
        }
    }

    private class PlayStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition stateTransition) {
            if (stateTransition.getTrackUrn() != null){
                lastStateTransition = stateTransition;
                if (stateTransition.isPlayerPlaying()){
                    publishPlayEvent(stateTransition.getProgress());
                } else {
                    publishStopEvent(stateTransition.getProgress(),
                            currentTrackSourceInfo, getStopEvent(stateTransition));
                }
            }
        }
    }

    private void createTrackObservable(TrackUrn trackUrn) {
        trackObservable = ReplaySubject.createWithSize(1);
        trackOperations.track(trackUrn).subscribe(trackObservable);
    }

    private int getStopEvent(Playa.StateTransition stateTransition) {
        if (stateTransition.isBuffering()){
            return PlaybackSessionEvent.STOP_REASON_BUFFERING;
        } else {
            if (stateTransition.getReason() == Playa.Reason.TRACK_COMPLETE){
                return playQueueManager.hasNextTrack()
                        ? PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED
                        : PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE;
            } else if (stateTransition.wasError()){
                return PlaybackSessionEvent.STOP_REASON_ERROR;
            } else {
                return PlaybackSessionEvent.STOP_REASON_PAUSE;
            }
        }
    }


    private void publishPlayEvent(final PlaybackProgress progress) {
        currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        if (playQueueManager.getCurrentTrackSourceInfo() != null) {
            final Observable<PlaybackSessionEvent> eventObservable = trackObservable.map(new Func1<PropertySet, PlaybackSessionEvent>() {
                @Override
                public PlaybackSessionEvent call(PropertySet track) {
                    return lastPlayEventData = PlaybackSessionEvent.forPlay(track, accountOperations.getLoggedInUserUrn(),
                            currentTrackSourceInfo, progress.position);

                }
            });
            eventObservable.subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
        }
    }

    private void publishStopEvent(final PlaybackProgress progress, final TrackSourceInfo trackSourceInfo, final int stopReason) {
        if (lastPlayEventData != null && trackSourceInfo != null) {
            trackObservable.map(new Func1<PropertySet, PlaybackSessionEvent>() {
                @Override
                public PlaybackSessionEvent call(PropertySet track) {
                    return PlaybackSessionEvent.forStop(track, accountOperations.getLoggedInUserUrn(),
                            trackSourceInfo, lastPlayEventData, stopReason, progress.position);
                }
            }).subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
            lastPlayEventData = null;
        }
    }
}
