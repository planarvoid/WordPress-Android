package com.soundcloud.android.offline

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.utils.TestDateProvider
import org.assertj.core.api.Assertions
import java.util.Collections
import java.util.concurrent.TimeUnit

fun TrackDownloadsStorage.insertTrackForPendingDownload(track: Urn) {
    val offlineContentUpdates = OfflineContentUpdates.builder()
            .newTracksToDownload(Collections.singletonList(track))
            .build()
    this.writeUpdates(offlineContentUpdates).test().assertComplete()
}

fun TrackDownloadsStorage.markTrackUnavailable(track: Urn) {
    val offlineContentUpdates = OfflineContentUpdates.builder()
            .unavailableTracks(Collections.singletonList(track))
            .build()
    this.writeUpdates(offlineContentUpdates).test().assertComplete()
}

fun TrackDownloadsStorage.updateTrackForRemoval(track: Urn) {
    val offlineContentUpdates = OfflineContentUpdates.builder()
            .tracksToRemove(Collections.singletonList(track))
            .build()
    this.writeUpdates(offlineContentUpdates).test().assertComplete()
}

fun TrackDownloadsStorage.assertDownloadResultsInserted(track: Urn) {
    this.getOfflineStates(Collections.singleton(track))
            .test()
            .assertValue(Collections.singletonMap(track, OfflineState.DOWNLOADED))
            .assertComplete()
}

fun TrackDownloadsStorage.assertDownloadUnavailable(track: Urn) {
    this.getOfflineStates(Collections.singleton(track))
            .test()
            .assertValue(Collections.singletonMap(track, OfflineState.UNAVAILABLE))
            .assertComplete()
}

fun TrackDownloadsStorage.markTrackDownloaded(track: Urn) {
    val downloadState = DownloadState.success(ModelFixtures.downloadRequestFromLikes(track))
    Assertions.assertThat(this.storeCompletedDownload(downloadState)).isTrue()
}

fun TrackDownloadsStorage.insertTrackPendingRemoval(urn: Urn, testDateProvider: TestDateProvider) {
    this.insertTrackForPendingDownload(urn)

    this.updateTrackForRemoval(urn)

    testDateProvider.advanceBy(4, TimeUnit.MINUTES)
}

fun TrackDownloadsStorage.insertDownloadedTrack(track: Urn) {

    this.insertTrackForPendingDownload(track)

    this.markTrackDownloaded(track)
}

