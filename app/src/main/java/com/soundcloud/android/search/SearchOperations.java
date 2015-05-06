package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
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
import com.soundcloud.android.api.model.SearchCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rx.Observable;
import rx.Scheduler;
import rx.android.LegacyPager;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(
        value = {"SE_BAD_FIELD"},
        justification = "we never serialize search operations")
class SearchOperations {

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    static final Func1<SearchCollection<? extends PropertySetSource>, SearchResult> TO_SEARCH_RESULT =
            new Func1<SearchCollection<? extends PropertySetSource>, SearchResult>() {
        @Override
        public SearchResult call(SearchCollection<? extends PropertySetSource> propertySetSources) {
            return new SearchResult(propertySetSources.getCollection(), propertySetSources.getNextLink(), propertySetSources.getQueryUrn());
        }
    };

    private final ApiClientRx apiClientRx;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final Scheduler scheduler;

    private final Func1<SearchResult, SearchResult> mergeLikeStatusForPlaylists = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult searchResult) {
            final List<PropertySet> playlistsIsLikedStatus;
            try {
                playlistsIsLikedStatus = loadPlaylistLikedStatuses.with(searchResult.getItems()).call();

                for (final PropertySet resultItem : searchResult) {
                    final Urn itemUrn = resultItem.getOrElse(PlaylistProperty.URN, Urn.NOT_SET);
                    final Optional<PropertySet> matchingPlaylistLikeStatus =
                            Iterables.tryFind(playlistsIsLikedStatus, matchingUrnPredicate(itemUrn));
                    if (itemUrn.isPlaylist() && matchingPlaylistLikeStatus.isPresent()) {
                        resultItem.update(matchingPlaylistLikeStatus.get());
                    }
                }
            } catch (Exception e) {
                ErrorUtils.handleSilentException(e);
            }
            return searchResult;
        }

        private Predicate<PropertySet> matchingUrnPredicate(final Urn itemUrn) {
            return new Predicate<PropertySet>() {
                @Override
                public boolean apply(PropertySet input) {
                    return input.get(PlaylistProperty.URN).equals(itemUrn);
                }
            };
        }
    };

    @Inject
    public SearchOperations(ApiClientRx apiClientRx, StoreTracksCommand storeTracksCommand,
                            StorePlaylistsCommand storePlaylistsCommand, StoreUsersCommand storeUsersCommand,
                            CacheUniversalSearchCommand cacheUniversalSearchCommand,
                            LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.scheduler = scheduler;
    }

    SearchResultPager pager(int searchType) {
        return new SearchResultPager(searchType);
    }

    Observable<SearchResult> searchResult(String query, int searchType) {
        return getSearchStrategy(searchType).searchResult(query);
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

    class SearchResultPager extends LegacyPager<SearchResult> {
        private final int searchType;
        private final List<Urn> allUrns = new ArrayList<>();
        private Urn queryUrn = Urn.NOT_SET;

        SearchResultPager(int searchType) {
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

            allUrns.addAll(CollectionUtils.extractUrnsFromEntities(searchResultsCollection.getItems()));

            if(searchResultsCollection.queryUrn.isPresent()) {
                queryUrn = searchResultsCollection.queryUrn.get();
            }

            if (nextHref.isPresent()) {
                return nextResultPage(nextHref.get(), searchType);
            } else {
                return LegacyPager.finish();
            }
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

        private final TypeToken<SearchCollection<ApiTrack>> typeToken = new TypeToken<SearchCollection<ApiTrack>>() {
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

        private final TypeToken<SearchCollection<ApiPlaylist>> typeToken = new TypeToken<SearchCollection<ApiPlaylist>>() {
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
                    .map(mergeLikeStatusForPlaylists);
        }
    }

    private final class UserSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchCollection<ApiUser>> typeToken = new TypeToken<SearchCollection<ApiUser>>() {
        };

        protected UserSearchStrategy() {
            super(ApiEndpoints.SEARCH_USERS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder builder) {
            return apiClientRx.mappedResponse(builder.build(), typeToken)
                    .subscribeOn(scheduler)
                    .doOnNext(storeUsersCommand.toAction())
                    .map(TO_SEARCH_RESULT);
        }
    }

    private final class UniversalSearchStrategy extends SearchStrategy {

        private final TypeToken<SearchCollection<ApiUniversalSearchItem>> typeToken = new TypeToken<SearchCollection<ApiUniversalSearchItem>>() {
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
                    .map(mergeLikeStatusForPlaylists);
        }
    }
}
