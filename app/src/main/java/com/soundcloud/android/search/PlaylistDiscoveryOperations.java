package com.soundcloud.android.search;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static rx.android.OperatorPaged.pagedWith;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
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
    private final PlaylistWriteStorage playlistWriteStorage;

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

    private final Action1<ModelCollection<ApiPlaylist>> preCachePlaylistResults = new Action1<ModelCollection<ApiPlaylist>>() {
        @Override
        public void call(ModelCollection<ApiPlaylist> collection) {
            fireAndForget(playlistWriteStorage.storePlaylistsAsync(collection.getCollection()));
        }
    };


    @Inject
    PlaylistDiscoveryOperations(RxHttpClient rxHttpClient, PlaylistTagStorage tagStorage, PlaylistWriteStorage playlistWriteStorage) {
        this.rxHttpClient = rxHttpClient;
        this.tagStorage = tagStorage;
        this.playlistWriteStorage = playlistWriteStorage;
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

    Observable<OperatorPaged.Page<ModelCollection<ApiPlaylist>>> playlistsForTag(final String tag) {
        final APIRequest<ModelCollection<ApiPlaylist>> request =
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

    private Observable<OperatorPaged.Page<ModelCollection<ApiPlaylist>>> getPlaylistResultsNextPage(String query, String nextHref) {
        final SoundCloudAPIRequest.RequestBuilder<ModelCollection<ApiPlaylist>> builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private SoundCloudAPIRequest.RequestBuilder<ModelCollection<ApiPlaylist>> createPlaylistResultsRequest(String url) {
        return SoundCloudAPIRequest.RequestBuilder.<ModelCollection<ApiPlaylist>>get(url)
                .forPrivateAPI(1)
                .forResource(new TypeToken<ModelCollection<ApiPlaylist>>() {
                });
    }

    private Observable<OperatorPaged.Page<ModelCollection<ApiPlaylist>>> getPlaylistResultsPage(
            String query, APIRequest<ModelCollection<ApiPlaylist>> request) {
        Observable<ModelCollection<ApiPlaylist>> source = rxHttpClient.fetchModels(request);
        source = source.doOnNext(preCachePlaylistResults).map(withSearchTag(query));
        return source.lift(pagedWith(discoveryResultsPager(query)));
    }

    private OperatorPaged.LegacyPager<ModelCollection<ApiPlaylist>> discoveryResultsPager(final String query) {
        return new OperatorPaged.LegacyPager<ModelCollection<ApiPlaylist>>() {
            @Override
            public Observable<OperatorPaged.Page<ModelCollection<ApiPlaylist>>> call(ModelCollection<ApiPlaylist> collection) {
                final Optional<Link> nextLink = collection.getNextLink();
                if (nextLink.isPresent()) {
                    return getPlaylistResultsNextPage(query, nextLink.get().getHref());
                } else {
                    return OperatorPaged.emptyObservable();
                }
            }
        };
    }

    private Func1<ModelCollection<ApiPlaylist>, ModelCollection<ApiPlaylist>> withSearchTag(final String searchTag) {
        return new Func1<ModelCollection<ApiPlaylist>, ModelCollection<ApiPlaylist>>() {
            @Override
            public ModelCollection<ApiPlaylist> call(ModelCollection<ApiPlaylist> collection) {
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
