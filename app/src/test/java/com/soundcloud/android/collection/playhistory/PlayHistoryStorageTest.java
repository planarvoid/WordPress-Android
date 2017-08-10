package com.soundcloud.android.collection.playhistory;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.collection.CollectionDatabase;
import com.soundcloud.android.collection.CollectionDatabaseOpenHelper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import android.content.ContentValues;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayHistoryStorageTest extends AndroidUnitTest {

    private static Urn urn1 = Urn.forTrack(1000L);
    private static Urn urn2 = Urn.forTrack(2000L);
    private static Urn urn3 = Urn.forTrack(3000L);

    private PlayHistoryStorage storage;
    private CollectionDatabaseOpenHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        dbHelper = new CollectionDatabaseOpenHelper(RuntimeEnvironment.application);
        CollectionDatabase collectionDatabase = new CollectionDatabase(dbHelper, Schedulers.trampoline());
        storage = new PlayHistoryStorage(collectionDatabase);
    }

    @Test
    public void loadTracksReturnsTracksSortedInReverseTimestampOrder() {
        insertTrackWithPlayHistory(urn2);
        insertTrackWithPlayHistory(urn1);
        insertTrackWithPlayHistory(urn3);

        storage.loadTrackUrns(10).test()
               .assertValueCount(1)
               .assertValue(Arrays.asList(urn3, urn2, urn1));
    }

    @Test
    public void loadTracksGroupedByLastPlayed() {
        insertTrackWithPlayHistory(urn1);
        insertTrackWithPlayHistory(urn2);
        insertTrackWithPlayHistory(urn3);

        // played on another device after sync
        insertTrackWithPlayHistory(urn3, 6000L);
        insertTrackWithPlayHistory(urn2, 4000L);
        insertTrackWithPlayHistory(urn1, 5000L);

        storage.loadTrackUrns(10).test()
               .assertValueCount(1)
               .assertValue(Arrays.asList(urn3, urn1, urn2));
    }

    @Test
    public void loadTracksReturnsTracks() {
        insertTrackWithPlayHistory(urn1);

        Urn actual = storage.loadTrackUrns(10).test().values().get(0).get(0);

        assertThat(actual).isEqualTo(urn1);
    }

    @Test
    public void loadUnSyncedPlayHistoryReturnsOnlyUnsynced() {
        insertUnsyncedPlayHistory(urn1, 1000L);

        insertSyncedPlayHistory(urn1, 2000L);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadUnSynced();

        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(urn1);
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(1000L);
    }

    @Test
    public void loadSyncedPlayHistoryReturnsOnlySynced() {
        insertUnsyncedPlayHistory(urn1, 1000L);
        insertSyncedPlayHistory(urn1, 2000L);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadSynced();

        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(urn1);
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(2000L);
    }

    @Test
    public void removePlayHistoryRemovesEntries() {
        insertTrackWithPlayHistory(urn1);
        insertTrackWithPlayHistory(urn2);
        insertTrackWithPlayHistory(urn3);

        storage.removeAll(Collections.singletonList(PlayHistoryRecord.create(urn2.getNumericId(), urn2, Urn.NOT_SET)));

        final List<Urn> existingTracks = storage.loadTrackUrns(10).test()
                                                .assertValueCount(1)
                                                .values().get(0);

        assertThat(existingTracks.size()).isEqualTo(2);

        assertThat(urn3).isEqualTo(existingTracks.get(0));
        assertThat(urn1).isEqualTo(existingTracks.get(1));
    }

    @Test
    public void insertPlayHistoryAddsEntriesAsSynced() {
        insertSyncedPlayHistory(urn1, 1000L);
        insertSyncedPlayHistory(urn2, 2000L);

        storage.insert(Collections.singletonList(
                PlayHistoryRecord.create(3000L, urn1, Urn.NOT_SET)));

        assertThat(storage.loadSynced().size()).isEqualTo(3);
    }

    @Test
    public void loadPlayHistoryForPlaybackGetsOnlyUrnsWithoutDuplicates() {
        insertTrackWithPlayHistory(urn1);
        insertTrackWithPlayHistory(urn2);

        insertSyncedPlayHistory(urn1, 3000L);

        storage.loadTrackUrnsForPlayback().test()
               .assertValueCount(1)
               .assertValues(Arrays.asList(urn1, urn2));
    }

    @Test
    public void clearClearsTable() {
        insertUnsyncedPlayHistory(urn1, 1500L);
        insertSyncedPlayHistory(urn2, 2000L);

        storage.clear();

        storage.loadTrackUrnsForPlayback().test()
               .assertValueCount(1)
               .assertValue(Collections.emptyList());
    }

    @Test
    public void hasPendingTracksToSync() throws Exception {
        insertUnsyncedPlayHistory(urn1, 1000L);
        assertThat(storage.hasPendingItemsToSync()).isTrue();
    }

    @Test
    public void hasNoPendingTracksToSync() throws Exception {
        insertSyncedPlayHistory(urn1, 1000L);
        assertThat(storage.hasPendingItemsToSync()).isFalse();
    }

    @Test
    public void insertsNewPlayHistory() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET);

        storage.upsertRow(record);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadUnSynced();
        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(Urn.forTrack(123L));
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(1000L);
    }

    @Test
    public void insertsMultipleTimesTheSamePlayHistoryWithDifferentTimestamp() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET);
        storage.upsertRow(record);

        PlayHistoryRecord record2 = PlayHistoryRecord.create(2000L, Urn.forTrack(123L), Urn.NOT_SET);
        storage.upsertRow(record2);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadUnSynced();
        assertThat(playHistoryRecords.size()).isEqualTo(2);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(Urn.forTrack(123L));
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(1000L);
        assertThat(playHistoryRecords.get(1).trackUrn()).isEqualTo(Urn.forTrack(123L));
        assertThat(playHistoryRecords.get(1).timestamp()).isEqualTo(2000L);
    }

    @Test
    public void insertsOnlyOnceWhenTimestampAlreadyExistsForSameTrack() {
        PlayHistoryRecord record = PlayHistoryRecord.create(1000L, Urn.forTrack(123L), Urn.NOT_SET);
        storage.upsertRow(record);
        storage.upsertRow(record);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadUnSynced();
        assertThat(playHistoryRecords.size()).isEqualTo(1);
        assertThat(playHistoryRecords.get(0).trackUrn()).isEqualTo(Urn.forTrack(123L));
        assertThat(playHistoryRecords.get(0).timestamp()).isEqualTo(1000L);
    }

    @Test
    public void trimsDatabaseToLimit() throws Exception {
        insertTrackWithPlayHistory(urn1);
        insertTrackWithPlayHistory(urn2);
        insertTrackWithPlayHistory(urn3);

        storage.trim(1);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadAll();
        assertThat(playHistoryRecords.size()).isEqualTo(1);
    }

    @Test
    public void shouldNotDeleteAnythingWhenAlreadyBelowLimit() throws Exception {
        insertTrackWithPlayHistory(urn1);
        insertTrackWithPlayHistory(urn2);
        insertTrackWithPlayHistory(urn3);

        storage.trim(10);

        final List<PlayHistoryRecord> playHistoryRecords = storage.loadAll();
        assertThat(playHistoryRecords.size()).isEqualTo(3);
    }

    private void insertTrackWithPlayHistory(Urn urn, long timestamp) {
        insertSyncedPlayHistory(urn, timestamp);
    }

    private void insertTrackWithPlayHistory(Urn urn) {
        insertSyncedPlayHistory(urn, urn.getNumericId());
    }

    public void insertSyncedPlayHistory(Urn urn, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(PlayHistoryModel.TIMESTAMP, timestamp);
        cv.put(PlayHistoryModel.TRACK_ID, urn.getNumericId());
        cv.put(PlayHistoryModel.SYNCED, 1);
        dbHelper.getWritableDatabase().insert(PlayHistoryModel.TABLE_NAME, PlayHistoryModel.SYNCED, cv);
    }

    public void insertUnsyncedPlayHistory(Urn urn, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(PlayHistoryModel.TIMESTAMP, timestamp);
        cv.put(PlayHistoryModel.TRACK_ID, urn.getNumericId());
        cv.put(PlayHistoryModel.SYNCED, 0);
        dbHelper.getWritableDatabase().insert(PlayHistoryModel.TABLE_NAME, PlayHistoryModel.SYNCED, cv);
    }

}
