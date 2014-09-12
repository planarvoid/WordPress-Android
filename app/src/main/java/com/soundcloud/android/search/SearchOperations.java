package com.soundcloud.android.search;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.LegacyPager;
import static rx.android.OperatorPaged.pagedWith;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistCollection;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.playlists.PlaylistTagsCollection;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SearchResultsCollection;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.OperatorPaged;
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
            List<PublicApiResource> cachedResults = new ArrayList<PublicApiResource>(results.size());
            for (PublicApiResource resource : results) {
                PublicApiResource cachedResource = modelManager.cache(resource, PublicApiResource.CacheUpdateMode.FULL);
                cachedResults.add(cachedResource);
            }
            return new SearchResultsCollection(cachedResults, results.getNextHref());
        }
    };

    private final Action1<ApiPlaylistCollection> preCachePlaylistResults = new Action1<ApiPlaylistCollection>() {
        @Override
        public void call(ApiPlaylistCollection collection) {
            fireAndForget(bulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), ApiPlaylist.TO_PLAYLIST)));
        }
    };

    private final ScModelManager modelManager;
    private final RxHttpClient rxHttpClient;
    private final PlaylistTagStorage tagStorage;
    private final BulkStorage bulkStorage;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient, PlaylistTagStorage tagStorage,
                            BulkStorage bulkStorage, ScModelManager modelManager) {
        this.rxHttpClient = rxHttpClient;
        this.tagStorage = tagStorage;
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

    Observable<PlaylistTagsCollection> getRecentPlaylistTags() {
        return tagStorage.getRecentTagsAsync();
    }

    Observable<PlaylistTagsCollection> getPlaylistTags() {
        return getCachedPlaylistTags().mergeMap(new Func1<PlaylistTagsCollection, Observable<PlaylistTagsCollection>>() {
            @Override
            public Observable<PlaylistTagsCollection> call(PlaylistTagsCollection tags) {
                if (tags.getCollection().isEmpty()) {
                    return fetchAndCachePopularTags();
                }
                return Observable.just(tags);
            }
        });
    }

    private Observable<PlaylistTagsCollection> getCachedPlaylistTags() {
        return tagStorage.getPopularTagsAsync();
    }

    private Observable<PlaylistTagsCollection> fetchAndCachePopularTags() {
        APIRequest<PlaylistTagsCollection> request = RequestBuilder.<PlaylistTagsCollection>get(APIEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                .forPrivateAPI(1)
                .forResource(TypeToken.of(PlaylistTagsCollection.class))
                .build();
        return rxHttpClient.<PlaylistTagsCollection>fetchModels(request).doOnNext(new Action1<PlaylistTagsCollection>() {
            @Override
            public void call(PlaylistTagsCollection tags) {
                tagStorage.cachePopularTags(tags.getCollection());
            }
        });
    }

    Observable<Page<ApiPlaylistCollection>> getPlaylistResults(final String query) {
        final APIRequest<ApiPlaylistCollection> request =
                createPlaylistResultsRequest(APIEndpoints.PLAYLIST_DISCOVERY.path())
                        .addQueryParameters("tag", query)
                        .build();
        return getPlaylistResultsPage(query, request).finallyDo(new Action0() {
            @Override
            public void call() {
                tagStorage.addRecentTag(query);
            }
        });
    }

    private Observable<Page<ApiPlaylistCollection>> getPlaylistResultsNextPage(String query, String nextHref) {
        final RequestBuilder<ApiPlaylistCollection> builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private RequestBuilder<ApiPlaylistCollection> createPlaylistResultsRequest(String url) {
        return RequestBuilder.<ApiPlaylistCollection>get(url)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ApiPlaylistCollection.class));
    }

    private Observable<Page<ApiPlaylistCollection>> getPlaylistResultsPage(
            String query, APIRequest<ApiPlaylistCollection> request) {
        Observable<ApiPlaylistCollection> source = rxHttpClient.fetchModels(request);
        source = source.doOnNext(preCachePlaylistResults).map(withSearchTag(query));
        return source.lift(pagedWith(discoveryResultsPager(query)));
    }

    private LegacyPager<ApiPlaylistCollection> discoveryResultsPager(final String query) {
        return new LegacyPager<ApiPlaylistCollection>() {
            @Override
            public Observable<Page<ApiPlaylistCollection>> call(ApiPlaylistCollection collection) {
                final Optional<Link> nextLink = collection.getNextLink();
                if (nextLink.isPresent()) {
                    return getPlaylistResultsNextPage(query, nextLink.get().getHref());
                } else {
                    return OperatorPaged.emptyObservable();
                }
            }
        };
    }

    private Func1<ApiPlaylistCollection, ApiPlaylistCollection> withSearchTag(final String searchTag) {
        return new Func1<ApiPlaylistCollection, ApiPlaylistCollection>() {
            @Override
            public ApiPlaylistCollection call(ApiPlaylistCollection collection) {
                for (ApiPlaylist playlist : collection) {
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
}
