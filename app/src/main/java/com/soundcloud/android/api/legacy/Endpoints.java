package com.soundcloud.android.api.legacy;

/**
 * Various SoundCloud API endpoints.
 * See <a href="http://developers.soundcloud.com/docs/api/">the API docs</a> for the most
 * recent listing.
 */
public interface Endpoints {
    String TOKEN = "/oauth2/token";

    String TRACKS = "/tracks";
    String TRACK_DETAILS = "/tracks/%d";
    String TRACK_FAVORITERS = "/tracks/%d/favoriters";

    String PLAYLISTS = "/playlists";

    String USERS = "/users";
    String USER_DETAILS = "/users/%d";
    String USER_FOLLOWINGS = "/users/%d/followings";
    String USER_FOLLOWERS = "/users/%d/followers";

    String MY_DETAILS = "/me";
    String MY_FOLLOWERS = "/me/followers";
    String MY_FOLLOWINGS = "/me/followings";
    String MY_FOLLOWING = "/me/followings/%d";

    String SUGGESTED_USERS = "/users/suggested";

    String RESOLVE = "/resolve";

    String SEND_PASSWORD = "/passwords/reset-instructions";
}
