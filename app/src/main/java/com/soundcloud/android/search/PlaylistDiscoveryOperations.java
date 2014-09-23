package com.soundcloud.android.search;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperatorPaged.pagedWith;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistCollection;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.storage.BulkStorage;
import rx.Observable;
import rx.android.OperatorPaged;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class PlaylistDiscoveryOperations {

    private final RxHttpClient rxHttpClient;
    private final PlaylistTagStorage tagStorage;
    private final BulkStorage bulkStorage;

    private final Func1<ModelCollection<String>, List<String>> collectionToList = new Func1<ModelCollection<String>, List<String>>() {
        @Override
        public List<String> call(ModelCollection<String> collection) {
            return collection.getCollection();
        }
    };

    private final Action1<ModelCollection<String>> cachePopularTags = new Action1<ModelCollection<String>>() {
        @Override
        public void call(ModelCollection<String> tags) {
            tagStorage.cachePopularTags(tags.getCollection());
        }
    };

    private final Action1<ApiPlaylistCollection> preCachePlaylistResults = new Action1<ApiPlaylistCollection>() {
        @Override
        public void call(ApiPlaylistCollection collection) {
            fireAndForget(bulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), ApiPlaylist.TO_PLAYLIST)));
        }
    };


    @Inject
    PlaylistDiscoveryOperations(RxHttpClient rxHttpClient, PlaylistTagStorage tagStorage, BulkStorage bulkStorage) {
        this.rxHttpClient = rxHttpClient;
        this.tagStorage = tagStorage;
        this.bulkStorage = bulkStorage;
    }


    Observable<List<String>> recentPlaylistTags() {
        return tagStorage.getRecentTagsAsync();
    }

    Observable<List<String>> popularPlaylistTags() {
        return getCachedPlaylistTags().mergeMap(new Func1<List<String>, Observable<List<String>>>() {
            @Override
            public Observable<List<String>> call(List<String> tags) {
                if (tags.isEmpty()) {
                    return fetchAndCachePopularTags();
                }
                return Observable.just(tags);
            }
        });
    }

    private Observable<List<String>> getCachedPlaylistTags() {
        return tagStorage.getPopularTagsAsync();
    }

    private Observable<List<String>> fetchAndCachePopularTags() {
        APIRequest<ModelCollection<String>> request = SoundCloudAPIRequest.RequestBuilder.<ModelCollection<String>>get(APIEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                .forPrivateAPI(1)
                .forResource(new TypeToken<ModelCollection<String>>() {
                })
                .build();
        return rxHttpClient.<ModelCollection<String>>fetchModels(request).doOnNext(cachePopularTags).map(collectionToList);
    }

    Observable<OperatorPaged.Page<ApiPlaylistCollection>> playlistsForTag(final String tag) {
        final APIRequest<ApiPlaylistCollection> request =
                createPlaylistResultsRequest(APIEndpoints.PLAYLIST_DISCOVERY.path())
                        .addQueryParameters("tag", tag)
                        .build();
        return getPlaylistResultsPage(tag, request).finallyDo(new Action0() {
            @Override
            public void call() {
                tagStorage.addRecentTag(tag);
            }
        });
    }

    private Observable<OperatorPaged.Page<ApiPlaylistCollection>> getPlaylistResultsNextPage(String query, String nextHref) {
        final SoundCloudAPIRequest.RequestBuilder<ApiPlaylistCollection> builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private SoundCloudAPIRequest.RequestBuilder<ApiPlaylistCollection> createPlaylistResultsRequest(String url) {
        return SoundCloudAPIRequest.RequestBuilder.<ApiPlaylistCollection>get(url)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ApiPlaylistCollection.class));
    }

    private Observable<OperatorPaged.Page<ApiPlaylistCollection>> getPlaylistResultsPage(
            String query, APIRequest<ApiPlaylistCollection> request) {
        Observable<ApiPlaylistCollection> source = rxHttpClient.fetchModels(request);
        source = source.doOnNext(preCachePlaylistResults).map(withSearchTag(query));
        return source.lift(pagedWith(discoveryResultsPager(query)));
    }

    private OperatorPaged.LegacyPager<ApiPlaylistCollection> discoveryResultsPager(final String query) {
        return new OperatorPaged.LegacyPager<ApiPlaylistCollection>() {
            @Override
            public Observable<OperatorPaged.Page<ApiPlaylistCollection>> call(ApiPlaylistCollection collection) {
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
