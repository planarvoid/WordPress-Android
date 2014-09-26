package com.soundcloud.android.search;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

// we never serialize search operations
@SuppressFBWarnings("SE_BAD_FIELD_INNER_CLASS")
class SearchOperations {

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    static final Func1<ModelCollection<PropertySetSource>, List<PropertySet>> TO_PROPERTY_SET = new Func1<ModelCollection<PropertySetSource>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(ModelCollection<PropertySetSource> searchResults) {
            List<PropertySet> propertyResults = new ArrayList<>();
            for (PropertySetSource result : searchResults) {
                propertyResults.add(result.toPropertySet());
            }
            return propertyResults;
        }
    };

    private final RxHttpClient rxHttpClient;

    @Inject
    public SearchOperations(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    SearchResultPager pager(int searchType) {
        return new SearchResultPager(searchType);
    }

    public Observable<ModelCollection<PropertySetSource>> getSearchResult(String query, int searchType) {
        return getSearchResult(query, searchType, true);
    }

    private Observable<ModelCollection<PropertySetSource>> getSearchResult(String query, int searchType, boolean nextPage) {
        switch (searchType) {
            case TYPE_ALL:
                return getAllSearchResults(query, nextPage);
            case TYPE_TRACKS:
                return getTrackSearchResults(query, nextPage);
            case TYPE_PLAYLISTS:
                 return getPlaylistSearchResults(query, nextPage);
            case TYPE_USERS:
                return getUserSearchResults(query, nextPage);
            default:
                throw new IllegalStateException("Unknown search type");
        }
    }

    private Observable<ModelCollection<PropertySetSource>> getAllSearchResults(String query, boolean firstPage) {
        final APIEndpoints endpoint = APIEndpoints.SEARCH_ALL;
        final TypeToken<ModelCollection<UniversalSearchResult>> typeToken = new TypeToken<ModelCollection<UniversalSearchResult>>() {
        };

        return getSearchResults(query, firstPage, endpoint, typeToken);
    }

    private Observable<ModelCollection<PropertySetSource>> getTrackSearchResults(String query, boolean firstPage) {
        final APIEndpoints endpoint = APIEndpoints.SEARCH_TRACKS;
        final TypeToken<ModelCollection<ApiTrack>> typeToken = new TypeToken<ModelCollection<ApiTrack>>() {
        };
        return getSearchResults(query, firstPage, endpoint, typeToken);
    }

    private Observable<ModelCollection<PropertySetSource>> getPlaylistSearchResults(String query, boolean firstPage) {
        final APIEndpoints endpoint = APIEndpoints.SEARCH_PLAYLISTS;
        final TypeToken<ModelCollection<ApiPlaylist>> typeToken = new TypeToken<ModelCollection<ApiPlaylist>>() {
        };
        return getSearchResults(query, firstPage, endpoint, typeToken);
    }

    private Observable<ModelCollection<PropertySetSource>> getUserSearchResults(String query, boolean firstPage) {
        final APIEndpoints endpoint = APIEndpoints.SEARCH_USERS;
        final TypeToken<ModelCollection<ApiUser>> typeToken = new TypeToken<ModelCollection<ApiUser>>() {
        };
        return getSearchResults(query, firstPage, endpoint, typeToken);
    }

    private <T extends PropertySetSource>
    Observable<ModelCollection<PropertySetSource>> getSearchResults(APIEndpoints apiEndpoint, @Nullable String query, TypeToken<ModelCollection<T>> typeToken) {
        final RequestBuilder<ModelCollection<T>> builder = createSearchRequestBuilder(apiEndpoint.path(), typeToken);
        return getPageObservable(builder.addQueryParameters("q", query).build());
    }

    private <T extends PropertySetSource>
    Observable<ModelCollection<PropertySetSource>> getSearchResults(String nextHref, TypeToken<ModelCollection<T>> typeToken) {
        final RequestBuilder<ModelCollection<T>> builder = createSearchRequestBuilder(nextHref, typeToken);
        return getPageObservable(builder.build());
    }

    private <T extends PropertySetSource>
    Observable<ModelCollection<PropertySetSource>> getSearchResults(String query, boolean firstPage, APIEndpoints endpoint, TypeToken<ModelCollection<T>> typeToken) {
        if (firstPage) {
            return getSearchResults(endpoint, query, typeToken);
        }
        return getSearchResults(query, typeToken);
    }

    private <T extends PropertySetSource>
    RequestBuilder<ModelCollection<T>> createSearchRequestBuilder(String path, TypeToken<ModelCollection<T>> typeToken) {
        return RequestBuilder.<ModelCollection<T>>get(path)
                .addQueryParameters("limit", String.valueOf(Consts.LIST_PAGE_SIZE))
                .forPrivateAPI(1)
                .forResource(typeToken);
    }

    private <T extends PropertySetSource>
    Observable<ModelCollection<PropertySetSource>> getPageObservable(APIRequest<ModelCollection<T>> request) {
        return rxHttpClient.fetchModels(request);
        //.map(cacheResources)
//                .doOnNext(new Action1<ModelCollection<T>>() {
//                    @Override
//                    public void call(ModelCollection<T> collection) {
//                        fireAndForget(bulkStorage.bulkInsertAsync(collection));
//                    }
//                });
    }

    class SearchResultPager extends Pager<ModelCollection<PropertySetSource>> {

        private final int searchType;

        SearchResultPager(int searchType) {
            this.searchType = searchType;
        }

        @Override
        public Observable<ModelCollection<PropertySetSource>> call(ModelCollection<PropertySetSource> searchResultsCollection) {
            final Optional<Link> nextHref = searchResultsCollection.getNextLink();
            if (nextHref.isPresent()) {
                return getSearchResult(nextHref.get().getHref(), searchType, false);
            } else {
                return Pager.finish();
            }
        }
    }

}
