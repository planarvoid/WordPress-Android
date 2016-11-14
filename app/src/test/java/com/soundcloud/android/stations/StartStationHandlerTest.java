package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
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

    @Mock Navigator navigator;
    @Mock StartStationPresenter startStationPresenter;
    @Mock Context context;
    @Mock EventBus eventBus;

    private StartStationHandler stationHandler;

    @Before
    public void setUp() throws Exception {
        stationHandler = new StartStationHandler(navigator, eventBus);
    }

    @Test
    public void opensInfoPageFromRecommendation() {
        stationHandler.startStation(context, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);

        verify(navigator).legacyOpenStationInfo(context, STATION_URN, DiscoverySource.STATIONS_SUGGESTIONS);
    }

    @Test
    public void opensInfoPage() {
        stationHandler.startStation(context, STATION_URN);

        verify(navigator).legacyOpenStationInfo(context, STATION_URN, DiscoverySource.STATIONS);
    }
}
