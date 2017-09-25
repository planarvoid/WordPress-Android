package com.soundcloud.android.deeplinks;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

public enum DeepLink {
    HOME,
    STREAM,
    DISCOVERY,
    SEARCH,
    RECORD,
    THE_UPLOAD,
    WEB_VIEW,
    TRACKED_REDIRECT,
    STATION,
    ENTITY,
    SOUNDCLOUD_GO_PLUS_UPSELL,
    SOUNDCLOUD_GO_BUY,
    SOUNDCLOUD_GO_PLUS_BUY,
    SOUNDCLOUD_GO_CHOICE,
    SOUNDCLOUD_GO_PLUS_CHOICE,
    NOTIFICATION_PREFERENCES,
    COLLECTION,
    OFFLINE_SETTINGS,
    CHARTS,
    TRACK_ENTITY,
    PLAYLIST_ENTITY,
    SYSTEM_PLAYLIST_ENTITY,
    USER_ENTITY,
    SHARE_APP,
    SYSTEM_SETTINGS,
    REMOTE_SIGN_IN,
    UNKNOWN,

    // Navigation Target Deeplinks

    ACTIVITIES,
    FOLLOWERS,
    FOLLOWINGS,
    SEARCH_AUTOCOMPLETE,
    AD_FULLSCREEN_VIDEO,
    AD_PRESTITIAL,
    AD_CLICKTHROUGH,
    SYSTEM_PLAYLIST,
    PLAYLISTS_AND_ALBUMS_COLLECTION,
    PLAYLISTS_COLLECTION,
    PLAYLISTS,
    PROFILE,
    PROFILE_REPOSTS,
    PROFILE_TRACKS,
    PROFILE_LIKES,
    PROFILE_ALBUMS,
    PROFILE_PLAYLISTS,
    LIKED_STATIONS,
    HELP_CENTER,
    LEGAL,
    BASIC_SETTINGS,
    EXTERNAL_APP;

    public static final String SOUNDCLOUD_SCHEME = "soundcloud";

    @VisibleForTesting
    static final EnumSet<DeepLink> LOGGED_IN_REQUIRED =
            EnumSet.of(REMOTE_SIGN_IN,
                       DISCOVERY,
                       SEARCH,
                       RECORD,
                       THE_UPLOAD,
                       ENTITY,
                       STATION,
                       TRACK_ENTITY,
                       PLAYLIST_ENTITY,
                       SYSTEM_PLAYLIST_ENTITY,
                       USER_ENTITY,
                       SOUNDCLOUD_GO_PLUS_UPSELL,
                       SOUNDCLOUD_GO_BUY,
                       SOUNDCLOUD_GO_PLUS_BUY,
                       SOUNDCLOUD_GO_CHOICE,
                       SOUNDCLOUD_GO_PLUS_CHOICE,
                       NOTIFICATION_PREFERENCES,
                       COLLECTION,
                       OFFLINE_SETTINGS,
                       CHARTS,
                       PROFILE,
                       SYSTEM_PLAYLIST,
                       PLAYLISTS_AND_ALBUMS_COLLECTION,
                       PLAYLISTS_COLLECTION,
                       PLAYLISTS,
                       PROFILE,
                       PROFILE_REPOSTS,
                       PROFILE_TRACKS,
                       PROFILE_LIKES,
                       PROFILE_ALBUMS,
                       PROFILE_PLAYLISTS,
                       LIKED_STATIONS,
                       BASIC_SETTINGS);

    @VisibleForTesting
    static final EnumSet<DeepLink> RESOLVE_REQUIRED =
            EnumSet.of(ENTITY,
                       TRACK_ENTITY,
                       USER_ENTITY,
                       PLAYLIST_ENTITY,
                       SYSTEM_PLAYLIST_ENTITY,
                       TRACKED_REDIRECT);

