package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.cache.ModelCache;
import com.soundcloud.android.dao.ResolverHelper;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ScModelManager {
    public  static final int RESOLVER_BATCH_SIZE = 100;
    private static final int DEFAULT_CACHE_CAPACITY = 100;

    private ContentResolver mResolver;

    private ModelCache<Track> mTrackCache = new ModelCache<Track>(DEFAULT_CACHE_CAPACITY * 4);
    private ModelCache<User> mUserCache = new ModelCache<User>(DEFAULT_CACHE_CAPACITY * 2);
    private ModelCache<Playlist> mPlaylistCache = new ModelCache<Playlist>(DEFAULT_CACHE_CAPACITY);


    public ScModelManager(Context c) {
        mResolver = c.getContentResolver();
    }

    public Track getCachedTrackFromCursor(Cursor cursor){
        return getCachedTrackFromCursor(cursor, DBHelper.Sounds._ID);
    }


    public Track getCachedTrackFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Track track = mTrackCache.get(id);

        // assumes track cache has always
        if (track == null) {
            track = new Track(cursor);
            mTrackCache.put(track);
        }
        return track;
    }

    public Playlist getCachedPlaylistFromCursor(Cursor cursor) {
        return getCachedPlaylistFromCursor(cursor, DBHelper.Sounds._ID);
    }

    public Playlist getCachedPlaylistFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Playlist playlist = mPlaylistCache.get(id);

        // assumes track cache has always
        if (playlist == null) {
            playlist = new Playlist(cursor);
            mPlaylistCache.put(playlist);
        }
        return playlist;
    }

    public User getCachedUserFromCursor(Cursor cursor) {
        return getCachedUserFromCursor(cursor,DBHelper.SoundView.USER_ID);
    }

    public User getCachedUserFromCursor(Cursor cursor, String col) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(col));
        User user = mUserCache.get(user_id);

        if (user == null) {
            user = User.fromTrackView(cursor);
            mUserCache.put(user);
        }
        return user;
    }

    public <T extends ScModel> List<T> loadLocalContent(ContentResolver resolver, Class<T> resourceType, Uri localUri) {
        Cursor itemsCursor = resolver.query(localUri, null, null, null, null);
        List<ScModel> items = new ArrayList<ScModel>();
        if (itemsCursor != null) {
            while (itemsCursor.moveToNext())
                if (Track.class.equals(resourceType)) {
                    items.add(getCachedTrackFromCursor(itemsCursor));
                } else if (User.class.equals(resourceType)) {
                    items.add(getUserFromCursor(itemsCursor));
                } else if (Friend.class.equals(resourceType)) {
                    items.add(new Friend(getUserFromCursor(itemsCursor)));
                } else if (SoundAssociation.class.equals(resourceType)) {
                    items.add(new SoundAssociation(itemsCursor));
                } else if (Playlist.class.equals(resourceType)) {
                    items.add(getCachedPlaylistFromCursor(itemsCursor));
                } else {
                    throw new IllegalArgumentException("NOT HANDLED YET " + resourceType);
                }
        }
        if (itemsCursor != null) itemsCursor.close();

        return  (List<T>) items;
    }

    private User getUserFromCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.Users._ID));
        User user = mUserCache.get(id);
        if (user == null) {
            user = new User(itemsCursor);
            mUserCache.put(user);
        }
        return user;
    }


    public @Nullable Playlist getPlaylist(Uri uri) {
        return (Playlist) getModel(uri);
    }

    public @Nullable void removeFromCache(Uri uri) {
        final ModelCache cacheFromUri = getCacheFromUri(uri);
        if (cacheFromUri != null){
            cacheFromUri.remove(UriUtils.getLastSegmentAsLong(uri));
        }
    }

    public @Nullable
    ModelCache getCacheFromUri(Uri uri) {
        switch (Content.match(uri)){
            case TRACK:     return mTrackCache;
            case USER:      return mUserCache;
            case PLAYLIST:  return mPlaylistCache;
        }
        return null;
    }

    public @Nullable ScModel getModel(Uri uri) {
        return getModel(uri, getCacheFromUri(uri));
    }

    /**
     * Gets a resource from local storage, optionally from a cache if one is provided
     * @param uri resource lookup uri {@link Content}
     * @param cache optional cache to lookup object in and cache to
     * @return the resource found, or null if no resource found
     */
    @Nullable ScModel getModel(Uri uri, @Nullable ModelCache cache) {
        ScModel resource = null;

        if (cache != null) resource = cache.get(UriUtils.getLastSegmentAsLong(uri));

        Content c = Content.match(uri);
        if (resource == null) {
            Cursor cursor = mResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    try {
                        resource = c.modelType.getConstructor(Cursor.class).newInstance(cursor);
                    } catch (Exception e) {
                        throw new AssertionError("Could not find constructor for resource. Uri: " + uri);
                    }
                }
                cursor.close();
            }
            if (cache != null && resource != null) {
                cache.put(resource);
            }
        }
        return resource;
    }

    public @Nullable Track getTrack(long id) {
        if (id < 0) return null;

        Track t = mTrackCache.get(id);
        if (t == null) {
            t = (Track) getModel(Content.TRACK.forId(id), null);
            if (t != null) mTrackCache.put(t);
        }
        return t;
    }

    public @Nullable User getUser(long id) {
            if (id < 0) return null;

            User u = mUserCache.get(id);
            if (u == null) {
                u = (User) getModel(Content.USER.forId(id));
                if (u != null) mUserCache.put(u);
            }
            return u;
        }


        public @Nullable Playlist getPlaylist(long id) {
            if (id < 0) return null;

            Playlist p = mPlaylistCache.get(id);
            if (p == null) {
                p = (Playlist) getModel(Content.PLAYLIST.forId(id));
                if (p != null) mPlaylistCache.put(p);
            }
            return p;
        }


    public List<Track> loadPlaylistTracks(ContentResolver resolver, long playlistId){
        return loadLocalContent(resolver,Track.class,Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlistId)));
    }

    public Track getCachedTrack(long id) {
        return mTrackCache.get(id);
    }

    public User getCachedUser(long id) {
        return mUserCache.get(id);
    }

    public ScResource cache(@Nullable ScResource resource) {
        return cache(resource, ScResource.CacheUpdateMode.NONE);
    }

    public ScResource cache(@Nullable ScResource resource, ScResource.CacheUpdateMode updateMode) {
        if (resource instanceof Track) {
            return cache((Track) resource, updateMode);
        } else if (resource instanceof Playlist) {
            return cache((Playlist) resource, updateMode);
        } else if (resource instanceof User) {
            return cache((User) resource, updateMode);
        } else {
            return resource;
        }
    }

    public Track cache(@Nullable Track track) {
        return cache(track, ScResource.CacheUpdateMode.NONE);
    }

    public Playlist cache(@Nullable Playlist playlist) {
        return cache(playlist, ScResource.CacheUpdateMode.NONE);
    }

    public Track cache(@Nullable Track track, ScResource.CacheUpdateMode updateMode) {
        if (track == null) return null;

        if (track.user != null) {
            track.user = cache(track.user, updateMode);
        }

        if (mTrackCache.containsKey(track.id)) {
            if (updateMode.shouldUpdate()) {
                return mTrackCache.get(track.id).updateFrom(track, updateMode);
            } else {
                return mTrackCache.get(track.id);
            }

        } else {
            mTrackCache.put(track);
            return track;
        }
    }

    public Playlist cache(@Nullable Playlist playlist, ScResource.CacheUpdateMode updateMode){
        if (playlist == null) return null;

        if (playlist.user != null){
            playlist.user = cache(playlist.user, updateMode);
        }

        if (playlist.tracks != null){
            for (int i = 0; i < playlist.tracks.size(); i++){
                playlist.tracks.set(i, cache(playlist.tracks.get(i),updateMode));
            }
        }

        if (mPlaylistCache.containsKey(playlist.id)){
            if (updateMode.shouldUpdate()){
                return mPlaylistCache.get(playlist.id).updateFrom(playlist, updateMode);
            } else {
                return mPlaylistCache.get(playlist.id);
            }
        } else {
            mPlaylistCache.put(playlist);
            return playlist;
        }
    }

    public ScResource cache(@Nullable User user) {
        return cache(user, ScResource.CacheUpdateMode.NONE);
    }

    public User cache(@Nullable User user, ScResource.CacheUpdateMode updateMode) {
        if (user == null) return null;

        if (mUserCache.containsKey(user.id)) {
            if (updateMode.shouldUpdate()) {
                return mUserCache.get(user.id).updateFrom(user, updateMode);
            } else {
                return mUserCache.get(user.id);
            }
        } else {
            mUserCache.put(user);
            return user;
        }
    }


    public ScResource cacheAndWrite(ScResource resource, ScResource.CacheUpdateMode mode) {
        if (resource instanceof Track) {
            return cacheAndWrite(((Track) resource), mode);
        } else if (resource instanceof User) {
            return cacheAndWrite(((User) resource), mode);
        } else if (resource instanceof Playlist) {
            return cacheAndWrite(((Playlist) resource), mode);
        }
        return resource;
    }

    public Track cacheAndWrite(Track track, ScResource.CacheUpdateMode mode) {
        if (track != null) {
            if (mode == ScResource.CacheUpdateMode.FULL) track.setUpdated();
            track = cache(track, mode);
            track.insert(mResolver);
        }
        return track;
    }

    public User cacheAndWrite(User user, ScResource.CacheUpdateMode mode) {
        if (user != null) {
            user = cache(user, mode);
            user.insert(mResolver);
        }
        return user;
    }

    public Playlist cacheAndWrite(Playlist playlist, ScResource.CacheUpdateMode mode) {
        if (playlist != null) {
            playlist = cache(playlist, mode);
            playlist.insert(mResolver);
        }
        return playlist;
    }



    /**
     * @param modelIds     a list of model ids
     * @param ignoreStored if it should ignore stored ids
     * @return a list of models which are not stored in the database
     * @throws java.io.IOException
     */
    public int fetchMissingCollectionItems(AndroidCloudAPI api,
                                           List<Long> modelIds,
                                           Content content,
                                           boolean ignoreStored, int maxToFetch) throws IOException {
        if (modelIds == null || modelIds.isEmpty()) {
            return 0;
        }
        // copy so we don't modify the original
        List<Long> ids = new ArrayList<Long>(modelIds);

        if (!ignoreStored) {
            ids.removeAll(getStoredIdsBatched(mResolver, modelIds, content));
        }

        List<Long> fetchIds = (maxToFetch > -1) ? new ArrayList<Long>(ids.subList(0, Math.min(ids.size(), maxToFetch)))
                    : ids;

        // XXX this has to be abstracted more. Hesitant to do so until the api is more final
        Request request = Track.class.equals(content.modelType) ||
               SoundAssociation.class.equals(content.modelType) ? Content.TRACKS.request() : Content.USERS.request();

        return SoundCloudDB.bulkInsertResources(mResolver, api.readListFromIds(request, fetchIds));
    }


    /**
     * @return a list of all ids for which objects are store in the database
     */
    private static List<Long> getStoredIdsBatched(ContentResolver resolver, List<Long> ids, Content content) {
        int i = 0;
        List<Long> storedIds = new ArrayList<Long>();
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, ids.size()));
            storedIds.addAll(ResolverHelper.idCursorToList(
                    resolver.query(content.uri, new String[]{BaseColumns._ID},
                            ResolverHelper.getWhereInClause(BaseColumns._ID, batch.size()) + " AND " + DBHelper.ResourceTable.LAST_UPDATED + " > 0"
                            , ResolverHelper.longListToStringArr(batch), null)
            ));
            i += RESOLVER_BATCH_SIZE;
        }
        return storedIds;
    }


    <T extends ScResource> int writeCollection(List<T> items, ScResource.CacheUpdateMode updateMode) {
        for (T item : items) {
            cache(item, updateMode);
        }
        return SoundCloudDB.bulkInsertResources(mResolver, items);
    }


    <T extends ScResource> int writeCollection(List<T> items, Uri localUri, long userId, ScResource.CacheUpdateMode updateMode) {
        if (items.isEmpty()) return 0;

        for (T item : items) {
            cache(item, updateMode);
        }

        return SoundCloudDB.insertCollection(mResolver, items, localUri, userId);
    }


    public List<Long> getLocalIds(Content content, long userId, int startIndex, int limit) {
        return ResolverHelper.idCursorToList(mResolver.query(
                ResolverHelper.addPagingParams(Content.COLLECTION_ITEMS.uri, startIndex, limit).build(),
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[]{String.valueOf(content.collectionType), String.valueOf(userId)},
                DBHelper.CollectionItems.SORT_ORDER));
    }

    public void clear() {
        mTrackCache.clear();
        mUserCache.clear();
    }
}
