package com.soundcloud.android.search;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.collection.LoadPlaylistRepostStatuses;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.discovery.PlaylistTagsItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistDiscoveryOperationsTest extends AndroidUnitTest {
    private static final List<String> POPULAR_TAGS = Arrays.asList("popTag1", "popTag2");
    private static final List<String> RECENT_TAGS = Arrays.asList("recentTag1", "recentTag2");

    private PlaylistDiscoveryOperations operations;
    private ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);

    @Mock private ApiClientRx apiClientRx;
    @Mock private PlaylistTagStorage tagStorage;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;
    @Mock private LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    @Mock private LoadPlaylistRepostStatuses loadPlaylistRepostStatuses;

    @Mock private Observer observer;

    private final TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setup() {
        operations = new PlaylistDiscoveryOperations(apiClientRx,
                                                     tagStorage,
                                                     storePlaylistsCommand,
                                                     loadPlaylistLikedStatuses,
                                                     loadPlaylistRepostStatuses,
                                                     Schedulers.immediate());
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiPlaylistCollection.class)))
                .thenReturn(Observable.<ApiPlaylistCollection>empty());
    }

    @Test
    public void shouldMakeGETRequestToPlaylistTagsEndpoint() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        operations.popularPlaylistTags().subscribe(observer);

        verify(apiClientRx).mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.PLAYLIST_DISCOVERY_TAGS.path())), isA(TypeToken.class));
    }

    @Test
    public void storesPopularTagsWhenRequestIsSuccessful() {
        ModelCollection<String> tags = new ModelCollection<>(Lists.newArrayList("tag"));
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(tags));

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage).cachePopularTags(tags.getCollection());
    }

    @Test
    public void doesNotStorePopularTagsWhenRequestFails() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.error(new Exception()));

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage, never()).cachePopularTags(anyList());
    }

    @Test
    public void loadsPopularTagsFromCacheIfStored() {
        List<String> tags = Lists.newArrayList("tag");
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(tags));

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage).getPopularTagsAsync();
        verifyZeroInteractions(apiClientRx);
    }

    @Test
    public void shouldMakeGETRequestToPlaylistDiscoveryEndpoint() {
        operations.playlistsForTag("electronic").subscribe(observer);

        verify(apiClientRx).mappedResponse(argThat(isApiRequestTo("GET",
                                                                                  ApiEndpoints.PLAYLIST_DISCOVERY.path())),
                                           eq(ApiPlaylistCollection.class));
    }

    @Test
    public void shouldMakeRequestPlaylistDiscoveryResultsWithCorrectParameters() {
        operations.playlistsForTag("electronic").subscribe(observer);

        MultiMap<String, String> parameters = new ListMultiMap<>();
        parameters.put("tag", "electronic");
        parameters.put("limit", String.valueOf(20));

        ArgumentCaptor<ApiRequest> resultCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(resultCaptor.capture(), eq(ApiPlaylistCollection.class));
        assertThat(resultCaptor.getValue().getQueryParameters()).isEqualTo(parameters);
    }

    @Test
    public void shouldDeliverPlaylistDiscoveryResultsToObserver() {
        SearchResult collection = toSearchResult(buildPlaylistSummariesResponse());

        operations.playlistsForTag("tag1").subscribe(observer);

        verify(observer).onNext(collection);
    }

    @Test
    public void shouldWritePlaylistDiscoveryResultToLocalStorage() {
        ApiPlaylistCollection collection = buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(storePlaylistsCommand).call(collection);
    }

    @Test
    public void shouldMergePlaylistLikedStatus() {
        Map<Urn, PropertySet> likeStatus = new HashMap<>(1);
        likeStatus.put(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(true)));

        SearchResult result = toSearchResult(buildPlaylistSummariesResponse());
        when(loadPlaylistLikedStatuses.call(result)).thenReturn(likeStatus);

        operations.playlistsForTag("tag1").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.isLikedByCurrentUser()).isTrue();
    }

    @Test
    public void shouldMergePlaylistNotLikedStatus() {
        Map<Urn, PropertySet> likeStatus = new HashMap<>(1);
        likeStatus.put(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_LIKE.bind(false)));

        SearchResult result = toSearchResult(buildPlaylistSummariesResponse());
        when(loadPlaylistLikedStatuses.call(result)).thenReturn(likeStatus);

        operations.playlistsForTag("tag1").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.isLikedByCurrentUser()).isFalse();
    }

    @Test
    public void shouldMergePlaylistRepostedStatus() {
        Map<Urn, PropertySet> repostStatus = new HashMap<>(1);
        repostStatus.put(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_REPOST.bind(true)));

        SearchResult result = toSearchResult(buildPlaylistSummariesResponse());
        when(loadPlaylistRepostStatuses.call(result)).thenReturn(repostStatus);

        operations.playlistsForTag("tag1").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.isRepostedByCurrentUser()).isTrue();
    }

    @Test
    public void shouldMergePlaylistNotRepostedStatus() {
        Map<Urn, PropertySet> repostStatus = new HashMap<>(1);
        repostStatus.put(playlist.getUrn(), PropertySet.from(PlaylistProperty.IS_USER_REPOST.bind(false)));

        SearchResult result = toSearchResult(buildPlaylistSummariesResponse());
        when(loadPlaylistRepostStatuses.call(result)).thenReturn(repostStatus);

        operations.playlistsForTag("tag1").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.isRepostedByCurrentUser()).isFalse();
    }

    @Test
    public void shouldPrependSearchedTagToPlaylistTags() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.getTags()).containsExactly("electronic", "tag1", "tag2", "tag3");
    }

    @Test
    public void shouldReorderTagListIfPlaylistTagSearchedForAlreadyExists() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("tag2").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.getTags()).containsExactly("tag2", "tag1", "tag3");
    }

    @Test
    public void shouldReorderTagListIfPlaylistTagSearchedWithDifferentCaseAlreadyExists() {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("Tag2").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.getTags()).containsExactly("Tag2", "tag1", "tag3");
    }

    @Test
    public void shouldNotReorderTagListIfSearchedTagIsSubsetOfAnExistingTag() {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("ag2").subscribe(observer);

        PlaylistItem playlistItem = PlaylistItem.from(captureFirstPlaylist());
        assertThat(playlistItem.getTags()).containsExactly("ag2", "tag1", "tag2", "tag3");
    }

    @Test
    public void addsSearchedTagToRecentTagsStorage() {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(tagStorage).addRecentTag(eq("electronic"));
    }

    @Test
    public void addsSearchedTagToRecentTagsStorageWhenRequestFails() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class),
                                        isA(TypeToken.class))).thenReturn(Observable.error(new Exception()));

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(tagStorage).addRecentTag(eq("electronic"));
    }

    @Test
    public void clearPlaylistTagsData() {
        operations.clearData();

        verify(tagStorage).clear();
        verifyNoMoreInteractions(tagStorage);
    }

    @Test
    public void shouldNotFetchPlaylistTagsIfTagsCachedIsNotExpired() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(singletonList("tag")));
        when(tagStorage.isTagsCacheExpired()).thenReturn(false);
        operations.popularPlaylistTags().subscribe(observer);

        verifyZeroInteractions(apiClientRx);
    }

    @Test
    public void loadsPlaylistDiscoTagsWhenThereAreNoRecommendations() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(POPULAR_TAGS));
        when(tagStorage.getRecentTagsAsync()).thenReturn(Observable.just(RECENT_TAGS));
        operations.playlistTags().subscribe(subscriber);

        final List<DiscoveryItem> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        assertPlaylistDiscoItem(onNextEvents.get(0), POPULAR_TAGS, RECENT_TAGS);
    }

    @Test
    public void loadsRecommendationsAndPopularTagsWhenPlaylistRecentTagsIsEmpty() {

        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(POPULAR_TAGS));
        when(tagStorage.getRecentTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));

        operations.playlistTags().subscribe(subscriber);

        final List<DiscoveryItem> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        assertPlaylistDiscoItem(onNextEvents.get(0), POPULAR_TAGS, Collections.<String>emptyList());
    }

    @Test
    public void loadsRecommendationsAndRecentTagsWhenPlaylistPopularTagsIsEmpty() {
        ModelCollection<String> emptyTags = new ModelCollection<>(Collections.<String>emptyList());
        when(tagStorage.getRecentTagsAsync()).thenReturn(Observable.just(RECENT_TAGS));
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Observable.just(emptyTags));

        operations.playlistTags().subscribe(subscriber);

        final List<DiscoveryItem> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        assertPlaylistDiscoItem(onNextEvents.get(0), Collections.<String>emptyList(), RECENT_TAGS);
    }

    @Test
    public void loadsRecommendationsAndRecentTagsReturnsEmptyWhenBothAreEmpty() {
        ModelCollection<String> emptyTags = new ModelCollection<>(Collections.<String>emptyList());
        when(tagStorage.getRecentTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Observable.just(emptyTags));

        operations.playlistTags().subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    private void assertPlaylistDiscoItem(DiscoveryItem discoveryItem,
                                         List<String> popularTags,
                                         List<String> recentTags) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.PlaylistTagsItem);

        final PlaylistTagsItem playlistDiscoItem = (PlaylistTagsItem) discoveryItem;
        assertThat(playlistDiscoItem.getPopularTags()).isEqualTo(popularTags);
        assertThat(playlistDiscoItem.getRecentTags()).isEqualTo(recentTags);
    }

    private PropertySet captureFirstPlaylist() {
        ArgumentCaptor<SearchResult> resultCaptor = ArgumentCaptor.forClass(SearchResult.class);
        verify(observer).onNext(resultCaptor.capture());

        return resultCaptor.getValue().getItems().get(0);
    }

    private ApiPlaylistCollection buildPlaylistSummariesResponse() {
        ApiPlaylistCollection collection = new ApiPlaylistCollection(singletonList(playlist), null, null);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiPlaylistCollection.class)))
                .thenReturn(Observable.just(collection));
        return collection;
    }

    private SearchResult toSearchResult(ApiPlaylistCollection collection) {
        return SearchResult.fromPropertySetSource(buildPlaylistSummariesResponse().getCollection(),
                                                  Optional.<Link>absent(), Optional.<Urn>absent());
    }
}
