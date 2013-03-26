package com.soundcloud.android.dao;

import android.net.Uri;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class PlaylistStorageTest {
    Playlist playlist;
    PlaylistStorage storage;

    @Before
    public void before() throws IOException {
        storage = new PlaylistStorage(Robolectric.application);
        playlist = TestHelper.readResource("/com/soundcloud/android/service/sync/playlist.json");
        expect(playlist).not.toBeNull();
    }

    @Test
    public void shouldCreatePlaylistLocally() throws Exception {
        final List<Track> tracks = createTracks(2);
        final boolean isPrivate = false;

        Playlist p = storage.createNewPlaylist(tracks.get(0).user, isPrivate, tracks);
        final Uri uri = p.toUri();

        Uri myPlaylistUri = storage.insertAsMyPlaylist(p);

        expect(myPlaylistUri).not.toBeNull();
        expect(Content.match(myPlaylistUri)).toBe(Content.ME_PLAYLIST);

        expect(Content.ME_PLAYLISTS).toHaveCount(1);
        Playlist p2 = storage.getPlaylistWithTracks(uri);
        expect(p2.tracks).toEqual(tracks);


        expect(p2.sharing).toBe(isPrivate ? Sharing.PRIVATE : Sharing.PUBLIC);

        List<Playlist> playlists = storage.getLocalPlaylists();
        expect(playlists.size()).toBe(1);
    }

    @Test
    public void shouldAddTrackToPlaylist() throws Exception {
        expect(playlist.tracks.size()).toEqual(41);
        storage.create(playlist);
        List<Track> tracks = createTracks(2);
        TestHelper.bulkInsert(tracks);

        for (Track track : tracks){
            final Uri insert = storage.addTrackToPlaylist(playlist, track.id, System.currentTimeMillis());
            expect(insert).not.toBeNull();
        }

        Playlist p2 = storage.getPlaylistWithTracks(playlist.id);

        expect(p2).not.toBeNull();
        expect(p2.tracks.size()).toEqual(43);
        expect(p2.tracks.get(41).id).toEqual(tracks.get(0).id); // check ordering
        expect(p2.tracks.get(42).id).toEqual(tracks.get(1).id); // check ordering
    }

    @Test
    public void shouldCreatePlaylist() throws Exception {
        expect(playlist.user.username).toEqual("Natalie");
        expect(playlist.tracks.size()).toEqual(41);

        long id = storage.create(playlist);
        expect(id).toEqual(2524386L);

        Playlist p2 = storage.getPlaylistWithTracks(id);

        expect(p2).not.toBeNull();
        expect(p2.user.username).toEqual("Natalie");
        expect(p2.tracks.size()).toEqual(41);

        expect(playlist.tracks.get(0).id).toEqual(p2.tracks.get(0).id);

        p2.tracks.remove(0);

        expect(storage.update(p2)).toBeTrue();

        Playlist p3 = storage.getPlaylistWithTracks(id);
        expect(p3).not.toBeNull();
        expect(p3.tracks.size()).toEqual(40);
        expect(p3.tracks.get(0).id).not.toEqual(playlist.tracks.get(0).id);
    }


    private static List<Track> createTracks(int n) {
        List<Track> items = new ArrayList<Track>(n);

        for (int i=0; i<n; i++) {
            User user = new User();
            user.permalink = "u"+i;
            user.id = i;

            Track track = new Track();
            track.id = i;
            track.user = user;
            items.add(track);
        }
        return items;
    }

    @Test
    public void shouldSyncChangesToExistingPlaylists() throws Exception {
        final Playlist playlist = new Playlist(12345);
        expect(storage.addTrackToPlaylist(playlist, 10696200, System.currentTimeMillis())).not.toBeNull();
        // local unpushed playlists (those with a negative timestamp) are not part of this sync step
        expect(storage.addTrackToPlaylist(new Playlist(-34243), 10696200, System.currentTimeMillis())).not.toBeNull();

        Set<Uri> urisToSync = storage.getPlaylistsDueForSync();
        expect(urisToSync.size()).toEqual(1);
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist.id))).toBeTrue();
    }

    @Test
    public void shouldSyncMultiplePlaylists() throws Exception {
        final Playlist playlist1 = new Playlist(12345);
        final Playlist playlist2 = new Playlist(544321);

        expect(storage.addTrackToPlaylist(playlist1, 10696200, System.currentTimeMillis())).not.toBeNull();
        expect(storage.addTrackToPlaylist(playlist2, 10696200, System.currentTimeMillis())).not.toBeNull();

        Set<Uri> urisToSync = storage.getPlaylistsDueForSync();
        expect(urisToSync.size()).toEqual(2);
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist1.id))).toBeTrue();
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist2.id))).toBeTrue();
    }
}
