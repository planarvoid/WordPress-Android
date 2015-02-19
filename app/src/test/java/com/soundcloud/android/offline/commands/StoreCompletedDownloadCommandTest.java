package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadResult;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class StoreCompletedDownloadCommandTest extends StorageIntegrationTest {

    private StoreCompletedDownloadCommand command;

    @Before
    public void setUp() {
        command = new StoreCompletedDownloadCommand(propeller());
    }

    @Test
    public void updatesDownloadTracksWithDownloadResults() throws PropellerWriteException {
        final Urn trackUrn = Urn.forTrack(123L);
        final DownloadResult downloadResult = DownloadResult.success(trackUrn);
        testFixtures().insertRequestedTrackDownload(trackUrn, 100L);

        command.with(downloadResult).call();

        databaseAssertions().assertDownloadResultsInserted(downloadResult);
    }

}