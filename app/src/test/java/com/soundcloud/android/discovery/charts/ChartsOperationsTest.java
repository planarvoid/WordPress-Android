package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackArtwork;
import com.soundcloud.java.optional.Optional;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ChartsOperationsTest extends AndroidUnitTest {
    private static final String GENRE = "all-music";
    private static final ChartType TYPE = ChartType.TOP;

    private final PublishSubject<Result> syncSubject = PublishSubject.create();
    private final TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();
    private final TestSubscriber<ApiChart<ApiTrack>> chartTrackSubscriber = new TestSubscriber<>();
    private final TestSubscriber<List<Chart>> genresSubscriber = new TestSubscriber<>();
    private final Chart musicChart = createGenreChart(1L, ChartCategory.MUSIC);
    private final Chart audioChart = createGenreChart(2L, ChartCategory.AUDIO);

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
        when(syncOperations.lazySyncIfStale(Syncable.CHART_GENRES)).thenReturn(syncSubject);
    }

    @Test
    public void lazySyncAndLoadFromStorage() {
        initChartsForModule();

        operations.featuredCharts().subscribe(subscriber);
        subscriber.assertNoValues();

        syncSubject.onNext(Result.SYNCING);

        subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);
    }

    @Test
    public void returnsDiscoveryItemWithHotAndNewAndTopFiftyChartsAndGenres() {
        initChartsForModule();

        operations.featuredCharts().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        subscriber.assertValueCount(1);
        final DiscoveryItem discoveryItem = subscriber.getOnNextEvents().get(0);

        assertThat(discoveryItem.getKind()).isEqualTo(ChartItem);
    }

    @Test
    public void absentDiscoveryItemWithoutNewAndHot() {
        final Chart topFiftyChart = createTopFiftyChart();
        initChartsWithTracks(topFiftyChart);

        operations.featuredCharts().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        subscriber.assertNoValues();
    }

    @Test
    public void absentDiscoveryItemWithoutTopFifty() {
        final Chart hotAndNewChart = createHotAndNewChart();
        initChartsWithTracks(hotAndNewChart);

        operations.featuredCharts().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        subscriber.assertNoValues();
    }

    @Test
    public void returnsApiChartFromApi() {
        final ApiChart<ApiTrack> apiChart = ChartsFixtures.createApiChart(GENRE, TYPE);
        when(chartsApi.chartTracks(TYPE, GENRE)).thenReturn(Observable.just(apiChart));

        operations.tracks(TYPE, GENRE).subscribe(chartTrackSubscriber);

        chartTrackSubscriber.assertValueCount(1);
        final ApiChart<ApiTrack> chartTrackItems = chartTrackSubscriber.getOnNextEvents().get(0);

        assertThat((chartTrackItems.tracks().getCollection().get(0)).getUrn()).isEqualTo(apiChart.tracks()
                                                                                                 .getCollection()
                                                                                                 .get(0)
                                                                                                 .getUrn());
        verify(storeTracksCommand).toAction1().call(apiChart.tracks());
    }

    @Test
    public void returnsFilteredGenresFromStorage() {
        final ChartCategory chartCategory = ChartCategory.MUSIC;
        initGenresStorage(chartCategory);

        operations.genresByCategory(chartCategory).subscribe(genresSubscriber);

        genresSubscriber.assertNoValues();

        syncSubject.onNext(Result.SYNCING);

        assertThat(genresSubscriber.getOnNextEvents().get(0)).containsExactly(musicChart);
    }

    private void initGenresStorage(ChartCategory chartCategory) {
        final List<Chart> genres = Lists.newArrayList(musicChart, audioChart);
        when(chartsStorage.genres(chartCategory)).thenReturn(Observable.just(genres));
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
                Collections.emptyList()
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
        return createGenreChart(localId, ChartCategory.MUSIC);
    }

    private Chart createGenreChart(long localId, ChartCategory category) {
        return createChart(localId, ChartType.TRENDING, category, ChartBucketType.FEATURED_GENRES);
    }

    private Chart createChart(long localId, ChartType trending, ChartCategory none, ChartBucketType chartBucketType) {
        final TrackArtwork trackArtwork = TrackArtwork.create(Urn.forTrack(localId), Optional.absent());

        return Chart.create(localId,
                            trending,
                            none,
                            "title",
                            new Urn("soundcloud:chart"),
                            chartBucketType,
                            Collections.singletonList(trackArtwork));
    }

    private void initChartsWithTracks(ChartBucket charts) {
        when(chartsStorage.featuredCharts()).thenReturn(Observable.just(charts));
    }
}
