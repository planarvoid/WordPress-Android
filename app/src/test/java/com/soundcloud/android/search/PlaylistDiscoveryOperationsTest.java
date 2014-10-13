package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistWriteStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistDiscoveryOperationsTest {

    @InjectMocks PlaylistDiscoveryOperations operations;

    @Mock PlaylistTagStorage tagStorage;
    @Mock RxHttpClient rxHttpClient;
    @Mock Observer observer;
    @Mock PlaylistWriteStorage playlistWriteStorage;

    @Before
    public void setup() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
    }

    @Test
    public void shouldMakeGETRequestToPlaylistTagsEndpoint() throws Exception {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        operations.popularPlaylistTags().subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", APIEndpoints.PLAYLIST_DISCOVERY_TAGS.path())));
    }

    @Test
    public void storesPopularTagsWhenRequestIsSuccessful() {
        ModelCollection<String> tags = new ModelCollection<>(Lists.newArrayList("tag"));
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(rxHttpClient.<ModelCollection<String>>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(tags));

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage).cachePopularTags(tags.getCollection());
    }

    @Test
    public void doesNotStorePopularTagsWhenRequestFails() {
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(Collections.<String>emptyList()));
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.error(new Exception()));

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage, never()).cachePopularTags(anyList());
    }

    @Test
    public void loadsPopularTagsFromCacheIfStored() {
        List<String> tags = Lists.newArrayList("tag");
        when(tagStorage.getPopularTagsAsync()).thenReturn(Observable.just(tags));

        operations.popularPlaylistTags().subscribe(observer);

        verify(tagStorage).getPopularTagsAsync();
        verifyZeroInteractions(rxHttpClient);
    }

    @Test
    public void shouldMakeGETRequestToPlaylistDiscoveryEndpoint() {
        operations.playlistsForTag("electronic").subscribe(observer);

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET",
                APIEndpoints.PLAYLIST_DISCOVERY.path())));
    }

    @Test
    public void shouldMakeRequestPlaylistDiscoveryResultsWithCorrectParameters() {
        operations.playlistsForTag("electronic").subscribe(observer);

        Multimap<String, String> parameters = ArrayListMultimap.create();
        parameters.put("tag", "electronic");

        ArgumentCaptor<APIRequest> resultCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchModels(resultCaptor.capture());
        expect(resultCaptor.getValue().getQueryParameters()).toEqual(parameters);
    }

    @Test
    public void shouldDeliverPlaylistDiscoveryResultsToObserver() throws CreateModelException {
        ModelCollection<ApiPlaylist> collection = buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(observer).onNext(collection);
    }

    @Test
    public void shouldWritePlaylistDiscoveryResultToLocalStorage() throws CreateModelException {
        ModelCollection<ApiPlaylist> collection = buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(playlistWriteStorage).storePlaylists(collection.getCollection());
    }

    private ModelCollection<ApiPlaylist> buildPlaylistSummariesResponse() throws CreateModelException {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        ModelCollection<ApiPlaylist> collection = new ModelCollection<>();
        collection.setCollection(Arrays.asList(playlist));
        when(rxHttpClient.<ModelCollection<ApiPlaylist>>fetchModels(any(APIRequest.class))).thenReturn(
                Observable.<ModelCollection<ApiPlaylist>>from(collection));
        return collection;
    }

    @Test
    public void shouldPrependSearchedTagToPlaylistTags() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        expect(apiPlaylist.getTags()).toContainExactly("electronic", "tag1", "tag2", "tag3");
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
        expect(apiPlaylist.getTags()).toContainExactly("tag2", "tag1", "tag3");
    }

    @Test
    public void shouldReorderTagListIfPlaylistTagSearchedWithDifferentCaseAlreadyExists() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("Tag2").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        expect(apiPlaylist.getTags()).toContainExactly("Tag2", "tag1", "tag3");
    }

    @Test
    public void shouldNotReorderTagListIfSearchedTagIsSubsetOfAnExistingTag() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("ag2").subscribe(observer);

        ApiPlaylist apiPlaylist = captureFirstApiPlaylist();
        expect(apiPlaylist.getTags()).toContainExactly("ag2", "tag1", "tag2", "tag3");
    }

    @Test
    public void addsSearchedTagToRecentTagsStorage() throws CreateModelException {
        buildPlaylistSummariesResponse();

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(tagStorage).addRecentTag(eq("electronic"));
    }

    @Test
    public void addsSearchedTagToRecentTagsStorageWhenRequestFails() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.error(new Exception()));

        operations.playlistsForTag("electronic").subscribe(observer);

        verify(tagStorage).addRecentTag(eq("electronic"));
    }

}