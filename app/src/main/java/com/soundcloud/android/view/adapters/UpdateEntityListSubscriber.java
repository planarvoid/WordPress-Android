package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

import java.util.Map;

public final class UpdateEntityListSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public UpdateEntityListSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final EntityStateChangedEvent event) {
        boolean changed = false;
        final Map<Urn, PropertySet> changeSet = event.getChangeMap();
        for (PropertySet item : adapter.getItems()) {
            final Urn urn = item.get(EntityProperty.URN);
            if (changeSet.containsKey(urn)){
                changed = true;
                item.update(changeSet.get(urn));
            }
        }
        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }
}
