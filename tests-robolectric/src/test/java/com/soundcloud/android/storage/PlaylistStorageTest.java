package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
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
        storage = new PlaylistStorage(resolver, playlistDAO, modelManager);
    }

    @Test
    public void shouldLoadExistingPlaylist() throws Exception {
        PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        when(playlistDAO.queryById(1L)).thenReturn(playlist);
        when(modelManager.cache(playlist)).thenReturn(playlist);

        PublicApiPlaylist loadedPlaylist = storage.loadPlaylist(1L);

        expect(loadedPlaylist).not.toBeNull();
        expect(loadedPlaylist).toEqual(playlist);
        expect(loadedPlaylist.getTrackCount()).toEqual(0);
        expect(loadedPlaylist.tracks).toNumber(0);
    }

    @Test
    public void shouldAddTrackToPlaylist() throws Exception {
        PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        PublicApiTrack track = new PublicApiTrack();
        expect(playlist.getTrackCount()).toBe(0);

        storage.addTrackToPlaylist(playlist, track.getId());

        ArgumentCaptor<ContentValues> cv = ArgumentCaptor.forClass(ContentValues.class);
        verify(resolver).insert(eq(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.getId()))), cv.capture());

        expect(playlist.getTrackCount()).toBe(1);
        expect(cv.getValue().getAsLong(TableColumns.PlaylistTracks.PLAYLIST_ID)).toBe(playlist.getId());
        expect(cv.getValue().getAsLong(TableColumns.PlaylistTracks.TRACK_ID)).toBe(track.getId());
        expect(cv.getValue().getAsLong(TableColumns.PlaylistTracks.ADDED_AT)).toBeGreaterThan(0L);
        expect(cv.getValue().getAsLong(TableColumns.PlaylistTracks.POSITION)).toBe(1L);
    }

    @Test
    public void shouldSyncChangesToExistingPlaylists() throws Exception {
        Cursor cursor = new TestCursor(new Object[][] {{1L}});

        when(resolver.query(Content.PLAYLIST_ALL_TRACKS.uri, new String[]{TableColumns.PlaylistTracks.PLAYLIST_ID},
                TableColumns.PlaylistTracks.ADDED_AT + " IS NOT NULL AND "
                        + TableColumns.PlaylistTracks.PLAYLIST_ID + " > 0", null, null)).thenReturn(cursor);

        Set<Uri> urisToSync = storage.getPlaylistsDueForSync();
        expect(urisToSync.size()).toEqual(1);
        expect(urisToSync.contains(Content.PLAYLIST.forId(1L))).toBeTrue();
    }
}
