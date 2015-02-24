package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
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

        PublicApiPlaylist playlist = new PublicApiPlaylist(123L);
        playlist.setTrackCount(10);

        PlaylistActivity playlistActivity = new PlaylistActivity();
        playlistActivity.playlist = playlist;

        ActivitiesAdapter baseAdapter = new ActivitiesAdapter(Content.ME_SOUND_STREAM.uri);
        receiver = new PlaylistChangedReceiver(baseAdapter);
        baseAdapter.addItems(Lists.<Activity>newArrayList(playlistActivity));

        final Intent intent = new Intent(PublicApiPlaylist.ACTION_CONTENT_CHANGED);
        intent.putExtra(PublicApiPlaylist.EXTRA_ID, 123L);
        intent.putExtra(PublicApiPlaylist.EXTRA_TRACKS_COUNT, 30);
        receiver.onReceive(Robolectric.application, intent);
        expect(playlist.getTrackCount()).toEqual(30);
    }
}
