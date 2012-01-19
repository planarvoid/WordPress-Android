package com.soundcloud.android.provider;

import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Parcelable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Content {
    ME("me", null, 100, User.class, -1),
    ME_TRACKS("me/tracks", Endpoints.MY_TRACKS, 101, Track.class, TRACK),
    ME_COMMENTS("me/comments", null, 102, Comment.class, -1),
    ME_FOLLOWINGS("me/followings", Endpoints.MY_FOLLOWINGS, 103, User.class, FOLLOWING),
    ME_FOLLOWINGS_ITEM("/me/followings/#", null, 104, User.class, -1),
    ME_FOLLOWERS("me/followers", Endpoints.MY_FOLLOWERS, 105, User.class, FOLLOWER),
    ME_FOLLOWERS_ITEM("me/followers/#", null, 106, User.class, -1),
    ME_FAVORITES("me/favorites", Endpoints.MY_FAVORITES, 107, Track.class, FAVORITE),
    ME_FAVORITES_ITEM("me/favorites/#", null, 108, Track.class, FAVORITE),
    ME_GROUPS("me/groups", null, 109, null, -1),
    ME_PLAYLISTS("me/playlists", null, 110, null, -1),

    // the ids of the following entries should not be changed, they are referenced in th db
    ME_SOUND_STREAM("me/activities/tracks", Endpoints.MY_ACTIVITIES, 140, Activity.class, -1),
    ME_EXCLUSIVE_STREAM("me/activities/tracks/exclusive", Endpoints.MY_EXCLUSIVE_TRACKS, 141, Activity.class, -1),
    ME_ACTIVITIES("me/activities/all/own", Endpoints.MY_NEWS, 142, Activity.class, -1),
    ME_ALL_ACTIVITIES("me/activities", null, 150, Activity.class, -1),

    ME_FRIENDS("me/connections/friends", Endpoints.MY_FRIENDS, 160, Friend.class, FRIEND),
    SUGGESTED_USERS("users/suggested", null, 161, User.class, SUGGESTED_USER),

    TRACKS("tracks", Endpoints.TRACKS, 201, Track.class, TRACK),
    TRACK_ITEM("tracks/#", null, 202, Track.class, -1),
    TRACK_COMMENTS("tracks/#/comments", null, 203, Comment.class, -1),
    TRACK_PERMISSIONS("tracks/#/permissions", null, 204, null, -1),
    TRACK_SECRET_TOKEN("tracks/#/secret-token", null, 205, null, -1),

    USERS("users", Endpoints.USERS, 301, User.class, -1),
    USER_ITEM("users/#", null, 302, User.class, -1),
    USER_TRACKS("users/#/tracks", null, 303, Track.class, TRACK),
    USER_FAVORITES("users/#/favorites", null, 304, Track.class, FAVORITE),
    USER_FOLLOWERS("users/#/followers", null, 305, User.class, FOLLOWER),
    USER_FOLLOWINGS("users/#/followings", null, 306, User.class, FOLLOWING),
    USER_COMMENTS("users/#/comments", null, 307, Comment.class, -1),
    USER_GROUPS("users/#/groups", null, 308, null, -1),
    USER_PLAYLISTS("users/#/playlists", null, 309, null, -1),

    COMMENTS("comments", null, 400, Comment.class, -1),
    COMMENT_ITEM("comments/#", null, 401, Comment.class, -1),

    PLAYLISTS("playlists", null, 501, null, -1),
    PLAYLIST_ITEMS("playlists/#", null, 502, null, -1),

    GROUPS("groups", null, 600, null, -1),
    GROUP_ITEM("groups/#", null, 602, null, -1),
    GROUP_USERS("groups/#/users", null, 603, User.class, -1),
    GROUP_MODERATORS("groups/#/moderators", null, 604, User.class, -1),
    GROUP_MEMBERS("groups/#/members", null, 605, User.class, -1),
    GROUP_CONTRIBUTORS("groups/#/contributors", null, 606, User.class, -1),
    GROUP_TRACKS("groups/#/tracks", null, 607, Track.class, -1),

    // LOCAL URIS
    COLLECTION_ITEMS("collection_items", null, 1000, null, -1),
    COLLECTIONS("collections", null, 1001, null, -1),
    COLLECTION_PAGES("collection_pages", null, 1002, null, -1),

    RECORDINGS("recordings", null, 1100, Recording.class, -1),
    RECORDING_ITEM("recordings/#", null, 1101, Recording.class, -1),

    TRACK_PLAYS("track_plays", null, 1300, null, -1),
    TRACK_PLAYS_ITEM("track_plays/#", null, 1301, null, -1),

    SEARCHES("searches", null, 1400, null, -1),
    SEARCHES_ITEM("searches/#", null, 1401, null, -1),
    SEARCHES_TRACKS("searches/tracks", null, 1402, Track.class, -1),
    SEARCHES_USERS("searches/users", null, 1403, User.class, -1),
    SEARCHES_TRACKS_ITEM("searches/tracks/*", null, 1404, Track.class, -1),
    SEARCHES_USERS_ITEM("searches/users/*", null, 1405, User.class, -1),

    TRACK_CLEANUP("cleanup/tracks", null, 9998, null, -1),
    USERS_CLEANUP("cleanup/users", null, 9999, null, -1),

    UNKNOWN(null, null, -1, null, -1)
    ;


    Content(String uri, String remoteUri, int id, Class<? extends Parcelable> resourceType, int collectionType) {
        this.uriPath =  uri;
        this.uri = Uri.parse("content://" + ScContentProvider.AUTHORITY + "/" + uriPath);
        this.id = id;
        this.resourceType = resourceType;
        this.collectionType = collectionType;
        this.remoteUri = remoteUri;
    }

    public final int collectionType;
    public final int id;
    public final Class<? extends Parcelable> resourceType;
    public final Uri uri;
    public final String uriPath;
    public final String remoteUri;

    static final private UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static final private Map<Integer, Content> sMap = new HashMap<Integer, Content>();
    static final private Map<Uri, Content> sUris = new HashMap<Uri, Content>();

    public static final int SYNCABLE_CEILING = 150;
    public static final int MINE_CEILING     = 200;

    public static final EnumSet<Content> ACTIVITIES = EnumSet.of(
        Content.ME_ACTIVITIES,
        Content.ME_SOUND_STREAM,
        Content.ME_EXCLUSIVE_STREAM
    );

    static {
        for (Content c : Content.values()) {
            if (c.id >= 0 && c.uri != null) {
                sMatcher.addURI(ScContentProvider.AUTHORITY, c.uriPath, c.id);
                sMap.put(c.id, c);
                sUris.put(c.uri, c);
            }
        }
    }

    public boolean isSyncable() {
        return id < SYNCABLE_CEILING;
    }

    public boolean isMine() {
        return id < MINE_CEILING;
    }

    public Uri.Builder buildUpon() {
        return uri.buildUpon();
    }

    public Request request() {
        if (remoteUri != null) {
            return Request.to(remoteUri);
        } else {
            throw new IllegalArgumentException("no remoteuri defined for content"+this);
        }
    }

    @Override
    public String toString() {
        return "Content." + name();
    }

    public static Content match(Uri uri) {
        if (uri == null) return null;
        final int match = sMatcher.match(uri);
        return match != -1 ? sMap.get(match) : UNKNOWN;
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

    public List<Long> getStoredIds(ContentResolver resolver, int pageIndex){
        return ApiSyncer.idCursorToList(resolver.query(
                        pageIndex == -1 ? Content.COLLECTION_ITEMS.uri : CloudUtils.getPagedUri(Content.COLLECTION_ITEMS.uri, pageIndex),
                        new String[]{DBHelper.CollectionItems.ITEM_ID},
                        DBHelper.CollectionItems.COLLECTION_TYPE + " = ?",
                        new String[]{String.valueOf(collectionType)},
                        DBHelper.CollectionItems.SORT_ORDER));

    }
}
