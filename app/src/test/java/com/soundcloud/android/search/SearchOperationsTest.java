package com.soundcloud.android.search;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class SearchOperationsTest {

    private SearchOperations operations;

    @Mock private RxHttpClient rxHttpClient;
    @Mock private Observer observer;

    @Before
    public void setUp() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        operations = new SearchOperations(rxHttpClient);
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.SEARCH_ALL.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_TRACKS).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.SEARCH_TRACKS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.SEARCH_PLAYLISTS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchUserEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_USERS).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.SEARCH_USERS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

}