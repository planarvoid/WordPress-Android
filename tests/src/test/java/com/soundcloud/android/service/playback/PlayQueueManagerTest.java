package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueManagerTest {
    ContentResolver resolver;
    PlayQueueManager pm;
    static final long USER_ID = 1L;

    @Mock
    ExploreTracksOperations exploreTracksOperations;
    PlaySourceInfo trackingInfo;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();

        pm = new PlayQueueManager(Robolectric.application, USER_ID, exploreTracksOperations);
        TestHelper.setUserId(USER_ID);

        trackingInfo = new PlaySourceInfo("origin-url", 123L, "explore-tag", "version_1");
    }

    @Test
    public void shouldHandleEmptyPlaylistWithAddItemsFromUri() throws Exception {
        pm.loadUri(Content.TRACKS.uri, 0, null, trackingInfo);
        expect(pm.length()).toEqual(0);
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.next()).toBeFalse();
        expect(pm.getNext()).toBeNull();
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);
    }

    @Test
    public void shouldAddItemsFromUri() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, 0, null, trackingInfo);

        expect(pm.getUri()).not.toBeNull();
        expect(pm.getUri()).toEqual(Content.TRACKS.uri);

        expect(pm.length()).toEqual(tracks.size());
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);

        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
        checkCurrentItemTrackingTriggerIsManual();

        expect(pm.next()).toBeTrue();
        track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev().title).toEqual("track #0");
        expect(pm.getNext().title).toEqual("track #2");

        expect(pm.next()).toBeTrue();
        track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();

        expect(pm.getNext()).toBeNull();
        expect(pm.next()).toBeFalse();
    }

    @Test
    public void shouldAddItemsFromUriWithPosition() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, 1, null, trackingInfo);

        expect(pm.length()).toEqual(tracks.size());
        expect(pm.isEmpty()).toBeFalse();

        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev()).not.toBeNull();
        expect(pm.getPrev().title).toEqual("track #0");

        checkCurrentItemTrackingTriggerIsManual();

        expect(pm.next()).toBeTrue();
        track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev()).not.toBeNull();
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();
    }

    private void checkCurrentItemTrackingTriggerIsManual() {
        final PlayQueueItem currentPlayQueueItem = pm.getCurrentPlayQueueItem();
        expect(currentPlayQueueItem.getTrackSourceInfo().getTrigger()).toEqual("manual");
    }

    private void checkCurrentItemTrackingTriggerIsAuto() {
        expect(pm.getCurrentPlayQueueItem().getTrackSourceInfo().getTrigger()).toEqual("auto");
    }

    @Test
    public void shouldAddItemsFromUriWithInvalidPosition() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, tracks.size() + 100, null, trackingInfo); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
        checkCurrentItemTrackingTriggerIsAuto();
    }

    @Test
    public void shouldAddItemsFromUriWithIncorrectPositionDown() throws Exception {
        List<Track> tracks = createTracks(10, true, 0);
        pm.loadUri(Content.TRACKS.uri, 7, new Track(5L), trackingInfo);

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #5");
        checkCurrentItemTrackingTriggerIsManual();
    }

    @Test
    public void shouldAddItemsFromUriWithIncorrectPositionUp() throws Exception {
        List<Track> tracks = createTracks(10, true, 0);
        pm.loadUri(Content.TRACKS.uri, 5, new Track(7L), trackingInfo);

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #7");
        checkCurrentItemTrackingTriggerIsManual();
    }

    @Test
    public void shouldAddItemsFromUriWithNegativePosition() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, -10, null, trackingInfo); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
        checkCurrentItemTrackingTriggerIsAuto();
    }

    @Test
    public void shouldSupportSetPlaylistWithTrackObjects() throws Exception {
        pm.setPlayQueue(createTracks(3, true, 0), 0, trackingInfo);
        expect(pm.length()).toEqual(3);

        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
        checkCurrentItemTrackingTriggerIsManual();

        expect(pm.next()).toBeTrue();
        track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev().title).toEqual("track #0");
        expect(pm.getNext().title).toEqual("track #2");
        checkCurrentItemTrackingTriggerIsAuto();

        expect(pm.next()).toBeTrue();
        track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();
        checkCurrentItemTrackingTriggerIsAuto();

        expect(pm.getNext()).toBeNull();
        expect(pm.next()).toBeFalse();
    }

    @Test
    public void shouldClearPlaylist() throws Exception {
        pm.setPlayQueue(createTracks(10, true, 0), 0, trackingInfo);
        pm.clear();
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.length()).toEqual(0);
        expect(pm.getCurrentTrackingInfo()).toBeNull();
    }

    @Test
    public void shouldSaveCurrentTracksToDB() throws Exception {
        expect(Content.PLAY_QUEUE).toBeEmpty();
        expect(Content.PLAY_QUEUE.uri).toBeEmpty();
        pm.setPlayQueue(createTracks(10, true, 0), 0, trackingInfo);
        expect(Content.PLAY_QUEUE.uri).toHaveCount(10);
    }

    @Test
    public void shouldLoadLikesAsPlaylist() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962), trackingInfo);

        expect(pm.length()).toEqual(2);
        expect(pm.getCurrentTrack().getId()).toEqual(56142962l);
        checkCurrentItemTrackingTriggerIsManual();

        expect(pm.next()).toBeTrue();
        expect(pm.getCurrentTrack().getId()).toEqual(56143158l);
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);
        checkCurrentItemTrackingTriggerIsAuto();
    }

    @Test
    public void shouldSaveAndRestoreLikesAsPlaylist() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 0, new Track(56143158L), trackingInfo);
        expect(pm.length()).toEqual(2);
        expect(pm.getCurrentTrack().getId()).toEqual(56143158L);
        expect(pm.getPosition()).toEqual(1);
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);

        pm.saveQueue(1000l);
        pm.clear();

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(56143158L);
        expect(pm.getPosition()).toEqual(1);
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);
    }

    @Test
    public void shouldSaveAndRestoreLikesAsPlaylistTwice() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962l), trackingInfo);
        expect(pm.length()).toEqual(2);
        expect(pm.getCurrentTrack().getId()).toEqual(56142962l);
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);
        pm.saveQueue(1000l);
        pm.clear();

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(56142962l);
        expect(pm.getPosition()).toEqual(0);
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);

        // test overwrite
        expect(pm.next()).toBeTrue();
        expect(pm.getCurrentTrackId()).toEqual(56143158l);
        pm.saveQueue(2000l);
        expect(pm.reloadQueue()).toEqual(2000l);
        expect(pm.getCurrentTrackId()).toEqual(56143158l);
        expect(pm.getPosition()).toEqual(1);
        expect(pm.getCurrentTrackingInfo()).toEqual(trackingInfo);
    }

    @Test
    public void shouldSaveAndRestoreLikesAsPlaylistWithMovedTrack() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962l), trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(56142962l);
        expect(pm.next()).toBeTrue();

        pm.saveQueue(1000l);

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(56143158l);
        expect(pm.getPosition()).toEqual(1);
    }

    @Test
    public void shouldSavePlaylistStateInUri() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, new Track(56142962l), trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(56142962l);
        expect(pm.next()).toBeTrue();
        expect(pm.getPlayQueueState(123L)).toEqual(
          Content.ME_LIKES.uri + "?trackId=56143158&playlistPos=1&seekPos=123&playSource-originUrl=origin-url&playSource-exploreTag=explore-tag&playSource-recommenderVersion=version_1&playSource-initialTrackId=123"
        );
    }

    @Test
    public void shouldSavePlaylistStateInUriWithSetPlaylist() throws Exception {
        pm.setPlayQueue(createTracks(10, true, 0), 5, trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(5L);
        expect(pm.getPlayQueueState(123L)).toEqual(
                Content.PLAY_QUEUE.uri + "?trackId=5&playlistPos=5&seekPos=123&playSource-originUrl=origin-url&playSource-exploreTag=explore-tag&playSource-recommenderVersion=version_1&playSource-initialTrackId=123"
        );
    }

    @Test
    public void shouldSkipUnstreamableTrackNext() throws Exception {
        ArrayList<PlayableHolder> playables = new ArrayList<PlayableHolder>();
        playables.addAll(createTracks(1, true, 0));
        playables.addAll(createTracks(1, false, 1));

        pm.setPlayQueue(playables, 0, trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(0L);
        expect(pm.next()).toEqual(false);

        playables.addAll(createTracks(1, true, 2));
        pm.setPlayQueue(playables, 0, trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(0L);
        expect(pm.next()).toEqual(true);
        expect(pm.getCurrentTrack().getId()).toEqual(2L);
    }

    @Test
    public void shouldSkipUnstreamableTrackPrev() throws Exception {
        ArrayList<PlayableHolder> playables = new ArrayList<PlayableHolder>();
        playables.addAll(createTracks(1, false, 0));
        playables.addAll(createTracks(1, true, 1));

        pm.setPlayQueue(playables, 1, trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(1L);
        expect(pm.prev()).toEqual(false);

        playables.addAll(0, createTracks(1, true, 2));
        pm.setPlayQueue(playables, 2, trackingInfo);
        expect(pm.getCurrentTrack().getId()).toEqual(1L);
        expect(pm.prev()).toEqual(true);
        expect(pm.getCurrentTrack().getId()).toEqual(2L);
    }

    @Test
    public void shouldRespondToUriChanges() throws Exception {
        Playlist p = TestHelper.readResource("/com/soundcloud/android/service/sync/playlist.json");
        TestHelper.insertWithDependencies(p);

        Uri playlistUri = p.toUri();
        expect(playlistUri).toEqual(Content.PLAYLIST.forQuery(String.valueOf(2524386)));

        pm.loadUri(playlistUri, 5, new Track(7L), trackingInfo);
        pm.saveQueue(1000l);

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getUri().getPath()).toEqual(playlistUri.getPath());

        final Uri newUri = Content.PLAYLIST.forQuery("321");
        PlayQueueManager.onPlaylistUriChanged(pm, DefaultTestRunner.application, playlistUri, newUri);
        expect(pm.getUri().getPath()).toEqual(newUri.getPath());
    }


    @Test
    public void shouldClearPlaylistState() throws Exception {
        pm.setPlayQueue(createTracks(10, true, 0), 5, trackingInfo);
        pm.saveQueue(1235);

        pm.clearState();
        expect(pm.reloadQueue()).toEqual(-1L);

        PlayQueueManager pm2 = new PlayQueueManager(Robolectric.application, USER_ID, exploreTracksOperations);
        expect(pm2.reloadQueue()).toEqual(-1L);
        expect(pm2.getPosition()).toEqual(0);
        expect(pm2.length()).toEqual(0);
    }

    @Test
    public void shouldSetSingleTrack() throws Exception {
        List<Track> tracks = createTracks(1, true, 0);
        pm.loadTrack(tracks.get(0), true, trackingInfo);
        expect(pm.length()).toEqual(1);
        expect(pm.getCurrentTrack()).toBe(tracks.get(0));
    }

    @Test
    public void shouldAddSeedTrackAndSetLoadingStateWhileLoadingExploreTracks() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(Observable.<ModelCollection<TrackSummary>>never());

        expect(pm.length()).toBe(0);
        pm.loadTrack(track, false, trackingInfo);
        pm.fetchRelatedTracks(track);
        expect(pm.length()).toBe(1);
        expect(pm.isFetchingRelated()).toBeTrue();
    }

    @Test
    public void shouldSetFailureStateAfterFailedRelatedLoad() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(Observable.<ModelCollection<TrackSummary>>error(new Exception()));

        pm.fetchRelatedTracks(track);
        expect(pm.isFetchingRelated()).toBeFalse();
        expect(pm.lastRelatedFetchFailed()).toBeTrue();
    }

    @Test
    public void shouldAddTrackSetIdleStateAndBroadcastChangeAfterSuccessfulRelatedLoad() throws Exception {
        Context context = Mockito.mock(Context.class);
        pm = new PlayQueueManager(context, USER_ID, exploreTracksOperations);

        TrackSummary trackSummary = TestHelper.getModelFactory().createModel(TrackSummary.class);
        ModelCollection<TrackSummary> relatedTracks = new ModelCollection<TrackSummary>(Lists.newArrayList(trackSummary));
        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(Observable.<ModelCollection<TrackSummary>>just(relatedTracks));

        Track track = TestHelper.getModelFactory().createModel(Track.class);

        expect(pm.length()).toBe(0);
        pm.loadTrack(track, false, trackingInfo);
        pm.fetchRelatedTracks(track);
        expect(pm.length()).toBe(2);

        expect(pm.isFetchingRelated()).toBeFalse();
        expect(pm.lastRelatedFetchFailed()).toBeFalse();

        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(3)).sendBroadcast(argumentCaptor.capture());
        final List<Intent> allValues = argumentCaptor.getAllValues();
        expect(allValues.size()).toEqual(3);
        assertThat(allValues.get(0).getAction(), is(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED));
        assertThat(allValues.get(1).getAction(), is(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
        assertThat(allValues.get(2).getAction(), is(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
    }

    @Test
    public void shouldRetryRelatedLoadWithSameObservable() throws Exception {
        Context context = Mockito.mock(Context.class);
        pm = new PlayQueueManager(context, USER_ID, exploreTracksOperations);

        Track track = TestHelper.getModelFactory().createModel(Track.class);
        final Observable<ModelCollection<TrackSummary>> observable = Mockito.mock(Observable.class);
        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(observable);

        pm.fetchRelatedTracks(track);
        pm.retryRelatedTracksFetch();
        verify(observable, times(2)).subscribe(any(Observer.class));
    }

    @Test
    public void shouldUnsubscribeAndNotBeLoadingAfterCallingSetTrack() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        final Observable<ModelCollection<TrackSummary>> observable = Mockito.mock(Observable.class);
        final Subscription subscription = Mockito.mock(Subscription.class);

        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(observable);
        when(observable.subscribe(any(Observer.class))).thenReturn(subscription);

        pm.fetchRelatedTracks(track);
        pm.loadTrack(track, false, trackingInfo);

        expect(pm.isFetchingRelated()).toBeFalse();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldUnsubscribeAndNotBeLoadingAfterCallingloadUri() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        final Observable<ModelCollection<TrackSummary>> observable = Mockito.mock(Observable.class);
        when(exploreTracksOperations.getRelatedTracks(any(Track.class))).thenReturn(observable);

        final Subscription subscription = Mockito.mock(Subscription.class);
        when(observable.subscribe(any(Observer.class))).thenReturn(subscription);

        pm.fetchRelatedTracks(track);
        pm.loadUri(Content.TRACKS.uri, 0, null, trackingInfo);

        expect(pm.isFetchingRelated()).toBeFalse();
        verify(subscription).unsubscribe();
    }


    private void insertLikes() throws IOException {
        List<Playable> likes = TestHelper.readResourceList("/com/soundcloud/android/service/sync/e1_likes.json");
        expect(TestHelper.bulkInsert(Content.ME_LIKES.uri, likes)).toEqual(3);
    }

    // TODO : replace with model factory
    private List<Track> createTracks(int n, boolean streamable, int startPos) {
        List<Track> list = new ArrayList<Track>();

        User user = new User();
        user.setId(0L);

        for (int i=0; i<n; i++) {
            Track t = new Track();
            t.setId((startPos +i));
            t.title = "track #"+(startPos+i);
            t.user = user;
            t.stream_url = streamable ? "http://www.soundcloud.com/sometrackurl" : null;
            TestHelper.insertWithDependencies(t);
            list.add(t);
        }
        return list;
    }
}
