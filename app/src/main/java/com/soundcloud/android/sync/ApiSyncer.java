package com.soundcloud.android.sync;

import static com.soundcloud.android.api.PublicCloudAPI.NotFoundException;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.TempEndpoints;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.storage.ConnectionDAO;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.Storage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.activities.Activities;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.exception.PlaylistUpdateException;
import com.soundcloud.android.tasks.FetchUserTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Performs the actual sync with the API. Used by {@link CollectionSyncRequest}.
 * <p/>
 * As a client, do not use this class directly, but use {@link SyncOperationsOld} instead.
 * <p/>
 * TODO: make package level visible again after removing {@link com.soundcloud.android.collections.tasks.ActivitiesLoader}
 * TODO: Split this up into different syncers
 */
public class ApiSyncer extends SyncStrategy {
    private static final int MAX_LOOKUP_COUNT = 100; // each time we sync, lookup a maximum of this number of items
    private static final int MAX_MY_PLAYLIST_TRACK_COUNT_SYNC = 30;

    private final ActivitiesStorage mActivitiesStorage;
    private final PlaylistStorage mPlaylistStorage;
    private final SoundAssociationStorage mSoundAssociationStorage;
    private final UserStorage mUserStorage;

    public ApiSyncer(Context context, ContentResolver resolver) {
        super(context, resolver);
        mActivitiesStorage = new ActivitiesStorage();
        mPlaylistStorage = new PlaylistStorage();
        mSoundAssociationStorage = new SoundAssociationStorage();
        mUserStorage = new UserStorage();
    }

