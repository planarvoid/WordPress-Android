package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.PlayQueueManager;

public class QueueUtils {

    static final float ALPHA_DISABLED = 0.3f;
    static final float ALPHA_ENABLED = 1.0f;


    static float getAlpha(PlayQueueManager.RepeatMode repeatMode, PlayState playstate) {
        switch (repeatMode) {
            case REPEAT_NONE:
                if (playstate == PlayState.PLAYED) {
                    return ALPHA_DISABLED;
                } else {
                    return ALPHA_ENABLED;
                }
            case REPEAT_ONE:
                if (playstate == PlayState.PLAYING || playstate == PlayState.PAUSED) {
                    return ALPHA_ENABLED;
                } else {
                    return ALPHA_DISABLED;
                }
            case REPEAT_ALL:
                return ALPHA_ENABLED;
            default:
                throw new IllegalStateException("Unknown value of repeat mode");
        }
    }


}
