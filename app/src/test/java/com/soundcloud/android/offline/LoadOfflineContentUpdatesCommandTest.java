package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class LoadOfflineContentUpdatesCommandTest extends StorageIntegrationTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN_2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN_3 = Urn.forTrack(789L);

    private static DownloadRequest downloadRequest = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2);


    private LoadOfflineContentUpdatesCommand command;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() {
        dateProvider = new TestDateProvider();
        command = new LoadOfflineContentUpdatesCommand(propeller(), dateProvider);
    }

    @Test
    public void returnsPendingRemovalAsTrackToRestoreWhenItIsRequested() {
        actualDownloadedTracks(TRACK_URN_2);
        actualPendingRemovals(TRACK_URN_1, dateProvider.getCurrentTime());
        actualPendingRemovals(TRACK_URN_2, dateProvider.getCurrentTime());

        final OfflineContentUpdates offlineContentUpdates = command.call(createExpectedContent(downloadRequest));

        assertThat(offlineContentUpdates.tracksToRestore()).containsExactly(downloadRequest.getUrn());
        assertThat(offlineContentUpdates.tracksToDownload()).isEmpty();
        assertThat(offlineContentUpdates.newTracksToDownload()).isEmpty();
        assertThat(offlineContentUpdates.tracksToRemove()).isEmpty();
    }

    @Test
    public void doesNotReturnPendingRemovalsRemovedAfter3Minutes() {
        actualPendingRemovals(TRACK_URN_1, dateProvider.getCurrentTime() - TimeUnit.MINUTES.toMillis(4));

        final OfflineContentUpdates offlineContentUpdates = command.call(createExpectedContent(downloadRequest));

        assertThat(offlineContentUpdates.tracksToRestore()).isEmpty();
        assertThat(offlineContentUpdates.tracksToDownload()).containsExactly(downloadRequest);
        assertThat(offlineContentUpdates.newTracksToDownload()).containsExactly(downloadRequest.getUrn());
        assertThat(offlineContentUpdates.tracksToRemove()).isEmpty();
    }

    @Test
    public void returnsPendingDownloadAsRemovedWhenItIsNoLongerRequested() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final OfflineContentUpdates offlineContentUpdates = command.call(createExpectedContent(downloadRequest));

        assertThat(offlineContentUpdates.tracksToRemove()).containsExactly(TRACK_URN_1);
        assertThat(offlineContentUpdates.tracksToDownload()).containsExactly(downloadRequest);
        assertThat(offlineContentUpdates.newTracksToDownload()).isEmpty();
        assertThat(offlineContentUpdates.tracksToRestore()).isEmpty();
    }

    @Test
    public void returnsFilteredOutCreatorOptOuts() {
        DownloadRequest creatorOptOut = ModelFixtures.creatorOptOutRequest(TRACK_URN_1);
        DownloadRequest downloadRequest = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2);


        OfflineContentUpdates updates = command.call(createExpectedContent(creatorOptOut, downloadRequest));

        assertThat(updates.tracksToDownload()).contains(downloadRequest);
        assertThat(updates.unavailableTracks()).contains(creatorOptOut.getUrn());
    }

    @Test
    public void returnsFilteredOutSnippets() {
        DownloadRequest snippet = ModelFixtures.snippetRequest(TRACK_URN_1);
        DownloadRequest downloadRequest = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2);

        OfflineContentUpdates updates = command.call(createExpectedContent(snippet, downloadRequest));

        assertThat(updates.tracksToDownload()).contains(downloadRequest);
        assertThat(updates.unavailableTracks()).contains(snippet.getUrn());
    }

    @Test
    public void returnsDownloadedTrackAsCreatorOptOutAfterPolicyChange() {
        final DownloadRequest creatorOptOutRequest = ModelFixtures.creatorOptOutRequest(TRACK_URN_1);
        actualDownloadedTracks(TRACK_URN_1);

        OfflineContentUpdates updates = command.call(createExpectedContent(creatorOptOutRequest));

        assertThat(updates.unavailableTracks()).contains(creatorOptOutRequest.getUrn());
        assertThat(updates.tracksToRemove()).isEmpty();
    }

    @Test
    public void returnsNewAndExistingDownloadsWithNoRemovals() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final DownloadRequest downloadRequest1 = ModelFixtures.downloadRequestFromLikes(TRACK_URN_1);
        final DownloadRequest downloadRequest2 = ModelFixtures.downloadRequestFromLikes(TRACK_URN_2);
        final DownloadRequest downloadRequest3 = ModelFixtures.downloadRequestFromLikes(TRACK_URN_3);
        final ExpectedOfflineContent expectedRequests = createExpectedContent(downloadRequest1,
                                                                              downloadRequest2,
                                                                              downloadRequest3);
        final OfflineContentUpdates offlineContentUpdates = command.call(expectedRequests);

        assertThat(offlineContentUpdates.tracksToRemove()).isEmpty();
        assertThat(offlineContentUpdates.tracksToDownload()).containsExactly(downloadRequest2, downloadRequest3);
        assertThat(offlineContentUpdates.newTracksToDownload()).containsExactly(downloadRequest3.getUrn());
        assertThat(offlineContentUpdates.tracksToRestore()).isEmpty();
    }

    private ExpectedOfflineContent createExpectedContent(DownloadRequest... downloadRequest) {
        return new ExpectedOfflineContent(Arrays.asList(downloadRequest),
                                          Collections.emptyList(),
                                          false,
                                          Collections.emptyList());
    }

    private void actualPendingRemovals(Urn track, long remoteAt) {
        testFixtures().insertTrackDownloadPendingRemoval(track, remoteAt);
    }

    private void actualDownloadRequests(Urn... tracks) {
        final long now = System.currentTimeMillis();
        for (Urn track : tracks) {
            testFixtures().insertTrackPendingDownload(track, now);
        }
    }

    private void actualDownloadedTracks(Urn... tracks) {
        final long now = System.currentTimeMillis();
        for (Urn track : tracks) {
            testFixtures().insertCompletedTrackDownload(track, 0, now);
        }
    }
}
