package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.assertContentUriCount;

import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlaylistManagerTest {
    ContentResolver resolver;
    PlaylistManager pm;
    static final long USER_ID = 1L;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        TrackCache cache = new TrackCache();
        pm = new PlaylistManager(Robolectric.application, cache, USER_ID);

        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldHandleEmptyPlaylistWithAddItemsFromUri() throws Exception {
        pm.setUri(Content.TRACKS.uri, 0);
        expect(pm.length()).toEqual(0);
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.next()).toBeFalse();
        expect(pm.getNext()).toBeNull();
    }

    @Test
    public void shouldAddItemsFromUri() throws Exception {
        List<Track> tracks = createTracks(3);
        pm.setUri(Content.TRACKS.uri, 0);

        expect(pm.length()).toEqual(tracks.size());

        Track track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");

        expect(pm.next()).toBeTrue();
        track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev().title).toEqual("track #0");
        expect(pm.getNext().title).toEqual("track #2");

        expect(pm.next()).toBeTrue();
        track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();

        expect(pm.getNext()).toBeNull();
        expect(pm.next()).toBeFalse();
    }

    @Test
    public void shouldAddItemsFromUriWithPosition() throws Exception {
        List<Track> tracks = createTracks(3);
        pm.setUri(Content.TRACKS.uri, 1);

        expect(pm.length()).toEqual(tracks.size());
        expect(pm.isEmpty()).toBeFalse();

        Track track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev()).not.toBeNull();
        expect(pm.getPrev().title).toEqual("track #0");

        expect(pm.next()).toBeTrue();
        track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev()).not.toBeNull();
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();
    }

    @Test
    public void shouldAddItemsFromUriWithInvalidPosition() throws Exception {
        List<Track> tracks = createTracks(3);
        pm.setUri(Content.TRACKS.uri, tracks.size() + 100); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
    }

    @Test
    public void shouldAddItemsFromUriWithNegativePosition() throws Exception {
        List<Track> tracks = createTracks(3);
        pm.setUri(Content.TRACKS.uri, -10); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
    }

    @Test
    public void shouldSupportSetPlaylistWithTrackObjects() throws Exception {
        pm.setPlaylist(createTracks(3), 0);
        expect(pm.length()).toEqual(3);

        Track track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");

        expect(pm.next()).toBeTrue();
        track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #1");
        expect(pm.getPrev().title).toEqual("track #0");
        expect(pm.getNext().title).toEqual("track #2");

        expect(pm.next()).toBeTrue();
        track = pm.getTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
        expect(pm.getPrev().title).toEqual("track #1");
        expect(pm.getNext()).toBeNull();

        expect(pm.getNext()).toBeNull();
        expect(pm.next()).toBeFalse();
    }

    @Test
    public void shouldClearPlaylist() throws Exception {
        pm.setPlaylist(createTracks(10), 0);
        pm.clear();
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.length()).toEqual(0);
    }

    @Test
    public void shouldSaveCurrentTracksToDB() throws Exception {
        assertContentUriCount(Content.PLAYLISTS, 0);
        assertContentUriCount(PlaylistManager.DEFAULT_PLAYLIST_URI, 0);
        pm.setPlaylist(createTracks(10), 0);
        assertContentUriCount(Content.PLAYLISTS, 1);
        assertContentUriCount(PlaylistManager.DEFAULT_PLAYLIST_URI, 10);
    }

    private List<Track> createTracks(int n) {
        List<Track> list = new ArrayList<Track>();

        User user = new User();
        user.id = 0L;

        for (int i=0; i<n; i++) {
            Track t = new Track();
            t.id = i;
            t.title = "track #"+i;
            t.user = user;
            SoundCloudDB.insertTrack(resolver, t);
            list.add(t);
        }
        return list;
    }
}
