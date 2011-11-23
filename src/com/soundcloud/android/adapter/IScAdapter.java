package com.soundcloud.android.adapter;

import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.widget.BaseAdapter;
import com.soundcloud.android.model.User;

public interface IScAdapter {
    Drawable getDrawableFromPosition(int position);

    Boolean getIconLoading(int position);

    void assignDrawableToPosition(Integer position, Drawable drawable);

    void setIconLoading(Integer position);

    void onEndOfList();
}
