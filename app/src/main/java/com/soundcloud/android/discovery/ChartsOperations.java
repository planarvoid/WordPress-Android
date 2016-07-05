package com.soundcloud.android.discovery;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.api.model.PagedCollection.pagingFunction;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
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

    private Action1<ApiChart> storeTracksFromChart = new Action1<ApiChart>() {
        @Override
        public void call(ApiChart apiChart) {
            storeTracksCommand.toAction1().call(apiChart.tracks());
        }
    };

    private Action1<ModelCollection<ApiChart>> storeTracksFromCharts = new Action1<ModelCollection<ApiChart>>() {
        @Override
        public void call(ModelCollection<ApiChart> charts) {
            for (final ApiChart chart : charts) {
                storeTracksFromChart.call(chart);
            }
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
    private final StoreTracksCommand storeTracksCommand;
    private final ChartsStorage chartsStorage;
    private final ChartsApi chartsApi;
    private final Scheduler scheduler;

    @Inject
    ChartsOperations(SyncOperations syncOperations,
                     StoreChartsCommand storeChartsCommand,
                     StoreTracksCommand storeTracksCommand,
                     ChartsStorage chartsStorage,
                     ChartsApi chartsApi,
                     @Named(HIGH_PRIORITY)Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storeChartsCommand = storeChartsCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.chartsStorage = chartsStorage;
        this.chartsApi = chartsApi;
        this.scheduler = scheduler;
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

    Observable<PagedChartTracks> firstPagedTracks(ChartType type, String genre) {
        return chartsApi
                .chartTracks(type, genre)
                .doOnNext(storeTracksFromChart)
                .map(PagedChartTracks.fromApiChart(true))
                .subscribeOn(scheduler);
    }

    Pager.PagingFunction<PagedChartTracks> nextPagedTracks() {
        return  pagingFunction(
                new Command<String, Observable<PagedChartTracks>>() {
                    @Override
                    public Observable<PagedChartTracks> call(String nextPageLink) {
                        return chartsApi
                                .chartTracks(nextPageLink)
                                .doOnNext(storeTracksFromChart)
                                .map(PagedChartTracks.fromApiChart(false))
                                .subscribeOn(scheduler);
                    }
                }, scheduler);
    }

    Observable<List<ApiChart>> genresByCategory(final ChartCategory chartCategory) {
        return chartsApi.allGenres()
                        .doOnNext(storeTracksFromCharts)
                        .map(filterGenresByCategory(chartCategory))
                        .subscribeOn(scheduler);
    }

    private Func1<ModelCollection<ApiChart>, List<ApiChart>> filterGenresByCategory(final ChartCategory chartCategory) {
        return new Func1<ModelCollection<ApiChart>, List<ApiChart>>() {
            @Override
            public List<ApiChart> call(ModelCollection<ApiChart> apiCharts) {
                final List<ApiChart> filteredGenres = new ArrayList<>();
                for (final ApiChart genre : apiCharts) {
                    if (genre.category() == chartCategory) {
                        filteredGenres.add(genre);
                    }
                }
                return filteredGenres;
            }
        };
    }

    void clearData() {
        storeChartsCommand.clearTables();
    }
}

