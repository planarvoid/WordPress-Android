package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperationPaged.Page;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.model.PlaylistTagsCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.PlaylistTagStorage;

import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.android.OperationPaged;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class SearchOperationsTest {

    private SearchOperations searchOperations;

    @Mock
    private RxHttpClient rxHttpClient;
    @Mock
    private PlaylistTagStorage tagStorage;
    @Mock
    private ScModelManager modelManager;

    @Mock
    private Observer observer;

    @Before
    public void setUp() {
        searchOperations = new SearchOperations(rxHttpClient, tagStorage, modelManager);
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() throws Exception {
        searchOperations.getAllSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_ALL.path())));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() throws Exception {
        searchOperations.getTrackSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_TRACKS.path())));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() throws Exception {
        searchOperations.getPlaylistSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_PLAYLISTS.path())));
    }

    @Test
    public void shouldMakeGETRequestToSearchPeopleEndpoint() throws Exception {
        searchOperations.getUserSearchResults("any query").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_USERS.path())));
    }

    @Test
    public void shouldMakeRequestToSearchAllWithCorrectQuery() throws Exception {
        searchOperations.getAllSearchResults("the query").subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchModels(argumentCaptor.capture());
        String sentQuery = argumentCaptor.getValue().getQueryParameters().get("q").toArray()[0].toString();
        expect(sentQuery).toEqual("the query");
    }

    @Test
    public void shouldMakeRequestToSearchAllWithPageSize() throws Exception {
        searchOperations.getAllSearchResults("blah").subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchModels(argumentCaptor.capture());
        String value = argumentCaptor.getValue().getQueryParameters().get("limit").toArray()[0].toString();
        int pageSize = Integer.valueOf(value);
        expect(pageSize).toEqual(30);
    }

    @Test
    public void shouldFilterUnknownResourcesForSearchAll() throws Exception {
        ScResource track = new Track();
        ScResource playlist = new Playlist();
        ScResource user = new User();
        ScResource unknown = new UnknownResource();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(
                track, playlist, unknown, user));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);
        Track cachedTrack = new Track();
        when(modelManager.cache(track, ScResource.CacheUpdateMode.FULL)).thenReturn(cachedTrack);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<OperationPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().getPagedCollection()).toContainInOrder(track, playlist, user);
    }

    @Test
    public void shouldCacheSearchResultsInModelManager() throws Exception {
        ScResource track = new Track();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        verify(modelManager).cache(track, ScResource.CacheUpdateMode.FULL);
    }

    @Test
    public void shouldEmitCachedModelsOnSearchResults() throws Exception {
        ScResource track = new Track();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track));
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);
        Track cachedTrack = new Track();
        when(modelManager.cache(track, ScResource.CacheUpdateMode.FULL)).thenReturn(cachedTrack);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<OperationPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        Track firstResultsTrack = (Track) Lists.newArrayList(captor.getValue().getPagedCollection()).get(0);
        expect(firstResultsTrack).toBe(cachedTrack);
    }

    @Test
    public void filteredCollectionKeepsNextHref() throws Exception {
        final ArrayList<ScResource> results = Lists.<ScResource>newArrayList(TestHelper.getModelFactory().createModel(Track.class));
        SearchResultsCollection collection = new SearchResultsCollection(results, "next-href");

        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<OperationPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        final Page<SearchResultsCollection> value = captor.getValue();
        expect(value.getPagedCollection().getNextHref()).toEqual("next-href");
    }

    @Test
    public void hasNextPageWhenNextHRefNotBlank() throws Exception {
        ScResource track = new Track();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track), "a NextHref");
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);
        ArgumentCaptor<OperationPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeTrue();
    }

    @Test
    public void hasNextPageIsFalseWhenNextHRefBlank() throws Exception {
        ScResource track = new Track();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track), "");
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);
        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getAllSearchResults("any query").subscribe(observer);
        ArgumentCaptor<OperationPaged.Page> captor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeFalse();
    }

    @Test
    public void fetchesNextPageObservableBasedOnNextHref() throws Exception {
        ScResource track = new Track();
        SearchResultsCollection collection = new SearchResultsCollection(Arrays.asList(track), "http://soundcloud.com/next-href.json");
        Observable<SearchResultsCollection> observable = Observable.<SearchResultsCollection>from(collection);

        when(rxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);
        searchOperations.getAllSearchResults("any query").subscribe(observer);

        ArgumentCaptor<APIRequest> captor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient, times(2)).fetchModels(captor.capture());
        expect(captor.getAllValues().get(1).getEncodedPath()).toEqual("/next-href.json");
    }

    @Test
    public void shouldMakeGETRequestToPlaylistTagsEndpoint() throws Exception {
        searchOperations.getPlaylistTags().subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.PLAYLIST_DISCOVERY_TAGS.path())));
    }

    @Test
    public void shouldMapPlaylistTagsToHaveAHashSymbol() {
        PlaylistTagsCollection tags = new PlaylistTagsCollection();
        tags.setCollection(Arrays.asList("tag1", "tag2", "tag3"));
        when(rxHttpClient.<PlaylistTagsCollection>fetchModels(any(APIRequest.class))).thenReturn(
                Observable.<PlaylistTagsCollection>from(tags));

        searchOperations.getPlaylistTags().subscribe(observer);

        ArgumentCaptor<PlaylistTagsCollection> tagsCaptor = ArgumentCaptor.forClass(PlaylistTagsCollection.class);
        verify(observer).onNext(tagsCaptor.capture());

        expect(tagsCaptor.getValue()).toContainExactly("#tag1", "#tag2", "#tag3");
    }

    @Test
    public void shouldReturnRecentPlaylistTagsWithHashSymbol() {
        PlaylistTagsCollection tags = new PlaylistTagsCollection();
        tags.setCollection(Arrays.asList("tag3", "tag2", "tag1"));
        when(tagStorage.getRecentTagsAsync()).thenReturn(Observable.<PlaylistTagsCollection>from(tags));

        searchOperations.getRecentPlaylistTags().subscribe(observer);

        ArgumentCaptor<PlaylistTagsCollection> tagsCaptor = ArgumentCaptor.forClass(PlaylistTagsCollection.class);
        verify(observer).onNext(tagsCaptor.capture());

        expect(tagsCaptor.getValue()).toContainExactly("#tag3", "#tag2", "#tag1");
    }

    @Test
    public void shouldMakeGETRequestToPlaylistDiscoveryEndpoint() {
        searchOperations.getPlaylistDiscoveryResults("electronic").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET",
                APIEndpoints.PLAYLIST_DISCOVERY_RESULTS.path("electronic"))));
    }

    @Test
    public void shouldDeliverPlaylistDiscoveryResultsToObserver() throws CreateModelException {
        PlaylistSummary playlist = TestHelper.getModelFactory().createModel(PlaylistSummary.class);
        PlaylistSummaryCollection collection = new PlaylistSummaryCollection();
        collection.setCollection(Arrays.asList(playlist));
        when(rxHttpClient.<PlaylistSummaryCollection>fetchModels(any(APIRequest.class))).thenReturn(
                Observable.<PlaylistSummaryCollection>from(collection));

        searchOperations.getPlaylistDiscoveryResults("electronic").subscribe(observer);

        ArgumentCaptor<Page> resultCaptor = ArgumentCaptor.forClass(Page.class);
        verify(observer).onNext(resultCaptor.capture());

        expect(resultCaptor.getValue().getPagedCollection()).toBe(collection);
    }

    @Test
    public void shouldStripHashSymbolFromSearchTag() {
        searchOperations.getPlaylistDiscoveryResults("#electronic").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET",
                APIEndpoints.PLAYLIST_DISCOVERY_RESULTS.path("electronic"))));
    }
}
