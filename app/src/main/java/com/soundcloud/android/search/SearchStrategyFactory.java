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
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchOperations.ContentType;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
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

    private static final Func1<SearchModelCollection<? extends PropertySetSource>, SearchResult> TO_SEARCH_RESULT =
            new Func1<SearchModelCollection<? extends PropertySetSource>, SearchResult>() {
                @Override
                public SearchResult call(SearchModelCollection<? extends PropertySetSource> searchCollection) {
                    return SearchResult.fromPropertySetSource(
                            searchCollection.getCollection(),
                            searchCollection.getNextLink(),
                            searchCollection.getQueryUrn());
                }
            };

    private static final Func1<SearchModelCollection<? extends PropertySetSource>, SearchResult> TO_SEARCH_RESULT_WITH_PREMIUM_CONTENT =
            new Func1<SearchModelCollection<? extends PropertySetSource>, SearchResult>() {
                @Override
                public SearchResult call(SearchModelCollection<? extends PropertySetSource> searchCollection) {
                    final List<? extends PropertySetSource> collection = searchCollection.getCollection();
                    final Optional<Link> nextLink = searchCollection.getNextLink();
                    final Optional<Urn> queryUrn = searchCollection.getQueryUrn();
                    final Optional<? extends SearchModelCollection<? extends PropertySetSource>> premiumContent =
                            searchCollection.premiumContent();
                    if (premiumContent.isPresent()) {
                        final SearchModelCollection<? extends PropertySetSource> premiumItems = premiumContent.get();
                        final SearchResult premiumSearchResult = SearchResult.fromPropertySetSource(premiumItems.getCollection(),
                                                                                                    premiumItems.getNextLink(),
                                                                                                    premiumItems.getQueryUrn(),
                                                                                                    premiumItems.resultsCount());
                        return SearchResult.fromPropertySetSource(collection,
                                                                  nextLink,
                                                                  queryUrn,
                                                                  Optional.of(premiumSearchResult),
                                                                  searchCollection.resultsCount());
                    }
                    return SearchResult.fromPropertySetSource(collection, nextLink, queryUrn);
                }
            };

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final LoadFollowingCommand loadFollowingCommand;
    private final FeatureFlags featureFlags;

    private final Func1<SearchResult, SearchResult> mergePlaylistLikeStatus = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult input) {
            final Map<Urn, PropertySet> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(input);
            for (final PropertySet resultItem : input) {
                final Urn itemUrn = resultItem.getOrElse(PlaylistProperty.URN, Urn.NOT_SET);
                if (playlistsIsLikedStatus.containsKey(itemUrn)) {
                    resultItem.update(playlistsIsLikedStatus.get(itemUrn));
                }
            }
            return input;
        }
    };

    private final Func1<SearchResult, SearchResult> mergeFollowings = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult input) {
            final Map<Urn, PropertySet> userIsFollowing = loadFollowingCommand.call(input);
            for (final PropertySet resultItem : input) {
                final Urn itemUrn = resultItem.getOrElse(UserProperty.URN, Urn.NOT_SET);
                if (userIsFollowing.containsKey(itemUrn)) {
                    resultItem.update(userIsFollowing.get(itemUrn));
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
                          LoadFollowingCommand loadFollowingCommand, FeatureFlags featureFlags) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadFollowingCommand = loadFollowingCommand;
        this.featureFlags = featureFlags;
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

        protected SearchStrategy(ApiEndpoints endpoint, ApiEndpoints premiumEndpoint) {
            this.endpoint = endpoint;
            this.premiumEndpoint = premiumEndpoint;
        }

        Observable<SearchResult> searchResult(String query, ContentType contentType) {
            return getSearchResultObservable(ApiRequest.get(getEndpoint(contentType))
                                                       .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE,
                                                                              String.valueOf(Consts.LIST_PAGE_SIZE))
                                                       .addQueryParam("q", query)
                                                       .forPrivateApi());
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
                              .map(TO_SEARCH_RESULT_WITH_PREMIUM_CONTENT)
                              .map(mergePlaylistLikeStatus)
                              .map(mergeFollowings);
        }
    }
}
