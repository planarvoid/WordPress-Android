package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistFragmentTest {

    private PlaylistFragment fragment;
    private FragmentActivity activity = new FragmentActivity();
    private PublicApiPlaylist playlist = new PublicApiPlaylist(1L);

    @Mock private PlaylistDetailsController controller;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private LegacyPlaylistOperations playlistOperations;
    @Mock private PlaybackStateProvider playbackStateProvider;
    @Mock private ImageOperations imageOperations;
    @Mock private EngagementsController engagementsController;
    @Mock private ItemAdapter adapter;
    @Mock private PullToRefreshController ptrController;
    @Mock private PlayQueueManager playQueueManager;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistFragment(controller, playbackOperations, playlistOperations, playbackStateProvider,
                imageOperations, engagementsController, ptrController, playQueueManager);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        when(controller.getAdapter()).thenReturn(adapter);
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.from(playlist));
    }

    @Test
    public void shouldForwardOnViewCreatedEventToController() {
        View layout = createFragmentView();

        verify(controller).onViewCreated(layout, Robolectric.application.getResources());
    }

    @Test
    public void shouldForwardOnDestroyViewEventToController() {
        fragment.onDestroyView();

        verify(controller).onDestroyView();
    }

    @Test
    public void shouldNotShowPlayToggleButtonWithNoTracks() throws Exception {
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldHidePlayToggleButtonWithNoTracks() throws Exception {
        final PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playlist.tracks = Lists.newArrayList(track);
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.getVisibility()).toBe(View.VISIBLE);
    }


    @Test
    public void shouldHidePlayToggleButtonOnSecondPlaylistEmissionWithNoTracks() throws Exception {
        final PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playlist.tracks = Lists.newArrayList(track);
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.from(Arrays.asList(playlist, new PublicApiPlaylist(playlist.getId()))));
        View layout = createFragmentView();

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPlayState() throws Exception {
        View layout = createFragmentView();

        View toggleButton = layout.findViewById(R.id.toggle_play_pause);
        toggleButton.performClick();

        verify(playbackOperations).playPlaylist(eq(playlist), eq(Screen.SIDE_MENU_STREAM));
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPauseState() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getId())).thenReturn(true);
        View layout = createFragmentView();

        View toggleButton = layout.findViewById(R.id.toggle_play_pause);
        toggleButton.performClick();

        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void shouldSetToggleToPlayStateWhenPlayingCurrentPlaylistOnResume() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getId())).thenReturn(true);
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(true);

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

    public void shouldOpenUserProfileWhenUsernameTextIsClicked() throws Exception {
        PublicApiUser user = new PublicApiUser();
        playlist.setUser(user);
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.from(playlist));
        when(playQueueManager.getPlaylistId()).thenReturn(playlist.getId());
        View layout = createFragmentView();

        View usernameView = layout.findViewById(R.id.username);
        usernameView.performClick();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(intent.getParcelableExtra(ProfileActivity.EXTRA_USER)).toEqual(user);
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
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.<PublicApiPlaylist>error(new Exception("something bad happened")));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setListShown(false);
        inOrder.verify(controller).setListShown(true);
    }

    @Test
    public void setsEmptyViewToOkWhenPlaylistIsReturned() throws Exception {
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.OK);
    }

    @Test
    public void setsEmptyViewToErrorWhenErrorIsReturned() throws Exception {
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("something bad happened")));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.ERROR);
    }

    @Test
    public void setsPlayableOnEngagementsControllerWhenPlaylistIsReturned() throws Exception {
        createFragmentView();
        verify(engagementsController).setPlayable(playlist);
    }

    @Test
    public void setsPlayableOnEngagementsControllerTwiceWhenPlaylistEmittedTwice() throws Exception {
        PublicApiPlaylist playlist2 = new PublicApiPlaylist(2L);
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(
                Observable.from(Arrays.asList(playlist, playlist2)));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(engagementsController);
        inOrder.verify(engagementsController).setPlayable(playlist);
        inOrder.verify(engagementsController).setPlayable(playlist2);
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() throws Exception {
        final PublicApiTrack track1 = createTrackWithTitle("Track 1");
        final PublicApiTrack track2 = createTrackWithTitle("Track 2");

        playlist.tracks = Lists.newArrayList(track1, track2);

        createFragmentView();

        InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track1.toPropertySet());
        inOrder.verify(adapter).addItem(track2.toPropertySet());
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterForEachPlaylistWhenPlaylistIsEmittedMultipleTimes() throws Exception {
        final PublicApiTrack track1 = createTrackWithTitle("Track 1");
        final PublicApiTrack track2 = createTrackWithTitle("Title 2");
        playlist.tracks = Lists.newArrayList(track1);
        PublicApiPlaylist playlist2 = new PublicApiPlaylist(playlist.getId());
        playlist2.tracks = Lists.newArrayList(track1, track2);

        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(
                Observable.from(Arrays.asList(playlist, playlist2)));

        createFragmentView();

        InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track1.toPropertySet());
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track1.toPropertySet());
        inOrder.verify(adapter).addItem(track2.toPropertySet());
    }

    @Test
    public void updatesContentWhenRefreshIsSuccessful() throws CreateModelException {
        final PublicApiTrack track1 = createTrackWithTitle("Track 1");
        final PublicApiTrack track2 = createTrackWithTitle("Track 2");
        playlist.tracks = Lists.newArrayList(track1);
        PublicApiPlaylist playlist2 = new PublicApiPlaylist(playlist.getId());
        playlist2.tracks = Lists.newArrayList(track2);
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.from(playlist));
        when(playlistOperations.refreshPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.from(playlist2));

        createFragmentView();
        fragment.onRefreshStarted(mock(View.class));

        InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).addItem(track1.toPropertySet());
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track2.toPropertySet());
    }

    @Test
    public void showsToastErrorWhenContentAlreadyShownAndRefreshFails() {
        when(playlistOperations.refreshPlaylist(any(PlaylistUrn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefreshStarted(mock(View.class));

        expect(ShadowToast.getTextOfLatestToast()).toEqual("There seems to be a connection problem. Please try again shortly.");
    }

    @Test
    public void doesNotShowInlineErrorWhenContentWhenAlreadyShownAndRefreshFails() throws CreateModelException {
        final PublicApiTrack track = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        playlist.tracks = Lists.newArrayList(track);
        when(playlistOperations.loadPlaylist(any(PlaylistUrn.class))).thenReturn(Observable.from(playlist));
        when(playlistOperations.refreshPlaylist(any(PlaylistUrn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefreshStarted(mock(View.class));

        verify(controller, times(1)).setEmptyViewStatus(anyInt());
    }

    @Test
    public void hidesRefreshStateWhenRefreshFails() {
        when(playlistOperations.refreshPlaylist(any(PlaylistUrn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefreshStarted(mock(View.class));

        verify(ptrController, times(2)).stopRefreshing();
    }

    @Test
    public void detatchesPullToRefreshControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(ptrController).onDestroyView();
    }

    @Test
    public void shouldSetPlayingStateWhenPlaybackStateChanges() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getId())).thenReturn(true);
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(true);
        View layout = createFragmentView();
        fragment.onStart();

        Robolectric.getShadowApplication().sendBroadcast(new Intent(PlaybackService.Broadcasts.PLAYSTATE_CHANGED));

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.isChecked()).toBeTrue();
    }

    @Test
    public void shouldSetPlayingStateWhenPlaybackMetaChanges() throws Exception {
        when(playQueueManager.getPlaylistId()).thenReturn(0L);
        View layout = createFragmentView();
        fragment.onStart();

        Robolectric.getShadowApplication().sendBroadcast(new Intent(PlaybackService.Broadcasts.META_CHANGED));

        ToggleButton toggleButton = (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
        expect(toggleButton.isChecked()).toBeFalse();
    }

    private PublicApiTrack createTrackWithTitle(String title) throws com.tobedevoured.modelcitizen.CreateModelException {
        final PublicApiTrack model = TestHelper.getModelFactory().createModel(PublicApiTrack.class);
        model.setTitle(title);
        return model;
    }

    private View createFragmentView() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PublicApiPlaylist.EXTRA_URN, playlist.getUrn());
        Screen.SIDE_MENU_STREAM.addToBundle(bundle);
        fragment.setArguments(bundle);
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new RelativeLayout(activity), null);
        fragment.onViewCreated(layout, null);
        return layout;
    }

}
