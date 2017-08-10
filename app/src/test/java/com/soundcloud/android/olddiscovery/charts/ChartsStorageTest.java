package com.soundcloud.android.olddiscovery.charts;

import static com.soundcloud.android.olddiscovery.charts.ChartsFixtures.createChartWithImageResources;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.storage.Tables.Charts;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ChartsStorageTest extends StorageIntegrationTest {

    private final TestObserver<ChartBucket> observer = new TestObserver<>();
    private final TestObserver<List<Chart>> genresObserver = new TestObserver<>();

    private ChartsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new ChartsStorage(propellerRxV2());
    }

    @Test
    public void returnsChartsWithTracksFromDb() {
        final Chart sourceChart1 = testFixtures().insertChart(createChartWithImageResources(ChartType.TOP,
                                                                                            ChartCategory.MUSIC),
                                                              Charts.BUCKET_TYPE_GLOBAL);
        final Chart sourceChart2 = testFixtures().insertChart(createChartWithImageResources(ChartType.TRENDING,
                                                                                            ChartCategory.MUSIC),
                                                              Charts.BUCKET_TYPE_GLOBAL);

        storage.featuredCharts().subscribe(observer);

        observer.assertValueCount(1);
        final ChartBucket firstEvent = observer.values().get(0);
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

        storage.genres(chartCategory).subscribe(genresObserver);

        genresObserver.assertValueCount(1);
        final List<Chart> result  = genresObserver.values().get(0);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).type()).isEqualTo(includedChart.type());
    }



    @Test
    public void chartsEmptyWhenNoValueInDb() {
        storage.featuredCharts().subscribe(observer);

        observer.assertValueCount(1);
        final ChartBucket charts = observer.values().get(0);
        assertThat(charts.getGlobal()).isEmpty();
        assertThat(charts.getFeaturedGenres()).isEmpty();
    }
}
