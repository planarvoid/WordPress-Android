package com.soundcloud.android.tracks;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.utils.DiffUtils.minus;
import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Iterators;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

public class TrackRepository {

    private static final Func1<Map<Urn, TrackItem>, PropertySet> TO_PROPERTY_MAP_VALUE_OR_EMPTY = track -> {
        if (track.isEmpty()) {
            return PropertySet.create();
        } else {
            return track.values().iterator().next().getSource();
        }
    };

    private static final Func1<Map<Urn, TrackItem>, TrackItem> TO_MAP_VALUE_OR_EMPTY = track -> {
        if (track.isEmpty()) {
            return null;
        } else {
            return track.values().iterator().next();
        }
    };

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

    @Deprecated // use trackItem
    public Observable<PropertySet> track(final Urn trackUrn) {
        return tracks(singletonList(trackUrn)).map(TO_PROPERTY_MAP_VALUE_OR_EMPTY);
    }

    public Observable<TrackItem> trackItem(final Urn trackUrn) {
        return tracks(singletonList(trackUrn)).map(TO_MAP_VALUE_OR_EMPTY);
    }

    public Observable<Map<Urn, TrackItem>> tracks(final List<Urn> requestedTracks) {
        checkTracksUrn(requestedTracks);

        return trackStorage
                .availableTracks(requestedTracks)
                .flatMap(syncMissingTracks(requestedTracks))
                .flatMap(continueWith(trackStorage.loadTracks(requestedTracks)))
                .subscribeOn(scheduler);
    }

    private Func1<List<Urn>, Observable<?>> syncMissingTracks(final List<Urn> requestedTracks) {
        return tracksAvailable -> {
            final List<Urn> missingTracks = minus(requestedTracks, tracksAvailable);
            if (missingTracks.isEmpty()) {
                return Observable.just(null);
            } else {
                return syncInitiator
                        .batchSyncTracks(missingTracks)
                        // The syncer is notifying back on the main thread, so this has to be.
                        .observeOn(scheduler);
            }
        };
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

    private void checkTracksUrn(List<Urn> trackUrns) {
        final boolean hasOnlyTracks = !Iterators.tryFind(trackUrns.iterator(), Urns.IS_NOT_TRACK).isPresent();
        checkArgument(hasOnlyTracks, "Trying to sync track without a valid track urn. trackUrns = [" + trackUrns + "]");
    }

    private Observable<PropertySet> trackFromStorage(Urn trackUrn) {
        return trackStorage.loadTrack(trackUrn).subscribeOn(scheduler);
    }

    private Observable<PropertySet> fullTrackFromStorage(Urn trackUrn) {
        return trackFromStorage(trackUrn)
                .zipWith(trackStorage.loadTrackDescription(trackUrn), PropertySetFunctions.mergeLeft())
                .subscribeOn(scheduler);
    }

    private Observable<PropertySet> syncThenLoadTrack(final Urn trackUrn,
                                                      final Observable<PropertySet> loadObservable) {
        return syncInitiator.syncTrack(trackUrn).flatMap(continueWith(loadObservable));
    }

}
