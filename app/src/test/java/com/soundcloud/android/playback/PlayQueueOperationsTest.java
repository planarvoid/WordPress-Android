package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;

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
    @Mock private ApiClientRxV2 apiClientRx;

    private PlaySessionSource playSessionSource;

    @Before
    public void before() throws CreateModelException {
        when(context.getSharedPreferences(PlayQueueOperations.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)).thenReturn(
                sharedPreferences);

        playQueueOperations = new PlayQueueOperations(context,
                                                      playQueueStorage,
                                                      storeTracksCommand,
                                                      apiClientRx,
                                                      Schedulers.trampoline());

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putString(anyString(), anyString())).thenReturn(sharedPreferencesEditor);
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

        when(playQueueStorage.load()).thenReturn(Single.just(Collections.singletonList(playQueueItem)));

        TestObserver<PlayQueue> testObserver = playQueueOperations.getLastStoredPlayQueue().test();
        testObserver.assertValueCount(1);
        testObserver.assertValue(PlayQueue.fromPlayQueueItems(Collections.singletonList(playQueueItem)));
    }

    @Test
    public void shouldReturnEmptyObservableIfStoredPlayQueueIsEmpty() throws Exception {
        when(playQueueStorage.load()).thenReturn(Single.never());
        TestObserver<PlayQueue> testObserver = playQueueOperations.getLastStoredPlayQueue().test();

        testObserver.assertValueCount(0);
        testObserver.assertTerminated();
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

        when(playQueueStorage.load()).thenReturn(Single.just(Arrays.asList(playQueueItem1, playQueueItem2)));

        PlayQueue playQueue = playQueueOperations.getLastStoredPlayQueue().test().values().get(0);
        assertThat(playQueue).containsExactly(playQueueItem1, playQueueItem2);
    }

    @Test
    public void shouldReturnEmptyQueueWhenPlayingTrackNotPresent() throws Exception {
        PlayQueueItem playQueueItem1 = new TrackQueueItem.Builder(Urn.forTrack(1L))
                .fromSource("source1", "version1")
                .withPlaybackContext(PlaybackContext.create(playSessionSource))
                .build();

        when(playQueueStorage.load()).thenReturn(Single.just(Collections.singletonList(playQueueItem1)));

        assertThat(playQueueOperations.getLastStoredPlayQueue()).isEqualTo(Maybe.empty());
    }

    @Test
    public void shouldReturnEmptyObservableWhenReloadingWithNoValidStoredLastTrack() throws Exception {
        assertThat(playQueueOperations.getLastStoredPlayQueue()).isEqualTo(Maybe.empty());
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
        playQueueOperations.saveQueue(playQueue).test();

        verify(playQueueStorage).store(playQueue);
    }

    @Test
    public void clearShouldRemovePreferencesAndDeleteFromDatabase() throws Exception {
        when(playQueueStorage.clear()).thenReturn(Completable.complete());
        playQueueOperations.clear();
        verify(sharedPreferencesEditor).remove(PlayQueueOperations.Keys.PLAY_POSITION.name());
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_COLLECTION_URN);
        verify(sharedPreferencesEditor).remove(PlaySessionSource.PREF_KEY_ORIGIN_SCREEN_TAG);
        verify(playQueueStorage).clear();
    }

    @Test
    public void getRelatedTracksShouldMakeGetRequestToRelatedTracksEndpoint() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Single.never());
        playQueueOperations.relatedTracks(Urn.forTrack(123), true).test();

        ArgumentCaptor<ApiRequest> argumentCaptor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(argumentCaptor.capture(), eq(RecommendedTracksCollection.class));
        assertThat(argumentCaptor.getValue().getMethod()).isEqualTo("GET");
        assertThat(argumentCaptor.getValue().getQueryParameters().get("continuous_play")).containsExactly("true");
        assertThat(argumentCaptor.getValue().getEncodedPath()).isEqualTo(ApiEndpoints.RELATED_TRACKS.path(Urn.forTrack(123L).toString()));
    }

    @Test
    public void getRelatedTracksShouldEmitTracksFromSuggestions() throws CreateModelException {
        ApiTrack suggestion1 = ModelFixtures.create(ApiTrack.class);
        ApiTrack suggestion2 = ModelFixtures.create(ApiTrack.class);
        RecommendedTracksCollection collection = createCollection(suggestion1, suggestion2);

        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Single.just(collection));

        TestObserver<RecommendedTracksCollection> testObserver = playQueueOperations.relatedTracks(Urn.forTrack(123), false).test();

        testObserver.assertValueCount(1);
        testObserver.assertValue(collection);
        testObserver.assertTerminated();
        testObserver.assertNoErrors();
    }

    @Test
    public void getRelatedTracksPlayQueueShouldReturnAnEmptyPlayQueueNoRelatedTracksReceivedFromApi() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class)))
                .thenReturn(Single.just(new RecommendedTracksCollection(Collections.emptyList(), "version")));

        TestObserver<PlayQueue> testObserver = playQueueOperations.relatedTracksPlayQueue(Urn.forTrack(123), false, playSessionSource).test();

        testObserver.assertValue(PlayQueue.empty());
    }

    @Test
    public void shouldWriteRelatedTracksInLocalStorage() throws Exception {
        RecommendedTracksCollection collection = createCollection(ModelFixtures.create(ApiTrack.class));
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(RecommendedTracksCollection.class))).thenReturn(Single.just(collection));

        playQueueOperations.relatedTracks(Urn.forTrack(1), false).test();

        verify(storeTracksCommand).call(collection);
    }

    private RecommendedTracksCollection createCollection(ApiTrack... suggestions) {
        return new RecommendedTracksCollection(Arrays.asList(suggestions), "version");
    }

}
