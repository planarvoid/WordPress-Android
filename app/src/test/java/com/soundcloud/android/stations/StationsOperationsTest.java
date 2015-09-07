package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;


@RunWith(MockitoJUnitRunner.class)
public class StationsOperationsTest {
    @Mock StationsStorage stationsStorage;
    @Mock StationsApi stationsApi;
    @Mock StoreTracksCommand storeTracksCommand;
    @Mock StoreStationCommand storeStationCommand;
    @Mock StationsSyncInitiator syncInitiator;

    private final Urn station = Urn.forTrackStation(123L);
    private StationsOperations operations;
    private Station stationFromDisk;
    private ApiStation apiStation;

    @Before
    public void setUp() {
        operations = new StationsOperations(
                stationsStorage,
                stationsApi,
                storeTracksCommand,
                storeStationCommand,
                syncInitiator,
                Schedulers.immediate()
        );

        stationFromDisk = StationFixtures.getStation(Urn.forTrackStation(123L));
        apiStation = StationFixtures.getApiStation();

        when(stationsStorage.station(station)).thenReturn(Observable.just(stationFromDisk));
        when(stationsApi.fetchStation(station)).thenReturn(Observable.just(apiStation));
    }

    @Test
    public void getStationShouldReturnAStationFromDiskIfDataIsAvailableInDatabase() {
        final TestSubscriber<Station> subscriber = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(stationFromDisk));
    }

    @Test
    public void getStationShouldFallbackToNetwork() {
        when(stationsStorage.station(station)).thenReturn(Observable.<Station>empty());

        final TestSubscriber<Station> subscriber  = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).doesNotContain(stationFromDisk);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldPersistApiStation() {
        when(stationsStorage.station(station)).thenReturn(Observable.<Station>empty());

        final TestSubscriber<Object> subscriber  = new TestSubscriber<>();
        operations.station(station).subscribe(subscriber);

        verify(storeTracksCommand).call(apiStation.getTrackRecords());
        verify(storeStationCommand).call(apiStation);
    }
}