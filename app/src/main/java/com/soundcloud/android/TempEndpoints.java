package com.soundcloud.android;

@Deprecated
public interface TempEndpoints {

    String PLAYLISTS            = "/playlists";
    String PLAYLIST_DETAILS     = "/playlists/%d";
    String PLAYLIST_TRACKS      = "/playlists/%d/tracks";
    String MY_PLAYLISTS         = "/me/playlists";
    String USER_PLAYLISTS       = "/users/%d/playlists";

    public interface e1 {
        String MY_STREAM           = "/e1/me/stream";
        String MY_ACTIVITIES       = "/e1/me/activities";
        String MY_TRACK_LIKES      = "/e1/me/track_likes";
        String MY_TRACK_LIKE       = "/e1/me/track_likes/%d";
        String MY_TRACK_REPOSTS    = "/e1/me/track_reposts";
        String MY_TRACK_REPOST     = "/e1/me/track_reposts/%d";
        String MY_PLAYLIST_LIKES   = "/e1/me/playlist_likes";
        String MY_PLAYLIST_LIKE    = "/e1/me/playlist_likes/%d";
        String MY_PLAYLIST_REPOSTS = "/e1/me/playlist_reposts";
        String MY_PLAYLIST_REPOST  = "/e1/me/playlist_reposts/%d";
        String USER_REPOSTS        = "/e1/users/%d/reposts";
        String USER_LIKES          = "/e1/users/%d/likes";
        String MY_SOUNDS           = "/e1/me/sounds";
        String MY_SOUNDS_MINI      = "/e1/me/sounds/mini";
        String MY_LIKES            = "/e1/me/likes";
        String USER_SOUNDS         = "/e1/users/%d/sounds";
        String TRACK_REPOSTERS     = "/e1/tracks/%d/reposters";
        String PLAYLIST_LIKERS     = "/e1/playlists/%d/likers";
        String PLAYLIST_REPOSTERS  = "/e1/playlists/%d/reposters";
    }

    public interface i1 {
        String ME_FACEBOOK_TOKEN  = "/i1/me/facebook_token";
        String MY_SHORTCUTS       = "/i1/me/shortcuts";
    }
}
