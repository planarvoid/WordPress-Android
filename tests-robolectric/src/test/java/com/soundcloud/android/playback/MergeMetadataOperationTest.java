package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.createTracksUrn;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class MergeMetadataOperationTest {
    private PlayQueue playQueue;
    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        playSessionSource = new PlaySessionSource("origin");
        playQueue = PlayQueue.fromTrackUrnList(createTracksUrn(1L, 2L), playSessionSource);
    }

    @Test
    public void operationShouldInsertAtTheGivenPosition() throws Exception {
        final PropertySet metadata = PropertySet.create()
                .put(TrackProperty.DESCRIPTION, "New description");
        new PlayQueueManager.MergeMetadataOperation(1, metadata).execute(playQueue);
        expect(playQueue.getMetaData(1).get(TrackProperty.DESCRIPTION)).toEqual("New description");
    }
}
