package com.soundcloud.android.provider;

import static com.soundcloud.android.provider.ScContentProvider.CollectionItemTypes.*;

import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Shortcut;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.service.sync.SyncConfig;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Content  {
    ME("me", Endpoints.MY_DETAILS, 100, User.class, -1, Table.USERS),
    @Deprecated ME_TRACKS("me/tracks", Endpoints.MY_TRACKS, 101, Track.class, ScContentProvider.CollectionItemTypes.TRACK, Table.COLLECTION_ITEMS),
    ME_COMMENTS("me/comments", null, 102, Comment.class, -1, Table.COMMENTS),
    ME_FOLLOWINGS("me/followings", Endpoints.MY_FOLLOWINGS, 103, User.class, FOLLOWING, Table.COLLECTION_ITEMS),
    ME_FOLLOWING("me/followings/#", null, 104, User.class, -1, null),
    ME_FOLLOWERS("me/followers", Endpoints.MY_FOLLOWERS, 105, User.class, FOLLOWER, Table.COLLECTION_ITEMS),
    ME_FOLLOWER("me/followers/#", null, 106, User.class, -1, null),
    ME_LIKES("me/likes", TempEndpoints.e1.USER_LIKES, 107, SoundAssociation.class, LIKE, Table.COLLECTION_ITEMS),
    ME_LIKE("me/likes/#", null, 108, Track.class, LIKE, null),
    ME_REPOSTS("me/reposts", null, 109, null, REPOST, Table.COLLECTION_ITEMS),
    ME_PLAYLISTS("me/playlists", TempEndpoints.MY_PLAYLISTS, 110, Playlist.class, ScContentProvider.CollectionItemTypes.PLAYLIST, Table.COLLECTION_ITEMS),
    ME_USERID("me/userid", null, 111, null, -1, null),

    ME_PLAYLIST("me/playlists/*", null, 112, Playlist.class, ScContentProvider.CollectionItemTypes.PLAYLIST, Table.COLLECTION_ITEMS),

    ME_SHORTCUT("me/shortcuts/#", TempEndpoints.i1.MY_SHORTCUTS, 115, Shortcut.class, -1, Table.SUGGESTIONS),
    ME_SHORTCUTS("me/shortcuts", TempEndpoints.i1.MY_SHORTCUTS, 116, Shortcut.class, -1, Table.SUGGESTIONS),
    ME_SHORTCUTS_ICON("me/shortcut_icon/#", null, 117, null, -1, Table.SUGGESTIONS),

    /* For pushing to the api*/
    ME_TRACK_REPOST("me/reposts/tracks/#", TempEndpoints.e1.MY_TRACK_REPOST, 120, Track.class, -1, null),
    ME_TRACK_LIKE("me/likes/tracks/#", TempEndpoints.e1.MY_TRACK_LIKE, 121, Track.class, -1, null),
    ME_PLAYLIST_REPOST("me/reposts/playlists/#", TempEndpoints.e1.MY_PLAYLIST_REPOST, 122, Playlist.class, -1, null),
    ME_PLAYLIST_LIKE("me/likes/playlists/#", TempEndpoints.e1.MY_PLAYLIST_LIKE, 123, Playlist.class, -1, null),

    ME_CONNECTION("me/connections/#",Endpoints.MY_CONNECTIONS, 130, Connection.class, -1, Table.CONNECTIONS),
    ME_CONNECTIONS("me/connections",Endpoints.MY_CONNECTIONS, 131, Connection.class, -1, Table.CONNECTIONS),
    ME_SOUNDS("me/sounds", TempEndpoints.e1.MY_SOUNDS_MINI, 132, SoundAssociation.class, -1, Table.COLLECTION_ITEMS),

    // the ids of the following entries should not be changed, they are referenced in th db
    ME_SOUND_STREAM("me/stream", TempEndpoints.e1.MY_STREAM, 140, Activity.class, -1, Table.ACTIVITIES),
    ME_ACTIVITIES("me/activities/all/own", TempEndpoints.e1.MY_ACTIVITIES, 142, Activity.class, -1, Table.ACTIVITIES),
    ME_ALL_ACTIVITIES("me/activities", null, 150, Activity.class, -1, Table.ACTIVITIES),

    ME_FRIENDS("me/connections/friends", Endpoints.MY_FRIENDS, 160, Friend.class, FRIEND, Table.COLLECTION_ITEMS),

    SUGGESTED_USERS("users/suggested", Endpoints.SUGGESTED_USERS, 190, User.class, SUGGESTED_USER, null),


    SOUNDS("sounds", null, 200, Playable.class, -1, Table.SOUNDS),

    TRACKS("tracks", Endpoints.TRACKS, 201, Track.class, ScContentProvider.CollectionItemTypes.TRACK, Table.SOUNDS),
    TRACK("tracks/#", Endpoints.TRACK_DETAILS, 202, Track.class, -1, Table.SOUNDS),
    TRACK_ARTWORK("tracks/#/artwork", null, 203, null, -1, Table.SOUNDS),
    TRACK_COMMENTS("tracks/#/comments", Endpoints.TRACK_COMMENTS, 204, Comment.class, -1, Table.COMMENTS),
    TRACK_PERMISSIONS("tracks/#/permissions", null, 205, null, -1, null),
    TRACK_SECRET_TOKEN("tracks/#/secret-token", null, 206, null, -1, null),
    TRACK_LIKERS("tracks/#/favoriters", Endpoints.TRACK_FAVORITERS, 207, User.class, -1, Table.USERS),
    TRACK_REPOSTERS("tracks/#/reposters", TempEndpoints.e1.TRACK_REPOSTERS, 208, User.class, -1, Table.USERS),
    TRACK_LOOKUP("tracks/*", Endpoints.TRACKS, 2250, Track.class, -1, Table.SOUNDS),

    USERS("users", Endpoints.USERS, 301, User.class, -1, Table.USERS),
    USER("users/#", Endpoints.USER_DETAILS, 302, User.class, -1, Table.USERS),
    @Deprecated USER_TRACKS("users/#/tracks", Endpoints.USER_TRACKS, 303, Track.class, ScContentProvider.CollectionItemTypes.TRACK, Table.SOUNDS),
    USER_SOUNDS("users/#/sounds", TempEndpoints.e1.USER_SOUNDS, 311, SoundAssociation.class, -1, Table.COLLECTION_ITEMS),
    USER_LIKES("users/#/likes", TempEndpoints.e1.USER_LIKES, 304, Track.class, LIKE, null),
    USER_FOLLOWERS("users/#/followers", Endpoints.USER_FOLLOWERS, 305, User.class, FOLLOWER, null),
    USER_FOLLOWINGS("users/#/followings", Endpoints.USER_FOLLOWINGS, 306, User.class, FOLLOWING, null),
    USER_COMMENTS("users/#/comments", null, 307, Comment.class, -1, null),
    USER_GROUPS("users/#/groups", null, 308, null, -1, null),
    USER_PLAYLISTS("users/#/playlists", TempEndpoints.USER_PLAYLISTS, 309, null, -1, null),
    USER_REPOSTS("users/#/reposts", TempEndpoints.e1.USER_REPOSTS, 310, Playable.class, REPOST, null),
    USER_LOOKUP("users/*", Endpoints.USERS, 350, User.class, -1, Table.USERS),

    COMMENTS("comments", null, 400, Comment.class, -1, Table.COMMENTS),
    COMMENT("comments/#", null, 401, Comment.class, -1, Table.COMMENTS),

    /* Use string wildcards here since we use negative numbers for local playlists, which breaks with number wildcards */
    PLAYLISTS("playlists", TempEndpoints.PLAYLISTS, 501, Playlist.class, ScContentProvider.CollectionItemTypes.PLAYLIST, Table.SOUNDS),
    PLAYLIST("playlists/*", TempEndpoints.PLAYLIST_DETAILS, 502, Playlist.class, ScContentProvider.CollectionItemTypes.PLAYLIST, Table.SOUNDS),
    PLAYLIST_TRACKS("playlists/*/tracks", TempEndpoints.PLAYLIST_TRACKS, 532, Track.class, -1, Table.PLAYLIST_TRACKS),
    PLAYLIST_LIKERS("playlists/*/likers", TempEndpoints.e1.PLAYLIST_LIKERS, 533, User.class, -1, Table.USERS),
    PLAYLIST_REPOSTERS("playlists/*/reposters", TempEndpoints.e1.PLAYLIST_REPOSTERS, 534, User.class, -1, Table.USERS),
    PLAYLIST_ALL_TRACKS("playlists/tracks", null, 535, Track.class, -1, Table.PLAYLIST_TRACKS), // used for sync service

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
    SEARCHES_ITEM("searches/#", null, 1401, null, -1, Table.SEARCHES),
    SEARCHES_TRACKS("searches/tracks", null, 1402, Track.class, -1, null),
    SEARCHES_USERS("searches/users", null, 1403, User.class, -1, null),
    SEARCHES_TRACK("searches/tracks/*", null, 1404, Track.class, ScContentProvider.CollectionItemTypes.SEARCH, null),
    SEARCHES_USER("searches/users/*", null, 1405, User.class, ScContentProvider.CollectionItemTypes.SEARCH, null),

    SEARCH("search", null, 1500, ScResource.class, -1, null),
    SEARCH_ITEM("search/*", null, 1501, ScResource.class, -1, null),

    PLAY_QUEUE("play_queue", null, 2000, null, -1, Table.PLAY_QUEUE),
    PLAY_QUEUE_ITEM("play_queue/#", null, 2001, null, -1, Table.PLAY_QUEUE),

    SOUND_STREAM_CLEANUP("cleanup/soundstream", null, 9996, null, -1, null),
    ACTIVITIES_CLEANUP("cleanup/activities", null, 9997, null, -1, null),
    TRACK_CLEANUP("cleanup/tracks", null, 9998, null, -1, null),
    USERS_CLEANUP("cleanup/users", null, 9999, null, -1, null),

    //Android global search
    ANDROID_SEARCH_SUGGEST(SearchManager.SUGGEST_URI_PATH_QUERY, null, 10000, null, -1, null),
    ANDROID_SEARCH_SUGGEST_PATH(SearchManager.SUGGEST_URI_PATH_QUERY + "/*", null, 10001, null, -1, null),
    ANDROID_SEARCH_REFRESH(SearchManager.SUGGEST_URI_PATH_SHORTCUT, null, 10002, null, -1, null),
    ANDROID_SEARCH_REFRESH_PATH(SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", null, 10003, null, -1, null),

    UNKNOWN(null, null, -1, null, -1, null);


    Content(String uri, String remoteUri, int id, Class<? extends ScModel> modelType,
            int collectionType,
            Table table) {
        this.uriPath = uri;
        this.uri = Uri.parse("content://" + ScContentProvider.AUTHORITY + "/" + uriPath);
        this.id = id;
        this.modelType = modelType;
        this.collectionType = collectionType;
        this.remoteUri = remoteUri;
        this.table = table;
    }

    public final int collectionType;
    public final int id;
    public final
    @Nullable
    Class<? extends ScModel> modelType;
    public final Uri uri;
    public final String uriPath;
    public final String remoteUri;
    public final Table table;

    static final private UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static final private SparseArray<Content> sMap = new SparseArray<Content>();
    static final private Map<Uri, Content> sUris = new HashMap<Uri, Content>();

    public static final int SYNCABLE_CEILING = 190;
    public static final int MINE_CEILING = 200;

    public static final EnumSet<Content> ACTIVITIES = EnumSet.of(
            Content.ME_ACTIVITIES,
            Content.ME_SOUND_STREAM
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

    public boolean isActivitiesItem() {
        return table == Table.ACTIVITIES || table == Table.ACTIVITY_VIEW;
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

    public Uri forQuery(String query) {
        if (uri.toString().contains("*")) {
            return Uri.parse(uri.toString().replace("*", Uri.encode(String.valueOf(query))));
        } else {
            return buildUpon().appendEncodedPath(String.valueOf(query)).build();
        }
    }

    public Request request() {
        return request(null);
    }

    public Request request(Uri contentUri) {
        if (remoteUri != null) {
            String query = null;
            if (contentUri != null){
                query = contentUri.getQuery();
            }

            final String resource = remoteUri + (query != null ? "?" + query : "");
            if (remoteUri.contains("%d")) {
                int substitute = 0;
                if (contentUri != null) {
                    for (String segment : contentUri.getPathSegments()) {
                        try {
                            substitute = Integer.parseInt(segment);
                            break;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                return Request.to(resource, substitute);
            } else {
                return Request.to(resource);
            }
        } else {
            throw new IllegalArgumentException("no remote uri defined for content" + this);
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

    public static Content match(String path) {
        return match(Uri.parse("content://" + ScContentProvider.AUTHORITY + "/" + path));
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

    public boolean isStale(long lastSync) {
        // do not auto refresh users when the list opens, because users are always changing
        if (modelType == User.class) return lastSync <= 0;
        final long staleTime = (modelType == Track.class) ? SyncConfig.TRACK_STALE_TIME :
                (modelType == Activity.class) ? SyncConfig.ACTIVITY_STALE_TIME :
                        SyncConfig.DEFAULT_STALE_TIME;

        return System.currentTimeMillis() - lastSync > staleTime;
    }
}
