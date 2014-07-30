package com.soundcloud.android.ads;

import java.util.concurrent.TimeUnit;

public interface AdConstants {
    int UNSKIPPABLE_TIME_SECS = 15;
    long UNSKIPPABLE_TIME_MS = TimeUnit.SECONDS.toMillis(UNSKIPPABLE_TIME_SECS);
}
