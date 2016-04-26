package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EndOfListDividerRenderer implements CellRenderer<UserSoundsItem> {
    @Inject
    EndOfListDividerRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_user_sounds_end_of_list_divider, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {

    }
}
