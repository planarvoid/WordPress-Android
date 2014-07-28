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

    private Observable<AudioAd> currentObservable;
    private Subscription audioAdSubscription = Subscriptions.empty();

    private final Func1<PlayQueueEvent, Boolean> isQueueUpdate = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return playQueueEvent.isQueueUpdate();
        }
    };

    private final Func1<Object, Boolean> hasNextNonAudioAdTrack = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return currentObservable == null
                    && playQueueManager.hasNextTrack()
                    && !playQueueManager.isNextTrackAudioAd()
                    && !playQueueManager.isCurrentTrackAudioAd();
        }
    };

    private final Func1<PropertySet, Boolean> isMonetizable = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet propertySet) {
            return propertySet.get(TrackProperty.MONETIZABLE);
        }
    };

    private final Func1<PropertySet, Observable<AudioAd>> fetchAudioAd = new Func1<PropertySet, Observable<AudioAd>>() {
        @Override
        public Observable<AudioAd> call(PropertySet propertySet) {
            return adsOperations.audioAd(propertySet.get(TrackProperty.URN));
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> resetAudioAd = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent event) {
            currentObservable = null;
            audioAdSubscription.unsubscribe();
            playQueueManager.clearAudioAd();
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
                .filter(hasNextNonAudioAdTrack)
                .subscribe(new PlayQueueSubscriber());

        eventBus.queue(EventQueue.PLAY_QUEUE)
                .filter(isQueueUpdate)
                .filter(hasNextNonAudioAdTrack)
                .subscribe(new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object event) {
            currentObservable = trackOperations.track(playQueueManager.getNextTrackUrn())
                    .filter(isMonetizable)
                    .mergeMap(fetchAudioAd)
                    .observeOn(AndroidSchedulers.mainThread());
            audioAdSubscription = currentObservable.subscribe(new AudioAdSubscriber(playQueueManager.getCurrentPosition()));
        }
    }

    private final class AudioAdSubscriber extends DefaultSubscriber<AudioAd> {
        private final int intendedPosition;

        private AudioAdSubscriber(int intendedPosition) {
            this.intendedPosition = intendedPosition;
        }

        @Override
        public void onNext(AudioAd audioAd) {
            /*
             * We're checking if we're still at the intended position before we try to insert the ad in the play queue.
             * This is a temporary work-around for a race condition where unsubscribe doesn't happen immediately and
             * we attempt to put an ad in the queue twice. Matthias, please help!
             */
            if (playQueueManager.getCurrentPosition() == intendedPosition) {
                playQueueManager.insertAudioAd(audioAd);
            }
        }
    }

}
