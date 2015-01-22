package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class StoreTrackDownloadsCommandTest extends StorageIntegrationTest {

    private final Urn TRACK_URN1 = Urn.forTrack(123L);
    private final Urn TRACK_URN2 = Urn.forTrack(124L);
    private final List<Urn> TRACK_URNS = Arrays.asList(TRACK_URN1, TRACK_URN2);

    private StoreTrackDownloadsCommand command;

    @Before
    public void setUp() {
        command = new StoreTrackDownloadsCommand(propeller());
    }

    @Test
    public void storeNewDownloadRequestsSavesRequestedDownloads() throws PropellerWriteException {
        command.with(TRACK_URNS).call();

        databaseAssertions().assertDownloadRequestsInserted(TRACK_URNS);
    }

    @Test
    public void storeNewDownloadRequestsDoesNotOverrideExistingRecords() throws PropellerWriteException {
        final long timestamp = 100;
        testFixtures().insertRequestedTrackDownload(TRACK_URN1, timestamp);

        command.with(TRACK_URNS).call();

        databaseAssertions().assertExistingDownloadRequest(timestamp, TRACK_URN1);
        databaseAssertions().assertDownloadRequestsInserted(Arrays.asList(TRACK_URN2));
    }

    @Test
    public void updatesToPendingRemovalWhenNotInLikesAnymore() throws PropellerWriteException {
        testFixtures().insertRequestedTrackDownload(TRACK_URN1, 100);

        command.with(Collections.<Urn>emptyList()).call();

        databaseAssertions().assertDownloadPendingRemoval(TRACK_URN1);
    }
}