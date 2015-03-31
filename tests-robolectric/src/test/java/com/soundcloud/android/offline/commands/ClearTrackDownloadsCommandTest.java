package com.soundcloud.android.offline.commands;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ClearTrackDownloadsCommandTest extends StorageIntegrationTest {

    private ClearTrackDownloadsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new ClearTrackDownloadsCommand(propeller());
    }

    @Test
    public void removesTrackDownloads() {
        Urn track = Urn.forTrack(123L);
        testFixtures().insertTrackPendingDownload(track, 123L);

        command.call(null);

        databaseAssertions().assertTrackDownloadNotStored(track);
    }

}