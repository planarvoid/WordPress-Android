package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackEvent;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestPager;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.observers.TestSubscriber;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamPresenterTest {

    private SoundStreamPresenter presenter;

    @Mock private SoundStreamOperations streamOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private MixedPlayableRecyclerItemAdapter adapter;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private DateProvider dateProvider;
    @Mock private Observer<Iterable<PlayableItem>> itemObserver;
    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;

    private TestEventBus eventBus = new TestEventBus();
    private TestSubscriber<PlaybackResult> testSubscriber = new TestSubscriber<>();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Before
    public void setUp() throws Exception {
        presenter = new SoundStreamPresenter(streamOperations, playbackOperations, adapter, imagePauseOnScrollListener,
                swipeRefreshAttacher, expandPlayerSubscriberProvider, eventBus);
        when(streamOperations.initialStreamItems()).thenReturn(Observable.<List<PropertySet>>empty());
        when(streamOperations.pagingFunction()).thenReturn(TestPager.<List<PropertySet>>singlePageFunction());
        when(view.findViewById(R.id.recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(adapter.getTrackRenderer()).thenReturn(trackRenderer);
        when(dateProvider.getCurrentTime()).thenReturn(100L);
        when(view.getResources()).thenReturn(Robolectric.application.getResources());
    }

    @Test
    public void canLoadStreamItems() {
        List<PropertySet> items = Arrays.asList(
                TestPropertySets.expectedPromotedTrack(),
                TestPropertySets.expectedTrackForListItem(Urn.forTrack(123L)),
                TestPropertySets.expectedLikedPlaylistForPlaylistsScreen()
        );
        when(streamOperations.initialStreamItems()).thenReturn(Observable.just(items));

        CollectionBinding<PlayableItem> binding = presenter.onBuildBinding(null);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(Arrays.asList(
                PromotedTrackItem.from(items.get(0)),
                TrackItem.from(items.get(1)),
                PlaylistItem.from(items.get(2))
        ));
    }

    @Test
    public void canRefreshStreamItems() {
        final PropertySet streamItem = TestPropertySets.expectedTrackForListItem(Urn.forTrack(123L));
        when(streamOperations.updatedStreamItems()).thenReturn(Observable.just(
                Collections.singletonList(streamItem)
        ));

        CollectionBinding<PlayableItem> binding = presenter.onRefreshBinding();
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(Collections.<PlayableItem>singletonList(TrackItem.from(streamItem)));
    }

    @Test
    public void playsTracksOnTrackItemClick() {
        final TrackItem clickedTrack = ModelFixtures.create(TrackItem.class);
        final List<Urn> streamTrackUrns = Arrays.asList(clickedTrack.getEntityUrn(), Urn.forTrack(634L));
        final Observable<List<Urn>> streamTracks = Observable.just(streamTrackUrns);

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);
        when(playbackOperations.playTracks(eq(streamTracks), eq(clickedTrack.getEntityUrn()), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onItemClicked(view, 0);

        expect(testSubscriber.getOnNextEvents()).toNumber(1);
        expect(testSubscriber.getOnNextEvents().get(0).isSuccess()).toBeTrue();
    }

    @Test
    public void tracksPromotedTrackItemClick() {
        final PromotedTrackItem clickedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        final List<Urn> streamTrackUrns = Arrays.asList(clickedTrack.getEntityUrn(), Urn.forTrack(634L));
        final Observable<List<Urn>> streamTracks = Observable.just(streamTrackUrns);

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);
        when(playbackOperations.playTracks(eq(streamTracks), eq(clickedTrack.getEntityUrn()), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onItemClicked(view, 0);

        expect(eventBus.lastEventOn(EventQueue.TRACKING)).toBeInstanceOf(PromotedTrackEvent.class);
    }

    @Test
    public void includesPromotedSourceInfoOnPlayFromPromotedTrack() {
        final PromotedTrackItem clickedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        final List<Urn> streamTrackUrns = Arrays.asList(clickedTrack.getEntityUrn(), Urn.forTrack(634L));
        final Observable<List<Urn>> streamTracks = Observable.just(streamTrackUrns);
        ArgumentCaptor<PlaySessionSource> captor = ArgumentCaptor.forClass(PlaySessionSource.class);
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);
        when(playbackOperations.playTracks(eq(streamTracks), eq(clickedTrack.getEntityUrn()), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onItemClicked(view, 0);

        verify(playbackOperations).playTracks(eq(streamTracks), eq(clickedTrack.getEntityUrn()), eq(0), captor.capture());
        PlaySessionSource sessionSource = captor.getValue();
        expect(sessionSource.isFromPromotedTrack()).toBeTrue();
    }

    @Test
    public void opensPlaylistScreenOnPlaylistItemClick() {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        when(adapter.getItem(0)).thenReturn(playlistItem);
        when(view.getContext()).thenReturn(Robolectric.application);

        presenter.onItemClicked(view, 0);

        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PlaylistDetailActivity.EXTRA_URN)).toEqual(playlistItem.getEntityUrn());
    }

    @Test
    public void configuresEmptyStateImage() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(emptyView).setImage(R.drawable.empty_stream);
    }

    @Test
    public void configuresEmptyStateForOnboardingFailure() {
        presenter.onCreate(fragment, null);
        presenter.setOnboardingSuccess(false);

        presenter.onViewCreated(fragment, view, null);

        verify(emptyView).setMessageText(R.string.error_onboarding_fail);
    }

    @Test
    public void configuresEmptyStateForOnboardingSuccess() {
        presenter.onCreate(fragment, null);
        presenter.setOnboardingSuccess(true);

        presenter.onViewCreated(fragment, view, null);

        verify(emptyView).setMessageText(R.string.list_empty_stream_message);
        verify(emptyView).setActionText(R.string.list_empty_stream_action);
    }

    @Test
    public void unsubscribesFromEventBusOnDestroyView() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        when(recyclerView.getAdapter()).thenReturn(adapter);

        presenter.onDestroyView(fragment);

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void trackChangedEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack));

        verify(trackRenderer).setPlayingTrack(playingTrack);
    }

    @Test
    public void newQueueEventUpdatesCurrentlyPlayingTrack() {
        final Urn playingTrack = Urn.forTrack(123L);
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(playingTrack));

        verify(trackRenderer).setPlayingTrack(playingTrack);
    }

}
