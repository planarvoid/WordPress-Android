package com.soundcloud.android.stations;

import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.app.Activity;

@RunWith(MockitoJUnitRunner.class)
public class StartStationHandlerTest {

    private final Urn STATION_URN = Urn.forArtistStation(123L);
    private final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock Navigator navigator;
    @Mock Activity activity;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;

    private StartStationHandler stationHandler;

    @Before
    public void setUp() throws Exception {
        stationHandler = new StartStationHandler(navigator, performanceMetricsEngine);
    }

    @Test
    public void opensInfoPageFromRecommendation() {
        stationHandler.startStation(activity, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);

        verify(navigator).navigateTo(eq(activity),
                                     argThat(matchesNavigationTarget(NavigationTarget.forStationInfo(STATION_URN,
                                                                                                     Optional.absent(),
                                                                                                     Optional.of(DiscoverySource.STATIONS_SUGGESTIONS),
                                                                                                     Optional.absent()))));
    }

    @Test
    public void opensInfoPage() {
        stationHandler.startStation(activity, STATION_URN);

        verify(navigator).navigateTo(eq(activity),
                                     argThat(matchesNavigationTarget(NavigationTarget.forStationInfo(STATION_URN,
                                                                                                     Optional.absent(),
                                                                                                     Optional.of(DiscoverySource.STATIONS),
                                                                                                     Optional.absent()))));
    }

    @Test
    public void shouldStartMeasuringLoadStationPerformanceOnStartStationWithSource() {
        stationHandler.startStation(activity, STATION_URN, DiscoverySource.STATIONS);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LOAD_STATION);
    }

    @Test
    public void shouldStartMeasuringLoadStationPerformanceOnStartStation() {
        stationHandler.startStation(activity, STATION_URN);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LOAD_STATION);
    }

    @Test
    public void shouldStartMeasuringLoadStationPerformanceOnOpenStationWithSeedTrack() {
        stationHandler.openStationWithSeedTrack(activity, TRACK_URN, mock(UIEvent.class));

        verify(performanceMetricsEngine).startMeasuring(MetricType.LOAD_STATION);
    }
}
