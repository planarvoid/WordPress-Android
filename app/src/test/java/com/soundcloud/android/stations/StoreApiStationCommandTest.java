package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class StoreApiStationCommandTest extends StorageIntegrationTest {
    private ApiStation station;
    private StoreApiStationCommand command;

    @Before
    public void setup() {
        command = new StoreApiStationCommand(propeller());
        station = StationFixtures.getApiStation();
    }

    @Test
    public void shouldAppendThePreviousPlayQueue() {
        ApiStation stationWithNewTracks = StationFixtures.getApiStation(station.getUrn());

        command.call(station);
        command.call(stationWithNewTracks);

        final ArrayList<Urn> allTracks = new ArrayList<>();
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
}