package com.soundcloud.android.stream;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Map;

class UpdateStreamEntitySubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
    private final StreamAdapter adapter;

    UpdateStreamEntitySubscriber(StreamAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final EntityStateChangedEvent event) {
        if (event.isFollowingKind()) {
            adapter.onFollowingEntityChange(event);
        } else {
            boolean changed = false;
            final Map<Urn, PropertySet> changeSet = event.getChangeMap();
            for (final StreamItem item : adapter.getItems()) {
                final Optional<ListItem> listItem = item.getListItem();
                if (listItem.isPresent() && changeSet.containsKey(listItem.get().getUrn())) {
                    changed = true;
                    listItem.get().update(changeSet.get(listItem.get().getUrn()));
                }
            }
            if (changed) {
                adapter.notifyDataSetChanged();
            }
        }
    }
}
