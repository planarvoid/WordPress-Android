package com.soundcloud.android.explore;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.tracks.TrackWriteStorage;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Action1;

import javax.inject.Inject;

class ExploreTracksOperations {

    private final TrackWriteStorage trackWriteStorage;
    private final ApiScheduler apiScheduler;

    private final Action1<SuggestedTracksCollection> cacheSuggestedTracks = new Action1<SuggestedTracksCollection>() {
        @Override
        public void call(SuggestedTracksCollection collection) {
            fireAndForget(trackWriteStorage.storeTracksAsync(collection.getCollection()));
        }
    };

    private final Pager<SuggestedTracksCollection> pager = new Pager<SuggestedTracksCollection>() {
        @Override
        public Observable<SuggestedTracksCollection> call(SuggestedTracksCollection apiTracks) {
            final Optional<Link> nextLink = apiTracks.getNextLink();
            if (nextLink.isPresent()) {
                return getSuggestedTracks(nextLink.get().getHref());
            } else {
                return Pager.finish();
            }
        }
    };

    @Inject
    ExploreTracksOperations(TrackWriteStorage trackWriteStorage, ApiScheduler apiScheduler) {
        this.trackWriteStorage = trackWriteStorage;
        this.apiScheduler = apiScheduler;
    }

    public Observable<ExploreGenresSections> getCategories() {
        ApiRequest<ExploreGenresSections> request = ApiRequest.Builder.<ExploreGenresSections>get(ApiEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateApi(1)
                .forResource(TypeToken.of(ExploreGenresSections.class)).build();
        return apiScheduler.mappedResponse(request);
    }

    public Observable<SuggestedTracksCollection> getSuggestedTracks(ExploreGenre category) {
        if (category == ExploreGenre.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(ApiEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreGenre.POPULAR_AUDIO_CATEGORY) {
            return getSuggestedTracks(ApiEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
        } else {
            return getSuggestedTracks(category.getSuggestedTracksPath());
        }
    }

    Pager<SuggestedTracksCollection> pager() {
        return pager;
    }

    private Observable<SuggestedTracksCollection> getSuggestedTracks(String endpoint) {
        ApiRequest<SuggestedTracksCollection> request = ApiRequest.Builder.<SuggestedTracksCollection>get(endpoint)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.CARD_PAGE_SIZE))
                .forPrivateApi(1)
                .forResource(TypeToken.of(SuggestedTracksCollection.class)).build();

        return apiScheduler.mappedResponse(request).doOnNext(cacheSuggestedTracks);
    }
}
