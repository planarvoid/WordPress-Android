package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncStateManager;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTracksFragmentTest {

    private PlaylistTracksFragment fragment;
    private FragmentActivity activity = new FragmentActivity();
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
    private EngagementsController engagementsController;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistTracksFragment(playbackOperations, playlistOperations, imageOperations, syncStateManager,
                engagementsController);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(playlist));
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldSyncMyPlaylistsIfPlaylistIsLocal() throws Exception {
        playlist.setId(-123);
        setFragmentArguments();

        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        fragment.onViewCreated(layout, null);

        Intent intent = Robolectric.shadowOf(activity).getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.ME_PLAYLISTS.uri);
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldSyncPlaylistIfPlaylistIsRemote() throws Exception {
        setFragmentArguments();

        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(new LocalCollection(playlist.toUri()));
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(
                Observable.<Playlist>error(new NotFoundException(playlist.getId())));

        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        fragment.onViewCreated(layout, null);

        Intent intent = Robolectric.shadowOf(activity).getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(playlist.toUri());
    }

    private void setFragmentArguments() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Playlist.EXTRA_URI, playlist.toUri());
        fragment.setArguments(bundle);
    }

}
