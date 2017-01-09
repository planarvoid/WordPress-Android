package com.soundcloud.android.search;

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
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.search.SearchOperations.ContentType;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

class SearchStrategyFactory {

    private static final Func1<SearchModelCollection<? extends SearchableItem>, SearchResult> TO_SEARCH_RESULT =
            searchCollection -> SearchResult.fromSearchableItems(
                    searchCollection.getCollection(),
                    searchCollection.getNextLink(),
                    searchCollection.getQueryUrn());

    private static final Func1<SearchModelCollection<SearchableItem>, SearchResult> TO_SEARCH_RESULT_WITH_PREMIUM_CONTENT =
            searchCollection -> {
                final List<SearchableItem> collection = searchCollection.getCollection();
                final Optional<Link> nextLink = searchCollection.getNextLink();
                final Optional<Urn> queryUrn = searchCollection.getQueryUrn();
                final Optional<? extends SearchModelCollection<SearchableItem>> premiumContent =
                        searchCollection.premiumContent();
                if (premiumContent.isPresent()) {
                    final SearchModelCollection<SearchableItem> premiumItems = premiumContent.get();
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

    private final Func1<SearchResult, SearchResult> mergePlaylistLikeStatus = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult input) {
            final Map<Urn, Boolean> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(Lists.transform(input.getItems(), SearchableItem::getUrn));
            for (final SearchableItem resultItem : input) {
                final Urn itemUrn = resultItem.getUrn();
                if (playlistsIsLikedStatus.containsKey(itemUrn)) {
                    ((PlaylistItem) resultItem).setLikedByCurrentUser(playlistsIsLikedStatus.get(itemUrn));
                }
            }
            return input;
        }
    };

    private final Func1<SearchResult, SearchResult> mergeFollowings = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult input) {
            final Map<Urn, Boolean> userIsFollowing = loadFollowingCommand.call(Lists.transform(input.getItems(), SearchableItem::getUrn));
            for (final SearchableItem resultItem : input) {
                final Urn itemUrn = resultItem.getUrn();
                if (userIsFollowing.containsKey(itemUrn)) {
                    ((UserItem) resultItem).setFollowing(userIsFollowing.get(itemUrn));
                }
            }
            return input;
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
                        cacheUniversalSearchCommand.with(apiUniversalSearchItems.premiumContent().get()).call();
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
                          LoadFollowingCommand loadFollowingCommand) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadFollowingCommand = loadFollowingCommand;
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
                              .map(searchResult -> searchResult.transform(TrackItem::from))
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
                              .map(searchResult -> searchResult.transform(PlaylistItem::from))
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
                              .map(searchResult -> searchResult.transform(UserItem::from))
                              .map(TO_SEARCH_RESULT)
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
                              .doOnNext(cacheUniversalSearchCommand.toAction())
                              .doOnNext(cachePremiumContent)
                              .map(item -> item.transform(ApiUniversalSearchItem::toSearchableItem))
                              .map(TO_SEARCH_RESULT_WITH_PREMIUM_CONTENT)
                              .map(mergePlaylistLikeStatus)
                              .map(mergeFollowings);
        }
    }
}
