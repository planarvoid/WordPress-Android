package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadPendingRemovalCommandTest extends StorageIntegrationTest {

    private LoadPendingRemovalCommand command;

    private final Urn TRACK_URN = Urn.forTrack(123L);
    private final long REMOVAL_DELAY = 3 * 60000;

    @Before
    public void setUp() {
        command = new LoadPendingRemovalCommand(propeller());
    }

    @Test
    public void loadDownloadsPendingRemovalThatSatisfyDelayCondition() throws Exception {
        final long removedAtTimestamp = System.currentTimeMillis() - 5 * 60000;
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, removedAtTimestamp);

        List<Urn> removals = command.with(REMOVAL_DELAY).call();
        expect(removals).toContainExactly(TRACK_URN);
    }

    @Test
    public void doesNotLoadDownloadsNotPendingRemoval() throws Exception {
        testFixtures().insertCompletedTrackDownload(TRACK_URN, REMOVAL_DELAY);

        List<Urn> removals = command.with(REMOVAL_DELAY).call();
        expect(removals).toBeEmpty();
    }

    @Test
    public void doesNotLoadDownloadsPendingRemovalThatDoNotSatisfyDelayCondition() throws Exception {
        final long tooShortRemovalTimestamp = System.currentTimeMillis() - 60000;
        testFixtures().insertTrackDownloadPendingRemoval(TRACK_URN, tooShortRemovalTimestamp);

        List<Urn> removals = command.with(REMOVAL_DELAY).call();
        expect(removals).toBeEmpty();
    }

}