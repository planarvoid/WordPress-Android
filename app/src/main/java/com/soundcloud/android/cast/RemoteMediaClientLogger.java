package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.utils.Log;

public class RemoteMediaClientLogger {

    private static final String TAG = "RemoteMediaClientLogger";

    public static void logState(String message, RemoteMediaClient remoteMediaClient) {
        Log.d(TAG, "PlayerState::" + message + ", status = " + getPlayerStateAsString(remoteMediaClient) + ", queue = " + getRemoteQueue(remoteMediaClient));
    }

    private static String getRemoteQueue(RemoteMediaClient remoteMediaClient) {
        if (remoteMediaClient.getMediaInfo() == null) {
            return "null mediaInfo";
        }

        if (remoteMediaClient.getMediaInfo().getCustomData() == null) {
            return "null customData";
        }

        return remoteMediaClient.getMediaInfo().getCustomData().toString();
    }

    private static String getPlayerStateAsString(RemoteMediaClient remoteMediaClient) {
        switch (remoteMediaClient.getPlayerState()) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                return "PLAYER_STATE_PLAYING";
            case MediaStatus.PLAYER_STATE_PAUSED:
                return "PLAYER_STATE_PAUSED";
            case MediaStatus.PLAYER_STATE_BUFFERING:
                return "PLAYER_STATE_BUFFERING";
            case MediaStatus.PLAYER_STATE_IDLE:
                return "PLAYER_STATE_IDLE";
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                return "PLAYER_STATE_UNKNOWN";
            default:
                return "UNDEFINED STATE";
        }
    }
}
