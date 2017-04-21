package com.soundcloud.android.olddiscovery.recommendations;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.OldDiscoveryAdapter;
import com.soundcloud.android.olddiscovery.OldDiscoveryAdapterFactory;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriberFactory;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class ViewAllRecommendedTracksPresenter extends RecyclerViewPresenter<List<OldDiscoveryItem>, OldDiscoveryItem>
        implements TrackRecommendationListener {
    private final RecommendedTracksOperations operations;
    private final UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory;
    private final OldDiscoveryAdapter adapter;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final EventBus eventBus;
    private final TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator;
    private Subscription subscription;


    @Inject
    ViewAllRecommendedTracksPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                      RecommendedTracksOperations operations,
                                      RecommendationBucketRendererFactory recommendationBucketRendererFactory,
                                      OldDiscoveryAdapterFactory adapterFactory,
                                      EventBus eventBus,
                                      TrackRecommendationPlaybackInitiator trackRecommendationPlaybackInitiator,
                                      UpdatePlayableAdapterSubscriberFactory updatePlayableAdapterSubscriberFactory,
                                      PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher, Options.custom().build());
        this.operations = operations;
        this.updatePlayableAdapterSubscriberFactory = updatePlayableAdapterSubscriberFactory;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.adapter = adapterFactory.create(recommendationBucketRendererFactory.create(false, this));
        this.eventBus = eventBus;
        this.trackRecommendationPlaybackInitiator = trackRecommendationPlaybackInitiator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        subscription = eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                                          updatePlayableAdapterSubscriberFactory.create(adapter));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        super.onDestroyView(fragment);
        subscription.unsubscribe();
    }

    @Override
    public void onReasonClicked(Urn seedUrn) {
        trackRecommendationPlaybackInitiator.playFromReason(seedUrn, Screen.RECOMMENDATIONS_MAIN, adapter.getItems());
    }

    @Override
    public void onTrackClicked(Urn seedUrn, Urn trackUrn) {
        trackRecommendationPlaybackInitiator.playFromRecommendation(seedUrn,
                                                                    trackUrn,
                                                                    Screen.RECOMMENDATIONS_MAIN,
                                                                    adapter.getItems());
    }

    @Override
    protected CollectionBinding<List<OldDiscoveryItem>, OldDiscoveryItem> onBuildBinding(Bundle bundle) {
        return collectionBindingBuilder()
                .addObserver(onNext(o -> endMeasuringLoadingPerformance()))
                .build();
    }

    private void endMeasuringLoadingPerformance() {
        performanceMetricsEngine.endMeasuring(MetricType.SUGGESTED_TRACKS_LOAD);
    }

    @Override
    protected CollectionBinding<List<OldDiscoveryItem>, OldDiscoveryItem> onRefreshBinding() {
        return collectionBindingBuilder()
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private CollectionBinding.Builder<List<OldDiscoveryItem>, OldDiscoveryItem, List<OldDiscoveryItem>> collectionBindingBuilder() {
        return CollectionBinding
                .from(getSource())
                .withAdapter(adapter);
    }


    private Observable<List<OldDiscoveryItem>> getSource() {
        return operations.allBuckets()
                         .concatWith(Observable.just(OldDiscoveryItem.forRecommendedTracksFooter()))
                         .toList();
    }
}
