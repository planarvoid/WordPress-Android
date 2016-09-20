package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.TxnResult;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class PlayQueueOperationsTest extends AndroidUnitTest {

    private static final String ORIGIN_PAGE = "origin:page";
    private static final long PLAYLIST_ID = 123L;
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(PLAYLIST_ID);

    private PlayQueueOperations playQueueOperations;

    @Mock private Context context;
    @Mock private PlayQueueStorage playQueueStorage;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private PlayQueue playQueue;
    @Mock private ApiClientRx apiClientRx;
    @Mock private Observer observer;

    private PlaySessionSource playSessionSource;

    @Before
    public void before() throws CreateModelException {
        when(context.getSharedPreferences(PlayQueueOperations.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)).thenReturn(
                sharedPreferences);

        playQueueOperations = new PlayQueueOperations(context,
                                                      playQueueStorage,
                                                      storeTracksCommand,
                                                      apiClientRx,
                                                      Schedulers.immediate());

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
        when(playQueueStorage.storeAsync(any(PlayQueue.class))).thenReturn(Observable.<TxnResult>empty());
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG), anyString())).thenReturn(
                "origin:page");
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_COLLECTION_URN),
                                         anyString())).thenReturn(Urn.forPlaylist(123L).toString());
        when(sharedPreferences.getString(eq(PlaySessionSource.PREF_KEY_COLLECTION_OWNER_URN), anyString())).thenReturn(
                Urn.forUser(123L).toString());
        when(sharedPreferences.getInt(eq(PlayQueueOperations.Keys.PLAY_POSITION.name()), anyInt())).thenReturn(1);

        playSessionSource = PlaySessionSource.forPlaylist(ORIGIN_PAGE, PLAYLIST_URN, Urn.forUser(2), 5);
    }

    @Test
    public void shouldReturnLastPlaySessionSourceFromPreferences() throws Exception {
        PlaySessionSource playSessionSource = playQueueOperations.getLastStoredPlaySessionSource();
        assertThat(playSessionSource.getOriginScreen()).isEqualTo(ORIGIN_PAGE);
        assertThat(playSessionSource.getCollectionUrn()).isEqualTo(Urn.forPlaylist(PLAYLIST_ID));

    }

    @Test
    public void shouldLoadAPreviouslyStoredPlayQueue() throws Exception {
        when(sharedPreferences.contains(PlayQueueOperations.Keys.PLAY_POSITION.name())).thenReturn(true);
        PlayQueueItem playQueueItem = new TrackQueueItem.Builder(Urn.forTrack(123L))
                .fromSource("source1", "version1")
                .withPlaybackContext(PlaybackContext.create(playSessionSource))
                .build();

        Observable<PlayQueueItem> itemObservable = Observable.just(playQueueItem);
        when(playQueueStorage.loadAsync()).thenReturn(itemObservable);

        ArgumentCaptor<PlayQueue> captor = ArgumentCaptor.forClass(PlayQueue.class);
        playQueueOperations.getLastStoredPlayQueue().subscribe(observer);
        verify(observer).onNext(captor.capture());
        assertThat(captor.getValue()).containsExactly(playQueueItem);
    }

    @Test
    public void shouldReturnEmptyObservableIfStoredPlayQueueIsEmpty() throws Exception {
        final TestSubscriber<PlayQueue> subscriber = new TestSubscriber<>();

        when(playQueueStorage.loadAsync()).thenReturn(Observable.<PlayQueueItem>empty());
        playQueueOperations.getLastStoredPlayQueue().subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
        assertThat(subscriber.getOnCompletedEvents()).hasSize(1);
    }

    @Test
    public void shouldCreateQueueFromItemsObservable() throws Exception {
        when(sharedPreferences.contains(PlayQueueOperations.Keys.PLAY_POSITION.name())).thenReturn(true);
        PlayQueueItem playQueueItem1 = new TrackQueueItem.Builder(Urn.forTrack(1L))
                .fromSource("source1", "version1")
                .withPlaybackContext(PlaybackContext.create(playSessionSource))
                .build();
        PlayQueueItem playQueueItem2 = new TrackQueueItem.Builder(Urn.forTrack(2L))
                .fromSource("source2", "version2")
                .withPlaybackContext(PlaybackContext.create(playSessionSource))
                .build();
        Observable<PlayQueueItem> itemObservable = Observable.from(Arrays.asList(playQueueItem1, playQueueItem2));

        when(playQueueStorage.loadAsync()).thenReturn(itemObservable);

        PlayQueue playQueue = playQueueOperations.getLastStoredPlayQueue().toBlocking().lastOrDefault(null);
        assertThat(playQueue).containsExactly(playQueueItem1, playQueueItem2);
    }

    @Test
    public void shouldReturnEmptyQueueWhenPlayingTrackNotPresent() throws Exception {
        PlayQueueItem playQueueItem1 = new TrackQueueItem.Builder(Urn.forTrack(1L))
                .fromSource("source1", "version1")
                .withPlaybackContext(PlaybackContext.create(playSessionSource))
                .build();
        Observable<PlayQueueItem> itemObservable = Observable.from(Arrays.asList(playQueueItem1));

        when(playQueueStorage.loadAsync()).thenReturn(itemObservable);

        assertThat(playQueueOperations.getLastStoredPlayQueue()).isEqualTo(Observable.empty());
    }

    @Test
    public void shouldReturnEmptyObservableWhenReloadingWithNoValidStoredLastTrack() throws Exception {
        assertThat(playQueueOperations.getLastStoredPlayQueue()).isEqualTo(Observable.empty());
    }

    @Test
    public void savePositionInfoShouldWritePlayQueueMetaDataToPreferences() throws Exception {
        playQueueOperations.savePlayInfo(8, playSessionSource);
        verify(sharedPreferencesEditor).putInt(PlayQueueOperations.Keys.PLAY_POSITION.name(), 8);
        verify(sharedPreferencesEditor).putString(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG, ORIGIN_PAGE);
        verify(sharedPreferencesEditor).putString(PlaySessionSource.PREF_KEY_COLLECTION_URN, PLAYLIST_URN.toString());
    }

    @Test
    public void saveShouldStoreAllPlayQueueItems() throws Exception {
        final PublishSubject<TxnResult> subject = PublishSubject.create();
        when(playQueueStorage.storeAsync(playQueue)).thenReturn(subject);

        playQueueOperations.saveQueue(playQueue);
        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void clearShouldRemovePreferencesAndDeleteFromDatabase() throws Exception {
        when(playQueueStorage.clearAsync()).thenReturn(Observable.<ChangeResult>empty());
        playQueueOperations.clear();
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.PLAY_POSITION.name());
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_COLLECTION_URN);
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG);
        verify(playQueueStorage).clearAsync();
    }

    @Test
    public void getRelatedTracksShouldMakeGetRequestToRelatedTracksEndpoint() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Observable.<RecommendedTracksCollection>empty());
        playQueueOperations.relatedTracks(Urn.forTrack(123), true).subscribe(observer);

        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture(), eq(RecommendedTracksCollection.class));
        assertThat(argumentCaptor.getValue().getMethod()).isEqualTo("GET");
        assertThat(argumentCaptor.getValue().getQueryParameters().get("continuous_play")).containsExactly("true");
        assertThat(argumentCaptor.getValue().getEncodedPath()).isEqualTo(ApiEndpoints.RELATED_TRACKS.path(Urn.forTrack(
                123L).toString()));
    }

    @Test
    public void getRelatedTracksShouldEmitTracksFromSuggestions() throws CreateModelException {
        Observer<ModelCollection<ApiTrack>> relatedObserver = mock(Observer.class);

        ApiTrack suggestion1 = ModelFixtures.create(ApiTrack.class);
        ApiTrack suggestion2 = ModelFixtures.create(ApiTrack.class);
        RecommendedTracksCollection collection = createCollection(suggestion1, suggestion2);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Observable.just(collection));

        playQueueOperations.relatedTracks(Urn.forTrack(123), false).subscribe(relatedObserver);

        ArgumentCaptor<ModelCollection> argumentCaptor = ArgumentCaptor.forClass(ModelCollection.class);
        verify(relatedObserver).onNext(argumentCaptor.capture());
        Iterator iterator = argumentCaptor.getValue().iterator();
        assertThat(iterator.next()).isEqualTo(suggestion1);
        assertThat(iterator.next()).isEqualTo(suggestion2);
        verify(relatedObserver).onCompleted();
        verify(relatedObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void getRelatedTracksPlayQueueShouldReturnAnEmptyPlayQueueNoRelatedTracksReceivedFromApi() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Observable.just(new RecommendedTracksCollection(Collections.<ApiTrack>emptyList(),
                                                                            "version")));

        TestSubscriber<PlayQueue> testSubscriber = new TestSubscriber<>();
        playQueueOperations.relatedTracksPlayQueue(Urn.forTrack(123), false, playSessionSource).subscribe(testSubscriber);

        testSubscriber.assertValues(PlayQueue.empty());
    }

    @Test
    public void shouldWriteRelatedTracksInLocalStorage() throws Exception {
        RecommendedTracksCollection collection = createCollection(ModelFixtures.create(ApiTrack.class));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Observable.just(collection));

        playQueueOperations.relatedTracks(Urn.forTrack(1), false).subscribe(observer);

        verify(storeTracksCommand).call(collection);
    }

    private RecommendedTracksCollection createCollection(ApiTrack... suggestions) {
        return new RecommendedTracksCollection(Arrays.asList(suggestions), "version");
    }

}
