package com.soundcloud.android.stations;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class RecommendedStationsOperations {

    private static final Func2<List<StationRecord>, List<StationRecord>, List<StationRecord>> MOVE_RECENT_TO_END =
            new Func2<List<StationRecord>, List<StationRecord>, List<StationRecord>>() {
                @Override
                public List<StationRecord> call(List<StationRecord> suggestions, List<StationRecord> recent) {
                    recent.retainAll(suggestions);
                    suggestions.removeAll(recent);
                    suggestions.addAll(Lists.reverse(recent));
                    return suggestions;
                }
            };

    private static final Func1<List<StationRecord>, DiscoveryItem> TO_RECOMMENDED_STATIONS_BUCKET =
            new Func1<List<StationRecord>, DiscoveryItem>() {
                @Override
                public DiscoveryItem call(List<StationRecord> stationRecords) {
                    return RecommendedStationsItem.create(stationRecords);
                }
            };

    private final StationsStorage stationsStorage;
    private final Scheduler scheduler;
    private final SyncOperations syncOperations;

    @Inject
    RecommendedStationsOperations(StationsStorage stationsStorage,
                                  @Named(HIGH_PRIORITY) Scheduler scheduler,
                                  SyncOperations syncOperations) {
        this.stationsStorage = stationsStorage;
        this.scheduler = scheduler;
        this.syncOperations = syncOperations;
    }

    public Observable<DiscoveryItem> stationsBucket() {
        return recommendedStations()
                .filter(IS_NOT_EMPTY_LIST)
                .map(TO_RECOMMENDED_STATIONS_BUCKET);
    }

    public void clearData() {
        stationsStorage.clear();
    }

    private Observable<List<StationRecord>> recommendedStations() {
        return syncOperations
                .lazySyncIfStale(Syncable.RECOMMENDED_STATIONS)
                .flatMap(continueWith(getCollection(RECOMMENDATIONS)))
                .zipWith(getCollection(RECENT), MOVE_RECENT_TO_END)
                .subscribeOn(scheduler);
    }

    private Observable<List<StationRecord>> getCollection(int collectionType) {
        return stationsStorage
                .getStationsCollection(collectionType)
                .toList()
                .subscribeOn(scheduler);
    }

}
