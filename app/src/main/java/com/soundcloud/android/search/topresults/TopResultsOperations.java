package com.soundcloud.android.search.topresults;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.CacheUniversalSearchCommand;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

class TopResultsOperations {
    static final String QUERY_PARAM = "q";
    static final String QUERY_URN_PARAM = "query_urn";
    static final int RESULT_LIMIT = 2;
    private static final TypeToken<ApiTopResults> TYPE_TOKEN = new TypeToken<ApiTopResults>() {
    };

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;

    @Inject
    TopResultsOperations(ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler, CacheUniversalSearchCommand cacheUniversalSearchCommand) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
    }

    public Observable<TopResults> search(SearchParams searchParams) {
        return search(searchParams.apiQuery(), searchParams.queryUrn());
    }

    public Observable<TopResults> search(String query, Optional<Urn> queryUrn) {
        final ApiRequest.Builder requestBuilder = ApiRequest.get(ApiEndpoints.SEARCH_TOP_RESULTS.path())
                                                            .forPrivateApi()
                                                            .addQueryParam(ApiRequest.Param.PAGE_SIZE.toString(), RESULT_LIMIT)
                                                            .addQueryParam(QUERY_PARAM, query);
        if (queryUrn.isPresent()) {
            requestBuilder.addQueryParam(QUERY_URN_PARAM, queryUrn.get().toString());
        }
        return apiClientRx.mappedResponse(requestBuilder.build(), TYPE_TOKEN)
                          .subscribeOn(scheduler)
                          .doOnNext(this::cacheItems)
                .map(ApiTopResultsMapper::toDomainModel);

    }

    private void cacheItems(ApiTopResults apiTopResults) {
        for (ApiTopResultsBucket apiTopResultsBucket : apiTopResults.buckets()) {
            cacheUniversalSearchCommand.call(apiTopResultsBucket.collection().getCollection());
        }
    }
}
