package com.soundcloud.android.stream;

import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.view.adapters.CellPresenter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

// PLEASE IGNORE THIS GUY FOR NOW.
// I just need something quick and dirty for testing right now.
// I will fully revisit how we do adapters and row binding in a later step.
class StreamItemPresenter implements CellPresenter<PropertySet> {

    @Inject
    StreamItemPresenter() {
    }

    @Override
    public TextView createItemView(int position, ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> streamItems) {
    }

    @Override
    public int getItemViewType() {
        return DEFAULT_ITEM_VIEW_TYPE;
    }
}
