package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class StoreStationCommandTest extends StorageIntegrationTest {
    private ApiStation station;
    private StoreStationCommand command;

    @Before
    public void setup() {
        command = new StoreStationCommand(propeller());
        station = StationFixtures.getApiStation();
    }

    @Test
    public void shouldRemoveThePreviousPlayQueue() {
        StationRecord stationWithEmptyPlayQueue = new ApiStation(station.getMetadata(), new ModelCollection<>(Collections.<ApiTrack>emptyList()));

        command.call(station);
        command.call(stationWithEmptyPlayQueue);

        databaseAssertions().assertStationsPlayQueueIsEmpty(station);
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