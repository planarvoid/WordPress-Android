package com.soundcloud.android.api;

import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum ApiEndpoints {
    // onboarding
    SUGGESTED_USER_CATEGORIES("/suggestions/users/categories"),
    SUGGESTED_USER_FACEBOOK_CATEGORIES("/suggestions/users/social/facebook"),

    // explore
    EXPLORE_TRACKS_POPULAR_MUSIC("/suggestions/tracks/popular/music"),
    EXPLORE_TRACKS_POPULAR_AUDIO("/suggestions/tracks/popular/audio"),
    EXPLORE_TRACKS_CATEGORIES("/suggestions/tracks/categories"),
    RELATED_TRACKS("/tracks/%s/related"),

    // search
    SEARCH_TRACKS("/search/tracks"),
    SEARCH_USERS("/search/users"),
    SEARCH_PLAYLISTS("/search/playlists"),
    SEARCH_ALL("/search/universal"),

    // playlist dicovery
    PLAYLIST_DISCOVERY("/suggestions/playlists"),
    PLAYLIST_DISCOVERY_TAGS("/suggestions/playlists/tags"),

    // personalized recommendations
    RECOMMENDATIONS("/you/personalized-tracks"),

    // ads + monetization
    ADS("/tracks/%s/ads"),
    AUDIO_AD("/tracks/%s/ads/audio"),
    POLICIES("/policies/tracks"),

    // consumer subs
    PRODUCTS("/products/google-play"),
    CHECKOUT("/checkout"),
    CHECKOUT_URN("/checkout/%s"),

    // likes
    LIKED_TRACKS("/likes/tracks"),
    LIKED_PLAYLISTS("/likes/playlists"),
    CREATE_TRACK_LIKES("/likes/tracks/create"),
    DELETE_TRACK_LIKES("/likes/tracks/delete"),
    CREATE_PLAYLIST_LIKES("/likes/playlists/create"),
    DELETE_PLAYLIST_LIKES("/likes/playlists/delete"),

    // posts
    MY_TRACK_POSTS("/you/posts_and_reposts/tracks"),
    MY_PLAYLIST_POSTS("/you/posts_and_reposts/playlists"),

    // profile
    MY_DOB("/you/profile/dob"),

    // playlist
    PLAYLISTS_CREATE("/playlists"),
    PLAYLISTS_FETCH("/playlists/fetch"),
    PLAYLIST_WITH_TRACKS("/playlists/%s/info"),
    PLAYLIST_ADD_TRACK("/playlists/%s/tracks"),
    PLAYLIST_REMOVE_TRACK("/playlists/%s/tracks/%s"),

    // stations
    STATION("/stations/%s/meta_and_tracks"),

    // other
    HLS_STREAM("/tracks/%s/streams/hls"),
    HTTP_STREAM("/tracks/%s/streams/http"),
    HTTPS_STREAM("/tracks/%s/streams/https"),
    SUBSCRIPTIONS("/subscriptions"),
    LOG_PLAY("/tracks/%s/plays"),
    IMAGES("/images/%s/%s"),
    STREAM("/stream"),
    TRACKS_FETCH("/tracks/fetch"),
    CONFIGURATION("/configuration/android"),

    // public API
    PLAY_PUBLISH("/tpub"),

    // public API (DEPRECATED)
    CURRENT_USER("/me"),
    RESOLVE("/resolve"),
    MY_TRACK_REPOSTS("/e1/me/track_reposts/%s"),
    MY_PLAYLIST_REPOSTS("/e1/me/playlist_reposts/%s"),
    BULK_FOLLOW_USERS("/me/suggested/users"),
    LEGACY_TRACKS("/tracks"),
    LEGACY_TRACK("/tracks/%s"),
    LEGACY_USERS("/users"),
    LEGACY_USER("/users/%s"),
    TRACK_COMMENTS("/tracks/%s/comments"),

    USER_SOUNDS("/e1/users/%s/sounds"),
    USER_LIKES("/e1/users/%s/likes"),
    USER_PLAYLISTS("/users/%s/playlists"),
    USER_FOLLOWINGS("/users/%s/followings"),
    USER_FOLLOWERS("/users/%s/followers"),

    OAUTH2_TOKEN("/oauth2/token");

    private final String path;

    ApiEndpoints(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public String path(Object... pathParams) {
        List encodedParams = Lists.transform(Arrays.asList(pathParams), encodingFunction);
        return String.format(Locale.US, path, encodedParams.toArray());
    }

    public String unencodedPath(Object... pathParams) {
        List encodedParams = Arrays.asList(pathParams);
        return String.format(Locale.US, path, encodedParams.toArray());
    }

    private static final Function<Object, Object> encodingFunction = new Function<Object, Object>() {
        @Override
        public Object apply(Object input) {
            return Uri.encode(String.valueOf(input));
        }
    };
}
