package com.soundcloud.android.deeplinks;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.util.EnumSet;
import java.util.regex.Pattern;

public enum DeepLink {
    HOME, 
    STREAM, 
    TRACK_RECOMMENDATIONS,
    DISCOVERY, 
    SEARCH,
    RECORD,
    WEB_VIEW,
    ENTITY,
    SOUNDCLOUD_GO_PLUS_UPSELL,
    SOUNDCLOUD_GO_PLUS_BUY,
    SOUNDCLOUD_GO_CHOICE,
    SOUNDCLOUD_GO_PLUS_CHOICE,
    NOTIFICATION_PREFERENCES,
    COLLECTION,
    OFFLINE_SETTINGS,
    CHARTS,
    CHARTS_ALL_GENRES,
    TRACK_ENTITY,
    PLAYLIST_ENTITY,
    USER_ENTITY,
    SHARE_APP,
    SYSTEM_SETTINGS;

    public static final String SOUNDCLOUD_SCHEME = "soundcloud";

    @VisibleForTesting
    static final EnumSet<DeepLink> LOGGED_IN_REQUIRED =
            EnumSet.of(TRACK_RECOMMENDATIONS,
                       DISCOVERY,
                       SEARCH,
                       RECORD,
                       ENTITY,
                       TRACK_ENTITY,
                       PLAYLIST_ENTITY,
                       USER_ENTITY,
                       SOUNDCLOUD_GO_PLUS_UPSELL,
                       SOUNDCLOUD_GO_PLUS_BUY,
                       SOUNDCLOUD_GO_CHOICE,
                       SOUNDCLOUD_GO_PLUS_CHOICE,
                       NOTIFICATION_PREFERENCES,
                       COLLECTION,
                       OFFLINE_SETTINGS,
                       CHARTS,
                       CHARTS_ALL_GENRES);

    @VisibleForTesting
    static final EnumSet<DeepLink> RESOLVE_REQUIRED =
            EnumSet.of(ENTITY,
                       TRACK_ENTITY,
                       USER_ENTITY,
                       PLAYLIST_ENTITY);

    private static final Pattern[] WEB_VIEW_URL_PATTERNS = {
            Pattern.compile("^/login/reset/[0-9a-f]+$"),
            Pattern.compile("^/login/forgot$"),
            Pattern.compile("^/emails/[0-9a-f]+$"),
            Pattern.compile("^/pages/.*$"),
            Pattern.compile("^/terms-of-use$"),
            Pattern.compile("^/jobs(/.*)?$")
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
        } else if (isHierarchicalSoundCloudScheme(uri)) {
            return fromHierarchicalSoundCloudScheme(uri);
        } else if (isWebScheme(uri)) {
            return fromWebScheme(uri);
        } else {
            return ENTITY;
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

    private static boolean isWebViewUrl(Uri uri) {
        for (Pattern pattern : WEB_VIEW_URL_PATTERNS) {
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
            case "suggestedtracks_all":
                return TRACK_RECOMMENDATIONS;
            case "suggested_tracks":
                if ("/all".equals(uri.getPath())) {
                    return TRACK_RECOMMENDATIONS;
                }
                return ENTITY;
            case "discovery":
                return DISCOVERY;
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
            case "soundcloudgo":
            case "go":
            case "ht_modal":
                switch(uri.getPath()) {
                    case "/soundcloudgo":
                        return SOUNDCLOUD_GO_CHOICE;
                    case "/soundcloudgoplus":
                        return SOUNDCLOUD_GO_PLUS_CHOICE;
                    default:
                        return SOUNDCLOUD_GO_PLUS_UPSELL;
                }
            case "buysoundcloudgo":
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
                String authority = uri.getAuthority();
                if (authority.equals("charts:audio") || authority.equals("charts:music")) {
                    return CHARTS_ALL_GENRES;
                }
                return CHARTS;
            case "tracks":
            case "sounds":
                return TRACK_ENTITY;
            case "users":
                return USER_ENTITY;
            case "playlists":
                return PLAYLIST_ENTITY;
            case "share_app":
                return SHARE_APP;
            case "share":
                if ("/app".equals(uri.getPath())) {
                    return SHARE_APP;
                }
                return ENTITY;
            case "open-notification-settings":
                return SYSTEM_SETTINGS;
            default:
                return ENTITY;
        }
    }

    public static boolean isWebScheme(Uri uri) {
        return uri.isHierarchical()
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
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
            case "/discover":
            case "/suggestedtracks_all":
            case "/suggested_tracks/all":
                return TRACK_RECOMMENDATIONS;
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
            case "/buy_ht":
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
            default:
                if (isChartsUrl(uri)) {
                    return CHARTS;
                }
                else if (isWebViewUrl(uri)) {
                    return WEB_VIEW;
                } else {
                    return ENTITY;
                }
        }
    }

    private static boolean isChartsUrl(Uri uri) {
        return uri.getPath().startsWith("/charts");
    }
}
