package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.storage.ConnectionDAO;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.Storage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.activities.Activities;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.tasks.FetchUserTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
    private final ActivitiesStorage activitiesStorage;

    private final SoundAssociationStorage soundAssociationStorage;
    private final UserStorage userStorage;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;

    public ApiSyncer(Context context, ContentResolver resolver) {
        this(context, resolver, SoundCloudApplication.fromContext(context).getEventBus());
    }

    public ApiSyncer(Context context, ContentResolver resolver, EventBus eventBus) {
        super(context, resolver);
        activitiesStorage = new ActivitiesStorage();
        soundAssociationStorage = new SoundAssociationStorage();
        userStorage = new UserStorage();
        this.eventBus = eventBus;
        accountOperations = SoundCloudApplication.fromContext(context).getAccountOperations();
    }

    @NotNull
    public ApiSyncResult syncContent(Uri uri, String action) throws IOException {
        final long userId = accountOperations.getLoggedInUserId();
        Content c = Content.match(uri);
        ApiSyncResult result = new ApiSyncResult(uri);

        if (userId <= 0) {
            Log.w(TAG, "Invalid user id, skipping sync ");
        } else if (c.remoteUri != null) {
            switch (c) {
                case ME:
                    result = syncMe(c);
                    if (result.success) {
                        resolver.notifyChange(Content.ME.uri, null);
                        User loggedInUser = accountOperations.getLoggedInUser();
                        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(loggedInUser));
                    }
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putLong(Consts.PrefKeys.LAST_USER_SYNC, System.currentTimeMillis())
                            .apply();

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

                case TRACK:
                case USER:
                    // sucks, but we'll kick out CP anyway
                    Storage<? extends ScResource> storage = c == Content.TRACK ? new TrackStorage() : new UserStorage();
                    result = doResourceFetchAndInsert(uri, storage);
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
                    if (resolver.update(uri, null, null, null) > 0) {
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




    private ApiSyncResult syncSoundAssociations(Content content, Uri uri, long userId) throws IOException {
        log("syncSoundAssociations(" + uri + ")");

        final Request request = Request.to(content.remoteUri, userId)
                .with("limit", 200)
                .with("representation", "mini");

        List<SoundAssociation> associations = api.readFullCollection(request, ScResource.ScResourceHolder.class);
        boolean changed = soundAssociationStorage.syncToLocal(associations, uri);
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
            final Activity oldestActivity = activitiesStorage.getOldestActivity(c);
            Request request = new Request(c.request()).add("limit", Consts.LIST_PAGE_SIZE);
            if (oldestActivity != null) request.add("cursor", oldestActivity.toGUID());
            activities = Activities.fetch(api, request);
            if (activities == null || activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(oldestActivity))) {
                // this can happen at the end of the list
                inserted = 0;
            } else {
                inserted = activitiesStorage.insert(c, activities);
            }
        } else {
            final Activity newestActivity = activitiesStorage.getLatestActivity(c);
            Request request = new Request(c.request());
            if (newestActivity != null) request.add("uuid[to]", newestActivity.toGUID());

            log("activities: performing activity fetch request " + request);
            activities = Activities.fetchRecent(api, request, MAX_LOOKUP_COUNT);

            if (activities.moreResourcesExist()) {
                // delete all activities to avoid gaps in the data
                resolver.delete(c.uri, null, null);
            }

            final Activity latestActivity = activitiesStorage.getLatestActivity(c);
            if (activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(latestActivity))) {
                // this can happen at the beginning of the list if the api returns the first item incorrectly
                inserted = 0;
            } else {
                inserted = activitiesStorage.insert(c, activities);
            }
            result.setSyncData(System.currentTimeMillis(), activities.size());
        }

        result.change = inserted > 0 ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
        log("activities: inserted " + inserted + " objects");
        return result;
    }

    private ApiSyncResult syncMe(Content c) throws IOException {
        ApiSyncResult result = new ApiSyncResult(c.uri);
        User user = new FetchUserTask(api).resolve(c.request());
        result.synced_at = System.currentTimeMillis();
        if (user != null) {
            userStorage.createOrUpdate(user);
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
        HttpResponse resp = api.get(request);

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            List<ScModel> models = api.getMapper().readValue(resp.getEntity().getContent(),
                    api.getMapper().getTypeFactory().constructCollectionType(List.class, c.modelType));

            List<ContentValues> cvs = new ArrayList<ContentValues>(models.size());
            for (ScModel model : models) {
                ContentValues cv = model.buildContentValues();
                if (cv != null) cvs.add(cv);
            }

            int inserted = 0;
            if (!cvs.isEmpty()) {
                inserted = resolver.bulkInsert(c.uri, cvs.toArray(new ContentValues[cvs.size()]));
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
        List<Connection> list = api.readList(Content.ME_CONNECTIONS.request());
        Set<Connection> remoteConnections = new HashSet<Connection>(list);
        ConnectionDAO connectionDAO = new ConnectionDAO(resolver);
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
        T resource = api.read(Content.match(contentUri).request(contentUri));

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

        List<ScResource> resources = api.readFullCollection(request, ScResource.ScResourceHolder.class);

        new BaseDAO<ScResource>(resolver) {
            @Override
            public Content getContent() {
                return content;
            }
        }.createCollection(resources);
        result.setSyncData(true, System.currentTimeMillis(), resources.size(), ApiSyncResult.CHANGED);
        return result;
    }
}
