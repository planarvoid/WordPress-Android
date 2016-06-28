package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ChartsOperationsTest extends AndroidUnitTest {
    private final PublishSubject<Result> syncSubject = PublishSubject.create();
    private final TestSubscriber<ChartBucket> subscriber = new TestSubscriber<>();
    private final TestSubscriber<PagedChartTracks> chartTrackSubscriber = new TestSubscriber<>();

    private ChartsOperations operations;

    @Mock private SyncOperations syncOperations;
    @Mock private StoreChartsCommand storeChartsCommand;
    @Mock private ChartsStorage chartsStorage;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private ChartsApi chartsApi;

    @Before
    public void setUp() {
        this.operations = new ChartsOperations(syncOperations, storeChartsCommand,
                                               storeTracksCommand,
                                               chartsStorage,
                                               chartsApi, Schedulers.immediate());
        when(syncOperations.lazySyncIfStale(Syncable.CHARTS)).thenReturn(syncSubject);
    }

    @Test
    public void lazySyncAndLoadFromStorage() {
        initChartsForModule();

        operations.charts().subscribe(subscriber);
        subscriber.assertNoValues();

        syncSubject.onNext(Result.SYNCING);

        subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);
    }

    @Test
    public void returnsDiscoveryItemWithHotAndNewAndTopFiftyChartsAndGenres() {
        final ChartBucket charts = initChartsForModule();

        operations.charts().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        subscriber.assertValueCount(1);
        final ChartBucket chartsItem = subscriber.getOnNextEvents().get(0);

        assertThat(chartsItem.getGlobal()).isEqualTo(charts.getGlobal());
        assertThat(chartsItem.getFeaturedGenres()).isEqualTo(charts.getFeaturedGenres());
    }

    @Test
    public void absentDiscoveryItemWithoutNewAndHot() {
        final Chart topFiftyChart = createTopFiftyChart();
        initChartsWithTracks(topFiftyChart);

        operations.charts().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        subscriber.assertNoValues();
    }

    @Test
    public void absentDiscoveryItemWithoutTopFifty() {
        final Chart hotAndNewChart = createHotAndNewChart();
        initChartsWithTracks(hotAndNewChart);

        operations.charts().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        subscriber.assertNoValues();
    }

    @Test
    public void returnsChartsFirstPageFromApi() {
        final ChartType type = ChartType.TOP;
        final String genre = "all-music";
        final String nextPageLink = "http://link";
        final ApiChart apiChart = createApiChart(type, genre, Optional.of(nextPageLink));
        when(chartsApi.chartTracks(type, genre)).thenReturn(Observable.just(apiChart));

        operations.firstPagedTracks(type, genre).subscribe(chartTrackSubscriber);

        chartTrackSubscriber.assertValueCount(1);
        final PagedChartTracks chartTrackItems = chartTrackSubscriber.getOnNextEvents().get(0);

        assertThat(chartTrackItems.items().getNextLink().get().getHref()).isEqualTo(nextPageLink);

        assertThat(chartTrackItems.firstPage()).isTrue();
        assertThat((chartTrackItems.items().getCollection().get(0)).getUrn()).isEqualTo(apiChart.tracks()
                                                                                                .getCollection()
                                                                                                .get(0)
                                                                                                .getUrn());
    }

    @Test
    public void returnsChartsNextAndLastPageFromApi() {
        final ChartType type = ChartType.TOP;
        final String genre = "all-music";
        final String nextPageLink = "http://link";
        final ApiChart apiChart = createApiChart(type, genre, Optional.of(nextPageLink));
        final ApiChart nextChart = createApiChart(type, genre, Optional.<String>absent());
        final PagedChartTracks page1 = new PagedChartTracks(false, apiChart);
        when(chartsApi.chartTracks(nextPageLink)).thenReturn(Observable.just(nextChart));

        operations.nextPagedTracks().call(page1).subscribe(chartTrackSubscriber);

        chartTrackSubscriber.assertValueCount(1);
        final PagedChartTracks chartTrackItems = chartTrackSubscriber.getOnNextEvents().get(0);

        assertThat(chartTrackItems.items().getNextLink()).isEqualTo(Optional.absent());

        assertThat(chartTrackItems.lastPage()).isTrue();

        assertThat((chartTrackItems.items().getCollection().get(0)).getUrn()).isEqualTo(nextChart.tracks()
                                                                                                 .getCollection()
                                                                                                 .get(0)
                                                                                                 .getUrn());
    }

    private ChartBucket initChartsForModule() {
        final ChartBucket chartBucket = ChartBucket.create(
                Arrays.asList(createHotAndNewChart(), createTopFiftyChart()),
                Arrays.asList(createGenreChart(3L), createGenreChart(4L), createGenreChart(5L))
        );
        initChartsWithTracks(chartBucket);
        return chartBucket;
    }

    private ChartBucket initChartsWithTracks(Chart chart) {
        final ChartBucket chartBucket = ChartBucket.create(
                Collections.singletonList(chart),
                Collections.<Chart>emptyList()
        );
        initChartsWithTracks(chartBucket);
        return chartBucket;
    }

    private Chart createHotAndNewChart() {
        return createChart(1L, ChartType.TRENDING, ChartCategory.NONE, ChartBucketType.GLOBAL);
    }

    private Chart createTopFiftyChart() {
        return createChart(2L, ChartType.TOP, ChartCategory.NONE, ChartBucketType.GLOBAL);
    }

    private Chart createGenreChart(long localId) {
        return createChart(localId, ChartType.TRENDING, ChartCategory.MUSIC, ChartBucketType.FEATURED_GENRES);
    }

    private Chart createChart(long localId, ChartType trending, ChartCategory none, ChartBucketType chartBucketType) {
        final ChartTrack chartTrack = ChartTrack.create(Urn.forTrack(localId), Optional.<String>absent());

        return Chart.create(localId,
                            trending,
                            none,
                            "title",
                            new Urn("soundcloud:chart"),
                            chartBucketType,
                            Collections.singletonList(chartTrack));
    }

    private ApiChart createApiChart(ChartType type, String genre, Optional<String> nextPageLink) {
        ApiTrack chartTrack = ModelFixtures.create(ApiTrack.class);
        Map<String, Link> links = new HashMap<>();
        if (nextPageLink.isPresent()) {
            links.put(ModelCollection.NEXT_LINK_REL, new Link(nextPageLink.get()));
        }
        return new ApiChart("title",
                            new Urn("soundcloud:chart:"+genre),
                            type,
                            ChartCategory.MUSIC,
                            12345L,
                            new ModelCollection<>(Collections.singletonList(chartTrack), links));
    }

    private void initChartsWithTracks(ChartBucket charts) {
        when(chartsStorage.charts()).thenReturn(Observable.just(charts));
    }
}
