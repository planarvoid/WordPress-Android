package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;

import android.content.Context;

public class UserlistSectionedRow extends UserlistRow {
    public UserlistSectionedRow(Context _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter, false);

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_sectioned_row;
    }
}
