
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.objects.Track;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UpdateRecentActivitiesTask extends AsyncTask<Void, Parcelable, Integer> {

    private SoundCloudApplication mApp;
    private ContentResolver mContentResolver;
    private long mCurrentUserId;
    private boolean mExclusive;

    private List<WeakReference<UpdateRecentActivitiesListener>> mListeners = new ArrayList<WeakReference<UpdateRecentActivitiesListener>>();


    protected Track[] tracks;

    public UpdateRecentActivitiesTask(SoundCloudApplication app,
                                      ContentResolver contentResolver,
                                      long userId,
                                      boolean exclusive) {
       mApp = app;
       mContentResolver = contentResolver;
       mCurrentUserId = userId;
       mExclusive = exclusive;
    }

    public void addListener(UpdateRecentActivitiesListener updateListener){

        for (WeakReference<UpdateRecentActivitiesListener> listener : mListeners){
            if (listener.get() != null && listener.get() == updateListener) return;
        }

        mListeners.add(new WeakReference<UpdateRecentActivitiesListener>(updateListener));
    }

    @Override
    protected void onPreExecute() {
        Log.i(getClass().getName(), "Starting event update");
    }

    @Override
    protected void onProgressUpdate(Parcelable... updates) {
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            return SoundCloudDB.updateActivities(mApp, mContentResolver, mCurrentUserId, mExclusive);
        } catch (IOException e) {
            Log.w(SoundCloudApplication.TAG, "error", e);
            return 0;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        Log.i(getClass().getName(), "Done event update with result " + result);

        for (WeakReference<UpdateRecentActivitiesListener> listener : mListeners){
            if (listener.get() != null) listener.get().onUpdate(result);
        }

        mApp.unlockUpdateRecentIncoming(mExclusive);
    }

 // Define our custom Listener interface
    public interface UpdateRecentActivitiesListener {
        public abstract void onUpdate(int added);
    }

}
