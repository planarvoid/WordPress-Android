package com.soundcloud.android;

@Deprecated
public interface TempEndpoints {

    String PLAYLISTS            = "/playlists";
    String PLAYLIST_DETAILS     = "/playlists/%d";
    String PLAYLIST_TRACKS      = "/playlists/%d/tracks";

    public interface e1 {
        String MY_STREAM         = "/e1/me/stream";
        String MY_ACTIVITIES     = "/e1/me/activities";
        String MY_REPOSTS        = "/e1/me/track_reposts";
        String MY_REPOST         = "/e1/me/track_reposts/%d";
        String USER_REPOSTS      = "/e1/users/%d/reposts";
        String USER_LIKES        = "/e1/users/%d/likes";
        String MY_SOUNDS         = "/e1/me/sounds";
        String MY_SOUNDS_MINI    = "/e1/me/sounds/mini";
        String MY_LIKES          = "/e1/me/likes";
        String USER_SOUNDS       = "/e1/users/%d/sounds";
        String TRACK_REPOSTERS   = "/e1/tracks/%d/reposters";
    }

    public interface i1 {
        String ME_FACEBOOK_TOKEN  = "/i1/me/facebook_token";
        String MY_SHORTCUTS       = "/i1/me/shortcuts";
    }
}
