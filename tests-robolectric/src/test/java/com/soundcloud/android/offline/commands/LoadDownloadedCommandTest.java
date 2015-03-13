package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class LoadDownloadedCommandTest extends StorageIntegrationTest {

    private LoadDownloadedCommand command;
    private ApiTrack apiTrack;

    @Before
    public void setUp() {
        command = new LoadDownloadedCommand(propeller());
        apiTrack = ModelFixtures.create(ApiTrack.class);

        testFixtures().insertTrack(apiTrack);
        testFixtures().insertLike(apiTrack.getUrn().getNumericId(), TableColumns.Sounds.TYPE_TRACK, new Date(0));
    }

    @Test
    public void returnsDownloaded() throws Exception {
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 100);

        expect(command.call()).toContainExactly(apiTrack.getUrn());
    }

}