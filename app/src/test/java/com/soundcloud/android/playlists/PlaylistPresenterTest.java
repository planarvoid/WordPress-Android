package com.soundcloud.android.playlists;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.Queue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;

import android.os.Bundle;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistPresenterTest extends AndroidUnitTest {

    private static final Urn UPDATED_PLAYLIST_URN = Urn.forPlaylist(456);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();

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

    private TestEventBus eventBus = new TestEventBus();
    private Bundle args;
    private PlaylistPresenter presenter;
    private PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(playlist.toPropertySet(),
                                                                           Arrays.asList(TrackItem.from(track1),
                                                                                         TrackItem.from(track2)));

    @Before
    public void setUp() throws Exception {
        args = PlaylistDetailFragment.createBundle(PLAYLIST_URN, Screen.PLAYLIST_DETAILS, null, null, false);
        fragmentRule.setFragmentArguments(args);

        when(operations.playlist(PLAYLIST_URN)).thenReturn(Observable.just(playlistWithTracks));
        when(upsellOperations.toListItems(playlistWithTracks)).thenReturn(listItems());
        when(adapterFactory.create(any(OnStartDragListener.class))).thenReturn(adapter);

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
                eventBus);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        presenter.onCreate(fragmentRule.getFragment(), args);

        verify(adapter).onNext(listItems());
    }

    @Test
    public void shouldGoBackOnPlaylistDeleted() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromEntityDeleted(PLAYLIST_URN));

        assertThat(fragmentRule.getActivity().isFinishing()).isTrue();
    }

    @Test
    public void shouldReplaceUrnWhenPlaylistPushed() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        final PropertySet updatedPlaylist = PropertySet.from(PlaylistProperty.URN.bind(UPDATED_PLAYLIST_URN));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromPlaylistPushedToServer(PLAYLIST_URN, updatedPlaylist));

        final Bundle fragmentArgs = fragmentRule.getFragment().getArguments();
        assertThat(fragmentArgs.get(PlaylistDetailFragment.EXTRA_URN)).isEqualTo(UPDATED_PLAYLIST_URN);
    }

    @Test
    public void shouldSetPlaylingTrackWhenTrackChanges() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(5)),
                                                                       Urn.NOT_SET,
                                                                       0));

        verify(adapter).updateNowPlaying(Urn.forTrack(5));
    }

    @Test
    public void shouldUpdateCurrentDownloadWhenEventReceived() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        when(adapter.getItems()).thenReturn(listItems());

        final Urn urn = track1.get(TrackProperty.URN);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED,
                         OfflineContentChangedEvent.downloading(Collections.singletonList(urn), false));

        assertThat(track1.get(OfflineProperty.OFFLINE_STATE)).isEqualTo(OfflineState.DOWNLOADING);

        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void shouldUpdateEntityWhenEventReceived() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        when(adapter.getItems()).thenReturn(listItems());

        final Urn urn = track1.get(TrackProperty.URN);
        final EntityStateChangedEvent likedChangedEvent = EntityStateChangedEvent.fromLike(urn, true, 2);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, likedChangedEvent);

        assertThat(track1.get(PlayableProperty.IS_USER_LIKE)).isTrue();
        assertThat(track1.get(PlayableProperty.LIKES_COUNT)).isEqualTo(2);
        verify(adapter).notifyDataSetChanged();
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
        final PublishSubject<PropertySet> editPlaylistOperation = PublishSubject.create();
        final List<Urn> tracks = Arrays.asList(track1.get(EntityProperty.URN), track2.get(EntityProperty.URN));
        final List<TrackItem> trackItems = Arrays.asList(TrackItem.from(track1), TrackItem.from(track2));
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
        when(operations.playlist(PLAYLIST_URN)).thenReturn(Observable.just(createAlbumPlaylist("ep", "2010-10-10")));

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
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksImpression(playlist.getUrn());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onUpsellItemCreated();

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(trackingEvent.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void onUpsellItemClickedSendsUpsellTrackingEvent() {
        UpgradeFunnelEvent expectedEvent = UpgradeFunnelEvent.forPlaylistTracksClick(playlist.getUrn());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onUpsellItemClicked(context());

        UpgradeFunnelEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(trackingEvent.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    private List<TypedListItem> listItems() {
        return Arrays.<TypedListItem>asList(TrackItem.from(track1), TrackItem.from(track2));
    }

    private PlaylistWithTracks createAlbumPlaylist(String type, String releaseDate) {
        PropertySet propertySet = playlist.toPropertySet();
        propertySet.put(PlaylistProperty.IS_ALBUM, true);
        propertySet.put(PlaylistProperty.SET_TYPE, type);
        propertySet.put(PlaylistProperty.RELEASE_DATE, releaseDate);

        return new PlaylistWithTracks(propertySet, Arrays.asList(TrackItem.from(track1), TrackItem.from(track2)));
    }

}
