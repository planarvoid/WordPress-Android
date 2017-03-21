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
    public static final String RECENTLY_PLAYED = "recently_played";
    public static final String PLAYLIST = "playlist";
    public static final String SINGLE = "single";
    public static final String SUGGESTED_CREATORS = "followings:suggestions";
    public static final String RECOMMENDED_PLAYLISTS = "playlist_discovery";
    public static final String MORE_PLAYLISTS_BY_USER = "more_playlists_by_user";
    public static final String SEARCH_TOP_RESULT = "search:top_result";
    public static final String SEARCH_HIGH_TIER = "search:high_tier";
    public static final String SEARCH_TRACKS = "search:tracks";
    public static final String SEARCH_PEOPLE = "search:people";
    public static final String SEARCH_ALBUMS = "search:albums";
    public static final String SEARCH_PLAYLISTS = "search:playlists";


    public static Module create(String name, int position) {
        return new AutoValue_Module(name, position);
    }

    public abstract String getName();

    public abstract int getPosition();
}
