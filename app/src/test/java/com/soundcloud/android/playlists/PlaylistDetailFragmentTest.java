package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
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
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistDetailFragmentTest {

    private PlaylistDetailFragment fragment;
    private FragmentActivity activity = new FragmentActivity();
    private PublicApiPlaylist playlist;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaylistDetailsController.Provider controllerProvider;
    @Mock private PlaylistDetailsController controller;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private LegacyPlaylistOperations legacyPlaylistOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistEngagementsController playlistEngagementsController;
    @Mock private ItemAdapter adapter;
    @Mock private PullToRefreshController ptrController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private Intent intent;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistDetailFragment(
                controllerProvider,
                playbackOperations,
                legacyPlaylistOperations,
                playlistOperations,
                eventBus,
                imageOperations,
                playlistEngagementsController,
                ptrController,
                playQueueManager,
                new PlaylistPresenter(imageOperations),
                TestSubscribers.expandPlayerSubscriber()
        );

        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        playlist = createPlaylist();

        when(controllerProvider.create()).thenReturn(controller);
        when(controller.getAdapter()).thenReturn(adapter);
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(Observable.just(playlist));
        when(playbackOperations.playTracks(any(Observable.class), any(Urn.class), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.<List<Urn>>empty());

        fragment.onAttach(activity);
        activity.setIntent(intent);
    }

    @Test
    public void shouldNotShowPlayToggleButtonWithNoTracks() throws Exception {
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldHidePlayToggleButtonWithNoTracks() throws Exception {
        final PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        playlist.tracks = Lists.newArrayList(track);
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void shouldNotAutoPlayWithAutoPlaySetOnIntentIfPlaylistHasNoItems() throws Exception {
        when(intent.getBooleanExtra(eq(PlaylistDetailActivity.EXTRA_AUTO_PLAY), anyBoolean())).thenReturn(true);
        when(controller.getAdapter()).thenReturn(adapter);
        createFragmentView();
        verifyNoMoreInteractions(playbackOperations);
    }

    @Test
    public void shouldAutoPlayIfAutoPlaySetOnIntentIfPlaylistIsNotEmpty() throws Exception {
        when(intent.getBooleanExtra(eq(PlaylistDetailActivity.EXTRA_AUTO_PLAY), anyBoolean())).thenReturn(true);

        final PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        when(adapter.getItem(0)).thenReturn(track.toPropertySet());
        Observable<List<Urn>> trackLoadDescriptor = Observable.empty();
        when(playlistOperations.trackUrnsForPlayback(playlist.getUrn())).thenReturn(trackLoadDescriptor);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        when(controller.hasTracks()).thenReturn(true);

        createFragmentView();

        verify(playbackOperations).playTracks(trackLoadDescriptor, track.getUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldHidePlayToggleButtonOnSecondPlaylistEmissionWithNoTracks() throws Exception {
        final PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        playlist.tracks = Lists.newArrayList(track);

        final PublicApiPlaylist playlist2 = createPlaylist(playlist.getId());

        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(Observable.from(Arrays.asList(playlist, playlist2)));
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPlayState() throws Exception {
        final PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        when(adapter.getItem(0)).thenReturn(track.toPropertySet());
        Observable<List<Urn>> trackLoadDescriptor = Observable.empty();
        when(playlistOperations.trackUrnsForPlayback(playlist.getUrn())).thenReturn(trackLoadDescriptor);
        View layout = createFragmentView();
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        playSessionSource.setPlaylist(playlist.getUrn(), playlist.getUserUrn());

        getToggleButton(layout).performClick();

        verify(playbackOperations).playTracks(trackLoadDescriptor, track.getUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPauseState() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getUrn())).thenReturn(true);
        View layout = createFragmentView();

        getToggleButton(layout).performClick();

        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void shouldUncheckPlayToggleOnTogglePlaystateWhenSkippingIsDisabled() throws Exception {
        when(playbackOperations.shouldDisableSkipping()).thenReturn(true);
        final PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        when(playlistOperations.trackUrnsForPlayback(playlist.getUrn())).thenReturn(Observable.<List<Urn>>empty());
        when(adapter.getItem(0)).thenReturn(track.toPropertySet());
        View layout = createFragmentView();
        ToggleButton toggleButton = getToggleButton(layout);

        toggleButton.performClick();

        expect(toggleButton.isChecked()).toBeFalse();
    }

    @Test
    public void shouldSetToggleToPlayStateWhenPlayingCurrentPlaylistOnResume() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getUrn())).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        expect(getToggleButton(layout).isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotSetToggleToPlayStateWhenPlayingDifferentPlaylistOnResume() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getUrn())).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        expect(getToggleButton(layout).isChecked()).toBeFalse();
    }

    @Test
     public void engagementsControllerStartsListeningInOnStart() throws Exception {
        createFragmentView();
        fragment.onStart();
        verify(playlistEngagementsController).startListeningForChanges();
    }

    @Test
    public void shouldOpenUserProfileWhenUsernameTextIsClicked() throws Exception {
        PublicApiUser user = new PublicApiUser();
        playlist.setUser(user);
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(Observable.just(playlist));
        when(playQueueManager.getPlaylistUrn()).thenReturn(playlist.getUrn());
        View layout = createFragmentView();

        View usernameView = layout.findViewById(R.id.username);
        usernameView.performClick();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(intent.getLongExtra(ProfileActivity.EXTRA_USER_ID, -2L)).toEqual(user.getId());
    }

    @Test
    public void engagementsControllerStopsListeningInOnStop() throws Exception {
        createFragmentView();
        fragment.onStop();
        verify(playlistEngagementsController).stopListeningForChanges();
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
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(Observable.<PublicApiPlaylist>error(new Exception("something bad happened")));
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
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("something bad happened")));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.ERROR);
    }

    @Test
    public void setsPlayableOnEngagementsControllerWhenPlaylistIsReturned() throws Exception {
        createFragmentView();
        verify(playlistEngagementsController).setPlayable(playlist);
    }

    @Test
    public void setsPlayableOnEngagementsControllerTwiceWhenPlaylistEmittedTwice() throws Exception {
        PublicApiPlaylist playlist2 = createPlaylist();
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(
                Observable.from(Arrays.asList(playlist, playlist2)));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(playlistEngagementsController);
        inOrder.verify(playlistEngagementsController).setPlayable(playlist);
        inOrder.verify(playlistEngagementsController).setPlayable(playlist2);
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
        PublicApiPlaylist playlist2 = createPlaylist(playlist.getId());
        playlist2.tracks = Lists.newArrayList(track1, track2);

        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(
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
        PublicApiPlaylist playlist2 = createPlaylist(playlist.getId());
        playlist2.tracks = Lists.newArrayList(track2);
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(Observable.just(playlist));
        when(legacyPlaylistOperations.refreshPlaylist(any(Urn.class))).thenReturn(Observable.just(playlist2));

        createFragmentView();
        fragment.onRefresh();

        InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).addItem(track1.toPropertySet());
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).addItem(track2.toPropertySet());
    }

    @Test
    public void showsToastErrorWhenContentAlreadyShownAndRefreshFails() {
        when(legacyPlaylistOperations.refreshPlaylist(any(Urn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefresh();

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.connection_list_error);
    }

    @Test
    public void doesNotShowInlineErrorWhenContentWhenAlreadyShownAndRefreshFails() throws CreateModelException {
        final PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        playlist.tracks = Lists.newArrayList(track);
        when(legacyPlaylistOperations.loadPlaylist(any(Urn.class))).thenReturn(Observable.just(playlist));
        when(legacyPlaylistOperations.refreshPlaylist(any(Urn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefresh();

        verify(controller, times(1)).setEmptyViewStatus(anyInt());
    }

    @Test
    public void hidesRefreshStateWhenRefreshFails() {
        when(legacyPlaylistOperations.refreshPlaylist(any(Urn.class))).thenReturn(
                Observable.<PublicApiPlaylist>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefresh();

        verify(ptrController, times(2)).stopRefreshing();
    }

    @Test
    public void shouldSetPlayingStateWhenPlaybackStateChanges() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlist.getUrn())).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        Robolectric.getShadowApplication().sendBroadcast(new Intent(PlaybackService.Broadcasts.PLAYSTATE_CHANGED));

        expect(getToggleButton(layout).isChecked()).toBeTrue();
    }

    @Test
    public void shouldSetPlayingStateWhenPlaybackMetaChanges() throws Exception {
        when(playQueueManager.getPlaylistUrn()).thenReturn(Urn.forPlaylist(123));
        View layout = createFragmentView();
        fragment.onStart();

        Robolectric.getShadowApplication().sendBroadcast(new Intent(PlaybackService.Broadcasts.META_CHANGED));

        expect(getToggleButton(layout).isChecked()).toBeFalse();
    }

    @Test
    public void shouldForwardOnViewCreatedEventToController() {
        Bundle savedInstanceState = new Bundle();
        View layout = createFragmentView(savedInstanceState);

        verify(controller).onViewCreated(layout, savedInstanceState);
    }

    @Test
    public void shouldForwardOnDestroyViewEventToController() {
        createFragmentView();
        fragment.onDestroyView();

        verify(controller).onDestroyView();
    }

    @Test
    public void onCreateViewRecreatesPlaylistDetailsController() {
        createFragmentView();

        fragment.onCreateView(activity.getLayoutInflater(), new RelativeLayout(activity), null);

        verify(controllerProvider, times(2)).create();
    }

    private PublicApiTrack createTrackWithTitle(String title) throws com.tobedevoured.modelcitizen.CreateModelException {
        final PublicApiTrack model = ModelFixtures.create(PublicApiTrack.class);
        model.setTitle(title);
        return model;
    }

    private View createFragmentView() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PlaylistDetailFragment.EXTRA_URN, playlist.getUrn());
        Screen.SIDE_MENU_STREAM.addToBundle(bundle);
        fragment.setArguments(bundle);
        return createFragmentView(new Bundle());
    }

    private View createFragmentView(Bundle savedInstanceState) {
        Bundle fragmentArguments = new Bundle();
        fragmentArguments.putParcelable(PlaylistDetailFragment.EXTRA_URN, playlist.getUrn());
        Screen.SIDE_MENU_STREAM.addToBundle(fragmentArguments);
        fragment.setArguments(fragmentArguments);
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new RelativeLayout(activity), null);
        fragment.onViewCreated(layout, savedInstanceState);
        return layout;
    }

    private PublicApiPlaylist createPlaylist(long id) throws CreateModelException {
        final PublicApiPlaylist apiPlaylist = createPlaylist();
        apiPlaylist.setId(id);
        return apiPlaylist;
    }

    private PublicApiPlaylist createPlaylist() throws CreateModelException {
        return ModelFixtures.create(PublicApiPlaylist.class);
    }

    private ToggleButton getToggleButton(View layout) {
        return (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
    }

}
