package com.soundcloud.android.activities;

import java.util.EnumSet;

public enum ActivityKind {

    UNKNOWN("unknown"),
    TRACK_LIKE("track_like"),
    PLAYLIST_LIKE("playlist_like"),
    TRACK_REPOST("track_repost"),
    PLAYLIST_REPOST("playlist_repost"),
    TRACK_COMMENT("track_comment"),
    USER_FOLLOW("user_follow");

    public static final EnumSet<ActivityKind> PLAYABLE_RELATED = EnumSet.of(TRACK_LIKE, PLAYLIST_LIKE,
            TRACK_REPOST, PLAYLIST_REPOST, TRACK_COMMENT);

    private final String identifier;

    ActivityKind(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return identifier;
    }

    public String identifier() {
        return identifier;
    }

    public static ActivityKind fromIdentifier(String identifier) {
        for (ActivityKind kind : values()) {
            if (kind.identifier.equals(identifier)) {
                return kind;
            }
        }
        return UNKNOWN;
    }
}
