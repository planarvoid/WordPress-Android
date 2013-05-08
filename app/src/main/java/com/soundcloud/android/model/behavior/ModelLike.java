package com.soundcloud.android.model.behavior;

import android.net.Uri;

public interface ModelLike {
    long getId();
    void setId(long id);
    Uri toUri();
}
