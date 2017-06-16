package com.soundcloud.android.stations;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.tracks.UpdatePlayingTrackObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.disposables.CompositeDisposable;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;
import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;
import static com.soundcloud.android.stations.StationInfoFragment.EXTRA_SOURCE;
import static com.soundcloud.android.stations.StationInfoFragment.EXTRA_URN;

class StationInfoPresenter extends RecyclerViewPresenter<List<StationInfoItem>, StationInfoItem>
        implements StationInfoClickListener {

    private final StartStationPresenter stationPresenter;
    private final StationsOperations stationOperations;
    private final StationInfoAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final EventBusV2 eventBus;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private DiscoverySource discoverySource;
    private Optional<Urn> seedTrack;
    private Urn stationUrn;

    @Inject
    StationInfoPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         StationInfoAdapterFactory adapterFactory,
                         StationsOperations stationOperations,
                         StartStationPresenter stationPresenter,
                         StationInfoTracksBucketRendererFactory bucketRendererFactory,
                         PlayQueueManager playQueueManager,
                         EventBusV2 eventBus,
                         PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher);
        this.stationOperations = stationOperations;
        this.stationPresenter = stationPresenter;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.adapter = adapterFactory.create(this, bucketRendererFactory.create(this));
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();

        disposables.add(eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackObserver(adapter)));
    }

    @Override
    public void onDestroy(Fragment fragment) {
        super.onDestroy(fragment);
        disposables.clear();
    }

    @Override
    protected CollectionBinding<List<StationInfoItem>, StationInfoItem> onBuildBinding(Bundle fragmentArgs) {
        discoverySource = getDiscoverySource(fragmentArgs);
        seedTrack = getSeedTrackUrn(fragmentArgs);
        stationUrn = fragmentArgs.getParcelable(EXTRA_URN);

        return CollectionBinding.fromV2(getStation(stationUrn))
                                .withAdapter(adapter)
                                .addObserver(onNext(this::endMeasureLoadStation))
                                .build();
    }

    private void endMeasureLoadStation(Iterable<StationInfoItem> items) {
        MetricParams metricParams = MetricParams.of(MetricKey.TRACKS_COUNT, findTracksCount(items));
        PerformanceMetric performanceMetric = PerformanceMetric.builder()
                                                               .metricType(MetricType.LOAD_STATION)
                                                               .metricParams(metricParams)
                                                               .build();

        performanceMetricsEngine.endMeasuring(performanceMetric);
    }

    private int findTracksCount(Iterable<StationInfoItem> items) {
        StationInfoItem stationInfoItem = Iterables.find(items, item -> item instanceof StationInfoTracksBucket);
        return ((StationInfoTracksBucket) stationInfoItem).stationTracks().size();
    }

    private DiscoverySource getDiscoverySource(Bundle fragmentArgs) {
        return DiscoverySource.from(fragmentArgs.getString(EXTRA_SOURCE, STATIONS.value()));
    }

    private Optional<Urn> getSeedTrackUrn(Bundle fragmentArgs) {
        return Optional.fromNullable(fragmentArgs.getParcelable(StationInfoFragment.EXTRA_SEED_TRACK));
    }

    private Maybe<List<StationInfoItem>> getStation(final Urn stationUrn) {
        return stationOperations.stationWithTracks(stationUrn, seedTrack)
                                .map(stationWithTracks -> Arrays.asList(StationInfoHeader.from(stationWithTracks), createTracksBucketViewModel(stationWithTracks)));
    }

    private StationInfoTracksBucket createTracksBucketViewModel(StationWithTracks station) {
        final Urn nowPlayingCollection = playQueueManager.getCollectionUrn();
        final Urn nowPlayingUrn = playQueueManager.getCurrentPlayQueueItem().getUrnOrNotSet();

        if (nowPlayingCollection.equals(stationUrn) && !Urn.NOT_SET.equals(nowPlayingUrn)) {
            return StationInfoTracksBucket.from(station, nowPlayingUrn);
        } else {
            return StationInfoTracksBucket.from(station);
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onPlayButtonClicked(Context context) {
        stationPresenter.startStation(context, stationUrn, discoverySource);
    }

    @Override
    public void onTrackClicked(Context context, int position) {
        stationPresenter.startStation(context,
                                      stationOperations.station(stationUrn),
                                      discoverySource, position);
    }

    @Override
    public void onLikeToggled(Context context, boolean liked) {
        stationOperations.toggleStationLikeAndForget(stationUrn, liked);
    }

}
