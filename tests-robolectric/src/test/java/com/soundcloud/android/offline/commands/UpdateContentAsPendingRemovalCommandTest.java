package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class UpdateContentAsPendingRemovalCommandTest extends StorageIntegrationTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private UpdateContentAsPendingRemovalCommand command;

    @Before
    public void setUp() throws Exception {
        command = new UpdateContentAsPendingRemovalCommand(propeller());
    }

    @Test
    public void updatesDownloadTracksWithDownloadResults() throws PropellerWriteException {
        testFixtures().insertRequestedTrackDownload(TRACK_URN, 100L);

        command.call();

        databaseAssertions().assertDownloadPendingRemoval(TRACK_URN);
    }
}