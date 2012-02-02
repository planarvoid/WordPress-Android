package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;

import android.os.Parcelable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LoadActivitiesTask extends RemoteCollectionTask {
    private Activities mCurrentActivities;
    private Activities mNewActivities;

    public LoadActivitiesTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        super(app, lazyEndlessAdapter);
    }

    @Override
    public void setAdapter(LazyEndlessAdapter eventsAdapterWrapper) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(eventsAdapterWrapper);
        mCurrentActivities = ((EventsAdapterWrapper) eventsAdapterWrapper).getActivities();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        EventsAdapterWrapper adapter = (EventsAdapterWrapper) mAdapterReference.get();
        if (adapter != null) {
            adapter.onNewEvents(mNewActivities, mParams.isRefresh);
        }
    }


    @Override
    protected Boolean doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading activities with params: " + mParams);

        mResponseCode = 0;
        mNewItems = new ArrayList<Parcelable>();
        long lastActivityTimestamp = mCurrentActivities.isEmpty() ? -1 : (mCurrentActivities.get(mCurrentActivities.size() - 1)).created_at.getTime();
        if (mParams.isRefresh) {
                mNewActivities = Activities.refresh(
                        mApp.getContentResolver(),
                        mParams.contentUri,
                        mCurrentActivities,
                        mParams.pageIndex == -1 ? 0 : lastActivityTimestamp
                );
        } else if (mParams.pageIndex != -1) {
            mNewActivities = Activities.mergeWithUriBefore(
                    mApp.getContentResolver(),
                    mParams.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build(),
                    mCurrentActivities.collection,
                    lastActivityTimestamp);
        } else {
            mNewActivities = Activities.EMPTY;
        }

        for (Activity a : mNewActivities) {
            a.resolve(mApp);
        }
        return true;

    }
}
