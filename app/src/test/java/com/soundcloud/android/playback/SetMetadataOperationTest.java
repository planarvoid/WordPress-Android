package com.soundcloud.android.playback;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SetMetadataOperationTest extends AndroidUnitTest {
    private PlayQueue playQueue;

    @Before
    public void setUp() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource("origin");
        playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playSessionSource);
    }

    @Test
    public void operationShouldInsertAtTheGivenPosition() throws Exception {
        final PropertySet metadata = PropertySet.create()
                .put(TrackProperty.DESCRIPTION, "New description");
        new PlayQueueManager.SetMetadataOperation(1, metadata).execute(playQueue);

        assertThat(playQueue.getMetaData(1).get(TrackProperty.DESCRIPTION)).isEqualTo("New description");
    }
}