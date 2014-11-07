package com.soundcloud.android.view.menu;

import com.soundcloud.android.playback.ui.TrackMenuWrapperListener;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

public interface PopupMenuWrapper {

    void inflate(int menuResourceId);
    void setOnMenuItemClickListener(TrackMenuWrapperListener trackMenuWrapperListener);
    void setOnDismissListener(TrackMenuWrapperListener trackMenuWrapperListener);
    void show();
    void setItemVisible(int itemId, boolean visible);
    void setItemEnabled(int itemId, boolean enabled);
    void setItemText(int itemId, String text);

    void dismiss();

    interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem menuItem);
    }

    interface Factory {
        PopupMenuWrapper build(Context context, View anchor);
    }
}
