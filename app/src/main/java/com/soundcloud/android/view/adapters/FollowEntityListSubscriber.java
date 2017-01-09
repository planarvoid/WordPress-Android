package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.presentation.FollowableItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;

public final class FollowEntityListSubscriber extends DefaultSubscriber<FollowingStatusEvent> {
    private final RecyclerItemAdapter adapter;

    public FollowEntityListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final FollowingStatusEvent event) {
        final Iterable<FollowableItem> filtered = Iterables.filter(adapter.getItems(), FollowableItem.class);
        boolean updated = false;
        for (FollowableItem item : filtered) {
            if (event.urn().equals(item.getUrn())) {
                final FollowableItem updatedListItem = item.updatedWithFollowing(event.isFollowed(), event.followingsCount());
                final int position = adapter.getItems().indexOf(item);
                adapter.getItems().set(position, updatedListItem);
                updated = true;
            }
        }
        if (updated) {
            adapter.notifyDataSetChanged();
        }
    }
}
