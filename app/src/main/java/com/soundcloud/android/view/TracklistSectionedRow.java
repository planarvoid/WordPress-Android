package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;

import android.content.Context;

public class TracklistSectionedRow extends TrackInfoBar {
    public TracklistSectionedRow(Context activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_sectioned_row;
    }
}
