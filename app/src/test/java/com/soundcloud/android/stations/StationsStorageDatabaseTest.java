package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.createStationsCollection;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.ChangeResult;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StationsStorageDatabaseTest extends StorageIntegrationTest {

    private final TestDateProvider dateProvider = new TestDateProvider();
    private final Urn stationUrn = Urn.forTrackStation(123L);
    private final long BEFORE_24H = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25L);
    private SharedPreferences sharedPreferences = sharedPreferences();


    private StationsStorage storage;
    private TestSubscriber<StationRecord> subscriber = new TestSubscriber<>();

    @Before
    public void setup() {
        storage = new StationsStorage(
                sharedPreferences,
                propeller(),
                dateProvider
        );
    }

    @Test
    public void shouldReturnEmptyIfStationIsAbsent() {
        storage.station(stationUrn).subscribe(subscriber);

        subscriber.assertValue(null);
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
        subscriber.assertReceivedOnNext(singletonList(station));
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
        TestSubscriber<StationWithTrackUrns> subscriber = new TestSubscriber<>();
        final int position = 20;
        final Urn station = testFixtures().insertStation(0).getUrn();

        storage.saveLastPlayedTrackPosition(station, position);
        storage.stationWithTrackUrns(station).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents().get(0).lastPlayedTrackPosition()).isEqualTo(20);
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
    public void shouldSaveLocalStationLike() {
        final TestSubscriber<ChangeResult> subscriber = new TestSubscriber<>();

        storage.updateLocalStationLike(stationUrn, true).subscribe(subscriber);

        databaseAssertions().assertLocalStationLike(stationUrn);
    }

    @Test
    public void shouldSaveLocalStationUnlike() {
        final TestSubscriber<ChangeResult> subscriber = new TestSubscriber<>();

        storage.updateLocalStationLike(stationUrn, false).subscribe(subscriber);

        databaseAssertions().assertLocalStationUnlike(stationUrn);
    }

    @Test
    public void shouldStationLikeOverridePreviousUnlikeState() {
        final ApiStation apiStation = testFixtures().insertUnlikedStation();
        final TestSubscriber<ChangeResult> subscriber = new TestSubscriber<>();

        storage.updateLocalStationLike(apiStation.getUrn(), true).subscribe(subscriber);

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
        final TestSubscriber<StationWithTrackUrns> subscriber = new TestSubscriber<>();

        storage.stationWithTrackUrns(apiStation.getUrn()).subscribe(subscriber);
        final StationWithTrackUrns stationWithTracks = subscriber.getOnNextEvents().get(0);

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

    private void assertReceivedStationInfoTracks(List<StationInfoTrack> receivedTracks, List<ApiTrack> apiTracks) {
        for (int i = 0; i < apiTracks.size(); i++) {
            final ApiTrack apiTrack = apiTracks.get(i);
            assertThat(apiTrack.getUrn()).isEqualTo(receivedTracks.get(i).getUrn());
            assertThat(apiTrack.getImageUrlTemplate()).isEqualTo(receivedTracks.get(i).getImageUrlTemplate());
        }
    }
}
