package com.soundcloud.android.discovery;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.charts.StoreChartsCommand;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
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

    private final ChartsSyncInitiator chartsSyncInitiator;
    private final StoreChartsCommand storeChartsCommand;
    private final ChartsStorage chartsStorage;
    private final FeatureFlags featureFlags;

    @Inject
    ChartsOperations(ChartsSyncInitiator chartsSyncInitiator,
                     StoreChartsCommand storeChartsCommand,
                     ChartsStorage chartsStorage,
                     FeatureFlags featureFlags) {
        this.chartsSyncInitiator = chartsSyncInitiator;
        this.storeChartsCommand = storeChartsCommand;
        this.chartsStorage = chartsStorage;
        this.featureFlags = featureFlags;
    }

    Observable<ChartBucket> charts() {
        if (featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)) {
            return chartsSyncInitiator
                    .syncCharts()
                    .flatMap(continueWith(chartsStorage.charts()))
                    .filter(HAS_EXPECTED_CONTENT);
        } else {
            return Observable.empty();
        }
    }

    void clearData() {
        storeChartsCommand.clearTables();
        chartsSyncInitiator.clearLastSyncTime();
    }

}

