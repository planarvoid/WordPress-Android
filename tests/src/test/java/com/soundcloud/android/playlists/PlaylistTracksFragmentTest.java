package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTracksFragmentTest {


    private PlaylistTracksFragment fragment;

    @Mock
    private FragmentActivity activity;
    @Mock
    private Playlist playlist;
    @Mock
    private Bundle bundle;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistTracksFragment();
        when(activity.getContentResolver()).thenReturn(Robolectric.application.getContentResolver());
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        when(bundle.containsKey(Playlist.EXTRA)).thenReturn(true);
        when(bundle.getParcelable(Playlist.EXTRA)).thenReturn(playlist);

    }

    @Test
    public void shouldSyncMyPlaylistsIfPlaylistIsLocal() throws Exception {
        when(playlist.isLocal()).thenReturn(true);
        when(playlist.toUri()).thenReturn(Uri.parse("fake/uri"));

        fragment.setArguments(bundle);
        fragment.onCreate(null);
        fragment.onRefresh(null);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startService(captor.capture());
        expect(captor.getValue().getData()).toEqual(Content.ME_PLAYLISTS.uri);

    }

    @Test
    public void shouldSyncPlaylistIfPlaylistIsRemote() throws Exception {
        when(playlist.isLocal()).thenReturn(false);

        final Uri uri = Uri.parse("fake/uri");
        when(playlist.toUri()).thenReturn(uri);

        fragment.setArguments(bundle);
        fragment.onCreate(null);
        fragment.onRefresh(null);

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startService(captor.capture());
        expect(captor.getValue().getData()).toEqual(uri);

    }
}
