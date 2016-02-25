package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class ViewAllRenderer implements CellRenderer<UserSoundsItem> {
    @Inject
    public ViewAllRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        //TODO: Need to display separate text for collection type (fix in styling story)
        ((TextView) itemView).setText("View all");
    }
}
