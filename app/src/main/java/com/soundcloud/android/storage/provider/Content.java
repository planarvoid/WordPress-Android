package com.soundcloud.android.storage.provider;


import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.FOLLOWER;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.FOLLOWING;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.REPOST;

import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.TempEndpoints;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.search.suggestions.Shortcut;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.sync.SyncConfig;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Content {
    ME("me", Endpoints.MY_DETAILS, 100, PublicApiUser.class, -1, Table.Users),
    ME_COMMENTS("me/comments", null, 102, PublicApiComment.class, -1, Table.Comments),
    ME_FOLLOWINGS("me/followings", Endpoints.MY_FOLLOWINGS, 103, UserAssociation.class, FOLLOWING, Table.UserAssociations),
    ME_FOLLOWING("me/followings/#", null, 104, UserAssociation.class, -1, null),
    ME_FOLLOWERS("me/followers", Endpoints.MY_FOLLOWERS, 105, UserAssociation.class, FOLLOWER, Table.UserAssociations),
    ME_FOLLOWER("me/followers/#", null, 106, PublicApiUser.class, -1, null),
    ME_LIKES("me/likes", TempEndpoints.e1.USER_LIKES, 107, SoundAssociation.class, LIKE, Table.Likes),
    ME_LIKE("me/likes/#", null, 108, PublicApiTrack.class, LIKE, null),
    ME_REPOSTS("me/reposts", null, 109, null, REPOST, Table.CollectionItems),
    ME_PLAYLISTS("me/playlists", TempEndpoints.MY_PLAYLISTS, 110, PublicApiPlaylist.class, ScContentProvider.CollectionItemTypes.PLAYLIST, Table.Posts),
    ME_USERID("me/userid", null, 111, null, -1, null),

    ME_PLAYLIST("me/playlists/*", null, 112, PublicApiPlaylist.class,  ScContentProvider.CollectionItemTypes.PLAYLIST, Table.Posts),

    /* For pushing to the api*/
    ME_TRACK_REPOST("me/reposts/tracks/#", TempEndpoints.e1.MY_TRACK_REPOST, 120, PublicApiTrack.class, -1, null),
    ME_TRACK_LIKE("me/likes/tracks/#", TempEndpoints.e1.MY_TRACK_LIKE, 121, PublicApiTrack.class, -1, null),
    ME_PLAYLIST_REPOST("me/reposts/playlists/#", TempEndpoints.e1.MY_PLAYLIST_REPOST, 122, PublicApiPlaylist.class, -1, null),
    ME_PLAYLIST_LIKE("me/likes/playlists/#", TempEndpoints.e1.MY_PLAYLIST_LIKE, 123, PublicApiPlaylist.class, -1, null),

    ME_SOUNDS("me/sounds", TempEndpoints.e1.MY_SOUNDS_MINI, 132, SoundAssociation.class, -1, Table.Posts),

    // the ids of the following entries should not be changed, they are referenced in th db
    ME_SOUND_STREAM("me/stream", TempEndpoints.e1.MY_STREAM, 140, Activity.class, -1, Table.Activities),
    ME_ACTIVITIES("me/activities/all/own", TempEndpoints.e1.MY_ACTIVITIES, 142, Activity.class, -1, Table.Activities),
    ME_ALL_ACTIVITIES("me/activities", null, 150, Activity.class, -1, Table.Activities),

    SUGGESTED_USERS("users/suggested", Endpoints.SUGGESTED_USERS, 190, PublicApiUser.class, -1, null),

    SOUNDS("sounds", null, 200, Playable.class, -1, Table.Sounds),

    TRACKS("tracks", Endpoints.TRACKS, 201, PublicApiTrack.class,  ScContentProvider.CollectionItemTypes.TRACK, Table.Sounds),
    TRACK("tracks/#", Endpoints.TRACK_DETAILS, 202, PublicApiTrack.class, -1, Table.Sounds),
    TRACK_ARTWORK("tracks/#/artwork", null, 203, null, -1, Table.Sounds),
    TRACK_PERMISSIONS("tracks/#/permissions", null, 205, null, -1, null),
    TRACK_SECRET_TOKEN("tracks/#/secret-token", null, 206, null, -1, null),
    TRACK_LIKERS("tracks/#/favoriters", Endpoints.TRACK_FAVORITERS, 207, PublicApiUser.class, -1, Table.Users),
    TRACK_REPOSTERS("tracks/#/reposters", TempEndpoints.e1.TRACK_REPOSTERS, 208, PublicApiUser.class, -1, Table.Users),
    TRACK_LOOKUP("tracks/q/*", Endpoints.TRACKS, 250, PublicApiTrack.class, -1, Table.Sounds),

    USERS("users", Endpoints.USERS, 301, PublicApiUser.class, -1, Table.Users),
    USER("users/#", Endpoints.USER_DETAILS, 302, PublicApiUser.class, -1, Table.Users),
    USER_SOUNDS("users/#/sounds", TempEndpoints.e1.USER_SOUNDS, 311, SoundAssociation.class, -1, Table.Likes),
    USER_LIKES("users/#/likes", TempEndpoints.e1.USER_LIKES, 304, PublicApiTrack.class, LIKE, null),
    USER_FOLLOWERS("users/#/followers", Endpoints.USER_FOLLOWERS, 305, PublicApiUser.class, FOLLOWER, null),
    USER_FOLLOWINGS("users/#/followings", Endpoints.USER_FOLLOWINGS, 306, PublicApiUser.class, FOLLOWING, null),
    USER_COMMENTS("users/#/comments", null, 307, PublicApiComment.class, -1, null),
    USER_GROUPS("users/#/groups", null, 308, null, -1, null),
    USER_PLAYLISTS("users/#/playlists", TempEndpoints.USER_PLAYLISTS, 309, null, -1, null),
    USER_REPOSTS("users/#/reposts", TempEndpoints.e1.USER_REPOSTS, 310, Playable.class, REPOST, null),
    USER_LOOKUP("users/q/*", Endpoints.USERS, 350, PublicApiUser.class, -1, Table.Users),

    COMMENTS("comments", null, 400, PublicApiComment.class, -1, Table.Comments),
    COMMENT("comments/#", null, 401, PublicApiComment.class, -1, Table.Comments),

    /* Use string wildcards here since we use negative numbers for local playlists, which breaks with number wildcards */
    PLAYLISTS("playlists", TempEndpoints.PLAYLISTS, 501, PublicApiPlaylist.class,  ScContentProvider.CollectionItemTypes.PLAYLIST, Table.Sounds),
    PLAYLIST_ALL_TRACKS("playlists/tracks", null, 502, PublicApiTrack.class, -1, Table.PlaylistTracks), // used for sync service
    PLAYLIST("playlists/*", TempEndpoints.PLAYLIST_DETAILS, 503, PublicApiPlaylist.class,  ScContentProvider.CollectionItemTypes.PLAYLIST, Table.Sounds),
    PLAYLIST_TRACKS("playlists/*/tracks", TempEndpoints.PLAYLIST_TRACKS, 532, PublicApiTrack.class, -1, Table.PlaylistTracks),
    PLAYLIST_LIKERS("playlists/*/likers", TempEndpoints.e1.PLAYLIST_LIKERS, 533, PublicApiUser.class, -1, Table.Users),
    PLAYLIST_REPOSTERS("playlists/*/reposters", TempEndpoints.e1.PLAYLIST_REPOSTERS, 534, PublicApiUser.class, -1, Table.Users),
    PLAYLIST_LOOKUP("playlists/q/*", Endpoints.PLAYLISTS, 535, PublicApiPlaylist.class, -1, Table.Sounds),

    // LOCAL URIS
    COLLECTIONS("collections", null, 1000, null, -1, Table.Collections),
    COLLECTION("collections/#", null, 1001, null, -1, Table.Collections),

    USER_ASSOCIATIONS("user_associations", null, 1010, null, -1, Table.UserAssociations),

    TRACK_METADATA("track_metadata", null, 1302, null, -1, Table.TrackMetadata),

    SEARCH("search", null, 1500, PublicApiResource.class, -1, null),
    SEARCH_ITEM("search/*", null, 1501, PublicApiResource.class, -1, null),

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

    /**
     * one of {@link com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes}
     */
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
    static final private SparseArray<Content> sMap = new SparseArray<>();
    static final private Map<Uri, Content> sUris = new HashMap<>();

    public static final int SYNCABLE_CEILING = 190;
    public static final int MINE_CEILING = 200;

    public static final EnumSet<Content> ACTIVITIES = EnumSet.of(
            Content.ME_ACTIVITIES,
            Content.ME_SOUND_STREAM
    );


    public static final EnumSet<Content> ID_BASED = EnumSet.of(
            Content.ME_FOLLOWINGS,
            Content.ME_FOLLOWERS
    );

    public static final EnumSet<Content> LISTEN_FOR_PLAYLIST_CHANGES = EnumSet.of(
            Content.ME_SOUND_STREAM,
            Content.ME_ACTIVITIES,
            Content.ME_SOUNDS,
            Content.USER_SOUNDS,
            Content.ME_LIKES,
            Content.USER_LIKES,
            Content.ME_PLAYLISTS,
            Content.USER_PLAYLISTS
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
        return id < SYNCABLE_CEILING && id > 0;
    }

    public boolean isActivitiesItem() {
        return table == Table.Activities || table == Table.ActivityView;
    }

    public boolean isMine() {
        return id < MINE_CEILING && id > 0;
    }

    public Uri.Builder buildUpon() {
        return uri.buildUpon();
    }

    public Uri withQuery(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("need even params");
        }

        Uri.Builder builder = buildUpon();
        for (int i = 0; i < args.length; i += 2) {
            builder.appendQueryParameter(args[i], args[i + 1]);
        }
        return builder.build();
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
            if (contentUri != null) {
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

    public boolean shouldListenForPlaylistChanges() {
        return LISTEN_FOR_PLAYLIST_CHANGES.contains(this);
    }

    @Override
    public String toString() {
        return "Content." + name();
    }

    public static Content match(Uri uri) {
        if (uri == null) {
            return null;
        }
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

    public static
    @Nullable
    Content byUri(Uri uri) {
        return sUris.get(uri);
    }

    public boolean isUserBased() {
        return PublicApiUser.class.equals(modelType) || UserAssociation.class.equals(modelType);
    }

    public boolean isStale(long lastSync) {
        // do not auto refresh users when the list opens, because users are always changing
        if (isUserBased()) {
            return lastSync <= 0;
        }
        final long staleTime = (modelType == PublicApiTrack.class) ? SyncConfig.TRACK_STALE_TIME :
                (modelType == Activity.class) ? SyncConfig.ACTIVITY_STALE_TIME :
                        SyncConfig.DEFAULT_STALE_TIME;

        return System.currentTimeMillis() - lastSync > staleTime;
    }

    @Nullable
    public Class<? extends ScModel> getModelType() {
        return modelType;
    }
}
