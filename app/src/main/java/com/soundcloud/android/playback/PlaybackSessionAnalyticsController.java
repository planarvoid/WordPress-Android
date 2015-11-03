package com.soundcloud.android.playback;

import android.webkit.URLUtil;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

import rx.functions.Func1;
import rx.subjects.ReplaySubject;

class PlaybackSessionAnalyticsController {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private PlaybackSessionEvent lastSessionEventData;
    private PropertySet lastPlayAudioAd;

    private TrackSourceInfo currentTrackSourceInfo;
    private Player.StateTransition lastStateTransition = Player.StateTransition.DEFAULT;
    private ReplaySubject<PropertySet> trackObservable;

    private final Func1<PropertySet, Boolean> lastEventWasNotPlayEvent = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet track) {
            return lastSessionEventData == null || !lastSessionEventData.isPlayEvent();
        }
    };

    @Inject
    public PlaybackSessionAnalyticsController(EventBus eventBus, TrackRepository trackRepository,
                                              AccountOperations accountOperations, PlayQueueManager playQueueManager,
                                              AdsOperations adsOperations) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
    }

    public void onStateTransition(Player.StateTransition stateTransition) {
        final Urn currentTrack = stateTransition.getTrackUrn();
        if (!currentTrack.equals(lastStateTransition.getTrackUrn())) {
            if (lastStateTransition.isPlayerPlaying()) {
                // publish skip event manually, since it went from playing the last track to playing the new
                // track without seeing a stop event first (which only happens if you change tracks manually)
                publishStopEvent(lastStateTransition, PlaybackSessionEvent.STOP_REASON_SKIP);
            }

            trackObservable = ReplaySubject.createWithSize(1);
            trackRepository.track(currentTrack).subscribe(trackObservable);
        }

        if (stateTransition.isPlayerPlaying()) {
            publishPlayEvent(stateTransition);
        } else {
            publishStopEvent(stateTransition, stopReasonFromTransition(stateTransition));
        }
        lastStateTransition = stateTransition;
    }

    private int stopReasonFromTransition(Player.StateTransition stateTransition) {
        if (stateTransition.isBuffering()) {
            return PlaybackSessionEvent.STOP_REASON_BUFFERING;
        } else {
            if (stateTransition.getReason() == Player.Reason.TRACK_COMPLETE) {
                return playQueueManager.hasNextItem()
                        ? PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED
                        : PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE;
            } else if (stateTransition.wasError()) {
                return PlaybackSessionEvent.STOP_REASON_ERROR;
            } else {
                return PlaybackSessionEvent.STOP_REASON_PAUSE;
            }
        }
    }

    private void publishPlayEvent(final Player.StateTransition stateTransition) {
        currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        if (currentTrackSourceInfo != null) {
            trackObservable
                    .filter(lastEventWasNotPlayEvent)
                    .map(stateTransitionToSessionPlayEvent(stateTransition))
                    .subscribe(eventBus.queue(EventQueue.TRACKING));
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionPlayEvent(final Player.StateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                final Urn loggedInUserUrn = accountOperations.getLoggedInUserUrn();
                final long progress = stateTransition.getProgress().getPosition();
                final String protocol = getProtocol(stateTransition);
                final String playerType = getPlayerType(stateTransition);
                final String connectionType = getConnectionType(stateTransition);
                final boolean localStoragePlayback = isLocalStoragePlayback(stateTransition);

                lastSessionEventData = PlaybackSessionEvent.forPlay(track, loggedInUserUrn, currentTrackSourceInfo,
                        progress, protocol, playerType, connectionType, localStoragePlayback);

                if (adsOperations.isCurrentItemAudioAd()) {
                    lastPlayAudioAd = playQueueManager.getCurrentPlayQueueItem().getMetaData();
                    lastSessionEventData = lastSessionEventData.withAudioAd(lastPlayAudioAd);
                } else {
                    final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
                    PlaySessionSource playSource = playQueueManager.getCurrentPlaySessionSource();

                    if (currentPlayQueueItem.isTrack()
                            && playQueueManager.isTrackFromCurrentPromotedItem(currentPlayQueueItem.getUrn())
                            && playSource.getPromotedSourceInfo().isFirstPlay()) {
                        PromotedSourceInfo promotedSourceInfo = playSource.getPromotedSourceInfo();
                        lastSessionEventData = lastSessionEventData.withPromotedTrack(promotedSourceInfo);

                        if (!playSource.getCollectionUrn().isPlaylist()) {
                            // promoted tracks & ads are a one-time deal but we need to preserve promoted playlists
                            // since they may contain more than one track and we need to report plays as promoted
                            // for all tracks in these playlists
                            promotedSourceInfo.setAsPlayed();
                        }
                    }

                    lastPlayAudioAd = null;
                }

                return lastSessionEventData;
            }
        };
    }

    private void publishStopEvent(final Player.StateTransition stateTransition, final int stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if (lastSessionEventData != null && currentTrackSourceInfo != null) {
            trackObservable.map(stateTransitionToSessionStopEvent(stopReason, stateTransition, lastSessionEventData)).subscribe(eventBus.queue(EventQueue.TRACKING));
            lastSessionEventData = null;
            lastPlayAudioAd = null;
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionStopEvent(final int stopReason, final Player.StateTransition stateTransition, final PlaybackSessionEvent lastPlayEventData) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                final long progress = stateTransition.getProgress().getPosition();
                final String protocol = getProtocol(stateTransition);
                final String playerType = getPlayerType(stateTransition);
                final String connectionType = getConnectionType(stateTransition);
                final boolean localStoragePlayback = isLocalStoragePlayback(stateTransition);
                PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(track, accountOperations.getLoggedInUserUrn(),
                        currentTrackSourceInfo, lastPlayEventData, progress, protocol, playerType,
                        connectionType, stopReason, localStoragePlayback);

                if (lastPlayAudioAd != null) {
                    stopEvent = stopEvent.withAudioAd(lastPlayAudioAd);
                }
                return stopEvent;
            }
        };
    }

    private String getPlayerType(Player.StateTransition stateTransition) {
        return stateTransition.getExtraAttribute(Player.StateTransition.EXTRA_PLAYER_TYPE);
    }

    private String getConnectionType(Player.StateTransition stateTransition) {
        return stateTransition.getExtraAttribute(Player.StateTransition.EXTRA_CONNECTION_TYPE);
    }

    private String getProtocol(Player.StateTransition stateTransition) {
        return stateTransition.getExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL);
    }

    private boolean isLocalStoragePlayback(Player.StateTransition stateTransition) {
        return URLUtil.isFileUrl(stateTransition.getExtraAttribute(Player.StateTransition.EXTRA_URI));
    }
}
