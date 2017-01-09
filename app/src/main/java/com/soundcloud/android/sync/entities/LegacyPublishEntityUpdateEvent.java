package com.soundcloud.android.sync.entities;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import java.util.Collection;

@Deprecated
public class LegacyPublishEntityUpdateEvent extends PublishUpdateEvent<ApiSyncable> {
    private final EventBus eventBus;

    @Inject
    public LegacyPublishEntityUpdateEvent(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call(Collection<ApiSyncable> input) {
        final Collection<EntityStateChangedEvent> updatedEntities = MoreCollections.transform(input, ApiSyncable::toUpdateEvent);
        if (!updatedEntities.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.mergeUpdates(updatedEntities));
            return true;
        }
        return false;
    }
}
