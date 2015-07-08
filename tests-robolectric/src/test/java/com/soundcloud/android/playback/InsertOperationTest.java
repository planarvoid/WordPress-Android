package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.createTracksUrn;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class InsertOperationTest {
    private PlayQueue playQueue;
    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        playSessionSource = new PlaySessionSource("origin");
        playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource);
    }

    @Test
    public void operationShouldInsertAtTheGivenPosition() throws Exception {
        new PlayQueueManager.InsertOperation(1, Urn.forTrack(123L), PropertySet.create(), true).execute(playQueue);
        expect(playQueue.getTrackIds()).toContainExactly(1L, 123L, 2L);
    }
}