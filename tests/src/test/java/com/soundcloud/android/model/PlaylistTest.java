package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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
        Playlist p = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        System.out.print(p);
    }


    private void comparePlaylists(Playlist playlist, Playlist playlist1) {
        expect(playlist1.id).toEqual(playlist.id);
        expect(playlist1.title).toEqual(playlist.title);
        expect(playlist1.permalink).toEqual(playlist.permalink);
        expect(playlist1.duration).toBeGreaterThan(0);
        expect(playlist1.duration).toEqual(playlist.duration);
        expect(playlist1.created_at).toEqual(playlist.created_at);
        expect(playlist1.tag_list).toEqual(playlist.tag_list);
        expect(playlist1.track_type).toEqual(playlist.track_type);
        expect(playlist1.permalink_url).toEqual(playlist.permalink_url);
        expect(playlist1.artwork_url).toEqual(playlist.artwork_url);
        expect(playlist1.waveform_url).toEqual(playlist.waveform_url);
        expect(playlist1.downloadable).toEqual(playlist.downloadable);
        expect(playlist1.download_url).toEqual(playlist.download_url);
        expect(playlist1.streamable).toEqual(playlist.streamable);
        expect(playlist1.stream_url).toEqual(playlist.stream_url);
        expect(playlist1.playback_count).toEqual(playlist.playback_count);
        expect(playlist1.download_count).toEqual(playlist.download_count);
        expect(playlist1.comment_count).toEqual(playlist.comment_count);
        expect(playlist1.favoritings_count).toEqual(playlist.favoritings_count);
        expect(playlist1.shared_to_count).toEqual(playlist.shared_to_count);
        expect(playlist1.user_id).toEqual(playlist.user_id);
        expect(playlist1.commentable).toEqual(playlist.commentable);
    }
}
