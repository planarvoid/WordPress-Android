package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncStateManager;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTracksFragmentTest {


    private PlaylistTracksFragment fragment;
    private Playlist playlist = new Playlist(1L);

    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlaylistOperations playlistOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private FragmentActivity activity;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistTracksFragment(playbackOperations, playlistOperations, imageOperations, syncStateManager);
        when(activity.getApplicationContext()).thenReturn(Robolectric.application);
        when(activity.getContentResolver()).thenReturn(Robolectric.application.getContentResolver());
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(playlist));

        Bundle bundle = new Bundle();
        bundle.putParcelable(Playlist.EXTRA_URI, playlist.toUri());
        fragment.setArguments(bundle);
    }

    @Test
    public void shouldSyncMyPlaylistsIfPlaylistIsLocal() throws Exception {
        playlist.setId(-1);

        fragment.onCreate(null);
        fragment.onRefresh(null);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startService(captor.capture());
        expect(captor.getValue().getData()).toEqual(Content.ME_PLAYLISTS.uri);

    }

    @Test
    public void shouldSyncPlaylistIfPlaylistIsRemote() throws Exception {
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(new LocalCollection(playlist.toUri()));

        fragment.onCreate(null);
        fragment.onRefresh(null);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startService(captor.capture());
        expect(captor.getValue().getData()).toEqual(playlist.toUri());

    }
}
