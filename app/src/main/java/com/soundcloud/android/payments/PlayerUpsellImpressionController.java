package com.soundcloud.android.payments;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerUpsellImpressionController {

    private PlayQueueItem lastImpression;

    private final EventBus eventBus;

    @Inject
    public PlayerUpsellImpressionController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void recordUpsellViewed(PlayQueueItem playQueueItem) {
        if (!playQueueItem.equals(lastImpression)) {
            lastImpression = playQueueItem;
            final UpgradeFunnelEvent event = UpgradeFunnelEvent.forPlayerImpression(playQueueItem.getUrn());
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }
}
