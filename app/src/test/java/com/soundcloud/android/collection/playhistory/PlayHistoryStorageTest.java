package com.soundcloud.android.collection.playhistory;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.Track;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayHistoryStorageTest extends StorageIntegrationTest {

    private PlayHistoryStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new PlayHistoryStorage(propeller());
    }

    @Test
    public void loadTracksReturnsTracksSortedInReverseTimestampOrder() {
        final Track expected1 = insertTrackWithPlayHistory(1000L);
        final Track expected2 = insertTrackWithPlayHistory(2000L);

        storage.loadTracks(10).test()
               .assertValueCount(1)
               .assertValue(Arrays.asList(expected2.urn(), expected1.urn()));
    }

    @Test
    public void loadTracksGroupsByLastPlayed() {
        final Track track1 = insertTrackWithPlayHistory(1000L);
        final Track track2 = insertTrackWithPlayHistory(2000L);
        final Track track3 = insertTrackWithPlayHistory(3000L);

        // played on another device after sync
        insertPlayHistory(track3.urn(), 4000L);
        insertPlayHistory(track2.urn(), 1500L);
        insertPlayHistory(track1.urn(), 2500L);

        storage.loadTracks(10).test()
               .assertValueCount(1)
               .assertValue(Arrays.asList(track3.urn(), track1.urn(), track2.urn()));
    }

    @Test
    public void loadTracksSetDownloadState() {
        final Track expected = insertTrackWithPlayHistory(1000L);
        testFixtures().insertCompletedTrackDownload(expected.urn(), 1000L, 2000L);

        Urn actual = storage.loadTracks(10).test()
                              .assertValueCount(1)
                              .values().get(0).get(0);

        assertThat(actual).isEqualTo(expected.urn());
    }

    @Test
    public void loadTracksReturnsTracks() {
        final Track expected = insertTrackWithPlayHistory(1000L);

        Urn actual = storage.loadTracks(10).test().values().get(0).get(0);

        assertThat(actual).isEqualTo(expected.urn());
    }

    @Test
    public void loadUnSyncedPlayHistoryReturnsOnlyUnsynced() {
        final Urn trackUrn = Urn.forTrack(123L);
        testFixtures().insertUnsyncedPlayHistory(1000L, trackUrn);
        testFixtures().insertPlayHistory(2000L, trackUrn);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadUnSyncedPlayHistory();

        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(trackUrn);
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(1000L);
    }

    @Test
    public void loadSyncedPlayHistoryReturnsOnlySynced() {
        final Urn trackUrn = Urn.forTrack(123L);
        testFixtures().insertUnsyncedPlayHistory(1000L, trackUrn);
        testFixtures().insertPlayHistory(2000L, trackUrn);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadSyncedPlayHistory();

        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(trackUrn);
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(2000L);
    }

    @Test
    public void setSyncedUpdatesEntries() {
        final Urn trackUrn = Urn.forTrack(123L);
        final Urn trackUrn2 = Urn.forTrack(234L);
        testFixtures().insertUnsyncedPlayHistory(1000L, trackUrn);
        testFixtures().insertUnsyncedPlayHistory(1500L, trackUrn2);
        testFixtures().insertPlayHistory(2000L, trackUrn);

        storage.setSynced(Arrays.asList(
                PlayHistoryRecord.create(1000L, trackUrn, Urn.NOT_SET),
                PlayHistoryRecord.create(1500L, trackUrn2, Urn.NOT_SET)));

        assertThat(storage.loadUnSyncedPlayHistory().size()).isEqualTo(0);
    }

    @Test
    public void removePlayHistoryRemovesEntries() {
        final Track trackItem1 = insertTrackWithPlayHistory(2000L);
        final Track trackItem2 = insertTrackWithPlayHistory(3000L);

        storage.removePlayHistory(Collections.singletonList(
                PlayHistoryRecord.create(2000L, trackItem1.urn(), Urn.NOT_SET)));

        final List<Urn> existingTracks = storage.loadTracks(10).test()
                                                  .assertValueCount(1)
                                                  .values().get(0);

        assertThat(existingTracks.size()).isEqualTo(1);

        assertThat(trackItem2.urn()).isEqualTo(existingTracks.get(0));
    }

    @Test
    public void insertPlayHistoryAddsEntriesAsSynced() {
        insertPlayHistory(Urn.forTrack(123), 1000L);
        insertPlayHistory(Urn.forTrack(234), 2000L);

        storage.insertPlayHistory(Collections.singletonList(
                PlayHistoryRecord.create(3000L, Urn.forTrack(123), Urn.NOT_SET)));

        assertThat(storage.loadSyncedPlayHistory().size()).isEqualTo(3);
    }

    @Test
    public void loadPlayHistoryForPlaybackGetsOnlyUrnsWithoutDuplicates() {
        final Urn urn1 = insertTrackWithPlayHistory(1000L).urn();
        final Urn urn2 = insertTrackWithPlayHistory(2000L).urn();

        insertPlayHistory(urn1, 3000L);

        storage.loadPlayHistoryForPlayback().test()
               .assertValueCount(1)
               .assertValues(Arrays.asList(urn1, urn2));
    }

    @Test
    public void clearClearsTable() {
        testFixtures().insertUnsyncedPlayHistory(1500L, Urn.forTrack(123L));
        testFixtures().insertPlayHistory(2000L, Urn.forTrack(234L));

        storage.clear();

        databaseAssertions().assertPlayHistoryCount(0);
    }

    private Track insertTrackWithPlayHistory(long timestamp) {
        final Track track = Track.from(testFixtures().insertTrack());
        insertPlayHistory(track.urn(), timestamp);
        return track;
    }

    private void insertPlayHistory(Urn urn, long timestamp) {
        testFixtures().insertPlayHistory(timestamp, urn);
    }

}
