package com.soundcloud.android.search;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

@SuppressFBWarnings(
        value = {"SE_BAD_FIELD"},
        justification = "we never serialize search operations")
class SearchOperations {

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    static final Func1<ModelCollection<? extends PropertySetSource>, SearchResult> TO_SEARCH_RESULT =
            new Func1<ModelCollection<? extends PropertySetSource>, SearchResult>() {
        @Override
        public SearchResult call(ModelCollection<? extends PropertySetSource> propertySetSources) {
            return new SearchResult(propertySetSources.getCollection(), propertySetSources.getNextLink());
        }
    };

    private final ApiScheduler apiScheduler;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final CacheUniversalSearchCommand cacheUniversalSearchCommand;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;

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
    public SearchOperations(ApiScheduler apiScheduler, StoreTracksCommand storeTracksCommand,
                            StorePlaylistsCommand storePlaylistsCommand, StoreUsersCommand storeUsersCommand,
                            CacheUniversalSearchCommand cacheUniversalSearchCommand,
                            LoadPlaylistLikedStatuses loadPlaylistLikedStatuses) {
        this.apiScheduler = apiScheduler;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.cacheUniversalSearchCommand = cacheUniversalSearchCommand;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
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

    private SearchStrategy<?> getSearchStrategy(int searchType) {
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

    class SearchResultPager extends Pager<SearchResult> {
        private final int searchType;

        SearchResultPager(int searchType) {
            this.searchType = searchType;
        }

        @Override
        public Observable<SearchResult> call(SearchResult searchResultsCollection) {
            final Optional<Link> nextHref = searchResultsCollection.nextHref;
            if (nextHref.isPresent()) {
                return nextResultPage(nextHref.get(), searchType);
            } else {
                return Pager.finish();
            }
        }
    }

    private abstract class SearchStrategy<ResultT> {

        private final TypeToken<ResultT> typeToken;
        private final String apiEndpoint;

        protected SearchStrategy(TypeToken<ResultT> typeToken, String apiEndpoint) {
            this.typeToken = typeToken;
            this.apiEndpoint = apiEndpoint;
        }

        private Observable<SearchResult> searchResult(String query) {
            return getSearchResultObservable(ApiRequest.Builder.<ResultT>get(apiEndpoint)
                    .addQueryParam(ApiRequest.Param.PAGE_SIZE, String.valueOf(Consts.LIST_PAGE_SIZE))
                    .addQueryParam("q", query)
                    .forPrivateApi(1)
                    .forResource(typeToken));
        }

        private Observable<SearchResult> nextResultPage(Link nextPageLink) {
            return getSearchResultObservable(ApiRequest.Builder.<ResultT>get(nextPageLink.getHref())
                    .forPrivateApi(1)
                    .forResource(typeToken));
        }

        protected abstract Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder<ResultT> builder);

    }

    private final class TrackSearchStrategy extends SearchStrategy<ModelCollection<ApiTrack>> {

        protected TrackSearchStrategy() {
            super(new TypeToken<ModelCollection<ApiTrack>>() {
            }, ApiEndpoints.SEARCH_TRACKS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder<ModelCollection<ApiTrack>> builder) {
            return apiScheduler.mappedResponse(builder.build())
                    .doOnNext(storeTracksCommand.toAction())
                    .map(TO_SEARCH_RESULT);
        }
    }

    private final class PlaylistSearchStrategy extends SearchStrategy<ModelCollection<ApiPlaylist>> {

        protected PlaylistSearchStrategy() {
            super(new TypeToken<ModelCollection<ApiPlaylist>>() {
            }, ApiEndpoints.SEARCH_PLAYLISTS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder<ModelCollection<ApiPlaylist>> builder) {
            return apiScheduler.mappedResponse(builder.build())
                    .doOnNext(storePlaylistsCommand.toAction())
                    .map(TO_SEARCH_RESULT)
                    .map(mergeLikeStatusForPlaylists);
        }
    }

    private final class UserSearchStrategy extends SearchStrategy<ModelCollection<ApiUser>> {

        protected UserSearchStrategy() {
            super(new TypeToken<ModelCollection<ApiUser>>() {
            }, ApiEndpoints.SEARCH_USERS.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder<ModelCollection<ApiUser>> builder) {
            return apiScheduler.mappedResponse(builder.build())
                    .doOnNext(storeUsersCommand.toAction())
                    .map(TO_SEARCH_RESULT);
        }
    }

    private final class UniversalSearchStrategy extends SearchStrategy<ModelCollection<ApiUniversalSearchItem>> {

        protected UniversalSearchStrategy() {
            super(new TypeToken<ModelCollection<ApiUniversalSearchItem>>() {
            }, ApiEndpoints.SEARCH_ALL.path());
        }

        @Override
        protected Observable<SearchResult> getSearchResultObservable(ApiRequest.Builder<ModelCollection<ApiUniversalSearchItem>> builder) {
            return apiScheduler.mappedResponse(builder.build())
                    .doOnNext(cacheUniversalSearchCommand.toAction())
                    .map(TO_SEARCH_RESULT)
                    .map(mergeLikeStatusForPlaylists);
        }
    }
}
