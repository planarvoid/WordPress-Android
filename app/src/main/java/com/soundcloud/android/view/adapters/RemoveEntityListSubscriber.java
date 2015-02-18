package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public final class RemoveEntityListSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public RemoveEntityListSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final EntityStateChangedEvent event) {
        Urn unlikedUrn = event.getNextUrn();
        int adapterCount = adapter.getCount();
        for (int position = 0; position < adapterCount; position++) {
            Urn itemUrn = adapter.getItem(position).get(EntityProperty.URN);
            if (itemUrn.equals(unlikedUrn)) {
                removeItemFromAdapterAt(position);
            }
        }
    }

    private void removeItemFromAdapterAt(int position) {
        adapter.removeAt(position);
        adapter.notifyDataSetChanged();
    }
}