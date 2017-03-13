package com.soundcloud.android.stations;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static java.lang.Math.min;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class RecommendedStationsOperations {
    @VisibleForTesting
    static final int STATIONS_IN_BUCKET = 12;

    private static final Func2<List<StationRecord>, List<StationRecord>, List<StationRecord>> MOVE_RECENT_TO_END =
            (suggestions, recent) -> calculateStationsSuggestions(suggestions, recent);
    private final Func1<SyncOperations.Result, Observable<DiscoveryItem>> loadRecommendedStations = result -> getCollection(RECOMMENDATIONS)
            .zipWith(getCollection(RECENT), MOVE_RECENT_TO_END)
            .filter(IS_NOT_EMPTY_LIST)
            .map(toDiscoveryItem())
            .switchIfEmpty(SyncOperations.emptyResult(result));

    private Func1<List<StationRecord>, DiscoveryItem> toDiscoveryItem() {
        return stationRecords -> RecommendedStationsBucketItem.create(transformToStationViewModels(
                stationRecords));
    }

    private final StationsStorage stationsStorage;
    private final PlayQueueManager playQueueManager;
    private final Scheduler scheduler;
    private final SyncOperations syncOperations;

    @Inject
    RecommendedStationsOperations(StationsStorage stationsStorage,
                                  PlayQueueManager playQueueManager,
                                  @Named(HIGH_PRIORITY) Scheduler scheduler,
                                  SyncOperations syncOperations) {
        this.stationsStorage = stationsStorage;
        this.playQueueManager = playQueueManager;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
    }

    public Observable<DiscoveryItem> recommendedStations() {
        return load(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_STATIONS));
    }

    public Observable<DiscoveryItem> refreshRecommendedStations() {
        return load(syncOperations.failSafeSync(Syncable.RECOMMENDED_STATIONS));
    }

    private Observable<DiscoveryItem> load(Observable<SyncOperations.Result> source) {
        return source
                .flatMap(loadRecommendedStations)
                .subscribeOn(scheduler);
    }

    public void clearData() {
        stationsStorage.clear();
    }

    private Observable<List<StationRecord>> getCollection(int collectionType) {
        return stationsStorage
                .getStationsCollection(collectionType)
                .toList()
                .subscribeOn(scheduler);
    }

    private List<StationViewModel> transformToStationViewModels(List<StationRecord> records) {
        final List<StationViewModel> models = new ArrayList<>(records.size());
        final Urn playingCollectionUrn = playQueueManager.getCollectionUrn();
        for (StationRecord record : records) {
            boolean isPlaying = record.getUrn().equals(playingCollectionUrn);
            StationViewModel viewModel = StationViewModel.create(record, isPlaying);
            models.add(viewModel);
        }
        return models;
    }

    private static List<StationRecord> calculateStationsSuggestions(List<StationRecord> suggestions,
                                                                    List<StationRecord> recent) {
        recent.retainAll(suggestions);
        suggestions.removeAll(recent);
        suggestions.addAll(Lists.reverse(recent));

        return suggestions.subList(0, min(suggestions.size(), STATIONS_IN_BUCKET));
    }

}
