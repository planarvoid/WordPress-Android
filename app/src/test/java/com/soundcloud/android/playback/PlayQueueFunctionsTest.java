package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import org.junit.Test;

public class PlayQueueFunctionsTest extends AndroidUnitTest {

    @Test
    public void isTrackQueueItemShouldReturnTrueIfCurrentPlayQueueItemEventHasTrack() {
        final PlayQueueItem trackItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(trackItem, Urn.NOT_SET, 0);

        assertThat(PlayQueueFunctions.IS_AUDIO_AD_QUEUE_ITEM.call(event)).isTrue();
    }

    @Test
    public void toTrackQueueItemShouldReturnTrackQueueItemFromCurrentPlayQueueItem() {
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(trackItem, Urn.NOT_SET, 0);

        assertThat(PlayQueueFunctions.TO_TRACK_QUEUE_ITEM.call(event)).isEqualTo(trackItem);
    }
}
