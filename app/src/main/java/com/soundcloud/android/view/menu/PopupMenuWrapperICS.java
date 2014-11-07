package com.soundcloud.android.view.menu;

import com.soundcloud.android.playback.ui.TrackPageMenuController;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PopupMenuWrapperICS implements PopupMenuWrapper {

    private final PopupMenu popupMenu;

    public PopupMenuWrapperICS(PopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    @Override
    public void inflate(int menuResourceId) {
        popupMenu.inflate(menuResourceId);
    }

    @Override
    public void setOnMenuItemClickListener(final TrackPageMenuController trackPageMenuController) {
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return trackPageMenuController.onMenuItemClick(item);
            }
        });
    }

    @Override
    public void show() {
        popupMenu.show();
    }

    @Override
    public void setItemVisible(int itemId, boolean visible) {
        popupMenu.getMenu().findItem(itemId).setVisible(visible);
    }

    @Override
    public void setItemEnabled(int itemId, boolean enabled) {
        popupMenu.getMenu().findItem(itemId).setEnabled(enabled);
    }

    @Override
    public void setItemText(int itemId, String text) {
        popupMenu.getMenu().findItem(itemId).setTitle(text);
    }

    @Override
    public void dismiss() {
        popupMenu.dismiss();
    }

    public static class Factory implements PopupMenuWrapper.Factory{
        @Inject
        public Factory() {
            // for dagger
        }
        @Override
        public PopupMenuWrapper build(Context context, View anchor) {
            return new PopupMenuWrapperICS(new PopupMenu(context, anchor));
        }
    }

}
