package com.soundcloud.android.api.legacy;

@Deprecated
public interface TempEndpoints {

    String PLAYLISTS            = "/playlists";
    String PLAYLIST_DETAILS     = "/playlists/%d";
    String PLAYLIST_TRACKS      = "/playlists/%d/tracks";
    String MY_PLAYLISTS         = "/me/playlists";
    String USER_PLAYLISTS       = "/users/%d/playlists";

    interface e1 {
        String MY_STREAM           = "/e1/me/stream";
        String MY_ACTIVITIES       = "/e1/me/activities";
        String MY_TRACK_LIKE       = "/e1/me/track_likes/%d";
        String MY_TRACK_REPOST     = "/e1/me/track_reposts/%d";
        String MY_PLAYLIST_LIKE    = "/e1/me/playlist_likes/%d";
        String MY_PLAYLIST_REPOST  = "/e1/me/playlist_reposts/%d";
        String USER_REPOSTS        = "/e1/users/%d/reposts";
        String USER_LIKES          = "/e1/users/%d/likes";
        String MY_SOUNDS_MINI      = "/e1/me/sounds/mini";
        String USER_SOUNDS         = "/e1/users/%d/sounds";
        String TRACK_REPOSTERS     = "/e1/tracks/%d/reposters";
        String PLAYLIST_LIKERS     = "/e1/playlists/%d/likers";
        String PLAYLIST_REPOSTERS  = "/e1/playlists/%d/reposters";
    }

    interface i1 {
        String MY_SHORTCUTS       = "/i1/me/shortcuts";
    }
}
