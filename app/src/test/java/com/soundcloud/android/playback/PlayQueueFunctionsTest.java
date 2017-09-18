package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import io.reactivex.functions.Predicate;
import org.junit.Test;
import rx.functions.Func1;

public class PlayQueueFunctionsTest extends AndroidUnitTest {

    @Test
    public void isTrackQueueItemShouldReturnTrueIfCurrentPlayQueueItemEventHasTrack() throws Exception {
        final PlayQueueItem trackItem = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(trackItem, Urn.NOT_SET, 0);

        assertThat(((Predicate<CurrentPlayQueueItemEvent>) currentItemEvent -> currentItemEvent.getCurrentPlayQueueItem().isAudioAd()).test(event)).isTrue();
    }

    @Test
    public void toTrackQueueItemShouldReturnTrackQueueItemFromCurrentPlayQueueItem() {
        final TrackQueueItem trackItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(trackItem, Urn.NOT_SET, 0);

        assertThat(((Func1<CurrentPlayQueueItemEvent, TrackQueueItem>) currentItemEvent -> (TrackQueueItem) currentItemEvent.getCurrentPlayQueueItem()).call(event)).isEqualTo(trackItem);
    }
}
