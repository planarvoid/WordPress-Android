package com.soundcloud.android.search;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Iterables.transform;
import static java.lang.String.format;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.search.SearchOperations.ContentType;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserItemRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SearchStrategyFactory {

    private static final Func1<SearchModelCollection<? extends ListItem>, SearchResult> TO_SEARCH_RESULT =
            searchCollection -> SearchResult.fromSearchableItems(
                    searchCollection.getCollection(),
                    searchCollection.getNextLink(),
                    searchCollection.getQueryUrn(),
                    searchCollection.resultsCount());

    private static final Func1<SearchModelCollection<ListItem>, SearchResult> TO_SEARCH_RESULT_WITH_PREMIUM_CONTENT =
            searchCollection -> {
                final List<ListItem> collection = searchCollection.getCollection();
                final Optional<Link> nextLink = searchCollection.getNextLink();
                final Optional<Urn> queryUrn = searchCollection.getQueryUrn();
                final Optional<? extends SearchModelCollection<ListItem>> premiumContent =
                        searchCollection.premiumContent();
                if (premiumContent.isPresent()) {
                    final SearchModelCollection<ListItem> premiumItems = premiumContent.get();
                    final SearchResult premiumSearchResult = SearchResult.fromSearchableItems(premiumItems.getCollection(),
                                                                                              premiumItems.getNextLink(),
                                                                                              premiumItems.getQueryUrn(),
                                                                                              premiumItems.resultsCount());
                    return SearchResult.fromSearchableItems(collection,
                                                            nextLink,
                                                            queryUrn,
                                                            Optional.of(premiumSearchResult),
                                                            searchCollection.resultsCount());
                }
                return SearchResult.fromSearchableItems(collection, nextLink, queryUrn);
            };

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final LoadFollowingCommand loadFollowingCommand;
    private final EntityItemCreator entityItemCreator;
    private final UserItemRepository userItemRepository;
    private final TrackItemRepository trackItemRepository;

    private final Func1<SearchResult, SearchResult> mergePlaylistLikeStatus = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult input) {
            List<ListItem> result = new ArrayList<>();
            final Map<Urn, Boolean> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(Lists.transform(input.getItems(), Entity::getUrn));
            for (final ListItem resultItem : input) {
                if (resultItem instanceof PlaylistItem) {
                    final Urn itemUrn = resultItem.getUrn();
                    final PlaylistItem playlistItem = (PlaylistItem) resultItem;
                    result.add(playlistItem.updateLikeState(playlistsIsLikedStatus.containsKey(itemUrn) && playlistsIsLikedStatus.get(itemUrn)));
                } else {
                    result.add(resultItem);
                }
            }
            return input.copyWithSearchableItems(result);
        }
    };

    private final Func1<SearchResult, SearchResult> mergeFollowings = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult input) {
            final Map<Urn, Boolean> userIsFollowing = loadFollowingCommand.call(Lists.transform(input.getItems(), Entity::getUrn));
            final List<ListItem> updatedSearchResult = new ArrayList<>(input.getItems().size());
            for (final ListItem resultItem : input) {
                final Urn itemUrn = resultItem.getUrn();
                if (userIsFollowing.containsKey(itemUrn)) {
                    final UserItem updatedUserItem = ((UserItem) resultItem).copyWithFollowing(userIsFollowing.get(itemUrn));
                    updatedSearchResult.add(updatedUserItem);
                } else {
                    updatedSearchResult.add(resultItem);
                }
            }
            return input.copyWithSearchableItems(updatedSearchResult);
        }
    };

    private final Action1<SearchModelCollection<ApiTrack>> cachePremiumTracks =
            new Action1<SearchModelCollection<ApiTrack>>() {
                @Override
                public void call(SearchModelCollection<ApiTrack> apiTrackSearchItems) {
                    if (apiTrackSearchItems.premiumContent().isPresent()) {
                        storeTracksCommand.call(apiTrackSearchItems.premiumContent().get());
                    }
                }
            };

    private final Action1<SearchModelCollection<ApiPlaylist>> cachePremiumPlaylists =
            new Action1<SearchModelCollection<ApiPlaylist>>() {
                @Override
                public void call(SearchModelCollection<ApiPlaylist> apiPlaylistSearchItems) {
                    if (apiPlaylistSearchItems.premiumContent().isPresent()) {
                        storePlaylistsCommand.call(apiPlaylistSearchItems.premiumContent().get());
                    }
                }
            };

    private final Action1<SearchModelCollection<ApiUser>> cachePremiumUsers =
            new Action1<SearchModelCollection<ApiUser>>() {
                @Override
                public void call(SearchModelCollection<ApiUser> apiUserSearchItems) {
                    if (apiUserSearchItems.premiumContent().isPresent()) {
                        storeUsersCommand.call(apiUserSearchItems.premiumContent().get());
                    }
                }
            };

    private final Action1<SearchModelCollection<ApiUniversalSearchItem>> cachePremiumContent =
            new Action1<SearchModelCollection<ApiUniversalSearchItem>>() {
                @Override
                public void call(SearchModelCollection<ApiUniversalSearchItem> apiUniversalSearchItems) {
                    if (apiUniversalSearchItems.premiumContent().isPresent()) {
                        cacheUniversalSearchCommand.call(apiUniversalSearchItems.premiumContent().get());
                    }
                }
            };

    @Inject
    SearchStrategyFactory(ApiClientRx apiClientRx,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          StorePlaylistsCommand storePlaylistsCommand,
                          StoreTracksCommand storeTracksCommand,
                          StoreUsersCommand storeUsersCommand,
                          CacheUniversalSearchCommand cacheUniversalSearchCommand,
                          LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                          LoadFollowingCommand loadFollowingCommand,
                          EntityItemCreator entityItemCreator,
                          UserItemRepository userItemRepository, TrackItemRepository trackItemRepository) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadFollowingCommand = loadFollowingCommand;
        this.entityItemCreator = entityItemCreator;
        this.userItemRepository = userItemRepository;
        this.trackItemRepository = trackItemRepository;
    }

    SearchStrategy getSearchStrategy(SearchType searchType) {
        switch (searchType) {
            case ALL:
                return new UniversalSearchStrategy();
            case TRACKS:
                return new TrackSearchStrategy();
            case PLAYLISTS:
                return new PlaylistSearchStrategy(ApiEndpoints.SEARCH_PLAYLISTS_WITHOUT_ALBUMS,
                                                  ApiEndpoints.SEARCH_PREMIUM_PLAYLISTS);
            case ALBUMS:
                return new PlaylistSearchStrategy(ApiEndpoints.SEARCH_ALBUMS, ApiEndpoints.SEARCH_PREMIUM_ALBUMS);
            case USERS:
                return new UserSearchStrategy();
            default:
                throw new IllegalStateException("Unknown search type");
        }
    }

    abstract class SearchStrategy {

        private final ApiEndpoints endpoint;
        private final ApiEndpoints premiumEndpoint;

        SearchStrategy(ApiEndpoints endpoint, ApiEndpoints premiumEndpoint) {
            this.endpoint = endpoint;
            this.premiumEndpoint = premiumEndpoint;
        }

        Observable<SearchResult> searchResult(String query, Optional<Urn> queryUrn, ContentType contentType) {
            final ApiRequest.Builder requestBuilder = ApiRequest.get(getEndpoint(contentType))
                                                                .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE,
                                                                                       String.valueOf(Consts.LIST_PAGE_SIZE))
                                                                .addQueryParam("q", query);
            if (queryUrn.isPresent()) {
                requestBuilder.addQueryParam("query_urn", queryUrn.get().toString());
            }
            return getSearchResultObservable(requestBuilder.forPrivateApi());
        }

        Observable<SearchResult> nextResultPage(Link nextPageLink) {
            return getSearchResultObservable(ApiRequest.get(nextPageLink.getHref())
                                                       .forPrivateApi());
        }

        protected abstract Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder);

        private String getEndpoint(ContentType contentType) {
            return contentType.equals(ContentType.PREMIUM) ? premiumEndpoint.path() : endpoint.path();
        }
    }

    private final class TrackSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiTrack>> typeToken =
                new TypeToken<SearchModelCollection<ApiTrack>>() {
                };

        protected TrackSearchStrategy() {
            super(ApiEndpoints.SEARCH_TRACKS, ApiEndpoints.SEARCH_PREMIUM_TRACKS);
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                              .subscribeOn(scheduler)
                              .doOnNext(storeTracksCommand.toAction1())
                              .doOnNext(cachePremiumTracks)
                              .map(searchResult -> searchResult.transform(entityItemCreator::trackItem))
                              .map(TO_SEARCH_RESULT);
        }
    }

    private final class PlaylistSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiPlaylist>> typeToken =
                new TypeToken<SearchModelCollection<ApiPlaylist>>() {
                };

        protected PlaylistSearchStrategy(ApiEndpoints endpoint, ApiEndpoints premiumEndpoint) {
            super(endpoint, premiumEndpoint);
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                              .subscribeOn(scheduler)
                              .doOnNext(storePlaylistsCommand.toAction1())
                              .doOnNext(cachePremiumPlaylists)
                              .map(searchResult -> searchResult.transform(entityItemCreator::playlistItem))
                              .map(TO_SEARCH_RESULT)
                              .map(mergePlaylistLikeStatus);
        }
    }

    private final class UserSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiUser>> typeToken =
                new TypeToken<SearchModelCollection<ApiUser>>() {
                };

        protected UserSearchStrategy() {
            super(ApiEndpoints.SEARCH_USERS, ApiEndpoints.SEARCH_PREMIUM_USERS);
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                              .subscribeOn(scheduler)
                              .doOnNext(storeUsersCommand.toAction1())
                              .doOnNext(cachePremiumUsers)
                              .flatMap(SearchStrategyFactory.this::toSearchResult)
                              .map(mergeFollowings);
        }
    }

    private final class UniversalSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiUniversalSearchItem>> typeToken =
                new TypeToken<SearchModelCollection<ApiUniversalSearchItem>>() {
                };

        protected UniversalSearchStrategy() {
            super(ApiEndpoints.SEARCH_ALL, ApiEndpoints.SEARCH_PREMIUM_ALL);
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                              .subscribeOn(scheduler)
                              .doOnNext(cacheUniversalSearchCommand)
                              .doOnNext(cachePremiumContent)
                              .flatMap(searchItems -> backfillUserItems(searchItems).map(userItemsMap -> searchItems.transform(searchItem -> toListItem(searchItem, userItemsMap))))
                              .map(TO_SEARCH_RESULT_WITH_PREMIUM_CONTENT)
                              .map(mergePlaylistLikeStatus)
                              .map(mergeFollowings);
        }

        private ListItem toListItem(ApiUniversalSearchItem searchItem, Map<Urn, UserItem> userItemMap) {
            if (searchItem.track().isPresent()) {
                return entityItemCreator.trackItem(searchItem.track().get());
            } else if (searchItem.playlist().isPresent()) {
                return entityItemCreator.playlistItem(searchItem.playlist().get());
            } else if (searchItem.user().isPresent()) {
                final Urn urn = searchItem.user().get().getUrn();
                if (userItemMap.containsKey(urn)) {
                    return userItemMap.get(urn);
                } else {
                    return entityItemCreator.userItem(searchItem.user().get(), false);
                }
            } else {
                throw new IllegalArgumentException(format("Empty ApiUniversalSearchItem: %s", this));
            }
        }

        private Observable<Map<Urn, UserItem>> backfillUserItems(Iterable<ApiUniversalSearchItem> searchItems) {
            final Iterable<Urn> userUrns = transform(filter(searchItems, searchItem -> searchItem.user().isPresent()), searchItem -> searchItem.user().get().getUrn());
            return RxJava.toV1Observable(userItemRepository.userItemsMap(userUrns));
        }
    }

    private Observable<SearchResult> toSearchResult(SearchModelCollection<ApiUser> searchResult) {
        return RxJava.toV1Observable(userItemRepository.userItems(searchResult))
                     .map(userItems -> SearchResult.fromSearchableItems(userItems, searchResult.getNextLink(), searchResult.getQueryUrn(), searchResult.resultsCount()));
    }
}
