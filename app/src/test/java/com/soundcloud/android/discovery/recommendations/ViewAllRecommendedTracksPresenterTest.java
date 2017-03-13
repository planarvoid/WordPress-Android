package com.soundcloud.android.discovery.recommendations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.DiscoveryAdapter;
import com.soundcloud.android.discovery.DiscoveryAdapterFactory;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.List;

public class ViewAllRecommendedTracksPresenterTest extends AndroidUnitTest {
    private static final Urn SEED_URN = new Urn("soundcloud:tracks:seed");
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final TrackQueueItem TRACK_QUEUE_ITEM = TestPlayQueueItem.createTrack(TRACK_URN);
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private View view;
    @Mock private Fragment fragment;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private RecommendedTracksOperations recommendedTracksOperations;
    @Mock private RecommendationBucketRendererFactory recommendationBucketRendererFactory;
    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private DiscoveryAdapterFactory adapterFactory;
    @Mock private DiscoveryAdapter adapter;
    @Mock private SimpleItemAnimator itemAnimator;
    @Mock private Bundle bundle;
    @Mock private TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    @Mock private List<DiscoveryItem> discoveryItems;
    @Mock private UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;

    private UpdatePlayableAdapterSubscriber updatePlayableAdapterSubscriber;
    private ViewAllRecommendedTracksPresenter presenter;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        when(recyclerView.getAdapter()).thenReturn(adapter);
        when(recyclerView.getItemAnimator()).thenReturn(itemAnimator);
        when(adapter.getItems()).thenReturn(discoveryItems);
        when(recommendedTracksOperations.allBuckets()).thenReturn(Observable.empty());
        when(recommendedTracksOperations.tracksForSeed(anyLong())).thenReturn(Observable.empty());
        when(recommendationBucketRendererFactory.create(eq(false),
                                                        any(ViewAllRecommendedTracksPresenter.class))).thenReturn(
                recommendationBucketRenderer);
        when(adapterFactory.create(any(RecommendationBucketRenderer.class))).thenReturn(adapter);
        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        updatePlayableAdapterSubscriber = spy(new UpdatePlayableAdapterSubscriber(adapter));
        when(updatePlayableAdapterSubscriberFactory.create(adapter)).thenReturn(updatePlayableAdapterSubscriber);

        this.presenter = new ViewAllRecommendedTracksPresenter(swipeRefreshAttacher,
                                                               recommendedTracksOperations,
                                                               recommendationBucketRendererFactory,
                                                               adapterFactory,
                                                               eventBus,
                                                               trackRecommendationPlaybackInitiator,
                                                               updatePlayableAdapterSubscriberFactory);
    }

    @Test
    public void updatesTrackAdapterRendererOnTrackChange() {
        presenter.onCreate(fragment, bundle);
        presenter.onViewCreated(fragment, view, null);

        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_QUEUE_ITEM,
                                                                                              Urn.NOT_SET,
                                                                                              1);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);
        verify(updatePlayableAdapterSubscriber).onNext(event);
        presenter.onDestroyView(fragment);
    }

    @Test
    public void doesNotUpdateTrackAdapterRendererOnTrackChangeAfterViewDestroyed() {
        presenter.onCreate(fragment, bundle);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(123L)),
                                                                       Urn.NOT_SET,
                                                                       1));

        verify(updatePlayableAdapterSubscriber, never()).onNext(any(CurrentPlayQueueItemEvent.class));
    }

    @Test
    public void propagatesOnReasonClickedToRecommendationPlaybackInitiator() {
        presenter.onReasonClicked(SEED_URN);
        verify(trackRecommendationPlaybackInitiator).playFromReason(SEED_URN,
                                                                    Screen.RECOMMENDATIONS_MAIN,
                                                                    discoveryItems);
    }

    @Test
    public void propagatesOnTrackClickedToRecommendationPlaybackInitiator() {
        presenter.onTrackClicked(SEED_URN, TRACK_URN);
        verify(trackRecommendationPlaybackInitiator).playFromRecommendation(SEED_URN,
                                                                            TRACK_URN,
                                                                            Screen.RECOMMENDATIONS_MAIN,
                                                                            discoveryItems);
    }
}
