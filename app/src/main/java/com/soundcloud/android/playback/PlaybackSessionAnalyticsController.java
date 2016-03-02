package com.soundcloud.android.playback;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdPlaybackProgressEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.webkit.URLUtil;

import javax.inject.Inject;

class PlaybackSessionAnalyticsController {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final AppboyPlaySessionState appboyPlaySessionState;
    private final StopReasonProvider stopReasonProvider;
    private final UuidProvider uuidProvider;
    private PlaybackSessionEvent lastSessionEventData;
    private AudioAd lastPlayAudioAd;

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
                                              AdsOperations adsOperations,
                                              AppboyPlaySessionState appboyPlaySessionState,
                                              StopReasonProvider stopReasonProvider, UuidProvider uuidProvider) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.appboyPlaySessionState = appboyPlaySessionState;
        this.stopReasonProvider = stopReasonProvider;
        this.uuidProvider = uuidProvider;
    }

    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        if (adsOperations.isCurrentItemAd()) {
            final PlayerAdData adData = (PlayerAdData) adsOperations.getCurrentTrackAdData().get();
            final PlaybackProgress progress = progressEvent.getPlaybackProgress();
            final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
            if (!adData.hasReportedFirstQuartile() && progress.isPastFirstQuartile()) {
                adData.setFirstQuartileReported();
                publishAdQuartileEvent(AdPlaybackProgressEvent.forFirstQuartile(progressEvent.getUrn(), adData, trackSourceInfo));
            } else if (!adData.hasReportedSecondQuartile() && progress.isPastSecondQuartile()) {
                adData.setSecondQuartileReported();
                publishAdQuartileEvent(AdPlaybackProgressEvent.forSecondQuartile(progressEvent.getUrn(), adData, trackSourceInfo));
            } else if (!adData.hasReportedThirdQuartile() && progress.isPastThirdQuartile()) {
                adData.setThirdQuartileReported();
                publishAdQuartileEvent(AdPlaybackProgressEvent.forThirdQuartile(progressEvent.getUrn(), adData, trackSourceInfo));
            }
        }
    }

    private void publishAdQuartileEvent(AdPlaybackProgressEvent adPlaybackProgressEvent) {
        eventBus.publish(EventQueue.TRACKING, adPlaybackProgressEvent);
    }

    public void onStateTransition(Player.StateTransition stateTransition) {
        final Urn currentTrack = stateTransition.getUrn();
        if (!currentTrack.equals(lastStateTransition.getUrn())) {
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
            publishStopEvent(stateTransition, stopReasonProvider.fromTransition(stateTransition));
        }
        lastStateTransition = stateTransition;
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
                final boolean marketablePlay = appboyPlaySessionState.isMarketablePlay();

                lastSessionEventData = PlaybackSessionEvent.forPlay(track, loggedInUserUrn, currentTrackSourceInfo,
                        progress, protocol, playerType, connectionType, localStoragePlayback, marketablePlay, uuidProvider.getRandomUuid());

                if (adsOperations.isCurrentItemAudioAd()) {
                    lastPlayAudioAd = (AudioAd) playQueueManager.getCurrentPlayQueueItem().getAdData().get();
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
                        connectionType, stopReason, localStoragePlayback, uuidProvider.getRandomUuid());

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
