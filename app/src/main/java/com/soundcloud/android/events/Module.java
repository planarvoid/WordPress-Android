package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Module {
    public static final String STREAM = "stream";
    public static final String USER_SPOTLIGHT = "users-spotlight";
    public static final String USER_TRACKS = "users-tracks";
    public static final String USER_ALBUMS = "users-albums";
    public static final String USER_PLAYLISTS = "users-playlists";
    public static final String USER_REPOSTS = "users-reposts";
    public static final String USER_LIKES = "users-likes";
    public static final String USER_FOLLOWING = "users-followings";
    public static final String USER_FOLLOWERS = "users-followers";
    public static final String RECENTLY_PLAYED = "recently-played";
    public static final String PLAYLIST = "playlist";
    public static final String SINGLE = "single";

    public static Module create(String name, int position) {
        return new AutoValue_Module(name, position);
    }

    public abstract String getName();

    public abstract int getPosition();
}
