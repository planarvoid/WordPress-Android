package com.soundcloud.android.provider;

import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;
import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.TRACK;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Endpoints;

import android.content.UriMatcher;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public enum Content {
    ME("/me", null, 100, -1),
    ME_TRACKS("/me/tracks", Endpoints.MY_TRACKS, 101, TRACK),
    ME_COMMENTS("/me/comments", null, 102, -1),
    ME_FOLLOWINGS("/me/followings", Endpoints.MY_FOLLOWINGS, 103, FOLLOWING),
    ME_FOLLOWINGS_ITEM("/me/followings/#", null, 104, -1),
    ME_FOLLOWERS("/me/followers", Endpoints.MY_FOLLOWERS, 105, FOLLOWER),
    ME_FOLLOWERS_ITEM("/me/followers/#", null, 106, -1),
    ME_FAVORITES("/me/favorites", Endpoints.MY_FAVORITES, 107, FAVORITE),
    ME_FAVORITES_ITEM("/me/favorites/#", null, 108, FAVORITE),
    ME_GROUPS("/me/groups", null, 109, -1),
    ME_PLAYLISTS("/me/playlists", null, 110, -1),

    ME_FRIENDS("/me/connections/friends", Endpoints.MY_FRIENDS, 160, FRIEND),
    SUGGESTED_USERS("/users/suggested", null, 161, SUGGESTED_USER),

    ME_SOUND_STREAM("/me/activities/tracks", Endpoints.MY_ACTIVITIES, 150, -1),
    ME_EXCLUSIVE_STREAM("/me/activities/tracks/exclusive", Endpoints.MY_EXCLUSIVE_TRACKS, 151, -1),
    ME_ACTIVITIES("/me/activities/all/own", Endpoints.MY_NEWS, 152, -1),

    TRACKS("/tracks", null, 201, TRACK),
    TRACK_ITEM("/tracks/#", null, 202, -1),
    TRACK_COMMENTS("/tracks/#/comments", null, 203, -1),
    TRACK_PERMISSIONS("/tracks/#/permissions", null, 204, -1),
    TRACK_SECRET_TOKEN("/tracks/#/secret-token", null, 205, -1),


    USERS("/users", null, 301, -1),
    USER_ITEM("/users/#", null, 302, -1),
    USER_TRACKS("/users/#/tracks", null, 303, TRACK),
    USER_FAVORITES("/users/#/favorites", null, 304, FAVORITE),
    USER_FOLLOWERS("/users/#/followers", null, 305, FOLLOWER),
    USER_FOLLOWINGS("/users/#/followings", null, 306, FOLLOWING),
    USER_COMMENTS("/users/#/comments", null, 307, -1),
    USER_GROUPS("/users/#/groups", null, 308, -1),
    USER_PLAYLISTS("/users/#/playlists", null, 309, -1),

    COMMENT_ITEM("/comments/#", null, 400, -1),

    PLAYLISTS("/playlists", null, 501, -1),
    PLAYLIST_ITEM("/playlists/#", null, 502, -1),

    GROUPS("/groups", null, 600, -1),
    GROUP_ITEM("/groups/#", null, 602, -1),
    GROUP_USERS("/groups/#/users", null, 603, -1),
    GROUP_MODERATORS("/groups/#/moderators", null, 604, -1),
    GROUP_MEMBERS("/groups/#/members", null, 605, -1),
    GROUP_CONTRIBUTORS("/groups/#/contributors", null, 606, -1),
    GROUP_TRACKS("/groups/#/tracks", null, 607, -1),

    // LOCAL URIS
    COLLECTION_ITEMS("/collection_items", null, 1000, -1),
    COLLECTIONS("/collections", null, 1001, -1),
    COLLECTION_PAGES("/collection_pages", null, 1002, -1),

    RECORDINGS("/recordings", null, 1100, -1),
    RECORDING_ITEM("/recordings/#", null, 1101, -1),

    EVENTS("/events", null, 1200, -1),
    EVENT_ITEM("/events/#", null, 1201, -1),

    TRACK_PLAYS("/track_plays", null, 1300, -1),
    TRACK_PLAYS_ITEM("/track_plays/#", null, 1301, -1),

    SEARCHES("/searches", null, 1400, -1),
    SEARCHES_ITEM("/searches/#", null, 1401, -1),
    SEARCHES_TRACKS("/searches/tracks", null, 1402, -1),
    SEARCHES_USERS("/searches/users", null, 1403, -1),
    SEARCHES_TRACKS_ITEM("/searches/tracks/*", null, 1404, -1),
    SEARCHES_USERS_ITEM("/searches/users/*", null, 1405, -1),


    TRACK_CLEANUP("/track_cleanup", null, 9998, -1),
    USERS_CLEANUP("/uses_cleanup", null, 9999, -1),
    UNKNOWN(null, null, -1, -1)
    ;


    Content(String uri, String remoteUri, int code, int collectionType) {
        this.uriPath =  uri;
        this.uri = Uri.parse("content://" + ScContentProvider.AUTHORITY + uriPath);
        this.code = code;
        this.collectionType = collectionType;
        this.remoteUri = remoteUri;
    }

    public final int collectionType;
    public final int code;
    public final Uri uri;
    public final String uriPath;
    public final String remoteUri;

    static final private UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static final private Map<Integer, Content> sMap = new HashMap<Integer, Content>();
    static final private Map<Uri, Content> sUris = new HashMap<Uri, Content>();

    public static final int SYNCABLE_CEILING = 150;

    static {
        for (Content c : Content.values()) {

            if (c.code >= 0 && c.uri != null) {
                sMatcher.addURI(ScContentProvider.AUTHORITY, c.uriPath.substring(1, c.uriPath.length()), c.code);
                sMap.put(c.code, c);
                sUris.put(c.uri, c);
            }
        }
    }

    public boolean isSyncable() {
        return code < SYNCABLE_CEILING;
    }

    public boolean isMine() {
        return code < 200;
    }

    public Uri.Builder buildUpon() {
        return uri.buildUpon();
    }

    public static Content match(Uri uri) {
        final int match = sMatcher.match(uri);
        return match != -1 ? sMap.get(match) : UNKNOWN;
    }

    public static boolean isSyncable(Uri uri) {
        return match(uri).isSyncable();
    }

    public static Content get(String s) {
        try {
            return Content.valueOf(s);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static Content byUri(Uri uri) {
        return sUris.get(uri);
    }

    public static Content fromEventType(int type) {
        switch (type) {
            case Consts.EventTypes.INCOMING:  return ME_SOUND_STREAM;
            case Consts.EventTypes.EXCLUSIVE: return ME_EXCLUSIVE_STREAM;
            case Consts.EventTypes.ACTIVITY:  return ME_ACTIVITIES;
            default:return UNKNOWN;
        }
    }

    public static Content forModel(Class<?> model) {
        if (Track.class.equals(model)) {
            return TRACKS;
        } else if (User.class.equals(model)) {
            return USERS;
        } else {
            throw new IllegalArgumentException("unknown model: " + model);
        }
    }
}
