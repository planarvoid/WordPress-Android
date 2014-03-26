package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import javax.inject.Provider;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistFragmentTest {

    private PlaylistFragment fragment;
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
    private EngagementsController engagementsController;
    @Mock
    private ItemAdapter<Track> adapter;
    @Mock
    private PlaylistDetailsController controller;

    private Provider<PlaylistDetailsController> controllerProvider = new Provider<PlaylistDetailsController>() {
        @Override
        public PlaylistDetailsController get() {
            return controller;
        }
    };

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistFragment(playbackOperations, playlistOperations, playbackStateProvider,
                imageOperations, engagementsController, controllerProvider);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        when(controller.getAdapter()).thenReturn(adapter);
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(playlist));
    }

    @Test
    public void shouldNotShowPlayToggleButtonWithNoTracks() throws Exception {
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldHidePlayToggleButtonWithNoTracks() throws Exception {
        playlist.tracks = Lists.newArrayList(new Track(1L));
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.getVisibility()).toBe(View.VISIBLE);
    }


    @Test
    public void shouldHidePlayToggleButtonOnSecondPlaylistEmissionWithNoTracks() throws Exception {
        playlist.tracks = Lists.newArrayList(new Track(1L));
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(Arrays.asList(playlist, new Playlist(playlist.getId()))));
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.getVisibility()).toBe(View.GONE);
    }


    @Test
    public void shouldPlayPlaylistOnToggleToPlayState() throws Exception {
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        toggleButton.performClick();

        verify(playbackOperations).playPlaylist(any(Context.class), eq(playlist), eq(Screen.SIDE_MENU_STREAM));
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPauseState() throws Exception {
        when(playbackStateProvider.getPlayQueuePlaylistId()).thenReturn(playlist.getId());
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        toggleButton.performClick();

        verify(playbackOperations).togglePlayback(any(Context.class));
    }

    @Test
    public void shouldSetToggleToPlayStateWhenPlayingCurrentPlaylistOnResume() throws Exception {
        when(playbackStateProvider.isPlaylistPlaying(playlist.getId())).thenReturn(true);

        View layout = createFragmentView();
        fragment.onResume();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.isChecked()).toBeTrue();
    }

    @Test
     public void engagementsControllerStartsListeningInOnStart() throws Exception {
        fragment.onStart();
        verify(engagementsController).startListeningForChanges();
    }

    @Test
    public void engagementsControllerStopsListeningInOnStop() throws Exception {
        fragment.onStart(); // call on stop to avoid unregistered listener error
        fragment.onStop();
        verify(engagementsController).stopListeningForChanges();
    }

    @Test
    public void callsShowContentWhenPlaylistIsReturned() throws Exception {
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setListShown(false);
        inOrder.verify(controller).setListShown(true);
    }

    @Test
    public void callsShowContentWhenErrorIsReturned() throws Exception {
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.<Playlist>error(new Exception("something bad happened")));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setListShown(false);
        inOrder.verify(controller).setListShown(true);
    }

    @Test
    public void setsEmptyViewToOkWhenPlaylistIsReturned() throws Exception {
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyListView.Status.OK);
    }

    @Test
    public void setsEmptyViewToErrorWhenErrorIsReturned() throws Exception {
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.<Playlist>error(new Exception("something bad happened")));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyListView.Status.ERROR);
    }

    @Test
    public void setsPlayableOnEngagementsControllerWhenPlaylistIsReturned() throws Exception {
        createFragmentView();
        verify(engagementsController).setPlayable(playlist);
    }

    @Test
    public void setsPlayableOnEngagementsControllerTwiceWhenPlaylistEmittedTwice() throws Exception {
        Playlist playlist2 = new Playlist(2L);
        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(Arrays.asList(playlist, playlist2)));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(engagementsController);
        inOrder.verify(engagementsController).setPlayable(playlist);
        inOrder.verify(engagementsController).setPlayable(playlist2);
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() throws Exception {
        final Track track1 = Mockito.mock(Track.class);
        final Track track2 = Mockito.mock(Track.class);
        playlist.tracks = Lists.newArrayList(track1, track2);

        createFragmentView();

        InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track1);
        inOrder.verify(adapter).addItem(track2);
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterForEachPlaylistWhenPlaylistIsEmittedMultipleTimes() throws Exception {
        final Track track1 = new Track(1L);
        final Track track2 = new Track(2L);
        playlist.tracks = Lists.newArrayList(track1);
        Playlist playlist2 = new Playlist(playlist.getId());
        playlist2.tracks = Lists.newArrayList(track1, track2);

        when(playlistOperations.loadPlaylist(anyLong())).thenReturn(Observable.from(Arrays.asList(playlist, playlist2)));

        createFragmentView();

        InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track1);
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track1);
        inOrder.verify(adapter).addItem(track2);
    }

    private View createFragmentView() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Playlist.EXTRA_URI, playlist.toUri());
        Screen.SIDE_MENU_STREAM.addToBundle(bundle);
        fragment.setArguments(bundle);
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
