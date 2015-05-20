package com.soundcloud.android.playback;

import android.os.Build;

public interface PlaybackConstants {

    static final String ONE_PLUS_CM = "bacon";

    boolean FORCE_MEDIA_PLAYER = (ONE_PLUS_CM.equalsIgnoreCase(Build.HARDWARE)
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP);

    long PROGRESS_DELAY_MS = 500L;
}
