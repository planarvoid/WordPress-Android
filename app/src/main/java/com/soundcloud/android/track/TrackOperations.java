package com.soundcloud.android.track;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

import android.os.ResultReceiver;

import javax.inject.Inject;

public class TrackOperations {

    private static final String LOG_TAG = "TrackOperations";

    private final ScModelManager mModelManager;
    private final TrackStorage mTrackStorage;
    private final SyncStateManager mSyncStateManager;
    private final SyncInitiator mSyncInitiator;

    @Inject
    public TrackOperations(ScModelManager modelManager, TrackStorage trackStorage,
                           SyncInitiator syncInitiator, SyncStateManager syncStateManager) {
        mModelManager = modelManager;
        mTrackStorage = trackStorage;
        mSyncInitiator = syncInitiator;
        mSyncStateManager = syncStateManager;
    }

    public Observable<Track> loadTrack(final long trackId, Scheduler observeOn) {
        final Track cachedTrack = mModelManager.getCachedTrack(trackId);
        if (cachedTrack != null) {
            return Observable.just(cachedTrack);
        } else {
            return mTrackStorage.getTrackAsync(trackId).map(cacheTrack(trackId, ScResource.CacheUpdateMode.NONE))
                    .observeOn(observeOn);
        }
    }

    public Observable<Track> loadSyncedTrack(final long trackId, Scheduler observeOn) {
        return loadTrack(trackId, observeOn).mergeMap(mSyncIfStale)
                .onErrorResumeNext(handleTrackNotFound(trackId));
    }

    public Observable<Track> loadStreamableTrack(final long trackId,  Scheduler observeOn) {
        return loadTrack(trackId, observeOn).mergeMap(mSyncIfNotStreamable)
                .onErrorResumeNext(handleTrackNotFound(trackId));
    }

    private final Func1<Track, Observable<Track>> mSyncIfStale = new Func1<Track, Observable<Track>>() {
        @Override
        public Observable<Track> call(Track track) {
            LocalCollection syncState = mSyncStateManager.fromContent(track.toUri());
            if (syncState.isSyncDue()) {
                Log.d(LOG_TAG, "Checking track sync state: stale = " + syncState.isSyncDue());
                return Observable.concat(Observable.just(track), syncThenLoadTrack(track.getId()));
            }
            Log.d(LOG_TAG, "Track up to date, emitting directly");
            return Observable.just(track);
        }
    };

    private final Func1<Track, Observable<Track>> mSyncIfNotStreamable = new Func1<Track, Observable<Track>>() {
        @Override
        public Observable<Track> call(Track track) {
            if (!track.isStreamable()) {
                Log.d(LOG_TAG, "Syncing unstreamable track = " + track);
                return syncThenLoadTrack(track.getId());
            }
            Log.d(LOG_TAG, "Track is streamable, emitting directly");
            return Observable.just(track);
        }
    };

    /**
     * Performs a sync on the given track, then reloads it from local storage.
     * @param trackId
     */
    private Observable<Track> syncThenLoadTrack(final long trackId) {
        return Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(final Subscriber<? super Long> subscriber) {
                final ResultReceiver resultReceiver = new SyncInitiator.ResultReceiverAdapter<Long>(subscriber, trackId);
                    Log.d(LOG_TAG, "Sending intent to sync track " + trackId);
                    mSyncInitiator.syncContentUri(Content.TRACK.forId(trackId), resultReceiver);
            }
        }).mergeMap(new Func1<Long, Observable<Track>>() {
            @Override
            public Observable<Track> call(Long trackId) {
                Log.d(LOG_TAG, "Reloading track from local storage: " + trackId);
                return mTrackStorage.getTrackAsync(trackId);
            }
        });
    }

    /**
     * If a track cannot be found in local storage, returns a sync sequence for resume purposes, otherwise
     * simply propagates the error.
     */
    private Func1<Throwable, Observable<? extends Track>> handleTrackNotFound(final long trackId) {
        return new Func1<Throwable, Observable<? extends Track>>() {
            @Override
            public Observable<? extends Track> call(Throwable throwable) {
                if (throwable instanceof NotFoundException) {
                    Log.d(LOG_TAG, "Track missing from local storage, will sync " + trackId);
                    return syncThenLoadTrack(trackId);
                }
                Log.d(LOG_TAG, "Caught error, forwarding to observer: " + throwable);
                return Observable.error(throwable);
            }
        };
    }

    public Observable<Track> markTrackAsPlayed(Track track) {
        return mTrackStorage.createPlayImpressionAsync(track);
    }

    private Func1<Track, Track> cacheTrack(final long trackId, final ScResource.CacheUpdateMode updateMode){
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track nullableTrack) {
                return mModelManager.cache(nullableTrack == null ? new Track(trackId) : nullableTrack, updateMode);
            }
        };
    }
}
