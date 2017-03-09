package com.soundcloud.android.playback;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    private final PlayQueueManager playQueueManager;
    private final PlaybackServiceController serviceController;
    private final TrackItemRepository trackItemRepository;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final PlaySessionStateProvider playSessionStateProvider;

    private final EventBus eventBus;

    private final Func1<PlayerLifeCycleEvent, Observable<Void>> togglePlayback = new Func1<PlayerLifeCycleEvent, Observable<Void>>() {
        @Override
        public Observable<Void> call(PlayerLifeCycleEvent playerLifeCycleEvent) {
            if (playerLifeCycleEvent.isServiceRunning()) {
                serviceController.togglePlayback();
                return Observable.empty();
            } else {
                return playCurrent();
            }
        }
    };

    private final Func1<PlayerLifeCycleEvent, Observable<Void>> resume = new Func1<PlayerLifeCycleEvent, Observable<Void>>() {
        @Override
        public Observable<Void> call(PlayerLifeCycleEvent playerLifeCycleEvent) {
            if (playerLifeCycleEvent.isServiceRunning()) {
                serviceController.resume();
                return Observable.empty();
            } else {
                return playCurrent();
            }
        }
    };

    private final Func1<TrackItem, Observable<Void>> playPlayableTrack = new Func1<TrackItem, Observable<Void>>() {
        @Override
        public Observable<Void> call(TrackItem trackItem) {
            final Urn trackUrn = trackItem.getUrn();
            Track track = trackItem.track();
            if (track.blocked()) {
                return Observable.error(new BlockedTrackException(trackUrn));
            } else {
                if (offlinePlaybackOperations.shouldPlayOffline(trackItem)) {
                    serviceController.play(AudioPlaybackItem.forOffline(track, getPosition(trackUrn)));
                } else if (track.snipped()) {
                    serviceController.play(AudioPlaybackItem.forSnippet(track, getPosition(trackUrn)));
                } else {
                    serviceController.play(AudioPlaybackItem.create(track, getPosition(trackUrn)));
                }
                return Observable.empty();
            }
        }
    };

    public DefaultPlaybackStrategy(PlayQueueManager playQueueManager, PlaybackServiceController serviceController,
                                   TrackItemRepository trackItemRepository, OfflinePlaybackOperations offlinePlaybackOperations,
                                   PlaySessionStateProvider playSessionStateProvider, EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.serviceController = serviceController;
        this.trackItemRepository = trackItemRepository;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void togglePlayback() {
        eventBus.queue(EventQueue.PLAYER_LIFE_CYCLE).first()
                .flatMap(togglePlayback)
                .subscribe(new DefaultSubscriber<>());

    }

    @Override
    public void resume() {
        eventBus.queue(EventQueue.PLAYER_LIFE_CYCLE).first()
                .flatMap(resume)
                .subscribe(new DefaultSubscriber<>());
    }

    @Override
    public void pause() {
        serviceController.pause();
    }

    @Override
    public Observable<Void> playCurrent() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            return trackItemRepository.track(currentPlayQueueItem.getUrn()).flatMap(playPlayableTrack);
        } else if (currentPlayQueueItem.isAd()) {
            return playCurrentAd(currentPlayQueueItem);
        } else {
            return Observable.empty();
        }
    }

    private Observable<Void> playCurrentAd(PlayQueueItem currentPlayQueueItem) {
        final AdData adData = currentPlayQueueItem.getAdData().get();
        final long position = getPosition(adData.getAdUrn());
        final PlaybackItem playbackItem = currentPlayQueueItem.isVideoAd()
                                          ? VideoAdPlaybackItem.create((VideoAd) adData, position)
                                          : AudioAdPlaybackItem.create((AudioAd) adData);

        serviceController.play(playbackItem);
        return Observable.empty();
    }

    private long getPosition(Urn urn) {
        return playSessionStateProvider.getLastProgressForItem(urn).getPosition();
    }

    @Override
    public Observable<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                                  final Urn initialTrackUrn,
                                                  final int initialTrackPosition,
                                                  final PlaySessionSource playSessionSource) {
        return Observable.defer(() -> {
            int updatedPosition = PlaybackUtils.correctStartPositionAndDeduplicateList(playQueue,
                                                                                       initialTrackPosition,
                                                                                       initialTrackUrn,
                                                                                       playSessionSource);
            playQueueManager.setNewPlayQueue(playQueue, playSessionSource, updatedPosition);

            return Observable.just(PlaybackResult.success());
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void fadeAndPause() {
        serviceController.fadeAndPause();
    }

    @Override
    public void seek(long position) {
        serviceController.seek(position);
    }
}
