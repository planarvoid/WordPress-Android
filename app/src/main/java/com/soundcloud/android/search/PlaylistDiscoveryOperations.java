package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import rx.Observable;
import rx.Scheduler;
import rx.android.LegacyPager;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class PlaylistDiscoveryOperations {

    private final ApiClientRx apiClientRx;
    private final PlaylistTagStorage tagStorage;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final Scheduler scheduler;

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

    @Inject
    PlaylistDiscoveryOperations(ApiClientRx apiClientRx, PlaylistTagStorage tagStorage,
                                StorePlaylistsCommand storePlaylistsCommand,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.tagStorage = tagStorage;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.scheduler = scheduler;
    }

    Observable<List<String>> recentPlaylistTags() {
        return tagStorage.getRecentTagsAsync();
    }

    Observable<List<String>> popularPlaylistTags() {
        return getCachedPlaylistTags().flatMap(new Func1<List<String>, Observable<List<String>>>() {
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
        ApiRequest request = ApiRequest.get(ApiEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                .forPrivateApi(1)
                .build();
        final TypeToken<ModelCollection<String>> resourceType = new TypeToken<ModelCollection<String>>() {
        };
        return apiClientRx.mappedResponse(request, resourceType)
                .subscribeOn(scheduler)
                .doOnNext(cachePopularTags)
                .map(collectionToList);
    }

    Observable<ApiPlaylistCollection> playlistsForTag(final String tag) {
        final ApiRequest request =
                createPlaylistResultsRequest(ApiEndpoints.PLAYLIST_DISCOVERY.path())
                        .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.CARD_PAGE_SIZE))
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

    private Observable<ApiPlaylistCollection> getPlaylistResultsNextPage(String query, String nextHref) {
        final ApiRequest.Builder builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private ApiRequest.Builder createPlaylistResultsRequest(String url) {
        return ApiRequest.get(url).forPrivateApi(1);
    }

    private Observable<ApiPlaylistCollection> getPlaylistResultsPage(String query, ApiRequest request) {
        return apiClientRx.mappedResponse(request, ApiPlaylistCollection.class)
                .subscribeOn(scheduler)
                .doOnNext(storePlaylistsCommand.toAction())
                .map(withSearchTag(query));
    }

    private Func1<ApiPlaylistCollection, ApiPlaylistCollection> withSearchTag(final String searchTag) {
        return new Func1<ApiPlaylistCollection, ApiPlaylistCollection>() {
            @Override
            public ApiPlaylistCollection call(ApiPlaylistCollection collection) {
                for (ApiPlaylist playlist : collection) {
                    LinkedList<String> tagsWithSearchTag = new LinkedList<>(
                            removeItemIgnoreCase(playlist.getTags(), searchTag));
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

    class PlaylistPager extends LegacyPager<ApiPlaylistCollection> {

        private final String query;

        PlaylistPager(String query) {
            this.query = query;
        }

        @Override
        public Observable<ApiPlaylistCollection> call(ApiPlaylistCollection collection) {
            final Optional<Link> nextLink = collection.getNextLink();
            if (nextLink.isPresent()) {
                return getPlaylistResultsNextPage(query, nextLink.get().getHref());
            } else {
                return LegacyPager.finish();
            }
        }
    }
}
