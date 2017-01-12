package com.soundcloud.android.playlists;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.Queue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;

import android.content.res.Resources;
import android.os.Bundle;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistPresenterTest extends AndroidUnitTest {

    private static final Urn UPDATED_PLAYLIST_URN = Urn.forPlaylist(456);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    private final TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private final TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_details_fragment, new Bundle());

    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = providerOf(mock(ExpandPlayerSubscriber.class));
    @Mock private PlaylistOperations operations;
    @Mock private PlaylistUpsellOperations upsellOperations;
    @Mock private SwipeRefreshAttacher swipeAttacher;
    @Mock private CollapsingScrollHelper profileScrollHelper;
    @Mock private PlaylistHeaderPresenter headerPresenter;
    @Mock private PlaylistContentPresenter playlistContentPresenter;
    @Mock private PlaylistAdapterFactory adapterFactory;
    @Mock private PlaylistAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private Resources resources;
    @Mock private PlaylistTrackItemRendererFactory trackRendererFactory;
    @Mock private PlaylistTrackItemRenderer trackItemRenderer;
    @Captor private ArgumentCaptor<List<PlaylistDetailItem>> trackItemCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Bundle args;
    private PlaylistPresenter presenter;
    private PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(PlaylistItem.from(apiPlaylist), Arrays.asList(track1, track2));
    private List<PlaylistDetailItem> itemList;


    @Before
    public void setUp() throws Exception {
        args = PlaylistDetailFragment.createBundle(PLAYLIST_URN, Screen.PLAYLIST_DETAILS, null, null, false);
        fragmentRule.setFragmentArguments(args);

        itemList = Arrays.asList(new PlaylistDetailHeaderItem(), new PlaylistDetailTrackItem(track1), new PlaylistDetailTrackItem(track2));
        when(operations.playlistWithTracksAndRecommendations(PLAYLIST_URN, true)).thenReturn(Observable.just(new PlaylistDetailsViewModel(playlistWithTracks,
                                                                                                                                          itemList)));
        when(trackRendererFactory.create(any(TrackItemMenuPresenter.RemoveTrackListener.class))).thenReturn(trackItemRenderer);
        when(adapterFactory.create(any(OnStartDragListener.class), same(headerPresenter), same(trackItemRenderer))).thenReturn(adapter);

        createPresenter();
    }

    private void createPresenter() {
        presenter = new PlaylistPresenter(operations,
                upsellOperations,
                swipeAttacher,
                headerPresenter,
                playlistContentPresenter,
                adapterFactory,
                playbackInitiator,
                expandPlayerSubscriberProvider,
                navigator,
                eventBus,
                resources,
                                          trackRendererFactory);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        presenter.onCreate(fragmentRule.getFragment(), args);

        verify(adapter).onNext(trackItemCaptor.capture());
        List<PlaylistDetailItem> itemList = trackItemCaptor.getValue();
        assertThat(itemList.get(0)).isInstanceOf(PlaylistDetailHeaderItem.class);
        assertThat(((PlaylistDetailTrackItem)  itemList.get(1)).getTrackItem()).isEqualTo(track1);
        assertThat(((PlaylistDetailTrackItem)  itemList.get(2)).getTrackItem()).isEqualTo(track2);
    }

    @Test
    public void shouldGoBackOnPlaylistDeleted() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        eventBus.publish(EventQueue.URN_STATE_CHANGED,
                         UrnStateChangedEvent.fromEntityDeleted(PLAYLIST_URN));

        assertThat(fragmentRule.getActivity().isFinishing()).isTrue();
    }

    @Test
    public void shouldReplaceUrnWhenPlaylistPushed() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        playlistItem.setUrn(UPDATED_PLAYLIST_URN);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistPushedToServer(PLAYLIST_URN, playlistItem));

        final Bundle fragmentArgs = fragmentRule.getFragment().getArguments();
        assertThat(fragmentArgs.get(PlaylistDetailFragment.EXTRA_URN)).isEqualTo(UPDATED_PLAYLIST_URN);
    }

    @Test
    public void shouldUpdateCurrentDownloadWhenEventReceived() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        when(adapter.getItems()).thenReturn(listItems());

        final Urn urn = track1.getUrn();
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         OfflineContentChangedEvent.downloading(Collections.singletonList(urn), false));

        assertThat(track1.getOfflineState()).isEqualTo(OfflineState.DOWNLOADING);

        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void shouldUpdateEntityWhenEventReceived() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        when(adapter.getItems()).thenReturn(listItems());

        final Urn urn = track1.getUrn();
        final LikesStatusEvent likedChangedEvent = LikesStatusEvent.create(urn, true, 2);
        eventBus.publish(EventQueue.LIKE_CHANGED, likedChangedEvent);

        assertThat(track1.isLikedByCurrentUser()).isTrue();
        assertThat(track1.getLikesCount()).isEqualTo(2);
        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroyView() {
        // workaround for https://soundcloud.atlassian.net/browse/MC-459
        final PublishSubject<Object> queueSubject = PublishSubject.create();
        final EventBus mockEventBus = mock(EventBus.class);

        when(mockEventBus.queue(any(Queue.class))).thenReturn(queueSubject);
        when(mockEventBus.subscribe(any(Queue.class), any(Observer.class))).thenReturn(mock(Subscription.class));
        createPresenter();

        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onDestroyView(fragmentRule.getFragment());

        assertThat(queueSubject.hasObservers()).isFalse();
    }

    @Test
    public void savePlaylist() {
        final PublishSubject<PlaylistItem> editPlaylistOperation = PublishSubject.create();
        final List<Urn> tracks = Arrays.asList(track1.getUrn(), track2.getUrn());
        final List<TrackItem> trackItems = Arrays.asList(track1, track2);
        when(adapter.getTracks()).thenReturn(trackItems);
        when(operations.editPlaylist(playlistWithTracks.getUrn(),
                                     playlistWithTracks.getTitle(),
                                     playlistWithTracks.isPrivate(),
                                     tracks)).thenReturn(editPlaylistOperation);

        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        presenter.savePlaylist();

        assertThat(editPlaylistOperation.hasObservers()).isTrue();
    }

    @Test
    public void setsTitleForActivityAsPlaylist() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        assertThat(fragmentRule.getActivity().getTitle()).isEqualTo("Playlist");
    }

    @Test
    public void setsTitleForActivityAsAlbumTypeWithReleaseDateWhenPlaylistIsAnAlbumAndReleaseDateIsAvailable() {
        when(operations.playlistWithTracksAndRecommendations(PLAYLIST_URN, true)).thenReturn(Observable.just(createAlbumPlaylist("ep", "2010-10-10")));

        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        assertThat(fragmentRule.getActivity().getTitle()).isEqualTo("EP · 2010");
    }

    @Test
    public void onUpsellItemDismissedUpsellsGetDisabled() {
        presenter.onUpsellItemDismissed(0);

        verify(upsellOperations).disableUpsell();
    }

    @Test
    public void onUpsellItemClickedOpensUpgradeScreen() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onUpsellItemClicked(context());

        verify(navigator).openUpgrade(context());
    }

    @Test
    public void onUpsellItemCreatedSendsUpsellTrackingEvent() {
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksImpression(apiPlaylist.getUrn());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onUpsellItemCreated();

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
    }

    @Test
    public void onUpsellItemClickedSendsUpsellTrackingEvent() {
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksClick(apiPlaylist.getUrn());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onUpsellItemClicked(context());

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
    }

    private List<PlaylistDetailItem> listItems() {
        List<PlaylistDetailItem> playlistDetailItems = new ArrayList<>();
        playlistDetailItems.add(new PlaylistDetailTrackItem(track1));
        playlistDetailItems.add(new PlaylistDetailTrackItem(track2));
        return playlistDetailItems;
    }

    private PlaylistDetailsViewModel createAlbumPlaylist(String type, String releaseDate) {
        apiPlaylist.setIsAlbum(true);
        apiPlaylist.setSetType(type);
        apiPlaylist.setReleaseDate(releaseDate);

        return new PlaylistDetailsViewModel(new PlaylistWithTracks(PlaylistItem.from(apiPlaylist), Arrays.asList(track1, track2)), Collections.emptyList());
    }

}
