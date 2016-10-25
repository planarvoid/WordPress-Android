package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;

import java.util.Map;

public final class UpdateEntityListSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
    private final ItemAdapter adapter;

    public UpdateEntityListSubscriber(ItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final EntityStateChangedEvent event) {
        boolean changed = false;
        final Map<Urn, PropertySet> changeSet = event.getChangeMap();
        final Iterable<ListItem> filtered = Iterables.filter(adapter.getItems(), ListItem.class);
        for (ListItem item : filtered) {
            final Urn urn = item.getUrn();
            if (changeSet.containsKey(urn)) {
                changed = true;
                item.update(changeSet.get(urn));
            }
        }
        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }
}
