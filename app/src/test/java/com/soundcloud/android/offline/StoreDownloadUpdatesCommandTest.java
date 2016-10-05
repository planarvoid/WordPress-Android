package com.soundcloud.android.offline;

import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

public class StoreDownloadUpdatesCommandTest extends StorageIntegrationTest {

    private CurrentDateProvider dateProvider;

    private static final Urn TRACK = Urn.forTrack(123L);
    private static final DownloadRequest request = ModelFixtures.downloadRequestFromLikes(TRACK);
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

        final OfflineContentUpdates offlineContentUpdates = OfflineContentUpdates.builder()
                                                                                 .tracksToRemove(singletonList(TRACK))
                                                                                 .build();

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadPendingRemoval(TRACK);
    }

    @Test
    public void storesNewDownloadRequestsAsPendingDownload() {
        final OfflineContentUpdates offlineContentUpdates = OfflineContentUpdates.builder()
                                                                                 .newTracksToDownload(singletonList(
                                                                                         TRACK))
                                                                                 .build();

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadRequestsInserted(singletonList(TRACK));
    }

    @Test
    public void storesRestoredRequestsAsDownloaded() {
        testFixtures().insertTrackDownloadPendingRemoval(TRACK, 1L, 2L);
        final OfflineContentUpdates offlineContentUpdates = OfflineContentUpdates.builder()
                                                                                 .tracksToRestore(singletonList(request.getUrn()))
                                                                                 .build();

        command.call(offlineContentUpdates);

        databaseAssertions().assertDownloadedAndNotMarkedForRemoval(TRACK);
    }

    @Test
    public void marksAsUnavailableOfflineCreatorOptOutTracks() {
        final DownloadRequest creatorOptOut = ModelFixtures.creatorOptOutRequest(TRACK);

        final OfflineContentUpdates offlineContentUpdates = OfflineContentUpdates.builder()
                                                                                 .unavailableTracks(singletonList(
                                                                                         creatorOptOut.getUrn()))
                                                                                 .build();

        command.call(offlineContentUpdates);

        databaseAssertions().assertTrackIsUnavailable(TRACK, dateProvider.getCurrentTime());
    }

    @Test
    public void marksAsUnavailableSnippetTracks() {
        final DownloadRequest snippet = ModelFixtures.snippetRequest(TRACK);
        final OfflineContentUpdates offlineContentUpdates = OfflineContentUpdates.builder()
                .unavailableTracks(singletonList(snippet.getUrn()))
                .build();

        command.call(offlineContentUpdates);

        databaseAssertions().assertTrackIsUnavailable(TRACK, dateProvider.getCurrentTime());
    }

}
