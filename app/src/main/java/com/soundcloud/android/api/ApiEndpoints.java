package com.soundcloud.android.api;

import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum ApiEndpoints {

    // auth
    SIGN_IN("/sign_in"),
    SIGN_UP("/sign_up"),
    RESET_PASSWORD("/users/passwords/reset"),

    // gcm
    GCM_REGISTER("/push/register"),
    GCM_DEREGISTER("/push/deregister"),

    RELATED_TRACKS("/tracks/%s/related"),

    // search
    SEARCH_TRACKS("/search/tracks"),
    SEARCH_USERS("/search/users"),
    SEARCH_ALBUMS("/search/albums"),
    SEARCH_PLAYLISTS_WITHOUT_ALBUMS("/search/playlists_without_albums"),
    SEARCH_ALL("/search/universal"),
    SEARCH_SUGGESTIONS("/search/suggest"),
    SEARCH_AUTOCOMPLETE("/search/autocomplete"),

    // search premium content
    SEARCH_PREMIUM_TRACKS("/search/tracks/premium_content"),
    SEARCH_PREMIUM_USERS("/search/users/premium_content"),
    SEARCH_PREMIUM_ALBUMS("/search/albums/premium_content"),
    SEARCH_PREMIUM_PLAYLISTS("/search/playlists/premium_content"),
    SEARCH_PREMIUM_ALL("/search/universal/premium_content"),

    // playlist dicovery
    PLAYLIST_DISCOVERY("/suggestions/playlists"),
    PLAYLIST_DISCOVERY_TAGS("/suggestions/playlists/tags"),

    // recommended playlists
    RECOMMENDED_PLAYLISTS("/suggestions/playlists/fresh"),

    // personalized recommendations
    TRACK_RECOMMENDATIONS("/you/personalized-tracks"),
    STATION_RECOMMENDATIONS("/you/personalized-stations"),

    // charts
    CHARTS_FEATURED("/charts/featured"),
    CHARTS_GENRES("/charts/genres"),
    CHARTS("/charts"),

    // suggested creators
    SUGGESTED_CREATORS("/you/suggested-creators"),

    // ads + monetization
    ADS("/tracks/%s/ads"),
    INLAY_ADS("/stream/ads"),
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

    // followings
    MY_FOLLOWINGS("/you/followings"),
    FOLLOWINGS("/followings/%s/users"),
    FOLLOWERS("/followers/%s/users"),
    USER_FOLLOWS("/follows/users/%s"),

    // profile
    MY_DOB("/you/profile/dob"),
    USER_POSTS("/users/%s/posted_and_reposted_tracks_and_playlists"),

    TRACKS_FETCH("/tracks/fetch"),
    USERS_FETCH("/users/fetch"),

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
    STATIONS_LIKED("/stations/liked"),
    STATIONS_FETCH("/stations/fetch"),
    STATIONS_MIGRATE_RECENT_TO_LIKED("/stations/recent/migrate-to-liked"),

    // timeline features
    STREAM("/stream"),
    ACTIVITIES("/activities"),
    STREAM_HIGHLIGHTS("/stream/highlights"),

    // streams
    HLS_STREAM("/tracks/%s/streams/hls"),
    HLS_SNIPPET_STREAM("/tracks/%s/streams/hls/snippet"),
    HTTP_STREAM("/tracks/%s/streams/http"),
    HTTPS_STREAM("/tracks/%s/streams/https"),

    IMAGES("/images/%s/%s"),
    CONFIGURATION("/configuration/android"),
    DEVICE_REGISTRATION("/device/registration"),
    RESOLVE_ENTITY("/resolve"),
    ME("/me"),
    NOTIFICATION_PREFERENCES("/notification_preferences"),

    // play history
    PLAY_HISTORY("/recently-played/tracks"),
    CLEAR_PLAY_HISTORY("/recently-played/tracks/all"),

    // recently played
    RECENTLY_PLAYED("/recently-played/contexts"),
    CLEAR_RECENTLY_PLAYED("/recently-played/contexts/all"),

    // public API
    PLAY_PUBLISH("/tpub"),

    // public API (DEPRECATED)
    CURRENT_USER("/me"),
    MY_TRACK_REPOSTS("/e1/me/track_reposts/%s"),
    MY_PLAYLIST_REPOSTS("/e1/me/playlist_reposts/%s"),
    LEGACY_USERS("/users"),
    TRACK_COMMENTS("/tracks/%s/comments"),

    OAUTH2_TOKEN("/oauth2/token"),
    LEGACY_RESET_PASSWORD("/passwords/reset-instructions");

    private static final Function<Object, Object> encodingFunction = input -> Uri.encode(String.valueOf(input));
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
}
