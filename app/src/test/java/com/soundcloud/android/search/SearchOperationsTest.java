package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class SearchOperationsTest {

    SearchOperations searchOperations;

    @Mock
    private SoundCloudRxHttpClient soundCloudRxHttpClient;
    @Mock
    private Observer<SearchResultsCollection> observer;

    @Before
    public void setUp() {
        searchOperations = new SearchOperations(soundCloudRxHttpClient);
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() throws Exception {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        searchOperations.getSearchResultsAll("any query").subscribe(observer);

        verify(soundCloudRxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_ALL.path())));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() throws Exception {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        searchOperations.getSearchResultsTracks("any query").subscribe(observer);

        verify(soundCloudRxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_TRACKS.path())));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() throws Exception {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        searchOperations.getSearchResultsPlaylists("any query").subscribe(observer);

        verify(soundCloudRxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_PLAYLISTS.path())));
    }

    @Test
    public void shouldMakeGETRequestToSearchPeopleEndpoint() throws Exception {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        searchOperations.getSearchResultsPeople("any query").subscribe(observer);

        verify(soundCloudRxHttpClient).fetchModels(argThat(isPublicApiRequestTo("GET", APIEndpoints.SEARCH_PEOPLE.path())));
    }

    @Test
    public void shouldMakeRequestToSearchAllWithCorrectQuery() throws Exception {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        searchOperations.getSearchResultsAll("the query").subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        String sentQuery = argumentCaptor.getValue().getQueryParameters().get("q").toArray()[0].toString();
        expect(sentQuery).toEqual("the query");
    }

    @Test
    public void shouldMakeRequestToSearchAllWithPageSize() throws Exception {
        when(soundCloudRxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        searchOperations.getSearchResultsAll("blah").subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(soundCloudRxHttpClient).fetchModels(argumentCaptor.capture());
        String value = argumentCaptor.getValue().getQueryParameters().get("limit").toArray()[0].toString();
        int pageSize = Integer.valueOf(value);
        expect(pageSize).toEqual(50);
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
        when(soundCloudRxHttpClient.<SearchResultsCollection>fetchModels(any(APIRequest.class))).thenReturn(observable);

        searchOperations.getSearchResultsAll("any query").subscribe(observer);

        ArgumentCaptor<SearchResultsCollection> captor = ArgumentCaptor.forClass(SearchResultsCollection.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue()).toContainInOrder(track, playlist, user);
    }

}
