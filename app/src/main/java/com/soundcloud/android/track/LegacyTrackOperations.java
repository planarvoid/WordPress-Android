package com.soundcloud.android.track;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;

@Deprecated
public class LegacyTrackOperations {

    private static final String LOG_TAG = "TrackOperations";

    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final SyncStateManager syncStateManager;
    private final SyncInitiator syncInitiator;

    @Inject
    public LegacyTrackOperations(ScModelManager modelManager, TrackStorage trackStorage,
                                 SyncInitiator syncInitiator, SyncStateManager syncStateManager) {
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.syncInitiator = syncInitiator;
        this.syncStateManager = syncStateManager;
    }

    public Observable<Track> loadTrack(final long trackId, Scheduler observeOn) {
        final Track cachedTrack = modelManager.getCachedTrack(trackId);
        if (cachedTrack != null) {
            return Observable.just(cachedTrack);
        } else {
            return trackStorage.getTrackAsync(trackId).map(cacheTrack(trackId, ScResource.CacheUpdateMode.NONE))
                    .observeOn(observeOn);
        }
    }

    public Observable<Track> loadSyncedTrack(final long trackId, final Scheduler observeOn) {
        return loadTrack(trackId, observeOn).mergeMap(new Func1<Track, Observable<Track>>() {
            @Override
            public Observable<Track> call(Track track) {
                LocalCollection syncState = syncStateManager.fromContent(track.toUri());
                if (syncState.isSyncDue()) {
                    Log.d(LOG_TAG, "Syncing stale track " + track);
                    return Observable.concat(Observable.just(track), syncThenLoadTrack(track.getUrn(), observeOn));
                }
                Log.d(LOG_TAG, "Track up to date, emitting directly");
                return Observable.just(track);
            }
        }).onErrorResumeNext(handleTrackNotFound(trackId, observeOn));
    }

    public Observable<Track> loadStreamableTrack(final long trackId,  final Scheduler observeOn) {
        return loadTrack(trackId, observeOn).mergeMap(new Func1<Track, Observable<Track>>() {
            @Override
            public Observable<Track> call(Track track) {
                if (!track.isStreamable()) {
                    Log.d(LOG_TAG, "Syncing unstreamable track = " + track);
                    return syncThenLoadTrack(track.getUrn(), observeOn);
                }
                Log.d(LOG_TAG, "Track is streamable, emitting directly");
                return Observable.just(track);
            }
        }).onErrorResumeNext(handleTrackNotFound(trackId, observeOn));
    }

    /**
     * Performs a sync on the given track, then reloads it from local storage.
     */
    private Observable<Track> syncThenLoadTrack(final TrackUrn trackUrn, Scheduler observeOn) {
        Log.d(LOG_TAG, "Sending intent to sync track " + trackUrn);
        return syncInitiator.syncTrack(trackUrn).mergeMap(new Func1<Boolean, Observable<Track>>() {
            @Override
            public Observable<Track> call(Boolean trackWasUpdated) {
                Log.d(LOG_TAG, "Reloading track from local storage: " + trackUrn);
                return trackStorage.getTrackAsync(trackUrn.numericId);
            }
        }).observeOn(observeOn);
    }

    /**
     * If a track cannot be found in local storage, returns a sync sequence for resume purposes, otherwise
     * simply propagates the error.
     */
    private Func1<Throwable, Observable<? extends Track>> handleTrackNotFound(final long trackId, final Scheduler observeOn) {
        return new Func1<Throwable, Observable<? extends Track>>() {
            @Override
            public Observable<? extends Track> call(Throwable throwable) {
                if (throwable instanceof NotFoundException) {
                    Log.d(LOG_TAG, "Track missing from local storage, will sync " + trackId);
                    return syncThenLoadTrack(Urn.forTrack(trackId), observeOn);
                }
                Log.d(LOG_TAG, "Caught error, forwarding to observer: " + throwable);
                return Observable.error(throwable);
            }
        };
    }

    public Observable<Track> markTrackAsPlayed(Track track) {
        return trackStorage.createPlayImpressionAsync(track);
    }

    private Func1<Track, Track> cacheTrack(final long trackId, final ScResource.CacheUpdateMode updateMode){
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track nullableTrack) {
                return modelManager.cache(nullableTrack == null ? new Track(trackId) : nullableTrack, updateMode);
            }
        };
    }
}
