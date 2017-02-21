package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import java.util.HashSet;
import java.util.Set;

public final class RemoveEntityListSubscriber extends DefaultSubscriber<Urn> {
    private final RecyclerItemAdapter adapter;

    public RemoveEntityListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final Urn urn) {
        Set<Object> remove = new HashSet<>();

        for (Object input : adapter.getItems()) {
            if (shouldRemove(input, urn)) {
                remove.add(input);
            }
        }

        for (Object o : remove) {
            removeItem(o);
        }
    }

    private boolean shouldRemove(Object item, Urn urn) {
        return item != null && item instanceof Entity && ((Entity) item).getUrn().equals(urn);
    }

    private void removeItem(Object item) {
        int position = adapter.getItems().indexOf(item);
        if (position >= 0) {
            removeItemFromAdapterAt(position);
        }
    }

    private void removeItemFromAdapterAt(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }
}
