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
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
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
                    publishStopEvent(lastStateTransition, PlaybackSessionEvent.STOP_REASON_NEW_QUEUE);
                } else {
                    publishStopEvent(lastStateTransition, PlaybackSessionEvent.STOP_REASON_SKIP);
                }
            }
            trackObservable = ReplaySubject.createWithSize(1);
            trackOperations.track(playQueueManager.getCurrentTrackUrn()).subscribe(trackObservable);
        }
    }

    private class PlayStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition stateTransition) {
            if (stateTransition.getTrackUrn() != null){
                lastStateTransition = stateTransition;
                if (stateTransition.isPlayerPlaying()){
                    publishPlayEvent(stateTransition);
                } else {
                    publishStopEvent(stateTransition, stopReasonFromTransition(stateTransition));
                }
            }
        }
    }

    private int stopReasonFromTransition(Playa.StateTransition stateTransition) {
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

    private void publishPlayEvent(final Playa.StateTransition stateTransition) {
        currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        if (currentTrackSourceInfo != null) {
            trackObservable.map(stateTransitionToSessionEvent(stateTransition)).subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionEvent(final Playa.StateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                final UserUrn loggedInUserUrn = accountOperations.getLoggedInUserUrn();
                final long progress = stateTransition.getProgress().position;
                final String protocol = stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL);
                if (playQueueManager.isCurrentTrackAudioAd()) {
                    lastPlayEventData = PlaybackSessionEvent.forAdPlay(playQueueManager.getAudioAd(), track,
                            loggedInUserUrn, protocol, currentTrackSourceInfo, progress);
                } else {
                    lastPlayEventData = PlaybackSessionEvent.forPlay(track, loggedInUserUrn, protocol, currentTrackSourceInfo, progress);
                }
                return lastPlayEventData;
            }
        };
    }

    private void publishStopEvent(final Playa.StateTransition stateTransition, final int stopReason) {
        if (lastPlayEventData != null && currentTrackSourceInfo != null) {
            final long progress = stateTransition.getProgress().position;
            final String protocol = stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL);
            trackObservable.map(new Func1<PropertySet, PlaybackSessionEvent>() {
                @Override
                public PlaybackSessionEvent call(PropertySet track) {
                    return PlaybackSessionEvent.forStop(track, accountOperations.getLoggedInUserUrn(), protocol,
                            currentTrackSourceInfo, lastPlayEventData, stopReason, progress);
                }
            }).subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
            lastPlayEventData = null;
        }
    }
}
