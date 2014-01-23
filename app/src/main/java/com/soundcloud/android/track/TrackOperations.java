package com.soundcloud.android.track;

import static rx.android.observables.AndroidObservable.fromActivity;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.api.Endpoints;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;
import java.util.Locale;

public class TrackOperations {

    private final ScModelManager mModelManager;
    private final TrackStorage mTrackStorage;
    private final RxHttpClient mRxHttpClient;

    @Inject
    public TrackOperations(ScModelManager modelManager, TrackStorage trackStorage, RxHttpClient rxHttpClient) {
        mModelManager = modelManager;
        mTrackStorage = trackStorage;
        mRxHttpClient = rxHttpClient;
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

    /**
     * Load the track from storage and fallback to the api if the stored track is incomplete : {@link com.soundcloud.android.model.Track#isIncomplete()}
     * Note: this will call onNext twice on an incomplete track, so this must be accounted for by the caller
     */
    public Observable<Track> loadCompleteTrack(final Activity fromActivity, final long trackId) {
        return loadTrack(trackId, AndroidSchedulers.mainThread()).mapMany(new Func1<Track, Observable<? extends Track>>() {
            @Override
            public Observable<? extends Track> call(final Track track) {
                return Observable.create(new Observable.OnSubscribeFunc<Track>() {
                    @Override
                    public Subscription onSubscribe(Observer<? super Track> observer) {
                        observer.onNext(track);
                        if (track.isIncomplete()) {
                            return fromActivity(fromActivity, getCompleteTrackFromApi(trackId)
                                    .map(cacheAndStoreTrack(trackId, ScResource.CacheUpdateMode.FULL)))
                                    .subscribe(observer);
                        } else {
                            observer.onCompleted();
                        }
                        return Subscriptions.empty();
                    }
                });
            }
        });
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

    private Func1<Track, Track> cacheAndStoreTrack(final long trackId, final ScResource.CacheUpdateMode updateMode){
        return new Func1<Track, Track>() {
            @Override
            public Track call(Track nullableTrack) {
                final Track track = mModelManager.cache(nullableTrack == null ? new Track(trackId) : nullableTrack, updateMode);
                mTrackStorage.createOrUpdate(track);
                return track;
            }
        };
    }

    private Observable<Track> getCompleteTrackFromApi(long trackId) {
        String trackEndpoint = String.format(Locale.US, Endpoints.TRACK_DETAILS, trackId);
        APIRequest<Track> request = SoundCloudAPIRequest.RequestBuilder.<Track>get(trackEndpoint)
                .forPublicAPI()
                .forResource(TypeToken.of(Track.class)).build();
        return mRxHttpClient.fetchModels(request);
    }
}
