package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

public final class RemoveEntityListSubscriber extends DefaultSubscriber<Urn> {
    private final ReactiveItemAdapter<? extends ListItem> adapter;

    public RemoveEntityListSubscriber(ItemAdapter<? extends ListItem> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final Urn urn) {
        int adapterCount = adapter.getItemCount();
        for (int position = 0; position < adapterCount; position++) {
            Urn itemUrn = adapter.getItem(position).getEntityUrn();
            if (itemUrn.equals(urn)) {
                removeItemFromAdapterAt(position);
                break;
            }
        }
    }

    private void removeItemFromAdapterAt(int position) {
        adapter.removeItem(position);
        adapter.notifyDataSetChanged();
    }
}