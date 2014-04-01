package com.soundcloud.android.explore;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.ExploreGenresSections;
import com.soundcloud.android.model.Link;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.BulkStorage;
import rx.Observable;
import rx.android.OperationPaged;
import rx.functions.Action1;
import rx.util.functions.Func1;

import javax.inject.Inject;

class ExploreTracksOperations extends ScheduledOperations {

    private final RxHttpClient mRxHttpClient;
    private final BulkStorage mBulkStorage;

    private final Action1<SuggestedTracksCollection> mCacheSuggestedTracks = new Action1<SuggestedTracksCollection>() {
        @Override
        public void call(SuggestedTracksCollection collection) {
            final Function<TrackSummary, Track> function = new Function<TrackSummary, Track>() {
                @Override
                public Track apply(TrackSummary input) {
                    return new Track(input);
                }
            };
            fireAndForget(mBulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), function)));
        }
    };

    @Inject
    ExploreTracksOperations(RxHttpClient rxHttpClient, BulkStorage bulkStorage) {
        mRxHttpClient = rxHttpClient;
        mBulkStorage = bulkStorage;
    }

    public Observable<ExploreGenresSections> getCategories() {
        APIRequest<ExploreGenresSections> request = SoundCloudAPIRequest.RequestBuilder.<ExploreGenresSections>get(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ExploreGenresSections.class)).build();
        return mRxHttpClient.fetchModels(request);
    }

    public Observable<Page<SuggestedTracksCollection>> getSuggestedTracks(ExploreGenre category) {
        if (category == ExploreGenre.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreGenre.POPULAR_AUDIO_CATEGORY) {
            return getSuggestedTracks(APIEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
        } else {
            return getSuggestedTracks(category.getSuggestedTracksPath());
        }
    }

    private Observable<Page<SuggestedTracksCollection>> getSuggestedTracks(String endpoint) {
        APIRequest<SuggestedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<SuggestedTracksCollection>get(endpoint)
                .addQueryParameters("limit", String.valueOf(Consts.CARD_PAGE_SIZE))
                .forPrivateAPI(1)
                .forResource(TypeToken.of(SuggestedTracksCollection.class)).build();

        Observable<SuggestedTracksCollection> source = mRxHttpClient.fetchModels(request);
        return Observable.create(paged(source.doOnNext(mCacheSuggestedTracks), nextPageGenerator));
    }

    private final TrackSummariesNextPageFunc nextPageGenerator = new TrackSummariesNextPageFunc() {
        @Override
        public Observable<Page<SuggestedTracksCollection>> call(SuggestedTracksCollection trackSummaries) {
            final Optional<Link> nextLink = trackSummaries.getNextLink();
            if (nextLink.isPresent()) {
                return getSuggestedTracks(nextLink.get().getHref());
            } else {
                return OperationPaged.emptyPageObservable();
            }
        }
    };

    private interface TrackSummariesNextPageFunc extends Func1<SuggestedTracksCollection, Observable<Page<SuggestedTracksCollection>>> {}
}
