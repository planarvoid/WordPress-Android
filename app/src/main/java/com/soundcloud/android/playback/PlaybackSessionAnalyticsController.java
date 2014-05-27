package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
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
    private ReplaySubject<Integer> durationObservable;

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
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (lastStateTransition.playSessionIsActive()) {
                if (event.getKind() == PlayQueueEvent.NEW_QUEUE){
                    publishStopEvent(lastStateTransition.getTrackUrn(), currentTrackSourceInfo, PlaybackSessionEvent.STOP_REASON_NEW_QUEUE);
                } else {
                    publishStopEvent(lastStateTransition.getTrackUrn(), currentTrackSourceInfo, PlaybackSessionEvent.STOP_REASON_SKIP);
                }
            }
            createDurationObservable(playQueueManager.getCurrentTrackId());

        }
    }

    private class PlayStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        @Override
        public void onNext(Playa.StateTransition stateTransition) {
            if (stateTransition.getTrackUrn() != null){
                lastStateTransition = stateTransition;
                if (stateTransition.isPlayerPlaying()){
                    publishPlayEvent(stateTransition.getTrackUrn());
                } else {
                    publishStopEvent(stateTransition.getTrackUrn(), currentTrackSourceInfo, getStopEvent(stateTransition));
                }
            }
        }
    }

    private void createDurationObservable(long trackId) {
        durationObservable = ReplaySubject.create(1);
        trackOperations.loadTrack(trackId, AndroidSchedulers.mainThread()).map(new Func1<Track, Integer>() {
            @Override
            public Integer call(Track track) {
                return track.duration;
            }
        }).subscribe(durationObservable);
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


    private void publishPlayEvent(final TrackUrn trackUrn) {
        currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        if (playQueueManager.getCurrentTrackSourceInfo() != null) {
            final Observable<PlaybackSessionEvent> eventObservable = durationObservable.map(new Func1<Integer, PlaybackSessionEvent>() {
                @Override
                public PlaybackSessionEvent call(Integer duration) {
                    return lastPlayEventData = PlaybackSessionEvent.forPlay(trackUrn, accountOperations.getLoggedInUserUrn(),
                            currentTrackSourceInfo, duration);

                }
            });
            eventObservable.subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
        }
    }

    private void publishStopEvent(final TrackUrn trackUrn, final TrackSourceInfo trackSourceInfo, final int stopReason) {
        if (lastPlayEventData != null && trackSourceInfo != null) {
            durationObservable.map(new Func1<Integer, PlaybackSessionEvent>() {
                @Override
                public PlaybackSessionEvent call(Integer duration) {
                    return PlaybackSessionEvent.forStop(trackUrn, accountOperations.getLoggedInUserUrn(),
                            trackSourceInfo, lastPlayEventData, duration, stopReason);
                }
            }).subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
            lastPlayEventData = null;
        }
    }
}