    private static final Pattern[] WEB_VIEW_URL_PATTERNS = {
            Pattern.compile("^/login/reset/[0-9a-f]+$"),
            Pattern.compile("^/login/forgot$"),
            Pattern.compile("^/signin/forgot$"),
            Pattern.compile("^/emails/[0-9a-f]+$"),
            Pattern.compile("^/pro(/.*)?$"),
            Pattern.compile("^/pages/.*$"),
            Pattern.compile("^/terms-of-use$"),
            Pattern.compile("^/connect(/.*)?$"),
            Pattern.compile("^/jobs(/.*)?$")
    };

    private static final Pattern[] TRACKED_REDIRECT_URL_PATTERNS = {
            Pattern.compile("^/-/.*$"),
    };

    private static final Pattern[] LEGACY_DEEPLINKS = {
            Pattern.compile(".*suggestedtracks_all/*$"),
            Pattern.compile(".*suggested_tracks/all/*$"),
    };

    public boolean requiresLoggedInUser() {
        return LOGGED_IN_REQUIRED.contains(this);
    }

    public boolean requiresResolve() {
        return RESOLVE_REQUIRED.contains(this);
    }

    @NonNull
    public static DeepLink fromUri(@Nullable Uri uri) {
        if (uri == null) {
            return HOME;
        } else if (isLegacyDeeplink(uri)) {
            return HOME;
        } else if (isHierarchicalSoundCloudScheme(uri)) {
            return fromHierarchicalSoundCloudScheme(uri);
        } else if (isWebScheme(uri)) {
            return fromWebScheme(uri);
        } else {
            return UNKNOWN;
        }
    }

    public static boolean isClickTrackingUrl(Uri uri) {
        return "soundcloud.com".equals(uri.getHost())
                && uri.getPath().startsWith("/-/t/click")
                && !TextUtils.isEmpty(uri.getQueryParameter("url"));
    }

    public static Uri extractClickTrackingRedirectUrl(@NonNull Uri uri) {
        String url = uri.getQueryParameter("url");
        return TextUtils.isEmpty(url) ? Uri.EMPTY : Uri.parse(url);
    }

