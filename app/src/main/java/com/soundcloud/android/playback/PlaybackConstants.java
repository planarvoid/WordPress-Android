package com.soundcloud.android.playback;

import android.os.Build;

public interface PlaybackConstants {

    static final String ONE_PLUS_CM = "bacon";

    boolean FORCE_MEDIA_PLAYER = ONE_PLUS_CM.equalsIgnoreCase(Build.HARDWARE);
    long PROGRESS_DELAY_MS = 500L;
}
