package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.tobedevoured.modelcitizen.CreateModelException;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueOperationsTest {

    private static final String ORIGIN_PAGE = "origin:page";
    private static final long SET_ID = 123L;

    @Inject
    PlayQueueOperations playQueueOperations;
    @Mock
    private Context context;
    @Mock
    private PlayQueueStorage playQueueStorage;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock
    private PlayQueue playQueue;
    @Mock
    private RxHttpClient rxHttpClient;
    @Mock
    private Observer observer;

    private PlaySessionSource playSessionSource;
    private Playlist playlist;

    @Before
    public void before() throws CreateModelException {
        when(context.getSharedPreferences(PlayQueueOperations.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)).thenReturn(sharedPreferences);

        ObjectGraph.create(new TestModule()).inject(this);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueueStorage.storeCollectionAsync(any(Collection.class))).thenReturn(Observable.<Collection<PlayQueueItem>>empty());
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG), anyString())).thenReturn("origin:page");
        when(sharedPreferences.getLong(eq(PlaySessionSource.PREF_KEY_PLAYLIST_ID), anyLong())).thenReturn(123L);
        when(sharedPreferences.getInt(eq(PlayQueueOperations.Keys.PLAY_POSITION.name()), anyInt())).thenReturn(1);

        playSessionSource = new PlaySessionSource(ORIGIN_PAGE);
        playlist = TestHelper.getModelFactory().createModel(Playlist.class);
        playSessionSource.setPlaylist(playlist);
    }

    @Test
    public void shouldReturnLastPlaySessionSourceFromPreferences() throws Exception {
        PlaySessionSource playSessionSource = playQueueOperations.getLastStoredPlaySessionSource();
        expect(playSessionSource.getOriginScreen()).toEqual(ORIGIN_PAGE);
        expect(playSessionSource.getPlaylistId()).toEqual(SET_ID);

    }

    @Test
    public void shouldLoadAPreviouslyStoredPlayQueue() throws Exception {
        PlayQueueItem playQueueItem = new PlayQueueItem(1L, "source1", "version1");
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(123L);

        Observable<List<PlayQueueItem>> itemObservable = Observable.just(Arrays.asList(playQueueItem));
        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(itemObservable);

        ArgumentCaptor<PlayQueue> captor = ArgumentCaptor.forClass(PlayQueue.class);
        playQueueOperations.getLastStoredPlayQueue().subscribe(observer);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().getItems()).toContainExactly(playQueueItem);
    }

    @Test
    public void shouldCreateQueueFromItemsObservable() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(123L);

        PlayQueueItem playQueueItem1 = new PlayQueueItem(1L, "source1", "version1");
        PlayQueueItem playQueueItem2 = new PlayQueueItem(2L, "source2", "version2");
        List<PlayQueueItem> items = Lists.newArrayList(playQueueItem1, playQueueItem2);
        Observable<List<PlayQueueItem>> itemObservable = Observable.just(items);

        when(playQueueStorage.getPlayQueueItemsAsync()).thenReturn(itemObservable);

        PlayQueue playQueue = playQueueOperations.getLastStoredPlayQueue().toBlockingObservable().lastOrDefault(null);
        expect(playQueue.getItems()).toContainExactly(playQueueItem1, playQueueItem2);
        expect(playQueue.getPosition()).toEqual(1);
    }

    @Test
    public void shouldReturnNullWhenReloadingWithNoValidStoredLastTrack() throws Exception {
        when(sharedPreferences.getLong(eq(PlayQueueOperations.Keys.TRACK_ID.name()), anyLong())).thenReturn(-1L);
        verifyZeroInteractions(playQueueStorage);
        expect(playQueueOperations.getLastStoredPlayQueue()).toBeNull();
    }

    @Test
    public void saveShouldWritePlayQueueMetaDataToPreferences() throws Exception {
        when(playQueue.getCurrentTrackId()).thenReturn(123L);
        when(playQueue.getPosition()).thenReturn(4);

        expect(playQueueOperations.saveQueue(playQueue, playSessionSource, 200L)).not.toBeNull();
        verify(sharedPreferencesEditor).putLong(PlayQueueOperations.Keys.SEEK_POSITION.name(), 200L);
        verify(sharedPreferencesEditor).putLong(PlayQueueOperations.Keys.TRACK_ID.name(), 123L);
        verify(sharedPreferencesEditor).putInt(PlayQueueOperations.Keys.PLAY_POSITION.name(), 4);
        verify(sharedPreferencesEditor).putString(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG, ORIGIN_PAGE);
        verify(sharedPreferencesEditor).putLong(PlaySessionSource.PREF_KEY_PLAYLIST_ID, playlist.getId());
    }

    @Test
    public void saveShouldStoreAllPlayQueueItems() throws Exception {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        final Collection<PlayQueueItem> collection = Mockito.mock(Collection.class);
        when(playQueue.getItems()).thenReturn(collection);
        when(playQueueStorage.storeCollectionAsync(collection)).thenReturn(observable);

        playQueueOperations.saveQueue(playQueue, playSessionSource, 200L);

        expect(observable.subscribedTo()).toBeTrue();
    }

    @Test
    public void clearShouldRemovePreferences() throws Exception {
        playQueueOperations.clear();
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.SEEK_POSITION.name());
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.TRACK_ID.name());
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.PLAY_POSITION.name());
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_PLAYLIST_ID);
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG);
    }

    @Test
    public void getRelatedTracksShouldMakeGetRequestToRelatedTracksEndpoint() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        playQueueOperations.getRelatedTracks(123L).subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
        expect(argumentCaptor.getValue().getUriPath()).toEqual(String.format(APIEndpoints.RELATED_TRACKS.path(),
                Urn.forTrack(123L).toString()));
    }

    @Test
    public void getRelatedTracksShouldEmitTracksFromSuggestions() {

        Observer<ModelCollection<TrackSummary>> relatedObserver = Mockito.mock(Observer.class);

        final ModelCollection<TrackSummary> collection = new ModelCollection<TrackSummary>();
        final TrackSummary suggestion1 = new TrackSummary("soundcloud:sounds:1");
        suggestion1.setUser(new UserSummary());
        final TrackSummary suggestion2 = new TrackSummary("soundcloud:sounds:2");
        suggestion2.setUser(new UserSummary());
        final ArrayList<TrackSummary> collection1 = Lists.newArrayList(suggestion1, suggestion2);
        collection.setCollection(collection1);

        when(rxHttpClient.<ModelCollection<TrackSummary>>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(collection));
        playQueueOperations.getRelatedTracks(123L).subscribe(relatedObserver);

        ArgumentCaptor<ModelCollection> argumentCaptor = ArgumentCaptor.forClass(ModelCollection.class);
        verify(relatedObserver).onNext(argumentCaptor.capture());
        Iterator iterator = argumentCaptor.getValue().iterator();
        expect(iterator.next()).toEqual(suggestion1);
        expect(iterator.next()).toEqual(suggestion2);
        verify(relatedObserver).onCompleted();
        verify(relatedObserver, never()).onError(any(Throwable.class));

    }

    @Module(library = true, injects = PlayQueueOperationsTest.class)
    class TestModule {
        @Provides
        Context provideContext(){
            return context;
        }

        @Provides
        PlayQueueStorage providePlayQueueStorage(){
            return playQueueStorage;
        }

        @Provides
        RxHttpClient provideRxHttpClient(){
            return rxHttpClient;
        }

    }

}
