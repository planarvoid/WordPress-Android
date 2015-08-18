package com.soundcloud.android.tracks;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class TrackRepository {

    private final TrackStorage trackStorage;
    private final EventBus eventBus;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    // TODO: should this be fired from the syncer instead?
    private final Action1<PropertySet> publishTrackChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet propertySet) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(propertySet));
        }
    };

    @Inject
    public TrackRepository(TrackStorage trackStorage,
                           EventBus eventBus, SyncInitiator syncInitiator, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.trackStorage = trackStorage;
        this.eventBus = eventBus;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> track(final Urn trackUrn) {
        return trackFromStorage(trackUrn)
                .onErrorResumeNext(syncThenLoadTrack(trackUrn, trackFromStorage(trackUrn)))
                .flatMap(syncIfEmpty(trackUrn));
    }

    Observable<PropertySet> fullTrackWithUpdate(final Urn trackUrn) {
        return Observable.concat(
                fullTrackFromStorage(trackUrn),
                syncThenLoadTrack(trackUrn, fullTrackFromStorage(trackUrn)).doOnNext(publishTrackChanged)
        );
    }

    private Func1<PropertySet, Observable<PropertySet>> syncIfEmpty(final Urn trackUrn) {
        return new Func1<PropertySet, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(PropertySet track) {
                return track.isEmpty() ? syncThenLoadTrack(trackUrn, trackFromStorage(trackUrn))
                        : Observable.just(track);
            }
        };
    }

    private Observable<PropertySet> trackFromStorage(Urn trackUrn) {
        return trackStorage.loadTrack(trackUrn).subscribeOn(scheduler);
    }

    private Observable<PropertySet> fullTrackFromStorage(Urn trackUrn) {
        return trackFromStorage(trackUrn)
                .zipWith(trackStorage.loadTrackDescription(trackUrn), PropertySetFunctions.mergeLeft())
                .subscribeOn(scheduler);
    }

    private Observable<PropertySet> syncThenLoadTrack(final Urn trackUrn, final Observable<PropertySet> loadObservable) {
        return syncInitiator.syncTrack(trackUrn).flatMap(continueWith(loadObservable));
    }
}
