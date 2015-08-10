package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

public class StoreDownloadUpdatesCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final DownloadRequest request = new DownloadRequest(TRACK, 12345L, "http://wav");
    private static final long NOW = System.currentTimeMillis();

    private StoreDownloadUpdatesCommand command;

    @Before
    public void setup() {
        command = new StoreDownloadUpdatesCommand(propeller());
    }

    @Test
    public void storeRemovedTracksAsPendingRemoval() {
        testFixtures().insertCompletedTrackDownload(TRACK, 0, NOW);

        final OfflineContentUpdates offlineContentUpdates = getOfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(TRACK)
        );

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadPendingRemoval(TRACK);
    }

    @Test
    public void storesNewDownloadRequestsAsPendingDownload() {
        final OfflineContentUpdates offlineContentUpdates = getOfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(request),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadRequestsInserted(Collections.singletonList(TRACK));
    }

    @Test
    public void storesRestoredRequestsAsDownloaded() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK, 1L, 2L);
        final OfflineContentUpdates offlineContentUpdates = getOfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(request),
                Collections.<Urn>emptyList()
        );

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadedAndNotMarkedForRemoval(TRACK);
    }

    private OfflineContentUpdates getOfflineContentRequests(List<DownloadRequest> allDownloadRequests,
                                                             List<DownloadRequest> newDownloadRequests,
                                                             List<DownloadRequest> newRestoredRequests,
                                                             List<Urn> toRemove) {
        return new OfflineContentUpdates(allDownloadRequests, newDownloadRequests, newRestoredRequests, toRemove);
    }
}
