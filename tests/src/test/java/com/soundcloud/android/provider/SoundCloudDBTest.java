package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.readJson;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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
public class SoundCloudDBTest {
    ContentResolver resolver;
    final static long USER_ID = 1L;

    @Before
    public void before() {
        resolver = DefaultTestRunner.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldBulkInsert() throws Exception {
        List<ScResource> items = createParcelables();
        expect(SoundCloudDB.bulkInsertResources(resolver, items)).toEqual(3);
    }

    @Test
    public void shouldBulkInsertWithCollections() throws Exception {
        List<ScResource> items = createParcelables();
        expect(SoundCloudDB.insertCollection(resolver, items, Content.ME_LIKES.uri, USER_ID)).toEqual(6);

        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
    }

    @Test
    public void shouldBulkInsertPlaylistTracksAndUpdateTrackCount() throws IOException {
        TrackHolder tracks = readJson(TrackHolder.class, "/com/soundcloud/android/provider/tracks.json");
        final long userId = 1;
        final long playlistId = 1;
        final Uri playlistUri = Content.PLAYLIST.forId(playlistId);
        final Uri playlistTracksUri = Content.PLAYLIST_TRACKS.forId(playlistId);

        // create a playlist
        resolver.insert(playlistUri, new Playlist(playlistId).buildContentValues());

        // add playlist tracks
        SoundCloudDB.insertCollection(resolver, tracks.collection, playlistTracksUri, userId);

        Cursor cursor = resolver.query(playlistTracksUri, null, null, null, null);
        expect(cursor.getCount()).toBe(3);
        cursor.close();

        cursor = resolver.query(playlistUri, null, null, null, null);
        expect(cursor.getCount()).toBe(1);
        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor.getInt(cursor.getColumnIndex(DBHelper.Sounds.TRACK_COUNT))).toBe(3);
        cursor.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBulkInsertWithoutOwnerId() throws Exception {
        SoundCloudDB.insertCollection(resolver, createParcelables(), Content.ME_LIKES.uri, -1);
    }

    private List<ScResource> createParcelables() {
        List<ScResource> items = new ArrayList<ScResource>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;

        User u2 = new User();
        u2.permalink = "u2";
        u2.id = 300L;

        User u2_ = new User();
        u2_.permalink = "u2";
        u2_.id = 300L;

        items.add(u1);
        items.add(t);
        items.add(u2_);
        items.add(null);
        return items;
    }
}
