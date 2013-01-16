package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class PlaylistTest {
    @Test
    public void shouldDeserializePlaylist() throws Exception {
        Playlist p = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        expect(p.title).toEqual("PA600QT Demos");
        expect(p.description).toEqual("blah blah blah");
        expect(p.release_year).toEqual(1992);
        expect(p.user_id).toEqual(1196047l);
        expect(p.tracks.size()).toEqual(14);
    }

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        Playlist playlist = AndroidCloudAPI.Wrapper.buildObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        Parcel p = Parcel.obtain();
        playlist.writeToParcel(p, 0);

        Playlist playlist2 = new Playlist(p);
        comparePlaylists(playlist, playlist2);
    }

    private void comparePlaylists(Playlist playlist, Playlist playlist1) {
        expect(playlist1.id).toEqual(playlist.id);
        expect(playlist1.title).toEqual(playlist.title);
        expect(playlist1.permalink).toEqual(playlist.permalink);
        expect(playlist1.duration).toBeGreaterThan(0);
        expect(playlist1.duration).toEqual(playlist.duration);
        expect(playlist1.created_at).toEqual(playlist.created_at);
        expect(playlist1.tag_list).toEqual(playlist.tag_list);
        expect(playlist1.permalink_url).toEqual(playlist.permalink_url);
        expect(playlist1.artwork_url).toEqual(playlist.artwork_url);
        expect(playlist1.downloadable).toEqual(playlist.downloadable);
        expect(playlist1.streamable).toEqual(playlist.streamable);
        expect(playlist1.user_id).toEqual(playlist.user_id);
    }
}
