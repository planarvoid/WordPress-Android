package com.soundcloud.android.model;

import android.content.*;
import android.widget.*;

public interface Playable {
    Track getTrack();
    CharSequence getTimeSinceCreated(Context context);
}
