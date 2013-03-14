package com.soundcloud.android.model;

import android.content.ContentValues;
import android.net.Uri;

public interface ModelLike {
    public long getId();
    public Uri toUri();
    public ContentValues buildContentValues();
}
