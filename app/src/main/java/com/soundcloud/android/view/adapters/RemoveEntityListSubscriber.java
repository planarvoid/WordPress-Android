package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public final class RemoveEntityListSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public RemoveEntityListSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final EntityStateChangedEvent event) {
        // if (event.isTrackLike() && !event.getNextChangeSet().get(TrackProperty.IS_LIKED)) {
        boolean changed = false;
        Urn unlikedUrn = event.getNextUrn();

        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItem(position).get(EntityProperty.URN).equals(unlikedUrn)) {
                changed = true;
                adapter.removeAt(position);
            }
        }

        if (changed) {
            adapter.notifyDataSetChanged();
        }
        //  }
    }
}