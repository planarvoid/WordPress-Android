package com.soundcloud.android.task;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.provider.Content;

import java.util.ArrayList;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadActivitiesTask extends RemoteCollectionTask {

    public LoadActivitiesTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        super(app, lazyEndlessAdapter);
    }


    @Override
    protected Boolean doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading activities with params: " + mParams);
        if (mParams.request != null) {
            return doRemoteLoad();
        } else if (mParams.contentUri != null) {
            mNewItems = new ArrayList<Parcelable>();

            for (Activity a : Activities.get(Content.match(mParams.contentUri), mApp.getContentResolver())) {

                a.resolve(mApp);
                mNewItems.add(a);
            }

        } else {
            throw new IllegalArgumentException("Incorrect paramaters");
        }
        return true;
    }
}
