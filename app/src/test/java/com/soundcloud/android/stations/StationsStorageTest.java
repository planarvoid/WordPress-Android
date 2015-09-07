package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProviderStub;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Collections;
import java.util.List;

public class StationsStorageTest extends StorageIntegrationTest {
    private final DateProviderStub dateProvider = new DateProviderStub();
    private StationsStorage storage;
    private TestSubscriber<Station> subscriber = new TestSubscriber<>();
    private final Urn stationUrn = Urn.forTrackStation(123L);

    @Before
    public void setup() {
        storage = new StationsStorage(propeller(), propellerRx(), dateProvider);
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

        storage.station(apiStation.getUrn()).subscribe(subscriber);

        final Station station = StationFixtures.getStation(apiStation);
        subscriber.assertReceivedOnNext(Collections.singletonList(station));
    }

    @Test
    public void shouldSaveLastPlayedTrackPosition() {
        final int position = 20;
        final Urn station = testFixtures().insertStation(0).getUrn();

        storage.saveLastPlayedTrackPosition(station, position).subscribe();

        databaseAssertions().assertStationPlayQueuePosition(station, position);
    }

    @Test
    public void shouldSaveRecentlyPlayedStation() {
        storage.saveUnsyncedRecentlyPlayedStation(stationUrn).subscribe();

        databaseAssertions().assertRecentStationsContains(stationUrn, dateProvider.getCurrentTime(), 1);
    }

    @Test
    public void shouldUpdateRecentlyPlayedStartedAtWhenStationPlayedAgain() {
        final int previousStartedAt = 0;
        testFixtures().insertRecentlyPlayedStationAtPosition(stationUrn, previousStartedAt);
        storage.saveUnsyncedRecentlyPlayedStation(stationUrn).subscribe();

        databaseAssertions().assertRecentStationsContains(stationUrn, previousStartedAt, 0);
        databaseAssertions().assertRecentStationsContains(stationUrn, dateProvider.getCurrentTime(), 1);
    }

    @Test
    public void shouldReturnRecentStationsInCorrectOrder() {
        final ApiStation firstStation = testFixtures().insertStation(0);
        final ApiStation secondStation = testFixtures().insertStation(0);
        final ApiStation thirdStation = testFixtures().insertStation(0);

        testFixtures().insertRecentlyPlayedStationAtPosition(firstStation.getUrn(), 0);
        testFixtures().insertLocallyPlayedRecentStation(secondStation.getUrn(), System.currentTimeMillis());
        testFixtures().insertRecentlyPlayedStationAtPosition(thirdStation.getUrn(), 1);

        final TestSubscriber<Station> subscriber = new TestSubscriber<>();
        storage.getStationsCollection(StationsCollectionsTypes.RECENT).subscribe(subscriber);

        subscriber.assertValues(
                StationFixtures.getStation(secondStation),
                StationFixtures.getStation(firstStation),
                StationFixtures.getStation(thirdStation)
        );
    }

    @Test
    public void shouldReturnRecentStationsToSync() {
        final ApiStation firstSyncedStation = testFixtures().insertStation(0);
        final ApiStation secondSyncedStation = testFixtures().insertStation(0);
        final ApiStation unsyncedStation = testFixtures().insertStation(0);
        final long timestamp = 1421315988L;

        testFixtures().insertRecentlyPlayedStationAtPosition(firstSyncedStation.getUrn(), 0);
        testFixtures().insertRecentlyPlayedStationAtPosition(secondSyncedStation.getUrn(), 1);
        testFixtures().insertRecentlyPlayedUnsyncedStation(unsyncedStation.getUrn(), timestamp);

        final List<PropertySet> recentStationsToSync = storage.getRecentStationsToSync();

        final PropertySet expectedProperties = PropertySet.from(
                StationProperty.URN.bind(unsyncedStation.getUrn()),
                StationProperty.UPDATED_LOCALLY_AT.bind(timestamp),
                StationProperty.POSITION.bind(0)
        );

        assertThat(recentStationsToSync).containsExactly(expectedProperties);
    }
}