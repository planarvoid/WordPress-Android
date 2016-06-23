package com.soundcloud.android.discovery;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;

class ChartsOperations {

    private final Func1<ChartBucket, Boolean> HAS_EXPECTED_CONTENT = new Func1<ChartBucket, Boolean>() {
        @Override
        public Boolean call(ChartBucket chartBucket) {
            final Optional<Chart> trending = Iterables.tryFind(chartBucket.getGlobal(), filterType(ChartType.TRENDING));
            final Optional<Chart> top = Iterables.tryFind(chartBucket.getGlobal(), filterType(ChartType.TOP));

            return trending.isPresent() && top.isPresent() && chartBucket.getFeaturedGenres().size() >= 3;
        }
    };

    private Predicate<Chart> filterType(final ChartType type) {
        return new Predicate<Chart>() {
            @Override
            public boolean apply(Chart chart) {
                return chart.type() == type;
            }
        };
    }

    private final SyncOperations syncOperations;
    private final StoreChartsCommand storeChartsCommand;
    private final ChartsStorage chartsStorage;

    @Inject
    ChartsOperations(SyncOperations syncOperations,
                     StoreChartsCommand storeChartsCommand,
                     ChartsStorage chartsStorage) {
        this.syncOperations = syncOperations;
        this.storeChartsCommand = storeChartsCommand;
        this.chartsStorage = chartsStorage;
    }

    Observable<ChartBucket> charts() {
        return load(syncOperations.lazySyncIfStale(Syncable.CHARTS));
    }

    Observable<ChartBucket> refreshCharts() {
        return load(syncOperations.sync(Syncable.CHARTS));
    }

    private Observable<ChartBucket> load(Observable<SyncOperations.Result> source) {
        return source
                .flatMap(continueWith(chartsStorage.charts()))
                .filter(HAS_EXPECTED_CONTENT);
    }

    void clearData() {
        storeChartsCommand.clearTables();
    }

}

