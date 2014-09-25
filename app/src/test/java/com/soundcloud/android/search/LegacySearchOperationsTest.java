package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SearchResultsCollection;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.android.OperatorPaged;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class LegacySearchOperationsTest {

    @InjectMocks private LegacySearchOperations searchOperations;

    @Mock
    private RxHttpClient rxHttpClient;
    @Mock
    private BulkStorage bulkStorage;
    @Mock
    private ScModelManager modelManager;

    @Mock
    private Observer observer;

    @Before
    public void setUp() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        when(bulkStorage.bulkInsertAsync(any(Iterable.class))).thenReturn(Observable.<Iterable>empty());
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() throws Exception {
        searchOperations.getAllSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(
                isPublicApiRequestTo("GET", APIEndpoints.LEGACY_SEARCH_ALL.path())
                        .withQueryParam("q", "any query")
                        .withQueryParam("limit", "30")));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() throws Exception {
        searchOperations.getTrackSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(
                isPublicApiRequestTo("GET", APIEndpoints.LEGACY_SEARCH_TRACKS.path())
                        .withQueryParam("q", "any query")
                        .withQueryParam("limit", "30")));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() throws Exception {
        searchOperations.getPlaylistSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(
                isPublicApiRequestTo("GET", APIEndpoints.LEGACY_SEARCH_PLAYLISTS.path())
                        .withQueryParam("q", "any query")
                        .withQueryParam("limit", "30")));
    }

    @Test
    public void shouldMakeGETRequestToSearchPeopleEndpoint() throws Exception {
        searchOperations.getUserSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(
                isPublicApiRequestTo("GET", APIEndpoints.LEGACY_SEARCH_USERS.path())
                        .withQueryParam("q", "any query")
                        .withQueryParam("limit", "30")));
    }

    @Test
    public void shouldFilterUnknownResourcesForSearchAll() throws Exception {
        PublicApiResource track = new PublicApiTrack();
        PublicApiResource playlist = new PublicApiPlaylist();
        PublicApiResource user = new PublicApiUser();
        PublicApiResource unknown = new UnknownResource();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(
                track, playlist, unknown, user));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);
        PublicApiTrack cachedTrack = new PublicApiTrack();
        when(modelManager.cache(track, PublicApiResource.CacheUpdateMode.FULL)).thenReturn(cachedTrack);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().getPagedCollection()).toContainExactly(track, playlist, user);
    }

    @Test
    public void shouldCacheSearchResultsInModelManager() throws Exception {
        PublicApiResource track = new PublicApiTrack();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        verify(modelManager).cache(track, PublicApiResource.CacheUpdateMode.FULL);
    }

    @Test
    public void shouldWriteSearchResultsToLocalStorage() {
        final PublicApiTrack track = new PublicApiTrack();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.<PublicApiResource>asList(track));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        verify(bulkStorage).bulkInsertAsync(any(SearchResultsCollection.class));
    }

    @Test
    public void shouldEmitCachedModelsOnSearchResults() throws Exception {
        PublicApiResource track = new PublicApiTrack();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);
        PublicApiTrack cachedTrack = new PublicApiTrack();
        when(modelManager.cache(track, PublicApiResource.CacheUpdateMode.FULL)).thenReturn(cachedTrack);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        PublicApiTrack firstResultsTrack = (PublicApiTrack) Lists.newArrayList(captor.getValue().getPagedCollection()).get(0);
        expect(firstResultsTrack).toBe(cachedTrack);
    }

    @Test
    public void filteredCollectionKeepsNextHref() throws Exception {
        final ArrayList<PublicApiResource> results = Lists.<PublicApiResource>newArrayList(ModelFixtures.create(PublicApiTrack.class));
        SearchResultsCollection collection = new SearchResultsCollection(results, "next-href");

        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        final Page<SearchResultsCollection> value = captor.getValue();
        expect(value.getPagedCollection().getNextHref()).toEqual("next-href");
    }

    @Test
    public void hasNextPageWhenNextHRefNotBlank() throws Exception {
        PublicApiResource track = new PublicApiTrack();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track), "a NextHref");
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);
        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeTrue();
    }

    @Test
    public void hasNextPageIsFalseWhenNextHRefBlank() throws Exception {
        PublicApiResource track = new PublicApiTrack();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track), "");
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);
        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeFalse();
    }

    @Test
    public void fetchesNextPageObservableBasedOnNextHref() throws Exception {
        PublicApiResource track = new PublicApiTrack();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track), "http://soundcloud.com/next-href.json");
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);

        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<APIRequest> captor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient, times(2)).fetchModels(captor.capture());
        expect(captor.getAllValues().get(1).getEncodedPath()).toEqual("/next-href.json");
    }
}
