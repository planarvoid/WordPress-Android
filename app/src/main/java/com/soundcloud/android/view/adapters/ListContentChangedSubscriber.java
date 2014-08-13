package com.soundcloud.android.view.adapters;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public final class ListContentChangedSubscriber extends DefaultSubscriber<PlayableUpdatedEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public ListContentChangedSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final PlayableUpdatedEvent event) {
        final int index = Iterables.indexOf(adapter.items, new Predicate<PropertySet>() {
            @Override
            public boolean apply(PropertySet item) {
                return item.get(PlayableProperty.URN).equals(event.getUrn());
            }
        });

        if (index > - 1) {
            adapter.getItem(index).merge(event.getChangeSet());
            adapter.notifyDataSetChanged();
        }
    }
}
