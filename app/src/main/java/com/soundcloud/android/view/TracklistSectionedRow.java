package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;

import android.content.Context;

public class TracklistSectionedRow extends TrackInfoBar {
    public TracklistSectionedRow(Context activity, ScBaseAdapter adapter) {
        super(activity, adapter);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_sectioned_row;
    }
}
