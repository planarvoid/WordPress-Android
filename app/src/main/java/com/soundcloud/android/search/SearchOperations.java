package com.soundcloud.android.search;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.UnknownResource;
import rx.Observable;
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
                    return new SearchResultsCollection(filteredList);
                }
            };

    private final RxHttpClient mRxHttpClient;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<SearchResultsCollection> getAllSearchResults(String query) {
        return getSearchResults(query, APIEndpoints.SEARCH_ALL);
    }

    public Observable<SearchResultsCollection> getTrackSearchResults(String query) {
        return getSearchResults(query, APIEndpoints.SEARCH_TRACKS);
    }

    public Observable<SearchResultsCollection> getPlaylistSearchResults(String query) {
        return getSearchResults(query, APIEndpoints.SEARCH_PLAYLISTS);
    }

    public Observable<SearchResultsCollection> getUserSearchResults(String query) {
        return getSearchResults(query, APIEndpoints.SEARCH_USERS);
    }

    private Observable<SearchResultsCollection> getSearchResults(String query, APIEndpoints apiEndpoint) {
        APIRequest<SearchResultsCollection> request = SoundCloudAPIRequest.RequestBuilder.<SearchResultsCollection>get(apiEndpoint.path())
                .addQueryParameters("q", query)
                .addQueryParameters("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE))
                .forPublicAPI()
                .forResource(TypeToken.of(SearchResultsCollection.class)).build();

        return mRxHttpClient.<SearchResultsCollection>fetchModels(request).map(FILTER_UNKOWN_RESOURCES);
    }
}
