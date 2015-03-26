package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ClearTrackDownloadsCommandTest extends StorageIntegrationTest {

    @Mock private Thread backgroundThread;

    private ClearTrackDownloadsCommand command;

    @Before
    public void setUp() throws Exception {
        command = new ClearTrackDownloadsCommand(propeller(), providerOf(backgroundThread));
    }

    @Test
    public void removesTrackDownloads() {
        Urn track = Urn.forTrack(123L);
        testFixtures().insertTrackPendingDownload(track, 123L);

        command.call(null);

        databaseAssertions().assertTrackDownloadNotStored(track);
    }

}