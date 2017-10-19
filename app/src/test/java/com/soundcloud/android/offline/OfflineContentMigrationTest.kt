package com.soundcloud.android.offline

import android.content.ContentValues
import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.propeller.ContentValuesBuilder
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.robolectric.RuntimeEnvironment
import java.util.Arrays
import java.util.Collections

class OfflineContentMigrationTest : StorageIntegrationTest() {

    private @Mock lateinit var offlineServiceInitiator: OfflineServiceInitiator
    private val offlineContentStorage = OfflineContentStorage(sharedPreferences(), Schedulers.trampoline())
    private val trackDownloadsStorage = TrackDownloadsStorage(CurrentDateProvider(),
                                                              OfflineDatabase(OfflineDatabaseOpenHelper(RuntimeEnvironment.application), Schedulers.trampoline()))

    private val createOfflineContent = "CREATE TABLE IF NOT EXISTS OfflineContent (" +
            "_id INTEGER," +
            "_type INTEGER," +
            "PRIMARY KEY (_id, _type)," +
            "FOREIGN KEY(_id, _type) REFERENCES Sounds(_id, _type)" +
            ");"

    private val createTrackDownloadsContent = "CREATE TABLE IF NOT EXISTS TrackDownloads (" +
            "_id INTEGER PRIMARY KEY," +
            "requested_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
            "downloaded_at INTEGER DEFAULT NULL," +
            "removed_at INTEGER DEFAULT NULL," +
            "unavailable_at INTEGER DEFAULT NULL" +
            ");"

    private lateinit var migration: OfflineContentMigration;

    @Before
    fun setup() {
        migration = OfflineContentMigration(offlineServiceInitiator, offlineContentStorage, trackDownloadsStorage, this.database())
    }

    @Test
    fun handlesNotHavingOfflineTablesToMigrate() {
        migration.applyMigration()
    }

    @Test
    fun keepsOfflineContentAfterMigration() {
        createTables()

        insertLikesMarkedForOfflineSync()
        val playlist = insertPlaylistMarkedForOfflineSync()

        migration.applyMigration()

        offlineContentStorage.isOfflinePlaylist(playlist.urn).test().assertValue(true).assertComplete()
        offlineContentStorage.isOfflineLikesEnabled.test().assertValue(true).assertComplete()
    }

    @Test
    fun keepsTracksAfterMigration() {
        createTables()

        val pendingDownloadTrack = Urn.forTrack(1)
        insertTrackPendingDownload(pendingDownloadTrack, 1)

        val removalTrack = Urn.forTrack(2)
        insertTrackDownloadPendingRemoval(removalTrack, 0, 1)

        val completedTrack = Urn.forTrack(3)
        insertCompletedTrackDownload(completedTrack, 0, 1)

        val unavailableTrack = Urn.forTrack(4)
        insertUnavailableTrackDownload(unavailableTrack, 1)

        migration.applyMigration()

        val map = trackDownloadsStorage.getOfflineStates(Arrays.asList(
                pendingDownloadTrack, removalTrack, completedTrack, unavailableTrack
        )).blockingGet()

        assertThat(map.getOrDefault(pendingDownloadTrack, OfflineState.NOT_OFFLINE)).isEqualTo(OfflineState.REQUESTED);
        assertThat(map.getOrDefault(removalTrack, OfflineState.NOT_OFFLINE)).isEqualTo(OfflineState.NOT_OFFLINE);
        assertThat(map.getOrDefault(completedTrack, OfflineState.NOT_OFFLINE)).isEqualTo(OfflineState.DOWNLOADED);
        assertThat(map.getOrDefault(unavailableTrack, OfflineState.NOT_OFFLINE)).isEqualTo(OfflineState.UNAVAILABLE);

        trackDownloadsStorage.tracksToRemove.test().assertValue(Collections.singletonList(removalTrack))
    }

    @Test
    fun startsSyncServiceAfterMigration() {
        createTables()

        insertLikesMarkedForOfflineSync()
        insertPlaylistMarkedForOfflineSync()

        migration.applyMigration()

        verify(offlineServiceInitiator).start()
    }

    private fun createTables() {
        database().execSQL(createOfflineContent)
        database().execSQL(createTrackDownloadsContent)
    }

    private fun insertPlaylistMarkedForOfflineSync(): ApiPlaylist {
        val apiPlaylist = testFixtures().insertPlaylist()
        insertPlaylistMarkedForOfflineSync(apiPlaylist)
        return apiPlaylist
    }

    private fun insertPlaylistMarkedForOfflineSync(playlist: ApiPlaylist) {
        val cv = ContentValuesBuilder.values()
        cv.put("_id", playlist.urn.numericId)
        cv.put("_type", 1)
        testFixtures().insertInto("OfflineContent", cv.get())
    }

    private fun insertLikesMarkedForOfflineSync() {
        val cv = ContentValuesBuilder.values()
        cv.put("_id", 0)
        cv.put("_type", 2)
        testFixtures().insertInto("OfflineContent", cv.get())
    }

    private fun insertTrackPendingDownload(trackUrn: Urn, requestedAt: Long) {
        val cv = ContentValues()
        cv.put("_id", trackUrn.numericId)
        cv.put("requested_at", requestedAt)
        testFixtures().insertInto("TrackDownloads", cv)
    }

    private fun insertCompletedTrackDownload(trackUrn: Urn, requestedAtTimestamp: Long, completedTimestamp: Long) {
        val cv = ContentValuesBuilder.values()
        cv.put("_id", trackUrn.numericId)
        cv.put("requested_at", requestedAtTimestamp)
        cv.put("downloaded_at", completedTimestamp)
        testFixtures().insertInto("TrackDownloads", cv.get())
    }

    private fun insertUnavailableTrackDownload(trackUrn: Urn, unavailableTimestamp: Long) {
        val cv = ContentValuesBuilder.values()
        cv.put("_id", trackUrn.numericId)
        cv.put("requested_at", unavailableTimestamp - 1)
        cv.put("unavailable_at", unavailableTimestamp)
        testFixtures().insertInto("TrackDownloads", cv.get())
    }

    private fun insertTrackDownloadPendingRemoval(trackUrn: Urn, requestedAtTimestamp: Long, removedAtTimestamp: Long) {
        val cv = ContentValuesBuilder.values()
        cv.put("_id", trackUrn.numericId)
        cv.put("requested_at", requestedAtTimestamp)
        cv.put("downloaded_at", requestedAtTimestamp)
        cv.put("removed_at", removedAtTimestamp)
        testFixtures().insertInto("TrackDownloads", cv.get())
    }
}
