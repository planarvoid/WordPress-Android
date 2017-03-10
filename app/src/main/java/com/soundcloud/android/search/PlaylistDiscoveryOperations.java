package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.collection.LoadPlaylistRepostStatuses;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.discovery.PlaylistTagsItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicates;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PlaylistDiscoveryOperations {

    private final ApiClientRx apiClientRx;
    private final PlaylistTagStorage tagStorage;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;
    private final EntityItemCreator entityItemCreator;
    private final Scheduler scheduler;

    private static final Func1<DiscoveryItem, Boolean> IS_NOT_EMPTY = playlistDiscoveryItem -> !((PlaylistTagsItem) playlistDiscoveryItem).isEmpty();

    private final Action1<ModelCollection<String>> cachePopularTags = new Action1<ModelCollection<String>>() {
        @Override
        public void call(ModelCollection<String> tags) {
            tagStorage.cachePopularTags(tags.getCollection());
        }
    };

    @Inject
    PlaylistDiscoveryOperations(ApiClientRx apiClientRx,
                                PlaylistTagStorage tagStorage,
                                StorePlaylistsCommand storePlaylistsCommand,
                                LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                                LoadPlaylistRepostStatuses loadPlaylistRepostStatuses,
                                EntityItemCreator entityItemCreator,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.tagStorage = tagStorage;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadPlaylistRepostStatuses = loadPlaylistRepostStatuses;
        this.entityItemCreator = entityItemCreator;
        this.scheduler = scheduler;
    }

    public Observable<DiscoveryItem> playlistTags() {
        return popularPlaylistTags()
                .zipWith(recentPlaylistTags(), (Func2<List<String>, List<String>, DiscoveryItem>) PlaylistTagsItem::create)
                .filter(IS_NOT_EMPTY);
    }

    public Observable<List<String>> recentPlaylistTags() {
        return tagStorage.getRecentTagsAsync();
    }

    public Observable<List<String>> popularPlaylistTags() {
        return getCachedPlaylistTags().flatMap(new Func1<List<String>, Observable<List<String>>>() {
            @Override
            public Observable<List<String>> call(List<String> tags) {
                if (tags.isEmpty() || tagStorage.isTagsCacheExpired()) {
                    return fetchAndCachePopularTags();
                }
                return Observable.just(tags);
            }
        });
    }

    public void clearData() {
        tagStorage.clear();
    }

    private Observable<List<String>> getCachedPlaylistTags() {
        return tagStorage.getPopularTagsAsync();
    }

    private Observable<List<String>> fetchAndCachePopularTags() {
        ApiRequest request = ApiRequest.get(ApiEndpoints.PLAYLIST_DISCOVERY_TAGS.path())
                                       .forPrivateApi()
                                       .build();
        final TypeToken<ModelCollection<String>> resourceType = new TypeToken<ModelCollection<String>>() {
        };
        return apiClientRx.mappedResponse(request, resourceType)
                          .subscribeOn(scheduler)
                          .doOnNext(cachePopularTags)
                          .map(ModelCollection::getCollection);
    }

    Observable<SearchResult> playlistsForTag(final String tag) {
        final ApiRequest request =
                createPlaylistResultsRequest(ApiEndpoints.PLAYLIST_DISCOVERY.path())
                        .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.CARD_PAGE_SIZE))
                        .addQueryParam("tag", tag)
                        .build();
        return getPlaylistResultsPage(tag, request).finallyDo(() -> tagStorage.addRecentTag(tag));
    }

    Pager.PagingFunction<SearchResult> pager(final String searchTag) {
        return searchResult -> {
            final Optional<Link> nextLink = searchResult.nextHref;
            if (nextLink.isPresent()) {
                return getPlaylistResultsNextPage(searchTag, nextLink.get().getHref());
            } else {
                return Pager.finish();
            }
        };
    }

    private Observable<SearchResult> getPlaylistResultsNextPage(String query, String nextHref) {
        final ApiRequest.Builder builder = createPlaylistResultsRequest(nextHref);
        return getPlaylistResultsPage(query, builder.build());
    }

    private ApiRequest.Builder createPlaylistResultsRequest(String url) {
        return ApiRequest.get(url).forPrivateApi();
    }

    private Observable<SearchResult> getPlaylistResultsPage(String query, ApiRequest request) {
        return apiClientRx.mappedResponse(request, ApiPlaylistCollection.class)
                          .subscribeOn(scheduler)
                          .doOnNext(storePlaylistsCommand.toAction1())
                          .map(withSearchTag(query))
                          .map(collection -> {
                              final SearchResult result = SearchResult.fromSearchableItems(collection.transform(entityItemCreator::playlistItem).getCollection(),
                                                                                           collection.getNextLink(),
                                                                                           collection.getQueryUrn());
                              return backfillSearchResult(result);
                          });
    }

    private Func1<ApiPlaylistCollection, ApiPlaylistCollection> withSearchTag(final String searchTag) {
        return collection -> {
            for (ApiPlaylist playlist : collection) {
                LinkedList<String> tagsWithSearchTag = new LinkedList<>(
                        removeItemIgnoreCase(playlist.getTags(), searchTag));
                tagsWithSearchTag.addFirst(searchTag);
                playlist.setTags(tagsWithSearchTag);
            }
            return collection;
        };
    }

    private Collection<String> removeItemIgnoreCase(List<String> list, String itemToRemove) {
        return MoreCollections.filter(list, Predicates.containsPattern("(?i)^(?!" + itemToRemove + "$).*$"));
    }

    private SearchResult backfillSearchResult(SearchResult result) {
        final List<ListItem> updatedList = new ArrayList<>();
        final List<Urn> urns = Lists.transform(result.getItems(), ListItem::getUrn);
        final Map<Urn, Boolean> playlistRepostStatus = loadPlaylistRepostStatuses.call(urns);
        final Map<Urn, Boolean> playlistLikedStatus = loadPlaylistLikedStatuses.call(urns);

        for (final ListItem resultItem : result) {
            final Urn itemUrn = resultItem.getUrn();
            boolean isRepostedByCurrentUser = playlistRepostStatus.containsKey(itemUrn) && playlistRepostStatus.get(itemUrn);
            boolean isLikedByCurrentUser = playlistLikedStatus.containsKey(itemUrn) && playlistLikedStatus.get(itemUrn);

            updatedList.add(((PlaylistItem) resultItem).updatedWithLikeAndRepostStatus(isLikedByCurrentUser, isRepostedByCurrentUser));
        }
        return result.copyWithSearchableItems(updatedList);
    }

}
