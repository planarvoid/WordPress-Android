package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class StoreStationCommandTest extends StorageIntegrationTest {
    private StationRecord station;
    private StoreStationCommand command;

    @Before
    public void setup() {
        command = new StoreStationCommand(propeller());
        station = StationFixtures.getApiStation();
    }

    @Test
    public void shouldRemoveThePreviousPlayQueue() {
        StationRecord stationWithEmptyPlayQueue = new ApiStation(station.getInfo(), new ModelCollection<>(Collections.<ApiTrack>emptyList()));

        command.call(station);
        command.call(stationWithEmptyPlayQueue);

        databaseAssertions().assertStationsPlayQueueIsEmpty(station);
    }

    @Test
    public void shouldUpdateWhenStationAlreadyExists() {
        final Urn stationUrn = station.getInfo().getUrn();
        StationRecord upsert = StationFixtures.getApiStation(stationUrn);

        command.call(station);
        command.call(upsert);

        databaseAssertions().assertStationIsUnique(stationUrn);
        databaseAssertions().assertStationInfoInserted(upsert.getInfo());
    }

    @Test
    public void shouldStoreTheStation() {
        command.call(station);

        databaseAssertions().assertStationInserted(station);
    }
}