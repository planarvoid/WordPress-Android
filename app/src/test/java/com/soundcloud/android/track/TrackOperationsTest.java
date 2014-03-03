package com.soundcloud.android.track;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.TrackStorage;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class TrackOperationsTest {

    @Mock
    private ScModelManager modelManager;
    @Mock
    private TrackStorage trackStorage;
    @Mock
    private Observer observer;
    @Mock
    private Playlist playlist;
    @Mock
    private RxHttpClient httpClient;

    private TrackOperations trackOperations;
    private Track track;

    @Before
    public void setUp() throws Exception {
        trackOperations = new TrackOperations(modelManager, trackStorage, httpClient);
        track = TestHelper.getModelFactory().createModel(Track.class);
    }

    @Test
    public void shouldLoadTrackFromCacheAndEmitOnUIThreadForPlayback() {
        when(modelManager.getCachedTrack(1L)).thenReturn(track);
        expect(trackOperations.loadTrack(1L, AndroidSchedulers.mainThread()).toBlockingObservable().last()).toBe(track);
        verifyZeroInteractions(trackStorage);
    }

    @Test
    public void shouldLoadTrackFromStorageAndEmitOnUIThreadForPlayback() {
        TestObservables.MockObservable loadFromStorageObservable = TestObservables.emptyObservable();
        when(trackStorage.getTrackAsync(1L)).thenReturn(loadFromStorageObservable);

        trackOperations.loadTrack(1L, AndroidSchedulers.mainThread()).subscribe(observer);

        expect(loadFromStorageObservable.subscribedTo()).toBeTrue();
    }

    @Test
    @Ignore("Revisit this after removing model manager")
    public void loadTrackShouldReplaceNullTrackForDummy() {
        Observable<Track> observable = Observable.just(null);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        when(modelManager.cache(track, ScResource.CacheUpdateMode.NONE)).thenReturn(track);

        trackOperations.loadTrack(1L, AndroidSchedulers.mainThread()).subscribe(observer);

        verify(observer).onNext(eq(new Track(1L)));
    }

    @Test
    public void loadTrackShouldCacheLoadedTrack() {
        Observable<Track> observable = Observable.just(track);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        Observer<Track> observer = mock(Observer.class);
        trackOperations.loadTrack(1L, AndroidSchedulers.mainThread()).subscribe(observer);

        verify(modelManager).cache(track, ScResource.CacheUpdateMode.NONE);
    }

    @Test
    public void loadCompleteTrackShouldCallOnNextOnceWithCompleteTrack() {
        Track track = Mockito.mock(Track.class);
        Observable<Track> observable = Observable.just(track);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        when(modelManager.cache(track, ScResource.CacheUpdateMode.NONE)).thenReturn(track);

        trackOperations.loadCompleteTrack(mock(Activity.class), 1L).subscribe(observer);

        verify(observer).onNext(track);
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void loadCompleteTrackShouldReturnIncompleteTrack() {
        Track incompleteTrack = Mockito.mock(Track.class);
        when(incompleteTrack.isIncomplete()).thenReturn(true);

        when(trackStorage.getTrackAsync(1L)).thenReturn(Observable.just(incompleteTrack));
        when(modelManager.cache(incompleteTrack, ScResource.CacheUpdateMode.NONE)).thenReturn(incompleteTrack);

        // to avoid NPE
        Observable apiObservable = Observable.just(Mockito.mock(Track.class));
        when(httpClient.fetchModels(any(APIRequest.class))).thenReturn(apiObservable);

        trackOperations.loadCompleteTrack(mock(Activity.class), 1L).subscribe(observer);

        verify(modelManager).cache(incompleteTrack, ScResource.CacheUpdateMode.NONE);
        verify(observer).onNext(incompleteTrack);
    }

    @Test
    public void loadCompleteTrackShouldReturnCompleteTrackFromApi() {
        Track incompleteTrack = Mockito.mock(Track.class);
        when(incompleteTrack.isIncomplete()).thenReturn(true);

        Track completedTrack = Mockito.mock(Track.class);

        when(trackStorage.getTrackAsync(1L)).thenReturn(Observable.just(incompleteTrack));
        when(modelManager.cache(incompleteTrack, ScResource.CacheUpdateMode.NONE)).thenReturn(incompleteTrack);

        Observable apiObservable = Observable.just(completedTrack);
        when(httpClient.fetchModels(any(APIRequest.class))).thenReturn(apiObservable);
        when(modelManager.cache(completedTrack, ScResource.CacheUpdateMode.FULL)).thenReturn(completedTrack);

        trackOperations.loadCompleteTrack(mock(Activity.class), 1L).subscribe(observer);

        verify(modelManager).cache(incompleteTrack, ScResource.CacheUpdateMode.NONE);
        verify(modelManager).cache(completedTrack, ScResource.CacheUpdateMode.FULL);
        verify(trackStorage).createOrUpdate(completedTrack);
        verify(observer).onNext(completedTrack);
    }

    @Test
    public void loadCompleteTrackShouldMakeTrackDetailsRequest(){
        Track incompleteTrack = Mockito.mock(Track.class);
        when(incompleteTrack.isIncomplete()).thenReturn(true);

        when(trackStorage.getTrackAsync(1L)).thenReturn(Observable.just(incompleteTrack));
        when(modelManager.cache(incompleteTrack, ScResource.CacheUpdateMode.NONE)).thenReturn(incompleteTrack);

        Observable apiObservable = Observable.just(Mockito.mock(Track.class));
        when(httpClient.fetchModels(any(APIRequest.class))).thenReturn(apiObservable);

        trackOperations.loadCompleteTrack(mock(Activity.class), 1L).subscribe(observer);

        verify(httpClient).fetchModels(argThat(isPublicApiRequestTo("GET", "/tracks/1")));
    }

}
