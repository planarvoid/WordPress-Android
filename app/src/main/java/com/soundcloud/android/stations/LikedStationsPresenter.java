package com.soundcloud.android.stations;

import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class LikedStationsPresenter extends RecyclerViewPresenter<List<StationViewModel>, StationViewModel> {

    private final Func1<StationRecord, StationViewModel> toViewModel = new Func1<StationRecord, StationViewModel>() {
        @Override
        public StationViewModel call(StationRecord station) {
            return StationViewModel.create(station, playQueueManager.getCollectionUrn().equals(station.getUrn()));
        }
    };

    private final StationsOperations operations;
    private final StationsAdapter adapter;
    private final Resources resources;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @LightCycle final StationsNowPlayingController stationsNowPlayingController;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    LikedStationsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                           StationsOperations operations,
                           StationsAdapter adapter,
                           Resources resources,
                           PlayQueueManager playQueueManager,
                           EventBus eventBus,
                           StationsNowPlayingController stationsNowPlayingController,
                           PerformanceMetricsEngine performanceMetricsEngine,
                           ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        super(swipeRefreshAttacher, Options.defaults());
        this.operations = operations;
        this.adapter = adapter;
        this.resources = resources;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.stationsNowPlayingController = stationsNowPlayingController;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
        this.stationsNowPlayingController.setAdapter(adapter);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<StationViewModel>, StationViewModel> onBuildBinding(Bundle bundle) {
        return CollectionBinding
                .from(stationsSource())
                .withAdapter(adapter)
                .addObserver(onNext(this::endMeasureLoadingTime))
                .build();
    }

    private void endMeasureLoadingTime(Iterable<StationViewModel> stations) {
        MetricParams params = MetricParams.of(MetricKey.STATIONS_COUNT, Iterables.size(stations));
        PerformanceMetric performanceMetric = PerformanceMetric.builder()
                                                               .metricType(MetricType.LIKED_STATIONS_LOAD)
                                                               .metricParams(params)
                                                               .build();
        performanceMetricsEngine.endMeasuring(performanceMetric);
    }

    @Override
    protected CollectionBinding<List<StationViewModel>, StationViewModel> onRefreshBinding() {
        return CollectionBinding
                .from(operations.syncLikedStations().flatMap(o -> stationsSource()))
                .withAdapter(adapter)
                .build();
    }

    private Observable<List<StationViewModel>> stationsSource() {
        return RxJava.toV1Observable(operations.collection(StationsCollectionsTypes.LIKED))
                     .map(toViewModel)
                     .toList();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        configureRecyclerView(view);
        configureEmptyView();

        subscription = eventBus.queue(EventQueue.URN_STATE_CHANGED)
                               .filter(event -> event.kind() == UrnStateChangedEvent.Kind.STATIONS_COLLECTION_UPDATED)
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(new RefreshLikedStationsSubscriber());
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
        super.onDestroy(fragment);
    }

    private void configureRecyclerView(View view) {
        RecyclerView recyclerView = getRecyclerView();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(),
                                                            resources.getInteger(R.integer.stations_grid_span_count)));
    }

    private void configureEmptyView() {
        final EmptyView emptyView = getEmptyView();
        emptyView.setMessageText(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.LIKED_STATIONS_EMPTY_VIEW_MESSAGE));
        emptyView.setImage(R.drawable.empty_collection_stations);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private class RefreshLikedStationsSubscriber extends DefaultSubscriber<UrnStateChangedEvent> {

        @Override
        public void onNext(UrnStateChangedEvent args) {
            adapter.clear();
            retryWith(onRefreshBinding());
        }
    }
}
