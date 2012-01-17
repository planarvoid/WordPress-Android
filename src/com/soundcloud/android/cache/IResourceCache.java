package com.soundcloud.android.cache;

import android.database.Cursor;
import android.os.Parcelable;

public interface IResourceCache {
    public Parcelable fromListItem(Parcelable listItem);
    public Parcelable fromCursor(Cursor cursor);
}
