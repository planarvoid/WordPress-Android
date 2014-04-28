package com.soundcloud.android.explore;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

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
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.storage.BulkStorage;
import rx.Observable;
import rx.android.OperatorPaged;
import rx.functions.Action1;

import javax.inject.Inject;

class ExploreTracksOperations {

    private final RxHttpClient rxHttpClient;
    private final BulkStorage bulkStorage;

    private final Action1<SuggestedTracksCollection> cacheSuggestedTracks = new Action1<SuggestedTracksCollection>() {
        @Override
        public void call(SuggestedTracksCollection collection) {
            fireAndForget(bulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), TrackSummary.TO_TRACK)));
        }
    };

    @Inject
    ExploreTracksOperations(RxHttpClient rxHttpClient, BulkStorage bulkStorage) {
        this.rxHttpClient = rxHttpClient;
        this.bulkStorage = bulkStorage;
    }

    public Observable<ExploreGenresSections> getCategories() {
        APIRequest<ExploreGenresSections> request = SoundCloudAPIRequest.RequestBuilder.<ExploreGenresSections>get(APIEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ExploreGenresSections.class)).build();
        return rxHttpClient.fetchModels(request);
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

        Observable<SuggestedTracksCollection> source = rxHttpClient.fetchModels(request);
        return source.doOnNext(cacheSuggestedTracks).lift(pagedWith(pager));
    }

    private final Pager<SuggestedTracksCollection> pager = new Pager<SuggestedTracksCollection>() {
        @Override
        public Observable<Page<SuggestedTracksCollection>> call(SuggestedTracksCollection trackSummaries) {
            final Optional<Link> nextLink = trackSummaries.getNextLink();
            if (nextLink.isPresent()) {
                return getSuggestedTracks(nextLink.get().getHref());
            } else {
                return OperatorPaged.emptyPageObservable();
            }
        }
    };
}
