package com.soundcloud.android.search;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperatorPaged.LegacyPager;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.pagedWith;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SearchResultsCollection;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.OperatorPaged;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class SearchOperations {

    private static final Func1<SearchResultsCollection, SearchResultsCollection> FILTER_UNKOWN_RESOURCES =
            new Func1<SearchResultsCollection, SearchResultsCollection>() {
                @Override
                public SearchResultsCollection call(SearchResultsCollection unfilteredResult) {
                    List<PublicApiResource> filteredList = new ArrayList<PublicApiResource>(Consts.LIST_PAGE_SIZE);
                    for (PublicApiResource resource : unfilteredResult) {
                        if (!(resource instanceof UnknownResource)) {
                            filteredList.add(resource);
                        }
                    }
                    return new SearchResultsCollection(filteredList, unfilteredResult.getNextHref());
                }
            };

    private final LegacyPager<SearchResultsCollection> searchResultsPager = new LegacyPager<SearchResultsCollection>() {
        @Override
        public Observable<Page<SearchResultsCollection>> call(SearchResultsCollection searchResultsCollection) {
            final String nextHref = searchResultsCollection.getNextHref();
            if (ScTextUtils.isNotBlank(nextHref)) {
                return getSearchResults(nextHref);
            } else {
                return OperatorPaged.emptyObservable();
            }
        }
    };

    private final Func1<SearchResultsCollection, SearchResultsCollection> cacheResources = new Func1<SearchResultsCollection, SearchResultsCollection>() {
        @Override
        public SearchResultsCollection call(SearchResultsCollection results) {
            List<PublicApiResource> cachedResults = new ArrayList<>(results.size());
            for (PublicApiResource resource : results) {
                PublicApiResource cachedResource = modelManager.cache(resource, PublicApiResource.CacheUpdateMode.FULL);
                cachedResults.add(cachedResource);
            }
            return new SearchResultsCollection(cachedResults, results.getNextHref());
        }
    };

    private final ScModelManager modelManager;
    private final RxHttpClient rxHttpClient;
    private final BulkStorage bulkStorage;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient, BulkStorage bulkStorage, ScModelManager modelManager) {
        this.rxHttpClient = rxHttpClient;
        this.bulkStorage = bulkStorage;
        this.modelManager = modelManager;
    }

    public Observable<Page<SearchResultsCollection>> getAllSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_ALL, query);
    }

    public Observable<Page<SearchResultsCollection>> getTrackSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_TRACKS, query);
    }

    public Observable<Page<SearchResultsCollection>> getPlaylistSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_PLAYLISTS, query);
    }

    public Observable<Page<SearchResultsCollection>> getUserSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_USERS, query);
    }

    private Observable<Page<SearchResultsCollection>> getSearchResults(APIEndpoints apiEndpoint, @Nullable String query) {
        final RequestBuilder<SearchResultsCollection> builder = createSearchRequestBuilder(apiEndpoint.path());
        return getPageObservable(builder.addQueryParameters("q", query).build());
    }

    private Observable<Page<SearchResultsCollection>> getSearchResults(String nextHref) {
        final RequestBuilder<SearchResultsCollection> builder = createSearchRequestBuilder(nextHref);
        return getPageObservable(builder.build());
    }

    private RequestBuilder<SearchResultsCollection> createSearchRequestBuilder(String path) {
        return RequestBuilder.<SearchResultsCollection>get(path)
                .addQueryParameters("limit", String.valueOf(Consts.LIST_PAGE_SIZE))
                .forPublicAPI()
                .forResource(TypeToken.of(SearchResultsCollection.class));
    }

    private Observable<Page<SearchResultsCollection>> getPageObservable(APIRequest<SearchResultsCollection> request) {
        Observable<SearchResultsCollection> source = rxHttpClient.<SearchResultsCollection>fetchModels(request)
                .map(FILTER_UNKOWN_RESOURCES)
                .map(cacheResources)
                .doOnNext(new Action1<SearchResultsCollection>() {
                    @Override
                    public void call(SearchResultsCollection collection) {
                        fireAndForget(bulkStorage.bulkInsertAsync(collection));
                    }
                });
        return source.lift(pagedWith(searchResultsPager));
    }

}
