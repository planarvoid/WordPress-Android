package com.soundcloud.android.view.adapters;

import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public final class PrependItemToListSubscriber extends DefaultSubscriber<PropertySet> {
    private final ItemAdapter<PropertySet> adapter;

    public PrependItemToListSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final PropertySet track) {
        adapter.prependItem(track);
        adapter.notifyDataSetChanged();
    }
}
