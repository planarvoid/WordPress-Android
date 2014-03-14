package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

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
    private PlaybackStateProvider playbackStateProvider;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private EngagementsController engagementsController;
    @Mock
    private PlaylistTracksAdapter adapter;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistTracksFragment(playbackOperations, playlistOperations, playbackStateProvider,
                imageOperations, syncStateManager, engagementsController, adapter);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(playlist));
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldSyncMyPlaylistsIfPlaylistIsLocal() throws Exception {
        playlist.setId(-123);

        createFragmentView();

        Intent intent = Robolectric.shadowOf(activity).getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(Content.ME_PLAYLISTS.uri);
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldSyncPlaylistIfPlaylistIsRemote() throws Exception {
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(new LocalCollection(playlist.toUri()));
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(
                Observable.<Playlist>error(new NotFoundException(playlist.getId())));

        createFragmentView();

        Intent intent = Robolectric.shadowOf(activity).getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData()).toEqual(playlist.toUri());
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldPlayPlaylistOnToggleToPlayState() throws Exception {
        Track firstTrack = new Track(1);
        when(adapter.getItem(0)).thenReturn(firstTrack);
        createFragmentView();

        fragment.onClick(mock(ToggleButton.class));

        verify(playbackOperations).playFromPlaylist(any(Context.class), eq(playlist), eq(0), eq(firstTrack), eq(Screen.SIDE_MENU_STREAM));
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldPlayPlaylistOnToggleToPauseState() throws Exception {
        when(playbackStateProvider.getPlayQueuePlaylistId()).thenReturn(playlist.getId());
        createFragmentView();

        fragment.onClick(mock(ToggleButton.class));

        verify(playbackOperations).togglePlayback(any(Context.class));
    }

    @Ignore("Fails with an error on PTR, try again when we moved to ActionBar PTR")
    @Test
    public void shouldSetToggleToPlayStateWhenPlayingCurrentPlaylistOnResume() throws Exception {
        View layout = createFragmentView();
        fragment.onResume();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.isChecked()).toBeTrue();
    }

    private void setFragmentArguments() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Playlist.EXTRA_URI, playlist.toUri());
        Screen.SIDE_MENU_STREAM.addToBundle(bundle);
        fragment.setArguments(bundle);
    }

    private View createFragmentView() {
        setFragmentArguments();
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
