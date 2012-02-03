package com.soundcloud.android.task;


import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoadActivitiesTask extends AsyncTask<Object, List<? super Parcelable>, Boolean>
        implements ILazyAdapterTask {

    protected SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;

    private Activities mNewActivities;
    private ActivitiesParams mParams;

    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    public static class ActivitiesParams {
        public Uri contentUri;
        public long timestamp;
        public boolean isRefresh;
        @Override
        public String toString() {
            return "ActivitiesParams{" +
                    "contentUri=" + contentUri +
                    ", timestamp=" + timestamp +
                    ", isRefresh=" + isRefresh +
                    '}';
        }
    }

    public LoadActivitiesTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
         mApp = app;
        setAdapter(lazyEndlessAdapter);
    }

    @Override
    public void setAdapter(LazyEndlessAdapter eventsAdapterWrapper) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(eventsAdapterWrapper);
    }

     @Override
    protected void onPostExecute(Boolean success) {
        EventsAdapterWrapper adapter = (EventsAdapterWrapper) mAdapterReference.get();
        if (adapter != null) {
            adapter.onNewEvents(mNewActivities, mParams.isRefresh);
        }
    }


    @Override
    protected Boolean doInBackground(Object... params) {
        mParams = (ActivitiesParams) params[0];
        Log.i(SoundCloudApplication.TAG, getClass().getSimpleName() + "Loading activities with params: " + mParams);

        mNewItems = new ArrayList<Parcelable>();
        if (mParams.isRefresh) {
            mNewActivities = Activities.getSince(mParams.contentUri,mApp.getContentResolver(),
                    mParams.timestamp);
        } else {
            mNewActivities = Activities.getBefore(
                    mParams.contentUri.buildUpon().appendQueryParameter("limit", String.valueOf(Consts.COLLECTION_PAGE_SIZE)).build(),
                    mApp.getContentResolver(),
                    mParams.timestamp);
        }
        return true;

    }
}
