package com.soundcloud.android.search.topresults;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.search.CacheUniversalSearchCommand;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;

class TopResultsOperations {
    static final String QUERY_PARAM = "q";
    static final String QUERY_URN_PARAM = "query_urn";
    static final int RESULT_LIMIT = 2;
    private static final TypeToken<ApiTopResults> TYPE_TOKEN = new TypeToken<ApiTopResults>() {
    };

    private final ApiClientRxV2 apiClientRx;
    private final Scheduler scheduler;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;
    private final LikesStateProvider likedStatuses;
    private final FollowingStateProvider followingStatuses;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final EntityItemCreator entityItemCreator;

    @Inject
    TopResultsOperations(ApiClientRxV2 apiClientRx,
                         @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                         CacheUniversalSearchCommand cacheUniversalSearchCommand,
                         LikesStateProvider likedStatuses,
                         FollowingStateProvider followingStatuses,
                         PlaySessionStateProvider playSessionStateProvider,
                         EntityItemCreator entityItemCreator) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.likedStatuses = likedStatuses;
        this.followingStatuses = followingStatuses;
        this.playSessionStateProvider = playSessionStateProvider;
        this.entityItemCreator = entityItemCreator;
    }

    public Observable<SearchResult> search(SearchParams params) {
        return searchDataSource(params).map(SearchResult.Data::create)
                                       .onErrorReturn(SearchResult.Error::create)
                                       .startWith(SearchResult.Loading.create(params.isRefreshing()));
    }

    private io.reactivex.Observable<TopResults> searchDataSource(SearchParams search) {
        final Single<ApiTopResults> searchMaybe = apiSearch(search);
        return searchMaybe.toObservable().flatMap(result -> io.reactivex.Observable.combineLatest(
                RxJava.toV2Observable(likedStatuses.likedStatuses()),
                followingStatuses.followingStatuses(),
                RxJava.toV2Observable(playSessionStateProvider.nowPlayingUrn()),
                (likedStatuses, followingStatuses, urn) -> ApiTopResultsMapper.toDomainModel(result, entityItemCreator, likedStatuses, followingStatuses, urn)));
    }

    private Single<ApiTopResults> apiSearch(SearchParams search) {
        Optional<Urn> queryUrn = search.queryUrn();
        final ApiRequest.Builder requestBuilder = ApiRequest.get(ApiEndpoints.SEARCH_TOP_RESULTS.path())
                                                            .forPrivateApi()
                                                            .addQueryParam(ApiRequest.Param.PAGE_SIZE.toString(), RESULT_LIMIT)
                                                            .addQueryParam(QUERY_PARAM, search.apiQuery());
        if (queryUrn.isPresent()) {
            requestBuilder.addQueryParam(QUERY_URN_PARAM, queryUrn.get().toString());
        }
        return apiClientRx.mappedResponse(requestBuilder.build(), TYPE_TOKEN)
                          .subscribeOn(scheduler)
                          .doOnSuccess(this::cacheItems);

    }

    private void cacheItems(ApiTopResults apiTopResults) {
        for (ApiTopResultsBucket apiTopResultsBucket : apiTopResults.buckets()) {
            cacheUniversalSearchCommand.call(apiTopResultsBucket.collection().getCollection());
        }
    }
}
