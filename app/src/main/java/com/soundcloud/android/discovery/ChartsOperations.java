package com.soundcloud.android.discovery;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.charts.StoreChartsCommand;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

class ChartsOperations {
    private final Func1<Chart, Observable<Chart>> ADD_TRACKS_TO_CHART = new Func1<Chart, Observable<Chart>>() {
        @Override
        public Observable<Chart> call(final Chart chart) {
            return chartsStorage.chartTracks(chart.getLocalId())
                                .map(PROPERTY_SET_TO_CHART_TRACK)
                                .toList()
                                .map(new Func1<List<ChartTrack>, Chart>() {
                                    @Override
                                    public Chart call(List<ChartTrack> chartTracks) {
                                        chart.setChartTracks(chartTracks);
                                        return chart;
                                    }
                                });
        }
    };

    private static final Func1<PropertySet, Chart> PROPERTY_SET_TO_CHART = new Func1<PropertySet, Chart>() {
        @Override
        public Chart call(PropertySet propertyBindings) {
            return Chart.fromPropertySet(propertyBindings);
        }
    };

    private final static Func1<PropertySet, ChartTrack> PROPERTY_SET_TO_CHART_TRACK = new Func1<PropertySet, ChartTrack>() {
        @Override
        public ChartTrack call(PropertySet propertyBindings) {
            return ChartTrack.fromPropertySet(propertyBindings);
        }
    };

    private final static Func1<List<Chart>, DiscoveryItem> CHART_LIST_TO_DISCOVERY_ITEM = new Func1<List<Chart>, DiscoveryItem>() {
        @Override
        public DiscoveryItem call(List<Chart> charts) {
            final Chart newAndHot = Iterables.find(charts, type(ChartCategory.NONE, ChartType.TRENDING));
            final Chart topFifty = Iterables.find(charts, type(ChartCategory.NONE, ChartType.TOP));
            return new ChartsItem(newAndHot, topFifty);
        }
    };

    @NonNull
    private static Predicate<Chart> type(final ChartCategory chartCategory, final ChartType chartType) {
        return new Predicate<Chart>() {
            @Override
            public boolean apply(@Nullable Chart input) {
                return input != null && input.getCategory() == chartCategory && input.getType() == chartType;
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
                     ChartsStorage chartsStorage, FeatureFlags featureFlags) {
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
        return chartsStorage.charts()
                            .map(PROPERTY_SET_TO_CHART)
                            .flatMap(ADD_TRACKS_TO_CHART)
                            .toList()
                            .map(CHART_LIST_TO_DISCOVERY_ITEM);
    }
}

