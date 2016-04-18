package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StoreStationCommandTest extends StorageIntegrationTest {

    private TestDateProvider dateProvider;
    private ApiStation station;
    private StoreStationCommand command;

    @Before
    public void setup() {
        station = StationFixtures.getApiStation();
        dateProvider = new TestDateProvider();
        command = new StoreStationCommand(propeller(), dateProvider);
    }

    @Test
    public void shouldAppendThePreviousPlayQueue() {
        ApiStation stationWithNewTracks = StationFixtures.getApiStation(station.getUrn());

        command.call(station);
        command.call(stationWithNewTracks);

        final ArrayList<StationTrack> allTracks = new ArrayList<>();
        allTracks.addAll(station.getTracks());
        allTracks.addAll(stationWithNewTracks.getTracks());
        databaseAssertions().assertStationPlayQueueContains(station.getUrn(), allTracks);
    }

    @Test
    public void shouldUpdateWhenStationAlreadyExists() {
        final Urn stationUrn = station.getUrn();
        ApiStation upsert = StationFixtures.getApiStation(stationUrn);

        command.call(station);
        command.call(upsert);

        databaseAssertions().assertStationIsUnique(stationUrn);
        databaseAssertions().assertStationInserted(upsert);
    }

    @Test
    public void shouldStoreTheStation() {
        command.call(station);

        databaseAssertions().assertStationInserted(station);
    }

    @Test
    public void shouldUpdatePlayQueueUpdatedAtTimestamp() {
        long previousUpdateDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)).getTime();
        testFixtures().insertStation(station, previousUpdateDate, 0);

        command.call(station);
        databaseAssertions().assertStationUpdateTime(station.getUrn(), dateProvider.getCurrentTime());
    }
}
