package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;

import java.io.IOException;
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
        pm = new PlaylistManager(Robolectric.application, SoundCloudApplication.TRACK_CACHE, USER_ID);

        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldHandleEmptyPlaylistWithAddItemsFromUri() throws Exception {
        pm.setUri(Content.TRACKS.uri, 0, null);
        expect(pm.length()).toEqual(0);
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.next()).toBeFalse();
        expect(pm.getNext()).toBeNull();
    }

    @Test
    public void shouldAddItemsFromUri() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.setUri(Content.TRACKS.uri, 0, null);

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
        pm.setUri(Content.TRACKS.uri, 1, null);

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
        pm.setUri(Content.TRACKS.uri, tracks.size() + 100, null); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #2");
    }

    @Test
    public void shouldAddItemsFromUriWithNegativePosition() throws Exception {
        List<Track> tracks = createTracks(3, true, 0);
        pm.setUri(Content.TRACKS.uri, -10, null); // out of range

        expect(pm.length()).toEqual(tracks.size());
        Track track = pm.getCurrentTrack();
        expect(track).not.toBeNull();
        expect(track.title).toEqual("track #0");
    }

    @Test
    public void shouldSupportSetPlaylistWithTrackObjects() throws Exception {
        pm.setPlaylist(createTracks(3, true, 0), 0);
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
        pm.setPlaylist(createTracks(10, true, 0), 0);
        pm.clear();
        expect(pm.isEmpty()).toBeTrue();
        expect(pm.length()).toEqual(0);
    }

    @Test
    public void shouldSaveCurrentTracksToDB() throws Exception {
        expect(Content.PLAYLIST).toBeEmpty();
        expect(PlaylistManager.DEFAULT_PLAYLIST_URI).toBeEmpty();
        pm.setPlaylist(createTracks(10, true, 0), 0);
        expect(Content.PLAYLIST).toHaveCount(1);
        expect(PlaylistManager.DEFAULT_PLAYLIST_URI).toHaveCount(10);
    }

    @Test
    public void shouldLoadFavoritesAsPlaylist() throws Exception {
        insertTracksAsUri(Content.ME_FAVORITE.uri);
        pm.setUri(Content.ME_FAVORITES.uri, 1, 10696200l);
        expect(pm.getCurrentTrack().id).toEqual(10696200l);
        expect(pm.next()).toBeTrue();
        expect(pm.getCurrentTrack().id).toEqual(10602324l);
    }

    @Test
    public void shouldSaveAndRestoreFavoritesAsPlaylist() throws Exception {
        insertTracksAsUri(Content.ME_FAVORITE.uri);
        pm.setUri(Content.ME_FAVORITES.uri, 1, 10853436l);
        expect(pm.getCurrentTrack().id).toEqual(10853436l);
        pm.saveQueue(1000l);
        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(10853436l);
        expect(pm.getPosition()).toEqual(0);
    }

    @Test
    public void shouldSaveAndRestoreFavoritesAsPlaylistWithMovedTrack() throws Exception {
        insertTracksAsUri(Content.ME_FAVORITE.uri);
        pm.setUri(Content.ME_FAVORITES.uri, 1, 10696200l);
        expect(pm.getCurrentTrack().id).toEqual(10696200l);
        expect(pm.next()).toBeTrue();

        pm.saveQueue(1000l);

        expect(pm.reloadQueue()).toEqual(1000l);
        expect(pm.getCurrentTrackId()).toEqual(10602324l);
        expect(pm.getPosition()).toEqual(2);
    }

    @Test
    public void shouldSavePlaylistStateInUri() throws Exception {
        insertTracksAsUri(Content.ME_FAVORITE.uri);
        pm.setUri(Content.ME_FAVORITES.uri, 1, 10696200l);
        expect(pm.getCurrentTrack().id).toEqual(10696200l);
        expect(pm.next()).toBeTrue();
        expect(pm.getPlaylistState(123L)).toEqual(
          Content.ME_FAVORITES.uri + "?trackId=10602324&playlistPos=2&seekPos=123"
        );
    }

    @Test
    public void shouldSavePlaylistStateInUriWithSetPlaylist() throws Exception {
        pm.setPlaylist(createTracks(10, true, 0), 5);
        expect(pm.getCurrentTrack().id).toEqual(5L);
        expect(pm.getPlaylistState(123L)).toEqual(
            PlaylistManager.DEFAULT_PLAYLIST_URI + "?trackId=5&playlistPos=5&seekPos=123"
        );
    }

    @Test
    public void shouldSkipUnstreamableTrackNext() throws Exception {
        ArrayList<Playable> playables = new ArrayList<Playable>();
        playables.addAll(createTracks(1, true, 0));
        playables.addAll(createTracks(1, false, 1));

        pm.setPlaylist(playables, 0);
        expect(pm.getCurrentTrack().id).toEqual(0L);
        expect(pm.next()).toEqual(false);

        playables.addAll(createTracks(1, true, 2));
        pm.setPlaylist(playables, 0);
        expect(pm.getCurrentTrack().id).toEqual(0L);
        expect(pm.next()).toEqual(true);
        expect(pm.getCurrentTrack().id).toEqual(2L);
    }

    @Test
    public void shouldSkipUnstreamableTrackPrev() throws Exception {
        ArrayList<Playable> playables = new ArrayList<Playable>();
        playables.addAll(createTracks(1, false, 0));
        playables.addAll(createTracks(1, true, 1));

        pm.setPlaylist(playables, 1);
        expect(pm.getCurrentTrack().id).toEqual(1L);
        expect(pm.prev()).toEqual(false);

        playables.addAll(0, createTracks(1, true, 2));
        pm.setPlaylist(playables, 2);
        expect(pm.getCurrentTrack().id).toEqual(1L);
        expect(pm.prev()).toEqual(true);
        expect(pm.getCurrentTrack().id).toEqual(2L);
    }


    @Test
    public void shouldClearPlaylistState() throws Exception {
        pm.setPlaylist(createTracks(10, true, 0), 5);
        pm.saveQueue(1235);

        PlaylistManager.clearState(Robolectric.application);
        expect(pm.reloadQueue()).toEqual(0L);

        PlaylistManager pm2 = new PlaylistManager(Robolectric.application, new TrackCache(), USER_ID);
        expect(pm2.reloadQueue()).toEqual(0L);
        expect(pm2.getPosition()).toEqual(0);
        expect(pm2.length()).toEqual(0);
    }

    private void insertTracksAsUri(Uri uri) throws IOException {
        List<Parcelable> items = new ArrayList<Parcelable>();
        ScModel.getCollectionFromStream(getClass().getResourceAsStream("tracks.json"), AndroidCloudAPI.Mapper, Track.class, items);
        for (Parcelable p: items){
            SoundCloudApplication.TRACK_CACHE.put((Track) p);
        }

        expect(SoundCloudDB.bulkInsertParcelables(resolver, items, uri, USER_ID)).toEqual(4);

        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(3);
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
            SoundCloudDB.insertTrack(resolver, t);
            list.add(t);
        }
        return list;
    }
}
