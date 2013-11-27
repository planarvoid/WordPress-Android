package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.PlaybackService.PlayExtras;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.Actions;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.UserSummary;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;
import rx.util.functions.Func1;

import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaybackOperationsTest {

    private PlaybackOperations playbackOperations;
    private Track track;

    @Mock
    private ScModelManager modelManager;
    @Mock
    private TrackStorage trackStorage;
    @Mock
    private RxHttpClient rxHttpClient;
    @Mock
    private Observer observer;

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(modelManager, trackStorage, rxHttpClient);
        track = TestHelper.getModelFactory().createModel(Track.class);
    }

    @Test
    public void playTrackShouldOpenPlayerActivityWithInitialTrackId() {
        playbackOperations.playTrack(Robolectric.application, track);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playTrackShouldStartPlaybackServiceWithPlayQueueFromInitialTrack() {
        playbackOperations.playTrack(Robolectric.application, track);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 0, track.getId());
    }

    @Test
    public void playFromUriShouldOpenPlayerActivityWithInitialTrackId() {
        playbackOperations = new PlaybackOperations(modelManager, new TrackStorage(), rxHttpClient);
        playbackOperations.playFromPlaylist(Robolectric.application, Content.ME_LIKES.uri, 0, track);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playFromUriShouldStartPlaybackServiceWithPlayQueueFromTracksCollection() {
        TestHelper.insertAsSoundAssociation(new Track(1L), Association.Type.TRACK_LIKE);
        TestHelper.insertAsSoundAssociation(new Track(2L), Association.Type.TRACK_LIKE);
        expect(Content.ME_LIKES).toHaveCount(2);

        playbackOperations = new PlaybackOperations(modelManager, new TrackStorage().<TrackStorage>subscribeOn(Schedulers.immediate()), rxHttpClient);
        playbackOperations.playFromPlaylist(Robolectric.application, Content.ME_LIKES.uri, 1, track);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1, 2L, 1L);
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L), new Track(3L), new Track(2L), new Track(1L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 4, null);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 2, 2L, 3L, 1L);

    }

    private void checkStartIntent(Intent startintent, int startPosition, Long... ids){
        expect(startintent).not.toBeNull();
        expect(startintent.getAction()).toBe(PlaybackService.Actions.PLAY_ACTION);
        expect(startintent.getIntExtra(PlayExtras.startPosition, -1)).toBe(startPosition);
        final List<Long> trackIdList = Longs.asList(startintent.getLongArrayExtra(PlayExtras.trackIdList));
        expect(trackIdList).toContainExactly(ids);
    }

    @Test
    public void playFromAdapterShouldOpenPlayerActivityWithInitialTrackFromPosition() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null); // clicked 2nd track

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(2L);
    }

    @Test
    public void playFromAdapterShouldStartPlaybackServiceWithListOfTracks() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1, 1L, 2L);
    }

    @Test
    public void playFromAdapterShouldIgnoreItemsThatAreNotTracks() throws Exception {
        List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, null);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1, 1L, 2L);
    }

    @Test
    public void playFromAdapterShouldStartPlaylistActivity() throws Exception {
        final Playlist playlist = new Playlist(1L);
        List<Playable> playables = Lists.newArrayList(new Track(1L), playlist, new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYLIST);
        expect(startedActivity.getData()).toEqual(playlist.toUri());
    }

    @Test(expected = AssertionError.class)
    public void playFromAdapterShouldThrowAssertionErrorWhenPositionGreaterThanSize() throws Exception {
        List<Playable> playables = Lists.<Playable>newArrayList(track);
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, Content.ME_LIKES.uri);
    }

    @Test
    public void shouldLoadTrackFromStorageAndEmitOnUIThreadForPlayback() {
        Observable<Track> observable = mock(Observable.class);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        when(observable.map(any(Func1.class))).thenReturn(observable);
        when(observable.observeOn(any(Scheduler.class))).thenReturn(observable);

        Observer<Track> observer = mock(Observer.class);
        playbackOperations.loadTrack(1L).subscribe(observer);

        verify(observable).observeOn(AndroidSchedulers.mainThread());
        verify(observable).subscribe(observer);
    }

    @Test
    @Ignore("Revisit this after removing model manager")
    public void loadTrackShouldReplaceNullTrackForDummy() {
        Observable<Track> observable = Observable.just(null);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        when(modelManager.cache(track)).thenReturn(track);

        Observer<Track> observer = mock(Observer.class);
        playbackOperations.loadTrack(1L).subscribe(observer);

        verify(observer).onNext(eq(new Track(1L)));
    }

    @Test
    public void loadTrackShouldCacheLoadedTrack() {
        final Track track = new Track(1L);
        Observable<Track> observable = Observable.just(track);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);
        Observer<Track> observer = mock(Observer.class);
        playbackOperations.loadTrack(1L).subscribe(observer);

        verify(modelManager).cache(eq(track));
    }

    @Test
    public void getRelatedTracksShouldMakeGetRequestToRelatedTracksEndpoint() {
        when(rxHttpClient.fetchModels(any(APIRequest.class))).thenReturn(Observable.empty());
        playbackOperations.getRelatedTracks(123L).subscribe(observer);

        ArgumentCaptor<APIRequest> argumentCaptor = ArgumentCaptor.forClass(APIRequest.class);
        verify(rxHttpClient).fetchModels(argumentCaptor.capture());
        expect(argumentCaptor.getValue().getMethod()).toEqual("GET");
        expect(argumentCaptor.getValue().getUriPath()).toEqual(String.format(APIEndpoints.RELATED_TRACKS.path(),
                ClientUri.fromTrack(123L).toString()));
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
        playbackOperations.getRelatedTracks(123L).subscribe(relatedObserver);

        ArgumentCaptor<ModelCollection> argumentCaptor = ArgumentCaptor.forClass(ModelCollection.class);
        verify(relatedObserver).onNext(argumentCaptor.capture());
        Iterator iterator = argumentCaptor.getValue().iterator();
        expect(iterator.next()).toEqual(suggestion1);
        expect(iterator.next()).toEqual(suggestion2);
        verify(relatedObserver).onCompleted();
        verify(relatedObserver, never()).onError(any(Throwable.class));

    }
}
