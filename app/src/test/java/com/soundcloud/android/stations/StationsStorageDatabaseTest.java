package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.PropertySet;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StationsStorageDatabaseTest extends StorageIntegrationTest {

    private final TestDateProvider dateProvider = new TestDateProvider();
    private final Urn stationUrn = Urn.forTrackStation(123L);
    private final long BEFORE_24H = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25L);

    private StationsStorage storage;
    private TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();

    @Before
    public void setup() {
        storage = new StationsStorage(
                mock(SharedPreferences.class),
                propeller(),
                dateProvider
        );
    }

    @Test
    public void shouldReturnEmptyIfStationIsAbsent() {
        storage.station(stationUrn).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void clearExpiredPlayQueuesWhenOlderThan24Hours() {
        final ApiStation station = StationFixtures.getApiStation(stationUrn, 10);
        testFixtures().insertStation(station, BEFORE_24H, 5);

        storage.clearExpiredPlayQueue(stationUrn).subscribe();

        databaseAssertions().assertStationPlayQueuePositionNotSet(stationUrn);
        databaseAssertions().assertStationPlayQueueContains(stationUrn, Lists.<StationTrack>emptyList());
    }

    @Test
    public void clearExpiredPlayQueuesNoOpWhenYoungerThan24Hours() {
        final ApiStation station = StationFixtures.getApiStation(stationUrn, 10);
        testFixtures().insertStation(station, System.currentTimeMillis(), 5);

        storage.clearExpiredPlayQueue(stationUrn).subscribe();

        databaseAssertions().assertStationPlayQueuePosition(stationUrn, 5);
        databaseAssertions().assertStationPlayQueueContains(stationUrn, station.getTracks());
    }

    @Test
    public void loadPlayQueueReturnsEmptyWhenNoContent() {
        final TestSubscriber<StationTrack> subscriber = new TestSubscriber<>();

        storage.loadPlayQueue(stationUrn, 30).subscribe(subscriber);

        subscriber.assertNoValues();
        subscriber.assertCompleted();
    }

    @Test
    public void loadPlayQueueReturnsAllContentWhenStartPositionIs0() {
        final TestSubscriber<StationTrack> subscriber = new TestSubscriber<>();
        final ApiStation station = StationFixtures.getApiStation(stationUrn, 10);
        testFixtures().insertStation(station);

        storage.loadPlayQueue(stationUrn, 0).subscribe(subscriber);

        subscriber.assertReceivedOnNext(station.getTracks());
        subscriber.assertCompleted();
    }

    @Test
    public void loadPlayQueueReturnsContentAfterGivenStartPosition() {
        final TestSubscriber<StationTrack> subscriber = new TestSubscriber<>();
        final int size = 10;
        final ApiStation station = StationFixtures.getApiStation(stationUrn, size);
        testFixtures().insertStation(station);

        storage.loadPlayQueue(stationUrn, 5).subscribe(subscriber);

        subscriber.assertReceivedOnNext(station.getTracks().subList(5, size));
        subscriber.assertCompleted();
    }

    @Test
    public void shouldReturnTheStation() {
        ApiStation apiStation = testFixtures().insertStation();

        storage.station(apiStation.getUrn()).subscribe(subscriber);

        final StationRecord station = StationFixtures.getStation(apiStation);
        subscriber.assertReceivedOnNext(Collections.singletonList(station));
    }

    @Test
    public void shouldSaveLastPlayedTrackPosition() {
        final int position = 20;
        final Urn station = testFixtures().insertStation(0).getUrn();

        storage.saveLastPlayedTrackPosition(station, position);

        databaseAssertions().assertStationPlayQueuePosition(station, position);
    }

    @Test
    public void shouldSaveRecentlyPlayedStation() {
        storage.saveUnsyncedRecentlyPlayedStation(stationUrn);

        databaseAssertions().assertRecentStationsContains(stationUrn, dateProvider.getCurrentTime(), 1);
    }

    @Test
    public void shouldUpdateRecentlyPlayedStartedAtWhenStationPlayedAgain() {
        final int previousStartedAt = 0;
        testFixtures().insertRecentlyPlayedStationAtPosition(stationUrn, previousStartedAt);
        storage.saveUnsyncedRecentlyPlayedStation(stationUrn);

        databaseAssertions().assertRecentStationsContains(stationUrn, previousStartedAt, 0);
        databaseAssertions().assertRecentStationsContains(stationUrn, dateProvider.getCurrentTime(), 1);
    }

    @Test
    public void shouldReturnRecentStationsInCorrectOrder() {
        final ApiStation firstStation = testFixtures().insertStation();
        final ApiStation secondStation = testFixtures().insertStation();
        final ApiStation thirdStation = testFixtures().insertStation();

        testFixtures().insertRecentlyPlayedStationAtPosition(firstStation.getUrn(), 0);
        testFixtures().insertLocallyPlayedRecentStation(secondStation.getUrn(), System.currentTimeMillis());
        testFixtures().insertRecentlyPlayedStationAtPosition(thirdStation.getUrn(), 1);

        final TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();
        storage.getStationsCollection(StationsCollectionsTypes.RECENT).subscribe(subscriber);

        subscriber.assertValues(
                StationFixtures.getStation(secondStation),
                StationFixtures.getStation(firstStation),
                StationFixtures.getStation(thirdStation)
        );
    }

    @Test
    public void shouldReturnRecentStationsToSync() {
        final ApiStation firstSyncedStation = testFixtures().insertStation();
        final ApiStation secondSyncedStation = testFixtures().insertStation();
        final ApiStation unsyncedStation = testFixtures().insertStation();
        final long timestamp = 1421315988L;

        testFixtures().insertRecentlyPlayedStationAtPosition(firstSyncedStation.getUrn(), 0);
        testFixtures().insertRecentlyPlayedStationAtPosition(secondSyncedStation.getUrn(), 1);
        testFixtures().insertRecentlyPlayedUnsyncedStation(unsyncedStation.getUrn(), timestamp);

        final List<PropertySet> recentStationsToSync = storage.getRecentStationsToSync();

        final PropertySet expectedProperties = PropertySet.from(
                StationProperty.URN.bind(unsyncedStation.getUrn()),
                StationProperty.UPDATED_LOCALLY_AT.bind(timestamp),
                StationProperty.POSITION.bind(unsyncedStation.getPreviousPosition())
        );

        assertThat(recentStationsToSync).containsExactly(expectedProperties);
    }
}
