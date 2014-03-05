package com.soundcloud.android.search;

import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;
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
import com.soundcloud.android.model.Link;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.storage.PlaylistTagStorage;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.OperationPaged;
import rx.functions.Action0;
import rx.util.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SearchOperations {

    private static final Func1<SearchResultsCollection, SearchResultsCollection> FILTER_UNKOWN_RESOURCES =
            new Func1<SearchResultsCollection, SearchResultsCollection>() {
                @Override
                public SearchResultsCollection call(SearchResultsCollection unfilteredResult) {
                    List<ScResource> filteredList = new ArrayList<ScResource>(Consts.LIST_PAGE_SIZE);
                    for (ScResource resource : unfilteredResult) {
                        if (!(resource instanceof UnknownResource)) {
                            filteredList.add(resource);
                        }
                    }
                    return new SearchResultsCollection(filteredList, unfilteredResult.getNextHref());
                }
            };

    private static final Func1<PlaylistTagsCollection, PlaylistTagsCollection> TO_HASH_TAGS =
            new Func1<PlaylistTagsCollection, PlaylistTagsCollection>() {
                @Override
                public PlaylistTagsCollection call(PlaylistTagsCollection tags) {
                    tags.setCollection(Lists.transform(tags.getCollection(), new Function<String, String>() {
                        @Override
                        public String apply(String tag) {
                            return "#" + tag;
                        }
                    }));
                    return tags;
                }
            };

    private final SearchResultsNextPageFunction mNextSearchResultsPageGenerator = new SearchResultsNextPageFunction() {
        @Override
        public Observable<Page<SearchResultsCollection>> call(SearchResultsCollection searchResultsCollection) {
            final String nextHref = searchResultsCollection.getNextHref();
            if (ScTextUtils.isNotBlank(nextHref)) {
                return getSearchResults(nextHref);
            } else {
                return OperationPaged.emptyPageObservable();
            }
        }
    };

    private final DiscoveryResultsNextPageFunction mNextDiscoveryResultsPageGenerator = new DiscoveryResultsNextPageFunction() {
        @Override
        public Observable<Page<PlaylistSummaryCollection>> call(PlaylistSummaryCollection collection) {
            final Optional<Link> nextLink = collection.getNextLink();
            if (nextLink.isPresent()) {
                return getPlaylistDiscoveryResultsNextPage(nextLink.get().getHref());
            } else {
                return OperationPaged.emptyPageObservable();
            }
        }
    };

    private final Func1<SearchResultsCollection, SearchResultsCollection> mCacheResources = new Func1<SearchResultsCollection, SearchResultsCollection>() {
        @Override
        public SearchResultsCollection call(SearchResultsCollection results) {
            List<ScResource> cachedResults = new ArrayList<ScResource>(results.size());
            for (ScResource resource : results) {
                ScResource cachedResource = mModelManager.cache(resource, ScResource.CacheUpdateMode.FULL);
                cachedResults.add(cachedResource);
            }
            return new SearchResultsCollection(cachedResults, results.getNextHref());
        }
    };

    private final RxHttpClient mRxHttpClient;
    private final PlaylistTagStorage mTagStorage;
    private final ScModelManager mModelManager;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient, PlaylistTagStorage tagStorage, ScModelManager modelManager) {
        mRxHttpClient = rxHttpClient;
        mTagStorage = tagStorage;
        mModelManager = modelManager;
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

    private Observable<Page<SearchResultsCollection>> getSearchResults(APIEndpoints apiEndpoint , @Nullable String query) {
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
        Observable<SearchResultsCollection> source = mRxHttpClient.<SearchResultsCollection>fetchModels(request)
                .map(FILTER_UNKOWN_RESOURCES)
                .map(mCacheResources);
        return Observable.create(paged(source, mNextSearchResultsPageGenerator));
    }

    Observable<PlaylistTagsCollection> getRecentPlaylistTags() {
        return mTagStorage.getRecentTagsAsync().map(TO_HASH_TAGS);
    }

    Observable<PlaylistTagsCollection> getPlaylistTags() {
        APIRequest<PlaylistTagsCollection> request = RequestBuilder.<PlaylistTagsCollection>get(APIEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(PlaylistTagsCollection.class))
                .build();
        return mRxHttpClient.<PlaylistTagsCollection>fetchModels(request).map(TO_HASH_TAGS);
    }

    Observable<Page<PlaylistSummaryCollection>> getPlaylistDiscoveryResults(final String query) {
        final String cleanTag = query.replaceFirst("#", "");
        final RequestBuilder<PlaylistSummaryCollection> builder = createPlaylistResultsRequest(APIEndpoints.PLAYLIST_DISCOVERY.path());
        return getPlaylistResultsPageObservable(builder.addQueryParameters("tag", cleanTag).build()).doOnCompleted(new Action0() {
            @Override
            public void call() {
                mTagStorage.addRecentTag(cleanTag);
            }
        });
    }

    Observable<Page<PlaylistSummaryCollection>> getPlaylistDiscoveryResultsNextPage(String nextHref) {
        final RequestBuilder<PlaylistSummaryCollection> builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPageObservable(builder.build());
    }

    private RequestBuilder<PlaylistSummaryCollection> createPlaylistResultsRequest(String url) {
        return RequestBuilder.<PlaylistSummaryCollection>get(url)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(PlaylistSummaryCollection.class));
    }


    private Observable<Page<PlaylistSummaryCollection>> getPlaylistResultsPageObservable(APIRequest<PlaylistSummaryCollection> request) {
        Observable<PlaylistSummaryCollection> source = mRxHttpClient.fetchModels(request);
        return Observable.create(paged(source, mNextDiscoveryResultsPageGenerator));
    }

    private interface SearchResultsNextPageFunction extends Func1<SearchResultsCollection, Observable<Page<SearchResultsCollection>>> {}
    private interface DiscoveryResultsNextPageFunction extends Func1<PlaylistSummaryCollection, Observable<Page<PlaylistSummaryCollection>>> {}
}
