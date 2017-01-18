package com.soundcloud.android.playback;

import java.util.EnumSet;

public enum PlayStateReason {
    NONE, PLAYBACK_COMPLETE, ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN, CAST_DISCONNECTED;

    public static final EnumSet<PlayStateReason> ERRORS =
            EnumSet.of(ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN);

    public static final EnumSet<PlayStateReason> PLAYBACK_STOPPED =
            EnumSet.of(PLAYBACK_COMPLETE, ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN);
}
