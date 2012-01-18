package com.soundcloud.android.task;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.provider.Content;

import java.util.ArrayList;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadActivitiesTask extends RemoteCollectionTask {

    private String mLastCursor;

    public LoadActivitiesTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        super(app, lazyEndlessAdapter);
    }

    @Override
    protected void respond(){
        EventsAdapterWrapper adapter = (EventsAdapterWrapper) mAdapterReference.get();
        if (adapter != null) {
            adapter.onNewEvents(mNewItems, mLastCursor, mResponseCode, keepGoing, mParams.isRefresh);
        }
    }

    @Override
    protected Boolean doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading activities with params: " + mParams);
        if (mParams.request != null) {
            return doRemoteLoad();
        } else if (mParams.contentUri != null) {
            mNewItems = new ArrayList<Parcelable>();
            Activities activities = Activities.get(Content.match(mParams.contentUri), mApp.getContentResolver());
                for (Activity a : activities) {
                    a.resolve(mApp);
                    mNewItems.add(a);
                }

            mLastCursor = activities.getLastCursor();
            publishProgress(mNewItems);
            return true;
        } else {
            throw new IllegalArgumentException("Incorrect paramaters");
        }
    }
}
