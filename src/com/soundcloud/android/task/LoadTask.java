package com.soundcloud.android.task;

import java.lang.ref.WeakReference;

import com.soundcloud.android.activity.ScActivity;
import org.apache.http.client.methods.HttpUriRequest;

import android.os.AsyncTask;
import android.os.Parcelable;

public abstract class LoadTask extends AsyncTask<HttpUriRequest, Parcelable, Boolean> {
    protected WeakReference<ScActivity> mActivityReference;

    public Class<?> loadModel;
    protected boolean mCancelled;

    @Override
    protected void onPreExecute() {
        mCancelled = false;

        System.gc();
    }

    public void setActivity(ScActivity activity) {
        if (activity != null) {
            mActivityReference = new WeakReference<ScActivity>(activity);
            activity.setException(null);
        }
    }

    @Override
    protected void onProgressUpdate(Parcelable... updates) {
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mActivityReference.get() != null)
            mActivityReference.get().setProgressBarIndeterminateVisibility(false);

    }
}
