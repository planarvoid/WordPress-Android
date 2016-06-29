package com.soundcloud.android.api;

import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum ApiEndpoints {

    // gcm
    GCM_REGISTER("/push/register"),
    GCM_DEREGISTER("/push/deregister"),

    // explore
    EXPLORE_TRACKS_POPULAR_MUSIC("/suggestions/tracks/popular/music"),
    EXPLORE_TRACKS_POPULAR_AUDIO("/suggestions/tracks/popular/audio"),
    EXPLORE_TRACKS_CATEGORIES("/suggestions/tracks/categories"),
    RELATED_TRACKS("/tracks/%s/related"),

    // search
    SEARCH_TRACKS("/search/tracks"),
    SEARCH_USERS("/search/users"),
    SEARCH_ALBUMS("/search/albums"),
    SEARCH_PLAYLISTS("/search/playlists"),
    SEARCH_PLAYLISTS_WITHOUT_ALBUMS("/search/playlists_without_albums"),
    SEARCH_ALL("/search/universal"),
    SEARCH_SUGGESTIONS("/search/suggest"),

    // search premium content
    SEARCH_PREMIUM_TRACKS("/search/tracks/premium_content"),
    SEARCH_PREMIUM_USERS("/search/users/premium_content"),
    SEARCH_PREMIUM_ALBUMS("/search/albums/premium_content"),
    SEARCH_PREMIUM_PLAYLISTS("/search/playlists/premium_content"),
    SEARCH_PREMIUM_ALL("/search/universal/premium_content"),

    // playlist dicovery
    PLAYLIST_DISCOVERY("/suggestions/playlists"),
    PLAYLIST_DISCOVERY_TAGS("/suggestions/playlists/tags"),

    // personalized recommendations
    TRACK_RECOMMENDATIONS("/you/personalized-tracks"),
    STATION_RECOMMENDATIONS("/you/personalized-stations"),

    // charts
    CHARTS_FEATURED("/charts/featured"),
    CHARTS_GENRES("/charts/genres"),
    CHARTS("/charts"),

    // ads + monetization
    ADS("/tracks/%s/ads"),
    POLICIES("/policies/tracks"),

    // consumer subs
    WEB_PRODUCTS("/products/android-web"),
    NATIVE_PRODUCTS("/products/google-play"),
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
    USER_POSTS("/users/%s/posted_and_reposted_tracks_and_playlists"),
    USER_POSTED_PLAYLISTS("/users/%s/posted_playlists"),

    // tracks
    TRACKS("/tracks/%s"),
    TRACKS_FETCH("/tracks/fetch"),

    // playlist
    PLAYLISTS_CREATE("/playlists"),
    PLAYLISTS_UPDATE("/playlists/%s"),
    PLAYLISTS_DELETE("/playlists/%s"),
    PLAYLISTS_FETCH("/playlists/fetch"),
    PLAYLIST_WITH_TRACKS("/playlists/%s/info"),
    PLAYLIST_ADD_TRACK("/playlists/%s/tracks"),
    PLAYLIST_REMOVE_TRACK("/playlists/%s/tracks/%s"),

    // profile
    PROFILE("/users/%s/profile/v2"),
    USER_REPOSTS("/users/%s/reposts"),
    USER_TRACKS("/users/%s/tracks/posted"),
    USER_ALBUMS("/users/%s/albums/posted"),
    USER_LIKES("/users/%s/likes"),
    USER_PLAYLISTS("/users/%s/playlists/posted"),

    // stations
    STATION("/stations/%s/station_and_tracks"),
    STATIONS("/stations"),

    // timeline features
    STREAM("/stream"),
    ACTIVITIES("/activities"),

    // streams
    STREAMS_TO_HLS("/streams/to-hls"),
    HLS_STREAM("/tracks/%s/streams/hls"),
    HLS_SNIPPET_STREAM("/tracks/%s/streams/hls/snippet"),
    HTTP_STREAM("/tracks/%s/streams/http"),
    HTTPS_STREAM("/tracks/%s/streams/https"),

    // other
    SUBSCRIPTIONS("/subscriptions"),
    LOG_PLAY("/tracks/%s/plays"),
    IMAGES("/images/%s/%s"),
    CONFIGURATION("/configuration/android"),
    DEVICE_REGISTRATION("/device/registration"),
    RESOLVE_ENTITY("/resolve"),
    ME("/me"),
    NOTIFICATION_PREFERENCES("/notification_preferences"),

    // public API
    PLAY_PUBLISH("/tpub"),

    // public API (DEPRECATED)
    CURRENT_USER("/me"),
    MY_TRACK_REPOSTS("/e1/me/track_reposts/%s"),
    MY_PLAYLIST_REPOSTS("/e1/me/playlist_reposts/%s"),
    LEGACY_TRACKS("/tracks"),
    LEGACY_TRACK("/tracks/%s"),
    LEGACY_USERS("/users"),
    LEGACY_USER("/users/%s"),
    TRACK_COMMENTS("/tracks/%s/comments"),

    LEGACY_USER_PLAYLISTS("/users/%s/playlists"),
    LEGACY_USER_FOLLOWINGS("/users/%s/followings"),
    LEGACY_USER_FOLLOWERS("/users/%s/followers"),
    LEGACY_USER_LIKES("/users/%s/liked_tracks_and_playlists"),

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
