package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import com.xtremelabs.robolectric.tester.android.database.SimpleTestCursor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistStorageTest {
    private PlaylistStorage storage;

    @Mock
    private PlaylistDAO playlistDAO;
    @Mock
    private TrackDAO trackDAO;
    @Mock
    private ContentResolver resolver;
    @Mock
    private ScModelManager modelManager;

    @Before
    public void before() throws IOException {
        storage = new PlaylistStorage(resolver, playlistDAO, trackDAO, modelManager);
    }

    @Test
    public void shouldLoadExistingPlaylist() throws Exception {
        Playlist playlist = new Playlist(1L);
        when(playlistDAO.queryById(1L)).thenReturn(playlist);
        when(modelManager.cache(playlist)).thenReturn(playlist);

        Playlist loadedPlaylist = storage.loadPlaylist(1L);

        expect(loadedPlaylist).not.toBeNull();
        expect(loadedPlaylist).toEqual(playlist);
        expect(loadedPlaylist.getTrackCount()).toEqual(0);
        expect(loadedPlaylist.tracks).toNumber(0);
    }

    @Test
    public void shouldLoadExistingPlaylistWithTracks() throws NotFoundException {
        Playlist playlist = new Playlist(1L);
        when(playlistDAO.queryById(1L)).thenReturn(playlist);
        when(modelManager.cache(playlist)).thenReturn(playlist);
        when(trackDAO.queryAllByUri(Content.PLAYLIST_TRACKS.forQuery("1"))).thenReturn(Arrays.asList(new Track()));

        Playlist loadedPlaylist = storage.loadPlaylistWithTracks(1L);
        expect(loadedPlaylist).not.toBeNull();
        expect(loadedPlaylist).toEqual(playlist);
        expect(loadedPlaylist.getTrackCount()).toEqual(1);
        expect(loadedPlaylist.tracks).toNumber(1);
    }

    @Test
    public void shouldCachePlaylistTracksAfterLoading() throws NotFoundException {
        Playlist playlist = new Playlist(1L);
        when(playlistDAO.queryById(1L)).thenReturn(playlist);
        when(modelManager.cache(playlist)).thenReturn(playlist);
        final Track track = new Track();
        when(trackDAO.queryAllByUri(Content.PLAYLIST_TRACKS.forQuery("1"))).thenReturn(Arrays.asList(track));

        Playlist loadedPlaylist = storage.loadPlaylistWithTracks(1L);
        loadedPlaylist.getTracks().get(0); // access the first track should trigger the cache

        verify(modelManager).cache(track);
    }

    @Test
    public void shouldStorePlaylist() throws Exception {
        Playlist playlist = new Playlist(1L);
        playlist.tracks = Lists.newArrayList(new Track(1L));

        storage.store(playlist);

        verify(playlistDAO).create(playlist);
    }

    @Test
    public void shouldGetPlaylistsCreatedByUser() {
        Playlist playlist = new Playlist(1L);
        Track track = new Track();

        Cursor cursor = new FakeCursor(1);
        when(resolver.query(Content.PLAYLISTS.uri, null, DBHelper.SoundView._ID + " < 0", null, DBHelper.SoundView._ID + " DESC")).thenReturn(cursor);
        when(modelManager.getCachedPlaylistFromCursor(cursor)).thenReturn(playlist);
        when(modelManager.cache(track)).thenReturn(track);
        when(trackDAO.queryAllByUri(Content.PLAYLIST_TRACKS.forId(playlist.getId()))).thenReturn(Arrays.asList(track));

        List<Playlist> playlists = storage.getLocalPlaylists();
        expect(playlists).toContainExactly(playlist);
        expect(playlist.tracks).toContainExactly(track);
    }

    @Test
    public void shouldAddTrackToPlaylist() throws Exception {
        Playlist playlist = new Playlist(1L);
        Track track = new Track();
        expect(playlist.getTrackCount()).toBe(0);

        storage.addTrackToPlaylist(playlist, track.getId());

        ArgumentCaptor<ContentValues> cv = ArgumentCaptor.forClass(ContentValues.class);
        verify(resolver).insert(eq(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.getId()))), cv.capture());

        expect(playlist.getTrackCount()).toBe(1);
        expect(cv.getValue().getAsLong(DBHelper.PlaylistTracks.PLAYLIST_ID)).toBe(playlist.getId());
        expect(cv.getValue().getAsLong(DBHelper.PlaylistTracks.TRACK_ID)).toBe(track.getId());
        expect(cv.getValue().getAsLong(DBHelper.PlaylistTracks.ADDED_AT)).toBeGreaterThan(0L);
        expect(cv.getValue().getAsLong(DBHelper.PlaylistTracks.POSITION)).toBe(1L);
    }

    @Test
    public void shouldSyncChangesToExistingPlaylists() throws Exception {
        Cursor cursor = new FakeCursor(new Object[][] {{1L}});

        when(resolver.query(Content.PLAYLIST_ALL_TRACKS.uri, new String[]{DBHelper.PlaylistTracks.PLAYLIST_ID},
                DBHelper.PlaylistTracks.ADDED_AT + " IS NOT NULL AND "
                        + DBHelper.PlaylistTracks.PLAYLIST_ID + " > 0", null, null)).thenReturn(cursor);

        Set<Uri> urisToSync = storage.getPlaylistsDueForSync();
        expect(urisToSync.size()).toEqual(1);
        expect(urisToSync.contains(Content.PLAYLIST.forId(1L))).toBeTrue();
    }

    //TODO: this does not yet test purging of playlist activity records
    @Test
    public void shouldRemovePlaylistAndAllDependentResources() {
        Playlist playlist = new Playlist(1L);

        storage.removePlaylist(playlist.toUri());

        verify(resolver).delete(Content.PLAYLIST.forQuery("1"), null, null);
        verify(resolver).delete(Content.PLAYLIST_TRACKS.forQuery("1"), null, null);

        // delete from collections
        String where = DBHelper.CollectionItems.ITEM_ID + " = 1 AND "
                + DBHelper.CollectionItems.RESOURCE_TYPE + " = " + Playable.DB_TYPE_PLAYLIST;

        verify(resolver).delete(Content.ME_PLAYLISTS.uri, where, null);
        verify(resolver).delete(Content.ME_SOUNDS.uri, where, null);
        verify(resolver).delete(Content.ME_LIKES.uri, where, null);

        // delete from activities
        where = DBHelper.Activities.SOUND_ID + " = 1 AND " +
                DBHelper.ActivityView.TYPE + " IN ( " + Activity.getDbPlaylistTypesForQuery() + " ) ";
        verify(resolver).delete(Content.ME_ALL_ACTIVITIES.uri, where, null);
    }

    @Test
    public void shouldLoadPlaylistTrackIds() throws Exception {
        final ArrayList<Long> idList = Lists.newArrayList(55724706L, 36831713L, 55104293L);
        when(trackDAO.queryIdsByUri(Content.PLAYLIST_TRACKS.forId(1L))).thenReturn(idList);
        expect(storage.getPlaylistTrackIds(1L)).toEqual(idList);
    }

    @Test
    public void shouldReturnUnpushedTracksForPlaylist() throws Exception {
        Cursor cursor = new FakeCursor(new Object[][] {{2L}, {3L}});

        when(resolver.query(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(1L)),
                new String[]{DBHelper.PlaylistTracksView._ID},
                DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " IS NOT NULL", null,
                DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " ASC")).thenReturn(cursor);

        expect(storage.getUnpushedTracksForPlaylist(1L)).toEqual(Lists.newArrayList( 2L, 3L));
    }


    private final class FakeCursor extends SimpleTestCursor {

        private int count;

        private FakeCursor(int count) {
            this.count = count;
            setResults(new Object[count][]);
        }

        private FakeCursor(Object[][] fakeResults) {
            this.count = fakeResults.length;
            setResults(fakeResults);
        }

        @Override
        public int getCount() {
            return count;
        }
    }
}
