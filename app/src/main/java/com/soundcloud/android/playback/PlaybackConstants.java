package com.soundcloud.android.playback;

public interface PlaybackConstants {

    //boolean FORCE_MEDIA_PLAYER = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
    boolean FORCE_MEDIA_PLAYER = true; // since Skippy is now broken on OnePlus phones
    long PROGRESS_DELAY_MS = 500L;
}
