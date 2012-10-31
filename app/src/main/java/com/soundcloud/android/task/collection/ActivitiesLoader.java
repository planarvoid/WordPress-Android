package com.soundcloud.android.task.collection;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.ApiSyncer;

import android.content.ContentResolver;

import java.io.IOException;

public class ActivitiesLoader extends CollectionLoader<Activity> {
    @Override
    public ReturnData<Activity> load(AndroidCloudAPI api, CollectionParams params) {
        ReturnData<Activity> returnData = new ReturnData<Activity>(params);
        returnData.success = true;

        Activities newActivities;
        final ContentResolver resolver = api.getContext().getContentResolver();
        if (params.isRefresh) {
            newActivities = Activities.getSince(params.contentUri, resolver, params.timestamp);
        } else {
            newActivities = getOlderActivities(resolver, params);
            if (newActivities.size() == 0) {

                ApiSyncer.Result result = null;
                try {
                    result = new ApiSyncer(api.getContext()).syncContent(params.contentUri, ApiSyncService.ACTION_APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                    returnData.success = false;
                }

                if (result != null && result.success) {
                    newActivities = getOlderActivities(resolver, params);
                }
            }
        }

        for (Activity a : newActivities) {
            a.resolve(api.getContext());
        }

        returnData.newItems = newActivities;
        returnData.keepGoing = newActivities.size() == params.maxToLoad;
        return returnData;
    }

    private Activities getOlderActivities(ContentResolver resolver, CollectionParams params) {
        return Activities.getBefore(
                params.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(params.maxToLoad)).build(),
                resolver,
                params.timestamp);
    }
}
