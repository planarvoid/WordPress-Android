package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.model.activities.PlaylistActivity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistChangedReceiverTest {

    PlaylistChangedReceiver receiver;

    @Mock
    ImageOperations imageOperations;

    @Test
    public void shouldHandlePlaylistActivityChange() throws IOException {

        Playlist playlist = new Playlist(123L);
        playlist.setTrackCount(10);

        PlaylistActivity playlistActivity = new PlaylistActivity();
        playlistActivity.playlist = playlist;

        ActivitiesAdapter baseAdapter = new ActivitiesAdapter(Content.ME_SOUND_STREAM.uri, imageOperations);
        receiver = new PlaylistChangedReceiver(baseAdapter);
        baseAdapter.addItems(Lists.<Activity>newArrayList(playlistActivity));

        final Intent intent = new Intent(Playlist.ACTION_CONTENT_CHANGED);
        intent.putExtra(Playlist.EXTRA_ID, 123L);
        intent.putExtra(Playlist.EXTRA_TRACKS_COUNT, 30);
        receiver.onReceive(Robolectric.application, intent);
        expect(playlist.getTrackCount()).toEqual(30);
    }
}
