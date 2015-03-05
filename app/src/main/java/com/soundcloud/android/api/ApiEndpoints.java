package com.soundcloud.android.api;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

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
    MY_PLAYLIST_POSTS("/posts-and-reposts/playlists"), //does not exist yet

    // profile
    MY_DOB("/you/profile/dob"),

    // playlist
    PLAYLISTS_CREATE("/playlists"),
    PLAYLISTS_FETCH("/playlists/fetch"),
    PLAYLIST_WITH_TRACKS("/playlists/%s/info"),
    PLAYLIST_ADD_TRACK("/playlists/%s/tracks"),
    PLAYLIST_REMOVE_TRACK("/playlists/%s/tracks/%s"),

    // other
    EXPERIMENTS("/experiments/%s"),
    HLS_STREAM("/tracks/%s/streams/hls"),
    SUBSCRIPTIONS("/subscriptions"),
    LOG_PLAY("/tracks/%s/plays"),
    IMAGES("/images/%s/%s"),
    STREAM("/stream"),
    TRACKS_FETCH("/tracks/fetch"),
    CONFIGURATION("/configuration/android"),

    // public API (DEPRECATED)
    CURRENT_USER("/me"),
    MY_TRACK_LIKES("/e1/me/track_likes/%s"),
    MY_TRACK_REPOSTS("/e1/me/track_reposts/%s"),
    MY_PLAYLIST_LIKES("/e1/me/playlist_likes/%s"),
    MY_PLAYLIST_REPOSTS("/e1/me/playlist_reposts/%s"),
    BULK_FOLLOW_USERS("/me/suggested/users"),
    LEGACY_SEARCH_ALL("/search"),
    LEGACY_SEARCH_TRACKS("/search/sounds"),
    LEGACY_SEARCH_PLAYLISTS("/search/sets"),
    LEGACY_SEARCH_USERS("/search/people"),
    TRACK_COMMENTS("/tracks/%s/comments");

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
