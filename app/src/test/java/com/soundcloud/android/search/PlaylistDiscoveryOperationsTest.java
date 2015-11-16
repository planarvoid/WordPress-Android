package com.soundcloud.android.search;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.PropellerWriteException;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistDiscoveryOperationsTest extends AndroidUnitTest {

    private PlaylistDiscoveryOperations operations;

    @Mock private PlaylistTagStorage tagStorage;
    @Mock private ApiClientRx apiClientRx;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private Observer observer;
    @Mock private StorePlaylistsCommand storePlaylistsCommand;

    @Before
    public void setup() {
        operations = new PlaylistDiscoveryOperations(apiClientRx, connectionHelper, tagStorage, storePlaylistsCommand, Schedulers.immediate());
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiPlaylistCollection.class)))
                .thenReturn(Observable.<ApiPlaylistCollection>empty());
    }

    @Test
    public void shouldMakeGETRequestToPlaylistTagsEndpoint() throws Exception {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        operations.popularPlaylistTags().subscribe(observer);

        verify(apiClientRx).mappedResponse(
                argThat(isApiRequestTo("GET", ApiEndpoints.PLAYLIST_DISCOVERY_TAGS.path())), isA(TypeToken.class));
    }

    @Test
    public void storesPopularTagsWhenRequestIsSuccessful() {
        ModelCollection<String> tags = new ModelCollection<>(Lists.newArrayList("tag"));
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.just(tags));
        when(connectionHelper.isNetworkConnected()).thenReturn(true);

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage).cachePopularTags(tags.getCollection());
    }

    @Test
    public void doesNotStorePopularTagsWhenRequestFails() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.error(new Exception()));

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
                ApiEndpoints.PLAYLIST_DISCOVERY.path())), eq(ApiPlaylistCollection.class));
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
    public void shouldDeliverPlaylistDiscoveryResultsToObserver() throws CreateModelException {
        ModelCollection<ApiPlaylist> collection = buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(observer).onNext(collection);
    }

    @Test
    public void shouldWritePlaylistDiscoveryResultToLocalStorage() throws CreateModelException, PropellerWriteException {
        ApiPlaylistCollection collection = buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(storePlaylistsCommand).call(collection);
    }

    private ApiPlaylistCollection buildPlaylistSummariesResponse() throws CreateModelException {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ApiPlaylistCollection collection = new ApiPlaylistCollection(Arrays.asList(playlist), null, null);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiPlaylistCollection.class))).thenReturn(Observable.just(collection));
        return collection;
    }

    @Test
    public void shouldPrependSearchedTagToPlaylistTags() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        assertThat(apiPlaylist.getTags()).containsExactly("electronic", "tag1", "tag2", "tag3");
    }

    private ApiPlaylist captureFirstApiPlaylist() {
        ArgumentCaptor<ModelCollection> resultCaptor = ArgumentCaptor.forClass(ModelCollection.class);
        verify(observer).onNext(resultCaptor.capture());

        return (ApiPlaylist) resultCaptor.getValue().getCollection().get(0);
    }

    @Test
    public void shouldReorderTagListIfPlaylistTagSearchedForAlreadyExists() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("tag2").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        assertThat(apiPlaylist.getTags()).containsExactly("tag2", "tag1", "tag3");
    }

    @Test
    public void shouldReorderTagListIfPlaylistTagSearchedWithDifferentCaseAlreadyExists() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("Tag2").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        assertThat(apiPlaylist.getTags()).containsExactly("Tag2", "tag1", "tag3");
    }

    @Test
    public void shouldNotReorderTagListIfSearchedTagIsSubsetOfAnExistingTag() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("ag2").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        assertThat(apiPlaylist.getTags()).containsExactly("ag2", "tag1", "tag2", "tag3");
    }

    @Test
    public void addsSearchedTagToRecentTagsStorage() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(tagStorage).addRecentTag(eq("electronic"));
    }

    @Test
    public void addsSearchedTagToRecentTagsStorageWhenRequestFails() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), isA(TypeToken.class))).thenReturn(Observable.error(new Exception()));

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
    public void shouldNotFetchPlaylistTagsIfNoInternetConnection() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.singletonList("tag")));
        when(tagStorage.isTagsCacheExpired()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        operations.popularPlaylistTags().subscribe(observer);

        verifyZeroInteractions(apiClientRx);
    }

    @Test
    public void shouldNotFetchPlaylistTagsIfTagsCachedIsNotExpired() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.singletonList("tag")));
        when(tagStorage.isTagsCacheExpired()).thenReturn(false);
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        operations.popularPlaylistTags().subscribe(observer);

        verifyZeroInteractions(apiClientRx);
    }
}
