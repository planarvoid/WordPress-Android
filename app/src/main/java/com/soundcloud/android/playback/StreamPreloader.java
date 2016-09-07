package com.soundcloud.android.playback;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class StreamPreloader {

    @VisibleForTesting
    static final long MOBILE_TIME_TOLERANCE = TimeUnit.SECONDS.toMillis(30);
    static final long CACHE_CUSHION = 1024 * 1024; // one mb. not sure what this should be

    private final EventBus eventBus;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final PlaybackServiceInitiator serviceInitiator;
    private final StreamCacheConfig streamCacheConfig;

    private Subscription preloadSubscription = RxUtils.invalidSubscription();

    private final Action1<CurrentPlayQueueItemEvent> unsubscribeFromPreload = new Action1<CurrentPlayQueueItemEvent>() {
        @Override
        public void call(CurrentPlayQueueItemEvent currentPlayQueueItemEvent) {
            preloadSubscription.unsubscribe();
        }
    };

    private final Func1<CurrentPlayQueueItemEvent, Boolean> hasNextTrackInPlayQueue = new Func1<CurrentPlayQueueItemEvent, Boolean>() {
        @Override
        public Boolean call(CurrentPlayQueueItemEvent currentPlayQueueItemEvent) {
            return hasSpaceInCache() && playQueueManager.hasNextItem() && playQueueManager.getNextPlayQueueItem()
                                                                                          .isTrack();
        }
    };

    private final Func1<PropertySet, Boolean> isNotOfflineTrack = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet track) {
            return !offlinePlaybackOperations.shouldPlayOffline(track);
        }
    };

    private final Func3<PlayStateEvent, ConnectionType, PlaybackProgressEvent, PlaybackNetworkState> toPlaybackNetworkState = new Func3<PlayStateEvent, ConnectionType, PlaybackProgressEvent, PlaybackNetworkState>() {
        @Override
        public PlaybackNetworkState call(PlayStateEvent playStateEvent,
                                         ConnectionType connectionType,
                                         PlaybackProgressEvent playbackProgressEvent) {
            return new PlaybackNetworkState(playStateEvent,
                                            playbackProgressEvent.getPlaybackProgress(),
                                            connectionType);
        }
    };

    private final Func1<PlaybackNetworkState, Boolean> checkNetworkAndProgressConditions = new Func1<PlaybackNetworkState, Boolean>() {
        @Override
        public Boolean call(PlaybackNetworkState playbackNetworkState) {
            if (playbackNetworkState.playStateEvent.isPlayerPlaying()) {
                if (playbackNetworkState.connectionType == ConnectionType.WIFI) {
                    return true;
                } else {
                    final PlaybackProgress playbackProgress = playbackNetworkState.playbackProgress;
                    return playbackNetworkState.connectionType.isMobile() &&
                            playbackProgress.isDurationValid() &&
                            playbackProgress.getDuration() - playbackProgress.getPosition() < MOBILE_TIME_TOLERANCE;
                }
            } else {
                return false;
            }

        }
    };

    private final Func1<PropertySet, Observable<PreloadItem>> waitForValidPreloadConditions = new Func1<PropertySet, Observable<PreloadItem>>() {
        @Override
        public Observable<PreloadItem> call(final PropertySet nextItem) {
            return Observable.combineLatest(
                    eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                    eventBus.queue(EventQueue.NETWORK_CONNECTION_CHANGED),
                    eventBus.queue(EventQueue.PLAYBACK_PROGRESS),
                    toPlaybackNetworkState)
                             .filter(checkNetworkAndProgressConditions)
                             .take(1)
                             .filter(cacheSpaceAvailable)
                             .map(toPreloadItem(nextItem));
        }
    };

    @NonNull
    private Func1<Object, PreloadItem> toPreloadItem(final PropertySet propertyBindings) {
        return new Func1<Object, PreloadItem>() {
            @Override
            public PreloadItem call(Object ignored) {
                final PlaybackType playbackType = propertyBindings.get(TrackProperty.SNIPPED) ?
                                                  PlaybackType.AUDIO_SNIPPET :
                                                  PlaybackType.AUDIO_DEFAULT;
                return new AutoParcel_PreloadItem(propertyBindings.get(TrackProperty.URN), playbackType);
            }
        };
    }

    private final Func1<PlaybackNetworkState, Boolean> cacheSpaceAvailable = new Func1<PlaybackNetworkState, Boolean>() {
        @Override
        public Boolean call(PlaybackNetworkState playbackNetworkState) {
            return hasSpaceInCache();
        }
    };

    private boolean hasSpaceInCache() {
        return streamCacheConfig.getRemainingCacheSpace() > CACHE_CUSHION;
    }

    @Inject
    public StreamPreloader(EventBus eventBus,
                           TrackRepository trackRepository,
                           PlayQueueManager playQueueManager,
                           OfflinePlaybackOperations offlinePlaybackOperations,
                           PlaybackServiceInitiator serviceInitiator, StreamCacheConfig streamCacheConfig) {
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.playQueueManager = playQueueManager;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.serviceInitiator = serviceInitiator;
        this.streamCacheConfig = streamCacheConfig;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .doOnNext(unsubscribeFromPreload)
                .filter(hasNextTrackInPlayQueue)
                .subscribe(new PreloadCandidateSubscriber());
    }

    private class PreloadCandidateSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent args) {
            final Urn urn = playQueueManager.getNextPlayQueueItem().getUrn();
            preloadSubscription = trackRepository.track(urn)
                                                 .filter(isNotOfflineTrack)
                                                 .flatMap(waitForValidPreloadConditions)
                                                 .subscribe(new PreloadSubscriber());
        }
    }

    private class PreloadSubscriber extends DefaultSubscriber<PreloadItem> {
        @Override
        public void onNext(PreloadItem preloadItem) {
            serviceInitiator.preload(preloadItem);
        }
    }

    private static class PlaybackNetworkState {
        private final PlaybackProgress playbackProgress;
        private final PlayStateEvent playStateEvent;
        private final ConnectionType connectionType;

        private PlaybackNetworkState(PlayStateEvent playStateEvent,
                                     PlaybackProgress playbackProgress,
                                     ConnectionType connectionType) {
            this.playbackProgress = playbackProgress;
            this.playStateEvent = playStateEvent;
            this.connectionType = connectionType;
        }

        @Override
        public String toString() {
            return "PlaybackNetworkState{" +
                    "playbackProgress=" + playbackProgress +
                    ", playStateEvent=" + playStateEvent +
                    ", connectionType=" + connectionType +
                    '}';
        }
    }
}