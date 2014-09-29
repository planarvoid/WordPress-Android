package com.soundcloud.android.search;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.users.UserWriteStorage;
import com.soundcloud.propeller.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(value = "SE_BAD_FIELD_INNER_CLASS", justification = "we never serialize search operations")
class SearchOperations {

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    static final Func1<SearchResult, List<PropertySet>> TO_PROPERTY_SET = new Func1<SearchResult, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(SearchResult searchResults) {
            List<PropertySet> propertyResults = new ArrayList<>();
            for (PropertySetSource result : searchResults.items) {
                propertyResults.add(result.toPropertySet());
            }
            return propertyResults;
        }
    };

    private static final Func1<ModelCollection<? extends PropertySetSource>, SearchResult> TO_SEARCH_RESULT = new Func1<ModelCollection<? extends PropertySetSource>, SearchResult>() {
        @Override
        public SearchResult call(ModelCollection<? extends PropertySetSource> propertySetSources) {
            return new SearchResult(propertySetSources.getCollection(), propertySetSources.getNextLink());
        }
    };

    private final Action1<ModelCollection<ApiTrack>> cacheTracks = new Action1<ModelCollection<ApiTrack>>() {
        @Override
        public void call(ModelCollection<ApiTrack> trackList) {
            fireAndForget(trackStorage.storeTracksAsync(trackList.getCollection()));
        }
    };

    private final Action1<ModelCollection<ApiPlaylist>> cachePlaylists = new Action1<ModelCollection<ApiPlaylist>>() {
        @Override
        public void call(ModelCollection<ApiPlaylist> playlistsResult) {
            fireAndForget(playlistStorage.storePlaylistsAsync(playlistsResult.getCollection()));
        }
    };

    private final Action1<ModelCollection<ApiUser>> cacheUsers = new Action1<ModelCollection<ApiUser>>() {
        @Override
        public void call(ModelCollection<ApiUser> userResult) {
            fireAndForget(userStorage.storeUsersAsync(userResult.getCollection()));
        }
    };

    private final Action1<ModelCollection<UniversalSearchResult>> cacheUniversal = new Action1<ModelCollection<UniversalSearchResult>>() {
        @Override
        public void call(ModelCollection<UniversalSearchResult> universalSearchResults) {
            List<ApiUser> users = new ArrayList<>();
            List<ApiPlaylist> playlists = new ArrayList<>();
            List<ApiTrack> tracks = new ArrayList<>();

            for (UniversalSearchResult result : universalSearchResults) {
                if (result.isUser()) {
                    users.add(result.getUser());
                } else if (result.isPlaylist()) {
                    playlists.add(result.getPlaylist());
                } else if (result.isTrack()) {
                    tracks.add(result.getTrack());
                }
            }

            if (!users.isEmpty()) {
                fireAndForget(userStorage.storeUsersAsync(users));
            }
            if (!playlists.isEmpty()) {
                fireAndForget(playlistStorage.storePlaylistsAsync(playlists));
            }
            if (!tracks.isEmpty()) {
                fireAndForget(trackStorage.storeTracksAsync(tracks));
            }
        }
    };

    private final RxHttpClient rxHttpClient;
    private final UserWriteStorage userStorage;
    private final PlaylistWriteStorage playlistStorage;
    private final TrackWriteStorage trackStorage;


    @Inject
    public SearchOperations(RxHttpClient rxHttpClient, UserWriteStorage userStorage, PlaylistWriteStorage playlistStorage, TrackWriteStorage trackStorage) {
        this.rxHttpClient = rxHttpClient;
        this.userStorage = userStorage;
        this.playlistStorage = playlistStorage;
        this.trackStorage = trackStorage;
    }

    SearchResultPager pager(int searchType) {
        return new SearchResultPager(searchType);
    }

    Observable<SearchResult> getSearchResult(String query, int searchType) {
        final SearchConfig config = configForType(searchType);
        final RequestBuilder<?> builder = createSearchRequestBuilder(config);
        builder.addQueryParameters("q", query);

        return getSearchResultObservable(searchType, builder);
    }

    private Observable<SearchResult> getSearchResult(Link link, int searchType) {
        final SearchConfig config = configForType(searchType);
        config.path = link.getHref();
        final RequestBuilder<?> builder = createSearchRequestBuilder(config);

        return getSearchResultObservable(searchType, builder);
    }

    private Observable<SearchResult> getSearchResultObservable(int searchType, RequestBuilder<?> builder) {
        switch (searchType) {
            case TYPE_ALL:
                return rxHttpClient.<ModelCollection<UniversalSearchResult>>fetchModels(builder.build()).doOnNext(cacheUniversal).map(TO_SEARCH_RESULT);
            case TYPE_TRACKS:
                return rxHttpClient.<ModelCollection<ApiTrack>>fetchModels(builder.build()).doOnNext(cacheTracks).map(TO_SEARCH_RESULT);
            case TYPE_PLAYLISTS:
                return rxHttpClient.<ModelCollection<ApiPlaylist>>fetchModels(builder.build()).doOnNext(cachePlaylists).map(TO_SEARCH_RESULT);
            case TYPE_USERS:
                return rxHttpClient.<ModelCollection<ApiUser>>fetchModels(builder.build()).doOnNext(cacheUsers).map(TO_SEARCH_RESULT);
            default:
                throw new IllegalStateException("Unknown search type");
        }
    }

    private SearchConfig configForType(int searchType) {
        switch (searchType) {
            case TYPE_ALL:
                return new SearchConfig(APIEndpoints.SEARCH_ALL.path(), new TypeToken<ModelCollection<UniversalSearchResult>>() {
                });
            case TYPE_TRACKS:
                return new SearchConfig(APIEndpoints.SEARCH_TRACKS.path(), new TypeToken<ModelCollection<ApiTrack>>() {
                });
            case TYPE_PLAYLISTS:
                return new SearchConfig(APIEndpoints.SEARCH_PLAYLISTS.path(), new TypeToken<ModelCollection<ApiPlaylist>>() {
                });
            case TYPE_USERS:
                return new SearchConfig(APIEndpoints.SEARCH_USERS.path(), new TypeToken<ModelCollection<ApiUser>>() {
                });
            default:
                throw new IllegalStateException("Unknown search type");
        }
    }

    private RequestBuilder createSearchRequestBuilder(SearchConfig config) {
        return RequestBuilder.get(config.path)
                .addQueryParameters("limit", String.valueOf(Consts.LIST_PAGE_SIZE))
                .forPrivateAPI(1)
                .forResource(config.typeToken);
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
                return getSearchResult(nextHref.get(), searchType);
            } else {
                return Pager.finish();
            }
        }
    }

    static class SearchConfig {

        private final TypeToken typeToken;
        private String path;

        public SearchConfig(String path, TypeToken typeToken) {
            this.path = path;
            this.typeToken = typeToken;
        }
    }
}
