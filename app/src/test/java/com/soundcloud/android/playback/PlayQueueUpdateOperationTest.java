package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayQueueUpdateOperationTest extends AndroidUnitTest {
    private PlayQueue playQueue;

    @Before
    public void setUp() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource("origin");
        playQueue = PlayQueue.fromTrackUrnList(TestUrns.createTrackUrns(1L, 2L), playSessionSource);
    }

    @Test
    public void insertAudioOperationShouldInsertAtTheGivenPosition() throws Exception {
        new PlayQueueManager.InsertAudioOperation(1, Urn.forTrack(123L), PropertySet.create(), true).execute(playQueue);
        assertThat(playQueue.getTrackItemUrns()).containsExactly(Urn.forTrack(1L), Urn.forTrack(123L), Urn.forTrack(2L));
    }

    @Test
    public void insertVideoOperationShouldInsertAtTheGivenPosition() throws Exception {
        final PropertySet videoMetaData = TestPropertySets.videoAdProperties(Urn.forTrack(123L));
        new PlayQueueManager.InsertVideoOperation(1, videoMetaData).execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(Urn.forTrack(1L));
        assertThat(playQueue.getPlayQueueItem(1).isVideo()).isTrue();
        assertThat(playQueue.getPlayQueueItem(1).getMetaData()).isEqualTo(videoMetaData);
        assertThat(playQueue.getPlayQueueItem(2).getUrn()).isEqualTo(Urn.forTrack(2L));
    }

    @Test
    public void setMetaDataOperationShouldSetMetadataForItemAtGivenPosition() throws Exception {
        final PropertySet metadata = PropertySet.create().put(TrackProperty.DESCRIPTION, "New description");
        new PlayQueueManager.SetMetadataOperation(1, metadata).execute(playQueue);

        assertThat(playQueue.getMetaData(1).get(TrackProperty.DESCRIPTION)).isEqualTo("New description");
    }
}
