package com.soundcloud.android.view.menu;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import javax.inject.Inject;

public class PopupMenuWrapper {

    private final PopupMenu popupMenu;
    private final Context context;

    public interface PopupMenuWrapperListener {
        boolean onMenuItemClick(MenuItem menuItem, Context context);

        void onDismiss();
    }

    public PopupMenuWrapper(PopupMenu popupMenu, Context context) {
        this.popupMenu = popupMenu;
        this.context = context;
    }

    public void inflate(int menuResourceId) {
        popupMenu.inflate(menuResourceId);
    }

    public MenuItem findItem(int itemId) {
        return popupMenu.getMenu().findItem(itemId);
    }

    public void setOnMenuItemClickListener(final PopupMenuWrapperListener popupMenuWrapperListener) {
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return popupMenuWrapperListener.onMenuItemClick(item, context);
            }
        });
    }

    public void setOnDismissListener(final PopupMenuWrapperListener popupMenuWrapperListener) {
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                popupMenuWrapperListener.onDismiss();
            }
        });
    }

    public void show() {
        popupMenu.show();
    }

    public void setItemVisible(int itemId, boolean visible) {
        popupMenu.getMenu().findItem(itemId).setVisible(visible);
    }

    public void setItemEnabled(int itemId, boolean enabled) {
        popupMenu.getMenu().findItem(itemId).setEnabled(enabled);
    }

    public void setItemText(int itemId, String text) {
        popupMenu.getMenu().findItem(itemId).setTitle(text);
    }

    public void dismiss() {
        popupMenu.dismiss();
    }

    public static class Factory {
        @Inject
        public Factory() {
            // for dagger
        }

        public PopupMenuWrapper build(Context context, View anchor) {
            return new PopupMenuWrapper(new PopupMenu(context, anchor), context);
        }
    }

}
