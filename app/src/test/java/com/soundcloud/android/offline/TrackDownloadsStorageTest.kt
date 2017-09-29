package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.utils.TestDateProvider
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.TimeUnit

class TrackDownloadsStorageTest : StorageIntegrationTest() {

    private lateinit var offlineDatabaseOpenHelper: OfflineDatabaseOpenHelper
    private lateinit var offlineDatabase: OfflineDatabase
    private lateinit var storage: TrackDownloadsStorage
    private val dateProvider: TestDateProvider = TestDateProvider()

    @Before
    fun setup() {
        offlineDatabaseOpenHelper = OfflineDatabaseOpenHelper(RuntimeEnvironment.application)
        offlineDatabase = OfflineDatabase(offlineDatabaseOpenHelper, Schedulers.trampoline())
        storage = TrackDownloadsStorage(dateProvider, offlineDatabase)
    }

    @Test
    fun failsToUpdateDownloadedTrackIfNotPending() {
        val downloadState = DownloadState.success(ModelFixtures.downloadRequestFromLikes(TRACK_1))
        assertThat(storage.storeCompletedDownload(downloadState)).isFalse()
    }

    @Test
    fun getTracksToRemoveReturnsTrackPendingRemovalSinceAtLeast3Minutes() {
        storage.insertTrackPendingRemoval(TRACK_1, dateProvider)

        storage.tracksToRemove.test().assertValue(listOf(TRACK_1)).assertComplete()
    }

    @Test
    fun updatesDownloadTracksWithDownloadResults() {
        storage.insertDownloadedTrack(TRACK_1)

        storage.assertDownloadResultsInserted(TRACK_1)
    }

    @Test
    fun markTrackUnavailableFailsWithoutPendingDownloadInsertsTrack() {
        val offlineContentUpdates = OfflineContentUpdates.builder()
                .unavailableTracks(Collections.singletonList(TRACK_1))
                .build()

        storage.writeUpdates(offlineContentUpdates).test().assertComplete()
    }

    @Test
    fun marksTrackUnavailableAfterPendingDownload() {
        storage.insertTrackForPendingDownload(TRACK_1)

        dateProvider.advanceBy(1, TimeUnit.MILLISECONDS)

        storage.markTrackUnavailable(TRACK_1)

        storage.assertDownloadUnavailable(TRACK_1)
    }

    @Test
    fun resetUnavailableAtWhenDownloaded() {
        storage.insertTrackForPendingDownload(TRACK_1)

        dateProvider.advanceBy(1, TimeUnit.MILLISECONDS)

        storage.markTrackUnavailable(TRACK_1)

        storage.markTrackDownloaded(TRACK_1)

        storage.assertDownloadResultsInserted(TRACK_1)
    }


    @Test
    fun returnsDownloadStatesForAllTracks() {
        storage.insertTrackForPendingDownload(TRACK_1)
        storage.insertTrackForPendingDownload(TRACK_2)
        storage.insertTrackForPendingDownload(TRACK_3)
        storage.insertTrackForPendingDownload(TRACK_4)

        dateProvider.advanceBy(1, TimeUnit.MILLISECONDS)

        storage.markTrackUnavailable(TRACK_2)
        storage.updateTrackForRemoval(TRACK_3)
        storage.markTrackDownloaded(TRACK_4)

        val subscriber = storage.offlineStates().test()

        val expectedStates = HashMap<Urn, OfflineState>()
        expectedStates.put(TRACK_1, OfflineState.REQUESTED)
        expectedStates.put(TRACK_2, OfflineState.UNAVAILABLE)
        expectedStates.put(TRACK_3, OfflineState.NOT_OFFLINE)
        expectedStates.put(TRACK_4, OfflineState.DOWNLOADED)

        subscriber.assertValue(expectedStates)
    }

    @Test
    fun returnsFirstTrackUrnAvailableOffline() {
        storage.insertTrackForPendingDownload(TRACK_1)
        storage.insertTrackForPendingDownload(TRACK_2)

        dateProvider.advanceBy(1, TimeUnit.MILLISECONDS)

        storage.markTrackDownloaded(TRACK_2)

        val offlineTracks = storage.onlyOfflineTracks(Arrays.asList(TRACK_1, TRACK_2))
        assertThat(offlineTracks).containsExactly(TRACK_2)
    }

    @Test
    fun returnsUrnNotSetWhenTracksNotAvailableOffline() {
        storage.insertTrackForPendingDownload(TRACK_1)
        storage.insertTrackForPendingDownload(TRACK_2)

        val offlineTracks = storage.onlyOfflineTracks(Arrays.asList(TRACK_1, TRACK_2))
        assertThat(offlineTracks).isEmpty()
    }

    @Test // this tests that batching works
    fun onlyOfflineTracksHandlesHugePlaylists() {
        val bigPlaylist = 1002
        val tracks = ArrayList<Urn>()
        for (i in 0 until bigPlaylist) {
            val track = Urn.forTrack(i.toLong())
            tracks.add(track)

            storage.insertTrackForPendingDownload(track)
            dateProvider.advanceBy(1, TimeUnit.MILLISECONDS)
            storage.markTrackDownloaded(track)
        }

        val offlineTracks = storage.onlyOfflineTracks(tracks)
        assertThat(offlineTracks).containsOnlyElementsOf(tracks)
    }


    companion object {

        private val TRACK_1 = Urn.forTrack(12L)
        private val TRACK_2 = Urn.forTrack(34L)
        private val TRACK_3 = Urn.forTrack(56L)
        private val TRACK_4 = Urn.forTrack(78L)
    }

}
