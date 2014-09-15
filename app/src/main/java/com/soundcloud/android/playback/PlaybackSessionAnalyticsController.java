package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
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
    private final AdsOperations adsOperations;
    private PlaybackSessionEvent lastPlayEventData;
    private PropertySet lastPlayAudioAd;

    private TrackSourceInfo currentTrackSourceInfo;
    private Playa.StateTransition lastStateTransition = Playa.StateTransition.DEFAULT;
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public PlaybackSessionAnalyticsController(EventBus eventBus, TrackOperations trackOperations,
                                              AccountOperations accountOperations, PlayQueueManager playQueueManager,
                                              AdsOperations adsOperations) {
        this.eventBus = eventBus;
        this.trackOperations = trackOperations;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
    }

    public void onStateTransition(Playa.StateTransition stateTransition) {
        final TrackUrn currentTrack = stateTransition.getTrackUrn();
        if (!currentTrack.equals(lastStateTransition.getTrackUrn())) {
            if (lastStateTransition.isPlayerPlaying()) {
                // publish skip event manually, since it went from playing the last track to playing the new
                // track without seeing a stop event first (which only happens if you change tracks manually)
                publishStopEvent(lastStateTransition, PlaybackSessionEvent.STOP_REASON_SKIP);
            }

            trackObservable = ReplaySubject.createWithSize(1);
            trackOperations.track(currentTrack).subscribe(trackObservable);
        }

        if (stateTransition.isPlayerPlaying()) {
            publishPlayEvent(stateTransition);
        } else {
            publishStopEvent(stateTransition, stopReasonFromTransition(stateTransition));
        }
        lastStateTransition = stateTransition;
    }

    private int stopReasonFromTransition(Playa.StateTransition stateTransition) {
        if (stateTransition.isBuffering()) {
            return PlaybackSessionEvent.STOP_REASON_BUFFERING;
        } else {
            if (stateTransition.getReason() == Playa.Reason.TRACK_COMPLETE) {
                return playQueueManager.hasNextTrack()
                        ? PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED
                        : PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE;
            } else if (stateTransition.wasError()) {
                return PlaybackSessionEvent.STOP_REASON_ERROR;
            } else {
                return PlaybackSessionEvent.STOP_REASON_PAUSE;
            }
        }
    }

    private void publishPlayEvent(final Playa.StateTransition stateTransition) {
        currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        if (currentTrackSourceInfo != null) {
            trackObservable.map(stateTransitionToSessionPlayEvent(stateTransition)).subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionPlayEvent(final Playa.StateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                final UserUrn loggedInUserUrn = accountOperations.getLoggedInUserUrn();
                final long progress = stateTransition.getProgress().position;
                final String protocol = stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL);
                lastPlayEventData = PlaybackSessionEvent.forPlay(track, loggedInUserUrn, protocol, currentTrackSourceInfo, progress);
                if (adsOperations.isCurrentTrackAudioAd()) {
                    lastPlayAudioAd = playQueueManager.getCurrentMetaData();
                    lastPlayEventData = lastPlayEventData.withAudioAd(lastPlayAudioAd);
                } else {
                    lastPlayAudioAd = null;
                }
                return lastPlayEventData;
            }
        };
    }

    private void publishStopEvent(final Playa.StateTransition stateTransition, final int stopReason) {
        if (lastPlayEventData != null && currentTrackSourceInfo != null) {
            trackObservable.map(stateTransitionToSessionStopEvent(stopReason, stateTransition)).subscribe(eventBus.queue(EventQueue.PLAYBACK_SESSION));
            lastPlayEventData = null;
            lastPlayAudioAd = null;
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionStopEvent(final int stopReason, final Playa.StateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                final long progress = stateTransition.getProgress().position;
                final String protocol = stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL);
                PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(track, accountOperations.getLoggedInUserUrn(), protocol,
                        currentTrackSourceInfo, lastPlayEventData, stopReason, progress);

                if (lastPlayAudioAd != null) {
                    stopEvent = stopEvent.withAudioAd(lastPlayAudioAd);
                }
                return stopEvent;
            }
        };
    }
}
