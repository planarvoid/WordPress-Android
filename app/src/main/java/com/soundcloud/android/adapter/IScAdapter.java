package com.soundcloud.android.adapter;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.quickaction.QuickAction;

import android.graphics.drawable.Drawable;

public interface IScAdapter {
    long getItemId(int position);

    Drawable getDrawableFromId(long id);

    boolean getIconNotReady(long id);

    void assignDrawableToId(long id, Drawable drawable);

    void setIconNotReady(long id);

    Content getContent();

    QuickAction getQuickActionMenu();
}
