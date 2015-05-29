package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineContentRequests;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
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

        final List<DownloadRequest> expectedRequests = Arrays.asList(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        expect(offlineContentRequests.newRestoredRequests).toContainExactly(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        expect(offlineContentRequests.allDownloadRequests).toBeEmpty();
        expect(offlineContentRequests.newDownloadRequests).toBeEmpty();
        expect(offlineContentRequests.newRemovedTracks).toBeEmpty();
    }

    @Test
    public void doesNotReturnPendingRemovalsRemovedAfter3Minutes() {
        actualPendingRemovals(TRACK_URN_1, now.getTime() - TimeUnit.MINUTES.toMillis(4));

        final List<DownloadRequest> expectedRequests = Arrays.asList(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        expect(offlineContentRequests.newRestoredRequests).toBeEmpty();
        expect(offlineContentRequests.allDownloadRequests).toContainExactly(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        expect(offlineContentRequests.newDownloadRequests).toContainExactly(new DownloadRequest(TRACK_URN_2, TRACK_DURATION));
        expect(offlineContentRequests.newRemovedTracks).toBeEmpty();
    }

    @Test
    public void returnsPendingDownloadAsRemovedWhenItIsNoLongerRequested() {
        actualDownloadedTracks(TRACK_URN_1);
        actualDownloadRequests(TRACK_URN_2);

        final DownloadRequest downloadRequest = new DownloadRequest(TRACK_URN_2, TRACK_DURATION);
        final List<DownloadRequest> expectedRequests = Arrays.asList(downloadRequest);
        final OfflineContentRequests offlineContentRequests = command.call(expectedRequests);

        expect(offlineContentRequests.newRemovedTracks).toContainExactly(TRACK_URN_1);
        expect(offlineContentRequests.allDownloadRequests).toContainExactly(downloadRequest);
        expect(offlineContentRequests.newDownloadRequests).toBeEmpty();
        expect(offlineContentRequests.newRestoredRequests).toBeEmpty();
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

        expect(offlineContentRequests.newRemovedTracks).toBeEmpty();
        expect(offlineContentRequests.allDownloadRequests).toContainExactly(downloadRequest2, downloadRequest3);
        expect(offlineContentRequests.newDownloadRequests).toContainExactly(downloadRequest3);
        expect(offlineContentRequests.newRestoredRequests).toBeEmpty();
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