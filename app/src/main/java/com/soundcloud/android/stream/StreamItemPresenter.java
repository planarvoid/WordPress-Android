package com.soundcloud.android.stream;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.view.adapters.CellPresenter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

// PLEASE IGNORE THIS GUY FOR NOW.
// I just need something quick and dirty for testing right now.
// I will fully revisit how we do adapters and row binding in a later step.
class StreamItemPresenter implements CellPresenter<PropertySet> {

    @Inject
    StreamItemPresenter() {
    }

    @Override
    public View createItemView(int position, ViewGroup parent, int itemViewType) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, int itemViewType, List<PropertySet> streamItems) {
        final PropertySet propertySet = streamItems.get(position);
        final Urn soundUrn = propertySet.get(StreamItemProperty.SOUND_URN);
        final String soundTitle = propertySet.get(StreamItemProperty.SOUND_TITLE);
        final String poster = propertySet.get(StreamItemProperty.POSTER);
        final Date createdAt = propertySet.get(StreamItemProperty.CREATED_AT);
        final boolean isRepost = propertySet.get(StreamItemProperty.REPOST);

        ((TextView) itemView).setText(
                createdAt + "\n" +
                        soundUrn + "\n" + soundTitle + "\n" + (isRepost ? "reposter: " + poster : "")
        );
    }
}
