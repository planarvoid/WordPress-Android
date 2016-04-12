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
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.Queue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import android.os.Bundle;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistPresenterTest extends AndroidUnitTest {

    private static final Urn UPDATED_PLAYLIST_URN = Urn.forPlaylist(456);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track3 = ModelFixtures.create(ApiTrack.class).toPropertySet();

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_details_fragment, new Bundle());

    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = providerOf(mock(ExpandPlayerSubscriber.class));
    @Mock private PlaylistOperations operations;
    @Mock private SwipeRefreshAttacher swipeAttacher;
    @Mock private CollapsingScrollHelper profileScrollHelper;
    @Mock private PlaylistHeaderPresenter headerPresenter;
    @Mock private PlaylistAdapterFactory adapterFactory;
    @Mock private PlaylistAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;

    private TestEventBus eventBus = new TestEventBus();
    private Bundle args;
    private PlaylistPresenter presenter;
    private PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(playlist.toPropertySet(), Arrays.asList(TrackItem.from(track1), TrackItem.from(track2)));
    private PlaylistWithTracks updatedPlaylistWithTracks = new PlaylistWithTracks(playlist.toPropertySet(), Arrays.asList(TrackItem.from(track1), TrackItem.from(track2), TrackItem.from(track3)));

    @Before
    public void setUp() throws Exception {
        args = PlaylistDetailFragment.createBundle(PLAYLIST_URN, Screen.PLAYLIST_DETAILS, null, null, false);
        fragmentRule.setFragmentArguments(args);

        when(adapterFactory.create(any(PlaylistHeaderPresenter.class))).thenReturn(adapter);
        when(operations.playlist(PLAYLIST_URN)).thenReturn(Observable.just(playlistWithTracks));

        presenter = new TestPlaylistPresenter(eventBus);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        presenter.onCreate(fragmentRule.getFragment(), args);

        verify(adapter).onNext(listItems());
    }

    @Test
    public void shouldAddTrackToPlaylist() {
        when(operations.playlist(PLAYLIST_URN)).thenReturn(Observable.just(playlistWithTracks), Observable.just(updatedPlaylistWithTracks));
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                EntityStateChangedEvent.fromTrackAddedToPlaylist(PLAYLIST_URN, 3));

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).onNext(listItems());
        inOrder.verify(adapter).clear();
        inOrder.verify(adapter).onNext(updatedTrackItems());
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
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(5)), Urn.NOT_SET, 0));

        verify(adapter).updateNowPlaying(Urn.forTrack(5));
    }

    @Test
    public void shouldUpdateCurrentDownloadWhenEventReceived() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);

        when(adapter.getItems()).thenReturn(listItems());

        final Urn urn = track1.get(TrackProperty.URN);
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, OfflineContentChangedEvent.downloading(Collections.singletonList(urn), false));

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
        presenter = new TestPlaylistPresenter(mockEventBus);

        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), args);
        presenter.onDestroyView(fragmentRule.getFragment());

        assertThat(queueSubject.hasObservers()).isFalse();
    }

    private List<ListItem> listItems() {
        return Arrays.<ListItem>asList(TrackItem.from(track1), TrackItem.from(track2));
    }

    private List<ListItem> updatedTrackItems() {
        return Arrays.<ListItem>asList(TrackItem.from(track1), TrackItem.from(track2), TrackItem.from(track3));
    }

    private class TestPlaylistPresenter extends PlaylistPresenter {
        public TestPlaylistPresenter(EventBus eventBus) {
            super(operations, swipeAttacher, PlaylistPresenterTest.this.headerPresenter, adapterFactory, eventBus, navigator,
                    new ViewStrategyFactory(providerOf(eventBus), providerOf(playbackInitiator), providerOf(operations), providerOf(expandPlayerSubscriberProvider)));
        }

        @Override
        protected Func1<PlaylistWithTracks, Iterable<ListItem>> getListItemTransformation() {
            return new Func1<PlaylistWithTracks, Iterable<ListItem>>() {
                @Override
                public Iterable<ListItem> call(PlaylistWithTracks playlistWithTracks) {
                    return new ArrayList<ListItem>(playlistWithTracks.getTracks());
                }
            };
        }
    }
}
