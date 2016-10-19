package com.soundcloud.android.discovery;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class ChartsOperations {

    private final Func1<ChartBucket, Boolean> HAS_EXPECTED_CONTENT = new Func1<ChartBucket, Boolean>() {
        @Override
        public Boolean call(ChartBucket chartBucket) {
            final Optional<Chart> trending = Iterables.tryFind(chartBucket.getGlobal(), filterType(ChartType.TRENDING));
            final Optional<Chart> top = Iterables.tryFind(chartBucket.getGlobal(), filterType(ChartType.TOP));

            return trending.isPresent() && top.isPresent() && chartBucket.getFeaturedGenres().size() >= 3;
        }
    };
    private final SyncOperations syncOperations;
    private final StoreChartsCommand storeChartsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final ChartsStorage chartsStorage;
    private final ChartsApi chartsApi;
    private final Scheduler scheduler;
    private final Action1<ApiChart> storeTracksFromChart = new Action1<ApiChart>() {
        @Override
        public void call(ApiChart apiChart) {
            storeTracksCommand.toAction1().call(apiChart.tracks());
        }
    };
    private final Func1<SyncOperations.Result, Observable<DiscoveryItem>> loadCharts = new Func1<SyncOperations.Result, Observable<DiscoveryItem>>() {
        @Override
        public Observable<DiscoveryItem> call(SyncOperations.Result result) {
            return chartsStorage.featuredCharts()
                                .filter(HAS_EXPECTED_CONTENT)
                                .subscribeOn(scheduler)
                                .switchIfEmpty(SyncOperations.<ChartBucket>emptyResult(result))
                                .map(toDiscoveryItem());
        }
    };

    @Inject
    ChartsOperations(SyncOperations syncOperations,
                     StoreChartsCommand storeChartsCommand,
                     StoreTracksCommand storeTracksCommand,
                     ChartsStorage chartsStorage,
                     ChartsApi chartsApi,
                     @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storeChartsCommand = storeChartsCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.chartsStorage = chartsStorage;
        this.chartsApi = chartsApi;
        this.scheduler = scheduler;
    }

    private Predicate<Chart> filterType(final ChartType type) {
        return new Predicate<Chart>() {
            @Override
            public boolean apply(Chart chart) {
                return chart.type() == type;
            }
        };
    }

    private Observable<DiscoveryItem> load(Observable<SyncOperations.Result> source) {
        return source.flatMap(loadCharts);
    }

    private Func1<ChartBucket, DiscoveryItem> toDiscoveryItem() {
        return new Func1<ChartBucket, DiscoveryItem>() {
            @Override
            public DiscoveryItem call(ChartBucket chartBucket) {
                return ChartsBucketItem.from(chartBucket);
            }
        };
    }

    private Func1<List<Chart>, List<Chart>> filterGenresByCategory(final ChartCategory chartCategory) {
        return new Func1<List<Chart>, List<Chart>>() {
            @Override
            public List<Chart> call(List<Chart> apiCharts) {
                List<Chart> filteredGenres = new ArrayList<>();
                for (Chart genre : apiCharts) {
                    if (genre.category() == chartCategory) {
                        filteredGenres.add(genre);
                    }
                }
                return filteredGenres;
            }
        };
    }

    Observable<DiscoveryItem> featuredCharts() {
        return load(syncOperations.lazySyncIfStale(Syncable.CHARTS));
    }

    Observable<DiscoveryItem> refreshFeaturedCharts() {
        return load(syncOperations.failSafeSync(Syncable.CHARTS));
    }

    Observable<ApiChart<ApiTrack>> tracks(ChartType type, String genre) {
        return chartsApi.chartTracks(type, genre)
                        .doOnNext(storeTracksFromChart)
                        .subscribeOn(scheduler);
    }

    Observable<List<Chart>> genresByCategory(ChartCategory chartCategory) {
        return syncOperations.lazySyncIfStale(Syncable.CHART_GENRES)
                             .flatMap(continueWith(chartsStorage.genres(chartCategory)
                                                                .subscribeOn(scheduler)))
                             .map(filterGenresByCategory(chartCategory));
    }

    void clearData() {
        storeChartsCommand.clearTables();
    }
}

