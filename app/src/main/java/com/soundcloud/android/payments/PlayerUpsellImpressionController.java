package com.soundcloud.android.payments;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayerUpsellImpressionController {

    private PlayQueueItem lastImpression;

    private final EventBus eventBus;
    private final FeatureFlags featureFlags;

    @Inject
    public PlayerUpsellImpressionController(EventBus eventBus, FeatureFlags featureFlags) {
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
    }

    public void recordUpsellViewed(PlayQueueItem playQueueItem) {
        if (featureFlags.isEnabled(Flag.SOUNDCLOUD_GO) && !playQueueItem.equals(lastImpression)){
            lastImpression = playQueueItem;
            final UpgradeTrackingEvent event = UpgradeTrackingEvent.forPlayerImpression(playQueueItem.getUrn());
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }
}
