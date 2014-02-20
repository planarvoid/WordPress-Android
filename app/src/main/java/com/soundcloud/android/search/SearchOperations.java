package com.soundcloud.android.search;

import static rx.android.OperationPaged.paged;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.OperationPaged;
import rx.util.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SearchOperations {

    private static final Func1<SearchResultsCollection, SearchResultsCollection> FILTER_UNKOWN_RESOURCES =
            new Func1<SearchResultsCollection, SearchResultsCollection>() {
                @Override
                public SearchResultsCollection call(SearchResultsCollection unfilteredResult) {
                    List<ScResource> filteredList = new ArrayList<ScResource>(Consts.COLLECTION_PAGE_SIZE);
                    for (ScResource resource : unfilteredResult) {
                        if (!(resource instanceof UnknownResource)) {
                            filteredList.add(resource);
                        }
                    }
                    return new SearchResultsCollection(filteredList, unfilteredResult.getNextHref());
                }
            };

    private final RxHttpClient mRxHttpClient;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<OperationPaged.Page<SearchResultsCollection>> getAllSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_ALL, query);
    }

    public Observable<OperationPaged.Page<SearchResultsCollection>> getTrackSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_TRACKS, query);
    }

    public Observable<OperationPaged.Page<SearchResultsCollection>> getPlaylistSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_PLAYLISTS, query);
    }

    public Observable<OperationPaged.Page<SearchResultsCollection>> getUserSearchResults(String query) {
        return getSearchResults(APIEndpoints.SEARCH_USERS, query);
    }

    private Observable<OperationPaged.Page<SearchResultsCollection>> getSearchResults(APIEndpoints apiEndpoint , @Nullable String query) {
        final SoundCloudAPIRequest.RequestBuilder<SearchResultsCollection> builder = createSearchRequestBuilder(apiEndpoint.path());
        return getPageObservable(builder.addQueryParameters("q", query).build());
    }

    private Observable<OperationPaged.Page<SearchResultsCollection>> getSearchResults(String nextHref) {
        final SoundCloudAPIRequest.RequestBuilder<SearchResultsCollection> builder = createSearchRequestBuilder(nextHref);
        return getPageObservable(builder.build());
    }

    private SoundCloudAPIRequest.RequestBuilder<SearchResultsCollection> createSearchRequestBuilder(String path) {
        return SoundCloudAPIRequest.RequestBuilder.<SearchResultsCollection>get(path)
                    .addQueryParameters("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE))
                    .forPublicAPI()
                    .forResource(TypeToken.of(SearchResultsCollection.class));
    }

    private Observable<OperationPaged.Page<SearchResultsCollection>> getPageObservable(APIRequest<SearchResultsCollection> request) {
        Observable<SearchResultsCollection> source = mRxHttpClient.<SearchResultsCollection>fetchModels(request).map(FILTER_UNKOWN_RESOURCES);
        return Observable.create(paged(source, nextPageGenerator));
    }

    private final SearchResultsNextPageFunction nextPageGenerator = new SearchResultsNextPageFunction() {
        @Override
        public Observable<OperationPaged.Page<SearchResultsCollection>> call(SearchResultsCollection searchResultsCollection) {
            final String nextHref = searchResultsCollection.getNextHref();
            if (ScTextUtils.isNotBlank(nextHref)) {
                return getSearchResults(nextHref);
            } else {
                return OperationPaged.emptyPageObservable();
            }
        }
    };

    private interface SearchResultsNextPageFunction extends Func1<SearchResultsCollection, Observable<OperationPaged.Page<SearchResultsCollection>>> {}
}
