package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;

public class UserlistSectionedRow extends UserlistRow {

    private static final String TAG = "UserlistSectionedRow";

    public UserlistSectionedRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_row_sectioned;
    }

}
