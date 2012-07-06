package com.soundcloud.android.provider;

import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import android.util.SparseArray;

import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Parcelable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Content {
    ME("me", Endpoints.MY_DETAILS, 100, User.class, -1, Table.USERS),
    ME_TRACKS("me/tracks", Endpoints.MY_TRACKS, 101, Track.class, ScContentProvider.CollectionItemTypes.TRACK, Table.COLLECTION_ITEMS),
    ME_COMMENTS("me/comments", null, 102, Comment.class, -1, Table.COMMENTS),
    ME_FOLLOWINGS("me/followings", Endpoints.MY_FOLLOWINGS, 103, User.class, FOLLOWING, Table.COLLECTION_ITEMS),
    ME_FOLLOWING("me/followings/#", null, 104, User.class, -1, null),
    ME_FOLLOWERS("me/followers", Endpoints.MY_FOLLOWERS, 105, User.class, FOLLOWER, Table.COLLECTION_ITEMS),
    ME_FOLLOWER("me/followers/#", null, 106, User.class, -1, null),
    ME_FAVORITES("me/favorites", Endpoints.MY_FAVORITES, 107, Track.class, FAVORITE, Table.COLLECTION_ITEMS),
    ME_FAVORITE("me/favorites/#", null, 108, Track.class, FAVORITE, null),
    ME_GROUPS("me/groups", null, 109, null, -1, null),
    ME_PLAYLISTS("me/playlists", null, 110, null, -1, null),
    ME_USERID("me/userid", null, 111, null, -1, null),

    // the ids of the following entries should not be changed, they are referenced in th db
    ME_SOUND_STREAM("me/activities/tracks", Endpoints.MY_ACTIVITIES, 140, Activity.class, -1, Table.ACTIVITIES),
    ME_EXCLUSIVE_STREAM("me/activities/tracks/exclusive", Endpoints.MY_EXCLUSIVE_TRACKS, 141, Activity.class, -1, Table.ACTIVITIES),
    ME_ACTIVITIES("me/activities/all/own", Endpoints.MY_NEWS, 142, Activity.class, -1, Table.ACTIVITIES),
    ME_ALL_ACTIVITIES("me/activities", null, 150, Activity.class, -1, Table.ACTIVITIES),

    ME_FRIENDS("me/connections/friends", Endpoints.MY_FRIENDS, 160, User.class, FRIEND, null),
    SUGGESTED_USERS("users/suggested", null, 161, User.class, SUGGESTED_USER, null),

    TRACKS("tracks", Endpoints.TRACKS, 201, Track.class, ScContentProvider.CollectionItemTypes.TRACK, Table.TRACKS),
    TRACK("tracks/#", null, 202, Track.class, -1, Table.TRACKS),
    TRACK_ARTWORK("tracks/#/artwork", null, 203, null, -1, Table.TRACKS),
    TRACK_COMMENTS("tracks/#/comments", null, 204, Comment.class, -1, Table.COMMENTS),
    TRACK_PERMISSIONS("tracks/#/permissions", null, 205, null, -1, null),
    TRACK_SECRET_TOKEN("tracks/#/secret-token", null, 206, null, -1, null),

    USERS("users", Endpoints.USERS, 301, User.class, -1, Table.USERS),
    USER("users/#", null, 302, User.class, -1, Table.USERS),
    USER_TRACKS("users/#/tracks", null, 303, Track.class, ScContentProvider.CollectionItemTypes.TRACK, Table.TRACKS),
    USER_FAVORITES("users/#/favorites", null, 304, Track.class, FAVORITE, null),
    USER_FOLLOWERS("users/#/followers", null, 305, User.class, FOLLOWER, null),
    USER_FOLLOWINGS("users/#/followings", null, 306, User.class, FOLLOWING, null),
    USER_COMMENTS("users/#/comments", null, 307, Comment.class, -1, null),
    USER_GROUPS("users/#/groups", null, 308, null, -1, null),
    USER_PLAYLISTS("users/#/playlists", null, 309, null, -1, null),

    COMMENTS("comments", null, 400, Comment.class, -1, Table.COMMENTS),
    COMMENT("comments/#", null, 401, Comment.class, -1, Table.COMMENTS),

    PLAYLISTS("playlists", null, 501, null, -1, Table.PLAYLIST_ITEMS),
    PLAYLIST("playlists/#", null, 502, null, -1, Table.PLAYLIST_ITEMS),

    GROUPS("groups", null, 600, null, -1, null),
    GROUP("groups/#", null, 602, null, -1, null),
    GROUP_USERS("groups/#/users", null, 603, User.class, -1, null),
    GROUP_MODERATORS("groups/#/moderators", null, 604, User.class, -1, null),
    GROUP_MEMBERS("groups/#/members", null, 605, User.class, -1, null),
    GROUP_CONTRIBUTORS("groups/#/contributors", null, 606, User.class, -1, null),
    GROUP_TRACKS("groups/#/tracks", null, 607, Track.class, -1, null),

    // LOCAL URIS
    COLLECTIONS("collections", null, 1000, null, -1, Table.COLLECTIONS),
    COLLECTION("collections/#", null, 1001, null, -1, Table.COLLECTIONS),
    COLLECTION_PAGES("collection_pages", null, 1002, null, -1, Table.COLLECTION_PAGES),
    COLLECTION_PAGE("collection_pages/#", null, 1003, null, -1, Table.COLLECTION_PAGES),
    COLLECTION_ITEMS("collection_items", null, 1004, null, -1, Table.COLLECTION_ITEMS),
    COLLECTION_ITEM("collection_items/#", null, 1005, null, -1, Table.COLLECTION_ITEMS),

    RECORDINGS("recordings", null, 1100, Recording.class, -1, Table.RECORDINGS),
    RECORDING("recordings/#", null, 1101, Recording.class, -1, Table.RECORDINGS),

    TRACK_PLAYS("track_plays", null, 1300, null, -1, Table.TRACK_METADATA),
    TRACK_PLAYS_ITEM("track_plays/#", null, 1301, null, -1, Table.TRACK_METADATA),
    TRACK_METADATA("track_metadata", null, 1302, null, -1, Table.TRACK_METADATA),

    SEARCHES("searches", null, 1400, null, -1, Table.SEARCHES),
    SEARCH("searches/#", null, 1401, null, -1, Table.SEARCHES),
    SEARCHES_TRACKS("searches/tracks", null, 1402, Track.class, -1, null),
    SEARCHES_USERS("searches/users", null, 1403, User.class, -1, null),
    SEARCHES_TRACK("searches/tracks/*", null, 1404, Track.class, ScContentProvider.CollectionItemTypes.SEARCH, null),
    SEARCHES_USER("searches/users/*", null, 1405, User.class, ScContentProvider.CollectionItemTypes.SEARCH, null),

    TRACK_CLEANUP("cleanup/tracks", null, 9998, null, -1, null),
    USERS_CLEANUP("cleanup/users", null, 9999, null, -1, null),

    //Android global search
    ANDROID_SEARCH_SUGGEST(SearchManager.SUGGEST_URI_PATH_QUERY, null, 10000, null, -1, null),
    ANDROID_SEARCH_SUGGEST_PATH(SearchManager.SUGGEST_URI_PATH_QUERY + "/*", null, 10001, null, -1, null),
    ANDROID_SEARCH_REFRESH(SearchManager.SUGGEST_URI_PATH_SHORTCUT, null, 10002, null, -1, null),
    ANDROID_SEARCH_REFRESH_PATH(SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", null, 10003, null, -1, null),

    UNKNOWN(null, null, -1, null, -1, null);


    Content(String uri, String remoteUri, int id, Class<? extends Parcelable> resourceType,
            int collectionType,
            Table table) {
        this.uriPath = uri;
        this.uri = Uri.parse("content://" + ScContentProvider.AUTHORITY + "/" + uriPath);
        this.id = id;
        this.resourceType = resourceType;
        this.collectionType = collectionType;
        this.remoteUri = remoteUri;
        this.table = table;
    }

    public final int collectionType;
    public final int id;
    public final
    @Nullable
    Class<? extends Parcelable> resourceType;
    public final Uri uri;
    public final String uriPath;
    public final String remoteUri;
    public final Table table;

    static final private UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static final private SparseArray<Content> sMap = new SparseArray<Content>();
    static final private Map<Uri, Content> sUris = new HashMap<Uri, Content>();

    public static final int SYNCABLE_CEILING = 150;
    public static final int MINE_CEILING = 200;

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

    public boolean isCollectionItem() {
        return table == Table.COLLECTION_ITEMS;
    }

    public boolean isMine() {
        return id < MINE_CEILING;
    }

    public Uri.Builder buildUpon() {
        return uri.buildUpon();
    }

    public Uri withQuery(String... args) {
        if (args.length % 2 != 0) throw new IllegalArgumentException("need even params");

        Uri.Builder builder = buildUpon();
        for (int i = 0; i < args.length; i += 2) {
            builder.appendQueryParameter(args[i], args[i + 1]);
        }
        return builder.build();
    }

    public Uri forId(long id) {
        if (uri.toString().contains("#")) {
            return Uri.parse(uri.toString().replace("#", String.valueOf(id)));
        } else {
            return buildUpon().appendEncodedPath(String.valueOf(id)).build();
        }
    }

    public Request request() {
        if (remoteUri != null) {
            return Request.to(remoteUri);
        } else {
            throw new IllegalArgumentException("no remoteuri defined for content" + this);
        }
    }

    public boolean hasRequest() {
        return remoteUri != null;
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



    public List<Long> getLocalIds(ContentResolver resolver, long userId) {
        return getLocalIds(resolver, userId, -1, -1);
    }

    public List<Long> getLocalIds(ContentResolver resolver, long userId, int startIndex, int limit) {
        return SoundCloudDB.idCursorToList(resolver.query(
                SoundCloudDB.addPagingParams(Content.COLLECTION_ITEMS.uri, startIndex, limit),
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[]{String.valueOf(collectionType), String.valueOf(userId)},
                DBHelper.CollectionItems.SORT_ORDER));
    }

    public boolean isStale(long lastSync) {
        // do not auto refresh users when the list opens, because users are always changing
        if (resourceType == User.class) return lastSync <= 0;
        final long staleTime = (resourceType == Track.class) ? SyncConfig.TRACK_STALE_TIME :
                (resourceType == Activity.class) ? SyncConfig.ACTIVITY_STALE_TIME :
                        SyncConfig.DEFAULT_STALE_TIME;

        return System.currentTimeMillis() - lastSync > staleTime;
    }
}
