package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class PlaylistDiscoveryOperations {

    private final ApiScheduler apiScheduler;
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
            playlistWriteStorage.storePlaylists(collection.getCollection());
        }
    };


    @Inject
    PlaylistDiscoveryOperations(ApiScheduler apiScheduler, PlaylistTagStorage tagStorage, PlaylistWriteStorage playlistWriteStorage) {
        this.apiScheduler = apiScheduler;
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
        ApiRequest<ModelCollection<String>> request = ApiRequest.Builder.<ModelCollection<String>>get(ApiEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                .forPrivateApi(1)
                .forResource(new TypeToken<ModelCollection<String>>() {
                })
                .build();
        return apiScheduler.mappedResponse(request).doOnNext(cachePopularTags).map(collectionToList);
    }

    Observable<ModelCollection<ApiPlaylist>> playlistsForTag(final String tag) {
        final ApiRequest<ModelCollection<ApiPlaylist>> request =
                createPlaylistResultsRequest(ApiEndpoints.PLAYLIST_DISCOVERY.path())
                        .addQueryParam("tag", tag)
                        .build();
        return getPlaylistResultsPage(tag, request).finallyDo(new Action0() {
            @Override
            public void call() {
                tagStorage.addRecentTag(tag);
            }
        });
    }

    PlaylistPager pager(String tag) {
        return new PlaylistPager(tag);
    }

    private Observable<ModelCollection<ApiPlaylist>> getPlaylistResultsNextPage(String query, String nextHref) {
        final ApiRequest.Builder<ModelCollection<ApiPlaylist>> builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private ApiRequest.Builder<ModelCollection<ApiPlaylist>> createPlaylistResultsRequest(String url) {
        return ApiRequest.Builder.<ModelCollection<ApiPlaylist>>get(url)
                .forPrivateApi(1)
                .forResource(new TypeToken<ModelCollection<ApiPlaylist>>() {
                });
    }

    private Observable<ModelCollection<ApiPlaylist>> getPlaylistResultsPage(
            String query, ApiRequest<ModelCollection<ApiPlaylist>> request) {
        return apiScheduler.mappedResponse(request).doOnNext(preCachePlaylistResults).map(withSearchTag(query));
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

    class PlaylistPager extends Pager<ModelCollection<ApiPlaylist>> {

        private final String query;

        PlaylistPager(String query) {
            this.query = query;
        }

        @Override
        public Observable<ModelCollection<ApiPlaylist>> call(ModelCollection<ApiPlaylist> collection) {
            final Optional<Link> nextLink = collection.getNextLink();
            if (nextLink.isPresent()) {
                return getPlaylistResultsNextPage(query, nextLink.get().getHref());
            } else {
                return Pager.finish();
            }
        }
    }
}
