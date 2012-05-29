package com.soundcloud.android.adapter;

import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.widget.BaseAdapter;
import com.soundcloud.android.model.User;

public interface IScAdapter {
    long getItemId(int position);

    Drawable getDrawableFromId(Long id);

    Boolean getIconNotReady(Long id);

    void assignDrawableToId(Long id, Drawable drawable);

    void setIconNotReady(Long id);

    void onEndOfList();
}
