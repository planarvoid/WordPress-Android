package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EntityUpdatedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

import java.util.Map;

public final class ListContentSyncedSubscriber extends DefaultSubscriber<EntityUpdatedEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public ListContentSyncedSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final EntityUpdatedEvent event) {
        boolean changed = false;
        final Map<Urn, PropertySet> changeSet = event.getChangeMap();
        for (PropertySet item : adapter.getItems()) {
            final Urn urn = item.get(EntityProperty.URN);
            if (changeSet.containsKey(urn)){
                changed = true;
                item.update(changeSet.get(urn));
            }
        }
        if (changed){
            adapter.notifyDataSetChanged();
        }
    }
}
