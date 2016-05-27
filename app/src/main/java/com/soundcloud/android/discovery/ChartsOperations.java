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
import java.util.List;

class ChartsOperations {

    private final static Func1<List<Chart>, Observable<DiscoveryItem>> CHARTS_TO_DISCOVERY_ITEM = new Func1<List<Chart>, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(List<Chart> charts) {
            final Optional<Chart> newAndHot = Iterables.tryFind(charts, global(ChartType.TRENDING));
            final Optional<Chart> topFifty = Iterables.tryFind(charts, global(ChartType.TOP));

            if (newAndHot.isPresent() && topFifty.isPresent()) {
                return Observable.<DiscoveryItem>just(new ChartsItem(newAndHot.get(), topFifty.get()));
            } else {
                return Observable.empty();
            }
        }
    };

    private static Predicate<Chart> global(final ChartType chartType) {
        return new Predicate<Chart>() {
            @Override
            public boolean apply(Chart input) {
                return input != null && !input.genre().isPresent() && input.type() == chartType;
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

    Observable<DiscoveryItem> charts() {
        if (featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)) {
            return chartsSyncInitiator.syncCharts().flatMap(continueWith(loadCharts()));
        } else {
            return Observable.empty();
        }
    }

    void clearData() {
        storeChartsCommand.clearTables();
        chartsSyncInitiator.clearLastSyncTime();
    }

    private Observable<DiscoveryItem> loadCharts() {
        return chartsStorage.charts().flatMap(CHARTS_TO_DISCOVERY_ITEM);
    }
}

