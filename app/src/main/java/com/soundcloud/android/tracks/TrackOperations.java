package com.soundcloud.android.tracks;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import java.util.List;

public class TrackOperations {

    @SuppressWarnings("UnusedDeclaration")
    private static final String LOG_TAG = "TrackOperations";

    private final TrackStorage trackStorage;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;
    private final SyncInitiator syncInitiator;

    private final Action1<PropertySet> publishPlayableChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet propertySet) {
            final PlayableUpdatedEvent event = PlayableUpdatedEvent.forUpdate(propertySet.get(TrackProperty.URN), propertySet);
            eventBus.publish(EventQueue.PLAYABLE_CHANGED, event);
        }
    };

    private final Func2<PropertySet, PropertySet, PropertySet> mergePropertySets = new Func2<PropertySet, PropertySet, PropertySet>() {
        @Override
        public PropertySet call(PropertySet propertySet, PropertySet propertySet2) {
            return propertySet.merge(propertySet2);
        }
    };

    @Inject
    public TrackOperations(TrackStorage trackStorage, AccountOperations accountOperations, EventBus eventBus, SyncInitiator syncInitiator) {
        this.trackStorage = trackStorage;
        this.accountOperations = accountOperations;
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
                        .doOnNext(publishPlayableChanged)
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
        return trackStorage.track(trackUrn, accountOperations.getLoggedInUserUrn());
    }

    private Observable<PropertySet> fullTrackFromStorage(Urn trackUrn) {
        return trackFromStorage(trackUrn).zipWith(trackStorage.trackDetails(trackUrn), mergePropertySets);
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
