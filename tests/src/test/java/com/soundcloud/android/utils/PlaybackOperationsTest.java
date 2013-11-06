package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueue;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.concurrency.AndroidSchedulers;
import rx.concurrency.Schedulers;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaybackOperationsTest {

    private PlaybackOperations playbackOperations;
    private Track track;

    @Mock
    private ScModelManager modelManager;
    @Mock
    private TrackStorage trackStorage;

    @Before
    public void setUp() throws Exception {
        playbackOperations = new PlaybackOperations(modelManager, trackStorage);
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
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        expect(startedService.getAction()).toBe(CloudPlaybackService.Actions.PLAY_ACTION);

        PlayQueue playQueue = startedService.getParcelableExtra(PlayQueue.EXTRA);
        expect(playQueue.size()).toBe(1);
        expect(playQueue.getTrackIdAt(0)).toBe(track.getId());
    }

    @Test
    public void playFromUriShouldOpenPlayerActivityWithInitialTrackId() {
        playbackOperations = new PlaybackOperations(modelManager, new TrackStorage());
        playbackOperations.playFromUriWithInitialTrack(Robolectric.application, Content.ME_LIKES.uri, 0, track);

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

        playbackOperations = new PlaybackOperations(modelManager, new TrackStorage().<TrackStorage>subscribeOn(Schedulers.immediate()));
        playbackOperations.playFromUriWithInitialTrack(Robolectric.application, Content.ME_LIKES.uri, 1, track);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        expect(startedService.getAction()).toBe(CloudPlaybackService.Actions.PLAY_ACTION);

        PlayQueue playQueue = startedService.getParcelableExtra(PlayQueue.EXTRA);
        expect(playQueue).not.toBeNull();
        expect(playQueue.getPosition()).toBe(1);
        expect(playQueue.size()).toBe(2);
        expect(playQueue.getTrackIdAt(0)).toBe(2L);
        expect(playQueue.getTrackIdAt(1)).toBe(1L);
    }

    @Test
    public void playFromAdapterShouldRemoveDuplicates() throws Exception {
        ArrayList<Track> playables = Lists.newArrayList(new Track(1L), new Track(2L), new Track(3L), new Track(2L), new Track(1L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 4, null);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        PlayQueue playQueue = startedService.getParcelableExtra(PlayQueue.EXTRA);
        expect(playQueue).not.toBeNull();
        expect(playQueue.size()).toBe(3);
        expect(playQueue.getPosition()).toBe(2);
        expect(playQueue.getTrackIdAt(0)).toBe(2L);
        expect(playQueue.getTrackIdAt(1)).toBe(3L);
        expect(playQueue.getTrackIdAt(2)).toBe(1L);
    }

    @Test
    public void playExploreTrackShouldSignalServiceToFetchRelatedTracks() throws Exception {
        playbackOperations.playExploreTrack(Robolectric.application, track, "ignored here", "ignored here");

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        expect(startedService.getBooleanExtra(CloudPlaybackService.PlayExtras.fetchRelated, false)).toBeTrue();
    }

    @Test
    public void playExploreTrackShouldForwardTrackingTagAndInitialTrackId() throws Exception {
        playbackOperations.playExploreTrack(Robolectric.application, track, "tracking_tag", "ignored here");

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        PlaySourceInfo playSourceInfo = startedService.getParcelableExtra(CloudPlaybackService.PlayExtras.trackingInfo);
        expect(playSourceInfo.getExploreTag()).toEqual("tracking_tag");
        expect(playSourceInfo.getInitialTrackId()).toEqual(track.getId());
    }

    @Test
    public void playExploreTrackShouldForwardOriginUrl() throws Exception {
        playbackOperations.playExploreTrack(Robolectric.application, track, "ignored here", "explore:trending music");

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        PlaySourceInfo playSourceInfo = startedService.getParcelableExtra(CloudPlaybackService.PlayExtras.trackingInfo);
        expect(playSourceInfo.getOriginUrl()).toEqual("explore:trending music");
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
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        PlayQueue playQueue = startedService.getParcelableExtra(PlayQueue.EXTRA);
        expect(playQueue).not.toBeNull();
        expect(playQueue.size()).toBe(2);
        expect(playQueue.getPosition()).toBe(1);
        expect(playQueue.getTrackIdAt(0)).toBe(1L);
        expect(playQueue.getTrackIdAt(1)).toBe(2L);
    }

    @Test
    public void playFromAdapterShouldIgnoreItemsThatAreNotTracks() throws Exception {
        List<Playable> playables = Lists.newArrayList(new Track(1L), new Playlist(), new Track(2L));

        playbackOperations.playFromAdapter(Robolectric.application, playables, 2, null);

        ShadowApplication application = Robolectric.shadowOf(Robolectric.application);
        Intent startedService = application.getNextStartedService();

        expect(startedService).not.toBeNull();
        PlayQueue playQueue = startedService.getParcelableExtra(PlayQueue.EXTRA);
        expect(playQueue).not.toBeNull();
        expect(playQueue.size()).toBe(2);
        expect(playQueue.getPosition()).toBe(1); // adjusted the position to ignore the playlist
        expect(playQueue.getTrackIdAt(0)).toBe(1L);
        expect(playQueue.getTrackIdAt(1)).toBe(2L);
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
        when(observable.observeOn(any(Scheduler.class))).thenReturn(observable);

        Observer<Track> observer = mock(Observer.class);
        playbackOperations.loadTrack(1L).subscribe(observer);

        verify(observable).observeOn(AndroidSchedulers.mainThread());
        verify(observable).subscribe(observer);
    }

    @Test
    public void loadTrackShouldReplaceNullTrackForDummy() {
        Observable<Track> observable = Observable.just(null);
        when(trackStorage.getTrackAsync(1L)).thenReturn(observable);

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
