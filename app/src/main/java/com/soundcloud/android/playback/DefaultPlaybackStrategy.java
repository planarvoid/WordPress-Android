package com.soundcloud.android.playback;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class DefaultPlaybackStrategy implements PlaybackStrategy {

    // https://github.com/soundcloud/SoundCloud-Android/issues/4503
    private static final String TAG_BUG_4503 = "BUG_4503";
    private final PlayQueueManager playQueueManager;
    private final PlaybackServiceInitiator serviceInitiator;
    private final TrackRepository trackRepository;
    private final AdsOperations adsOperations;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EventBus eventBus;

    private final Func1<PlayerLifeCycleEvent, Observable<Void>> togglePlayback = new Func1<PlayerLifeCycleEvent, Observable<Void>>() {
        @Override
        public Observable<Void> call(PlayerLifeCycleEvent playerLifeCycleEvent) {
            if (playerLifeCycleEvent.isServiceRunning()) {
                serviceInitiator.togglePlayback();
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
                serviceInitiator.resume();
                return Observable.empty();
            } else {
                return playCurrent();
            }
        }
    };

    private final Func1<PropertySet, Observable<Void>> playPlayableTrack = new Func1<PropertySet, Observable<Void>>() {
        @Override
        public Observable<Void> call(PropertySet track) {
            final Urn trackUrn = track.get(TrackProperty.URN);
            if (track.getOrElse(TrackProperty.BLOCKED, false)) {
                return Observable.error(new BlockedTrackException(trackUrn));
            } else {
                if (adsOperations.isCurrentItemAudioAd()) {
                    final AudioAd audioAd = (AudioAd) adsOperations.getCurrentTrackAdData().get();
                    serviceInitiator.play(AudioAdPlaybackItem.create(track, audioAd));
                } else if (offlinePlaybackOperations.shouldPlayOffline(track)) {
                    serviceInitiator.play(AudioPlaybackItem.forOffline(track, getPosition(trackUrn)));
                } else if (track.get(TrackProperty.SNIPPED)) {
                    serviceInitiator.play(AudioPlaybackItem.forSnippet(track, getPosition(trackUrn)));
                } else {
                    serviceInitiator.play(AudioPlaybackItem.create(track, getPosition(trackUrn)));
                }
                return Observable.empty();
            }
        }
    };

    public DefaultPlaybackStrategy(PlayQueueManager playQueueManager, PlaybackServiceInitiator serviceInitiator,
                                   TrackRepository trackRepository, AdsOperations adsOperations,
                                   OfflinePlaybackOperations offlinePlaybackOperations,
                                   PlaySessionStateProvider playSessionStateProvider, EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.serviceInitiator = serviceInitiator;
        this.trackRepository = trackRepository;
        this.adsOperations = adsOperations;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void togglePlayback() {
        eventBus.queue(EventQueue.PLAYER_LIFE_CYCLE).first()
                .flatMap(togglePlayback)
                .subscribe(new DefaultSubscriber<Void>());

    }

    @Override
    public void resume() {
        eventBus.queue(EventQueue.PLAYER_LIFE_CYCLE).first()
                .flatMap(resume)
                .subscribe(new DefaultSubscriber<Void>());
    }

    @Override
    public void pause() {
        serviceInitiator.pause();
    }

    @Override
    public Observable<Void> playCurrent() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            return trackRepository.track(currentPlayQueueItem.getUrn()).flatMap(playPlayableTrack);
        } else if (currentPlayQueueItem.isVideo()) {
            final VideoAd videoAd = (VideoAd) currentPlayQueueItem.getAdData().get();
            serviceInitiator.play(VideoPlaybackItem.create(videoAd, getPosition(videoAd.getAdUrn())));
            return Observable.empty();
        } else {
            return Observable.empty();
        }
    }

    private long getPosition(Urn urn) {
        return playSessionStateProvider.getLastProgressForItem(urn).getPosition();
    }

    @Override
    public Observable<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                                  final Urn initialTrackUrn,
                                                  final int initialTrackPosition,
                                                  final PlaySessionSource playSessionSource) {
        return Observable
                .create(new Observable.OnSubscribe<PlaybackResult>() {
                    @Override
                    public void call(Subscriber<? super PlaybackResult> subscriber) {
                        final int updatedPosition = PlaybackUtils.correctStartPositionAndDeduplicateList(playQueue, initialTrackPosition,
                                initialTrackUrn, playSessionSource);
                         String message = "setNewQueue -> " +
                                "playQueue = [" + playQueue + "], " +
                                "initialTrackUrn = [" + initialTrackUrn + "], " +
                                "initialTrackPosition = [" + initialTrackPosition + "], " +
                                 "playSessionSource = [" + playSessionSource + "]" +
                                 "updatedPosition = [" + updatedPosition + "]" +
                                 "updated PlayQueueItem = [" + playQueue.getPlayQueueItem(updatedPosition) + "]";
                        if (initialTrackPosition < playQueue.size()) {
                            message += "initial PlayQueueItem = [" + playQueue.getPlayQueueItem(initialTrackPosition) + "]";
                        }

                        Log.d(TAG_BUG_4503,
                                message
                        );

                        playQueueManager.setNewPlayQueue(playQueue, playSessionSource, updatedPosition);
                        subscriber.onNext(PlaybackResult.success());
                        subscriber.onCompleted();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void fadeAndPause() {
        serviceInitiator.fadeAndPause();
    }

    @Override
    public void seek(long position) {
        serviceInitiator.seek(position);
    }
}
