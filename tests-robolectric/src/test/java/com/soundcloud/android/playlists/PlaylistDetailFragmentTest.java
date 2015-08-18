package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
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
import rx.observers.TestSubscriber;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistDetailFragmentTest {

    private PlaylistDetailFragment fragment;
    private FragmentActivity activity = new FragmentActivity();
    private PlaylistWithTracks playlistWithTracks;
    private TestEventBus eventBus = new TestEventBus();

    private TestSubscriber<PlaybackResult> playerExpandSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(playerExpandSubscriber);

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
    @Mock private FeatureOperations featureOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private Navigator navigator;

    @Before
    public void setUp() {
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
                expandPlayerSubscriberProvider,
                featureFlags,
                featureOperations,
                accountOperations,
                navigator
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
    public void showsUpsellWhenClickingOnMidTierTrackAndUserCanUpgrade() {
        final ListView list = (ListView) createFragmentView().findViewById(android.R.id.list);
        when(adapter.getItem(0)).thenReturn(new TrackItem(TestPropertySets.midTierTrack()));
        when(featureOperations.upsellMidTier()).thenReturn(true);

        list.getOnItemClickListener().onItemClick(list, mock(View.class), /* offset for header */ 1, 123);

        verify(navigator).openUpgrade(activity);
    }

    @Test
    public void doesNotShowUpsellWhenClickingOnMidTierTrackAndUserCannotUpgrade() {
        final ListView list = (ListView) createFragmentView().findViewById(android.R.id.list);
        when(adapter.getItem(0)).thenReturn(new TrackItem(TestPropertySets.midTierTrack()));

        list.getOnItemClickListener().onItemClick(list, mock(View.class), /* offset for header */ 1, 123);

        verify(navigator, never()).openUpgrade(any(Context.class));
    }

    @Test
    public void playsPlaylistWhenClickingPlayableTrack() {
        final ListView list = (ListView) createFragmentView().findViewById(android.R.id.list);
        final TrackItem trackItem = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(trackItem);

        final PlaybackResult playbackResult = PlaybackResult.success();
        when(offlinePlaybackOperations.playPlaylist(playlistWithTracks.getUrn(), trackItem.getEntityUrn(), 0, getPlaySessionSource()))
                .thenReturn(Observable.just(playbackResult));

        list.getOnItemClickListener().onItemClick(list, mock(View.class), /* offset for header */ 1, 123);

        playerExpandSubscriber.assertReceivedOnNext(Arrays.asList(playbackResult));
    }

    @Test
    public void shouldNotShowPlayToggleButtonWithNoTracks() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(createPlaylistWithoutTracks()));
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void playlistInfoForOwnPlaylistSetsTrackRemovalForPlaylistOnController() {
        final PlaylistWithTracks playlistWithoutTracks = createPlaylistWithoutTracks();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlistWithoutTracks.getCreatorUrn());
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithoutTracks));
        createFragmentView();

        verify(controller).showTrackRemovalOptions(eq(playlistWithoutTracks.getUrn()), any(PlaylistDetailsController.Listener.class));
    }

    @Test
    public void playlistContentChangeForcesReloadOfPlaylistInfo() {
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
    public void playlistInfoForNonOwnedPlaylistDoesNotSetTracksAsRemovableOnController() {
        final PlaylistWithTracks playlistWithoutTracks = createPlaylistWithoutTracks();
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithoutTracks));
        createFragmentView();

        verify(controller, never()).showTrackRemovalOptions(any(Urn.class), any(PlaylistDetailsController.Listener.class));
    }

    @Test
    public void shouldShowPlayToggleButtonWithTracks() {
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void shouldNotAutoPlayWithAutoPlaySetOnIntentIfPlaylistHasNoItems() {
        when(intent.getBooleanExtra(eq(PlaylistDetailActivity.EXTRA_AUTO_PLAY), anyBoolean())).thenReturn(true);
        when(controller.getAdapter()).thenReturn(adapter);
        createFragmentView();
        verifyNoMoreInteractions(playbackOperations);
    }

    @Test
    public void shouldAutoPlayIfAutoPlaySetOnIntentIfPlaylistIsNotEmpty() {
        when(intent.getBooleanExtra(eq(PlaylistDetailActivity.EXTRA_AUTO_PLAY), anyBoolean())).thenReturn(true);

        TrackItem playlistTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(playlistTrack);

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(Screen.SIDE_MENU_STREAM, playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn());

        when(controller.hasTracks()).thenReturn(true);

        createFragmentView();

        verify(offlinePlaybackOperations).playPlaylist(playlistWithTracks.getUrn(), playlistTrack.getEntityUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldHidePlayToggleButtonOnSecondPlaylistEmissionWithNoTracks() {
        final PlaylistWithTracks updatedPlaylistWithTracks = createPlaylistWithoutTracks();

        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks, updatedPlaylistWithTracks));
        View layout = createFragmentView();

        expect(getToggleButton(layout).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPlayState() {
        final TrackItem playlistTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(playlistTrack);
        View layout = createFragmentView();

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(Screen.SIDE_MENU_STREAM, playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn());

        getToggleButton(layout).performClick();

        verify(offlinePlaybackOperations).playPlaylist(playlistWithTracks.getUrn(), playlistTrack.getEntityUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldPlayPlaylistOnToggleToPauseState() {
        when(playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())).thenReturn(true);
        View layout = createFragmentView();

        getToggleButton(layout).performClick();

        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void shouldUncheckPlayToggleOnTogglePlayStateWhenSkippingIsDisabled() {
        when(playbackOperations.shouldDisableSkipping()).thenReturn(true);
        when(adapter.getItem(0)).thenReturn(ModelFixtures.create(TrackItem.class));
        View layout = createFragmentView();
        ToggleButton toggleButton = getToggleButton(layout);

        toggleButton.performClick();

        expect(toggleButton.isChecked()).toBeFalse();
    }

    @Test
    public void shouldSetToggleToPlayStateWhenPlayingCurrentPlaylistOnResume() {
        when(playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        expect(getToggleButton(layout).isChecked()).toBeTrue();
    }

    @Test
    public void shouldNotSetToggleToPlayStateWhenPlayingDifferentPlaylistOnResume() {
        when(playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())).thenReturn(false);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        expect(getToggleButton(layout).isChecked()).toBeFalse();
    }

    @Test
     public void engagementsControllerStartsListeningInOnStart() {
        createFragmentView();
        fragment.onStart();
        verify(playlistEngagementsPresenter).onStart(fragment);
    }

    @Test
    public void shouldOpenUserProfileWhenUsernameTextIsClicked() {
        when(playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())).thenReturn(true);
        View layout = createFragmentView();

        View usernameView = layout.findViewById(R.id.username);
        usernameView.performClick();

        verify(navigator).openProfile(any(Context.class), eq(playlistWithTracks.getCreatorUrn()));
    }

    @Test
    public void engagementsControllerStopsListeningInOnStop() {
        createFragmentView();
        fragment.onStop();
        verify(playlistEngagementsPresenter).onStop(fragment);
    }

    @Test
    public void callsShowContentWhenPlaylistIsReturned() {
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setListShown(false);
        inOrder.verify(controller).setListShown(true);
    }

    @Test
    public void callsShowContentWhenErrorIsReturned() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>error(new Exception("something bad happened")));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setListShown(false);
        inOrder.verify(controller).setListShown(true);
    }

    @Test
    public void setsEmptyViewToOkWhenPlaylistIsReturned() {
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.OK);
    }

    @Test
    public void setsEmptyViewToErrorWhenErrorIsReturned() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(new Exception("something bad happened")));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.ERROR);
    }

    @Test
    public void setsEmptyViewToConnectionErrorWhenApiRequestNetworkError() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.<PlaylistWithTracks>error(TestApiResponses.networkError().getFailure()));
        createFragmentView();
        verify(controller).setEmptyViewStatus(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void setsPlayableOnEngagementsControllerWhenPlaylistIsReturned() {
        createFragmentView();
        verify(playlistEngagementsPresenter).setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
    }

    @Test
    public void setsPlayableOnEngagementsControllerTwiceWhenPlaylistEmittedTwice() {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.from(Arrays.asList(playlistWithTracks, updatedPlaylistWithTracks)));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(playlistEngagementsPresenter);
        inOrder.verify(playlistEngagementsPresenter).setPlaylistInfo(playlistWithTracks, getPlaySessionSource());
        inOrder.verify(playlistEngagementsPresenter).setPlaylistInfo(updatedPlaylistWithTracks, getPlaySessionSource(updatedPlaylistWithTracks));
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() {
        expect(playlistWithTracks.getTracks().size()).toBeGreaterThan(0);
        createFragmentView();

        verify(controller).setContent(playlistWithTracks);
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterForEachPlaylistWhenPlaylistIsEmittedMultipleTimes() {
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

        verify(controller, times(1)).setEmptyViewStatus(any(EmptyView.Status.class));
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
    public void shouldSetPlayingStateWhenPlaybackStateChanges() {
        when(playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())).thenReturn(true);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.NOT_SET));

        View layout = createFragmentView();
        fragment.onResume();

        Robolectric.getShadowApplication().sendBroadcast(new Intent(PlaybackService.Broadcasts.PLAYSTATE_CHANGED));

        expect(getToggleButton(layout).isChecked()).toBeTrue();
    }

    @Test
    public void shouldSetPlayingStateWhenPlaybackMetaChanges() {
        when(playQueueManager.isCurrentCollection(Urn.forPlaylist(123))).thenReturn(true);
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
        return PlaySessionSource.forPlaylist(Screen.SIDE_MENU_STREAM, playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn());
    }

}
