package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;

public final class LikeEntityListSubscriber extends DefaultSubscriber<LikesStatusEvent> {
    private final RecyclerItemAdapter adapter;

    public LikeEntityListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final LikesStatusEvent event) {
        final Iterable<ListItem> filtered = Iterables.filter(adapter.getItems(), ListItem.class);
        for (ListItem item : filtered) {
            final Urn urn = item.getUrn();
            if (event.likes().containsKey(urn) && item instanceof LikeableItem) {
                final ListItem updatedListItem = ((LikeableItem)item).updatedWithLike(event.likes().get(urn));
                final int position = adapter.getItems().indexOf(item);
                if (adapter.getItems().size() > position) {
                    adapter.getItems().set(position, updatedListItem);
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
