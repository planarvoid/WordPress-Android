package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadOfflineLikesUrnsTest extends StorageIntegrationTest {

    private LoadOfflineLikesUrns command;

    @Before
    public void setup() {
        command = new LoadOfflineLikesUrns(propeller());
    }

    @Test
    public void shouldLoadTrackLikes() throws Exception {
        Urn downloadUrn = Urn.forTrack(123L);
        testFixtures().insertCompletedTrackDownload(downloadUrn, new Date(100).getTime());
        testFixtures().insertTrackDownloadPendingRemoval(Urn.forTrack(234L), new Date(200).getTime());

        List<Urn> trackLikes = command.call();

        expect(trackLikes).toContainExactly(downloadUrn);
    }

}