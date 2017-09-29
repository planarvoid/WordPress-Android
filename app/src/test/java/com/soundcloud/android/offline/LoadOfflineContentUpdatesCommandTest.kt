package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.utils.TestDateProvider
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Arrays

class LoadOfflineContentUpdatesCommandTest : AndroidUnitTest() {

    private lateinit var command: LoadOfflineContentUpdatesCommand
    private val dateProvider = TestDateProvider()
    private val trackDownloadsStorage = TrackDownloadsStorage(dateProvider, OfflineDatabase(OfflineDatabaseOpenHelper(AndroidUnitTest.context()), Schedulers.trampoline()))

    @Before
    fun setUp() {
        command = LoadOfflineContentUpdatesCommand(trackDownloadsStorage)
    }

    @Test
    fun returnsPendingRemovalAsTrackToRestoreWhenItIsRequested() {
        actualDownloadedTracks(TRACK_URN_2)
        actualPendingRemovals(TRACK_URN_1)
        actualPendingRemovals(TRACK_URN_2)

        val offlineContentUpdates = command.call(createExpectedContent(downloadRequest))

        assertThat(offlineContentUpdates.tracksToRestore()).containsExactly(downloadRequest.urn)
        assertThat(offlineContentUpdates.tracksToDownload()).isEmpty()
        assertThat(offlineContentUpdates.newTracksToDownload()).isEmpty()
        assertThat(offlineContentUpdates.tracksToRemove()).isEmpty()
    }

    @Test
    fun doesNotReturnPendingRemovalsRemovedAfter3Minutes() {
        actualPendingRemovals(TRACK_URN_1);

        val offlineContentUpdates = command.call(createExpectedContent(downloadRequest))

        assertThat(offlineContentUpdates.tracksToRestore()).isEmpty()
        assertThat(offlineContentUpdates.tracksToDownload()).containsExactly(downloadRequest)
        assertThat(offlineContentUpdates.newTracksToDownload()).containsExactly(downloadRequest.urn)
        assertThat(offlineContentUpdates.tracksToRemove()).isEmpty()
    }

    @Test
    fun returnsPendingDownloadAsRemovedWhenItIsNoLongerRequested() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        val offlineContentUpdates = command.call(createExpectedContent(downloadRequest))

        assertThat(offlineContentUpdates.tracksToRemove()).containsExactly(TRACK_URN_1)
        assertThat(offlineContentUpdates.tracksToDownload()).containsExactly(downloadRequest)
        assertThat(offlineContentUpdates.newTracksToDownload()).isEmpty()
        assertThat(offlineContentUpdates.tracksToRestore()).isEmpty()
    }

    @Test
    fun returnsFilteredOutCreatorOptOuts() {
        val creatorOptOut = ModelFixtures.creatorOptOutRequest(TRACK_URN_1)
        val downloadRequest = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2)

        val updates = command.call(createExpectedContent(creatorOptOut, downloadRequest))

        assertThat(updates.tracksToDownload()).contains(downloadRequest)
        assertThat(updates.unavailableTracks()).contains(creatorOptOut.urn)
    }

    @Test
    fun returnsFilteredOutSnippets() {
        val snippet = ModelFixtures.snippetRequest(TRACK_URN_1)
        val downloadRequest = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2)

        val updates = command.call(createExpectedContent(snippet, downloadRequest))

        assertThat(updates.tracksToDownload()).contains(downloadRequest)
        assertThat(updates.unavailableTracks()).contains(snippet.urn)
    }

    @Test
    fun returnsDownloadedTrackAsCreatorOptOutAfterPolicyChange() {
        val creatorOptOutRequest = ModelFixtures.creatorOptOutRequest(TRACK_URN_1)
        actualDownloadedTracks(TRACK_URN_1);

        val updates = command.call(createExpectedContent(creatorOptOutRequest))

        assertThat(updates.unavailableTracks()).contains(creatorOptOutRequest.urn)
        assertThat(updates.tracksToRemove()).isEmpty()
    }

    @Test
    fun returnsNewAndExistingDownloadsWithNoRemovals() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        val downloadRequest1 = ModelFixtures.downloadRequestFromLikes(TRACK_URN_1)
        val downloadRequest2 = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2)
        val downloadRequest3 = ModelFixtures.downloadRequestFromLikes(TRACK_URN_3)
        val expectedRequests = createExpectedContent(downloadRequest1,
                                                     downloadRequest2,
                                                     downloadRequest3)
        val offlineContentUpdates = command.call(expectedRequests)

        assertThat(offlineContentUpdates.tracksToRemove()).isEmpty()
        assertThat(offlineContentUpdates.tracksToDownload()).containsExactly(downloadRequest2, downloadRequest3)
        assertThat(offlineContentUpdates.newTracksToDownload()).containsExactly(downloadRequest3.urn)
        assertThat(offlineContentUpdates.tracksToRestore()).isEmpty()
    }

    private fun createExpectedContent(vararg downloadRequest: DownloadRequest): ExpectedOfflineContent {
        return ExpectedOfflineContent(Arrays.asList(*downloadRequest),
                                      emptyList(),
                                      false,
                                      emptyList())
    }

    companion object {

        private val TRACK_URN_1 = Urn.forTrack(123L)
        private val TRACK_URN_2 = Urn.forTrack(456L)
        private val TRACK_URN_3 = Urn.forTrack(789L)

        private val downloadRequest = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2)
    }

    fun actualPendingRemovals(track: Urn) {
        trackDownloadsStorage.insertTrackPendingRemoval(track, dateProvider)
    }

    fun actualDownloadRequests(vararg tracks: Urn) {
        for (track in tracks) {
            trackDownloadsStorage.insertTrackForPendingDownload(track)
        }
    }

    fun actualDownloadedTracks(track: Urn) {
        trackDownloadsStorage.insertDownloadedTrack(track)
    }
}
