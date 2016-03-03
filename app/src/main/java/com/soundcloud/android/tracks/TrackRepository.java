package com.soundcloud.android.tracks;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class TrackRepository {

    private final TrackStorage trackStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    @Inject
    public TrackRepository(TrackStorage trackStorage,
                           SyncInitiator syncInitiator,
                           @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.trackStorage = trackStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> track(final Urn trackUrn) {
        checkTrackUrn(trackUrn);
        return trackFromStorage(trackUrn)
                .onErrorResumeNext(syncThenLoadTrack(trackUrn, trackFromStorage(trackUrn)))
                .flatMap(syncIfEmpty(trackUrn));
    }

    Observable<PropertySet> fullTrackWithUpdate(final Urn trackUrn) {
        checkTrackUrn(trackUrn);
        return Observable.concat(
                fullTrackFromStorage(trackUrn),
                syncThenLoadTrack(trackUrn, fullTrackFromStorage(trackUrn))
        );
    }

    private void checkTrackUrn(Urn trackUrn) {
        checkArgument(trackUrn.isTrack(), "Trying to sync track without a valid track urn");
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
