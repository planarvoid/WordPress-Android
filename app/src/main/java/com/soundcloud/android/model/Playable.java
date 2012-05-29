package com.soundcloud.android.model;

import android.content.*;
import android.os.Parcelable;
import android.widget.*;

public interface Playable extends Parcelable {
    Track getTrack();
    CharSequence getTimeSinceCreated(Context context);
    void refreshTimeSinceCreated(Context context);
}
