package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineContentRequests;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class StoreDownloadUpdatesCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final long NOW = System.currentTimeMillis();

    private StoreDownloadUpdatesCommand command;

    @Before
    public void setup() {
        command = new StoreDownloadUpdatesCommand(propeller());
    }

    @Test
    public void storeRemovedTracksAsPendingRemoval() {
        testFixtures().insertCompletedTrackDownload(TRACK, 0, NOW);

        final OfflineContentRequests offlineContentRequests = getOfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(TRACK)
        );

        command.call(offlineContentRequests);

        databaseAssertions().assertDownloadPendingRemoval(TRACK);
    }

    @Test
    public void storesNewDownloadRequestsAsPendingDownload() {
        final OfflineContentRequests offlineContentRequests = getOfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(new DownloadRequest(TRACK, 12345L)),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList()
        );

        command.call(offlineContentRequests);

        databaseAssertions().assertDownloadRequestsInserted(Arrays.asList(TRACK));
    }

    @Test
    public void storesRestoredRequestsAsDownloaded() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK, 1L, 2L);
        final OfflineContentRequests offlineContentRequests = getOfflineContentRequests(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Arrays.asList(new DownloadRequest(TRACK, 12345L)),
                Collections.<Urn>emptyList()
        );

        command.call(offlineContentRequests);

        databaseAssertions().assertDownloadedAndNotMarkedForRemoval(TRACK);
    }

    private OfflineContentRequests getOfflineContentRequests(List<DownloadRequest> allDownloadRequests,
                                                             List<DownloadRequest> newDownloadRequests,
                                                             List<DownloadRequest> newRestoredRequests,
                                                             List<Urn> toRemove) {
        return new OfflineContentRequests(allDownloadRequests, newDownloadRequests, newRestoredRequests, toRemove);
    }
}
