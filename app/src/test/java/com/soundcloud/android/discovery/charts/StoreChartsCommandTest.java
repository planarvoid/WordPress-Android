package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.discovery.charts.ChartsFixtures.createChartWithImageResources;
import static com.soundcloud.android.discovery.charts.ChartsFixtures.createChartWithImageResourcesWithTrackUrn;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiChartBucket;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

public class StoreChartsCommandTest extends StorageIntegrationTest {

    private StoreChartsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new StoreChartsCommand(propeller());
    }

    @Test
    public void shouldStoreChart() {
        final ApiChart<ApiImageResource> apiChart = createChartWithImageResources(ChartType.TOP, ChartCategory.AUDIO);

        final ApiChartBucket apiChartBucket = new ApiChartBucket(singletonList(apiChart),
                                                                 Tables.Charts.BUCKET_TYPE_GLOBAL);
        command.call(singletonList(apiChartBucket));

        databaseAssertions().assertChartInserted(apiChartBucket);
    }

    @Test
    public void shouldDeleteOldChart() {
        final ApiChart<ApiImageResource> oldChart = createChartWithImageResourcesWithTrackUrn(ChartType.TOP,
                                                                                              ChartCategory.AUDIO,
                                                                                              Urn.forTrack(1L));
        final ApiChart<ApiImageResource> newChart = createChartWithImageResourcesWithTrackUrn(ChartType.TRENDING,
                                                                                              ChartCategory.MUSIC,
                                                                                              Urn.forTrack(1L));

        final ApiChartBucket oldChartBucket = new ApiChartBucket(singletonList(oldChart),
                                                                 Tables.Charts.BUCKET_TYPE_GLOBAL);
        command.call(singletonList(oldChartBucket));

        final ApiChartBucket newChartBucket = new ApiChartBucket(singletonList(newChart),
                                                                 Tables.Charts.BUCKET_TYPE_FEATURED_GENRES);
        command.call(singletonList(newChartBucket));

        databaseAssertions().assertChartRemoved(oldChartBucket);
        databaseAssertions().assertChartInserted(newChartBucket);
    }
}
