package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.UpdatableItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;

import java.util.Map;

public final class UpdateEntityListSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
    private final RecyclerItemAdapter adapter;

    public UpdateEntityListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final EntityStateChangedEvent event) {
        final Map<Urn, PropertySet> changeSet = event.getChangeMap();
        final Iterable<ListItem> filtered = Iterables.filter(adapter.getItems(), ListItem.class);
        for (ListItem item : filtered) {
            final Urn urn = item.getUrn();
            if (changeSet.containsKey(urn) && item instanceof UpdatableItem) {
                final ListItem updatedListItem = ((UpdatableItem)item).updated(changeSet.get(urn));
                final int position = adapter.getItems().indexOf(item);
                if (adapter.getItems().size() > position) {
                    adapter.getItems().set(position, updatedListItem);
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
