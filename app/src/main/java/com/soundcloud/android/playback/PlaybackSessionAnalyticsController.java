package com.soundcloud.android.playback;

import static com.soundcloud.android.ApplicationModule.CURRENT_DATE_PROVIDER;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackSessionEventArgs;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.annotation.NonNull;
import android.webkit.URLUtil;

import javax.inject.Inject;
import javax.inject.Named;

class PlaybackSessionAnalyticsController {

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final AppboyPlaySessionState appboyPlaySessionState;
    private final StopReasonProvider stopReasonProvider;
    private final UuidProvider uuidProvider;
    private final DateProvider dateProvider;

    private PlaybackSessionEvent lastSessionEventData;
    private AdData lastPlayAd;

    private Optional<TrackSourceInfo> currentTrackSourceInfo = Optional.absent();
    private PlaybackStateTransition lastStateTransition = PlaybackStateTransition.DEFAULT;
    private ReplaySubject<PropertySet> trackObservable;

    @Inject
    public PlaybackSessionAnalyticsController(EventBus eventBus, TrackRepository trackRepository,
                                              AccountOperations accountOperations, PlayQueueManager playQueueManager,
                                              AdsOperations adsOperations, AppboyPlaySessionState appboyPlaySessionState,
                                              StopReasonProvider stopReasonProvider, UuidProvider uuidProvider,
                                              @Named(CURRENT_DATE_PROVIDER) DateProvider dateProvider) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.appboyPlaySessionState = appboyPlaySessionState;
        this.stopReasonProvider = stopReasonProvider;
        this.uuidProvider = uuidProvider;
        this.dateProvider = dateProvider;
    }

    public void onProgressEvent(PlaybackProgressEvent progressEvent) {
        if (adsOperations.isCurrentItemAd()) {
            final PlayerAdData adData = (PlayerAdData) adsOperations.getCurrentTrackAdData().get();
            final PlaybackProgress progress = progressEvent.getPlaybackProgress();
            final TrackSourceInfo trackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
            if (!adData.hasReportedFirstQuartile() && progress.isPastFirstQuartile()) {
                adData.setFirstQuartileReported();
                publishAdQuartileEvent(AdPlaybackSessionEvent.forFirstQuartile(adData, trackSourceInfo));
            } else if (!adData.hasReportedSecondQuartile() && progress.isPastSecondQuartile()) {
                adData.setSecondQuartileReported();
                publishAdQuartileEvent(AdPlaybackSessionEvent.forSecondQuartile(adData, trackSourceInfo));
            } else if (!adData.hasReportedThirdQuartile() && progress.isPastThirdQuartile()) {
                adData.setThirdQuartileReported();
                publishAdQuartileEvent(AdPlaybackSessionEvent.forThirdQuartile(adData, trackSourceInfo));
            }
        }
    }

    private void publishAdQuartileEvent(AdPlaybackSessionEvent adPlaybackSessionEvent) {
        eventBus.publish(EventQueue.TRACKING, adPlaybackSessionEvent);
    }

    public void onStateTransition(PlaybackStateTransition stateTransition) {
        final Urn currentItem = stateTransition.getUrn();
        if (!currentItem.equals(lastStateTransition.getUrn())) {
            if (lastStateTransition.isPlayerPlaying()) {
                // publish skip event manually, since it went from playing the last track to playing the new
                // track without seeing a stop event first (which only happens if you change tracks manually)
                publishStopEvent(lastStateTransition, PlaybackSessionEvent.STOP_REASON_SKIP);
            }

            if (!adsOperations.isCurrentItemVideoAd()) {
                trackObservable = ReplaySubject.createWithSize(1);
                trackRepository.track(currentItem).subscribe(trackObservable);
            }
        }

        if (stateTransition.isPlayerPlaying()) {
            publishPlayEvent(stateTransition);
        } else {
            publishStopEvent(stateTransition, stopReasonProvider.fromTransition(stateTransition));
        }
        lastStateTransition = stateTransition;
    }

    private void publishPlayEvent(final PlaybackStateTransition stateTransition) {
        currentTrackSourceInfo = Optional.fromNullable(playQueueManager.getCurrentTrackSourceInfo());
        if (currentTrackSourceInfo.isPresent() && lastEventWasNotPlayEvent()) {
            if (adsOperations.isCurrentItemVideoAd()) {
                lastPlayAd = adsOperations.getCurrentTrackAdData().get();
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forPlay((PlayerAdData) lastPlayAd, currentTrackSourceInfo.get(), stateTransition));
            } else {
                trackObservable
                        .map(stateTransitionToSessionPlayEvent(stateTransition))
                        .subscribe(eventBus.queue(EventQueue.TRACKING));
            }
        }
    }

    private boolean lastEventWasNotPlayEvent() {
        return (lastSessionEventData == null || !lastSessionEventData.isPlayEvent()) && lastPlayAd == null;
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionPlayEvent(final PlaybackStateTransition stateTransition) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                lastSessionEventData = PlaybackSessionEvent.forPlay(getEventArgs(stateTransition, track));
                if (adsOperations.isCurrentItemAudioAd()) {
                    lastPlayAd = playQueueManager.getCurrentPlayQueueItem().getAdData().get();
                    lastSessionEventData = lastSessionEventData.withAudioAd((AudioAd) lastPlayAd);
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

                    lastPlayAd = null;
                }

                return lastSessionEventData;
            }
        };
    }

    private void publishStopEvent(final PlaybackStateTransition stateTransition, final int stopReason) {
        // note that we only want to publish a stop event if we have a corresponding play event. This value
        // will be nulled out after it is used, and we will not publish another stop event until a play event
        // creates a new value for lastSessionEventData
        if ((lastSessionEventData != null || lastPlayAd != null) && currentTrackSourceInfo.isPresent()) {
            if (lastPlayAd instanceof VideoAd) {
                eventBus.publish(EventQueue.TRACKING, AdPlaybackSessionEvent.forStop((VideoAd) lastPlayAd, currentTrackSourceInfo.get(), stateTransition, stopReason));
            } else {
                trackObservable
                        .map(stateTransitionToSessionStopEvent(stopReason, stateTransition, lastSessionEventData))
                        .subscribe(eventBus.queue(EventQueue.TRACKING));
            }
            lastSessionEventData = null;
            lastPlayAd = null;
        }
    }

    private Func1<PropertySet, PlaybackSessionEvent> stateTransitionToSessionStopEvent(final int stopReason, final PlaybackStateTransition stateTransition, final PlaybackSessionEvent lastPlayEventData) {
        return new Func1<PropertySet, PlaybackSessionEvent>() {
            @Override
            public PlaybackSessionEvent call(PropertySet track) {
                final PlaybackSessionEventArgs args = getEventArgs(stateTransition, track);
                PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(lastPlayEventData, stopReason, args);
                if (lastPlayAd instanceof AudioAd) {
                    stopEvent = stopEvent.withAudioAd((AudioAd) lastPlayAd);
                }
                return stopEvent;
            }
        };
    }

    @NonNull
    private PlaybackSessionEventArgs getEventArgs(PlaybackStateTransition stateTransition, PropertySet track) {
        return PlaybackSessionEventArgs.create(track,
                accountOperations.getLoggedInUserUrn(),
                currentTrackSourceInfo.get(),
                stateTransition.getProgress().getPosition(),
                getProtocol(stateTransition),
                getPlayerType(stateTransition),
                getConnectionType(stateTransition),
                isLocalStoragePlayback(stateTransition),
                appboyPlaySessionState.isMarketablePlay(),
                uuidProvider.getRandomUuid(),
                dateProvider);
    }

    private String getPlayerType(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE);
    }

    private String getConnectionType(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE);
    }

    private String getProtocol(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL);
    }

    private boolean isLocalStoragePlayback(PlaybackStateTransition stateTransition) {
        return URLUtil.isFileUrl(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_URI));
    }
}
