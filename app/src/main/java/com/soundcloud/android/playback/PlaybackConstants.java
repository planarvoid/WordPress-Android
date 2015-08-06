package com.soundcloud.android.playback;

import android.os.Build;

public interface PlaybackConstants {

    String MARSHALL_BRAND = "Marshall";
    String MARSHALL_MODEL = "London";

    // Force MP for marshall, as Skippy does not support their EQ needs yet
    boolean FORCE_MEDIA_PLAYER = MARSHALL_MODEL.equalsIgnoreCase(Build.MODEL)
            && MARSHALL_BRAND.equals(Build.BRAND);

    long PROGRESS_DELAY_MS = 500L;
}
