package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.createStationsCollection;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WriteStationsRecommendationsCommandTest extends StorageIntegrationTest {
    private static final Urn STATION1 = Urn.forArtistStation(1L);
    private static final Urn STATION2 = Urn.forArtistStation(2L);

    private WriteStationsRecommendationsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new WriteStationsRecommendationsCommand(propeller());
    }

    @Test
    public void storeNewRecommendations() {
        List<Urn> stations = Arrays.asList(STATION1, STATION2);
        ModelCollection<ApiStationMetadata> stationsCollection = createStationsCollection(stations);

        command.call(stationsCollection);

        databaseAssertions().assertRecommendedStationsEquals(stations);
    }

    @Test
    public void deleteOldRecommendations() {
        List<Urn> oldStations = Collections.singletonList(STATION1);
        List<Urn> newStations = Collections.singletonList(STATION2);
        createStationsCollection(oldStations);

        command.call(createStationsCollection(newStations));

        databaseAssertions().assertRecommendedStationsEquals(newStations);
    }

    @Test
    public void overrideWhenEmpty() {
        List<Urn> oldStations = Collections.singletonList(STATION1);
        List<Urn> newStations = Collections.emptyList();
        createStationsCollection(oldStations);

        command.call(createStationsCollection(newStations));

        databaseAssertions().assertRecommendedStationsEquals(newStations);
    }

}
