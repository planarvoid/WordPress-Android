package com.soundcloud.android.task.collection;

import com.soundcloud.android.dao.ActivitiesDAO;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.ApiSyncer;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.util.Log;

import java.io.IOException;

public class ActivitiesLoader extends CollectionLoader<Activity> {
    @Override
    public ReturnData<Activity> load(AndroidCloudAPI api, CollectionParams params) {
        ReturnData<Activity> returnData = new ReturnData<Activity>(params);
        returnData.success = true;

        Activities newActivities;
        final ContentResolver resolver = api.getContext().getContentResolver();
        if (params.isRefresh) {
            newActivities = ActivitiesDAO.getSince(params.contentUri, resolver, params.timestamp);
            returnData.keepGoing = newActivities.size() >= params.maxToLoad;

        } else {
            newActivities = getOlderActivities(resolver, params);
            if (newActivities.size() < params.maxToLoad) {

                ApiSyncer.Result result = null;
                try {
                    result = new ApiSyncer(api.getContext()).syncContent(params.contentUri, ApiSyncService.ACTION_APPEND);
                } catch (CloudAPI.InvalidTokenException e) {
                    // TODO, move this once we centralize our error handling
                    // InvalidTokenException should expose the response code so we don't have to hardcode it here
                    returnData.responseCode = HttpStatus.SC_UNAUTHORIZED;
                    returnData.success = false;
                } catch (IOException e) {
                    Log.w(SoundCloudApplication.TAG, e);
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

        returnData.keepGoing = returnData.success && newActivities.size() > 0;
        returnData.newItems = newActivities;
        return returnData;
    }

    private Activities getOlderActivities(ContentResolver resolver, CollectionParams params) {
        return ActivitiesDAO.getBefore(
                params.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(params.maxToLoad)).build(),
                resolver,
                params.timestamp);
    }
}
