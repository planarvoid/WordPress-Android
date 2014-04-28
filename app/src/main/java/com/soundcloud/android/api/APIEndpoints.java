package com.soundcloud.android.api;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum APIEndpoints {
    // mobile API
    SUGGESTED_USER_CATEGORIES("/suggestions/users/categories"),
    SUGGESTED_USER_FACEBOOK_CATEGORIES("/suggestions/users/social/facebook"),
    EXPLORE_TRACKS_POPULAR_MUSIC("/suggestions/tracks/popular/music"),
    EXPLORE_TRACKS_POPULAR_AUDIO("/suggestions/tracks/popular/audio"),
    EXPLORE_TRACKS_CATEGORIES("/suggestions/tracks/categories"),
    PLAYLIST_DISCOVERY("/suggestions/playlists"),
    PLAYLIST_DISCOVERY_TAGS("/suggestions/playlists/tags"),
    RELATED_TRACKS("/tracks/%s/related"),
    EXPERIMENTS("/experiments/%s"),
    HLS_STREAM("/tracks/%s/streams/hls"),

    // public API (DEPRECATED)
    CURRENT_USER("/me"),
    MY_TRACK_LIKES("/e1/me/track_likes"),
    MY_TRACK_REPOSTS("/e1/me/track_reposts"),
    MY_PLAYLIST_LIKES("/e1/me/playlist_likes"),
    MY_PLAYLIST_REPOSTS("/e1/me/playlist_reposts"),
    BULK_FOLLOW_USERS("/me/suggested/users"),
    SEARCH_ALL("/search"),
    SEARCH_TRACKS("/search/sounds"),
    SEARCH_PLAYLISTS("/search/sets"),
    SEARCH_USERS("/search/people");

    private final String path;

    APIEndpoints(String path) {
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
