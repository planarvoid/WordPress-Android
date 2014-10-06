package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.matchers.ApiRequestTo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueOperationsTest {

    private static final String ORIGIN_PAGE = "origin:page";
    private static final long PLAYLIST_ID = 123L;

    private PlayQueueOperations playQueueOperations;

    @Mock private Context context;
    @Mock private PlayQueueStorage playQueueStorage;
    @Mock private TrackWriteStorage trackWriteStorage;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private PlayQueue playQueue;
    @Mock private RxHttpClient rxHttpClient;
    @Mock private Observer observer;

    private PlaySessionSource playSessionSource;
    private PublicApiPlaylist playlist;

    @Before
    public void before() throws CreateModelException {
        when(context.getSharedPreferences(PlayQueueOperations.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)).thenReturn(sharedPreferences);

        playQueueOperations = new PlayQueueOperations(context, playQueueStorage, trackWriteStorage, rxHttpClient);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueueStorage.storeAsync(any(PlayQueue.class))).thenReturn(Observable.<TxnResult>empty());
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG), anyString())).thenReturn("origin:page");
        when(sharedPreferences.getLong(eq(PlaySessionSource.PREF_KEY_PLAYLIST_ID), anyLong())).thenReturn(123L);
        when(sharedPreferences.getInt(eq(PlayQueueOperations.Keys.PLAY_POSITION.name()), anyInt())).thenReturn(1);

        playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playlist = ModelFixtures.create(PublicApiPlaylist.class);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());
    }

    @Test
    public void shouldReturnLastPlaySessionSourceFromPreferences() throws Exception {
        PlaySessionSource playSessionSource = playQueueOperations.getLastStoredPlaySessionSource();
        expect(playSessionSource.getOriginScreen()).toEqual(ORIGIN_PAGE);
        expect(playSessionSource.getPlaylistUrn()).toEqual(Urn.forPlaylist(PLAYLIST_ID));

    }

    @Test
    public void shouldLoadAPreviouslyStoredPlayQueue() throws Exception {
        PlayQueueItem playQueueItem = PlayQueueItem.fromTrack(Urn.forTrack(1L), "source1", "version1");
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(123L);

        Observable<PlayQueueItem> itemObservable = Observable.just(playQueueItem);
        when(playQueueStorage.loadAsync()).thenReturn(itemObservable);

        ArgumentCaptor<PlayQueue> captor = ArgumentCaptor.forClass(PlayQueue.class);
        playQueueOperations.getLastStoredPlayQueue().subscribe(observer);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue()).toContainExactly(playQueueItem);
    }

    @Test
    public void shouldCreateQueueFromItemsObservable() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(123L);

        PlayQueueItem playQueueItem1 = PlayQueueItem.fromTrack(Urn.forTrack(1L), "source1", "version1");
        PlayQueueItem playQueueItem2 = PlayQueueItem.fromTrack(Urn.forTrack(2L), "source2", "version2");
        Observable<PlayQueueItem> itemObservable = Observable.from(Lists.newArrayList(playQueueItem1, playQueueItem2));

        when(playQueueStorage.loadAsync()).thenReturn(itemObservable);

        PlayQueue playQueue = playQueueOperations.getLastStoredPlayQueue().toBlockingObservable().lastOrDefault(null);
        expect(playQueue).toContainExactly(playQueueItem1, playQueueItem2);
    }

    @Test
    public void shouldReturnNullWhenReloadingWithNoValidStoredLastTrack() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(-1L);
        verifyZeroInteractions(playQueueStorage);
        expect(playQueueOperations.getLastStoredPlayQueue()).toBeNull();
    }

    @Test
    public void savePositionInfoShouldWritePlayQueueMetaDataToPreferences() throws Exception {
        playQueueOperations.savePositionInfo(8, Urn.forTrack(456), playSessionSource, 200L);
        verify(sharedPreferencesEditor).putLong(PlayQueueOperations.Keys.SEEK_POSITION.name(), 200L);
        verify(sharedPreferencesEditor).putLong(PlayQueueOperations.Keys.TRACK_ID.name(), 456L);
        verify(sharedPreferencesEditor).putInt(PlayQueueOperations.Keys.PLAY_POSITION.name(), 8);
        verify(sharedPreferencesEditor).putString(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG, ORIGIN_PAGE);
        verify(sharedPreferencesEditor).putLong(PlaySessionSource.PREF_KEY_PLAYLIST_ID, playlist.getId());
    }

    @Test
    public void saveShouldStoreAllPlayQueueItems() throws Exception {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(playQueueStorage.storeAsync(playQueue)).thenReturn(observable);

        playQueueOperations.saveQueue(playQueue);

        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void clearShouldRemovePreferencesAndDeleteFromDatabase() throws Exception {
        when(playQueueStorage.clearAsync()).thenReturn(Observable.<ChangeResult>empty());
        playQueueOperations.clear();
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.SEEK_POSITION.name());
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.TRACK_ID.name());
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.PLAY_POSITION.name());
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_PLAYLIST_ID);
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG);
        verify(playQueueStorage).clearAsync();
    }

    @Test
    public void getRelatedTracksShouldMakeGetRequestToRelatedTracksEndpoint() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        playQueueOperations.getRelatedTracks(Urn.forTrack(123)).subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
        expect(argumentCaptor.getValue().getEncodedPath()).toEqual(APIEndpoints.RELATED_TRACKS.path(Urn.forTrack(123L).toString()));
    }

    @Test
    public void getRelatedTracksShouldEmitTracksFromSuggestions() throws CreateModelException {
        Observer<ModelCollection<ApiTrack>> relatedObserver = mock(Observer.class);

        ApiTrack suggestion1 = ModelFixtures.create(ApiTrack.class);
        ApiTrack suggestion2 = ModelFixtures.create(ApiTrack.class);
        RecommendedTracksCollection collection = createCollection(suggestion1, suggestion2);

        when(rxHttpClient.<RecommendedTracksCollection>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(collection));
        when(trackWriteStorage.storeTracksAsync(anyCollection())).thenReturn(Observable.<TxnResult>empty());

        playQueueOperations.getRelatedTracks(Urn.forTrack(123)).subscribe(relatedObserver);

        ArgumentCaptor<ModelCollection> argumentCaptor = ArgumentCaptor.forClass(ModelCollection.class);
        verify(relatedObserver).onNext(argumentCaptor.capture());
        Iterator iterator = argumentCaptor.getValue().iterator();
        expect(iterator.next()).toEqual(suggestion1);
        expect(iterator.next()).toEqual(suggestion2);
        verify(relatedObserver).onCompleted();
        verify(relatedObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void shouldWriteRelatedTracksInLocalStorage() throws Exception {
        RecommendedTracksCollection collection = createCollection(
                ModelFixtures.create(ApiTrack.class));
        when(rxHttpClient.<RecommendedTracksCollection>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(collection));

        playQueueOperations.getRelatedTracks(Urn.forTrack(1)).subscribe(observer);

        final List<ApiTrack> resources = Arrays.asList(collection.getCollection().get(0));
        verify(trackWriteStorage).storeTracksAsync(resources);
    }

    @Test
    public void fetchAndStorePoliciesMakeGetRequestToRelatedTracksEndpoint() {
        ModelCollection returnCollection = new ModelCollection();

        final ApiRequestTo expectedRequest = isMobileApiRequestTo("POST", APIEndpoints.POLICIES.path());
        expectedRequest.withContent(Lists.newArrayList("soundcloud:tracks:123"));

        when(rxHttpClient.fetchModels(argThat(expectedRequest))).thenReturn(Observable.<Object>just(returnCollection));
        when(trackWriteStorage.storePoliciesAsync(anyCollection())).thenReturn(Observable.<TxnResult>empty());
        final ModelCollection<PolicyInfo> first = playQueueOperations.fetchAndStorePolicies(Lists.newArrayList(Urn.forTrack(123))).toBlocking().first();
        expect(first).toBe(returnCollection);
    }

    private RecommendedTracksCollection createCollection(ApiTrack... suggestions) {
        final RecommendedTracksCollection collection = new RecommendedTracksCollection();
        collection.setCollection(Lists.newArrayList(suggestions));
        return collection;
    }

}
