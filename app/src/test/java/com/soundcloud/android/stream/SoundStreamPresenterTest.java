package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesDialogPresenter;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
import com.soundcloud.android.view.NewItemsIndicator;
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
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
    @Mock private View view;
    @Mock private FeatureFlags featureFlags;
    @Mock private NewItemsIndicator newItemsIndicator;

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
                facebookInvitesDialogPresenter,
                navigator,
                featureFlags,
                newItemsIndicator);

        when(streamOperations.initialStreamItems()).thenReturn(Observable.<List<StreamItem>>empty());
        when(streamOperations.updatedStreamItemsForStart()).thenReturn(Observable.<List<StreamItem>>empty());
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
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        verify(itemClickListener).onPostClick(streamTracks, view, 0, clickedTrack);
    }

    @Test
    public void tracksPromotedTrackItemClick() {
        final PromotedTrackItem clickedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        final Observable<List<PropertySet>> streamTracks = Observable.just(
                Arrays.asList(clickedTrack.getEntityUrn().toPropertySet(), Urn.forTrack(634L).toPropertySet()));

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
    }

    @Test
    public void tracksPromotedPlaylistItemClick() {
        final PromotedPlaylistItem clickedPlaylist = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        final Observable<List<PropertySet>> streamTracks = Observable.just(
                Arrays.asList(clickedPlaylist.getEntityUrn().toPropertySet(), Urn.forTrack(634L).toPropertySet()));

        when(adapter.getItem(0)).thenReturn(clickedPlaylist);
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

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
        when(streamOperations.urnsForPlayback()).thenReturn(streamTracks);

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

        verify(adapter).updateNowPlaying(playingTrack);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromNewQueue(TestPlayQueueItem.createTrack(playingTrack), Urn.NOT_SET, 0));

        verify(adapter).updateNowPlaying(playingTrack);
    }

    @Test
    public void shouldPublishTrackingEventOnFacebookInvitesButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(FacebookInvitesItem.LISTENER_URN);
        presenter.onCreate(fragmentRule.getFragment(), null);
        when(adapter.getItem(0)).thenReturn(item);

        presenter.onListenerInvitesClicked(0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(FacebookInvitesEvent.class);
    }

    @Test
    public void shouldOpenFacebookInvitesDialogOnFacebookInvitesButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(FacebookInvitesItem.LISTENER_URN);
        when(adapter.getItem(0)).thenReturn(item);
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onListenerInvitesClicked(0);

        verify(facebookInvitesDialogPresenter).showForListeners(fragmentRule.getActivity());
    }

    @Test
    public void shouldPublishTrackingEventOnFacebookCloseButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(FacebookInvitesItem.LISTENER_URN);
        when(adapter.getItem(0)).thenReturn(item);

        presenter.onListenerInvitesDismiss(0);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(FacebookInvitesEvent.class);
    }

    @Test
    public void shouldNotOpenFacebookInvitesDialogOnFacebookInvitesCloseButtonClick() {
        final FacebookInvitesItem item = new FacebookInvitesItem(FacebookInvitesItem.LISTENER_URN);
        when(adapter.getItem(0)).thenReturn(item);
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onListenerInvitesDismiss(0);

        verify(facebookInvitesDialogPresenter, never()).showForListeners(fragmentRule.getActivity());
    }

    @Test
    public void shouldNotDoAnythingWhenClickingOnFacebookInvitesNotification() {
        final FacebookInvitesItem item = new FacebookInvitesItem(FacebookInvitesItem.LISTENER_URN);
        when(adapter.getItem(0)).thenReturn(item);

        presenter.onItemClicked(view, 0);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
        verify(facebookInvitesDialogPresenter, never()).showForListeners(fragmentRule.getActivity());
    }

    @Test
    public void onStationOnboardingItemClosedDisableOnboarding() {
        presenter.onStationOnboardingItemClosed(0);

        verify(stationsOperations).disableOnboarding();
    }

    @Test
    public void onUpsellItemDismissedUpsellsGetDisabled() {
        presenter.onUpsellItemDismissed(0);

        verify(streamOperations).disableUpsell();
    }

    @Test
    public void onUpsellItemClickedOpensUpgradeScreen() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onUpsellItemClicked();

        verify(navigator).openUpgrade(fragmentRule.getActivity());
    }

    @Test
    public void onUpsellItemClickedSendsUpsellTrackingEvent() {
        UpgradeTrackingEvent expectedEvent = UpgradeTrackingEvent.forStreamClick();

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onUpsellItemClicked();

        UpgradeTrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeTrackingEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(trackingEvent.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void onRefreshableOverlayClickedUpdatesStreamAgain() {
        when(streamOperations.initialStreamItems())
                .thenReturn(Observable.just(Collections.<StreamItem>emptyList()));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onNewItemsIndicatorClicked();

        verify(streamOperations, times(2)).initialStreamItems();
    }

    @Test
    public void onStreamRefreshNewItemsSinceDate() {
        when(featureFlags.isEnabled(Flag.AUTO_REFRESH_STREAM)).thenReturn(true);
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class)))
                .thenReturn(new Date(123L));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.STREAM, StreamEvent.fromStreamRefresh());

        verify(newItemsIndicator).update(5);
    }

    @Test
    public void onStreamRefreshUpdatesOnlyWhenThereAreVisibleItems() {
        when(featureFlags.isEnabled(Flag.AUTO_REFRESH_STREAM)).thenReturn(true);
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class))).thenReturn(null);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.STREAM, StreamEvent.fromStreamRefresh());

        verify(newItemsIndicator, never()).update(5);
    }

    @Test
    public void shouldRefreshOnCreate() {
        when(featureFlags.isEnabled(Flag.AUTO_REFRESH_STREAM)).thenReturn(true);
        when(streamOperations.updatedStreamItemsForStart()).thenReturn(Observable.just(Collections.<StreamItem>emptyList()));
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class))).thenReturn(new Date(123L));
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newItemsIndicator).update(5);
    }

    @Test
    public void shouldNotUpdateIndicatorWhenUpdatedItemsForStartIsEmpty() {
        when(featureFlags.isEnabled(Flag.AUTO_REFRESH_STREAM)).thenReturn(true);
        when(streamOperations.updatedStreamItemsForStart()).thenReturn(Observable.<List<StreamItem>>empty());
        when(streamOperations.getFirstItemTimestamp(anyListOf(StreamItem.class))).thenReturn(new Date(123L));
        when(streamOperations.newItemsSince(123L)).thenReturn(Observable.just(5));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newItemsIndicator, never()).update(5);
    }

    @Test
    public void shouldResetOverlayOnRefreshBinding() {
        presenter.onRefreshBinding();

        verify(newItemsIndicator).hideAndReset();
    }

    @Test
    public void shouldSetOverlayViewOnViewCreated() {
        when(featureFlags.isEnabled(Flag.AUTO_REFRESH_STREAM)).thenReturn(true);
        presenter.onCreate(fragmentRule.getFragment(), null);

        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        verify(newItemsIndicator).setTextView(any(TextView.class));
    }
}
