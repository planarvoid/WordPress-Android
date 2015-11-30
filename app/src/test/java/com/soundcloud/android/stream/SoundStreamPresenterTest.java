package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.StreamDesignExperiment;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SoundStreamPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private SoundStreamPresenter presenter;

    @Mock private SoundStreamOperations streamOperations;
    @Mock private SoundStreamAdapter adapter;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DateProvider dateProvider;
    @Mock private Observer<Iterable<StreamItem>> itemObserver;
    @Mock private MixedItemClickListener.Factory itemClickListenerFactory;
    @Mock private MixedItemClickListener itemClickListener;
    @Mock private Navigator navigator;
    @Mock private FacebookInvitesDialogPresenter facebookInvitesDialogPresenter;
    @Mock private StationsOperations stationsOperations;
    @Mock private StreamDesignExperiment streamExperiment;
    @Mock private View view;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(itemClickListenerFactory.create(Screen.STREAM, null)).thenReturn(itemClickListener);
        presenter = new SoundStreamPresenter(
                streamOperations,
                adapter,
                stationsOperations,
                imagePauseOnScrollListener,
                swipeRefreshAttacher,
                eventBus,
                itemClickListenerFactory,
                facebookInvitesDialogPresenter, streamExperiment);
        when(streamOperations.initialStreamItems()).thenReturn(Observable.<List<StreamItem>>empty());
        when(streamOperations.pagingFunction()).thenReturn(TestPager.<List<StreamItem>>singlePageFunction());
        when(dateProvider.getCurrentTime()).thenReturn(100L);
    }

    @Test
    public void canLoadStreamItems() {
        List<StreamItem> items = Arrays.<StreamItem>asList(
                PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack()),
                TrackItem.from(TestPropertySets.expectedTrackForListItem(Urn.forTrack(123L))),
                PlaylistItem.from(TestPropertySets.expectedLikedPlaylistForPlaylistsScreen())
        );
        when(streamOperations.initialStreamItems()).thenReturn(Observable.just(items));

        CollectionBinding<StreamItem> binding = presenter.onBuildBinding(null);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(items);
    }

    @Test
    public void canRefreshStreamItems() {
        final StreamItem streamItem = TrackItem.from(TestPropertySets.expectedTrackForListItem(Urn.forTrack(123L)));
        when(streamOperations.updatedStreamItems()).thenReturn(Observable.just(
                Collections.singletonList(streamItem)
        ));

        CollectionBinding<StreamItem> binding = presenter.onRefreshBinding();
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(Collections.singletonList(streamItem));
    }

    @Test
    public void forwardsTrackClicksToClickListener() {
        final TrackItem clickedTrack = ModelFixtures.create(TrackItem.class);
        final Observable<List<PropertySet>> streamTracks = Observable.just(
                Arrays.asList(clickedTrack.getEntityUrn().toPropertySet(), Urn.forTrack(634L).toPropertySet()));

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        verify(itemClickListener).onPostClick(streamTracks, view, 0, clickedTrack);
    }

    @Test
    public void tracksPromotedTrackItemClick() {
        final PromotedTrackItem clickedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        final Observable<List<PropertySet>> streamTracks = Observable.just(
                Arrays.asList(clickedTrack.getEntityUrn().toPropertySet(), Urn.forTrack(634L).toPropertySet()));

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
    }

    @Test
    public void tracksPromotedPlaylistItemClick() {
        final PromotedPlaylistItem clickedPlaylist = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        final Observable<List<PropertySet>> streamTracks = Observable.just(
                Arrays.asList(clickedPlaylist.getEntityUrn().toPropertySet(), Urn.forTrack(634L).toPropertySet()));

        when(adapter.getItem(0)).thenReturn(clickedPlaylist);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(itemClickListener).onPostClick(streamTracks, view, 0, clickedPlaylist);
    }

    @Test
    public void forwardsPlaylistClicksToClickListener() {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        final Observable<List<PropertySet>> streamTracks = Observable.just(
                Arrays.asList(playlistItem.getEntityUrn().toPropertySet(),
                        Urn.forTrack(634L).toPropertySet()));

        when(adapter.getItem(0)).thenReturn(playlistItem);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        verify(itemClickListener).onPostClick(streamTracks, view, 0, playlistItem);
    }

    @Test
    public void unsubscribesFromEventBusOnDestroyView() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onDestroyView(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void trackChangedEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(playingTrack), Urn.NOT_SET, 0));

        adapter.updateNowPlaying(playingTrack);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(playingTrack), Urn.NOT_SET, 0));

        adapter.updateNowPlaying(playingTrack);
    }

    @Test
    public void shouldPublishTrackingEventOnFacebookInvitesButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(Collections.<String>emptyList());
        presenter.onCreate(fragmentRule.getFragment(), null);
        when(adapter.getItem(0)).thenReturn(item);

        presenter.onFacebookInvitesInviteButtonClicked(0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(StreamNotificationEvent.class);
    }

    @Test
    public void shouldOpenFacebookInvitesDialogOnFacebookInvitesButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(Collections.<String>emptyList());
        when(adapter.getItem(0)).thenReturn(item);
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onFacebookInvitesInviteButtonClicked(0);

        verify(facebookInvitesDialogPresenter).show(fragmentRule.getActivity());
    }

    @Test
    public void shouldPublishTrackingEventOnFacebookCloseButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(Collections.<String>emptyList());
        when(adapter.getItem(0)).thenReturn(item);

        presenter.onFacebookInvitesCloseButtonClicked(0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(StreamNotificationEvent.class);
    }

    @Test
    public void shouldNotOpenFacebookInvitesDialogOnFacebookInvitesCloseButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(Collections.<String>emptyList());
        when(adapter.getItem(0)).thenReturn(item);
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onFacebookInvitesCloseButtonClicked(0);

        verify(facebookInvitesDialogPresenter, never()).show(fragmentRule.getActivity());
    }

    @Test
    public void shouldNotDoAnythingWhenClickingOnFacebookInvitesNotification() {
        final FacebookInvitesItem item = new FacebookInvitesItem(Collections.<String>emptyList());
        when(adapter.getItem(0)).thenReturn(item);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
        verify(facebookInvitesDialogPresenter, never()).show(fragmentRule.getActivity());
    }

    @Test
    public void onStationOnboardingItemClosedDiableOnboarding() {
        presenter.onStationOnboardingItemClosed(0);

        verify(stationsOperations).disableOnboarding();
    }

}
