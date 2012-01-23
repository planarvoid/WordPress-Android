package com.soundcloud.android.task;

import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.CloudUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadActivitiesTask extends RemoteCollectionTask {

    private long firstActivityTimestamp;
    private long lastActivityTimestamp;
    private Activities mNewActivities;

    public LoadActivitiesTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        super(app, lazyEndlessAdapter);
    }

    @Override
    public void setAdapter(LazyEndlessAdapter eventsAdapterWrapper) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(eventsAdapterWrapper);
        if (!eventsAdapterWrapper.getData().isEmpty()){
            firstActivityTimestamp = ((Activity) eventsAdapterWrapper.getData().get(0)).created_at.getTime();
            lastActivityTimestamp = ((Activity) eventsAdapterWrapper.getData().get(eventsAdapterWrapper.getData().size()-1)).created_at.getTime();
        }
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

        if (mParams.isRefresh && firstActivityTimestamp > 0) {
            mNewActivities = Activities.getSince(mParams.contentUri, mApp.getContentResolver(), firstActivityTimestamp);
        } else if (mParams.pageIndex != -1) {
            mNewActivities = Activities.getBefore(mParams.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build(),
                    mApp.getContentResolver(), lastActivityTimestamp);
        } else {
            mNewActivities = Activities.getBefore(mParams.contentUri, mApp.getContentResolver(), lastActivityTimestamp);
        }

        for (Activity a : mNewActivities) {
            a.resolve(mApp);
        }
        return true;

    }
}
