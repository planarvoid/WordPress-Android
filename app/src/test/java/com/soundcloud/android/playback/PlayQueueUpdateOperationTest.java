package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestUrns;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

public class PlayQueueUpdateOperationTest extends AndroidUnitTest {
    private PlayQueue playQueue;

    @Before
    public void setUp() throws Exception {
        PlaySessionSource playSessionSource = new PlaySessionSource("origin");
        playQueue = TestPlayQueue.fromUrns(TestUrns.createTrackUrns(1L, 2L), playSessionSource);
    }

    @Test
    public void insertAudioOperationShouldInsertAtTheGivenPosition() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        new PlayQueueManager.InsertAudioOperation(1, Urn.forTrack(123L), audioAd, true).execute(playQueue);
        assertThat(playQueue.getTrackItemUrns()).containsExactly(Urn.forTrack(1L), Urn.forTrack(123L), Urn.forTrack(2L));
    }

    @Test
    public void insertVideoOperationShouldInsertAtTheGivenPosition() throws Exception {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        new PlayQueueManager.InsertVideoOperation(1, videoAd).execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(Urn.forTrack(1L));
        assertThat(playQueue.getPlayQueueItem(1).isVideo()).isTrue();
        assertThat(playQueue.getPlayQueueItem(1).getAdData()).isEqualTo(Optional.of(videoAd));
        assertThat(playQueue.getPlayQueueItem(2).getUrn()).isEqualTo(Urn.forTrack(2L));
    }

    @Test
    public void setMetaDataOperationShouldSetAdDataForItemAtGivenPosition() throws Exception {
        final LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        new PlayQueueManager.SetAdDataOperation(1, Optional.<AdData>of(leaveBehindAd)).execute(playQueue);

        assertThat(playQueue.getAdData(1)).isEqualTo(Optional.of(leaveBehindAd));
    }
}
