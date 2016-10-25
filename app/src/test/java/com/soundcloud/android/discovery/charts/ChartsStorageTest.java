package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.discovery.charts.ChartsFixtures.createChartWithImageResources;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.storage.Tables.Charts;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.List;

public class ChartsStorageTest extends StorageIntegrationTest {

    private final TestSubscriber<ChartBucket> subscriber = new TestSubscriber<>();
    private final TestSubscriber<List<Chart>> genresSubscriber = new TestSubscriber<>();

    private ChartsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new ChartsStorage(propellerRx());
    }

    @Test
    public void returnsChartsWithTracksFromDb() {
        final Chart sourceChart1 = testFixtures().insertChart(createChartWithImageResources(ChartType.TOP,
                                                                                            ChartCategory.MUSIC),
                                                              Charts.BUCKET_TYPE_GLOBAL);
        final Chart sourceChart2 = testFixtures().insertChart(createChartWithImageResources(ChartType.TRENDING,
                                                                                            ChartCategory.MUSIC),
                                                              Charts.BUCKET_TYPE_GLOBAL);

        storage.featuredCharts().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final ChartBucket firstEvent = subscriber.getOnNextEvents().get(0);
        final Chart actualChart1 = firstEvent.getGlobal().get(0);
        final Chart actualChart2 = firstEvent.getGlobal().get(1);

        assertThat(actualChart1.type()).isEqualTo(sourceChart1.type());
        assertThat(actualChart2.type()).isEqualTo(sourceChart2.type());
    }

    @Test
    public void returnsGenresWithTracksFromDb() {
        //Excluded chart
        final ChartCategory chartCategory = ChartCategory.MUSIC;
        testFixtures().insertChart(createChartWithImageResources(ChartType.TOP, chartCategory),
                                   Charts.BUCKET_TYPE_GLOBAL);
        final Chart includedChart = testFixtures().insertChart(createChartWithImageResources(ChartType.TRENDING,
                                                                                             chartCategory),
                                                               Charts.BUCKET_TYPE_ALL_GENRES);

        storage.genres(chartCategory).subscribe(genresSubscriber);

        genresSubscriber.assertValueCount(1);
        final List<Chart> result  = genresSubscriber.getOnNextEvents().get(0);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).type()).isEqualTo(includedChart.type());
    }



    @Test
    public void chartsEmptyWhenNoValueInDb() {
        storage.featuredCharts().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final ChartBucket charts = subscriber.getOnNextEvents().get(0);
        assertThat(charts.getGlobal()).isEmpty();
        assertThat(charts.getFeaturedGenres()).isEmpty();
    }
}
