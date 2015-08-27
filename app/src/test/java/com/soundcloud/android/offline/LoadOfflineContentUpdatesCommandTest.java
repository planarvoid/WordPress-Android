package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadOfflineContentUpdatesCommandTest extends StorageIntegrationTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN_2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN_3 = Urn.forTrack(789L);

    private static final long DURATION = 12345L;
    private static final String WAVEFORM = "http://wav";

    private static DownloadRequest downloadRequest = new DownloadRequest(TRACK_URN_2, DURATION, WAVEFORM);


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

        final List<DownloadRequest> expectedRequests = Collections.singletonList(downloadRequest);
        final OfflineContentUpdates offlineContentUpdates = command.call(expectedRequests);

        assertThat(offlineContentUpdates.newRestoredRequests).containsExactly(downloadRequest);
        assertThat(offlineContentUpdates.allDownloadRequests).isEmpty();
        assertThat(offlineContentUpdates.newDownloadRequests).isEmpty();
        assertThat(offlineContentUpdates.newRemovedTracks).isEmpty();
    }

    @Test
    public void doesNotReturnPendingRemovalsRemovedAfter3Minutes() {
        actualPendingRemovals(TRACK_URN_1, dateProvider.getCurrentTime() - TimeUnit.MINUTES.toMillis(4));

        final List<DownloadRequest> expectedRequests = Collections.singletonList(downloadRequest);
        final OfflineContentUpdates offlineContentUpdates = command.call(expectedRequests);

        assertThat(offlineContentUpdates.newRestoredRequests).isEmpty();
        assertThat(offlineContentUpdates.allDownloadRequests).containsExactly(downloadRequest);
        assertThat(offlineContentUpdates.newDownloadRequests).containsExactly(downloadRequest);
        assertThat(offlineContentUpdates.newRemovedTracks).isEmpty();
    }

    @Test
    public void returnsPendingDownloadAsRemovedWhenItIsNoLongerRequested() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final List<DownloadRequest> expectedRequests = Collections.singletonList(downloadRequest);
        final OfflineContentUpdates offlineContentUpdates = command.call(expectedRequests);

        assertThat(offlineContentUpdates.newRemovedTracks).containsExactly(TRACK_URN_1);
        assertThat(offlineContentUpdates.allDownloadRequests).containsExactly(downloadRequest);
        assertThat(offlineContentUpdates.newDownloadRequests).isEmpty();
        assertThat(offlineContentUpdates.newRestoredRequests).isEmpty();
    }

    @Test
    public void returnsFilteredOutCreatorOptOuts() {
        final List<DownloadRequest> expectedRequests = new ArrayList<>(2);
        DownloadRequest creatorOptOut = new DownloadRequest.Builder(TRACK_URN_1, DURATION, WAVEFORM, false).build();
        DownloadRequest downloadRequest = new DownloadRequest.Builder(TRACK_URN_2, DURATION, WAVEFORM, true).build();

        expectedRequests.add(creatorOptOut);
        expectedRequests.add(downloadRequest);

        OfflineContentUpdates updates = command.call(expectedRequests);

        assertThat(updates.allDownloadRequests).contains(downloadRequest);
        assertThat(updates.creatorOptOutRequests).contains(creatorOptOut);
    }

    @Test
    public void returnsDownloadedTrackAsCreatorOptOutAfterPolicyChange() {
        final List<DownloadRequest> expectedRequest = new ArrayList<>(1);
        expectedRequest.add(new DownloadRequest.Builder(TRACK_URN_1, DURATION, WAVEFORM, false).build());
        actualDownloadedTracks(TRACK_URN_1);

        OfflineContentUpdates updates = command.call(expectedRequest);

        assertThat(updates.creatorOptOutRequests).contains(expectedRequest.get(0));
        assertThat(updates.newRemovedTracks).isEmpty();
    }

    @Test
    public void returnsNewAndExistingDownloadsWithNoRemovals() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final DownloadRequest downloadRequest1 = new DownloadRequest(TRACK_URN_1, DURATION, WAVEFORM);
        final DownloadRequest downloadRequest2 = new DownloadRequest(TRACK_URN_2, DURATION, WAVEFORM);
        final DownloadRequest downloadRequest3 = new DownloadRequest(TRACK_URN_3, DURATION, WAVEFORM);
        final List<DownloadRequest> expectedRequests = Arrays.asList(downloadRequest1, downloadRequest2, downloadRequest3);
        final OfflineContentUpdates offlineContentUpdates = command.call(expectedRequests);

        assertThat(offlineContentUpdates.newRemovedTracks).isEmpty();
        assertThat(offlineContentUpdates.allDownloadRequests).containsExactly(downloadRequest2, downloadRequest3);
        assertThat(offlineContentUpdates.newDownloadRequests).containsExactly(downloadRequest3);
        assertThat(offlineContentUpdates.newRestoredRequests).isEmpty();
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