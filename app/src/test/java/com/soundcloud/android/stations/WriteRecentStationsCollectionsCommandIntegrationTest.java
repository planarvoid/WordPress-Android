package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.WriteRecentStationsCollectionsCommand.SyncCollectionsMetadata;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WriteRecentStationsCollectionsCommandIntegrationTest extends StorageIntegrationTest {
    private final TestDateProvider dateProvider = new TestDateProvider();
    private WriteRecentStationsCollectionsCommand command;

    @Before
    public void setUp() {
        command = new WriteRecentStationsCollectionsCommand(propeller());
    }

    @Test
    public void shouldUpdateLocalContentWithRemoteContent() throws Exception {
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Arrays.asList(Urn.forTrackStation(0L), Urn.forTrackStation(1L), Urn.forTrackStation(2L))
        );

        final ApiStationMetadata stationZero = remoteContent.getRecents().get(0);
        final ApiStationMetadata stationOne = remoteContent.getRecents().get(1);
        final ApiStationMetadata stationTwo = remoteContent.getRecents().get(2);

        testFixtures().insertRecentlyPlayedUnsyncedStation(stationZero.getUrn(), dateProvider.getCurrentTime() - 2);
        testFixtures().insertRecentlyPlayedUnsyncedStation(stationOne.getUrn(), dateProvider.getCurrentTime() - 1);
        testFixtures().insertRecentlyPlayedStationAtPosition(stationTwo.getUrn(), 1);

        command.call(buildSyncMetadata(remoteContent));

        databaseAssertions().assertStationMetadataInserted(stationZero);
        databaseAssertions().assertStationMetadataInserted(stationOne);
        databaseAssertions().assertRecentStationsAtPosition(stationZero.getUrn(), 0);
        databaseAssertions().assertRecentStationsAtPosition(stationOne.getUrn(), 1);
        databaseAssertions().assertRecentStationsAtPosition(stationTwo.getUrn(), 2);
        databaseAssertions().assertLocalStationDeleted(stationZero.getUrn());
        databaseAssertions().assertLocalStationDeleted(stationOne.getUrn());
    }

    @Test
    public void shouldHaveADefaultLastPlayedPositionOfNull() {
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.singletonList(Urn.forTrackStation(0L))
        );
        final ApiStationMetadata remoteStation = remoteContent.getRecents().get(0);

        command.call(buildSyncMetadata(remoteContent));

        databaseAssertions().assertStationMetadataInserted(remoteStation);
    }

    @Test
    public void shouldNotClearLastPlayedPositionFromStation() {
        final int lastPlayedPosition = 50;
        final ApiStation localStation = testFixtures().insertStation(lastPlayedPosition);
        databaseAssertions().assertStationMetadataInserted(localStation.getMetadata(), lastPlayedPosition);

        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.singletonList(localStation.getUrn())
        );

        command.call(buildSyncMetadata(remoteContent));

        final ApiStationMetadata remoteStation = remoteContent.getRecents().get(0);

        databaseAssertions().assertStationMetadataInserted(remoteStation, lastPlayedPosition);
    }

    @Test
    public void shouldAddToLocalContentNewRemoteContent() throws Exception {
        final ApiStationsCollections remoteContent = StationFixtures.collections();

        command.call(buildSyncMetadata(remoteContent));

        final List<ApiStationMetadata> recents = remoteContent.getRecents();
        for (int i = 0; i < recents.size(); i++) {
            ApiStationMetadata station = recents.get(i);
            databaseAssertions().assertStationMetadataInserted(station);
            databaseAssertions().assertRecentStationsAtPosition(station.getUrn(), i);
        }
    }

    @Test
    public void shouldDeleteLocalContentAbsentInRemoteContent() throws Exception {
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.<Urn>emptyList()
        );

        final ApiStationMetadata localStation = testFixtures().insertStation().getMetadata();
        testFixtures().insertRecentlyPlayedUnsyncedStation(localStation.getUrn(), dateProvider.getCurrentTime() - 1);

        command.call(buildSyncMetadata(remoteContent));

        databaseAssertions().assertStationMetadataInserted(localStation);
        databaseAssertions().assertNoStationsCollections();
    }

    @Test
    public void shouldIgnoreLocalContentCreatedAfterTheSyncingStarted() throws Exception {
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Collections.<Urn>emptyList()
        );


        final ApiStationMetadata localStation = testFixtures().insertStation().getMetadata();
        final long time = dateProvider.getCurrentTime() + 1;
        testFixtures().insertRecentlyPlayedUnsyncedStation(localStation.getUrn(), time);

        command.call(buildSyncMetadata(remoteContent));

        databaseAssertions().assertStationMetadataInserted(localStation);
        databaseAssertions().assertRecentStationsContains(localStation.getUrn(), time, 1);
    }

    @Test
    public void shouldFilterRemoteContentWhenLocalContentWasUpdatedAfterSyncingStarted() throws Exception {
        final ApiStationsCollections remoteContent = StationFixtures.collections(
                Arrays.asList(Urn.forTrackStation(1L), Urn.forTrackStation(2L))
        );

        final ApiStationMetadata stationZero = remoteContent.getRecents().get(0);
        final ApiStationMetadata stationOne = remoteContent.getRecents().get(1);

        final long stationZeroUpdatedAt = dateProvider.getCurrentTime() + 1;
        testFixtures().insertRecentlyPlayedUnsyncedStation(stationZero.getUrn(), stationZeroUpdatedAt);
        testFixtures().insertRecentlyPlayedUnsyncedStation(stationOne.getUrn(), dateProvider.getCurrentTime() - 1);

        command.call(buildSyncMetadata(remoteContent));

        databaseAssertions().assertStationMetadataInserted(stationZero);
        databaseAssertions().assertStationMetadataInserted(stationOne);
        databaseAssertions().assertRecentStationsContains(stationZero.getUrn(), stationZeroUpdatedAt, 1);
        databaseAssertions().assertRecentStationsAtPosition(stationOne.getUrn(), 1);
    }

    private SyncCollectionsMetadata buildSyncMetadata(ApiStationsCollections remoteContent) {
        return new SyncCollectionsMetadata(
                dateProvider.getCurrentTime(),
                remoteContent
        );
    }
}
