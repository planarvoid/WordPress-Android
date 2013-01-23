package com.soundcloud.android.service.playback;

import static org.mockito.Mockito.*;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

@RunWith(DefaultTestRunner.class)
public class CloudPlaybackServiceTest {

    private CloudPlaybackService service;
    private @Mock AssociationManager associationManager;

    @Before
    public void setup() {
        service = new CloudPlaybackService();
        service.onCreate();
        service.setAssociationManager(associationManager);
    }

    @Test
    public void shouldAddLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.ADD_LIKE_ACTION);
        intent.setData(track.toUri());

        service.sendBroadcast(intent);

        verify(associationManager).setLike(isA(Track.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForTrackViaIntent() throws Exception {
        Track track = TestHelper.readJson(Track.class, "/com/soundcloud/android/model/track.json");
        SoundCloudApplication.MODEL_MANAGER.cache(track);

        Intent intent = new Intent(CloudPlaybackService.REMOVE_LIKE_ACTION);
        intent.setData(track.toUri());

        service.sendBroadcast(intent);

        verify(associationManager).setLike(isA(Track.class), eq(false));
    }

    @Test
    public void shouldAddLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.ADD_LIKE_ACTION);
        intent.setData(playlist.toUri());

        service.sendBroadcast(intent);

        verify(associationManager).setLike(isA(Playlist.class), eq(true));
    }

    @Test
    public void shouldRemoveLikeForPlaylistViaIntent() throws Exception {
        Playlist playlist = TestHelper.readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);

        Intent intent = new Intent(CloudPlaybackService.REMOVE_LIKE_ACTION);
        intent.setData(playlist.toUri());

        service.sendBroadcast(intent);

        verify(associationManager).setLike(isA(Playlist.class), eq(false));
    }

}
