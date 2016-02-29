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
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.storage.Table;
import org.jetbrains.annotations.Nullable;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.SparseArray;

public enum Content {
    // these are still used but should be ported off of Content enum
    ME_LIKES("me/likes", null, 107, null, LIKE, Table.Likes),
    ME_SOUNDS("me/sounds", null, 132, null, -1, Table.Posts),
    ME_SOUND_STREAM("me/stream", null, 140, null, -1, null),
    ME_ACTIVITIES("me/activities/all/own", null, 142, null, -1, null),

    // legacy stuff
    ME("me", Endpoints.MY_DETAILS, 100, PublicApiUser.class, -1, Table.Users),
    ME_COMMENTS("me/comments", null, 102, PublicApiComment.class, -1, Table.Comments),
    ME_FOLLOWINGS("me/followings", Endpoints.MY_FOLLOWINGS, 103, UserAssociation.class, FOLLOWING, Table.UserAssociations),
    ME_FOLLOWING("me/followings/#", null, 104, UserAssociation.class, -1, null),
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

    SUGGESTED_USERS("users/suggested", Endpoints.SUGGESTED_USERS, 190, PublicApiUser.class, -1, null),

    SOUNDS("sounds", null, 200, Playable.class, -1, Table.Sounds),

    TRACKS("tracks", Endpoints.TRACKS, 201, PublicApiTrack.class,  ScContentProvider.CollectionItemTypes.TRACK, Table.Sounds),
    TRACK("tracks/#", Endpoints.TRACK_DETAILS, 202, PublicApiTrack.class, -1, Table.Sounds),
    TRACK_ARTWORK("tracks/#/artwork", null, 203, null, -1, Table.Sounds),
    TRACK_PERMISSIONS("tracks/#/permissions", null, 205, null, -1, null),
    TRACK_SECRET_TOKEN("tracks/#/secret-token", null, 206, null, -1, null),

    USERS("users", Endpoints.USERS, 301, PublicApiUser.class, -1, Table.Users),
    USER("users/#", Endpoints.USER_DETAILS, 302, PublicApiUser.class, -1, Table.Users),
    USER_FOLLOWERS("users/#/followers", Endpoints.USER_FOLLOWERS, 305, PublicApiUser.class, FOLLOWER, null),
    USER_FOLLOWINGS("users/#/followings", Endpoints.USER_FOLLOWINGS, 306, PublicApiUser.class, FOLLOWING, null),
    USER_COMMENTS("users/#/comments", null, 307, PublicApiComment.class, -1, null),
    USER_GROUPS("users/#/groups", null, 308, null, -1, null),
    USER_PLAYLISTS("users/#/playlists", TempEndpoints.USER_PLAYLISTS, 309, null, -1, null),

    COMMENTS("comments", null, 400, PublicApiComment.class, -1, Table.Comments),
    COMMENT("comments/#", null, 401, PublicApiComment.class, -1, Table.Comments),

    /* Use string wildcards here since we use negative numbers for local playlists, which breaks with number wildcards */
    PLAYLISTS("playlists", TempEndpoints.PLAYLISTS, 501, PublicApiPlaylist.class,  ScContentProvider.CollectionItemTypes.PLAYLIST, Table.Sounds),
    PLAYLIST_ALL_TRACKS("playlists/tracks", null, 502, PublicApiTrack.class, -1, Table.PlaylistTracks), // used for sync service
    PLAYLIST("playlists/*", TempEndpoints.PLAYLIST_DETAILS, 503, PublicApiPlaylist.class,  ScContentProvider.CollectionItemTypes.PLAYLIST, Table.Sounds),
    PLAYLIST_TRACKS("playlists/*/tracks", TempEndpoints.PLAYLIST_TRACKS, 532, PublicApiTrack.class, -1, Table.PlaylistTracks),

    // LOCAL URIS
    COLLECTIONS("collections", null, 1000, null, -1, Table.Collections),
    COLLECTION("collections/#", null, 1001, null, -1, Table.Collections),

    USER_ASSOCIATIONS("user_associations", null, 1010, null, -1, Table.UserAssociations),

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

    public static final int SYNCABLE_CEILING = 190;

    static {
        for (Content c : Content.values()) {
            if (c.id >= 0 && c.uri != null) {
                sMatcher.addURI(ScContentProvider.AUTHORITY, c.uriPath, c.id);
                sMap.put(c.id, c);
            }
        }
    }

    public boolean isSyncable() {
        return id < SYNCABLE_CEILING && id > 0;
    }

    public Uri.Builder buildUpon() {
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

    public boolean isUserBased() {
        return PublicApiUser.class.equals(modelType) || UserAssociation.class.equals(modelType);
    }
}
