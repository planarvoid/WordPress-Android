package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class DividerRenderer implements CellRenderer<UserSoundsItem> {
    @Inject
    DividerRenderer() {}

    @Override
    public View createItemView(ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        //TODO: Replace with actual divider
        ((TextView) itemView).setText("DIVIDER");
    }
}
