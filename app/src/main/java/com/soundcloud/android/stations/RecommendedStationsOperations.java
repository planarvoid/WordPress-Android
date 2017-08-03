package com.soundcloud.android.stations;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static java.lang.Math.min;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class RecommendedStationsOperations {
    @VisibleForTesting
    static final int STATIONS_IN_BUCKET = 12;
    private final StationsRepository stationsRepository;
    private final PlayQueueManager playQueueManager;
    private final Scheduler scheduler;
    private final NewSyncOperations syncOperations;

    @Inject
    RecommendedStationsOperations(StationsRepository stationsRepository,
                                  PlayQueueManager playQueueManager,
                                  @Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                                  NewSyncOperations syncOperations) {
        this.stationsRepository = stationsRepository;
        this.playQueueManager = playQueueManager;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
    }

    public Maybe<OldDiscoveryItem> recommendedStations() {
        return load(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_STATIONS));
    }

    public Maybe<OldDiscoveryItem> refreshRecommendedStations() {
        return load(syncOperations.failSafeSync(Syncable.RECOMMENDED_STATIONS));
    }

    private Maybe<OldDiscoveryItem> load(Single<SyncResult> source) {
        return source.flatMapMaybe(result -> getCollection(RECOMMENDATIONS).zipWith(getCollection(RECENT), RecommendedStationsOperations::calculateStationsSuggestions)
                                                                           .filter(stationRecords -> !stationRecords.isEmpty())
                                                                           .map(toDiscoveryItem())
                                                                           .switchIfEmpty(NewSyncOperations.emptyResult(result)))
                     .subscribeOn(scheduler);
    }

    public void clearData() {
        stationsRepository.clearData();
    }

    private Single<List<StationRecord>> getCollection(int collectionType) {
        return stationsRepository.loadStationsCollection(collectionType)
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

    private Function<List<StationRecord>, OldDiscoveryItem> toDiscoveryItem() {
        return stationRecords -> RecommendedStationsBucketItem.create(transformToStationViewModels(
                stationRecords));
    }
}
