package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;

import android.content.Context;

public class UserlistSectionedRow extends UserlistRow {
    public UserlistSectionedRow(Context _activity, ScBaseAdapter _adapter) {
        super(_activity, _adapter, false);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_sectioned_row;
    }
}