    @NotNull
    public ApiSyncResult syncContent(Uri uri, String action) throws IOException {
        final long userId = SoundCloudApplication.getUserIdFromContext(mContext);
        Content c = Content.match(uri);
        ApiSyncResult result = new ApiSyncResult(uri);

        if (userId <= 0) {
            Log.w(TAG, "Invalid user id, skipping sync ");
        } else if (c.remoteUri != null) {
            switch (c) {
                case ME:
                    result = syncMe(c, userId);
                    if (result.success) {
                        mResolver.notifyChange(Content.ME.uri, null);
                        User loggedInUser = SoundCloudApplication.fromContext(mContext).getLoggedInUser();
                        EventBus.CURRENT_USER_CHANGED.publish(CurrentUserChangedEvent.forUserUpdated(loggedInUser));
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
                    Storage<? extends ScResource> storage = c == Content.TRACK ? new TrackStorage() : new UserStorage();
                    result = doResourceFetchAndInsert(uri, storage);
                    break;

                case PLAYLIST:
                    result = syncPlaylist(uri);
                    break;

                case ME_SHORTCUTS:
                    result = syncMyGenericResource(c);
                    break;

                case ME_CONNECTIONS:
                    result = syncMyConnections();
                    break;
            }
        } else {
            switch (c) {
                case PLAYABLE_CLEANUP:
                case USERS_CLEANUP:
                case SOUND_STREAM_CLEANUP:
                case ACTIVITIES_CLEANUP:
                    result = new ApiSyncResult(c.uri);
                    result.success = true;
                    if (mResolver.update(uri, null, null, null) > 0) {
                        result.change = ApiSyncResult.CHANGED;
                    }
                    result.setSyncData(System.currentTimeMillis(), -1);
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
     * <p/>
     * This is specific because the Api does not return these as sound associations, otherwise
     * we could use that path
     */
    private ApiSyncResult syncMyPlaylists() throws IOException {
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
                    playlist.tracks = mApi.readList(Request.to(TempEndpoints.PLAYLIST_TRACKS, playlist.getId()));
                } catch (IOException e) {
                    // don't let the track fetch fail the sync, it is just an optimization
                    Log.e(TAG, "Failed to fetch playlist tracks for playlist " + playlist, e);
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

                if (createObject.tracks == null) {
                    // add the tracks
                    createObject.tracks = new ArrayList<ScModel>();
                    Cursor itemsCursor = mResolver.query(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(p.getId())),
                            new String[]{DBHelper.PlaylistTracksView._ID}, null, null, null);

                    if (itemsCursor != null) {
                        while (itemsCursor.moveToNext()) {
                            createObject.tracks.add(new ScModel(itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.PlaylistTracksView._ID))));
                        }
                        itemsCursor.close();
                    }
                }

                final String content = createObject.toJson();
                log("Pushing new playlist to api: " + content);

                Request r = Request.to(TempEndpoints.PLAYLISTS).withContent(content, "application/json");
                Playlist playlist;
                final ScResource result = mApi.create(r);
                if (result instanceof Playlist){
                    playlist = (Playlist) result;
                } else {
                    // Debugging. Return objects sometimes are not playlists. Need to log this to get more info about the cause of this
                    SoundCloudApplication.handleSilentException("Error adding new playlist " + p, new PlaylistUpdateException(content));
                    continue; // this will get retried next sync
                }


                // update local state
                p.localToGlobal(mContext, playlist);
                SoundCloudApplication.sModelManager.removeFromCache(toDelete);

                mPlaylistStorage.store(playlist);
                mSoundAssociationStorage.addCreation(playlist);

                mSyncStateManager.updateLastSyncSuccessTime(p.toUri(), System.currentTimeMillis());

                // remove all traces of the old temporary playlist
                mPlaylistStorage.removePlaylist(toDelete);
            }
        }
        return playlistsToUpload.size();
    }

    private ApiSyncResult syncSoundAssociations(Content content, Uri uri, long userId) throws IOException {
        log("syncSoundAssociations(" + uri + ")");

        final Request request = Request.to(content.remoteUri, userId)
                .with("limit", 200)
                .with("representation", "mini");

        List<SoundAssociation> associations = mApi.readFullCollection(request, ScResource.ScResourceHolder.class);
        return syncLocalSoundAssocations(uri, associations);
    }

    private ApiSyncResult syncLocalSoundAssocations(Uri uri, List<SoundAssociation> associations) {
        boolean changed = mSoundAssociationStorage.syncToLocal(associations, uri);

        ApiSyncResult result = new ApiSyncResult(uri);
        result.change = changed ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
        result.setSyncData(System.currentTimeMillis(), associations.size());
        result.success = true;
        return result;
    }

    private ApiSyncResult syncActivities(Uri uri, String action) throws IOException {
        ApiSyncResult result = new ApiSyncResult(uri);
        log("syncActivities(" + uri + ")");

        final Content c = Content.match(uri);
        final int inserted;
        Activities activities;
        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            final Activity oldestActivity = mActivitiesStorage.getOldestActivity(c);
            Request request = new Request(c.request()).add("limit", Consts.COLLECTION_PAGE_SIZE);
            if (oldestActivity != null) request.add("cursor", oldestActivity.toGUID());
            activities = Activities.fetch(mApi, request);
            if (activities == null || activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(oldestActivity))) {
                // this can happen at the end of the list
                inserted = 0;
            } else {
                inserted = mActivitiesStorage.insert(c, activities);
            }
        } else {
            final Activity newestActivity = mActivitiesStorage.getLatestActivity(c);
            Request request = new Request(c.request());
            if (newestActivity != null) request.add("uuid[to]", newestActivity.toGUID());

            log("activities: performing activity fetch request " + request);
            activities = Activities.fetchRecent(mApi, request, MAX_LOOKUP_COUNT);

            if (activities.moreResourcesExist()) {
                // delete all activities to avoid gaps in the data
                mResolver.delete(c.uri, null, null);
            }

            final Activity latestActivity = mActivitiesStorage.getLatestActivity(c);
            if (activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(latestActivity))) {
                // this can happen at the beginning of the list if the api returns the first item incorrectly
                inserted = 0;
            } else {
                inserted = mActivitiesStorage.insert(c, activities);
            }
            result.setSyncData(System.currentTimeMillis(), activities.size());
        }

        result.change = inserted > 0 ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
        log("activities: inserted " + inserted + " objects");
        return result;
    }

    private ApiSyncResult syncMe(Content c, long userId) throws IOException {
        ApiSyncResult result = new ApiSyncResult(c.uri);
        User user = new FetchUserTask(mApi).resolve(c.request());
        result.synced_at = System.currentTimeMillis();
        if (user != null) {
            mUserStorage.createOrUpdate(user);
            result.change = ApiSyncResult.CHANGED;
            result.success = true;
        }
        return result;
    }

    /**
     * Good for syncing any generic item that doesn't require special ordering or cache handling
     * e.g. Shortcuts, Connections
     *
     * @param c the content to be synced
     * @return the syncresult
     * @throws IOException
     */
    private ApiSyncResult syncMyGenericResource(Content c) throws IOException {
        log("Syncing generic resource " + c.uri);

        ApiSyncResult result = new ApiSyncResult(c.uri);
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

            result.setSyncData(System.currentTimeMillis(), inserted);
            result.success = true;
        } else {
            Log.w(TAG, "request " + request + " returned " + resp.getStatusLine());
        }
        return result;
    }

    private ApiSyncResult syncMyConnections() throws IOException {
        log("Syncing my connections");

        ApiSyncResult result = new ApiSyncResult(Content.ME_CONNECTIONS.uri);
        // compare local vs remote connections
        List<Connection> list = mApi.readList(Content.ME_CONNECTIONS.request());
        Set<Connection> remoteConnections = new HashSet<Connection>(list);
        ConnectionDAO connectionDAO = new ConnectionDAO(mResolver);
        Set<Connection> storedConnections = new HashSet<Connection>(connectionDAO.queryAll());

        if (storedConnections.equals(remoteConnections)) {
            result.change = ApiSyncResult.UNCHANGED;
        } else {
            result.change = ApiSyncResult.CHANGED;
            // remove unneeded connections
            storedConnections.removeAll(remoteConnections);
            connectionDAO.deleteAll(storedConnections);
        }
        int inserted = connectionDAO.createCollection(remoteConnections);
        result.setSyncData(System.currentTimeMillis(), inserted);
        result.success = true;
        return result;
    }

    private ApiSyncResult syncPlaylist(Uri contentUri) throws IOException {
        log("Syncing playlist " + contentUri);

        ApiSyncResult result = new ApiSyncResult(contentUri);

        try {
            Playlist p = mApi.read(Content.match(contentUri).request(contentUri));

            Cursor c = mResolver.query(
                    Content.PLAYLIST_TRACKS.forId(p.getId()), new String[]{DBHelper.PlaylistTracksView._ID},
                    DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " IS NOT NULL", null,
                    DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " ASC");


            if (c != null && c.getCount() > 0) {
                Set<Long> toAdd = new LinkedHashSet<Long>(c.getCount());
                for (Track t : p.tracks) {
                    toAdd.add(t.getId());
                }
                while (c.moveToNext()) {
                    toAdd.add(c.getLong(0));
                }

                Playlist.ApiUpdateObject updateObject = new Playlist.ApiUpdateObject(toAdd);
                final String content = updateObject.toJson();
                log("Pushing new playlist content to api: " + content);

                Request r = Content.PLAYLIST.request(contentUri).withContent(content, "application/json");
                final ScResource scResource = mApi.update(r);
                if (scResource instanceof Playlist){
                    p = (Playlist) scResource;
                } else {
                    // Debugging. Return objects sometimes are not playlists. In this case the user will lose addition.
                    // This is an edge case so I think its acceptable until we can figure out the root of the problem [JS]
                    SoundCloudApplication.handleSilentException("Error updating playlist " + p, new PlaylistUpdateException(content));
                }
            }
            if (c != null) c.close();

            p = mPlaylistStorage.storeAsync(p).toBlockingObservable().last();
            final Uri insertedUri = p.toUri();
            if (insertedUri != null) {
                log("inserted " + insertedUri.toString());
                result.setSyncData(true, System.currentTimeMillis(), 1, ApiSyncResult.CHANGED);
            } else {
                log("failed to create to " + contentUri);
                result.success = false;
            }
            return result;

        } catch (NotFoundException e) {
            log("Received a 404 on playlist, deleting " + contentUri.toString());
            mPlaylistStorage.removePlaylist(contentUri);
            result.setSyncData(true, System.currentTimeMillis(), 0, ApiSyncResult.CHANGED);
            return result;
        }
    }

    /**
     * Fetch a single resource from the api and create it into the content provider.
     *
     * @param contentUri the content point to get the request and content provider destination from.
     *                   see {@link Content}
     * @return the result of the operation
     * @throws IOException
     */
    private <T extends ScResource> ApiSyncResult doResourceFetchAndInsert(Uri contentUri, Storage<T> storage) throws IOException {
        ApiSyncResult result = new ApiSyncResult(contentUri);
        T resource = mApi.read(Content.match(contentUri).request(contentUri));

        storage.store(resource);

        final Uri insertedUri = resource.toUri();

        if (insertedUri != null) {
            log("inserted " + insertedUri.toString());
            result.setSyncData(true, System.currentTimeMillis(), 1, ApiSyncResult.CHANGED);
        } else {
            log("failed to create to " + contentUri);
            result.success = false;
        }
        return result;
    }

    /**
     * Fetch Api Resources and create them into the content provider. Plain resource inserts, no extra
     * content values will be inserted
     *
     * @throws IOException
     */
    private ApiSyncResult fetchAndInsertCollection(final Content content, Uri contentUri) throws IOException {
        ApiSyncResult result = new ApiSyncResult(contentUri);
        log("fetchAndInsertCollection(" + contentUri + ")");

        Request request = Request.to(content.remoteUri).add("ids", contentUri.getLastPathSegment());

        if (Content.PLAYLIST_LOOKUP.equals(content)) {
            HttpUtils.addQueryParams(request, "representation", "compact");
        }

        List<ScResource> resources = mApi.readFullCollection(request, ScResource.ScResourceHolder.class);

        new BaseDAO<ScResource>(mResolver) {
            @Override
            public Content getContent() {
                return content;
            }
        }.createCollection(resources);
        result.setSyncData(true, System.currentTimeMillis(), resources.size(), ApiSyncResult.CHANGED);
        return result;
    }
}
