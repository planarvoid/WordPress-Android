package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;

public class TracklistSectionedRow extends TracklistRow {
    public TracklistSectionedRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_sectioned_row;
    }
}
