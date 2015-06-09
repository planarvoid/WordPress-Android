package com.soundcloud.android.collections.tasks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.ApiSyncer;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

public class ActivitiesLoader implements CollectionLoader<Activity> {
    @Override
    public ReturnData<Activity> load(PublicCloudAPI api, CollectionParams<Activity> params) {
        final ActivitiesStorage storage = new ActivitiesStorage();

        boolean keepGoing = true;
        boolean success = false;
        int responseCode = HttpStatus.SC_OK;
        Activities newActivities;

        if (params.isRefresh) {
            newActivities = storage.getCollectionSince(params.contentUri, params.timestamp);
            success = true;
        } else {
            newActivities = getOlderActivities(storage, params);
            if (newActivities.size() < params.maxToLoad) {
                try {
                    Context context = SoundCloudApplication.instance;
                    ApiSyncer syncer = new ApiSyncer(context, context.getContentResolver());
                    ApiSyncResult result = syncer.syncContent(params.contentUri, ApiSyncService.ACTION_APPEND);
                    if (result.success) {
                        success = true;
                        newActivities = getOlderActivities(storage, params);
                    }
                } catch (CloudAPI.InvalidTokenException e) {
                    // TODO, move this once we centralize our error handling
                    // InvalidTokenException should expose the response code so we don't have to hardcode it here
                    responseCode = HttpStatus.SC_UNAUTHORIZED;
                } catch (IOException e) {
                    Log.w(SoundCloudApplication.TAG, e);
                }
            } else {
                success = true;
            }
            keepGoing = success && newActivities.size() > 0;
        }
        return new ReturnData<>(newActivities.collection, params, null, responseCode,  keepGoing, success);
    }

    private Activities getOlderActivities(ActivitiesStorage storage, CollectionParams params) {
        return storage.getCollectionBefore(
                params.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(params.maxToLoad)).build(),
                params.timestamp);
    }
}
