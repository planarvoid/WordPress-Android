package com.soundcloud.android.search;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.SearchCollection;
import com.soundcloud.android.associations.LoadFollowingCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.rx.Pager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchOperationsTest extends AndroidUnitTest {

    private SearchOperations operations;

    @Mock private ApiClientRx apiClientRx;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private CacheUniversalSearchCommand cacheUniversalSearchCommand;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private LoadFollowingCommand loadFollowingCommand;

    private ApiTrack track;
    private ApiPlaylist playlist;
    private ApiUser user;
    private TestSubscriber<SearchResult> subscriber;

    @Before
    public void setUp() {
        subscriber = new TestSubscriber<>();
        track = ModelFixtures.create(ApiTrack.class);
        playlist = ModelFixtures.create(ApiPlaylist.class);
        user = ModelFixtures.create(ApiUser.class);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.empty());

        operations = new SearchOperations(apiClientRx, storeTracksCommand, storePlaylistsCommand, storeUsersCommand,
                cacheUniversalSearchCommand, loadPlaylistLikedStatuses, loadFollowingCommand, Schedulers.immediate());
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() {
        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_ALL.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() {
        operations.searchResult("query", SearchOperations.TYPE_TRACKS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_TRACKS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsEndpoint() {
        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchUserEndpoint() {
        operations.searchResult("query", SearchOperations.TYPE_USERS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_USERS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldCacheUserSearchResult() throws PropellerWriteException {
        final SearchCollection<ApiUser> users = new SearchCollection<>();
        users.setCollection(ModelFixtures.create(ApiUser.class, 2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(users));

        operations.searchResult("query", SearchOperations.TYPE_USERS).subscribe(subscriber);

        verify(storeUsersCommand).call(users);
    }

    @Test
    public void shouldCachePlaylistSearchResult() throws Exception {
        final SearchCollection<ApiPlaylist> playlists = new SearchCollection<>();
        playlists.setCollection(ModelFixtures.create(ApiPlaylist.class, 2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(playlists));

        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(playlists);
    }

    @Test
    public void shouldCacheTrackSearchResult() throws Exception {
        final SearchCollection<ApiTrack> tracks = new SearchCollection<>();
        tracks.setCollection(ModelFixtures.create(ApiTrack.class, 2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(tracks));

        operations.searchResult("query", SearchOperations.TYPE_TRACKS).subscribe(subscriber);

        verify(storeTracksCommand).call(tracks);
    }

    @Test
    public void shouldCacheUniversalSearchResult() throws Exception {
        final Observable observable = Observable.just(new SearchCollection<>(Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist))));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        verify(cacheUniversalSearchCommand).call();
    }

    @Test
    public void shouldBackFillLikesForPlaylistsInUniversalSearchResult() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist));

        final Observable observable = Observable.just(new SearchCollection<>(apiUniversalSearchItems));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        final PropertySet playlistIsLikedStatus = PropertySet.from(
                PlaylistProperty.URN.bind(playlist.getUrn()),
                PlaylistProperty.IS_LIKED.bind(true));

        final SearchResult expectedSearchResult = new SearchResult(apiUniversalSearchItems, Optional.<Link>absent(), Optional.<Urn>absent());
        final Map<Urn, PropertySet> likedPlaylists = Collections.singletonMap(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_LIKED.bind(true)));
        when(loadPlaylistLikedStatuses.call(expectedSearchResult)).thenReturn(likedPlaylists);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PropertySet playlistPropSet = searchResult.getItems().get(2);
        assertThat(playlistPropSet).isEqualTo(playlist.toPropertySet().merge(playlistIsLikedStatus));
    }

    @Test
    public void shouldBackFillFollowingsForUsersInUniversalSearchResult() throws Exception {
        final ApiUser user2 = ModelFixtures.create(ApiUser.class);
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forUser(user2),
                ApiUniversalSearchItem.forPlaylist(playlist));
        final SearchResult expectedSearchResult = new SearchResult(apiUniversalSearchItems, Optional.<Link>absent(), Optional.<Urn>absent());
        final Map<Urn, PropertySet> userFollowings = Collections.singletonMap(user.getUrn(), PropertySet.from(UserProperty.IS_FOLLOWED_BY_ME.bind(true)));
        final PropertySet userIsFollowing = PropertySet.from(UserProperty.IS_FOLLOWED_BY_ME.bind(true));

        final Observable observable = Observable.just(new SearchCollection<>(apiUniversalSearchItems));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);
        when(loadFollowingCommand.call(expectedSearchResult)).thenReturn(userFollowings);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);

        final PropertySet followedUserPropertySet = searchResult.getItems().get(0);
        assertThat(followedUserPropertySet).isEqualTo(user.toPropertySet().merge(userIsFollowing));

        final PropertySet nonFollowedUserPropertySet = searchResult.getItems().get(2);
        assertThat(nonFollowedUserPropertySet).isEqualTo(user2.toPropertySet());
    }

    @Test
    public void shouldRetainOrderWhenBackfillingLikesForPlaylistsInUniversalSearchResult() throws Exception {
        final ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forPlaylist(playlist), // should be enriched with like status
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forPlaylist(playlist2), // should be enriched with like status
                ApiUniversalSearchItem.forTrack(track));
        final Observable observable = Observable.just(new SearchCollection<>(apiUniversalSearchItems));

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        final Map<Urn, PropertySet> likedPlaylists = new HashMap<>(2);
        likedPlaylists.put(playlist2.getUrn(), PropertySet.from(PlaylistProperty.IS_LIKED.bind(true)));
        likedPlaylists.put(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_LIKED.bind(false)));
        when(loadPlaylistLikedStatuses.call(PropertySets.toPropertySets(apiUniversalSearchItems))).thenReturn(likedPlaylists);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PropertySet playlist1Set = searchResult.getItems().get(0);
        final PropertySet playlist2Set = searchResult.getItems().get(2);
        // expect things to still be in correct order
        assertThat(playlist1Set.get(PlaylistProperty.URN)).isEqualTo(playlist.toPropertySet().get(PlaylistProperty.URN));
        assertThat(playlist2Set.get(PlaylistProperty.URN)).isEqualTo(playlist2.toPropertySet().get(PlaylistProperty.URN));
    }

    @Test
    public void shouldBackFillLikesForPlaylistsInPlaylistSearch() throws Exception {
        final List<ApiPlaylist> apiPlaylists = Arrays.asList(playlist);
        final Observable searchObservable = Observable.just(new SearchCollection<>(apiPlaylists));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(searchObservable);

        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_LIKED.bind(true));
        Map<Urn, PropertySet> likedPlaylists = Collections.singletonMap(playlist.getUrn(), playlistIsLikedStatus);
        final SearchResult expectedSearchResult = new SearchResult(apiPlaylists, Optional.<Link>absent(), Optional.<Urn>absent());
        when(loadPlaylistLikedStatuses.call(expectedSearchResult)).thenReturn(likedPlaylists);

        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PropertySet playlistPropSet = searchResult.getItems().get(0);
        assertThat(playlistPropSet).isEqualTo(playlist.toPropertySet().merge(playlistIsLikedStatus));
    }

    @Test
    public void shouldProvideResultPager() {
        final SearchCollection<ApiPlaylist> firstPage = new SearchCollection<>(Collections.singletonList(playlist));
        final SearchCollection<ApiPlaylist> lastPage = new SearchCollection<>(Collections.singletonList(playlist));
        firstPage.setLinks(Collections.singletonMap(SearchCollection.NEXT_LINK_REL, new Link("http://api-mobile.sc.com/next")));

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.<Object>just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/next")), isA(TypeToken.class))).
                thenReturn(Observable.<Object>just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_PLAYLISTS);
        final Pager<SearchResult, SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", SearchOperations.TYPE_PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldProvideResultPagerWithQuerySourceInfo() {
        final Urn queryUrn = new Urn("soundcloud:search:urn");
        final SearchCollection<ApiPlaylist> firstPage = new SearchCollection<>(Collections.singletonList(playlist));
        firstPage.setQueryUrn(queryUrn.toString());

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.<Object>just(firstPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_PLAYLISTS);
        final Pager<SearchResult, SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", SearchOperations.TYPE_PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
        assertThat(pagingFunction.getSearchQuerySourceInfo().getQueryUrn()).isEqualTo(queryUrn);
    }
}
