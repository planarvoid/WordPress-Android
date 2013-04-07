package com.soundcloud.android.model;

import android.net.Uri;

public interface ModelLike {
    long getId();
    void setId(long id);
    Uri toUri();
}
