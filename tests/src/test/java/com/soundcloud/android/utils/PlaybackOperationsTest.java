package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.PlaybackService.PlayExtras;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;
import rx.util.functions.Func1;

import android.content.Intent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaybackOperationsTest {

    private PlaybackOperations playbackOperations;
    private Track track;

    private Screen ORIGIN_SCREEN = Screen.EXPLORE_TRENDING_MUSIC;

    @Mock
    private ScModelManager modelManager;
    @Mock
    private TrackStorage trackStorage;

    @Mock
    private Observer observer;

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(modelManager, trackStorage);
        track = TestHelper.getModelFactory().createModel(Track.class);
    }

    @Test
    public void playTrackShouldOpenPlayerActivityWithInitialTrackId() {
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playTrackShouldStartPlaybackServiceWithPlayQueueFromInitialTrack() {
        playbackOperations.playTrack(Robolectric.application, track, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 0, new PlaySessionSource(ORIGIN_SCREEN.toUri()), track.getId());
    }

    @Test
    public void playFromUriShouldOpenPlayerActivityWithInitialTrackId() {
        playbackOperations = new PlaybackOperations(modelManager, new TrackStorage());
        playbackOperations.playFromPlaylist(Robolectric.application, Content.ME_LIKES.uri, 0, track, Screen.YOUR_LIKES);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(track.getId());
    }

    @Test
    public void playFromUriShouldStartPlaybackServiceWithPlayQueueFromTracksCollection() throws CreateModelException {
        List<Track> tracks = TestHelper.createTracks(3);
        Playlist playlist = TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        playbackOperations = new PlaybackOperations(modelManager, new TrackStorage().<TrackStorage>subscribeOn(Schedulers.immediate()));
        playbackOperations.playFromPlaylist(Robolectric.application, playlist.toUri(), 1, tracks.get(1), ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1,  new PlaySessionSource(ORIGIN_SCREEN.toUri(), playlist.getId()),
                tracks.get(0).getId(), tracks.get(1).getId(), tracks.get(2).getId());
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L), new Track(3L), new Track(2L), new Track(1L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 4, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 2,  new PlaySessionSource(ORIGIN_SCREEN.toUri()), 2L, 3L, 1L);

    }

    private void checkStartIntent(Intent startintent, int startPosition, PlaySessionSource playSessionSource, Long... ids){
        expect(startintent).not.toBeNull();
        expect(startintent.getAction()).toBe(PlaybackService.Actions.PLAY_ACTION);
        expect(startintent.getIntExtra(PlayExtras.startPosition, -1)).toBe(startPosition);
        expect(startintent.getParcelableExtra(PlayExtras.playSessionSource)).toEqual(playSessionSource);
        final List<Long> trackIdList = Longs.asList(startintent.getLongArrayExtra(PlayExtras.trackIdList));
        expect(trackIdList).toContainExactly(ids);
    }

    @Test
    public void playFromAdapterShouldOpenPlayerActivityWithInitialTrackFromPosition() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, Screen.SIDE_MENU_STREAM); // clicked 2nd track

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYER);
        expect(startedActivity.getLongExtra(Track.EXTRA_ID, -1)).toBe(2L);
    }

    @Test
    public void playFromAdapterShouldStartPlaybackServiceWithListOfTracks() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1,  new PlaySessionSource(ORIGIN_SCREEN.toUri()), 1L, 2L);
    }

    @Test
    public void playFromAdapterShouldIgnoreItemsThatAreNotTracks() throws Exception {
        List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 1,  new PlaySessionSource(ORIGIN_SCREEN.toUri()), 1L, 2L);
    }

    @Test
    public void playFromAdapterWithUriShouldAdjustPlayPositionWithUpdatedContent() throws IOException {
        final List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));
        final ArrayList<Long> value = Lists.newArrayList(5L, 1L, 2L);

        when(trackStorage.getTrackIdsForUriAsync(Content.ME_LIKES.uri)).thenReturn(Observable.<List<Long>>just(value));
        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, Content.ME_LIKES.uri, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        checkStartIntent(application.getNextStartedService(), 2,  new PlaySessionSource(ORIGIN_SCREEN.toUri()), 5L, 1L, 2L);
    }


    @Test
    public void playFromAdapterShouldStartPlaylistActivity() throws Exception {
        final Playlist playlist = new Playlist(1L);
        List<Playable> playables = Lists.newArrayList(new Track(1L), playlist, new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, null, ORIGIN_SCREEN);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedActivity = application.getNextStartedActivity();

        expect(startedActivity).not.toBeNull();
        expect(startedActivity.getAction()).toBe(Actions.PLAYLIST);
        expect(startedActivity.getData()).toEqual(playlist.toUri());
    }

    @Test(expected = AssertionError.class)
    public void playFromAdapterShouldThrowAssertionErrorWhenPositionGreaterThanSize() throws Exception {
        List<Playable> playables = Lists.<Playable>newArrayList(track);
        playbackOperations.playFromAdapter(Robolectric.application, playables, 1, Content.ME_LIKES.uri, ORIGIN_SCREEN);
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
}
