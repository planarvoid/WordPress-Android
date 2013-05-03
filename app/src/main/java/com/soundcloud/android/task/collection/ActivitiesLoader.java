package com.soundcloud.android.task.collection;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import android.util.Log;

import java.io.IOException;

public class ActivitiesLoader implements CollectionLoader<Activity> {
    @Override
    public ReturnData<Activity> load(AndroidCloudAPI api, CollectionParams<Activity> params) {
        final ActivitiesStorage storage = new ActivitiesStorage();

        boolean keepGoing, success = false;
        int responseCode = EmptyListView.Status.OK;
        Activities newActivities;

        if (params.isRefresh) {
            newActivities = storage.getSince(params.contentUri, params.timestamp);
            success = true;
        } else {
            newActivities = getOlderActivities(storage, params);
            if (newActivities.size() < params.maxToLoad) {
                try {
                    ApiSyncer syncer = new ApiSyncer(api.getContext(), api.getContext().getContentResolver());
                    ApiSyncer.Result result = syncer.syncContent(params.contentUri, ApiSyncService.ACTION_APPEND);
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
        }
        keepGoing = success && newActivities.size() > 0;
        return new ReturnData<Activity>(newActivities.collection, params, null, responseCode,  keepGoing, success);
    }

    private Activities getOlderActivities(ActivitiesStorage storage, CollectionParams params) {
        return storage.getBefore(
                params.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(params.maxToLoad)).build(),
                params.timestamp);
    }
}
