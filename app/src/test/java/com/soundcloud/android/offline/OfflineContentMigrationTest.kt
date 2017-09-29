package com.soundcloud.android.offline

import android.content.ContentValues
import com.nhaarman.mockito_kotlin.verify
import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables.OfflineContent
import com.soundcloud.android.storage.Tables.TrackDownloads
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

    private lateinit var migration: OfflineContentMigration;

    @Before
    fun setup() {
        migration = OfflineContentMigration(offlineServiceInitiator, offlineContentStorage, trackDownloadsStorage, this.database())
    }

    @Test
    fun keepsOfflineContentAfterMigration() {
        insertLikesMarkedForOfflineSync()
        val playlist = insertPlaylistMarkedForOfflineSync()

        migration.applyMigration()

        offlineContentStorage.isOfflinePlaylist(playlist.urn).test().assertValue(true).assertComplete()
        offlineContentStorage.isOfflineLikesEnabled.test().assertValue(true).assertComplete()
    }

    @Test
    fun keepsTracksAfterMigration() {
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
        insertLikesMarkedForOfflineSync()
        insertPlaylistMarkedForOfflineSync()

        migration.applyMigration()

        verify(offlineServiceInitiator).start()
    }

    fun insertPlaylistMarkedForOfflineSync(): ApiPlaylist {
        val apiPlaylist = testFixtures().insertPlaylist()
        insertPlaylistMarkedForOfflineSync(apiPlaylist)
        return apiPlaylist
    }

    fun insertPlaylistMarkedForOfflineSync(playlist: ApiPlaylist) {
        val cv = ContentValuesBuilder.values()
        cv.put(OfflineContent._ID, playlist.urn.numericId)
        cv.put(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)
        testFixtures().insertInto(OfflineContent.TABLE, cv.get())
    }

    fun insertLikesMarkedForOfflineSync() {
        val cv = ContentValuesBuilder.values()
        cv.put(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
        cv.put(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION)
        testFixtures().insertInto(OfflineContent.TABLE, cv.get())
    }

    fun insertTrackPendingDownload(trackUrn: Urn, requestedAt: Long) {
        val cv = ContentValues()
        cv.put(TrackDownloads._ID.name(), trackUrn.numericId)
        cv.put(TrackDownloads.REQUESTED_AT.name(), requestedAt)
        testFixtures().insertInto(TrackDownloads.TABLE, cv)
    }

    fun insertCompletedTrackDownload(trackUrn: Urn, requestedAtTimestamp: Long, completedTimestamp: Long) {
        val cv = ContentValuesBuilder.values()
        cv.put(TrackDownloads._ID, trackUrn.numericId)
        cv.put(TrackDownloads.REQUESTED_AT, requestedAtTimestamp)
        cv.put(TrackDownloads.DOWNLOADED_AT, completedTimestamp)
        testFixtures().insertInto(TrackDownloads.TABLE, cv.get())
    }

    fun insertUnavailableTrackDownload(trackUrn: Urn, unavailableTimestamp: Long) {
        val cv = ContentValuesBuilder.values()
        cv.put(TrackDownloads._ID, trackUrn.numericId)
        cv.put(TrackDownloads.REQUESTED_AT, unavailableTimestamp - 1)
        cv.put(TrackDownloads.UNAVAILABLE_AT, unavailableTimestamp)
        testFixtures().insertInto(TrackDownloads.TABLE, cv.get())
    }

    fun insertTrackDownloadPendingRemoval(trackUrn: Urn, removedAtTimestamp: Long) {
        insertTrackDownloadPendingRemoval(trackUrn, 0, removedAtTimestamp)
    }

    fun insertTrackDownloadPendingRemoval(trackUrn: Urn, requestedAtTimestamp: Long, removedAtTimestamp: Long) {
        val cv = ContentValuesBuilder.values()
        cv.put(TrackDownloads._ID, trackUrn.numericId)
        cv.put(TrackDownloads.REQUESTED_AT, requestedAtTimestamp)
        cv.put(TrackDownloads.DOWNLOADED_AT, requestedAtTimestamp)
        cv.put(TrackDownloads.REMOVED_AT, removedAtTimestamp)
        testFixtures().insertInto(TrackDownloads.TABLE, cv.get())
    }

}
