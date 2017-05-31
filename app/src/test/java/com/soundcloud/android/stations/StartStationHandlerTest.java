package com.soundcloud.android.stations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.Context;

@RunWith(MockitoJUnitRunner.class)
public class StartStationHandlerTest {

    private final Urn STATION_URN = Urn.forArtistStation(123L);
    private final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock NavigationExecutor navigationExecutor;
    @Mock Context context;
    @Mock EventBus eventBus;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;

    private StartStationHandler stationHandler;

    @Before
    public void setUp() throws Exception {
        stationHandler = new StartStationHandler(navigationExecutor, eventBus, performanceMetricsEngine);
    }

    @Test
    public void opensInfoPageFromRecommendation() {
        stationHandler.startStation(context, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);

        verify(navigationExecutor).legacyOpenStationInfo(context, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);
    }

    @Test
    public void opensInfoPage() {
        stationHandler.startStation(context, STATION_URN);

        verify(navigationExecutor).legacyOpenStationInfo(context, STATION_URN, DiscoverySource.STATIONS);
    }

    @Test
    public void shouldStartMeasuringLoadStationPerformanceOnStartStationWithSource() {
        stationHandler.startStation(context, STATION_URN, DiscoverySource.STATIONS);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LOAD_STATION);
    }

    @Test
    public void shouldStartMeasuringLoadStationPerformanceOnStartStation() {
        stationHandler.startStation(context, STATION_URN);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LOAD_STATION);
    }

    @Test
    public void shouldStartMeasuringLoadStationPerformanceOnOpenStationWithSeedTrack() {
        stationHandler.openStationWithSeedTrack(context, TRACK_URN, mock(UIEvent.class));

        verify(performanceMetricsEngine).startMeasuring(MetricType.LOAD_STATION);
    }
}
