package com.soundcloud.android.explore;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.commands.StoreTracksCommand;
import rx.Observable;
import rx.android.Pager;

import javax.inject.Inject;

class ExploreTracksOperations {

    private final StoreTracksCommand storeTracksCommand;
    private final ApiScheduler apiScheduler;

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
    ExploreTracksOperations(StoreTracksCommand storeTracksCommand, ApiScheduler apiScheduler) {
        this.storeTracksCommand = storeTracksCommand;
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

        return apiScheduler.mappedResponse(request).doOnNext(storeTracksCommand.toAction());
    }
}
