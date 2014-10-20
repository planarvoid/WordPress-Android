package com.soundcloud.android.search;

import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.users.UserWriteStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SearchOperationsTest {

    private SearchOperations operations;

    @Mock private RxHttpClient rxHttpClient;
    @Mock private Observer<SearchResult> observer;
    @Mock private UserWriteStorage userStorage;
    @Mock private PlaylistWriteStorage playlistWriteStorage;
    @Mock private PlaylistStorage playlistStorage;
    @Mock private TrackWriteStorage trackStorage;

    private ApiTrack track;
    private ApiPlaylist playlist;
    private ApiUser user;

    @Before
    public void setUp() {
        track = ModelFixtures.create(ApiTrack.class);
        playlist = ModelFixtures.create(ApiPlaylist.class);
        user = ModelFixtures.create(ApiUser.class);

        when(rxHttpClient.fetchModels(any(ApiRequest.class))).thenReturn(Observable.empty());

        operations = new SearchOperations(rxHttpClient, userStorage, playlistWriteStorage, playlistStorage, trackStorage);
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_ALL.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_TRACKS).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_TRACKS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchUserEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_USERS).subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_USERS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldCacheUserSearchResult() {
        List<ApiUser> userList = ModelFixtures.create(ApiUser.class, 2);
        final Observable<ModelCollection<ApiUser>> observable = getUserCollectionObservable(userList);
        when(rxHttpClient.<ModelCollection<ApiUser>>fetchModels(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_USERS).subscribe(observer);

        verify(userStorage).storeUsers(userList);
    }

    @Test
    public void shouldCachePlaylistSearchResult() {
        List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        final Observable<ModelCollection<ApiPlaylist>> observable = getPlaylistCollectionObservable(playlists);
        when(rxHttpClient.<ModelCollection<ApiPlaylist>>fetchModels(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(playlistWriteStorage).storePlaylists(playlists);
    }

    @Test
    public void shouldCacheTrackSearchResult() {
        List<ApiTrack> trackList = ModelFixtures.create(ApiTrack.class, 2);
        final Observable<ModelCollection<ApiTrack>> observable = getTrackCollectionObservable(trackList);
        when(rxHttpClient.<ModelCollection<ApiTrack>>fetchModels(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_TRACKS).subscribe(observer);

        verify(trackStorage).storeTracks(trackList);
    }

    @Test
    public void shouldCacheUniversalSearchResult() {
        List<UniversalSearchResult> results = getUniversalSearchResultList(user, track, playlist);
        final Observable<ModelCollection<UniversalSearchResult>> observable = getUniversalCollectionObservable(results);
        when(rxHttpClient.<ModelCollection<UniversalSearchResult>>fetchModels(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        verify(trackStorage).storeTracks(Lists.newArrayList(track));
        verify(playlistWriteStorage).storePlaylists(Lists.newArrayList(playlist));
        verify(userStorage).storeUsers(Lists.newArrayList(user));
    }

    @Test
    public void shouldBackFillLikesForPlaylists() {
        final Observable<ModelCollection<ApiPlaylist>> observable = getPlaylistCollectionObservable(Arrays.asList(playlist));
        when(rxHttpClient.<ModelCollection<ApiPlaylist>>fetchModels(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(playlistStorage).backFillLikesStatus(Arrays.asList(playlist.toPropertySet()));
    }

    private Observable<ModelCollection<UniversalSearchResult>> getUniversalCollectionObservable(List<UniversalSearchResult> results) {
        ModelCollection<UniversalSearchResult> collection = new ModelCollection<>(results);
        return Observable.<ModelCollection<UniversalSearchResult>>from(collection);
    }

    private List<UniversalSearchResult> getUniversalSearchResultList(ApiUser user, ApiTrack track, ApiPlaylist playlist) {
        return Lists.newArrayList(
                UniversalSearchResult.forUser(user),
                UniversalSearchResult.forPlaylist(playlist),
                UniversalSearchResult.forTrack(track));
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