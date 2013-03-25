package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncerTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueManagerTest {
    ContentResolver resolver;
    PlayQueueManager pm;
    static final long USER_ID = 1L;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        pm = new PlayQueueManager(Robolectric.application, USER_ID);
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldHandleEmptyPlaylistWithAddItemsFromUri() throws Exception {
        pm.loadUri(Content.TRACKS.uri, 0, null);
        expect(pm.length()).toEqual(0);
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.next()).toBeFalse();
        expect(pm.getNext()).toBeNull();
    }

    @Test
    public void shouldAddItemsFromUri() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, 0, null);

        expect(pm.getUri()).not.toBeNull();
        expect(pm.getUri()).toEqual(Content.TRACKS.uri);

        expect(pm.length()).toEqual(tracks.size());

        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");

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
        pm.loadUri(Content.TRACKS.uri, 1, null);

        expect(pm.length()).toEqual(tracks.size());
        expect(pm.isEmpty()).toBeFalse();

        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev()).not.toBeNull();
        expect(pm.getPrev().title).toEqual("track #0");

        expect(pm.next()).toBeTrue();
        track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev()).not.toBeNull();
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();
    }

    @Test
    public void shouldAddItemsFromUriWithInvalidPosition() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, tracks.size() + 100, null); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
    }

    @Test
    public void shouldAddItemsFromUriWithIncorrectPositionDown() throws Exception {
        List<Track> tracks = createTracks(10, true, 0);
        pm.loadUri(Content.TRACKS.uri, 7, 5L);

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #5");
    }

    @Test
    public void shouldAddItemsFromUriWithIncorrectPositionUp() throws Exception {
        List<Track> tracks = createTracks(10, true, 0);
        pm.loadUri(Content.TRACKS.uri, 5, 7L);

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #7");
    }

    @Test
    public void shouldAddItemsFromUriWithNegativePosition() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.loadUri(Content.TRACKS.uri, -10, null); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
    }

    @Test
    public void shouldSupportSetPlaylistWithTrackObjects() throws Exception {
        pm.setPlayQueue(createTracks(3, true, 0), 0);
        expect(pm.length()).toEqual(3);

        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");

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
    public void shouldClearPlaylist() throws Exception {
        pm.setPlayQueue(createTracks(10, true, 0), 0);
        pm.clear();
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.length()).toEqual(0);
    }

    @Test
    public void shouldSaveCurrentTracksToDB() throws Exception {
        expect(Content.PLAY_QUEUE).toBeEmpty();
        expect(Content.PLAY_QUEUE.uri).toBeEmpty();
        pm.setPlayQueue(createTracks(10, true, 0), 0);
        expect(Content.PLAY_QUEUE.uri).toHaveCount(10);
    }

    @Test
    public void shouldLoadLikesAsPlaylist() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, 56142962);

        expect(pm.length()).toEqual(2);
        expect(pm.getCurrentTrack().id).toEqual(56142962l);
        expect(pm.next()).toBeTrue();
        expect(pm.getCurrentTrack().id).toEqual(56143158l);
    }

    @Test
    public void shouldSaveAndRestoreLikesAsPlaylist() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, 56142962l);
        expect(pm.length()).toEqual(2);
        expect(pm.getCurrentTrack().id).toEqual(56142962l);
        expect(pm.getPosition()).toEqual(0);
        pm.saveQueue(1000l);
        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(56142962l);
        expect(pm.getPosition()).toEqual(0);
    }

    @Test
    public void shouldSaveAndRestoreLikesAsPlaylistTwice() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, 56142962l);
        expect(pm.length()).toEqual(2);
        expect(pm.getCurrentTrack().id).toEqual(56142962l);
        pm.saveQueue(1000l);
        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(56142962l);
        expect(pm.getPosition()).toEqual(0);

        // test overwrite
        expect(pm.next()).toBeTrue();
        expect(pm.getCurrentTrackId()).toEqual(56143158l);
        pm.saveQueue(2000l);
        expect(pm.reloadQueue()).toEqual(2000l);
        expect(pm.getCurrentTrackId()).toEqual(56143158l);
        expect(pm.getPosition()).toEqual(1);
    }

    @Test
    public void shouldSaveAndRestoreLikesAsPlaylistWithMovedTrack() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, 56142962l);
        expect(pm.getCurrentTrack().id).toEqual(56142962l);
        expect(pm.next()).toBeTrue();

        pm.saveQueue(1000l);

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(56143158l);
        expect(pm.getPosition()).toEqual(1);
    }

    @Test
    public void shouldSavePlaylistStateInUri() throws Exception {
        insertLikes();
        pm.loadUri(Content.ME_LIKES.uri, 1, 56142962l);
        expect(pm.getCurrentTrack().id).toEqual(56142962l);
        expect(pm.next()).toBeTrue();
        expect(pm.getPlayQueueState(123L)).toEqual(
          Content.ME_LIKES.uri + "?trackId=56143158&playlistPos=1&seekPos=123"
        );
    }

    @Test
    public void shouldSavePlaylistStateInUriWithSetPlaylist() throws Exception {
        pm.setPlayQueue(createTracks(10, true, 0), 5);
        expect(pm.getCurrentTrack().id).toEqual(5L);
        expect(pm.getPlayQueueState(123L)).toEqual(
                Content.PLAY_QUEUE.uri + "?trackId=5&playlistPos=5&seekPos=123"
        );
    }

    @Test
    public void shouldSkipUnstreamableTrackNext() throws Exception {
        ArrayList<PlayableHolder> playables = new ArrayList<PlayableHolder>();
        playables.addAll(createTracks(1, true, 0));
        playables.addAll(createTracks(1, false, 1));

        pm.setPlayQueue(playables, 0);
        expect(pm.getCurrentTrack().id).toEqual(0L);
        expect(pm.next()).toEqual(false);

        playables.addAll(createTracks(1, true, 2));
        pm.setPlayQueue(playables, 0);
        expect(pm.getCurrentTrack().id).toEqual(0L);
        expect(pm.next()).toEqual(true);
        expect(pm.getCurrentTrack().id).toEqual(2L);
    }

    @Test
    public void shouldSkipUnstreamableTrackPrev() throws Exception {
        ArrayList<PlayableHolder> playables = new ArrayList<PlayableHolder>();
        playables.addAll(createTracks(1, false, 0));
        playables.addAll(createTracks(1, true, 1));

        pm.setPlayQueue(playables, 1);
        expect(pm.getCurrentTrack().id).toEqual(1L);
        expect(pm.prev()).toEqual(false);

        playables.addAll(0, createTracks(1, true, 2));
        pm.setPlayQueue(playables, 2);
        expect(pm.getCurrentTrack().id).toEqual(1L);
        expect(pm.prev()).toEqual(true);
        expect(pm.getCurrentTrack().id).toEqual(2L);
    }

    @Test
    public void shouldRespondToUriChanges() throws Exception {
        Playlist p = TestHelper.readResource("/com/soundcloud/android/service/sync/playlist.json");

        Uri playlistUri = p.insert(resolver);
        expect(playlistUri).toEqual(Content.PLAYLIST.forQuery(String.valueOf(2524386)));

        pm.loadUri(playlistUri, 5, 7L);
        pm.saveQueue(1000l);

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getUri().getPath()).toEqual(playlistUri.getPath());

        final Uri newUri = Content.PLAYLIST.forQuery("321");
        PlayQueueManager.onPlaylistUriChanged(pm, DefaultTestRunner.application,playlistUri, newUri);
        expect(pm.getUri().getPath()).toEqual(newUri.getPath());
    }


    @Test
    public void shouldClearPlaylistState() throws Exception {
        pm.setPlayQueue(createTracks(10, true, 0), 5);
        pm.saveQueue(1235);

        PlayQueueManager.clearState(Robolectric.application);
        expect(pm.reloadQueue()).toEqual(-1L);

        PlayQueueManager pm2 = new PlayQueueManager(Robolectric.application, USER_ID);
        expect(pm2.reloadQueue()).toEqual(-1L);
        expect(pm2.getPosition()).toEqual(0);
        expect(pm2.length()).toEqual(0);
    }

    @Test
    public void shouldSetSingleTrack() throws Exception {
        List<Track> tracks = createTracks(1, true, 0);
        pm.setTrack(tracks.get(0), true);
        expect(pm.length()).toEqual(1);
        expect(pm.getCurrentTrack()).toBe(tracks.get(0));
    }

    private void insertLikes() throws IOException {
        SoundAssociationHolder old = TestHelper.getObjectMapper().readValue(
                ApiSyncerTest.class.getResourceAsStream("e1_likes.json"),
                SoundAssociationHolder.class);

        TestHelper.bulkInsert(Content.ME_LIKES.uri, old.collection);

//        expect(SoundCloudApplication.MODEL_MANAGER.writeCollection(old.collection, Content.ME_LIKES.uri, USER_ID,
//                ScResource.CacheUpdateMode.NONE)).toEqual(18); // 2 tracks, 1 playlist
//
        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(3); // 2 tracks, 1 playlist
    }

    private List<Track> createTracks(int n, boolean streamable, int startPos) {
        List<Track> list = new ArrayList<Track>();

        User user = new User();
        user.id = 0L;

        for (int i=0; i<n; i++) {
            Track t = new Track();
            t.id = (startPos +i);
            t.title = "track #"+(startPos+i);
            t.user = user;
            t.stream_url = streamable ? "http://www.soundcloud.com/sometrackurl" : null;
            t.insert(resolver);
            list.add(t);
        }
        return list;
    }
}
