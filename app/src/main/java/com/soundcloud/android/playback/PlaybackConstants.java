package com.soundcloud.android.playback;

import android.os.Build;

public interface PlaybackConstants {

    boolean FORCE_MEDIA_PLAYER = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    long PROGRESS_DELAY_MS = 500L;
}
