package com.soundcloud.android.ads;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

public class AdsController {

    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;

    private Subscription audioAdSubscription = Subscriptions.empty();

    private Func1<PlayQueueEvent, Boolean> hasNextTrackFilter = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return playQueueManager.getNextTrackUrn() != TrackUrn.NOT_SET;
        }
    };

    private Func1<PropertySet, Boolean> isMonetizeableFilter = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet propertySet) {
            return propertySet.get(TrackProperty.MONETIZABLE);
        }
    };

    private Action1<PlayQueueEvent> unsubscriberFromPreviousAdOp = new Action1<PlayQueueEvent>() {
        @Override
        public void call(PlayQueueEvent playQueueEvent) {
            audioAdSubscription.unsubscribe();
        }
    };

    private Func1<PropertySet, Observable<AudioAd>> fetchAudioAdFunction = new Func1<PropertySet, Observable<AudioAd>>() {
        @Override
        public Observable<AudioAd> call(PropertySet propertySet) {
            return adsOperations.getAudioAd(propertySet.get(TrackProperty.URN));
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
        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(PlayQueueEvent.TRACK_HAS_CHANGED_FILTER)
                .doOnNext(unsubscriberFromPreviousAdOp)
                .filter(hasNextTrackFilter)
                .subscribe(new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            audioAdSubscription = trackOperations.track(playQueueManager.getNextTrackUrn())
                    .filter(isMonetizeableFilter)
                    .mergeMap(fetchAudioAdFunction)
                    .subscribe(new AudioAdSubscriber());

        }
    }

    private class AudioAdSubscriber extends DefaultSubscriber<AudioAd> {
        @Override
        public void onNext(AudioAd audioAd) {
            playQueueManager.insertAd(audioAd);
        }
    }

}
