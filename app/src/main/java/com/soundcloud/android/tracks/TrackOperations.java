package com.soundcloud.android.tracks;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.PropertySetFunctions;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class TrackOperations {

    private final LoadTrackCommand loadTrack;
    private final LoadTrackDescriptionCommand loadTrackDescription;
    private final EventBus eventBus;
    private final SyncInitiator syncInitiator;

    // TODO: should this be fired from the syncer instead?
    private final Action1<PropertySet> publishTrackChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet propertySet) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(propertySet));
        }
    };

    @Inject
    public TrackOperations(LoadTrackCommand loadTrack, LoadTrackDescriptionCommand loadTrackDescription, EventBus eventBus, SyncInitiator syncInitiator) {
        this.loadTrack = loadTrack;
        this.loadTrackDescription = loadTrackDescription;
        this.eventBus = eventBus;
        this.syncInitiator = syncInitiator;
    }

    public Observable<PropertySet> track(final Urn trackUrn) {
        return trackFromStorage(trackUrn).toList().flatMap(syncIfEmpty(trackUrn));
    }

    Observable<PropertySet> fullTrackWithUpdate(final Urn trackUrn) {
        return Observable.concat(
                fullTrackFromStorage(trackUrn),
                syncThenLoadTrack(trackUrn, fullTrackFromStorage(trackUrn))
                        .doOnNext(publishTrackChanged)
        );
    }

    private Func1<List<PropertySet>, Observable<PropertySet>> syncIfEmpty(final Urn trackUrn) {
        return new Func1<List<PropertySet>, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(List<PropertySet> propertySets) {
                return propertySets.isEmpty() ? syncThenLoadTrack(trackUrn, trackFromStorage(trackUrn))
                        : Observable.just(propertySets.get(0));
            }
        };
    }

    private Observable<PropertySet> trackFromStorage(Urn trackUrn) {
        return loadTrack.with(trackUrn).toObservable();
    }

    private Observable<PropertySet> fullTrackFromStorage(Urn trackUrn) {
        return trackFromStorage(trackUrn).zipWith(loadTrackDescription.with(trackUrn).toObservable(), PropertySetFunctions.mergeLeft());
    }

    private Observable<PropertySet> syncThenLoadTrack(final Urn trackUrn, final Observable<PropertySet> loadObservable) {
        return syncInitiator.syncTrack(trackUrn)
                .flatMap(new Func1<Boolean, Observable<PropertySet>>() {
                    @Override
                    public Observable<PropertySet> call(Boolean trackWasUpdated) {
                        return loadObservable;
                    }
                });
    }
}
