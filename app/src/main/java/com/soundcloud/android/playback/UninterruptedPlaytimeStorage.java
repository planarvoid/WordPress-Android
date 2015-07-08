package com.soundcloud.android.playback;

import com.soundcloud.android.events.PlayerType;

import android.content.SharedPreferences;

import javax.inject.Inject;

class UninterruptedPlaytimeStorage {

    private static final String TIME_FOR_MEDIA_PLAYER = "uninterrupted_play_time_ms_media_player";
    private static final String TIME_FOR_SKIPPY = "uninterrupted_play_time_ms_skippy";

    private final SharedPreferences sharedPreferences;

    @Inject
    UninterruptedPlaytimeStorage(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    long getPlayTime(PlayerType playerType) {
        return sharedPreferences.getLong(getPlayerTypeKey(playerType), 0L);
    }

    void setPlaytime(long time, PlayerType playerType) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(getPlayerTypeKey(playerType), time);
        editor.apply();
    }

    private String getPlayerTypeKey(PlayerType playerType) {
        if (playerType.getValue().equals(PlayerType.MEDIA_PLAYER.getValue())) {
            return TIME_FOR_MEDIA_PLAYER;
        } else {
            return TIME_FOR_SKIPPY;
        }
    }

}
