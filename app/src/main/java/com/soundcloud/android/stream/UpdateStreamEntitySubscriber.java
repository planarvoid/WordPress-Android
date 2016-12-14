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
            final Map<Urn, PropertySet> changeSet = event.getChangeMap();
            for (int position = 0; position < adapter.getItems().size(); position++) {
                final StreamItem item = adapter.getItem(position);
                final Optional<ListItem> listItem = item.getListItem();
                if (listItem.isPresent() && changeSet.containsKey(listItem.get().getUrn())) {
                    final StreamItem updatedStreamItem = item.copyWith(changeSet.get(listItem.get().getUrn()));
                    adapter.setItem(position, updatedStreamItem);
                }
            }
        }
    }
}
