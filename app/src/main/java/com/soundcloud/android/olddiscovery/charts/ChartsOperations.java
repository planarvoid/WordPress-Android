package com.soundcloud.android.olddiscovery.charts;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class ChartsOperations {

    private final Predicate<ChartBucket> hasExpectedContent = chartBucket -> {
        final Optional<Chart> trending = Iterables.tryFind(chartBucket.getGlobal(), filterType(ChartType.TRENDING));
        final Optional<Chart> top = Iterables.tryFind(chartBucket.getGlobal(), filterType(ChartType.TOP));

        return trending.isPresent() && top.isPresent() && chartBucket.getFeaturedGenres().size() >= 3;
    };

    private final Function<SyncResult, Single<OldDiscoveryItem>> loadCharts =
            new Function<SyncResult, Single<OldDiscoveryItem>>() {
                @Override
                public Single<OldDiscoveryItem> apply(@NonNull SyncResult result) throws Exception {
                    return chartsStorage.featuredCharts()
                                        .filter(hasExpectedContent)
                                        .subscribeOn(scheduler)
                                        .switchIfEmpty(NewSyncOperations.emptyResult(result))
                                        .toSingle()
                                        .map(ChartsBucketItem::from);
                }
            };

    private final Consumer<ApiChart> storeTracksFromChart = new Consumer<ApiChart>() {
        @Override
        public void accept(@NonNull ApiChart apiChart) throws Exception {
            storeTracksCommand.call(apiChart.tracks());
        }
    };

    private final NewSyncOperations syncOperations;
    private final StoreChartsCommand storeChartsCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final ChartsStorage chartsStorage;
    private final ChartsApi chartsApi;
    private final Scheduler scheduler;

    @Inject
    ChartsOperations(NewSyncOperations syncOperations,
                     StoreChartsCommand storeChartsCommand,
                     StoreTracksCommand storeTracksCommand,
                     ChartsStorage chartsStorage,
                     ChartsApi chartsApi,
                     @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.syncOperations = syncOperations;
        this.storeChartsCommand = storeChartsCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.chartsStorage = chartsStorage;
        this.chartsApi = chartsApi;
        this.scheduler = scheduler;
    }

    private com.soundcloud.java.functions.Predicate<Chart> filterType(final ChartType type) {
        return chart -> chart.type() == type;
    }

    private Single<OldDiscoveryItem> load(Single<SyncResult> source) {
        return source.flatMap(loadCharts);
    }

    private Function<List<Chart>, List<Chart>> filterGenresByCategory(final ChartCategory chartCategory) {
        return apiCharts -> {
            List<Chart> filteredGenres = new ArrayList<>();
            for (Chart genre : apiCharts) {
                if (genre.category() == chartCategory) {
                    filteredGenres.add(genre);
                }
            }
            return filteredGenres;
        };
    }

    public Single<OldDiscoveryItem> featuredCharts() {
        return load(syncOperations.lazySyncIfStale(Syncable.CHARTS));
    }

    public Single<OldDiscoveryItem> refreshFeaturedCharts() {
        return load(syncOperations.sync(Syncable.CHARTS));
    }

    Observable<ApiChart<ApiTrack>> tracks(ChartType type, String genre) {
        return chartsApi.chartTracks(type, genre)
                        .doOnNext(storeTracksFromChart)
                        .subscribeOn(scheduler);
    }

    Single<List<Chart>> genresByCategory(ChartCategory chartCategory) {
        return syncOperations.lazySyncIfStale(Syncable.CHART_GENRES)
                             .flatMap(o -> chartsStorage.genres(chartCategory)
                                                        .subscribeOn(scheduler))
                             .map(filterGenresByCategory(chartCategory));
    }

    public void clearData() {
        storeChartsCommand.clearTables();
    }
}

