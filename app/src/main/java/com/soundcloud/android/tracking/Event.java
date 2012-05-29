package com.soundcloud.android.tracking;

import com.at.ATParams;

public interface Event {
    ATParams atParams(Object... args);
    Level2 level2();
}
