package com.soundcloud.android.deeplinks;

import com.soundcloud.android.model.Urn;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.EnumSet;
import java.util.regex.Pattern;

public enum DeepLink {
    HOME, STREAM, EXPLORE, USER, TRACK, PLAYLIST, SEARCH, RECORD, WEB_VIEW, OTHER;

    private static final EnumSet<DeepLink> LOGGED_IN_REQUIRED = EnumSet.of(EXPLORE, USER, TRACK, PLAYLIST, SEARCH, RECORD);
    private static final EnumSet<DeepLink> RESOLVE_REQUIRED = EnumSet.of(USER, TRACK, PLAYLIST);

    private static final Pattern[] WEB_VIEW_URL_PATTERNS = {
            Pattern.compile("^/login/reset/[0-9a-f]+$"),
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
        } else if (isSoundCloudUrn(uri)) {
            return fromSoundCloudUrn(new Urn(uri.toString()));
        } else if (isSoundCloudScheme(uri)) {
            return fromSoundCloudScheme(uri);
        } else if (isWebScheme(uri)) {
            return fromWebScheme(uri);
        } else {
            return OTHER;
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

    private static boolean isSoundCloudUrn(Uri uri) {
        return uri.isOpaque() && Urn.isSoundCloudUrn(uri.toString());
    }

    private static DeepLink fromSoundCloudUrn(Urn urn) {
        if (urn.isTrack()) {
            return TRACK;
        } else if (urn.isPlaylist()) {
            return PLAYLIST;
        } else if (urn.isUser()) {
            return USER;
        } else {
            return OTHER;
        }
    }

    private static boolean isSoundCloudScheme(Uri uri) {
        return uri.isHierarchical() && Urn.SOUNDCLOUD_SCHEME.equals(uri.getScheme());
    }

    private static DeepLink fromSoundCloudScheme(Uri uri) {
        String host = uri.getHost();

        switch (host) {
            case "home":
                return HOME;
            case "stream":
                return STREAM;
            case "explore":
                return EXPLORE;
            case "search":
            case "search:people":
            case "search:sounds":
            case "search:sets":
            case "search:users":
            case "search:tracks":
            case "search:playlists":
                return SEARCH;
            case "upload":
            case "record":
                return RECORD;
            default:
                try {
                    UrnResolver resolver = new UrnResolver();
                    return fromSoundCloudUrn(resolver.toUrn(uri));
                } catch (IllegalArgumentException e) {
                    return OTHER;
                }
        }
    }

    private static boolean isWebScheme(Uri uri) {
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
            case "/explore":
                return EXPLORE;
            case "/upload":
                return RECORD;
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
            default:
                if (isWebViewUrl(uri)) {
                    return WEB_VIEW;
                } else {
                    return OTHER;
                }
        }
    }
}
