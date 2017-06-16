package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.createStationsCollection;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StationsStorageTest extends StorageIntegrationTest {

    private final TestDateProvider dateProvider = new TestDateProvider();
    private final Urn stationUrn = Urn.forTrackStation(123L);
    private final long BEFORE_24H = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25L);
    private SharedPreferences sharedPreferences = sharedPreferences();

    private StationsStorage storage;

    @Before
    public void setup() {
        storage = new StationsStorage(
                sharedPreferences,
                propeller(),
                propellerRxV2(),
                dateProvider
        );
    }

    @Test
    public void shouldReturnEmptyIfStationIsAbsent() {
        storage.station(stationUrn).test().assertNoValues().assertComplete();
    }

    @Test
    public void clearExpiredPlayQueuesWhenOlderThan24Hours() {
        final ApiStation station = StationFixtures.getApiStation(stationUrn, 10);
        testFixtures().insertStation(station, BEFORE_24H, 5);

        storage.clearExpiredPlayQueue(stationUrn).subscribe();

        databaseAssertions().assertStationPlayQueuePositionNotSet(stationUrn);
        databaseAssertions().assertStationPlayQueueContains(stationUrn, Lists.emptyList());
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
    public void clearRemovesDataFromAllStationTables() {
        testFixtures().insertStation();
        testFixtures().insertRecentlyPlayedStationAtPosition(stationUrn, 2);

        storage.clear();

        databaseAssertions().assertNoStations();
        databaseAssertions().assertNoStationsCollections();
        databaseAssertions().assertNoStationPlayQueues();
    }

    @Test
    public void loadPlayQueueReturnsEmptyWhenNoContent() {
        storage.loadPlayQueue(stationUrn, 30)
               .test()
               .assertValue(Lists.emptyList())
               .assertComplete();
    }

    @Test
    public void loadPlayQueueReturnsAllContentWhenStartPositionIs0() {
        final ApiStation station = StationFixtures.getApiStation(stationUrn, 10);
        testFixtures().insertStation(station);

        storage.loadPlayQueue(stationUrn, 0).test()
               .assertValue(station.getTracks())
               .assertComplete();
    }

    @Test
    public void loadPlayQueueReturnsContentAfterGivenStartPosition() {
        final int size = 10;
        final ApiStation station = StationFixtures.getApiStation(stationUrn, size);
        testFixtures().insertStation(station);

        storage.loadPlayQueue(stationUrn, 5).test()
               .assertValue(station.getTracks().subList(5, size))
               .assertComplete();
    }

    @Test
    public void shouldReturnTheStation() {
        final ApiStation apiStation = testFixtures().insertStation();
        final StationRecord station = StationFixtures.getStation(apiStation);

        storage.station(apiStation.getUrn()).test()
               .assertValue(station);
    }

    @Test
    public void shouldSaveLastPlayedTrackPosition() {
        final int position = 20;
        final Urn station = testFixtures().insertStation(0).getUrn();

        storage.saveLastPlayedTrackPosition(station, position);

        databaseAssertions().assertStationPlayQueuePosition(station, position);
    }

    @Test
    public void shouldLoadLastPlayedTrackPosition() {
        final int position = 20;
        final Urn station = testFixtures().insertStation(0).getUrn();

        storage.saveLastPlayedTrackPosition(station, position);
        final StationWithTrackUrns stationWithTrackUrns = storage.stationWithTrackUrns(station).test().values().get(0);

        assertThat(stationWithTrackUrns.lastPlayedTrackPosition()).isEqualTo(20);
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

        storage.getStationsCollection(StationsCollectionsTypes.RECENT).test()
               .assertValue(Lists.newArrayList(StationFixtures.getStation(secondStation),
                                               StationFixtures.getStation(firstStation),
                                               StationFixtures.getStation(thirdStation))
               );
    }

    @Test
    public void shouldSaveLocalStationLike() {
        storage.updateLocalStationLike(stationUrn, true).test();

        databaseAssertions().assertLocalStationLike(stationUrn);
    }

    @Test
    public void shouldSaveLocalStationUnlike() {
        storage.updateLocalStationLike(stationUrn, false).test();

        databaseAssertions().assertLocalStationUnlike(stationUrn);
    }

    @Test
    public void shouldStationLikeOverridePreviousUnlikeState() {
        final ApiStation apiStation = testFixtures().insertUnlikedStation();

        storage.updateLocalStationLike(apiStation.getUrn(), true).test();

        databaseAssertions().assertLocalStationLike(apiStation.getUrn());
    }

    @Test
    public void getLocalLikedStationsShouldReturnLocalChanges() {
        final Urn localLikedStation = testFixtures().insertLocalLikedStation().getUrn();
        testFixtures().insertLocalUnlikedStation();
        testFixtures().insertLikedStation();

        assertThat(storage.getLocalLikedStations()).containsExactly(localLikedStation);
    }

    @Test
    public void getLocalLikedStationsShouldReturnEmptyWheNone() {
        testFixtures().insertLocalUnlikedStation();
        testFixtures().insertLikedStation();

        assertThat(storage.getLocalLikedStations()).isEmpty();
    }

    @Test
    public void getLocalUnlikedStationsShouldReturnLocalUnlikedStations() {
        testFixtures().insertLocalLikedStation();
        final Urn localUnlikedSation = testFixtures().insertLocalUnlikedStation().getUrn();
        testFixtures().insertUnlikedStation();

        assertThat(storage.getLocalUnlikedStations()).containsExactly(localUnlikedSation);
    }

    @Test
    public void getLocalUnlikedStationsShouldReturnEmptyWhenNone() {
        testFixtures().insertLocalLikedStation();
        testFixtures().insertUnlikedStation();

        assertThat(storage.getLocalUnlikedStations()).isEmpty();
    }

    @Test
    public void shouldReturnCorrectLikeStatusWhenLoadingStationWithTracks() {
        final ApiStation apiStation = testFixtures().insertLikedStation();

        final StationWithTrackUrns stationWithTracks = storage.stationWithTrackUrns(apiStation.getUrn()).test().values().get(0);

        assertThat(stationWithTracks.liked()).isTrue();
        assertThat(stationWithTracks.title()).isEqualTo(apiStation.getTitle());
        assertThat(stationWithTracks.type()).isEqualTo(apiStation.getType());
    }

    @Test
    public void shouldFilterRemoteContentWhenLocalContentWasUpdatedAfterSyncingStarted() throws Exception {
        final List<Urn> stationUrns = Arrays.asList(Urn.forTrackStation(1L), Urn.forTrackStation(2L));
        final ModelCollection<ApiStationMetadata> remoteContent = createStationsCollection(stationUrns);

        final List<ApiStationMetadata> collection = remoteContent.getCollection();
        storage.setLikedStationsAndAddNewMetaData(stationUrns, collection);

        databaseAssertions().assertLikedStationHasSize(2);
        databaseAssertions().assertStationMetadataInserted(collection.get(0));
        databaseAssertions().assertStationMetadataInserted(collection.get(1));
    }

    @Test
    public void loadsUrnByPermalink() throws Exception {
        testFixtures().insertStation();
        ApiStation station = testFixtures().insertStation();
        String permalink = station.getPermalink();

        final Urn urn = storage.urnForPermalink(permalink).blockingGet();

        assertThat(urn).isEqualTo(station.getUrn());
    }

    @Test
    public void loadsUrnByPermalinkWithStationPrefix() throws Exception {
        testFixtures().insertStation();
        ApiStation station = testFixtures().insertStation();
        String permalink = "stations/" + station.getPermalink();

        final Urn urn = storage.urnForPermalink(permalink).blockingGet();

        assertThat(urn).isEqualTo(station.getUrn());
    }

    @Test
    public void loadsUrnByPermalinkNotFound() throws Exception {
        testFixtures().insertStation();

        storage.urnForPermalink("testing")
                .test()
                .assertNoValues()
                .assertComplete();
    }
}
