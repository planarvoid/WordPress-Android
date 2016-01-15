package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.PlayQueueAssertions;
import org.junit.Test;

public class TrackQueueItemTest extends AndroidUnitTest {

    @Test
    public void copiesTrackQueueItem() {
        TrackQueueItem trackQueueItem = new TrackQueueItem.Builder(Urn.forTrack(123))
                .withAdData(AdFixtures.getAudioAd(Urn.forTrack(455)))
                .relatedEntity(Urn.forPlaylist(678))
                .persist(false)
                .blocked(true)
                .fromSource("source","source-version")
                .build();
        PlayQueueAssertions.assertPlayQueueItemsEqual(trackQueueItem, new TrackQueueItem.Builder(trackQueueItem).build());
    }
}
