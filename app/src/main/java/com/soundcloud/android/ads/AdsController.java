package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdsController {

    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;

    private Subscription audioAdSubscription = Subscriptions.empty();

    private final Func1<PlayQueueEvent, Boolean> isQueueUpdateFilter = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return playQueueEvent.isQueueUpdate();
        }
    };

    private final Func1<Object, Boolean> hasNextNonAudioAdTrackFilter = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return playQueueManager.hasNextTrack()
                    && !playQueueManager.isNextTrackAudioAd()
                    && !playQueueManager.isCurrentTrackAudioAd();
        }
    };

    private final Func1<PropertySet, Boolean> isMonetizeableFilter = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet propertySet) {
            return propertySet.get(TrackProperty.MONETIZABLE);
        }
    };

    private final Func1<PropertySet, Observable<AudioAd>> fetchAudioAdFunction = new Func1<PropertySet, Observable<AudioAd>>() {
        @Override
        public Observable<AudioAd> call(PropertySet propertySet) {
            return adsOperations.audioAd(propertySet.get(TrackProperty.URN));
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> resetAudioAd = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent event) {
            playQueueManager.clearAudioAd();
            audioAdSubscription.unsubscribe();
        }
    };

    @Inject
    public AdsController(EventBus eventBus, AdsOperations adsOperations, PlayQueueManager playQueueManager, TrackOperations trackOperations) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .doOnNext(resetAudioAd)
                .filter(hasNextNonAudioAdTrackFilter)
                .subscribe(new PlayQueueSubscriber());

        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(isQueueUpdateFilter)
                .filter(hasNextNonAudioAdTrackFilter)
                .subscribe(new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            audioAdSubscription = trackOperations.track(playQueueManager.getNextTrackUrn())
                    .filter(isMonetizeableFilter)
                    .mergeMap(fetchAudioAdFunction)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new AudioAdSubscriber());

        }
    }

    private class AudioAdSubscriber extends DefaultSubscriber<AudioAd> {
        @Override
        public void onNext(AudioAd audioAd) {
            playQueueManager.insertAudioAd(audioAd);
        }
    }

}
