package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public final class ListContentChangedSubscriber extends DefaultSubscriber<PlayableUpdatedEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public ListContentChangedSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final PlayableUpdatedEvent event) {
        for (PropertySet item : adapter.getItems()) {
            if (item.getOrElse(PlayableProperty.URN, Urn.NOT_SET).equals(event.getUrn())) {
                item.update(event.getChangeSet());
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }
}
