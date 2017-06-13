package com.soundcloud.android.playback;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class StreamPreloader {

    @VisibleForTesting
    static final long MOBILE_TIME_TOLERANCE = TimeUnit.SECONDS.toMillis(30);
    static final long CACHE_CUSHION = 1024 * 1024; // one mb. not sure what this should be

    private final EventBusV2 eventBus;
    private final TrackItemRepository trackItemRepository;
    private final PlayQueueManager playQueueManager;
    private final CastConnectionHelper castConnectionHelper;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final PlaybackServiceController serviceController;
    private final StreamCacheConfig.SkippyConfig skippyConfig;

    private Disposable preloadSubscription = Disposables.disposed();

    private final Consumer<CurrentPlayQueueItemEvent> unsubscribeFromPreload = currentPlayQueueItemEvent -> preloadSubscription.dispose();

    private final Predicate<CurrentPlayQueueItemEvent> hasNextTrackInPlayQueue = new Predicate<CurrentPlayQueueItemEvent>() {
        @Override
        public boolean test(CurrentPlayQueueItemEvent currentPlayQueueItemEvent) {
            return hasSpaceInCache() && playQueueManager.hasNextItem() && playQueueManager.getNextPlayQueueItem()
                                                                                          .isTrack();
        }
    };

    private final Predicate<PlaybackNetworkState> checkNetworkAndProgressConditions = playbackNetworkState -> {
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

    };

    private final Function<TrackItem, Maybe<PreloadItem>> waitForValidPreloadConditions = new Function<TrackItem, Maybe<PreloadItem>>() {
        @Override
        public Maybe<PreloadItem> apply(final TrackItem nextItem) {
            return Observable.combineLatest(
                    eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED),
                    eventBus.queue(EventQueue.NETWORK_CONNECTION_CHANGED),
                    eventBus.queue(EventQueue.PLAYBACK_PROGRESS),
                    (playStateEvent, connectionType, playbackProgressEvent) -> new PlaybackNetworkState(playStateEvent, playbackProgressEvent.getPlaybackProgress(), connectionType))
                             .filter(checkNetworkAndProgressConditions)
                             .firstElement()
                             .filter(playbackNetworkState -> hasSpaceInCache())
                             .map(toPreloadItem(nextItem));
        }
    };

    @NonNull
    private Function<Object, PreloadItem> toPreloadItem(final TrackItem trackItem) {
        return ignored -> {
            final PlaybackType playbackType = trackItem.track().snipped() ?
                                              PlaybackType.AUDIO_SNIPPET :
                                              PlaybackType.AUDIO_DEFAULT;
            return new AutoValue_PreloadItem(trackItem.getUrn(), playbackType);
        };
    }

    private boolean hasSpaceInCache() {
        return skippyConfig.getRemainingCacheSpace() > CACHE_CUSHION;
    }

    @Inject
    StreamPreloader(EventBusV2 eventBus,
                    TrackItemRepository trackItemRepository,
                    PlayQueueManager playQueueManager,
                    CastConnectionHelper castConnectionHelper,
                    OfflinePlaybackOperations offlinePlaybackOperations,
                    PlaybackServiceController serviceController, StreamCacheConfig.SkippyConfig skippyConfig) {
        this.eventBus = eventBus;
        this.trackItemRepository = trackItemRepository;
        this.playQueueManager = playQueueManager;
        this.castConnectionHelper = castConnectionHelper;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.serviceController = serviceController;
        this.skippyConfig = skippyConfig;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                .doOnNext(unsubscribeFromPreload)
                .filter(ignore -> !castConnectionHelper.isCasting())
                .filter(hasNextTrackInPlayQueue)
                .subscribe(new PreloadCandidateSubscriber());
    }

    private class PreloadCandidateSubscriber extends DefaultObserver<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent args) {
            final Urn urn = playQueueManager.getNextPlayQueueItem().getUrn();
            preloadSubscription = trackItemRepository.track(urn)
                                                     .filter(track -> !offlinePlaybackOperations.shouldPlayOffline(track))
                                                     .flatMap(waitForValidPreloadConditions)
                                                     .subscribeWith(new PreloadSubscriber());
        }
    }

    private class PreloadSubscriber extends DefaultMaybeObserver<PreloadItem> {
        @Override
        public void onSuccess(PreloadItem preloadItem) {
            serviceController.preload(preloadItem);
        }
    }

    private static class PlaybackNetworkState {
        private final PlaybackProgress playbackProgress;
        private final PlayStateEvent playStateEvent;
        private final ConnectionType connectionType;

        PlaybackNetworkState(PlayStateEvent playStateEvent,
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
