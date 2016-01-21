package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings(
        value = {"SE_BAD_FIELD"},
        justification = "we never serialize search operations")
class SearchOperations {

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    static final Func1<SearchModelCollection<? extends PropertySetSource>, SearchResult> TO_SEARCH_RESULT =
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
                        final SearchResult premiumSearchResult = new SearchResult(premiumItems.getCollection(),
                                premiumItems.getNextLink(), premiumItems.getQueryUrn(), premiumItems.resultsCount());
                        return new SearchResult(collection, nextLink, queryUrn, Optional.of(premiumSearchResult),
                                searchCollection.resultsCount());
                    }
                    return new SearchResult(collection, nextLink, queryUrn);
                }
            };

    private final ApiClientRx apiClientRx;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final LoadFollowingCommand loadFollowingCommand;
    private final Scheduler scheduler;

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

    @Inject
    SearchOperations(ApiClientRx apiClientRx, StoreTracksCommand storeTracksCommand,
                     StorePlaylistsCommand storePlaylistsCommand, StoreUsersCommand storeUsersCommand,
                     CacheUniversalSearchCommand cacheUniversalSearchCommand,
                     LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                     LoadFollowingCommand loadFollowingCommand,
                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.loadFollowingCommand = loadFollowingCommand;
        this.scheduler = scheduler;
    }

    Observable<SearchResult> searchResult(String query, int searchType) {
        return getSearchStrategy(searchType).searchResult(query);
    }

    SearchPagingFunction pagingFunction(final int searchType) {
        return new SearchPagingFunction(searchType);
    }

    private Observable<SearchResult> nextResultPage(Link link, int searchType) {
        return getSearchStrategy(searchType).nextResultPage(link);
    }

    private SearchStrategy getSearchStrategy(int searchType) {
        switch (searchType) {
            case TYPE_ALL:
                return new UniversalSearchStrategy();
            case TYPE_TRACKS:
                return new TrackSearchStrategy();
            case TYPE_PLAYLISTS:
                return new PlaylistSearchStrategy();
            case TYPE_USERS:
                return new UserSearchStrategy();
            default:
                throw new IllegalStateException("Unknown search type");
        }
    }

    private abstract class SearchStrategy {

        private final String apiEndpoint;

        protected SearchStrategy(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }

        private Observable<SearchResult> searchResult(String query) {
            return getSearchResultObservable(ApiRequest.get(apiEndpoint)
                    .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.LIST_PAGE_SIZE))
                    .addQueryParam("q", query)
                    .forPrivateApi(1));
        }

        private Observable<SearchResult> nextResultPage(Link nextPageLink) {
            return getSearchResultObservable(ApiRequest.get(nextPageLink.getHref())
                    .forPrivateApi(1));
        }

        protected abstract Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder);

    }

    private final class TrackSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiTrack>> typeToken = new TypeToken<SearchModelCollection<ApiTrack>>() {
        };

        protected TrackSearchStrategy() {
            super(ApiEndpoints.SEARCH_TRACKS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                    .subscribeOn(scheduler)
                    .doOnNext(storeTracksCommand.toAction())
                    .map(TO_SEARCH_RESULT);
        }
    }

    private final class PlaylistSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiPlaylist>> typeToken =
                new TypeToken<SearchModelCollection<ApiPlaylist>>() {
                };

        protected PlaylistSearchStrategy() {
            super(ApiEndpoints.SEARCH_PLAYLISTS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                    .subscribeOn(scheduler)
                    .doOnNext(storePlaylistsCommand.toAction())
                    .map(TO_SEARCH_RESULT)
                    .map(mergePlaylistLikeStatus);
        }
    }

    private final class UserSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiUser>> typeToken = new TypeToken<SearchModelCollection<ApiUser>>() {
        };

        protected UserSearchStrategy() {
            super(ApiEndpoints.SEARCH_USERS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                    .subscribeOn(scheduler)
                    .doOnNext(storeUsersCommand.toAction())
                    .map(TO_SEARCH_RESULT)
                    .map(mergeFollowings);
        }
    }

    private final class UniversalSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchModelCollection<ApiUniversalSearchItem>> typeToken =
                new TypeToken<SearchModelCollection<ApiUniversalSearchItem>>() {
                };

        protected UniversalSearchStrategy() {
            super(ApiEndpoints.SEARCH_ALL.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                    .subscribeOn(scheduler)
                    .doOnNext(cacheUniversalSearchCommand.toAction())
                    .map(TO_SEARCH_RESULT)
                    .map(mergePlaylistLikeStatus)
                    .map(mergeFollowings);
        }
    }

    class SearchPagingFunction implements PagingFunction<SearchResult> {

        private final int searchType;
        private final List<Urn> allUrns = new ArrayList<>();
        private Urn queryUrn = Urn.NOT_SET;

        public SearchPagingFunction(int searchType) {
            this.searchType = searchType;
        }

        public SearchQuerySourceInfo getSearchQuerySourceInfo() {
            return new SearchQuerySourceInfo(queryUrn);
        }

        public SearchQuerySourceInfo getSearchQuerySourceInfo(int clickPosition, Urn clickUrn) {
            SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn, clickPosition, clickUrn);
            searchQuerySourceInfo.setQueryResults(allUrns);
            return searchQuerySourceInfo;
        }

        @Override
        public Observable<SearchResult> call(SearchResult searchResultsCollection) {
            final Optional<Link> nextHref = searchResultsCollection.nextHref;

            allUrns.addAll(PropertySets.extractUrns(searchResultsCollection.getItems()));

            if (searchResultsCollection.queryUrn.isPresent()) {
                queryUrn = searchResultsCollection.queryUrn.get();
            }

            if (nextHref.isPresent()) {
                return nextResultPage(nextHref.get(), searchType);
            } else {
                return Pager.finish();
            }
        }
    }
}
