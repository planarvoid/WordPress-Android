package com.soundcloud.android.playback;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayQueueFunctionsTest extends AndroidUnitTest {

    @Test
    public void isTrackQueueItemShouldReturnTrueIfCurrentPlayQueueItemEventHasTrack() {
        final PlayQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(trackItem, Urn.NOT_SET, 0);

        assertThat(PlayQueueFunctions.IS_TRACK_QUEUE_ITEM.call(event)).isTrue();
    }

    @Test
    public void toTrackQueueItemShouldReturnTrackQueueItemFromCurrentPlayQueueItem() {
        final TrackQueueItem trackItem = (TrackQueueItem) TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(trackItem, Urn.NOT_SET, 0);

        assertThat(PlayQueueFunctions.TO_TRACK_QUEUE_ITEM.call(event)).isEqualTo(trackItem);
    }
}
