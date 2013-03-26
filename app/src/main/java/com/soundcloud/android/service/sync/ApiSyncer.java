package com.soundcloud.android.service.sync;

import static com.soundcloud.android.model.ScModelManager.validateResponse;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Performs the actual sync with the API. Used by {@link CollectionSyncRequest}.
 */
public class ApiSyncer {
    public static final String TAG = ApiSyncService.LOG_TAG;
    private static final int MAX_LOOKUP_COUNT = 100; // each time we sync, lookup a maximum of this number of items
    private static final int MAX_MY_PLAYLIST_TRACK_COUNT_SYNC = 100;

    private final AndroidCloudAPI mApi;
    private final ContentResolver mResolver;
    private final Context mContext;

    public ApiSyncer(Context context) {
        mApi = (AndroidCloudAPI) context.getApplicationContext();
        mResolver = context.getContentResolver();
        mContext = context;
    }

    public Result syncContent(Uri uri, String action) throws IOException {
        final long userId = SoundCloudApplication.getUserIdFromContext(mContext);
        Content c = Content.match(uri);
        Result result = new Result(uri);

        if (userId <= 0){
            Log.w(TAG, "Invalid user id, skipping sync ");
        } else if (c.remoteUri != null) {
            switch (c) {
                case ME:
                    result = syncMe(c, userId);
                    if (result.success) {
                        mResolver.notifyChange(Content.ME.uri, null);
                    }
                    PreferenceManager.getDefaultSharedPreferences(mContext)
                            .edit()
                            .putLong(Consts.PrefKeys.LAST_USER_SYNC, System.currentTimeMillis())
                            .commit();

                    break;
                case ME_ALL_ACTIVITIES:
                case ME_ACTIVITIES:
                case ME_SOUND_STREAM:
                    result = syncActivities(uri, action);
                    result.success = true;
                    break;

                case ME_LIKES:
                case ME_SOUNDS:
                    result = syncSoundAssociations(c, uri, userId);
                    break;

                case ME_TRACKS:
                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                case ME_FRIENDS:
                    result = syncContent(c, userId);
                    result.success = true;
                    break;

                case PLAYLIST_LOOKUP:
                case TRACK_LOOKUP:
                case USER_LOOKUP:
                    result = fetchAndInsertCollection(c, uri);
                    break;

                case ME_PLAYLISTS:
                    result = syncMyPlaylists();
                    break;

                case PLAYLIST_TRACKS:
                    result = fetchAndInsertCollectionToUri(uri, userId);
                    break;

                case TRACK:
                case USER:
                    result = doResourceFetchAndInsert(uri);
                    break;

                case PLAYLIST:
                    result = syncPlaylist(uri);
                    break;

                case ME_SHORTCUTS:
                    result = syncMyGenericResource(c);
                    break;

                case ME_CONNECTIONS:
                    result = syncMyConnections();
                    if (result.change == Result.CHANGED){
                        // connections changed so make sure friends gets auto synced next opportunity
                        LocalCollection.forceToStale(Content.ME_FRIENDS.uri, mResolver);
                    }
                    break;
            }
        } else {
            switch (c) {
                case PLAYABLE_CLEANUP:
                case USERS_CLEANUP:
                case SOUND_STREAM_CLEANUP:
                case ACTIVITIES_CLEANUP:
                    result = new Result(c.uri);
                    result.success = true;
                    if (mResolver.update(uri, null, null, null) > 0) {
                        result.change = Result.CHANGED;
                    }
                    result.setSyncData(System.currentTimeMillis(),-1,null);
                    break;
                default:
                    Log.w(TAG, "no remote URI defined for " + c);
            }

        }
        return result;
    }


