package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

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
    public void collectionShouldReturnFromStorageIfAvailable() {
        final Station station = StationFixtures.getStation(Urn.forTrackStation(123L));
        final TestSubscriber<Station> subscriber = new TestSubscriber<>();
        final PublishSubject<SyncResult> syncResults = PublishSubject.create();

        when(stationsStorage.getStationsCollection(StationsCollectionsTypes.RECENT))
                .thenReturn(Observable.just(station));
        when(syncInitiator.syncRecentStations()).thenReturn(syncResults);

        operations.collection(StationsCollectionsTypes.RECENT).subscribe(subscriber);

        subscriber.assertValue(station);
        assertThat(syncResults.hasObservers()).isFalse();
    }

    @Test
    public void collectionShouldTriggerSyncerIfNothingInLocalDatabase() {
        final Observable<Station> storageStations1 = Observable.empty();
        final PublishSubject<SyncResult> syncResults = PublishSubject.create();
        final Station station = StationFixtures.getStation(Urn.forTrackStation(123L));
        final Observable<Station> storageStations2 = Observable.just(station);
        final TestSubscriber<Station> subscriber = new TestSubscriber<>();

        when(stationsStorage.getStationsCollection(StationsCollectionsTypes.RECENT))
                .thenReturn(storageStations1, storageStations2);
        when(syncInitiator.syncRecentStations()).thenReturn(syncResults);

        operations.collection(StationsCollectionsTypes.RECENT).subscribe(subscriber);

        subscriber.assertNoValues();
        syncResults.onNext(SyncResult.success("action", true));
        syncResults.onCompleted();
        subscriber.assertValue(station);
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