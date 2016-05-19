package com.soundcloud.android.stations;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class RecommendedStationsOperations {

    private static final Func1<ModelCollection<ApiStationMetadata>, List<StationRecord>> TO_STATION_RECORD =
            new Func1<ModelCollection<ApiStationMetadata>, List<StationRecord>>() {
                @Override
                public List<StationRecord> call(ModelCollection<ApiStationMetadata> apiStationMetadatas) {
                    return new ArrayList<>(transform(apiStationMetadatas.getCollection(), new Function<ApiStationMetadata, StationRecord>() {
                        @Override
                        public StationRecord apply(ApiStationMetadata input) {
                            return Station.from(input);
                        }
                    }));
                }
            };

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
                    return new RecommendedStationsBucket(stationRecords);
                }
            };

    private final StationsStorage stationsStorage;
    private final StationsApi stationsApi;
    private final WriteStationsRecommendationsCommand writeCommand;
    private final FeatureFlags featureFlags;
    private final Scheduler scheduler;

    @Inject
    public RecommendedStationsOperations(StationsStorage stationsStorage,
                                         StationsApi stationsApi,
                                         WriteStationsRecommendationsCommand writeCommand,
                                         FeatureFlags featureFlags,
                                         @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.stationsStorage = stationsStorage;
        this.stationsApi = stationsApi;
        this.writeCommand = writeCommand;
        this.featureFlags = featureFlags;
        this.scheduler = scheduler;
    }

    public Observable<DiscoveryItem> stationsBucket() {
        if (featureFlags.isEnabled(Flag.RECOMMENDED_STATIONS)) {
            return recommendedStations()
                    .filter(IS_NOT_EMPTY_LIST)
                    .map(TO_RECOMMENDED_STATIONS_BUCKET);
        } else {
            return Observable.empty();
        }
    }

    public void clearData() {
        stationsStorage.clear();
    }

    private Observable<List<StationRecord>> recommendedStations() {
        return storedRecommendedStations()
                .flatMap(loadIfEmpty())
                .zipWith(recentStations(), MOVE_RECENT_TO_END)
                .subscribeOn(scheduler);
    }

    private Func1<List<StationRecord>, Observable<List<StationRecord>>> loadIfEmpty() {
        return new Func1<List<StationRecord>, Observable<List<StationRecord>>>() {
            @Override
            public Observable<List<StationRecord>> call(List<StationRecord> stations) {
                return stations.isEmpty()
                        ? stationsRefresh()
                        : Observable.just(stations);
            }
        };
    }

    private Observable<List<StationRecord>> stationsRefresh() {
        return stationsApi
                .fetchStationRecommendations()
                .map(TO_STATION_RECORD)
                .doOnNext(writeCommand.toAction1())
                .subscribeOn(scheduler);
    }

    private Observable<List<StationRecord>> storedRecommendedStations() {
        return stationsStorage
                .getStationsCollection(StationsCollectionsTypes.SUGGESTIONS)
                .toList();
    }

    private Observable<List<StationRecord>> recentStations() {
        return stationsStorage
                .getStationsCollection(StationsCollectionsTypes.RECENT)
                .toList();
    }

}
