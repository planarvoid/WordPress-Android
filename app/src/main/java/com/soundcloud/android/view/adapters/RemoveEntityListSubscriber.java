package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

public final class RemoveEntityListSubscriber extends DefaultSubscriber<Urn> {
    private final RecyclerItemAdapter adapter;

    public RemoveEntityListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final Urn urn) {
        int adapterCount = adapter.getItemCount();
        for (int position = 0; position < adapterCount; position++) {
            final Object item = adapter.getItem(position);
            if (item instanceof Entity) {
                Urn itemUrn = ((Entity) item).getUrn();
                if (itemUrn.equals(urn)) {
                    removeItemFromAdapterAt(position);
                    break;
                }
            }
        }
    }

    private void removeItemFromAdapterAt(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }
}
