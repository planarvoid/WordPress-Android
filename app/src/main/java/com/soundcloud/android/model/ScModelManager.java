package com.soundcloud.android.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.cache.UserCache;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
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
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ScModelManager {

    public static final int RESOLVER_BATCH_SIZE = 100;
    private static final int API_LOOKUP_BATCH_SIZE = 200;

    private ContentResolver mResolver;
    private ObjectMapper mMapper;

    // XXX should not be exposed
    public static TrackCache TRACK_CACHE = new TrackCache();
    public static UserCache USER_CACHE = new UserCache();
    private Context mContext;

    public ScModelManager(Context c, ObjectMapper mapper) {
        mContext = c;
        mResolver = c.getContentResolver();
        mMapper = mapper;
    }

    private User getLoggedInUser() {
        User user = getUser(SoundCloudApplication.getUserId());
        if (user != null){
            return user;
        } else {
            user = new User();
            user.id = SoundCloudApplication.getUserId();
            return (User) cache(user);
        }

    }

    public Activity getActivityFromCursor(Cursor cursor){
        Activity a = Activity.Type.fromString(cursor.getString(cursor.getColumnIndex(DBHelper.Activities.TYPE))).fromCursor(cursor);
        if (a != null) {
            a.setCachedTrack(getTrackFromCursor(cursor, DBHelper.ActivityView.TRACK_ID));
            a.setCachedUser(getUserFromActivityCursor(cursor));
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

    public Activities getActivitiesFromJson(InputStream is) throws IOException {
        Activities activities = mMapper.readValue(is, Activities.class);
        for (Activity a : activities) {
            a.setCachedTrack(SoundCloudApplication.MODEL_MANAGER.cache(a.getTrack(), ScModel.CacheUpdateMode.MINI));
            a.setCachedUser(SoundCloudApplication.MODEL_MANAGER.cache(a.getUser(), ScModel.CacheUpdateMode.MINI));
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
    public
    @NotNull
    <T extends ScResource> CollectionHolder<T> getCollectionFromStream(InputStream is) throws IOException {

        List<ScResource> items = new ArrayList<ScResource>();
        CollectionHolder holder = mMapper.readValue(is, ScResource.ScResourceHolder.class);
        for (ScResource m : (ScResource.ScResourceHolder) holder) {
            items.add(cache(m, ScModel.CacheUpdateMode.FULL));
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
        return getTrackFromCursor(cursor, DBHelper.Tracks._ID);
    }


    public Track getTrackFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Track track = TRACK_CACHE.get(id);

        // assumes track cache has always
        if (track == null) {
            track = new Track(cursor);

            final long user_id = cursor.getLong(cursor.getColumnIndex(DBHelper.TrackView.USER_ID));
            User user = USER_CACHE.get(user_id);

            if (user == null) {
                user = User.fromTrackView(cursor);
                USER_CACHE.put(user);
            }
            track.user = user;
            TRACK_CACHE.put(track);
        }
        return track;
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
        User user = USER_CACHE.get(id);
        if (user == null) {
            user = new User(itemsCursor);
            USER_CACHE.put(user);
        }
        return user;
    }

    public User getUserFromActivityCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.ActivityView.USER_ID));
        User user = USER_CACHE.get(id);
        if (user == null) {
            user = User.fromActivityView(itemsCursor);
            USER_CACHE.put(user);
        }
        return user;
    }

    public @Nullable Track getTrack(long id) {
        if (id < 0) return null;

        Track t = TRACK_CACHE.get(id);
        if (t == null) {
            t = getTrack(Content.TRACK.forId(id));
            if (t != null) TRACK_CACHE.put(t);
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

        User u = USER_CACHE.get(id);
        if (u == null) {
            u = getUser(Content.USER.forId(id));
            if (u != null) USER_CACHE.put(u);
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

    public ScResource getCachedModel(Class<? extends ScResource> modelClass, long id) {
        if (Track.class.equals(modelClass)) {
            return getCachedTrack(id);
        } else if (User.class.equals(modelClass)) {
            return getCachedUser(id);
        }
        return null;
    }

    public Track getCachedTrack(long id) {
        return TRACK_CACHE.get(id);
    }

    public User getCachedUser(long id) {
        return USER_CACHE.get(id);
    }

    public ScResource cache(@Nullable ScResource resource) {
        return cache(resource, ScModel.CacheUpdateMode.NONE);
    }

    public ScResource cache(@Nullable ScResource resource, ScModel.CacheUpdateMode updateMode) {
        if (resource instanceof Track) {
            return cache((Track) resource, updateMode);
        } else if (resource instanceof User) {
            return cache((User) resource, updateMode);
        } else {
            return resource;
        }
    }

    public Track cache(@Nullable Track track) {
        return cache(track, ScModel.CacheUpdateMode.NONE);
    }

    public Track cache(@Nullable Track track, ScModel.CacheUpdateMode updateMode) {
        if (track == null) return null;

        if (track.user != null) {
            track.user = cache(track.user, updateMode);
        }

        if (TRACK_CACHE.containsKey(track.id)) {
            if (updateMode.shouldUpdate()) {
                return TRACK_CACHE.get(track.id).updateFrom(track, updateMode);
            } else {
                return TRACK_CACHE.get(track.id);
            }

        } else {
            TRACK_CACHE.put(track);
            return track;
        }
    }

    public ScResource cache(@Nullable User user) {
        return cache(user, ScModel.CacheUpdateMode.NONE);
    }

    public User cache(@Nullable User user, ScModel.CacheUpdateMode updateMode) {
        if (user == null) return null;

        if (USER_CACHE.containsKey(user.id)) {
            if (updateMode.shouldUpdate()) {
                return USER_CACHE.get(user.id).updateFrom(user, updateMode);
            } else {
                return USER_CACHE.get(user.id);
            }
        } else {
            USER_CACHE.put(user);
            return user;
        }
    }

    public boolean markTrackAsPlayed(Track track) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TrackMetadata._ID, track.id);
        return mResolver.insert(Content.TRACK_PLAYS.uri, contentValues) != null;
    }

    public Uri write(Track track) {
        return write(track, true);
    }

    public Uri write(Track track, boolean cache) {
        if (cache) TRACK_CACHE.putWithLocalFields(track);
        return SoundCloudDB.upsertTrack(mResolver, track);
    }

    public Uri write(User user) {
        return write(user, true);
    }

    public Uri write(User user, boolean cache) {
        if (cache) USER_CACHE.putWithLocalFields(user);
        return SoundCloudDB.upsertUser(mResolver, user);
    }

    private static List<ScResource> doBatchLookup(AndroidCloudAPI api, List<Long> ids, Content content) throws IOException {
        List<ScResource> resources = new ArrayList<ScResource>();
        int i = 0;
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + API_LOOKUP_BATCH_SIZE, ids.size()));
            InputStream is = validateResponse(
                    api.get(
                            Request.to(content.remoteUri)
                                    .add("linked_partitioning", "1")
                                    .add("limit", API_LOOKUP_BATCH_SIZE)
                                    .add("ids", TextUtils.join(",", batch)))).getEntity().getContent();

            resources.addAll(SoundCloudApplication.MODEL_MANAGER.getCollectionFromStream(is).collection);

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

    public int writeMissingCollectionItems(AndroidCloudAPI api,
                                               List<Long> modelIds,
                                               Content content,
                                               boolean ignoreStored) throws IOException {
        return writeMissingCollectionItems(api, modelIds, content, ignoreStored, -1);
    }

    /**
     * @param modelIds     a list of model ids
     * @param ignoreStored if it should ignore stored ids
     * @return a list of models which are not stored in the database
     * @throws java.io.IOException
     */
    public int writeMissingCollectionItems(AndroidCloudAPI api,
                                           List<Long> modelIds,
                                           Content content,
                                           boolean ignoreStored, int maxToFetch) throws IOException {
        if (modelIds == null || modelIds.isEmpty()) {
            return 0;
        }
        // copy so we don't modify the original
        List<Long> ids = new ArrayList<Long>(modelIds);

        if (!ignoreStored) {
            ids.removeAll(getStoredIds(mResolver, modelIds, content));
        }

        List<Long> fetchIds = (maxToFetch > -1) ? new ArrayList<Long>(ids.subList(0, Math.min(ids.size(), maxToFetch)))
                    : ids;

        return SoundCloudDB.bulkInsertModels(mResolver, doBatchLookup(api, fetchIds, content));
    }

    /**
     * @return a list of all ids for which objects are store in the database
     */
    private static List<Long> getStoredIds(ContentResolver resolver, List<Long> ids, Content content) {
        int i = 0;
        List<Long> storedIds = new ArrayList<Long>();
        while (i < ids.size()) {
            List<Long> batch = ids.subList(i, Math.min(i + RESOLVER_BATCH_SIZE, ids.size()));
            storedIds.addAll(SoundCloudDB.idCursorToList(
                    resolver.query(content.uri, new String[]{BaseColumns._ID},
                            DBHelper.getWhereInClause(BaseColumns._ID, batch) + " AND " + DBHelper.ResourceTable.LAST_UPDATED + " > 0"
                            , longListToStringArr(batch), null)
            ));
            i += RESOLVER_BATCH_SIZE;
        }
        return storedIds;
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

    public int writeCollectionFromStream(InputStream is, ScModel.CacheUpdateMode updateMode) throws IOException {
        return writeCollectionFromStream(is, null, -1, updateMode);
    }

    public int writeCollectionFromStream(InputStream is, Uri uri, long userId, ScModel.CacheUpdateMode updateMode) throws IOException {
        return writeCollection(getCollectionFromStream(is).collection, uri, userId, updateMode);
    }

    public Object writeCollection(List<ScResource> items, ScModel.CacheUpdateMode updateMode) {
        return writeCollection(items, null, -1l, updateMode);
    }

    public <T extends ScResource> int writeCollection(List<T> items, Uri localUri, long userId, ScModel.CacheUpdateMode updateMode) {
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
                SoundCloudDB.addPagingParams(Content.COLLECTION_ITEMS.uri, startIndex, limit),
                new String[]{DBHelper.CollectionItems.ITEM_ID},
                DBHelper.CollectionItems.COLLECTION_TYPE + " = ? AND " + DBHelper.CollectionItems.USER_ID + " = ?",
                new String[]{String.valueOf(content.collectionType), String.valueOf(userId)},
                DBHelper.CollectionItems.SORT_ORDER));
    }
}
