package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChartsStorageTest extends StorageIntegrationTest {

    private final TestSubscriber<List<Chart>> subscriber = new TestSubscriber<>();

    private ChartsStorage storage;
    private Scheduler scheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        storage = new ChartsStorage(propellerRx(), scheduler);
    }

    @Test
    public void returnsChartsWithTracksFromDb() {
        final Chart sourceChart1 = testFixtures().insertChart(createChart(3, ChartType.TOP));
        final Chart sourceChart2 = testFixtures().insertChart(createChart(3, ChartType.TRENDING));

        storage.charts().subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(Arrays.asList(sourceChart1, sourceChart2)));
    }

    @Test
    public void chartsEmptyWhenNoValueInDb() {
        storage.charts().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final List<Chart> charts = subscriber.getOnNextEvents().get(0);
        assertThat(charts.size()).isEqualTo(0);
    }

    @NonNull
    private ApiChart createChart(int countOfTracks, ChartType type) {
        final ModelCollection<ApiTrack> chartTracks = new ModelCollection<>(ModelFixtures.create(ApiTrack.class,
                                                                                                 countOfTracks));
        return new ApiChart("page", "title", null, type, ChartCategory.NONE, chartTracks);
    }
}
