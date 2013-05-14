package com.soundcloud.android.service.sync;

import static com.soundcloud.android.AndroidCloudAPI.NotFoundException;
import static com.soundcloud.android.dao.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.dao.ResolverHelper.longListToStringArr;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.BaseDAO;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.dao.ConnectionDAO;
import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.dao.SoundAssociationStorage;
import com.soundcloud.android.dao.Storage;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.dao.UserStorage;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
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
    private static final int BULK_INSERT_BATCH_SIZE = 500;

    private final AndroidCloudAPI mApi;
    private final ContentResolver mResolver;
    private final Context mContext;
    private final SyncStateManager mSyncStateManager;
    private final ActivitiesStorage mActivitiesStorage;
    private final PlaylistStorage mPlaylistStorage;
    private final SoundAssociationStorage mSoundAssociationStorage;
    private final CollectionStorage mCollectionStorage;
    private final UserStorage mUserStorage;


    private int mBulkInsertBatchSize = BULK_INSERT_BATCH_SIZE;

    public ApiSyncer(Context context, ContentResolver resolver) {
        mApi = (AndroidCloudAPI) context.getApplicationContext();
        mResolver = resolver;
        mContext = context;
        mSyncStateManager = new SyncStateManager();
        mActivitiesStorage = new ActivitiesStorage();
        mPlaylistStorage = new PlaylistStorage();
        mSoundAssociationStorage = new SoundAssociationStorage();
        mCollectionStorage = new CollectionStorage();
        mUserStorage = new UserStorage();
    }

    // Tests want to override this value
    /* package */ void setBulkInsertBatchSize(int batchSize) {
        mBulkInsertBatchSize = batchSize;
    }

    public @NotNull Result syncContent(Uri uri, String action) throws IOException {
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

                case ME_FOLLOWINGS:
                case ME_FOLLOWERS:
                case ME_FRIENDS:
                    result = syncIdBasedContent(c, userId);
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
                case TRACK:
                case USER:
                    // sucks, but we'll kick out CP anyway
                    result = doResourceFetchAndInsert(uri, c == Content.TRACK ? new TrackStorage() : new UserStorage());
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
                        mSyncStateManager.forceToStale(Content.ME_FRIENDS.uri);
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

        List<ScResource> resources = mApi.readFullCollection(request, ScResource.ScResourceHolder.class);

        // manually build the sound association holder
        List<SoundAssociation> associations = new ArrayList<SoundAssociation>();

        for (ScResource resource : resources) {
            Playlist playlist = (Playlist) resource;
            associations.add(new SoundAssociation(playlist));
            boolean onWifi = IOUtils.isWifiConnected(mContext);
                // if we have never synced the playlist or are on wifi and past the stale time, fetch the tracks
                final LocalCollection localCollection = mSyncStateManager.fromContent(playlist.toUri());

                final boolean playlistStale = (localCollection.shouldAutoRefresh() && onWifi) || localCollection.last_sync_success <= 0;

                if (playlistStale && playlist.getTrackCount() < MAX_MY_PLAYLIST_TRACK_COUNT_SYNC) {
                    try {
                        playlist.tracks = mApi.readList(Request.to(TempEndpoints.PLAYLIST_TRACKS, playlist.id));
                    } catch (IOException e) {
                        // don't let the track fetch fail the sync, it is just an optimization
                        Log.e(TAG,"Failed to fetch playlist tracks for playlist " + playlist, e);
                    }
                }
                associations.add(new SoundAssociation(playlist));
            }
        return syncLocalSoundAssocations(Content.ME_PLAYLISTS.uri, associations);
    }

    /* package */ int pushLocalPlaylists() throws IOException {

        // check for local playlists that need to be pushed
        List<Playlist> playlistsToUpload = mPlaylistStorage.getLocalPlaylists();
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
                Playlist added = mApi.create(r);

                // update local state
                p.localToGlobal(mContext, added);
                mPlaylistStorage.create(added);
                mSoundAssociationStorage.addCreation(added);

                mSyncStateManager.updateLastSyncSuccessTime(p.toUri(), System.currentTimeMillis());

                // remove all traces of the old temporary playlist
                mPlaylistStorage.removePlaylist(toDelete);
            }
        }
        return playlistsToUpload.size();
    }

    private Result syncSoundAssociations(Content content, Uri uri, long userId) throws IOException {
        log("syncSoundAssociations(" + uri + ")");

        final Request request = Request.to(content.remoteUri, userId)
                .with("limit", 200)
                .with("representation", "mini");

        List<SoundAssociation> associations = mApi.readFullCollection(request, ScResource.ScResourceHolder.class);
        return syncLocalSoundAssocations(uri, associations);
    }

    private Result syncLocalSoundAssocations(Uri uri, List<SoundAssociation> associations) {
        boolean changed = mSoundAssociationStorage.syncToLocal(associations, uri);

        Result result = new Result(uri);
        result.change =  changed ? Result.CHANGED : Result.UNCHANGED;
        result.setSyncData(System.currentTimeMillis(), associations.size(), null);
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
            final Activity lastActivity = mActivitiesStorage.getLastActivity(c);
            Request request = new Request(c.request()).add("limit", Consts.COLLECTION_PAGE_SIZE);
            if (lastActivity != null) request.add("cursor", lastActivity.toGUID());
            activities = Activities.fetch(mApi, request);
            if (activities == null || activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(lastActivity))) {
                // this can happen at the end of the list
                inserted = 0;
            } else {
                inserted = mActivitiesStorage.insert(c, activities);
            }
        } else {
            String future_href = mSyncStateManager.getExtraFromUri(uri);

            Request request = future_href == null ? c.request() : Request.to(future_href);
            activities = Activities.fetchRecent(mApi, request, MAX_LOOKUP_COUNT);

                if (activities.moreResourcesExist()) {
                // delete all activities to avoid gaps in the data
                mResolver.delete(c.uri, null, null);
            }

            if (activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(mActivitiesStorage.getFirstActivity(c)))) {
                // this can happen at the beginning of the list if the api returns the first item incorrectly
                inserted = 0;
            } else {
                inserted = mActivitiesStorage.insert(c, activities);
            }
            result.setSyncData(System.currentTimeMillis(), activities.size(), activities.future_href);
        }

        result.change = inserted > 0 ? Result.CHANGED : Result.UNCHANGED;
        log("activities: inserted " + inserted + " objects");
        return result;
    }

    private Result syncIdBasedContent(Content content, final long userId) throws IOException {
        Result result = new Result(content.uri);
        if (!Content.ID_BASED.contains(content)) return result;

        List<Long> local  = mCollectionStorage.getLocalIds(content, userId);
        List<Long> remote = mApi.readFullCollection(Request.to(content.remoteUri + "/ids"), IdHolder.class);

        log("Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), remote.size(), null);

        if (checkUnchanged(content, result, local, remote)) return result;
        handleDeletions(content, local, remote);

        int startPosition = 1;
        int added = 0;
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // load the first page of items to get proper last_seen ordering
                // parse and add first items
                List<ScResource> resources = mApi.readList(Request.to(content.remoteUri)
                        .add(Wrapper.LINKED_PARTITIONING, "1")
                        .add("limit", Consts.COLLECTION_PAGE_SIZE));

                added = mCollectionStorage.insertCollection(resources, content.uri, userId);

                // remove items from master remote list and adjust start index
                for (ScResource u : resources) {
                    remote.remove(u.id);
                }
                startPosition = resources.size();
                break;
            case ME_FRIENDS:
                // sync all friends. It is the only way ordering works properly
                added = mCollectionStorage.fetchAndStoreMissingCollectionItems(
                        mApi,
                        remote,
                        Content.USERS,
                        false
                );
                break;
        }

        log("Added " + added + " new items for this endpoint");

        insertInBatches(content, userId, remote, startPosition);

        return result;
    }

    private void insertInBatches(final Content content, final long userId, final List<Long> ids, final int startPosition) {
        int numBatches = 1;
        int batchSize = ids.size();
        if (ids.size() > mBulkInsertBatchSize) {
            // split up the transaction into batches, so as to not block readers too long
            numBatches = (int) Math.ceil((float) ids.size() / mBulkInsertBatchSize);
            batchSize = mBulkInsertBatchSize;
        }
        log("numBatches: " + numBatches);
        log("batchSize: " + batchSize);

        // insert in batches so as to not hold a write lock in a single transaction for too long
        int positionOffset = startPosition;
        for (int i = 0; i < numBatches; i++) {
            int batchStart = i * batchSize;
            int batchEnd = Math.min(batchStart + batchSize, ids.size());
            log("batch " + i + ": start / end = " + batchStart + " / " + batchEnd);

            List<Long> idBatch = ids.subList(batchStart, batchEnd);
            ContentValues[] cv = new ContentValues[idBatch.size()];

            for (int j = 0; j < idBatch.size(); j++) {
                long id = idBatch.get(j);
                cv[j] = new ContentValues();
                cv[j].put(DBHelper.CollectionItems.POSITION, positionOffset + j);
                cv[j].put(DBHelper.CollectionItems.ITEM_ID, id);
                cv[j].put(DBHelper.CollectionItems.USER_ID, userId);
            }
            positionOffset += idBatch.size();
            mResolver.bulkInsert(content.uri, cv);
        }
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
        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);
        if (!itemDeletions.isEmpty()) {
            log("Need to remove " + itemDeletions.size() + " items");
            int i = 0;
            while (i < itemDeletions.size()) {
                List<Long> batch = itemDeletions.subList(i, Math.min(i + BaseDAO.RESOLVER_BATCH_SIZE, itemDeletions.size()));
                mResolver.delete(content.uri, getWhereInClause(DBHelper.CollectionItems.ITEM_ID, batch.size()), longListToStringArr(batch));
                i += BaseDAO.RESOLVER_BATCH_SIZE;
            }
        }
        return itemDeletions;
    }

    private Result syncMe(Content c, long userId) throws IOException {
        Result result = new Result(c.uri);
        User user = new FetchUserTask(mApi).resolve(c.request());
        result.synced_at = System.currentTimeMillis();
        if (user != null) {
            mUserStorage.createOrUpdate(user);
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
        // compare local vs remote connections
        List<Connection> list = mApi.readList(Content.ME_CONNECTIONS.request());
        Set<Connection> remoteConnections = new HashSet<Connection>(list);
        ConnectionDAO connectionDAO = new ConnectionDAO(mResolver);
        Set<Connection> storedConnections = new HashSet<Connection>(connectionDAO.queryAll());

        if (storedConnections.equals(remoteConnections)) {
            result.change = Result.UNCHANGED;
        } else {
            result.change = Result.CHANGED;
            // remove unneeded connections
            storedConnections.removeAll(remoteConnections);
            connectionDAO.deleteAll(storedConnections);
        }
        int inserted = connectionDAO.createCollection(remoteConnections);
        result.setSyncData(System.currentTimeMillis(), inserted, null);
        result.success = true;
        return result;
    }

    private Result syncPlaylist(Uri contentUri) throws IOException {
        log("Syncing playlist " + contentUri);

        Result result = new Result(contentUri);

        try {
            Playlist p = mApi.read(Content.match(contentUri).request(contentUri));

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
                p = mApi.update(r);
            }
            if (c != null) c.close();

            mPlaylistStorage.create(p);
            final Uri insertedUri = p.toUri();
            if (insertedUri != null) {
                log("inserted " + insertedUri.toString());
                result.setSyncData(true, System.currentTimeMillis(), 1, Result.CHANGED);
            } else {
                log("failed to create to " + contentUri);
                result.success = false;
            }
            return result;

        } catch (NotFoundException e) {
            log("Received a 404 on playlist, deleting " + contentUri.toString());
            mPlaylistStorage.removePlaylist(contentUri);
            result.setSyncData(true, System.currentTimeMillis(), 0, Result.CHANGED);
            return result;
        }
    }

    /**
     * Fetch a single resource from the api and create it into the content provider.
     * @param contentUri the content point to get the request and content provider destination from.
     *                   see {@link Content}
     * @return the result of the operation
     * @throws IOException
     */
    private <T extends ScResource> Result doResourceFetchAndInsert(Uri contentUri, Storage<T> storage) throws IOException {
        Result result = new Result(contentUri);
        T resource = mApi.read(Content.match(contentUri).request(contentUri));

        storage.create(resource);

        final Uri insertedUri = resource.toUri();

        if (insertedUri != null){
            log("inserted " + insertedUri.toString());
            result.setSyncData(true, System.currentTimeMillis(), 1, Result.CHANGED);
        } else {
            log("failed to create to " + contentUri);
            result.success = false;
        }
        return result;
    }

    /**
     * Fetch Api Resources and create them into the content provider. Plain resource inserts, no extra
     * content values will be inserted
     * @throws IOException
     */
    private Result fetchAndInsertCollection(final Content content, Uri contentUri) throws IOException {
        Result result = new Result(contentUri);
        log("fetchAndInsertCollection(" + contentUri + ")");

        Request request = Request.to(content.remoteUri).add("ids", contentUri.getLastPathSegment());

        if (Content.PLAYLIST_LOOKUP.equals(content)) {
            HttpUtils.addQueryParams(request, "representation", "compact");
        }

        List<ScResource> resources = mApi.readFullCollection(request, ScResource.ScResourceHolder.class);

        new BaseDAO<ScResource>(mResolver) {
            @Override public Content getContent() {
                return content;
            }
        }.createCollection(resources);
        result.setSyncData(true, System.currentTimeMillis(), resources.size(), Result.CHANGED);
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
