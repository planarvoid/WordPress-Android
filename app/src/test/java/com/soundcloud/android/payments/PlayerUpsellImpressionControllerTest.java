package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
        controller = new PlayerUpsellImpressionController(eventBus, featureFlags);
        when(featureFlags.isEnabled(Flag.PLAYER_UPSELL_TRACKING)).thenReturn(true);
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

    private void assertImpressionFired() {
        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event).isInstanceOf(UpgradeTrackingEvent.class);
        assertThat(event.getKind()).isEqualTo(UpgradeTrackingEvent.KIND_UPSELL_IMPRESSION);
        assertThat(event.get(UpgradeTrackingEvent.KEY_TCODE)).isEqualTo("soundcloud:tcode:1017");
        assertThat(event.get(AdTrackingKeys.KEY_PAGE_URN)).isEqualTo(Urn.forTrack(123).toString());
    }
}
