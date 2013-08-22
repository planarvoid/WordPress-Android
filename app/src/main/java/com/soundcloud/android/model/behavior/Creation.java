package com.soundcloud.android.model.behavior;

import android.content.Context;

public interface Creation {
    CharSequence getTimeSinceCreated(Context context);
    void refreshTimeSinceCreated(Context context);
}
