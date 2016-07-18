package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

@RunWith(MockitoJUnitRunner.class)
public class StartStationHandlerTest {

    private final Urn STATION_URN = Urn.forArtistStation(123L);

    @Mock FeatureFlags featureFlags;
    @Mock Navigator navigator;
    @Mock StartStationPresenter startStationPresenter;
    @Mock Context context;
    @Mock EventBus eventBus;

    private StartStationHandler stationHandler;

    @Before
    public void setUp() throws Exception {
        stationHandler = new StartStationHandler(navigator, startStationPresenter, featureFlags, eventBus);
    }

    @Test
    public void opensInfoPageFromRecommendation() {
        when(featureFlags.isEnabled(Flag.STATION_INFO_PAGE)).thenReturn(true);

        stationHandler.startStation(context, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);

        verify(navigator).openStationInfo(context, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);
    }

    @Test
    public void opensInfoPage() {
        when(featureFlags.isEnabled(Flag.STATION_INFO_PAGE)).thenReturn(true);

        stationHandler.startStation(context, STATION_URN);

        verify(navigator).openStationInfo(context, STATION_URN, DiscoverySource.STATIONS);
    }
}
