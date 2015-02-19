package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class OfflineTrackCountCommandTest extends StorageIntegrationTest {

    private OfflineTrackCountCommand command;

    private static final Urn TRACK1_URN = Urn.forTrack(101L);
    private static final long TRACK1_DOWNLOAD_COMPLETION_TIMESTAMP = 1L;
    private static final Urn TRACK2_URN = Urn.forTrack(102L);
    private static final long TRACK2_DOWNLOAD_COMPLETION_TIMESTAMP = 2L;
    private static final long TRACK2_DOWNLOAD_REMOVAL_TIMESTAMP = 3L;

    @Before
    public void setUp() throws Exception {
        command = new OfflineTrackCountCommand(propeller());
    }

    @Test
    public void shouldCountDownloadedLikedTracks() throws Exception {
        testFixtures().insertCompletedTrackDownload(TRACK1_URN, TRACK1_DOWNLOAD_COMPLETION_TIMESTAMP);
        testFixtures().insertCompletedTrackDownload(TRACK2_URN, TRACK2_DOWNLOAD_COMPLETION_TIMESTAMP);

        expect(command.call()).toEqual(2);
    }

    @Test
    public void shouldNotCountDownloadedTracksPendingRemoval() throws Exception {
        testFixtures().insertCompletedTrackDownload(TRACK1_URN, TRACK1_DOWNLOAD_COMPLETION_TIMESTAMP);
        testFixtures().insertTrackDownloadPendingRemoval(TRACK2_URN, TRACK2_DOWNLOAD_REMOVAL_TIMESTAMP);

        expect(command.call()).toEqual(1);
    }

    @Test
    public void shouldReturnZeroIfNoDownloadsWereMade() throws Exception {
        expect(command.call()).toEqual(0);
    }
}