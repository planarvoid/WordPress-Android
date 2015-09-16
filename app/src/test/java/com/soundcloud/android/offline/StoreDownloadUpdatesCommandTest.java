package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class StoreDownloadUpdatesCommandTest extends StorageIntegrationTest {

    private CurrentDateProvider dateProvider;

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final DownloadRequest request = new DownloadRequest(TRACK, 12345L, "http://wav");
    private static final long NOW = System.currentTimeMillis();

    private StoreDownloadUpdatesCommand command;

    @Before
    public void setup() {
        dateProvider = new TestDateProvider();
        command = new StoreDownloadUpdatesCommand(propeller(), dateProvider);
    }

    @Test
    public void storeRemovedTracksAsPendingRemoval() {
        testFixtures().insertCompletedTrackDownload(TRACK, 0, NOW);

        final OfflineContentUpdates offlineContentUpdates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(TRACK));

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadPendingRemoval(TRACK);
    }

    @Test
    public void storesNewDownloadRequestsAsPendingDownload() {
        final OfflineContentUpdates offlineContentUpdates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(request),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList());

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadRequestsInserted(Collections.singletonList(TRACK));
    }

    @Test
    public void storesRestoredRequestsAsDownloaded() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK, 1L, 2L);
        final OfflineContentUpdates offlineContentUpdates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(request),
                Collections.<DownloadRequest>emptyList(),
                Collections.<Urn>emptyList());

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadedAndNotMarkedForRemoval(TRACK);
    }

    @Test
    public void marksAsUnavailableOfflineCreatorOptOutTracks() {
        final DownloadRequest creatorOptOut = new DownloadRequest.Builder(TRACK, 1L, "http://wav", false).build();
        final OfflineContentUpdates offlineContentUpdates = new OfflineContentUpdates(
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.<DownloadRequest>emptyList(),
                Collections.singletonList(creatorOptOut),
                Collections.<Urn>emptyList());

        command.call(offlineContentUpdates);

        databaseAssertions().assertTrackIsUnavailable(TRACK, dateProvider.getCurrentTime());
    }

}
