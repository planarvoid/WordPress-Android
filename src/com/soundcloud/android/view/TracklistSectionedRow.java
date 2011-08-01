package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;

public class TracklistSectionedRow extends TracklistRow {

    private static final String TAG = "UserlistSectionedRow";

    public TracklistSectionedRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_sectioned_row;
    }

}
