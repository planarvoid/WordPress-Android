package com.soundcloud.android.task;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.ScModel;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RemoteActivitiesTask extends RemoteCollectionTask {

    private Activities mNewActivities;
    private CollectionParams mParams;

    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    public RemoteActivitiesTask(SoundCloudApplication app) {
        super(app);
    }

    public RemoteActivitiesTask(SoundCloudApplication app, Callback callback) {
        super(app, callback);
    }

    @Override
    protected ReturnData doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.d(SoundCloudApplication.TAG, getClass().getSimpleName() + "Loading activities with params: " + mParams);

        mNewItems = new ArrayList<Parcelable>();
        if (mParams.isRefresh) {
            mNewActivities = Activities.getSince(mParams.contentUri, mApp.getContentResolver(),
                    mParams.timestamp);
        } else {
            mNewActivities = Activities.getBefore(
                    mParams.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(mParams.maxToLoad)).build(),
                    mApp.getContentResolver(),
                    mParams.timestamp);
        }

        for (Activity a : mNewActivities) {
            a.resolve(mApp);
        }
        return new ReturnData( mNewActivities, mNextHref, mResponseCode, keepGoing, mParams.isRefresh, true);
    }
}