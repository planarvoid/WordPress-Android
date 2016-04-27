package com.soundcloud.android.search;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
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

    private static final int SEARCH_RESULTS_COUNT = 30;
    private static final int TRACK_RESULTS_COUNT = 10;
    private static final int PLAYLIST_RESULTS_COUNT = 10;
    private static final int USER_RESULTS_COUNT = 10;
    private static final Urn TRACK_ONE_URN = Urn.forTrack(1L);
    private static final Urn TRACK_TWO_URN = Urn.forTrack(2L);
    private static final Urn PREMIUM_TRACK_URN = Urn.forTrack(3L);

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
    public void shouldMakeGETRequestToPremiumSearchUserEndpoint() {
        operations.searchPremiumResult("query", SearchOperations.TYPE_USERS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_USERS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchAllEndpoint() {
        operations.searchPremiumResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_ALL.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchTracksEndpoint() {
        operations.searchPremiumResult("query", SearchOperations.TYPE_TRACKS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_TRACKS.path())
                .withQueryParam("limit", "30")
                .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchPlaylistsEndpoint() {
        operations.searchPremiumResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_PLAYLISTS.path())
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
    public void shouldCacheUserSearchResult() {
        final SearchModelCollection<ApiUser> users = new SearchModelCollection<>(ModelFixtures.create(ApiUser.class, 2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(users));

        operations.searchResult("query", SearchOperations.TYPE_USERS).subscribe(subscriber);

        verify(storeUsersCommand).call(users);
    }

    @Test
    public void shouldCachePremiumUserSearchResult() {
        final List<ApiUser> apiUsers = ModelFixtures.create(ApiUser.class, 2);
        final SearchModelCollection<ApiUser> premiumUsers = new SearchModelCollection<>(apiUsers);
        final SearchModelCollection<ApiUser> users = new SearchModelCollection<>(apiUsers,
                Collections.<String, Link>emptyMap(), "query", premiumUsers, TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(users));

        operations.searchResult("query", SearchOperations.TYPE_USERS).subscribe(subscriber);

        verify(storeUsersCommand).call(users);
        verify(storeUsersCommand).call(premiumUsers);
    }

    @Test
    public void shouldCachePlaylistSearchResult() {
        final SearchModelCollection<ApiPlaylist> playlists = new SearchModelCollection<>(ModelFixtures.create(ApiPlaylist.class, 2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(playlists));

        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(playlists);
    }

    @Test
    public void shouldCachePremiumPlaylistSearchResult() {
        final List<ApiPlaylist> apiPlaylists = ModelFixtures.create(ApiPlaylist.class, 2);
        final SearchModelCollection<ApiPlaylist> premiumPlaylists = new SearchModelCollection<>(apiPlaylists);
        final SearchModelCollection<ApiPlaylist> playlists = new SearchModelCollection<>(apiPlaylists,
                Collections.<String, Link>emptyMap(), "query", premiumPlaylists, TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(playlists));

        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(playlists);
        verify(storePlaylistsCommand).call(premiumPlaylists);
    }

    @Test
    public void shouldCacheTrackSearchResult() {
        final SearchModelCollection<ApiTrack> tracks = new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class, 2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(tracks));

        operations.searchResult("query", SearchOperations.TYPE_TRACKS).subscribe(subscriber);

        verify(storeTracksCommand).call(tracks);
    }

    @Test
    public void shouldCachePremiumTracksSearchResult()  {
        final List<ApiTrack> apiTracks = ModelFixtures.create(ApiTrack.class, 2);
        final SearchModelCollection<ApiTrack> premiumTracks = new SearchModelCollection<>(apiTracks);
        final SearchModelCollection<ApiTrack> tracks = new SearchModelCollection<>(apiTracks,
                Collections.<String, Link>emptyMap(), "query", premiumTracks, TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(tracks));

        operations.searchResult("query", SearchOperations.TYPE_TRACKS).subscribe(subscriber);

        verify(storeTracksCommand).call(tracks);
        verify(storeTracksCommand).call(premiumTracks);
    }

    @Test
    public void shouldCacheUniversalSearchResult() {
        final Observable observable = Observable.just(new SearchModelCollection<>(Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist))));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        verify(cacheUniversalSearchCommand).call();
    }

    @Test
    public void shouldCachePremiumUniversalSearchResult() {
        final List<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist));

        final SearchModelCollection<ApiUniversalSearchItem> premiumUniversalSearchItems = new SearchModelCollection<>(apiUniversalSearchItems);
        final SearchModelCollection<ApiUniversalSearchItem> universalSearchItems = new SearchModelCollection<>(apiUniversalSearchItems,
                Collections.<String, Link>emptyMap(), "query", premiumUniversalSearchItems, TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT);

        final Observable observable = Observable.just(universalSearchItems);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        verify(cacheUniversalSearchCommand, times(2)).call();
    }

    @Test
    public void shouldBackFillLikesForPlaylistsInUniversalSearchResult() {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist));

        final Observable observable = Observable.just(new SearchModelCollection<>(apiUniversalSearchItems));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        final PropertySet playlistIsLikedStatus = PropertySet.from(
                PlaylistProperty.URN.bind(playlist.getUrn()),
                PlaylistProperty.IS_USER_LIKE.bind(true));

        final SearchResult expectedSearchResult = SearchResult.fromPropertySetSource(apiUniversalSearchItems,
                Optional.<Link>absent(), Optional.<Urn>absent());
        final Map<Urn, PropertySet> likedPlaylists = Collections.singletonMap(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true)));
        when(loadPlaylistLikedStatuses.call(expectedSearchResult)).thenReturn(likedPlaylists);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PropertySet playlistPropSet = searchResult.getItems().get(2);
        assertThat(playlistPropSet).isEqualTo(playlist.toPropertySet().merge(playlistIsLikedStatus));
    }

    @Test
    public void shouldBackFillFollowingsForUsersInUniversalSearchResult() {
        final ApiUser user2 = ModelFixtures.create(ApiUser.class);
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forUser(user2),
                ApiUniversalSearchItem.forPlaylist(playlist));
        final SearchResult expectedSearchResult = SearchResult.fromPropertySetSource(apiUniversalSearchItems,
                Optional.<Link>absent(), Optional.<Urn>absent());
        final Map<Urn, PropertySet> userFollowings = Collections.singletonMap(user.getUrn(), PropertySet.from(UserProperty.IS_FOLLOWED_BY_ME.bind(true)));
        final PropertySet userIsFollowing = PropertySet.from(UserProperty.IS_FOLLOWED_BY_ME.bind(true));

        final Observable observable = Observable.just(new SearchModelCollection<>(apiUniversalSearchItems));
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
    public void shouldRetainOrderWhenBackfillingLikesForPlaylistsInUniversalSearchResult() {
        final ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forPlaylist(playlist), // should be enriched with like status
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forPlaylist(playlist2), // should be enriched with like status
                ApiUniversalSearchItem.forTrack(track));
        final Observable observable = Observable.just(new SearchModelCollection<>(apiUniversalSearchItems));

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        final Map<Urn, PropertySet> likedPlaylists = new HashMap<>(2);
        likedPlaylists.put(playlist2.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true)));
        likedPlaylists.put(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(false)));
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
    public void shouldBackFillLikesForPlaylistsInPlaylistSearch() {
        final List<ApiPlaylist> apiPlaylists = Arrays.asList(playlist);
        final Observable searchObservable = Observable.just(new SearchModelCollection<>(apiPlaylists));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(searchObservable);

        final PropertySet playlistIsLikedStatus = PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true));
        Map<Urn, PropertySet> likedPlaylists = Collections.singletonMap(playlist.getUrn(), playlistIsLikedStatus);
        final SearchResult expectedSearchResult = SearchResult.fromPropertySetSource(apiPlaylists, Optional.<Link>absent(),
                Optional.<Urn>absent());
        when(loadPlaylistLikedStatuses.call(expectedSearchResult)).thenReturn(likedPlaylists);

        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PropertySet playlistPropSet = searchResult.getItems().get(0);
        assertThat(playlistPropSet).isEqualTo(playlist.toPropertySet().merge(playlistIsLikedStatus));
    }

    @Test
    public void shouldProvideResultPager() {
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                Collections.singletonList(playlist),
                Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link("http://api-mobile.sc.com/next")));
        final SearchModelCollection<ApiPlaylist> lastPage = new SearchModelCollection<>(Collections.singletonList(playlist));

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.<Object>just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/next")), isA(TypeToken.class))).
                thenReturn(Observable.<Object>just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_PLAYLISTS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", SearchOperations.TYPE_PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldProvidePremiumResultPager() {
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                Collections.singletonList(playlist),
                Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link("http://api-mobile.sc.com/premium/next")));
        final SearchModelCollection<ApiPlaylist> lastPage = new SearchModelCollection<>(Collections.singletonList(playlist));

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_PLAYLISTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.<Object>just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/premium/next")), isA(TypeToken.class))).
                thenReturn(Observable.<Object>just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingPremiumFunction(SearchOperations.TYPE_PLAYLISTS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchPremiumResult("q", SearchOperations.TYPE_PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldProvideResultPagerWithQuerySourceInfo() {
        final Urn queryUrn = new Urn("soundcloud:search:urn");
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                Collections.singletonList(playlist),
                Collections.<String, Link>emptyMap(),
                queryUrn.toString(),
                null,
                TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT,
                USER_RESULTS_COUNT
        );

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS.path())), isA(TypeToken.class)))
                .thenReturn(Observable.<Object>just(firstPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_PLAYLISTS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", SearchOperations.TYPE_PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
        assertThat(pagingFunction.getSearchQuerySourceInfo().getQueryUrn()).isEqualTo(queryUrn);
    }

    @Test
    public void shouldMapPremiumContentInUniversalSearchItemWhenBuildingSearchResult() {
        final ArrayList<ApiUniversalSearchItem> searchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist));
        final SearchModelCollection<ApiUniversalSearchItem> apiPremiumUniversalSearchItems = new SearchModelCollection<>(searchItems);
        mockPremiumSearchApiResponse(searchItems, apiPremiumUniversalSearchItems);

        final SearchResult premiumSearchResult = SearchResult.fromPropertySetSource(searchItems, Optional.<Link>absent(),
                Optional.<Urn>absent());
        final SearchResult expectedSearchResult = SearchResult.fromPropertySetSource(searchItems,
                Optional.<Link>absent(), Optional.<Urn>absent(), Optional.of(premiumSearchResult), SEARCH_RESULTS_COUNT);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isTrue();
        assertThat(searchResult.getPremiumContent().get().getItems().size()).isEqualTo(3);
        assertThat(searchResult.getPremiumContent().get().getItems()).isEqualTo(expectedSearchResult.getPremiumContent().get().getItems());
    }

    @Test
    public void shouldNotMapPremiumContentInTracksWhenBuildingSearchResult() {
        final List<ApiTrack> searchTrackItems = Collections.singletonList(track);
        final SearchModelCollection<ApiTrack> apiPremiumTracks = new SearchModelCollection<>(searchTrackItems);
        mockPremiumSearchApiResponse(searchTrackItems, apiPremiumTracks);

        operations.searchResult("query", SearchOperations.TYPE_TRACKS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void shouldNotMapPremiumContentInPlaylistsWhenBuildingSearchResult() {
        final List<ApiPlaylist> searchPlaylistItems = Collections.singletonList(playlist);
        final SearchModelCollection<ApiPlaylist> apiPremiumPlaylists = new SearchModelCollection<>(searchPlaylistItems);
        mockPremiumSearchApiResponse(searchPlaylistItems, apiPremiumPlaylists);

        operations.searchResult("query", SearchOperations.TYPE_PLAYLISTS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void shouldNotMapPremiumContentInUsersWhenBuildingSearchResult() {
        final List<ApiUser> searchUserItems = Collections.singletonList(user);
        final SearchModelCollection<ApiUser> apiPremiumUsers = new SearchModelCollection<>(searchUserItems);
        mockPremiumSearchApiResponse(searchUserItems, apiPremiumUsers);

        operations.searchResult("query", SearchOperations.TYPE_USERS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void premiumContentShouldBeAbsentWhenPremiumCollectionIsNull() {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                ApiUniversalSearchItem.forUser(user),
                ApiUniversalSearchItem.forTrack(track),
                ApiUniversalSearchItem.forPlaylist(playlist));
        mockPremiumSearchApiResponse(apiUniversalSearchItems, null);

        operations.searchResult("query", SearchOperations.TYPE_ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void premiumContentUrnShouldNotBeIncludedInPaginatedContentWhenAbsent() {
        final PropertySet trackOne = PropertySet.create().put(EntityProperty.URN, TRACK_ONE_URN);
        final PropertySet trackTwo = PropertySet.create().put(EntityProperty.URN, TRACK_TWO_URN);
        final SearchResult searchResult = SearchResult.fromPropertySets(Arrays.asList(trackOne, trackTwo), Optional.<Link>absent(), Urn.NOT_SET);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).containsExactly(TRACK_ONE_URN, TRACK_TWO_URN);
    }

    @Test
    public void premiumContentUrnShouldBeIncludedInPaginatedContent() {
        final PropertySet premiumTrack = PropertySet.create().put(EntityProperty.URN, PREMIUM_TRACK_URN);
        final ArrayList<ApiUniversalSearchItem> searchItems = Lists.newArrayList(ApiUniversalSearchItem.forTrack(track));
        final SearchResult premiumSearchResult = SearchResult.fromPropertySets(Collections.singletonList(premiumTrack),
                Optional.<Link>absent(), Urn.NOT_SET);
        final SearchResult searchResult = SearchResult.fromPropertySetSource(searchItems, Optional.<Link>absent(),
                Optional.<Urn>absent(), Optional.of(premiumSearchResult), SEARCH_RESULTS_COUNT);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).containsExactly(PREMIUM_TRACK_URN, track.getUrn());
    }

    @Test
    public void contentUpsellUrnShouldNotBeIncludedInPaginatedContent() {
        final PropertySet upsellItem = PropertySet.create().put(EntityProperty.URN, SearchUpsellItem.UPSELL_URN);
        final PropertySet trackOne = PropertySet.create().put(EntityProperty.URN, TRACK_ONE_URN);
        final PropertySet trackTwo = PropertySet.create().put(EntityProperty.URN, TRACK_TWO_URN);
        final SearchResult searchResult = SearchResult.fromPropertySets(Arrays.asList(upsellItem, trackOne, trackTwo), Optional.<Link>absent(), Urn.NOT_SET);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).containsExactly(TRACK_ONE_URN, TRACK_TWO_URN);
    }

    @Test
    public void pagingShouldReturnAnEmptyPageWhenResultIsEmpty() {
        final SearchResult searchResult = SearchResult.fromPropertySets(Collections.<PropertySet>emptyList(), Optional.<Link>absent(), Urn.NOT_SET);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchOperations.TYPE_ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).isEmpty();
    }

    private <T> void mockPremiumSearchApiResponse(List<T> searchItems, SearchModelCollection<T> apiPremiumItems) {
        final Observable observable = Observable.just(new SearchModelCollection<>(searchItems,
                Collections.<String, Link>emptyMap(), "queryUrn", apiPremiumItems,
                TRACK_RESULTS_COUNT, PLAYLIST_RESULTS_COUNT, USER_RESULTS_COUNT));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);
    }
}
