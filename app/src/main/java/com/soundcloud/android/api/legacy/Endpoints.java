package com.soundcloud.android.api.legacy;

/**
 * Various SoundCloud API endpoints.
 * See <a href="http://developers.soundcloud.com/docs/api/">the API docs</a> for the most
 * recent listing.
 */
public interface Endpoints {
    String TOKEN = "/oauth2/token";

    String TRACKS = "/tracks";

    String PLAYLISTS = "/playlists";

    String MY_DETAILS = "/me";
    String MY_FOLLOWINGS = "/me/followings";
    String MY_FOLLOWING = "/me/followings/%d";

    String RESOLVE = "/resolve";

    String SEND_PASSWORD = "/passwords/reset-instructions";
}
