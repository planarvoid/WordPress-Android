package com.soundcloud.android.stations;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.java.collections.Lists;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                    return new RecommendedStationsItem(stationRecords);
                }
            };
    private static final long SYNC_THRESHOLD = TimeUnit.DAYS.toMillis(1);

    private final Action1<List<StationRecord>> SYNC_IF_NEEDED = new Action1<List<StationRecord>>() {
        @Override
        public void call(List<StationRecord> stationRecords) {
            if (needsRefresh()) {
                refreshRecommendedStations();
            }
        }
    };
    private final StationsStorage stationsStorage;
    private final FeatureFlags featureFlags;
    private final Scheduler scheduler;
    private final SyncStateStorage syncStateStorage;
    private final StationsSyncInitiator syncInitiator;

    @Inject
    RecommendedStationsOperations(StationsStorage stationsStorage,
                                  FeatureFlags featureFlags,
                                  @Named(HIGH_PRIORITY) Scheduler scheduler,
                                  SyncStateStorage syncStateStorage,
                                  StationsSyncInitiator syncInitiator) {
        this.stationsStorage = stationsStorage;
        this.featureFlags = featureFlags;
        this.scheduler = scheduler;
        this.syncStateStorage = syncStateStorage;
        this.syncInitiator = syncInitiator;
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
        final Observable<List<StationRecord>> source;

        if (hasSyncedBefore()) {
            source = getCollection(RECOMMENDATIONS);
        } else {
            source = stationsRefresh();
        }

        return source
                .zipWith(getCollection(RECENT), MOVE_RECENT_TO_END)
                .doOnNext(SYNC_IF_NEEDED)
                .subscribeOn(scheduler);
    }

    private boolean hasSyncedBefore() {
        return syncStateStorage.hasSyncedBefore(StationsSyncInitiator.RECOMMENDATIONS);
    }

    private boolean needsRefresh() {
        return !syncStateStorage.hasSyncedWithin(StationsSyncInitiator.RECOMMENDATIONS, SYNC_THRESHOLD);
    }

    private Subscription refreshRecommendedStations() {
        return fireAndForget(syncInitiator.syncRecommendedStations());
    }

    private Observable<List<StationRecord>> stationsRefresh() {
        return syncInitiator.syncRecommendedStations()
                .flatMap(continueWith(getCollection(RECOMMENDATIONS)));
    }

    private Observable<List<StationRecord>> getCollection(int collectionType) {
        return stationsStorage
                .getStationsCollection(collectionType)
                .toList()
                .subscribeOn(scheduler);
    }

}
