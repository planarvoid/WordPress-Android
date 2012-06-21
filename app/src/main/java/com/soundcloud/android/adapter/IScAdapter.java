package com.soundcloud.android.adapter;

import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.widget.BaseAdapter;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.quickaction.QuickAction;

public interface IScAdapter {
    long getItemId(int position);

    Drawable getDrawableFromId(Long id);

    Boolean getIconNotReady(Long id);

    void assignDrawableToId(Long id, Drawable drawable);

    void setIconNotReady(Long id);

    void onEndOfList();

    Content getContent();

    QuickAction getQuickActionMenu();
}