    /**
     * Pushes any locally created playlists to the server, fetches the user's playlists from the server,
     * and fetches tracks for these playlists that are missing locally.
     *
     * This is specific because the Api does not return these as sound associations, otherwise
     * we could use that path
     */
    private Result syncMyPlaylists() throws IOException {
        pushLocalPlaylists();

        final Request request = Request.to(Content.ME_PLAYLISTS.remoteUri)
                .add("representation", "compact").with("limit", 200);

        ScResource.ScResourceHolder holder = CollectionHolder.fetchAllResourcesHolder(mApi,
                request, ScResource.ScResourceHolder.class);

        // manually build the sound association holder
        SoundAssociationHolder soundAssociationHolder = new SoundAssociationHolder(new ArrayList<SoundAssociation>());
        for (ScResource resource : holder) {
            Playlist playlist = (Playlist) resource;
            soundAssociationHolder.collection.add(new SoundAssociation(playlist, playlist.created_at, SoundAssociation.Type.PLAYLIST));
            boolean onWifi = IOUtils.isWifiConnected(mContext);

                // if we have never synced the playlist or are on wifi and past the stale time, fetch the tracks
                final LocalCollection localCollection = LocalCollection.fromContentUri(playlist.toUri(), mResolver, true);
                if (localCollection == null) continue;

                final boolean playlistStale = (localCollection.shouldAutoRefresh() && onWifi) || localCollection.last_sync_success <= 0;

                if (playlistStale && playlist.getTrackCount() < MAX_MY_PLAYLIST_TRACK_COUNT_SYNC) {
                    try {
                        HttpResponse resp = mApi.get(Request.to(TempEndpoints.PLAYLIST_TRACKS, playlist.id));
                        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            playlist.tracks = mApi.getMapper().readValue(resp.getEntity().getContent(),
                                    mApi.getMapper().getTypeFactory().constructCollectionType(List.class, Track.class));
                        }
                    } catch (IOException e) {
                        // don't let the track fetch fail the sync, it is just an optimization
                        Log.e(TAG,"Failed to fetch playlist tracks for playlist " + playlist, e);
                    }
                }
                soundAssociationHolder.add(new SoundAssociation(playlist, playlist.created_at, SoundAssociation.Type.PLAYLIST));
            }
        return syncLocalSoundAssocationsToHolder(Content.ME_PLAYLISTS.uri, soundAssociationHolder);
    }

    /* package */ int pushLocalPlaylists() throws IOException {

        // check for local playlists that need to be pushed
        List<Playlist> playlistsToUpload = Playlist.getLocalPlaylists(mResolver);
        if (!playlistsToUpload.isEmpty()) {

            for (Playlist p : playlistsToUpload) {

                Uri toDelete = p.toUri();

                Playlist.ApiCreateObject createObject = new Playlist.ApiCreateObject(p);

                if (createObject.tracks == null){
                    // add the tracks
                    createObject.tracks = new ArrayList<ScModel>();
                    Cursor itemsCursor = mResolver.query(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(p.id)),
                            new String[]{DBHelper.PlaylistTracksView._ID}, null, null, null);

                    if (itemsCursor != null) {
                        while (itemsCursor.moveToNext()) {
                            createObject.tracks.add(new ScModel(itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.PlaylistTracksView._ID))));
                        }
                        itemsCursor.close();
                    }
                }

                final String content = mApi.getMapper().writeValueAsString(createObject);
                log("Pushing new playlist to api: " + content);
                Request r = Request.to(TempEndpoints.PLAYLISTS).withContent(content, "application/json");
                InputStream is = validateResponse(mApi.post(r)).getEntity().getContent();

                Playlist added = SoundCloudApplication.MODEL_MANAGER.getModelFromStream(is);

                // update local state
                p.localToGlobal(mContext, added);
                added.insertAsMyPlaylist(mResolver);

                LocalCollection lc = LocalCollection.fromContentUri(p.toUri(), mResolver, true);
                if (lc != null) {
                    lc.updateLastSyncSuccessTime(System.currentTimeMillis(), mResolver);
                }

                // remove all traces of the old temporary playlist
                Playlist.removePlaylist(mResolver, toDelete);

            }
        }
        return playlistsToUpload.size();
    }

    private Result syncSoundAssociations(Content content, Uri uri, long userId) throws IOException {
        log("syncSoundAssociations(" + uri + ")");

        SoundAssociationHolder holder = CollectionHolder.fetchAllResourcesHolder(mApi,
                Request.to(content.remoteUri, userId).with("limit", 200).with("representation", "mini"),
                SoundAssociationHolder.class);

        return syncLocalSoundAssocationsToHolder(uri, holder);
    }

    private Result syncLocalSoundAssocationsToHolder(Uri uri, SoundAssociationHolder holder) {
        boolean changed = holder.syncToLocal(mResolver, uri);

        Result result = new Result(uri);
        result.change =  changed ? Result.CHANGED : Result.UNCHANGED;
        result.setSyncData(System.currentTimeMillis(), holder.collection.size(), null);
        result.success = true;
        return result;
    }

    private Result syncActivities(Uri uri, String action) throws IOException {
        Result result = new Result(uri);
        log("syncActivities(" + uri + ")");

        final Content c = Content.match(uri);
        final int inserted;
        Activities activities;
        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            final Activity lastActivity = Activities.getLastActivity(c, mResolver);
            Request request = new Request(c.request()).add("limit", Consts.COLLECTION_PAGE_SIZE);
            if (lastActivity != null) request.add("cursor", lastActivity.toGUID());
            activities = Activities.fetch(mApi, request);
            if (activities == null || activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(lastActivity))) {
                // this can happen at the end of the list
                inserted = 0;
            } else {
                inserted = activities.insert(c, mResolver);
            }
        } else {
            String future_href = LocalCollection.getExtraFromUri(uri, mResolver);
            Request request = future_href == null ? c.request() : Request.to(future_href);
            activities = Activities.fetchRecent(mApi, request, MAX_LOOKUP_COUNT);

                if (activities.hasMore()) {
                // delete all activities to avoid gaps in the data
                mResolver.delete(c.uri, null, null);
            }

            if (activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(Activities.getFirstActivity(c, mResolver)))) {
                // this can happen at the beginning of the list if the api returns the first item incorrectly
                inserted = 0;
            } else {
                inserted = activities.insert(c, mResolver);
            }
            result.setSyncData(System.currentTimeMillis(), activities.size(), activities.future_href);
        }

        result.change = inserted > 0 ? Result.CHANGED : Result.UNCHANGED;
        log("activities: inserted " + inserted + " objects");
        return result;
    }

    private Result syncContent(Content content, final long userId) throws IOException {
        Result result = new Result(content.uri);

        List<Long> local = SoundCloudApplication.MODEL_MANAGER.getLocalIds(content, userId);
        List<Long> remote = CollectionHolder.fetchAllResources(mApi, Request.to(content.remoteUri + "/ids"), IdHolder.class);

        log("Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), remote.size(), null);

        if (checkUnchanged(content, result, local, remote)) return result;
        handleDeletions(content, local, remote);

        int startPosition = 1;
        int added;
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // load the first page of items to get proper last_seen ordering
                InputStream is = validateResponse(mApi.get(Request.to(content.remoteUri)
                        .add("linked_partitioning", "1")
                        .add("limit", Consts.COLLECTION_PAGE_SIZE)))
                        .getEntity().getContent();

                // parse and add first items
                CollectionHolder<User> firstUsers = SoundCloudApplication.MODEL_MANAGER.getCollectionFromStream(is);
                added = SoundCloudApplication.MODEL_MANAGER.writeCollection(
                        firstUsers.collection, content.uri, userId, ScResource.CacheUpdateMode.FULL
                );

                // remove items from master remote list and adjust start index
                for (User u : firstUsers) {
                    remote.remove(u.id);
                }

                startPosition = firstUsers.size();
                break;
            case ME_FRIENDS:
                // sync all friends. It is the only way ordering works properly
                added = SoundCloudApplication.MODEL_MANAGER.fetchMissingCollectionItems(
                        mApi,
                        remote,
                        Content.USERS,
                        false,
                        -1
                );
                break;
            default:
                // ensure the first couple of pages of items for quick loading
                added = SoundCloudApplication.MODEL_MANAGER.fetchMissingCollectionItems(
                        mApi,
                        remote,
                        Track.class.equals(content.modelType) ? Content.TRACKS : Content.USERS,
                        false,
                        MAX_LOOKUP_COUNT
                );
                break;
        }

        log("Added " + added + " new items for this endpoint");
        ContentValues[] cv = new ContentValues[remote.size()];
        int i = 0;
        for (Long id : remote) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, startPosition + i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, id);
            cv[i].put(DBHelper.CollectionItems.USER_ID, userId);
            i++;
        }
        mResolver.bulkInsert(content.uri, cv);
        return result;
    }

    private boolean checkUnchanged(Content content, Result result, List<Long> local, List<Long> remote) {
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                Set<Long> localSet = new HashSet<Long>(local);
                Set<Long> remoteSet = new HashSet<Long>(remote);
                if (!localSet.equals(remoteSet)) {
                    result.change = Result.CHANGED;
                    result.extra = "0"; // reset sync misses
                } else {
                    result.change = remoteSet.isEmpty() ? Result.UNCHANGED : Result.REORDERED; // always mark users as reordered so we get the first page
                }
                break;
            default:
                if (!local.equals(remote)) {
                    // items have been added or removed (not just ordering) so this is a sync hit
                    result.change = Result.CHANGED;
                    result.extra = "0"; // reset sync misses
                } else {
                    result.change = Result.UNCHANGED;
                    log("Cloud Api service: no change in URI " + content.uri + ". Skipping sync.");
                }
        }
        return result.change == Result.UNCHANGED;
    }

    private List<Long> handleDeletions(Content content, List<Long> local, List<Long> remote) {
        // This only works when items are unique in a collection, fine for now
        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);
        if (!itemDeletions.isEmpty()) {
            log("Need to remove " + itemDeletions.size() + " items");
            int i = 0;
            while (i < itemDeletions.size()) {
                List<Long> batch = itemDeletions.subList(i, Math.min(i + SoundCloudDB.RESOLVER_BATCH_SIZE, itemDeletions.size()));
                mResolver.delete(content.uri, DBHelper.getWhereInClause(DBHelper.CollectionItems.ITEM_ID, batch), ScModelManager.longListToStringArr(batch));
                i += SoundCloudDB.RESOLVER_BATCH_SIZE;
            }
        }
        return itemDeletions;
    }

    private Result syncMe(Content c, long userId) throws IOException {
        Result result = new Result(c.uri);
        User user = new FetchUserTask(mApi).resolve(c.request());
        result.synced_at = System.currentTimeMillis();
        if (user != null) {
            SoundCloudApplication.MODEL_MANAGER.cacheAndWrite(user, ScResource.CacheUpdateMode.FULL);
            result.change = Result.CHANGED;
            result.success = true;
        }
        return result;
    }

    /**
     * Good for syncing any generic item that doesn't require special ordering or cache handling
     * e.g. Shortcuts, Connections
     * @param c the content to be synced
     * @return the syncresult
     * @throws IOException
     */
    private Result syncMyGenericResource(Content c) throws IOException {
        log("Syncing generic resource " + c.uri);

        Result result = new Result(c.uri);
        final Request request = c.request();
        HttpResponse resp = mApi.get(request);
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            List<ScModel> models = mApi.getMapper().readValue(resp.getEntity().getContent(),
                    mApi.getMapper().getTypeFactory().constructCollectionType(List.class, c.modelType));

            List<ContentValues> cvs = new ArrayList<ContentValues>(models.size());
            for (ScModel model : models) {
                ContentValues cv = model.buildContentValues();
                if (cv != null) cvs.add(cv);
            }

            int inserted = 0;
            if (!cvs.isEmpty()) {
                inserted = mResolver.bulkInsert(c.uri, cvs.toArray(new ContentValues[cvs.size()]));
                log("inserted " + inserted + " generic models");
            }

            result.setSyncData(System.currentTimeMillis(), inserted, null);
            result.success = true;
        } else if (Log.isLoggable(TAG, Log.WARN)){
            Log.w(TAG, "request "+ request +" returned "+resp.getStatusLine());
        }
        return result;
    }

    private Result syncMyConnections() throws IOException {
        log("Syncing my connections");

        Result result = new Result(Content.ME_CONNECTIONS.uri);
        HttpResponse resp = mApi.get(Content.ME_CONNECTIONS.request());
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

            // compare local vs remote connections
            HashSet<Connection> remoteConnections = mApi.getMapper().readValue(resp.getEntity().getContent(),
                    mApi.getMapper().getTypeFactory().constructCollectionType(Set.class, Content.ME_CONNECTIONS.modelType));

            Cursor c = mResolver.query(Content.ME_CONNECTIONS.uri, null, null, null, null);
            HashSet<Connection> storedConnections = new HashSet<Connection>();
            if (c != null && c.moveToFirst()) {
                do {
                    storedConnections.add(new Connection(c));
                } while (c.moveToNext());
            }

            if (storedConnections.equals(remoteConnections)) {
                result.change = Result.UNCHANGED;
            } else {
                result.change = Result.CHANGED;
                // remove unneeded connections
                storedConnections.removeAll(remoteConnections);
                List<Long> toRemove = new ArrayList<Long>();
                for (Connection storedConnection : storedConnections) {
                    toRemove.add(storedConnection.id);
                }
                if (!toRemove.isEmpty()) {
                    mResolver.delete(Content.ME_CONNECTIONS.uri, DBHelper.getWhereInClause(DBHelper.Connections._ID, toRemove), ScModelManager.longListToStringArr(toRemove));
                }
            }

            // write anyways to update connections
            List<ContentValues> cvs = new ArrayList<ContentValues>(remoteConnections.size());
            for (Connection connection : remoteConnections) {
                ContentValues cv = connection.buildContentValues();
                if (cv != null) cvs.add(cv);
            }

            int inserted = 0;
            if (!cvs.isEmpty()) {
                inserted = mResolver.bulkInsert(Content.ME_CONNECTIONS.uri, cvs.toArray(new ContentValues[cvs.size()]));
                log("inserted " + inserted + " generic models");
            }

            result.setSyncData(System.currentTimeMillis(), inserted, null);
            result.success = true;
        }
        return result;
    }

    private Result syncPlaylist(Uri contentUri) throws IOException {
        log("Syncing playlist " + contentUri);

        Result result = new Result(contentUri);
        final HttpResponse response = mApi.get(Content.match(contentUri).request(contentUri));

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            log("Received a 404 on playlist, deleting " + contentUri.toString());
            Playlist.removePlaylist(mResolver,contentUri);
            result.setSyncData(true, System.currentTimeMillis(), 0, Result.CHANGED);
            return result;
        }

        InputStream is = validateResponse(response).getEntity().getContent();

        // todo, one call
        Playlist p = (Playlist) SoundCloudApplication.MODEL_MANAGER.cache(
                SoundCloudApplication.MODEL_MANAGER.getModelFromStream(is), ScResource.CacheUpdateMode.FULL);

        Cursor c = mResolver.query(
                Content.PLAYLIST_TRACKS.forId(p.id), new String[]{DBHelper.PlaylistTracksView._ID},
                DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " IS NOT NULL", null,
                DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " ASC");


        if (c != null && c.getCount() > 0) {
            Set<Long> toAdd = new LinkedHashSet<Long>(c.getCount());
            for (Track t : p.tracks) {
                toAdd.add(t.id);
            }
            while (c.moveToNext()) {
                toAdd.add(c.getLong(0));
            }

            Playlist.ApiUpdateObject updateObject = new Playlist.ApiUpdateObject(toAdd);
            final String content = mApi.getMapper().writeValueAsString(updateObject);
            log("Pushing new playlist content to api: " + content);

            Request r = Content.PLAYLIST.request(contentUri).withContent(content, "application/json");
            is.close();

            is = validateResponse(mApi.put(r)).getEntity().getContent();
            p = (Playlist) SoundCloudApplication.MODEL_MANAGER.cache(
                            SoundCloudApplication.MODEL_MANAGER.getModelFromStream(is), ScResource.CacheUpdateMode.FULL);
        }
        if (c != null) c.close();

        final Uri insertedUri = p.insert(mResolver);
        if (insertedUri != null) {
            log("inserted " + insertedUri.toString());
            result.setSyncData(true, System.currentTimeMillis(), 1, Result.CHANGED);
        } else {
            log("failed to insert to " + contentUri);
            result.success = false;
        }
        return result;
    }

    /**
     * Fetch a single resource from the api and insert it into the content provider.
     * @param contentUri the content point to get the request and content provider destination from.
     *                   see {@link Content}
     * @return the result of the operation
     * @throws IOException
     */
    private Result doResourceFetchAndInsert(Uri contentUri) throws IOException {
        Result result = new Result(contentUri);
        InputStream is = validateResponse(mApi.get(Content.match(contentUri).request(contentUri))).getEntity().getContent();

        final Uri insertedUri = SoundCloudApplication.MODEL_MANAGER.getModelFromStream(is).insert(mResolver);
        if (insertedUri != null){
            log("inserted " + insertedUri.toString());
            result.setSyncData(true, System.currentTimeMillis(), 1, Result.CHANGED);
        } else {
            log("failed to insert to " + contentUri);
            result.success = false;
        }
        return result;
    }

    /**
     * Fetch Api Resources and insert them into the content provider. Plain resource inserts, no extra
     * content values will be inserted
     * @throws IOException
     */
    private Result fetchAndInsertCollection(Content content, Uri contentUri) throws IOException {
        Result result = new Result(contentUri);
        log("fetchAndInsertCollection(" + contentUri + ")");

        Request request = Request.to(content.remoteUri).add("ids", contentUri.getLastPathSegment());
        if (Content.PLAYLIST_LOOKUP.equals(content)) {
            HttpUtils.addQueryParams(request, "representation", "compact");
        }

        ScResource.ScResourceHolder holder = CollectionHolder.fetchAllResourcesHolder(mApi,
                request, ScResource.ScResourceHolder.class);

        SoundCloudDB.bulkInsertResources(mResolver, holder.collection);
        result.setSyncData(true, System.currentTimeMillis(), holder.collection.size(), Result.CHANGED);
        return result;
    }


    /**
     * Fetch Api Resources and insert them into the content provider using a specific content uri
     * that may require special handling in {@link SoundCloudDB#insertCollection(ContentResolver, List, Uri, long)}
     * @param uri contentUri to insert to
     * @param userId logged in user (only used in associations, e.g. followers)
     * @return the result of the operation
     * @throws IOException
     */
    private Result fetchAndInsertCollectionToUri(Uri uri, long userId) throws IOException {
           Result result = new Result(uri);
           log("fetchAndInsertCollectionToUri(" + uri + ")");

           ScResource.ScResourceHolder holder = CollectionHolder.fetchAllResourcesHolder(mApi,
                   Content.match(uri).request(uri).add("linked_partitioning", "1"), ScResource.ScResourceHolder.class);

        SoundCloudDB.insertCollection(mResolver, holder.collection, uri, userId);
        result.setSyncData(true, System.currentTimeMillis(), holder.collection.size(), Result.CHANGED);
        return result;
       }


    public static class Result {
        public static final int UNCHANGED = 0;
        public static final int REORDERED = 1;
        public static final int CHANGED   = 2;

        public final Uri uri;
        public final SyncResult syncResult = new SyncResult();

        /** One of {@link #UNCHANGED}, {@link #REORDERED}, {@link #CHANGED}. */
        public int change;

        public boolean success;

        public long synced_at;
        public int new_size;
        public String extra;

        public Result(Uri uri) {
            this.uri = uri;
        }

        public void setSyncData(boolean success, long synced_at, int new_size, int change){
            this.success = success;
            this.synced_at = synced_at;
            this.new_size = new_size;
            this.change = change;
        }

        public void setSyncData(long synced_at, int new_size, @Nullable String extra){
            this.synced_at = synced_at;
            this.new_size = new_size;
            this.extra = extra;
        }

        public static Result fromAuthException(Uri uri) {
            Result r = new Result(uri);
            r.syncResult.stats.numAuthExceptions++;
            return r;
        }

        public static Result fromIOException(Uri uri) {
            Result r = new Result(uri);
            r.syncResult.stats.numIoExceptions++;
            return r;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "uri=" + uri +
                    ", syncResult=" + syncResult +
                    ", change=" + change +
                    ", success=" + success +
                    ", synced_at=" + synced_at +
                    ", new_size=" + new_size +
                    ", extra='" + extra + '\'' +
                    '}';
        }
    }

    private static void log(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }

    public static class IdHolder extends CollectionHolder<Long> {
    }
}
