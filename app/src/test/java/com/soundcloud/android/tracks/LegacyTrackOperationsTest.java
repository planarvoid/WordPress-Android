package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class LegacyTrackOperationsTest {

    @Mock
    private ScModelManager modelManager;
    @Mock
    private TrackStorage trackStorage;
    @Mock
    private Observer observer;
    @Mock
    private PublicApiPlaylist playlist;
    @Mock
    private RxHttpClient httpClient;
    @Mock
    private SyncInitiator syncInitiator;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private LocalCollection syncState;

    private LegacyTrackOperations trackOperations;
    private PublicApiTrack track;

    @Before
    public void setUp() throws Exception {
        trackOperations = new LegacyTrackOperations(modelManager, trackStorage, syncInitiator, syncStateManager);
        track = new PublicApiTrack(1L);
        when(modelManager.cache(track, PublicApiResource.CacheUpdateMode.NONE)).thenReturn(track);
    }

    @Test
    public void shouldLoadTrackFromStorageAndEmitOnUIThreadForPlayback() {
        MockObservable loadFromStorageObservable = TestObservables.emptyObservable();
        when(trackStorage.getTrackAsync(1L)).thenReturn(loadFromStorageObservable);

        trackOperations.loadTrack(1L, Schedulers.immediate()).subscribe(observer);

        expect(loadFromStorageObservable.subscribedTo()).toBeTrue();
    }

    @Test
    @Ignore("Revisit this after removing model manager")
    public void loadTrackShouldReplaceNullTrackForDummy() {
        Observable<PublicApiTrack> observable = Observable.just(null);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);

        trackOperations.loadTrack(1L, Schedulers.immediate()).subscribe(observer);

        verify(observer).onNext(eq(new PublicApiTrack(1L)));
    }

    @Test
    public void loadTrackShouldCacheLoadedTrack() {
        Observable<PublicApiTrack> observable = Observable.just(track);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        Observer<PublicApiTrack> observer = mock(Observer.class);
        trackOperations.loadTrack(1L, Schedulers.immediate()).subscribe(observer);

        verify(modelManager).cache(track, PublicApiResource.CacheUpdateMode.NONE);
    }

    @Test
    public void loadSyncedTrackShouldSyncTrackWhenNotPresentInLocalStorage() throws Exception {
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(
                Observable.<PublicApiTrack>error(new NotFoundException(track.getId())), Observable.just(track));
        when(syncInitiator.syncTrack(track.getUrn())).thenReturn(Observable.just(true));

        trackOperations.loadSyncedTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncTrack(track.getUrn());
        callbacks.verify(observer).onNext(track);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadSyncedTrackShouldForwardErrorsFromLocalStorage() throws Exception {
        Exception exception = new Exception();
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.<PublicApiTrack>error(exception));

        trackOperations.loadSyncedTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        verify(observer).onError(exception);
        verifyNoMoreInteractions(observer);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadSyncedTrackShouldEmitTrackImmediatelyFromStorageIfTrackUpToDate() {
        when(syncState.isSyncDue()).thenReturn(false);
        when(syncStateManager.fromContent(track.toUri())).thenReturn(syncState);
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.just(track));

        trackOperations.loadSyncedTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        InOrder callbacks = inOrder(observer);
        callbacks.verify(observer).onNext(track);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadSyncedTrackShouldNotSyncIfTrackUpToDate() {
        when(syncState.isSyncDue()).thenReturn(false);
        when(syncStateManager.fromContent(track.toUri())).thenReturn(syncState);
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.just(track));

        trackOperations.loadSyncedTrack(track.getId(), Schedulers.immediate()).subscribe(observer);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadSyncedTrackShouldEmitLocalTrackThenSyncedTrackIfLocalTrackTrackExistsButNeedsSyncing() {
        when(syncState.isSyncDue()).thenReturn(true);
        when(syncStateManager.fromContent(track.toUri())).thenReturn(syncState);
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.just(track));
        when(syncInitiator.syncTrack(track.getUrn())).thenReturn(Observable.just(true));

        trackOperations.loadSyncedTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncTrack(track.getUrn());
        callbacks.verify(observer, times(2)).onNext(track);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }


    public void loadStreamableTrackShouldSyncTrackWhenNotPresentInLocalStorage() throws Exception {
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(
                Observable.<PublicApiTrack>error(new NotFoundException(track.getId())), Observable.just(track));

        trackOperations.loadStreamableTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncTrack(track.getUrn());
        callbacks.verify(observer).onNext(track);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadStreamableTrackShouldForwardErrorsFromLocalStorage() throws Exception {
        Exception exception = new Exception();
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.<PublicApiTrack>error(exception));

        trackOperations.loadStreamableTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        verify(observer).onError(exception);
        verifyNoMoreInteractions(observer);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadStreamableTrackShouldEmitTrackImmediatelyFromStorageIfTrackStreamable() {
        track.stream_url = "asdf";
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.just(track));

        trackOperations.loadStreamableTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        InOrder callbacks = inOrder(observer);
        callbacks.verify(observer).onNext(track);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadStreamableTrackShouldNotSyncIfTrackStreamable() {
        track.stream_url = "asdf";
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.just(track));
        trackOperations.loadStreamableTrack(track.getId(), Schedulers.immediate()).subscribe(observer);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadTrackShouldTriggerSyncIfTrackExistsButIsNotStreamable() {
        when(trackStorage.getTrackAsync(track.getId())).thenReturn(Observable.just(track));
        when(syncInitiator.syncTrack(track.getUrn())).thenReturn(Observable.just(true));

        trackOperations.loadStreamableTrack(track.getId(), Schedulers.immediate()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncTrack(track.getUrn());
        callbacks.verify(observer).onNext(track);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }
}
