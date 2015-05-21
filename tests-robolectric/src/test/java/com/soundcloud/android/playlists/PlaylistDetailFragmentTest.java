package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.ListItemAdapter;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistDetailFragmentTest {

    private PlaylistDetailFragment fragment;
    private FragmentActivity activity = new FragmentActivity();
    private PlaylistWithTracks playlistWithTracks;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaylistDetailsController.Provider controllerProvider;
    @Mock private PlaylistDetailsController controller;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private OfflinePlaybackOperations offlinePlaybackOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PlaylistEngagementsPresenter playlistEngagementsPresenter;
    @Mock private ListItemAdapter<TrackItem> adapter;
    @Mock private PullToRefreshController ptrController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private Intent intent;
    @Mock private FeatureFlags featureFlags;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        fragment = new PlaylistDetailFragment(
                controllerProvider,
                playbackOperations,
                offlinePlaybackOperations,
                playlistOperations,
                eventBus,
                imageOperations,
                playlistEngagementsPresenter,
                ptrController,
                playQueueManager,
                new PlaylistPresenter(imageOperations),
                TestSubscribers.expandPlayerSubscriber(),
                featureFlags,
                accountOperations
        );

        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);

        playlistWithTracks = createPlaylist();

        when(controllerProvider.create()).thenReturn(controller);
        when(controller.getAdapter()).thenReturn(adapter);
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks));
        when(offlinePlaybackOperations.playPlaylist(any(Urn.class), any(Urn.class), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.<PlaybackResult>empty());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(312L));

        fragment.onAttach(activity);
        activity.setIntent(intent);
    }

    @Test
    public void shouldNotShowPlayToggleButtonWithNoTracks() throws Exception {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(createPlaylistWithoutTracks()));
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void playlistInfoForOwnPlaylistSetsTrackRemovalForPlaylistOnController() throws Exception {
        final PlaylistWithTracks playlistWithoutTracks = createPlaylistWithoutTracks();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlistWithoutTracks.getCreatorUrn());
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithoutTracks));
        createFragmentView();

        verify(controller).showTrackRemovalOptions(eq(playlistWithoutTracks.getUrn()), any(PlaylistDetailsController.Listener.class));
    }

    @Test
    public void playlistContentChangeForcesReloadOfPlaylistInfo() throws Exception {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlistWithTracks.getCreatorUrn());
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks), Observable.just(updatedPlaylistWithTracks));
        createFragmentView();

        ArgumentCaptor<PlaylistDetailsController.Listener> captor = ArgumentCaptor.forClass(PlaylistDetailsController.Listener.class);
        verify(controller).showTrackRemovalOptions(same(playlistWithTracks.getUrn()), captor.capture());

        captor.getValue().onPlaylistContentChanged();

        InOrder inOrder = Mockito.inOrder(playlistEngagementsPresenter);
        inOrder.verify(playlistEngagementsPresenter).setPlaylistInfo(eq(playlistWithTracks), any(PlaySessionSource.class));
        inOrder.verify(playlistEngagementsPresenter).setPlaylistInfo(eq(updatedPlaylistWithTracks), any(PlaySessionSource.class));
    }

    @Test
    public void playlistInfoForNonOwnedPlaylistDoesNotSetTracksAsRemovableOnController() throws Exception {
        final PlaylistWithTracks playlistWithoutTracks = createPlaylistWithoutTracks();
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithoutTracks));
        createFragmentView();

        verify(controller, never()).showTrackRemovalOptions(any(Urn.class), any(PlaylistDetailsController.Listener.class));
    }

    @Test
    public void shouldShowPlayToggleButtonWithTracks() throws Exception {
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

        TrackItem playlistTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(playlistTrack);

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        playSessionSource.setPlaylist(playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn());

        when(controller.hasTracks()).thenReturn(true);

        createFragmentView();

        verify(offlinePlaybackOperations).playPlaylist(playlistWithTracks.getUrn(), playlistTrack.getEntityUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldHidePlayToggleButtonOnSecondPlaylistEmissionWithNoTracks() throws Exception {
        final PlaylistWithTracks updatedPlaylistWithTracks = createPlaylistWithoutTracks();

        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks, updatedPlaylistWithTracks));
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPlayState() throws Exception {
        final TrackItem playlistTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(playlistTrack);
        View layout = createFragmentView();

        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        playSessionSource.setPlaylist(playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn());

        getToggleButton(layout).performClick();

        verify(offlinePlaybackOperations).playPlaylist(playlistWithTracks.getUrn(), playlistTrack.getEntityUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPauseState() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlistWithTracks.getUrn())).thenReturn(true);
        View layout = createFragmentView();

        getToggleButton(layout).performClick();

        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void shouldUncheckPlayToggleOnTogglePlayStateWhenSkippingIsDisabled() throws Exception {
        when(playbackOperations.shouldDisableSkipping()).thenReturn(true);
        when(adapter.getItem(0)).thenReturn(ModelFixtures.create(TrackItem.class));
        View layout = createFragmentView();
        ToggleButton toggleButton = getToggleButton(layout);

        toggleButton.performClick();

        expect(toggleButton.isChecked()).toBeFalse();
    }

    @Test
    public void shouldSetToggleToPlayStateWhenPlayingCurrentPlaylistOnResume() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlistWithTracks.getUrn())).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        expect(getToggleButton(layout).isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotSetToggleToPlayStateWhenPlayingDifferentPlaylistOnResume() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlistWithTracks.getUrn())).thenReturn(false);
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
        verify(playlistEngagementsPresenter).onStart(fragment);
    }

    @Test
    public void shouldOpenUserProfileWhenUsernameTextIsClicked() throws Exception {
        when(playQueueManager.getPlaylistUrn()).thenReturn(playlistWithTracks.getUrn());
        View layout = createFragmentView();

        View usernameView = layout.findViewById(R.id.username);
        usernameView.performClick();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getComponent().getClassName()).toEqual(ProfileActivity.class.getCanonicalName());
        expect(intent.getParcelableExtra(ProfileActivity.EXTRA_USER_URN)).toEqual(playlistWithTracks.getCreatorUrn());
    }

    @Test
    public void engagementsControllerStopsListeningInOnStop() throws Exception {
        createFragmentView();
        fragment.onStop();
        verify(playlistEngagementsPresenter).onStop(fragment);
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
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>error(new Exception("something bad happened")));
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
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(new Exception("something bad happened")));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void setsEmptyViewToConnectionErrorWhenApiRequestNetworkError() throws Exception {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(TestApiResponses.networkError().getFailure()));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void setsPlayableOnEngagementsControllerWhenPlaylistIsReturned() throws Exception {
        createFragmentView();
        verify(playlistEngagementsPresenter).setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
    }

    @Test
    public void setsPlayableOnEngagementsControllerTwiceWhenPlaylistEmittedTwice() throws Exception {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.from(Arrays.asList(playlistWithTracks, updatedPlaylistWithTracks)));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(playlistEngagementsPresenter);
        inOrder.verify(playlistEngagementsPresenter).setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        inOrder.verify(playlistEngagementsPresenter).setPlaylistInfo(updatedPlaylistWithTracks, getPlaySessionSource(updatedPlaylistWithTracks));
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() throws Exception {
        expect(playlistWithTracks.getTracks().size()).toBeGreaterThan(0);
        createFragmentView();

        verify(controller).setContent(playlistWithTracks);
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterForEachPlaylistWhenPlaylistIsEmittedMultipleTimes() throws Exception {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        expect(playlistWithTracks.getTracks().size()).toBeGreaterThan(0);
        expect(updatedPlaylistWithTracks.getTracks().size()).toBeGreaterThan(0);

        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.from(Arrays.asList(playlistWithTracks, updatedPlaylistWithTracks)));

        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setContent(playlistWithTracks);
        inOrder.verify(controller).setContent(updatedPlaylistWithTracks);
    }

    @Test
    public void showsToastErrorWhenContentAlreadyShownAndRefreshFails() {
        when(playlistOperations.updatedPlaylistInfo(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefresh();

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.connection_list_error);
    }

    @Test
    public void doesNotShowInlineErrorWhenContentWhenAlreadyShownAndRefreshFails() throws CreateModelException {

        when(playlistOperations.updatedPlaylistInfo(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefresh();

        verify(controller, times(1)).setEmptyViewStatus(anyInt());
    }

    @Test
    public void hidesRefreshStateWhenRefreshFails() {
        when(playlistOperations.updatedPlaylistInfo(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(new Exception("cannot refresh")));
        when(controller.hasContent()).thenReturn(true);

        createFragmentView();
        fragment.onRefresh();

        verify(ptrController, times(2)).stopRefreshing();
    }

    @Test
    public void shouldSetPlayingStateWhenPlaybackStateChanges() throws Exception {
        when(playQueueManager.isCurrentPlaylist(playlistWithTracks.getUrn())).thenReturn(true);
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

    private View createFragmentView() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(PlaylistDetailFragment.EXTRA_URN, playlistWithTracks.getUrn());
        Screen.SIDE_MENU_STREAM.addToBundle(bundle);
        fragment.setArguments(bundle);
        return createFragmentView(new Bundle());
    }

    private View createFragmentView(Bundle savedInstanceState) {
        Bundle fragmentArguments = new Bundle();
        fragmentArguments.putParcelable(PlaylistDetailFragment.EXTRA_URN, playlistWithTracks.getUrn());
        Screen.SIDE_MENU_STREAM.addToBundle(fragmentArguments);
        fragment.setArguments(fragmentArguments);
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new RelativeLayout(activity), null);
        fragment.onViewCreated(layout, savedInstanceState);
        return layout;
    }

    private PlaylistWithTracks createPlaylist() {
        return new PlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class).toPropertySet(),
                ModelFixtures.trackItems(10));
    }

    private PlaylistWithTracks createPlaylistWithoutTracks() {
        return new PlaylistWithTracks(
                ModelFixtures.create(ApiPlaylist.class).toPropertySet(),
                Collections.<TrackItem>emptyList());
    }

    private ToggleButton getToggleButton(View layout) {
        return (ToggleButton) layout.findViewById(R.id.toggle_play_pause);
    }

    private PlaySessionSource getPlaySessionSource() {
        return getPlaySessionSource(playlistWithTracks);
    }

    private PlaySessionSource getPlaySessionSource(PlaylistWithTracks playlistWithTracks) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        playSessionSource.setPlaylist(playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn());;
        return playSessionSource;
    }

}
