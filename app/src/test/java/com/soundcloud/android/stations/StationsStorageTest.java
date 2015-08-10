package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProviderStub;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StationsStorageTest extends StorageIntegrationTest {
    private final DateProviderStub dateProvider = new DateProviderStub();
    private StationsStorage storage;
    private TestSubscriber<Station> subscriber = new TestSubscriber<>();
    private final Urn stationUrn = Urn.forTrackStation(123L);

    @Before
    public void setup() {
        storage = new StationsStorage(propellerRx(), dateProvider);
    }

    @Test
    public void shouldReturnEmptyIfStationIsAbsent() {
        storage.station(Urn.forTrackStation(999L)).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void shouldReturnTheStation() {
        ApiStation apiStation = testFixtures().insertStation(0);

        storage.station(apiStation.getInfo().getUrn()).subscribe(subscriber);

        final Station station = StationFixtures.getStation(apiStation);
        subscriber.assertReceivedOnNext(Collections.singletonList(station));
    }

    @Test
    public void shouldSaveLastPlayedTrackPosition() {
        final int position = 20;
        final Urn station = testFixtures().insertStation(0).getInfo().getUrn();

        storage.saveLastPlayedTrackPosition(station, position).subscribe();

        databaseAssertions().assertStationPosition(station, position);
    }

    @Test
    public void shouldSaveRecentlyPlayedStation() {
        storage.saveRecentlyPlayedStation(stationUrn).subscribe();

        databaseAssertions().assertRecentStationContains(stationUrn, dateProvider.getCurrentTime(), 1);
    }

    @Test
    public void shouldUpdateRecentlyPlayedStartedAtWhenStationPlayedAgain() {
        final int previousStartedAt = 0;
        testFixtures().insertRecentlyPlayedStation(stationUrn, previousStartedAt);
        storage.saveRecentlyPlayedStation(stationUrn).subscribe();

        databaseAssertions().assertRecentStationContains(stationUrn, previousStartedAt, 0);
        databaseAssertions().assertRecentStationContains(stationUrn, dateProvider.getCurrentTime(), 1);
    }

    @Test
    public void shouldReturnRecentStationsInDescendingOrder() {
        final ApiStation firstStation = testFixtures().insertStation(0);
        final ApiStation secondStation = testFixtures().insertStation(0);
        final ApiStation thirdStation = testFixtures().insertStation(0);

        testFixtures().insertRecentlyPlayedStation(firstStation.getInfo().getUrn(), 0);
        testFixtures().insertRecentlyPlayedStation(secondStation.getInfo().getUrn(), 2);
        testFixtures().insertRecentlyPlayedStation(thirdStation.getInfo().getUrn(), 1);

        final TestSubscriber<List<Station>> subscriber = new TestSubscriber<>();
        storage.recentStations().subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(
                StationFixtures.getStation(secondStation),
                StationFixtures.getStation(thirdStation),
                StationFixtures.getStation(firstStation)
        ));
    }

}