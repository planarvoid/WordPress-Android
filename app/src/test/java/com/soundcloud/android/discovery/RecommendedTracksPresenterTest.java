package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.TracksRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

public class RecommendedTracksPresenterTest extends AndroidUnitTest {

    private static final long SEED_ID = 123L;
    private static final Urn RECOMMENDED_ENTITY_1 = Urn.forTrack(1L);
    private static final Urn RECOMMENDED_ENTITY_2 = Urn.forTrack(2L);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private View view;
    @Mock private Fragment fragment;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private Resources resources;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private TracksRecyclerItemAdapter adapter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private Bundle bundle;
    @Mock private TrackItemRenderer trackItemRenderer;

    private RecommendedTracksPresenter presenter;
    private TestEventBus eventBus = new TestEventBus();
    private TestSubscriber<PlaybackResult> testSubscriber = new TestSubscriber<>();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Mock TrackItem recommendedTrackItemOne;
    @Mock TrackItem recommendedTrackItemTwo;

    @Before
    public void setUp() {
        this.presenter = new RecommendedTracksPresenter(swipeRefreshAttacher, discoveryOperations,
                adapter, expandPlayerSubscriberProvider, playbackOperations, eventBus);

        when(fragment.getArguments()).thenReturn(bundle);
        when(bundle.getLong(RecommendedTracksPresenter.EXTRA_LOCAL_SEED_ID)).thenReturn(SEED_ID);
        when(adapter.getTrackRenderer()).thenReturn(trackItemRenderer);


        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.getContext()).thenReturn(context());
        when(view.getResources()).thenReturn(context().getResources());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        when(recommendedTrackItemOne.getEntityUrn()).thenReturn(RECOMMENDED_ENTITY_1);
        when(recommendedTrackItemTwo.getEntityUrn()).thenReturn(RECOMMENDED_ENTITY_2);

        final List<TrackItem> trackItems = Arrays.asList(recommendedTrackItemOne, recommendedTrackItemTwo);
        when(discoveryOperations.recommendedTracksForSeed(anyLong())).thenReturn(Observable.just(trackItems));
        when(adapter.getItems()).thenReturn(trackItems);
    }

    @Test
    public void clickOnTrackPlaysItAndEnqueueRecommendedTracks() {
        when(adapter.getItem(1)).thenReturn(recommendedTrackItemTwo);

        final Observable<List<Urn>> playQueue = Observable.just(Arrays.asList(RECOMMENDED_ENTITY_1, RECOMMENDED_ENTITY_2));
        when(discoveryOperations.recommendedTracks()).thenReturn(playQueue);

        final PlaybackResult successResult = PlaybackResult.success();
        when(playbackOperations.playTracks(eq(playQueue), eq(RECOMMENDED_ENTITY_2), eq(0), any(PlaySessionSource.class)))
                .thenReturn(Observable.just(successResult));

        presenter.onItemClicked(view, 1);
        testSubscriber.assertValues(successResult);
    }

    @Test
    public void updatesTrackAdapterRendererOnTrackChange() {
        presenter.onCreate(fragment, bundle);
        presenter.onViewCreated(fragment, view, null);

        final Urn playingTrack = Urn.forTrack(123L);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack, Urn.NOT_SET, 1));

        InOrder inOrder = Mockito.inOrder(trackItemRenderer, adapter);
        inOrder.verify(trackItemRenderer).setPlayingTrack(playingTrack);
        inOrder.verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotUpdateTrackAdapterRendererOnTrackChangeAfterViewDestroyed() {
        presenter.onCreate(fragment, bundle);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        final Urn playingTrack = Urn.forTrack(123L);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(playingTrack, Urn.NOT_SET, 1));

        verify(trackItemRenderer, never()).setPlayingTrack(any(Urn.class));
        verify(adapter, never()).notifyDataSetChanged();
    }

    @Test
    public void updatesTrackItemsOnEntityStateChanged() {
        presenter.onCreate(fragment, bundle);
        presenter.onViewCreated(fragment, view, null);

        final PropertySet changeSet = PropertySet.from(TrackProperty.URN.bind(RECOMMENDED_ENTITY_1));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(changeSet));

        InOrder inOrder = Mockito.inOrder(recommendedTrackItemOne, adapter);
        inOrder.verify(recommendedTrackItemOne).update(changeSet);
        inOrder.verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void doesNotUpdateTrackItemsOnEntityStateChangedAfterOnDestroyView() {
        presenter.onCreate(fragment, bundle);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        final PropertySet changeSet = PropertySet.from(TrackProperty.URN.bind(RECOMMENDED_ENTITY_1));
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(changeSet));

        verify(recommendedTrackItemOne, never()).update(any(PropertySet.class));
        verify(adapter, never()).notifyDataSetChanged();
    }
}