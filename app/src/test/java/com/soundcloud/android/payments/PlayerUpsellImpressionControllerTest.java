package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayerUpsellImpressionControllerTest extends AndroidUnitTest {

    private PlayerUpsellImpressionController controller;

    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus = new TestEventBus();
    private TrackQueueItem playQueueItem;

    @Before
    public void setUp() throws Exception {
        controller = new PlayerUpsellImpressionController(eventBus);
    }

    @Test
    public void firesAnImpressionWhenPlayQueueItemSelected() {
        playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123));
        controller.recordUpsellViewed(playQueueItem);

        assertImpressionFired();
    }

    @Test
    public void doesNotfireSecondImpressionForSamePlayQueueItem() {
        playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123));
        controller.recordUpsellViewed(playQueueItem);
        controller.recordUpsellViewed(playQueueItem);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
    }

    @Test
    public void firesSecondImpressionForSamePlayQueueItemWhenNotConsecutive() {
        playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123));
        controller.recordUpsellViewed(playQueueItem);
        controller.recordUpsellViewed(TestPlayQueueItem.createTrack(Urn.forTrack(321)));
        controller.recordUpsellViewed(playQueueItem);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(3);
    }

    private void assertImpressionFired() {
        UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);
        assertThat(event.kind()).isEqualTo(UpgradeFunnelEvent.Kind.UPSELL_IMPRESSION);
        assertThat(event.adjustToken().get()).isEqualTo(UpgradeFunnelEvent.AdjustToken.HIGH_TIER_TRACK_PLAYED);
        assertThat(event.pageUrn().get()).isEqualTo(Urn.forTrack(123).toString());
    }
}
