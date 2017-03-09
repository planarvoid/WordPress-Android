package com.soundcloud.android.search;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRepository;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;
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
    private static final Urn TRACK_ONE_URN = Urn.forTrack(101L);
    private static final Urn TRACK_TWO_URN = Urn.forTrack(102L);
    private static final Urn PREMIUM_TRACK_URN = Urn.forTrack(103L);

    private SearchOperations operations;

    @Mock private ApiClientRx apiClientRx;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private StoreUsersCommand storeUsersCommand;
    @Mock private CacheUniversalSearchCommand cacheUniversalSearchCommand;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private LoadFollowingCommand loadFollowingCommand;
    @Mock private FeatureFlags featureFlags;
    @Mock private TrackItemRepository trackRepository;

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
        operations = new SearchOperations(new SearchStrategyFactory(apiClientRx,
                                                                    Schedulers.immediate(),
                                                                    storePlaylistsCommand,
                                                                    storeTracksCommand,
                                                                    storeUsersCommand,
                                                                    cacheUniversalSearchCommand,
                                                                    loadPlaylistLikedStatuses,
                                                                    loadFollowingCommand
        ), trackRepository);
    }

    @Test
    public void shouldMakeGETRequestToSearchAllEndpoint() {
        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_ALL.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchTracksEndpoint() {
        operations.searchResult("query", Optional.absent(), SearchType.TRACKS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_TRACKS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchPlaylistsWithoutAlbumsEndpointWhenAlbumsFeatureFlagEnabled() {
        operations.searchResult("query", Optional.absent(), SearchType.PLAYLISTS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET",
                                                                                  ApiEndpoints.SEARCH_PLAYLISTS_WITHOUT_ALBUMS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchAlbumsEndpoint() {
        operations.searchResult("query", Optional.absent(), SearchType.ALBUMS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_ALBUMS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchUserEndpoint() {
        operations.searchPremiumResult("query", SearchType.USERS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_USERS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchAllEndpoint() {
        operations.searchPremiumResult("query", SearchType.ALL).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_ALL.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchTracksEndpoint() {
        operations.searchPremiumResult("query", SearchType.TRACKS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_TRACKS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchPlaylistsEndpoint() {
        operations.searchPremiumResult("query", SearchType.PLAYLISTS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_PLAYLISTS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToPremiumSearchAlbumsEndpoint() {
        operations.searchPremiumResult("query", SearchType.ALBUMS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_ALBUMS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldMakeGETRequestToSearchUserEndpoint() {
        operations.searchResult("query", Optional.absent(), SearchType.USERS).subscribe(subscriber);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_USERS.path())
                                                                           .withQueryParam("limit", "30")
                                                                           .withQueryParam("q", "query")), isA(TypeToken.class));
    }

    @Test
    public void shouldCacheUserSearchResult() {
        final SearchModelCollection<ApiUser> users = new SearchModelCollection<>(ModelFixtures.create(ApiUser.class,
                                                                                                      2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.just(users));

        operations.searchResult("query", Optional.absent(), SearchType.USERS).subscribe(subscriber);

        verify(storeUsersCommand).call(users);
    }

    @Test
    public void shouldCachePremiumUserSearchResult() {
        final List<ApiUser> apiUsers = ModelFixtures.create(ApiUser.class, 2);
        final SearchModelCollection<ApiUser> premiumUsers = new SearchModelCollection<>(apiUsers);
        final SearchModelCollection<ApiUser> users = new SearchModelCollection<>(apiUsers,
                                                                                 Collections.emptyMap(),
                                                                                 "query",
                                                                                 premiumUsers,
                                                                                 TRACK_RESULTS_COUNT,
                                                                                 PLAYLIST_RESULTS_COUNT,
                                                                                 USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.just(users));

        operations.searchResult("query", Optional.absent(), SearchType.USERS).subscribe(subscriber);

        verify(storeUsersCommand).call(users);
        verify(storeUsersCommand).call(premiumUsers);
    }

    @Test
    public void shouldCachePlaylistSearchResult() {
        final SearchModelCollection<ApiPlaylist> playlists = new SearchModelCollection<>(ModelFixtures.create(
                ApiPlaylist.class,
                2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(
                playlists));

        operations.searchResult("query", Optional.absent(), SearchType.PLAYLISTS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(playlists);
    }

    @Test
    public void shouldCacheAlbumSearchResult() {
        final SearchModelCollection<ApiPlaylist> albums = new SearchModelCollection<>(ModelFixtures.create(ApiPlaylist.class,
                                                                                                           2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.just(albums));

        operations.searchResult("query", Optional.absent(), SearchType.ALBUMS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(albums);
    }

    @Test
    public void shouldCachePremiumPlaylistSearchResult() {
        final List<ApiPlaylist> apiPlaylists = ModelFixtures.create(ApiPlaylist.class, 2);
        final SearchModelCollection<ApiPlaylist> premiumPlaylists = new SearchModelCollection<>(apiPlaylists);
        final SearchModelCollection<ApiPlaylist> playlists = new SearchModelCollection<>(apiPlaylists,
                                                                                         Collections.emptyMap(),
                                                                                         "query",
                                                                                         premiumPlaylists,
                                                                                         TRACK_RESULTS_COUNT,
                                                                                         PLAYLIST_RESULTS_COUNT,
                                                                                         USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(
                playlists));

        operations.searchResult("query", Optional.absent(), SearchType.PLAYLISTS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(playlists);
        verify(storePlaylistsCommand).call(premiumPlaylists);
    }

    @Test
    public void shouldCachePremiumAlbumSearchResult() {
        final List<ApiPlaylist> apiAlbums = ModelFixtures.create(ApiPlaylist.class, 2);
        final SearchModelCollection<ApiPlaylist> premiumAlbums = new SearchModelCollection<>(apiAlbums);
        final SearchModelCollection<ApiPlaylist> albums = new SearchModelCollection<>(apiAlbums,
                                                                                      Collections.emptyMap(),
                                                                                      "query",
                                                                                      premiumAlbums,
                                                                                      TRACK_RESULTS_COUNT,
                                                                                      PLAYLIST_RESULTS_COUNT,
                                                                                      USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.just(albums));

        operations.searchResult("query", Optional.absent(), SearchType.ALBUMS).subscribe(subscriber);

        verify(storePlaylistsCommand).call(albums);
        verify(storePlaylistsCommand).call(premiumAlbums);
    }

    @Test
    public void shouldCacheTrackSearchResult() {
        final SearchModelCollection<ApiTrack> tracks = new SearchModelCollection<>(ModelFixtures.create(ApiTrack.class,
                                                                                                        2));
        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.just(tracks));

        operations.searchResult("query", Optional.absent(), SearchType.TRACKS).subscribe(subscriber);

        verify(storeTracksCommand).call(tracks);
    }

    @Test
    public void shouldCachePremiumTracksSearchResult() {
        final List<ApiTrack> apiTracks = ModelFixtures.create(ApiTrack.class, 2);
        final SearchModelCollection<ApiTrack> premiumTracks = new SearchModelCollection<>(apiTracks);
        final SearchModelCollection<ApiTrack> tracks = new SearchModelCollection<>(apiTracks,
                                                                                   Collections.emptyMap(),
                                                                                   "query",
                                                                                   premiumTracks,
                                                                                   TRACK_RESULTS_COUNT,
                                                                                   PLAYLIST_RESULTS_COUNT,
                                                                                   USER_RESULTS_COUNT);

        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.just(tracks));

        operations.searchResult("query", Optional.absent(), SearchType.TRACKS).subscribe(subscriber);

        verify(storeTracksCommand).call(tracks);
        verify(storeTracksCommand).call(premiumTracks);
    }

    @Test
    public void shouldCacheUniversalSearchResult() {
        final Observable observable = Observable.just(new SearchModelCollection<>(Lists.newArrayList(
                forUser(user),
                forTrack(track),
                forPlaylist(playlist))));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        verify(cacheUniversalSearchCommand).call(anyIterable());
    }

    @Test
    public void shouldCachePremiumUniversalSearchResult() {
        final List<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                forUser(user),
                forTrack(track),
                forPlaylist(playlist));

        final SearchModelCollection<ApiUniversalSearchItem> premiumUniversalSearchItems = new SearchModelCollection<>(
                apiUniversalSearchItems);
        final SearchModelCollection<ApiUniversalSearchItem> universalSearchItems = new SearchModelCollection<>(
                apiUniversalSearchItems,
                Collections.emptyMap(),
                "query",
                premiumUniversalSearchItems,
                TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT,
                USER_RESULTS_COUNT);

        final Observable observable = Observable.just(universalSearchItems);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        verify(cacheUniversalSearchCommand, times(2)).call(anyIterable());
    }

    @Test
    public void shouldBackFillLikesForPlaylistsInUniversalSearchResult() {
        final List<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(forUser(user), forTrack(track), forPlaylist(playlist));

        final Observable observable = Observable.just(new SearchModelCollection<>(apiUniversalSearchItems));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        final SearchResult expectedSearchResult = SearchResult.fromSearchableItems(Lists.transform(apiUniversalSearchItems, ApiUniversalSearchItem::toListItem),
                                                                                   Optional.absent(),
                                                                                   Optional.absent());
        final Map<Urn, Boolean> likedPlaylists = Collections.singletonMap(playlist.getUrn(), true);
        when(loadPlaylistLikedStatuses.call(eq(Lists.transform(expectedSearchResult.getItems(), Entity::getUrn)))).thenReturn(likedPlaylists);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PlaylistItem playlistItem = (PlaylistItem) searchResult.getItems().get(2);
        assertThat(playlistItem.getUrn()).isEqualTo(playlist.getUrn());
        assertThat(playlistItem.isUserLike()).isTrue();
    }

    @Test
    public void shouldBackFillFollowingsForUsersInUniversalSearchResult() {
        final ApiUser user2 = ModelFixtures.create(ApiUser.class);
        final List<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                forUser(user),
                forTrack(track),
                forUser(user2),
                forPlaylist(playlist));
        final SearchResult expectedSearchResult = SearchResult.fromSearchableItems(Lists.transform(apiUniversalSearchItems, ApiUniversalSearchItem::toListItem),
                                                                                   Optional.absent(),
                                                                                   Optional.absent());
        final Map<Urn, Boolean> userFollowings = Collections.singletonMap(user.getUrn(), true);

        final Observable observable = Observable.just(new SearchModelCollection<>(apiUniversalSearchItems));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);
        when(loadFollowingCommand.call(Lists.transform(expectedSearchResult.getItems(), Entity::getUrn))).thenReturn(userFollowings);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);

        final UserItem followedUser = (UserItem) searchResult.getItems().get(0);
        assertThat(followedUser.isFollowedByMe()).isTrue();

        final UserItem nonFollowedUserItem = (UserItem) searchResult.getItems().get(2);
        assertThat(nonFollowedUserItem).isEqualTo(UserItem.from(user2));
    }

    @Test
    public void shouldRetainOrderWhenBackfillingLikesForPlaylistsInUniversalSearchResult() {
        final ApiPlaylist playlist2 = ModelFixtures.create(ApiPlaylist.class);
        final List<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                forPlaylist(playlist), // should be enriched with like status
                forUser(user),
                forPlaylist(playlist2), // should be enriched with like status
                forTrack(track));
        final Observable observable = Observable.just(new SearchModelCollection<>(apiUniversalSearchItems));

        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);

        final Map<Urn, Boolean> likedPlaylists = new HashMap<>(2);
        likedPlaylists.put(playlist2.getUrn(), true);
        likedPlaylists.put(playlist.getUrn(), false);
        when(loadPlaylistLikedStatuses.call(Lists.transform(apiUniversalSearchItems, item -> item.toListItem().getUrn()))).thenReturn(
                likedPlaylists);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final ListItem playlist1Set = searchResult.getItems().get(0);
        final ListItem playlist2Set = searchResult.getItems().get(2);
        // expect things to still be in correct order
        assertThat(playlist1Set.getUrn()).isEqualTo(playlist.getUrn());
        assertThat(playlist2Set.getUrn()).isEqualTo(playlist2.getUrn());
    }

    @Test
    public void shouldBackFillLikesForPlaylistsInPlaylistSearch() {
        final List<ApiPlaylist> apiPlaylists = singletonList(playlist);
        final Observable searchObservable = Observable.just(new SearchModelCollection<>(apiPlaylists));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(searchObservable);

        Map<Urn, Boolean> likedPlaylists = Collections.singletonMap(playlist.getUrn(), true);
        when(loadPlaylistLikedStatuses.call(Lists.transform(apiPlaylists, ApiPlaylist::getUrn))).thenReturn(likedPlaylists);

        operations.searchResult("query", Optional.absent(), SearchType.PLAYLISTS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PlaylistItem playlistItem = (PlaylistItem) searchResult.getItems().get(0);
        assertThat(playlistItem.isUserLike()).isTrue();
    }

    @Test
    public void shouldBackFillLikesForAlbumsInAlbumSearch() {
        final List<ApiPlaylist> apiPlaylists = singletonList(playlist);
        final Observable searchObservable = Observable.just(new SearchModelCollection<>(apiPlaylists));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(searchObservable);

        Map<Urn, Boolean> likedPlaylists = Collections.singletonMap(playlist.getUrn(), true);
        when(loadPlaylistLikedStatuses.call(Lists.transform(apiPlaylists, ApiPlaylist::getUrn))).thenReturn(likedPlaylists);

        operations.searchResult("query", Optional.absent(), SearchType.ALBUMS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        final PlaylistItem albumItem = (PlaylistItem) searchResult.getItems().get(0);
        assertThat(albumItem.isUserLike()).isTrue();
    }

    @Test
    public void shouldProvideResultPagerForPlaylists() {
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                singletonList(playlist),
                Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link("http://api-mobile.sc.com/next")));
        final SearchModelCollection<ApiPlaylist> lastPage = new SearchModelCollection<>(singletonList(
                playlist));

        when(apiClientRx.mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS_WITHOUT_ALBUMS.path())),
                isA(TypeToken.class)))
                .thenReturn(Observable.just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/next")), isA(TypeToken.class)))
                .thenReturn(Observable.just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.PLAYLISTS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", Optional.absent(), SearchType.PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldProvideResultPagerForAlbum() {
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                singletonList(playlist),
                Collections.singletonMap(ModelCollection.NEXT_LINK_REL, new Link("http://api-mobile.sc.com/next")));
        final SearchModelCollection<ApiPlaylist> lastPage = new SearchModelCollection<>(singletonList(
                playlist));

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_ALBUMS.path())),
                                        isA(TypeToken.class)))
                .thenReturn(Observable.just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/next")), isA(TypeToken.class)))
                .thenReturn(Observable.just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.ALBUMS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", Optional.absent(), SearchType.ALBUMS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }


    @Test
    public void shouldProvidePremiumResultPagerForPlaylists() {
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                singletonList(playlist),
                Collections.singletonMap(ModelCollection.NEXT_LINK_REL,
                                         new Link("http://api-mobile.sc.com/premium/next")));
        final SearchModelCollection<ApiPlaylist> lastPage = new SearchModelCollection<>(singletonList(
                playlist));

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_PLAYLISTS.path())),
                                        isA(TypeToken.class)))
                .thenReturn(Observable.just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/premium/next")), isA(TypeToken.class)))
                .thenReturn(Observable.just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.PLAYLISTS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchPremiumResult("q", SearchType.PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldProvidePremiumResultPagerForAlbums() {
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                singletonList(playlist),
                Collections.singletonMap(ModelCollection.NEXT_LINK_REL,
                                         new Link("http://api-mobile.sc.com/premium/next")));
        final SearchModelCollection<ApiPlaylist> lastPage = new SearchModelCollection<>(singletonList(
                playlist));

        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PREMIUM_ALBUMS.path())),
                                        isA(TypeToken.class)))
                .thenReturn(Observable.just(firstPage));
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", "/premium/next")), isA(TypeToken.class)))
                .thenReturn(Observable.just(lastPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.ALBUMS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchPremiumResult("q", SearchType.ALBUMS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldProvideResultPagerWithQuerySourceInfo() {
        final Urn queryUrn = new Urn("soundcloud:search:urn");
        final SearchModelCollection<ApiPlaylist> firstPage = new SearchModelCollection<>(
                singletonList(playlist),
                Collections.emptyMap(),
                queryUrn.toString(),
                null,
                TRACK_RESULTS_COUNT,
                PLAYLIST_RESULTS_COUNT,
                USER_RESULTS_COUNT
        );

        when(apiClientRx.mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.SEARCH_PLAYLISTS_WITHOUT_ALBUMS.path())),
                isA(TypeToken.class)))
                .thenReturn(Observable.just(firstPage));

        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.PLAYLISTS);
        final Pager<SearchResult> searchResultPager = Pager.create(pagingFunction);
        searchResultPager.page(operations.searchResult("q", Optional.absent(), SearchType.PLAYLISTS)).subscribe(subscriber);
        searchResultPager.next();

        subscriber.assertValueCount(1);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldMapPremiumContentInUniversalSearchItemWhenBuildingSearchResult() {
        final ArrayList<ApiUniversalSearchItem> searchItems = Lists.newArrayList(
                forUser(user),
                forTrack(track),
                forPlaylist(playlist));
        final SearchModelCollection<ApiUniversalSearchItem> apiPremiumUniversalSearchItems =
                new SearchModelCollection<>(searchItems);
        mockPremiumSearchApiResponse(searchItems, apiPremiumUniversalSearchItems);

        List<ListItem> searchableItems = Lists.transform(searchItems, ApiUniversalSearchItem::toListItem);
        final SearchResult premiumSearchResult = SearchResult.fromSearchableItems(searchableItems,
                                                                                  Optional.absent(),
                                                                                  Optional.absent());
        final SearchResult expectedSearchResult = SearchResult.fromSearchableItems(searchableItems,
                                                                                   Optional.absent(),
                                                                                   Optional.absent(),
                                                                                   Optional.of(premiumSearchResult),
                                                                                   SEARCH_RESULTS_COUNT);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isTrue();
        assertThat(searchResult.getPremiumContent().get().getItems().size()).isEqualTo(3);
        assertThat(searchResult.getPremiumContent().get().getItems()).isEqualTo(expectedSearchResult.getPremiumContent()
                                                                                                    .get()
                                                                                                    .getItems());
    }

    @Test
    public void shouldNotMapPremiumContentInTracksWhenBuildingSearchResult() {
        final List<ApiTrack> searchTrackItems = singletonList(track);
        final SearchModelCollection<ApiTrack> apiPremiumTracks = new SearchModelCollection<>(searchTrackItems);
        mockPremiumSearchApiResponse(searchTrackItems, apiPremiumTracks);

        operations.searchResult("query", Optional.absent(), SearchType.TRACKS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void shouldNotMapPremiumContentInPlaylistsWhenBuildingSearchResult() {
        final List<ApiPlaylist> searchPlaylistItems = singletonList(playlist);
        final SearchModelCollection<ApiPlaylist> apiPremiumPlaylists = new SearchModelCollection<>(searchPlaylistItems);
        mockPremiumSearchApiResponse(searchPlaylistItems, apiPremiumPlaylists);

        operations.searchResult("query", Optional.absent(), SearchType.PLAYLISTS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void shouldNotMapPremiumContentInAlbumsWhenBuildingSearchResult() {
        final List<ApiPlaylist> searchPlaylistItems = singletonList(playlist);
        final SearchModelCollection<ApiPlaylist> apiPremiumPlaylists = new SearchModelCollection<>(searchPlaylistItems);
        mockPremiumSearchApiResponse(searchPlaylistItems, apiPremiumPlaylists);

        operations.searchResult("query", Optional.absent(), SearchType.ALBUMS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void shouldNotMapPremiumContentInUsersWhenBuildingSearchResult() {
        final List<ApiUser> searchUserItems = singletonList(user);
        final SearchModelCollection<ApiUser> apiPremiumUsers = new SearchModelCollection<>(searchUserItems);
        mockPremiumSearchApiResponse(searchUserItems, apiPremiumUsers);

        operations.searchResult("query", Optional.absent(), SearchType.USERS).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void premiumContentShouldBeAbsentWhenPremiumCollectionIsNull() {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = Lists.newArrayList(
                forUser(user),
                forTrack(track),
                forPlaylist(playlist));
        mockPremiumSearchApiResponse(apiUniversalSearchItems, null);

        operations.searchResult("query", Optional.absent(), SearchType.ALL).subscribe(subscriber);

        subscriber.assertValueCount(1);
        final SearchResult searchResult = subscriber.getOnNextEvents().get(0);
        assertThat(searchResult.getPremiumContent().isPresent()).isFalse();
    }

    @Test
    public void premiumContentUrnShouldNotBeIncludedInPaginatedContentWhenAbsent() {
        final TrackItem trackOne = ModelFixtures.trackItem(TRACK_ONE_URN);
        final TrackItem trackTwo = ModelFixtures.trackItem(TRACK_TWO_URN);
        final SearchResult searchResult = SearchResult.fromSearchableItems(Arrays.asList(trackOne, trackTwo),
                                                                           Optional.absent(),
                                                                           Urn.NOT_SET);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).containsExactly(TRACK_ONE_URN, TRACK_TWO_URN);
    }

    @Test
    public void premiumContentUrnShouldBeIncludedInPaginatedContent() {
        final TrackItem premiumTrack = ModelFixtures.trackItem(PREMIUM_TRACK_URN);
        final SearchResult premiumSearchResult = SearchResult.fromSearchableItems(singletonList(premiumTrack),
                                                                                  Optional.absent(), Urn.NOT_SET);
        final SearchResult searchResult = SearchResult.fromSearchableItems(Lists.newArrayList(TrackItem.from(track)),
                                                                           Optional.absent(),
                                                                           Optional.absent(),
                                                                           Optional.of(premiumSearchResult),
                                                                           SEARCH_RESULTS_COUNT);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).containsExactly(PREMIUM_TRACK_URN, track.getUrn());
    }

    @Test
    public void contentUpsellUrnShouldNotBeIncludedInPaginatedContent() {
        final UpsellSearchableItem upsellItem = UpsellSearchableItem.forUpsell();
        final TrackItem trackOne = ModelFixtures.trackItem(TRACK_ONE_URN);
        final TrackItem trackTwo = ModelFixtures.trackItem(TRACK_TWO_URN);
        final SearchResult searchResult = SearchResult.fromSearchableItems(Arrays.asList(upsellItem, trackOne, trackTwo),
                                                                           Optional.absent(),
                                                                           Urn.NOT_SET);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).containsExactly(TRACK_ONE_URN, TRACK_TWO_URN);
    }

    @Test
    public void pagingShouldReturnAnEmptyPageWhenResultIsEmpty() {
        final SearchResult searchResult = SearchResult.fromSearchableItems(Collections.emptyList(),
                                                                           Optional.absent(),
                                                                           Urn.NOT_SET);
        final SearchOperations.SearchPagingFunction pagingFunction = operations.pagingFunction(SearchType.ALL);

        pagingFunction.call(searchResult);

        assertThat(pagingFunction.getAllUrns()).isEmpty();
    }

    private <T> void mockPremiumSearchApiResponse(List<T> searchItems, SearchModelCollection<T> apiPremiumItems) {
        final Observable observable = Observable.just(new SearchModelCollection<>(searchItems,
                                                                                  Collections.emptyMap(),
                                                                                  "queryUrn",
                                                                                  apiPremiumItems,
                                                                                  TRACK_RESULTS_COUNT,
                                                                                  PLAYLIST_RESULTS_COUNT,
                                                                                  USER_RESULTS_COUNT));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(observable);
    }

    private ApiUniversalSearchItem forTrack(ApiTrack track) {
        return new ApiUniversalSearchItem(null, null, track);
    }

    private ApiUniversalSearchItem forPlaylist(ApiPlaylist playlist) {
        return new ApiUniversalSearchItem(null, playlist, null);
    }

    private ApiUniversalSearchItem forUser(ApiUser user) {
        return new ApiUniversalSearchItem(user, null, null);
    }

}
