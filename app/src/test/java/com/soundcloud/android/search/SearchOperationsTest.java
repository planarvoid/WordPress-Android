package com.soundcloud.android.search;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.users.UserWriteStorage;
import com.soundcloud.propeller.TxnResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SearchOperationsTest {

    private SearchOperations operations;

    @Mock private RxHttpClient rxHttpClient;
    @Mock private Observer<SearchResult> observer;
    @Mock private UserWriteStorage userStorage;
    @Mock private PlaylistWriteStorage playlistStorage;
    @Mock private TrackWriteStorage trackStorage;

    private ApiTrack track;
    private ApiPlaylist playlist;
    private ApiUser user;

    @Before
    public void setUp() {
        track = ModelFixtures.create(ApiTrack.class);
        playlist = ModelFixtures.create(ApiPlaylist.class);
        user = ModelFixtures.create(ApiUser.class);

        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        when(userStorage.storeUsersAsync(any(List.class))).thenReturn(Observable.<TxnResult>empty());
        when(trackStorage.storeTracksAsync(any(List.class))).thenReturn(Observable.<TxnResult>empty());
        when(playlistStorage.storePlaylistsAsync(any(List.class))).thenReturn(Observable.<TxnResult>empty());

        operations = new SearchOperations(rxHttpClient, userStorage, playlistStorage, trackStorage);
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

    @Test
    public void shouldCacheUserSearchResult() {
        List<ApiUser> userList = ModelFixtures.create(ApiUser.class, 2);
        final Observable<ModelCollection<ApiUser>> observable = getUserCollectionObservable(userList);
        when(rxHttpClient.<ModelCollection<ApiUser>>fetchModels(any(APIRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_USERS).subscribe(observer);

        verify(userStorage).storeUsersAsync(userList);
    }

    @Test
    public void shouldCachePlaylistSearchResult() {
        List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        final Observable<ModelCollection<ApiPlaylist>> observable = getPlaylistCollectionObservable(playlists);
        when(rxHttpClient.<ModelCollection<ApiPlaylist>>fetchModels(any(APIRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(playlistStorage).storePlaylistsAsync(playlists);
    }

    @Test
    public void shouldCacheTrackSearchResult() {
        List<ApiTrack> trackList = ModelFixtures.create(ApiTrack.class, 2);
        final Observable<ModelCollection<ApiTrack>> observable = getTrackCollectionObservable(trackList);
        when(rxHttpClient.<ModelCollection<ApiTrack>>fetchModels(any(APIRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_TRACKS).subscribe(observer);

        verify(trackStorage).storeTracksAsync(trackList);
    }

    @Test
    public void shouldCacheUniversalSearchResult() {
        List<UniversalSearchResult> results = getUniversalSearchResultList(user, track, playlist);
        final Observable<ModelCollection<UniversalSearchResult>> observable = getUniversalCollectionObservable(results);
        when(rxHttpClient.<ModelCollection<UniversalSearchResult>>fetchModels(any(APIRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        verify(trackStorage).storeTracksAsync(Lists.newArrayList(track));
        verify(playlistStorage).storePlaylistsAsync(Lists.newArrayList(playlist));
        verify(userStorage).storeUsersAsync(Lists.newArrayList(user));
    }

    private Observable<ModelCollection<UniversalSearchResult>> getUniversalCollectionObservable(List<UniversalSearchResult> results) {
        ModelCollection<UniversalSearchResult> collection = new ModelCollection<>(results);
        return Observable.<ModelCollection<UniversalSearchResult>>from(collection);
    }

    private List<UniversalSearchResult> getUniversalSearchResultList(ApiUser user, ApiTrack track, ApiPlaylist playlist) {
        UniversalSearchResult userResult = new UniversalSearchResult();
        userResult.setUser(user);

        UniversalSearchResult playlistResult = new UniversalSearchResult();
        playlistResult.setPlaylist(playlist);

        UniversalSearchResult trackResult = new UniversalSearchResult();
        trackResult.setTrack(track);

        return Lists.newArrayList(userResult, playlistResult, trackResult);
    }

    private Observable<ModelCollection<ApiTrack>> getTrackCollectionObservable(List<ApiTrack> tracks) {
        ModelCollection<ApiTrack> collection = new ModelCollection<>(tracks);
        return Observable.<ModelCollection<ApiTrack>>from(collection);
    }

    private Observable<ModelCollection<ApiPlaylist>> getPlaylistCollectionObservable(List<ApiPlaylist> playlists) {
        ModelCollection<ApiPlaylist> collection = new ModelCollection<>(playlists);
        return Observable.<ModelCollection<ApiPlaylist>>from(collection);
    }

    private Observable<ModelCollection<ApiUser>> getUserCollectionObservable(List<ApiUser> userList) {
        ModelCollection<ApiUser> collection = new ModelCollection<>(userList);
        return Observable.<ModelCollection<ApiUser>>from(collection);
    }
}