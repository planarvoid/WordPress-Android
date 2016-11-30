package com.soundcloud.android.playlists;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static java.util.Collections.singletonList;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
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
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LegacyPlaylistDetailFragmentTest extends AndroidUnitTest {

    private final Observable<List<Urn>> playlistTrackUrns = Observable.just(singletonList(Urn.forTrack(1)));
    private LegacyPlaylistDetailFragment fragment;
    private PlaylistWithTracks playlistWithTracks;
    private TestEventBus eventBus = new TestEventBus();

    private TestSubscriber<PlaybackResult> playerExpandSubscriber = new TestSubscriber<>();
    private Provider expandPlayerSubscriberProvider = providerOf(playerExpandSubscriber);

    @Mock private PlaylistDetailsController.Provider controllerProvider;
    @Mock private PlaylistDetailsController controller;
    @Mock private PlaySessionController playSessionController;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private LegacyPlaylistEngagementsPresenter playlistEngagementsPresenter;
    @Mock private ListItemAdapter<TypedListItem> adapter;
    @Mock private PullToRefreshController ptrController;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private Navigator navigator;

    @Before
    public void setUp() {
        fragment = new LegacyPlaylistDetailFragment(
                controllerProvider,
                playSessionController,
                playbackInitiator,
                playlistOperations,
                eventBus,
                imageOperations,
                playlistEngagementsPresenter,
                ptrController,
                playQueueManager,
                new PlaylistHeaderViewFactory(InjectionSupport.providerOf(imageOperations)),
                expandPlayerSubscriberProvider,
                accountOperations,
                navigator
        );

        playlistWithTracks = createPlaylist();

        when(controllerProvider.create()).thenReturn(controller);
        when(controller.getAdapter()).thenReturn(adapter);
        when(adapter.getViewTypeCount()).thenReturn(1);
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks));
        when(playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn())).thenReturn(playlistTrackUrns);
        when(playbackInitiator.playTracks(any(Observable.class),
                                          any(Urn.class),
                                          anyInt(),
                                          any(PlaySessionSource.class))).thenReturn(Observable.<PlaybackResult>empty());
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(312L));
    }

    @Test
    public void playsPlaylistWhenClickingPlayableTrack() {
        final ListView list = (ListView) createFragmentView().findViewById(android.R.id.list);
        final TrackItem trackItem = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(trackItem);

        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(playlistTrackUrns, trackItem.getUrn(), 0, getPlaySessionSource()))
                .thenReturn(Observable.just(playbackResult));

        list.getOnItemClickListener().onItemClick(list, mock(View.class), /* offset for header */ 1, 123);

        playerExpandSubscriber.assertReceivedOnNext(singletonList(playbackResult));
    }

    @Test
    public void shouldNotShowPlayToggleButtonWithNoTracks() {
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(createPlaylistWithoutTracks()));
        View layout = createFragmentView();

        assertThat(getPlayButton(layout)).isNotVisible();
    }

    @Test
    public void playlistInfoForOwnPlaylistSetsTrackRemovalForPlaylistOnController() {
        final PlaylistWithTracks playlistWithoutTracks = createPlaylistWithoutTracks();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlistWithoutTracks.getCreatorUrn());
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithoutTracks));
        createFragmentView();

        verify(controller).showTrackRemovalOptions(any(PlaylistDetailsController.Listener.class));
    }

    @Test
    public void playlistContentChangeForcesReloadOfPlaylistInfo() {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        when(accountOperations.getLoggedInUserUrn()).thenReturn(playlistWithTracks.getCreatorUrn());
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks),
                                                                     Observable.just(updatedPlaylistWithTracks));
        createFragmentView();

        ArgumentCaptor<PlaylistDetailsController.Listener> captor = ArgumentCaptor.forClass(PlaylistDetailsController.Listener.class);
        verify(controller).showTrackRemovalOptions(captor.capture());

        captor.getValue().onPlaylistContentChanged();

        InOrder inOrder = Mockito.inOrder(playlistEngagementsPresenter);
        inOrder.verify(playlistEngagementsPresenter)
               .setPlaylistInfo(eq(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource())));
        inOrder.verify(playlistEngagementsPresenter)
               .setPlaylistInfo(eq(PlaylistHeaderItem.create(updatedPlaylistWithTracks, getPlaySessionSource())));
    }

    @Test
    public void playlistInfoForNonOwnedPlaylistDoesNotSetTracksAsRemovableOnController() {
        final PlaylistWithTracks playlistWithoutTracks = createPlaylistWithoutTracks();
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithoutTracks));
        createFragmentView();

        verify(controller, never()).showTrackRemovalOptions(any(PlaylistDetailsController.Listener.class));
    }

    @Test
    public void shouldShowPlayToggleButtonWithTracks() {
        View layout = createFragmentView();

        assertThat(getPlayButton(layout)).isVisible();
    }

    @Test
    public void shouldNotAutoPlayWithAutoPlaySetOnIntentIfPlaylistHasNoItems() {
        when(controller.getAdapter()).thenReturn(adapter);
        createFragmentView(true);
        verifyNoMoreInteractions(playSessionController);
    }

    @Test
    public void shouldAutoPlayIfAutoPlaySetOnIntentIfPlaylistIsNotEmpty() {
        TrackItem playlistTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(playlistTrack);

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(Screen.STREAM,
                                                                                  playlistWithTracks.getUrn(),
                                                                                  playlistWithTracks.getCreatorUrn(),
                                                                                  playlistWithTracks.getTrackCount());

        when(controller.hasTracks()).thenReturn(true);

        createFragmentView(true);

        verify(playbackInitiator).playTracks(playlistTrackUrns, playlistTrack.getUrn(), 0, playSessionSource);
    }

    @Test
    public void shouldHidePlayToggleButtonOnSecondPlaylistEmissionWithNoTracks() {
        final PlaylistWithTracks updatedPlaylistWithTracks = createPlaylistWithoutTracks();

        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.just(playlistWithTracks,
                                                                                     updatedPlaylistWithTracks));
        View layout = createFragmentView();

        assertThat(getPlayButton(layout)).isNotVisible();
    }

    @Test
    public void shouldPlayPlaylistOnPlayClick() {
        final TrackItem playlistTrack = ModelFixtures.create(TrackItem.class);
        when(adapter.getItem(0)).thenReturn(playlistTrack);
        View layout = createFragmentView();

        final PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(Screen.STREAM,
                                                                                  playlistWithTracks.getUrn(),
                                                                                  playlistWithTracks.getCreatorUrn(),
                                                                                  playlistWithTracks.getTrackCount());

        getPlayButton(layout).performClick();

        verify(playbackInitiator).playTracks(playlistTrackUrns, playlistTrack.getUrn(), 0, playSessionSource);
    }

    @Test
    public void engagementsControllerStartsListeningInOnStart() {
        createFragmentView();
        verify(playlistEngagementsPresenter).onStart(fragment);
    }

    @Test
    public void shouldOpenUserProfileWhenUsernameTextIsClicked() {
        when(playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())).thenReturn(true);
        View layout = createFragmentView();

        View usernameView = layout.findViewById(R.id.username);
        usernameView.performClick();

        verify(navigator).legacyOpenProfile(any(Context.class), eq(playlistWithTracks.getCreatorUrn()));
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
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(Observable.<PlaylistWithTracks>error(new Exception(
                "something bad happened")));
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
        verify(playlistEngagementsPresenter).setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks,
                                                                                       getPlaySessionSource()));
    }

    @Test
    public void setsTitleForActivityAsPlaylist() {
        createFragmentView();

        assertThat(fragment.getActivity().getTitle()).isEqualTo("Playlist");
    }

    @Test
    public void setsTitleForActivityAsAlbumTypeWithReleaseDateWhenPlaylistIsAnAlbumAndReleaseDateIsAvailable() {
        when(playlistOperations.playlist(any(Urn.class)))
                .thenReturn(Observable.just(createAlbumPlaylist("ep", "2010-10-10")));

        createFragmentView();

        assertThat(fragment.getActivity().getTitle()).isEqualTo("EP · 2010");
    }

    @Test
    public void setsPlayableOnEngagementsControllerTwiceWhenPlaylistEmittedTwice() {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.from(Arrays.asList(playlistWithTracks, updatedPlaylistWithTracks)));
        createFragmentView();

        InOrder inOrder = Mockito.inOrder(playlistEngagementsPresenter);
        inOrder.verify(playlistEngagementsPresenter)
               .setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
        inOrder.verify(playlistEngagementsPresenter)
               .setPlaylistInfo(PlaylistHeaderItem.create(updatedPlaylistWithTracks,
                                                          getPlaySessionSource(updatedPlaylistWithTracks)));
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterWhenPlaylistIsReturned() {
        assertThat(playlistWithTracks.getTracks().size()).isGreaterThan(0);
        createFragmentView();

        verify(controller).setContent(playlistWithTracks, null);
    }

    @Test
    public void clearsAndAddsAllItemsToAdapterForEachPlaylistWhenPlaylistIsEmittedMultipleTimes() {
        PlaylistWithTracks updatedPlaylistWithTracks = createPlaylist();
        assertThat(playlistWithTracks.getTracks().size()).isGreaterThan(0);
        assertThat(updatedPlaylistWithTracks.getTracks().size()).isGreaterThan(0);

        when(playlistOperations.playlist(any(Urn.class))).thenReturn(
                Observable.from(Arrays.asList(playlistWithTracks, updatedPlaylistWithTracks)));

        createFragmentView();

        InOrder inOrder = Mockito.inOrder(controller);
        inOrder.verify(controller).setContent(playlistWithTracks, null);
        inOrder.verify(controller).setContent(updatedPlaylistWithTracks, null);
    }

    @Test
    public void shouldSetEmptyStateMessage() {
        when(playlistOperations.playlist(any(Urn.class)))
                .thenReturn(Observable.just(createAlbumPlaylist("ep", "2010-10-10")));

        createFragmentView();

        verify(controller).setEmptyStateMessage("Empty EP", "This EP has no tracks yet.");
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
    public void shouldForwardOnViewCreatedEventToController() {
        View layout = createFragmentView();

        verify(controller).onViewCreated(same(layout), any(Bundle.class));
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

        fragment.onCreateView(fragment.getLayoutInflater(null), new RelativeLayout(fragment.getActivity()), null);

        verify(controllerProvider, times(2)).create();
    }

    @Test
    public void entityStateChangedForPlaylistUpdateChangesUrn() {
        createFragmentView();

        final ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);
        final EntityStateChangedEvent event = EntityStateChangedEvent.fromPlaylistPushedToServer(
                playlistWithTracks.getUrn(), newPlaylist.toPropertySet());

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);

        assertThat(fragment.getArguments()
                           .<Parcelable>getParcelable(LegacyPlaylistDetailFragment.EXTRA_URN)).isEqualTo(newPlaylist.getUrn());
    }

    private View createFragmentView() {
        return createFragmentView(true);
    }

    private View createFragmentView(boolean autoplay) {
        Bundle fragmentArguments = new Bundle();
        fragmentArguments.putParcelable(LegacyPlaylistDetailFragment.EXTRA_URN, playlistWithTracks.getUrn());
        fragmentArguments.putBoolean(LegacyPlaylistDetailFragment.EXTRA_AUTOPLAY, autoplay);
        Screen.STREAM.addToBundle(fragmentArguments);
        fragment.setArguments(fragmentArguments);
        SupportFragmentTestUtil.startVisibleFragment(fragment);
        return fragment.getView();
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

    private PlaylistWithTracks createAlbumPlaylist(String type, String releaseDate) {
        PropertySet propertySet = ModelFixtures.create(ApiPlaylist.class).toPropertySet();
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, type);
        propertySet.put(PlaylistProperty.RELEASE_DATE, releaseDate);

        return new PlaylistWithTracks(
                propertySet,
                Collections.<TrackItem>emptyList());
    }

    private ImageButton getPlayButton(View layout) {
        return (ImageButton) layout.findViewById(R.id.btn_play);
    }

    private PlaySessionSource getPlaySessionSource() {
        return getPlaySessionSource(playlistWithTracks);
    }

    private PlaySessionSource getPlaySessionSource(PlaylistWithTracks playlistWithTracks) {
        return PlaySessionSource.forPlaylist(Screen.STREAM,
                                             playlistWithTracks.getUrn(),
                                             playlistWithTracks.getCreatorUrn(),
                                             playlistWithTracks.getTrackCount());
    }

}
