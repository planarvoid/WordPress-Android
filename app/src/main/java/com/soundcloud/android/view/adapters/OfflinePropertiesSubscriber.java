package com.soundcloud.android.view.adapters;

import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.support.v7.widget.RecyclerView;

import java.util.List;

public class OfflinePropertiesSubscriber<ItemT, VH extends RecyclerView.ViewHolder> extends DefaultSubscriber<OfflineProperties> {

    private final RecyclerItemAdapter<ItemT, VH> adapter;

    public OfflinePropertiesSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(OfflineProperties args) {
        final List<ItemT> items = adapter.getItems();
        for (int position = 0; position < items.size(); position++) {
            final ItemT item = items.get(position);
            if (item instanceof ListItem && item instanceof OfflineItem) {
                final ListItem listItem = (ListItem) item;
                final OfflineItem offlineItem = (OfflineItem) item;
                final OfflineState offlineState = args.state(listItem.getUrn());
                final ListItem updatedListItem = offlineItem.updatedWithOfflineState(offlineState);
                if (items.size() > position) {
                    items.set(position, (ItemT) updatedListItem);
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
