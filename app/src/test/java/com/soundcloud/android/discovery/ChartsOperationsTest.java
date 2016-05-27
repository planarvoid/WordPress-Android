package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.sync.charts.StoreChartsCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collections;


public class ChartsOperationsTest extends AndroidUnitTest {
    private final PublishSubject<Boolean> syncSubject = PublishSubject.create();
    private final TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    private ChartsOperations operations;

    @Mock private ChartsSyncInitiator chartsSyncInitiator;
    @Mock private StoreChartsCommand storeChartsCommand;
    @Mock private ChartsStorage chartsStorage;
    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() {
        this.operations = new ChartsOperations(chartsSyncInitiator, storeChartsCommand, chartsStorage, featureFlags);
        when(featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)).thenReturn(true);
        when(chartsSyncInitiator.syncCharts()).thenReturn(syncSubject);
    }

    @Test
    public void returnsEmptyObservableWhenFeatureFlagIsDisabled() {
        when(featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)).thenReturn(false);

        operations.charts().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    @Test
    public void waitsForSyncerToReturnData() {
        final Chart hotAndNewChart = createHotAndNewChart();
        final Chart topFiftyChart = createTopFiftyChart();
        initChartsWithTracks(hotAndNewChart, topFiftyChart);

        operations.charts().subscribe(subscriber);
        subscriber.assertNoValues();

        syncSubject.onNext(true);

        subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);
    }

    @Test
    public void returnsDiscoveryItemWithHotAndNewAndTopFiftyCharts() {
        final Chart hotAndNewChart = createHotAndNewChart();
        final Chart topFiftyChart = createTopFiftyChart();
        initChartsWithTracks(hotAndNewChart, topFiftyChart);

        operations.charts().subscribe(subscriber);
        syncSubject.onNext(true);

        subscriber.assertValueCount(1);
        final DiscoveryItem chartsItem = subscriber.getOnNextEvents().get(0);

        assertThat(chartsItem).isInstanceOf(ChartsItem.class);
        assertThat(((ChartsItem) chartsItem).getNewAndHotChart().localId()).isEqualTo(hotAndNewChart.localId());
        assertThat(((ChartsItem) chartsItem).getTopFiftyChart().localId()).isEqualTo(topFiftyChart.localId());
    }

    @Test
    public void absentDiscoveryItemWithoutNewAndHot() {
        final Chart topFiftyChart = createTopFiftyChart();
        initChartsWithTracks(topFiftyChart);

        operations.charts().subscribe(subscriber);
        syncSubject.onNext(true);

        subscriber.assertNoValues();
    }

    @Test
    public void absentDiscoveryItemWithoutTopFifty() {
        final Chart hotAndNewChart = createHotAndNewChart();
        initChartsWithTracks(hotAndNewChart);

        operations.charts().subscribe(subscriber);
        syncSubject.onNext(true);

        subscriber.assertNoValues();
    }

    private Chart createHotAndNewChart() {
        return createChart(1L, ChartType.TRENDING, ChartCategory.NONE);
    }

    private Chart createTopFiftyChart() {
        return createChart(2L, ChartType.TOP, ChartCategory.NONE);
    }

    private Chart createChart(long localId, ChartType trending, ChartCategory none) {
        return Chart.create(localId,
                            trending,
                            none,
                            "title",
                            "page",
                            Optional.<Urn>absent(),
                            Collections.singletonList(ChartTrack.create(Urn.forTrack(localId),
                                                                        Optional.<String>absent())));
    }

    private void initChartsWithTracks(Chart... charts) {
        when(chartsStorage.charts()).thenReturn(Observable.just(Arrays.asList(charts)));
    }

}
