package com.soundcloud.android.storage.provider;

import com.soundcloud.android.sync.SyncConfig;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;

public enum Content {
    // these are still used but should be ported off of Content enum
    ME_LIKES("me/likes"),
    ME_SOUNDS("me/sounds"),
    ME_SOUND_STREAM("me/stream"),
    ME_ACTIVITIES("me/activities/all/own"),
    ME_FOLLOWINGS("me/followings"),
    ME_FOLLOWING("me/followings/#"),

    // legacy stuff
    ME("me"),
    ME_PLAYLISTS("me/playlists"),
    TRACKS("tracks"),
    TRACK("tracks/#"),
    /* Use string wildcards here since we use negative numbers for local playlists, which breaks with number wildcards */
    PLAYLIST("playlists/*"),

    SEARCH_ITEM("search/*"),
    UNKNOWN(null);


    Content(String uri) {
        this.uriPath = uri;
        this.uri = Uri.parse("content://" + SyncConfig.AUTHORITY + "/" + uriPath);
    }

    public final Uri uri;
    private final String uriPath;

    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final SparseArray<Content> ORDINAL_TO_CONTENT_MAP = new SparseArray<>();

    static {
        for (Content content : Content.values()) {
            if (content != UNKNOWN) {
                MATCHER.addURI(SyncConfig.AUTHORITY, content.uriPath, content.ordinal());
                ORDINAL_TO_CONTENT_MAP.put(content.ordinal(), content);
            }
        }
    }

    private Uri.Builder buildUpon() {
        return uri.buildUpon();
    }

    public Uri forId(long id) {
        final String uriString = uri.toString();
        if (uriString.contains("#")) {
            return Uri.parse(uriString.replace("#", String.valueOf(id)));
        } else if (uriString.contains("*")) {
            return Uri.parse(uriString.replace("*", String.valueOf(id)));
        } else {
            return buildUpon().appendEncodedPath(String.valueOf(id)).build();
        }
    }

    @Override
    public String toString() {
        return "Content." + name();
    }

    public static Content match(Uri uri) {
        if (uri == null) {
            return null;
        }
        final int match = MATCHER.match(uri);

        return match != -1 ? ORDINAL_TO_CONTENT_MAP.get(match) : UNKNOWN;
    }
}
