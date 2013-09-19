package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.PlaylistActivity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistChangedReceiverTest {

    PlaylistChangedReceiver receiver;

    @Test
    public void shouldHandlePlaylistActivityChange() throws IOException {

        Playlist playlist = new Playlist(123L);
        playlist.setTrackCount(10);

        PlaylistActivity playlistActivity = new PlaylistActivity();
        playlistActivity.playlist = playlist;

        ActivityAdapter baseAdapter = new ActivityAdapter(Content.ME_SOUND_STREAM.uri);
        receiver = new PlaylistChangedReceiver(baseAdapter);
        baseAdapter.addItems(Lists.<Activity>newArrayList(playlistActivity));

        final Intent intent = new Intent(Playlist.ACTION_CONTENT_CHANGED);
        intent.putExtra(Playlist.EXTRA_ID, 123L);
        intent.putExtra(Playlist.EXTRA_TRACKS_COUNT, 30);
        receiver.onReceive(Robolectric.application, intent);
        expect(playlist.getTrackCount()).toEqual(30);
    }
}
