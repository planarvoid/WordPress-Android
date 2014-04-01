package com.soundcloud.android.search;

import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.Link;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.storage.PlaylistTagStorage;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.OperationPaged;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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

    private final Action1<PlaylistSummaryCollection> mPreCachePlaylistResults = new Action1<PlaylistSummaryCollection>() {
        @Override
        public void call(PlaylistSummaryCollection collection) {
            fireAndForget(mBulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), PlaylistSummary.TO_PLAYLIST)));
        }
    };

    private final ScModelManager mModelManager;
    private final RxHttpClient mRxHttpClient;
    private final PlaylistTagStorage mTagStorage;
    private final BulkStorage mBulkStorage;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient, PlaylistTagStorage tagStorage,
                            BulkStorage bulkStorage, ScModelManager modelManager) {
        mRxHttpClient = rxHttpClient;
        mTagStorage = tagStorage;
        mBulkStorage = bulkStorage;
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
                .map(mCacheResources)
                .doOnNext(new Action1<SearchResultsCollection>() {
                    @Override
                    public void call(SearchResultsCollection collection) {
                        fireAndForget(mBulkStorage.bulkInsertAsync(collection));
                    }
                });
        return Observable.create(paged(source, mNextSearchResultsPageGenerator));
    }

    Observable<PlaylistTagsCollection> getRecentPlaylistTags() {
        return mTagStorage.getRecentTagsAsync();
    }

    Observable<PlaylistTagsCollection> getPlaylistTags() {
        APIRequest<PlaylistTagsCollection> request = RequestBuilder.<PlaylistTagsCollection>get(APIEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(PlaylistTagsCollection.class))
                .build();
        return mRxHttpClient.fetchModels(request);
    }

    Observable<Page<PlaylistSummaryCollection>> getPlaylistResults(final String query) {
        final APIRequest<PlaylistSummaryCollection> request =
                createPlaylistResultsRequest(APIEndpoints.PLAYLIST_DISCOVERY.path())
                        .addQueryParameters("tag", query)
                        .build();

        return getPlaylistResultsPage(query, request).doOnCompleted(new Action0() {
            @Override
            public void call() {
                mTagStorage.addRecentTag(query);
            }
        });
    }

    private Observable<Page<PlaylistSummaryCollection>> getPlaylistResultsNextPage(String query, String nextHref) {
        final RequestBuilder<PlaylistSummaryCollection> builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private RequestBuilder<PlaylistSummaryCollection> createPlaylistResultsRequest(String url) {
        return RequestBuilder.<PlaylistSummaryCollection>get(url)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(PlaylistSummaryCollection.class));
    }

    private Observable<Page<PlaylistSummaryCollection>> getPlaylistResultsPage(
            String query, APIRequest<PlaylistSummaryCollection> request) {
        Observable<PlaylistSummaryCollection> source = mRxHttpClient.fetchModels(request);
        source = source.doOnNext(mPreCachePlaylistResults).map(withSearchTag(query));
        return Observable.create(paged(source, nextDiscoveryResultsPageGenerator(query)));
    }

    private DiscoveryResultsNextPageFunction nextDiscoveryResultsPageGenerator(final String query) {
        return new DiscoveryResultsNextPageFunction() {
            @Override
            public Observable<Page<PlaylistSummaryCollection>> call(PlaylistSummaryCollection collection) {
                final Optional<Link> nextLink = collection.getNextLink();
                if (nextLink.isPresent()) {
                    return getPlaylistResultsNextPage(query, nextLink.get().getHref());
                } else {
                    return OperationPaged.emptyPageObservable();
                }
            }
        };
    };

    private Func1<PlaylistSummaryCollection, PlaylistSummaryCollection> withSearchTag(final String searchTag) {
        return new Func1<PlaylistSummaryCollection, PlaylistSummaryCollection>() {
            @Override
            public PlaylistSummaryCollection call(PlaylistSummaryCollection collection) {
                for (PlaylistSummary playlist : collection) {
                    LinkedList<String> tagsWithSearchTag = new LinkedList<String>(removeItemIgnoreCase(playlist.getTags(), searchTag));
                    tagsWithSearchTag.addFirst(searchTag);
                    playlist.setTags(tagsWithSearchTag);
                }
                return collection;
            }
        };
    }

    private Collection<String> removeItemIgnoreCase(List<String> list, String itemToRemove) {
        return Collections2.filter(list, Predicates.containsPattern("(?i)^(?!" + itemToRemove + "$).*$"));
    }

    private interface SearchResultsNextPageFunction extends Func1<SearchResultsCollection, Observable<Page<SearchResultsCollection>>> {}
    private interface DiscoveryResultsNextPageFunction extends Func1<PlaylistSummaryCollection, Observable<Page<PlaylistSummaryCollection>>> {}
}
