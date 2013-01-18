package com.soundcloud.android.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.cache.PlaylistCache;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.cache.UserCache;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.PlaylistActivity;
import com.soundcloud.android.model.act.PlaylistRepostActivity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ScModelManager {

    private static final int API_LOOKUP_BATCH_SIZE = 200;

    private ContentResolver mResolver;
    private ObjectMapper mMapper;

    private TrackCache mTrackCache = new TrackCache();
    private UserCache mUserCache = new UserCache();
    private PlaylistCache mPlaylistCache = new PlaylistCache();

    private Context mContext;

    public ScModelManager(Context c, ObjectMapper mapper) {
        mContext = c;
        mResolver = c.getContentResolver();
        mMapper = mapper;
    }

    public Activity getActivityFromCursor(Cursor cursor) {
        Activity a = Activity.Type
                .fromString(cursor.getString(cursor.getColumnIndex(DBHelper.Activities.TYPE)))
                .fromCursor(cursor);
        if (a != null) {
            a.setUser(getUserFromActivityCursor(cursor));
            if (a.getType().isPlaylistActivity()){
                a.setPlaylist(getPlaylistFromCursor(cursor, DBHelper.ActivityView.SOUND_ID));
            } else {
                a.setTrack(getTrackFromCursor(cursor, DBHelper.ActivityView.SOUND_ID));
            }
        }
        return a;
    }

    public Activities getActivitiesFromCursor(Cursor cursor) {
        Activities activities = new Activities();
        while (cursor != null && cursor.moveToNext()) {
            final Activity activityFromCursor = getActivityFromCursor(cursor);
            if (activityFromCursor != null) activities.add(activityFromCursor);
        }
        if (cursor != null) cursor.close();
        return activities;
    }

    public @Nullable Activities getActivitiesFromJson(InputStream is) throws IOException {
        Activities activities = mMapper.readValue(is, Activities.class);
        if (activities == null) return null;

        for (Activity a : activities) {
            a.setPlaylist(cache(a.getPlaylist(),ScResource.CacheUpdateMode.MINI));
            a.setTrack(cache(a.getTrack(), ScResource.CacheUpdateMode.MINI));
            a.setUser(cache(a.getUser(), ScResource.CacheUpdateMode.MINI));
        }
        return activities;
    }

    /**
     * Turn an input stream into a collection of objects, using the cache to ensure that there is only one instance
     * of each resource object in memory
     *
     * @return the Resource Collection
     * @throws IOException
     */
    public @NotNull <T extends ScResource> CollectionHolder<T> getCollectionFromStream(InputStream is) throws IOException {

        List<ScResource> items = new ArrayList<ScResource>();
        CollectionHolder holder = mMapper.readValue(is, ScResource.ScResourceHolder.class);
        for (ScResource m : (ScResource.ScResourceHolder) holder) {
            items.add(cache(m, ScResource.CacheUpdateMode.FULL));
        }
        holder.collection = items;
        holder.resolve(mContext);
        return holder;
    }

    public
    @NotNull
    <T extends ScResource> T getModelFromStream(InputStream is, Class<T> modelClass) throws IOException {
        return mMapper.readValue(is, modelClass);
    }

    public
    @NotNull
    <T extends ScResource> T getModelFromStream(InputStream is) throws IOException {
        return (T) getModelFromStream(is, ScResource.class);
    }

    public Track getTrackFromCursor(Cursor cursor){
        return getTrackFromCursor(cursor, DBHelper.Sounds._ID);
    }


    public Track getTrackFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Track track = mTrackCache.get(id);

        // assumes track cache has always
        if (track == null) {
            track = new Track(cursor);
            mTrackCache.put(track);
        }
        track.user = getCachedUserFromCursor(cursor,DBHelper.SoundView.USER_ID);
        return track;
    }

    public Playlist getPlaylistFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Playlist playlist = mPlaylistCache.get(id);

        // assumes track cache has always
        if (playlist == null) {
            playlist = new Playlist(cursor);
            mPlaylistCache.put(playlist);
        }
        playlist.user = getCachedUserFromCursor(cursor, DBHelper.SoundView.USER_ID);
        return playlist;
    }

    private User getCachedUserFromCursor(Cursor cursor, String col) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(col));
        User user = mUserCache.get(user_id);

        if (user == null) {
            user = User.fromTrackView(cursor);
            mUserCache.put(user);
        }
        return user;
    }

    public <T extends ScModel> CollectionHolder<T> loadLocalContent(ContentResolver resolver, Class<T> resourceType, Uri localUri) {
        Cursor itemsCursor = resolver.query(localUri, null, null, null, null);
        List<ScModel> items = new ArrayList<ScModel>();
        if (itemsCursor != null) {
            while (itemsCursor.moveToNext())
                if (Track.class.equals(resourceType)) {
                    items.add(getTrackFromCursor(itemsCursor));
                } else if (User.class.equals(resourceType)) {
                    items.add(getUserFromCursor(itemsCursor));
                } else if (Friend.class.equals(resourceType)) {
                    items.add(new Friend(getUserFromCursor(itemsCursor)));
                } else if (SoundAssociation.class.equals(resourceType)) {
                    SoundAssociation soundAssociation;
                    if (Playable.isTrackCursor(itemsCursor)){
                        soundAssociation = new SoundAssociation(itemsCursor, getTrackFromCursor(itemsCursor, DBHelper.SoundAssociationView._ID));
                    } else {
                        soundAssociation = new SoundAssociation(itemsCursor, getPlaylistFromCursor(itemsCursor, DBHelper.SoundAssociationView._ID));
                    }
                    soundAssociation.user = getCachedUserFromCursor(itemsCursor, DBHelper.SoundAssociationView.SOUND_ASSOCIATION_USER_ID);
                    items.add(soundAssociation);
                } else {
                    throw new IllegalArgumentException("NOT HANDLED YET " + resourceType);
                }
        }
        if (itemsCursor != null) itemsCursor.close();

        CollectionHolder<T> holder = new CollectionHolder<T>((List<T>) items);
        holder.resolve(mContext);
        return holder;
    }

    public User getUserFromCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.Users._ID));
        User user = mUserCache.get(id);
        if (user == null) {
            user = new User(itemsCursor);
            mUserCache.put(user);
        }
        return user;
    }

    public User getUserFromActivityCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.ActivityView.USER_ID));
        User user = mUserCache.get(id);
        if (user == null) {
            user = User.fromActivityView(itemsCursor);
            mUserCache.put(user);
        }
        return user;
    }

    public @Nullable Track getTrack(long id) {
        if (id < 0) return null;

        Track t = mTrackCache.get(id);
        if (t == null) {
            t = getTrack(Content.TRACK.forId(id));
            if (t != null) mTrackCache.put(t);
        }
        return t;
    }

    public @Nullable Track getTrack(Uri uri) {
        Track t = null;
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                t = getTrackFromCursor(cursor);
            }
            cursor.close();
        }
        return t;
    }

    public @Nullable User getUser(long id) {
        if (id < 0) return null;

        User u = mUserCache.get(id);
        if (u == null) {
            u = getUser(Content.USER.forId(id));
            if (u != null) mUserCache.put(u);
        }
        return u;
    }

    public @Nullable User getUser(Uri uri) {
        User u = null;
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                u = getUserFromCursor(cursor);
            }
            cursor.close();
        }
        return u;
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

    public boolean markTrackAsPlayed(Track track) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TrackMetadata._ID, track.id);
        return mResolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    public Uri write(Track track) {
        return SoundCloudDB.upsertTrack(mResolver, track);
    }

    public ScResource cacheAndWrite(ScResource resource, ScResource.CacheUpdateMode mode) {
        if (resource instanceof Track){
            return cacheAndWrite(((Track) resource),mode);
        } else if (resource instanceof User){
            return cacheAndWrite(((User) resource),mode);
        }
        return resource;
    }

    public Track cacheAndWrite(Track track, ScResource.CacheUpdateMode mode) {
        if (track != null) {
            if (mode == ScResource.CacheUpdateMode.FULL) track.setUpdated();
            track = cache(track, mode);
            write(track);
        }
        return track;
    }

    public Uri write(User user) {
        return SoundCloudDB.upsertUser(mResolver, user);
    }

    public User cacheAndWrite(User user, ScResource.CacheUpdateMode mode) {
        if (user != null) {
            user = cache(user, mode);
            write(user);
        }
        return user;
    }

    private List<ScResource> doBatchLookup(AndroidCloudAPI api, List<Long> ids, String path) throws IOException {
        List<ScResource> resources = new ArrayList<ScResource>();
        int i = 0;
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, ids.size()));
            InputStream is = validateResponse(
                    api.get(
                            Request.to(path)
                                    .add("linked_partitioning", "1")
                                    .add("limit", API_LOOKUP_BATCH_SIZE)
                                    .add("ids", TextUtils.join(",", batch)))).getEntity().getContent();

            resources.addAll(getCollectionFromStream(is).collection);

            i += API_LOOKUP_BATCH_SIZE;
        }
        return resources;
    }

    public static HttpResponse validateResponse(HttpResponse response) throws IOException {
        final int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_UNAUTHORIZED) {
            throw new CloudAPI.InvalidTokenException(HttpStatus.SC_UNAUTHORIZED,
                    response.getStatusLine().getReasonPhrase());
        } else if (code != HttpStatus.SC_OK && code != HttpStatus.SC_NOT_MODIFIED) {
            throw new IOException("Invalid response: " + response.getStatusLine());
        }
        return response;
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
            ids.removeAll(SoundCloudDB.getStoredIdsBatched(mResolver, modelIds, content));
        }

        List<Long> fetchIds = (maxToFetch > -1) ? new ArrayList<Long>(ids.subList(0, Math.min(ids.size(), maxToFetch)))
                    : ids;

        return SoundCloudDB.bulkInsertModels(mResolver, doBatchLookup(api, fetchIds,
                // XXX this has to be abstracted more. Hesitant to do so until the api is more final
                Track.class.equals(content.modelType) || SoundAssociation.class.equals(content.modelType)
                        ? Content.TRACKS.remoteUri : Content.USERS.remoteUri));
    }

    public static String[] longListToStringArr(List<Long> deletions) {
        int i = 0;
        String[] idList = new String[deletions.size()];
        for (Long id : deletions) {
            idList[i] = String.valueOf(id);
            i++;
        }
        return idList;
    }

    public int writeCollectionFromStream(InputStream is, ScResource.CacheUpdateMode updateMode) throws IOException {
        return writeCollectionFromStream(is, null, -1, updateMode);
    }

    public int writeCollectionFromStream(InputStream is, Uri uri, long userId, ScResource.CacheUpdateMode updateMode) throws IOException {
        return writeCollection(getCollectionFromStream(is).collection, uri, userId, updateMode);
    }

    public Object writeCollection(List<ScResource> items, ScResource.CacheUpdateMode updateMode) {
        return writeCollection(items, null, -1l, updateMode);
    }


    public <T extends ScResource> int writeCollection(List<T> items, Uri localUri, long userId, ScResource.CacheUpdateMode updateMode) {
        if (items.isEmpty()) return 0;

        for (T item : items) {
            cache(item, updateMode);
        }

        return SoundCloudDB.bulkInsertModels(mResolver, items, localUri, userId);
    }

    public List<Long> getLocalIds(Content content, long userId) {
        return getLocalIds(content, userId, -1, -1);
    }


    public List<Long> getLocalIds(Content content, long userId, int startIndex, int limit) {
        return SoundCloudDB.idCursorToList(mResolver.query(
                SoundCloudDB.addPagingParams(Content.COLLECTION_ITEMS.uri, startIndex, limit).build(),
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[]{String.valueOf(content.collectionType), String.valueOf(userId)},
                DBHelper.CollectionItems.SORT_ORDER));
    }

    public void clear() {
        mTrackCache.clear();
        mUserCache.clear();
    }

    public int writeCollection(CollectionHolder<? extends ScResource> models,
                               ScResource.CacheUpdateMode mode) {
        if (models.isEmpty()) return 0;
        for (ScResource m : models) {
            cache(m, mode);
        }
        return models.insert(mResolver);
    }
}
