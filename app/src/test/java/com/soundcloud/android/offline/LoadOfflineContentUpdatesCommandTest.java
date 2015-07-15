package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadOfflineContentUpdatesCommandTest extends StorageIntegrationTest {

    private static final Urn TRACK_URN_1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN_2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN_3 = Urn.forTrack(789L);
    private static final long TRACK_DURATION = 12345L;

    @Mock private DateProvider dateProvider;

    private LoadOfflineContentUpdatesCommand command;
    private Date now;

    @Before
    public void setUp() {
        now = new Date();
        when(dateProvider.getCurrentDate()).thenReturn(now);
        command = new LoadOfflineContentUpdatesCommand(propeller(), dateProvider);
    }

    @Test
    public void returnsPendingRemovalAsTrackToRestoreWhenItIsRequested() {
        actualDownloadedTracks(TRACK_URN_2);
        actualPendingRemovals(TRACK_URN_1, now.getTime());
        actualPendingRemovals(TRACK_URN_2, now.getTime());

        final List<DownloadRequest> expectedRequests = Collections.singletonList(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        assertThat(offlineContentRequests.newRestoredRequests).containsExactly(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        assertThat(offlineContentRequests.allDownloadRequests).isEmpty();
        assertThat(offlineContentRequests.newDownloadRequests).isEmpty();
        assertThat(offlineContentRequests.newRemovedTracks).isEmpty();
    }

    @Test
    public void doesNotReturnPendingRemovalsRemovedAfter3Minutes() {
        actualPendingRemovals(TRACK_URN_1, now.getTime() - TimeUnit.MINUTES.toMillis(4));

        final List<DownloadRequest> expectedRequests = Collections.singletonList(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        assertThat(offlineContentRequests.newRestoredRequests).isEmpty();
        assertThat(offlineContentRequests.allDownloadRequests).containsExactly(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        assertThat(offlineContentRequests.newDownloadRequests).containsExactly(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        assertThat(offlineContentRequests.newRemovedTracks).isEmpty();
    }

    @Test
    public void returnsPendingDownloadAsRemovedWhenItIsNoLongerRequested() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final DownloadRequest downloadRequest = new DownloadRequest(TRACK_URN_2, TRACK_DURATION);
        final List<DownloadRequest> expectedRequests = Collections.singletonList(downloadRequest);
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        assertThat(offlineContentRequests.newRemovedTracks).containsExactly(TRACK_URN_1);
        assertThat(offlineContentRequests.allDownloadRequests).containsExactly(downloadRequest);
        assertThat(offlineContentRequests.newDownloadRequests).isEmpty();
        assertThat(offlineContentRequests.newRestoredRequests).isEmpty();
    }

    @Test
    public void returnsNewAndExistingDownloadsWithNoRemovals() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final DownloadRequest downloadRequest1 = new DownloadRequest(TRACK_URN_1, TRACK_DURATION);
        final DownloadRequest downloadRequest2 = new DownloadRequest(TRACK_URN_2, TRACK_DURATION);
        final DownloadRequest downloadRequest3 = new DownloadRequest(TRACK_URN_3, TRACK_DURATION);
        final List<DownloadRequest> expectedRequests = Arrays.asList(downloadRequest1, downloadRequest2, downloadRequest3);
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        assertThat(offlineContentRequests.newRemovedTracks).isEmpty();
        assertThat(offlineContentRequests.allDownloadRequests).containsExactly(downloadRequest2, downloadRequest3);
        assertThat(offlineContentRequests.newDownloadRequests).containsExactly(downloadRequest3);
        assertThat(offlineContentRequests.newRestoredRequests).isEmpty();
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