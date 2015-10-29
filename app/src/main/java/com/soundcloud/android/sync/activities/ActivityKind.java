package com.soundcloud.android.sync.activities;

public enum ActivityKind {

    TRACK_LIKE("track_like"),
    PLAYLIST_LIKE("playlist_like"),
    TRACK_REPOST("track_repost"),
    PLAYLIST_REPOST("playlist_repost"),
    TRACK_COMMENT("track_comment"),
    USER_FOLLOW("user_follow");

    private final String name;

    ActivityKind(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String tableConstant() {
        return name;
    }
}
