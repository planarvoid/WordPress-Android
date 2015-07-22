package com.soundcloud.android.explore;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.android.LegacyPager;

import javax.inject.Inject;
import javax.inject.Named;

class ExploreTracksOperations {

    private final StoreTracksCommand storeTracksCommand;
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;

    private final LegacyPager<SuggestedTracksCollection> pager = new LegacyPager<SuggestedTracksCollection>() {
        @Override
        public Observable<SuggestedTracksCollection> call(SuggestedTracksCollection apiTracks) {
            final Optional<Link> nextLink = apiTracks.getNextLink();
            if (nextLink.isPresent()) {
                return getSuggestedTracks(nextLink.get().getHref());
            } else {
                return LegacyPager.finish();
            }
        }
    };

    @Inject
    ExploreTracksOperations(StoreTracksCommand storeTracksCommand, ApiClientRx apiClientRx,
                            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeTracksCommand = storeTracksCommand;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
    }

    Observable<ExploreGenresSections> getCategories() {
        ApiRequest request = ApiRequest.get(ApiEndpoints.EXPLORE_TRACKS_CATEGORIES.path())
                .forPrivateApi(1)
                .build();
        return apiClientRx.mappedResponse(request, ExploreGenresSections.class).subscribeOn(scheduler);
    }

    Observable<SuggestedTracksCollection> getSuggestedTracks(ExploreGenre category) {
        if (category == ExploreGenre.POPULAR_MUSIC_CATEGORY) {
            return getSuggestedTracks(ApiEndpoints.EXPLORE_TRACKS_POPULAR_MUSIC.path());
        } else if (category == ExploreGenre.POPULAR_AUDIO_CATEGORY) {
            return getSuggestedTracks(ApiEndpoints.EXPLORE_TRACKS_POPULAR_AUDIO.path());
        } else {
            return getSuggestedTracks(category.getSuggestedTracksPath());
        }
    }

    LegacyPager<SuggestedTracksCollection> pager() {
        return pager;
    }

    private Observable<SuggestedTracksCollection> getSuggestedTracks(String endpoint) {
        ApiRequest request = ApiRequest.get(endpoint)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.CARD_PAGE_SIZE))
                .forPrivateApi(1)
                .build();

        return apiClientRx.mappedResponse(request, SuggestedTracksCollection.class)
                .doOnNext(storeTracksCommand.toAction())
                .subscribeOn(scheduler);
    }
}
