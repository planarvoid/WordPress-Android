package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(DefaultTestRunner.class)
public class PlaylistStorageTest {
    Playlist playlist;
    PlaylistStorage storage;

    @Before
    public void before() throws IOException {
        storage = new PlaylistStorage();
        playlist = TestHelper.readResource("/com/soundcloud/android/sync/playlist.json");
        expect(playlist).not.toBeNull();
    }

    @Test
    public void shouldLoadExistingPlaylist() throws Exception {
        TestHelper.insert(playlist);

        playlist = storage.loadPlaylist(2524386L);

        expect(playlist).not.toBeNull();
        expect(playlist.getId()).toEqual(2524386L);
    }

    @Test
    public void shouldCreatePlaylistWithTracks() throws Exception {
        expect(playlist.user.username).toEqual("Natalie");
        expect(playlist.tracks.size()).toEqual(41);

        playlist = storage.storeAsync(playlist).toBlockingObservable().last();
        expect(playlist.getId()).toEqual(2524386L);
        expect(Content.TRACKS).toHaveCount(41);
        expect(Content.PLAYLIST_ALL_TRACKS).toHaveCount(41);
    }

    @Test
    public void shouldGetPlaylistsCreatedByUser() {
        final List<Track> tracks = createTracks(2);
        TestHelper.createNewUserPlaylist(tracks.get(0).user, true, tracks);

        List<Playlist> playlists = storage.getLocalPlaylists();
        expect(playlists.size()).toBe(1);
        expect(playlists.get(0).tracks).toContainExactly(tracks.get(0), tracks.get(1));
    }

    @Test
    public void shouldAddTrackToPlaylist() throws Exception {
        expect(playlist.tracks.size()).toEqual(41);
        TestHelper.insertWithDependencies(playlist);
        List<Track> tracks = createTracks(2);
        TestHelper.bulkInsert(tracks);

        for (Track track : tracks){
            storage.addTrackToPlaylist(playlist, track.getId());
        }

        Playlist p2 = TestHelper.loadPlaylist(playlist.getId());

        expect(p2).not.toBeNull();
        expect(p2.tracks.size()).toEqual(43);
        expect(p2.tracks.get(41).getId()).toEqual(tracks.get(0).getId()); // check ordering
        expect(p2.tracks.get(42).getId()).toEqual(tracks.get(1).getId()); // check ordering
    }

    @Test
    public void shouldSyncChangesToExistingPlaylists() throws Exception {
        final Playlist playlist = new Playlist(12345);
        expect(storage.addTrackToPlaylist(playlist, 10696200, System.currentTimeMillis())).not.toBeNull();
        // local unpushed playlists (those with a negative timestamp) are not part of this sync step
        expect(storage.addTrackToPlaylist(new Playlist(-34243), 10696200, System.currentTimeMillis())).not.toBeNull();

        Set<Uri> urisToSync = storage.getPlaylistsDueForSync();
        expect(urisToSync.size()).toEqual(1);
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist.getId()))).toBeTrue();
    }

    @Test
    public void shouldSyncMultiplePlaylists() throws Exception {
        final Playlist playlist1 = new Playlist(12345);
        final Playlist playlist2 = new Playlist(544321);

        expect(storage.addTrackToPlaylist(playlist1, 10696200, System.currentTimeMillis())).not.toBeNull();
        expect(storage.addTrackToPlaylist(playlist2, 10696200, System.currentTimeMillis())).not.toBeNull();

        Set<Uri> urisToSync = storage.getPlaylistsDueForSync();
        expect(urisToSync.size()).toEqual(2);
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist1.getId()))).toBeTrue();
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist2.getId()))).toBeTrue();
    }

    //TODO: this does not yet test purging of playlist activity records
    @Test
    public void shouldRemovePlaylistAndAllDependentResources() {
        TestHelper.insertWithDependencies(playlist);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_LIKE);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_REPOST);
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST);

        expect(Content.TRACKS).toHaveCount(41);
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.PLAYLIST_ALL_TRACKS).toHaveCount(41);
        expect(Content.ME_LIKES).toHaveCount(1);
        expect(Content.ME_REPOSTS).toHaveCount(1);
        expect(Content.ME_PLAYLISTS).toHaveCount(1);

        storage.removePlaylist(playlist.toUri());

        expect(Content.TRACKS).toHaveCount(41); // referenced tracks should NOT be removed
        expect(Content.PLAYLISTS).toHaveCount(0);
        expect(Content.PLAYLIST_ALL_TRACKS).toHaveCount(0);
        expect(Content.ME_LIKES).toHaveCount(0);
        expect(Content.ME_REPOSTS).toHaveCount(0);
        expect(Content.ME_PLAYLISTS).toHaveCount(0);
    }

    private static List<Track> createTracks(int n) {
        List<Track> items = new ArrayList<Track>(n);

        for (int i=0; i<n; i++) {
            User user = new User();
            user.permalink = "u"+i;
            user.setId(i);

            Track track = new Track();
            track.setId(i);
            track.user = user;
            items.add(track);
        }
        return items;
    }
}