    private static boolean isLegacyDeeplink(Uri uri) {
        for (Pattern pattern : LEGACY_DEEPLINKS) {
            if (pattern.matcher(uri.toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWebViewUrl(Uri uri) {
        for (Pattern pattern : WEB_VIEW_URL_PATTERNS) {
            if (pattern.matcher(uri.getPath()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTrackedRedirect(Uri uri) {
        for (Pattern pattern : TRACKED_REDIRECT_URL_PATTERNS) {
            if (pattern.matcher(uri.getPath()).matches()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHierarchicalSoundCloudScheme(Uri uri) {
        return uri.isHierarchical() && SOUNDCLOUD_SCHEME.equals(uri.getScheme());
    }

    private static DeepLink fromHierarchicalSoundCloudScheme(Uri uri) {
        String host = uri.getHost();

        switch (host) {
            case "":
            case "home":
                return HOME;
            case "stream":
                return STREAM;
            case "discover":
                switch (uri.getPath()) {
                    case "":
                    case "/":
                        return DISCOVERY;
                    case "/new-tracks-for-you":
                        return THE_UPLOAD;
                    default:
                        return ENTITY;
                }
            case "search":
            case "search:people":
            case "search:sounds":
            case "search:sets":
            case "search:users":
            case "search:tracks":
            case "search:playlists":
                return SEARCH;
            case "collection":
                return COLLECTION;
            case "upload":
            case "record":
                return RECORD;
            case "the-upload":
                return THE_UPLOAD;
            case "stations":
                if (isStationsUrl("/stations" + uri.getPath())) {
                    return STATION;
                }
                return ENTITY;
            case "soundcloudgo":
            case "go":
            case "ht_modal":
                switch (uri.getPath()) {
                    case "/soundcloudgo":
                        return SOUNDCLOUD_GO_CHOICE;
                    case "/soundcloudgoplus":
                        return SOUNDCLOUD_GO_PLUS_CHOICE;
                    default:
                        return SOUNDCLOUD_GO_PLUS_UPSELL;
                }
            case "buysoundcloudgo":
            case "buy_mt":
                return SOUNDCLOUD_GO_BUY;
            case "buysoundcloudgoplus":
            case "buy_ht":
                return SOUNDCLOUD_GO_PLUS_BUY;
            case "settings":
                switch (uri.getPath()) {
                    case "/offline_listening":
                        return OFFLINE_SETTINGS;
                    case "/notification_preferences":
                        return NOTIFICATION_PREFERENCES;
                    default:
                        return ENTITY;
                }
            case "settings_offlinelistening":
                return OFFLINE_SETTINGS;
            case "notification_preferences":
                return NOTIFICATION_PREFERENCES;
            case "charts":
                return CHARTS;
            case "tracks":
            case "sounds":
                return TRACK_ENTITY;
            case "users":
                return USER_ENTITY;
            case "playlists":
                return PLAYLIST_ENTITY;
            case "system-playlists":
                return SYSTEM_PLAYLIST_ENTITY;
            case "share_app":
                return SHARE_APP;
            case "share":
                if ("/app".equals(uri.getPath())) {
                    return SHARE_APP;
                }
                return ENTITY;
            case "open-notification-settings":
                return SYSTEM_SETTINGS;
            case "remote-sign-in":
                return REMOTE_SIGN_IN;
            default:
                return ENTITY;
        }
    }

    public static boolean isWebScheme(Uri uri) {
        return uri.isHierarchical()
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && (uri.getHost() != null && uri.getHost().contains("soundcloud.com"));
    }

    private static DeepLink fromWebScheme(Uri uri) {
        switch (uri.getPath()) {
            case "":
            case "/":
            case "/home":
                return HOME;
            case "/stream":
                return STREAM;
            case "/upload":
                return RECORD;
            case "/discover/new-tracks-for-you":
            case "/the-upload":
                return THE_UPLOAD;
            case "/discover":
                return DISCOVERY;
            case "/charts":
                return CHARTS;
            case "/search":
            case "/search/sounds":
            case "/search/people":
            case "/search/sets":
            case "/search/tracks":
            case "/search/users":
            case "/search/playlists":
            case "/tracks/search":
            case "/people/search":
                return SEARCH;
            case "/ht_modal":
            case "/soundcloudgo":
            case "/go":
                return SOUNDCLOUD_GO_PLUS_UPSELL;
            case "/soundcloudgo/soundcloudgo":
            case "/go/soundcloudgo":
                return SOUNDCLOUD_GO_CHOICE;
            case "/soundcloudgo/soundcloudgoplus":
            case "/go/soundcloudgoplus":
                return SOUNDCLOUD_GO_PLUS_CHOICE;
            case "/go/buy/go":
                return SOUNDCLOUD_GO_BUY;
            case "/go/buy/go-plus":
                return SOUNDCLOUD_GO_PLUS_BUY;
            case "/notification_preferences":
            case "/settings/notifications":
            case "/settings/notification_preferences":
                return NOTIFICATION_PREFERENCES;
            case "/settings_offlinelistening":
            case "/settings/offline_listening":
                return OFFLINE_SETTINGS;
            case "/share_app":
            case "/share/app":
                return SHARE_APP;
            case "/open-notification-settings":
                return SYSTEM_SETTINGS;
            case "/system-playlists":
                return SYSTEM_PLAYLIST_ENTITY;
            default:
                if (isRemoteSignIn(uri)) {
                    return REMOTE_SIGN_IN;
                } else if (isChartsUrl(uri)) {
                    return CHARTS;
                } else if (isStationsUrl(uri.getPath())) {
                    return STATION;
                } else if (isTrackedRedirect(uri)) {
                    return TRACKED_REDIRECT;
                } else if (isWebViewUrl(uri)) {
                    return WEB_VIEW;
                } else {
                    return ENTITY;
                }
        }
    }

    private static boolean isRemoteSignIn(Uri uri) {
        List<String> segments = uri.getPathSegments();
        return segments != null && !segments.isEmpty() && segments.get(0).equals("activate");
    }

    private static boolean isStationsUrl(String path) {
        return path.startsWith("/stations/track/") || path.startsWith("/stations/artist/");
    }

    private static boolean isChartsUrl(Uri uri) {
        return uri.getPath().startsWith("/charts");
    }
}
