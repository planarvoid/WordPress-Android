package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class UserSoundsPlaylistItemRenderer implements CellRenderer<UserSoundsItem> {
    @Override
    public View createItemView(ViewGroup parent) {
        return new View(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {

    }
}
