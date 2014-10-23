package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.users.UserWriteStorage;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SearchOperationsTest {

    private SearchOperations operations;

    @Mock private ApiScheduler apiScheduler;
    @Mock private UserWriteStorage userStorage;
    @Mock private PlaylistWriteStorage playlistWriteStorage;
    @Mock private PlaylistStorage playlistStorage;
    @Mock private TrackWriteStorage trackStorage;

    private ApiTrack track;
    private ApiPlaylist playlist;
    private ApiUser user;
    private TestObserver<SearchResult> observer;

    @Before
    public void setUp() {
        observer = new TestObserver<>();
        track = ModelFixtures.create(ApiTrack.class);
        playlist = ModelFixtures.create(ApiPlaylist.class);
        user = ModelFixtures.create(ApiUser.class);

        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(Observable.empty());

        operations = new SearchOperations(apiScheduler, userStorage, playlistWriteStorage, playlistStorage, trackStorage);
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_ALL.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_TRACKS).subscribe(observer);

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_TRACKS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldMakeGETRequestToSearchUserEndpoint() {
        operations.getSearchResult("query", SearchOperations.TYPE_USERS).subscribe(observer);

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_USERS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")));
    }

    @Test
    public void shouldCacheUserSearchResult() {
        List<ApiUser> users = ModelFixtures.create(ApiUser.class, 2);
        Observable observable = Observable.just(new ModelCollection<>(users));
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_USERS).subscribe(observer);

        verify(userStorage).storeUsers(users);
    }

    @Test
    public void shouldCachePlaylistSearchResult() {
        List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        Observable observable = Observable.just(new ModelCollection<>(playlists));
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        verify(playlistWriteStorage).storePlaylists(playlists);
    }

    @Test
    public void shouldCacheTrackSearchResult() {
        List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        Observable observable = Observable.just(new ModelCollection<>(tracks));
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_TRACKS).subscribe(observer);

        verify(trackStorage).storeTracks(tracks);
    }

    @Test
    public void shouldCacheUniversalSearchResult() {
        Observable observable = Observable.just(new ModelCollection<>(Lists.newArrayList(
                UniversalSearchResult.forUser(user),
                UniversalSearchResult.forTrack(track),
                UniversalSearchResult.forPlaylist(playlist))));
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(observable);

        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        verify(trackStorage).storeTracks(Lists.newArrayList(track));
        verify(playlistWriteStorage).storePlaylists(Lists.newArrayList(playlist));
        verify(userStorage).storeUsers(Lists.newArrayList(user));
    }

    @Test
    public void shouldBackFillLikesForPlaylistsInUniversalSearchResult() {
        Observable observable = Observable.just(new ModelCollection<>(Lists.newArrayList(
                UniversalSearchResult.forUser(user),
                UniversalSearchResult.forTrack(track),
                UniversalSearchResult.forPlaylist(playlist))));
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(observable);

        PropertySet playlistIsLikedStatus = PropertySet.from(
                PlaylistProperty.URN.bind(playlist.getUrn()),
                PlaylistProperty.IS_LIKED.bind(true));
        when(playlistStorage.playlistLikes(anyList())).thenReturn(Arrays.asList(playlistIsLikedStatus));

        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        SearchResult searchResult = observer.getOnNextEvents().get(0);
        PropertySet playlistPropSet = searchResult.getItems().get(2);
        expect(playlistPropSet).toEqual(playlist.toPropertySet().merge(playlistIsLikedStatus));
    }

    @Test
    public void shouldRetainOrderWhenBackfillingLikesForPlaylistsInUniversalSearchResult() {
        ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        Observable observable = Observable.just(new ModelCollection<>(Lists.newArrayList(
                UniversalSearchResult.forPlaylist(playlist), // should be enriched with like status
                UniversalSearchResult.forUser(user),
                UniversalSearchResult.forPlaylist(playlist2), // should be enriched with like status
                UniversalSearchResult.forTrack(track))));
        when(apiScheduler.<ModelCollection<UniversalSearchResult>>mappedResponse(any(ApiRequest.class))).thenReturn(observable);
        when(playlistStorage.playlistLikes(anyList())).thenReturn(Arrays.asList(
                // the database call returns playlist2 first, so changes order! which is valid.
                PropertySet.from(PlaylistProperty.URN.bind(playlist2.getUrn()), PlaylistProperty.IS_LIKED.bind(true)),
                PropertySet.from(PlaylistProperty.URN.bind(playlist.getUrn()), PlaylistProperty.IS_LIKED.bind(false))
        ));

        operations.getSearchResult("query", SearchOperations.TYPE_ALL).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        SearchResult searchResult = observer.getOnNextEvents().get(0);
        PropertySet playlist1Set = searchResult.getItems().get(0);
        PropertySet playlist2Set = searchResult.getItems().get(2);
        // expect things to still be in correct order
        expect(playlist1Set.get(PlaylistProperty.URN)).toEqual(playlist.toPropertySet().get(PlaylistProperty.URN));
        expect(playlist2Set.get(PlaylistProperty.URN)).toEqual(playlist2.toPropertySet().get(PlaylistProperty.URN));
    }

    @Test
    public void shouldBackFillLikesForPlaylists() {
        Observable observable = Observable.just(new ModelCollection<>(Arrays.asList(playlist)));
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(observable);
        PropertySet playlistIsLikedStatus = PropertySet.from(
                PlaylistProperty.URN.bind(playlist.getUrn()),
                PlaylistProperty.IS_LIKED.bind(true));
        when(playlistStorage.playlistLikes(anyList())).thenReturn(Arrays.asList(playlistIsLikedStatus));

        operations.getSearchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        SearchResult searchResult = observer.getOnNextEvents().get(0);
        PropertySet playlistPropSet = searchResult.getItems().get(0);
        expect(playlistPropSet).toEqual(playlist.toPropertySet().merge(playlistIsLikedStatus));
    }

    @Test
    public void shouldProvideResultPager() {
        ModelCollection<ApiPlaylist> firstPage = new ModelCollection<>(Arrays.asList(playlist));
        ModelCollection<ApiPlaylist> lastPage = new ModelCollection<>(Arrays.asList(playlist));
        firstPage.setLinks(Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link("http://api-mobile.sc.com/next")));

        when(apiScheduler.mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path()))))
                .thenReturn(Observable.<Object>just(firstPage));
        when(apiScheduler.mappedResponse(argThat(isMobileApiRequestTo("GET", "/next")))).
                thenReturn(Observable.<Object>just(lastPage));

        SearchOperations.SearchResultPager pager = operations.pager(SearchOperations.TYPE_PLAYLISTS);
        pager.page(operations.getSearchResult("q", SearchOperations.TYPE_PLAYLISTS)).subscribe(observer);
        pager.next();

        expect(observer.getOnNextEvents()).toNumber(2);
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }
}