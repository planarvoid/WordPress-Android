package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;

public interface PlaylistHeaderRenderer extends CellRenderer<PlaylistCollectionHeaderItem> {

    interface OnSettingsClickListener {
        void onSettingsClicked(View view);
    }

    void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener);
}
