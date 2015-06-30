package com.soundcloud.android.sync;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.storage.Storage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.LegacySyncStrategy;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.api.legacy.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Performs the actual sync with the API. Used by {@link LegacySyncJob}.
 * <p/>
 * As a client, do not use this class directly, but use {@link com.soundcloud.android.sync.SyncInitiator} instead.
 * <p/>
 * TODO: make package level visible again after removing {@link com.soundcloud.android.collections.tasks.ActivitiesLoader}
 * TODO: Split this up into different syncers
 */
public class ApiSyncer extends LegacySyncStrategy {

    private static final int MAX_LOOKUP_COUNT = 100; // each time we sync, lookup a maximum of this number of items

    @Inject ActivitiesStorage activitiesStorage;
    @Inject LegacyUserStorage userStorage;
    @Inject EventBus eventBus;
    @Inject ApiClient apiClient;

    public ApiSyncer(Context context, ContentResolver resolver) {
        super(context, resolver);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ApiSyncer(Context context, ContentResolver resolver, EventBus eventBus, ApiClient apiClient, AccountOperations accountOperations) {
        super(context, resolver, accountOperations);
        activitiesStorage = new ActivitiesStorage();
        userStorage = new LegacyUserStorage();
        this.eventBus = eventBus;
        this.apiClient = apiClient;
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
                        PublicApiUser loggedInUser = accountOperations.getLoggedInUser();
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
                    result = safeSyncActivities(uri, action);
                    break;

                case PLAYLIST_LOOKUP:
                case TRACK_LOOKUP:
                case USER_LOOKUP:
                    result = fetchAndInsertCollection(c, uri);
                    break;

                case TRACK:
                case USER:
                    // sucks, but we'll kick out CP anyway
                    Storage<? extends PublicApiResource> storage = c == Content.TRACK ? new TrackStorage() : new LegacyUserStorage();
                    result = doResourceFetchAndInsert(uri, storage);
                    break;

                case ME_SHORTCUTS:
                    result = syncMyGenericResource(c);
                    break;
            }
        }

        return result;
    }

    /**
     * Safely sync activities, catching NPE caused by bad PublicApi responses, specifically :
     * https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/540f085ae3de5099bace67b3
     *
     * Rethrows as IOException which will be caught in {@link com.soundcloud.android.sync.LegacySyncJob#run()}
     */
    @SuppressWarnings({"PMD.AvoidCatchingGenericException"})
    private ApiSyncResult safeSyncActivities(Uri uri, String action) throws IOException {
        ApiSyncResult result;
        try {
            result = syncActivities(uri, action);
            result.success = true;
        } catch (RuntimeException ex) {
            ErrorUtils.handleSilentException(ex);
            throw new IOException("Problem syncing activities : " + ex);
        }
        return result;
    }

    private ApiSyncResult syncActivities(Uri uri, String action) throws IOException {
        ApiSyncResult result = new ApiSyncResult(uri);
        log("syncActivities(" + uri + "); action=" + action);

        final Content c = Content.match(uri);
        if (ApiSyncService.ACTION_APPEND.equals(action)) {
            appendActivities(result, c);
        } else if (ApiSyncService.ACTION_HARD_REFRESH.equals(action)) {
            hardRefreshActivities(result, c);
        } else {
            prependActivities(result, c);
        }
        return result;
    }

    private void prependActivities(ApiSyncResult result, Content c) throws IOException {
        Activities activities;
        int inserted;
        final Activity newestActivity = activitiesStorage.getLatestActivity(c);
        Request request = new Request(c.request());
        if (newestActivity != null) {
            request.add("uuid[to]", newestActivity.toGUID());
        }

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
        result.change = inserted > 0 ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
    }

    private void hardRefreshActivities(ApiSyncResult result, Content c) throws IOException {
        Activities activities;
        activities = Activities.fetchRecent(api, c.request(), MAX_LOOKUP_COUNT);
        resolver.delete(c.uri, null, null);
        activitiesStorage.insert(c, activities);
        result.setSyncData(true, System.currentTimeMillis(), activities.size(), ApiSyncResult.CHANGED);
    }

    private void appendActivities(ApiSyncResult result, Content c) throws IOException {
        Activities activities;
        int inserted;
        final Activity oldestActivity = activitiesStorage.getOldestActivity(c);
        Request request = new Request(c.request()).add("limit", Consts.LIST_PAGE_SIZE);
        if (oldestActivity != null) {
            request.add("cursor", oldestActivity.toGUID());
        }
        activities = Activities.fetch(api, request);
        if (activities == null || activities.isEmpty() || (activities.size() == 1 && activities.get(0).equals(oldestActivity))) {
            // this can happen at the end of the list
            result.change = ApiSyncResult.UNCHANGED;
        } else {
            inserted = activitiesStorage.insert(c, activities);
            result.change = inserted > 0 ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
        }
    }

    private ApiSyncResult syncMe(Content c) {
        ApiSyncResult result = new ApiSyncResult(c.uri);
        PublicApiUser user;
        ApiRequest request = ApiRequest.get(ApiEndpoints.CURRENT_USER.path())
                .forPublicApi()
                .build();
        try {
            user = apiClient.fetchMappedResponse(request, PublicApiUser.class);
            user.setUpdated();
            SoundCloudApplication.sModelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL);
        } catch (IOException | ApiRequestException | ApiMapperException e) {
            e.printStackTrace();
            user = null;
        }
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
     * e.g. Shortcuts
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

            List<ContentValues> cvs = new ArrayList<>(models.size());
            for (ScModel model : models) {
                ContentValues cv = model.buildContentValues();
                if (cv != null) {
                    cvs.add(cv);
                }
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

    /**
     * Fetch a single resource from the api and create it into the content provider.
     *
     * @param contentUri the content point to get the request and content provider destination from.
     *                   see {@link Content}
     * @return the result of the operation
     * @throws IOException
     */
    private <T extends PublicApiResource> ApiSyncResult doResourceFetchAndInsert(Uri contentUri, Storage<T> storage) throws IOException {
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

        List<PublicApiResource> resources = api.readFullCollection(request, PublicApiResource.ResourceHolder.class);

        new BaseDAO<PublicApiResource>(resolver) {
            @Override
            public Content getContent() {
                return content;
            }
        }.createCollection(resources);
        result.setSyncData(true, System.currentTimeMillis(), resources.size(), ApiSyncResult.CHANGED);
        return result;
    }
}
